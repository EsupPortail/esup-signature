package org.esupportail.esupsignature.config.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apereo.cas.client.session.SingleSignOutHttpSessionListener;
import org.apache.commons.lang3.BooleanUtils;
import org.apereo.cas.client.util.AbstractConfigurationFilter;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.security.cas.CasJwtDecoder;
import org.esupportail.esupsignature.config.security.jwt.CustomJwtAuthenticationConverter;
import org.esupportail.esupsignature.config.security.jwt.MdcUsernameFilter;
import org.esupportail.esupsignature.config.security.otp.OtpAuthenticationProvider;
import org.esupportail.esupsignature.config.sms.SmsProperties;
import org.esupportail.esupsignature.entity.enums.ExternalAuth;
import org.esupportail.esupsignature.service.security.IndexEntryPoint;
import org.esupportail.esupsignature.service.security.LogoutHandlerImpl;
import org.esupportail.esupsignature.service.security.OidcSecurityService;
import org.esupportail.esupsignature.service.security.OidcOtpSecurityService;
import org.esupportail.esupsignature.service.security.SecurityService;
import org.esupportail.esupsignature.service.security.cas.CasSecurityServiceImpl;
import org.esupportail.esupsignature.service.security.oauth.CustomAuthorizationRequestResolver;
import org.esupportail.esupsignature.service.security.oauth.OidcUserSecurityServiceResolver;
import org.esupportail.esupsignature.service.security.oauth.OAuth2FailureHandler;
import org.esupportail.esupsignature.service.security.oauth.OAuthAuthenticationSuccessHandler;
import org.esupportail.esupsignature.service.security.oauth.ValidatingOAuth2UserService;
import org.esupportail.esupsignature.service.security.oauth.franceconnect.FranceConnectSecurityServiceImpl;
import org.esupportail.esupsignature.service.security.oauth.proconnect.ProConnectSecurityServiceImpl;
import org.esupportail.esupsignature.service.security.shib.DevShibRequestFilter;
import org.esupportail.esupsignature.service.security.shib.ShibSecurityServiceImpl;
import org.esupportail.esupsignature.service.security.su.SuAuthenticationSuccessHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.security.oauth2.client.autoconfigure.ConditionalOnOAuth2ClientRegistrationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.ExceptionMappingAuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Configuration
@EnableWebSecurity(debug = false)
@EnableMethodSecurity(
		securedEnabled = true,
		jsr250Enabled = true)
public class WebSecurityConfig {

