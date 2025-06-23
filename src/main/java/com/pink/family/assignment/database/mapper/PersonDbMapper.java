package com.pink.family.assignment.database.mapper;

import com.pink.family.assignment.database.entity.PersonEntity;
import com.pink.family.assignment.database.entity.enums.RelationshipType;
import com.pink.family.assignment.dto.PersonDto;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Map between the DTOs and the Database Entities.
 * This is to prevent "attached" database entities to be accessed outside a transaction scope, or unintentional
 * updates being made to the database entities.
 * To that end whatever code is calling this class should remember to add {@link org.springframework.transaction.annotation.Transactional}
 */
@Slf4j
public class PersonDbMapper {

    public static PersonDto mapDto(
        @NotNull PersonEntity personEntity
    ) {
        return mapDto(personEntity, Set.of(), new HashSet<>());
    }

    public static PersonDto mapDto(
        @NotNull PersonEntity personEntity,
        @Nullable Set<RelationshipType> relationshipFilter
    ) {
        return mapDto(personEntity, relationshipFilter, new HashSet<>());
    }
    /**
     * Map a PersonEntity database entity with only specific relationships.
     * @param personEntity
     * Database entity to map
     * @param relationshipFilter
     * Relationships to include. If empty includes all.
     */
    public static PersonDto mapDto(
        @NotNull PersonEntity personEntity,
        @Nullable Set<RelationshipType> relationshipFilter,
        @NotNull final Set<Long> mappedIds
    ) {
        if (relationshipFilter == null) {
            relationshipFilter = Set.of();
        }

        if (mappedIds.contains(personEntity.getInternalId())) {
            // Already mapped this person, return shallow DTO to prevent cycles
            return mapDtoNoRel(personEntity);
        }

        // Mark this person as mapped
        mappedIds.add(personEntity.getInternalId());

        // Start mapping base DTO without relationships
        PersonDto mappedDto = mapDtoNoRel(personEntity);

        // If filter is empty, map all relationships, else filter by type
        Set<RelationshipType> filter = relationshipFilter.isEmpty() ?
            Set.of(RelationshipType.values()) : relationshipFilter;

        for (var rel : personEntity.getRelationships()) {
            RelationshipType type = rel.getRelationshipType();

            if (!filter.contains(type)) {
                continue; // skip unwanted relationship types
            }

            PersonEntity relatedPerson = rel.getRelatedPerson();
            RelationshipType inverseType = rel.getInversRelationshipType();

            // Recursively map related person with appropriate filters,
            // passing the current mappedIds to avoid cycles
            // IMPORTANT: For your specific case:
            // - If mapping a parent -> child relationship, next map the child with filter = {PARENT}
            // - So the child's parents get mapped, including the original parent plus others

            Set<RelationshipType> nextFilter;

            if (type == RelationshipType.PARENT) {
                // From parent, map children, but on children, map their parents
                nextFilter = Set.of(RelationshipType.PARENT);
            } else if (type == RelationshipType.CHILD) {
                // From child, if you want to map parents or partners, customize here
                nextFilter = Set.of(RelationshipType.PARENT);
            } else {
                // For other relationships, just pass empty or same filter
                nextFilter = relationshipFilter;
            }

            PersonDto relatedDto = mapDto(relatedPerson, nextFilter, mappedIds);

            mappedDto.addRelationship(type, inverseType, relatedDto);
        }

        return mappedDto;
    }


    /**
     * Map a Person Entity to a Person DTO but do not map relationships.
     * This is useful since relationships are two-way and can lead to endless recursion.
     *
     * @param person
     * Person entity to map.
     * @return
     * The mapped Person DTO or Null of no Person exists.
     */
    public static PersonDto mapDtoNoRel(@NonNull PersonEntity person) {
        return
            PersonDto
                .builder()
                .internalId(person.getInternalId())
                .externalId(person.getExternalId())
                .name(person.getName())
                .dateOfBirth(person.getDateOfBirth())
                .build();
    }

}
