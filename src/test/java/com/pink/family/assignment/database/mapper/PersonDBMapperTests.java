package com.pink.family.assignment.database.mapper;

import com.pink.family.assignment.database.entity.PersonEntity;
import com.pink.family.assignment.database.entity.enums.RelationshipType;
import com.pink.family.assignment.dto.PersonDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PersonDbMapperTest {

    @Test
    @DisplayName("Depth 0: Should map person only, no relationships")
    void testDepth0() {
        PersonEntity solo = create("Solo", 1L);
        PersonEntity child = create("Child", 2L);
        solo.addRelationship(child, RelationshipType.PARENT, RelationshipType.CHILD);

        PersonDto dto = PersonDbMapper.mapDto(solo, 0);

        assertNotNull(dto);
        assertTrue(dto.getRelations(RelationshipType.PARENT).isEmpty());
    }

    @Test
    @DisplayName("Depth 1: Should map direct relationships only")
    void testDepth1() {
        PersonEntity parent = create("Parent", 1L);
        PersonEntity child = create("Child", 2L);
        parent.addRelationship(child, RelationshipType.PARENT, RelationshipType.CHILD);

        PersonDto dto = PersonDbMapper.mapDto(parent, 1);

        assertEquals(1, dto.getRelations(RelationshipType.PARENT).size());
        PersonDto childDto = dto.getRelations(RelationshipType.PARENT).iterator().next();
        assertEquals("Child", childDto.getName());
        assertFalse(childDto.getRelations(RelationshipType.CHILD).isEmpty());
    }

    @Test
    @DisplayName("Depth 2: Child should map both parents")
    void testDepth2_ChildWithTwoParents() {
        PersonEntity parent1 = create("Parent1", 1L);
        PersonEntity parent2 = create("Parent2", 2L);
        PersonEntity child = create("Child", 3L);

        parent1.addRelationship(child, RelationshipType.PARENT, RelationshipType.CHILD);
        parent2.addRelationship(child, RelationshipType.PARENT, RelationshipType.CHILD);

        PersonDto dto = PersonDbMapper.mapDto(parent1, 2);

        PersonDto childDto = dto.getRelations(RelationshipType.PARENT).iterator().next();
        assertEquals("Child", childDto.getName());

        Set<PersonDto> childParents = childDto.getRelations(RelationshipType.CHILD);
        assertEquals(2, childParents.size());
        assertTrue(childParents.stream().anyMatch(p -> p.getName().equals("Parent1")));
        assertTrue(childParents.stream().anyMatch(p -> p.getName().equals("Parent2")));
    }

    @Test
    @DisplayName("Cycle: Should not recurse infinitely")
    void testCycleMapping() {
        PersonEntity a = create("A", 1L);
        PersonEntity b = create("B", 2L);
        a.addRelationship(b, RelationshipType.PARTNER, RelationshipType.PARTNER);
        b.addRelationship(a, RelationshipType.PARTNER, RelationshipType.PARTNER);

        PersonDto dto = PersonDbMapper.mapDto(a, 3);
        assertEquals(1, dto.getRelations(RelationshipType.PARTNER).size());

        PersonDto partner = dto.getRelations(RelationshipType.PARTNER).iterator().next();
        assertEquals("B", partner.getName());
        assertEquals(1, partner.getRelations(RelationshipType.PARTNER).size());
        assertTrue(partner.getRelations(RelationshipType.PARTNER).stream()
            .anyMatch(p -> p.getName().equals("A")));
    }

    @Test
    @DisplayName("Depth overreach: Should safely stop at deepest layer")
    void testDepthLimitStops() {
        PersonEntity a = create("A", 1L);
        PersonEntity b = create("B", 2L);
        PersonEntity c = create("C", 3L);

        a.addRelationship(b, RelationshipType.PARENT, RelationshipType.CHILD);
        b.addRelationship(c, RelationshipType.PARENT, RelationshipType.CHILD);

        PersonDto dto = PersonDbMapper.mapDto(a, 2);
        PersonDto bDto = dto.getRelations(RelationshipType.PARENT).iterator().next();
        assertEquals("B", bDto.getName());

        PersonDto cDto = bDto.getRelations(RelationshipType.PARENT).iterator().next();
        assertEquals("C", cDto.getName());
        assertTrue(cDto.getRelations(RelationshipType.PARENT).isEmpty());
    }

    private PersonEntity create(String name, long id) {
        return PersonEntity.builder()
            .internalId(id)
            .name(name)
            .externalId(id * 10)
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .build();
    }
}
