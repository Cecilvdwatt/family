package com.pink.family.assignment.database.entity.id;

import com.pink.family.assignment.database.entity.PersonEntity;
import com.pink.family.assignment.database.entity.enums.RelationshipType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonRelationshipId implements Serializable {

    @Column(nullable = false)
    private Long personId;

    @Column(nullable = false)
    private Long relatedPersonId;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false)
    private RelationshipType relationshipType;


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PersonRelationshipId that)) {
            return false;
        }
        return Objects.equals(personId, that.personId) && Objects.equals(relatedPersonId,
            that.relatedPersonId
        ) && relationshipType == that.relationshipType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(personId, relatedPersonId, relationshipType);
    }
}
