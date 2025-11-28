package org.esupportail.esupsignature.web.controller.admin;

import org.esupportail.esupsignature.service.ConfigService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/admin/configs")
@Controller
public class ConfigAdminController {

    public ConfigAdminController(ConfigService configService) {
        this.configService = configService;
    }

    @ModelAttribute("adminMenu")
    String getCurrentMenu() {
        return "active";
    }

    @ModelAttribute("activeMenu")
    public String getActiveMenu() {
        return "configs";
    }

    private final ConfigService configService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("config", configService.getConfig());
        return "admin/configs/list";
    }

    @PostMapping("/add-mapping-filter-group")
    public String addMappingFiltgerGroup(@RequestParam String group, @RequestParam String filter) {
        configService.addMappingFiltersGroups(group, filter);
        return "redirect:/admin/configs";
    }
}
