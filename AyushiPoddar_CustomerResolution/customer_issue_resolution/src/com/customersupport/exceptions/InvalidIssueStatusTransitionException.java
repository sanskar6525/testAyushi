package com.customersupport.exceptions;

public class InvalidIssueStatusTransitionException extends RuntimeException {
    public InvalidIssueStatusTransitionException(String message) {
        super(message);
    }
}