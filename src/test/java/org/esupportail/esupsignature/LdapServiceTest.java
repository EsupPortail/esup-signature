package org.esupportail.esupsignature;

import org.esupportail.esupsignature.service.ldap.LdapPersonService;
import org.junit.Test;
import org.junit.runner.RunWith;
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
@TestPropertySource(properties = "app.scheduling.enable=false")
public class LdapServiceTest {

    @Resource
    private LdapPersonService ldapPersonService;

    @Resource
    private LdapContextSource ldapContextSource;

    @Test(timeout = 1000)
    public void testLdap() {
        assumeTrue("ldap not configured", ldapContextSource.getUserDn() != null);
        ldapPersonService.getOrganizationalUnitLdap("0");
    }

}
