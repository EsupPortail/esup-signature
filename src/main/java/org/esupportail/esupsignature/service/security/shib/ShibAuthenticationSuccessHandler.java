package org.esupportail.esupsignature.service.security.shib;

import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

@Service
public class ShibAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

	private static final Logger logger = LoggerFactory.getLogger(ShibAuthenticationSuccessHandler.class);

	@Resource
	private UserService userService;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) {
		String eppn = authentication.getName();
        String email = httpServletRequest.getHeader("mail");
        String name = httpServletRequest.getHeader("sn");
		String firstname = httpServletRequest.getHeader("givenName");
		if(StringUtils.hasText(eppn) && StringUtils.hasText(email) && StringUtils.hasText(name) && StringUtils.hasText(firstname)) {
			name = new String(name.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
			firstname = new String(firstname.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
			userService.createUserWithAuthentication(eppn, name, firstname, email, authentication, UserType.shib);
		} else {
			logger.warn("eppn : " + eppn + ", mail : " + email + ", name : " + name + ", givenName : " + firstname);
			throw new EsupSignatureRuntimeException("At least one shib attribut is missing. Needed attributs are eppn, mail, sn and givenName");
		}
		httpServletRequest.getSession().setAttribute("securityServiceName", "ShibSecurityServiceImpl");
	}
	
}