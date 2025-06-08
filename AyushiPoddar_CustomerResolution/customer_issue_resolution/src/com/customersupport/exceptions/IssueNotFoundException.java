package com.customersupport.exceptions;

public class IssueNotFoundException extends RuntimeException {
    public IssueNotFoundException(String message) {
        super(message);
    }
}