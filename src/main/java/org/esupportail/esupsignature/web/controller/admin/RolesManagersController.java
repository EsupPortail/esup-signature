package org.esupportail.esupsignature.web.controller.admin;

import jakarta.annotation.Resource;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequestMapping({"/manager/roles-managers", "/admin/roles-managers"})
@Controller
public class RolesManagersController {

    @ModelAttribute("activeMenu")
    public String getActiveMenu() {
        return "rolesManagers";
    }

    @Resource
    private UserService userService;

    @ModelAttribute("urlPrefix")
    public String getUrlPrefix(@ModelAttribute("authUserEppn") String authUserEppn) {
        if (userService.getRoles(authUserEppn).contains("ROLE_ADMIN")) {
            return "/admin";
        }
        return "/manager";
    }

    @GetMapping
    public String getRoles(@ModelAttribute("authUserEppn") String authUserEppn, Model model) {
        Map<String, List<User>> roleManagers;
        if(userService.getRoles(authUserEppn).contains("ROLE_ADMIN")) {
            roleManagers = userService.getRoleManagersMap();
            model.addAttribute("roles", userService.getAllRoles());
        } else {
            roleManagers = userService.getRoleManagersMap(userService.getManagersRoles(authUserEppn));
        }
        model.addAttribute("roleManagers", roleManagers);
        return "admin/roles-managers";
    }

    @PostMapping("/create-role")
    public String createRoles(@ModelAttribute("urlPrefix") String urlPrefix, @RequestParam String role, @RequestParam(required = false) List<String> rolesManagers) {
        userService.createRole(role, rolesManagers);
        return "redirect:" + urlPrefix + "/roles-managers";
    }

    @PostMapping("/edit-role")
    public String editRoles(@ModelAttribute("urlPrefix") String urlPrefix, @RequestParam String role, @RequestParam(required = false) List<String> rolesManagers) {
        userService.updateRole(role, rolesManagers);
        return "redirect:" + urlPrefix + "/roles-managers";
    }


}
