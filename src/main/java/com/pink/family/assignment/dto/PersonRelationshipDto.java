package com.pink.family.assignment.dto;

import com.pink.family.assignment.database.entity.enums.RelationshipType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PersonRelationshipDto {
    private RelationshipType type;
    private PersonDto person;
    private PersonDto relatedPerson;
}