package org.esupportail.esupsignature.web.controller.admin;

import org.esupportail.esupsignature.dto.ui.global.UiMessageDto;
import org.esupportail.esupsignature.entity.enums.UiParams;
import org.esupportail.esupsignature.service.PaperlessService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@RequestMapping("/admin/paperless")
@Controller
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class PaperlessAdminController {

    private final PaperlessService paperlessService;

    public PaperlessAdminController(PaperlessService paperlessService) {
        this.paperlessService = paperlessService;
    }

    @ModelAttribute("activeMenu")
    public String getActiveMenu() {
        return "paperless";
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("paperlessUrl", paperlessService.getPaperlessUrl());
        model.addAttribute("paperlessToken", paperlessService.getPaperlessToken());
        model.addAttribute("isConfigured", paperlessService.isConfigured());
        model.addAttribute("paperlessSourceFieldId",   paperlessService.getCustomFieldId(UiParams.paperlessSourceFieldId));
        model.addAttribute("paperlessSignedFieldId",   paperlessService.getCustomFieldId(UiParams.paperlessSignedFieldId));
        model.addAttribute("paperlessEsignRequestFieldId",  paperlessService.getCustomFieldId(UiParams.paperlessEsignRequestFieldId));
        model.addAttribute("paperlessSignatureDateFieldId", paperlessService.getCustomFieldId(UiParams.paperlessSignatureDateFieldId));
        model.addAttribute("paperlessSignataireFieldId",    paperlessService.getCustomFieldId(UiParams.paperlessSignataireFieldId));
        return "admin/paperless";
    }

    @PostMapping("/save")
    public String save(@RequestParam(value = "paperlessUrl", defaultValue = "") String url,
                       @RequestParam(value = "paperlessToken", defaultValue = "") String token,
                       RedirectAttributes redirectAttributes) {
        paperlessService.saveConfig(url.trim(), token.trim());
        redirectAttributes.addFlashAttribute("message", new UiMessageDto("success", "Configuration Paperless enregistrée"));
        return "redirect:/admin/paperless";
    }

    @PostMapping("/save-fields")
    public String saveFields(@RequestParam(value = "paperlessSourceFieldId",        defaultValue = "") String sourceFieldId,
                             @RequestParam(value = "paperlessSignedFieldId",         defaultValue = "") String signedFieldId,
                             @RequestParam(value = "paperlessEsignRequestFieldId",   defaultValue = "") String esignRequestFieldId,
                             @RequestParam(value = "paperlessSignatureDateFieldId",  defaultValue = "") String signatureDateFieldId,
                             @RequestParam(value = "paperlessSignataireFieldId",     defaultValue = "") String signataireFieldId,
                             RedirectAttributes redirectAttributes) {
        paperlessService.saveCustomFieldsConfig(
                sourceFieldId.trim(), signedFieldId.trim(),
                esignRequestFieldId.trim(), signatureDateFieldId.trim(),
                signataireFieldId.trim());
        redirectAttributes.addFlashAttribute("message", new UiMessageDto("success", "Champs personnalisés enregistrés"));
        return "redirect:/admin/paperless";
    }

    @PostMapping("/test")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testConnection() {
        Map<String, Object> result = paperlessService.testConnection();
        boolean success = Boolean.TRUE.equals(result.get("success"));
        return success ? ResponseEntity.ok(result) : ResponseEntity.status(502).body(result);
    }
}
