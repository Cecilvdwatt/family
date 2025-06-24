package com.pink.family.assignment.api.exception;

/**
 * Thrown when the code does not work as was intended likely due to a data or coding error.
 */
public class PinkSystemException extends RuntimeException {

    public PinkSystemException(String message) {
        super(message);
    }
}
