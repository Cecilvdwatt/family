package com.pink.family.assignment.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.UUID;

/**
 * Service used for logging.
 * Used {@link MDC} to keep track of request information.
 */
@Slf4j
@Service
public class LoggingService {

    /**
     * Generate a new ID.
     */
    public String generateID() {
        String id = UUID.randomUUID().toString();
        log.debug("Generated new ID: {}", id);
        return id;
    }

    /**
     * Set the Request ID
     */
    public void setRequestId(String id) {
        if(ObjectUtils.isEmpty(id)) {
            id = generateID();
        }
        MDC.put(Constants.REQUEST_ID_MDC, id);
    }

    /**
     * Get the Request ID
     */
    public String getRequestId() {

        // MDC is thread specific, so for each API call it will have its own MDC context.
        String requestId = MDC.get(Constants.REQUEST_ID_MDC);

        if(ObjectUtils.isEmpty(requestId)) {
            requestId = generateID();
            setRequestId(requestId);
        }

        return requestId;
    }


    public static class Constants {
        public static final String REQUEST_ID_MDC = "CORRELATION_ID_MDC_MARKER";
    }

}
