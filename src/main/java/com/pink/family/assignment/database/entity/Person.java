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
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String bsn;
    private String name;
    private String surname;
    private LocalDate dateOfBirth;

    @OneToMany(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private Set<PersonRelationship> relationships = new HashSet<>();

    public Person(Long id, String bsn, String name, String surname, LocalDate dateOfBirth) {
        this.id = id;
        this.bsn = bsn;
        this.name = name;
        this.surname = surname;
        this.dateOfBirth = dateOfBirth;
        this.relationships = new HashSet<>();
    }

    public Person(Long id, String bsn, String name, String surname, LocalDate dateOfBirth, Set<PersonRelationship> relationships) {
        this.id = id;
        this.bsn = bsn;
        this.name = name;
        this.surname = surname;
        this.dateOfBirth = dateOfBirth;
        this.relationships = relationships != null ? relationships : new HashSet<>();
    }

    public void addRelationship(Person relatedPerson, RelationshipType type) {
        PersonRelationship rel = new PersonRelationship(
            new PersonRelationshipId(this.id, relatedPerson.getId()),
            this,
            relatedPerson,
            type
        );
        relationships.add(rel);
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
        Person person = (Person) o;
        return getId() != null && Objects.equals(getId(), person.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ?
            ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() :
            getClass().hashCode();
    }
}
