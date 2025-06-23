package com.pink.family.assignment.database.entity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

/**
 * Relationship types currently supported in the database.
 */
@Getter
@AllArgsConstructor
public enum RelationshipType {
    MOTHER,
    FATHER,
    CHILD,
    PARTNER;

    /**
     * A relationship goes both ways. This method returns the expected types of inverse relationships.
     */
    public static Set<RelationshipType> getInverses(RelationshipType rel){
        return switch(rel) {
            case MOTHER,FATHER -> Set.of(CHILD);
            case CHILD -> Set.of(MOTHER, FATHER);
            case PARTNER -> Set.of(PARTNER);
        };
    }

    /**
     * A super-set of relationship types.
     * As new types are added expand this list with new parental relationships.
     */
    public static Set<RelationshipType> getParentTypes() {
        return Set.of(MOTHER, FATHER);
    }

}