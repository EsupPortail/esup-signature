package org.esupportail.esupsignature.service.security.cas;

import jakarta.annotation.Resource;
import org.apereo.cas.client.validation.Cas20ServiceTicketValidator;
import org.esupportail.esupsignature.config.ldap.LdapProperties;
import org.esupportail.esupsignature.config.security.WebSecurityProperties;
import org.esupportail.esupsignature.config.security.cas.CasProperties;
import org.esupportail.esupsignature.repository.MappingFiltersGroupsRepository;
import org.esupportail.esupsignature.repository.MappingGroupsRolesRepository;
import org.esupportail.esupsignature.service.ldap.LdapGroupService;
import org.esupportail.esupsignature.service.security.Group2UserRoleService;
import org.esupportail.esupsignature.service.security.SecurityService;
import org.esupportail.esupsignature.service.security.SpelGroupService;
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
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;

import java.util.ArrayList;
import java.util.List;

public class CasSecurityServiceImpl implements SecurityService {

	@Resource
	private WebSecurityProperties webSecurityProperties;

	@Resource
	private SpelGroupService spelGroupService;

	@Resource
	private LdapGroupService ldapGroupService;

	@Resource
	private CasProperties casProperties;

	@Resource
	private LdapProperties ldapProperties;

	@Resource
	private CasAuthenticationSuccessHandler casAuthenticationSuccessHandler;

	@Resource
	private LdapContextSource ldapContextSource;

	@Resource
	private MappingFiltersGroupsRepository mappingFiltersGroupsRepository;

	@Resource
	private MappingGroupsRolesRepository mappingGroupsRolesRepository;

	@Resource
	private RegisterSessionAuthenticationStrategy registerSessionAuthenticationStrategy;

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
		authenticationFilter.setSessionAuthenticationStrategy(registerSessionAuthenticationStrategy);
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
		List<AuthenticationProvider> authenticatedAuthenticationProviders = new ArrayList<>();
		authenticatedAuthenticationProviders.add(casAuthenticationProvider());
		return new ProviderManager(authenticatedAuthenticationProviders);
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
		LdapUserSearch ldapUserSearch = new FilterBasedLdapUserSearch(ldapProperties.getSearchBase(), ldapProperties.getUserIdSearchFilter(), ldapContextSource);
		CasLdapAuthoritiesPopulator casLdapAuthoritiesPopulator = new CasLdapAuthoritiesPopulator(ldapContextSource, ldapProperties.getGroupSearchBase());
		casLdapAuthoritiesPopulator.setMappingFiltersGroupsRepository(mappingFiltersGroupsRepository);
		casLdapAuthoritiesPopulator.setMappingGroupsRolesRepository(mappingGroupsRolesRepository);
		casLdapAuthoritiesPopulator.setRolePrefix("");
		casLdapAuthoritiesPopulator.setGroupPrefixRoleName(webSecurityProperties.getGroupToRoleFilterPattern());
		casLdapAuthoritiesPopulator.setMappingGroupesRoles(webSecurityProperties.getMappingGroupsRoles());
		casLdapAuthoritiesPopulator.setLdapGroupService(ldapGroupService);
		Group2UserRoleService group2UserRoleService = new Group2UserRoleService();
		group2UserRoleService.setGroupPrefixRoleName(webSecurityProperties.getGroupToRoleFilterPattern());
		group2UserRoleService.setMappingGroupesRoles(webSecurityProperties.getMappingGroupsRoles());
		group2UserRoleService.setGroupService(spelGroupService);
		casLdapAuthoritiesPopulator.setGroup2UserRoleService(group2UserRoleService);
		LdapUserDetailsService ldapUserDetailsService = new LdapUserDetailsService(ldapUserSearch, casLdapAuthoritiesPopulator);
		LdapUserDetailsMapper ldapUserDetailsMapper = new LdapUserDetailsMapper();
		ldapUserDetailsMapper.setRoleAttributes(new String[] {});
		ldapUserDetailsService.setUserDetailsMapper(ldapUserDetailsMapper);
		return ldapUserDetailsService;
	}

}
