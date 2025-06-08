package com.customersupport.repository;

import com.customersupport.model.Agent;

import java.util.List;
import java.util.Optional;

public interface AgentRepository {
    // save now returns Optional to indicate if a new agent was saved or existing one was updated/returned
    Optional<Agent> save(Agent agent);
    Optional<Agent> findByEmail(String email);
    Optional<Agent> findById(String agentId); // Added findById for consistency
    List<Agent> findAll();
}
