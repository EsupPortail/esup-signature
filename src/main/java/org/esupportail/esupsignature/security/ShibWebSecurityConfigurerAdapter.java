package org.esupportail.esupsignature.security;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.session.ConcurrentSessionFilter;


public class ShibWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {
	
	@Resource
	private DatabaseUserDetailsService databaseUserDetailsService;

	@Autowired
	private ShibAuthenticatedUserDetailsService shibAuthUserDetailsService;
	
	@Autowired
	private SwitchUserFilter switchUserFilter;
	
	@Autowired
	ConcurrentSessionFilter concurrencyFilter;
	
	@Autowired
	RegisterSessionAuthenticationStrategy sessionAuthenticationStrategy;
	
	@Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(shibPreauthAuthProvider());
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
		http.addFilterBefore(shibFilter(), AbstractPreAuthenticatedProcessingFilter.class);
		http.addFilterBefore(switchUserFilter, SwitchUserFilter.class);
		http.addFilterBefore(concurrencyFilter, ConcurrentSessionFilter.class);
		http.sessionManagement().sessionAuthenticationStrategy(sessionAuthenticationStrategy);
		http.csrf().disable();		
	}

	public RequestHeaderAuthenticationFilter shibFilter() throws Exception {
		RequestHeaderAuthenticationFilter authenticationFilter = new RequestHeaderAuthenticationFilter();
		authenticationFilter.setPrincipalRequestHeader("REMOTE_USER");
		authenticationFilter.setCredentialsRequestHeader("MEMBER");
		authenticationFilter.setAuthenticationManager(shibAuthenticationManager());
		authenticationFilter.setExceptionIfHeaderMissing(false);
		return authenticationFilter;
	}
	
	public AuthenticationManager shibAuthenticationManager() {
		List<AuthenticationProvider> authenticatedAuthenticationProviders = new ArrayList<AuthenticationProvider>();
		authenticatedAuthenticationProviders.add(shibPreauthAuthProvider());
		AuthenticationManager authenticationManager = new ProviderManager(authenticatedAuthenticationProviders);
		return authenticationManager;
		
	}

	public PreAuthenticatedAuthenticationProvider shibPreauthAuthProvider() {
		PreAuthenticatedAuthenticationProvider authenticationProvider = new PreAuthenticatedAuthenticationProvider();
		authenticationProvider.setPreAuthenticatedUserDetailsService(shibAuthUserDetailsService);
		return authenticationProvider;
	}
	
	public LoginUrlAuthenticationEntryPoint getEntryPoint() {
		return new LoginUrlAuthenticationEntryPoint("/");
	}

}
