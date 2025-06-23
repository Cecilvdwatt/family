package com.pink.family.assignment.database.repository;

import com.pink.family.assignment.CacheConfig;
import com.pink.family.assignment.database.entity.PersonEntity;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository for accessing the {@link PersonEntity} database entity.
 */
public interface PersonRepository extends JpaRepository<PersonEntity, Long> {

    // Design Note:
    ////////////////
    // No tricky where's in sight! Although with how the data is structured it would have been a lot simpler
    // to get the children by doing a SQL join. But at least this gave me a lot of fun figuring out how to map
    // to the DTOs without making it result in stackoverflows!

    @EntityGraph(attributePaths = {"relationships"}) // set which items should be fetched eagerly.
    @Cacheable(cacheNames = CacheConfig.Constant.PERSON_BY_EXTERNAL_ID)
    Optional<PersonEntity> findByExternalId(Long externalId);

    @EntityGraph(attributePaths = {"relationships"})
    @Cacheable(value = CacheConfig.Constant.PERSONS_BY_NAME_DOB, key = "#name + '_' + #dob")
    Set<PersonEntity> findAllByNameAndDateOfBirth(String name, LocalDate dob);

    Set<PersonEntity> findByExternalIdIn(Set<Long> externalIds);

}
