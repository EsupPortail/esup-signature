package org.esupportail.esupsignature.security.shib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.esupportail.esupsignature.security.Group2UserRoleService;
import org.esupportail.esupsignature.security.SecurityConfig;
import org.esupportail.esupsignature.security.SpelGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;

@ConfigurationProperties(prefix="security.shib")
public class ShibSecurityConfigImpl implements SecurityConfig {


	private String principalRequestHeader;
	private String credentialsRequestHeader;

	public String getPrincipalRequestHeader() {
		return principalRequestHeader;
	}

	public void setPrincipalRequestHeader(String principalRequestHeader) {
		this.principalRequestHeader = principalRequestHeader;
	}

	public String getCredentialsRequestHeader() {
		return credentialsRequestHeader;
	}

	public void setCredentialsRequestHeader(String credentialsRequestHeader) {
		this.credentialsRequestHeader = credentialsRequestHeader;
	}

	@Autowired
	private ShibAuthenticationSuccessHandler shibAuthenticationSuccessHandler;

	public String getName() {
		return "Compte d'un autre établissement (Shibboleth)";
	}
	
	public String getLoginUrl() {
		return "/login/shibentry";
	}
	
	public LoginUrlAuthenticationEntryPoint getAuthenticationEntryPoint() {
		return new LoginUrlAuthenticationEntryPoint("/");
	}

	public ShibRequestHeaderAuthenticationFilter getAuthenticationProcessingFilter() {
		ShibRequestHeaderAuthenticationFilter authenticationFilter = new ShibRequestHeaderAuthenticationFilter();
		authenticationFilter.setPrincipalRequestHeader(principalRequestHeader);
		authenticationFilter.setCredentialsRequestHeader(credentialsRequestHeader);
		authenticationFilter.setAuthenticationManager(shibAuthenticationManager());
		authenticationFilter.setExceptionIfHeaderMissing(false);
		authenticationFilter.setAuthenticationSuccessHandler(shibAuthenticationSuccessHandler);
		return authenticationFilter;
	}
	
	public AuthenticationManager shibAuthenticationManager() {
		List<AuthenticationProvider> authenticatedAuthenticationProviders = new ArrayList<AuthenticationProvider>();
		authenticatedAuthenticationProviders.add(shibPreauthAuthProvider());
		AuthenticationManager authenticationManager = new ProviderManager(authenticatedAuthenticationProviders);
		return authenticationManager;
		
	}
 
	public PreAuthenticatedAuthenticationProvider shibPreauthAuthProvider() {
		PreAuthenticatedAuthenticationProvider authenticationProvider = new PreAuthenticatedAuthenticationProvider();
		authenticationProvider.setPreAuthenticatedUserDetailsService(shibAuthenticatedUserDetailsService());
		return authenticationProvider;
	}

	public ShibAuthenticatedUserDetailsService shibAuthenticatedUserDetailsService() {
		ShibAuthenticatedUserDetailsService shibAuthenticatedUserDetailsService = new ShibAuthenticatedUserDetailsService();
		Map<String, String> mappingGroupesRoles = new HashMap<String, String>();
		mappingGroupesRoles.put("cn=for.esup-signature.admin,ou=groups,dc=univ-rouen", "ROLE_ADMIN");
		mappingGroupesRoles.put("cn=for.esup-signature.manager,ou=groups,dc=univ-rouen", "ROLE_MANAGER");
		
		SpelGroupService groupService = new SpelGroupService();
		Map<String, String> groups4eppnSpel = new HashMap<String, String>();
		groups4eppnSpel.put("cn=for.esup-signature.admin,ou=groups,dc=univ-rouen", "true");
		groups4eppnSpel.put("cn=for.esup-signature.manager,ou=groups,dc=univ-rouen", "true");
		groupService.setGroups4eppnSpel(groups4eppnSpel);
		
		Group2UserRoleService group2UserRoleService = new Group2UserRoleService();
		group2UserRoleService.setMappingGroupesRoles(mappingGroupesRoles);
		
		group2UserRoleService.setGroupService(groupService);
		shibAuthenticatedUserDetailsService.setGroup2UserRoleService(group2UserRoleService);
		shibAuthenticatedUserDetailsService.setMappingGroupesRoles(mappingGroupesRoles);
		return shibAuthenticatedUserDetailsService;
	}
	
}
