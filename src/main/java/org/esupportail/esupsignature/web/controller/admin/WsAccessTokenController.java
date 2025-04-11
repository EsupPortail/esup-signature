package org.esupportail.esupsignature.web.controller.admin;

import jakarta.annotation.Resource;
import org.esupportail.esupsignature.dto.js.JsMessage;
import org.esupportail.esupsignature.service.WorkflowService;
import org.esupportail.esupsignature.service.WsAccessTokenService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/ws-access-token")
public class WsAccessTokenController {

    @ModelAttribute("adminMenu")
    String getCurrentMenu() {
        return "active";
    }

    @ModelAttribute("activeMenu")
    public String getActiveMenu() {
        return "tokensApi";
    }

    @Resource
    private WsAccessTokenService wsAccessTokenService;

    @Resource
    private WorkflowService workflowService;

    @GetMapping()
    public String list(@ModelAttribute("authUserEppn") String authUserEppn, Model model) {
        model.addAttribute("wsAccessTokens", wsAccessTokenService.getAll());
        model.addAttribute("globalWsAccessToken", wsAccessTokenService.getGlobalToken());
        model.addAttribute("workflows", workflowService.getAllWorkflows());
        return "admin/ws-access-token/list";
    }

    @PostMapping()
    public String create(@ModelAttribute("authUserEppn") String authUserEppn,
                         @RequestParam String appName, @RequestParam List<Long> workflowIds, RedirectAttributes redirectAttributes) {
        wsAccessTokenService.createToken(appName, workflowIds);
        redirectAttributes.addFlashAttribute("message", new JsMessage("success", "Token ajouté"));
        return "redirect:/admin/ws-access-token";
    }

    @PutMapping("/{wsAccessTokenId}")
    public String update(@PathVariable Long wsAccessTokenId, @RequestParam String appName, @RequestParam List<Long> workflowIds, RedirectAttributes redirectAttributes) {
        wsAccessTokenService.updateToken(wsAccessTokenId, appName, workflowIds);
        redirectAttributes.addFlashAttribute("message", new JsMessage("success", "Modification effectuée"));
        return "redirect:/admin/ws-access-token";
    }

    @PutMapping("/renew/{wsAccessTokenId}")
    public String renew(@PathVariable Long wsAccessTokenId, RedirectAttributes redirectAttributes) {
        wsAccessTokenService.renew(wsAccessTokenId);
        redirectAttributes.addFlashAttribute("message", new JsMessage("success", "Régénération effectuée"));
        return "redirect:/admin/ws-access-token";
    }

    @GetMapping("/toggle-public/{wsAccessTokenId}")
    public String togglePublic(@PathVariable Long wsAccessTokenId, RedirectAttributes redirectAttributes) {
        wsAccessTokenService.togglePublic(wsAccessTokenId);
        return "redirect:/admin/ws-access-token";
    }

    @GetMapping("/reset")
    public String reset(@ModelAttribute("authUserEppn") String authUserEppn, RedirectAttributes redirectAttributes) {
        if(wsAccessTokenService.createDefaultWsAccessToken()) {
            redirectAttributes.addFlashAttribute("message", new JsMessage("success", "Reset effectué"));
        } else {
            redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Reset impossible car il existe des règles"));
        }
        return "redirect:/admin/ws-access-token";
    }

    @DeleteMapping("/{wsAccessTokenId}")
    public String delete(@PathVariable Long wsAccessTokenId, RedirectAttributes redirectAttributes) {
        wsAccessTokenService.delete(wsAccessTokenId);
        redirectAttributes.addFlashAttribute("message", new JsMessage("success", "Suppression effectuée"));
        return "redirect:/admin/ws-access-token";
    }

}
