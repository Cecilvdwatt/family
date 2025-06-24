package com.pink.family.assignment.service;

import com.pink.family.assignment.database.dao.PersonDao;
import com.pink.family.assignment.database.entity.enums.RelationshipType;
import com.pink.family.assignment.dto.PersonDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonServiceTests {

    @Mock
    private PersonDao personDao;

    @InjectMocks
    private PersonService personService;

    private PersonDto buildPerson(long id, String name) {
        return PersonDto.builder()
            .internalId(id)
            .externalId(id + 1000)
            .name(name)
            .dateOfBirth(LocalDate.of(1980, 1, 1))
            .relationships(emptyRelationships())
            .build();
    }

    private PersonDto buildChild(long id, String name, int ageYears, PersonDto... parents) {
        PersonDto child = PersonDto.builder()
            .internalId(id)
            .externalId(id + 1000)
            .name(name)
            .dateOfBirth(LocalDate.now().minusYears(ageYears))
            .relationships(emptyRelationships())
            .build();

        for (PersonDto parent : parents) {
            if (parent != null) {
                child.getRelations(RelationshipType.CHILD).add(parent);
            }
        }
        return child;
    }

    private EnumMap<RelationshipType, Set<PersonDto>> emptyRelationships() {
        EnumMap<RelationshipType, Set<PersonDto>> map = new EnumMap<>(RelationshipType.class);
        for (RelationshipType type : RelationshipType.values()) {
            map.put(type, new HashSet<>());
        }
        return map;
    }

    private void linkPartners(PersonDto a, PersonDto b) {
        a.getRelations(RelationshipType.PARTNER).add(b);
        b.getRelations(RelationshipType.PARTNER).add(a);
    }

    private void addChildren(PersonDto parent, PersonDto... children) {
        for (PersonDto child : children) {
            parent.getRelations(RelationshipType.PARENT).add(child);
        }
    }

    @Nested
    class FindByNameDobTests {

        @Test
        @DisplayName("Returns empty when 3 children are shared with one partner and one is under 18")
        void success_whenSharedChildrenWithOnePartner() {
            PersonDto main = buildPerson(1L, "Main");
            PersonDto partner = buildPerson(2L, "Partner");

            PersonDto child1 = buildChild(3L, "Child1", 17, main, partner);
            PersonDto child2 = buildChild(4L, "Child2", 16, main, partner);
            PersonDto child3 = buildChild(5L, "Child3", 18, main, partner);

            linkPartners(main, partner);
            addChildren(main, child1, child2, child3);

            when(personDao.findAllPersonFromNameDobWithPartnerChildren("Main", main.getDateOfBirth()))
                .thenReturn(Set.of(main));

            assertThat(personService.hasPartnerAndChildrenNameSurnameDob("Main", main.getDateOfBirth())).isEmpty();
        }

        @Test
        @DisplayName("Returns NO_PARTNER if no partner exists")
        void fail_whenNoPartnerExists() {
            PersonDto main = buildPerson(1L, "Main");

            PersonDto child1 = buildChild(3L, "Child1", 10, main);
            PersonDto child2 = buildChild(4L, "Child2", 9, main);
            PersonDto child3 = buildChild(5L, "Child3", 8, main);

            addChildren(main, child1, child2, child3);

            when(personDao.findAllPersonFromNameDobWithPartnerChildren("Main", main.getDateOfBirth()))
                .thenReturn(Set.of(main));

            assertThat(personService.hasPartnerAndChildrenNameSurnameDob("Main", main.getDateOfBirth()))
                .contains(PersonService.Constants.ErrorMsg.NO_PARTNER);
        }

        @Test
        @DisplayName("Returns NO_SHARED_CHILDREN if children have mixed partners")
        void fail_whenChildrenHaveDifferentPartners() {
            PersonDto main = buildPerson(1L, "Main");
            PersonDto partner1 = buildPerson(2L, "Partner1");
            PersonDto partner2 = buildPerson(3L, "Partner2");

            PersonDto child1 = buildChild(4L, "Child1", 12, main, partner1);
            PersonDto child2 = buildChild(5L, "Child2", 11, main, partner2);
            PersonDto child3 = buildChild(6L, "Child3", 10, main, partner2);

            linkPartners(main, partner1);
            linkPartners(main, partner2);
            addChildren(main, child1, child2, child3);

            when(personDao.findAllPersonFromNameDobWithPartnerChildren("Main", main.getDateOfBirth()))
                .thenReturn(Set.of(main));

            assertThat(personService.hasPartnerAndChildrenNameSurnameDob("Main", main.getDateOfBirth()))
                .contains(PersonService.Constants.ErrorMsg.NO_SHARED_CHILDREN);
        }
    }

    @Nested
    class FindByExternalIdTests {

        @Test
        @DisplayName("Returns empty when 3 children shared with partner and one is under 18")
        void success_whenThreeChildrenSharedWithPartner() {
            PersonDto main = buildPerson(1L, "Main");
            PersonDto partner = buildPerson(2L, "Partner");

            PersonDto child1 = buildChild(10L, "Child1", 12, main, partner);
            PersonDto child2 = buildChild(11L, "Child2", 10, main, partner);
            PersonDto child3 = buildChild(12L, "Child3", 8, main, partner);

            linkPartners(main, partner);
            addChildren(main, child1, child2, child3);

            when(personDao.findPersonFromExternalId(anyLong(), anyInt()))
                .thenReturn(Set.of(main));

            assertThat(personService.hasPartnerAndChildrenExternalId(main.getExternalId())).isEmpty();
        }

        @Test
        @DisplayName("Returns NO_PARTNER when no partners exist")
        void fails_whenNoPartnersExist() {
            PersonDto main = buildPerson(1L, "Main");

            PersonDto child1 = buildChild(10L, "C1", 12, main);
            PersonDto child2 = buildChild(11L, "C2", 13, main);
            PersonDto child3 = buildChild(12L, "C3", 14, main);

            addChildren(main, child1, child2, child3);

            when(personDao.findPersonFromExternalId(anyLong(), anyInt()))
                .thenReturn(Set.of(main));

            assertThat(
                personService.hasPartnerAndChildrenExternalId(main.getExternalId()))
                .contains(PersonService.Constants.ErrorMsg.NO_PARTNER);
        }

        @Test
        @DisplayName("Returns NO_SHARED_CHILDREN if children don't have a common second parent")
        void fails_whenChildrenHaveDifferentPartners() {
            PersonDto main = buildPerson(1L, "Main");
            PersonDto partner1 = buildPerson(2L, "Partner1");
            PersonDto partner2 = buildPerson(3L, "Partner2");

            PersonDto child1 = buildChild(10L, "C1", 12, main, partner1);
            PersonDto child2 = buildChild(11L, "C2", 13, main, partner2);
            PersonDto child3 = buildChild(12L, "C3", 14, main, partner2);

            linkPartners(main, partner1);
            linkPartners(main, partner2);
            addChildren(main, child1, child2, child3);

            when(personDao.findPersonFromExternalId(anyLong(), anyInt()))
                .thenReturn(Set.of(main));

            assertThat(personService.hasPartnerAndChildrenExternalId(main.getExternalId()))
                .contains(PersonService.Constants.ErrorMsg.NO_SHARED_CHILDREN);
        }
    }
}
