package com.pink.family.assignment.database.dao;

import com.pink.family.assignment.database.entity.PersonEntity;
import com.pink.family.assignment.database.entity.PersonRelationshipEntity;
import com.pink.family.assignment.database.entity.id.PersonRelationshipId;
import com.pink.family.assignment.database.entity.enums.RelationshipType;

import com.pink.family.assignment.dto.PersonDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class PersonDaoTests {

    @Autowired
    private PersonDao personDao;

    @Autowired
    private PersonRelationshipDao relationshipDao;

    @Test
    void testCreateAndReadPerson() {
        PersonEntity person = PersonEntity.builder()
            .name("John")
            .externalId(123456789L)
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .build();

        PersonEntity saved = personDao.save(person);

        Optional<PersonEntity> found = personDao.findById(saved.getInternalId());
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

        PersonEntity saved = personDao.save(person);

        saved.setName("Janet");
        PersonEntity updated = personDao.save(saved);

        Optional<PersonEntity> found = personDao.findById(updated.getInternalId());
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

        PersonEntity saved = personDao.save(person);
        Long id = saved.getInternalId();

        personDao.delete(saved);
        Optional<PersonEntity> found = personDao.findById(id);
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

        p1 = personDao.save(p1);
        p2 = personDao.save(p2);

        p1.addRelationship(p2, RelationshipType.PARTNER, RelationshipType.PARTNER);
        p1 = personDao.save(p1);
        p2 = personDao.save(p2);

        // Manually remove inverse relationship from p2 because hibernate doesn't do so automatically
        // even if you have the right annotations.
        PersonEntity finalP = p1;
        p2.getRelationships().removeIf(r -> r.getRelatedPerson().getInternalId().equals(finalP.getInternalId()));
        personDao.save(p2); // Persist the cleaned-up p2

        Long id = p1.getInternalId();
        personDao.delete(p1);

        assertThat(personDao.findById(id)).isNotPresent();

        // Verify p2 still exists and has no relationships
        Optional<PersonEntity> found = personDao.findById(p2.getInternalId());
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

        p1 = personDao.save(p1);
        p2 = personDao.save(p2);

        // Add relationship p1 -> p2
        p1.addRelationship(p2, RelationshipType.PARTNER, RelationshipType.PARTNER);
        personDao.save(p1);
        p2 = personDao.save(p2);

        PersonRelationshipId relId = new PersonRelationshipId(p1.getInternalId(), p2.getInternalId(), RelationshipType.PARTNER);
        Optional<PersonRelationshipEntity> relOpt = relationshipDao.findById(relId);

        assertThat(relOpt).isPresent();
        PersonRelationshipEntity rel = relOpt.get();
        assertThat(rel.getPerson().getInternalId()).isEqualTo(p1.getInternalId());
        assertThat(rel.getRelatedPerson().getInternalId()).isEqualTo(p2.getInternalId());
        assertThat(rel.getId().getRelationshipType()).isEqualTo(RelationshipType.PARTNER);
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

        p1 = personDao.save(p1);
        p2 = personDao.save(p2);

        p1.addRelationship(p2, RelationshipType.PARENT, RelationshipType.CHILD);
        personDao.save(p1);

        PersonRelationshipId relId = new PersonRelationshipId(p1.getInternalId(), p2.getInternalId(), RelationshipType.PARENT);

        // Remove the relationship
        relationshipDao.deleteById(relId);

        assertThat(relationshipDao.findById(relId)).isNotPresent();
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
        personDao.save(person);

        Set<PersonEntity> result = personDao.findAllByNameAndDateOfBirth("John", LocalDate.of(1990, 1, 1));

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
        personDao.save(person);

        Set<PersonEntity> result =
            personDao.findAllByNameAndDateOfBirth(
                "Wrong",
                LocalDate.of(1985, 5, 5)
            );

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return multiple people with same name, surname, and dob")
    void findAllByNameAndDateOfBirth_returnsMultiple() {
        // given
        personDao.save(PersonEntity.builder()
            .name("Emma")
            .externalId(888111999L)
            .dateOfBirth(LocalDate.of(2000, 3, 10))
            .build());

        personDao.save(PersonEntity.builder()
            .name("Emma")
            .externalId(888111998L)
            .dateOfBirth(LocalDate.of(2000, 3, 10))
            .build());

        Set<PersonEntity> result =
            personDao.findAllByNameAndDateOfBirth(
                "Emma",
                LocalDate.of(2000, 3, 10)
            );

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(PersonEntity::getExternalId)
            .containsExactlyInAnyOrder(888111999L, 888111998L);
    }

    @Nested
    class UpdatePerson {

        @Test
        @DisplayName("Should create person with parents, partners and children relationships")
        @Transactional
        void shouldCreatePersonWithRelationships() {
            // Given
            Long mainId = 100L+ System.nanoTime() % 100000;
            Long parentId = 200L+ System.nanoTime() % 100000;
            Long childId = 300L+ System.nanoTime() % 100000;
            Long partnerId = 400L+ System.nanoTime() % 100000;

            PersonDto result = personDao.updatePerson(
                mainId,
                "John Doe",
                LocalDate.of(1990, 1, 1),
                Map.of(
                    RelationshipType.CHILD, Set.of(parentId),
                    RelationshipType.PARTNER, Set.of(partnerId),
                    RelationshipType.PARENT, Set.of(childId)
                )
            );

            assertThat(result).isNotNull();
            assertThat(result.getExternalId()).isEqualTo(mainId);
            assertThat(result.getRelations(RelationshipType.CHILD)).hasSize(1);
            assertThat(result.getRelations(RelationshipType.PARTNER)).hasSize(1);
            assertThat(result.getRelations(RelationshipType.PARENT)).hasSize(1);

            assertThat(result.getRelations(RelationshipType.CHILD).iterator().next().getExternalId())
                .isEqualTo(parentId);
            assertThat(result.getRelations(RelationshipType.PARENT).iterator().next().getExternalId())
                .isEqualTo(childId);
        }

        @Test
        public void testUpdatePerson_WithOneChild_ShouldCreateChildRelationship() {
            // Given: create and save the child person
            PersonEntity child = new PersonEntity();
            child.setExternalId(201L);
            child.setName("Child Person");
            child.setDateOfBirth(LocalDate.of(2012, 3, 15));
            personDao.save(child);

            // When: call updatePerson with 1 child relationship
            Long parentExternalId = 200L+ System.nanoTime() % 100000;
            Map<RelationshipType, Set<Long>> relatedIdsByType = Map.of(
                RelationshipType.CHILD, Set.of(201L+ System.nanoTime() % 100000)
            );

            PersonDto result = personDao.updatePerson(
                parentExternalId,
                "Parent Person",
                LocalDate.of(1980, 5, 5),
                relatedIdsByType
            );

            // Then: fetch parent and assert relationships
            PersonEntity parent = personDao.findByExternalIdEntity(parentExternalId).get();

            assertThat("Parent Person").isEqualTo(parent.getName());
            assertThat(LocalDate.of(1980, 5, 5)).isEqualTo(parent.getDateOfBirth());

            // Validate the relationship
            Set<PersonRelationshipEntity> relationships = parent.getRelationships();
            assertThat(relationships).hasSize(1);

            PersonRelationshipEntity rel = relationships.iterator().next();
            assertThat(RelationshipType.CHILD).isEqualTo(rel.getRelationshipType());
            assertThat(child.getInternalId()).isEqualTo(rel.getRelatedPerson().getInternalId());
        }

        @Test
        @DisplayName("Should update name and birthDate")
        @Transactional
        void shouldUpdatePersonNameAndDob() {
            // Create person first
            Long externalId = 555L+ System.nanoTime() % 100000;
            personDao.updatePerson(
                externalId,
                "Initial", LocalDate.of(2000, 1, 1), Map.of());

            // Update it
            PersonDto updated = personDao.updatePerson(
                externalId,
                "Updated Name", LocalDate.of(1995, 5, 5), Map.of());

            assertThat(updated).isNotNull();
            assertThat(updated.getName()).isEqualTo("Updated Name");
            assertThat(updated.getDateOfBirth()).isEqualTo(LocalDate.of(1995, 5, 5));
        }


    }
}
