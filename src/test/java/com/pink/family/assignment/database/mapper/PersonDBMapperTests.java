package com.pink.family.assignment.database.mapper;

import com.pink.family.assignment.database.entity.PersonEntity;
import com.pink.family.assignment.database.entity.enums.RelationshipType;
import com.pink.family.assignment.dto.PersonDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PersonDBMapperTests {
    @Test
    @DisplayName("Should successfully map when two partners share a child")
    void tesMapDto_TwoParentsOneChild() {

        PersonEntity mainPerson = PersonEntity.builder()
            .internalId(1L)
            .name("Main")
            .externalId(987654321L)
            .dateOfBirth(LocalDate.of(1980, 1, 1))
            .build();

        PersonEntity partnerPerson = PersonEntity.builder()
            .internalId(2L)
            .name("Partner")
            .externalId(987654321L)
            .dateOfBirth(LocalDate.of(1982, 1, 1))
            .build();

        PersonEntity childPerson = PersonEntity.builder()
            .internalId(3L)
            .name("Child")
            .externalId(123456780L)
            .dateOfBirth(LocalDate.of(2000, 5, 15))
            .build();

        // link main and partner
        mainPerson.addRelationship(partnerPerson, RelationshipType.PARTNER, RelationshipType.PARTNER);

        // link main and child
        mainPerson.addRelationship(childPerson, RelationshipType.PARENT, RelationshipType.CHILD);

        // link partner and child
        partnerPerson.addRelationship(childPerson, RelationshipType.PARENT, RelationshipType.CHILD);

        PersonDto dto
            = PersonDbMapper.mapDto(
                mainPerson,
                Set.of(RelationshipType.PARENT, RelationshipType.PARTNER)
        );

        assertNotNull(dto);
        assertEquals(mainPerson.getInternalId(), dto.getInternalId());
        assertEquals(mainPerson.getName(), dto.getName());
        assertEquals(mainPerson.getExternalId(), dto.getExternalId());
        assertEquals(mainPerson.getDateOfBirth(), dto.getDateOfBirth());

        assertNotNull(dto.getRelations());
        assertEquals(1, dto.getRelations(RelationshipType.PARENT).size());
        assertEquals(1, dto.getRelations(RelationshipType.PARTNER).size());

        var child = dto.getRelations(RelationshipType.PARENT).stream().findFirst().orElseThrow();
        // Father/Main should not be added since we're mapping from the perspective of the main
        assertEquals(2, child.getRelations(RelationshipType.CHILD).size());

        var partner = dto.getRelations(RelationshipType.PARTNER).stream().findFirst().orElseThrow();
        // the partner shares a child so a child should be added
        assertEquals(1, partner.getRelations(RelationshipType.PARENT).size());
        // we don't expect the main to the added since we're mapping from the perspective of main
        assertEquals(1, partner.getRelations(RelationshipType.PARTNER).size());
    }

    @Test
    @DisplayName("Should successfully map even if there are no partners")
    void testMapDto_SinglePersonNoRelationships() {
        PersonEntity solo = PersonEntity.builder()
            .internalId(10L)
            .name("Solo")
            .externalId(111111111L)
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .build();

        PersonDto dto = PersonDbMapper.mapDto(solo);

        assertNotNull(dto);
        assertEquals(solo.getInternalId(), dto.getInternalId());
        assertEquals(solo.getName(), dto.getName());
        assertEquals(solo.getExternalId(), dto.getExternalId());
        assertEquals(solo.getDateOfBirth(), dto.getDateOfBirth());

        assertNotNull(dto.getRelations());
        for (RelationshipType type : RelationshipType.values()) {
            assertTrue(dto.getRelations(type).isEmpty());
        }
    }

    @Test
    @DisplayName("Should successfully map for multiple children")
    void testMapDto_MultipleChildren() {
        PersonEntity parent = PersonEntity.builder()
            .internalId(20L)
            .name("Parent")
            .externalId(222222222L)
            .dateOfBirth(LocalDate.of(1970, 6, 15))
            .build();

        PersonEntity child1 = PersonEntity.builder()
            .internalId(21L)
            .name("Child1")
            .externalId(333333333L)
            .dateOfBirth(LocalDate.of(2000, 3, 10))
            .build();

        PersonEntity child2 = PersonEntity.builder()
            .internalId(22L)
            .name("Child2")
            .externalId(444444444L)
            .dateOfBirth(LocalDate.of(2002, 7, 25))
            .build();

        // add a wife just to make sure it filters correctly.
        PersonEntity wife = PersonEntity.builder()
            .internalId(23L)
            .name("Wife")
            .externalId(222222222L)
            .dateOfBirth(LocalDate.of(1970, 6, 15))
            .build();

        parent.addRelationship(child1, RelationshipType.PARENT, RelationshipType.CHILD);
        parent.addRelationship(child2, RelationshipType.PARENT, RelationshipType.CHILD);
        parent.addRelationship(wife, RelationshipType.PARTNER, RelationshipType.PARTNER);

        // Use filter to include RelationshipType.PARENT because that’s the parent's relationship to children
        PersonDto dto = PersonDbMapper.mapDto(parent, Set.of(RelationshipType.PARENT));

        assertNotNull(dto);
        assertEquals(0, dto.getRelations(RelationshipType.PARTNER).size());
        assertEquals(0, dto.getRelations(RelationshipType.CHILD).size());
        assertEquals(2, dto.getRelations(RelationshipType.PARENT).size());
        assertTrue(
            dto.getRelations(RelationshipType.PARENT)
                .stream()
                .anyMatch(c -> c.getInternalId().equals(child1.getInternalId())));

        assertEquals(
            dto
                .getRelations(RelationshipType.PARENT)
                .stream()
                .findFirst()
                .orElseThrow()
                .getRelations(RelationshipType.PARENT)
                .size(),
            0);

        assertEquals(
            dto
                .getRelations(RelationshipType.PARENT)
                .stream()
                .findFirst()
                .orElseThrow()
                .getRelations(RelationshipType.PARENT)
                .size(),
            0);

        assertEquals(
            dto
                .getRelations(RelationshipType.PARENT)
                .stream()
                .findFirst()
                .orElseThrow()
                .getRelations(RelationshipType.PARTNER)
                .size(),
            0);

        assertEquals(0, dto.getRelations(RelationshipType.CHILD).size());
        assertEquals(2, dto.getRelations(RelationshipType.PARENT).size());

        assertTrue(
            dto.getRelations(
                RelationshipType.PARENT)
                    .stream()
                    .anyMatch(c -> c.getInternalId().equals(child2.getInternalId())));
    }


    @Test
    @DisplayName("Check that circular relationships are mapped successfully")
    void testMapDto_PartnerCircularRelationship() {
        PersonEntity personA = PersonEntity.builder()
            .internalId(30L)
            .name("Alice")
            .externalId(555555555L)
            .dateOfBirth(LocalDate.of(1985, 4, 20))
            .build();

        PersonEntity personB = PersonEntity.builder()
            .internalId(31L)
            .name("Bob")
            .externalId(666666666L)
            .dateOfBirth(LocalDate.of(1983, 12, 11))
            .build();

        personA.addRelationship(personB, RelationshipType.PARTNER, RelationshipType.PARTNER);

        PersonDto dto = PersonDbMapper.mapDto(personA, Set.of(RelationshipType.PARTNER));

        assertNotNull(dto);
        assertEquals(1, dto.getRelations(RelationshipType.PARTNER).size());
        assertEquals(0, dto.getRelations(RelationshipType.CHILD).size());
        assertEquals(0, dto.getRelations(RelationshipType.PARENT).size());

        PersonDto partner = dto.getRelations(RelationshipType.PARTNER).iterator().next();
        assertEquals(0, partner.getRelations(RelationshipType.CHILD).size());
        assertEquals(0, partner.getRelations(RelationshipType.PARENT).size());

        // The partner relationship back to the original person should be present
        assertEquals(1, partner.getRelations(RelationshipType.PARTNER).size());
        assertTrue(partner.getRelations(RelationshipType.PARTNER).stream()
            .anyMatch(p -> p.getInternalId().equals(personA.getInternalId())));
    }

    @Test
    @DisplayName("Should successfully map and return only the partners")
    void testMapDto_FilterOnlyPartners() {
        PersonEntity person = PersonEntity.builder()
            .internalId(40L)
            .name("Filter")
            .externalId(777777777L)
            .dateOfBirth(LocalDate.of(1995, 8, 8))
            .build();

        PersonEntity partner = PersonEntity.builder()
            .internalId(41L)
            .name("Partner")
            .externalId(888888888L)
            .dateOfBirth(LocalDate.of(1994, 9, 9))
            .build();

        PersonEntity child = PersonEntity.builder()
            .internalId(42L)
            .name("Child")
            .externalId(999999999L)
            .dateOfBirth(LocalDate.of(2018, 5, 5))
            .build();

        person.addRelationship(partner, RelationshipType.PARTNER, RelationshipType.PARTNER);
        person.addRelationship(child, RelationshipType.PARENT, RelationshipType.CHILD);

        PersonDto dto = PersonDbMapper.mapDto(person, Set.of(RelationshipType.PARTNER));

        assertNotNull(dto);
        assertEquals(1, dto.getRelations(RelationshipType.PARTNER).size());
        assertTrue(dto.getRelations(RelationshipType.PARTNER).stream()
            .anyMatch(p -> p.getInternalId().equals(partner.getInternalId())));

        // CHILD relationships should be filtered out
        assertTrue(dto.getRelations(RelationshipType.PARENT).isEmpty());
    }

    @Test
    @DisplayName("Should not include relationships not specified in the filter")
    void testMapDto_IgnoresUnfilteredRelationships() {
        PersonEntity main = PersonEntity.builder()
            .internalId(50L)
            .name("Main")
            .externalId(3000000001L)
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .build();

        PersonEntity child = PersonEntity.builder()
            .internalId(51L)
            .name("Child")
            .externalId(2000000002L)
            .dateOfBirth(LocalDate.of(2010, 2, 2))
            .build();

        PersonEntity partner = PersonEntity.builder()
            .internalId(52L)
            .name("Partner")
            .externalId(1000000003L)
            .dateOfBirth(LocalDate.of(1991, 3, 3))
            .build();

        // Establish both relationships
        main.addRelationship(child, RelationshipType.PARENT, RelationshipType.CHILD);
        main.addRelationship(partner, RelationshipType.PARTNER, RelationshipType.PARTNER);

        // Only PARTNER should be included in the result
        PersonDto dto = PersonDbMapper.mapDto(main, Set.of(RelationshipType.PARTNER));

        assertNotNull(dto);
        assertEquals(1, dto.getRelations(RelationshipType.PARTNER).size());

        // CHILD and FATHER relationships should not be included
        assertTrue(dto.getRelations(RelationshipType.PARENT).isEmpty());
        assertTrue(dto.getRelations(RelationshipType.CHILD).isEmpty());
    }


}