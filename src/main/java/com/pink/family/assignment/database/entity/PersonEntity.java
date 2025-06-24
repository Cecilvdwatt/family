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
import lombok.extern.slf4j.Slf4j;
import org.hibernate.proxy.HibernateProxy;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
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
    @Column(unique = true, nullable = true)
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

        log.info("{} : Adding relationship {} with {}", this, type, related);

        // because sets we won't add multiples but there's no need to create a new object if we don't have to
        var id = new PersonRelationshipId(this.getInternalId(), related.getInternalId());
        if(!this.relationships.contains(id)) {
            PersonRelationshipEntity rel = new PersonRelationshipEntity();
            rel.setPerson(this);
            rel.setRelatedPerson(related);
            rel.setRelationshipType(type);
            rel.setInversRelationshipType(inverseType);
            rel.setId(id);
            this.relationships.add(rel);
        }

        id = new PersonRelationshipId(related.getInternalId(), this.getInternalId());
        if(!related.relationships.contains(id)) {
            PersonRelationshipEntity inverse = new PersonRelationshipEntity();
            inverse.setPerson(related);
            inverse.setRelatedPerson(this);
            inverse.setRelationshipType(inverseType);
            inverse.setInversRelationshipType(type);
            inverse.setId(id);
            related.relationships.add(inverse);
        }
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

    public String prettyPrint() {
        StringBuilder sb = new StringBuilder();
        prettyPrintHelper(sb, 0, new HashSet<>());
        return sb.toString();
    }

    private void prettyPrintHelper(StringBuilder sb, int indent, Set<Long> visited) {
        if (internalId == null) {
            sb.append("  ".repeat(indent)).append("[Person with null internalId]\n");
            return;
        }
        if (visited.contains(internalId)) {
            sb.append("  ".repeat(indent))
                .append("[Already printed person ID ").append(internalId).append("]\n");
            return;
        }
        visited.add(internalId);

        String indentStr = "  ".repeat(indent);
        sb.append(indentStr)
            .append("PersonEntity: ")
            .append(name).append(" (internalId=").append(internalId)
            .append(", externalId=").append(externalId)
            .append(", dob=").append(dateOfBirth).append(")\n");

        if (relationships != null && !relationships.isEmpty()) {
            for (PersonRelationshipEntity rel : relationships) {
                if (rel == null) continue;
                RelationshipType type = rel.getRelationshipType();
                PersonEntity related = rel.getRelatedPerson();
                if (related == null) continue;

                sb.append(indentStr).append("  ").append(type).append(":\n");
                related.prettyPrintHelper(sb, indent + 2, visited);
            }
        }
    }

    @Override
    public String toString() {
        // Count relationships by type
        Map<RelationshipType, Long> relCountByType = getRelationships().stream()
            .collect(Collectors.groupingBy(
                PersonRelationshipEntity::getRelationshipType,
                Collectors.counting()
            ));

        // Build relationship summary string
        String relSummary = relCountByType.entrySet().stream()
            .map(e -> String.format("%s[%d]", e.getKey(), e.getValue()))
            .collect(Collectors.joining("; "));

        return String.format("PersonEntity{id=%d, externalId=%s, name='%s', dob=%s, relationships=[%s]}",
            getInternalId(),
            getExternalId(),
            getName(),
            getDateOfBirth(),
            relSummary.isEmpty() ? "none" : relSummary
        );
    }


}
