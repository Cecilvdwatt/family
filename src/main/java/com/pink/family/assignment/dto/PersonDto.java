package com.pink.family.assignment.database.dto;

package com.pink.family.assignment.dto;

import com.pink.family.assignment.database.entity.enums.RelationshipType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@Builder
public class PersonDto {
    private Long id;
    private String bsn;
    private String name;
    private String surname;
    private LocalDate dateOfBirth;
    private Set<PersonRelationshipDto> relationships;
}
