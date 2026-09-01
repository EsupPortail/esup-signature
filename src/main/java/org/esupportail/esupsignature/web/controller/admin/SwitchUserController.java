package org.esupportail.esupsignature.web.controller.admin;

import jakarta.servlet.http.HttpSession;
import org.esupportail.esupsignature.dto.ui.global.UiMessageDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/admin")
@Controller
@PreAuthorize("hasRole('ROLE_ADMIN')")
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
    public String switchUser(HttpSession session, Model model) {
        Object errorMessage = session.getAttribute("suErrorMsg");
        if (errorMessage != null) {
            model.addAttribute("message", new UiMessageDto("error", errorMessage.toString()));
            session.removeAttribute("suErrorMsg");
        }
        return "admin/su";
    }

}
