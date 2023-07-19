package org.esupportail.esupsignature.service.ldap;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.ldap.LdapProperties;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.security.GroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty({"spring.ldap.base", "ldap.search-base"})
@EnableConfigurationProperties({GlobalProperties.class, LdapProperties.class})
public class LdapGroupService implements GroupService {

    private static final Logger logger = LoggerFactory.getLogger(LdapGroupService.class);

    Map<String, String> ldapFiltersGroups = new HashMap<>();

    private final LdapProperties ldapProperties;

    private final GlobalProperties globalProperties;

    @Resource
    private LdapTemplate ldapTemplate;

    public LdapGroupService(LdapProperties ldapProperties, GlobalProperties globalProperties) {
        this.ldapProperties = ldapProperties;
        this.globalProperties = globalProperties;
        for(Map.Entry<String, String> entry : ldapProperties.getMappingFiltersGroups().entrySet()) {
            ldapFiltersGroups.put(entry.getValue(), entry.getKey());
        }
    }

    public Map<String, String> getLdapFiltersGroups() {
        return ldapFiltersGroups;
    }

    @Override
    public List<Map.Entry<String, String>> getAllGroupsStartWith(String search) {
        List<Map.Entry<String, String>> groups = new ArrayList<>();
        logger.debug("search groups by name");
        if(ldapProperties.getAllGroupsSearchFilter() != null) {
            String formattedFilter = MessageFormat.format(ldapProperties.getAllGroupsSearchFilter(), search);
            StringBuilder objectClasses = new StringBuilder();
            for(String objectClass : ldapProperties.getGroupObjectClasses()) {
                objectClasses.append("(objectClass=").append(objectClass).append(")");
            }
            formattedFilter = "(&(|" + objectClasses + ")(" + formattedFilter + "))";
            logger.debug(formattedFilter);
            groups = ldapTemplate.search(LdapQueryBuilder.query().attributes("cn", "description").base(ldapProperties.getGroupSearchBase()).filter(formattedFilter),
                    (ContextMapper<Map.Entry<String, String>>) ctx -> {
                        DirContextAdapter searchResultContext = (DirContextAdapter) ctx;
                        return new AbstractMap.SimpleEntry<>(searchResultContext.getStringAttribute("cn"), searchResultContext.getStringAttribute("description"));
                    });
        }
        return groups;
    }

    public List<String>  getAllPrefixGroups(String search) {
        return ldapTemplate.search(LdapQueryBuilder.query().attributes("cn", "description").base(ldapProperties.getGroupSearchBase()).filter("cn=" + search.replace("(\\w*)", "") + "*"),
                (ContextMapper<Map.Entry<String, String>>) ctx -> {
                    DirContextAdapter searchResultContext = (DirContextAdapter) ctx;
                    return new AbstractMap.SimpleEntry<>(searchResultContext.getStringAttribute("cn"), searchResultContext.getStringAttribute("description"));
                }).stream().map(Map.Entry::getKey).collect(Collectors.toList());
    }

    @Override
    public List<String> getGroupsOfUser(String username) {
        String formattedFilter = MessageFormat.format(ldapProperties.getEppnLeftPartSearchFilter(), (Object[]) new String[] { username });
        logger.debug("search GroupLdap with : " + formattedFilter);
        List<String> dns = ldapTemplate.search(LdapQueryBuilder.query().attributes("dn").filter(formattedFilter),
                (ContextMapper<String>) ctx -> {
                    DirContextAdapter searchResultContext = (DirContextAdapter) ctx;
                    return searchResultContext.getNameInNamespace();
                });
        List<String> groups = new ArrayList<>();
        if(!dns.isEmpty()) {
            String userDn = dns.get(0);
            String formattedGroupSearchFilter = MessageFormat.format(ldapProperties.getGroupSearchFilter(), userDn, username);
            LdapQuery groupSearchQuery = LdapQueryBuilder.query().attributes("cn").base(ldapProperties.getGroupSearchBase()).filter(formattedGroupSearchFilter);
            groups = ldapTemplate.search(groupSearchQuery, (ContextMapper<String>) ctx -> {
                        DirContextAdapter searchResultContext = (DirContextAdapter) ctx;
                        return searchResultContext.getStringAttribute("cn");
                    });
        }
        for(String ldapFilter: ldapFiltersGroups.keySet()) {
            String hardcodedFilter = MessageFormat.format(ldapProperties.getMemberSearchFilter(), username, ldapFilter);
            List<String> filterDns = ldapTemplate.search(LdapQueryBuilder.query().attributes("dn").filter(hardcodedFilter),
                    (ContextMapper<String>) ctx -> {
                        DirContextAdapter searchResultContext = (DirContextAdapter) ctx;
                        return searchResultContext.getNameInNamespace();
                    });

            if(!filterDns.isEmpty()) {
                groups.add(ldapFiltersGroups.get(ldapFilter));
            }
        }
        return groups;
    }

    public void addLdapRoles(Set<GrantedAuthority> grantedAuthorities, List<String> ldapGroups, String groupPrefixRoleName, Map<String, String> mappingGroupesRoles) {
        for(String groupName : ldapGroups) {
            if(groupName != null) {
                Matcher m = Pattern.compile(groupPrefixRoleName).matcher(groupName);
                if (mappingGroupesRoles != null && mappingGroupesRoles.containsKey(groupName)) {
                    grantedAuthorities.add(new SimpleGrantedAuthority(mappingGroupesRoles.get(groupName)));
                } else if (m.matches()) {
                    grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + m.group(1).toUpperCase()));
                }
            }
        }
    }

    @Override
    public List<String> getMembers(String groupName) throws EsupSignatureRuntimeException {
        List<String> eppns = new ArrayList<>();
        String groupCn;
        List<Map.Entry<String, String>> group = getAllGroupsStartWith(groupName);
        //TODO : faire d'abord une requete start with * puis une requete stricte avec le cn
        if(group.size() == 1 ) {
            groupCn = group.stream().map(Map.Entry::getKey).toList().get(0);
        } else if (group.size() > 0) {
            groupCn = groupName;
        } else {
            return eppns;
        }
        logger.debug("getMembers of : " + groupCn);
        if (ldapProperties.getMembersOfGroupSearchFilter() != null) {
            String formattedFilter = MessageFormat.format(ldapProperties.getMembersOfGroupSearchFilter(), groupCn);
            eppns = ldapTemplate.search(ldapProperties.getSearchBase(), formattedFilter, (ContextMapper<String>) ctx -> {
                DirContextAdapter searchResultContext = (DirContextAdapter) ctx;
                return searchResultContext.getStringAttribute("mail");
            });
        }
        if(group.size() > 0 && eppns.size() == 0) {
            throw new EsupSignatureRuntimeException("empty group " + groupCn);
        }
        return eppns;
    }

    public String getDomain() {
        return globalProperties.getDomain();
    }
}
