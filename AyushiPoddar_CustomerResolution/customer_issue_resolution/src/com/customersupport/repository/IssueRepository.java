package com.customersupport.repository;

import com.customersupport.model.Issue;

import java.util.List;
import java.util.Optional;

public interface IssueRepository {
    Issue save(Issue issue);
    Optional<Issue> findById(String issueId);
    List<Issue> findAll();
}
