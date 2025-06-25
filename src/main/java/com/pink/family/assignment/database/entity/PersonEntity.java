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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
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
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class PersonEntity {

    /**
     * Internal ID used by the application.
     * Simple long for easy use and maintenance.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "internal_id", updatable = false)
    private Long internalId;
    /**
     * External ID, the assignment wasn't specific, so
     * the assumption is that this could be anything from a
     * passport number to a club membership number
     * so the uniqueness cannot be guaranteed.
     */

    @Column(name = "external_id")
    private Long externalId;

    @Column(name = "person_name")
    private String name;

    @Column(name = "person_date_of_birth")
    private LocalDate dateOfBirth;

    @Builder.Default
    @Column(name = "person_deleted", nullable = false)
    private boolean deleted = false;

    @OneToMany(
        mappedBy = "person",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
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

    public PersonEntity(
        Long internalId,
        Long externalId,
        String name,
        LocalDate dateOfBirth,
        Set<PersonRelationshipEntity> relationships)
    {
        this.internalId = internalId;
        this.externalId = externalId;
        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.relationships = relationships != null ?
            relationships :
            new HashSet<>();
    }

    public void addRelationship(PersonEntity relatedPerson, RelationshipType type, RelationshipType inverseType) {
        if (relatedPerson == null || type == null || inverseType == null) {
            return;
        }

        if (this.getInternalId() == null || relatedPerson.getInternalId() == null) {
            throw new IllegalStateException(
                "Both persons must have non-null IDs before adding relationship. Ensure entities have been saved first.");
        }

        log.info("{} : Adding relationship {} with {}", this, type, relatedPerson);

        // Forward relationship
        PersonRelationshipId id = new PersonRelationshipId(this.getInternalId(), relatedPerson.getInternalId(), type);

        log.debug("Forward Relationship Id: {}", id);
        PersonRelationshipEntity forwardRel = this.relationships.stream()
            .filter(r -> r.getId().equals(id))
            .findFirst()
            .orElse(null);

        if (forwardRel == null) {
            forwardRel = new PersonRelationshipEntity();
            forwardRel.setId(id);
            forwardRel.setPerson(this);
            forwardRel.setRelatedPerson(relatedPerson);
            this.relationships.add(forwardRel);
        }

        // Inverse relationship
        PersonRelationshipId inverseId = new PersonRelationshipId(
            relatedPerson.getInternalId(),
            this.getInternalId(),
            inverseType
        );

        log.debug("Backwards Id: {}", inverseId);
        PersonRelationshipEntity inverseRel = relatedPerson.relationships.stream()
            .filter(r -> r.getId().equals(inverseId))
            .findFirst()
            .orElse(null);

        if (inverseRel == null) {
            inverseRel = new PersonRelationshipEntity();
            inverseRel.setId(inverseId);
            inverseRel.setPerson(relatedPerson);
            inverseRel.setRelatedPerson(this);
            relatedPerson.relationships.add(inverseRel);
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
            .append(", dob=").append(dateOfBirth).append(")")
            .append(", deleted=").append(deleted)
            .append("\n");

        if (relationships != null && !relationships.isEmpty()) {
            for (PersonRelationshipEntity rel : relationships) {
                if (rel == null) {
                    continue;
                }
                RelationshipType type = rel.getId().getRelationshipType();
                PersonEntity related = rel.getRelatedPerson();
                if (related == null) {
                    continue;
                }

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
            relSummary.isEmpty() ?
                "none" :
                relSummary
        );
    }


}
