package com.pink.family.assignment.database.entity;

import com.pink.family.assignment.database.entity.enums.RelationshipType;
import com.pink.family.assignment.database.entity.id.PersonRelationshipId;
import jakarta.persistence.CascadeType;
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
    private Long id;
    private String bsn;
    private String name;
    private String surname;
    private LocalDate dateOfBirth;

    @OneToMany(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private Set<PersonRelationshipEntity> relationships = new HashSet<>();

    public PersonEntity(Long id, String bsn, String name, String surname, LocalDate dateOfBirth) {
        this.id = id;
        this.bsn = bsn;
        this.name = name;
        this.surname = surname;
        this.dateOfBirth = dateOfBirth;
        this.relationships = new HashSet<>();
    }

    public PersonEntity(Long id, String bsn, String name, String surname, LocalDate dateOfBirth, Set<PersonRelationshipEntity> relationships) {
        this.id = id;
        this.bsn = bsn;
        this.name = name;
        this.surname = surname;
        this.dateOfBirth = dateOfBirth;
        this.relationships = relationships != null ? relationships : new HashSet<>();
    }

    public void addRelationship(PersonEntity related, RelationshipType type, RelationshipType inverseType) {
        if (this.getId() == null || related.getId() == null) {
            throw new IllegalStateException(
                "Both persons must have non-null IDs before adding relationship. Ensure entities have been saved first.");
        }

        PersonRelationshipEntity rel = new PersonRelationshipEntity();
        rel.setPerson(this);
        rel.setRelatedPerson(related);
        rel.setRelationshipType(type);
        rel.setInversRelationshipType(inverseType);
        rel.setId(new PersonRelationshipId(this.getId(), related.getId()));
        this.relationships.add(rel);

        PersonRelationshipEntity inverse = new PersonRelationshipEntity();
        inverse.setPerson(related);
        inverse.setRelatedPerson(this);
        inverse.setRelationshipType(inverseType);
        inverse.setInversRelationshipType(type);
        inverse.setId(new PersonRelationshipId(related.getId(), this.getId()));
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
        return getId() != null && Objects.equals(getId(), person.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ?
            ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() :
            getClass().hashCode();
    }
}
