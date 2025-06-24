package com.pink.family.assignment.database.dao;

import com.pink.family.assignment.api.exception.PinkSystemException;
import com.pink.family.assignment.database.entity.PersonEntity;
import com.pink.family.assignment.database.entity.PersonRelationshipEntity;
import com.pink.family.assignment.database.entity.enums.RelationshipType;
import com.pink.family.assignment.database.mapper.PersonDbMapper;
import com.pink.family.assignment.database.repository.PersonRepository;
import com.pink.family.assignment.dto.PersonDto;
import com.pink.family.assignment.service.PersonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The data access object for a PersonEntity entity.
 * This object returns detached {@link PersonDto} objects in order to ensure that we do not expose
 * attached objects outside and potentially run into issues such as no transaction or accidentally
 * make modifications to the underlying data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PersonDao {

    private final PersonRepository personRepository;
    private final PersonRelationshipDao personRelationshipDao;

    /**
     * Attempt to find a PersonEntity record in the database and any associated Children and Partners using an external
     * Id.
     *
     * @return An empty optional if the person could not be found. An optional with a PersonEntity record containing on
     * their children and partners.
     */
    @Transactional(readOnly = true)
    public Optional<PersonDto> findPersonFromExternalId(Long externalId, int relationshipDepth) {
        return personRepository
            .findByExternalId(externalId)
            .map(personEntity -> {
                log.debug("\nLoaded person entity (with relationships, depth={}):\n{}", relationshipDepth, personEntity.prettyPrint());
                PersonDto dto = PersonDbMapper.mapDto(personEntity, relationshipDepth);
                log.debug("\nMapped person DTO (with relationships, depth={}):\n{}", relationshipDepth, dto.prettyPersonDtoString());
                return dto;
            });
    }


    /**
     * Attempt to find a PersonEntity record in the database and any associated Children and Partners using a name,
     * surname and dob.
     *
     * @return A list of PersonEntity Records matching the provided criteria along with their children and partners.
     */
    @Transactional(readOnly = true)
    public Set<PersonDto> findAllPersonFromNameDobWithPartnerChildren(
        String name,
        LocalDate dob
    ) {
        return personRepository
            .findAllByNameAndDateOfBirth(name, dob)
            .stream()
            .map(e -> PersonDbMapper.mapDto(e, 2))
            .collect(Collectors.toSet());
    }

    @Transactional
    public PersonDto updatePerson(
        Long externalId,
        String name,
        LocalDate dateOfBirth,
        Map<RelationshipType, Set<Long>> relatedIdsByType
    ) {

        Optional<PersonEntity> mainEntitySet = personRepository.findByExternalId(externalId);

        log.debug("Found Person(s) {}", mainEntitySet);

        PersonEntity mainEntity = null;

        if (mainEntitySet.isEmpty()) {
            log.debug("Found empty person. Constructing new one");
            mainEntity = PersonEntity.builder().externalId(externalId).build();
        } else if (mainEntitySet.isPresent()) {
            mainEntity = mainEntitySet.get();

            if(mainEntity.isDeleted()) {
                return  null;
            }

        } else {
            throw new PinkSystemException(PersonService.Constants.ErrorMsg.NO_DISTINCT_RECORD);
        }

        log.debug("Using {}", mainEntity);
        log.debug("Using Relationships: {}", relatedIdsByType);

        // Collect all related IDs from the map
        Set<Long> allIds = relatedIdsByType.values().stream()
            .flatMap(Set::stream)
            .collect(Collectors.toSet());

        log.debug("All IDs: {}", allIds);

        // Fetch all related persons from DB
        Set<PersonEntity> foundPersons = personRepository.findByExternalIdIn(allIds);

        log.debug("Found Person(s):\n {}", foundPersons);

        // Add missing related persons as new entities (not saved yet)
        Set<PersonEntity> allPersons = new HashSet<>(foundPersons);
        for (Long id : allIds) {

            boolean exists
                = allPersons.stream()
                .anyMatch(p -> Objects.equals(p.getExternalId(), id));

            if (!exists) {
                log.debug("Did Not Find Person with id {}. Will add.", id);
                allPersons.add(PersonEntity.builder().externalId(id).build());
            }
        }

        // Update mainEntity's name and DOB if provided
        if (!ObjectUtils.isEmpty(name)) {
            log.debug("Updated Person with name {}", name);
            mainEntity.setName(name);
        } else {
            log.debug("Not Updating name");
        }
        if (dateOfBirth != null) {
            log.debug("Updated Person with dateOfBirth {}", dateOfBirth);
            mainEntity.setDateOfBirth(dateOfBirth);
        } else {
            log.debug("Not Updating dateOfBirth");
        }

        // Add main entity to all persons for saving
        allPersons.add(mainEntity);

        // Save all persons, get managed entities back
        List<PersonEntity> savedPersons = personRepository.saveAll(new HashSet<>(allPersons));
        Map<Long, PersonEntity> personById
            = savedPersons
                .stream()
                .collect(Collectors.toMap(PersonEntity::getExternalId, Function.identity()));

        // Add relationships using managed entities only, for all relationship types dynamically
        for (Map.Entry<RelationshipType, Set<Long>> entry : relatedIdsByType.entrySet()) {

            RelationshipType relType = entry.getKey();
            Set<Long> ids = entry.getValue();

            log.debug("Processing Relations {} with {}", relType, ids);

            if (ids == null) {
                continue;
            }

            for (Long id : ids) {
                PersonEntity related = personById.get(id);
                if (related == null) {
                    continue;
                };

                // Add relationship with proper inverse
                mainEntity.addRelationship(related, relType, relType.getInverse());
            }
        }

        // Save relationships (managed entities only)
        //personRelationshipDao.saveAll(new HashSet<>(mainEntity.getRelationships()));

        // Map to DTO without relationships first
        PersonDto mainDto = PersonDbMapper.mapDtoNoRel(mainEntity);

        // Add relationship DTOs to main DTO dynamically, using inverse relationship
        relatedIdsByType.forEach((relType, ids) -> {
            if (ids == null) return;

            ids.stream()
                .map(personById::get)
                .filter(Objects::nonNull)
                .map(PersonDbMapper::mapDtoNoRel)
                .forEach(dto -> mainDto.addRelationship(relType, dto));
        });

        log.debug("Updated Entity {}", mainEntity.prettyPrint());

        return mainDto;
    }


    @Transactional
    public void deleteAll() {
        log.info("Deleting all PersonEntity records");
        personRepository.deleteAll();
        log.debug("All PersonEntity records deleted");
    }

    @Transactional
    public PersonEntity save(PersonEntity main) {
        log.info("Saving PersonEntity with externalId={}", main.getExternalId());

        // Check for invalid relationships
        List<PersonRelationshipEntity> invalidRels = main.getRelationships().stream()
            .filter(rel -> rel.getPerson() == null || rel.getPerson().getInternalId() == null
                || rel.getRelatedPerson() == null || rel.getRelatedPerson().getInternalId() == null)
            .toList();

        if (!invalidRels.isEmpty()) {
            String errMsg = String.format(
                "Found %d invalid relationships without proper internal IDs: %s",
                invalidRels.size(),
                invalidRels.stream().map(Object::toString).collect(Collectors.joining(", "))
            );
            log.error(errMsg);
            throw new PinkSystemException(errMsg);
        }

        // All relationships valid — save them first
        personRelationshipDao.saveAll(main.getRelationships());
        log.info("Saved {} relationships", main.getRelationships().size());

        PersonEntity saved = personRepository.save(main);
        log.info("Saved PersonEntity: internalId={}, externalId={}", saved.getInternalId(), saved.getExternalId());
        log.debug("Saved PersonEntity:\n{}", saved.prettyPrint());

        return saved;
    }

    @Transactional
    public PersonEntity saveAndFlush(PersonEntity toSaveAndFlush) {
        log.info("Saving and flushing Person {}", this);
        PersonEntity saved = save(toSaveAndFlush);
        personRepository.flush();
        log.debug("Saved and flushed Person");
        return saved;
    }

    @Transactional
    public void flush() {
        log.info("Flushing Person {}", this);
        personRepository.flush();
        log.debug("Flushed Person");
    }

    @Transactional
    public List<PersonEntity> saveAll(List<PersonEntity> persons) {
        log.info("Saving batch of {} PersonEntity records", persons.size());

        // Collect all invalid relationships across all persons
        List<PersonRelationshipEntity> invalidRels = persons.stream()
            .flatMap(p -> p.getRelationships().stream())
            .filter(rel -> rel.getPerson() == null || rel.getPerson().getInternalId() == null
                || rel.getRelatedPerson() == null || rel.getRelatedPerson().getInternalId() == null)
            .toList();

        if (!invalidRels.isEmpty()) {
            String errMsg = String.format(
                "Found %d invalid relationships without proper internal IDs in batch: %s",
                invalidRels.size(),
                invalidRels.stream().map(Object::toString).collect(Collectors.joining(", "))
            );
            log.error(errMsg);
            throw new IllegalStateException(errMsg);
        }

        // Save all relationships first
        Set<PersonRelationshipEntity> allRelationships = persons.stream()
            .flatMap(p -> p.getRelationships().stream())
            .collect(Collectors.toSet());

        personRelationshipDao.saveAll(allRelationships);
        log.info("Saved {} relationships", allRelationships.size());

        // Save all persons
        List<PersonEntity> savedPersons = personRepository.saveAll(persons);
        log.info("Saved {} PersonEntity records", savedPersons.size());

        return savedPersons;
    }

    @Transactional(readOnly = true)
    public Set<PersonEntity> findAllByNameAndDateOfBirth(String name, LocalDate dob) {
        return personRepository.findAllByNameAndDateOfBirth(name, dob);
    }

    @Transactional(readOnly = true)
    public Optional<PersonEntity> findById(Long internalId) {
        log.info("Finding PersonEntity internalId={}", internalId);
        var toReturn = personRepository.findById(internalId);
        log.debug("Found PersonEntity: {}", toReturn);
        return toReturn;
    }

    @Transactional
    public void delete(PersonEntity saved) {
        log.info("Deleting PersonEntity: {}", saved);
        personRepository.delete(saved);
        log.debug("Deleted PersonEntity");
    }

    @Transactional(readOnly = true)
    public Optional<PersonEntity> findByExternalIdEntity(Long externalId) {
        log.debug("Finding PersonEntity By External ID={}", externalId);
        var toReturn = personRepository.findByExternalId(externalId);
        log.debug("Found PersonEntity: {}", toReturn);
        return toReturn;
    }
    @Transactional(readOnly = true)
    public Optional<PersonDto> findByExternalId(Long externalId) {
        log.debug("Finding PersonEntity By External ID={}", externalId);
        var toReturn = personRepository.findByExternalId(externalId);
        log.debug("Found PersonEntity: {}", toReturn);
        return toReturn.map(e -> PersonDbMapper.mapDto(e, 3));
    }

    @Transactional
    public void softDeletePersons(Set<Long> toDelete) {
        log.info("Soft Deleting PersonEntities: {}", toDelete);
        personRepository.updateDeleteByExternalId(toDelete, true);
        log.debug("Soft Deleted PersonEntities");
    }

    @Transactional(readOnly = true)
    public Set<PersonEntity> findAll() {
        return new HashSet<>(personRepository.findAll());
    }
}
