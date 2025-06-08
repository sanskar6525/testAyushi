package com.customersupport.exceptions;

public class NoAgentAvailableException extends RuntimeException {
    public NoAgentAvailableException(String message) {
        super(message);
    }
}