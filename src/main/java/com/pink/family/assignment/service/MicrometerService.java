package com.pink.family.assignment.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MicrometerService {

    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timer> timers = new ConcurrentHashMap<>();

    public void increment(String name) {
        counters.computeIfAbsent(name, n ->
            Counter.builder(n)
                .description("Custom counter: " + n)
                .register(meterRegistry)
        ).increment();
    }

    public void time(String name, Timer.Sample sample) {
        sample.stop(getTimer(name));
    }

    public Timer getTimer(String name) {
        return Timer.builder(name)
            .register(meterRegistry);
    }

    public Timer.Sample getSample() {
        return Timer.start(meterRegistry);
    }
}

