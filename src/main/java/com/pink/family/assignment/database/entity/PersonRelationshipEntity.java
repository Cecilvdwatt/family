package com.pink.family.assignment.database.entity;

import com.pink.family.assignment.database.entity.enums.RelationshipType;
import com.pink.family.assignment.database.entity.id.PersonRelationshipId;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

// Design Note:
///////////////
// We could in principle have added a 'father', 'mother' and 'partner' column to the person entity that self-referenced
// the person table. However, it was decided to do it this way as it gives us a bit more flexibility in the types of
// relationships we can add down the line (for example we might want to distinguish between a biological parent and a legal
// parent). This does add a bit of performance cost though since we'll be doing table joins. But if additional relationships
// needed to be support in the future using a simpler model (with additional columns) could potentially result in a lot of
// rework, and introducing bugs to the code. This way might have a performance hit, but long term maintainability and
// extendability was considered a better design route.
// If performance issues do crop up it can be optimised at that point. "Avoid pre-optimisation" is a saying for a reason
// as it likely results in a lot of wasted time, and might not even lead to much or any benefit.
@Entity
@Table(name = "person_relationships")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
public class PersonRelationshipEntity {

    @EmbeddedId
    private PersonRelationshipId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("personId")
    @JoinColumn(name = "person_id")
    @ToString.Exclude
    private PersonEntity person;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("relatedPersonId")
    @JoinColumn(name = "related_person_id")
    @ToString.Exclude
    private PersonEntity relatedPerson;

    public RelationshipType getRelationshipType() {
        return getId().getRelationshipType();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ?
            ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() :
            o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ?
            ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() :
            this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        PersonRelationshipEntity that = (PersonRelationshipEntity) o;
        return id != null
            && Objects.equals(id, that.id);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(id);
    }
}
