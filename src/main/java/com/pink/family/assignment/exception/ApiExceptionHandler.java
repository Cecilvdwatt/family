package com.pink.family.assignment.exception;

import com.pink.family.api.rest.server.model.ErrorResponse;
import com.pink.family.assignment.service.LoggingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Exception handling for the API
 */
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class ApiExceptionHandler {

    private final LoggingService loggingService;

    /**
     * And expected error occurred during an API call.
     */
    @ExceptionHandler(PinkApiException.class)
    public ResponseEntity<ErrorResponse> handleApiError(PinkApiException ex) {

        ErrorResponse error = new ErrorResponse();
        error.setCode(String.valueOf(ex.getResponseCode()));
        error.setMessage(ex.getMessage());
        error.setRequestId(loggingService.getRequestId());
        return ResponseEntity.status(ex.getResponseCode()).body(error);
    }

    /**
     * Catch any unexpected errors
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {

        log.error("Unexpected Error!", ex);
        ErrorResponse error = new ErrorResponse();
        error.setCode("500");
        error.setMessage(ex.getMessage());
        error.setRequestId(loggingService.getRequestId());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
