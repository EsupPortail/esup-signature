package org.esupportail.esupsignature.config.security.shib;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.shib")
public class ShibProperties {

    private String idpUrl;
    private String principalRequestHeader;
    private String credentialsRequestHeader;
    private String groupMappingRoleAdmin;
    private String groupMappingRoleManager;

    public String getIdpUrl() {
        return idpUrl;
    }

    public void setIdpUrl(String idpUrl) {
        this.idpUrl = idpUrl;
    }

    public String getPrincipalRequestHeader() {
        return principalRequestHeader;
    }

    public void setPrincipalRequestHeader(String principalRequestHeader) {
        this.principalRequestHeader = principalRequestHeader;
    }

    public String getCredentialsRequestHeader() {
        return credentialsRequestHeader;
    }

    public void setCredentialsRequestHeader(String credentialsRequestHeader) {
        this.credentialsRequestHeader = credentialsRequestHeader;
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
}
