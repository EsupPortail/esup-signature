package org.esupportail.esupsignature.service.security.shib;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.security.shib.ShibProperties;
import org.esupportail.esupsignature.service.ldap.LdapGroupService;
import org.esupportail.esupsignature.service.security.Group2UserRoleService;
import org.esupportail.esupsignature.service.security.SecurityService;
import org.esupportail.esupsignature.service.security.SpelGroupService;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
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

	@Autowired
	private ObjectProvider<LdapGroupService> ldapGroupService;

	@Resource
	private GlobalProperties globalProperties;

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
	public LoginUrlAuthenticationEntryPoint getAuthenticationEntryPoint() {
		return new LoginUrlAuthenticationEntryPoint("/");
	}

	@Override
	public ShibRequestHeaderAuthenticationFilter getAuthenticationProcessingFilter() {
		ShibRequestHeaderAuthenticationFilter authenticationFilter = new ShibRequestHeaderAuthenticationFilter();
		authenticationFilter.setPrincipalRequestHeader(shibProperties.getPrincipalRequestHeader());
		if(shibProperties.getCredentialsRequestHeader() != null) {
			authenticationFilter.setCredentialsRequestHeader(shibProperties.getCredentialsRequestHeader());
		}
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
		SpelGroupService groupService = new SpelGroupService();
		Map<String, String> groups4eppnSpel = new HashMap<>();
		if(shibProperties.getGroupMappingSpel() != null) {
			for(String groupName: shibProperties.getGroupMappingSpel().keySet()) {
				String spelRule = shibProperties.getGroupMappingSpel().get(groupName);
				groups4eppnSpel.put(groupName, spelRule);
			}
		}
		groupService.setGroups4eppnSpel(groups4eppnSpel);

		Group2UserRoleService group2UserRoleService = new Group2UserRoleService();
		group2UserRoleService.setPrefix(globalProperties.getGroupPrefixRoleName());
		group2UserRoleService.setMappingGroupesRoles(globalProperties.getMappingGroupsRoles());
		group2UserRoleService.setGroupService(groupService);

		shibAuthenticatedUserDetailsService.setGroupPrefixRoleName(globalProperties.getGroupPrefixRoleName());
		shibAuthenticatedUserDetailsService.setGroup2UserRoleService(group2UserRoleService);
		shibAuthenticatedUserDetailsService.setMappingGroupesRoles(globalProperties.getMappingGroupsRoles());
		shibAuthenticatedUserDetailsService.setLdapGroupService(ldapGroupService.getIfAvailable());
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
