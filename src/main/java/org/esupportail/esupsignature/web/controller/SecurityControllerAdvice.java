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

@ControllerAdvice(basePackages = {"org.esupportail.esupsignature.web.controller", "org.esupportail.esupsignature.web.wssecure"})
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
            String eppn = tryGetEppnFromLdap(auth);
            if (httpSession.getAttribute("suEppn") != null) {
                eppn = (String) httpSession.getAttribute("suEppn");
            }
            logger.debug("userEppn used is : " + eppn);
            return eppn;
        } else {
            return null;
        }
    }

    @ModelAttribute(value = "authUserEppn")
    public String getAuthUserEppn() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && !auth.getName().equals("anonymousUser")) {
            String eppn = tryGetEppnFromLdap(auth);
            logger.debug("authUserEppn used is : " + eppn);
            return eppn;
        } else {
            return null;
        }
    }

    public String tryGetEppnFromLdap(Authentication auth) {
        String eppn = auth.getName();
        if(ldapPersonService != null) {
            List<PersonLdap> personLdaps =  ldapPersonService.getPersonLdap(auth.getName());
            if(personLdaps.size() > 0) {
                eppn = personLdaps.get(0).getEduPersonPrincipalName();
                if (eppn == null) {
                    eppn = userService.buildEppn(auth.getName());
                }
            }
        }
        return eppn;
    }


}
