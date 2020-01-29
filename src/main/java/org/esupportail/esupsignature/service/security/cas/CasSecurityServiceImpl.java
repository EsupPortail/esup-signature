package org.esupportail.esupsignature.service.security.cas;

import org.esupportail.esupsignature.config.security.cas.CasProperties;
import org.esupportail.esupsignature.service.security.SecurityService;
import org.jasig.cas.client.session.SingleSignOutFilter;
import org.jasig.cas.client.validation.Cas20ServiceTicketValidator;
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
	private CasProperties casProperties;

	@Resource
	private CasAuthenticationSuccessHandler casAuthenticationSuccessHandler;
	
	@Resource
	private RegisterSessionAuthenticationStrategy sessionAuthenticationStrategy;

	@Resource
	private LdapContextSource ldapContextSource;
	

	public String getName() {
		return "Compte Université de Rouen Normandie (CAS)";
	}
	
	public String getLoginUrl() {
		return "/login/casentry";
	}
	
	public CasAuthenticationEntryPoint getAuthenticationEntryPoint() {
		CasAuthenticationEntryPoint authenticationEntryPoint = new CasAuthenticationEntryPoint();
		authenticationEntryPoint.setLoginUrl(casProperties.getUrl() + "/login");
		authenticationEntryPoint.setServiceProperties(serviceProperties());
		return authenticationEntryPoint;
	}
	
	
	public ServiceProperties serviceProperties() {
		ServiceProperties serviceProperties = new ServiceProperties();
		serviceProperties.setService(casProperties.getService());
		serviceProperties.setSendRenew(false);
		return serviceProperties;
	}
	
	
	public CasAuthenticationFilter getAuthenticationProcessingFilter() {
		CasAuthenticationFilter authenticationFilter = new CasAuthenticationFilter();
		authenticationFilter.setAuthenticationManager(casAuthenticationManager());
		authenticationFilter.setSessionAuthenticationStrategy(sessionAuthenticationStrategy);
		authenticationFilter.setAuthenticationSuccessHandler(casAuthenticationSuccessHandler);
		return authenticationFilter;
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
		authenticationProvider.setKey(casProperties.getKey());
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

		LdapUserSearch ldapUserSearch = new FilterBasedLdapUserSearch("ou=people", "(uid={0})", ldapContextSource);
		CasLdapAuthoritiesPopulator casLdapAuthoritiesPopulator = new CasLdapAuthoritiesPopulator(ldapContextSource,"ou=groups");

		Map<String, String> mappingGroupesRoles = new HashMap<String, String>();
		mappingGroupesRoles.put(casProperties.getGroupMappingRoleAdmin(), "ROLE_ADMIN");
		mappingGroupesRoles.put(casProperties.getGroupMappingRoleManager(), "ROLE_MANAGER");
		casLdapAuthoritiesPopulator.setMappingGroupesRoles(mappingGroupesRoles);

		LdapUserDetailsService ldapUserDetailsService = new LdapUserDetailsService(ldapUserSearch,
				casLdapAuthoritiesPopulator);

		LdapUserDetailsMapper ldapUserDetailsMapper = new LdapUserDetailsMapper();
		ldapUserDetailsMapper.setRoleAttributes(new String[] {});

		ldapUserDetailsService.setUserDetailsMapper(ldapUserDetailsMapper);

		return ldapUserDetailsService;
	}
	
	public SingleSignOutFilter singleLogoutFilter() {
		SingleSignOutFilter singleSignOutFilter = new SingleSignOutFilter();
		singleSignOutFilter.setCasServerUrlPrefix(casProperties.getUrl() + "/logout");
		return singleSignOutFilter;
	}

	public LogoutFilter requestSingleLogoutFilter() {
		SecurityContextLogoutHandler securityContextLogoutHandler = new SecurityContextLogoutHandler();
		LogoutFilter logoutFilter = new LogoutFilter(casProperties.getUrl() + "/logout", securityContextLogoutHandler);
		logoutFilter.setFilterProcessesUrl("/logout");
		return logoutFilter;
	}
	
}
