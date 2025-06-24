package com.pink.family.assignment.database.repository;

import com.pink.family.assignment.database.dao.PersonDao;
import com.pink.family.assignment.database.entity.PersonEntity;
import com.pink.family.assignment.database.entity.PersonRelationshipEntity;
import com.pink.family.assignment.database.entity.enums.RelationshipType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class PersonRepositoryTests {

    @Autowired
    private PersonRepository personRepository;

    private PersonEntity john;
    private PersonEntity jane;
    private PersonEntity child;

    @BeforeEach
    void setup() {
        // Clean DB before each test
        personRepository.deleteAll();

        john = PersonEntity.builder()
            .externalId(1001L)
            .name("John Doe")
            .dateOfBirth(LocalDate.of(1980, 5, 15))
            .build();

        jane = PersonEntity.builder()
            .externalId(1002L)
            .name("Jane Doe")
            .dateOfBirth(LocalDate.of(1982, 3, 20))
            .build();

        child = PersonEntity.builder()
            .externalId(2001L)
            .name("Child Doe")
            .dateOfBirth(LocalDate.of(2010, 7, 1))
            .build();

        // Save all
        personRepository.saveAll(List.of(john, jane, child));

        // Setup relationships
        john.addRelationship(jane, RelationshipType.PARTNER, RelationshipType.PARTNER);
        john.addRelationship(child, RelationshipType.PARENT, RelationshipType.CHILD);

        // Save all
        personRepository.saveAll(List.of(john, jane, child));
    }

    @Test
    void testFindByExternalId() {
        Optional<PersonEntity> found = personRepository.findByExternalId(1001L);
        assertThat(found).isNotEmpty();

        PersonEntity person = found.get();
        assertThat(person.getName()).isEqualTo("John Doe");
        assertThat(person.getRelationships()).isNotEmpty();
    }

    @Test
    void testFindAllByNameAndDateOfBirth() {
        Set<PersonEntity> found = personRepository.findAllByNameAndDateOfBirth("Jane Doe", LocalDate.of(1982, 3, 20));
        assertThat(found).hasSize(1);

        PersonEntity janeFound = found.iterator().next();
        assertThat(janeFound.getExternalId()).isEqualTo(1002L);
    }

    @Test
    void testFindByExternalIdIn() {
        Set<PersonEntity> foundSet = personRepository.findByExternalIdIn(Set.of(1001L, 2001L));
        assertThat(foundSet).hasSize(2);
    }
}
