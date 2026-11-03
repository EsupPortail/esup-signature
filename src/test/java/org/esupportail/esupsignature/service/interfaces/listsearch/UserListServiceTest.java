package org.esupportail.esupsignature.service.interfaces.listsearch;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.repository.UserRepository;
import org.esupportail.esupsignature.service.ldap.LdapPersonService;
import org.esupportail.esupsignature.service.ldap.entry.PersonLdap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserListServiceTest {

    private final UserList userList = mock(UserList.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final GlobalProperties properties = new GlobalProperties();
    private UserListService service;

    @BeforeEach
    void setUp() {
        properties.setDomain("example.org");
        properties.setForcedExternalsDomainList(List.of());
        service = new UserListService(List.of(userList), userRepository, null, properties);
    }

    @Test
    void membersAndAliasesAreReturnedWithoutLdapAndDuplicatesAreRemoved() {
        when(userList.getUsersEmailFromList("team@example.org")).thenReturn(List.of("a@example.org", "b@example.org"));
        when(userList.getUsersEmailFromAliases("team@example.org")).thenReturn(List.of("a@example.org", "c@example.org"));

        assertEquals(Set.of("a@example.org", "b@example.org", "c@example.org"),
                new HashSet<>(service.getUsersEmailFromList("team@example.org")));
    }

    @Test
    void membersAreReturnedWithLdapWhenAddressIsNotAPerson() {
        LdapPersonService ldap = mock(LdapPersonService.class);
        service = new UserListService(List.of(userList), userRepository, ldap, properties);
        when(ldap.getPersonLdapByMail("team@example.org")).thenReturn(List.of());
        when(userList.getUsersEmailFromList("team@example.org")).thenReturn(List.of("a@example.org"));

        assertEquals(List.of("a@example.org"), service.getUsersEmailFromList("team@example.org"));
    }

    @Test
    void ldapPersonIsNotExpandedAsAList() {
        LdapPersonService ldap = mock(LdapPersonService.class);
        service = new UserListService(List.of(userList), userRepository, ldap, properties);
        when(ldap.getPersonLdapByMail("person@example.org")).thenReturn(List.of(new PersonLdap()));

        assertTrue(service.getUsersEmailFromList("person@example.org").isEmpty());
        verifyNoInteractions(userList, userRepository);
    }

    @Test
    void localPersonIsNotExpandedAsAListWithoutLdap() {
        User user = new User();
        user.setUserType(UserType.external);
        when(userRepository.findByEmailIgnoreCase("person@example.org")).thenReturn(Optional.of(user));

        assertTrue(service.getUsersEmailFromList("person@example.org").isEmpty());
        verifyNoInteractions(userList);
    }

    @Test
    void localGroupIsExpandedWithoutLdap() {
        User group = new User();
        group.setUserType(UserType.group);
        when(userRepository.findByEmailIgnoreCase("team@example.org")).thenReturn(Optional.of(group));
        when(userList.getUsersEmailFromList("team@example.org")).thenReturn(List.of("a@example.org"));

        assertEquals(List.of("a@example.org"), service.getUsersEmailFromList("team@example.org"));
    }

    @Test
    void forcedExternalAddressIsNotExpanded() {
        properties.setForcedExternalsDomainList(List.of("external.example.org"));

        assertTrue(service.getUsersEmailFromList("team@external.example.org").isEmpty());
        verifyNoInteractions(userList, userRepository);
    }

    @Test
    void providerFailureIsPropagated() {
        when(userList.getUsersEmailFromList("team@example.org")).thenThrow(new EsupSignatureRuntimeException("error.sympa.soap"));

        assertThrows(EsupSignatureRuntimeException.class, () -> service.getUsersEmailFromList("team@example.org"));
    }
}
