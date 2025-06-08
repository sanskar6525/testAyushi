
package com.customersupport.Strategy;

import com.customersupport.enums.AgentStatus;
import com.customersupport.enums.IssueStatus;
import com.customersupport.enums.IssueType;
import com.customersupport.model.Agent;
import com.customersupport.model.Issue;

import java.util.*;

public class AssignmentStrategy implements IssueAssignmentStrategy {

    // A simple Map to track the last used index for each issue type for round-robin.
    // We can use a simple Integer because the calling service is synchronized.
    private final Map<IssueType, Integer> lastAssignedIndex = new HashMap<>();

    /**
     * Finds a free agent and assigns them the issue.
     * If no agent is free, the issue is put on a waiting list.
     */
    @Override
    public Optional<Agent> findAndAssignAgent(Issue issue, Map<String, Agent> allAgents, Map<IssueType, Queue<String>> waitingIssuesMap) {

        // --- Step 1: Find all suitable agents who are free ---
        // Instead of using Java Streams, we use a simple for loop.
        List<Agent> suitableAgents = new ArrayList<>();
        for (Agent agent : allAgents.values()) {
            if (agent.getStatus() == AgentStatus.FREE && agent.canHandle(issue.getType())) {
                suitableAgents.add(agent);
            }
        }

        // --- Step 2: Check if any suitable agent was found ---
        if (suitableAgents.isEmpty()) {
            // No free agent found, so add the issue to the waiting queue.
            System.out.println("No free agent available for " + issue.getType() + ". Adding " + issue.getId() + " to waitlist.");

            // Get the queue for this issue type, creating it if it doesn't exist.
            Queue<String> queue = waitingIssuesMap.get(issue.getType());
            if (queue == null) {
                queue = new LinkedList<>(); // Using a simple LinkedList for the queue
                waitingIssuesMap.put(issue.getType(), queue);
            }
            queue.add(issue.getId());
            issue.setStatus(IssueStatus.WAITING);

            return Optional.empty(); // Return empty to signal no agent was assigned.
        }

        // --- Step 3: If agents are available, pick one using Round-Robin ---
        // Get the last index we used for this issue type. Default to -1 if never used.
        int lastIndex = lastAssignedIndex.getOrDefault(issue.getType(), -1);

        // Find the next index, wrapping around if we reach the end of the list.
        int nextIndex = (lastIndex + 1) % suitableAgents.size();

        // Get the agent to assign.
        Agent agentToAssign = suitableAgents.get(nextIndex);

        // Update the last used index for the next time.
        lastAssignedIndex.put(issue.getType(), nextIndex);

        // --- Step 4: Assign the issue ---
        agentToAssign.assignIssue(issue.getId()); // Marks agent as BUSY
        issue.assignAgent(agentToAssign.getAgentId()); // Marks issue as IN_PROGRESS

        return Optional.of(agentToAssign); // Return the agent that was assigned.
    }

    /**
     * Called when an agent becomes free. Checks the waiting list for any work
     * the agent can do.
     */
    @Override
    public Optional<Issue> assignWaitingIssueToAgent(Agent agent, Map<String, Issue> allIssues, Map<IssueType, Queue<String>> waitingIssuesMap) {

        // --- Step 1: Loop through the agent's areas of expertise ---
        for (IssueType expertiseType : agent.getExpertiseTypes()) {
            Queue<String> waitingQueue = waitingIssuesMap.get(expertiseType);

            // --- Step 2: Check if there's a waiting queue for this expertise and if it's not empty ---
            if (waitingQueue != null && !waitingQueue.isEmpty()) {

                // --- Step 3: Get the ID of the issue at the front of the queue ---
                // .poll() gets and REMOVES the first item. If queue is empty, it returns null.
                String issueId = waitingQueue.poll();

                if (issueId != null) {
                    Issue waitingIssue = allIssues.get(issueId);

                    // --- Step 4: Double-check that the issue is still valid to be assigned ---
                    if (waitingIssue != null && waitingIssue.getStatus() == IssueStatus.WAITING) {

                        // --- Step 5: Assign the issue ---
                        agent.assignIssue(waitingIssue.getId());
                        waitingIssue.assignAgent(agent.getAgentId());

                        System.out.println("Agent " + agent.getName() + " picked up waiting issue " + waitingIssue.getId() + ".");
                        return Optional.of(waitingIssue); // Success! Return the assigned issue.
                    }
                }
            }
        }

        // If we finish the loop and haven't found any suitable work.
        System.out.println("No suitable waiting issues for agent " + agent.getName() + ".");
        return Optional.empty();
    }
}