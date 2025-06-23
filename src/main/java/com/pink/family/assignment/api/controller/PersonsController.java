package com.pink.family.assignment.api.controller;

import com.pink.family.api.rest.server.model.PersonRequest;
import com.pink.family.api.rest.server.reference.PersonsApi;
import com.pink.family.assignment.api.exception.PinkApiException;
import com.pink.family.assignment.service.LoggingService;
import com.pink.family.assignment.service.PersonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RestController;

/**
 * The controller for the Persons API methods i.e. /persons/**
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class PersonsController implements PersonsApi {

    private final PersonService personService;
    private final LoggingService loggingService;

    @Override
    public ResponseEntity<Void> personsCheckPartnerChildrenPost(PersonRequest person) {

        loggingService.setRequestId(person.getRequestId());

        String result = "";
        // we have a bsn to try and use
        if(!ObjectUtils.isEmpty(person.getBsn())) {
            result = personService.hasPartnerAndChildrenBsn(person.getBsn());
        } else if(ObjectUtils.isEmpty(person.getName()) && ObjectUtils.isEmpty(person.getSurname()) && ObjectUtils.isEmpty(person.getDateOfBirth())) {
            // Design note:
            ///////////////
            // could have created multiple API endpoints that receive specific data
            // but the requirement was to receive a Person record. As such decided to go for a single API
            // that does different fallback lookups.
            throw new PinkApiException("Request is missing BSN, Name, Surname and Date of Birth", 444);
        }

        // We were unable to find a matching record using the Bsn try the
        // name, surname and dob next.
        if(result.equals(PersonService.Constants.ErrorMsg.NO_RECORD)) {
            result = personService.hasPartnerAndChildrenNameSurnameDob(
                person.getName(),
                person.getSurname(),
                person.getDateOfBirth()
            );
        }

        // No issues encountered.
        if(ObjectUtils.isEmpty(result)) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            // 444 isn't a standard http response code.
            throw new PinkApiException(result, 444);
        }

    }
}
