package org.esupportail.esupsignature.config.ldap;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "ldap")
public class LdapProperties {

    private String searchBase;
    private String groupSearchBase;
    private String groupSearchFilter;
    private String memberSearchFilter;
    private String searchFilter;
    private String affiliationFilter;
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

    public Map<String, String> getLdapFiltersGroups() {
        return ldapFiltersGroups;
    }

    public void setLdapFiltersGroups(Map<String, String> ldapFiltersGroups) {
        this.ldapFiltersGroups = ldapFiltersGroups;
    }
}
