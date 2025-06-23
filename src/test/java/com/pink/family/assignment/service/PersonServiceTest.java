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
class PersonServiceTest {

    @Mock
    private PersonDao personDao;

    @InjectMocks
    private PersonService personService;

    @Nested
    class FindNameSurnameDob {

        @Test
        @DisplayName("Should return true when three children are shared between partners")
        void success_whenThreeChildrenSharedWithPartner() {
            PersonDto partner = PersonDto.builder()
                .id(2L)
                .name("Partner")
                .externalId("987654321")
                .dateOfBirth(LocalDate.of(1984, 4, 4))
                .relationships(emptyRelationships())
                .build();

            PersonDto main = PersonDto.builder()
                .id(1L)
                .name("Main")
                .externalId("123456789")
                .dateOfBirth(LocalDate.of(1980, 1, 1))
                .relationships(emptyRelationships())
                .build();

            PersonDto child1 = buildChild(10L, "Child1", main, partner);
            PersonDto child2 = buildChild(11L, "Child2", main, partner);
            PersonDto child3 = buildChild(12L, "Child3", main, partner);

            main.getRelationships().get(RelationshipType.PARTNER).add(partner);
            main.getRelationships().get(RelationshipType.CHILD).addAll(Set.of(child1, child2, child3));

            when(
                personDao.findAllPersonFromNameDobWithPartnerChildren(
                    main.getName(),
                    main.getDateOfBirth()))
                .thenReturn(Set.of(main));

            String result =
                personService.hasPartnerAndChildrenNameSurnameDob(
                    main.getName(),
                    main.getDateOfBirth());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return false when there are no partners")
        void fails_whenNoPartners() {
            PersonDto main = PersonDto.builder()
                .id(1L)
                .name("Main")
                .externalId("123456789")
                .dateOfBirth(LocalDate.of(1980, 1, 1))
                .relationships(emptyRelationships())
                .build();

            main.getRelationships().get(RelationshipType.CHILD)
                .addAll(Set.of(
                    buildChild(10L, "C1", main, null),
                    buildChild(11L, "C2", main, null),
                    buildChild(12L, "C3", main, null)
                ));

            when(
                personDao.findAllPersonFromNameDobWithPartnerChildren(
                    main.getName(),
                    main.getDateOfBirth()))
                .thenReturn(Set.of(main));

            String result = personService.hasPartnerAndChildrenNameSurnameDob(
                main.getName(), main.getDateOfBirth());

            assertThat(result).isEqualTo(PersonService.Constants.ErrorMsg.NO_PARTNER);
        }

        @Test
        @DisplayName("Should return false when 3 children are shared with any single partner")
        void fails_whenChildrenDoNotShareSamePartner() {
            PersonDto partner1 = PersonDto.builder()
                .id(2L).name("Partner1").externalId("987654321")
                .dateOfBirth(LocalDate.of(1984, 4, 4))
                .relationships(emptyRelationships())
                .build();

            PersonDto partner2 = PersonDto.builder()
                .id(3L).name("Partner2").externalId("987654320")
                .dateOfBirth(LocalDate.of(1986, 5, 5))
                .relationships(emptyRelationships())
                .build();

            PersonDto main = PersonDto.builder()
                .id(1L).name("Main").externalId("123456789")
                .dateOfBirth(LocalDate.of(1980, 1, 1))
                .relationships(emptyRelationships())
                .build();

            main.getRelationships().get(RelationshipType.PARTNER).addAll(Set.of(partner1, partner2));
            main.getRelationships().get(RelationshipType.CHILD).addAll(Set.of(
                buildChild(10L, "C1", main, partner1),
                buildChild(11L, "C2", main, partner2),
                buildChild(12L, "C3", main, partner2)
            ));

            when(personDao.findAllPersonFromNameDobWithPartnerChildren(main.getName(), main.getDateOfBirth()))
                .thenReturn(Set.of(main));

            String result = personService.hasPartnerAndChildrenNameSurnameDob(
                main.getName(), main.getDateOfBirth());

            assertThat(result).isEqualTo(PersonService.Constants.ErrorMsg.NO_SHARED_CHILDREN);
        }
    }
    
    @Nested
    class FindExternalIdTests {

