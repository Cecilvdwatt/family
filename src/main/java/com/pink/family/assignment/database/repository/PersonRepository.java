package com.pink.family.assignment.database.repository;

import com.pink.family.assignment.CacheConfig;
import com.pink.family.assignment.database.entity.PersonEntity;
import jakarta.validation.constraints.NotNull;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

/**
 * Repository for accessing the {@link PersonEntity} database entity.
 */
@Repository
@Transactional
public interface PersonRepository extends JpaRepository<PersonEntity, Long> {

    @EntityGraph(attributePaths = {"relationships"})
    @Cacheable(
        cacheNames = CacheConfig.Constant.PERSON_BY_EXTERNAL_ID,
        key = "#externalId")
    Optional<PersonEntity> findByExternalId(@NotNull Long externalId);

    @EntityGraph(attributePaths = {"relationships", "relationships.relatedPerson.relationships"})
    @Cacheable(
        value = CacheConfig.Constant.PERSONS_BY_NAME_DOB,
        key = "#name + '_' + #dob")
    Set<PersonEntity> findAllByNameAndDateOfBirth(String name, LocalDate dob);

    @EntityGraph(attributePaths = {"relationships", "relationships.relatedPerson.relationships"})
    Set<PersonEntity> findByExternalIdIn(Set<Long> externalIds);

    @Modifying
    @Transactional
    @Query("UPDATE PersonEntity p SET p.deleted = :delete WHERE p.externalId in :externalIds")
    int updateDeleteByExternalId(@Param("externalIds") Set<Long> externalId, @Param("delete") boolean delete);

}
