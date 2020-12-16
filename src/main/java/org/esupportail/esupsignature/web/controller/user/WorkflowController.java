package org.esupportail.esupsignature.web.controller.user;

import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.service.WorkflowService;
import org.esupportail.esupsignature.service.WorkflowStepService;
import org.esupportail.esupsignature.web.controller.ws.json.JsonMessage;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;

@Controller
@RequestMapping("/user/workflows")

public class WorkflowController {

    @Resource
    private WorkflowService workflowService;

    @Resource
    private WorkflowStepService workflowStepService;

    @PreAuthorize("@preAuthorizeService.workflowOwner(#name, #userId)")
    @GetMapping(value = "/{name}", produces = "text/html")
    public String show(@ModelAttribute("userId") Long userId, @PathVariable("name") String name, Model model, RedirectAttributes redirectAttributes) {
        model.addAttribute("fromAdmin", false);
        model.addAttribute("signTypes", SignType.values());
        Workflow workflow = workflowService.getWorkflowByName(name);
        if(workflow != null) {
            model.addAttribute("workflow", workflow);
            return "user/workflows/show";
        } else {
            workflow = workflowService.getWorkflowByName(workflowService.getWorkflowClassByName(name).getClass().getSimpleName());
            if (workflow != null) {
                model.addAttribute("workflow", workflow);
                return "user/workflows/show";
            }
        }
        redirectAttributes.addFlashAttribute("Workflow introuvable");
        return "redirect:/";
    }


    @PreAuthorize("@preAuthorizeService.workflowOwner(#id, #userId)")
    @PostMapping(value = "/add-step/{id}")
    public String addStep(@ModelAttribute("userId") Long userId, @PathVariable("id") Long id,
                          @RequestParam("signType") String signType,
                          @RequestParam(name="description", required = false) String description,
                          @RequestParam(value = "recipientsEmails", required = false) String[] recipientsEmails,
                          @RequestParam(name="changeable", required = false) Boolean changeable,
                          @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete) {
        Workflow workflow = workflowService.getById(id);
        workflowStepService.addStep(workflow, signType, description, recipientsEmails, changeable, allSignToComplete);
        return "redirect:/user/workflows/" + workflow.getName();
    }

    @PreAuthorize("@preAuthorizeService.workflowOwner(#id, #userId)")
    @GetMapping(value = "/update-step/{id}/{step}")
    public String changeStepSignType(@ModelAttribute("userId") Long userId,
                                     @PathVariable("id") Long id,
                                     @PathVariable("step") Integer step,
                                     @RequestParam(name="signType") SignType signType,
                                     @RequestParam(name="description") String description,
                                     @RequestParam(name="changeable", required = false) Boolean changeable,
                                     @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete) {
        Workflow workflow = workflowService.getById(id);
        workflowStepService.updateStep(workflow.getWorkflowSteps().get(step), signType, description, changeable, allSignToComplete);
        return "redirect:/user/workflows/" + workflow.getName();
    }

    @PreAuthorize("@preAuthorizeService.workflowOwner(#id, #userId)")
    @DeleteMapping(value = "/remove-step-recipent/{id}/{workflowStepId}")
    public String removeStepRecipient(@ModelAttribute("userId") Long userId, @PathVariable("id") Long id,
                                      @PathVariable("workflowStepId") Long workflowStepId, RedirectAttributes redirectAttributes) {
        Workflow workflow = workflowService.getById(id);
        WorkflowStep workflowStep = workflowStepService.removeStepRecipient(workflowStepId, userId);
        redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Participant supprimé"));
        return "redirect:/user/workflows/" + workflow.getName() + "#" + workflowStep.getId();
    }

    @PreAuthorize("@preAuthorizeService.workflowOwner(#id, #userId)")
    @PostMapping(value = "/add-step-recipents/{id}/{workflowStepId}")
    public String addStepRecipient(@ModelAttribute("userId") Long userId,
                                   @PathVariable("id") Long id,
                                   @PathVariable("workflowStepId") Long workflowStepId,
                                   @RequestParam String recipientsEmails, RedirectAttributes redirectAttributes) {
        Workflow workflow = workflowService.getById(id);
        WorkflowStep workflowStep = workflowStepService.addStepRecipients(workflowStepId, recipientsEmails);
        redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Participant ajouté"));
        return "redirect:/user/workflows/" + workflow.getName() + "#" + workflowStep.getId();
    }

    @PreAuthorize("@preAuthorizeService.workflowOwner(#id, #userId)")
    @DeleteMapping(value = "/remove-step/{id}/{stepNumber}")
    public String addStep(@ModelAttribute("userId") Long userId,
                          @PathVariable("id") Long id,
                          @PathVariable("stepNumber") Integer stepNumber) {
        Workflow workflow = workflowService.getById(id);
        workflowStepService.removeStep(workflow, stepNumber);
        return "redirect:/user/workflows/" + workflow.getName();
    }



    @DeleteMapping(value = "/{id}", produces = "text/html")
    @PreAuthorize("@preAuthorizeService.workflowOwner(#id, #userId)")
    public String delete(@ModelAttribute("userId") Long userId, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Workflow workflow = workflowService.getById(id);
        workflowService.delete(workflow);
        return "redirect:/";
    }

}
