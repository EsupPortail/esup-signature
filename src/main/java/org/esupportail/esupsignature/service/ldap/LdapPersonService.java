package org.esupportail.esupsignature.service.ldap;

import org.esupportail.esupsignature.config.ldap.LdapProperties;
import org.esupportail.esupsignature.repository.ldap.PersonLdapLightRepository;
import org.esupportail.esupsignature.repository.ldap.PersonLdapRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.MessageFormat;
import java.util.List;

@Service
@ConditionalOnProperty({"spring.ldap.base"})
@EnableConfigurationProperties(LdapProperties.class)
public class LdapPersonService {

    private static final Logger logger = LoggerFactory.getLogger(LdapPersonService.class);

    @Resource
    private LdapProperties ldapProperties;

    @Resource
    private PersonLdapRepository personLdapRepository;

    @Resource
    private PersonLdapLightRepository personLdapLightRepository;

    public List<PersonLdap> search(String searchString) {
        String formattedFilter = MessageFormat.format(ldapProperties.getUsersSearchFilter(), (Object[]) new String[] { searchString });
        logger.debug("search on ldap with : " + formattedFilter);
        return (List<PersonLdap>) personLdapRepository.findAll(LdapQueryBuilder.query().countLimit(10).base(ldapProperties.getSearchBase()).filter(formattedFilter));
    }

    public List<PersonLdapLight> searchLight(String searchString) {
        String formattedFilter = MessageFormat.format(ldapProperties.getUsersSearchFilter(), (Object[]) new String[] { searchString });
        logger.debug("search on ldap with : " + formattedFilter);

        return (List<PersonLdapLight>) personLdapLightRepository.findAll(LdapQueryBuilder.query().countLimit(10).base(ldapProperties.getSearchBase()).filter(formattedFilter));
    }

    public List<PersonLdapLight> getPersonLdapLight(String authName) {
        String formattedFilter = MessageFormat.format(ldapProperties.getUserIdSearchFilter(), (Object[]) new String[] { authName });
        logger.debug("search on ldap with : " + formattedFilter);
        return (List<PersonLdapLight>) personLdapLightRepository.findAll(LdapQueryBuilder.query().countLimit(10).base(ldapProperties.getSearchBase()).filter(formattedFilter));
    }

    public List<PersonLdapLight> getPersonLdapLightByEppn(String eppn) {
        logger.debug("search on ldap by eppn : " + eppn);
        return personLdapLightRepository.findByEduPersonPrincipalName(eppn);
    }

    public List<PersonLdap> getPersonLdapByEppn(String eppn) {
        logger.debug("search on ldap by eppn : " + eppn);
        return personLdapRepository.findByEduPersonPrincipalName(eppn);
    }

    public List<PersonLdap> getPersonLdapByMail(String mail) {
        return personLdapRepository.findByMail(mail);
    }
}