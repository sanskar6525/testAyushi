package com.customersupport.service;

import com.customersupport.enums.AgentStatus;
import com.customersupport.enums.IssueStatus;
import com.customersupport.enums.IssueType;
import com.customersupport.exceptions.*; // Import all custom exceptions
import com.customersupport.model.Agent;
import com.customersupport.model.Issue;
import com.customersupport.repository.AgentRepository;
import com.customersupport.repository.IssueRepository;
import com.customersupport.Strategy.IssueAssignmentStrategy; // Import assignment strategy

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue; // For thread-safe queues
import java.util.stream.Collectors;

public class IssueService {
    private final IssueRepository issueRepo;
    private final AgentRepository agentRepo;
    private final IssueAssignmentStrategy assignmentStrategy;

    // Centralized waiting issues, categorized by IssueType
    private final Map<IssueType, Queue<String>> waitingIssues; // IssueType -> Queue of issueIds

    // No explicit lock object is needed when using 'synchronized' methods directly.

    public IssueService(IssueRepository issueRepo, AgentRepository agentRepo, IssueAssignmentStrategy assignmentStrategy) {
        this.issueRepo = issueRepo;
        this.agentRepo = agentRepo;
        this.assignmentStrategy = assignmentStrategy;
        this.waitingIssues = new ConcurrentHashMap<>(); // Thread-safe map for queues
    }

