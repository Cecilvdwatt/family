package com.pink.family.assignment.database.repository;

import com.pink.family.assignment.database.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for accessing the {@link Person} database entity.
 */
public interface PersonRepository extends JpaRepository<Person, Long> {
}
