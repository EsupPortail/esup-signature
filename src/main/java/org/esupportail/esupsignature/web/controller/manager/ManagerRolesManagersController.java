package org.esupportail.esupsignature.web.controller.manager;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestMapping("/manager/rolesManagers")
@Controller
public class ManagerRolesManagersController {

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
        List<String> allRoles =  manager.getManagersRoles();
        for (String role : allRoles) {
            roleManagers.put(role, userService.getByManagersRoles(role));
        }
        model.addAttribute("roleManagers", roleManagers);
        return "managers/managerRoles";
    }

    @PostMapping("/editRole")
    @PreAuthorize("@preAuthorizeService.roleManager(#role, #authUserEppn)")
    public String editRoles(@RequestParam String role, @RequestParam List<String> rolesManagers, @ModelAttribute("authUserEppn") String authUserEppn) {
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

        return "redirect:/manager/rolesManagers";

    }
}
