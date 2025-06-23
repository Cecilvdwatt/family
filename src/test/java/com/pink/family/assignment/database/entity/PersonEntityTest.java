package com.pink.family.assignment.database.entity;

import com.pink.family.assignment.database.entity.enums.RelationshipType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PersonEntityTest {

    @Test
    void testAddOneRelationship() {
        PersonEntity p1 = new PersonEntity(1L, 100L, "Person1", LocalDate.of(1980, 1, 1));
        PersonEntity p2 = new PersonEntity(2L, 200L, "Person2", LocalDate.of(1985, 2, 2));

        p1.addRelationship(p2, RelationshipType.PARENT, RelationshipType.CHILD);

        assertEquals(1, p1.getRelationships().size());
        assertEquals(1, p2.getRelationships().size());

        var rel = p1.getRelationships().iterator().next();
        assertEquals(p1, rel.getPerson());
        assertEquals(p2, rel.getRelatedPerson());
        assertEquals(RelationshipType.PARENT, rel.getRelationshipType());
        assertEquals(RelationshipType.CHILD, rel.getInversRelationshipType());
    }

    @Test
    void testAddTwoDifferentRelationships() {
        PersonEntity p1 = new PersonEntity(1L, 100L, "Person1", LocalDate.of(1980, 1, 1));
        PersonEntity p2 = new PersonEntity(2L, 200L, "Person2", LocalDate.of(1985, 2, 2));

        p1.addRelationship(p2, RelationshipType.PARENT, RelationshipType.CHILD);
        p1.addRelationship(p2, RelationshipType.PARTNER, RelationshipType.PARTNER);

        assertEquals(2, p1.getRelationships().size());
        assertEquals(2, p2.getRelationships().size());

        assertTrue(p1.getRelationships().stream()
            .anyMatch(r -> r.getRelationshipType() == RelationshipType.PARENT));
        assertTrue(p1.getRelationships().stream()
            .anyMatch(r -> r.getRelationshipType() == RelationshipType.PARTNER));
    }

    @Test
    void testAddThreeDifferentRelationships() {
        PersonEntity p1 = new PersonEntity(1L, 100L, "Person1", LocalDate.of(1980, 1, 1));
        PersonEntity p2 = new PersonEntity(2L, 200L, "Person2", LocalDate.of(1985, 2, 2));

        p1.addRelationship(p2, RelationshipType.PARENT, RelationshipType.CHILD);
        p1.addRelationship(p2, RelationshipType.PARTNER, RelationshipType.PARTNER);
        p1.addRelationship(p2, RelationshipType.CHILD, RelationshipType.PARENT);

        assertEquals(3, p1.getRelationships().size());
        assertEquals(3, p2.getRelationships().size());

        assertTrue(p1.getRelationships().stream()
            .anyMatch(r -> r.getRelationshipType() == RelationshipType.PARENT));
        assertTrue(p1.getRelationships().stream()
            .anyMatch(r -> r.getRelationshipType() == RelationshipType.PARTNER));
        assertTrue(p1.getRelationships().stream()
            .anyMatch(r -> r.getRelationshipType() == RelationshipType.CHILD));
    }

    @Test
    void testAddDuplicateRelationshipDoesNotIncreaseCount() {
        PersonEntity p1 = new PersonEntity(1L, 100L, "Person1", LocalDate.of(1980, 1, 1));
        PersonEntity p2 = new PersonEntity(2L, 200L, "Person2", LocalDate.of(1985, 2, 2));

        p1.addRelationship(p2, RelationshipType.PARENT, RelationshipType.CHILD);
        int countBefore = p1.getRelationships().size();

        // Adding the same relationship again
        p1.addRelationship(p2, RelationshipType.PARENT, RelationshipType.CHILD);
        int countAfter = p1.getRelationships().size();

        assertEquals(countBefore, countAfter, "Duplicate relationship should not increase count");
    }

    @Test
    void testAddRelationshipThrowsOnNullId() {
        PersonEntity p1 = new PersonEntity(null, 100L, "Person1", LocalDate.of(1980, 1, 1));
        PersonEntity p2 = new PersonEntity(2L, 200L, "Person2", LocalDate.of(1985, 2, 2));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            p1.addRelationship(p2, RelationshipType.PARENT, RelationshipType.CHILD);
        });

        assertEquals("Both persons must have non-null IDs before adding relationship. Ensure entities have been saved first.", exception.getMessage());
    }
}
