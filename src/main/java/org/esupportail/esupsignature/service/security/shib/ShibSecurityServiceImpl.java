package org.esupportail.esupsignature.service.security.shib;

import org.esupportail.esupsignature.config.security.shib.ShibProperties;
import org.esupportail.esupsignature.service.security.Group2UserRoleService;
import org.esupportail.esupsignature.service.security.SecurityService;
import org.esupportail.esupsignature.service.security.SpelGroupService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShibSecurityServiceImpl implements SecurityService {

	@Resource
	private ShibProperties shibProperties;

	@Resource
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
		authenticationFilter.setPrincipalRequestHeader(shibProperties.getPrincipalRequestHeader());
		authenticationFilter.setCredentialsRequestHeader(shibProperties.getCredentialsRequestHeader());
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
		Map<String, String> mappingGroupesRoles = new HashMap<>();
		mappingGroupesRoles.put(shibProperties.getGroupMappingRoleAdmin(), "ROLE_ADMIN");
		mappingGroupesRoles.put(shibProperties.getGroupMappingRoleManager(), "ROLE_MANAGER");

		SpelGroupService groupService = new SpelGroupService();
		Map<String, String> groups4eppnSpel = new HashMap<>();
		groups4eppnSpel.put(shibProperties.getGroupMappingRoleAdmin(), "true");
		groups4eppnSpel.put(shibProperties.getGroupMappingRoleManager(), "true");
		groupService.setGroups4eppnSpel(groups4eppnSpel);
		
		Group2UserRoleService group2UserRoleService = new Group2UserRoleService();
		group2UserRoleService.setMappingGroupesRoles(mappingGroupesRoles);
		
		group2UserRoleService.setGroupService(groupService);
		shibAuthenticatedUserDetailsService.setGroup2UserRoleService(group2UserRoleService);
		shibAuthenticatedUserDetailsService.setMappingGroupesRoles(mappingGroupesRoles);
		return shibAuthenticatedUserDetailsService;
	}
	
}
