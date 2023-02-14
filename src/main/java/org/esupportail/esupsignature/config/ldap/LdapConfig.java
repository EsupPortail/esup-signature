package org.esupportail.esupsignature.config.ldap;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.service.ldap.LdapGroupService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.ldap.repository.config.EnableLdapRepositories;
import org.springframework.ldap.core.LdapTemplate;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty({"spring.ldap.base", "ldap.search-base"})
@EnableConfigurationProperties({GlobalProperties.class, LdapProperties.class})
@EnableLdapRepositories(basePackages = "org.esupportail.esupsignature.repository.ldap")
public class LdapConfig {

    private final LdapProperties ldapProperties;

    private final GlobalProperties globalProperties;

    @Resource
    private LdapTemplate ldapTemplate;

    public LdapConfig(LdapProperties ldapProperties, GlobalProperties globalProperties) {
        this.ldapProperties = ldapProperties;
        this.globalProperties = globalProperties;
    }

    @Bean
    public LdapGroupService ldapGroupService() {
        Map<String, String> ldapFiltersGroups = new HashMap<>();

        for(Map.Entry<String, String> entry : ldapProperties.getMappingFiltersGroups().entrySet()) {
            ldapFiltersGroups.put(entry.getValue(), entry.getKey());
        }
        LdapGroupService ldapGroupService = new LdapGroupService();
        ldapGroupService.setLdapFiltersGroups(ldapFiltersGroups);
        ldapGroupService.setLdapTemplate(ldapTemplate);
        ldapGroupService.setGroupSearchBase(ldapProperties.getGroupSearchBase());
        ldapGroupService.setGroupSearchFilter(ldapProperties.getGroupSearchFilter());
        ldapGroupService.setAllGroupsSearchFilter(ldapProperties.getAllGroupsSearchFilter());
        ldapGroupService.setMembersOfGroupSearchFilter(ldapProperties.getMembersOfGroupSearchFilter());
        ldapGroupService.setMemberSearchBase(ldapProperties.getSearchBase());
        ldapGroupService.setMemberSearchFilter(ldapProperties.getMemberSearchFilter());
        ldapGroupService.setDomain(globalProperties.getDomain());
        return ldapGroupService;
    }
    
}
