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
    public String hasPartnerAndChildrenExternalId(String externalId) {

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

        // If the main person object is null, we can’t proceed
        if (mainPerson == null) {
            return Constants.ErrorMsg.EMPTY_PERSON;
        }

        // Get all partners of the main person
        // Note: we assume a person may have multiple partners (bigamy allowed)
        Set<PersonDto> partners = mainPerson.getRelationships().get(RelationshipType.PARTNER);

        // If no partner exists, no match is possible
        if (partners.isEmpty()) {
            return Constants.ErrorMsg.NO_PARTNER;
        }

        // Get all children from the main person’s relationships
        Set<PersonDto> children = mainPerson.getRelationships().get(RelationshipType.CHILD);

        // The spec requires exactly 3 children — no more, no less
        if (children.size() != 3) {
            return Constants.ErrorMsg.NO_CHILDREN;
        }

        // Now we know:
        // * Main person has at least one partner
        // * Main person has exactly 3 children

        // We need to confirm:
        // 1. All 3 children have the *same partner* as the other parent
        // 2. At least one of those children is under 18

        Set<RelationshipType> parentTypes = RelationshipType.getParentTypes();

        for (PersonDto partner : partners) {
            int matchingChildCount = 0;
            boolean hasChildUnder18 = false;

            for (PersonDto child : children) {
                boolean matchedParent = false;

                // Check if this partner is listed as a parent of the child
                for (RelationshipType parentType : parentTypes) {
                    boolean hasParent = child.getRelationships(parentType)
                        .stream()
                        .anyMatch(p -> p.getId().equals(partner.getId()));

                    if (hasParent) {
                        matchedParent = true;
                        break;
                    }
                }

                if (matchedParent) {
                    matchingChildCount++;

                    // Check if the child is under 18
                    if (!hasChildUnder18 && child.getDateOfBirth() != null) {
                        int age = Period.between(child.getDateOfBirth(), LocalDate.now()).getYears();
                        if (age < 18) {
                            hasChildUnder18 = true;
                        }
                    }
                }
            }

            // If all 3 children share this partner as a parent AND at least one is under 18, we have a match
            if (matchingChildCount == 3 && hasChildUnder18) {
                return Strings.EMPTY;
            }
        }

        // No partner found that satisfies both conditions
        return Constants.ErrorMsg.NO_SHARED_CHILDREN;
    }


    public static class Constants {

        public static class ErrorMsg {

            public static final String NO_RECORD = "Could not find Person.";
            public static final String NO_SHARED_CHILDREN = "Does not share 3 children with a partner of which at least 1 is under 18";
            public static final String NO_CHILDREN = "Does not have exactly 3 children";
            public static final String NO_PARTNER = "Has No Partner";
            public static final String EMPTY_PERSON = "Null PersonEntity Record.";
        }
    }

}
