package org.esupportail.esupsignature.config.ldap;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "ldap")
public class LdapProperties {

    private String searchBase = "ou=people";
    private String groupSearchBase = "ou=groups";
    private String groupSearchFilter = "member={0}";
    private String memberSearchFilter = "(&(uid={0})({1}))";
    private String searchFilter = "(uid={0})";
    private String affiliationFilter = "member";
    private String groupPrefixRoleName = "";
    private String domain;
    private Map<String, String> ldapFiltersGroups = new HashMap<>();

    public String getSearchBase() {
        return searchBase;
    }

    public void setSearchBase(String searchBase) {
        this.searchBase = searchBase;
    }

    public String getGroupSearchBase() {
        return groupSearchBase;
    }

    public void setGroupSearchBase(String groupSearchBase) {
        this.groupSearchBase = groupSearchBase;
    }

    public String getGroupSearchFilter() {
        return groupSearchFilter;
    }

    public void setGroupSearchFilter(String groupSearchFilter) {
        this.groupSearchFilter = groupSearchFilter;
    }

    public String getMemberSearchFilter() {
        return memberSearchFilter;
    }

    public void setMemberSearchFilter(String memberSearchFilter) {
        this.memberSearchFilter = memberSearchFilter;
    }

    public String getSearchFilter() {
        return searchFilter;
    }

    public void setSearchFilter(String searchFilter) {
        this.searchFilter = searchFilter;
    }

    public String getAffiliationFilter() {
        return affiliationFilter;
    }

    public void setAffiliationFilter(String affiliationFilter) {
        this.affiliationFilter = affiliationFilter;
    }

    public String getGroupPrefixRoleName() {
        return groupPrefixRoleName;
    }

    public void setGroupPrefixRoleName(String groupPrefixRoleName) {
        this.groupPrefixRoleName = groupPrefixRoleName;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Map<String, String> getLdapFiltersGroups() {
        return ldapFiltersGroups;
    }

    public void setLdapFiltersGroups(Map<String, String> ldapFiltersGroups) {
        this.ldapFiltersGroups = ldapFiltersGroups;
    }
}
