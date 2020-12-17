package org.esupportail.esupsignature.service.security.cas;

import org.esupportail.esupsignature.service.ldap.LdapGroupService;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
		Set<GrantedAuthority> grantedAuthorities = new HashSet<>();
		List<String> ldapGroups = ldapGroupService.getGroups(username.toLowerCase());
		ldapGroupService.addLdapRoles(grantedAuthorities, ldapGroups, groupPrefixRoleName, mappingGroupesRoles);
		return grantedAuthorities;
	}

}
