package com.pink.family.assignment.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * General catch all for any issues that arose during the API call.
 */
@Getter
public class ApiException extends RuntimeException {

    private final int responseCode;

    public ApiException(String message, int code){
        super(message);
        responseCode = code;
    }


}
