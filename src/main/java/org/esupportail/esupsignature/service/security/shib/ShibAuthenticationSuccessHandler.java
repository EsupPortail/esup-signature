package org.esupportail.esupsignature.service.security.shib;

import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

@Service
public class ShibAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

	private static final Logger logger = LoggerFactory.getLogger(ShibAuthenticationSuccessHandler.class);

	@Resource
	private UserService userService;

	@Resource
	private RegisterSessionAuthenticationStrategy registerSessionAuthenticationStrategy;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) {
		String eppn = authentication.getName();
        String email = httpServletRequest.getHeader("mail");
		if(email == null) {
			throw new EsupSignatureRuntimeException("user must have an email");
		}        String name = httpServletRequest.getHeader("sn");
		String firstname = httpServletRequest.getHeader("givenName");
		logger.info("shib auth with - eppn : " + eppn + ", mail : " + email + ", name : " + name + ", givenName : " + firstname);
		if(StringUtils.hasText(eppn) && StringUtils.hasText(email) && StringUtils.hasText(name) && StringUtils.hasText(firstname)) {
			name = new String(name.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
			firstname = new String(firstname.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
			userService.createUserWithAuthentication(eppn, name, firstname, email, authentication, UserType.shib);
		} else {
			throw new EsupSignatureRuntimeException("At least one shib attribut is missing. Needed attributs are eppn, mail, sn and givenName");
		}
		registerSessionAuthenticationStrategy.onAuthentication(authentication, httpServletRequest, httpServletResponse);
		httpServletRequest.getSession().setAttribute("securityServiceName", "ShibSecurityServiceImpl");
	}
	
}