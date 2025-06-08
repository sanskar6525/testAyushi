package com.customersupport.model;

import com.customersupport.enums.AgentStatus;
import com.customersupport.enums.IssueType;
import com.customersupport.exceptions.AgentBusyException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Agent {
    private final String agentId; // Added agentId for explicit ID, email is unique identifier
    private final String email;
    private final String name;
    private final Set<IssueType> expertiseTypes; // Changed to IssueType enum
    private String currentAssignedIssueId; // Null if free, holds ID of the issue being worked on
    private AgentStatus status; // Added status enum
    private final List<String> workHistory; // Stores issueIds (references to issues worked on)

    // Constructor now takes agentId and expertiseTypes as IssueType
    public Agent(String agentId, String email, String name, List<IssueType> expertiseTypes) {
        this.agentId = agentId;
        this.email = email;
        this.name = name;
        this.expertiseTypes = new HashSet<>(expertiseTypes);
        this.currentAssignedIssueId = null;
        this.status = AgentStatus.FREE; // Initially free
        this.workHistory = new ArrayList<>();
    }

    // --- Getters ---
    public String getAgentId() { return agentId; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public Set<IssueType> getExpertiseTypes() {
        return new HashSet<>(expertiseTypes); // Return a copy for encapsulation
    }
    public String getCurrentAssignedIssueId() { return currentAssignedIssueId; }
    public AgentStatus getStatus() { return status; }
    public List<String> getWorkHistory() {
        return new ArrayList<>(workHistory); // Return a copy for encapsulation
    }

    // --- Business Logic / Controlled Setters ---
    public boolean canHandle(IssueType issueType) { // Helper to check expertise
        return expertiseTypes.contains(issueType);
    }

    public void assignIssue(String issueId)  {
        this.currentAssignedIssueId = issueId;
        this.status = AgentStatus.BUSY;
    }

    public void markFree() {
        this.currentAssignedIssueId = null;
        this.status = AgentStatus.FREE;
    }

    public void addToWorkHistory(String issueId) { // Only adds to history when resolved
        this.workHistory.add(issueId);
    }

}
