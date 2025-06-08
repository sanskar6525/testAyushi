package com.customersupport;

import com.customersupport.controller.AgentController;
import com.customersupport.controller.IssueController;
import com.customersupport.enums.IssueType;
import com.customersupport.model.Agent;
import com.customersupport.model.Issue;
import com.customersupport.repository.InMemoryIssueRepository;
import com.customersupport.repository.InMemoryAgentRepository;
import com.customersupport.service.AgentService;
import com.customersupport.service.IssueService;
import com.customersupport.Strategy.AssignmentStrategy;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        // --- System Initialization ---
        InMemoryIssueRepository issueRepo = new InMemoryIssueRepository();
        InMemoryAgentRepository agentRepo = new InMemoryAgentRepository();
        AgentService agentService = new AgentService(agentRepo);
        IssueService issueService = new IssueService(issueRepo, agentRepo, new AssignmentStrategy());
        AgentController agentController = new AgentController(agentService);
        IssueController issueController = new IssueController(issueService);

        try {
            Agent a1 = agentController.addAgent("agent1@test.com", "Agent 1", Arrays.asList(IssueType.PAYMENT_RELATED, IssueType.GOLD_RELATED));
            Agent a2 = agentController.addAgent("agent2@test.com", "Agent 2", Arrays.asList(IssueType.MUTUAL_FUND_RELATED));

            agentController.listAllAgents().forEach(System.out::println);

            Issue i1 = issueController.createIssue("T1", "PAYMENT_RELATED", "Payment Failed", "My payment failed but money is debited", "testUser1@test.com");
            Issue i2 = issueController.createIssue("T2", "MUTUAL_FUND_RELATED", "Purchase Failed", "Unable to purchase Mutual Fund", "testUser2@test.com");
            Issue i3 = issueController.createIssue("T3", "PAYMENT_RELATED", "Payment Failed", "My payment failed but money is debited", "testUser2@test.com");

            //issueController.getIssues(Collections.emptyMap()).forEach(System.out::println);

            System.out.println("Assigning " + i1.getId() + "...");
            issueController.assignIssue(i1.getId()); // Should be assigned to A1

            System.out.println("Assigning " + i2.getId() + "...");
            issueController.assignIssue(i2.getId()); // Should be assigned to A2

            System.out.println("Assigning " + i3.getId() + "...");
            issueController.assignIssue(i3.getId()); // Should go to waitlist as A1 is busy

            issueController.getIssues(Collections.emptyMap()).forEach(System.out::println);



            // Resolve I1 first. This frees up Agent A1.
            // When A1 becomes free, the system will automatically assign the waiting I3 to A1.
            //System.out.println("Resolving " + i1.getId() + " (this will free up Agent 1)...");
            issueController.resolveIssue(i1.getId(), "Payment reversed for T1.");

            // Resolve I2. This frees up Agent A2.
            //System.out.println("Resolving " + i2.getId() + "...");
            issueController.resolveIssue(i2.getId(), "Mutual fund purchase successful on retry.");

            // Now that I3 has been automatically assigned to A1, we can resolve it.
            //System.out.println("Resolving " + i3.getId() + "...");
            issueController.resolveIssue(i3.getId(), "Payment reversed for T3.");


            // --- 8. View Final Agent Work History ---
            //System.out.println("\n--- 8. Final Agent Work History ---");
            Map<String, List<String>> history = issueController.viewAgentsWorkHistory();
            history.forEach((agentDisplay, issueList) -> {
                String agentShortId = "";
                if (agentDisplay.contains("Agent 1")) agentShortId = "A1";
                else if (agentDisplay.contains("Agent 2")) agentShortId = "A2";
                System.out.println(agentShortId + " -> " + issueList);
            });

        } catch (Exception e) {
            System.err.println("\nAn unexpected error occurred during execution: " + e.getMessage());
            e.printStackTrace();
        }
    }
}