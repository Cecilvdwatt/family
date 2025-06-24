package com.pink.family.assignment.service;

import com.pink.family.assignment.api.exception.PinkDebugException;
import com.pink.family.assignment.api.exception.PinkSystemException;
import com.pink.family.assignment.database.dao.PersonDao;
import com.pink.family.assignment.database.entity.enums.RelationshipType;
import com.pink.family.assignment.dto.PersonDto;
import com.pink.family.assignment.util.MaskUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PersonService {

    private final PersonDao personDao;

    public PersonService(PersonDao personDao) {
        this.personDao = personDao;
    }

    public void softDeletePersons(Set<Long> toDelete) {
        personDao.softDeletePersons(toDelete);
    }

    public static class Constants {
        public static class ErrorMsg {
            public static final String NO_RECORD = "No record found";
            public static final String NO_PARTNER = "Does not have a partner";
            public static final String NO_SHARED_CHILDREN = "No shared children";
            public static final String NO_UNDERAGE_CHILD = "Does not have a child under 18";
            public static final String NOT_EXACTLY_3_CHILDREN = "Does not have exactly 3 children";
            public static final String NO_DISTINCT_RECORD = "Could not find a single matching record";
        }
    }

    /**
     * Check by name + DOB if person has a partner and exactly 3 children
     * shared with that partner, and at least one child under 18.
     */
    public Optional<String> hasPartnerAndChildrenNameSurnameDob(String name, LocalDate dob) {
        Set<PersonDto> persons = personDao.findAllPersonFromNameDobWithPartnerChildren(name, dob);

        if(persons.isEmpty()) {
            log.debug("No person found with name {} and dob {}", name, dob, PinkDebugException.inst());
            return Optional.of(Constants.ErrorMsg.NO_RECORD);
        }

        if(persons.size() > 1) {
            log.debug("Found Multiple of {} {}", name, dob, PinkDebugException.inst());
            return Optional.of(Constants.ErrorMsg.NO_DISTINCT_RECORD);
        }

        PersonDto person = persons.iterator().next();
        if(person.isDeleted()) {
            return Optional.empty();
        }

        String error = validatePersonPartnerAndChildren(person);
        if (error.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(error);
        }
    }

    /**
     * Check by external ID if person has a partner and exactly 3 children
     * shared with that partner, and at least one child under 18.
     *
     * @return
     * An empty optional if no issues were encountered. An optional containing an error if an issues was encountered.
     */
    public Optional<String> hasPartnerAndChildrenExternalId(Long externalId) {
        Optional<PersonDto> optPerson = personDao.findPersonFromExternalId(externalId, 3);

        if(optPerson.isEmpty()) {
            log.debug("No person found for external ID {}", MaskUtil.maskExternalId(externalId), PinkDebugException.inst());
            return Optional.of(Constants.ErrorMsg.NO_RECORD);
        }

        PersonDto person = optPerson.get();
        if(person.isDeleted()) {
            return Optional.empty();
        }

        String error = validatePersonPartnerAndChildren(person);
        if (error.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(error);
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
            return Constants.ErrorMsg.NOT_EXACTLY_3_CHILDREN;
        }

        if (partners == null || partners.isEmpty()) {
            log.debug("Person {} does not any partners", person, PinkDebugException.inst());
            return Constants.ErrorMsg.NO_PARTNER;
        }

        boolean hasUnder18 = children.stream()
            .anyMatch(child -> child.getDateOfBirth() != null &&
                child.getDateOfBirth().isAfter(LocalDate.now().minusYears(18)));

        if (!hasUnder18) {
            log.debug("All children of {} are 18 or older", person, PinkDebugException.inst());
            return Constants.ErrorMsg.NO_UNDERAGE_CHILD;
        }

        // Find partners shared by all children (excluding the main person)
        Set<PersonDto> sharedPartners = new HashSet<>(partners);

        for (PersonDto child : children) {
            Set<PersonDto> childParents = child.getRelations(RelationshipType.CHILD);

            // We expect at least the primary parent to be there.
            if (childParents == null || childParents.isEmpty()) {
                log.error("Child {} has no parent records. This should not be possible, likely a mapping or data issue", child);
                throw new PinkSystemException("CHILD " + child.getName() + " has no parent records");
            }

            // Remove main person, only keep other parents/partners
            Set<PersonDto> otherParents = new HashSet<>(childParents);
            otherParents.remove(person);

            if (otherParents.isEmpty()) {
                // child has no other parent besides main person
                log.debug("Child {} haas no other parents", child, PinkDebugException.inst());
                return Constants.ErrorMsg.NO_SHARED_CHILDREN;
            }

            // intersecting, checking if the children is shared between the main and any of their partners.
            sharedPartners.retainAll(otherParents);

            if (sharedPartners.isEmpty()) {
                log.debug("No children are shared by {}", person, PinkDebugException.inst());
                return Constants.ErrorMsg.NO_SHARED_CHILDREN;
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
    )
    {
        Map<RelationshipType, Set<Long>> relations = new HashMap<>();
        relations.put(RelationshipType.CHILD, parentsId);
        relations.put(RelationshipType.PARENT, childrenIds);
        relations.put(RelationshipType.PARTNER, partnerIds);

        return personDao.updatePerson(externalId, name, dateOfBirth, relations);
    }
}
