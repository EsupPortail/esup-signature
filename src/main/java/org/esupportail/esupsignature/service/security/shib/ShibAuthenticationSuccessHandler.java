package org.esupportail.esupsignature.service.security.shib;

import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.ldap.PersonLdap;
import org.esupportail.esupsignature.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Enumeration;

@Service
public class ShibAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	private static final Logger logger = LoggerFactory.getLogger(ShibAuthenticationSuccessHandler.class);

	@Resource
	private UserService userService;

	//private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
	
	//pas de redirection ici !
	@Override
	public void onAuthenticationSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) {
		String eppn = authentication.getName();
        String email = httpServletRequest.getHeader("mail");
        String name = httpServletRequest.getHeader("sn");
        String firstName = httpServletRequest.getHeader("givenName");
		Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
        while(headerNames.hasMoreElements()){
        	String headerName = headerNames.nextElement();
			try {
				Field personLdapField = PersonLdap.class.getDeclaredField(headerName);
				httpServletRequest.getSession().setAttribute(personLdapField.getName(), httpServletRequest.getHeader(personLdapField.getName()));
			} catch (NoSuchFieldException e) {
				logger.debug("skip " + headerName);
			}
		}
        if(eppn == null || email == null || name == null || firstName == null) {
        	throw new EsupSignatureRuntimeException("At least one shib attribut is missing. Needed attributs are eppn, mail, sn and givenName");
		} else {
			userService.createUser(eppn, name, firstName, email);
		}
		httpServletRequest.getSession().setAttribute("securityServiceName", "ShibSecurityServiceImpl");
        /*
		DefaultSavedRequest defaultSavedRequest = (DefaultSavedRequest) request.getSession().getAttribute("SPRING_SECURITY_SAVED_REQUEST");
		String targetURL = defaultSavedRequest.getRedirectUrl();
        redirectStrategy.sendRedirect(request, response, targetURL);
        */
	}
	
}