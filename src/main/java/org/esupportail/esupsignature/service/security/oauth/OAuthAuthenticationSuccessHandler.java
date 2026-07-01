package org.esupportail.esupsignature.service.security.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.web.savedrequest.SimpleSavedRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OAuthAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

	private static final Logger logger = LoggerFactory.getLogger(OAuthAuthenticationSuccessHandler.class);

	private final UserService userService;
	private final OAuth2AuthorizedClientService authorizedClientService;

    public OAuthAuthenticationSuccessHandler(UserService userService, OAuth2AuthorizedClientService authorizedClientService) {
        this.userService = userService;
        this.authorizedClientService = authorizedClientService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        DefaultOAuth2User defaultOidcUser = (DefaultOAuth2User) authentication.getPrincipal();

        String id = defaultOidcUser.getAttributes().get("sub").toString();

        String registrationId = "";
        if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
            registrationId = oauth2Token.getAuthorizedClientRegistrationId();
        }

        String name, firstName;
        if ("azuread".equals(registrationId)) {
            String fullName = defaultOidcUser.getAttributes().getOrDefault("name", "Unknown").toString();
            String[] nameParts = fullName.split(" ", 2);
            if (nameParts.length >= 2) {
                firstName = nameParts[0];
                name = nameParts[1];
            } else {
                firstName = fullName;
                name = fullName;
            }
        } else {
            name = defaultOidcUser.getAttributes().containsKey("family_name")
                    ? defaultOidcUser.getAttributes().get("family_name").toString()
                    : defaultOidcUser.getAttributes().get("usual_name").toString();
            firstName = defaultOidcUser.getAttributes().get("given_name").toString();
        }

        String email = defaultOidcUser.getAttributes().get("email").toString();
        logSuspiciousUserFieldLength("sub", id);
        logSuspiciousUserFieldLength("name", name);
        logSuspiciousUserFieldLength("given_name", firstName);
        logSuspiciousUserFieldLength("email", email);

        try {
            if ("azuread".equals(registrationId)) {
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

                Object groupsObj = defaultOidcUser.getAttributes().get("groups");
                if (groupsObj instanceof List<?> groups) {
                    for (Object group : groups) {
                        String groupId = group.toString();
                        if ("298ce1d5-fa91-42b1-982b-7306ce762da4".equals(groupId)) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                        }
                    }
                }

                OAuth2AuthenticationToken tempAuth = new OAuth2AuthenticationToken(
                    defaultOidcUser, authorities, registrationId);
                SecurityContextHolder.getContext().setAuthentication(tempAuth);

                User user = userService.createUserWithAuthentication(email, name, firstName, email, tempAuth, UserType.azuread);

                Map<String, String> oidcAttrs = new HashMap<>();
                defaultOidcUser.getAttributes().forEach((key, val) -> {
                    if (val != null) {
                        if (val instanceof String || val instanceof Number || val instanceof Boolean) {
                            oidcAttrs.put(key, val.toString());
                        } else if (val instanceof List) {
                            String listStr = ((List<?>) val).stream()
                                .map(Object::toString)
                                .collect(Collectors.joining(","));
                            oidcAttrs.put(key, listStr);
                        }
                    }
                });

                try {
                    org.springframework.security.oauth2.client.OAuth2AuthorizedClient authorizedClient =
                        authorizedClientService.loadAuthorizedClient(registrationId, authentication.getName());
                    if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
                        String tokenValue = authorizedClient.getAccessToken().getTokenValue();
                        Map<String, String> graphAttrs = userService.fetchMicrosoftGraphProfile(tokenValue);
                        if (graphAttrs != null && !graphAttrs.isEmpty()) {
                            oidcAttrs.putAll(graphAttrs);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error calling Microsoft Graph: {}", e.getMessage());
                }

                userService.updateOidcAttributes(user.getEppn(), oidcAttrs);

                List<String> roleStrings = authorities.stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
                userService.updateRoles(email, roleStrings);

                request.getSession().setAttribute("securityServiceName", registrationId);
                request.getSession().setAttribute("SPRING_SECURITY_SAVED_REQUEST", new SimpleSavedRequest("/user"));
                super.onAuthenticationSuccess(request, response, tempAuth);

            } else {
                if (userService.checkMailDomain(email) != UserType.external) {
                    request.getSession().invalidate();
                    SecurityContextHolder.clearContext();
                    throw new EsupSignatureUserException("L'authentification via OTP (ProConnect ou autre) n'est pas supportée pour les utilisateurs internes.");
                }

                userService.createUser(id, name, firstName, email, UserType.external, true);
                Authentication newAuth;
                List<SimpleGrantedAuthority> simpleGrantedAuthorities = new ArrayList<>();
                simpleGrantedAuthorities.add(new SimpleGrantedAuthority("ROLE_OTP"));
                if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
                    request.getSession().setAttribute("securityServiceName", oauth2Token.getAuthorizedClientRegistrationId());
                    newAuth = new OAuth2AuthenticationToken(oauth2Token.getPrincipal(), simpleGrantedAuthorities, oauth2Token.getAuthorizedClientRegistrationId());
                } else {
                    request.getSession().setAttribute("securityServiceName", "sms");
                    newAuth = new UsernamePasswordAuthenticationToken(authentication.getPrincipal(), authentication.getCredentials(), simpleGrantedAuthorities);
                }

                SecurityContextHolder.getContext().setAuthentication(newAuth);
                Object targetUrlAttr = request.getSession().getAttribute("after_oauth_redirect");
                String targetUrl = targetUrlAttr != null ? targetUrlAttr.toString() : "/";
                if (targetUrl.isBlank()) targetUrl = "/";
                request.getSession().setAttribute("SPRING_SECURITY_SAVED_REQUEST", new SimpleSavedRequest(targetUrl));
                super.onAuthenticationSuccess(request, response, authentication);
            }

        } catch (EsupSignatureUserException e) {
            request.getSession().setAttribute("errorMsg", e.getMessage());
            response.sendRedirect("/");
        }
    }

	private void logSuspiciousUserFieldLength(String field, String value) {
		if (value != null && value.length() > 255) {
			logger.warn("Attribut OIDC trop long pour User [{}] length={}", field, value.length());
		}
	}

}