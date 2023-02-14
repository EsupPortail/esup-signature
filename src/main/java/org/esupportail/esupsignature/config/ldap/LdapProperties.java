package org.esupportail.esupsignature.config.ldap;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "ldap")
public class LdapProperties {

    /**
     * Base de recherche des utilisateurs, ex : ou=people
     */
    private String searchBase;
    /**
     * Base de recherche des groupes, ex : ou=groups
     */
    private String groupSearchBase;
    /**
     * Filtre utilisé pour rechercher les groupes d’un utilisateur, ex : member={0}
     */
    private String groupSearchFilter;
    /**
     * Filtre utilisé pour rechercher des groupes, ex : mail=*{0}
     */
    private String allGroupsSearchFilter;
    /**
     * Filtre utilisé pour retrouver les membres d’un groupe, ex : memberOf=cn={0},ou=groups,dc=univ-ville,dc=fr
     */
    private String membersOfGroupSearchFilter;
    /**
     * Filtre pour contrôler l’appartenance d’un utilisateur à un groupe, ex : &(uid={0})({1}))
     */
    private String memberSearchFilter;
    /**
     * Le champ dans lequel on trouve le login des utilisateurs, ex : (uid={0})
     */
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
