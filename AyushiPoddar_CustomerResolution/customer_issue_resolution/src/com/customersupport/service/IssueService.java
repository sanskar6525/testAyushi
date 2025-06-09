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

    // Centralized map to store a waiting Queue for each of the Issue Types that the system provides
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
        // The lock is acquired on 'this' (which is the current instance calling this function, the IssueService instance) when entering,
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
            //Converting the string issueType from input to the specific enum string 
            issueType = IssueType.valueOf(issueTypeStr.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            System.out.println("Warning: Unknown issue type '" + issueTypeStr + "'. Setting to OTHER.");
            issueType = IssueType.OTHER; // Fallback to OTHER type
        }

        //Generating a new issueId
        String issueId = "I" + (issueRepo.findAll().size() + 1); // Simple sequential ID (in real world, UUID)
        //Creating a new issue
        Issue issue = new Issue(issueId, transactionId, issueType, subject, description, customerEmail);
        //saving the issue in our in-memory
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

        // filtering out issues that are not in OPEN or WAITING status and allowing all other status issues to be assigned to an agent
        if (issue.getStatus() != IssueStatus.OPEN && issue.getStatus() != IssueStatus.WAITING) {
            System.out.println("Issue " + issueId + " is already " + issue.getStatus() + ". Cannot re-assign.");
            return issue.getAssignedAgentId() != null ? agentRepo.findById(issue.getAssignedAgentId()).orElse(null) : null;
        }

        //Making a map that holds an Agent Object as value against the agentId as key
        Map<String, Agent> allAgentsMap = agentRepo.findAll().stream().collect(Collectors.toMap(Agent::getAgentId, a -> a));
        Optional<Agent> assignedAgentOpt = assignmentStrategy.findAndAssignAgent(issue, allAgentsMap, waitingIssues);

        //If the findAndAssignAgent method doesn't return an agent, signifies no suitable agent is either available or no agent is free
        //Hence printing the concerned message
        if (assignedAgentOpt.isEmpty()) {
            System.out.println("Issue " + issueId + " could not be assigned immediately. It's now in WAITING state.");
            issueRepo.save(issue); // Persist status change in the in-memory
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

        //Fetching all the filter values against whom issues are to be filtered out
        String customerEmailFilter = filter.get("email");
        String issueTypeFilter = filter.get("type");
        String issueIdFilter = filter.get("issueId");
        String issueStatusFilter = filter.get("status");

        //Using the stream api to iterate through each of the existing ISSUES from issueRepo and filtering out based on the filters given in input
        return issueRepo.findAll().stream()
                .filter(issue -> {
                    boolean matches = true;
                    //CHECKING FOR THE email FILTER
                    if (customerEmailFilter != null && !customerEmailFilter.isBlank()) {
                        matches &= issue.getCustomerEmail().equalsIgnoreCase(customerEmailFilter);
                    }
                    //CHECKING FOR THE issueType FILTER
                    if (issueTypeFilter != null && !issueTypeFilter.isBlank()) {
                        try {
                            //Converting the string issueType from input to the specific enum string 
                            IssueType expectedType = IssueType.valueOf(issueTypeFilter.toUpperCase().replace(" ", "_"));
                            matches &= issue.getType() == expectedType;
                        } catch (IllegalArgumentException e) {
                            throw new InvalidFilterException("Invalid issue type provided in filter: " + issueTypeFilter);
                        }
                    }

                    //CHECKING FOR THE issueId FILTER
                    if (issueIdFilter != null && !issueIdFilter.isBlank()) {
                        matches &= issue.getId().equalsIgnoreCase(issueIdFilter);
                    }

                    //CHECKING FOR THE issueStatus FILTER
                    if (issueStatusFilter != null && !issueStatusFilter.isBlank()) {
                        try {
                            //Converting the string issueStatus from input to the specific enum string 
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
    //Used to update the given issue's status and give the final resolution
    public synchronized void updateIssue(String issueId, IssueStatus status, String resolution) {
        // This method is also synchronized.

        //Fetching the issue from issueRepo based on the issueId from input
        Issue issue = issueRepo.findById(issueId)
                .orElseThrow(() -> new IssueNotFoundException("Issue with ID '" + issueId + "' not found for update."));

       
        //Throwing error if this method is being used to RESOLVE or CANCEL an issue
        if (status == IssueStatus.RESOLVED || status == IssueStatus.CLOSED) {
            throw new InvalidIssueStatusTransitionException("Use resolveIssue() for final resolution. Cannot directly update to RESOLVED/CLOSED via updateIssue().");
        }

        //Throwing error if the issue is already in  RESOLVE or CLOSED status
        //Because we cant allow changes in an issue after it is in RESOLVED or CLOSED status
        if (issue.getStatus() == IssueStatus.RESOLVED || issue.getStatus() == IssueStatus.CLOSED) {
            throw new InvalidIssueStatusTransitionException("Cannot update an issue that is already " + issue.getStatus() + ".");
        }

        //Checking if issue is not already in the status which the user wants it to move into
        if (status != null && issue.getStatus() != status) {

            //Throwing error if the issue is IN_PROGRESS, but there is no assignedAgentId
            if (status == IssueStatus.IN_PROGRESS && issue.getAssignedAgentId() == null) {
                throw new InvalidIssueStatusTransitionException("Issue cannot be IN_PROGRESS without being assigned to an agent.");
            }
            issue.setStatus(status);
        }

        //Setting the resolution
        if (resolution != null && !resolution.isBlank()) {
            issue.setResolution(resolution);
        }

        //Persisting the change in our im-memory repo
        issueRepo.save(issue);
        System.out.println(">>> Issue " + issueId + " status updated to " + issue.getStatus() + " and/or resolution updated.");
    }

    // 5. for resolving the issue
    public synchronized void resolveIssue(String issueId, String resolution) {
        // This method is also synchronized.

        //Fetching the issue from issueRepo based on the issueId from input
        Issue issue = issueRepo.findById(issueId)
                .orElseThrow(() -> new IssueNotFoundException("Issue with ID '" + issueId + "' not found for resolution."));

         //Throwing error if the issue is already in  RESOLVE or CANCEL status
        if (issue.getStatus() == IssueStatus.RESOLVED || issue.getStatus() == IssueStatus.CLOSED) {
            System.out.println("Issue " + issueId + " is already " + issue.getStatus() + ".");
            return;
        }
         //Throwing error if the issue is not in IN_PROGRESS status
        if (issue.getStatus() != IssueStatus.IN_PROGRESS) {
            throw new InvalidIssueStatusTransitionException("Issue " + issueId + " must be IN_PROGRESS to be RESOLVED. Current status: " + issue.getStatus());
        }

        //Throwing error if no resolution is given to the issue
        if (resolution == null || resolution.isBlank()) {
            throw new IllegalArgumentException("Resolution details must be provided to resolve an issue.");
        }

        issue.setStatus(IssueStatus.RESOLVED);
        issue.setResolution(resolution);
        issueRepo.save(issue);
        System.out.println(">>> Issue " + issueId + " marked RESOLVED.");

        //Fetching the agentId to whom the issue was assigned
        String assignedAgentId = issue.getAssignedAgentId();
        if (assignedAgentId != null) {
            Agent agent = agentRepo.findById(assignedAgentId)
                    .orElseThrow(() -> new AgentNotFoundException("Agent with ID '" + assignedAgentId + "' not found for resolved issue " + issueId));

            //Adding this resolved issue to the agent's workhistory
            agent.addToWorkHistory(issueId);

            //making the agent available now
            agent.markFree();
            agentRepo.save(agent);

            Map<String, Issue> allIssuesMap = issueRepo.findAll().stream().collect(Collectors.toMap(Issue::getId, i -> i));

            //As this agent is now free, trying to get this agent assigned to other available issue that falls in his/her expertise
            assignmentStrategy.assignWaitingIssueToAgent(agent, allIssuesMap, waitingIssues);
        } else {
            System.out.println("Issue " + issueId + " was resolved without being assigned to an agent.");
        }
    }

    // 6. viewAgentsWorkHistory()
    public synchronized Map<String, List<String>> viewAgentsWorkHistory() {
        // This method is also synchronized for a consistent read.

        //this is to make a map that contains a list of issues that the agent has worked upon as value against the agent's name as key
        Map<String, List<String>> history = new HashMap<>();
        System.out.println("\n--- Agent Work History ---");

        //Iterating across each of the agents present inside the agentRepo, to create workhistory for all agents
        for (Agent agent : agentRepo.findAll()) {
            history.put(agent.getName() + " (" + agent.getEmail() + ")", agent.getWorkHistory());
        }
        return history;
    }
}
