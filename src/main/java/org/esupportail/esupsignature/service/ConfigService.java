package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.security.WebSecurityProperties;
import org.esupportail.esupsignature.dto.page.admin.AdminConfigViewDto;
import org.esupportail.esupsignature.entity.Config;
import org.esupportail.esupsignature.repository.ConfigRepository;
import org.esupportail.esupsignature.service.ldap.LdapGroupService;
import org.esupportail.esupsignature.service.security.SpelGroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ConfigService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);

    private final ConfigRepository configRepository;
    private final LdapGroupService ldapGroupService;
    private final WebSecurityProperties webSecurityProperties;
    private final SpelGroupService spelGroupService;
    private final GlobalProperties globalProperties;
    private final Map<String, String> yamlMappingGroupsRoles = new HashMap<>();

    public ConfigService(ConfigRepository configRepository, @Autowired(required = false) LdapGroupService ldapGroupService,
                         WebSecurityProperties webSecurityProperties, SpelGroupService spelGroupService,
                         GlobalProperties globalProperties) {
        this.configRepository = configRepository;
        this.ldapGroupService = ldapGroupService;
        this.webSecurityProperties = webSecurityProperties;
        this.spelGroupService = spelGroupService;
        this.globalProperties = globalProperties;
        yamlMappingGroupsRoles.putAll(webSecurityProperties.getMappingGroupsRoles());
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void loadMappingGroupsRoles() {
        Map<String, String> effectiveMappings = webSecurityProperties.getMappingGroupsRoles();
        effectiveMappings.clear();
        yamlMappingGroupsRoles.forEach((group, role) -> {
            if(validateGroupName(group, "security.web.mapping-groups-roles")) {
                effectiveMappings.put(group, role);
            }
        });
        Iterator<Config> configs = configRepository.findAll().iterator();
        if(configs.hasNext()) {
            configs.next().getMappingGroupsRoles()
                    .forEach((group, role) -> {
                        if(validateGroupName(group, "config_mapping_groups_roles")) {
                            effectiveMappings.put(group, role);
                        }
                    });
        }
    }

    @Transactional
    public synchronized void refreshMappings() {
        loadMappingGroupsRoles();
        spelGroupService.initGroupMappingSpel();
        if(ldapGroupService != null) {
            ldapGroupService.loadLdapFiltersGroups();
        }
    }

    private boolean validateGroupName(String groupName, String source) {
        if(groupName != null && groupName.trim().endsWith(":")) {
            logger.error("Invalid group name [{}] in {}: the trailing ':' is YAML syntax and must not be part of the group name",
                    groupName, source);
            return false;
        }
        return true;
    }

    @Transactional
    public Config getConfig() {
        Iterator<Config> configs = configRepository.findAll().iterator();
        if(configs.hasNext()) {
            return configs.next();
        } else {
            return configRepository.save(new Config());
        }
    }

    @Transactional(readOnly = true)
    public AdminConfigViewDto getAdminConfigView() {
        Config config = getConfig();
        AdminConfigViewDto dto = new AdminConfigViewDto();
        dto.setMappingFiltersGroups(new LinkedHashMap<>(config.getMappingFiltersGroups()));
        dto.setMappingGroupsRoles(new LinkedHashMap<>(config.getMappingGroupsRoles()));
        dto.setGroupMappingSpel(new LinkedHashMap<>(config.getGroupMappingSpel()));
        dto.setHideAutoSign(config.getHideAutoSign());
        return dto;
    }

    @Transactional
    public void addMappingFiltersGroups(String group, String filter) {
        validateGroupName(group, "config_mapping_filters_groups");
        getConfig().getMappingFiltersGroups().put(group, filter);
        refreshMappings();
    }

    @Transactional
    public void deleteMappingFiltersGroups(String group) {
        getConfig().getMappingFiltersGroups().remove(group);
        refreshMappings();
    }

    @Transactional
    public void addMappingGroupsRoles(String group, String role) {
        validateGroupName(group, "config_mapping_groups_roles");
        getConfig().getMappingGroupsRoles().put(group, role);
        refreshMappings();
    }

    @Transactional
    public void deleteMappingGroupsRoles(String group) {
        getConfig().getMappingGroupsRoles().remove(group);
        refreshMappings();
    }

    @Transactional
    public void addGroupMappingSpel(String group, String spel) {
        getConfig().getGroupMappingSpel().put(group, spel);
        refreshMappings();
    }

    @Transactional
    public void deleteGroupMappingSpel(String group) {
        getConfig().getGroupMappingSpel().remove(group);
        refreshMappings();
    }

    @Transactional
    public void updateHideAutoSign(Boolean hideAutoSign) {
        getConfig().setHideAutoSign(hideAutoSign);
        if(globalProperties != null) {
            globalProperties.setHideAutoSign(hideAutoSign);
        }
    }

}
