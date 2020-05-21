package org.esupportail.esupsignature;

import org.esupportail.esupsignature.service.ldap.LdapPersonService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import javax.naming.ldap.LdapContext;

import static org.junit.Assume.assumeTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EsupSignatureApplication.class)
@TestPropertySource(properties = {"app.scheduling.enable=false"})
public class LdapServiceTest {

    @Autowired(required = false)
    private LdapContextSource ldapContextSource;

    @Autowired(required = false)
    private LdapPersonService ldapPersonService;

    @Test(timeout = 5000)
    public void testLdap() {
        assumeTrue("ldap not configured", ldapContextSource.getUserDn() != null && !ldapContextSource.getUserDn().equals(""));
        ldapPersonService.getOrganizationalUnitLdap("0");
    }

}
