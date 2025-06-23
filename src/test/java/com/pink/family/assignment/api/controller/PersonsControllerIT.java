package com.pink.family.assignment.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pink.family.api.rest.server.model.SpecificPersonCheckRequest;
import com.pink.family.assignment.database.entity.PersonEntity;
import com.pink.family.assignment.database.entity.enums.RelationshipType;
import com.pink.family.assignment.database.repository.PersonRepository;
import com.pink.family.assignment.service.PersonService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@Transactional // reset the h2 data
class PersonsControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        personRepository.deleteAll(); // reset database between tests
    }

    @Test
    @DisplayName("Should return 200 when a person has a partner and exactly 3 children with the same partner")
    void shouldReturn200_WhenPartnerAnd3ChildrenExist() throws Exception {
        PersonEntity main = PersonEntity.builder()
            .name("John")
            .externalId("123456789")
            .dateOfBirth(LocalDate.of(1990, 5, 20))
            .build();

        PersonEntity partner = PersonEntity.builder()
            .name("Jane")
            .externalId("987654321")
            .dateOfBirth(LocalDate.of(1988, 3, 15))
            .build();

        PersonEntity child1 = createChild("Child1", LocalDate.of(2010, 1, 1));
        PersonEntity child2 = createChild("Child2", LocalDate.of(2012, 2, 2));
        // make sure the child is under 18
        PersonEntity child3 = createChild("Child3", LocalDate.of(LocalDate.now().getYear() - 10, 3, 3));

        // Save all first to get IDs assigned
        main = personRepository.save(main);
        partner = personRepository.save(partner);
        child1 = personRepository.save(child1);
        child2 = personRepository.save(child2);
        child3 = personRepository.save(child3);

        // Add relationships after IDs are assigned
        main.addRelationship(partner, RelationshipType.PARTNER, RelationshipType.PARTNER);
        main.addRelationship(child1, RelationshipType.CHILD, RelationshipType.FATHER);
        main.addRelationship(child2, RelationshipType.CHILD, RelationshipType.FATHER);
        main.addRelationship(child3, RelationshipType.CHILD, RelationshipType.FATHER);

        partner.addRelationship(child1, RelationshipType.CHILD, RelationshipType.MOTHER);
        partner.addRelationship(child2, RelationshipType.CHILD, RelationshipType.MOTHER);
        partner.addRelationship(child3, RelationshipType.CHILD, RelationshipType.MOTHER);

        // Save all entities again to persist the relationships
        personRepository.saveAndFlush(main);
        personRepository.saveAndFlush(partner);
        personRepository.saveAndFlush(child1);
        personRepository.saveAndFlush(child2);
        personRepository.saveAndFlush(child3);

        // Prepare API request object
        SpecificPersonCheckRequest request = new SpecificPersonCheckRequest()
            .requestId("RQ123")
            .name("Jane")
            .id("123456789")
            .dateOfBirth(LocalDate.of(1990, 5, 20));

        // Perform POST request and expect 200 OK
        mockMvc.perform(post("/v1/people/check-existing-person")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
    }


    @Test
    @DisplayName("Should return 444 when no partner or children exist")
    void shouldReturn444_WhenNoPartnerOrChildren() throws Exception {
        personRepository.save(PersonEntity.builder()
            .externalId("000000000")
            .name("Lonely")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .build());

        SpecificPersonCheckRequest apiRequest = new SpecificPersonCheckRequest()
            .requestId("REQ444")
            .id("000000000")
            .name("Lonely")
            .dateOfBirth(LocalDate.of(1990, 1, 1));

        mockMvc.perform(post("/v1/people/check-existing-person")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(apiRequest)))
            .andExpect(status().is(444))
            .andExpect(jsonPath("$.code", is("444")))
            .andExpect(jsonPath("$.message", is(PersonService.Constants.ErrorMsg.NO_PARTNER)))
            .andExpect(jsonPath("$.requestId", is("REQ444")));
    }

    @Test
    @DisplayName("Should return 444 when person has more than 3 children with partner")
    void shouldReturn444_WhenMoreThan3ChildrenExist() throws Exception {
        PersonEntity main = PersonEntity.builder()
            .name("Anna")
            .externalId("111222333")
            .dateOfBirth(LocalDate.of(1985, 7, 15))
            .build();
        PersonEntity partner = PersonEntity.builder()
            .name("Mark")
            .externalId("444555666")
            .dateOfBirth(LocalDate.of(1984, 6, 10))
            .build();
        main = personRepository.save(main);
        partner = personRepository.save(partner);

        PersonEntity child1 = personRepository.save(createChild("Child1", LocalDate.of(2005, 1, 1)));
        PersonEntity child2 = personRepository.save(createChild("Child2", LocalDate.of(2007, 2, 2)));
        PersonEntity child3 = personRepository.save(createChild("Child3", LocalDate.of(2009, 3, 3)));
        PersonEntity child4 = personRepository.save(createChild("Child4", LocalDate.of(2011, 4, 4)));

        main.addRelationship(partner, RelationshipType.PARTNER, RelationshipType.PARTNER);
        main.addRelationship(child1, RelationshipType.CHILD, RelationshipType.FATHER);
        main.addRelationship(child2, RelationshipType.CHILD, RelationshipType.FATHER);
        main.addRelationship(child3, RelationshipType.CHILD, RelationshipType.FATHER);
        main.addRelationship(child4, RelationshipType.CHILD, RelationshipType.FATHER);

        partner.addRelationship(child1, RelationshipType.CHILD, RelationshipType.MOTHER);
        partner.addRelationship(child2,RelationshipType.CHILD, RelationshipType.MOTHER);
        partner.addRelationship(child3, RelationshipType.CHILD, RelationshipType.MOTHER);
        partner.addRelationship(child4, RelationshipType.CHILD, RelationshipType.MOTHER);

        personRepository.saveAndFlush(main);
        personRepository.saveAndFlush(partner);

        SpecificPersonCheckRequest request = new SpecificPersonCheckRequest()
            .requestId("RQ999")
            .name("Anna")
            .id("111222333")
            .dateOfBirth(LocalDate.of(1985, 7, 15));

        mockMvc.perform(post("/v1/people/check-existing-person")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is(444))
            .andExpect(jsonPath("$.code", is("444")))
            .andExpect(jsonPath("$.message", is(PersonService.Constants.ErrorMsg.NO_CHILDREN)))
            .andExpect(jsonPath("$.requestId", is("RQ999")));
    }

    @Test
    @DisplayName("Should return 444 when children have different partners")
    void shouldReturn444_WhenChildrenHaveDifferentPartners() throws Exception {
        PersonEntity main = PersonEntity.builder()
            .name("Emma")
            .externalId("222333444")
            .dateOfBirth(LocalDate.of(1987, 8, 20))
            .build();
        PersonEntity partner1 = PersonEntity.builder()
            .name("Liam")
            .externalId("555666777")
            .dateOfBirth(LocalDate.of(1986, 7, 10))
            .build();
        PersonEntity partner2 = PersonEntity.builder()
            .name("Noah")
            .externalId("888999000")
            .dateOfBirth(LocalDate.of(1985, 5, 5))
            .build();

        main = personRepository.save(main);
        partner1 = personRepository.save(partner1);
        partner2 = personRepository.save(partner2);

        PersonEntity child1 = personRepository.save(createChild("Child1", LocalDate.of(2008, 3, 3)));
        PersonEntity child2 = personRepository.save(createChild("Child2", LocalDate.of(2010, 4, 4)));
        PersonEntity child3 = personRepository.save(createChild("Child3", LocalDate.of(2012, 5, 5)));

        main.addRelationship(partner1, RelationshipType.PARTNER, RelationshipType.PARTNER);
        main.addRelationship(child1, RelationshipType.CHILD, RelationshipType.FATHER);
        main.addRelationship(child2, RelationshipType.CHILD, RelationshipType.FATHER);
        main.addRelationship(child3, RelationshipType.CHILD, RelationshipType.FATHER);

        // Different partners for children
        partner1.addRelationship(child1, RelationshipType.CHILD, RelationshipType.MOTHER);
        partner2.addRelationship(child2, RelationshipType.CHILD, RelationshipType.MOTHER);
        partner1.addRelationship(child3, RelationshipType.CHILD, RelationshipType.MOTHER);

        personRepository.saveAndFlush(main);
        personRepository.saveAndFlush(partner1);
        personRepository.saveAndFlush(partner2);

        SpecificPersonCheckRequest request = new SpecificPersonCheckRequest()
            .requestId("RQ888")
            .name("Emma")
            .id("222333444")
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
            .externalId("333444555")
            .dateOfBirth(LocalDate.of(1980, 9, 30))
            .build();
        PersonEntity partner = PersonEntity.builder()
            .name("Sophia")
            .externalId("666777888")
            .dateOfBirth(LocalDate.of(1982, 10, 25))
            .build();

        main = personRepository.save(main);
        partner = personRepository.save(partner);

        PersonEntity child1 = personRepository.save(createChild("AdultChild1", LocalDate.of(1995, 1, 1)));
        PersonEntity child2 = personRepository.save(createChild("AdultChild2", LocalDate.of(1994, 2, 2)));
        PersonEntity child3 = personRepository.save(createChild("AdultChild3", LocalDate.of(1993, 3, 3)));

        main.addRelationship(partner, RelationshipType.PARTNER, RelationshipType.PARTNER);
        main.addRelationship(child1, RelationshipType.CHILD, RelationshipType.FATHER);
        main.addRelationship(child2, RelationshipType.CHILD, RelationshipType.FATHER);
        main.addRelationship(child3, RelationshipType.CHILD, RelationshipType.FATHER);

        partner.addRelationship(child1, RelationshipType.CHILD, RelationshipType.MOTHER);
        partner.addRelationship(child2, RelationshipType.CHILD, RelationshipType.MOTHER);
        partner.addRelationship(child3, RelationshipType.CHILD, RelationshipType.MOTHER);

        personRepository.saveAndFlush(main);
        personRepository.saveAndFlush(partner);

        SpecificPersonCheckRequest request = new SpecificPersonCheckRequest()
            .requestId("RQ777")
            .name("Oliver")
            .id("333444555")
            .dateOfBirth(LocalDate.of(1980, 9, 30));

        mockMvc.perform(post("/v1/people/check-existing-person")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is(444))
            .andExpect(jsonPath("$.code", is("444")))
            .andExpect(jsonPath("$.message", is(PersonService.Constants.ErrorMsg.NO_SHARED_CHILDREN)))
            .andExpect(jsonPath("$.requestId", is("RQ777")));
    }


    @Test
    @DisplayName("Should return 200 when no external id but name, surname, and DOB match")
    void shouldReturn200_WhenNoExternalIdButNameSurnameDobMatch() throws Exception {
        PersonEntity main = PersonEntity.builder()
            .name("Lucas")
            .externalId("444555666")
            .dateOfBirth(LocalDate.of(1992, 11, 11))
            .build();

        PersonEntity partner = PersonEntity.builder()
            .name("Mia")
            .externalId("777888999")
            .dateOfBirth(LocalDate.of(1990, 12, 12))
            .build();

        PersonEntity child1 = personRepository.save(createChild("Child1", LocalDate.of(2010, 1, 1)));
        PersonEntity child2 = personRepository.save(createChild("Child2", LocalDate.of(2012, 2, 2)));
        PersonEntity child3 = personRepository.save(createChild("Child3", LocalDate.of(2014, 3, 3)));

        main = personRepository.save(main);
        partner = personRepository.save(partner);

        main.addRelationship(partner, RelationshipType.PARTNER, RelationshipType.PARTNER);
        main.addRelationship(child1, RelationshipType.CHILD, RelationshipType.FATHER);
        main.addRelationship(child2, RelationshipType.CHILD, RelationshipType.FATHER);
        main.addRelationship(child3, RelationshipType.CHILD, RelationshipType.FATHER);

        partner.addRelationship(child1, RelationshipType.CHILD, RelationshipType.MOTHER);
        partner.addRelationship(child2, RelationshipType.CHILD, RelationshipType.MOTHER);
        partner.addRelationship(child3, RelationshipType.CHILD, RelationshipType.MOTHER);

        personRepository.saveAndFlush(main);
        personRepository.saveAndFlush(partner);

        SpecificPersonCheckRequest request = new SpecificPersonCheckRequest()
            .requestId("RQ666")
            .name("Lucas")
            .dateOfBirth(LocalDate.of(1992, 11, 11));

        mockMvc.perform(post("/v1/people/check-existing-person")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
    }

    private PersonEntity createChild(String name, LocalDate dob) {
        return PersonEntity.builder()
            .name(name)
            .externalId(UUID.randomUUID().toString().replaceAll("[^0-9]", "").substring(0, 9))
            .dateOfBirth(dob)
            .build();
    }
}
