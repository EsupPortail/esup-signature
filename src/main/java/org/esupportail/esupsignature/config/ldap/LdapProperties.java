package org.esupportail.esupsignature.config.ldap;

import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "ldap")
public class LdapProperties {

    /**
     * Base de recherche des utilisateurs, ex : ou=people
     */
    private String searchBase = "ou=people";
    /**
     * Filtre de recherche des utilisateurs
     */
    private String usersSearchFilter = "(&(|(displayName={0}*)(cn={0}*)(uid={0})(mail={0}*))(mail=*))";
    /**
     * Base de recherche des groupes, ex : ou=groups
     */
    private String groupSearchBase = "ou=groups";
    /**
     * Filtre utilisé pour rechercher les groupes d’un utilisateur, ex : member={0}
     */
    private String groupSearchFilter = "member={0}";
    /**
     * Filtre utilisé pour rechercher des groupes, ex : cn=*{0}
     */
    private String allGroupsSearchFilter = "cn=*{0}";
    /**
     * Filtre utilisé pour retrouver les membres d’un groupe, ex : memberOf=cn={0},ou=groups,dc=univ-ville,dc=fr
     */
    private String membersOfGroupSearchFilter = "memberOf=cn={0},ou=groups,dc=univ-ville,dc=fr";
    /**
     * Filtre pour contrôler l’appartenance d’un utilisateur à un groupe, ex : &(uid={0})({1}))
     */
    private String memberSearchFilter = "&(uid={0})({1}))";
    /**
     * Le champ dans lequel on trouve le login des utilisateurs récupéré au moment de l’authentification, ex : (uid={0})
     */
    private String userIdSearchFilter = "(uid={0})";
    /**
     * Le champ dans lequel on trouve l'eppn des utilisateurs c'est ce champ qui sera utilisé comme identifiant unique en base, ex : (eduPersonPrincipalName={0})
     */
    private String userEppnSearchFilter = "(eduPersonPrincipalName={0})";
    /**
     * Le champ dans lequel on trouve l'email des utilisateurs , ex : (mail={0})
     */
    private String userMailSearchFilter = "(mail={0})";
    /**
     * Le champ dans lequel on trouve la partie gauche de l’EPPN (par défaut idem userIdSearchFilter)
     */
    private String eppnLeftPartSearchFilter;
    /**
     * Requete pour trouver les OU
     */
    private String ouSearchFilter = "(supannCodeEntite={0})";
    /**
     * Object classes correspondant aux utilisateurs (un "ou" est appliqué aux valeurs de cette liste)
     */
    private List<String> userObjectClasses = List.of("inetOrgPerson");
    /**
     * Object classes correspondant aux groupes (un "ou" est appliqué aux valeurs de cette liste)
     */
    private List<String> groupObjectClasses = List.of("groupOfNames");
    /**
     * Object classes correspondant aux OU (un "ou" est appliqué aux valeurs de cette liste)
     */
    private List<String> ouObjectClasses = List.of("organizationalUnit");
    /**
     * Object classes correspondant aux alias (un "ou" est appliqué aux valeurs de cette liste)
     */
    private List<String> aliasObjectClasses = List.of("nisMailAlias");

    @PostConstruct
    private void initEppnLeftPartSearchFilter() {
        if(eppnLeftPartSearchFilter == null) {
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
