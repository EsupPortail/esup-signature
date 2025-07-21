package org.esupportail.esupsignature.web.controller.user;

import jakarta.annotation.Resource;
import org.esupportail.esupsignature.dto.json.WorkflowStepDto;
import org.esupportail.esupsignature.dto.js.JsMessage;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.SignLevel;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.CertificatService;
import org.esupportail.esupsignature.service.RecipientService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.esupportail.esupsignature.service.WorkflowStepService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Arrays;
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

    @Resource
    private RecipientService recipientService;

    @PreAuthorize("@preAuthorizeService.workflowOwner(#id, #userEppn)")
    @GetMapping(value = "/{id}", produces = "text/html")
    public String show(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id, Model model) {
        model.addAttribute("fromAdmin", false);
        Workflow workflow = workflowService.getById(id);
        model.addAttribute("workflow", workflow);
        model.addAttribute("certificats", certificatService.getAllCertificats());
        model.addAttribute("workflowRole", "user");
        return "user/workflows/show";
    }


    @PreAuthorize("@preAuthorizeService.workflowOwner(#id, #authUserEppn)")
    @PostMapping(value = "/add-step/{id}")
    public String addStep(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                          @RequestParam("signType") String signType,
                          @RequestParam(name="description", required = false) String description,
                          @RequestParam(name="recipientsEmails", required = false) String[] recipientsEmails,
                          @RequestParam(name="changeable", required = false) Boolean changeable,
                          @RequestParam(name="maxRecipients", required = false) Integer maxRecipients,
                          @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete,
                          @RequestParam(name="attachmentRequire", required = false) Boolean attachmentRequire) throws EsupSignatureRuntimeException {
        List<String> recipients = new ArrayList<>();
        if(recipientsEmails != null) {
            recipients = List.of(recipientsEmails);
        }
        WorkflowStepDto workflowStepDto = new WorkflowStepDto(SignType.fromString(signType), description, recipientService.convertRecipientEmailsToRecipientDto(recipients), changeable, maxRecipients, allSignToComplete, attachmentRequire);
        workflowStepService.addStep(id, workflowStepDto, authUserEppn, false, false, null);
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
                                     @RequestParam(name="singleSignWithAnnotation", required = false) Boolean singleSignWithAnnotation,
                                     @RequestParam(name="repeatable", required = false) Boolean repeatable,
                                     @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete,
                                     @RequestParam(name="attachmentAlert", required = false) Boolean attachmentAlert,
                                     @RequestParam(name="attachmentRequire", required = false) Boolean attachmentRequire,
                                     @RequestParam(name="minSignLevel", required = false) SignLevel minSignLevel,
                                     @RequestParam(name="maxSignLevel", required = false) SignLevel maxSignLevel,
                                     @RequestParam(name="sealVisa", required = false) Boolean sealVisa,
                                     RedirectAttributes redirectAttributes) {
        Workflow workflow = workflowService.getById(id);
        try {
            workflowStepService.updateStep(workflow.getWorkflowSteps().get(step).getId(), signType, description, changeable, repeatable, multiSign, singleSignWithAnnotation, allSignToComplete, maxRecipients, attachmentAlert, attachmentRequire, false, null, minSignLevel, maxSignLevel, sealVisa);
        } catch (EsupSignatureRuntimeException e) {
            redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Type de signature impossible pour une étape infinie"));
        }
        return "redirect:/user/workflows/" + id;
    }

    @PreAuthorize("@preAuthorizeService.workflowOwner(#id, #userEppn)")
    @DeleteMapping(value = "/remove-step-recipent/{id}/{workflowStepId}")
    public String removeStepRecipient(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id,
                                      @RequestParam(value = "userToRemoveEppn") String userToRemoveEppn,
                                      @PathVariable("workflowStepId") Long workflowStepId, RedirectAttributes redirectAttributes) {
        WorkflowStep workflowStep = workflowStepService.removeStepRecipient(workflowStepId, userToRemoveEppn);
        redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Participant supprimé"));
        return "redirect:/user/workflows/" + id + "#" + workflowStep.getId();
    }

    @PreAuthorize("@preAuthorizeService.workflowOwner(#id, #userEppn)")
    @PostMapping(value = "/add-step-recipents/{id}/{workflowStepId}")
    public String addStepRecipient(@ModelAttribute("userEppn") String userEppn,
                                   @PathVariable("id") Long id,
                                   @PathVariable("workflowStepId") Long workflowStepId,
                                   @RequestParam String recipientsEmails, RedirectAttributes redirectAttributes) {
        Workflow workflow = workflowService.getById(id);
        WorkflowStep workflowStep = null;
        try {
            workflowStep = workflowStepService.addStepRecipients(workflowStepId, recipientService.convertRecipientEmailsToRecipientDto(Arrays.stream(recipientsEmails.split(",")).toList()));
            redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Participant ajouté"));
        } catch (EsupSignatureRuntimeException e) {
            redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Participant non ajouté"));
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
        try {
            workflowService.delete(id);
            redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Le circuit a bien été supprimé"));
        } catch (EsupSignatureRuntimeException e) {
            redirectAttributes.addFlashAttribute("message", new JsMessage("error", e.getMessage()));
        }
        return "redirect:/";
    }


    //add sendAlertToAllRecipients update to workflow
    @PutMapping(value = "/{id}")
    @PreAuthorize("@preAuthorizeService.workflowOwner(#id, #userEppn)")
    public String rename(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id,
                         @RequestParam(required = false) List<String> viewers,
                            @RequestParam(required = false) Boolean sendAlertToAllRecipients,
                         @RequestParam String name) {
        workflowService.rename(id, name);
        workflowService.addViewers(id, viewers);
        workflowService.updateSendAlertToAllRecipients(id, sendAlertToAllRecipients);
        return "redirect:/user/workflows/" + id;
    }
}
