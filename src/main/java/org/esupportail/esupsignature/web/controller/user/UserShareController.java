package org.esupportail.esupsignature.web.controller.user;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.UserShare;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.FormService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.UserShareService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

@CrossOrigin(origins = "*")
@RequestMapping("/user/users/shares")
@Controller
@ConditionalOnExpression("${global.share-mode} > 0")
public class UserShareController {

    @Resource
    private UserShareService userShareService;

    @Resource
    private UserService userService;

    @Resource
    private FormService formService;

    @Resource
    private WorkflowService workflowService;

    @GetMapping
    public String params(@ModelAttribute("authUserEppn") String authUserEppn, Model model) {
        List<UserShare> userShares = userShareService.getUserSharesByUser(authUserEppn);
        model.addAttribute("userShares", userShares);
        model.addAttribute("shareTypes", ShareType.values());
        model.addAttribute("forms", formService.getAuthorizedToShareForms());
        model.addAttribute("workflows", workflowService.getAuthorizedToShareWorkflows());
        model.addAttribute("activeMenu", "shares");
        return "user/users/shares/list";
    }

    @GetMapping("/update/{id}")
    public String params(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        model.addAttribute("activeMenu", "shares");
        UserShare userShare = userShareService.getById(id);
        if(userShare.getUser().getEppn().equals(authUserEppn)) {
            model.addAttribute("shareTypes", ShareType.values());
            model.addAttribute("userShare", userShare);
            return "user/users/shares/update";
        } else {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Accès refusé"));
            return "redirect:/user/users/shares";
        }
    }

    @PostMapping("/add")
    public String addShare(@ModelAttribute("authUserEppn") String authUserEppn,
                           @RequestParam(value = "signWithOwnSign", required = false) Boolean signWithOwnSign,
                           @RequestParam(value = "form", required = false) Long[] form,
                           @RequestParam(value = "workflow", required = false) Long[] workflow,
                           @RequestParam("types") String[] types,
                           @RequestParam("userIds") String[] userEmails,
                           @RequestParam("beginDate") String beginDate,
                           @RequestParam("endDate") String endDate, Model model,
                           RedirectAttributes redirectAttributes) {
        User authUser = userService.getUserByEppn(authUserEppn);
        if(form == null) form = new Long[] {};
        if(workflow == null) workflow = new Long[] {};
        if(signWithOwnSign == null) signWithOwnSign = false;
        try {
            userShareService.addUserShare(authUser, signWithOwnSign, form, workflow, types, userEmails, beginDate, endDate);
        } catch (EsupSignatureUserException e) {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("error", e.getMessage()));
        }
        return "redirect:/user/users/shares";
    }

    @PostMapping("/update/{id}")
    public String updateShare(@ModelAttribute("authUserEppn") String authUserEppn,
                              @PathVariable("id") Long id,
                              @RequestParam(value = "signWithOwnSign", required = false) Boolean signWithOwnSign,
                              @RequestParam("types") String[] types,
                              @RequestParam("userIds") String[] userEmails,
                              @RequestParam("beginDate") String beginDate,
                              @RequestParam("endDate") String endDate) {
        userShareService.updateUserShare(authUserEppn, types, userEmails, beginDate, endDate, id, signWithOwnSign);
        return "redirect:/user/users/shares";
    }

    @DeleteMapping("/del/{id}")
    public String delShare(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable long id, RedirectAttributes redirectAttributes) {
        userShareService.delete(id, authUserEppn);
        redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Élément supprimé"));
        return "redirect:/user/users/shares";
    }

    @GetMapping("/change")
    public String change(@ModelAttribute("authUserEppn") String authUserEppn, @RequestParam(required = false) String eppn, @RequestParam(required = false) Long userShareId, RedirectAttributes redirectAttributes, HttpSession httpSession, HttpServletRequest httpServletRequest) {
        if(eppn == null || eppn.isEmpty()) {
            httpSession.setAttribute("suEppn", null);
            redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Délégation désactivée"));
        } else {
            if(userShareService.isOneShareActive(eppn, authUserEppn)) {
                httpSession.setAttribute("suEppn", eppn);
                httpSession.setAttribute("userShareId", userShareId);
                redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Délégation activée : " + eppn));
            } else {
                redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Aucune délégation active en ce moment"));
            }
        }
        return "redirect:/";
    }


}
