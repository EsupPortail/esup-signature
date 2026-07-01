package org.esupportail.esupsignature.web.controller.admin;

import org.esupportail.esupsignature.entity.enums.UiParams;
import org.esupportail.esupsignature.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/admin/branding")
public class BrandingAdminController {

    private final UserService userService;

    public BrandingAdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("loginTitle", userService.getSystemUiParam(UiParams.loginTitle));
        model.addAttribute("loginSubtitle", userService.getSystemUiParam(UiParams.loginSubtitle));
        model.addAttribute("hasBackground", userService.getSystemUiDocument(UiParams.loginBackgroundDocumentId) != null);
        return "admin/branding";
    }

    @PostMapping("/save-text")
    public String saveText(@RequestParam(required = false) String loginTitle,
                           @RequestParam(required = false) String loginSubtitle,
                           RedirectAttributes redirectAttributes) {
        userService.setSystemUiParam(UiParams.loginTitle, loginTitle);
        userService.setSystemUiParam(UiParams.loginSubtitle, loginSubtitle);
        redirectAttributes.addFlashAttribute("message", "Textes enregistrés avec succès");
        return "redirect:/admin/branding";
    }

    @PostMapping("/upload-background")
    public String uploadBackground(@RequestParam MultipartFile backgroundImage,
                                   RedirectAttributes redirectAttributes) throws IOException {
        if (backgroundImage != null && !backgroundImage.isEmpty()) {
            userService.setSystemUiImage(UiParams.loginBackgroundDocumentId, backgroundImage, "login-background");
            redirectAttributes.addFlashAttribute("message", "Image de fond enregistrée avec succès");
        }
        return "redirect:/admin/branding";
    }

    @PostMapping("/clear-background")
    public String clearBackground(RedirectAttributes redirectAttributes) {
        userService.clearSystemUiImage(UiParams.loginBackgroundDocumentId);
        redirectAttributes.addFlashAttribute("message", "Image de fond supprimée");
        return "redirect:/admin/branding";
    }
}
