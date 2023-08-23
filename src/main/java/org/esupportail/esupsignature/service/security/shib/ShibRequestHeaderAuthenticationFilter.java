package org.esupportail.esupsignature.service.security.shib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;

import jakarta.servlet.http.HttpServletRequest;

public class ShibRequestHeaderAuthenticationFilter extends RequestHeaderAuthenticationFilter {
	
	private static final Logger logger = LoggerFactory.getLogger(ShibRequestHeaderAuthenticationFilter.class);

	private String credentialsRequestHeader4thisClass;

/*	
	@Override
	protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, Authentication authResult) throws IOException, ServletException {
        super.successfulAuthentication(request, response, authResult);
        String eppn = authResult.getName();
        log.info("UserUi " + eppn + " authenticated");
        String email = request.getHeader("mail");
        String name = request.getHeader("sn");
        String firstName = request.getHeader("givenName");
        userService.createUser(eppn, name, firstName, email);
        log.info("UserUi " + eppn + " created");
    }
	*/
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
