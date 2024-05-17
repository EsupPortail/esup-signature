package org.esupportail.esupsignature.service.ldap;

import org.esupportail.esupsignature.config.ldap.LdapProperties;
import org.esupportail.esupsignature.service.ldap.entry.OrganizationalUnitLdap;
import org.esupportail.esupsignature.service.ldap.mapper.OrganizationalUnitLdapAttributesMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.text.MessageFormat;
import java.util.List;

@Service
@ConditionalOnProperty(prefix = "spring.ldap", name = "base")
@EnableConfigurationProperties(LdapProperties.class)
public class LdapOrganizationalUnitService {

    private static final Logger logger = LoggerFactory.getLogger(LdapOrganizationalUnitService.class);

    @Resource
    private LdapProperties ldapProperties;

    @Resource
    private LdapTemplate ldapTemplate;

    public List<OrganizationalUnitLdap> getOrganizationalUnitLdaps(String supannCodeEntite) {
        String formattedFilter = MessageFormat.format(ldapProperties.getOuSearchFilter(), (Object[]) new String[] { supannCodeEntite });
        StringBuilder objectClasses = new StringBuilder();
        for(String objectClass : ldapProperties.getOuObjectClasses()) {
            objectClasses.append("(objectClass=").append(objectClass).append(")");
        }
        formattedFilter = "(&(|" + objectClasses + ")" + formattedFilter + ")";
        logger.debug("search OrganizationalUnit by mail : " + formattedFilter);
        LdapQuery ldapQuery = LdapQueryBuilder.query().countLimit(10).filter(formattedFilter);
        return ldapTemplate.search(ldapQuery, new OrganizationalUnitLdapAttributesMapper());
    }

    public OrganizationalUnitLdap getOrganizationalUnitLdap(String supannCodeEntite) {
        List<OrganizationalUnitLdap> organizationalUnitLdap = getOrganizationalUnitLdaps(supannCodeEntite);
        if(!organizationalUnitLdap.isEmpty()) {
            return organizationalUnitLdap.get(0);
        }
        return null;
    }

}