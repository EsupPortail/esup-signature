package org.esupportail.esupsignature.service.security.cas;

import org.esupportail.esupsignature.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Service
public class CasAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	@Resource
	private UserService userService;

	private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
	
	@Override
	public void onAuthenticationSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) throws IOException {
        userService.createUserWithAuthentication(authentication);
		String targetURL = "/";
		DefaultSavedRequest defaultSavedRequest = (DefaultSavedRequest) httpServletRequest.getSession().getAttribute("SPRING_SECURITY_SAVED_REQUEST");
		if(defaultSavedRequest != null) {
			defaultSavedRequest.getRedirectUrl();
		}
		httpServletRequest.getSession().setAttribute("securityServiceName", "CasSecurityServiceImpl");
        redirectStrategy.sendRedirect(httpServletRequest, httpServletResponse, targetURL);
	}

}