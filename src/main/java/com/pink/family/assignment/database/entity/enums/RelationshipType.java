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
    PARENT,
    CHILD,
    PARTNER;

    /**
     * A relationship goes both ways. This method returns the expected types of inverse relationships.
     */
    public static RelationshipType getInverses(RelationshipType rel){
        return switch(rel) {
            case PARENT -> CHILD;
            case CHILD -> PARENT;
            case PARTNER -> PARTNER;
        };
    }

    public RelationshipType getInverse() {
        return RelationshipType.getInverses(this);
    }
}