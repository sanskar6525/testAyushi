package com.customersupport.controller;

import com.customersupport.enums.IssueStatus;
import com.customersupport.enums.IssueType; // Import IssueType enum
import com.customersupport.model.Agent;
import com.customersupport.model.Issue;
import com.customersupport.service.IssueService;
// No direct dependency on AgentService here if agent lookups are handled by Main or a dedicated agent controller

import java.util.List;
import java.util.Map;

public class IssueController {
    private final IssueService issueService;
    // Optional: If IssueController directly needs to get Agent details (e.g., for returning Agent object),
    // it would need AgentService injected here. But based on problem statement, it mainly processes issues.
    // private final AgentService agentService; // Uncomment if needed

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
        // this.agentService = agentService; // Uncomment if needed
    }

    // Changed 'type' parameter to IssueType enum, consistent with IssueService
    public Issue createIssue(String transactionId, String type, // Type is still String here as received from UI/API
                             String subject, String description, String email) {
        return issueService.createIssue(transactionId, type, subject, description, email);
    }

    public Agent assignIssue(String issueId) {
        return issueService.assignIssue(issueId);
    }

    public List<Issue> getIssues(Map<String, String> filter) {
        return issueService.getIssues(filter);
    }

    public void updateIssue(String issueId, IssueStatus status, String resolution) {
        issueService.updateIssue(issueId, status, resolution);
    }

    public void resolveIssue(String issueId, String resolution) {
        issueService.resolveIssue(issueId, resolution);
    }

    public Map<String, List<String>> viewAgentsWorkHistory() {
        return issueService.viewAgentsWorkHistory();
    }
}
