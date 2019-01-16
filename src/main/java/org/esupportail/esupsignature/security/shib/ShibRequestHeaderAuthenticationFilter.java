package org.esupportail.esupsignature.security.shib;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.esupportail.esupsignature.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class ShibRequestHeaderAuthenticationFilter extends RequestHeaderAuthenticationFilter {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private String credentialsRequestHeader4thisClass;
	
	@Override
	protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, Authentication authResult) throws IOException, ServletException {
        super.successfulAuthentication(request, response, authResult);
        String eppn = authResult.getName();
        String email = request.getHeader("mail");
        String name = request.getHeader("sn");
        String firstName = request.getHeader("givenName");
        User user;
        if(User.countFindUsersByEppnEquals(eppn) > 0) {
        	user = User.findUsersByEppnEquals(eppn).getSingleResult();
        } else {
        	user = new User();
        }
      	user.setEppn(eppn);
       	user.setName(name);
       	user.setFirstname(firstName);
       	user.setEmail(email);
       	user.persist();
       
        log.info("User " + eppn + " authenticated");
    }
	
	/* 
	 * Surcharge de la méthode initiale : si pas d'attributs correspondant à credentialsRequestHeader (shib) ; on continue  :
	 * 	credentials ldap suffisent (et pas de credentials du tout aussi ...). 
	 * 
	 * @see org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter#getPreAuthenticatedCredentials(javax.servlet.http.HttpServletRequest)
	 */
	@Override
    protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
		String credentials = null;
        if (credentialsRequestHeader4thisClass != null) {
        	credentials = request.getHeader(credentialsRequestHeader4thisClass);
        }
        if(credentials == null) {
        	return "N/A"; 
        } else {
        	return credentials;
        }
    }
	
    public void setCredentialsRequestHeader(String credentialsRequestHeader) {
        super.setCredentialsRequestHeader(credentialsRequestHeader);
        this.credentialsRequestHeader4thisClass = credentialsRequestHeader;
    }

}
