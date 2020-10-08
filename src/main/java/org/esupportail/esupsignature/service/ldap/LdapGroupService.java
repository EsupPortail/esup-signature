package org.esupportail.esupsignature.service.ldap;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.esupportail.esupsignature.service.security.GroupService;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.LdapTemplate;

public class LdapGroupService implements GroupService {

    Map<String, String> ldapFiltersGroups;

    private LdapTemplate ldapTemplate;

    private String groupSearchBase;

    private String groupSearchFilter;

    private String memberSearchBase;

    private String memberSearchFilter;

    private String domain;

    public Map<String, String> getLdapFiltersGroups() {
        return ldapFiltersGroups;
    }

    public void setLdapFiltersGroups(Map<String, String> ldapFiltersGroups) {
        this.ldapFiltersGroups = ldapFiltersGroups;
    }

    public void setLdapTemplate(LdapTemplate ldapTemplate) {
        this.ldapTemplate = ldapTemplate;
    }

    public void setGroupSearchBase(String groupSearchBase) {
        this.groupSearchBase = groupSearchBase;
    }

    public void setGroupSearchFilter(String groupSearchFilter) {
        this.groupSearchFilter = groupSearchFilter;
    }

    public void setMemberSearchBase(String memberSearchBase) {
        this.memberSearchBase = memberSearchBase;
    }

    public void setMemberSearchFilter(String memberSearchFilter) {
        this.memberSearchFilter = memberSearchFilter;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    @Override
    public List<String> getGroups(String eppn) {

        String username = eppn.replaceAll("@.*", "");

        List<String> dns = ldapTemplate.search(query().where("uid").is(username),
                (ContextMapper<String>) ctx -> {
                    DirContextAdapter searchResultContext = (DirContextAdapter) ctx;
                    String dn = searchResultContext.getNameInNamespace();
                    return dn;
                });

        List<String> groups = new ArrayList<>();

        if(!dns.isEmpty()) {
            String userDn = dns.get(0);
            String formattedFilter = MessageFormat.format(groupSearchFilter, new String[] { userDn, username });

            groups = ldapTemplate.search(
                    groupSearchBase, formattedFilter, (ContextMapper<String>) ctx -> {
                        DirContextAdapter searchResultContext = (DirContextAdapter)ctx;
                        String groupName = searchResultContext.getStringAttribute("cn");
                        return "ROLE_" + groupName.toUpperCase();
                    });
        }

        for(String ldapFilter: ldapFiltersGroups.keySet()) {

            String hardcodedFilter = MessageFormat.format(memberSearchFilter, new String[] {username, ldapFilter});

            List<String> filterDns = ldapTemplate.search(query().filter(hardcodedFilter),
                    (ContextMapper<String>) ctx -> {
                        DirContextAdapter searchResultContext = (DirContextAdapter)ctx;
                        String dn = searchResultContext.getNameInNamespace();
                        return dn;
                    });

            if(!filterDns.isEmpty()) {
                groups.add(ldapFiltersGroups.get(ldapFilter));
            }

        }
        return groups;
    }

    @Override
    public List<String> getMembers(String groupName) {

        String formattedFilter = MessageFormat.format(memberSearchFilter, new String[] {groupName});

        List<String> eppns = ldapTemplate.search(
                memberSearchBase, formattedFilter, (ContextMapper<String>) ctx -> {
                    DirContextAdapter searchResultContext = (DirContextAdapter)ctx;
                    String eppn = searchResultContext.getStringAttribute("eduPersonPrincipalName");
                    return eppn;
                });

        return eppns;
    }

}