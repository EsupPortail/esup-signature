package org.esupportail.esupsignature.service.security.cas;

import org.esupportail.esupsignature.config.ldap.LdapProperties;
import org.esupportail.esupsignature.config.security.cas.CasProperties;
import org.esupportail.esupsignature.repository.ldap.PersonLdapRepository;
import org.esupportail.esupsignature.service.ldap.LdapGroupService;
import org.esupportail.esupsignature.service.ldap.LdapPersonService;
import org.esupportail.esupsignature.service.security.SecurityService;
import org.jasig.cas.client.validation.Cas20ServiceTicketValidator;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.cas.authentication.CasAssertionAuthenticationToken;
import org.springframework.security.cas.authentication.CasAuthenticationProvider;
import org.springframework.security.cas.web.CasAuthenticationEntryPoint;
import org.springframework.security.cas.web.CasAuthenticationFilter;
import org.springframework.security.core.userdetails.UserDetailsByNameServiceWrapper;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;
import org.springframework.security.ldap.userdetails.LdapUserDetailsService;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CasSecurityServiceImpl implements SecurityService {

	@Resource
	private LdapTemplate ldapTemplate;

	@Resource
	private LdapPersonService ldapPersonService;

	@Resource
	private CasProperties casProperties;

	@Resource
	private LdapProperties ldapProperties;

	@Resource
	private CasAuthenticationSuccessHandler casAuthenticationSuccessHandler;
	
	@Resource
	private RegisterSessionAuthenticationStrategy sessionAuthenticationStrategy;

	@Resource
	private LdapContextSource ldapContextSource;
	
	@Override
	public String getTitle() {
		return casProperties.getTitle();
	}

	@Override
	public String getLoginUrl() {
		return "/login/casentry";
	}

	@Override
	public String getLogoutUrl() {
		return casProperties.getUrl() + "/logout";
	}

	@Override
	public String getDomain() {
		return casProperties.getDomain();
	}

	@Override
	public CasAuthenticationEntryPoint getAuthenticationEntryPoint() {
		CasAuthenticationEntryPoint authenticationEntryPoint = new CasAuthenticationEntryPoint();
		authenticationEntryPoint.setLoginUrl(casProperties.getUrl() + "/login");
		authenticationEntryPoint.setServiceProperties(serviceProperties());
		return authenticationEntryPoint;
	}

	@Override
	public CasAuthenticationFilter getAuthenticationProcessingFilter() {
		CasAuthenticationFilter authenticationFilter = new CasAuthenticationFilter();
		authenticationFilter.setAuthenticationManager(casAuthenticationManager());
		authenticationFilter.setSessionAuthenticationStrategy(sessionAuthenticationStrategy);
		authenticationFilter.setAuthenticationSuccessHandler(casAuthenticationSuccessHandler);
		return authenticationFilter;
	}

	@Override
	public UserDetailsService getUserDetailsService() {
		return this.ldapUserDetailsService();
	}

	public ServiceProperties serviceProperties() {
		ServiceProperties serviceProperties = new ServiceProperties();
		serviceProperties.setService(casProperties.getService());
		serviceProperties.setSendRenew(false);
		return serviceProperties;
	}

	public AuthenticationManager casAuthenticationManager() {
		List<AuthenticationProvider> authenticatedAuthenticationProviders = new ArrayList<AuthenticationProvider>();
		authenticatedAuthenticationProviders.add(casAuthenticationProvider());
		AuthenticationManager authenticationManager = new ProviderManager(authenticatedAuthenticationProviders);
		return authenticationManager;
	}

	
	public CasAuthenticationProvider casAuthenticationProvider() {
		CasAuthenticationProvider authenticationProvider = new CasAuthenticationProvider();
		authenticationProvider.setAuthenticationUserDetailsService(casAuthUserDetailsService());
		authenticationProvider.setServiceProperties(serviceProperties());
		authenticationProvider.setTicketValidator(cas20ServiceTicketValidator());
		authenticationProvider.setKey("EsupSignatureCAS");
		return authenticationProvider;
	}
	
	
	public Cas20ServiceTicketValidator cas20ServiceTicketValidator() {
		return new Cas20ServiceTicketValidator(casProperties.getUrl());
	}
	
	
	public UserDetailsByNameServiceWrapper<CasAssertionAuthenticationToken> casAuthUserDetailsService() {
		UserDetailsByNameServiceWrapper<CasAssertionAuthenticationToken> byNameServiceWrapper = new UserDetailsByNameServiceWrapper<>();
		byNameServiceWrapper.setUserDetailsService(ldapUserDetailsService());
		return byNameServiceWrapper;
	}
	
	
	public LdapUserDetailsService ldapUserDetailsService() {

		LdapUserSearch ldapUserSearch = new FilterBasedLdapUserSearch(ldapProperties.getSearchBase(), ldapProperties.getSearchFilter(), ldapContextSource);

		CasLdapAuthoritiesPopulator casLdapAuthoritiesPopulator = new CasLdapAuthoritiesPopulator(ldapContextSource, casProperties.getGroupSearchBase());

		Map<String, String> mappingGroupesRoles = new HashMap<>();
		mappingGroupesRoles.put(casProperties.getGroupMappingRoleAdmin(), "ROLE_ADMIN");
		casLdapAuthoritiesPopulator.setMappingGroupesRoles(mappingGroupesRoles);

		Map<String, String> ldapFiltersGroups = new HashMap<>();

		ldapFiltersGroups.put("eduPersonAffiliation:=student", casProperties.getGroupPrefixRoleName() + ".ROLE." + "STUDENT");
		ldapFiltersGroups.put("eduPersonAffiliation:=staff", casProperties.getGroupPrefixRoleName() + ".ROLE." + "STAFF");

		LdapGroupService ldapGroupService = new LdapGroupService();
		ldapGroupService.setPrefixRoleName(casProperties.getGroupPrefixRoleName());
		ldapGroupService.setLdapFiltersGroups(ldapFiltersGroups);
		ldapGroupService.setLdapTemplate(ldapTemplate);
		ldapGroupService.setGroupSearchBase("ou=groups");
	    ldapGroupService.setGroupSearchFilter("member={0}");
	    ldapGroupService.setMemberSearchBase("ou=people");
		ldapGroupService.setMemberSearchFilter("(&(uid={0})({1}))");

		casLdapAuthoritiesPopulator.setGroupService(ldapGroupService);

		LdapUserDetailsService ldapUserDetailsService = new LdapUserDetailsService(ldapUserSearch,
				casLdapAuthoritiesPopulator);

		LdapUserDetailsMapper ldapUserDetailsMapper = new LdapUserDetailsMapper();
		ldapUserDetailsMapper.setRoleAttributes(new String[] {});

		ldapUserDetailsService.setUserDetailsMapper(ldapUserDetailsMapper);

		return ldapUserDetailsService;
	}



//	public SingleSignOutFilter singleLogoutFilter() {
//		SingleSignOutFilter singleSignOutFilter = new SingleSignOutFilter();
//		singleSignOutFilter.setCasServerUrlPrefix(casProperties.getUrl() + "/logout");
//		return singleSignOutFilter;
//	}

	public LogoutFilter requestSingleLogoutFilter() {
		SecurityContextLogoutHandler securityContextLogoutHandler = new SecurityContextLogoutHandler();
		LogoutFilter logoutFilter = new LogoutFilter(casProperties.getUrl() + "/logout", securityContextLogoutHandler);
		logoutFilter.setFilterProcessesUrl("/logout");
		return logoutFilter;
	}
	
}
