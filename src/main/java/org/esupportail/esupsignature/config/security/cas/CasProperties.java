package org.esupportail.esupsignature.config.security.cas;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.cas")
public class CasProperties {

    private String title;
    private String key;
    private String url;
    private String service;
    private String groupMappingRoleAdmin;
    private String groupMappingRoleManager;
    private String domain;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getGroupMappingRoleAdmin() {
        return groupMappingRoleAdmin;
    }

    public void setGroupMappingRoleAdmin(String groupMappingRoleAdmin) {
        this.groupMappingRoleAdmin = groupMappingRoleAdmin;
    }

    public String getGroupMappingRoleManager() {
        return groupMappingRoleManager;
    }

    public void setGroupMappingRoleManager(String groupMappingRoleManager) {
        this.groupMappingRoleManager = groupMappingRoleManager;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
}
