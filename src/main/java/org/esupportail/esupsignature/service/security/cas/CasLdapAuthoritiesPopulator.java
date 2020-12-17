package org.esupportail.esupsignature.service.security.cas;

import org.esupportail.esupsignature.service.ldap.LdapGroupService;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CasLdapAuthoritiesPopulator extends DefaultLdapAuthoritiesPopulator {

	private LdapGroupService ldapGroupService;

	private String groupPrefixRoleName;

	protected Map<String, String> mappingGroupesRoles;

	public void setLdapGroupService(LdapGroupService ldapGroupService) {
		this.ldapGroupService = ldapGroupService;
	}

	public void setMappingGroupesRoles(Map<String, String> mappingGroupesRoles) {
		this.mappingGroupesRoles = mappingGroupesRoles;
	}

	public void setGroupPrefixRoleName(String groupPrefixRoleName) {
		this.groupPrefixRoleName = groupPrefixRoleName;
	}

	public CasLdapAuthoritiesPopulator(ContextSource contextSource, String groupSearchBase) {
		super(contextSource, groupSearchBase);
	}

	@Override
	protected Set<GrantedAuthority> getAdditionalRoles(DirContextOperations user, String username) {

		Set<GrantedAuthority> additionalRoles = new HashSet<>();
		List<String> ldapGroups = ldapGroupService.getGroups(username.toLowerCase());
		for(String groupName : ldapGroups) {
			if(groupName != null) {
				Matcher m = Pattern.compile(groupPrefixRoleName).matcher(groupName);
				if (mappingGroupesRoles != null && mappingGroupesRoles.containsKey(groupName)) {
					additionalRoles.add(new SimpleGrantedAuthority(mappingGroupesRoles.get(groupName)));
				} else if (m.matches()) {
					additionalRoles.add(new SimpleGrantedAuthority("ROLE_" + m.group(1).toUpperCase()));
				}
			}
		}
		return additionalRoles;
	}

}
