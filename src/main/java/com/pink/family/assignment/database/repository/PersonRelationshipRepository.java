package com.pink.family.assignment.database.repository;

import com.pink.family.assignment.database.entity.PersonEntity;
import com.pink.family.assignment.database.entity.PersonRelationshipEntity;
import com.pink.family.assignment.database.entity.id.PersonRelationshipId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonRelationshipRepository extends JpaRepository<PersonRelationshipEntity, PersonRelationshipId> {

    List<PersonRelationshipEntity> findByRelatedPerson(PersonEntity relatedPerson);
    void deleteAllByPersonOrRelatedPerson(PersonEntity person, PersonEntity relatedPerson);
}
