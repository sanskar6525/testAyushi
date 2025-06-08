package com.customersupport.controller;

import com.customersupport.enums.IssueType;
import com.customersupport.model.Agent;
import com.customersupport.service.AgentService;

import java.util.List;
import java.util.Map; // Added for the example in Main, though not directly used in controller's methods here

public class AgentController {
    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    public Agent addAgent(String email, String name, List<IssueType> expertiseTypes) {
        return agentService.addAgent(email, name, expertiseTypes);
    }

    // Renamed from getAgent(String email) to getAgentByEmail to match AgentService
    public Agent getAgentByEmail(String email) {
        return agentService.getAgentByEmail(email);
    }

    // Added to get agent by ID if needed (for internal usage in Main, example)
    public Agent getAgentById(String agentId) {
        return agentService.getAgentById(agentId);
    }

    public List<Agent> listAllAgents() {
        return agentService.getAllAgents();
    }
}
