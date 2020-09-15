package org.esupportail.esupsignature.web.controller.admin;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.UserService;
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

    @ModelAttribute("activeMenu")
    public String getActiveMenu() {
        return "su";
    }

    @GetMapping("/su")
    public String switchUser() {
        return "admin/su";
    }

}
