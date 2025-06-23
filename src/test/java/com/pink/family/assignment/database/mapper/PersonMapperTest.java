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
class PersonMapperTest {
    @Test
    @DisplayName("Should successfully map when two partners share a child")
    void tesToDto_TwoParentsOneChild() {

        PersonEntity mainPerson = PersonEntity.builder()
            .id(1L)
            .name("Main")
            .externalId("987654321")
            .dateOfBirth(LocalDate.of(1980, 1, 1))
            .build();

        PersonEntity partnerPerson = PersonEntity.builder()
            .id(2L)
            .name("Partner")
            .externalId("987654321")
            .dateOfBirth(LocalDate.of(1982, 1, 1))
            .build();

        PersonEntity childPerson = PersonEntity.builder()
            .id(3L)
            .name("Child")
            .externalId("123456780")
            .dateOfBirth(LocalDate.of(2000, 5, 15))
            .build();

        // link main and partner
        mainPerson.addRelationship(partnerPerson, RelationshipType.PARTNER, RelationshipType.PARTNER);

        // link main and child
        mainPerson.addRelationship(childPerson, RelationshipType.FATHER, RelationshipType.CHILD);

        // link partner and child
        partnerPerson.addRelationship(childPerson, RelationshipType.MOTHER, RelationshipType.CHILD);

        PersonDto dto = PersonMapper.toDto(mainPerson,
            Set.of(RelationshipType.FATHER, RelationshipType.MOTHER, RelationshipType.PARTNER),
            null
        );

        assertNotNull(dto);
        assertEquals(mainPerson.getId(), dto.getId());
        assertEquals(mainPerson.getName(), dto.getName());
        assertEquals(mainPerson.getExternalId(), dto.getExternalId());
        assertEquals(mainPerson.getDateOfBirth(), dto.getDateOfBirth());

        assertNotNull(dto.getRelationships());
        assertEquals(1, dto.getRelationships(RelationshipType.FATHER).size());
        assertEquals(1, dto.getRelationships(RelationshipType.PARTNER).size());

        var child = dto.getRelationships().get(RelationshipType.FATHER).stream().findFirst().orElseThrow();
        // Father/Main should not be added since we're mapping from the perspective of the main
        assertEquals(2, child.getRelationships(RelationshipType.CHILD).size());

        var partner = dto.getRelationships(RelationshipType.PARTNER).stream().findFirst().orElseThrow();
        // the partner shares a child so a child should be added
        assertEquals(1, partner.getRelationships(RelationshipType.MOTHER).size());
        // we don't expect the main to the added since we're mapping from the perspective of main
        assertEquals(1, partner.getRelationships(RelationshipType.PARTNER).size());
    }

    @Test
    @DisplayName("Should successfully map even if there are no partners")
    void testToDto_SinglePersonNoRelationships() {
        PersonEntity solo = PersonEntity.builder()
            .id(10L)
            .name("Solo")
            .externalId("111111111")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .build();

        PersonDto dto = PersonMapper.toDto(solo, null, null);

        assertNotNull(dto);
        assertEquals(solo.getId(), dto.getId());
        assertEquals(solo.getName(), dto.getName());
        assertEquals(solo.getExternalId(), dto.getExternalId());
        assertEquals(solo.getDateOfBirth(), dto.getDateOfBirth());

        assertNotNull(dto.getRelationships());
        for (RelationshipType type : RelationshipType.values()) {
            assertTrue(dto.getRelationships(type).isEmpty());
        }
    }

    @Test
    @DisplayName("Should successfully map for multiple children")
    void testToDto_MultipleChildren() {
        PersonEntity parent = PersonEntity.builder()
            .id(20L)
            .name("Parent")
            .externalId("222222222")
            .dateOfBirth(LocalDate.of(1970, 6, 15))
            .build();

        PersonEntity child1 = PersonEntity.builder()
            .id(21L)
            .name("Child1")
            .externalId("333333333")
            .dateOfBirth(LocalDate.of(2000, 3, 10))
            .build();

        PersonEntity child2 = PersonEntity.builder()
            .id(22L)
            .name("Child2")
            .externalId("444444444")
            .dateOfBirth(LocalDate.of(2002, 7, 25))
            .build();

        // add a wife just to make sure it filters correctly.
        PersonEntity wife = PersonEntity.builder()
            .id(23L)
            .name("Wife")
            .externalId("222222222")
            .dateOfBirth(LocalDate.of(1970, 6, 15))
            .build();

        parent.addRelationship(child1, RelationshipType.FATHER, RelationshipType.CHILD);
        parent.addRelationship(child2, RelationshipType.FATHER, RelationshipType.CHILD);
        parent.addRelationship(wife, RelationshipType.PARTNER, RelationshipType.PARTNER);

        // Use filter to include RelationshipType.FATHER because that’s the parent's relationship to children
        PersonDto dto = PersonMapper.toDto(parent, Set.of(RelationshipType.FATHER), null);

        assertNotNull(dto);
        assertEquals(0, dto.getRelationships(RelationshipType.PARTNER).size());
        assertEquals(0, dto.getRelationships(RelationshipType.CHILD).size());
        assertEquals(0, dto.getRelationships(RelationshipType.MOTHER).size());

        assertEquals(2, dto.getRelationships(RelationshipType.FATHER).size());
        assertTrue(
            dto.getRelationships(RelationshipType.FATHER)
                .stream()
                .anyMatch(c -> c.getId().equals(child1.getId())));

        assertEquals(
            dto
                .getRelationships(RelationshipType.FATHER)
                .stream()
                .findFirst()
                .orElseThrow()
                .getRelationships(RelationshipType.FATHER)
                .size(),
            0);

        assertEquals(
            dto
                .getRelationships(RelationshipType.FATHER)
                .stream()
                .findFirst()
                .orElseThrow()
                .getRelationships(RelationshipType.MOTHER)
                .size(),
            0);

        assertEquals(
            dto
                .getRelationships(RelationshipType.FATHER)
                .stream()
                .findFirst()
                .orElseThrow()
                .getRelationships(RelationshipType.PARTNER)
                .size(),
            0);

        assertEquals(0, dto.getRelationships(RelationshipType.CHILD).size());
        assertEquals(0, dto.getRelationships(RelationshipType.MOTHER).size());

        assertTrue(
            dto.getRelationships(
                RelationshipType.FATHER)
                    .stream()
                    .anyMatch(c -> c.getId().equals(child2.getId())));
    }


