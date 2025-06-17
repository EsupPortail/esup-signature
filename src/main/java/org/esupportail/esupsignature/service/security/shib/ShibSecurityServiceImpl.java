package org.esupportail.esupsignature.service.security.shib;

import jakarta.annotation.Resource;
import org.esupportail.esupsignature.config.security.WebSecurityProperties;
import org.esupportail.esupsignature.config.security.shib.ShibProperties;
import org.esupportail.esupsignature.repository.MappingFiltersGroupsRepository;
import org.esupportail.esupsignature.repository.MappingGroupsRolesRepository;
import org.esupportail.esupsignature.service.ldap.LdapGroupService;
import org.esupportail.esupsignature.service.security.Group2UserRoleService;
import org.esupportail.esupsignature.service.security.SecurityService;
import org.esupportail.esupsignature.service.security.SpelGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@Order(2)
@ConditionalOnProperty(prefix = "security.shib", name = "principal-request-header")
public class ShibSecurityServiceImpl implements SecurityService {

	private final LdapGroupService ldapGroupService;
	private final WebSecurityProperties webSecurityProperties;
	private final SpelGroupService spelGroupService;
	private final MappingFiltersGroupsRepository mappingFiltersGroupsRepository;
	private final MappingGroupsRolesRepository mappingGroupsRolesRepository;
	private final ShibProperties shibProperties;
	private final ShibAuthenticationSuccessHandler shibAuthenticationSuccessHandler;

    public ShibSecurityServiceImpl(@Autowired(required = false) LdapGroupService ldapGroupService, @Autowired(required = false) WebSecurityProperties webSecurityProperties, SpelGroupService spelGroupService, MappingFiltersGroupsRepository mappingFiltersGroupsRepository, MappingGroupsRolesRepository mappingGroupsRolesRepository, ShibProperties shibProperties, ShibAuthenticationSuccessHandler shibAuthenticationSuccessHandler) {
        this.ldapGroupService = ldapGroupService;
        this.webSecurityProperties = webSecurityProperties;
        this.spelGroupService = spelGroupService;
        this.mappingFiltersGroupsRepository = mappingFiltersGroupsRepository;
        this.mappingGroupsRolesRepository = mappingGroupsRolesRepository;
        this.shibProperties = shibProperties;
        this.shibAuthenticationSuccessHandler = shibAuthenticationSuccessHandler;
    }

    @Override
	public String getTitle() {
		return shibProperties.getTitle();
	}

	@Override
	public String getCode() {
		return this.getClass().getSimpleName();
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
	public String getLoggedOutUrl() {
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
