package com.pink.family.assignment.database.dto;

import com.pink.family.assignment.database.entity.enums.RelationshipType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PersonRelationshipDto {
    private Long relatedPersonId;
    private RelationshipType type;
}