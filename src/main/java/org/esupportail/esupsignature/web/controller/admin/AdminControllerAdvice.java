package org.esupportail.esupsignature.web.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import org.esupportail.esupsignature.dto.view.ui.AdminUiStatusDto;
import org.esupportail.esupsignature.service.view.ui.AdminUiFetchService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = {"org.esupportail.esupsignature.web.controller.admin"})
public class AdminControllerAdvice {

    private final AdminUiFetchService adminUiFetchService;

    public AdminControllerAdvice(AdminUiFetchService adminUiFetchService) {
        this.adminUiFetchService = adminUiFetchService;
    }

    @ModelAttribute
    public void globalAttributes(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, Model model, HttpServletRequest httpServletRequest) {
        String path = httpServletRequest.getRequestURI();
        if (path.startsWith("/admin")) {
            model.addAttribute("adminMenu", "active");
        } else {
            model.addAttribute("managerMenu", "active");
        }

        AdminUiStatusDto adminUiStatus = adminUiFetchService.buildAdminUiStatus();
        model.addAttribute("nbSessions", adminUiStatus.nbSessions());
        model.addAttribute("dssStatus", adminUiStatus.dssStatus());
    }

}
