package org.esupportail.esupsignature;

import org.esupportail.esupsignature.service.ldap.LdapOrganizationalUnitService;
import org.esupportail.esupsignature.service.ldap.LdapPersonLightService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EsupSignatureApplication.class)
@TestPropertySource(properties = {"app.scheduling.enable=false"})
public class LdapServiceTest {

    @Autowired(required = false)
    private LdapContextSource ldapContextSource;

    @Autowired(required = false)
    private LdapOrganizationalUnitService ldapOrganizationalUnitService;

    @Autowired(required = false)
    private LdapPersonLightService ldapPersonLightService;

    @Test
    public void testLdapPerson() {
        assumeTrue(ldapContextSource.getUserDn() != null && !ldapContextSource.getUserDn().isEmpty(), "LDAP not configured");
        assertDoesNotThrow(() -> ldapPersonLightService.getPersonLdapLight("test"),
                "La recherche LDAP d'une personne ne doit pas échouer avec la configuration fournie par l'exploitant.");
    }

    @Test
    public void testLdapOu() {
        assumeTrue(ldapContextSource.getUserDn() != null && !ldapContextSource.getUserDn().isEmpty(), "LDAP not configured");
        assertNotNull(ldapOrganizationalUnitService, "Le service LDAP des structures doit être disponible quand LDAP est configuré.");
        assertDoesNotThrow(() -> ldapOrganizationalUnitService.getOrganizationalUnitLdap("test"),
                "La recherche LDAP d'une structure ne doit pas échouer avec la configuration fournie par l'exploitant.");
    }

}
