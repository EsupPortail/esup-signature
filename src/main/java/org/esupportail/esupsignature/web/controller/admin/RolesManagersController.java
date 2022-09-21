package org.esupportail.esupsignature.web.controller.admin;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequestMapping("/admin/roles-managers")
@Controller
public class RolesManagersController {

    @ModelAttribute("adminMenu")
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
    public String getRoles(Model model) {
        Map<String, List<User>> roleManagers = new HashMap<>();
        List<User> users =  userService.getByManagersRolesUsers();
        for (String role : users.stream().flatMap(user -> user.getManagersRoles().stream().map(String::new)).collect(Collectors.toList())) {
            roleManagers.put(role, userService.getByManagersRoles(role));
        }
        model.addAttribute("roleManagers", roleManagers);
        model.addAttribute("roles", userService.getAllRoles());
        return "admin/roles-managers";
    }

    @PostMapping("/create-role")
    public String createRoles(@RequestParam String role, @RequestParam(required = false) List<String> rolesManagers) {
        if(rolesManagers != null) {
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
        return "redirect:/admin/roles-managers";
    }

    @PostMapping("/edit-role")
    public String editRoles(@RequestParam String role, @RequestParam(required = false) List<String> rolesManagers) {
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
        return "redirect:/admin/roles-managers";
    }
}
