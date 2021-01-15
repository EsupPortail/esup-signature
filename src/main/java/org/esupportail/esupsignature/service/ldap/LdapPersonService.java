package org.esupportail.esupsignature.service.ldap;

import org.esupportail.esupsignature.config.ldap.LdapProperties;
import org.esupportail.esupsignature.repository.ldap.PersonLdapRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.MessageFormat;
import java.util.List;

@Service
@ConditionalOnProperty(prefix = "spring.ldap", name = "base")
@EnableConfigurationProperties(LdapProperties.class)
public class LdapPersonService {

    @Autowired
    private LdapTemplate ldapTemplate;

    @Resource
    private LdapProperties ldapProperties;

    @Resource
    private PersonLdapRepository personLdapRepository;

    public List<PersonLdap> search(String searchString) {
        return personLdapRepository.findByDisplayNameStartingWithIgnoreCaseOrCnStartingWithIgnoreCaseOrUidStartingWithOrMailStartingWith(searchString, searchString, searchString, searchString);

    }

	public PersonLdapRepository getPersonLdapRepository() {
		return personLdapRepository;
	}

    public List<PersonLdap> getPersonLdap(String uid) {
        String formattedFilter = MessageFormat.format(ldapProperties.getUserIdSearchFilter(), new String[] { uid });
        return ldapTemplate.search(ldapProperties.getSearchBase(), formattedFilter, new PersonLdapAttributesMapper());
    }

}