package org.esupportail.esupsignature.config;

import java.util.HashMap;
import java.util.Map;

import org.esupportail.esupsignature.ldap.PersonLdapDao;
import org.esupportail.esupsignature.security.Group2UserRoleService;
import org.esupportail.esupsignature.security.SpelGroupService;
import org.esupportail.esupsignature.security.cas.CasLdapAuthoritiesPopulator;
import org.esupportail.esupsignature.security.cas.CasWebSecurityConfigurerAdapter;
import org.esupportail.esupsignature.security.shib.ShibAuthenticatedUserDetailsService;
import org.esupportail.esupsignature.security.shib.ShibWebSecurityConfigurerAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;
import org.springframework.security.ldap.userdetails.LdapUserDetailsService;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.session.ConcurrentSessionFilter;

@Configuration
@EnableWebSecurity
@PropertySources({@PropertySource("security.properties")})
public class SecurityConfig {
	
	@Value("${security.filter}")
	private String filter;
	
	//TODO : repair cas

	@Bean
	public LdapContextSource ldapContextSource() {
		LdapContextSource contextSource = new LdapContextSource();
		contextSource.setUrl("ldap://ldap.univ-rouen.fr");
		contextSource.setBase("dc=univ-rouen,dc=fr");
		contextSource.setUserDn("cn=consult,dc=univ-rouen,dc=fr");
		contextSource.setPassword("iletoqp");
		contextSource.afterPropertiesSet();
		return contextSource;
	}
	
	@Bean
	public LdapTemplate ldapTemplate() {
		return new LdapTemplate(ldapContextSource());
	}
	
	@Bean
	public PersonLdapDao personLdapDao() {
		PersonLdapDao personLdapDao = new PersonLdapDao();
		personLdapDao.setLdapTemplate(ldapTemplate());
		return personLdapDao;
	}
	
	@Bean
	public LdapUserDetailsService ldapUserDetailsService() throws Exception {
		

		LdapUserSearch ldapUserSearch = new FilterBasedLdapUserSearch("ou=people", "(uid={0})", ldapContextSource());
		CasLdapAuthoritiesPopulator casLdapAuthoritiesPopulator = new CasLdapAuthoritiesPopulator(ldapContextSource(), "ou=groups");
		
		Map<String, String> mappingGroupesRoles = new HashMap<String, String>();
		mappingGroupesRoles.put("ROLE_FOR.ESUP-SIGNATURE.ADMIN", "ROLE_ADMIN");
		mappingGroupesRoles.put("ROLE_FOR.ESUP-SIGNATURE.MANAGER", "ROLE_MANAGER");
		casLdapAuthoritiesPopulator.setMappingGroupesRoles(mappingGroupesRoles);
		
		LdapUserDetailsService ldapUserDetailsService = new LdapUserDetailsService(ldapUserSearch, casLdapAuthoritiesPopulator);

		LdapUserDetailsMapper ldapUserDetailsMapper = new LdapUserDetailsMapper();
		ldapUserDetailsMapper.setRoleAttributes(new String[] {});

		ldapUserDetailsService.setUserDetailsMapper(ldapUserDetailsMapper);

		return ldapUserDetailsService;
	}
	
	@Bean
	public ShibAuthenticatedUserDetailsService shibAuthenticatedUserDetailsService() {
		ShibAuthenticatedUserDetailsService shibAuthenticatedUserDetailsService = new ShibAuthenticatedUserDetailsService();
		Map<String, String> mappingGroupesRoles = new HashMap<String, String>();
		mappingGroupesRoles.put("group_admin", "ROLE_ADMIN");
		mappingGroupesRoles.put("group_manager", "ROLE_MANAGER");
		
		SpelGroupService groupService = new SpelGroupService();
		Map<String, String> groups4eppnSpel = new HashMap<String, String>();
		mappingGroupesRoles.put("group_admin", "true");
		mappingGroupesRoles.put("group_manager", "true");
		groupService.setGroups4eppnSpel(groups4eppnSpel);
		
		Group2UserRoleService group2UserRoleService = new Group2UserRoleService();
		group2UserRoleService.setMappingGroupesRoles(mappingGroupesRoles);
		
		group2UserRoleService.setGroupService(groupService);
		shibAuthenticatedUserDetailsService.setGroup2UserRoleService(group2UserRoleService);
		shibAuthenticatedUserDetailsService.setMappingGroupesRoles(mappingGroupesRoles);
		return shibAuthenticatedUserDetailsService;
	}
	
	@Bean
	public WebSecurityConfigurerAdapter webSecurityConfigurerAdapter() {
		switch (filter) {
		case "SHIB" :
			return new ShibWebSecurityConfigurerAdapter();
		case "CAS" :
			return new CasWebSecurityConfigurerAdapter();
		default :
			return null;
		}
	}
	
	@Bean
	public SessionRegistryImpl sessionRegistry() {
		return new SessionRegistryImpl();
	}
	
	@Bean
	public ConcurrentSessionFilter concurrencyFilter() {
		ConcurrentSessionFilter concurrentSessionFilter = new ConcurrentSessionFilter(sessionRegistry());
		return concurrentSessionFilter;
	}
	
	@Bean
	public RegisterSessionAuthenticationStrategy sessionAuthenticationStrategy() {
		RegisterSessionAuthenticationStrategy authenticationStrategy = new RegisterSessionAuthenticationStrategy(sessionRegistry());
		return authenticationStrategy;
	}

}