        @Test
        void success_whenThreeChildrenSharedWithPartner() {
            // Build the partner
            PersonDto partner = PersonDto.builder()
                .id(2L)
                .name("Partner")
                .externalId("987654321")
                .dateOfBirth(LocalDate.of(1984, 4, 4))
                .relationships(emptyRelationships())
                .build();

            // Build main person
            PersonDto main = PersonDto.builder()
                .id(1L)
                .name("Main")
                .externalId("123456789")
                .dateOfBirth(LocalDate.of(1980, 1, 1))
                .relationships(emptyRelationships())
                .build();

            // Build children and set both main and partner as parents
            PersonDto child1 = buildChild(10L, "Child1", main, partner);
            PersonDto child2 = buildChild(11L, "Child2", main, partner);
            PersonDto child3 = buildChild(12L, "Child3", main, partner);

            // Add partner and children to main's relationships
            main.getRelationships().get(RelationshipType.PARTNER).add(partner);
            main.getRelationships().get(RelationshipType.CHILD).addAll(Set.of(child1, child2, child3));

            when(personDao.findPersonFromExternalIdWithPartnerChildren("123456789"))
                .thenReturn(Optional.of(main));

            assertThat(personService.hasPartnerAndChildrenExternalId("123456789")).isEmpty();
        }

        @Test
        @DisplayName("Should return false when there are no partners")
        void fails_whenNoPartners() {
            PersonDto main = PersonDto.builder()
                .id(1L)
                .name("Main")
                .externalId("123456789")
                .dateOfBirth(LocalDate.of(1980, 1, 1))
                .relationships(emptyRelationships())
                .build();

            // Add 3 children, but no partners
            main.getRelationships().get(RelationshipType.CHILD)
                .addAll(Set.of(
                    buildChild(10L, "C1", main, null),
                    buildChild(11L, "C2", main, null),
                    buildChild(12L, "C3", main, null)
                ));

            when(personDao.findPersonFromExternalIdWithPartnerChildren("123456789"))
                .thenReturn(Optional.of(main));

            assertThat(personService.hasPartnerAndChildrenExternalId("123456789"))
                .isEqualTo(PersonService.Constants.ErrorMsg.NO_PARTNER);
        }


        @Test
        @DisplayName("Should return false when 3 children are not shared with any partners")
        void fails_whenChildrenDoNotShareSamePartner() {
            PersonDto partner1 = PersonDto.builder()
                .id(2L).name("Partner1").externalId("987654321")
                .dateOfBirth(LocalDate.of(1984, 4, 4))
                .relationships(emptyRelationships())
                .build();

            PersonDto partner2 = PersonDto.builder()
                .id(3L).name("Partner2").externalId("987654320")
                .dateOfBirth(LocalDate.of(1986, 5, 5))
                .relationships(emptyRelationships())
                .build();

            PersonDto main = PersonDto.builder()
                .id(1L).name("Main").externalId("123456789")
                .dateOfBirth(LocalDate.of(1980, 1, 1))
                .relationships(emptyRelationships())
                .build();

            main.getRelationships().get(RelationshipType.PARTNER).addAll(Set.of(partner1, partner2));
            main.getRelationships().get(RelationshipType.CHILD).addAll(Set.of(
                buildChild(10L, "C1", main, partner1),
                buildChild(11L, "C2", main, partner2),
                buildChild(12L, "C3", main, partner2)
            ));

            when(personDao.findPersonFromExternalIdWithPartnerChildren("123456789"))
                .thenReturn(Optional.of(main));

            assertThat(personService.hasPartnerAndChildrenExternalId("123456789"))
                .isEqualTo(PersonService.Constants.ErrorMsg.NO_SHARED_CHILDREN);
        }
    }

    private PersonDto buildChild(Long id, String name, PersonDto parent1, PersonDto parent2) {
        EnumMap<RelationshipType, Set<PersonDto>> rels = emptyRelationships();
        rels.get(RelationshipType.FATHER).add(parent1);
        rels.get(RelationshipType.MOTHER).add(parent2);
        return PersonDto.builder()
            .id(id)
            .name(name)
            .externalId("C" + id)
            .dateOfBirth(LocalDate.of(2010, 6, 15))
            .relationships(rels)
            .build();
    }

    private EnumMap<RelationshipType, Set<PersonDto>> emptyRelationships() {
        EnumMap<RelationshipType, Set<PersonDto>> rels = new EnumMap<>(RelationshipType.class);
        for (RelationshipType type : RelationshipType.values()) {
            rels.put(type, new HashSet<>());
        }
        return rels;
    }
}
