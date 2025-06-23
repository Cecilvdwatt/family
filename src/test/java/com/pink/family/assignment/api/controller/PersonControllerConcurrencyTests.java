package com.pink.family.assignment.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pink.family.api.rest.server.model.SpecificPersonCheckRequest;
import com.pink.family.assignment.database.entity.PersonEntity;
import com.pink.family.assignment.database.entity.enums.RelationshipType;
import com.pink.family.assignment.database.repository.PersonRelationshipRepository;
import com.pink.family.assignment.database.repository.PersonRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.FunctionTimer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Statistic;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    private PersonRepository personRepository;

    @Autowired
    private PersonRelationshipRepository personRelationshipRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        personRelationshipRepository.deleteAll();
        personRelationshipRepository.flush();

        personRepository.deleteAll();
        personRepository.flush();

        meterRegistry.clear();
    }

    @Test
    void concurrencySimpleSuccess() throws Exception {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.execute(status -> {
            personRelationshipRepository.deleteAll();
            personRelationshipRepository.flush();

            personRepository.deleteAll();
            personRepository.flush();

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

            PersonEntity child1 = createChild("Child1", LocalDate.of(2010, 1, 1));
            PersonEntity child2 = createChild("Child2", LocalDate.of(2012, 2, 2));
            PersonEntity child3 = createChild("Child3", LocalDate.now().minusYears(10));

            main = personRepository.save(main);
            partner = personRepository.save(partner);
            child1 = personRepository.save(child1);
            child2 = personRepository.save(child2);
            child3 = personRepository.save(child3);

            main.addRelationship(partner, RelationshipType.PARTNER, RelationshipType.PARTNER);
            main.addRelationship(child1, RelationshipType.CHILD, RelationshipType.PARENT);
            main.addRelationship(child2, RelationshipType.CHILD, RelationshipType.PARENT);
            main.addRelationship(child3, RelationshipType.CHILD, RelationshipType.PARENT);

            partner.addRelationship(child1, RelationshipType.CHILD, RelationshipType.PARENT);
            partner.addRelationship(child2, RelationshipType.CHILD, RelationshipType.PARENT);
            partner.addRelationship(child3, RelationshipType.CHILD, RelationshipType.PARENT);

            personRepository.saveAndFlush(main);
            personRepository.saveAndFlush(partner);
            personRepository.saveAndFlush(child1);
            personRepository.saveAndFlush(child2);
            personRepository.saveAndFlush(child3);

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
                        .andExpect(content().string("")) // body check first
                        .andExpect(status().isOk());    // then status check 200

                    return System.nanoTime() - start;
                }));
            }

            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.MINUTES);

            long totalTime = 0;
            for (Future<Long> future : futures) {
                totalTime += future.get();
            }

            double avgNanos = (double) totalTime / threadCount;
            double avgMillis = avgNanos / 1_000_000.0;
            double avgSeconds = avgMillis / 1000.0;

            log.info(
                "\n***************************************************" +
                    "\n***********************************" +
                    "\n********************" +
                    "\nAverage response time: {} ms ({} s), Threads: {}, Total Time (ns): {}\n",
                String.format("%.3f", avgMillis),
                String.format("%.3f", avgSeconds),
                threadCount, totalTime);

            outputMicro();

            log.info(
                "\n********************" +
                    "\n***********************************" +
                    "\n***************************************************");
        }
    }

    private void outputMicro() {
        // Assuming meterRegistry is already initialized and available
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
                    metricOutput.append(" | Total Time (seconds) = ").append(timer.totalTime(getBaseTimeUnit())); // Adjust base unit as needed
                    metricOutput.append(" | Max (seconds) = ").append(timer.max(getBaseTimeUnit())); // Adjust base unit as needed
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
                    // For other meter types or if you still want to try extracting a general value
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
        return TimeUnit.SECONDS; // Or whatever your registry's base time unit is
    }

    @Test
    void concurrencyWithVaryingThreadCounts() throws Exception {
        // Prepare test data once in a transaction
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        List<PersonEntity> persons = txTemplate.execute(status -> {
            personRelationshipRepository.deleteAll();
            personRelationshipRepository.flush();
            personRepository.deleteAll();
            personRepository.flush();

            List<PersonEntity> created = new ArrayList<>();
            // Create enough persons to cover max thread count (e.g. 10000)
            int maxPersons = 10000;
            for (int i = 0; i < maxPersons; i++) {
                PersonEntity p = PersonEntity.builder()
                    .name("Person" + i)
                    .externalId(new Random().nextLong())
                    .dateOfBirth(LocalDate.of(1990, 1, 1).plusDays(i))
                    .build();
                created.add(personRepository.save(p));
            }

            // Assign partners and create exactly 3 children per pair
            for (int i = 0; i < created.size() - 1; i += 2) {
                PersonEntity p1 = created.get(i);
                PersonEntity p2 = created.get(i + 1);

                p1.addRelationship(p2, RelationshipType.PARTNER, RelationshipType.PARTNER);
                p2.addRelationship(p1, RelationshipType.PARTNER, RelationshipType.PARTNER);
                personRepository.save(p1);
                personRepository.save(p2);

                for (int c = 1; c <= 3; c++) {
                    LocalDate dob = (c == 3) ? LocalDate.now().minusYears(10) : LocalDate.now().minusYears(20 + c);
                    PersonEntity child = PersonEntity.builder()
                        .name(p1.getName() + "Child" + c)
                        .externalId(new Random().nextLong())
                        .dateOfBirth(dob)
                        .build();
                    child = personRepository.save(child);

                    p1.addRelationship(child, RelationshipType.CHILD, RelationshipType.PARENT);
                    p2.addRelationship(child, RelationshipType.CHILD, RelationshipType.PARENT);

                    child.addRelationship(p1, RelationshipType.PARENT, RelationshipType.CHILD);
                    child.addRelationship(p2, RelationshipType.PARENT, RelationshipType.CHILD);

                    personRepository.save(child);
                }
                personRepository.save(p1);
                personRepository.save(p2);
            }
            personRepository.flush();
            return created;
        });

        // Test with varying thread counts
        int[] threadCounts = {1, 10, 100, 1000, 10000};

        for (int threadCount : threadCounts) {
            // Use only as many persons as threadCount (for the test)
            assert persons != null;
            List<PersonEntity> testPersons = persons.subList(0, threadCount);

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            List<Future<Long>> futures = new CopyOnWriteArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                PersonEntity person = testPersons.get(i);
                int finalI = i;
                futures.add(executor.submit(() -> {
                    long start = System.nanoTime();

                    SpecificPersonCheckRequest request = new SpecificPersonCheckRequest()
                        .requestId("RQ" + finalI)
                        .name(person.getName())
                        .id(person.getExternalId())
                        .dateOfBirth(person.getDateOfBirth());

                    mockMvc.perform(post("/v1/people/check-existing-person")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                        .andExpect(content().string("")) // empty body on success
                        .andExpect(status().isOk());

                    return System.nanoTime() - start;
                }));
            }

            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.MINUTES);

            long totalTime = 0;
            for (Future<Long> future : futures) {
                totalTime += future.get();
            }

            double avgNanos = (double) totalTime / threadCount;
            double avgMillis = avgNanos / 1_000_000.0;
            double avgSeconds = avgMillis / 1000.0;

            log.info(
                "\n***************************************************" +
                    "\n***********************************" +
                    "\n********************" +
                    "\nAverage response time: {} ms ({} s), Threads: {}, Total Time (ns): {}\n",
                String.format("%.3f", avgMillis),
                String.format("%.3f", avgSeconds),
                threadCount, totalTime);

            outputMicro();

            log.info(
                "\n********************" +
                    "\n***********************************" +
                    "\n***************************************************");
        }
    }

    private PersonEntity createChild(String name, LocalDate dob) {
        return PersonEntity.builder()
            .name(name)
            .externalId(new Random().nextLong())
            .dateOfBirth(dob)
            .build();
    }
}
