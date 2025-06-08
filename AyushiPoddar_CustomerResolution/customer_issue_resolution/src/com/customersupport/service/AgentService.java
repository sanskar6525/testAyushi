package com.customersupport.service;

import com.customersupport.enums.AgentStatus;
import com.customersupport.enums.IssueType;
import com.customersupport.exceptions.AgentNotFoundException;
import com.customersupport.exceptions.InvalidFilterException; // Using new exception for invalid input
import com.customersupport.model.Agent;
import com.customersupport.repository.AgentRepository;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AgentService {
    private final AgentRepository agentRepo;

    public AgentService(AgentRepository agentRepo) {
        this.agentRepo = agentRepo;
    }

    public Agent addAgent(String email, String name, List<IssueType> expertiseTypes) {
        // Input Validations
        if (email == null || email.isEmpty() || name == null || name.isEmpty() || expertiseTypes == null || expertiseTypes.isEmpty()) {
            throw new IllegalArgumentException("Agent email, name, and expertise cannot be empty.");
        }

        // Check if agent already exists (important for unique emails)
        if (agentRepo.findByEmail(email).isPresent()) {
            System.out.println("Agent with email '" + email + "' already exists. Returning existing agent.");
            return agentRepo.findByEmail(email).get(); // Return existing agent
        }

        String agentId = "A" + (agentRepo.findAll().size() + 1); // Simple sequential ID based on current count

        Agent agent = new Agent(agentId, email, name, expertiseTypes);
        return agentRepo.save(agent).orElseThrow(() -> new RuntimeException("Failed to save new agent.")); // Should always be present
    }

    public Agent getAgentByEmail(String email) { // Renamed for clarity
        return agentRepo.findByEmail(email)
                .orElseThrow(() -> new AgentNotFoundException("Agent with email '" + email + "' not found."));
    }

    public Agent getAgentById(String agentId) { // Added for lookup by ID
        return agentRepo.findById(agentId)
                .orElseThrow(() -> new AgentNotFoundException("Agent with ID '" + agentId + "' not found."));
    }

    public List<Agent> getAllAgents() {
        return agentRepo.findAll();
    }
}
