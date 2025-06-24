package com.pink.family.assignment.api.mapper;

import com.pink.family.api.rest.server.model.FullPerson;
import com.pink.family.api.rest.server.model.PersonBasic;
import com.pink.family.api.rest.server.model.PersonDetailsRequest;
import com.pink.family.api.rest.server.model.Relation;
import com.pink.family.assignment.database.entity.enums.RelationshipType;
import com.pink.family.assignment.dto.PersonDto;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.Optional;

@Slf4j
public class PersonApiMapper {

    public static FullPerson mapToApi(PersonDto dto, PersonDetailsRequest request) {
        if (dto == null) {
            return null;
        }

        FullPerson person = new FullPerson()
            .id(dto.getExternalId())
            .name(dto.getName())
            .birthDate(dto.getDateOfBirth());

        // For each requested relation, find the matching PersonDto in dto relations and map full info
        if (request.getParent1() != null && request.getParent1().getId() != null) {
            Optional<PersonDto> match = dto.getRelations(RelationshipType.CHILD)
                .stream()
                .filter(p -> Objects.equals(p.getExternalId(), request.getParent1().getId()))
                .findFirst();

            match.ifPresent(p -> person.setParent1(mapPersonDtoToBasic(p)));
        }

        if (request.getParent2() != null && request.getParent2().getId() != null) {
            Optional<PersonDto> match = dto.getRelations(RelationshipType.CHILD)
                .stream()
                .filter(p -> Objects.equals(p.getExternalId(), request.getParent2().getId()))
                .findFirst();

            match.ifPresent(p -> person.setParent2(mapPersonDtoToBasic(p)));
        }

        if (request.getChildren() != null) {
            for (Relation childRel : request.getChildren()) {
                if (childRel != null && childRel.getId() != null) {
                    Optional<PersonDto> match = dto.getRelations(RelationshipType.PARENT)
                        .stream()
                        .filter(c -> Objects.equals(c.getExternalId(), childRel.getId()))
                        .findFirst();

                    match.ifPresent(c -> person.addChildrenItem(mapPersonDtoToBasic(c)));
                }
            }
        }

        if (request.getPartner() != null && request.getPartner().getId() != null) {
            Optional<PersonDto> match = dto.getRelations(RelationshipType.PARTNER)
                .stream()
                .filter(p -> Objects.equals(p.getExternalId(), request.getPartner().getId()))
                .findFirst();

            match.ifPresent(p -> person.setPartner(mapPersonDtoToBasic(p)));
        }

        return person;
    }

    private static PersonBasic mapPersonDtoToBasic(PersonDto p) {
        if (p == null) return null;
        return new PersonBasic()
            .id(p.getExternalId())
            .name(p.getName())
            .birthDate(p.getDateOfBirth());
    }

}
