package com.pink.family.assignment.api.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class ResponseFlushingFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException
    {

        try {
            chain.doFilter(request, response);
        } catch (Exception e) {
            log.error("**** oops ****", e);
            // no error sent back!
        }

        System.out.println("**** ResponseFlushingFilter ****");
        // Force flush after every request
        response.flushBuffer();
    }
}