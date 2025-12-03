package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.security.WebSecurityProperties;
import org.esupportail.esupsignature.entity.Config;
import org.esupportail.esupsignature.repository.ConfigRepository;
import org.esupportail.esupsignature.service.ldap.LdapGroupService;
import org.esupportail.esupsignature.service.security.SpelGroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Iterator;

@Service
public class ConfigService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);

    private final ConfigRepository configRepository;
    private final LdapGroupService ldapGroupService;
    private final WebSecurityProperties webSecurityProperties;
    private final SpelGroupService spelGroupService;
    private final GlobalProperties globalProperties;

    public ConfigService(ConfigRepository configRepository, @Autowired(required = false) LdapGroupService ldapGroupService, WebSecurityProperties webSecurityProperties, SpelGroupService spelGroupService, GlobalProperties globalProperties) {
        this.configRepository = configRepository;
        this.ldapGroupService = ldapGroupService;
        this.webSecurityProperties = webSecurityProperties;
        this.spelGroupService = spelGroupService;
        this.globalProperties = globalProperties;
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

    @Transactional
    public void addMappingFiltersGroups(String group, String filter) {
        getConfig().getMappingFiltersGroups().put(group, filter);
        if(ldapGroupService != null) {
            ldapGroupService.loadLdapFiltersGroups();
        }
    }

    @Transactional
    public void deleteMappingFiltersGroups(String group) {
        getConfig().getMappingFiltersGroups().remove(group);
        if(ldapGroupService != null) {
            ldapGroupService.loadLdapFiltersGroups();
        }
    }

    @Transactional
    public void addMappingGroupsRoles(String group, String role) {
        getConfig().getMappingGroupsRoles().put(group, role);
        webSecurityProperties.getMappingGroupsRoles().put(group, role);
    }

    @Transactional
    public void deleteMappingGroupsRoles(String group) {
        getConfig().getMappingGroupsRoles().remove(group);
        webSecurityProperties.getMappingGroupsRoles().remove(group);
    }

    @Transactional
    public void addGroupMappingSpel(String group, String spel) {
        getConfig().getGroupMappingSpel().put(group, spel);
        if(spelGroupService != null) {
            spelGroupService.initGroupMappingSpel();
        }
    }

    @Transactional
    public void deleteGroupMappingSpel(String group) {
        getConfig().getGroupMappingSpel().remove(group);
        if(spelGroupService != null) {
            spelGroupService.initGroupMappingSpel();
        }
    }

    @Transactional
    public void updateHideAutoSign(Boolean hideAutoSign) {
        getConfig().setHideAutoSign(hideAutoSign);
        if(globalProperties != null) {
            globalProperties.setHideAutoSign(hideAutoSign);
        }
    }

}
