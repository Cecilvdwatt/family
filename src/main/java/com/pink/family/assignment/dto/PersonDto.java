package com.pink.family.assignment.dto;

import com.pink.family.assignment.database.entity.enums.RelationshipType;
import com.pink.family.assignment.util.MaskUtil;
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
    private Long id;
    @Getter
    private String bsn;
    @Getter
    private String name;
    @Getter
    private String surname;
    @Getter
    private LocalDate dateOfBirth;
    private Map<RelationshipType, Set<PersonDto>> relationships;

    public Map<RelationshipType, Set<PersonDto>> getRelationships() {
        if(relationships == null) {
            relationships = new HashMap<>();

            for (RelationshipType type : RelationshipType.values()) {
                relationships.put(type, new HashSet<>());
            }
        }

        return relationships;
    }

    public Set<PersonDto> getRelationships(RelationshipType typeToGet) {

        return getRelationships().get(typeToGet);
    }

    public void addRelationship(RelationshipType type, RelationshipType inverse, PersonDto relation) {

        addRelationshipNoInverse(type, relation);
        relation.addRelationshipNoInverse(inverse, this);

    }

    public void addRelationshipNoInverse(RelationshipType type, PersonDto relation) {

        Set<PersonDto> relations = getRelationships(type);

        if(relations == null) {
            relations = new HashSet<>();
            getRelationships().put(type, relations);
        }

        relations.add(relation);
    }

    @Override
    public String toString() {
        return

            "PersonEntity DTO \n - %s - %s %s [%s] relationships=%s"
                .formatted(
                    id,
                    name,
                    surname,
                    MaskUtil.maskBsn(bsn),
                    getRelationships()
                        .entrySet()
                        .stream()
                        .map(e ->
                            e.getValue().isEmpty() ?
                                "[NOT " + e.getKey() + "]" :
                                "[%s - %s]".formatted(
                                    e.getKey(),
                                    e.getValue().stream()
                                        .map(r -> r.name + " " + r.surname)
                                        .collect(Collectors.joining(", "))
                                )
                            )
                        .collect(Collectors.joining(", ")));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PersonDto personDto)) {
            return false;
        }
        return Objects.equals(id, personDto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