    // 1. createIssue(transactionId, issueType, subject, description, email)
    public synchronized Issue createIssue(String transactionId, String issueTypeStr, String subject, String description, String customerEmail) {
        // The 'synchronized' keyword on the method signature ensures that
        // only one thread can execute this method at a time for this instance.
        // The lock is acquired on 'this' (the IssueService instance) when entering,
        // and released when exiting (normally or via exception).
        // No 'try-finally' block for lock release is needed with 'synchronized' methods.

        // Input Validations
        if (transactionId == null || transactionId.isBlank() || issueTypeStr == null || issueTypeStr.isBlank() ||
                subject == null || subject.isBlank() || description == null || description.isBlank() ||
                customerEmail == null || customerEmail.isBlank()) {
            throw new IllegalArgumentException("All issue fields must be non-empty.");
        }

        IssueType issueType;
        try {
            issueType = IssueType.valueOf(issueTypeStr.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            System.out.println("Warning: Unknown issue type '" + issueTypeStr + "'. Setting to OTHER.");
            issueType = IssueType.OTHER; // Fallback to OTHER type
        }

        String issueId = "I" + (issueRepo.findAll().size() + 1); // Simple sequential ID (in real world, UUID)
        Issue issue = new Issue(issueId, transactionId, issueType, subject, description, customerEmail);
        issueRepo.save(issue);

        System.out.println(">>> Issue " + issue.getId() + " created against transaction \"" + issue.getTransactionId() + "\"");

        // Attempt to assign the issue immediately using the strategy
        // This call happens within the synchronized context.
        //assignIssueInternal(issueId);

        return issue;
    }

    // 2. assignIssue(issueId) - Public method for manual assignment or system retry
    public synchronized Agent assignIssue(String issueId) {
        // This method is also synchronized.
        return assignIssueInternal(issueId); // Delegate to the internal method
    }

    // Internal helper for assignment logic. Assumes the 'synchronized' lock is already held by the caller.
    private Agent assignIssueInternal(String issueId) {
        Issue issue = issueRepo.findById(issueId)
                .orElseThrow(() -> new IssueNotFoundException("Issue with ID '" + issueId + "' not found for assignment."));

        // Only assign if the issue is OPEN or WAITING
        if (issue.getStatus() != IssueStatus.OPEN && issue.getStatus() != IssueStatus.WAITING) {
            System.out.println("Issue " + issueId + " is already " + issue.getStatus() + ". Cannot re-assign.");
            return issue.getAssignedAgentId() != null ? agentRepo.findById(issue.getAssignedAgentId()).orElse(null) : null;
        }

        Map<String, Agent> allAgentsMap = agentRepo.findAll().stream().collect(Collectors.toMap(Agent::getAgentId, a -> a));
        Optional<Agent> assignedAgentOpt = assignmentStrategy.findAndAssignAgent(issue, allAgentsMap, waitingIssues);

        if (assignedAgentOpt.isEmpty()) {
            System.out.println("Issue " + issueId + " could not be assigned immediately. It's now in WAITING state.");
            issueRepo.save(issue); // Persist status change
            return null;
        } else {
            issueRepo.save(issue);
            Agent assignedAgent = assignedAgentOpt.get();
            agentRepo.save(assignedAgent);
            return assignedAgent;
        }
    }


    // 3. getIssues(filter)
    public synchronized List<Issue> getIssues(Map<String, String> filter) {
        // This method is also synchronized to ensure a consistent snapshot of issues
        // while other threads might be creating/updating/resolving them.

        List<Issue> filteredIssues = new ArrayList<>();
        if (filter == null || filter.isEmpty()) {
            return new ArrayList<>(issueRepo.findAll());
        }

        String customerEmailFilter = filter.get("email");
        String issueTypeFilter = filter.get("type");
        String issueIdFilter = filter.get("issueId");
        String issueStatusFilter = filter.get("status");

        return issueRepo.findAll().stream()
                .filter(issue -> {
                    boolean matches = true;
                    if (customerEmailFilter != null && !customerEmailFilter.isBlank()) {
                        matches &= issue.getCustomerEmail().equalsIgnoreCase(customerEmailFilter);
                    }
                    if (issueTypeFilter != null && !issueTypeFilter.isBlank()) {
                        try {
                            IssueType expectedType = IssueType.valueOf(issueTypeFilter.toUpperCase().replace(" ", "_"));
                            matches &= issue.getType() == expectedType;
                        } catch (IllegalArgumentException e) {
                            throw new InvalidFilterException("Invalid issue type provided in filter: " + issueTypeFilter);
                        }
                    }
                    if (issueIdFilter != null && !issueIdFilter.isBlank()) {
                        matches &= issue.getId().equalsIgnoreCase(issueIdFilter);
                    }
                    if (issueStatusFilter != null && !issueStatusFilter.isBlank()) {
                        try {
                            IssueStatus expectedStatus = IssueStatus.valueOf(issueStatusFilter.toUpperCase().replace(" ", "_"));
                            matches &= issue.getStatus() == expectedStatus;
                        } catch (IllegalArgumentException e) {
                            throw new InvalidFilterException("Invalid issue status provided in filter: " + issueStatusFilter);
                        }
                    }
                    return matches;
                })
                .collect(Collectors.toList());
    }

    // 4. updateIssue(issueId, status, resolution)
    public synchronized void updateIssue(String issueId, IssueStatus status, String resolution) {
        // This method is also synchronized.
        Issue issue = issueRepo.findById(issueId)
                .orElseThrow(() -> new IssueNotFoundException("Issue with ID '" + issueId + "' not found for update."));

        // --- Status Transition Validation ---
        if (status == IssueStatus.RESOLVED || status == IssueStatus.CLOSED) {
            throw new InvalidIssueStatusTransitionException("Use resolveIssue() for final resolution. Cannot directly update to RESOLVED/CLOSED via updateIssue().");
        }
        if (issue.getStatus() == IssueStatus.RESOLVED || issue.getStatus() == IssueStatus.CLOSED) {
            throw new InvalidIssueStatusTransitionException("Cannot update an issue that is already " + issue.getStatus() + ".");
        }

        if (status != null && issue.getStatus() != status) {
            if (status == IssueStatus.IN_PROGRESS && issue.getAssignedAgentId() == null) {
                throw new InvalidIssueStatusTransitionException("Issue cannot be IN_PROGRESS without being assigned to an agent.");
            }
            issue.setStatus(status);
        }

        if (resolution != null && !resolution.isBlank()) {
            issue.setResolution(resolution);
        }

        issueRepo.save(issue);
        System.out.println(">>> Issue " + issueId + " status updated to " + issue.getStatus() + " and/or resolution updated.");
    }

    // 5. resolveIssue(issueId, resolution)
    public synchronized void resolveIssue(String issueId, String resolution) {
        // This method is also synchronized.
        Issue issue = issueRepo.findById(issueId)
                .orElseThrow(() -> new IssueNotFoundException("Issue with ID '" + issueId + "' not found for resolution."));

        // --- Status Transition Validation for Resolve ---
        if (issue.getStatus() == IssueStatus.RESOLVED || issue.getStatus() == IssueStatus.CLOSED) {
            System.out.println("Issue " + issueId + " is already " + issue.getStatus() + ".");
            return;
        }
        if (issue.getStatus() != IssueStatus.IN_PROGRESS) {
            throw new InvalidIssueStatusTransitionException("Issue " + issueId + " must be IN_PROGRESS to be RESOLVED. Current status: " + issue.getStatus());
        }

        if (resolution == null || resolution.isBlank()) {
            throw new IllegalArgumentException("Resolution details must be provided to resolve an issue.");
        }

        issue.setStatus(IssueStatus.RESOLVED);
        issue.setResolution(resolution);
        issueRepo.save(issue);
        System.out.println(">>> Issue " + issueId + " marked RESOLVED.");

        String assignedAgentId = issue.getAssignedAgentId();
        if (assignedAgentId != null) {
            Agent agent = agentRepo.findById(assignedAgentId)
                    .orElseThrow(() -> new AgentNotFoundException("Agent with ID '" + assignedAgentId + "' not found for resolved issue " + issueId));
            agent.addToWorkHistory(issueId);
            agent.markFree();
            agentRepo.save(agent);

            Map<String, Issue> allIssuesMap = issueRepo.findAll().stream().collect(Collectors.toMap(Issue::getId, i -> i));
            assignmentStrategy.assignWaitingIssueToAgent(agent, allIssuesMap, waitingIssues);
        } else {
            System.out.println("Issue " + issueId + " was resolved without being assigned to an agent.");
        }
    }

    // 6. viewAgentsWorkHistory()
    public synchronized Map<String, List<String>> viewAgentsWorkHistory() {
        // This method is also synchronized for a consistent read.
        Map<String, List<String>> history = new HashMap<>();
        System.out.println("\n--- Agent Work History ---");
        for (Agent agent : agentRepo.findAll()) {
            history.put(agent.getName() + " (" + agent.getEmail() + ")", agent.getWorkHistory());
        }
        return history;
    }
}