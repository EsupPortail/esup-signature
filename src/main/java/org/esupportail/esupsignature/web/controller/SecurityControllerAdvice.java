package org.esupportail.esupsignature.web.controller;

import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.ldap.LdapPersonService;
import org.esupportail.esupsignature.service.ldap.PersonLdap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.List;

@ControllerAdvice(basePackages = {"org.esupportail.esupsignature.web.controller"})
public class SecurityControllerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(SecurityControllerAdvice.class);

    @Resource
    private UserService userService;

    @Autowired(required = false)
    private LdapPersonService ldapPersonService;

    @ModelAttribute(value = "userEppn")
    public String getUserEppn(HttpSession httpSession) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && !auth.getName().equals("anonymousUser")) {
            String eppn = auth.getName();
            if(ldapPersonService != null) {
                List<PersonLdap> personLdaps =  ldapPersonService.getPersonLdap(auth.getName());
                eppn = personLdaps.get(0).getEduPersonPrincipalName();
            }
            if (httpSession.getAttribute("suEppn") != null) {
                eppn = (String) httpSession.getAttribute("suEppn");
            }
            logger.debug("eppn used is : " + eppn);
            return userService.buildEppn(eppn);
        } else {
            return null;
        }
    }

    @ModelAttribute(value = "authUserEppn")
    public String getAuthUserEppn() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && !auth.getName().equals("anonymousUser")) {
            String eppn = auth.getName();
            if(ldapPersonService != null) {
                List<PersonLdap> personLdaps =  ldapPersonService.getPersonLdap(auth.getName());
                eppn = personLdaps.get(0).getEduPersonPrincipalName();
            }
            logger.debug("eppn used is : " + eppn);
            return userService.buildEppn(eppn);
        } else {
            return null;
        }
    }
}
