package com.pink.family.assignment.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pink.family.api.rest.server.model.FullPerson;
import com.pink.family.api.rest.server.model.PersonDetailsRequest;
import com.pink.family.api.rest.server.model.Relation;
import com.pink.family.api.rest.server.model.SpecificPersonCheckRequest;
import com.pink.family.assignment.database.dao.PersonDao;
import com.pink.family.assignment.database.entity.PersonEntity;
import com.pink.family.assignment.database.entity.enums.RelationshipType;
import com.pink.family.assignment.service.PersonService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)

class PersonControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PersonDao personDao;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        personDao.deleteAll(); // reset database between tests
    }

    @Nested
    class CheckExistingPerson {
        @Test
        @DisplayName("Should return 444 when multiple people match name and DOB")
        void shouldReturn444_WhenMultiplePeopleMatchNameAndDob() throws Exception {
            personDao.save(PersonEntity.builder()
                .name("Alex")
                .externalId(111L)
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build());

            personDao.save(PersonEntity.builder()
                .name("Alex")
                .externalId(222L)
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build());

            SpecificPersonCheckRequest request = new SpecificPersonCheckRequest()
                .requestId("RQ_MULTIPLE")
                .name("Alex")
                .dateOfBirth(LocalDate.of(1990, 1, 1));

            mockMvc.perform(post("/v1/people/check-existing-person")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(444))
                .andExpect(jsonPath("$.code", is("444")))
                .andExpect(jsonPath("$.message", is(PersonService.Constants.ErrorMsg.NO_DISTINCT_RECORD)))
                .andExpect(jsonPath("$.requestId", is("RQ_MULTIPLE")));
        }

        @Test
        @DisplayName("Should return 444 when only some children are shared with the partner")
        void shouldReturn444_WhenNotAllChildrenAreSharedWithPartner() throws Exception {
            PersonEntity main = personDao.save(PersonEntity.builder()
                .name("SharedDad")
                .externalId(321L)
                .dateOfBirth(LocalDate.of(1985, 5, 5))
                .build());

            PersonEntity partner1 = personDao.save(PersonEntity.builder()
                .name("Mom1")
                .externalId(654L)
                .dateOfBirth(LocalDate.of(1985, 6, 6))
                .build());

            PersonEntity partner2 = personDao.save(PersonEntity.builder()
                .name("Mom2")
                .externalId(987L)
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
                .id(321L)
                .dateOfBirth(LocalDate.of(1985, 5, 5));

            mockMvc.perform(post("/v1/people/check-existing-person")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(444))
                .andExpect(jsonPath("$.code", is("444")))
                .andExpect(jsonPath("$.message", is(PersonService.Constants.ErrorMsg.NO_SHARED_CHILDREN)))
                .andExpect(jsonPath("$.requestId", is("RQ_NOT_SHARED")));
        }


        @Test
        @DisplayName("Should return 200 when external ID is missing but name and DOB match an existing person")
        void shouldReturn200_WhenIdMissingButNameAndDobMatch() throws Exception {
            // Save existing person
            PersonEntity existing = personDao.save(
                PersonEntity.builder()
                    .name("NoIDPerson")
                    .externalId(888777666L)
                    .dateOfBirth(LocalDate.of(1990, 6, 15))
                    .build()
            );

            // Add partner and exactly 3 children
            PersonEntity partner = personDao.save(
                PersonEntity.builder()
                    .name("Partner")
                    .externalId(999888777L)
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

            mockMvc.perform(post("/v1/people/check-existing-person")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        }


        @Test
        @DisplayName("Should return 200 when a person has a partner and exactly 3 children with the same partner")
        void shouldReturn200_WhenPartnerAnd3ChildrenExist() throws Exception {
            PersonEntity main = PersonEntity.builder()
                .name("John")
                .externalId(123456789L)
                .dateOfBirth(LocalDate.of(1990, 5, 20))
                .build();

            PersonEntity partner = PersonEntity.builder()
                .name("Jane")
                .externalId(987654321L)
                .dateOfBirth(LocalDate.of(1988, 3, 15))
                .build();

            PersonEntity child1 = createChild("Child1", LocalDate.of(2010, 1, 1));
            PersonEntity child2 = createChild("Child2", LocalDate.of(2012, 2, 2));
            // make sure the child is under 18
            PersonEntity child3 = createChild("Child3", LocalDate.of(LocalDate.now().getYear() - 10, 3, 3));

            // Save all first to get IDs assigned
            main = personDao.save(main);
            partner = personDao.save(partner);
            child1 = personDao.save(child1);
            child2 = personDao.save(child2);
            child3 = personDao.save(child3);

            // Add relationships after IDs are assigned
            main.addRelationship(partner, RelationshipType.PARTNER, RelationshipType.PARTNER);
            main.addRelationship(child1, RelationshipType.PARENT, RelationshipType.CHILD);
            main.addRelationship(child2, RelationshipType.PARENT, RelationshipType.CHILD);
            main.addRelationship(child3, RelationshipType.PARENT, RelationshipType.CHILD);

            partner.addRelationship(child1, RelationshipType.PARENT, RelationshipType.CHILD);
            partner.addRelationship(child2, RelationshipType.PARENT, RelationshipType.CHILD);
            partner.addRelationship(child3, RelationshipType.PARENT, RelationshipType.CHILD);

            log.info("SAVING ALL TEST DATA");
            // Save all entities again to persist the relationships
            personDao.saveAndFlush(main);
            personDao.saveAndFlush(partner);
            personDao.saveAndFlush(child1);
            personDao.saveAndFlush(child2);
            personDao.saveAndFlush(child3);

            // Prepare API request object
            SpecificPersonCheckRequest request = new SpecificPersonCheckRequest()
                .requestId("RQ123")
                .name("Jane")
                .id(123456789L)
                .dateOfBirth(LocalDate.of(1990, 5, 20));

            // Perform POST request and expect 200 OK
            mockMvc.perform(post("/v1/people/check-existing-person")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 444 when person has more than 3 children with partner")
        void shouldReturn444_WhenMoreThan3ChildrenExist() throws Exception {
            PersonEntity main = PersonEntity.builder()
                .name("Anna")
                .externalId(111222333L)
                .dateOfBirth(LocalDate.of(1985, 7, 15))
                .build();
            PersonEntity partner = PersonEntity.builder()
                .name("Mark")
                .externalId(444555666L)
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
                .id(111222333L)
                .dateOfBirth(LocalDate.of(1985, 7, 15));

            mockMvc.perform(post("/v1/people/check-existing-person")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(444))
                .andExpect(jsonPath("$.code", is("444")))
                .andExpect(jsonPath("$.message", is(PersonService.Constants.ErrorMsg.NOT_EXACTLY_3_CHILDREN)))
                .andExpect(jsonPath("$.requestId", is("RQ999")));
        }

        @Test
        @DisplayName("Should return 444 when children have different partners")
        void shouldReturn444_WhenChildrenHaveDifferentPartners() throws Exception {
            PersonEntity main = PersonEntity.builder()
                .name("Emma")
                .externalId(222333444L)
                .dateOfBirth(LocalDate.of(1987, 8, 20))
                .build();
            PersonEntity partner1 = PersonEntity.builder()
                .name("Liam")
                .externalId(555666777L)
                .dateOfBirth(LocalDate.of(1986, 7, 10))
                .build();
            PersonEntity partner2 = PersonEntity.builder()
                .name("Noah")
                .externalId(888999000L)
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

            // Different partners for children
            partner1.addRelationship(child1, RelationshipType.PARENT, RelationshipType.CHILD);
            partner2.addRelationship(child2, RelationshipType.PARENT, RelationshipType.CHILD);
            partner1.addRelationship(child3, RelationshipType.PARENT, RelationshipType.CHILD);

            personDao.saveAndFlush(main);
            personDao.saveAndFlush(partner1);
            personDao.saveAndFlush(partner2);

            SpecificPersonCheckRequest request = new SpecificPersonCheckRequest()
                .requestId("RQ888")
                .name("Emma")
                .id(222333444L)
                .dateOfBirth(LocalDate.of(1987, 8, 20));

            mockMvc.perform(post("/v1/people/check-existing-person")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(444))
                .andExpect(jsonPath("$.code", is("444")))
                .andExpect(jsonPath("$.message", is(PersonService.Constants.ErrorMsg.NO_SHARED_CHILDREN)))
                .andExpect(jsonPath("$.requestId", is("RQ888")));
        }


        @Test
        @DisplayName("Should return 444 when none of the children are under 18")
        void shouldReturn444_WhenChildrenAreAllAdults() throws Exception {
            PersonEntity main = PersonEntity.builder()
                .name("Oliver")
                .externalId(333444555L)
                .dateOfBirth(LocalDate.of(1980, 9, 30))
                .build();
            PersonEntity partner = PersonEntity.builder()
                .name("Sophia")
                .externalId(666777888L)
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
                .id(333444555L)
                .dateOfBirth(LocalDate.of(1980, 9, 30));

            mockMvc.perform(post("/v1/people/check-existing-person")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(444))
                .andExpect(jsonPath("$.code", is("444")))
                .andExpect(jsonPath("$.message", is(PersonService.Constants.ErrorMsg.NO_UNDERAGE_CHILD)))
                .andExpect(jsonPath("$.requestId", is("RQ777")));
        }


        @Test
        @DisplayName("Should return 200 when no external id but name, surname, and DOB match")
        void shouldReturn200_WhenNoExternalIdButNameSurnameDobMatch() throws Exception {
            PersonEntity main = PersonEntity.builder()
                .name("Lucas")
                .externalId(444555666L)
                .dateOfBirth(LocalDate.of(1992, 11, 11))
                .build();

            PersonEntity partner = PersonEntity.builder()
                .name("Mia")
                .externalId(777888999L)
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

            mockMvc.perform(post("/v1/people/check-existing-person")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 444 when children are not linked to main person but only to partner")
        void shouldReturn444_WhenChildrenOnlyLinkedToPartner() throws Exception {
            PersonEntity main = personDao.save(PersonEntity.builder()
                .name("IsolatedParent")
                .externalId(123L)
                .dateOfBirth(LocalDate.of(1980, 1, 1))
                .build());

            PersonEntity partner = personDao.save(PersonEntity.builder()
                .name("EngagedParent")
                .externalId(456L)
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
                .id(123L)
                .dateOfBirth(LocalDate.of(1980, 1, 1));

            mockMvc.perform(post("/v1/people/check-existing-person")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(444))
                .andExpect(jsonPath("$.code", is("444")))
                .andExpect(jsonPath("$.message", is(PersonService.Constants.ErrorMsg.NO_SHARED_CHILDREN)))
                .andExpect(jsonPath("$.requestId", is("RQ_CHILDREN_NOT_MINE")));
        }

        @Test
        @DisplayName("Should return 400 when malformed JSON is sent")
        void shouldReturn400_WhenMalformedJson() throws Exception {
            String badJson = "{ \"requestId\": \"RQ_BAD\", \"name\": \"Oops\", "; // truncated JSON

            mockMvc.perform(post("/v1/people/check-existing-person")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(badJson))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when name is missing all values")
        void shouldReturn400_WhenNameMissing() throws Exception {
            String request = """
                    {
                        "requestId": "RQ_MISSING",
                        "id": ,
                        "dateOfBirth": ""
                    }
                """;

            mockMvc.perform(post("/v1/people/check-existing-person")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 444 when no matching person is found")
        void shouldReturn444_WhenNoMatchFound() throws Exception {
            SpecificPersonCheckRequest request = new SpecificPersonCheckRequest()
                .requestId("RQ_NO_MATCH")
                .id(999999999L)
                .name("Ghost")
                .dateOfBirth(LocalDate.of(1900, 1, 1));

            mockMvc.perform(post("/v1/people/check-existing-person")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(444))
                .andExpect(jsonPath("$.code", is("444")))
                .andExpect(jsonPath("$.message", is(PersonService.Constants.ErrorMsg.NO_RECORD)))
                .andExpect(jsonPath("$.requestId", is("RQ_NO_MATCH")));
        }
    }

    @Nested
    class PersonPost {

        @Test
        @DisplayName("Should successfully update person with parent, partner, child and return full relationship details")
        void testV1PeoplePost_WithValidData_AllExist_AndFullDetailsReturned() throws Exception {

            Long mainId = 100L;
            Long parent1Id = 1L;
            Long parent2Id = 2L;
            Long partnerId = 3L;
            Long childId = 4L;

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

            var result = mockMvc.perform(post("/v1/people")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(mainId))
                .andExpect(jsonPath("$.name").value("Main Updated"))
                .andExpect(jsonPath("$.birthDate").value("1990-05-20"))

                // Parent 1
                .andExpect(jsonPath("$.parent1.id").value(parent1Id))
                .andExpect(jsonPath("$.parent1.name").value("Parent One"))
                .andExpect(jsonPath("$.parent1.birthDate").value("1960-01-01"))

                // Parent 2
                .andExpect(jsonPath("$.parent2.id").value(parent2Id))
                .andExpect(jsonPath("$.parent2.name").value("Parent Two"))
                .andExpect(jsonPath("$.parent2.birthDate").value("1965-02-02"))

                // Partner
                .andExpect(jsonPath("$.partner.id").value(partnerId))
                .andExpect(jsonPath("$.partner.name").value("Partner Person"))
                .andExpect(jsonPath("$.partner.birthDate").value("1991-03-03"))

                // Child
                .andExpect(jsonPath("$.children[0].id").value(childId))
                .andExpect(jsonPath("$.children[0].name").value("Child Person"))
                .andExpect(jsonPath("$.children[0].birthDate").value("2010-04-04"))
                .andReturn();

            // Validate returned DTO as object
            String json = result.getResponse().getContentAsString();
            FullPerson person = objectMapper.readValue(json, FullPerson.class);

            assertThat(person.getId()).isEqualTo(mainId);
            assertThat(person.getName()).isEqualTo("Main Updated");
            assertThat(person.getBirthDate()).isEqualTo(LocalDate.of(1990, 5, 20));

            assertThat(person.getParent1().getName()).isEqualTo("Parent One");
            assertThat(person.getParent2().getName()).isEqualTo("Parent Two");
            assertThat(person.getPartner().getName()).isEqualTo("Partner Person");
            assertThat(person.getChildren().get(0).getName()).isEqualTo("Child Person");

            // Double-check DB persisted update
            var saved = personDao.findByExternalId(mainId).stream().findFirst();
            assertThat(saved).isPresent();
            assertThat(saved.get().getName()).isEqualTo("Main Updated");
            assertThat(saved.get().getDateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 20));
        }

        @Test
        @DisplayName("Should update person with partial relationships: one parent, no partner, multiple children")
        void testV1PeoplePost_PartialRelationships() throws Exception {
            Long mainId = 300L;
            Long parent1Id = 10L;
            Long child1Id = 20L;
            Long child2Id = 21L;

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
                // No parent2 provided
                // No partner provided
                .addChildrenItem(new Relation().id(child1Id))
                .addChildrenItem(new Relation().id(child2Id));

            var result = mockMvc.perform(post("/v1/people")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(mainId))
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.birthDate").value("1990-01-01"))
                // Check parent1 exists
                .andExpect(jsonPath("$.parent1.id").value(parent1Id))
                .andExpect(jsonPath("$.parent1.name").value("Single Parent"))
                .andExpect(jsonPath("$.parent1.birthDate").value("1955-06-15"))
                // parent2 and partner should NOT exist
                .andExpect(jsonPath("$.parent2").doesNotExist())
                .andExpect(jsonPath("$.partner").doesNotExist())
                // Children array contains 2 children with correct IDs and names
                .andExpect(jsonPath("$.children").isArray())
                .andExpect(jsonPath("$.children.length()").value(2))
                .andExpect(jsonPath("$.children[?(@.id == %d)].name", child1Id).value("Child One"))
                .andExpect(jsonPath("$.children[?(@.id == %d)].name", child2Id).value("Child Two"))
                .andReturn();

            // Check DB updated accordingly
            var saved = personDao.findByExternalId(mainId).stream().findFirst();
            assertThat(saved).isPresent();
            assertThat(saved.get().getName()).isEqualTo("Updated Name");
            assertThat(saved.get().getDateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 1));
        }

        @Test
        @DisplayName("Should keep existing relationships if no relations are sent in update")
        void testUpdatePerson_NoRelationsSent_ExistingRelationsNotCleared() throws Exception {

            Long mainId = 100L;
            Long parentId = 1L;
            Long partnerId = 2L;
            Long childId = 3L;

            // Save related persons
            personDao.save(PersonEntity.builder().externalId(parentId).name("Existing Parent").build());
            personDao.save(PersonEntity.builder().externalId(partnerId).name("Existing Partner").build());
            personDao.save(PersonEntity.builder().externalId(childId).name("Existing Child").build());

            // Save main person with existing relationships
            PersonEntity mainPerson = PersonEntity.builder()
                .externalId(mainId)
                .name("Main Person")
                .build();

            PersonEntity parent = personDao.findByExternalId(parentId).iterator().next();
            PersonEntity partner = personDao.findByExternalId(partnerId).iterator().next();
            PersonEntity child = personDao.findByExternalId(childId).iterator().next();

            personDao.saveAll(List.of(mainPerson, parent, partner, child));

            mainPerson.addRelationship(parent, RelationshipType.CHILD, RelationshipType.PARENT);
            mainPerson.addRelationship(partner, RelationshipType.PARTNER, RelationshipType.PARTNER);
            mainPerson.addRelationship(child, RelationshipType.PARENT, RelationshipType.CHILD);

            personDao.save(mainPerson);

            // Prepare update request WITHOUT any relations (empty sets)
            var request = new PersonDetailsRequest()
                .id(mainId)
                .name("Main Updated")                 // Change name to trigger update
                .birthDate(LocalDate.of(1990, 1, 1))
                .parent1(null)
                .parent2(null)
                .partner(null)
                .children(null);                     // No relationships sent

            var result = mockMvc.perform(post("/v1/people")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

            // Reload main person from DB
            var saved = personDao.findByExternalId(mainId).stream().findFirst();
            assertThat(saved).isPresent();

            PersonEntity updatedPerson = saved.get();

            // Name and DOB should be updated
            assertThat(updatedPerson.getName()).isEqualTo("Main Updated");
            assertThat(updatedPerson.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 1));

            // Relationships should NOT be cleared — should still have 3 relations
            assertThat(updatedPerson.getRelationships()).isNotEmpty();
            assertThat(updatedPerson.getRelationships().size()).isEqualTo(3);

            // Check related external IDs still present
            Set<Long> relatedIds = updatedPerson.getRelationships().stream()
                .map(rel -> rel.getRelatedPerson().getExternalId())
                .collect(Collectors.toSet());

            assertThat(relatedIds).containsExactlyInAnyOrder(parentId, partnerId, childId);
        }

        @Test
        @DisplayName("Should add new relationships and keep existing relationships not mentioned in update")
        void testUpdatePerson_PartialRelationsSent_ExistingRelationsRetained() throws Exception {

            Long mainId = 100L;
            Long existingParentId = 1L;
            Long existingPartnerId = 2L;
            Long existingChildId = 3L;

            Long newPartnerId = 4L;  // New partner to add

            // Save persons
            personDao.save(PersonEntity.builder().externalId(existingParentId).name("Existing Parent").build());
            personDao.save(PersonEntity.builder().externalId(existingPartnerId).name("Existing Partner").build());
            personDao.save(PersonEntity.builder().externalId(existingChildId).name("Existing Child").build());
            personDao.save(PersonEntity.builder().externalId(newPartnerId).name("New Partner").build());

            // Save main person with existing relationships
            PersonEntity mainPerson = personDao.save(PersonEntity.builder()
                .externalId(mainId)
                .name("Main Person")
                .build());

            PersonEntity existingParent = personDao.findByExternalId(existingParentId).iterator().next();
            PersonEntity existingPartner = personDao.findByExternalId(existingPartnerId).iterator().next();
            PersonEntity existingChild = personDao.findByExternalId(existingChildId).iterator().next();

            mainPerson.addRelationship(existingParent, RelationshipType.CHILD, RelationshipType.PARENT);
            mainPerson.addRelationship(existingPartner, RelationshipType.PARTNER, RelationshipType.PARTNER);
            mainPerson.addRelationship(existingChild, RelationshipType.PARENT, RelationshipType.CHILD);

            personDao.save(mainPerson);

            // Prepare update request with only new partner relationship (no parents or children)
            var request = new PersonDetailsRequest()
                .id(mainId)
                .name("Main Updated")
                .birthDate(LocalDate.of(1990, 1, 1))
                .partner(new Relation().id(newPartnerId));  // Add new partner only

            var result = mockMvc.perform(post("/v1/people")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

            // Reload main person from DB
            var savedOpt = personDao.findByExternalId(mainId).stream().findFirst();
            assertThat(savedOpt).isPresent();

            PersonEntity updatedPerson = savedOpt.get();

            // Name and DOB updated
            assertThat(updatedPerson.getName()).isEqualTo("Main Updated");
            assertThat(updatedPerson.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 1));

            // Relationships count should now be 4 (existing 3 + new 1)
            assertThat(updatedPerson.getRelationships()).hasSize(4);

            // Related IDs should include existing and new partner
            Set<Long> relatedIds = updatedPerson.getRelationships().stream()
                .map(rel -> rel.getRelatedPerson().getExternalId())
                .collect(Collectors.toSet());

            assertThat(relatedIds).contains(existingParentId, existingPartnerId, existingChildId, newPartnerId);
        }

        @Test
        @DisplayName("Should update person details without modifying existing relationships when same relations are sent")
        void testUpdatePerson_DetailsUpdated_NoRelationChange() throws Exception {

            Long mainId = 100L;
            Long parentId = 1L;
            Long partnerId = 2L;
            Long childId = 3L;

            // Save related persons
            personDao.save(PersonEntity.builder().externalId(parentId).name("Parent").build());
            personDao.save(PersonEntity.builder().externalId(partnerId).name("Partner").build());
            personDao.save(PersonEntity.builder().externalId(childId).name("Child").build());

            // Save main person with existing relations
            PersonEntity mainPerson = personDao.save(PersonEntity.builder()
                .externalId(mainId)
                .name("Original Name")
                .dateOfBirth(LocalDate.of(1980, 1, 1))
                .build());

            PersonEntity parent = personDao.findByExternalId(parentId).iterator().next();
            PersonEntity partner = personDao.findByExternalId(partnerId).iterator().next();
            PersonEntity child = personDao.findByExternalId(childId).iterator().next();

            mainPerson.addRelationship(parent, RelationshipType.CHILD, RelationshipType.PARENT);
            mainPerson.addRelationship(partner, RelationshipType.PARTNER, RelationshipType.PARTNER);
            mainPerson.addRelationship(child, RelationshipType.PARENT, RelationshipType.CHILD);

            personDao.save(mainPerson);

            // Prepare update request with same relationships
            var request = new PersonDetailsRequest()
                .id(mainId)
                .name("Updated Name")
                .birthDate(LocalDate.of(1990, 2, 2))
                .parent1(new Relation().id(parentId))
                .partner(new Relation().id(partnerId))
                .addChildrenItem(new Relation().id(childId));

            var result = mockMvc.perform(post("/v1/people")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

            // Reload main person from DB
            var savedOpt = personDao.findByExternalId(mainId).stream().findFirst();
            assertThat(savedOpt).isPresent();

            PersonEntity updatedPerson = savedOpt.get();

            // Name and DOB updated
            assertThat(updatedPerson.getName()).isEqualTo("Updated Name");
            assertThat(updatedPerson.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 2, 2));

            // Relationships count should remain unchanged (3)
            assertThat(updatedPerson.getRelationships()).hasSize(3);

            // Confirm relationships are the same external IDs
            Set<Long> relatedIds = updatedPerson.getRelationships().stream()
                .map(rel -> rel.getRelatedPerson().getExternalId())
                .collect(Collectors.toSet());

            assertThat(relatedIds).containsExactlyInAnyOrder(parentId, partnerId, childId);
        }

        @Test
        @DisplayName("Should add new relationships without removing existing ones")
        void testAddNewRelationships_PreservesExisting() throws Exception {

            Long mainId = 100L;
            Long existingParentId = 1L;
            Long existingPartnerId = 2L;
            Long newChildId = 5L;    // New child to be added

            // Save related persons
            personDao.save(PersonEntity.builder().externalId(existingParentId).name("Existing Parent").build());
            personDao.save(PersonEntity.builder().externalId(existingPartnerId).name("Existing Partner").build());
            personDao.save(PersonEntity.builder().externalId(newChildId).name("New Child").build());

            // Save main person with existing parent and partner
            PersonEntity mainPerson = personDao.save(PersonEntity.builder()
                .externalId(mainId)
                .name("Original Name")
                .dateOfBirth(LocalDate.of(1980, 1, 1))
                .build());

            PersonEntity existingParent = personDao.findByExternalId(existingParentId).iterator().next();
            PersonEntity existingPartner = personDao.findByExternalId(existingPartnerId).iterator().next();

            mainPerson.addRelationship(existingParent, RelationshipType.CHILD, RelationshipType.PARENT);
            mainPerson.addRelationship(existingPartner, RelationshipType.PARTNER, RelationshipType.PARTNER);

            personDao.save(mainPerson);

            // Prepare request adding a new child relationship (existing relationships remain)
            var request = new PersonDetailsRequest()
                .id(mainId)
                .name("Updated Name")
                .birthDate(LocalDate.of(1990, 2, 2))
                .parent1(new Relation().id(existingParentId))
                .partner(new Relation().id(existingPartnerId))
                .addChildrenItem(new Relation().id(newChildId));

            var result = mockMvc.perform(post("/v1/people")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

            var savedOpt = personDao.findByExternalId(mainId).stream().findFirst();
            assertThat(savedOpt).isPresent();

            PersonEntity updatedPerson = savedOpt.get();

            // Verify updated name and birthDate
            assertThat(updatedPerson.getName()).isEqualTo("Updated Name");
            assertThat(updatedPerson.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 2, 2));

            // Relationships count is now 3 (existing parent, existing partner, new child)
            assertThat(updatedPerson.getRelationships()).hasSize(3);

            Set<Long> relatedIds = updatedPerson.getRelationships().stream()
                .map(rel -> rel.getRelatedPerson().getExternalId())
                .collect(Collectors.toSet());

            assertThat(relatedIds).containsExactlyInAnyOrder(existingParentId, existingPartnerId, newChildId);
        }

        @Test
        @DisplayName("Should preserve existing relationships when null or empty relation sets are passed")
        void testUpdateWithNullOrEmptyRelations_PreservesExistingRelationships() throws Exception {

            Long mainId = 100L;
            Long parentId = 1L;
            Long partnerId = 2L;
            Long childId = 3L;

            // Save related persons
            personDao.save(PersonEntity.builder().externalId(parentId).name("Parent").build());
            personDao.save(PersonEntity.builder().externalId(partnerId).name("Partner").build());
            personDao.save(PersonEntity.builder().externalId(childId).name("Child").build());

            // Save main person with relationships
            PersonEntity mainPerson = personDao.save(PersonEntity.builder()
                .externalId(mainId)
                .name("Original Name")
                .dateOfBirth(LocalDate.of(1980, 1, 1))
                .build());

            PersonEntity parent = personDao.findByExternalId(parentId).iterator().next();
            PersonEntity partner = personDao.findByExternalId(partnerId).iterator().next();
            PersonEntity child = personDao.findByExternalId(childId).iterator().next();

            mainPerson.addRelationship(parent, RelationshipType.CHILD, RelationshipType.PARENT);
            mainPerson.addRelationship(partner, RelationshipType.PARTNER, RelationshipType.PARTNER);
            mainPerson.addRelationship(child, RelationshipType.PARENT, RelationshipType.CHILD);

            personDao.save(mainPerson);

            // Prepare request with null or empty relationship sets
            var request = new PersonDetailsRequest()
                .id(mainId)
                .name("Updated Name")
                .birthDate(LocalDate.of(1990, 2, 2))
                .parent1(null)   // null parent1
                .parent2(null)   // null parent2
                .partner(null)   // null partner
                .children(Collections.emptyList()); // empty children list

            var result = mockMvc.perform(post("/v1/people")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

            // Verify DB relationships are still intact
            var savedOpt = personDao.findByExternalId(mainId).stream().findFirst();
            assertThat(savedOpt).isPresent();

            PersonEntity updatedPerson = savedOpt.get();

            // Relationships count remains 3
            assertThat(updatedPerson.getRelationships()).hasSize(3);

            Set<Long> relatedIds = updatedPerson.getRelationships().stream()
                .map(rel -> rel.getRelatedPerson().getExternalId())
                .collect(Collectors.toSet());

            assertThat(relatedIds).containsExactlyInAnyOrder(parentId, partnerId, childId);

            // Check updated name and DOB are persisted
            assertThat(updatedPerson.getName()).isEqualTo("Updated Name");
            assertThat(updatedPerson.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 2, 2));
        }

        @Test
        @DisplayName("Should create new person when externalId does not exist and relationships are provided")
        void testCreateNewPersonWhenExternalIdNotFound() throws Exception {

            Long mainId = 999L; // Not in DB
            Long parentId = 1L;
            Long partnerId = 2L;
            Long childId = 3L;

            // Save related persons only
            personDao.save(PersonEntity.builder().externalId(parentId).name("Parent").build());
            personDao.save(PersonEntity.builder().externalId(partnerId).name("Partner").build());
            personDao.save(PersonEntity.builder().externalId(childId).name("Child").build());

            var request = new PersonDetailsRequest()
                .id(mainId)
                .name("New Person")
                .birthDate(LocalDate.of(2000, 1, 1))
                .parent1(new Relation().id(parentId))
                .parent2(null)  // Optional
                .partner(new Relation().id(partnerId))
                .addChildrenItem(new Relation().id(childId));

            var result = mockMvc.perform(post("/v1/people")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(mainId))
                .andExpect(jsonPath("$.name").value("New Person"))
                .andExpect(jsonPath("$.birthDate").value("2000-01-01"))
                .andExpect(jsonPath("$.parent1.id").value(parentId))
                .andExpect(jsonPath("$.partner.id").value(partnerId))
                .andExpect(jsonPath("$.children[0].id").value(childId))
                .andReturn();

            // Verify DB entry created with relationships
            var savedOpt = personDao.findByExternalId(mainId).stream().findFirst();
            assertThat(savedOpt).isPresent();

            PersonEntity savedPerson = savedOpt.get();

            assertThat(savedPerson.getName()).isEqualTo("New Person");
            assertThat(savedPerson.getDateOfBirth()).isEqualTo(LocalDate.of(2000, 1, 1));

            Set<Long> relatedIds = savedPerson.getRelationships().stream()
                .map(rel -> rel.getRelatedPerson().getExternalId())
                .collect(Collectors.toSet());

            assertThat(relatedIds).containsExactlyInAnyOrder(parentId, partnerId, childId);
        }

        @Test
        @DisplayName("Should create all persons if none of the IDs exist")
        void testV1PeoplePost_WithAllMissingIds_ShouldStillPass() throws Exception {

            // IDs that are NOT yet in the DB
            Long mainId = 200L;
            Long parent1Id = 201L;
            Long parent2Id = 202L;
            Long partnerId = 203L;
            Long childId = 204L;

            var request = new PersonDetailsRequest()
                .id(mainId)
                .name("AutoCreated Main")
                .birthDate(LocalDate.of(1992, 1, 1))
                .parent1(new Relation().id(parent1Id))
                .parent2(new Relation().id(parent2Id))
                .partner(new Relation().id(partnerId))
                .addChildrenItem(new Relation().id(childId));

            var result = mockMvc.perform(post("/v1/people")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(mainId))
                .andExpect(jsonPath("$.name").value("AutoCreated Main"))
                .andExpect(jsonPath("$.birthDate").value("1992-01-01"))
                .andExpect(jsonPath("$.parent1.id").value(parent1Id))
                .andExpect(jsonPath("$.parent2.id").value(parent2Id))
                .andExpect(jsonPath("$.partner.id").value(partnerId))
                .andExpect(jsonPath("$.children[0].id").value(childId))
                .andReturn();

            String json = result.getResponse().getContentAsString();
            FullPerson person = objectMapper.readValue(json, FullPerson.class);

            assertThat(person.getId()).isEqualTo(mainId);
            assertThat(person.getName()).isEqualTo("AutoCreated Main");
            assertThat(person.getParent1().getId()).isEqualTo(parent1Id);
            assertThat(person.getParent2().getId()).isEqualTo(parent2Id);
            assertThat(person.getPartner().getId()).isEqualTo(partnerId);
            assertThat(person.getChildren()).hasSize(1);
            assertThat(person.getChildren().get(0).getId()).isEqualTo(childId);

            assertThat(personDao.findByExternalId(mainId)).isNotEmpty();
            assertThat(personDao.findByExternalId(parent1Id)).isNotEmpty();
            assertThat(personDao.findByExternalId(parent2Id)).isNotEmpty();
            assertThat(personDao.findByExternalId(partnerId)).isNotEmpty();
            assertThat(personDao.findByExternalId(childId)).isNotEmpty();
        }

        @Test
        @DisplayName("Should update only name and DOB when no relationships provided")
        void testV1PeoplePost_UpdateBasicInfoOnly() throws Exception {

            Long mainId = 300L;

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

            var result = mockMvc.perform(post("/v1/people")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(mainId))
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.birthDate").value("1995-03-15"))
                .andExpect(jsonPath("$.parent1").doesNotExist())
                .andExpect(jsonPath("$.parent2").doesNotExist())
                .andExpect(jsonPath("$.partner").doesNotExist())
                .andExpect(jsonPath("$.children").isEmpty())
                .andReturn();

            // Verify persisted entity has updated info
            var updatedPerson = personDao.findByExternalId(mainId).stream().findFirst();
            assertThat(updatedPerson).isPresent();
            assertThat(updatedPerson.get().getName()).isEqualTo("Updated Name");
            assertThat(updatedPerson.get().getDateOfBirth()).isEqualTo(LocalDate.of(1995, 3, 15));
            assertThat(updatedPerson.get().getRelationships()).isEmpty(); // Ensure no new relationships
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
