package com.pink.family.assignment.database.mapper;

import com.pink.family.assignment.database.entity.PersonEntity;
import com.pink.family.assignment.database.entity.enums.RelationshipType;
import com.pink.family.assignment.dto.PersonDto;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Map between the DTOs and the Database Entities.
 * This is to prevent "attached" database entities to be accessed outside a transaction scope, or unintentional
 * updates being made to the database entities.
 * To that end whatever code is calling this class should remember to add {@link org.springframework.transaction.annotation.Transactional}
 */
@Slf4j
public class PersonDbMapper {

    /**
     * Entry method that maps PersonEntity to PersonDto with full depth.
     * @param personEntity PersonEntity to map
     * @param maxDepth Maximum depth of relationships to recurse (0 = no relationships)
     * @return PersonDto with relationships mapped up to maxDepth
     */
    public static PersonDto mapDto(@NonNull PersonEntity personEntity, int maxDepth) {
        return mapDto(personEntity, maxDepth, 0, new HashMap<>());
    }

    /**
     * Internal recursive method to map with depth control.
     *
     * @param personEntity The person entity to map
     * @param maxDepth Maximum depth allowed
     * @param currentDepth Current depth level in recursion
     * @param mappedDtos already mapped (cycle protection)
     * @return Mapped PersonDto with relationships up to maxDepth
     */
    private static PersonDto mapDto(@NonNull PersonEntity personEntity, int maxDepth, int currentDepth, @NonNull Map<Long, PersonDto> mappedDtos) {
        Long internalId = personEntity.getInternalId();
        if (internalId == null) {
            // Fallback: just return a new mapped dto with no relationships
            return mapDtoNoRel(personEntity);
        }

        if (mappedDtos.containsKey(internalId)) {
            // Already mapped, return existing reference
            return mappedDtos.get(internalId);
        }

        // Create and store a shallow dto to prevent infinite recursion
        PersonDto dto = mapDtoNoRel(personEntity);
        mappedDtos.put(internalId, dto);

        if (currentDepth >= maxDepth || maxDepth == 0) {
            return dto; // Stop recursion here
        }

        if (personEntity.getRelationships() != null) {
            for (var rel : personEntity.getRelationships()) {
                RelationshipType type = rel.getRelationshipType();
                RelationshipType inverseType = rel.getInversRelationshipType();
                PersonEntity relatedPerson = rel.getRelatedPerson();

                if (relatedPerson != null) {
                    PersonDto relatedDto = mapDto(relatedPerson, maxDepth, currentDepth + 1, mappedDtos);
                    dto.addRelationship(type, inverseType, relatedDto);
                }
            }
        }

        return dto;
    }


    /**
     * Map PersonEntity to PersonDto without relationships (shallow).
     * Used to break recursion cycles or for depth zero.
     */
    public static PersonDto mapDtoNoRel(@NonNull PersonEntity person) {
        return PersonDto.builder()
            .internalId(person.getInternalId())
            .externalId(person.getExternalId())
            .name(person.getName())
            .dateOfBirth(person.getDateOfBirth())
            .build();
    }
}
