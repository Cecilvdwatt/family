package com.pink.family.assignment.api.controller;

import com.pink.family.api.rest.server.model.Person;
import com.pink.family.api.rest.server.reference.PersonsApi;
import org.springframework.http.ResponseEntity;

/**
 * The controller for the Persons API methods i.e. /persons/**
 */
public class PersonsController implements PersonsApi {

    @Override
    public ResponseEntity<Void> personsCheckPartnerChildrenPost(Person person) {
        return null;
    }
}
