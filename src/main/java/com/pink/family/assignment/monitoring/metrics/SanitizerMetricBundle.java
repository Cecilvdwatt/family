package com.pink.family.assignment.monitoring.metrics;


import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * A very basic micrometer implementation for creating metric.
 * Could be refined further with more meaningful measures, but serves as good "proof of concept".
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SanitizerMetricBundle {

    @NonNull
    private final MeterRegistry meterRegistry;

    /**
     * Record how long a method took.
     */
    public void recordTime(String className, String methodName, Timer.Sample timer) {

        timer.stop(
            meterRegistry.timer(
                "method.time",
                "class", className,
                "method", methodName));
    }

    /**
     * Record the number of failures for a method.
     */
    public void recordError(String className, String methodName, String errorType, Object[] methodArgs) {

        Tags paramTags = Tags.of("class", className, "method", methodName);
        for(int i = 0; i < methodArgs.length; i++) {
            paramTags.and("arg" + i, Objects.toString(methodArgs[i]));
        }

        meterRegistry.counter("method.error", paramTags).increment();

        recordCountAll(className, methodName, methodArgs);
    }

    /**
     * Record the number of success for a method.
     */
    public void recordSuccess(String className, String methodName, Object[] methodArgs) {


        Tags paramTags = Tags.of("class", className, "method", methodName);
        for(int i = 0; i < methodArgs.length; i++) {
            paramTags.and("arg" + i, Objects.toString(methodArgs[i]));
        }

        meterRegistry.counter("method.success",paramTags).increment();

        recordCountAll(className, methodName, methodArgs);
    }

    private void recordCountAll(String className, String methodName, Object[] methodArgs) {

        Tags paramTags = Tags.of("class", className, "method", methodName);
        for(int i = 0; i < methodArgs.length; i++) {
            paramTags.and("arg" + i, Objects.toString(methodArgs[i]));
        }

        meterRegistry.counter("method.success",paramTags).increment();

        // we have a separate counter for all despite that we could just add
        // success and failures because it saves as a bit of processing time.
        // over huge data sets those additions add up.
        meterRegistry.counter("method.all", paramTags).increment();

    }
}



