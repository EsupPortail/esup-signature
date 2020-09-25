package org.esupportail.esupsignature.config.ldap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ldap")
public class LdapProperties {

    private String searchBase = "ou=people";
    private String searchFilter = "(uid={0})";
    private String affiliationFilter = "member";

    public String getSearchBase() {
        return searchBase;
    }

    public void setSearchBase(String searchBase) {
        this.searchBase = searchBase;
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
}
