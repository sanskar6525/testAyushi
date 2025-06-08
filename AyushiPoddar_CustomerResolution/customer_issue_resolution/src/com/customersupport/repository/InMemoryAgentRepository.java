package com.customersupport.repository;

import com.customersupport.model.Agent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAgentRepository implements AgentRepository {
    private final Map<String, Agent> agentMapByEmail = new ConcurrentHashMap<>(); // Map by email
    private final Map<String, Agent> agentMapById = new ConcurrentHashMap<>();   // Map by ID

    @Override
    public Optional<Agent> save(Agent agent) {
        // In a real system, you'd likely have a database handling unique constraints.
        // For in-memory, let's say saving adds or updates.
        // If agent with this email already exists, return existing (for addAgent check).
        if (agentMapByEmail.containsKey(agent.getEmail())) {
            // If already exists, return the existing agent, don't overwrite if it's meant to be unique add.
            // Or overwrite if it's an update operation. For now, assume adding new or updating existing.
            agentMapByEmail.put(agent.getEmail(), agent); // Overwrite if it's an update
            agentMapById.put(agent.getAgentId(), agent);
            return Optional.of(agent);
        } else {
            agentMapByEmail.put(agent.getEmail(), agent);
            agentMapById.put(agent.getAgentId(), agent);
            return Optional.of(agent);
        }
    }

    @Override
    public Optional<Agent> findByEmail(String email) {
        return Optional.ofNullable(agentMapByEmail.get(email));
    }

    @Override
    public Optional<Agent> findById(String agentId) { // New method
        return Optional.ofNullable(agentMapById.get(agentId));
    }

    @Override
    public List<Agent> findAll() {
        return new ArrayList<>(agentMapByEmail.values());
    }
}