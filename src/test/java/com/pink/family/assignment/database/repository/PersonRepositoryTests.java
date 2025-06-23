package com.pink.family.assignment.database.repository;

import com.pink.family.assignment.database.entity.PersonEntity;
import com.pink.family.assignment.database.entity.PersonRelationshipEntity;
import com.pink.family.assignment.database.entity.id.PersonRelationshipId;
import com.pink.family.assignment.database.entity.enums.RelationshipType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class PersonRepositoryTests {

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private PersonRelationshipRepository relationshipRepository;

    @Test
    void testCreateAndReadPerson() {
        PersonEntity person = PersonEntity.builder()
            .name("John")
            .externalId(123456789L)
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .build();

        PersonEntity saved = personRepository.save(person);

        Optional<PersonEntity> found = personRepository.findById(saved.getInternalId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("John");
    }

    @Test
    void testUpdatePerson() {
        PersonEntity person = PersonEntity.builder()
            .name("Jane")
            .externalId(987654321L)
            .dateOfBirth(LocalDate.of(1985, 5, 15))
            .build();

        PersonEntity saved = personRepository.save(person);

        saved.setName("Janet");
        PersonEntity updated = personRepository.save(saved);

        Optional<PersonEntity> found = personRepository.findById(updated.getInternalId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Janet");
    }

    @Test
    @DisplayName("Test that a PersonEntity can be successfully deleted")
    void testDeletePerson() {
        PersonEntity person = PersonEntity.builder()
            .name("Mark")
            .externalId(111222333L)
            .dateOfBirth(LocalDate.of(1970, 12, 25))
            .build();

        PersonEntity saved = personRepository.save(person);
        Long id = saved.getInternalId();

        personRepository.delete(saved);
        Optional<PersonEntity> found = personRepository.findById(id);
        assertThat(found).isNotPresent();
    }

    @Test
    @DisplayName("Test that a PersonEntity can be successfully deleted if they have a relationship")
    void testDeletePersonWithRelationship() {
        PersonEntity p1 = PersonEntity.builder()
            .name("Alice")
            .externalId(555555555L)
            .dateOfBirth(LocalDate.of(1995, 6, 10))
            .build();

        PersonEntity p2 = PersonEntity.builder()
            .name("Bob")
            .externalId(666666666L)
            .dateOfBirth(LocalDate.of(1993, 8, 20))
            .build();

        p1 = personRepository.save(p1);
        p2 = personRepository.save(p2);

        p1.addRelationship(p2, RelationshipType.PARTNER, RelationshipType.PARTNER);
        p1 = personRepository.save(p1);
        p2 = personRepository.save(p2);

        // Manually remove inverse relationship from p2 because hibernate doesn't do so automatically
        // even if you have the right annotations.
        PersonEntity finalP = p1;
        p2.getRelationships().removeIf(r -> r.getRelatedPerson().getInternalId().equals(finalP.getInternalId()));
        personRepository.save(p2); // Persist the cleaned-up p2

        Long id = p1.getInternalId();
        personRepository.delete(p1);

        assertThat(personRepository.findById(id)).isNotPresent();

        // Verify p2 still exists and has no relationships
        Optional<PersonEntity> found = personRepository.findById(p2.getInternalId());
        assertThat(found).isPresent();
        assertThat(found.get().getRelationships()).isEmpty();
    }


    @Test
    @DisplayName("Test that a relationship can be successfully added to a PersonEntity")
    void testAddRelationship() {
        PersonEntity p1 = PersonEntity.builder()
            .name("Alice")
            .externalId(555555555L)
            .dateOfBirth(LocalDate.of(1995, 6, 10))
            .build();

        PersonEntity p2 = PersonEntity.builder()
            .name("Bob")
            .externalId(666666666L)
            .dateOfBirth(LocalDate.of(1993, 8, 20))
            .build();

        p1 = personRepository.save(p1);
        p2 = personRepository.save(p2);

        // Add relationship p1 -> p2
        p1.addRelationship(p2, RelationshipType.PARTNER, RelationshipType.PARTNER);
        personRepository.save(p1);
        p2 = personRepository.save(p2);

        PersonRelationshipId relId = new PersonRelationshipId(p1.getInternalId(), p2.getInternalId());
        Optional<PersonRelationshipEntity> relOpt = relationshipRepository.findById(relId);

        assertThat(relOpt).isPresent();
        PersonRelationshipEntity rel = relOpt.get();
        assertThat(rel.getPerson().getInternalId()).isEqualTo(p1.getInternalId());
        assertThat(rel.getRelatedPerson().getInternalId()).isEqualTo(p2.getInternalId());
        assertThat(rel.getRelationshipType()).isEqualTo(RelationshipType.PARTNER);
    }

    @Test
    @DisplayName("Test that the relationship between Persons can be successfully deleted.")
    void testDeleteRelationship() {
        PersonEntity p1 = PersonEntity.builder()
            .name("Charlie")
            .externalId(777777777L)
            .dateOfBirth(LocalDate.of(1980, 3, 3))
            .build();

        PersonEntity p2 = PersonEntity.builder()
            .name("Diana")
            .externalId(888888888L)
            .dateOfBirth(LocalDate.of(1982, 7, 7))
            .build();

        p1 = personRepository.save(p1);
        p2 = personRepository.save(p2);

        p1.addRelationship(p2, RelationshipType.PARENT, RelationshipType.CHILD);
        personRepository.save(p1);

        PersonRelationshipId relId = new PersonRelationshipId(p1.getInternalId(), p2.getInternalId());

        // Remove the relationship
        relationshipRepository.deleteById(relId);

        assertThat(relationshipRepository.findById(relId)).isNotPresent();
    }

    @Test
    @DisplayName("Should return matching person by name, surname and dob")
    void findAllByNameAndDateOfBirth_returnsMatch() {
        // given
        PersonEntity person = PersonEntity.builder()
            .name("John")
            .externalId(111222333L)
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .build();
        personRepository.save(person);

        Set<PersonEntity> result = personRepository.findAllByNameAndDateOfBirth("John", LocalDate.of(1990, 1, 1));

        assertThat(result).hasSize(1);
        assertThat(result.stream().findFirst().orElseThrow().getExternalId()).isEqualTo(111222333L);
    }

    @Test
    @DisplayName("Should return empty when name or surname does not match")
    void findAllByNameAndDateOfBirth_returnsEmptyIfMismatch() {
        // given
        PersonEntity person = PersonEntity.builder()
            .name("Alice")
            .externalId(555555555L)
            .dateOfBirth(LocalDate.of(1985, 5, 5))
            .build();
        personRepository.save(person);

        Set<PersonEntity> result =
            personRepository.findAllByNameAndDateOfBirth(
                "Wrong",
                LocalDate.of(1985, 5, 5));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return multiple people with same name, surname, and dob")
    void findAllByNameAndDateOfBirth_returnsMultiple() {
        // given
        personRepository.save(PersonEntity.builder()
            .name("Emma")
            .externalId(888111999L)
            .dateOfBirth(LocalDate.of(2000, 3, 10))
            .build());

        personRepository.save(PersonEntity.builder()
            .name("Emma")
            .externalId(888111998L)
            .dateOfBirth(LocalDate.of(2000, 3, 10))
            .build());

        Set<PersonEntity> result =
            personRepository.findAllByNameAndDateOfBirth(
                "Emma",
                LocalDate.of(2000, 3, 10));

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(PersonEntity::getExternalId)
            .containsExactlyInAnyOrder(888111999L, 888111998L);
    }
}
