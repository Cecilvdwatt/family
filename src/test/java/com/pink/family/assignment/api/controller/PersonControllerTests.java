package com.pink.family.assignment.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pink.family.api.rest.client.ApiClient;
import com.pink.family.api.rest.client.reference.DefaultApi;
import com.pink.family.api.rest.client.model.ErrorResponse;
import com.pink.family.api.rest.client.model.FullPerson;
import com.pink.family.api.rest.client.model.PersonDetailsRequest;
import com.pink.family.api.rest.client.model.Relation;
import com.pink.family.api.rest.client.model.SpecificPersonCheckRequest;
import com.pink.family.assignment.constants.ErrorMessages;
import com.pink.family.assignment.database.dao.PersonDao;
import com.pink.family.assignment.database.entity.PersonEntity;
import com.pink.family.assignment.database.entity.enums.RelationshipType;
import com.pink.family.assignment.database.repository.PersonRelationshipRepository;
import com.pink.family.assignment.database.repository.PersonRepository;
import com.pink.family.assignment.dto.PersonDto;
import com.pink.family.assignment.service.PersonService;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cache.CacheManager;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.testcontainers.junit.jupiter.TestcontainersExtension;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.LongStream;


@Slf4j
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(TestcontainersExtension.class)
class PersonControllerTests {

    // Start container eagerly before any property resolution
    @Container
    public static MSSQLServerContainer<?> dbContainer
            = new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2019-latest")
        .acceptLicense()
        .withPassword("A_Str0ng_P@ssword123!"); // Required and must meet SQL Server's complexity rules



