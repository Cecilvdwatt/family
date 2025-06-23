package com.pink.family.assignment.database.dao;

import com.pink.family.assignment.database.entity.PersonEntity;
import com.pink.family.assignment.database.entity.enums.RelationshipType;
import com.pink.family.assignment.database.mapper.PersonDbMapper;
import com.pink.family.assignment.database.repository.PersonRelationshipRepository;
import com.pink.family.assignment.database.repository.PersonRepository;
import com.pink.family.assignment.dto.PersonDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.stream.Stream;


/**
 * The data access object for a PersonEntity entity.
 * This object returns detached {@link PersonDto} objects in order to ensure that we do not expose
 * attached objects outside and potentially run into issues such as no transaction or accidentally
 * make modifications to the underlying data.
 */
@Service
@RequiredArgsConstructor
public class PersonDao {

    private final PersonRepository personRepository;
    private final PersonRelationshipRepository personRelationshipRepository;

    /**
     * Attempt to find a PersonEntity record in the database and any associated Children and Partners using an external
     * Id.
     *
     * @return An empty optional if the person could not be found. An optional with a PersonEntity record containing on
     * their children and partners.
     */
    @Transactional(readOnly = true)
    public Optional<PersonDto> findPersonFromExternalIdWithPartnerChildren(Long externalId) {
        // Fetch person eagerly with relationships to avoid lazy loading
        Optional<PersonEntity> personEntityOpt = personRepository.findByExternalId(externalId);
        if (personEntityOpt.isEmpty()) {
            return Optional.empty();
        }

        PersonEntity rootPerson = personEntityOpt.get();

        // Filter to include PARTNER and CHILD relationships (i.e. the root's partners and children)
        Set<RelationshipType> filter = Set.of(RelationshipType.PARTNER, RelationshipType.PARENT);

        // Map the entity graph to DTO recursively using the filter
        PersonDto dto = PersonDbMapper.mapDto(rootPerson, filter);

        return Optional.of(dto);
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
    )
    {
        return
            personRepository
                .findAllByNameAndDateOfBirth(name, dob)
                .stream()
                .map(
                    person ->
                        PersonDbMapper.mapDto(
                            person,
                            Set.of(RelationshipType.PARTNER, RelationshipType.CHILD)
                        )
                )
                .collect(Collectors.toSet());
    }

    @Transactional
    public PersonDto updatePerson(
        Long externalId,
        String name,
        LocalDate dateOfBirth,
        Set<Long> parentsId,
        Set<Long> partnerIds,
        Set<Long> childrenIds)
    {

        // Find main entity or create new with externalId
        PersonEntity mainEntity = personRepository.findByExternalId(externalId)
            .orElse(PersonEntity.builder().externalId(externalId).build());

        // Collect all related IDs
        Set<Long> allIds = Stream.of(
                parentsId.stream(),
                partnerIds.stream(),
                childrenIds.stream()
            )
            .flatMap(Function.identity())
            .collect(Collectors.toSet());

        // Fetch all related persons from DB
        Set<PersonEntity> foundPersons = personRepository.findByExternalIdIn(allIds);

        // Add missing related persons as new entities (not saved yet)
        Set<PersonEntity> allPersons = new HashSet<>(foundPersons);
        for (Long id : allIds) {
            boolean exists = allPersons.stream()
                .anyMatch(p -> Objects.equals(p.getExternalId(), id));
            if (!exists) {
                allPersons.add(PersonEntity.builder().externalId(id).build());
            }
        }

        // Add main entity to all persons for saving
        allPersons.add(mainEntity);

        // Save all persons, get managed entities back
        List<PersonEntity> savedPersons = personRepository.saveAll(allPersons);
        Map<Long, PersonEntity> personById = savedPersons.stream()
            .collect(Collectors.toMap(PersonEntity::getExternalId, Function.identity()));

        // Update mainEntity reference with managed entity
        mainEntity = personById.get(externalId);

        // Update mainEntity's name and DOB if provided
        if (!ObjectUtils.isEmpty(name)) {
            mainEntity.setName(name);
        }
        if (dateOfBirth != null) {
            mainEntity.setDateOfBirth(dateOfBirth);
        }

        // Clear existing relationships to avoid duplicates (optional, depends on your logic)
        mainEntity.getRelationships().clear();

        // Add relationships using managed entities only
        for (Long id : allIds) {
            PersonEntity related = personById.get(id);
            if (related == null) {
                continue;
            }

            if (parentsId.contains(id)) {
                mainEntity.addRelationship(related, RelationshipType.CHILD, RelationshipType.PARENT);
            } else if (partnerIds.contains(id)) {
                mainEntity.addRelationship(related, RelationshipType.PARTNER, RelationshipType.PARTNER);
            } else if (childrenIds.contains(id)) {
                mainEntity.addRelationship(related, RelationshipType.PARENT, RelationshipType.CHILD);
            }
        }

        // Save relationships (managed entities only)
        personRelationshipRepository.saveAll(mainEntity.getRelationships());

        // Map to DTO without relationships first
        PersonDto mainDto = PersonDbMapper.mapDtoNoRel(mainEntity);

        // Add relationship DTOs to main DTO
        Map<RelationshipType, Set<Long>> relationshipsToAdd
            = Map.of(
                RelationshipType.PARTNER, partnerIds,
                RelationshipType.PARENT, childrenIds,
                RelationshipType.CHILD, parentsId
        );

        relationshipsToAdd.forEach((relType, ids) -> {
            ids.stream()
                .map(personById::get)
                .filter(Objects::nonNull)
                .map(PersonDbMapper::mapDtoNoRel)
                .forEach(dto -> mainDto.addRelationship(relType.getInverse(), relType, dto));
        });

        return mainDto;
    }


}
