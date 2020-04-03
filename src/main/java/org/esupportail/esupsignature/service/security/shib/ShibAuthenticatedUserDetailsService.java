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
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ShibAuthenticatedUserDetailsService implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {

	private static final Logger logger = LoggerFactory.getLogger(ShibAuthenticatedUserDetailsService.class);

	protected Map<String, String> mappingGroupesRoles;
	
	protected Group2UserRoleService group2UserRoleService;

	
	public void setMappingGroupesRoles(Map<String, String> mappingGroupesRoles) {
		this.mappingGroupesRoles = mappingGroupesRoles;
	}
	
	public void setGroup2UserRoleService(Group2UserRoleService group2UserRoleService) {
		this.group2UserRoleService = group2UserRoleService;
	}
	
	public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token) throws AuthenticationException {
		List<SimpleGrantedAuthority> authorities = new ArrayList<>();
		logger.info("load user details from : " + token.getName());
		String credentials = (String) token.getCredentials();
		logger.info("credentials : " + credentials);
		try {
			for (String credential : StringUtils.split(credentials, ";")) {
				if (mappingGroupesRoles != null && mappingGroupesRoles.containsKey(credential)) {
					authorities.add(new SimpleGrantedAuthority(mappingGroupesRoles.get(credential)));
				}
			}
		} catch (Exception e) {
			logger.warn("unable to find credentials", e);
		}
		try {
			for (String roleFromLdap : group2UserRoleService.getRoles(token.getName())) {
				authorities.add(new SimpleGrantedAuthority(roleFromLdap));
				logger.info("loading authorities : " + authorities.get(0).getAuthority());
			}
		} catch (Exception e) {
			logger.warn("unable to find authorities", e);
		}
		return createUserDetails(token, authorities);
	}

	protected UserDetails createUserDetails(Authentication token, Collection<? extends GrantedAuthority> authorities) {
		return new User(token.getName(), "N/A", true, true, true, true, authorities);
	}
}
