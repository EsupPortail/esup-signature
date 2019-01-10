package org.esupportail.esupsignature.security;

import java.util.ArrayList;
import java.util.List;

import org.jasig.cas.client.session.SingleSignOutFilter;
import org.jasig.cas.client.validation.Cas20ServiceTicketValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.cas.authentication.CasAssertionAuthenticationToken;
import org.springframework.security.cas.authentication.CasAuthenticationProvider;
import org.springframework.security.cas.web.CasAuthenticationEntryPoint;
import org.springframework.security.cas.web.CasAuthenticationFilter;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsByNameServiceWrapper;
import org.springframework.security.ldap.userdetails.LdapUserDetailsService;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.session.ConcurrentSessionFilter;

public class CasWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

	@Value("${security.cas.key}")
	private String casKey;	
	@Value("${security.cas.url}")
	private String casUrl;
	@Value("${security.cas.service}")
	private String casService;

	@Autowired
	private LdapUserDetailsService ldapUserDetailsService;

	@Autowired
	private SwitchUserFilter switchUserFilter;
	
	@Autowired
	ConcurrentSessionFilter concurrencyFilter;
	
	@Autowired
	RegisterSessionAuthenticationStrategy sessionAuthenticationStrategy;

	@Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(casAuthenticationProvider());
    }
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.logout().logoutSuccessUrl("/").permitAll();
		http.authorizeRequests()
				.antMatchers("/j_spring_security_switch_user").access("hasRole('ROLE_ADMIN')")
				.antMatchers("/admin/", "/admin/**").access("hasRole('ROLE_ADMIN')")
				.antMatchers("/manager/", "/manager/**").access("hasRole('ROLE_ADMIN') or hasRole('ROLE_ADMIN')")
				.antMatchers("/nfc-ws/**").access("hasIpAddress('127.0.0.1') or hasIpAddress('Esup-NFC-Tag IP Address')")
				.antMatchers("/login/**").access("isAuthenticated()")
				.antMatchers("/**").access("permitAll");
		AccessDeniedHandlerImpl accessDeniedHandlerImpl = new AccessDeniedHandlerImpl();
		accessDeniedHandlerImpl.setErrorPage("/denied");
		http.exceptionHandling().accessDeniedHandler(accessDeniedHandlerImpl);
		http.exceptionHandling().authenticationEntryPoint(getEntryPoint());
		http.addFilterBefore(casFilter(), CasAuthenticationFilter.class);
		http.addFilterBefore(singleLogoutFilter(), CasAuthenticationFilter.class);
		http.addFilterBefore(requestSingleLogoutFilter(), LogoutFilter.class);
		http.addFilterBefore(switchUserFilter, SwitchUserFilter.class);
		http.addFilterBefore(concurrencyFilter, ConcurrentSessionFilter.class);
		http.sessionManagement().sessionAuthenticationStrategy(sessionAuthenticationStrategy);
		http.csrf().disable();		
	}

	public CasAuthenticationFilter casFilter() {
		CasAuthenticationFilter authenticationFilter = new CasAuthenticationFilter();
		authenticationFilter.setAuthenticationManager(casAuthenticationManager());
		authenticationFilter.setSessionAuthenticationStrategy(sessionAuthenticationStrategy);
		return authenticationFilter;
	}
	
	public AuthenticationManager casAuthenticationManager() {
		List<AuthenticationProvider> authenticatedAuthenticationProviders = new ArrayList<AuthenticationProvider>();
		authenticatedAuthenticationProviders.add(casAuthenticationProvider());
		AuthenticationManager authenticationManager = new ProviderManager(authenticatedAuthenticationProviders);
		return authenticationManager;
	}

	@Bean
	public CasAuthenticationProvider casAuthenticationProvider() {
		CasAuthenticationProvider authenticationProvider = new CasAuthenticationProvider();
		authenticationProvider.setAuthenticationUserDetailsService(casAuthUserDetailsService());
		authenticationProvider.setServiceProperties(serviceProperties());
		authenticationProvider.setTicketValidator(cas20ServiceTicketValidator());
		authenticationProvider.setKey(casKey);
		return authenticationProvider;
	}
	
	@Bean
    public Cas20ServiceTicketValidator cas20ServiceTicketValidator() {
        return new Cas20ServiceTicketValidator(casUrl);
    }
	
	@Bean
	public CasAuthenticationEntryPoint getEntryPoint() {
		CasAuthenticationEntryPoint authenticationEntryPoint = new CasAuthenticationEntryPoint();
		authenticationEntryPoint.setLoginUrl(casUrl + "/login");
		authenticationEntryPoint.setServiceProperties(serviceProperties());
		return authenticationEntryPoint;
	}
	
	@Bean
	public ServiceProperties serviceProperties(){
		ServiceProperties serviceProperties = new ServiceProperties();
		serviceProperties.setService(casService);
		serviceProperties.setSendRenew(false);
		return serviceProperties;
	}

	
	public UserDetailsByNameServiceWrapper<CasAssertionAuthenticationToken> casAuthUserDetailsService() {
		UserDetailsByNameServiceWrapper<CasAssertionAuthenticationToken> byNameServiceWrapper = new UserDetailsByNameServiceWrapper<>();
		byNameServiceWrapper.setUserDetailsService(ldapUserDetailsService);
		return byNameServiceWrapper;
	}

	public SingleSignOutFilter singleLogoutFilter() {
		SingleSignOutFilter singleSignOutFilter = new SingleSignOutFilter();
		return singleSignOutFilter;
	}

	public LogoutFilter requestSingleLogoutFilter() {
		SecurityContextLogoutHandler securityContextLogoutHandler = new SecurityContextLogoutHandler();
		LogoutFilter logoutFilter = new LogoutFilter(casUrl + "logout", securityContextLogoutHandler);
		logoutFilter.setFilterProcessesUrl("/j_spring_cas_security_logout");
		return logoutFilter;
	}
	
}
