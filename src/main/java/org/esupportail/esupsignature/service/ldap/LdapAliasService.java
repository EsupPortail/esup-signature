package org.esupportail.esupsignature.service.ldap;

import org.esupportail.esupsignature.config.ldap.LdapProperties;
import org.esupportail.esupsignature.service.ldap.entry.AliasLdap;
import org.esupportail.esupsignature.service.ldap.mapper.AliasLdapAttributesMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.MessageFormat;
import java.util.List;

@Service
@ConditionalOnProperty({"spring.ldap.base"})
public class LdapAliasService {

    private static final Logger logger = LoggerFactory.getLogger(LdapAliasService.class);

    private final LdapProperties ldapProperties;
    private final LdapTemplate ldapTemplate;

    public LdapAliasService(LdapProperties ldapProperties, LdapTemplate ldapTemplate) {
        this.ldapProperties = ldapProperties;
        this.ldapTemplate = ldapTemplate;
    }

    public List<AliasLdap> searchByMail(String mail, boolean strict) {
        logger.debug("search AliasLdap by mail " + mail);
		if(!strict) mail = "*" + mail + "*";
        String formattedFilter = MessageFormat.format(ldapProperties.getAllAliasesSearchFilter(), (Object[]) new String[] { mail });
        StringBuilder objectClasses = new StringBuilder();
        for(String objectClass : ldapProperties.getAliasObjectClasses()) {
            objectClasses.append("(objectClass=").append(objectClass).append(")");
        }
        if(StringUtils.hasText(objectClasses)) {
            formattedFilter = "(&(|" + objectClasses + ")" + formattedFilter + ")";
        }
        logger.debug("search AliasLdap query : " + formattedFilter);
        LdapQuery ldapQuery = LdapQueryBuilder.query().countLimit(20).filter(formattedFilter);
        List<AliasLdap> aliasLdaps = ldapTemplate.search(ldapQuery, new AliasLdapAttributesMapper());
        logger.debug("search AliasLdap found " + aliasLdaps.size() + " results");
        return aliasLdaps;
    }

}