	private static final Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);

	private final String apiKey = "SomeKey1234567890";

	private final GlobalProperties globalProperties;
	private final OAuthAuthenticationSuccessHandler oAuthAuthenticationSuccessHandler;
	private final WebSecurityProperties webSecurityProperties;
	private final ClientRegistrationRepository clientRegistrationRepository;
	private final List<SecurityService> securityServices;
	private final RegisterSessionAuthenticationStrategy sessionAuthenticationStrategy;
	private final SessionRegistryImpl sessionRegistry;
	private final LogoutHandlerImpl logoutHandler;
	private final CasJwtDecoder casJwtDecoder;
	private final OidcUserSecurityServiceResolver oidcUserSecurityServiceResolver;
	private final Environment environment;
	private DevShibRequestFilter devShibRequestFilter;

	public WebSecurityConfig(GlobalProperties globalProperties, OAuthAuthenticationSuccessHandler oAuthAuthenticationSuccessHandler, WebSecurityProperties webSecurityProperties, @Autowired(required = false) ClientRegistrationRepository clientRegistrationRepository, List<SecurityService> securityServices, RegisterSessionAuthenticationStrategy sessionAuthenticationStrategy, SessionRegistryImpl sessionRegistry, LogoutHandlerImpl logoutHandler, @Autowired(required = false) CasJwtDecoder casJwtDecoder, OidcUserSecurityServiceResolver oidcUserSecurityServiceResolver, Environment environment) {
        this.globalProperties = globalProperties;
        this.oAuthAuthenticationSuccessHandler = oAuthAuthenticationSuccessHandler;
        this.webSecurityProperties = webSecurityProperties;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.securityServices = securityServices;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.sessionRegistry = sessionRegistry;
        this.logoutHandler = logoutHandler;
        this.casJwtDecoder = casJwtDecoder;
        this.oidcUserSecurityServiceResolver = oidcUserSecurityServiceResolver;
        this.environment = environment;
    }

	@Bean
	@ConditionalOnProperty(name = "spring.security.oauth2.client.provider.cas.issuer-uri")
	public SecurityFilterChain wsJwtSecurityFilter(HttpSecurity http) throws Exception {
		http.securityMatcher("/ws-jwt/**");
		if (StringUtils.hasText(issuerUri)) {
			http.oauth2ResourceServer(oauth2 -> oauth2.bearerTokenResolver(bearerTokenResolver())
					.authenticationEntryPoint(wsJwtAuthenticationEntryPoint())
					.withObjectPostProcessor(new ObjectPostProcessor<BearerTokenAuthenticationFilter>() {
						@Override
						public <O extends BearerTokenAuthenticationFilter> O postProcess(O filter) {
							filter.setAuthenticationFailureHandler(wsJwtAuthenticationFailureHandler());
							return filter;
						}
					})
					.jwt(jwt -> jwt.decoder(casJwtDecoder)
					.jwtAuthenticationConverter(new CustomJwtAuthenticationConverter(globalProperties.getDomain()))));
			http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
		} else {
			http.authorizeHttpRequests(auth -> auth.anyRequest().denyAll());
		}
		http.cors(AbstractHttpConfigurer::disable);
		http.addFilterAfter(new MdcUsernameFilter(), AuthorizationFilter.class);
		return http.build();
	}

	public AuthenticationEntryPoint wsJwtAuthenticationEntryPoint() {
		return (request, response, authException) -> {
			logger.warn("Authentification JWT refusee pour {} {} : {}", request.getMethod(), request.getRequestURI(), authException.getMessage());
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT invalide ou expire");
		};
	}

	public AuthenticationFailureHandler wsJwtAuthenticationFailureHandler() {
		return (request, response, exception) -> wsJwtAuthenticationEntryPoint().commence(request, response, exception);
	}

	@Value("${spring.security.oauth2.client.provider.cas.issuer-uri:}")
	private String issuerUri;

	@Bean
	public BearerTokenResolver bearerTokenResolver() {
		return new BearerTokenResolver() {
			private final DefaultBearerTokenResolver defaultResolver = new DefaultBearerTokenResolver();

			@Override
			public String resolve(HttpServletRequest request) {
				String token = defaultResolver.resolve(request);
				if (token != null) return token;
				if (request.getCookies() != null) {
					for (Cookie cookie : request.getCookies()) {
						if ("jwt".equalsIgnoreCase(cookie.getName())) {
							return cookie.getValue();
						}
					}
				}
				return null;
			}
		};
	}

	@Bean
	@Order(4)
	@ConditionalOnProperty(prefix = "security.shib.dev", name = "enable", havingValue = "true")
	public DevShibRequestFilter devClientRequestFilter() {
		devShibRequestFilter = new DevShibRequestFilter();
		return devShibRequestFilter;
	}

	@Bean
	public HttpSessionEventPublisher sessionEventPublisher() {
		return new HttpSessionEventPublisher();
	}

	@Bean
	public SingleSignOutHttpSessionListener singleSignOutHttpSessionListener() {
		return new SingleSignOutHttpSessionListener();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		List<SecurityService> activeSecurityServices = getActiveSecurityServices();
		List<OidcSecurityService> activeOidcSecurityServices = getActiveOidcSecurityServices();
		http.sessionManagement(sessionManagement -> sessionManagement.sessionAuthenticationStrategy(sessionAuthenticationStrategy).maximumSessions(5).sessionRegistry(sessionRegistry));
		if(devShibRequestFilter != null) {
			http.addFilterBefore(devShibRequestFilter, OAuth2AuthorizationRequestRedirectFilter.class);
		}
		http.exceptionHandling(exceptionHandling -> exceptionHandling.defaultAuthenticationEntryPointFor(new IndexEntryPoint("/"), PathPatternRequestMatcher.withDefaults().matcher("/")));
		AccessDeniedHandlerImpl accessDeniedHandlerImpl = new AccessDeniedHandlerImpl();
		accessDeniedHandlerImpl.setErrorPage("/denied");
		http.exceptionHandling(exceptionHandling -> exceptionHandling.accessDeniedHandler(accessDeniedHandlerImpl));
		if(!activeOidcSecurityServices.isEmpty()) {
			http.oauth2Login(oauth2Login -> oauth2Login.loginPage("/")
				.successHandler(oAuthAuthenticationSuccessHandler)
				.failureHandler(new OAuth2FailureHandler())
				.userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint.oidcUserService(validatingOAuth2UserService()))
				.authorizationEndpoint(authorizationEndpoint -> authorizationEndpoint.authorizationRequestResolver(customAuthorizationRequestResolver())));
		}
		for(SecurityService securityService : activeSecurityServices) {
			http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests.requestMatchers(securityService.getLoginUrl()).authenticated());
			http.exceptionHandling(exceptionHandling -> exceptionHandling.defaultAuthenticationEntryPointFor(securityService.getAuthenticationEntryPoint(), PathPatternRequestMatcher.withDefaults().matcher(securityService.getLoginUrl())));
			GenericFilterBean authenticationProcessingFilter = securityService.getAuthenticationProcessingFilter();
			if(authenticationProcessingFilter != null) {
				http.addFilterBefore(securityService.getAuthenticationProcessingFilter(), OAuth2AuthorizationRequestRedirectFilter.class);
				AbstractConfigurationFilter singleSignOutFilter = securityService.getSingleSignOutFilter();
				if(singleSignOutFilter != null) {
					http.addFilterBefore(singleSignOutFilter, LogoutFilter.class);
				}
			}
		}
		if(globalProperties.getEnableSu()) {
			for (SecurityService securityService : securityServices) {
				if (securityService instanceof CasSecurityServiceImpl) {
					switchUserFilter().setUserDetailsService(securityService.getUserDetailsService());
				} else if (securityService instanceof ShibSecurityServiceImpl) {
					switchUserFilter().setUserDetailsService(securityService.getUserDetailsService());
					break;
				}
			}
		}
		http.logout(logout -> logout.addLogoutHandler(logoutHandler).invalidateHttpSession(true)
						.logoutUrl("/logout").logoutSuccessUrl("/logged-out"));
		http.csrf(csrf -> csrf.ignoringRequestMatchers(("/resources/**"))
				.ignoringRequestMatchers("/ws/**")
				.ignoringRequestMatchers("/nexu-sign/**")
				.ignoringRequestMatchers("/h2-console/**")
				.ignoringRequestMatchers("/login/cas")
				.ignoringRequestMatchers("/cas/slo")
				.ignoringRequestMatchers("/csp-report")
				.ignoringRequestMatchers("/public/mobile-sign/**"));
		Set<String> formAction = new LinkedHashSet<>();
		formAction.add("'self'");
		addFormActionOrigin(formAction, globalProperties.getRootUrl(), "globalProperties.rootUrl");
		for(SecurityService securityService : activeSecurityServices) {
			addFormActionOrigin(formAction, securityService.getLoggedOutUrl(), "security service " + securityService.getCode());
		}
		for(OidcSecurityService securityService : getActiveOidcSecurityServices()) {
			addOidcAuthorizationFormActionOrigin(formAction, securityService);
		}
		http.headers(headers -> {
			headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin);
			if(webSecurityProperties.isContentSecurityPolicyEnabled()) {
				Set<String> connectSrc = new LinkedHashSet<>();
				connectSrc.add("'self'");
				addConnectSrcOrigin(connectSrc, globalProperties.getNexuUrl(), "globalProperties.nexuUrl");
				headers.addHeaderWriter((request, response) -> {
					Set<String> requestFormAction = new LinkedHashSet<>(formAction);
					addRequestOrigin(requestFormAction, request);
					response.setHeader("Content-Security-Policy", buildCspPolicy(connectSrc, requestFormAction, false));
					response.setHeader("Content-Security-Policy-Report-Only", buildCspPolicy(connectSrc, requestFormAction, true));
				});
			} else {
				logger.warn("Content-Security-Policy headers are disabled by configuration");
			}
		});
		setAuthorizeRequests(http);
		return http.build();
	}

	@Bean
	public ValidatingOAuth2UserService validatingOAuth2UserService() {
		return new ValidatingOAuth2UserService(getActiveOidcSecurityServices(), clientRegistrationRepository, List.of(environment.getActiveProfiles()).contains("dev"));
	}

	@Bean
	@ConditionalOnOAuth2ClientRegistrationProperties
	public CustomAuthorizationRequestResolver customAuthorizationRequestResolver() {
		return new CustomAuthorizationRequestResolver(clientRegistrationRepository, getActiveOidcSecurityServices());
	}

	private List<SecurityService> getActiveSecurityServices() {
		List<SecurityService> activeSecurityServices = new ArrayList<>(securityServices.stream().filter(this::isActiveSecurityService).toList());
		activeSecurityServices.addAll(oidcUserSecurityServiceResolver.getConfiguredServices());
		return activeSecurityServices;
	}

	private List<OidcSecurityService> getActiveOidcSecurityServices() {
		List<OidcSecurityService> oidcSecurityServices = new ArrayList<>(securityServices.stream()
				.filter(OidcSecurityService.class::isInstance)
				.map(OidcSecurityService.class::cast)
				.filter(this::hasClientRegistration)
				.toList());
		oidcSecurityServices.addAll(oidcUserSecurityServiceResolver.getConfiguredServices());
		return oidcSecurityServices;
	}

	private boolean isActiveSecurityService(SecurityService securityService) {
		return !(securityService instanceof OidcSecurityService) || hasClientRegistration((OidcSecurityService) securityService);
	}

	private boolean hasClientRegistration(OidcSecurityService securityService) {
		return clientRegistrationRepository != null && clientRegistrationRepository.findByRegistrationId(securityService.getCode()) != null;
	}

	private void addFormActionOrigin(Set<String> formAction, String url, String source) {
		if(!StringUtils.hasText(url)) {
			return;
		}
		try {
			URI uri = new URI(url);
			String scheme = uri.getScheme();
			String host = uri.getHost();
			if(!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) || !StringUtils.hasText(host)) {
				logger.warn("Ignoring invalid form-action URL from {}: {}", source, url);
				return;
			}
			StringBuilder origin = new StringBuilder(scheme.toLowerCase()).append("://").append(host);
			if(uri.getPort() != -1) {
				origin.append(":").append(uri.getPort());
			}
			formAction.add(origin.toString());
		} catch (URISyntaxException e) {
			logger.warn("Ignoring invalid form-action URL from {}: {}", source, url);
		}
	}

	private void addConnectSrcOrigin(Set<String> connectSrc, String url, String source) {
		if(!StringUtils.hasText(url)) {
			return;
		}
		try {
			URI uri = new URI(url);
			String origin = getHttpOrigin(uri);
			if(origin == null) {
				logger.warn("Ignoring invalid connect-src URL from {}: {}", source, url);
				return;
			}
			connectSrc.add(origin);
			if("localhost".equalsIgnoreCase(uri.getHost())) {
				connectSrc.add(buildOrigin(uri.getScheme(), "127.0.0.1", uri.getPort()));
			} else if("127.0.0.1".equals(uri.getHost())) {
				connectSrc.add(buildOrigin(uri.getScheme(), "localhost", uri.getPort()));
			}
		} catch (URISyntaxException e) {
			logger.warn("Ignoring invalid connect-src URL from {}: {}", source, url);
		}
	}

	private void addOidcAuthorizationFormActionOrigin(Set<String> formAction, OidcSecurityService securityService) {
		if(clientRegistrationRepository == null) {
			return;
		}
		ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(securityService.getCode());
		if(registration == null) {
			return;
		}
		addFormActionOrigin(formAction, registration.getProviderDetails().getAuthorizationUri(), "OIDC authorization URI " + securityService.getCode());
	}

	private String buildCspPolicy(Set<String> connectSrc, Set<String> formAction, boolean reportOnly) {
		String scriptSrc = reportOnly ? "script-src 'self'" : "script-src 'self' 'unsafe-inline' 'unsafe-eval'";
		String reportUri = reportOnly ? "; report-uri /csp-report" : "";
		return "default-src 'self'; "
				+ scriptSrc
				+ "; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; font-src 'self' data:; connect-src " + String.join(" ", connectSrc)
				+ "; object-src blob:; frame-src 'self' blob:; base-uri 'self'; frame-ancestors 'self'; form-action " + String.join(" ", formAction)
				+ reportUri;
	}

	private void addRequestOrigin(Set<String> formAction, HttpServletRequest request) {
		String forwardedProto = getFirstHeaderValue(request, "X-Forwarded-Proto");
		String forwardedHost = getFirstHeaderValue(request, "X-Forwarded-Host");
		if(StringUtils.hasText(forwardedProto) && StringUtils.hasText(forwardedHost)) {
			addFormActionOrigin(formAction, forwardedProto + "://" + forwardedHost, "request forwarded origin");
			return;
		}
		int port = request.getServerPort();
		if(isDefaultPort(request.getScheme(), port)) {
			port = -1;
		}
		formAction.add(buildOrigin(request.getScheme(), request.getServerName(), port));
	}

	private String getFirstHeaderValue(HttpServletRequest request, String headerName) {
		String header = request.getHeader(headerName);
		if(!StringUtils.hasText(header)) {
			return null;
		}
		return header.split(",")[0].trim();
	}

	private boolean isDefaultPort(String scheme, int port) {
		return ("http".equalsIgnoreCase(scheme) && port == 80) || ("https".equalsIgnoreCase(scheme) && port == 443);
	}

	private String getHttpOrigin(URI uri) {
		String scheme = uri.getScheme();
		String host = uri.getHost();
		if(!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) || !StringUtils.hasText(host)) {
			return null;
		}
		return buildOrigin(scheme, host, uri.getPort());
	}

	private String buildOrigin(String scheme, String host, int port) {
		StringBuilder origin = new StringBuilder(scheme.toLowerCase()).append("://").append(host);
		if(port != -1) {
			origin.append(":").append(port);
		}
		return origin.toString();
	}

	private void setAuthorizeRequests(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests.requestMatchers(("/")).permitAll());
		http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests.requestMatchers(("/logged-out")).permitAll());
		http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests.requestMatchers(("/csp-report")).permitAll());
		http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests.requestMatchers(("/ws/workflows/*/datas/csv"))
				.access(new WebExpressionAuthorizationManager("hasIpAddress('" + webSecurityProperties.getCsvAccessAuthorizeMask() + "')")));
		setIpsAutorizations(http, webSecurityProperties.getWsAccessAuthorizeIps(), "/ws/**");
		setIpsAutorizations(http, webSecurityProperties.getActuatorsAccessAuthorizeIps(), "/actuator/**");
		http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests
				.requestMatchers("/api-docs/**").hasAnyRole("ADMIN")
				.requestMatchers("/swagger-ui/**").hasAnyRole("ADMIN")
				.requestMatchers("/swagger-ui.html").hasAnyRole("ADMIN")
				.requestMatchers("/admin/**").hasAnyRole("ADMIN")
				.requestMatchers("/manager/**").hasAnyRole("MANAGER")
				.requestMatchers("/user/**").hasAnyRole("USER")
				.requestMatchers("/nexu-sign/**").hasAnyRole("USER", "OTP")
				.requestMatchers("/otp/**").hasAnyRole("OTP")
				.requestMatchers("/ws-secure/**").hasAnyRole("USER", "OTP")
                .requestMatchers("/log").authenticated()
				.anyRequest().permitAll())
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                (request, response, authException) ->
                                        response.sendError(HttpServletResponse.SC_FORBIDDEN),
                                PathPatternRequestMatcher.withDefaults().matcher("/log")
                        )
                );
	}

	private void setIpsAutorizations(HttpSecurity http, String[] authorizeIps, String path) throws Exception {
		StringBuilder hasIpAddresses = new StringBuilder();
		int nbIps = 0;
		if(authorizeIps != null && authorizeIps.length > 0) {
			for (String ip : authorizeIps) {
				nbIps++;
				hasIpAddresses.append("hasIpAddress('").append(ip).append("')");
				if(nbIps < authorizeIps.length) {
					hasIpAddresses.append(" or ");
				}
			}
			String finalHasIpAddresses = hasIpAddresses.toString();
			if(StringUtils.hasText(finalHasIpAddresses)) {
				http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests.requestMatchers(path)
						.access(new WebExpressionAuthorizationManager(finalHasIpAddresses)));
			} else {
				http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests.requestMatchers(path).denyAll());
			}
		} else {
			http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests.requestMatchers(path).denyAll());
		}
	}

	@Bean
	@ConditionalOnProperty(value="global.enable-su",havingValue = "true")
	public SwitchUserFilter switchUserFilter() {
		SwitchUserFilter switchUserFilter = new SwitchUserFilter();
		switchUserFilter.setUserDetailsService(new InMemoryUserDetailsManager());
		switchUserFilter.setSwitchUserUrl("/admin/su-login");
		switchUserFilter.setExitUserUrl("/su-logout");
		switchUserFilter.setSuccessHandler(new SuAuthenticationSuccessHandler());
		switchUserFilter.setFailureHandler(new ExceptionMappingAuthenticationFailureHandler());
		return switchUserFilter;
	}

	@Bean
	public AuthenticationManager authenticationManagerBean() {
		return new ProviderManager(List.of(new OtpAuthenticationProvider()));
	}

	@Bean
	public List<ExternalAuth> getExternalAuths(List<OidcOtpSecurityService> securityServices, SmsProperties smsProperties) {
		List<OidcOtpSecurityService> activeOidcSecurityServices = securityServices.stream().filter(this::hasClientRegistration).toList();
		List<ExternalAuth> externalAuths = new ArrayList<>(List.of(ExternalAuth.values()));
		if(globalProperties.getSmsRequired()) externalAuths.remove(ExternalAuth.open);
		if(activeOidcSecurityServices.stream().noneMatch(s -> s instanceof ProConnectSecurityServiceImpl)) externalAuths.remove(ExternalAuth.proconnect);
		if(activeOidcSecurityServices.stream().noneMatch(s -> s instanceof FranceConnectSecurityServiceImpl)) externalAuths.remove(ExternalAuth.franceconnect);
		if(BooleanUtils.isFalse(smsProperties.getEnableSms())) externalAuths.remove(ExternalAuth.sms);
		return externalAuths;
	}

}
