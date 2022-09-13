package org.esupportail.esupsignature.web.controller.admin;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.repository.UserRepository;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;

@RequestMapping("/admin/users")
@Controller
public class UserAdminController {

    @ModelAttribute("adminMenu")
    String getCurrentMenu() {
        return "active";
    }

    @ModelAttribute("activeMenu")
    public String getActiveMenu() {
        return "users";
    }

    @Resource
    private UserRepository userRepository;

    @Resource
    private UserService userService;

    @GetMapping
    public String list(@SortDefault(value = "name", sort = "ASC") @PageableDefault(size = 10) Pageable pageable, Model model) {
        Page<User> users = userRepository.findAll(pageable);
        model.addAttribute("users", users);
        return "admin/users/list";
    }

    @PostMapping("/anonymize/{id}")
    public String anonymize(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.anonymize(id);
        redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Opération effectuée"));
        return "redirect:/admin/users";
    }

}
