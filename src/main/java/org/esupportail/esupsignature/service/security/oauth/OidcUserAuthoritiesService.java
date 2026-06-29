package org.esupportail.esupsignature.service.security.oauth;

import org.esupportail.esupsignature.config.security.WebSecurityProperties;
import org.esupportail.esupsignature.service.security.OidcUserSecurityService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OidcUserAuthoritiesService {

    private final WebSecurityProperties webSecurityProperties;

    public OidcUserAuthoritiesService(WebSecurityProperties webSecurityProperties) {
        this.webSecurityProperties = webSecurityProperties;
    }

    public Set<GrantedAuthority> buildAuthorities(Map<String, Object> claims, OidcUserSecurityService oidcUserSecurityService) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        Set<String> groups = extractGroups(claims, oidcUserSecurityService);
        for (String group : groups) {
            addMappedAuthority(authorities, group);
            String cn = extractCn(group);
            if (StringUtils.hasText(cn)) {
                addMappedAuthority(authorities, cn);
            }
        }
        return authorities;
    }

    private Set<String> extractGroups(Map<String, Object> claims, OidcUserSecurityService oidcUserSecurityService) {
        Set<String> groups = new LinkedHashSet<>();
        for (String claimName : oidcUserSecurityService.getGroupsClaims()) {
            Object claimValue = claims.get(claimName);
            addClaimValue(groups, claimValue);
        }
        return groups;
    }

    private void addClaimValue(Set<String> groups, Object claimValue) {
        if (claimValue == null) {
            return;
        }
        if (claimValue instanceof Collection<?> collection) {
            collection.forEach(value -> addClaimValue(groups, value));
            return;
        }
        if (claimValue.getClass().isArray()) {
            int length = Array.getLength(claimValue);
            for (int index = 0; index < length; index++) {
                addClaimValue(groups, Array.get(claimValue, index));
            }
            return;
        }
        String value = claimValue.toString().trim();
        if (!StringUtils.hasText(value)) {
            return;
        }
        if (value.contains(";")) {
            for (String group : value.split(";")) {
                addGroup(groups, group);
            }
        } else if (value.contains("\n")) {
            for (String group : value.split("\\R")) {
                addGroup(groups, group);
            }
        } else if (value.contains(",") && !value.contains("=")) {
            for (String group : value.split(",")) {
                addGroup(groups, group);
            }
        } else {
            addGroup(groups, value);
        }
    }

    private void addGroup(Set<String> groups, String group) {
        if (StringUtils.hasText(group)) {
            groups.add(group.trim());
        }
    }

    private void addMappedAuthority(Set<GrantedAuthority> authorities, String group) {
        Map<String, String> mappingGroupsRoles = webSecurityProperties.getMappingGroupsRoles();
        if (mappingGroupsRoles != null && mappingGroupsRoles.containsKey(group)) {
            authorities.add(new SimpleGrantedAuthority(mappingGroupsRoles.get(group)));
            return;
        }
        String groupToRoleFilterPattern = webSecurityProperties.getGroupToRoleFilterPattern();
        if (StringUtils.hasText(groupToRoleFilterPattern)) {
            Matcher matcher = Pattern.compile(groupToRoleFilterPattern).matcher(group);
            if (matcher.matches()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + matcher.group(1).toUpperCase()));
            }
        }
    }

    private String extractCn(String group) {
        try {
            LdapName ldapName = new LdapName(group);
            for (Rdn rdn : ldapName.getRdns()) {
                if ("cn".equalsIgnoreCase(rdn.getType())) {
                    return rdn.getValue().toString();
                }
            }
        } catch (Exception ignored) {
            return "";
        }
        return "";
    }
}
