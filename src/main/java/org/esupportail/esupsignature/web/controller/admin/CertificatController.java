package org.esupportail.esupsignature.web.controller.admin;

import org.esupportail.esupsignature.config.sign.SignProperties;
import org.esupportail.esupsignature.service.CertificatService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

@RequestMapping("/admin/certificats")
@Controller
public class CertificatController {

    @ModelAttribute("adminMenu")
    String getCurrentMenu() {
        return "active";
    }

    @ModelAttribute("activeMenu")
    public String getActiveMenu() {
        return "certificats";
    }

    @Resource
    private CertificatService certificatService;

    @Resource
    private UserService userService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("certificats", certificatService.getAllCertificats());
        model.addAttribute("roles", userService.getAllRoles());
        return "admin/certificats/list";
    }

    @PostMapping
    public String addCertificat(
            @RequestParam MultipartFile keystore,
            @RequestParam List<String> roleNames,
            @RequestParam String password,
            RedirectAttributes redirectAttributes) throws IOException {
        certificatService.addCertificat(keystore, roleNames, password);
        redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Certificat ajouté"));
        return "redirect:/admin/certificats";
    }

    @DeleteMapping
    public String deleteCertificat(@RequestParam Long id,
            RedirectAttributes redirectAttributes) throws IOException {
        certificatService.delete(id);
        redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Certificat supprimé"));
        return "redirect:/admin/certificats";
    }

}
