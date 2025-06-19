package org.esupportail.esupsignature.service.security.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
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

	private final UserService userService;

    public OAuthAuthenticationSuccessHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
	public void onAuthenticationSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) throws IOException, ServletException {
		DefaultOAuth2User defaultOidcUser = ((DefaultOAuth2User) authentication.getPrincipal());
		String id = defaultOidcUser.getAttributes().get("sub").toString();
		String name = defaultOidcUser.getAttributes().containsKey("family_name")
				? defaultOidcUser.getAttributes().get("family_name").toString()
				: defaultOidcUser.getAttributes().get("usual_name").toString();
		String firstName = defaultOidcUser.getAttributes().get("given_name").toString();
		String email = defaultOidcUser.getAttributes().get("email").toString();
		try {
			if(userService.checkMailDomain(email) != UserType.external) {
				httpServletRequest.getSession().invalidate();
				SecurityContextHolder.clearContext();
				throw new EsupSignatureUserException("L'authentification via OTP (ProConnect ou autre) n'est pas supportée pour les utilisateurs internes.");
			}
			userService.createUser(id, name, firstName, email, UserType.external, true);
			Authentication newAuth;
			List<SimpleGrantedAuthority> simpleGrantedAuthorities = new ArrayList<>();
			simpleGrantedAuthorities.add(new SimpleGrantedAuthority("ROLE_OTP"));
			if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
				String registrationId = oauth2Token.getAuthorizedClientRegistrationId();
				httpServletRequest.getSession().setAttribute("securityServiceName", registrationId);
				newAuth = new OAuth2AuthenticationToken(oauth2Token.getPrincipal(), simpleGrantedAuthorities, oauth2Token.getAuthorizedClientRegistrationId());
			} else {
				httpServletRequest.getSession().setAttribute("securityServiceName", "sms");
				newAuth = new UsernamePasswordAuthenticationToken(authentication.getPrincipal(), authentication.getCredentials(), simpleGrantedAuthorities);
			}

			SecurityContextHolder.getContext().setAuthentication(newAuth);
			String targetUrl = httpServletRequest.getSession().getAttribute("after_oauth_redirect").toString();
			if (targetUrl == null || targetUrl.isBlank()) {
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
	
}