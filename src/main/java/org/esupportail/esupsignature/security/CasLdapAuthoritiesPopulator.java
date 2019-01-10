package org.esupportail.esupsignature.security;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;

public class CasLdapAuthoritiesPopulator extends DefaultLdapAuthoritiesPopulator {
	
	protected Map<String, String> mappingGroupesRoles;
	
	public void setMappingGroupesRoles(Map<String, String> mappingGroupesRoles) {
		this.mappingGroupesRoles = mappingGroupesRoles;
	}
	
	public CasLdapAuthoritiesPopulator(ContextSource contextSource,
			String groupSearchBase) {
		super(contextSource, groupSearchBase);
	}

	@Override
	 protected Set<GrantedAuthority> getAdditionalRoles(DirContextOperations user, String username) {

		String userDn = user.getNameInNamespace();

		Set<GrantedAuthority> roles = getGroupMembershipRoles(userDn, username.toLowerCase());

		Set<GrantedAuthority> extraRoles = new HashSet<GrantedAuthority>();

		for(GrantedAuthority role: roles) {
			if(mappingGroupesRoles != null && mappingGroupesRoles.containsKey(role.getAuthority())) 
				extraRoles.add(new SimpleGrantedAuthority(mappingGroupesRoles.get(role.getAuthority())));
		}

		return extraRoles;
	}

}
