package org.esupportail.esupsignature.service.security.shib;

import org.esupportail.esupsignature.config.security.WebSecurityProperties;
import org.esupportail.esupsignature.config.security.shib.ShibProperties;
import org.esupportail.esupsignature.repository.MappingFiltersGroupsRepository;
import org.esupportail.esupsignature.repository.MappingGroupsRolesRepository;
import org.esupportail.esupsignature.service.ldap.LdapGroupService;
import org.esupportail.esupsignature.service.security.Group2UserRoleService;
import org.esupportail.esupsignature.service.security.SecurityService;
import org.esupportail.esupsignature.service.security.SpelGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;

import jakarta.annotation.Resource;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ShibSecurityServiceImpl implements SecurityService {

	private LdapGroupService ldapGroupService;

	private WebSecurityProperties webSecurityProperties;

	private SpelGroupService spelGroupService;

	@Autowired(required = false)
	public void setLdapGroupService(LdapGroupService ldapGroupService) {
		this.ldapGroupService = ldapGroupService;
	}

	@Autowired(required = false)
	public void setWebSecurityProperties(WebSecurityProperties webSecurityProperties) {
		this.webSecurityProperties = webSecurityProperties;
	}

	@Autowired(required = false)
	public void setSpelGroupService(SpelGroupService spelGroupService) {
		this.spelGroupService = spelGroupService;
	}

	@Resource
	private MappingFiltersGroupsRepository mappingFiltersGroupsRepository;

	@Resource
	private MappingGroupsRolesRepository mappingGroupsRolesRepository;

	@Resource
	private ShibProperties shibProperties;

	@Resource
	private ShibAuthenticationSuccessHandler shibAuthenticationSuccessHandler;

	@Override
	public String getTitle() {
		return shibProperties.getTitle();
	}

	@Override
	public String getDescription() {
		return "Pour accéder au service Université de Rouen Normandie - Système de Gestion de Cartes - Leocarte, sélectionnez ou cherchez l'établissement auquel vous appartenez.";
	}

	@Override
	public String getLoginUrl() {
		return "/login/shibentry";
	}

	@Override
	public String getLogoutUrl() {
		return shibProperties.getIdpUrl() + "/idp/profile/Logout";
	}

	@Override
	public LoginUrlAuthenticationEntryPoint getAuthenticationEntryPoint() {
		return new LoginUrlAuthenticationEntryPoint("/");
	}

	@Resource
	private DatabaseUserDetailsService databaseUserDetailsService;

	@Override
	public ShibRequestHeaderAuthenticationFilter getAuthenticationProcessingFilter() {
		ShibRequestHeaderAuthenticationFilter authenticationFilter = new ShibRequestHeaderAuthenticationFilter();
		authenticationFilter.setPrincipalRequestHeader(shibProperties.getPrincipalRequestHeader());
		if(StringUtils.hasText(shibProperties.getCredentialsRequestHeader())) {
			authenticationFilter.setCredentialsRequestHeader(shibProperties.getCredentialsRequestHeader());
		}
		authenticationFilter.setAuthenticationManager(shibAuthenticationManager());
		authenticationFilter.setExceptionIfHeaderMissing(false);
		authenticationFilter.setAuthenticationSuccessHandler(shibAuthenticationSuccessHandler);
		return authenticationFilter;
	}

	@Override
	public UserDetailsService getUserDetailsService() {
		return databaseUserDetailsService;
	}

	public AuthenticationManager shibAuthenticationManager() {
		List<AuthenticationProvider> authenticatedAuthenticationProviders = new ArrayList<>();
		authenticatedAuthenticationProviders.add(shibPreauthAuthProvider());
		return new ProviderManager(authenticatedAuthenticationProviders);
		
	}
 
	public PreAuthenticatedAuthenticationProvider shibPreauthAuthProvider() {
		PreAuthenticatedAuthenticationProvider authenticationProvider = new PreAuthenticatedAuthenticationProvider();
		authenticationProvider.setPreAuthenticatedUserDetailsService(shibAuthenticatedUserDetailsService());
		return authenticationProvider;
	}

	public ShibAuthenticatedUserDetailsService shibAuthenticatedUserDetailsService() {
		ShibAuthenticatedUserDetailsService shibAuthenticatedUserDetailsService = new ShibAuthenticatedUserDetailsService();
		Group2UserRoleService group2UserRoleService = new Group2UserRoleService();
		group2UserRoleService.setGroupPrefixRoleName(webSecurityProperties.getGroupToRoleFilterPattern());
		group2UserRoleService.setMappingGroupesRoles(webSecurityProperties.getMappingGroupsRoles());
		group2UserRoleService.setGroupService(spelGroupService);
		shibAuthenticatedUserDetailsService.setGroupPrefixRoleName(webSecurityProperties.getGroupToRoleFilterPattern());
		shibAuthenticatedUserDetailsService.setGroup2UserRoleService(group2UserRoleService);
		shibAuthenticatedUserDetailsService.setMappingGroupesRoles(webSecurityProperties.getMappingGroupsRoles());
		shibAuthenticatedUserDetailsService.setLdapGroupService(ldapGroupService);
		shibAuthenticatedUserDetailsService.setMappingFiltersGroupsRepository(mappingFiltersGroupsRepository);
		shibAuthenticatedUserDetailsService.setMappingGroupsRolesRepository(mappingGroupsRolesRepository);
		return shibAuthenticatedUserDetailsService;
	}

}
