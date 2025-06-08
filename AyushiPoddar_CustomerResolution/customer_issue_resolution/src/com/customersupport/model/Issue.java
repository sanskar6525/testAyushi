package com.customersupport.model;

import com.customersupport.enums.IssueStatus;
import com.customersupport.enums.IssueType;

import java.time.LocalDateTime; // For tracking creation and update times

public class Issue {
    private final String id;
    private final String transactionId;
    private final IssueType type; // Changed to IssueType enum
    private final String subject;
    private final String description;
    private final String customerEmail;
    private IssueStatus status;
    private String resolution;
    private String assignedAgentId; // Changed to agentId (String) for consistency with Agent's ID
    private LocalDateTime createdAt; // Added creation timestamp
    private LocalDateTime updatedAt; // Added update timestamp

    public Issue(String id, String transactionId, IssueType type, String subject,
                 String description, String customerEmail) {
        this.id = id;
        this.transactionId = transactionId;
        this.type = type;
        this.subject = subject;
        this.description = description;
        this.customerEmail = customerEmail;
        this.status = IssueStatus.OPEN; // Initially OPEN
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getTransactionId() { return transactionId; }
    public IssueType getType() { return type; } // Getter returns IssueType enum
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public String getCustomerEmail() { return customerEmail; }
    public IssueStatus getStatus() { return status; }
    public String getResolution() { return resolution; }
    public String getAssignedAgentId() { return assignedAgentId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // --- Controlled Setters / Updaters ---
    public void setStatus(IssueStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now(); // Update timestamp on status change
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
        this.updatedAt = LocalDateTime.now(); // Update timestamp on resolution change
    }

    public void setAssignedAgentId(String assignedAgentId) { // Updated setter
        this.assignedAgentId = assignedAgentId;
        this.updatedAt = LocalDateTime.now(); // Update timestamp on assignment
    }

    // Convenience method to handle assignment details in one go
    public void assignAgent(String agentId) {
        this.setAssignedAgentId(agentId);
        this.setStatus(IssueStatus.IN_PROGRESS); // Automatically set to IN_PROGRESS when assigned
    }

}