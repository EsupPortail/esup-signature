package org.esupportail.esupsignature.service.security.cas;

import org.esupportail.esupsignature.service.ldap.LdapGroupService;
import org.esupportail.esupsignature.service.security.GroupService;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CasLdapAuthoritiesPopulator extends DefaultLdapAuthoritiesPopulator {

	protected LdapGroupService ldapGroupService;

	protected Map<String, String> mappingGroupesRoles;
	
	public void setMappingGroupesRoles(Map<String, String> mappingGroupesRoles) {
		this.mappingGroupesRoles = mappingGroupesRoles;
	}

	public void setLdapGroupService(LdapGroupService ldapGroupService) {
		this.ldapGroupService = ldapGroupService;
	}

	public CasLdapAuthoritiesPopulator(ContextSource contextSource, String groupSearchBase) {
		super(contextSource, groupSearchBase);
	}

	@Override
	protected Set<GrantedAuthority> getAdditionalRoles(DirContextOperations user, String username) {

		Set<GrantedAuthority> additionalRoles = new HashSet<>();

		for(String groupName : ldapGroupService.getGroups(username.toLowerCase())) {
			if(groupName != null) {
				if (mappingGroupesRoles != null && mappingGroupesRoles.containsKey(groupName)) {
					additionalRoles.add(new SimpleGrantedAuthority(mappingGroupesRoles.get(groupName)));
				} else {
					additionalRoles.add(new SimpleGrantedAuthority(groupName));
				}
			}
		}

		return additionalRoles;
	}

}
