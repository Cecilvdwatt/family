package com.pink.family.assignment.api.controller;

import com.pink.family.api.rest.server.model.FullPerson;
import com.pink.family.api.rest.server.model.PersonDetailsRequest;
import com.pink.family.api.rest.server.model.Relation;
import com.pink.family.api.rest.server.model.SpecificPersonCheckRequest;
import com.pink.family.api.rest.server.reference.V1Api;
import com.pink.family.assignment.api.exception.PinkApiException;
import com.pink.family.assignment.api.mapper.PersonApiMapper;
import com.pink.family.assignment.dto.PersonDto;
import com.pink.family.assignment.service.LoggingService;
import com.pink.family.assignment.service.PersonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PersonController implements V1Api {

    private final PersonService personService;
    private final LoggingService loggingService;

    @Override
    public ResponseEntity<Void> v1PeopleCheckExistingPersonPost(SpecificPersonCheckRequest specificPersonCheckRequest) {

        log.debug("""
            #############################################
            ######## v1PeopleCheckExistingPersonPost
            #############################################
            """);
        loggingService.setRequestId(specificPersonCheckRequest.getRequestId());

        Optional<String> result;

        if (!ObjectUtils.isEmpty(specificPersonCheckRequest.getId())) {
            result = personService.hasPartnerAndChildrenExternalId(specificPersonCheckRequest.getId());
            log.debug("Result From ID: {}", result.map(String::valueOf).orElse("N/A"));
        } else if (ObjectUtils.isEmpty(specificPersonCheckRequest.getName()) || ObjectUtils.isEmpty(specificPersonCheckRequest.getDateOfBirth())) {
            throw new PinkApiException("Request is missing ID, Name or Date of Birth", 444);
        } else {
            log.debug("No ID, but fallback information available.");
            result = personService.hasPartnerAndChildrenNameSurnameDob(
                specificPersonCheckRequest.getName(),
                specificPersonCheckRequest.getDateOfBirth()
            );
        }

        if (result.isEmpty() || result.get().isEmpty()) {
            return ResponseEntity.ok().build();
        } else {
            throw new PinkApiException(result.get(), 444);
        }
    }

    @Override
    public ResponseEntity<Void> v1PeopleDelete(List<Long> requestBody) {
        personService.softDeletePersons(new HashSet<>(requestBody));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<FullPerson> v1PeoplePost(PersonDetailsRequest personDetailsRequest) {
        PersonDto updated =
            personService.retrieveAndUpdate(
                personDetailsRequest.getId(),
                personDetailsRequest.getName(),
                personDetailsRequest.getBirthDate(),
                Stream
                    .of(
                        personDetailsRequest.getParent1(),
                        personDetailsRequest.getParent2())
                    .filter(Objects::nonNull)
                    .map(Relation::getId)
                    .collect(Collectors.toSet()),
                Stream
                    .of(personDetailsRequest.getPartner())
                    .filter(Objects::nonNull)
                    .map(Relation::getId)
                    .collect(Collectors.toSet()),
                CollectionUtils.isEmpty(personDetailsRequest.getChildren())
                ?
                    Set.of()
                    : personDetailsRequest.getChildren().stream().map(Relation::getId).collect(Collectors.toSet())
            );

        return ResponseEntity.ok(PersonApiMapper.mapToApi(updated, personDetailsRequest));
    }
}
