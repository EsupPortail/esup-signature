package org.esupportail.esupsignature.service.security.cas;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
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
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@Order(1)
@ConditionalOnProperty({"spring.ldap.base", "security.cas.service"})
public class CasSecurityServiceImpl implements SecurityService {

	private static final Logger logger = LoggerFactory.getLogger(CasSecurityServiceImpl.class);

	private final WebSecurityProperties webSecurityProperties;
	private final SpelGroupService spelGroupService;
	private final LdapGroupService ldapGroupService;
	private final CasProperties casProperties;
	private final LdapProperties ldapProperties;
	private final CasAuthenticationSuccessHandler casAuthenticationSuccessHandler;
	private final LdapContextSource ldapContextSource;
	private final MappingFiltersGroupsRepository mappingFiltersGroupsRepository;
	private final MappingGroupsRolesRepository mappingGroupsRolesRepository;
	private final RegisterSessionAuthenticationStrategy registerSessionAuthenticationStrategy;

	private LdapUserDetailsService ldapUserDetailsService;

    public CasSecurityServiceImpl(WebSecurityProperties webSecurityProperties, SpelGroupService spelGroupService, LdapGroupService ldapGroupService, CasProperties casProperties, LdapProperties ldapProperties, CasAuthenticationSuccessHandler casAuthenticationSuccessHandler, LdapContextSource ldapContextSource, MappingFiltersGroupsRepository mappingFiltersGroupsRepository, MappingGroupsRolesRepository mappingGroupsRolesRepository, RegisterSessionAuthenticationStrategy registerSessionAuthenticationStrategy) {
        this.webSecurityProperties = webSecurityProperties;
        this.spelGroupService = spelGroupService;
        this.ldapGroupService = ldapGroupService;
        this.ldapGroupService.loadLdapFiltersGroups();
        this.casProperties = casProperties;
        this.ldapProperties = ldapProperties;
        this.casAuthenticationSuccessHandler = casAuthenticationSuccessHandler;
        this.ldapContextSource = ldapContextSource;
        this.mappingFiltersGroupsRepository = mappingFiltersGroupsRepository;
        this.mappingGroupsRolesRepository = mappingGroupsRolesRepository;
        this.registerSessionAuthenticationStrategy = registerSessionAuthenticationStrategy;
    }

    @Override
	public String getTitle() {
		return casProperties.getTitle();
	}

	@Override
	public String getCode() {
		return this.getClass().getSimpleName();
	}

	@Override
	public String getDescription() {
		return "Si vous êtes personnel ou étudiant utilisez votre compte unique CAS";
	}

	@Override
	public String getLoginUrl() {
		return "/login/casentry";
	}

	@Override
	public String getLoggedOutUrl() {
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
		if(this.ldapUserDetailsService != null) return this.ldapUserDetailsService;
		if(!StringUtils.hasText(ldapProperties.getGroupSearchBase())) {
			logger.warn("no groupSearchBase found, unable to get users groups automatically");
		}
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
		LdapUserSearch ldapUserSearch = new FilterBasedLdapUserSearch(ldapProperties.getSearchBase(), ldapProperties.getCasUserSearchFilter(), ldapContextSource);
		LdapUserDetailsService ldapUserDetailsService = new LdapUserDetailsService(ldapUserSearch, casLdapAuthoritiesPopulator);
		LdapUserDetailsMapper ldapUserDetailsMapper = new LdapUserDetailsMapper();
		ldapUserDetailsMapper.setRoleAttributes(new String[] {});
		ldapUserDetailsService.setUserDetailsMapper(ldapUserDetailsMapper);
		this.ldapUserDetailsService = ldapUserDetailsService;
		return ldapUserDetailsService;
	}

}
