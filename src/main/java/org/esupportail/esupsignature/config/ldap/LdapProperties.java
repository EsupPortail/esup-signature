package org.esupportail.esupsignature.config.ldap;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.annotation.PostConstruct;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "ldap")
public class LdapProperties {

    /**
     * Base de recherche des utilisateurs, ex : ou=people
     */
    private String searchBase = "";
    /**
     * Filtre de recherche des utilisateurs
     */
    private String usersSearchFilter;
    /**
     * Base de recherche des groupes, ex : ou=groups
     */
    private String groupSearchBase;
    /**
     * Filtre utilisé pour rechercher les groupes d’un utilisateur, ex : member={0}
     */
    private String groupSearchFilter;
    /**
     * Filtre utilisé pour rechercher des groupes, ex : cn=*{0}
     */
    private String allGroupsSearchFilter;
    /**
     * Filtre utilisé pour rechercher des aliases, ex : (mail=*{0})
     */
    private String allAliasesSearchFilter;
    /**
     * Filtre utilisé pour retrouver les membres d’un groupe, ex : memberOf=cn={0},ou=groups,dc=univ-ville,dc=fr
     */
    private String membersOfGroupSearchFilter;
    /**
     * Filtre pour contrôler l’appartenance d’un utilisateur à un groupe, ex : (&(uid={0})({1}))
     */
    private String memberSearchFilter;
    /**
     * Le champ dans lequel on trouve le login des utilisateurs récupéré au moment de l’authentification, ex : (uid={0})
     */
    private String userIdSearchFilter;
    /**
     * Le champ dans lequel on trouve l'eppn des utilisateurs c'est ce champ qui sera utilisé comme identifiant unique en base, ex : (eduPersonPrincipalName={0})
     */
    private String userEppnSearchFilter;
    /**
     * Le champ dans lequel on trouve l'email des utilisateurs , ex : (mail={0})
     */
    private String userMailSearchFilter;
    /**
     * Le champ dans lequel on trouve la partie gauche de l’EPPN (par défaut idem userIdSearchFilter)
     */
    private String eppnLeftPartSearchFilter;
    /**
     * Requete pour trouver les OU des utilisateurs (utile seulement pour le pré-remplissage de l'affectation dans les formulaires)
     */
    private String ouSearchFilter;
    /**
     * Object classes correspondant aux utilisateurs (un "ou" est appliqué aux valeurs de cette liste)
     */
    private List<String> userObjectClasses;
    /**
     * Object classes correspondant aux groupes (un "ou" est appliqué aux valeurs de cette liste)
     */
    private List<String> groupObjectClasses;
    /**
     * Object classes correspondant aux OU (un "ou" est appliqué aux valeurs de cette liste)
     */
    private List<String> ouObjectClasses;
    /**
     * Object classes correspondant aux alias (un "ou" est appliqué aux valeurs de cette liste)
     */
    private List<String> aliasObjectClasses;

    @PostConstruct
    private void initEppnLeftPartSearchFilter() {
        if(!StringUtils.hasText(eppnLeftPartSearchFilter)) {
            eppnLeftPartSearchFilter = userIdSearchFilter;
        }
    }

    private Map<String, String> mappingFiltersGroups = new HashMap<>();

    public String getSearchBase() {
        return searchBase;
    }

    public void setSearchBase(String searchBase) {
        this.searchBase = searchBase;
    }

    public String getUsersSearchFilter() {
        return usersSearchFilter;
    }

    public void setUsersSearchFilter(String usersSearchFilter) {
        this.usersSearchFilter = usersSearchFilter;
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

    public String getAllAliasesSearchFilter() {
        return allAliasesSearchFilter;
    }

    public void setAllAliasesSearchFilter(String allAliasesSearchFilter) {
        this.allAliasesSearchFilter = allAliasesSearchFilter;
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

    public String getUserEppnSearchFilter() {
        return userEppnSearchFilter;
    }

    public void setUserEppnSearchFilter(String userEppnSearchFilter) {
        this.userEppnSearchFilter = userEppnSearchFilter;
    }

    public String getUserMailSearchFilter() {
        return userMailSearchFilter;
    }

    public void setUserMailSearchFilter(String userMailSearchFilter) {
        this.userMailSearchFilter = userMailSearchFilter;
    }

    public Map<String, String> getMappingFiltersGroups() {
        return mappingFiltersGroups;
    }

    public void setMappingFiltersGroups(Map<String, String> mappingFiltersGroups) {
        this.mappingFiltersGroups = mappingFiltersGroups;
    }

    public String getEppnLeftPartSearchFilter() {
        return eppnLeftPartSearchFilter;
    }

    public void setEppnLeftPartSearchFilter(String eppnLeftPartSearchFilter) {
        this.eppnLeftPartSearchFilter = eppnLeftPartSearchFilter;
    }

    public String getOuSearchFilter() {
        return ouSearchFilter;
    }

    public void setOuSearchFilter(String ouSearchFilter) {
        this.ouSearchFilter = ouSearchFilter;
    }

    public List<String> getUserObjectClasses() {
        return userObjectClasses;
    }

    public void setUserObjectClasses(List<String> userObjectClasses) {
        this.userObjectClasses = userObjectClasses;
    }

    public List<String> getGroupObjectClasses() {
        return groupObjectClasses;
    }

    public void setGroupObjectClasses(List<String> groupObjectClasses) {
        this.groupObjectClasses = groupObjectClasses;
    }

    public List<String> getOuObjectClasses() {
        return ouObjectClasses;
    }

    public void setOuObjectClasses(List<String> ouObjectClasses) {
        this.ouObjectClasses = ouObjectClasses;
    }

    public List<String> getAliasObjectClasses() {
        return aliasObjectClasses;
    }

    public void setAliasObjectClasses(List<String> aliasObjectClasses) {
        this.aliasObjectClasses = aliasObjectClasses;
    }
}
