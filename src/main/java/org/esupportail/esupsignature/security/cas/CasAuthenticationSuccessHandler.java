package org.esupportail.esupsignature.security.cas;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.ldap.PersonLdap;
import org.esupportail.esupsignature.ldap.PersonLdapDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class CasAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	@Autowired
	PersonLdapDao personDao;
	
	private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
	
	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		List<PersonLdap> persons =  personDao.getPersonNamesByUid(authentication.getName());
		String eppn = persons.get(0).getEduPersonPrincipalName();
		User user;
		if(User.countFindUsersByEppnEquals(eppn) > 0) {
    		user = User.findUsersByEppnEquals(eppn).getSingleResult();
    	} else {
	    	user = new User();
    	}
		user.setName(persons.get(0).getSn());
		user.setFirstname(persons.get(0).getGivenName());
		user.setEppn(persons.get(0).getEduPersonPrincipalName());
		user.setEmail(persons.get(0).getMail());
		if(user.getId() != null) {
			user.merge();
		} else {
			user.persist();
		}
		redirectStrategy.sendRedirect(request, response, "/");
	}

}