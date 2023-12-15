package org.esupportail.esupsignature.web.controller.admin;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.UserRepository;
import org.esupportail.esupsignature.service.utils.AnonymizeService;
import org.esupportail.esupsignature.dto.js.JsMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.annotation.Resource;

@RequestMapping("/admin/users")
@Controller
@PreAuthorize("hasRole('ROLE_ADMIN')")
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
    private AnonymizeService anonymizeService;

    @GetMapping
    public String list(@RequestParam(required = false) String searchText, @SortDefault(value = "name", direction = Sort.Direction.ASC) @PageableDefault(size = 10) Pageable pageable, Model model) {
        Page<User> users;
        if(searchText == null) {
            users = userRepository.findAll(pageable);
        } else {
            users = userRepository.findByEppnOrPhoneOrEmailAndUserTypeNot(searchText, searchText, searchText, pageable);
        }
        model.addAttribute("searchText", searchText);
        model.addAttribute("users", users);
        return "admin/users/list";
    }

    @PostMapping("/anonymize/{id}")
    public String anonymize(@ModelAttribute("userEppn") String userEppn, @PathVariable Long id, @RequestParam(required = false) Boolean force, RedirectAttributes redirectAttributes) {
        try {
            anonymizeService.anonymize(id, force);
            redirectAttributes.addFlashAttribute("message", new JsMessage("success", "Suppression effectuée"));
        } catch (EsupSignatureUserException e) {
            redirectAttributes.addFlashAttribute("message", new JsMessage("error", e.getMessage()));
        }
        return "redirect:/admin/users";
    }

}
