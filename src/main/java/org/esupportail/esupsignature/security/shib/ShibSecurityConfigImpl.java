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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix="security")
public class ShibSecurityConfigImpl implements SecurityConfig {


	private String shibPrincipalRequestHeader;	
	private String shibCredentialsRequestHeader;	

	public String getShibPrincipalRequestHeader() {
		return shibPrincipalRequestHeader;
	}

	public void setShibPrincipalRequestHeader(String shibPrincipalRequestHeader) {
		this.shibPrincipalRequestHeader = shibPrincipalRequestHeader;
	}

	public String getShibCredentialsRequestHeader() {
		return shibCredentialsRequestHeader;
	}

	public void setShibCredentialsRequestHeader(String shibCredentialsRequestHeader) {
		this.shibCredentialsRequestHeader = shibCredentialsRequestHeader;
	}

	@Autowired
	private ShibAuthenticationSuccessHandler shibAuthenticationSuccessHandler;

	public String getName() {
		return "Shibboleth";
	}
	
	public String getLoginUrl() {
		return "/login/shibentry";
	}
	
	public LoginUrlAuthenticationEntryPoint getAuthenticationEntryPoint() {
		return new LoginUrlAuthenticationEntryPoint("/");
	}

	public ShibRequestHeaderAuthenticationFilter getAuthenticationProcessingFilter() {
		ShibRequestHeaderAuthenticationFilter authenticationFilter = new ShibRequestHeaderAuthenticationFilter();
		authenticationFilter.setPrincipalRequestHeader(shibPrincipalRequestHeader);
		authenticationFilter.setCredentialsRequestHeader(shibCredentialsRequestHeader);
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
