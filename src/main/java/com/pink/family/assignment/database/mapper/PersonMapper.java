package com.pink.family.assignment.database.mapper;

import com.pink.family.assignment.database.entity.PersonEntity;
import com.pink.family.assignment.database.entity.enums.RelationshipType;
import com.pink.family.assignment.dto.PersonDto;
import com.pink.family.assignment.monitoring.metrics.annotation.SanitizerMeasure;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Map between the DTOs and the Database Entities.
 * This is to prevent "attached" database entities to be accessed outside a transaction scope, or unintentional
 * updates being made to the database entities.
 * To that end whatever code is calling this class should remember to add {@link org.springframework.transaction.annotation.Transactional}
 */
@Slf4j
public class PersonMapper {

    /**
     * Map a PersonEntity database entity with only specific relationships.
     * @param person
     * Database entity to map
     * @param relationshipFilter
     * Relationships to include. If empty includes all.
     */
    @SanitizerMeasure
    public static PersonDto toDto(
        @Nullable PersonEntity person,
        @Nullable Set<RelationshipType> relationshipFilter,
        @Nullable Set<PersonDto> mapped
    ) {
        if (person == null) {
            return null;
        }

        if (mapped == null) {
            mapped = new HashSet<>();
        } else {
            Optional<PersonDto> existing = mapped.stream()
                .filter(dto -> Objects.equals(dto.getId(), person.getId()))
                .findFirst();
            if (existing.isPresent()) {
                log.debug("Using existing mapped person: {}", existing.get());
                return existing.get();
            }
        }

        PersonDto mappedDto = toDtoNoRel(person);
        mapped.add(mappedDto);

        log.debug(
            "\nMapping: {} {} {}\n * Looking for: {}\n * has relationships:\n{}",
            person.getId(), person.getName(), person.getSurname(), relationshipFilter,
            person.getRelationships().stream()
                .map(e -> "    - %s %s %s [Relationship: %s]".formatted(
                    e.getRelatedPerson().getId(),
                    e.getRelatedPerson().getName(),
                    e.getRelatedPerson().getSurname(),
                    e.getRelationshipType()))
                .collect(Collectors.joining("\n"))
        );

        for (var rel : person.getRelationships()) {
            RelationshipType type = rel.getRelationshipType();
            RelationshipType inverse = rel.getInversRelationshipType();
            PersonEntity relatedPerson = rel.getRelatedPerson();

            log.debug(" * Examining Relationship: {} {} [{}]", relatedPerson.getName(), relatedPerson.getSurname(), type);

            if (!CollectionUtils.isEmpty(relationshipFilter) && !relationshipFilter.contains(type)) {
                log.debug("   -> Relationship type {} filtered out", type);
                continue;
            }

            Optional<PersonDto> optRelation = mapped.stream()
                .filter(e -> Objects.equals(e.getId(), relatedPerson.getId()))
                .findFirst();

            PersonDto relatedDto;
            if (optRelation.isPresent()) {
                relatedDto = optRelation.get();
                log.debug("   -> Already mapped: {}", relatedDto);
            } else {
                Set<RelationshipType> inverseFilter = CollectionUtils.isEmpty(relationshipFilter)
                    ? null
                    : RelationshipType.getInverses(type);
                relatedDto = toDto(relatedPerson, inverseFilter, mapped);
            }

            mappedDto.addRelationship(type, inverse, relatedDto);

            log.debug("   -> Added {} {} [{}] to {} {}",
                relatedDto.getName(), relatedDto.getSurname(), type,
                mappedDto.getName(), mappedDto.getSurname());
        }

        log.debug("Mapped DTO (in progress): \n{}", mappedDto);
        return mappedDto;
    }

    private static PersonDto toDtoNoRel(PersonEntity person) {
        return PersonDto.builder()
            .id(person.getId())
            .bsn(person.getBsn())
            .name(person.getName())
            .surname(person.getSurname())
            .dateOfBirth(person.getDateOfBirth())
            .build();
    }

}
