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

    @PostMapping("/mapping-filter-group")
    public String addMappingFilterGroup(@RequestParam String group, @RequestParam String filter) {
        configService.addMappingFiltersGroups(group, filter);
        return "redirect:/admin/configs";
    }

    @DeleteMapping("/mapping-filter-group")
    public String deleteMappingFilterGroup(@RequestParam String group) {
        configService.deleteMappingFiltersGroups(group);
        return "redirect:/admin/configs";
    }

    @PostMapping("/mapping-group-role")
    public String addMappingGroupRole(@RequestParam String group, @RequestParam String role) {
        configService.addMappingGroupsRoles(group, role);
        return "redirect:/admin/configs";
    }

    @DeleteMapping("/mapping-group-role")
    public String deleteMappingGroupRole(@RequestParam String group) {
        configService.deleteMappingGroupsRoles(group);
        return "redirect:/admin/configs";
    }

    @PostMapping("/group-mapping-spel")
    public String addGroupMappingSpel(@RequestParam String group, @RequestParam String spel) {
        configService.addGroupMappingSpel(group, spel);
        return "redirect:/admin/configs";
    }

    @DeleteMapping("/group-mapping-spel")
    public String deleteGroupMappingSpel(@RequestParam String group) {
        configService.deleteGroupMappingSpel(group);
        return "redirect:/admin/configs";
    }

    @PostMapping("/update-hide-auto-sign")
    public String updateHideAutoSign(@RequestParam(required = false) Boolean hideAutoSign) {
        configService.updateHideAutoSign(hideAutoSign);
        return "redirect:/admin/configs";
    }
}
