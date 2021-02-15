package org.esupportail.esupsignature.web.controller.user;

import io.swagger.v3.oas.annotations.Hidden;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.service.UserPropertieService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.esupportail.esupsignature.service.WorkflowStepService;
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import java.util.Arrays;

@Hidden
@Controller
@RequestMapping("/user/workflows")

public class WorkflowController {

    @Resource
    private WorkflowService workflowService;

    @Resource
    private WorkflowStepService workflowStepService;

    @Resource
    private UserService userService;

    @Resource
    private UserPropertieService userPropertieService;

    @PreAuthorize("@preAuthorizeService.workflowOwner(#id, #userEppn)")
    @GetMapping(value = "/{id}", produces = "text/html")
    public String show(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id, Model model) {
        model.addAttribute("fromAdmin", false);
        model.addAttribute("signTypes", SignType.values());
        Workflow workflow = workflowService.getById(id);
        model.addAttribute("workflow", workflow);
        return "user/workflows/show";
    }


    @PreAuthorize("@preAuthorizeService.workflowOwner(#id, #userEppn)")
    @PostMapping(value = "/add-step/{id}")
    public String addStep(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id,
                          @RequestParam("signType") String signType,
                          @RequestParam(name="description", required = false) String description,
                          @RequestParam(value = "recipientsEmails", required = false) String[] recipientsEmails,
                          @RequestParam(name="changeable", required = false) Boolean changeable,
                          @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete) {
        userPropertieService.createUserPropertieFromMails(userService.getByEppn(userEppn), Arrays.asList(recipientsEmails));
        workflowStepService.addStep(id, signType, description, recipientsEmails, changeable, allSignToComplete);
        return "redirect:/user/workflows/" + id;
    }

    @PreAuthorize("@preAuthorizeService.workflowOwner(#id, #userEppn)")
    @PostMapping(value = "/update-step/{id}/{step}")
    public String changeStepSignType(@ModelAttribute("userEppn") String userEppn,
                                     @PathVariable("id") Long id,
                                     @PathVariable("step") Integer step,
                                     @RequestParam(name="signType") SignType signType,
                                     @RequestParam(name="description") String description,
                                     @RequestParam(name="changeable", required = false) Boolean changeable,
                                     @RequestParam(name="repeatable", required = false) Boolean repeatable,
                                     @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete) {
        Workflow workflow = workflowService.getById(id);
        workflowStepService.updateStep(workflow.getWorkflowSteps().get(step).getId(), signType, description, changeable, repeatable, allSignToComplete);
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
                                   @RequestParam String recipientsEmails, RedirectAttributes redirectAttributes) {
        Workflow workflow = workflowService.getById(id);
        WorkflowStep workflowStep = workflowStepService.addStepRecipients(workflowStepId, recipientsEmails);
        redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Participant ajouté"));
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
        workflowService.delete(workflow);
        redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Le circuit à bien été supprimé"));
        return "redirect:/";
    }

    @DeleteMapping(value = "/silent-delete/{id}", produces = "text/html")
    @PreAuthorize("@preAuthorizeService.workflowOwner(#id, #userEppn)")
    @ResponseBody
    public void silentDelete(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id) {
        Workflow workflow = workflowService.getById(id);
        workflowService.delete(workflow);
    }

}
