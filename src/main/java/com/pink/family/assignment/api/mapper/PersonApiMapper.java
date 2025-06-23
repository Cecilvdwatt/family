package com.pink.family.assignment.api.mapper;

import com.pink.family.api.rest.server.model.Relation;
import com.pink.family.assignment.dto.PersonDto;
import com.pink.family.assignment.util.CompareUtil;

import java.util.Objects;
import java.util.Set;

public class PersonApiMapper {

    public static PersonDto mapDtoFromRelation(Relation relation, Set<PersonDto> existingRelations) {

        // looking through the existing relationships to see if we already have
        // this person as a relative, update if necessary or create a new person.
        return (PersonDto) existingRelations
            .stream()
            .filter(e -> Objects.equals(e.getExternalId(), relation.getId()))
            // the relations we get in the API are only IDs so nothing really to map other than
            // if it exists already.
            .findFirst()
            .orElse(
                PersonDto
                    .builder()
                    .externalId(relation.getId())
                    .build()
            );
    }
}
