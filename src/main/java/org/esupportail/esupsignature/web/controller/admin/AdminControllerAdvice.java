package org.esupportail.esupsignature.web.controller.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dss.service.OJService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final OJService ojService;

    @Resource
    private SessionRegistry sessionRegistry;

    public AdminControllerAdvice(OJService ojService) {
        this.ojService = ojService;
    }

    @ModelAttribute
    public void globalAttributes(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, Model model, HttpServletRequest httpServletRequest) throws JsonProcessingException {
        model.addAttribute("nbSessions", sessionRegistry.getAllPrincipals().size());
        try {
            model.addAttribute("dssStatus", ojService.checkOjFreshness());
        } catch (IOException e) {
            logger.debug("enable to get dss status");
        }
    }

}
