package com.pink.family.assignment.database.repository;

import com.pink.family.assignment.database.entity.PersonRelationship;
import com.pink.family.assignment.database.entity.id.PersonRelationshipId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRelationshipRepository extends JpaRepository<PersonRelationship, PersonRelationshipId> {
}
