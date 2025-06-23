package com.pink.family.assignment.service;

import com.pink.family.assignment.database.dao.PersonDao;
import com.pink.family.assignment.database.entity.enums.RelationshipType;
import com.pink.family.assignment.dto.PersonDto;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;
import java.util.Set;


/**
 * Service class for performing PersonEntity related operations.
 * This class contains the business logic around a PersonEntity entity, and is responsible for retrieving the
 * person data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonDao personDao;

    /**
     * Using the externalId number lookup a person record and return true if:
     * <br />
     * 1. A record exists for that person
     * 2. The person has a partner
     * 3. Has exactly 3 children and all 3 have that same partner listed as mother or father
     * 4. At least one of those children is under 18
     *
     * @param externalId
     * The external ID (passport, national ID etc.)
     * @return
     * An empty string is no issues were encountered.
     * A string containing a description of the failure cause if the check failed.
     */
    public String hasPartnerAndChildrenExternalId(Long externalId) {

        // Fetch the person by external Id, with only their child and partner relationships
        Optional<PersonDto> optMainPerson = personDao.findPersonFromExternalIdWithPartnerChildren(externalId);

        if(optMainPerson.isEmpty()) {
            return Constants.ErrorMsg.NO_RECORD;
        }
        // if no person was found we return false.
        return this.hasPartnerAndChildren(optMainPerson.get());

    }

    /**
     *
     * Using the External ID lookup a person record and return true if:
     * <br />
     * 1. A record exists for that person
     * 2. The person has a partner
     * 3. Has exactly 3 children and all 3 have that same partner listed as mother or father
     * 4. At least one of those children is under 18
     *
     * @param name
     * Name of the person to check
     * @param dateOfBirth
     * Date of birth of the person to check
     * @return
     * An empty string is no issues were encountered.
     * A string containing a description of the failure cause if the check failed.
     */
    public String hasPartnerAndChildrenNameSurnameDob(String name, LocalDate dateOfBirth) {

        Set<PersonDto> persons = personDao.findAllPersonFromNameDobWithPartnerChildren(name, dateOfBirth);

        if(CollectionUtils.isEmpty(persons)) {
            return Constants.ErrorMsg.NO_RECORD;
        }
        // Although very unlikely it is possible for two people to have the
        // same name and date of birth
        if(persons.size() > 1) {
            // usually you'd return a 400 bad request or maybe even a 409 conflict
            // with the scope of the assignment I'll just log a waning and return a false.
            log.warn("Duplicate Matches for {} {}", name, dateOfBirth);
            return "Duplicate Matches for %s %s".formatted(name, dateOfBirth);
        }

        // the orElseThrow here is just for the compiler, we do a check above, so we know there is at least
        // 1 record.
        return hasPartnerAndChildren(persons.stream().findFirst().orElseThrow());

    }


    /**
     * For the provided person record check that:
     * 1. The person has a partner
     * 2. Has exactly 3 children and all 3 have that same partner listed as mother or father
     * 3. At least one of those children is under 18
     * @param mainPerson
     * The person record to check.
     * @return
     * An empty string is no issues were encountered.
     * A string containing a description of the failure cause if the check failed.
     *
     */
    private String hasPartnerAndChildren(@Nullable PersonDto mainPerson) {
        if (mainPerson == null) {
            return Constants.ErrorMsg.EMPTY_PERSON;
        }

        Set<PersonDto> partners = mainPerson.getRelations(RelationshipType.PARTNER);
        if (partners.isEmpty()) {
            return Constants.ErrorMsg.NO_PARTNER;
        }

        Set<PersonDto> children = mainPerson.getRelations(RelationshipType.PARENT);
        if (children.size() != 3) {
            return Constants.ErrorMsg.NUM_CHILDREN;
        }

        boolean matchFound
            = partners
                .stream()
                .anyMatch(
                    partner -> {
                        // Count how many children share this partner as a parent via inverse CHILD relationship
                        long matchingChildren
                            = children
                                .stream()
                                .filter(p -> p.getInternalId().equals(partner.getInternalId()))
                                .count();

                        if (matchingChildren != 3) {
                            return false;
                        }

                    // Check if any child is under 18
                    return
                        children
                            .stream()
                            .anyMatch(
                                child ->
                                    child.getDateOfBirth() != null &&
                                    Period.between(child.getDateOfBirth(), LocalDate.now()).getYears() < 18);
                });

        return matchFound ? Strings.EMPTY : Constants.ErrorMsg.NO_SHARED_CHILDREN;
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
        return personDao.updatePerson(externalId, name, dateOfBirth, parentsId, partnerIds, childrenIds);
    }

    public static class Constants {

        public static class ErrorMsg {

            public static final String NO_RECORD = "Could not find Person.";
            public static final String NO_SHARED_CHILDREN = "Does not share 3 children with a partner of which at least 1 is under 18";
            public static final String NUM_CHILDREN = "Does not have exactly 3 children";
            public static final String NO_PARTNER = "Has No Partner";
            public static final String EMPTY_PERSON = "Null PersonEntity Record.";
        }
    }

}
