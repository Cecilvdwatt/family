package com.pink.family.assignment.database.dao;

import com.pink.family.assignment.database.entity.PersonRelationshipEntity;
import com.pink.family.assignment.database.entity.id.PersonRelationshipId;
import com.pink.family.assignment.database.repository.PersonRelationshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonRelationshipDao {

    private final PersonRelationshipRepository personRelationshipRepository;

    @Transactional
    public List<PersonRelationshipEntity> saveAll(Set<PersonRelationshipEntity> relationships) {
        log.debug("Saving batch of {} PersonRelationshipEntity records", relationships.size());

        // Validate all relationships
        List<PersonRelationshipEntity> invalidRels = relationships.stream()
            .filter(rel -> rel.getPerson() == null || rel.getPerson().getInternalId() == null
                || rel.getRelatedPerson() == null || rel.getRelatedPerson().getInternalId() == null)
            .collect(Collectors.toList());

        if (!invalidRels.isEmpty()) {
            String errMsg = String.format(
                "Found %d invalid relationships without proper internal IDs: %s",
                invalidRels.size(),
                invalidRels.stream().map(Object::toString).collect(Collectors.joining(", "))
            );
            log.error(errMsg);
            throw new IllegalStateException(errMsg);
        }

        List<PersonRelationshipEntity> uniqueRelationships = relationships.stream()
            .filter(distinctByKey(PersonRelationshipEntity::getId))
            .collect(Collectors.toList());

        // Save all valid relationships
        List<PersonRelationshipEntity> saved = personRelationshipRepository.saveAll(uniqueRelationships);

        log.debug("Saved {} PersonRelationshipEntity records", saved.size());
        return saved;
    }

    @Transactional
    public void deleteAll() {
        log.debug("Deleting all PersonRelationshipEntity records");
        personRelationshipRepository.deleteAll();
        log.debug("Deleted all PersonRelationshipEntity records");
    }

    @Transactional
    public void flush() {
        log.debug("Flushing all PersonRelationshipEntity records");
        personRelationshipRepository.flush();
        log.debug("Flushed all PersonRelationshipEntity records");
    }

    @Transactional
    public void deleteById(PersonRelationshipId relId) {
        log.debug("Deleting PersonRelationshipEntity record with id {}", relId);
        personRelationshipRepository.deleteById(relId);
        log.debug("Deleted PersonRelationshipEntity record with id {}", relId);
    }

    public Optional<PersonRelationshipEntity> findById(PersonRelationshipId relId) {
        log.debug("Finding PersonRelationshipEntity record with id {}", relId);
        var toReturn = personRelationshipRepository.findById(relId);
        log.debug("Found PersonRelationshipEntity record with id {}", relId);
        return toReturn;
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }
}
