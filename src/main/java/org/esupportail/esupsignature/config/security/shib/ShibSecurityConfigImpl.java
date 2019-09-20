package org.esupportail.esupsignature.config.security.shib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.esupportail.esupsignature.service.security.Group2UserRoleService;
import org.esupportail.esupsignature.service.security.SecurityService;
import org.esupportail.esupsignature.service.security.SpelGroupService;
import org.esupportail.esupsignature.service.security.shib.ShibAuthenticatedUserDetailsService;
import org.esupportail.esupsignature.service.security.shib.ShibAuthenticationSuccessHandler;
import org.esupportail.esupsignature.service.security.shib.ShibRequestHeaderAuthenticationFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;

import javax.annotation.Resource;

@Configuration
@EnableConfigurationProperties(ShibProperties.class)
public class ShibSecurityConfigImpl implements SecurityService {

	private ShibProperties shibProperties;

	public ShibSecurityConfigImpl(ShibProperties shibProperties) {
		this.shibProperties = shibProperties;
	}

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
