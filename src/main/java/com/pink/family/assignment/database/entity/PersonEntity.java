package com.pink.family.assignment.database.entity;

import com.pink.family.assignment.database.entity.enums.RelationshipType;
import com.pink.family.assignment.database.entity.id.PersonRelationshipId;
import jakarta.annotation.Nullable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "persons")
@Getter
@Setter
@ToString
@NoArgsConstructor
@Builder
public class PersonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long internalId;
    @Nullable
    private Long externalId;
    @Nullable
    private String name;
    @Nullable
    private LocalDate dateOfBirth;

    @OneToMany(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private Set<PersonRelationshipEntity> relationships = new HashSet<>();

    public PersonEntity(Long internalId, Long externalId, String name, LocalDate dateOfBirth) {
        this.internalId = internalId;
        this.externalId = externalId;
        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.relationships = new HashSet<>();
    }

    public PersonEntity(Long internalId, Long externalId, String name, LocalDate dateOfBirth, Set<PersonRelationshipEntity> relationships) {
        this.internalId = internalId;
        this.externalId = externalId;
        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.relationships = relationships != null ? relationships : new HashSet<>();
    }

    public void addRelationship(PersonEntity related, RelationshipType type, RelationshipType inverseType) {
        if (this.getInternalId() == null || related.getInternalId() == null) {
            throw new IllegalStateException(
                "Both persons must have non-null IDs before adding relationship. Ensure entities have been saved first.");
        }

        PersonRelationshipEntity rel = new PersonRelationshipEntity();
        rel.setPerson(this);
        rel.setRelatedPerson(related);
        rel.setRelationshipType(type);
        rel.setInversRelationshipType(inverseType);
        rel.setId(new PersonRelationshipId(this.getInternalId(), related.getInternalId()));
        this.relationships.add(rel);

        PersonRelationshipEntity inverse = new PersonRelationshipEntity();
        inverse.setPerson(related);
        inverse.setRelatedPerson(this);
        inverse.setRelationshipType(inverseType);
        inverse.setInversRelationshipType(type);
        inverse.setId(new PersonRelationshipId(related.getInternalId(), this.getInternalId()));
        related.relationships.add(inverse);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        Class<?> oEffectiveClass = o instanceof HibernateProxy ?
            ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() :
            o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ?
            ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() :
            this.getClass();
        if (thisEffectiveClass != oEffectiveClass) {
            return false;
        }
        PersonEntity person = (PersonEntity) o;
        return getInternalId() != null && Objects.equals(getInternalId(), person.getInternalId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ?
            ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() :
            getClass().hashCode();
    }
}
