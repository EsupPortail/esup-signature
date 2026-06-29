package org.esupportail.esupsignature.service.security.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.security.OidcOtpSecurityService;
import org.esupportail.esupsignature.service.security.OidcUserSecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.web.savedrequest.SimpleSavedRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OAuthAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

	private static final Logger logger = LoggerFactory.getLogger(OAuthAuthenticationSuccessHandler.class);

	private final UserService userService;
	private final List<OidcOtpSecurityService> oidcOtpSecurityServices;
	private final OidcUserSecurityServiceResolver oidcUserSecurityServiceResolver;
	private final OidcUserAuthoritiesService oidcUserAuthoritiesService;

    public OAuthAuthenticationSuccessHandler(UserService userService,
                                             List<OidcOtpSecurityService> oidcOtpSecurityServices,
                                             OidcUserSecurityServiceResolver oidcUserSecurityServiceResolver,
                                             OidcUserAuthoritiesService oidcUserAuthoritiesService) {
        this.userService = userService;
        this.oidcOtpSecurityServices = oidcOtpSecurityServices;
        this.oidcUserSecurityServiceResolver = oidcUserSecurityServiceResolver;
        this.oidcUserAuthoritiesService = oidcUserAuthoritiesService;
    }

    @Override
	public void onAuthenticationSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) throws IOException, ServletException {
		DefaultOAuth2User defaultOidcUser = ((DefaultOAuth2User) authentication.getPrincipal());
		String registrationId = authentication instanceof OAuth2AuthenticationToken oauth2Token ? oauth2Token.getAuthorizedClientRegistrationId() : "";
		try {
			if(isOidcOtpRegistration(registrationId)) {
				handleOtpAuthentication(httpServletRequest, authentication, defaultOidcUser, registrationId);
			} else {
				Optional<OidcUserSecurityService> oidcUserSecurityService = oidcUserSecurityServiceResolver.resolve(registrationId);
				if(oidcUserSecurityService.isPresent()) {
					handleUserAuthentication(httpServletRequest, authentication, defaultOidcUser, registrationId, oidcUserSecurityService.get());
				}
			}
			Object targetUrlAttr = httpServletRequest.getSession().getAttribute("after_oauth_redirect");
			String targetUrl = targetUrlAttr != null ? targetUrlAttr.toString() : "/";
			if (targetUrl.isBlank()) {
				targetUrl = "/";
			}
			SavedRequest savedRequest = new SimpleSavedRequest(targetUrl);
			httpServletRequest.getSession().setAttribute("SPRING_SECURITY_SAVED_REQUEST", savedRequest);
			super.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);
		} catch (EsupSignatureUserException e) {
			httpServletRequest.getSession().setAttribute("errorMsg", e.getMessage());
			httpServletResponse.sendRedirect("/");
		}

	}

	private void handleOtpAuthentication(HttpServletRequest httpServletRequest, Authentication authentication, DefaultOAuth2User defaultOidcUser, String registrationId) {
		String id = getRequiredClaim(defaultOidcUser.getAttributes(), "sub");
		String name = defaultOidcUser.getAttributes().containsKey("family_name")
				? getRequiredClaim(defaultOidcUser.getAttributes(), "family_name")
				: getRequiredClaim(defaultOidcUser.getAttributes(), "usual_name");
		String firstName = getRequiredClaim(defaultOidcUser.getAttributes(), "given_name");
		String email = getRequiredClaim(defaultOidcUser.getAttributes(), "email");
		logSuspiciousUserFieldLength("sub", id);
		logSuspiciousUserFieldLength("family_name", name);
		logSuspiciousUserFieldLength("given_name", firstName);
		logSuspiciousUserFieldLength("email", email);
		if(userService.checkMailDomain(email) != UserType.external) {
			httpServletRequest.getSession().invalidate();
			SecurityContextHolder.clearContext();
			throw new EsupSignatureUserException("L'authentification via OTP (ProConnect ou autre) n'est pas supportée pour les utilisateurs internes.");
		}
		List<SimpleGrantedAuthority> simpleGrantedAuthorities = new ArrayList<>();
		simpleGrantedAuthorities.add(new SimpleGrantedAuthority("ROLE_OTP"));
		Authentication newAuth = buildAuthentication(authentication, simpleGrantedAuthorities, registrationId);
		SecurityContextHolder.getContext().setAuthentication(newAuth);
		httpServletRequest.getSession().setAttribute("securityServiceName", registrationId);
		userService.createUser(id, name, firstName, email, UserType.external, true);
	}

	private void handleUserAuthentication(HttpServletRequest httpServletRequest, Authentication authentication, DefaultOAuth2User defaultOidcUser, String registrationId, OidcUserSecurityService oidcUserSecurityService) {
		String id = getRequiredClaim(defaultOidcUser.getAttributes(), oidcUserSecurityService.getPrincipalClaim());
		String name = getRequiredClaim(defaultOidcUser.getAttributes(), oidcUserSecurityService.getLastnameClaim());
		String firstName = getRequiredClaim(defaultOidcUser.getAttributes(), oidcUserSecurityService.getFirstnameClaim());
		String email = getRequiredClaim(defaultOidcUser.getAttributes(), oidcUserSecurityService.getEmailClaim());
		logSuspiciousUserFieldLength(oidcUserSecurityService.getPrincipalClaim(), id);
		logSuspiciousUserFieldLength(oidcUserSecurityService.getLastnameClaim(), name);
		logSuspiciousUserFieldLength(oidcUserSecurityService.getFirstnameClaim(), firstName);
		logSuspiciousUserFieldLength(oidcUserSecurityService.getEmailClaim(), email);
		Collection<GrantedAuthority> authorities = oidcUserAuthoritiesService.buildAuthorities(defaultOidcUser.getAttributes(), oidcUserSecurityService);
		Authentication newAuth = buildAuthentication(authentication, authorities, registrationId);
		SecurityContextHolder.getContext().setAuthentication(newAuth);
		httpServletRequest.getSession().setAttribute("securityServiceName", registrationId);
		userService.createUser(id, name, firstName, email, oidcUserSecurityService.getUserType(), true);
	}

	private Authentication buildAuthentication(Authentication authentication, Collection<? extends GrantedAuthority> authorities, String registrationId) {
		if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
			return new OAuth2AuthenticationToken(oauth2Token.getPrincipal(), authorities, registrationId);
		}
		return new UsernamePasswordAuthenticationToken(authentication.getPrincipal(), authentication.getCredentials(), authorities);
	}

	private boolean isOidcOtpRegistration(String registrationId) {
		return oidcOtpSecurityServices.stream().anyMatch(service -> service.getCode().equals(registrationId));
	}

	private String getRequiredClaim(Map<String, Object> claims, String claimName) {
		Object value = claims.get(claimName);
		if(value == null || value.toString().isBlank()) {
			throw new EsupSignatureUserException("Attribut OIDC manquant : " + claimName);
		}
		return value.toString();
	}

	private void logSuspiciousUserFieldLength(String field, String value) {
		if (value != null && value.length() > 255) {
			logger.warn("Attribut OIDC trop long pour User [{}] length={}", field, value.length());
		}
	}

}
