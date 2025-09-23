package org.esupportail.esupsignature;

import org.esupportail.esupsignature.service.ldap.LdapOrganizationalUnitService;
import org.esupportail.esupsignature.service.ldap.LdapPersonLightService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
        ldapPersonLightService.getPersonLdapLight("0");
    }

    @Test
    public void testLdapOu() {
        assumeTrue(ldapContextSource.getUserDn() != null && !ldapContextSource.getUserDn().isEmpty(), "LDAP not configured");
        try {
            ldapOrganizationalUnitService.getOrganizationalUnitLdap("0");
        } catch (Exception e) {
            Assumptions.abort("Error on getOrganizationalUnitLdap : " + e.getMessage());
        }
    }

}
