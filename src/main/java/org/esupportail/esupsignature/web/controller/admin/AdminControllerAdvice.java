package org.esupportail.esupsignature.web.controller.admin;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dss.service.DSSService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.io.IOException;

@ControllerAdvice(basePackages = {"org.esupportail.esupsignature.web.controller.admin"})
@EnableConfigurationProperties(GlobalProperties.class)
public class AdminControllerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(AdminControllerAdvice.class);

    private final DSSService dssService;

    private final SessionRegistry sessionRegistry;

    public AdminControllerAdvice(@Autowired(required = false) DSSService dssService, SessionRegistry sessionRegistry) {
        this.dssService = dssService;
        this.sessionRegistry = sessionRegistry;
    }

    @ModelAttribute
    public void globalAttributes(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, Model model) {
        model.addAttribute("nbSessions", sessionRegistry.getAllPrincipals().size());
        try {
            if(dssService != null) {
                model.addAttribute("dssStatus", dssService.refreshIsNeeded());
            }
        } catch (IOException e) {
            logger.debug("enable to get dss status");
        }
    }

}
