package org.esupportail.esupsignature.service.security.oauth;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.service.UserService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
import java.util.List;

@Service
public class OAuthAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

	@Resource
	private UserService userService;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) throws IOException, ServletException {
		DefaultOAuth2User defaultOidcUser = ((DefaultOAuth2User) authentication.getPrincipal());
		String id = defaultOidcUser.getAttributes().get("sub").toString();
		String name = defaultOidcUser.getAttributes().containsKey("family_name")
				? defaultOidcUser.getAttributes().get("family_name").toString()
				: defaultOidcUser.getAttributes().get("usual_name").toString();
		String firstName = defaultOidcUser.getAttributes().get("given_name").toString();
		String email = defaultOidcUser.getAttributes().get("email").toString();
		userService.createUser(id, name, firstName, email, UserType.external, true);
		if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
			String registrationId = oauth2Token.getAuthorizedClientRegistrationId();
			httpServletRequest.getSession().setAttribute("securityServiceName", registrationId);
		}
		List<SimpleGrantedAuthority> simpleGrantedAuthorities = new ArrayList<>();
		simpleGrantedAuthorities.add(new SimpleGrantedAuthority("ROLE_OTP"));
		Authentication newAuth = new UsernamePasswordAuthenticationToken(authentication.getPrincipal(), authentication.getCredentials(), simpleGrantedAuthorities);
		SecurityContextHolder.getContext().setAuthentication(newAuth);
		String targetUrl = httpServletRequest.getSession().getAttribute("after_oauth_redirect").toString();
		if (targetUrl == null || targetUrl.isBlank()) {
			targetUrl = "/";
		}
		SavedRequest savedRequest = new SimpleSavedRequest(targetUrl);
		httpServletRequest.getSession().setAttribute("SPRING_SECURITY_SAVED_REQUEST", savedRequest);
		super.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);
	}
	
}