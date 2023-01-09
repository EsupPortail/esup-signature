package org.esupportail.esupsignature.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix="security.web")
public class WebSecurityProperties {

    private String[] wsAccessAuthorizeIps;
    private String groupToRoleFilterPattern;
    private Map<String, String> mappingGroupsRoles;
    private Map<String, String> groupMappingSpel;
    private String franceConnectAcr = "eidas1";
    private String franceConnectLogoutEndpoint;
    private String[] excludedEmails = new String[] {"vincent.bonamy.univ-rouen.fr"};

    public String[] getWsAccessAuthorizeIps() {
        return wsAccessAuthorizeIps;
    }

    public void setWsAccessAuthorizeIps(String[] wsAccessAuthorizeIps) { this.wsAccessAuthorizeIps = wsAccessAuthorizeIps; }

    public String getGroupToRoleFilterPattern() {
        return groupToRoleFilterPattern;
    }

    public void setGroupToRoleFilterPattern(String groupToRoleFilterPattern) {
        this.groupToRoleFilterPattern = groupToRoleFilterPattern;
    }

    public Map<String, String> getMappingGroupsRoles() {
        return mappingGroupsRoles;
    }

    public void setMappingGroupsRoles(Map<String, String> mappingGroupsRoles) {
        this.mappingGroupsRoles = mappingGroupsRoles;
    }

    public Map<String, String> getGroupMappingSpel() {
        return groupMappingSpel;
    }

    public void setGroupMappingSpel(Map<String, String> groupMappingSpel) {
        this.groupMappingSpel = groupMappingSpel;
    }

    public String getFranceConnectAcr() {
        return franceConnectAcr;
    }

    public void setFranceConnectAcr(String franceConnectAcr) {
        this.franceConnectAcr = franceConnectAcr;
    }

    public String getFranceConnectLogoutEndpoint() {
        return franceConnectLogoutEndpoint;
    }

    public void setFranceConnectLogoutEndpoint(String franceConnectLogoutEndpoint) {
        this.franceConnectLogoutEndpoint = franceConnectLogoutEndpoint;
    }

    public String[] getExcludedEmails() {
        return excludedEmails;
    }

    public void setExcludedEmails(String[] excludedEmails) {
        this.excludedEmails = excludedEmails;
    }
}
