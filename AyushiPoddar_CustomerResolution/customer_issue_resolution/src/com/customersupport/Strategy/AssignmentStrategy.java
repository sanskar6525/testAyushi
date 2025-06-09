
package com.customersupport.Strategy;

import com.customersupport.enums.AgentStatus;
import com.customersupport.enums.IssueStatus;
import com.customersupport.enums.IssueType;
import com.customersupport.model.Agent;
import com.customersupport.model.Issue;

import java.util.*;

public class AssignmentStrategy implements IssueAssignmentStrategy {

    // Map that stores the last assigned index against each ISSUE TYPE
    private final Map<IssueType, Integer> lastAssignedIndex = new HashMap<>();

    /**
     * Finds a free agent and assigns them the issue.
     * If no agent is free, the issue is put on a waiting list.
     */
    @Override
    public Optional<Agent> findAndAssignAgent(Issue issue, Map<String, Agent> allAgents, Map<IssueType, Queue<String>> waitingIssuesMap) {

        // --- Step 1: Finding a list of all suitable agents who are free and whose expertise includes the current ISSUE TYPE ---
        List<Agent> suitableAgents = new ArrayList<>();
        for (Agent agent : allAgents.values()) {
            if (agent.getStatus() == AgentStatus.FREE && agent.canHandle(issue.getType())) {
                suitableAgents.add(agent);
            }
        }

        // If no suitable or no free agent is available, we would add the issue in the waiting queue for that specific ISSUE TYPE
        if (suitableAgents.isEmpty()) {
            // No free agent found, so add the issue to the waiting queue.
            System.out.println("No free agent available for " + issue.getType() + ". Adding " + issue.getId() + " to waitlist.");

            // Fetching the waiting queue for this issue type, if it doesn't exist, then creating a new one and adding it in the waitingIssueMap
            Queue<String> queue = waitingIssuesMap.get(issue.getType());
            if (queue == null) {
                queue = new LinkedList<>(); // Using a simple LinkedList for the queue
                waitingIssuesMap.put(issue.getType(), queue);
            }
            //Adding the current issue in the queue
            queue.add(issue.getId());
            issue.setStatus(IssueStatus.WAITING);

            return Optional.empty(); // Return empty to signal no agent was assigned.
        }

        // --- Step 3 : Now, if there are agents available, 
        //we would find a suitable index position from the suitableAgents list, to whom we can assign the issue by the round-robin principle

        
        // For that we would first fetch the index to which issues of this given issueType was previously assigned to 
        int lastIndex = lastAssignedIndex.getOrDefault(issue.getType(), -1);

        // Find the next index by incrementing the previously used by 1 
        //and if the new Index comes out to be greater than the size of the list of suitable Agents,
        //we would just make the index position to circle back to the start of the list
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
     * this is called when agent who is free and an issue from the waiting queue is to be assigned to him/her
     * Called when an agent becomes free. Checks the waiting list for any work
     */
    @Override
    public Optional<Issue> assignWaitingIssueToAgent(Agent agent, Map<String, Issue> allIssues, Map<IssueType, Queue<String>> waitingIssuesMap) {

        // --- Step 1: Iterating through the given agent's list of expertise
        for (IssueType expertiseType : agent.getExpertiseTypes()) {
            //Fetching the waiting queue for this IssueType
            Queue<String> waitingQueue = waitingIssuesMap.get(expertiseType);

            // --- Step 2: Making sure that there exists a waiting queue for this IssueType and it is not empty
            if (waitingQueue != null && !waitingQueue.isEmpty()) {

                // --- Step 3: fetching the out the issueId that exists at the very front of the queue, 
                //so that issues are assigned in the order they came in
                String issueId = waitingQueue.poll();

                //Just a validation for issueId
                if (issueId != null) {
                    Issue waitingIssue = allIssues.get(issueId);

                    // --- Step 4: Double-check that the issue is still valid to be assigned by checking if it is in WAITING status or not
                    if (waitingIssue != null && waitingIssue.getStatus() == IssueStatus.WAITING) {

                        // --- Step 5: Assign the issue to this agent---
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
