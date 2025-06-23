package com.pink.family.assignment.api.controller;

import com.pink.family.api.rest.server.model.FullPerson;
import com.pink.family.api.rest.server.model.PersonDetailsRequest;
import com.pink.family.api.rest.server.model.SpecificPersonCheckRequest;
import com.pink.family.api.rest.server.reference.V1Api;
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
public class PersonsController implements V1Api {

    private final PersonService personService;
    private final LoggingService loggingService;

    @Override
    public ResponseEntity<Void> v1PeopleCheckExistingPersonPost(SpecificPersonCheckRequest specificPersonCheckRequest) {

        loggingService.setRequestId(specificPersonCheckRequest.getRequestId());

        String result = "";
        // we have a external id to try and use
        if(!ObjectUtils.isEmpty(specificPersonCheckRequest.getId())) {
            result = personService.hasPartnerAndChildrenExternalId(specificPersonCheckRequest.getId());
        } else if(ObjectUtils.isEmpty(specificPersonCheckRequest.getName()) && ObjectUtils.isEmpty(specificPersonCheckRequest.getDateOfBirth())) {
            throw new PinkApiException("Request is missing ID, Name and Date of Birth", 444);
        }

        // We were unable to find a matching record using the External ID try the
        // name and dob next.
        if(result.equals(PersonService.Constants.ErrorMsg.NO_RECORD)) {
            result = personService.hasPartnerAndChildrenNameSurnameDob(
                specificPersonCheckRequest.getName(),
                specificPersonCheckRequest.getDateOfBirth()
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

    @Override
    public ResponseEntity<FullPerson> v1PeoplePost(PersonDetailsRequest personDetailsRequest) {
        return null;
    }
}
