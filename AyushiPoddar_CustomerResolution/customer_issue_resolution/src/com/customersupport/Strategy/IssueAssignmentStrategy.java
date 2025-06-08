package com.customersupport.Strategy;

import com.customersupport.enums.IssueType;
import com.customersupport.model.Agent;
import com.customersupport.model.Issue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;

public interface IssueAssignmentStrategy {
    // Finds a suitable agent for the given issue and assigns it.
    // Returns Optional of assigned Agent if successful, or empty if issue is put in waiting list.
    Optional<Agent> findAndAssignAgent(Issue issue, Map<String, Agent> allAgents, Map<IssueType, Queue<String>> waitingIssuesMap);

    // Attempts to assign a waiting issue to a newly free agent.
    // Returns Optional of assigned Issue if successful, or empty.
    Optional<Issue> assignWaitingIssueToAgent(Agent agent, Map<String, Issue> allIssues, Map<IssueType, Queue<String>> waitingIssuesMap);
}
