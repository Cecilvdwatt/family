package com.pink.family.assignment.database.entity.id;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.*;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonRelationshipId implements Serializable {
    private Long personId;
    private Long relatedPersonId;
}
