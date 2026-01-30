package org.esupportail.esupsignature.web.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import org.esupportail.esupsignature.dss.service.DSSService;
import org.esupportail.esupsignature.repository.custom.SessionRepositoryCustom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.io.IOException;

@ControllerAdvice(basePackages = {"org.esupportail.esupsignature.web.controller.admin"})
public class AdminControllerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(AdminControllerAdvice.class);

    private final DSSService dssService;

    private final SessionRepositoryCustom sessionRepositoryCustom;

    public AdminControllerAdvice(@Autowired(required = false) DSSService dssService, SessionRepositoryCustom sessionRepositoryCustom) {
        this.dssService = dssService;
        this.sessionRepositoryCustom = sessionRepositoryCustom;
    }

    @ModelAttribute
    public void globalAttributes(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, Model model, HttpServletRequest httpServletRequest) {
        String path = httpServletRequest.getRequestURI();
        if (path.startsWith("/admin")) {
            model.addAttribute("adminMenu", "active");
        } else {
            model.addAttribute("managerMenu", "active");
        }

        model.addAttribute("nbSessions", sessionRepositoryCustom.findAllSessionIds().size());
        try {
            if(dssService != null) {
                model.addAttribute("dssStatus", dssService.refreshIsNeeded());
            }
        } catch (IOException e) {
            logger.debug("enable to get dss status");
        }
    }

}
