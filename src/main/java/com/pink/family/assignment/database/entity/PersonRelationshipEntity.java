package com.pink.family.assignment.database.entity;

import com.pink.family.assignment.database.entity.enums.RelationshipType;
import com.pink.family.assignment.database.entity.id.PersonRelationshipId;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
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
