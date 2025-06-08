package com.customersupport.exceptions;

public class AgentBusyException extends RuntimeException {
    public AgentBusyException(String message) {
        super(message);
    }
}