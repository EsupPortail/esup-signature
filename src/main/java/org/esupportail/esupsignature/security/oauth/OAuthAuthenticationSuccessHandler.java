package org.esupportail.esupsignature.security.oauth;

import java.io.IOException;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.esupportail.esupsignature.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.stereotype.Component;

@Component
public class OAuthAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	@Resource
	private UserService userService;

	private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
	
	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
		DefaultOAuth2User defaultOidcUser = ((DefaultOAuth2User)authentication.getPrincipal());
		String id = defaultOidcUser.getAttributes().get("sub").toString();
		String name = defaultOidcUser.getAttributes().get("family_name").toString();
		String prenom = defaultOidcUser.getAttributes().get("given_name").toString();
		String email = defaultOidcUser.getAttributes().get("email").toString();
		userService.createUser(id, name, prenom, email, false);
		DefaultSavedRequest defaultSavedRequest = (DefaultSavedRequest) request.getSession().getAttribute("SPRING_SECURITY_SAVED_REQUEST");
		String targetURL = defaultSavedRequest.getRedirectUrl();
        redirectStrategy.sendRedirect(request, response, targetURL);
	}
	
}