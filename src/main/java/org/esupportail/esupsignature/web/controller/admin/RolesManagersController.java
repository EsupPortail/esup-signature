package org.esupportail.esupsignature.web.controller.admin;

import jakarta.annotation.Resource;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequestMapping({"/manager/roles-managers", "/admin/roles-managers"})
@Controller
public class RolesManagersController {

    @ModelAttribute("activeMenu")
    public String getActiveMenu() {
        return "rolesManagers";
    }

    @Resource
    private UserService userService;

    @GetMapping
    public String getRoles(@ModelAttribute("authUserEppn") String authUserEppn, Model model) {
        Map<String, List<User>> roleManagers = new HashMap<>();
        User authUser = userService.getByEppn(authUserEppn);
        if(userService.getRoles(authUserEppn).contains("ROLE_ADMIN")) {
            List<User> users = userService.getByManagersRolesUsers();
            for (String role : users.stream().flatMap(user -> user.getManagersRoles().stream().map(String::new)).toList()) {
                roleManagers.put(role, userService.getByManagersRoles(role));
            }
            model.addAttribute("roles", userService.getAllRoles());
        } else {
            Set<String> allRoles =  authUser.getManagersRoles();
            for (String role : allRoles) {
                roleManagers.put(role, userService.getByManagersRoles(role));
            }
        }
        model.addAttribute("roleManagers", roleManagers);
        return "admin/roles-managers";
    }

    @PostMapping("/create-role")
    public String createRoles(@RequestParam String role, @RequestParam(required = false) List<String> rolesManagers) {
        userService.createRole(role, rolesManagers);
        return "redirect:/admin/roles-managers";
    }

    @PostMapping("/edit-role")
    public String editRoles(@RequestParam String role, @RequestParam(required = false) List<String> rolesManagers) {
        userService.updateRole(role, rolesManagers);
        return "redirect:/admin/roles-managers";
    }


}
