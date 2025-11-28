package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.Config;
import org.esupportail.esupsignature.repository.ConfigRepository;
import org.esupportail.esupsignature.service.ldap.LdapGroupService;
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

    public ConfigService(ConfigRepository configRepository, @Autowired(required = false) LdapGroupService ldapGroupService) {
        this.configRepository = configRepository;
        this.ldapGroupService = ldapGroupService;
    }

    @Transactional
    public Config getConfig() {
        Iterator<Config> configs = configRepository.findAll().iterator();
        if(configs.hasNext()) return configs.next();
        return null;
    }

    @Transactional
    public void addMappingFiltersGroups(String group, String filter) {
        Config config = getConfig();
        if(config != null) {
            config.getMappingFiltersGroups().put(group, filter);
        }
        if(ldapGroupService != null) {
            ldapGroupService.loadLdapFiltersGroups();
        }
    }

}
