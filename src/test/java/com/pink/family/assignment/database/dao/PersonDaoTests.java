package com.pink.family.assignment.database.dao;

import com.pink.family.assignment.database.entity.PersonEntity;
import com.pink.family.assignment.database.entity.enums.RelationshipType;
import com.pink.family.assignment.database.repository.PersonRelationshipRepository;
import com.pink.family.assignment.database.repository.PersonRepository;
import com.pink.family.assignment.dto.PersonDto;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional
public class PersonDaoTests {

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private PersonRelationshipRepository personRelationshipRepository;

    @Autowired
    private PersonDao personDao;

    @Autowired
    private EntityManager entityManager;

    @Nested
    class FindPersonTests {

        @Test
        void testFindPersonFromExternalIdWithPartnerChildren() {
            // Create entities
            PersonEntity mainPerson = PersonEntity.builder()
                .externalId(1L)
                .name("John")
                .dateOfBirth(LocalDate.of(1980, 1, 1))
                .build();
            PersonEntity child = PersonEntity.builder()
                .externalId(2L)
                .name("Child")
                .dateOfBirth(LocalDate.of(2010, 5, 12))
                .build();
            PersonEntity partner = PersonEntity.builder()
                .externalId(3L)
                .name("Wife")
                .dateOfBirth(LocalDate.of(1982, 3, 22))
                .build();

            // Save all persons first (assigns DB IDs)
            mainPerson = personRepository.saveAndFlush(mainPerson);
            child = personRepository.saveAndFlush(child);
            partner = personRepository.saveAndFlush(partner);


            // Add relationships *to the mainPerson*
            mainPerson.addRelationship(child, RelationshipType.PARENT, RelationshipType.CHILD);
            mainPerson.addRelationship(partner, RelationshipType.PARTNER, RelationshipType.PARTNER);

            // Save mainPerson and flush to persist relationships
            mainPerson = personRepository.saveAndFlush(mainPerson);
            personRepository.saveAndFlush(child);
            personRepository.saveAndFlush(partner);

            // Save relationships explicitly
            personRelationshipRepository.saveAllAndFlush(mainPerson.getRelationships());

            Optional<PersonDto> result = personDao.findPersonFromExternalIdWithPartnerChildren(mainPerson.getExternalId());

            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo(mainPerson.getName());

            log.debug("Asserting: " + result);
            // Validate relationships count and type
            assertThat(result.get().getRelations()).hasSize(2);
            assertThat(result.get().getRelations(RelationshipType.PARENT)).hasSize(1);
            assertThat(result.get().getRelations(RelationshipType.PARTNER)).hasSize(1);
        }

    }

    @Nested
    class UpdatePersonTests {

        @Test
        void testUpdatePersonRelationships() {
            PersonEntity oldParent = PersonEntity.builder().externalId(10L).name("Anna").build();
            PersonEntity oldPartner = PersonEntity.builder().externalId(11L).name("Tom").build();
            PersonEntity child = PersonEntity.builder().externalId(12L).name("Sally").build();

            personRepository.saveAll(List.of(oldParent, oldPartner, child));
            entityManager.flush();
            entityManager.clear();

            PersonDto updated = personDao.updatePerson(
                99L,
                "New Person",
                LocalDate.of(1995, 6, 15),
                Set.of(10L),   // parents
                Set.of(11L),   // partners
                Set.of(12L)    // children
            );

            assertThat(updated).isNotNull();
            assertThat(updated.getName()).isEqualTo("New Person");
            assertThat(updated.getRelations(RelationshipType.PARENT)).hasSize(1);
            assertThat(updated.getRelations(RelationshipType.CHILD)).hasSize(1);
            assertThat(updated.getRelations(RelationshipType.PARTNER)).hasSize(1);
        }

        @Test
        void testUpdateWithOnlyChildren() {
            PersonEntity child1 = PersonEntity.builder().externalId(5L).name("Child1").build();
            PersonEntity child2 = PersonEntity.builder().externalId(6L).name("Child2").build();

            personRepository.saveAll(List.of(child1, child2));
            entityManager.flush();
            entityManager.clear();

            PersonDto updated = personDao.updatePerson(
                101L,
                "ParentOnly",
                LocalDate.of(1988, 3, 3),
                Set.of(),
                Set.of(),
                Set.of(5L, 6L)
            );

            assertThat(updated).isNotNull();
            assertThat(updated.getName()).isEqualTo("ParentOnly");
            assertThat(updated.getRelations(RelationshipType.CHILD)).hasSize(2);
            assertThat(updated.getRelations(RelationshipType.PARENT)).isNullOrEmpty();
            assertThat(updated.getRelations(RelationshipType.PARTNER)).isNullOrEmpty();
        }

        @Test
        void testUpdateWithOnlyParents() {
            PersonEntity parent1 = PersonEntity.builder().externalId(20L).name("Parent1").build();
            PersonEntity parent2 = PersonEntity.builder().externalId(21L).name("Parent2").build();

            personRepository.saveAll(List.of(parent1, parent2));
            entityManager.flush();
            entityManager.clear();

            PersonDto updated = personDao.updatePerson(
                102L,
                "ChildOnly",
                LocalDate.of(2000, 1, 1),
                Set.of(20L, 21L),
                Set.of(),
                Set.of()
            );

            assertThat(updated).isNotNull();
            assertThat(updated.getName()).isEqualTo("ChildOnly");
            assertThat(updated.getRelations(RelationshipType.PARENT)).hasSize(2);
            assertThat(updated.getRelations(RelationshipType.CHILD)).isNullOrEmpty();
            assertThat(updated.getRelations(RelationshipType.PARTNER)).isNullOrEmpty();
        }

        @Test
        void testUpdateWithNoRelationships() {
            PersonDto updated = personDao.updatePerson(
                103L,
                "Lonely Person",
                LocalDate.of(1990, 7, 7),
                Set.of(),
                Set.of(),
                Set.of()
            );

            assertThat(updated).isNotNull();
            assertThat(updated.getName()).isEqualTo("Lonely Person");
            assertThat(updated.getRelations(RelationshipType.PARENT)).isNullOrEmpty();
            assertThat(updated.getRelations(RelationshipType.CHILD)).isNullOrEmpty();
            assertThat(updated.getRelations(RelationshipType.PARTNER)).isNullOrEmpty();
        }
    }
}
