package org.esupportail.esupsignature.config.security.cas;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.cas")
public class CasProperties {

    private String title;
    private String url;
    private String service;
    private String groupMappingRoleAdmin;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

}
