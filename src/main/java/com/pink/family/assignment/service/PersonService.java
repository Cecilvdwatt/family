package com.pink.family.assignment.service;

import com.pink.family.assignment.api.exception.PinkDebugException;
import com.pink.family.assignment.api.exception.PinkSystemException;
import com.pink.family.assignment.constants.ErrorMessages;
import com.pink.family.assignment.constants.MeterKeys;
import com.pink.family.assignment.database.dao.PersonDao;
import com.pink.family.assignment.database.entity.enums.RelationshipType;
import com.pink.family.assignment.dto.PersonDto;
import com.pink.family.assignment.util.MaskUtil;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonDao personDao;
    private final MicrometerService micrometerService;


    public void softDeletePersons(Set<Long> toDelete) {
        personDao.softDeletePersons(toDelete);
    }

    /**
     * Check by name + DOB if person has a partner and exactly 3 children shared with that partner, and at least one
     * child under 18.
     */
    public Optional<String> hasPartnerAndChildrenNameSurnameDob(String name, LocalDate dob) {

        Timer.Sample timer = micrometerService.getSample();
        try {
            Set<PersonDto> persons = personDao.findAllPersonFromNameDobWithPartnerChildren(name, dob);

            if (persons.isEmpty()) {
                log.debug("No person found with name {} and dob {}", name, dob, PinkDebugException.inst());
                return Optional.of(ErrorMessages.NO_RECORD);
            }

            if (persons.size() > 1) {
                log.debug("Found Multiple of {} {}", name, dob, PinkDebugException.inst());
                return Optional.of(ErrorMessages.NO_DISTINCT_RECORD);
            }

            PersonDto person = persons.iterator().next();
            if (person.isDeleted()) {
                return Optional.empty();
            }

            String error = validatePersonPartnerAndChildren(person);
            if (error.isEmpty()) {
                return Optional.empty();
            } else {
                return Optional.of(error);
            }
        } finally {
            micrometerService.time(MeterKeys.TIME_CHECK_PARTNER_CHILDREN_FALLBACK, timer);
            micrometerService.increment(MeterKeys.COUNT_CHECK_PARTNER_CHILDREN_FALLBACK);
        }
    }

    /**
     * Check by external ID if person has a partner and exactly 3 children shared with that partner, and at least one
     * child under 18.
     *
     * @return An empty optional if no issues were encountered. An optional containing an error if an issues was
     * encountered.
     */
    public Optional<String> hasPartnerAndChildrenExternalId(Long externalId) {

        Timer.Sample timer = micrometerService.getSample();
        try {
            Optional<PersonDto> optPerson = personDao.findPersonFromExternalId(externalId, 3);

            if (optPerson.isEmpty()) {
                log.debug("No person found for external ID {}",
                    MaskUtil.maskExternalId(externalId),
                    PinkDebugException.inst()
                );
                return Optional.of(ErrorMessages.NO_RECORD);
            }

            PersonDto person = optPerson.get();
            if (person.isDeleted()) {
                return Optional.empty();
            }

            String error = validatePersonPartnerAndChildren(person);
            if (error.isEmpty()) {
                return Optional.empty();
            } else {
                return Optional.of(error);
            }
        } finally {
            micrometerService.time(MeterKeys.TIME_CHECK_PARTNER_CHILDREN_ID, timer);
            micrometerService.increment(MeterKeys.COUNT_CHECK_PARTNER_CHILDREN_ID);
        }

    }

    /**
     * Core validation logic used by both methods.
     */
    private String validatePersonPartnerAndChildren(PersonDto person) {

        log.debug("Validating that Person has Partner and shared Children:\n{}", person);

        Set<PersonDto> children = person.getRelations(RelationshipType.PARENT);
        Set<PersonDto> partners = person.getRelations(RelationshipType.PARTNER);

        if (children == null || children.size() != 3) {
            log.debug("Person {} does not have exactly 3 children", person.getName(), PinkDebugException.inst());
            return ErrorMessages.NOT_EXACTLY_3_CHILDREN;
        }

        if (partners == null || partners.isEmpty()) {
            log.debug("Person {} does not any partners", person, PinkDebugException.inst());
            return ErrorMessages.NO_PARTNER;
        }

        boolean hasUnder18 = children.stream()
            .anyMatch(child -> child.getDateOfBirth() != null &&
                child.getDateOfBirth().isAfter(LocalDate.now().minusYears(18)));

        if (!hasUnder18) {
            log.debug("All children of {} are 18 or older", person, PinkDebugException.inst());
            return ErrorMessages.NO_UNDERAGE_CHILD;
        }

        // Find partners shared by all children (excluding the main person)
        Set<PersonDto> sharedPartners = new HashSet<>(partners);

        for (PersonDto child : children) {
            Set<PersonDto> childParents = child.getRelations(RelationshipType.CHILD);

            // We expect at least the primary parent to be there.
            if (childParents == null || childParents.isEmpty()) {
                log.error("Child {} has no parent records. This should not be possible, likely a mapping or data issue",
                    child
                );
                throw new PinkSystemException("CHILD " + child.getName() + " has no parent records");
            }

            // Remove main person, only keep other parents/partners
            Set<PersonDto> otherParents = new HashSet<>(childParents);
            otherParents.remove(person);

            if (otherParents.isEmpty()) {
                // child has no other parent besides main person
                log.debug("Child {} haas no other parents", child, PinkDebugException.inst());
                return ErrorMessages.NO_SHARED_CHILDREN;
            }

            // intersecting, checking if the children is shared between the main and any of their partners.
            sharedPartners.retainAll(otherParents);

            if (sharedPartners.isEmpty()) {
                log.debug("No children are shared by {}", person, PinkDebugException.inst());
                return ErrorMessages.NO_SHARED_CHILDREN;
            }
        }

        return ""; // no error
    }

    public PersonDto retrieveAndUpdate(
        Long externalId,
        String name,
        LocalDate dateOfBirth,
        Set<Long> parentsId,
        Set<Long> partnerIds,
        Set<Long> childrenIds
    ){
        Timer.Sample timer = micrometerService.getSample();
        try {
            Map<RelationshipType, Set<Long>> relations = new HashMap<>();
            relations.put(RelationshipType.CHILD, parentsId);
            relations.put(RelationshipType.PARENT, childrenIds);
            relations.put(RelationshipType.PARTNER, partnerIds);

            return personDao.updatePerson(externalId, name, dateOfBirth, relations);
        } finally {
            micrometerService.time(MeterKeys.TIME_RETRIEVE_AND_UPDATE, timer);
            micrometerService.increment(MeterKeys.COUNT_RETRIEVE_AND_UPDATE);
        }
    }
}
