package com.customersupport.repository;

import com.customersupport.model.Issue;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryIssueRepository implements IssueRepository {
    private final Map<String, Issue> issueMap = new ConcurrentHashMap<>();

    @Override
    public Issue save(Issue issue) {
        issueMap.put(issue.getId(), issue);
        return issue;
    }

    @Override
    public Optional<Issue> findById(String issueId) {
        return Optional.ofNullable(issueMap.get(issueId));
    }

    @Override
    public List<Issue> findAll() {
        return new ArrayList<>(issueMap.values());
    }
}
