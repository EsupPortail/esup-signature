package org.esupportail.esupsignature.web.controller.user;

import org.esupportail.esupsignature.entity.Certificat;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.CertificatService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.esupportail.esupsignature.service.WorkflowStepService;
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.annotation.Resource;
import java.util.List;

@Controller
@RequestMapping("/user/workflows")
public class WorkflowController {

    @Resource
    private WorkflowService workflowService;

    @Resource
    private WorkflowStepService workflowStepService;

    @Resource
    private CertificatService certificatService;

    @PreAuthorize("@preAuthorizeService.workflowOwner(#id, #userEppn)")
    @GetMapping(value = "/{id}", produces = "text/html")
    public String show(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id, Model model) {
        model.addAttribute("fromAdmin", false);
        Workflow workflow = workflowService.getById(id);
        model.addAttribute("workflow", workflow);
        List<Certificat> certificats = certificatService.getAllCertificats();
        model.addAttribute("certificats", certificats);
        return "user/workflows/show";
    }


    @PreAuthorize("@preAuthorizeService.workflowOwner(#id, #userEppn)")
    @PostMapping(value = "/add-step/{id}")
    public String addStep(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id,
                          @RequestParam("signType") String signType,
                          @RequestParam(name="description", required = false) String description,
                          @RequestParam(value = "recipientsEmails", required = false) String[] recipientsEmails,
                          @RequestParam(name="maxRecipients", required = false) Integer maxRecipients,
                          @RequestParam(name="changeable", required = false) Boolean changeable,
                          @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete,
                          @RequestParam(name="attachmentRequire", required = false) Boolean attachmentRequire) throws EsupSignatureRuntimeException {
        workflowStepService.addStep(id, signType, description, recipientsEmails, changeable, allSignToComplete, maxRecipients, userEppn, true, attachmentRequire, false, null);
        return "redirect:/user/workflows/" + id;
    }

    @PreAuthorize("@preAuthorizeService.workflowOwner(#id, #userEppn)")
    @PostMapping(value = "/update-step/{id}/{step}")
    public String changeStepSignType(@ModelAttribute("userEppn") String userEppn,
                                     @PathVariable("id") Long id,
                                     @PathVariable("step") Integer step,
                                     @RequestParam(name="signType") SignType signType,
                                     @RequestParam(name="description") String description,
                                     @RequestParam(name="maxRecipients", required = false) Integer maxRecipients,
                                     @RequestParam(name="changeable", required = false) Boolean changeable,
                                     @RequestParam(name="multiSign", required = false) Boolean multiSign,
                                     @RequestParam(name="repeatable", required = false) Boolean repeatable,
                                     @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete,
                                     @RequestParam(name="attachmentAlert", required = false) Boolean attachmentAlert,
                                     @RequestParam(name="attachmentRequire", required = false) Boolean attachmentRequire,
                                     RedirectAttributes redirectAttributes) {
        Workflow workflow = workflowService.getById(id);
        try {
            workflowStepService.updateStep(workflow.getWorkflowSteps().get(step).getId(), signType, description, changeable, repeatable, multiSign, allSignToComplete, maxRecipients, attachmentAlert, attachmentRequire, false, null);
        } catch (EsupSignatureRuntimeException e) {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Type de signature impossible pour une étape infinie"));
        }
        return "redirect:/user/workflows/" + id;
    }

    @PreAuthorize("@preAuthorizeService.workflowOwner(#id, #userEppn)")
    @DeleteMapping(value = "/remove-step-recipent/{id}/{workflowStepId}")
    public String removeStepRecipient(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id,
                                      @RequestParam(value = "userToRemoveEppn") String userToRemoveEppn,
                                      @PathVariable("workflowStepId") Long workflowStepId, RedirectAttributes redirectAttributes) {
        WorkflowStep workflowStep = workflowStepService.removeStepRecipient(workflowStepId, userToRemoveEppn);
        redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Participant supprimé"));
        return "redirect:/user/workflows/" + id + "#" + workflowStep.getId();
    }

    @PreAuthorize("@preAuthorizeService.workflowOwner(#id, #userEppn)")
    @PostMapping(value = "/add-step-recipents/{id}/{workflowStepId}")
    public String addStepRecipient(@ModelAttribute("userEppn") String userEppn,
                                   @PathVariable("id") Long id,
                                   @PathVariable("workflowStepId") Long workflowStepId,
                                   @RequestParam String[] recipientsEmails, RedirectAttributes redirectAttributes) {
        Workflow workflow = workflowService.getById(id);
        WorkflowStep workflowStep = null;
        try {
            workflowStep = workflowStepService.addStepRecipients(workflowStepId, recipientsEmails);
            redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Participant ajouté"));
        } catch (EsupSignatureRuntimeException e) {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Participant non ajouté"));
        }
        return "redirect:/user/workflows/" + workflow.getId() + "#" + workflowStep.getId();
    }

    @PreAuthorize("@preAuthorizeService.workflowOwner(#id, #userEppn)")
    @DeleteMapping(value = "/remove-step/{id}/{stepNumber}")
    public String addStep(@ModelAttribute("userEppn") String userEppn,
                          @PathVariable("id") Long id,
                          @PathVariable("stepNumber") Integer stepNumber) {
        Workflow workflow = workflowService.getById(id);
        workflowStepService.removeStep(workflow, stepNumber);
        return "redirect:/user/workflows/" + workflow.getId();
    }

    @DeleteMapping(value = "/{id}", produces = "text/html")
    @PreAuthorize("@preAuthorizeService.workflowOwner(#id, #userEppn)")
    public String delete(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Workflow workflow = workflowService.getById(id);
        try {
            workflowService.delete(workflow);
            redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Le circuit a bien été supprimé"));
        } catch (EsupSignatureRuntimeException e) {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("error", e.getMessage()));
        }
        return "redirect:/";
    }

    @DeleteMapping(value = "/silent-delete/{id}", produces = "text/html")
    @PreAuthorize("@preAuthorizeService.workflowOwner(#id, #userEppn)")
    @ResponseBody
    public void silentDelete(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id) throws EsupSignatureRuntimeException {
        Workflow workflow = workflowService.getById(id);
        workflowService.delete(workflow);
    }

    @PutMapping(value = "/{id}")
    @PreAuthorize("@preAuthorizeService.workflowOwner(#id, #userEppn)")
    public String rename(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id,
                         @RequestParam(required = false) List<String> viewers,
                         @RequestParam String name) {
        workflowService.rename(id, name);
        workflowService.addViewers(id, viewers);
        return "redirect:/user/workflows/" + id;
    }
}