    static {
        dbContainer.start();
        // Wait for container to be ready (ping or simple connection test)
        try (Connection conn = DriverManager.getConnection(dbContainer.getJdbcUrl(), dbContainer.getUsername(), dbContainer.getPassword())) {
            log.debug("Successful connection to container {}", conn);
        } catch (SQLException e) {
            throw new RuntimeException("container not ready", e);
        }
    }

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", dbContainer::getJdbcUrl);
        registry.add("spring.datasource.username", dbContainer::getUsername);
        registry.add("spring.datasource.password", dbContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        //registry.add("spring.jpa.show-sql", () -> "true");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.SQLServerDialect");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private PersonDao personDao;

    @Autowired
    private ObjectMapper objectMapper;

    private DefaultApi api;
    @Autowired
    private CacheManager cacheManager;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    PersonRelationshipRepository personRelationshipRepository;

    @Autowired
    PersonRepository personRepository;

    @Autowired
    EntityManager entityManager;

    RestTemplate restTemplate;


    @BeforeEach
    void setUp() {

        log.debug("Port Variable: {}", port);
        log.debug("Container Host: {}", dbContainer.getHost());
        log.debug("Container Port: {}", dbContainer.getFirstMappedPort());

        log.debug("Port Variable: {}", port);

        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(Timeout.ofSeconds(10))
            .setResponseTimeout(Timeout.ofSeconds(10))
            .build();

        CloseableHttpClient httpClient = HttpClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(30000); // crank it up for debugging

        restTemplate = new RestTemplate(factory);

        ApiClient apiClient = new ApiClient(restTemplate);
        apiClient.setBasePath("http://localhost:" + port);
        api = new DefaultApi(apiClient);

        // Reset cache and DB
        cacheManager.getCacheNames()
            .forEach(name -> Objects.requireNonNull(cacheManager.getCache(name)).clear());

        personRelationshipRepository.deleteAll();
        personRepository.deleteAll();
    }

    @Nested
    class CheckExistingPerson {


        @Test
        @DisplayName("Should return 444 when multiple people match name and DOB")
        void shouldReturn444_WhenMultiplePeopleMatchNameAndDob() {
            // Setup: create multiple people with same name and DOB
            personDao.save(PersonEntity.builder()
                .name("Alex")
                .externalId(getId())
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build());

            personDao.save(PersonEntity.builder()
                .name("Alex")
                .externalId(getId())
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build());

            // Request using name + DOB that matches both above

            try {
                api.v1PeopleCheckExistingPersonPostWithHttpInfo(
                    new SpecificPersonCheckRequest()
                        .requestId("RQ_MULTIPLE")
                        .name("Alex")
                        .dateOfBirth(LocalDate.of(1990, 1, 1)));
                fail("Expected failure");
            } catch (RestClientResponseException e) {
                assertEquals(444, e.getStatusCode().value());
                ErrorResponse error = e.getResponseBodyAs(ErrorResponse.class);
                assert error != null;
                assertEquals("444", error.getCode());
                assertEquals(ErrorMessages.NO_DISTINCT_RECORD, error.getMessage());
                assertEquals("RQ_MULTIPLE", error.getRequestId());
            }
        }


        @Test
        @DisplayName("Should return 444 when only some children are shared with the partner")
        void shouldReturn444_WhenNotAllChildrenAreSharedWithPartner() {

            Long mainId = getId();
            PersonEntity main = personDao.save(PersonEntity.builder()
                .name("SharedDad")
                .externalId(mainId)
                .dateOfBirth(LocalDate.of(1985, 5, 5))
                .build());

            PersonEntity partner1 = personDao.save(PersonEntity.builder()
                .name("Mom1")
                .externalId(getId())
                .dateOfBirth(LocalDate.of(1985, 6, 6))
                .build());

            PersonEntity partner2 = personDao.save(PersonEntity.builder()
                .name("Mom2")
                .externalId(getId())
                .dateOfBirth(LocalDate.of(1986, 7, 7))
                .build());

            PersonEntity child1 = personDao.save(createChild("Kid1", LocalDate.of(2010, 1, 1)));
            PersonEntity child2 = personDao.save(createChild("Kid2", LocalDate.of(2011, 1, 1)));
            PersonEntity child3 = personDao.save(createChild("Kid3", LocalDate.of(2012, 1, 1)));

            // All children linked to main
            main.addRelationship(child1, RelationshipType.PARENT, RelationshipType.CHILD);
            main.addRelationship(child2, RelationshipType.PARENT, RelationshipType.CHILD);
            main.addRelationship(child3, RelationshipType.PARENT, RelationshipType.CHILD);

            main.addRelationship(partner1, RelationshipType.PARTNER, RelationshipType.PARTNER);
            main.addRelationship(partner2, RelationshipType.PARTNER, RelationshipType.PARTNER);

            // Split partners
            partner1.addRelationship(child1, RelationshipType.PARENT, RelationshipType.CHILD);
            partner2.addRelationship(child2, RelationshipType.PARENT, RelationshipType.CHILD);
            partner2.addRelationship(child3, RelationshipType.PARENT, RelationshipType.CHILD);

            // Save everything
            personDao.saveAndFlush(main);
            personDao.saveAndFlush(partner1);
            personDao.saveAndFlush(partner2);

            SpecificPersonCheckRequest request = new SpecificPersonCheckRequest()
                .requestId("RQ_NOT_SHARED")
                .name("SharedDad")
                .id(mainId)
                .dateOfBirth(LocalDate.of(1985, 5, 5));

            try {
                api.v1PeopleCheckExistingPersonPostWithHttpInfo(request);
                fail("Expected failure");
            } catch (RestClientResponseException e) {
                assertEquals(444, e.getStatusCode().value());
                ErrorResponse error = e.getResponseBodyAs(ErrorResponse.class);
                assert error != null;
                assertEquals("444", error.getCode());
                assertEquals(ErrorMessages.NO_SHARED_CHILDREN, error.getMessage());
                assertEquals("RQ_NOT_SHARED", error.getRequestId());
            }
        }


        @Test
        @DisplayName("Should return 200 when external ID is missing but name and DOB match an existing person")
        void shouldReturn200_WhenIdMissingButNameAndDobMatch() {
            // Save existing person
            PersonEntity existing = personDao.save(
                PersonEntity.builder()
                    .name("NoIDPerson")
                    .externalId(getId())
                    .dateOfBirth(LocalDate.of(1990, 6, 15))
                    .build()
            );

            // Add partner and exactly 3 children
            PersonEntity partner = personDao.save(
                PersonEntity.builder()
                    .name("Partner")
                    .externalId(getId())
                    .dateOfBirth(LocalDate.of(1991, 7, 16))
                    .build()
            );

            PersonEntity child1 = personDao.save(createChild("Child1", LocalDate.of(2010, 1, 1)));
            PersonEntity child2 = personDao.save(createChild("Child2", LocalDate.of(2011, 2, 2)));
            PersonEntity child3 = personDao.save(createChild("AdultChild3", LocalDate.now().minusYears(10)));

            existing.addRelationship(partner, RelationshipType.PARTNER, RelationshipType.PARTNER);
            existing.addRelationship(child1, RelationshipType.PARENT, RelationshipType.CHILD);
            existing.addRelationship(child2, RelationshipType.PARENT, RelationshipType.CHILD);
            existing.addRelationship(child3, RelationshipType.PARENT, RelationshipType.CHILD);

            partner.addRelationship(child1, RelationshipType.PARENT, RelationshipType.CHILD);
            partner.addRelationship(child2, RelationshipType.PARENT, RelationshipType.CHILD);
            partner.addRelationship(child3, RelationshipType.PARENT, RelationshipType.CHILD);

            personDao.saveAndFlush(existing);
            personDao.saveAndFlush(partner);

            // Request missing the ID, but with correct name and dob
            SpecificPersonCheckRequest request = new SpecificPersonCheckRequest()
                .requestId("RQ_MISSING_ID")
                .name("NoIDPerson")
                .dateOfBirth(LocalDate.of(1990, 6, 15));

            // Using the Swagger-generated client
            var response = api.v1PeopleCheckExistingPersonPostWithHttpInfo(request);
            assertEquals(200, response.getStatusCode().value());
        }


        @Test
        @DisplayName("Should return 200 when a person has a partner and exactly 3 children with the same partner")
        void shouldReturn200_WhenPartnerAnd3ChildrenExist() {

            Long mainId =getId();
            // Setup (same as before)
            PersonEntity main = PersonEntity.builder()
                .name("John")
                .externalId(mainId)
                .dateOfBirth(LocalDate.of(1990, 5, 20))
                .build();

           Long partnerId = getId();
            PersonEntity partner = PersonEntity.builder()
                .name("Jane")
                .externalId(partnerId)
                .dateOfBirth(LocalDate.of(1988, 3, 15))
                .build();

            PersonEntity child1 = createChild("Child1", LocalDate.of(2010, 1, 1));
            PersonEntity child2 = createChild("Child2", LocalDate.of(2012, 2, 2));
            PersonEntity child3 = createChild("Child3", LocalDate.of(LocalDate.now().getYear() - 10, 3, 3));

            main = personDao.save(main);
            partner = personDao.save(partner);
            child1 = personDao.save(child1);
            child2 = personDao.save(child2);
            child3 = personDao.save(child3);

            main.addRelationship(partner, RelationshipType.PARTNER, RelationshipType.PARTNER);
            main.addRelationship(child1, RelationshipType.PARENT, RelationshipType.CHILD);
            main.addRelationship(child2, RelationshipType.PARENT, RelationshipType.CHILD);
            main.addRelationship(child3, RelationshipType.PARENT, RelationshipType.CHILD);

            partner.addRelationship(child1, RelationshipType.PARENT, RelationshipType.CHILD);
            partner.addRelationship(child2, RelationshipType.PARENT, RelationshipType.CHILD);
            partner.addRelationship(child3, RelationshipType.PARENT, RelationshipType.CHILD);

            personDao.saveAndFlush(main);
            personDao.saveAndFlush(partner);
            personDao.saveAndFlush(child1);
            personDao.saveAndFlush(child2);
            personDao.saveAndFlush(child3);

            SpecificPersonCheckRequest request = new SpecificPersonCheckRequest()
                .requestId("RQ123")
                .name("Jane")
                .id(mainId)
                .dateOfBirth(LocalDate.of(1990, 5, 20));

            var response = api.v1PeopleCheckExistingPersonPostWithHttpInfo(request);
            assertEquals(200, response.getStatusCode().value());
        }


        @Test
        @DisplayName("Should return 444 when person has more than 3 children with partner")
        void shouldReturn444_WhenMoreThan3ChildrenExist() throws Exception {

            Long mainId = getId();
            PersonEntity main = PersonEntity.builder()
                .name("Anna")
                .externalId(mainId)
                .dateOfBirth(LocalDate.of(1985, 7, 15))
                .build();

            Long partnerId = getId();
            PersonEntity partner = PersonEntity.builder()
                .name("Mark")
                .externalId(partnerId)
                .dateOfBirth(LocalDate.of(1984, 6, 10))
                .build();
            main = personDao.save(main);
            partner = personDao.save(partner);

            PersonEntity child1 = personDao.save(createChild("Child1", LocalDate.of(2005, 1, 1)));
            PersonEntity child2 = personDao.save(createChild("Child2", LocalDate.of(2007, 2, 2)));
            PersonEntity child3 = personDao.save(createChild("Child3", LocalDate.of(2009, 3, 3)));
            PersonEntity child4 = personDao.save(createChild("Child4", LocalDate.of(2011, 4, 4)));

            main.addRelationship(partner, RelationshipType.PARTNER, RelationshipType.PARTNER);
            main.addRelationship(child1, RelationshipType.PARENT, RelationshipType.CHILD);
            main.addRelationship(child2, RelationshipType.PARENT, RelationshipType.CHILD);
            main.addRelationship(child3, RelationshipType.PARENT, RelationshipType.CHILD);
            main.addRelationship(child4, RelationshipType.PARENT, RelationshipType.CHILD);

            partner.addRelationship(child1, RelationshipType.PARENT, RelationshipType.CHILD);
            partner.addRelationship(child2, RelationshipType.PARENT, RelationshipType.CHILD);
            partner.addRelationship(child3, RelationshipType.PARENT, RelationshipType.CHILD);
            partner.addRelationship(child4, RelationshipType.PARENT, RelationshipType.CHILD);

            personDao.saveAndFlush(main);
            personDao.saveAndFlush(partner);

            SpecificPersonCheckRequest request = new SpecificPersonCheckRequest()
                .requestId("RQ999")
                .name("Anna")
                .id(mainId)
                .dateOfBirth(LocalDate.of(1985, 7, 15));

            try {
                api.v1PeopleCheckExistingPersonPostWithHttpInfo(request);
                fail("Expected failure");
            } catch (RestClientResponseException e) {
                assertEquals(444, e.getStatusCode().value());
                ErrorResponse error = objectMapper.readValue(e.getResponseBodyAsString(), ErrorResponse.class);
                assertEquals("444", error.getCode());
                assertEquals(ErrorMessages.NOT_EXACTLY_3_CHILDREN, error.getMessage());
                assertEquals("RQ999", error.getRequestId());
            }
        }


        @Test
        @DisplayName("Should return 444 when children have different partners")
        void shouldReturn444_WhenChildrenHaveDifferentPartners() throws Exception {

            Long mainId = getId();
            PersonEntity main = PersonEntity.builder()
                .name("Emma")
                .externalId(mainId)
                .dateOfBirth(LocalDate.of(1987, 8, 20))
                .build();

            Long partnerId = getId();
            PersonEntity partner1 = PersonEntity.builder()
                .name("Liam")
                .externalId(partnerId)
                .dateOfBirth(LocalDate.of(1986, 7, 10))
                .build();

            Long partnerId2 = getId();
            PersonEntity partner2 = PersonEntity.builder()
                .name("Noah")
                .externalId(partnerId2)
                .dateOfBirth(LocalDate.of(1985, 5, 5))
                .build();

            main = personDao.save(main);
            partner1 = personDao.save(partner1);
            partner2 = personDao.save(partner2);

            PersonEntity child1 = personDao.save(createChild("Child1", LocalDate.of(2008, 3, 3)));
            PersonEntity child2 = personDao.save(createChild("Child2", LocalDate.of(2010, 4, 4)));
            PersonEntity child3 = personDao.save(createChild("Child3", LocalDate.of(2012, 5, 5)));

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
                .requestId("RQ888")
                .name("Emma")
                .id(mainId)
                .dateOfBirth(LocalDate.of(1987, 8, 20));

            try {
                api.v1PeopleCheckExistingPersonPostWithHttpInfo(request);
                fail("Expected failure");
            } catch (RestClientResponseException e) {
                assertEquals(444, e.getStatusCode().value());
                ErrorResponse error = objectMapper.readValue(e.getResponseBodyAsString(), ErrorResponse.class);
                assertEquals("444", error.getCode());
                assertEquals(ErrorMessages.NO_SHARED_CHILDREN, error.getMessage());
                assertEquals("RQ888", error.getRequestId());
            }
        }


        @Test
        @DisplayName("Should return 444 when none of the children are under 18")
        void shouldReturn444_WhenChildrenAreAllAdults() throws Exception {
            Long mainId = getId();
            PersonEntity main = PersonEntity.builder()
                .name("Oliver")
                .externalId(mainId)
                .dateOfBirth(LocalDate.of(1980, 9, 30))
                .build();
            PersonEntity partner = PersonEntity.builder()
                .name("Sophia")
                .externalId(getId())
                .dateOfBirth(LocalDate.of(1982, 10, 25))
                .build();

            main = personDao.save(main);
            partner = personDao.save(partner);

            PersonEntity child1 = personDao.save(createChild("AdultChild1", LocalDate.of(1995, 1, 1)));
            PersonEntity child2 = personDao.save(createChild("AdultChild2", LocalDate.of(1994, 2, 2)));
            PersonEntity child3 = personDao.save(createChild("AdultChild3", LocalDate.of(1996, 2, 2)));

            main.addRelationship(partner, RelationshipType.PARTNER, RelationshipType.PARTNER);
            main.addRelationship(child1, RelationshipType.PARENT, RelationshipType.CHILD);
            main.addRelationship(child2, RelationshipType.PARENT, RelationshipType.CHILD);
            main.addRelationship(child3, RelationshipType.PARENT, RelationshipType.CHILD);

            partner.addRelationship(child1, RelationshipType.PARENT, RelationshipType.CHILD);
            partner.addRelationship(child2, RelationshipType.PARENT, RelationshipType.CHILD);
            partner.addRelationship(child3, RelationshipType.PARENT, RelationshipType.CHILD);

            personDao.saveAndFlush(main);
            personDao.saveAndFlush(partner);

            SpecificPersonCheckRequest request = new SpecificPersonCheckRequest()
                .requestId("RQ777")
                .name("Oliver")
                .id(mainId)
                .dateOfBirth(LocalDate.of(1980, 9, 30));

            try {
                api.v1PeopleCheckExistingPersonPostWithHttpInfo(request);
                fail("Expected failure");
            } catch (RestClientResponseException e) {
                assertEquals(444, e.getStatusCode().value());
                ErrorResponse error = objectMapper.readValue(e.getResponseBodyAsString(), ErrorResponse.class);
                assertEquals("444", error.getCode());
                assertEquals(ErrorMessages.NO_UNDERAGE_CHILD, error.getMessage());
                assertEquals("RQ777", error.getRequestId());
            }
        }

        @Test
        @DisplayName("Should return 200 when no external id but name, surname, and DOB match")
        void shouldReturn200_WhenNoExternalIdButNameSurnameDobMatch() {
            Long mainId = getId();
            PersonEntity main = PersonEntity.builder()
                .name("Lucas")
                .externalId(mainId)
                .dateOfBirth(LocalDate.of(1992, 11, 11))
                .build();

            Long partnerId = getId();
            PersonEntity partner = PersonEntity.builder()
                .name("Mia")
                .externalId(partnerId)
                .dateOfBirth(LocalDate.of(1990, 12, 12))
                .build();

            PersonEntity child1 = personDao.save(createChild("Child1", LocalDate.of(2010, 1, 1)));
            PersonEntity child2 = personDao.save(createChild("Child2", LocalDate.of(2012, 2, 2)));
            PersonEntity child3 = personDao.save(createChild("Child3", LocalDate.of(2014, 3, 3)));

            main = personDao.save(main);
            partner = personDao.save(partner);

            main.addRelationship(partner, RelationshipType.PARTNER, RelationshipType.PARTNER);
            main.addRelationship(child1, RelationshipType.PARENT, RelationshipType.CHILD);
            main.addRelationship(child2, RelationshipType.PARENT, RelationshipType.CHILD);
            main.addRelationship(child3, RelationshipType.PARENT, RelationshipType.CHILD);

            partner.addRelationship(child1, RelationshipType.PARENT, RelationshipType.CHILD);
            partner.addRelationship(child2, RelationshipType.PARENT, RelationshipType.CHILD);
            partner.addRelationship(child3, RelationshipType.PARENT, RelationshipType.CHILD);

            personDao.saveAndFlush(main);
            personDao.saveAndFlush(partner);

            SpecificPersonCheckRequest request = new SpecificPersonCheckRequest()
                .requestId("RQ666")
                .name("Lucas")
                .dateOfBirth(LocalDate.of(1992, 11, 11));

            var response = api.v1PeopleCheckExistingPersonPostWithHttpInfo(request);
            assertEquals(200, response.getStatusCode().value());
        }


        @Test
        @DisplayName("Should return 444 when children are not linked to main person but only to partner")
        void shouldReturn444_WhenChildrenOnlyLinkedToPartner() throws Exception {
            Long mainId = getId();
            PersonEntity main = personDao.save(PersonEntity.builder()
                .name("IsolatedParent")
                .externalId(mainId)
                .dateOfBirth(LocalDate.of(1980, 1, 1))
                .build());

            Long partnerId = getId();
            PersonEntity partner = personDao.save(PersonEntity.builder()
                .name("EngagedParent")
                .externalId(partnerId)
                .dateOfBirth(LocalDate.of(1980, 2, 2))
                .build());

            PersonEntity child1 = personDao.save(createChild("Child1", LocalDate.of(2010, 1, 1)));
            PersonEntity child2 = personDao.save(createChild("Child2", LocalDate.of(2011, 1, 1)));
            PersonEntity child3 = personDao.save(createChild("Child3", LocalDate.of(2012, 1, 1)));

            PersonEntity child4 = personDao.save(createChild("Child4", LocalDate.of(2010, 1, 1)));
            PersonEntity child5 = personDao.save(createChild("Child5", LocalDate.of(2011, 1, 1)));
            PersonEntity child6 = personDao.save(createChild("Child6", LocalDate.of(2012, 1, 1)));

            main.addRelationship(partner, RelationshipType.PARTNER, RelationshipType.PARTNER);
            main.addRelationship(child4, RelationshipType.PARENT, RelationshipType.CHILD);
            main.addRelationship(child5, RelationshipType.PARENT, RelationshipType.CHILD);
            main.addRelationship(child6, RelationshipType.PARENT, RelationshipType.CHILD);

            partner.addRelationship(child1, RelationshipType.PARENT, RelationshipType.CHILD);
            partner.addRelationship(child2, RelationshipType.PARENT, RelationshipType.CHILD);
            partner.addRelationship(child3, RelationshipType.PARENT, RelationshipType.CHILD);

            personDao.saveAndFlush(main);
            personDao.saveAndFlush(partner);

            SpecificPersonCheckRequest request = new SpecificPersonCheckRequest()
                .requestId("RQ_CHILDREN_NOT_MINE")
                .name("IsolatedParent")
                .id(mainId)
                .dateOfBirth(LocalDate.of(1980, 1, 1));

            try {
                api.v1PeopleCheckExistingPersonPostWithHttpInfo(request);
                fail("Expected failure");
            } catch (RestClientResponseException e) {
                assertEquals(444, e.getStatusCode().value());
                ErrorResponse error = objectMapper.readValue(e.getResponseBodyAsString(), ErrorResponse.class);
                assertEquals("444", error.getCode());
                assertEquals(ErrorMessages.NO_SHARED_CHILDREN, error.getMessage());
                assertEquals("RQ_CHILDREN_NOT_MINE", error.getRequestId());
            }
        }


        @Test
        @DisplayName("Should return 444 when no matching person is found")
        void shouldReturn444_WhenNoMatchFound() throws Exception {

            Long mainId = getId();
            SpecificPersonCheckRequest request = new SpecificPersonCheckRequest()
                .requestId("RQ_NO_MATCH")
                .id(mainId)
                .name("Ghost")
                .dateOfBirth(LocalDate.of(1900, 1, 1));

            try {
                api.v1PeopleCheckExistingPersonPostWithHttpInfo(request);
                fail("Expected failure");
            } catch (RestClientResponseException e) {
                assertEquals(444, e.getStatusCode().value());
                ErrorResponse error = objectMapper.readValue(e.getResponseBodyAsString(), ErrorResponse.class);
                assertEquals("444", error.getCode());
                assertEquals(ErrorMessages.NO_RECORD, error.getMessage());
                assertEquals("RQ_NO_MATCH", error.getRequestId());
            }
        }
    }

    @Nested
    class PersonPost {

        @Test
        @DisplayName("Should successfully update person with parent, partner, child and return full relationship details")
        void testV1PeoplePost_WithValidData_AllExist_AndFullDetailsReturned() {

            Long mainId = getId();
            Long parent1Id = getId();
            Long parent2Id = getId();
            Long partnerId = getId();
            Long childId = getId();

            personDao.save(PersonEntity.builder()
                .externalId(parent1Id).name("Parent One").dateOfBirth(LocalDate.of(1960, 1, 1)).build());
            personDao.save(PersonEntity.builder()
                .externalId(parent2Id).name("Parent Two").dateOfBirth(LocalDate.of(1965, 2, 2)).build());
            personDao.save(PersonEntity.builder()
                .externalId(partnerId).name("Partner Person").dateOfBirth(LocalDate.of(1991, 3, 3)).build());
            personDao.save(PersonEntity.builder()
                .externalId(childId).name("Child Person").dateOfBirth(LocalDate.of(2010, 4, 4)).build());
            personDao.save(PersonEntity.builder()
                .externalId(mainId).name("Main").build());

            var request = new PersonDetailsRequest()
                .id(mainId)
                .name("Main Updated")
                .birthDate(LocalDate.of(1990, 5, 20))
                .parent1(new Relation().id(parent1Id))
                .parent2(new Relation().id(parent2Id))
                .partner(new Relation().id(partnerId))
                .addChildrenItem(new Relation().id(childId));

            FullPerson person;
            try {
                person = api.v1PeoplePost(request);
            } catch (RestClientResponseException e) {
                fail("API call failed with status " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
                return; // defensive, though fail() will already abort
            }

            assertThat(person.getId()).isEqualTo(mainId);
            assertThat(person.getName()).isEqualTo("Main Updated");
            assertThat(person.getBirthDate()).isEqualTo(LocalDate.of(1990, 5, 20));

            assert person.getParent1() != null;
            assertThat(person.getParent1().getName()).isEqualTo("Parent One");
            assert person.getParent2() != null;
            assertThat(person.getParent2().getName()).isEqualTo("Parent Two");
            assert person.getPartner() != null;
            assertThat(person.getPartner().getName()).isEqualTo("Partner Person");
            assert person.getChildren() != null;
            assertThat(person.getChildren().getFirst().getName()).isEqualTo("Child Person");

            // Verify update persisted to DB
            var saved = personDao.findByExternalId(mainId).stream().findFirst();
            assertThat(saved).isPresent();
            assertThat(saved.orElseThrow().getName()).isEqualTo("Main Updated");
            assertThat(saved.orElseThrow().getDateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 20));
        }


        @Test
        @DisplayName("Should update person with partial relationships: one parent, no partner, multiple children")
        void testV1PeoplePost_PartialRelationships() {

            Long mainId = getId();
            Long parent1Id = getId();
            Long child1Id = getId();
            Long child2Id = getId();

            // Save related entities in DB
            personDao.save(PersonEntity.builder()
                .externalId(parent1Id).name("Single Parent").dateOfBirth(LocalDate.of(1955, 6, 15)).build());
            personDao.save(PersonEntity.builder()
                .externalId(child1Id).name("Child One").dateOfBirth(LocalDate.of(2005, 8, 8)).build());
            personDao.save(PersonEntity.builder()
                .externalId(child2Id).name("Child Two").dateOfBirth(LocalDate.of(2010, 9, 9)).build());
            personDao.save(PersonEntity.builder()
                .externalId(mainId).name("Original Name").dateOfBirth(LocalDate.of(1985, 3, 3)).build());

            var request = new PersonDetailsRequest()
                .id(mainId)
                .name("Updated Name")
                .birthDate(LocalDate.of(1990, 1, 1))
                .parent1(new Relation().id(parent1Id))
                .addChildrenItem(new Relation().id(child1Id))
                .addChildrenItem(new Relation().id(child2Id));

            FullPerson person;
            try {
                person = api.v1PeoplePost(request);
            } catch (RestClientResponseException e) {
                fail("API call failed with status " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
                return;
            }

            // Validate response fields
            assertThat(person.getId()).isEqualTo(mainId);
            assertThat(person.getName()).isEqualTo("Updated Name");
            assertThat(person.getBirthDate()).isEqualTo(LocalDate.of(1990, 1, 1));

            assertThat(person.getParent1()).isNotNull();
            assert person.getParent1() != null;
            assertThat(person.getParent1().getId()).isEqualTo(parent1Id);
            assertThat(person.getParent1().getName()).isEqualTo("Single Parent");
            assertThat(person.getParent1().getBirthDate()).isEqualTo(LocalDate.of(1955, 6, 15));

            assertThat(person.getParent2()).isNull();
            assertThat(person.getPartner()).isNull();

            assertThat(person.getChildren()).hasSize(2);
            assertThat(person.getChildren())
                .anyMatch(child -> {
                    assert child.getId() != null;
                    if (!child.getId().equals(child1Id)) {
                        return false;
                    }
                    assert child.getName() != null;
                    return child.getName().equals("Child One");
                })
                .anyMatch(child -> {
                    assert child.getId() != null;
                    if (!child.getId().equals(child2Id)) {
                        return false;
                    }
                    assert child.getName() != null;
                    return child.getName().equals("Child Two");
                });

            // Check DB updated accordingly
            var saved = personDao.findByExternalId(mainId).stream().findFirst();
            assertThat(saved).isPresent();
            assertThat(saved.orElseThrow().getName()).isEqualTo("Updated Name");
            assertThat(saved.orElseThrow().getDateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 1));
        }


        @Test
        @DisplayName("Should keep existing relationships if no relations are sent in update")
        void testUpdatePerson_NoRelationsSent_ExistingRelationsNotCleared() {
            Long mainId = getId();
            Long parentId = getId();
            Long partnerId = getId();
            Long childId = getId();

            PersonEntity mainPerson = personDao.saveAndFlush(PersonEntity.builder()
                .externalId(mainId)
                .name("Main Person")
                .build());

            PersonEntity parent = personDao.saveAndFlush(PersonEntity.builder().externalId(parentId).name("Existing Parent").build());
            PersonEntity partner = personDao.saveAndFlush(PersonEntity.builder().externalId(partnerId).name("Existing Partner").build());
            PersonEntity child = personDao.saveAndFlush(PersonEntity.builder().externalId(childId).name("Existing Child").build());

            mainPerson.addRelationship(parent, RelationshipType.CHILD, RelationshipType.PARENT);
            mainPerson.addRelationship(partner, RelationshipType.PARTNER, RelationshipType.PARTNER);
            mainPerson.addRelationship(child, RelationshipType.PARENT, RelationshipType.CHILD);
            personDao.save(mainPerson);

            var request = new PersonDetailsRequest()
                .id(mainId)
                .name("Main Updated")
                .birthDate(LocalDate.of(1990, 1, 1))
                .parent1(null)
                .parent2(null)
                .partner(null)
                .children(null);

            FullPerson response = api.v1PeoplePostWithHttpInfo(request).getBody();

            assert response != null;
            assertThat(response.getId()).isEqualTo(mainId);
            assertThat(response.getName()).isEqualTo("Main Updated");
            assertThat(response.getBirthDate()).isEqualTo(LocalDate.of(1990, 1, 1));

            var updated = personDao.findByExternalIdDto(mainId);
            assertThat(updated).isPresent();
            PersonDto updatedPerson = updated.orElseThrow();

            assertThat(updatedPerson.countAllRelations()).isEqualTo(3);

            assertThat(updatedPerson.hasRelationExId(parentId)).isTrue();
            assertThat(updatedPerson.hasRelationExId(RelationshipType.CHILD, parentId)).isTrue();

            assertThat(updatedPerson.hasRelationExId(childId)).isTrue();
            assertThat(updatedPerson.hasRelationExId(RelationshipType.PARENT, childId)).isTrue();

            assertThat(updatedPerson.hasRelationExId(partnerId)).isTrue();
            assertThat(updatedPerson.hasRelationExId(RelationshipType.PARTNER, partnerId)).isTrue();
        }

        @Test
        @DisplayName("Should add new relationships and keep existing relationships not mentioned in update")
        void testUpdatePerson_PartialRelationsSent_ExistingRelationsRetained() {
            Long mainId = getId();
            Long existingParentId = getId();
            Long existingPartnerId = getId();
            Long existingChildId = getId();
            Long newPartnerId = getId();

            personDao.save(PersonEntity.builder().externalId(existingParentId).name("Existing Parent").build());
            personDao.save(PersonEntity.builder().externalId(existingPartnerId).name("Existing Partner").build());
            personDao.save(PersonEntity.builder().externalId(existingChildId).name("Existing Child").build());
            personDao.save(PersonEntity.builder().externalId(newPartnerId).name("New Partner").build());

            PersonEntity mainPerson = personDao.save(PersonEntity.builder()
                .externalId(mainId)
                .name("Main Person")
                .build());

            PersonEntity parent = personDao.findByExternalIdEntity(existingParentId).orElseThrow();
            PersonEntity partner = personDao.findByExternalIdEntity(existingPartnerId).orElseThrow();
            PersonEntity child = personDao.findByExternalIdEntity(existingChildId).orElseThrow();

            mainPerson.addRelationship(parent, RelationshipType.CHILD, RelationshipType.PARENT);
            mainPerson.addRelationship(partner, RelationshipType.PARTNER, RelationshipType.PARTNER);
            mainPerson.addRelationship(child, RelationshipType.PARENT, RelationshipType.CHILD);
            personDao.save(mainPerson);

            var request = new PersonDetailsRequest()
                .id(mainId)
                .name("Main Updated")
                .birthDate(LocalDate.of(1990, 1, 1))
                .partner(new Relation().id(newPartnerId));

            FullPerson response = api.v1PeoplePostWithHttpInfo(request).getBody();

            assert response != null;
            assertThat(response.getId()).isEqualTo(mainId);
            assertThat(response.getName()).isEqualTo("Main Updated");


            var updated = personDao.findByExternalIdDto(mainId);
            assertThat(updated).isPresent();
            PersonDto updatedPerson = updated.orElseThrow();

            assertThat(updatedPerson.countAllRelations()).isEqualTo(4);

            assertThat(updatedPerson.hasRelationExId(existingParentId)).isTrue();
            assertThat(updatedPerson.hasRelationExId(RelationshipType.CHILD, existingParentId)).isTrue();

            assertThat(updatedPerson.hasRelationExId(existingChildId)).isTrue();
            assertThat(updatedPerson.hasRelationExId(RelationshipType.PARENT, existingChildId)).isTrue();

            assertThat(updatedPerson.hasRelationExId(existingPartnerId)).isTrue();
            assertThat(updatedPerson.hasRelationExId(RelationshipType.PARTNER, existingPartnerId)).isTrue();

            assertThat(updatedPerson.hasRelationExId(newPartnerId)).isTrue();
            assertThat(updatedPerson.hasRelationExId(RelationshipType.PARTNER, newPartnerId)).isTrue();
        }

        @Test
        @DisplayName("Should update person details without modifying existing relationships when same relations are sent")
        void testUpdatePerson_DetailsUpdated_NoRelationChange() {
            Long mainId = getId();
            Long parentId = getId();
            Long partnerId = getId();
            Long childId = getId();

            personDao.save(PersonEntity.builder().externalId(parentId).name("Parent").build());
            personDao.save(PersonEntity.builder().externalId(partnerId).name("Partner").build());
            personDao.save(PersonEntity.builder().externalId(childId).name("Child").build());

            PersonEntity mainPerson = personDao.save(PersonEntity.builder()
                .externalId(mainId)
                .name("Original Name")
                .dateOfBirth(LocalDate.of(1980, 1, 1))
                .build());

            PersonEntity parent = personDao.findByExternalIdEntity(parentId).orElseThrow();
            PersonEntity partner = personDao.findByExternalIdEntity(partnerId).orElseThrow();
            PersonEntity child = personDao.findByExternalIdEntity(childId).orElseThrow();

            mainPerson.addRelationship(parent, RelationshipType.CHILD, RelationshipType.PARENT);
            mainPerson.addRelationship(partner, RelationshipType.PARTNER, RelationshipType.PARTNER);
            mainPerson.addRelationship(child, RelationshipType.PARENT, RelationshipType.CHILD);
            personDao.save(mainPerson);

            var request = new PersonDetailsRequest()
                .id(mainId)
                .name("Updated Name")
                .birthDate(LocalDate.of(1990, 2, 2))
                .parent1(new Relation().id(parentId))
                .partner(new Relation().id(partnerId))
                .addChildrenItem(new Relation().id(childId));

            FullPerson response = api.v1PeoplePostWithHttpInfo(request).getBody();

            assert response != null;
            assertThat(response.getId()).isEqualTo(mainId);
            assertThat(response.getName()).isEqualTo("Updated Name");

            var updated = personDao.findByExternalIdDto(mainId);
            assertThat(updated).isPresent();
            PersonDto updatedPerson = updated.orElseThrow();

            assertThat(updatedPerson.countAllRelations()).isEqualTo(3);

            assertThat(updatedPerson.hasRelationExId(parentId)).isTrue();
            assertThat(updatedPerson.hasRelationExId(RelationshipType.CHILD, parentId)).isTrue();

            assertThat(updatedPerson.hasRelationExId(childId)).isTrue();
            assertThat(updatedPerson.hasRelationExId(RelationshipType.PARENT, childId)).isTrue();

            assertThat(updatedPerson.hasRelationExId(partnerId)).isTrue();
            assertThat(updatedPerson.hasRelationExId(RelationshipType.PARTNER, partnerId)).isTrue();
        }

        @Test
        @DisplayName("Should add new relationships without removing existing ones")
        void testAddNewRelationships_PreservesExisting() {
            Long mainId = getId();
            Long existingParentId = getId();
            Long existingPartnerId = getId();
            Long newChildId = getId();

            personDao.save(PersonEntity.builder().externalId(existingParentId).name("Existing Parent").build());
            personDao.save(PersonEntity.builder().externalId(existingPartnerId).name("Existing Partner").build());
            personDao.save(PersonEntity.builder().externalId(newChildId).name("New Child").build());

            PersonEntity mainPerson = personDao.save(PersonEntity.builder()
                .externalId(mainId)
                .name("Original Name")
                .dateOfBirth(LocalDate.of(1980, 1, 1))
                .build());

            PersonEntity parent = personDao.findByExternalIdEntity(existingParentId).orElseThrow();
            PersonEntity partner = personDao.findByExternalIdEntity(existingPartnerId).orElseThrow();

            mainPerson.addRelationship(parent, RelationshipType.CHILD, RelationshipType.PARENT);
            mainPerson.addRelationship(partner, RelationshipType.PARTNER, RelationshipType.PARTNER);
            personDao.save(mainPerson);

            var request = new PersonDetailsRequest()
                .id(mainId)
                .name("Updated Name")
                .birthDate(LocalDate.of(1990, 2, 2))
                .parent1(new Relation().id(existingParentId))
                .partner(new Relation().id(existingPartnerId))
                .addChildrenItem(new Relation().id(newChildId));

            FullPerson response = api.v1PeoplePostWithHttpInfo(request).getBody();

            assert response != null;
            assertThat(response.getId()).isEqualTo(mainId);
            assertThat(response.getName()).isEqualTo("Updated Name");

            var updated = personDao.findAllDto().stream().filter(e -> Objects.equals(e.getExternalId(), mainId))
                .findFirst();

            PersonDto updatedPerson = updated.orElseThrow();

            assertThat(updatedPerson.countAllRelations()).isEqualTo(3);

            assertThat(updatedPerson.hasRelationExId(existingParentId)).isTrue();
            assertThat(updatedPerson.hasRelationExId(RelationshipType.CHILD, existingParentId)).isTrue();

            assertThat(updatedPerson.hasRelationExId(newChildId)).isTrue();
            assertThat(updatedPerson.hasRelationExId(RelationshipType.PARENT, newChildId)).isTrue();

            assertThat(updatedPerson.hasRelationExId(existingPartnerId)).isTrue();
            assertThat(updatedPerson.hasRelationExId(RelationshipType.PARTNER, existingPartnerId)).isTrue();
        }

        @Test
        @DisplayName("Should preserve existing relationships when null or empty relation sets are passed")
        void testUpdateWithNullOrEmptyRelations_PreservesExistingRelationships() {
            Long mainId = getId();
            Long parentId = getId();
            Long partnerId = getId();
            Long childId = getId();

            personDao.save(PersonEntity.builder().externalId(parentId).name("Parent").build());
            personDao.save(PersonEntity.builder().externalId(partnerId).name("Partner").build());
            personDao.save(PersonEntity.builder().externalId(childId).name("Child").build());

            PersonEntity mainPerson = personDao.save(PersonEntity.builder()
                .externalId(mainId)
                .name("Original Name")
                .dateOfBirth(LocalDate.of(1980, 1, 1))
                .build());

            PersonEntity parent = personDao.findByExternalIdEntity(parentId).orElseThrow();
            PersonEntity partner = personDao.findByExternalIdEntity(partnerId).orElseThrow();
            PersonEntity child = personDao.findByExternalIdEntity(childId).orElseThrow();

            mainPerson.addRelationship(parent, RelationshipType.CHILD, RelationshipType.PARENT);
            mainPerson.addRelationship(partner, RelationshipType.PARTNER, RelationshipType.PARTNER);
            mainPerson.addRelationship(child, RelationshipType.PARENT, RelationshipType.CHILD);
            personDao.save(mainPerson);

            var request = new PersonDetailsRequest()
                .id(mainId)
                .name("Updated Name")
                .birthDate(LocalDate.of(1990, 2, 2))
                .parent1(null)
                .parent2(null)
                .partner(null)
                .children(Collections.emptyList());

            FullPerson response = api.v1PeoplePostWithHttpInfo(request).getBody();

            assert response != null;
            assertThat(response.getId()).isEqualTo(mainId);
            assertThat(response.getName()).isEqualTo("Updated Name");

            var updated = personDao.findByExternalIdDto(mainId);
            assertThat(updated).isPresent();
            PersonDto updatedPerson = updated.orElseThrow();

            assertThat(updatedPerson.countAllRelations()).isEqualTo(3);

            assertThat(updatedPerson.hasRelationExId(parentId)).isTrue();
            assertThat(updatedPerson.hasRelationExId(RelationshipType.CHILD, parentId)).isTrue();

            assertThat(updatedPerson.hasRelationExId(childId)).isTrue();
            assertThat(updatedPerson.hasRelationExId(RelationshipType.PARENT, childId)).isTrue();

            assertThat(updatedPerson.hasRelationExId(partnerId)).isTrue();
            assertThat(updatedPerson.hasRelationExId(RelationshipType.PARTNER, partnerId)).isTrue();
        }

        @Test
        @DisplayName("Should create new person when externalId does not exist and relationships are provided")
        void testCreateNewPersonWhenExternalIdNotFound() {
            Long mainId = getId(); // Not in DB
            Long parentId = getId();
            Long partnerId = getId();
            Long childId = getId();

            // Save related persons only
            personDao.save(PersonEntity.builder().externalId(parentId).name("Parent").build());
            personDao.save(PersonEntity.builder().externalId(partnerId).name("Partner").build());
            personDao.save(PersonEntity.builder().externalId(childId).name("Child").build());

            var request = new PersonDetailsRequest()
                .id(mainId)
                .name("New Person")
                .birthDate(LocalDate.of(2000, 1, 1))
                .parent1(new Relation().id(parentId))
                .parent2(null)
                .partner(new Relation().id(partnerId))
                .addChildrenItem(new Relation().id(childId));

            FullPerson response;
            try {
                response = api.v1PeoplePost(request);
            } catch (RestClientResponseException e) {
                fail("API call failed with status " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
                return;
            }

            assertThat(response.getId()).isEqualTo(mainId);
            assertThat(response.getName()).isEqualTo("New Person");
            assertThat(response.getBirthDate()).isEqualTo(LocalDate.of(2000, 1, 1));

            assertThat(response.getParent1()).isNotNull();
            assert response.getParent1() != null;
            assertThat(response.getParent1().getId()).isEqualTo(parentId);

            assertThat(response.getPartner()).isNotNull();
            assert response.getPartner() != null;
            assertThat(response.getPartner().getId()).isEqualTo(partnerId);

            assertThat(response.getChildren()).hasSize(1);
            assert response.getChildren() != null;
            assertThat(response.getChildren().getFirst().getId()).isEqualTo(childId);
        }



        @Test
        @DisplayName("Should create all persons if none of the IDs exist")
        void testV1PeoplePost_WithAllMissingIds_ShouldStillPass() {
            // IDs that are NOT yet in the DB
            Long mainId = getId();
            Long parent1Id = getId();
            Long parent2Id = getId();
            Long partnerId = getId();
            Long childId = getId();

            var request = new PersonDetailsRequest()
                .id(mainId)
                .name("AutoCreated Main")
                .birthDate(LocalDate.of(1992, 1, 1))
                .parent1(new Relation().id(parent1Id))
                .parent2(new Relation().id(parent2Id))
                .partner(new Relation().id(partnerId))
                .addChildrenItem(new Relation().id(childId));

            FullPerson person;
            try {
                person = api.v1PeoplePost(request);
            } catch (RestClientResponseException e) {
                fail("API call failed with status " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
                return;
            }

            assertThat(person.getId()).isEqualTo(mainId);
            assertThat(person.getName()).isEqualTo("AutoCreated Main");
            assertThat(person.getBirthDate()).isEqualTo(LocalDate.of(1992, 1, 1));

            assertThat(person.getParent1()).isNotNull();
            assert person.getParent1() != null;
            assertThat(person.getParent1().getId()).isEqualTo(parent1Id);

            assertThat(person.getParent2()).isNotNull();
            assert person.getParent2() != null;
            assertThat(person.getParent2().getId()).isEqualTo(parent2Id);

            assertThat(person.getPartner()).isNotNull();
            assert person.getPartner() != null;
            assertThat(person.getPartner().getId()).isEqualTo(partnerId);

            assertThat(person.getChildren()).hasSize(1);
            assert person.getChildren() != null;
            assertThat(person.getChildren().getFirst().getId()).isEqualTo(childId);

            assertThat(personDao.findAll()).hasSize(5);
        }

        @Test
        @DisplayName("Should update only name and DOB when no relationships provided")
        void testV1PeoplePost_UpdateBasicInfoOnly() {
            Long mainId = getId();

            // Save initial person with no relationships
            personDao.save(PersonEntity.builder()
                .externalId(mainId)
                .name("Old Name")
                .dateOfBirth(LocalDate.of(1980, 1, 1))
                .build());

            var request = new PersonDetailsRequest()
                .id(mainId)
                .name("Updated Name")
                .birthDate(LocalDate.of(1995, 3, 15));
            // No relationships set

            FullPerson person;
            try {
                person = api.v1PeoplePost(request);
            } catch (RestClientResponseException e) {
                fail("API call failed with status " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
                return;
            }

            assertThat(person.getId()).isEqualTo(mainId);
            assertThat(person.getName()).isEqualTo("Updated Name");
            assertThat(person.getBirthDate()).isEqualTo(LocalDate.of(1995, 3, 15));

            assertThat(person.getParent1()).isNull();
            assertThat(person.getParent2()).isNull();
            assertThat(person.getPartner()).isNull();
            assertThat(person.getChildren()).isEmpty();

            // Verify persistence
            var updatedPerson = personDao.findByExternalIdEntity(mainId).stream().findFirst();
            assertThat(updatedPerson).isPresent();
            assertThat(updatedPerson.orElseThrow().getName()).isEqualTo("Updated Name");
            assertThat(updatedPerson.orElseThrow().getDateOfBirth()).isEqualTo(LocalDate.of(1995, 3, 15));
            assertThat(updatedPerson.orElseThrow().getRelationships()).isEmpty();
        }


        @Test
        @DisplayName("Should delete valid persons and ignore non-existent ones")
        void testDeleteMixedValidAndInvalidIds() {
            Long validId = getId();
            Long invalidId = getId();

            personDao.save(PersonEntity.builder()
                .externalId(validId)
                .name("Valid Person")
                .dateOfBirth(LocalDate.of(1995, 1, 1))
                .deleted(false)
                .build());

            try {
                // Uses generated client; assumes v1PeopleDelete is available and mapped correctly
                api.v1PeopleDelete(List.of(validId, invalidId));
            } catch (RestClientResponseException e) {
                fail("API DELETE call failed with status " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
                return;
            }

            // Verify the valid person is soft-deleted
            PersonEntity fetched = personDao.findByExternalIdEntity(validId).orElseThrow();
            assertThat(fetched.isDeleted()).isTrue();
        }


        @Test
        @DisplayName("Should handle duplicate IDs in request gracefully")
        void testDeleteWithDuplicateIdsInRequest() {
            Long externalId = getId();

            personDao.save(PersonEntity.builder()
                .externalId(externalId)
                .name("Duplicate Target")
                .dateOfBirth(LocalDate.of(1985, 2, 2))
                .deleted(false)
                .build());

            try {
                api.v1PeopleDelete(List.of(externalId, externalId, externalId));
            } catch (RestClientResponseException e) {
                fail("API DELETE call failed with status " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
                return;
            }

            PersonEntity fetched = personDao.findByExternalIdEntity(externalId).orElseThrow();
            assertThat(fetched.isDeleted()).isTrue();
        }


        @Test
        @DisplayName("Should handle large batch delete")
        void testDeleteLargeBatch() {
            List<Long> ids = LongStream.rangeClosed(7000, 7050)
                .boxed()
                .toList();

            List<PersonEntity> people = ids.stream()
                .map(id -> PersonEntity.builder()
                    .externalId(id)
                    .name("Bulk Person " + id)
                    .dateOfBirth(LocalDate.of(1990, 1, 1))
                    .deleted(false)
                    .build())
                .toList();

            personDao.saveAll(people);

            try {
                api.v1PeopleDelete(ids);
            } catch (RestClientResponseException e) {
                fail("API DELETE call failed with status " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
                return;
            }

            ids.forEach(id -> {
                PersonEntity entity = personDao.findByExternalIdEntity(id).orElseThrow();
                assertThat(entity.isDeleted()).isTrue();
            });
        }



    }

    @Nested
    class Delete {
        @Test
        @DisplayName("Should mark people as blacklisted when DELETE is called")
        void testDeleteApiMarksBlacklisted() {
            Long externalId = getId();

            personDao.saveAndFlush(
                PersonEntity.builder()
                    .externalId(externalId)
                    .name("Mark Me")
                    .dateOfBirth(LocalDate.of(1990, 1, 1))
                    .build());

            try {
                api.v1PeopleDelete(List.of(externalId));
            } catch (RestClientResponseException e) {
                fail("API DELETE failed with status " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
                return;
            }

            personDao.flush();
            PersonEntity updated = personDao.findByExternalIdEntity(externalId).orElseThrow();
            assertThat(updated.isDeleted()).isTrue();
        }


        @Test
        @DisplayName("Should ignore blacklisted person ID in POST /v1/people")
        void testPostIgnoresBlacklistedPerson() {
            Long externalId = getId();

            PersonEntity blacklisted = PersonEntity.builder()
                .externalId(externalId)
                .name("Blacklisted User")
                .dateOfBirth(LocalDate.of(1985, 5, 5))
                .deleted(true)
                .build();

            personDao.save(blacklisted);

            var request = new PersonDetailsRequest()
                .id(externalId)
                .name("Should Not Be Processed")
                .birthDate(LocalDate.of(1990, 1, 1));

            FullPerson response = api.v1PeoplePost(request);

            assertThat(response).isNull();

            // Ensure no update occurred in DB
            PersonEntity fetched = personDao.findByExternalIdEntity(externalId).orElseThrow();
            assertThat(fetched.getName()).isEqualTo("Blacklisted User");
            assertThat(fetched.getDateOfBirth()).isEqualTo(LocalDate.of(1985, 5, 5));
            assertThat(fetched.isDeleted()).isTrue();
        }


        @Test
        @DisplayName("Should not fail when blacklisting the same person twice")
        void testBlacklistIdempotency() {
            Long externalId = getId();

            personDao.saveAndFlush(PersonEntity.builder()
                .externalId(externalId)
                .name("Already Blacklisted")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .deleted(true)
                .build());

            try {
                api.v1PeopleDelete(List.of(externalId));
            } catch (RestClientResponseException e) {
                fail("API DELETE failed with status " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
                return;
            }

            personDao.flush();
            var fetched = personDao.findByExternalIdEntity(externalId).orElseThrow();
            assertThat(fetched.isDeleted()).isTrue(); // Still blacklisted, no side effects
        }


        @Test
        @DisplayName("Should mark multiple people as deleted")
        void testDeleteMultiplePeople() {
            Long id1 = getId();
            Long id2 = getId();

            personDao.saveAll(List.of(
                PersonEntity.builder().externalId(id1).name("Person 1").dateOfBirth(LocalDate.of(1990, 1, 1)).deleted(false).build(),
                PersonEntity.builder().externalId(id2).name("Person 2").dateOfBirth(LocalDate.of(1991, 2, 2)).deleted(false).build()
            ));

            try {
                api.v1PeopleDelete(List.of(id1, id2));
            } catch (RestClientResponseException e) {
                fail("API DELETE failed with status " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
                return;
            }

            personDao.flush();

            PersonEntity person1 = personDao.findByExternalIdEntity(id1).orElseThrow();
            PersonEntity person2 = personDao.findByExternalIdEntity(id2).orElseThrow();

            assertThat(person1.isDeleted()).isTrue();
            assertThat(person2.isDeleted()).isTrue();
        }


        @Test
        @DisplayName("Should silently ignore non-existent IDs")
        void testDeleteIgnoresMissingIds() {
            Long nonExistentId = getId();

            try {
                api.v1PeopleDelete(List.of(nonExistentId));
            } catch (RestClientResponseException e) {
                fail("API DELETE failed with status " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
                return;
            }

            assertThat(personDao.findByExternalIdEntity(nonExistentId)).isEmpty();
        }


    }

    private PersonEntity createChild(String name, LocalDate dob) {
        return PersonEntity.builder()
            .name(name)
            .externalId(new Random().nextLong())
            .dateOfBirth(dob)
            .build();
    }

    private Long getId() {
        var id = ThreadLocalRandom.current().nextInt(1000) + System.nanoTime() % 100000;
        log.debug("Using ID {}", id);
        return id;
    }
}
