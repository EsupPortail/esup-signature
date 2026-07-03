package org.esupportail.esupsignature.service.security.oauth;

import org.esupportail.esupsignature.config.security.WebSecurityProperties;
import org.esupportail.esupsignature.service.security.Group2UserRoleService;
import org.esupportail.esupsignature.service.security.OidcUserSecurityService;
import org.esupportail.esupsignature.service.security.SpelGroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.lang.reflect.Array;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OidcUserAuthoritiesService {

    private static final Logger logger = LoggerFactory.getLogger(OidcUserAuthoritiesService.class);

    private final Group2UserRoleService group2UserRoleService;

    private final WebSecurityProperties webSecurityProperties;

    public OidcUserAuthoritiesService(SpelGroupService spelGroupService, WebSecurityProperties webSecurityProperties) {
        this.webSecurityProperties = webSecurityProperties;
        group2UserRoleService = new Group2UserRoleService();
        group2UserRoleService.setGroupPrefixRoleName(webSecurityProperties.getGroupToRoleFilterPattern());
        group2UserRoleService.setMappingGroupesRoles(webSecurityProperties.getMappingGroupsRoles());
        spelGroupService.initGroupMappingSpel();
        group2UserRoleService.setGroupService(spelGroupService);
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
        try {
            String principalName = Optional.ofNullable(claims.get(oidcUserSecurityService.getPrincipalClaim())).map(Object::toString).orElse("");
            for (String roleFromSpel : group2UserRoleService.getRoles(principalName)) {
                SimpleGrantedAuthority simpleGrantedAuthority = new SimpleGrantedAuthority(roleFromSpel);
                authorities.add(simpleGrantedAuthority);
                logger.debug("loading authorities : " + simpleGrantedAuthority.getAuthority());
            }
        } catch (Exception e) {
            logger.warn("unable to find authorities", e);
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
