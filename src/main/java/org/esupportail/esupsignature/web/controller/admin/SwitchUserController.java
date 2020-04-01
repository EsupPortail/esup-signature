package org.esupportail.esupsignature.web.controller.admin;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;

@RequestMapping("/admin")
@Controller
public class SwitchUserController {

    @ModelAttribute("adminMenu")
    String getCurrentMenu() {
        return "active";
    }

    @Resource
    private UserService userService;

    @ModelAttribute("user")
    public User getUser() {
        return userService.getUserFromAuthentication();
    }

    @GetMapping("/su")
    public String switchUser(Model model) {

        return "admin/su";
    }

}
