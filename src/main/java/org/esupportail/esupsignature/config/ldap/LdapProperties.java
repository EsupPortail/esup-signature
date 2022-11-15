package org.esupportail.esupsignature.config.ldap;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "ldap")
public class LdapProperties {

    private String searchBase;
    private String groupSearchBase;
    private String groupSearchFilter;
    private String allGroupsSearchFilter;
    private String membersOfGroupSearchFilter;
    private String memberSearchFilter;
    private String userIdSearchFilter;
    private Map<String, String> mappingFiltersGroups = new HashMap<>();

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

    public String getAllGroupsSearchFilter() {
        return allGroupsSearchFilter;
    }

    public void setAllGroupsSearchFilter(String allGroupsSearchFilter) {
        this.allGroupsSearchFilter = allGroupsSearchFilter;
    }

    public String getMembersOfGroupSearchFilter() {
        return membersOfGroupSearchFilter;
    }

    public void setMembersOfGroupSearchFilter(String membersOfGroupSearchFilter) {
        this.membersOfGroupSearchFilter = membersOfGroupSearchFilter;
    }

    public String getMemberSearchFilter() {
        return memberSearchFilter;
    }

    public void setMemberSearchFilter(String memberSearchFilter) {
        this.memberSearchFilter = memberSearchFilter;
    }

    public String getUserIdSearchFilter() {
        return userIdSearchFilter;
    }

    public void setUserIdSearchFilter(String userIdSearchFilter) {
        this.userIdSearchFilter = userIdSearchFilter;
    }

    public Map<String, String> getMappingFiltersGroups() {
        return mappingFiltersGroups;
    }

    public void setMappingFiltersGroups(Map<String, String> mappingFiltersGroups) {
        this.mappingFiltersGroups = mappingFiltersGroups;
    }
}
