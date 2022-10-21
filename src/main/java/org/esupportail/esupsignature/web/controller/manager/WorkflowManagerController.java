package org.esupportail.esupsignature.web.controller.manager;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.esupportail.esupsignature.service.WorkflowStepService;
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@RequestMapping("/manager/workflows")
@Controller
public class WorkflowManagerController {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowManagerController.class);

    @Resource
    private SignBookService signBookService;

    @ModelAttribute("managerMenu")
    public String getAdminMenu() {
        return "active";
    }

    @ModelAttribute("activeMenu")
    public String getActiveMenu() {
        return "workflows";
    }

    @Resource
    private UserService userService;

    @Resource
    private WorkflowService workflowService;

    @Resource
    private WorkflowStepService workflowStepService;

    @GetMapping
    @PreAuthorize("@preAuthorizeService.isManager(#authUserEppn)")
    public String list(@ModelAttribute("authUserEppn") String authUserEppn, Model model) {
        model.addAttribute("workflows", workflowService.getManagerWorkflows(authUserEppn));
        model.addAttribute("roles", userService.getManagersRoles(authUserEppn));
        return "managers/workflows/list";
    }

    @GetMapping(value = "/{id}")
    @PreAuthorize("@preAuthorizeService.workflowManager(#id, #authUserEppn)")
    public String show(@PathVariable("id") Long id, Model model, @ModelAttribute("authUserEppn") String authUserEppn) {
        model.addAttribute("fromAdmin", true);
        Workflow workflow = workflowService.getById(id);
        model.addAttribute("workflow", workflow);
        return "managers/workflows/show";
    }

    @PostMapping(produces = "text/html")
    @PreAuthorize("@preAuthorizeService.isManager(#authUserEppn)")
    public String create(@ModelAttribute("authUserEppn") String authUserEppn, @RequestParam(name = "managerRole") String managerRole, @RequestParam(name = "title", required = false) String title, @RequestParam(name = "description") String description, RedirectAttributes redirectAttributes) {
        if(title == null) {
            title = description;
        }
        Workflow workflow;
        try {
            workflow = workflowService.createWorkflow(title, description, userService.getByEppn(authUserEppn));
            workflow.setManagerRole(managerRole);
        } catch (EsupSignatureException e) {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Un circuit possède déjà ce préfixe"));
            return "redirect:/manager/workflows/";
        }
        return "redirect:/manager/workflows/" + workflow.getId();
    }

    @GetMapping(value = "/update/{id}")
    @PreAuthorize("@preAuthorizeService.workflowManager(#id, #authUserEppn)")
    public String updateForm(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, Model model) {
        Workflow workflow = workflowService.getById(id);
        User manager = userService.getByEppn(authUserEppn);
        model.addAttribute("workflow", workflow);
        model.addAttribute("roles", manager.getManagersRoles());
        model.addAttribute("sourceTypes", DocumentIOType.values());
        model.addAttribute("targetTypes", DocumentIOType.values());
        model.addAttribute("shareTypes", ShareType.values());
        return "managers/workflows/update";
    }

    @PostMapping(value = "/update")
    @PreAuthorize("@preAuthorizeService.workflowManager(#workflow.id, #authUserEppn)")
    public String update(@ModelAttribute("authUserEppn") String authUserEppn,
                         @Valid Workflow workflow,
                         @RequestParam(value = "types", required = false) String[] types,
                         @RequestParam(required = false) List<String> managers, Model model) {
        User authUser = userService.getUserByEppn(authUserEppn);
        workflow.setPublicUsage(false);
        Workflow updateWorkflow = workflowService.update(workflow, authUser, types, managers);
        return "redirect:/manager/workflows/update/" + updateWorkflow.getId();
    }

    @DeleteMapping(value = "/{id}", produces = "text/html")
    @PreAuthorize("@preAuthorizeService.workflowManager(#id, #authUserEppn)")
    public String delete(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Workflow workflow = workflowService.getById(id);
        try {
            workflowService.delete(workflow);
        } catch (EsupSignatureException e) {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("error", e.getMessage()));
        }
        return "redirect:/manager/workflows";
    }

    @PostMapping(value = "/add-step/{id}")
    @PreAuthorize("@preAuthorizeService.workflowManager(#id, #authUserEppn)")
    public String addStep(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                          @RequestParam("signType") String signType,
                          @RequestParam(name="description", required = false) String description,
                          @RequestParam(value = "recipientsEmails", required = false) String[] recipientsEmails,
                          @RequestParam(name="maxRecipients", required = false) Integer maxRecipients,
                          @RequestParam(name="changeable", required = false) Boolean changeable,
                          @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete,
                          @RequestParam(name="attachmentRequire", required = false) Boolean attachmentRequire
                        ) throws EsupSignatureException {
        workflowStepService.addStep(id, signType, description, recipientsEmails, changeable, allSignToComplete, maxRecipients, authUserEppn, false, attachmentRequire, false, null);
        return "redirect:/manager/workflows/" + id;
    }

    @PostMapping(value = "/update-step/{id}/{step}")
    @PreAuthorize("@preAuthorizeService.workflowManager(#id, #authUserEppn)")
    public String changeStepSignType(@ModelAttribute("authUserEppn") String authUserEppn,
                                     @PathVariable("id") Long id,
                                     @PathVariable("step") Integer step,
                                     @RequestParam(name="signType") SignType signType,
                                     @RequestParam(name="description") String description,
                                     @RequestParam(name="maxRecipients", required = false) Integer maxRecipients,
                                     @RequestParam(name="repeatable", required = false) Boolean repeatable,
                                     @RequestParam(name="multiSign", required = false) Boolean multiSign,
                                     @RequestParam(name="changeable", required = false) Boolean changeable,
                                     @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete,
                                     @RequestParam(name="attachmentAlert", required = false) Boolean attachmentAlert,
                                     @RequestParam(name="attachmentRequire", required = false) Boolean attachmentRequire,
                                     @RequestParam(name="autoSign", required = false) Boolean autoSign,
                                     RedirectAttributes redirectAttributes) {
        Workflow workflow = workflowService.getById(id);
        try {
            workflowStepService.updateStep(workflow.getWorkflowSteps().get(step).getId(), signType, description, changeable, repeatable, multiSign, allSignToComplete, maxRecipients, attachmentAlert, attachmentRequire, autoSign, null
            );
        } catch (EsupSignatureException e) {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Type de signature impossible pour une étape infinie"));
        }
        return "redirect:/manager/workflows/" + id;
    }

    @DeleteMapping(value = "/remove-step-recipent/{id}/{workflowStepId}")
    @PreAuthorize("@preAuthorizeService.workflowManager(#id, #authUserEppn)")
    public String removeStepRecipient(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                                      @PathVariable("workflowStepId") Long workflowStepId,
                                      @RequestParam(value = "userToRemoveEppn") String userToRemoveEppn, RedirectAttributes redirectAttributes) {
        WorkflowStep workflowStep = workflowStepService.removeStepRecipient(workflowStepId, userToRemoveEppn);
        redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Participant supprimé"));
        return "redirect:/manager/workflows/" + id + "#" + workflowStep.getId();
    }

    @PostMapping(value = "/add-step-recipents/{id}/{workflowStepId}")
    @PreAuthorize("@preAuthorizeService.workflowManager(#id, #authUserEppn)")
    public String addStepRecipient(@ModelAttribute("authUserEppn") String authUserEppn,
                                   @PathVariable("id") Long id,
                                   @PathVariable("workflowStepId") Long workflowStepId,
                                   @RequestParam String[] recipientsEmails, RedirectAttributes redirectAttributes) {
        WorkflowStep workflowStep = workflowStepService.addStepRecipients(workflowStepId, recipientsEmails);
        redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Participant ajouté"));
        return "redirect:/manager/workflows/" + id + "#" + workflowStep.getId();
    }

    @DeleteMapping(value = "/remove-step/{id}/{stepNumber}")
    @PreAuthorize("@preAuthorizeService.workflowManager(#id, #authUserEppn)")
    public String addStep(@ModelAttribute("authUserEppn") String authUserEppn,
                          @PathVariable("id") Long id,
                          @PathVariable("stepNumber") Integer stepNumber) {
        Workflow workflow = workflowService.getById(id);
        workflowStepService.removeStep(workflow, stepNumber);
        return "redirect:/manager/workflows/" + id;
    }

    @GetMapping(value = "/get-files-from-source/{id}")
    @PreAuthorize("@preAuthorizeService.workflowManager(#id, #authUserEppn)")
    public String getFileFromSource(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) throws EsupSignatureException {
        User authUser = userService.getUserByEppn(authUserEppn);
        int nbImportedFiles = signBookService.importFilesFromSource(id, authUser, authUser);
        if(nbImportedFiles == 0) {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Aucun fichier à importer"));
        } else {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("info", nbImportedFiles + " ficher(s) importé(s)"));
        }
        return "redirect:/manager/workflows/" + id;
    }

    @PostMapping(value = "/add-target/{id}")
    @PreAuthorize("@preAuthorizeService.workflowManager(#id, #authUserEppn)")
    public String addTarget(@PathVariable("id") Long id,
                            @RequestParam("documentsTargetUri") String documentsTargetUri,
                            @ModelAttribute("authUserEppn") String authUserEppn,
                            RedirectAttributes redirectAttributes) throws EsupSignatureException, EsupSignatureFsException {
        if(workflowService.addTarget(id, documentsTargetUri)) {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Destination ajoutée"));
        } else {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("warn", "Une destination mail existe déjà"));
        }
        return "redirect:/manager/workflows/update/" + id;
    }

    @GetMapping(value = "/delete-target/{id}/{targetId}")
    @PreAuthorize("@preAuthorizeService.workflowManager(#id, #authUserEppn)")
    public String deleteTarget(@PathVariable("id") Long id,
                               @PathVariable("targetId") Long targetId,
                               @ModelAttribute("authUserEppn") String authUserEppn,
                               RedirectAttributes redirectAttributes) {
        workflowService.deleteTarget(id, targetId);
        redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Destination supprimée"));
        return "redirect:/manager/workflows/update/" + id;
    }

}
