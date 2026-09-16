package org.esupportail.esupsignature;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.security.WebSecurityProperties;
import org.esupportail.esupsignature.entity.Config;
import org.esupportail.esupsignature.repository.ConfigRepository;
import org.esupportail.esupsignature.service.ConfigService;
import org.esupportail.esupsignature.service.ldap.LdapGroupService;
import org.esupportail.esupsignature.service.security.SpelGroupService;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfigServiceTest {

    @Test
    void refreshMappingsReloadsEveryMappingAndKeepsTheEffectiveRoleMapReference() {
        WebSecurityProperties webSecurityProperties = new WebSecurityProperties();
        webSecurityProperties.setMappingGroupsRoles(new HashMap<>(Map.of(
                "yaml-only", "ROLE_YAML",
                "shared-group", "ROLE_FROM_YAML")));
        Map<String, String> effectiveMappings = webSecurityProperties.getMappingGroupsRoles();

        Config config = new Config();
        config.getMappingGroupsRoles().put("shared-group", "ROLE_FROM_CONFIG");
        config.getMappingGroupsRoles().put("database-only", "ROLE_DATABASE");
        ConfigRepository configRepository = mock(ConfigRepository.class);
        when(configRepository.findAll()).thenReturn(List.of(config));

        LdapGroupService ldapGroupService = mock(LdapGroupService.class);
        SpelGroupService spelGroupService = mock(SpelGroupService.class);
        ConfigService service = new ConfigService(
                configRepository,
                ldapGroupService,
                webSecurityProperties,
                spelGroupService,
                mock(GlobalProperties.class));

        service.refreshMappings();

        assertThat(webSecurityProperties.getMappingGroupsRoles()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "yaml-only", "ROLE_YAML",
                "shared-group", "ROLE_FROM_CONFIG",
                "database-only", "ROLE_DATABASE"));
        assertThat(webSecurityProperties.getMappingGroupsRoles()).isSameAs(effectiveMappings);
        verify(ldapGroupService).loadLdapFiltersGroups();
        verify(spelGroupService).initGroupMappingSpel();
    }

}
