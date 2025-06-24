package com.pink.family.assignment.api.exception;

/**
 * Exception used to get debug information.
 */
public class PinkDebugException extends Exception {

    public PinkDebugException(String message) {
        super(message);
    }

    public static PinkDebugException inst(){
        return new PinkDebugException("DEBUG");
    }
}
