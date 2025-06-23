package com.pink.family.assignment.database.repository;

import com.pink.family.assignment.database.entity.PersonRelationshipEntity;
import com.pink.family.assignment.database.entity.id.PersonRelationshipId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRelationshipRepository extends JpaRepository<PersonRelationshipEntity, PersonRelationshipId> {
}
