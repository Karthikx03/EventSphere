package com.eventsphere.exception;

/**
 * Thrown when a request is well-formed but breaks a business rule,
 * e.g. booking more seats than are left. Maps to HTTP 409 Conflict.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