    @Test
    @DisplayName("Check that circular relationships are mapped successfully")
    void testToDto_PartnerCircularRelationship() {
        PersonEntity personA = PersonEntity.builder()
            .id(30L)
            .name("Alice")
            .externalId("555555555")
            .dateOfBirth(LocalDate.of(1985, 4, 20))
            .build();

        PersonEntity personB = PersonEntity.builder()
            .id(31L)
            .name("Bob")
            .externalId("666666666")
            .dateOfBirth(LocalDate.of(1983, 12, 11))
            .build();

        personA.addRelationship(personB, RelationshipType.PARTNER, RelationshipType.PARTNER);

        PersonDto dto = PersonMapper.toDto(personA, Set.of(RelationshipType.PARTNER), null);

        assertNotNull(dto);
        assertEquals(1, dto.getRelationships(RelationshipType.PARTNER).size());
        assertEquals(0, dto.getRelationships(RelationshipType.CHILD).size());
        assertEquals(0, dto.getRelationships(RelationshipType.FATHER).size());
        assertEquals(0, dto.getRelationships(RelationshipType.MOTHER).size());

        PersonDto partner = dto.getRelationships(RelationshipType.PARTNER).iterator().next();
        assertEquals(0, partner.getRelationships(RelationshipType.CHILD).size());
        assertEquals(0, partner.getRelationships(RelationshipType.FATHER).size());
        assertEquals(0, partner.getRelationships(RelationshipType.MOTHER).size());

        // The partner relationship back to the original person should be present
        assertEquals(1, partner.getRelationships(RelationshipType.PARTNER).size());
        assertTrue(partner.getRelationships(RelationshipType.PARTNER).stream()
            .anyMatch(p -> p.getId().equals(personA.getId())));
    }

    @Test
    @DisplayName("Should successfully map and return only the partners")
    void testToDto_FilterOnlyPartners() {
        PersonEntity person = PersonEntity.builder()
            .id(40L)
            .name("Filter")
            .externalId("777777777")
            .dateOfBirth(LocalDate.of(1995, 8, 8))
            .build();

        PersonEntity partner = PersonEntity.builder()
            .id(41L)
            .name("Partner")
            .externalId("888888888")
            .dateOfBirth(LocalDate.of(1994, 9, 9))
            .build();

        PersonEntity child = PersonEntity.builder()
            .id(42L)
            .name("Child")
            .externalId("999999999")
            .dateOfBirth(LocalDate.of(2018, 5, 5))
            .build();

        person.addRelationship(partner, RelationshipType.PARTNER, RelationshipType.PARTNER);
        person.addRelationship(child, RelationshipType.FATHER, RelationshipType.CHILD);

        PersonDto dto = PersonMapper.toDto(person, Set.of(RelationshipType.PARTNER), null);

        assertNotNull(dto);
        assertEquals(1, dto.getRelationships(RelationshipType.PARTNER).size());
        assertTrue(dto.getRelationships(RelationshipType.PARTNER).stream()
            .anyMatch(p -> p.getId().equals(partner.getId())));

        // CHILD relationships should be filtered out
        assertTrue(dto.getRelationships(RelationshipType.FATHER).isEmpty());
    }

    @Test
    @DisplayName("Should not include relationships not specified in the filter")
    void testToDto_IgnoresUnfilteredRelationships() {
        PersonEntity main = PersonEntity.builder()
            .id(50L)
            .name("Main")
            .externalId("000000001")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .build();

        PersonEntity child = PersonEntity.builder()
            .id(51L)
            .name("Child")
            .externalId("000000002")
            .dateOfBirth(LocalDate.of(2010, 2, 2))
            .build();

        PersonEntity partner = PersonEntity.builder()
            .id(52L)
            .name("Partner")
            .externalId("000000003")
            .dateOfBirth(LocalDate.of(1991, 3, 3))
            .build();

        // Establish both relationships
        main.addRelationship(child, RelationshipType.FATHER, RelationshipType.CHILD);
        main.addRelationship(partner, RelationshipType.PARTNER, RelationshipType.PARTNER);

        // Only PARTNER should be included in the result
        PersonDto dto = PersonMapper.toDto(main, Set.of(RelationshipType.PARTNER), null);

        assertNotNull(dto);
        assertEquals(1, dto.getRelationships(RelationshipType.PARTNER).size());

        // CHILD and FATHER relationships should not be included
        assertTrue(dto.getRelationships(RelationshipType.FATHER).isEmpty());
        assertTrue(dto.getRelationships(RelationshipType.CHILD).isEmpty());
        assertTrue(dto.getRelationships(RelationshipType.MOTHER).isEmpty());
    }


}