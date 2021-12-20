package org.esupportail.esupsignature.service.security.cas;

import org.esupportail.esupsignature.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Service
public class CasAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

	@Resource
	private UserService userService;

	private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

	@Override
	public void onAuthenticationSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) throws IOException, ServletException {
        userService.createUserWithAuthentication(authentication);
		httpServletRequest.getSession().setAttribute("securityServiceName", "CasSecurityServiceImpl");
		DefaultSavedRequest defaultSavedRequest = (DefaultSavedRequest) httpServletRequest.getSession().getAttribute("SPRING_SECURITY_SAVED_REQUEST");
		if(defaultSavedRequest != null) {
			String queryString = defaultSavedRequest.getQueryString();
			if (queryString != null && queryString.split("=")[0].equals("redirect")) {
				this.redirectStrategy.sendRedirect(httpServletRequest, httpServletResponse, queryString.split("=")[1]);
				return;
			}
		}
		this.redirectStrategy.sendRedirect(httpServletRequest, httpServletResponse, "/");
	}

}