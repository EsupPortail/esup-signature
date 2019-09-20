package org.esupportail.esupsignature.service.security.shib;

import java.io.IOException;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.esupportail.esupsignature.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Service;

@Service
public class ShibAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	@Resource
	private UserService userService;

	//private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
	
	//pas de redirection ici !
	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
		String eppn = authentication.getName();
        String email = request.getHeader("mail");
        String name = request.getHeader("sn");
        String firstName = request.getHeader("givenName");
        userService.createUser(eppn, name, firstName, email, false);
        /*
		DefaultSavedRequest defaultSavedRequest = (DefaultSavedRequest) request.getSession().getAttribute("SPRING_SECURITY_SAVED_REQUEST");
		String targetURL = defaultSavedRequest.getRedirectUrl();
        redirectStrategy.sendRedirect(request, response, targetURL);
        */
	}
	
}