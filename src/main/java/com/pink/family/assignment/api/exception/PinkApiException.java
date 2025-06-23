package com.pink.family.assignment.api.exception;

import lombok.Getter;

/**
 * General catch all for any issues that arose during the API call.
 */
@Getter
public class PinkApiException extends RuntimeException {

    private final int responseCode;

    public PinkApiException(String message, int code){
        super(message);
        responseCode = code;
    }


}
