package com.pink.family.assignment.database.repository;

import com.pink.family.assignment.database.entity.Person;
import com.pink.family.assignment.database.entity.PersonRelationship;
import com.pink.family.assignment.database.entity.id.PersonRelationshipId;
import com.pink.family.assignment.database.entity.enums.RelationshipType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class PersonRepositoryTest {

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private PersonRelationshipRepository relationshipRepository;

    @Test
    void testCreateAndReadPerson() {
        Person person = Person.builder()
            .name("John")
            .surname("Doe")
            .bsn("123456789")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .build();

        Person saved = personRepository.save(person);

        Optional<Person> found = personRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("John");
    }

    @Test
    void testUpdatePerson() {
        Person person = Person.builder()
            .name("Jane")
            .surname("Doe")
            .bsn("987654321")
            .dateOfBirth(LocalDate.of(1985, 5, 15))
            .build();

        Person saved = personRepository.save(person);

        saved.setName("Janet");
        Person updated = personRepository.save(saved);

        Optional<Person> found = personRepository.findById(updated.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Janet");
    }

    @Test
    void testDeletePerson() {
        Person person = Person.builder()
            .name("Mark")
            .surname("Smith")
            .bsn("111222333")
            .dateOfBirth(LocalDate.of(1970, 12, 25))
            .build();

        Person saved = personRepository.save(person);
        Long id = saved.getId();

        personRepository.delete(saved);
        Optional<Person> found = personRepository.findById(id);
        assertThat(found).isNotPresent();
    }

    @Test
    void testAddRelationship() {
        Person p1 = Person.builder()
            .name("Alice")
            .surname("Wonderland")
            .bsn("555555555")
            .dateOfBirth(LocalDate.of(1995, 6, 10))
            .build();

        Person p2 = Person.builder()
            .name("Bob")
            .surname("Builder")
            .bsn("666666666")
            .dateOfBirth(LocalDate.of(1993, 8, 20))
            .build();

        p1 = personRepository.save(p1);
        p2 = personRepository.save(p2);

        // Add relationship p1 -> p2
        p1.addRelationship(p2, RelationshipType.PARTNER);  // or whatever enum value you have
        personRepository.save(p1);

        PersonRelationshipId relId = new PersonRelationshipId(p1.getId(), p2.getId());
        Optional<PersonRelationship> relOpt = relationshipRepository.findById(relId);

        assertThat(relOpt).isPresent();
        PersonRelationship rel = relOpt.get();
        assertThat(rel.getPerson().getId()).isEqualTo(p1.getId());
        assertThat(rel.getRelatedPerson().getId()).isEqualTo(p2.getId());
        assertThat(rel.getRelationshipType()).isEqualTo(RelationshipType.PARTNER);
    }

    @Test
    void testDeleteRelationship() {
        Person p1 = Person.builder()
            .name("Charlie")
            .surname("Chaplin")
            .bsn("777777777")
            .dateOfBirth(LocalDate.of(1980, 3, 3))
            .build();

        Person p2 = Person.builder()
            .name("Diana")
            .surname("Prince")
            .bsn("888888888")
            .dateOfBirth(LocalDate.of(1982, 7, 7))
            .build();

        p1 = personRepository.save(p1);
        p2 = personRepository.save(p2);

        p1.addRelationship(p2, RelationshipType.PARENT);
        personRepository.save(p1);

        PersonRelationshipId relId = new PersonRelationshipId(p1.getId(), p2.getId());

        // Remove the relationship
        relationshipRepository.deleteById(relId);

        assertThat(relationshipRepository.findById(relId)).isNotPresent();
    }
}
