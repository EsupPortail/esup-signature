package org.esupportail.esupsignature.web.controller.admin;

import org.esupportail.esupsignature.service.ChartsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/admin/charts")
@Controller
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class ChartsController {

    private final ChartsService chartsService;

    public ChartsController(ChartsService chartsService) {
        this.chartsService = chartsService;
    }

    @ModelAttribute("adminMenu")
    String getCurrentMenu() {
        return "active";
    }

    @ModelAttribute("activeMenu")
    public String getActiveMenu() {
        return "charts";
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("signsByYears", chartsService.getSignsByYears());
        return "admin/charts";
    }

}
