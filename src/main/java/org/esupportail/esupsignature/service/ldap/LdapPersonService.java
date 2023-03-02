package org.esupportail.esupsignature.service.ldap;

import org.esupportail.esupsignature.config.ldap.LdapProperties;
import org.esupportail.esupsignature.repository.ldap.PersonLdapLightRepository;
import org.esupportail.esupsignature.repository.ldap.PersonLdapRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.MessageFormat;
import java.util.List;

@Service
@ConditionalOnProperty({"spring.ldap.base"})
@EnableConfigurationProperties(LdapProperties.class)
public class LdapPersonService {

    @Resource
    private LdapProperties ldapProperties;

    @Resource
    private LdapTemplate ldapTemplate;

    @Resource
    private PersonLdapRepository personLdapRepository;

    @Resource
    private PersonLdapLightRepository personLdapLightRepository;

    public List<PersonLdap> search(String searchString) {
        String formattedFilter = MessageFormat.format(ldapProperties.getUsersSearchFilter(), (Object[]) new String[] { searchString });
        return ldapTemplate.search(LdapQueryBuilder.query().countLimit(10).base(ldapProperties.getSearchBase()).filter(formattedFilter), new PersonLdapAttributesMapper());
    }

    public List<PersonLdapLight> searchLight(String searchString) {
        String formattedFilter = MessageFormat.format(ldapProperties.getUsersSearchFilter(), (Object[]) new String[] { searchString });
        return ldapTemplate.search(LdapQueryBuilder.query().countLimit(10).base(ldapProperties.getSearchBase()).filter(formattedFilter), new PersonLdapLightAttributesMapper());
    }
    public PersonLdapRepository getPersonLdapRepository() {
		return personLdapRepository;
	}

    public PersonLdapLightRepository getPersonLdapLightRepository() {
        return personLdapLightRepository;
    }

    public List<PersonLdapLight> getPersonLdapLight(String authName) {
        String formattedFilter = MessageFormat.format(ldapProperties.getUserIdSearchFilter(), (Object[]) new String[] { authName });
        return ldapTemplate.search(ldapProperties.getSearchBase(), formattedFilter, new PersonLdapLightAttributesMapper());
    }

    public List<PersonLdapLight> getPersonLdapLightByEppn(String eppn) {
        String formattedFilter = MessageFormat.format("(eduPersonPrincipalName={0})", (Object[]) new String[] { eppn });
        return ldapTemplate.search(ldapProperties.getSearchBase(), formattedFilter, new PersonLdapLightAttributesMapper());
    }

    public List<PersonLdap> getPersonLdapByEppn(String eppn) {
        String formattedFilter = MessageFormat.format("(eduPersonPrincipalName={0})", (Object[]) new String[] { eppn });
        return ldapTemplate.search(ldapProperties.getSearchBase(), formattedFilter, new PersonLdapAttributesMapper());
    }

}