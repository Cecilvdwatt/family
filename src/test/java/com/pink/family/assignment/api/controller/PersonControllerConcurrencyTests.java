package com.pink.family.assignment.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pink.family.api.rest.server.model.SpecificPersonCheckRequest;
import com.pink.family.assignment.constants.ErrorMessages;
import com.pink.family.assignment.database.dao.PersonDao;
import com.pink.family.assignment.database.dao.PersonRelationshipDao;
import com.pink.family.assignment.database.entity.PersonEntity;
import com.pink.family.assignment.database.entity.enums.RelationshipType;
import com.pink.family.assignment.service.PersonService;
import io.micrometer.core.instrument.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.is;

/**
 * Just a little fun class to check multithreaded performance.
 */
@Slf4j
@SpringBootTest(properties = {
    "logging.level.com.pink.family=INFO"
})
@AutoConfigureMockMvc
class PersonControllerConcurrencyTests {

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PersonDao personDao;

    @Autowired
    private PersonRelationshipDao personRelationshipDao;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        personRelationshipDao.deleteAll();
        personRelationshipDao.flush();

        personDao.deleteAll();
        personDao.flush();

        meterRegistry.clear();
    }

    @Test
    void concurrencySimpleSuccess() throws Exception {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.execute(status -> {
            personRelationshipDao.deleteAll();
            personRelationshipDao.flush();

            personDao.deleteAll();
            personDao.flush();

            PersonEntity main = PersonEntity.builder()
                .name("Jane")
                .externalId(123456789L)
                .dateOfBirth(LocalDate.of(1990, 5, 20))
                .build();

            PersonEntity partner = PersonEntity.builder()
                .name("John")
                .externalId(987654321L)
                .dateOfBirth(LocalDate.of(1988, 3, 15))
                .build();

            main = personDao.save(main);
            partner = personDao.save(partner);

            PersonEntity child1 = PersonEntity.builder()
                .name("Child1")
                .externalId(new Random().nextLong())
                .dateOfBirth(LocalDate.now().minusYears(10))
                .build();

            PersonEntity child2 = PersonEntity.builder()
                .name("Child2")
                .externalId(new Random().nextLong())
                .dateOfBirth(LocalDate.now().minusYears(20))
                .build();

            PersonEntity child3 = PersonEntity.builder()
                .name("Child3")
                .externalId(new Random().nextLong())
                .dateOfBirth(LocalDate.now().minusYears(22))
                .build();

            child1 = personDao.save(child1);
            child2 = personDao.save(child2);
            child3 = personDao.save(child3);

            main.addRelationship(partner, RelationshipType.PARTNER, RelationshipType.PARTNER);
            partner.addRelationship(main, RelationshipType.PARTNER, RelationshipType.PARTNER);

            for (PersonEntity child : List.of(child1, child2, child3)) {
                main.addRelationship(child, RelationshipType.PARENT, RelationshipType.CHILD);
                partner.addRelationship(child, RelationshipType.PARENT, RelationshipType.CHILD);
                child.addRelationship(main, RelationshipType.CHILD, RelationshipType.PARENT);
                child.addRelationship(partner, RelationshipType.CHILD, RelationshipType.PARENT);
            }

            personDao.save(main);
            personDao.save(partner);
            personDao.save(child1);
            personDao.save(child2);
            personDao.save(child3);
            personDao.flush();

            return null;
        });

        final int[] threadCounts = {1, 10, 100, 1000, 10000};

        for (int threadCount : threadCounts) {
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            List<Future<Long>> futures = new CopyOnWriteArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                int finalI = i;
                futures.add(executor.submit(() -> {
                    long start = System.nanoTime();

                    SpecificPersonCheckRequest request = new SpecificPersonCheckRequest()
                        .requestId("RQ" + finalI)
                        .name("Jane")
                        .id(123456789L)
                        .dateOfBirth(LocalDate.of(1990, 5, 20));

                    mockMvc.perform(post("/v1/people/check-existing-person")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                        .andExpect(content().string(""))
                        .andExpect(status().isOk());

                    return System.nanoTime() - start;
                }));
            }

            executor.shutdown();
            if(!executor.awaitTermination(5, TimeUnit.MINUTES)) {
                throw new RuntimeException("Timeout waiting for threads to finish");
            }

            long totalTime = 0;
            for (Future<Long> future : futures) {
                totalTime += future.get();
            }

            double avgNanos = (double) totalTime / threadCount;
            double avgMillis = avgNanos / 1_000_000.0;
            double avgSeconds = avgMillis / 1000.0;

            putMeasures(avgMillis, avgSeconds, threadCount, totalTime);

        }
    }

    private void putMeasures(double avgMillis, double avgSeconds, double threadCount, double totalTime) {
        log.info(
            """
            ***************************************************
            "***********************************
            "********************
            "Average response time: {} ms ({} s), Threads: {}, Total Time (ns): {}
            """,
            String.format("%.3f", avgMillis),
            String.format("%.3f", avgSeconds),
            threadCount, totalTime);

        outputMicro();

        log.info(
            """
            ********************
            ***********************************
            ***************************************************""");
    }

    private void outputMicro() {
        meterRegistry.getMeters()
            .forEach(meter -> {
                String name = meter.getId().getName();
                StringBuilder metricOutput = new StringBuilder("Metric: ").append(name);

                if (meter instanceof Counter) {
                    metricOutput.append(" (Counter) = ").append(((Counter) meter).count());
                } else if (meter instanceof Gauge) {
                    metricOutput.append(" (Gauge) = ").append(((Gauge) meter).value());
                } else if (meter instanceof Timer timer) {
                    metricOutput.append(" (Timer)");
                    metricOutput.append(" | Count = ").append(timer.count());
                    metricOutput.append(" | Total Time (seconds) = ").append(timer.totalTime(getBaseTimeUnit()));
                    metricOutput.append(" | Max (seconds) = ").append(timer.max(getBaseTimeUnit()));
                } else if (meter instanceof DistributionSummary summary) {
                    metricOutput.append(" (DistributionSummary)");
                    metricOutput.append(" | Count = ").append(summary.count());
                    metricOutput.append(" | Total = ").append(summary.totalAmount());
                    metricOutput.append(" | Max = ").append(summary.max());
                } else if (meter instanceof LongTaskTimer ltt) {
                    metricOutput.append(" (LongTaskTimer)");
                    metricOutput.append(" | Active Tasks = ").append(ltt.activeTasks());
                    metricOutput.append(" | Duration (seconds) = ").append(ltt.duration(getBaseTimeUnit()));
                } else if (meter instanceof FunctionCounter) {
                    metricOutput.append(" (FunctionCounter) = ").append(((FunctionCounter) meter).count());
                } else if (meter instanceof FunctionTimer ft) {
                    metricOutput.append(" (FunctionTimer)");
                    metricOutput.append(" | Count = ").append(ft.count());
                    metricOutput.append(" | Total Time (seconds) = ").append(ft.totalTime(getBaseTimeUnit()));
                } else {
                    Double value = StreamSupport.stream(meter.measure().spliterator(), false)
                        .filter(ms -> ms.getStatistic() == Statistic.VALUE || ms.getStatistic() == Statistic.COUNT || ms.getStatistic() == Statistic.TOTAL)
                        .map(Measurement::getValue)
                        .findFirst()
                        .orElse(Double.NaN);
                    metricOutput.append(" (Unknown/Other Meter Type) = ").append(value);
                }
                log.info("\n{}", metricOutput);
            });

        meterRegistry.clear();
    }

    private TimeUnit getBaseTimeUnit() {
        return TimeUnit.SECONDS;
    }

    @Test
    @DisplayName("Concurrency test with 1 thread should return expected error")
    void concurrencyWithVaryingThreadCounts() throws Exception {
        PersonEntity main = PersonEntity.builder()
            .name("TestUser")
            .externalId(1122334455L)
            .dateOfBirth(LocalDate.of(1985, 5, 15))
            .build();

        PersonEntity partner1 = PersonEntity.builder()
            .name("PartnerOne")
            .externalId(9988776655L)
            .dateOfBirth(LocalDate.of(1984, 4, 10))
            .build();

        PersonEntity partner2 = PersonEntity.builder()
            .name("PartnerTwo")
            .externalId(8877665544L)
            .dateOfBirth(LocalDate.of(1983, 3, 20))
            .build();

        main = personDao.save(main);
        partner1 = personDao.save(partner1);
        partner2 = personDao.save(partner2);

        PersonEntity child1 = personDao.save(createChild("Child1", LocalDate.of(2008, 1, 1)));
        PersonEntity child2 = personDao.save(createChild("Child2", LocalDate.of(2009, 2, 2)));
        PersonEntity child3 = personDao.save(createChild("Child3", LocalDate.of(2010, 3, 3)));

        main.addRelationship(partner1, RelationshipType.PARTNER, RelationshipType.PARTNER);
        main.addRelationship(child1, RelationshipType.PARENT, RelationshipType.CHILD);
        main.addRelationship(child2, RelationshipType.PARENT, RelationshipType.CHILD);
        main.addRelationship(child3, RelationshipType.PARENT, RelationshipType.CHILD);

        partner1.addRelationship(child1, RelationshipType.PARENT, RelationshipType.CHILD);
        partner2.addRelationship(child2, RelationshipType.PARENT, RelationshipType.CHILD);
        partner1.addRelationship(child3, RelationshipType.PARENT, RelationshipType.CHILD);

        personDao.saveAndFlush(main);
        personDao.saveAndFlush(partner1);
        personDao.saveAndFlush(partner2);

        SpecificPersonCheckRequest request = new SpecificPersonCheckRequest()
            .requestId("RQ2")
            .name("TestUser")
            .id(1122334455L)
            .dateOfBirth(LocalDate.of(1985, 5, 15));

        int threads = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>();

        long start = System.nanoTime();

        for (int i = 0; i < threads; i++) {
            futures.add(executorService.submit(() -> {
                try {
                    mockMvc.perform(post("/v1/people/check-existing-person")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().is(444))
                        .andExpect(jsonPath("$.code").value("444"))
                        .andExpect(jsonPath("$.message", is(ErrorMessages.NO_SHARED_CHILDREN)))
                        .andExpect(jsonPath("$.requestId", is("RQ2")));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        executorService.shutdown();

        long duration = System.nanoTime() - start;
        double avgNanos = (double) duration / threads;
        double avgMillis = avgNanos / 1_000_000.0;
        double avgSeconds = avgMillis / 1000.0;

        putMeasures(avgMillis, avgSeconds, threads, duration);
    }

    private PersonEntity createChild(String name, LocalDate dob) {
        return PersonEntity.builder()
            .name(name)
            .externalId(new Random().nextLong())
            .dateOfBirth(dob)
            .build();
    }
}
