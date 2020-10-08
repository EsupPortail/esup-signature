package org.esupportail.esupsignature.config.ldap;

import org.esupportail.esupsignature.service.ldap.LdapGroupService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.LdapTemplate;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(prefix = "spring.ldap", name = "base")
@EnableConfigurationProperties(LdapProperties.class)
public class LdapConfig {

    @Resource
    private LdapProperties ldapProperties;

    @Resource
    private LdapTemplate ldapTemplate;
    
    @Bean
    public LdapGroupService ldapGroupService() {
        Map<String, String> ldapFiltersGroups = new HashMap<>();

        for(Map.Entry<String, String> entry : ldapProperties.getLdapFiltersGroups().entrySet()) {
            ldapFiltersGroups.put(entry.getValue(), ldapProperties.getGroupPrefixRoleName() + ".ROLE." + entry.getKey().toUpperCase());
        }

        LdapGroupService ldapGroupService = new LdapGroupService();
        ldapGroupService.setLdapFiltersGroups(ldapFiltersGroups);
        ldapGroupService.setLdapTemplate(ldapTemplate);
        ldapGroupService.setGroupSearchBase(ldapProperties.getGroupSearchBase());
        ldapGroupService.setGroupSearchFilter(ldapProperties.getGroupSearchFilter());
        ldapGroupService.setMemberSearchBase(ldapProperties.getSearchBase());
        ldapGroupService.setMemberSearchFilter(ldapProperties.getMemberSearchFilter());
        ldapGroupService.setDomain(ldapProperties.getDomain());
        //todo mettre en conf

        return ldapGroupService;
    }
    
}
