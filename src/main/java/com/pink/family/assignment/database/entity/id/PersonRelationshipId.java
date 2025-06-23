package com.pink.family.assignment.database.entity.id;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonRelationshipId implements Serializable {

    private Long personId;
    private Long relatedPersonId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PersonRelationshipId that)) return false;
        return Objects.equals(personId, that.personId) &&
            Objects.equals(relatedPersonId, that.relatedPersonId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(personId, relatedPersonId);
    }
}
