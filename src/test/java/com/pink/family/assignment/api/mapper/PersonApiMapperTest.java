package com.pink.family.assignment.api.mapper;

import com.pink.family.api.rest.server.model.FullPerson;
import com.pink.family.api.rest.server.model.PersonDetailsRequest;
import com.pink.family.api.rest.server.model.Relation;
import com.pink.family.assignment.database.entity.enums.RelationshipType;
import com.pink.family.assignment.dto.PersonDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PersonApiMapperTest {

    private static PersonDto basePerson(Long id, String name) {
        return PersonDto.builder()
            .externalId(id)
            .internalId(id)
            .name(name)
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .build();
    }

    @Test
    @DisplayName("Should return null when input DTO is null")
    void testNullInputReturnsNull() {
        assertNull(PersonApiMapper.mapToApi(null, new PersonDetailsRequest()));
    }

    @Test
    @DisplayName("Should map minimal person with no relationships")
    void testMinimalPersonNoRelations() {
        PersonDto dto = basePerson(1L, "John Doe");
        FullPerson full = PersonApiMapper.mapToApi(dto, new PersonDetailsRequest());

        assertNotNull(full);
        assertEquals(1L, full.getId());
        assertEquals("John Doe", full.getName());
        assertEquals(LocalDate.of(1990, 1, 1), full.getBirthDate());
        assertNull(full.getParent1());
        assertNull(full.getParent2());
        assertNull(full.getPartner());
        assertTrue(full.getChildren().isEmpty());
    }

    @Test
    @DisplayName("Should map parent1 correctly when match found in CHILD relationships")
    void testParentMatchingFromChildRelation() {
        PersonDto parent1 = basePerson(2L, "Parent One");
        PersonDto child = basePerson(1L, "Child");
        child.addRelationship(RelationshipType.CHILD, parent1);

        Relation parentRel = new Relation().id(2L);
        PersonDetailsRequest request = new PersonDetailsRequest().parent1(parentRel);

        FullPerson result = PersonApiMapper.mapToApi(child, request);

        assertNotNull(result.getParent1());
        assertEquals(2L, result.getParent1().getId());
        assertNull(result.getParent2());
    }

    @Test
    @DisplayName("Should map children correctly from PARENT relationships")
    void testChildrenMatchingFromParentRelation() {
        PersonDto child1 = basePerson(2L, "Child One");
        PersonDto parent = basePerson(1L, "Parent");
        parent.addRelationship(RelationshipType.PARENT, child1);

        Relation childRel = new Relation().id(2L);
        PersonDetailsRequest request = new PersonDetailsRequest().children(java.util.List.of(childRel));

        FullPerson result = PersonApiMapper.mapToApi(parent, request);

        assertEquals(1, result.getChildren().size());
        assertEquals(2L, result.getChildren().getFirst().getId());
    }

    @Test
    @DisplayName("Should map partner correctly from PARTNER relationships")
    void testPartnerMatching() {
        PersonDto partner = basePerson(2L, "Partner");
        PersonDto person = basePerson(1L, "Person");
        person.addRelationship(RelationshipType.PARTNER, partner);

        Relation partnerRel = new Relation().id(2L);
        PersonDetailsRequest request = new PersonDetailsRequest().partner(partnerRel);

        FullPerson result = PersonApiMapper.mapToApi(person, request);

        assertNotNull(result.getPartner());
        assertEquals(2L, result.getPartner().getId());
    }

    @Test
    @DisplayName("Should not map relationships when no matching IDs found")
    void testNoMatchOnWrongIds() {
        PersonDto person = basePerson(1L, "Person");
        person.addRelationship(RelationshipType.PARENT, basePerson(3L, "Child"));

        PersonDetailsRequest request = new PersonDetailsRequest()
            .parent1(new Relation().id(100L))
            .children(java.util.List.of(new Relation().id(999L)))
            .partner(new Relation().id(888L));

        FullPerson result = PersonApiMapper.mapToApi(person, request);

        assertNull(result.getParent1());
        assertNull(result.getPartner());
        assertTrue(result.getChildren().isEmpty());
    }

    @Test
    @DisplayName("Should handle empty DTO relationships gracefully")
    void testHandlesEmptyRelationsGracefully() {
        PersonDto person = basePerson(1L, "Empty");
        PersonDetailsRequest request = new PersonDetailsRequest()
            .parent1(new Relation().id(2L))
            .parent2(new Relation().id(3L))
            .children(java.util.List.of(new Relation().id(4L)))
            .partner(new Relation().id(5L));

        FullPerson result = PersonApiMapper.mapToApi(person, request);

        assertNull(result.getParent1());
        assertNull(result.getParent2());
        assertNull(result.getPartner());
        assertTrue(result.getChildren().isEmpty());
    }
}
