package com.customersupport.exceptions;

public class InvalidFilterException extends RuntimeException {
    public InvalidFilterException(String message) {
        super(message);
    }
}