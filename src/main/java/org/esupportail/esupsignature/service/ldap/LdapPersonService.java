package org.esupportail.esupsignature.service.ldap;

import org.esupportail.esupsignature.config.ldap.LdapProperties;
import org.esupportail.esupsignature.repository.ldap.PersonLdapLightRepository;
import org.esupportail.esupsignature.repository.ldap.PersonLdapRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.MessageFormat;
import java.util.List;

@Service
@ConditionalOnProperty({"spring.ldap.base"})
@EnableConfigurationProperties(LdapProperties.class)
public class LdapPersonService {

    @Resource
    private LdapTemplate ldapTemplate;

    @Resource
    private LdapProperties ldapProperties;

    @Resource
    private PersonLdapRepository personLdapRepository;

    @Resource
    private PersonLdapLightRepository personLdapLightRepository;

    public List<PersonLdap> search(String searchString) {
        return personLdapRepository.findByDisplayNameStartingWithIgnoreCaseOrCnStartingWithIgnoreCaseOrUidStartingWithOrMailStartingWith(searchString, searchString, searchString, searchString);
    }

    public List<PersonLdapLight> searchLight(String searchString) {
        return personLdapLightRepository.fullTextSearch(searchString);
    }
    public PersonLdapRepository getPersonLdapRepository() {
		return personLdapRepository;
	}

    public List<PersonLdap> getPersonLdap(String authName) {
        String formattedFilter = MessageFormat.format(ldapProperties.getUserIdSearchFilter(), new String[] { authName });
        return ldapTemplate.search(ldapProperties.getSearchBase(), formattedFilter, new PersonLdapAttributesMapper());
    }

}