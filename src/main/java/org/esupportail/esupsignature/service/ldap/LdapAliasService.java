package org.esupportail.esupsignature.service.ldap;

import org.esupportail.esupsignature.config.ldap.LdapProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@ConditionalOnProperty({"spring.ldap.base", "ldap.list-search-base", "ldap.list-search-filter"})
public class LdapAliasService {

    @Resource
    private LdapProperties ldapProperties;

    @Resource
    private LdapTemplate ldapTemplate;

    public List<AliasLdap> searchAlias(String searchString) {
        String formattedFilter = MessageFormat.format(ldapProperties.getListSearchFilter(), new String[] { searchString });
        List<AliasLdap> aliasLdaps = ldapTemplate.search(ldapProperties.getListSearchBase(), formattedFilter, new AliasLdapAttributesMapper());
        aliasLdaps.removeAll(Collections.singleton(null));
        return  aliasLdaps;
    }
}
