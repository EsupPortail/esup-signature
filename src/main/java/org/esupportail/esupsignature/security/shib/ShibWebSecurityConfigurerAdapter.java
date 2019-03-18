package org.esupportail.esupsignature.security.shib;

import java.util.ArrayList;
import java.util.List;

import org.esupportail.esupsignature.security.AuthorizeRequestsHelper;
import org.esupportail.esupsignature.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.session.ConcurrentSessionFilter;


public class ShibWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {
	
	@Value("${security.shib.principalRequestHeader}")
	private String principalRequestHeader;	
	@Value("${security.shib.credentialsRequestHeader}")
	private String credentialsRequestHeader;	
	@Value("${security.nfcWsAccessAuthorizeIps}")
	private String[] nfcWsAccessAuthorizeIps;	
	
	@Autowired
	private ShibAuthenticatedUserDetailsService shibAuthUserDetailsService;
	
	@Autowired
	private SwitchUserFilter switchUserFilter;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private ConcurrentSessionFilter concurrencyFilter;
	
	@Autowired
	private RegisterSessionAuthenticationStrategy sessionAuthenticationStrategy;
	
	@Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(shibPreauthAuthProvider());
    }
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		AuthorizeRequestsHelper.setAuthorizeRequests(http, nfcWsAccessAuthorizeIps);
		http.exceptionHandling().authenticationEntryPoint(getEntryPoint());
		http.addFilterBefore(authenticationFilter(), AbstractPreAuthenticatedProcessingFilter.class);
		http.addFilterBefore(switchUserFilter, SwitchUserFilter.class);
		http.addFilterBefore(concurrencyFilter, ConcurrentSessionFilter.class);
		http.sessionManagement().sessionAuthenticationStrategy(sessionAuthenticationStrategy);
		http.csrf().disable();		
	}
	
	public RequestHeaderAuthenticationFilter authenticationFilter() throws Exception {
		ShibRequestHeaderAuthenticationFilter authenticationFilter = new ShibRequestHeaderAuthenticationFilter();
		authenticationFilter.setUserService(userService);
		authenticationFilter.setPrincipalRequestHeader(principalRequestHeader);
		authenticationFilter.setCredentialsRequestHeader(credentialsRequestHeader);
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
