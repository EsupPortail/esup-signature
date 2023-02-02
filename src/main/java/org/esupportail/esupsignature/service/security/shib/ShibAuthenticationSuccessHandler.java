package org.esupportail.esupsignature.service.security.shib;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class ShibAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

	private static final Logger logger = LoggerFactory.getLogger(ShibAuthenticationSuccessHandler.class);

	@Resource
	private UserService userService;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) throws ServletException, IOException {
		String eppn = authentication.getName();
        String email = httpServletRequest.getHeader("mail");
        String name = httpServletRequest.getHeader("sn");
		String firstname = httpServletRequest.getHeader("givenName");
		if(eppn == null || email == null || name == null || firstname == null) {
        	throw new EsupSignatureException("At least one shib attribut is missing. Needed attributs are eppn, mail, sn and givenName");
		} else {
			name = new String(name.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
			firstname = new String(firstname.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
			User user = userService.getUserByEmail(email);
			if(user != null) {
				userService.updateUserInfos(user.getId(), eppn, name, firstname, UserType.shib);
			} else {
				userService.createUser(eppn, name, firstname, email, UserType.shib, true);
			}
		}
		httpServletRequest.getSession().setAttribute("securityServiceName", "ShibSecurityServiceImpl");
	}
	
}