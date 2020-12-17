package org.esupportail.esupsignature.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix="security.web")
public class WebSecurityProperties {

    private String[] wsAccessAuthorizeIps;
    private String groupPrefixRoleName;
    private Map<String, String> mappingGroupsRoles;
    private Map<String, String> groupMappingSpel;

    public String[] getWsAccessAuthorizeIps() {
        return wsAccessAuthorizeIps;
    }

    public void setWsAccessAuthorizeIps(String[] wsAccessAuthorizeIps) { this.wsAccessAuthorizeIps = wsAccessAuthorizeIps; }

    public String getGroupPrefixRoleName() {
        return groupPrefixRoleName;
    }

    public void setGroupPrefixRoleName(String groupPrefixRoleName) {
        this.groupPrefixRoleName = groupPrefixRoleName;
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
}
