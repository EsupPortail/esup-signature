package org.esupportail.esupsignature.service.security.cas;

import org.esupportail.esupsignature.service.ldap.LdapGroupService;
import org.esupportail.esupsignature.service.security.Group2UserRoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CasLdapAuthoritiesPopulator extends DefaultLdapAuthoritiesPopulator {

	private static final Logger logger = LoggerFactory.getLogger(CasLdapAuthoritiesPopulator.class);

	private LdapGroupService ldapGroupService;

	private String groupPrefixRoleName;

	protected Map<String, String> mappingGroupesRoles;

	private Group2UserRoleService group2UserRoleService;

	public void setLdapGroupService(LdapGroupService ldapGroupService) {
		this.ldapGroupService = ldapGroupService;
	}

	public void setMappingGroupesRoles(Map<String, String> mappingGroupesRoles) {
		this.mappingGroupesRoles = mappingGroupesRoles;
	}

	public void setGroupPrefixRoleName(String groupPrefixRoleName) {
		this.groupPrefixRoleName = groupPrefixRoleName;
	}

	public void setGroup2UserRoleService(Group2UserRoleService group2UserRoleService) {
		this.group2UserRoleService = group2UserRoleService;
	}

	public CasLdapAuthoritiesPopulator(ContextSource contextSource, String groupSearchBase) {
		super(contextSource, groupSearchBase);
	}

	@Override
	protected Set<GrantedAuthority> getAdditionalRoles(DirContextOperations user, String username) {
		Set<GrantedAuthority> grantedAuthorities = new HashSet<>();
		List<String> ldapGroups = ldapGroupService.getGroups(username.toLowerCase());
		for (String roleFromLdap : group2UserRoleService.getRoles(username.toLowerCase())) {
			SimpleGrantedAuthority simpleGrantedAuthority = new SimpleGrantedAuthority(roleFromLdap);
			grantedAuthorities.add(simpleGrantedAuthority);
			logger.debug("loading authorities : " + simpleGrantedAuthority.getAuthority());
		}
		ldapGroupService.addLdapRoles(grantedAuthorities, ldapGroups, groupPrefixRoleName, mappingGroupesRoles);
		return grantedAuthorities;
	}

}
