package org.esupportail.esupsignature.web.controller.user;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.WorkflowService;
import org.esupportail.esupsignature.web.controller.ws.json.JsonMessage;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/user/workflows")
@Transactional
public class WorkflowController {

    @Resource
    WorkflowService workflowService;

    @PreAuthorize("@workflowService.preAuthorizeOwner(#name, #user)")
    @GetMapping(value = "/{name}", produces = "text/html")
    public String show(@ModelAttribute("user") User user, @PathVariable("name") String name, Model model, RedirectAttributes redirectAttributes) {
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


    @PreAuthorize("@workflowService.preAuthorizeOwner(#id, #user)")
    @PostMapping(value = "/add-step/{id}")
    public String addStep(@ModelAttribute("user") User user, @PathVariable("id") Long id,
                          @RequestParam("signType") String signType,
                          @RequestParam(name="description", required = false) String description,
                          @RequestParam(value = "recipientsEmails", required = false) String[] recipientsEmails,
                          @RequestParam(name="changeable", required = false) Boolean changeable,
                          @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete) throws EsupSignatureUserException {
        Workflow workflow = workflowService.getWorkflowById(id);
        workflowService.addStep(signType, description, recipientsEmails, changeable, allSignToComplete, workflow);
        return "redirect:/user/workflows/" + workflow.getName();
    }

    @PreAuthorize("@workflowService.preAuthorizeOwner(#id, #user)")
    @GetMapping(value = "/update-step/{id}/{step}")
    public String changeStepSignType(@ModelAttribute("user") User user,
                                     @PathVariable("id") Long id,
                                     @PathVariable("step") Integer step,
                                     @RequestParam(name="signType") SignType signType,
                                     @RequestParam(name="description") String description,
                                     @RequestParam(name="changeable", required = false) Boolean changeable,
                                     @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete) {
        Workflow workflow = workflowService.getWorkflowById(id);
        workflowService.updateStep(step, signType, description, changeable, allSignToComplete, workflow);
        return "redirect:/user/workflows/" + workflow.getName();
    }

    @PreAuthorize("@workflowService.preAuthorizeOwner(#id, #user)")
    @DeleteMapping(value = "/remove-step-recipent/{id}/{workflowStepId}")
    public String removeStepRecipient(@ModelAttribute("user") User user, @PathVariable("id") Long id,
                                      @PathVariable("workflowStepId") Long workflowStepId,
                                      @RequestParam(value = "userId") Long userId, RedirectAttributes redirectAttributes) {
        Workflow workflow = workflowService.getWorkflowById(id);
        WorkflowStep workflowStep = workflowService.removeStepRecipient(user, workflowStepId, userId, workflow);
        redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Participant supprimé"));
        return "redirect:/user/workflows/" + workflow.getName() + "#" + workflowStep.getId();
    }

    @PreAuthorize("@workflowService.preAuthorizeOwner(#id, #user)")
    @PostMapping(value = "/add-step-recipents/{id}/{workflowStepId}")
    public String addStepRecipient(@ModelAttribute("user") User user,
                                   @PathVariable("id") Long id,
                                   @PathVariable("workflowStepId") Long workflowStepId,
                                   @RequestParam String recipientsEmails, RedirectAttributes redirectAttributes, HttpServletRequest httpServletRequest) {
        Workflow workflow = workflowService.getWorkflowById(id);
        WorkflowStep workflowStep = workflowService.addStepRecipients(user, workflowStepId, recipientsEmails, workflow);
        redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Participant ajouté"));
        return "redirect:/user/workflows/" + workflow.getName() + "#" + workflowStep.getId();
    }

    @PreAuthorize("@workflowService.preAuthorizeOwner(#id, #user)")
    @DeleteMapping(value = "/remove-step/{id}/{stepNumber}")
    public String addStep(@ModelAttribute("user") User user,
                          @PathVariable("id") Long id,
                          @PathVariable("stepNumber") Integer stepNumber) {
        Workflow workflow = workflowService.getWorkflowById(id);
        workflowService.removeStep(stepNumber, workflow);
        return "redirect:/user/workflows/" + workflow.getName();
    }



    @DeleteMapping(value = "/{id}", produces = "text/html")
    @PreAuthorize("@workflowService.preAuthorizeOwner(#id, #user)")
    public String delete(@ModelAttribute("user") User user, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Workflow workflow = workflowService.getWorkflowById(id);
        workflowService.deleteWorkflow(workflow);
        return "redirect:/";
    }

}
