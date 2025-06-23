package com.pink.family.assignment.dto;

import com.pink.family.assignment.database.entity.enums.RelationshipType;
import com.pink.family.assignment.util.MaskUtil;
import jakarta.persistence.Column;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Setter
@Builder
public class PersonDto {

    @Getter
    @Column(name = "id")
    private Long internalId;

    @Getter
    @Column(unique = true)
    private Long externalId;

    @Getter
    private String name;

    @Getter
    private LocalDate dateOfBirth;

    private Map<RelationshipType, Set<PersonDto>> relationships;

    public Map<RelationshipType, Set<PersonDto>> getRelations() {
        if (relationships == null) {
            relationships = new HashMap<>();
        }
        return relationships;
    }

    public Set<PersonDto> getRelations(RelationshipType typeToGet) {
        return getRelations().getOrDefault(typeToGet, Set.of());
    }

    public void addRelationship(RelationshipType type, RelationshipType inverse, PersonDto relation) {
        addRelationshipNoInverse(type, relation);
        relation.addRelationshipNoInverse(inverse, this);
    }

    public void addRelationshipNoInverse(RelationshipType type, PersonDto relation) {
        getRelations()
            .computeIfAbsent(type, t -> new HashSet<>())
            .add(relation);
    }

    @Override
    public String toString() {
        return "%s - %s [%s] \nrelationships=%s"
            .formatted(
                internalId,
                name,
                MaskUtil.maskExternalId(externalId),
                getRelations()
                    .entrySet()
                    .stream()
                    .map(e ->
                        "\n[%s of %s]".formatted(
                            e.getKey(),
                            e.getValue().stream().map(r -> r.name).collect(Collectors.joining("x, "))
                        )
                    )
                    .collect(Collectors.joining(", "))
            );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PersonDto personDto)) return false;
        return Objects.equals(internalId, personDto.internalId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(internalId);
    }
}
