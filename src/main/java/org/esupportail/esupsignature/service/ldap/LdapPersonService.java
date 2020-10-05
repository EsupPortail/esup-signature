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

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Resource
    private LdapProperties ldapProperties;

    @Resource
    private LdapTemplate ldapTemplate;

    @Resource
    private PersonLdapRepository personLdapRepository;

    private Map<String, LdapTemplate> ldapTemplates;

    @Autowired
    public LdapPersonService(Map<String, LdapTemplate> ldapTemplates) {
        this.ldapTemplates = ldapTemplates;
    }

    public List<PersonLdap> search(String searchString, String ldapTemplateName) {
        LdapTemplate ldapTemplateSelected = ldapTemplate;
        if (ldapTemplateName != null && !ldapTemplateName.isEmpty() && ldapTemplates.containsKey(ldapTemplateName)) {
            ldapTemplateSelected = ldapTemplates.get(ldapTemplateName);
        }
        if (ldapTemplateSelected != null) {
            List<PersonLdap> results = personLdapRepository.findByDisplayNameStartingWithIgnoreCaseOrCnStartingWithIgnoreCaseOrDisplayNameStartingWithIgnoreCaseOrUidOrMailStartingWith(searchString, searchString, searchString, searchString);
            List<PersonLdap> filteredPersons = new ArrayList<>();
            for(PersonLdap personLdap : results) {
                for(String affiationName : ldapProperties.getAffiliationFilter().split(",")) {
                    if (personLdap.getEduPersonAffiliation().contains(affiationName.trim()) || personLdap.getEduPersonPrimaryAffiliation().equals(affiationName.trim())) {
                        filteredPersons.add(personLdap);
                    }
                }
            }
            return filteredPersons;
        } else {
            log.debug("No ldapTemplate found -> LdapPersonService.searchByCommonName result is empty");
        }
        return new ArrayList<>();
    }

}