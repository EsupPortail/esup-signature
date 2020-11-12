package org.esupportail.esupsignature.service.ldap;

import org.esupportail.esupsignature.config.ldap.LdapProperties;
import org.esupportail.esupsignature.repository.ldap.OrganizationalUnitLdapRepository;
import org.esupportail.esupsignature.repository.ldap.PersonLdapRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(prefix = "spring.ldap", name = "base")
@EnableConfigurationProperties(LdapProperties.class)
public class LdapPersonService {

    @Resource
    private LdapProperties ldapProperties;

    @Resource
    private PersonLdapRepository personLdapRepository;

    public List<PersonLdap> search(String searchString) {
        List<PersonLdap> results = personLdapRepository.findByDisplayNameStartingWithIgnoreCaseOrCnStartingWithIgnoreCaseOrUidStartingWithOrMailStartingWith(searchString, searchString, searchString, searchString);
        List<PersonLdap> filteredPersons = new ArrayList<>();
        for(PersonLdap personLdap : results) {
            for(String affiationName : ldapProperties.getAffiliationFilter().split(",")) {
                if (personLdap.getEduPersonAffiliation().contains(affiationName.trim()) || personLdap.getEduPersonPrimaryAffiliation().equals(affiationName.trim())) {
                    filteredPersons.add(personLdap);
                }
            }
        }
        return filteredPersons;
    }

	public PersonLdapRepository getPersonLdapRepository() {
		return personLdapRepository;
	}

}