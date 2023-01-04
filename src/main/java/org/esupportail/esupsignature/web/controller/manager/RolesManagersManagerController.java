package org.esupportail.esupsignature.web.controller.manager;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequestMapping("/manager/roles-managers")
@Controller
public class RolesManagersManagerController {

    @ModelAttribute("managerMenu")
    String getCurrentMenu() {
        return "active";
    }

    @ModelAttribute("activeMenu")
    public String getActiveMenu() {
        return "rolesManagers";
    }

    @Resource
    UserService userService;

    @GetMapping
    public String getRoles(@ModelAttribute("authUserEppn") String authUserEppn, Model model) {
        Map<String, List<User>> roleManagers = new HashMap<>();
        User manager = userService.getByEppn(authUserEppn);
        Set<String> allRoles =  manager.getManagersRoles();
        for (String role : allRoles) {
            roleManagers.put(role, userService.getByManagersRoles(role));
        }
        model.addAttribute("roleManagers", roleManagers);
        return "managers/roles-managers";
    }

    @PostMapping("/edit-role")
    @PreAuthorize("@preAuthorizeService.roleManager(#role, #authUserEppn)")
    public String editRoles(@ModelAttribute("authUserEppn") String authUserEppn, @RequestParam String role, @RequestParam(required = false) List<String> rolesManagers) {
        if(rolesManagers != null) {
            for (User user : userService.getByManagersRoles(role)) {
                if (!rolesManagers.contains(user.getEmail())) {
                    user.getManagersRoles().remove(role);
                }
            }
            for (String mail : rolesManagers) {
                User user = userService.getUserByEmail(mail);
                if (!user.getManagersRoles().contains(role)) {
                    user.getManagersRoles().add(role);
                }
            }
        } else {
            for (User user : userService.getByManagersRoles(role)) {
                user.getManagersRoles().remove(role);
            }
        }
        return "redirect:/manager/roles-managers";
    }
}
