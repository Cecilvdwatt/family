package com.pink.family.assignment.database.dao;

import com.pink.family.assignment.database.entity.enums.RelationshipType;
import com.pink.family.assignment.database.mapper.PersonMapper;
import com.pink.family.assignment.database.repository.PersonRepository;
import com.pink.family.assignment.dto.PersonDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * The data access object for a PersonEntity entity.
 * This object returns detached {@link PersonDto} objects in order to ensure that we do not expose
 * attached objects outside and potentially run into issues such as no transaction or accidentally
 * make modifications to the underlying data.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PersonDao {

    private final PersonRepository personRepository;

    /**
     * Attempt to find a PersonEntity record in the database and any associated Children and Partners using a bsn.
     *
     * @return
     * An empty optional if the person could not be found.
     * An optional with a PersonEntity record containing on their children and partners.
     */
    public Optional<PersonDto> findPersonFromBsnWithPartnerChildren(String bsn) {
        return
            personRepository
                .findByBsn(bsn)
                .map(person ->
                    PersonMapper.toDto(
                        person,
                        Set.of(RelationshipType.PARTNER, RelationshipType.CHILD),
                        null));
    }

    /**
     * Attempt to find a PersonEntity record in the database and any associated Children and Partners using
     * a name, surname and dob.
     *
     * @return
     * A list of PersonEntity Records matching the provided criteria along with their children and partners.
     */
    public Set<PersonDto> findAllPersonFromNameSurnameDobWithPartnerChildren(
        String name,
        String surname,
        LocalDate dob
    ) {
        return personRepository
            .findAllByNameAndSurnameAndDateOfBirth(name, surname, dob)
            .stream()
            .map(person ->
                PersonMapper.toDto(
                    person,
                    Set.of(RelationshipType.PARTNER, RelationshipType.CHILD),
                    null))
            .collect(Collectors.toSet());
    }


}
