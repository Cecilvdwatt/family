package com.pink.family.assignment.dto;

import com.pink.family.assignment.database.entity.enums.RelationshipType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Builder
public class PersonDto {

    @Getter
    private Long internalId;

    @Getter
    private Long externalId;

    @Getter
    private String name;

    @Getter
    private LocalDate dateOfBirth;

    @Getter
    private boolean deleted;

    private Map<RelationshipType, Set<PersonDto>> relationships;

    public Map<RelationshipType, Set<PersonDto>> getRelations() {
        if (relationships == null) {
            relationships = new EnumMap<>(RelationshipType.class);
            for (RelationshipType type : RelationshipType.values()) {
                relationships.put(type, new HashSet<>());
            }
        }
        return relationships;
    }

    public Set<PersonDto> getRelations(RelationshipType type) {
        return getRelations().computeIfAbsent(type, k -> new HashSet<>());
    }

    public void addRelationship(RelationshipType type, PersonDto relation) {
        if (type != null) {
            addRelationshipNoInverse(type, relation);
            relation.addRelationshipNoInverse(type.getInverse(), this);
        }
    }

    public void addRelationshipNoInverse(RelationshipType type, PersonDto relation) {
        if (type == null) {
            return;
        }
        getRelations()
            .computeIfAbsent(type, t -> new HashSet<>())
            .add(relation);
    }

    @Override
    public String toString() {
        return "%s - %s [%s] relationships:%s"
            .formatted(
                internalId,
                name,
                externalId,
                getRelations()
                    .entrySet()
                    .stream()
                    .map(e ->
                        " [%s of %s]".formatted(
                            e.getKey(),
                            e.getValue().stream().map(r -> r.name).reduce((a,b)->a+", "+b).orElse("")
                        )
                    )
                    .reduce((a,b) -> a+", "+b).orElse("")
            );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PersonDto personDto)) return false;
        return internalId != null && internalId.equals(personDto.internalId);
    }

    @Override
    public int hashCode() {
        return internalId != null ? internalId.hashCode() : 0;
    }

    public String prettyPersonDtoString() {
        return prettyPersonDtoString(new HashSet<>(), 0);
    }

    private String prettyPersonDtoString(Set<Long> visited, int indentLevel) {
        StringBuilder sb = new StringBuilder();
        String indent = "  ".repeat(indentLevel);

        if (visited.contains(this.getExternalId())) {
            sb.append(indent).append("- [Already printed: ")
                .append(this.getName())
                .append(" (ExternalId: ").append(this.getExternalId()).append(")]\n");
            return sb.toString();
        }

        visited.add(this.getExternalId());

        sb.append(indent)
            .append("Person: ").append(this.getName())
            .append(" (ExternalId: ").append(this.getExternalId()).append(")")
            .append(" (InternalId: ").append(this.getInternalId()).append(")\n");

        for (var entry : this.getRelations().entrySet()) {
            RelationshipType relType = entry.getKey();
            Set<PersonDto> relatedPersons = entry.getValue();
            if (relatedPersons.isEmpty()) continue;

            String relatedNames = relatedPersons.stream()
                .map(PersonDto::getName)
                .collect(Collectors.joining(","));

            sb.append("  ".repeat(indentLevel + 1))
                .append(relType)
                .append(" [").append(relatedPersons.size()).append(":").append(relatedNames).append("]:\n");

            for (PersonDto related : relatedPersons) {
                sb.append(related.prettyPersonDtoString(visited, indentLevel + 2));
            }
        }

        return sb.toString();
    }




}
