/**
 * Licensed to ESUP-Portail under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * ESUP-Portail licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.esupportail.esupsignature.service.security.shib;

import org.esupportail.esupsignature.service.ldap.LdapGroupService;
import org.esupportail.esupsignature.service.security.Group2UserRoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShibAuthenticatedUserDetailsService implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {

	private static final Logger logger = LoggerFactory.getLogger(ShibAuthenticatedUserDetailsService.class);

	private LdapGroupService ldapGroupService;

	private String groupPrefixRoleName;

	private Map<String, String> mappingGroupesRoles;

	private Group2UserRoleService group2UserRoleService;

	public void setLdapGroupService(LdapGroupService ldapGroupService) {
		this.ldapGroupService = ldapGroupService;
	}

	public void setGroupPrefixRoleName(String groupPrefixRoleName) {
		this.groupPrefixRoleName = groupPrefixRoleName;
	}

	public void setMappingGroupesRoles(Map<String, String> mappingGroupesRoles) {
		this.mappingGroupesRoles = mappingGroupesRoles;
	}
	
	public void setGroup2UserRoleService(Group2UserRoleService group2UserRoleService) {
		this.group2UserRoleService = group2UserRoleService;
	}

	public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token) throws AuthenticationException {
		Set<GrantedAuthority> grantedAuthorities = new HashSet<>();
		Set<String> ldapGroups = new HashSet<>();
		if(!token.getCredentials().equals("")) {
			logger.debug("load user details from : " + token.getName());
			String credentials = token.getCredentials().toString();
			logger.debug("credentials : " + credentials);
			String[] splitCredentials = credentials.split(";");
			for (String credential : splitCredentials) {
				try{
					LdapName ln = new LdapName(credential);
					for(Rdn rdn : ln.getRdns()) {
						if(rdn.getType().equalsIgnoreCase("CN")) {
							ldapGroups.add(rdn.getValue().toString());
							break;
						}
					}
				} catch (Exception e) {
					logger.debug("unable to find credentials", e);
				}
				for(String mappingGroupesRole : mappingGroupesRoles.keySet()) {
					if (credential.contains(mappingGroupesRole)) {
						grantedAuthorities.add(new SimpleGrantedAuthority(mappingGroupesRoles.get(mappingGroupesRole)));
					}
				}
				if(groupPrefixRoleName != null) {
					Matcher m = Pattern.compile(groupPrefixRoleName).matcher(credential);
					if (m.matches()) {
						grantedAuthorities.add(new SimpleGrantedAuthority(credential));
					}
				}
			}

			try {
				for (String roleFromSpel : group2UserRoleService.getRoles(token.getName())) {
					SimpleGrantedAuthority simpleGrantedAuthority = new SimpleGrantedAuthority(roleFromSpel);
					grantedAuthorities.add(simpleGrantedAuthority);
					logger.debug("loading authorities : " + simpleGrantedAuthority.getAuthority());
				}
				if (ldapGroupService != null && ldapGroupService.getDomain().equals(token.getName().split("@")[1])) {
					ldapGroups.addAll(ldapGroupService.getGroups(token.getName().replaceAll("@.*", "")));
					ldapGroupService.addLdapRoles(grantedAuthorities, new ArrayList<>(ldapGroups), groupPrefixRoleName, mappingGroupesRoles);
				}
			} catch (Exception e) {
				logger.warn("unable to find authorities", e);
			}
			return createUserDetails(token, grantedAuthorities);
		}
		return new User("anonymousUser", "N/A", false, false, false, false, grantedAuthorities);
	}

	protected UserDetails createUserDetails(Authentication token, Collection<? extends GrantedAuthority> grantedAuthorities) {
		return new User(token.getName(), "N/A", true, true, true, true, grantedAuthorities);
	}
}
