package org.esupportail.esupsignature.service.security.shib;

import org.esupportail.esupsignature.config.security.shib.ShibProperties;
import org.esupportail.esupsignature.service.file.FileService;
import org.esupportail.esupsignature.service.security.Group2UserRoleService;
import org.esupportail.esupsignature.service.security.SecurityService;
import org.esupportail.esupsignature.service.security.SpelGroupService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShibSecurityServiceImpl implements SecurityService {

	@Resource
	private ShibProperties shibProperties;

	@Resource
	private FileService fileService;

	@Resource
	private ShibAuthenticationSuccessHandler shibAuthenticationSuccessHandler;

	@Override
	public String getTitle() {
		return shibProperties.getTitle();
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
	public String getDomain() {
		return "";
	}

	@Override
	public LoginUrlAuthenticationEntryPoint getAuthenticationEntryPoint() {
		return new LoginUrlAuthenticationEntryPoint("/");
	}

	@Override
	public ShibRequestHeaderAuthenticationFilter getAuthenticationProcessingFilter() {
		ShibRequestHeaderAuthenticationFilter authenticationFilter = new ShibRequestHeaderAuthenticationFilter();
		authenticationFilter.setPrincipalRequestHeader(shibProperties.getPrincipalRequestHeader());
		authenticationFilter.setCredentialsRequestHeader(shibProperties.getCredentialsRequestHeader());
		authenticationFilter.setAuthenticationManager(shibAuthenticationManager());
		authenticationFilter.setExceptionIfHeaderMissing(true);
		authenticationFilter.setAuthenticationSuccessHandler(shibAuthenticationSuccessHandler);
		return authenticationFilter;
	}

	@Override
	public UserDetailsService getUserDetailsService() {
		return (UserDetailsService) this.shibAuthenticatedUserDetailsService();
	}

	public AuthenticationManager shibAuthenticationManager() {
		List<AuthenticationProvider> authenticatedAuthenticationProviders = new ArrayList<>();
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

		SpelGroupService groupService = new SpelGroupService();
		Map<String, String> groups4eppnSpel = new HashMap<>();
		groups4eppnSpel.put(shibProperties.getGroupMappingRoleAdmin(), "true");
		groupService.setGroups4eppnSpel(groups4eppnSpel);
		
		Group2UserRoleService group2UserRoleService = new Group2UserRoleService();
		group2UserRoleService.setMappingGroupesRoles(mappingGroupesRoles);
		
		group2UserRoleService.setGroupService(groupService);
		shibAuthenticatedUserDetailsService.setGroup2UserRoleService(group2UserRoleService);
		shibAuthenticatedUserDetailsService.setMappingGroupesRoles(mappingGroupesRoles);
		return shibAuthenticatedUserDetailsService;
	}

	public File getDomainsWhiteList() {
		try {
			return fileService.getFileFromUrl(shibProperties.getDomainsWhiteListUrl());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
