package org.esupportail.esupsignature.web.controller.admin;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.DisplayWorkflowType;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.esupportail.esupsignature.service.WorkflowStepService;
import org.esupportail.esupsignature.web.controller.ws.json.JsonMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

@RequestMapping("/admin/workflows")
@Controller

public class WorkflowAdminController {

	private static final Logger logger = LoggerFactory.getLogger(WorkflowAdminController.class);
	
	@ModelAttribute("adminMenu")
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

	@GetMapping(produces = "text/html")
	public String list(@RequestParam(name = "displayWorkflowType", required = false) DisplayWorkflowType displayWorkflowType, Model model) {
		model.addAttribute("displayWorkflowType", displayWorkflowType);
		model.addAttribute("workflows", workflowService.getWorkflowsByDisplayWorkflowType(displayWorkflowType));
		return "admin/workflows/list";
	}

	@GetMapping(value = "/{name}", produces = "text/html")
	public String show(@PathVariable("name") String name, Model model, RedirectAttributes redirectAttributes) {
		model.addAttribute("fromAdmin", true);
		model.addAttribute("signTypes", SignType.values());
		Workflow workflow = workflowService.getWorkflowByName(name);
		if(workflow != null) {
			model.addAttribute("workflow", workflow);
			return "admin/workflows/show";
		} else {
			workflow = workflowService.getWorkflowByName(workflowService.getWorkflowClassByName(name).getClass().getSimpleName());
			if (workflow != null) {
				model.addAttribute("workflow", workflow);
				return "admin/workflows/show";
			}
		}
		redirectAttributes.addFlashAttribute("Workflow introuvable");
		return "redirect:/admin/workflows";
	}

	@PostMapping(produces = "text/html")
	public String create(@ModelAttribute("authUserId") Long authUserId, @RequestParam(name = "title") String title, @RequestParam(name = "description") String description, RedirectAttributes redirectAttributes) {
		Workflow workflow;
		try {
			workflow = workflowService.createWorkflow(title, description, userService.getSystemUser());
		} catch (EsupSignatureException e) {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Un circuit porte déjà ce nom"));
			return "redirect:/admin/workflows/";
		}
		return "redirect:/admin/workflows/" + workflow.getName();
	}

    @GetMapping(value = "/update/{id}")
    public String updateForm(@ModelAttribute("authUserId") Long authUserId, @PathVariable("id") Long id, Model model) {
		Workflow workflow = workflowService.getById(id);
		model.addAttribute("workflow", workflow);
		model.addAttribute("sourceTypes", DocumentIOType.values());
		model.addAttribute("targetTypes", DocumentIOType.values());
		model.addAttribute("shareTypes", ShareType.values());
		model.addAttribute("signTypes", Arrays.asList(SignType.values()));
        return "admin/workflows/update";
    }
	
    @PostMapping(value = "/update")
    public String update(@ModelAttribute("authUserId") Long authUserId,
						 @Valid Workflow workflow,
						 @RequestParam(value = "types", required = false) String[] types,
						 @RequestParam(required = false) List<String> managers) {
		User authUser = userService.getUserById(authUserId);
		Workflow updateWorkflow = workflowService.update(workflow, authUser, types, managers);
        return "redirect:/admin/workflows/update/" + updateWorkflow.getId();
    }

    @DeleteMapping(value = "/{id}", produces = "text/html")
    public String delete(@ModelAttribute("authUserId") Long authUserId, @PathVariable("id") Long id) {
    	Workflow workflow = workflowService.getById(id);
		workflowService.delete(workflow);
        return "redirect:/admin/workflows";
    }

	@PostMapping(value = "/add-step/{id}")
	public String addStep(@ModelAttribute("authUserId") Long authUserId, @PathVariable("id") Long id,
						  @RequestParam("signType") String signType,
						  @RequestParam(name="description", required = false) String description,
						  @RequestParam(value = "recipientsEmails", required = false) String[] recipientsEmails,
						  @RequestParam(name="changeable", required = false) Boolean changeable,
						  @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete) {
		Workflow workflow = workflowService.getById(id);
		workflowStepService.addStep(workflow, signType, description, recipientsEmails, changeable, allSignToComplete);
		return "redirect:/admin/workflows/" + workflow.getName();
	}

	@GetMapping(value = "/update-step/{id}/{step}")
	public String changeStepSignType(@ModelAttribute("authUserId") Long authUserId,
									 @PathVariable("id") Long id,
									 @PathVariable("step") Integer step,
									 @RequestParam(name="signType") SignType signType,
									 @RequestParam(name="description") String description,
									 @RequestParam(name="changeable", required = false) Boolean changeable,
									 @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete) {
		Workflow workflow = workflowService.getById(id);
		workflowStepService.updateStep(workflow.getWorkflowSteps().get(step), signType, description, changeable, allSignToComplete);
		return "redirect:/admin/workflows/" + workflow.getName();
	}

	@DeleteMapping(value = "/remove-step-recipent/{id}/{workflowStepId}")
	public String removeStepRecipient(@ModelAttribute("authUserId") Long authUserId, @PathVariable("id") Long id,
									  @PathVariable("workflowStepId") Long workflowStepId,
									  @RequestParam(value = "userId") Long userId, RedirectAttributes redirectAttributes) {
		Workflow workflow = workflowService.getById(id);
		WorkflowStep workflowStep = workflowStepService.removeStepRecipient(workflowStepId, userId);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Participant supprimé"));
		return "redirect:/admin/workflows/" + workflow.getName() + "#" + workflowStep.getId();
	}

	@PostMapping(value = "/add-step-recipents/{id}/{workflowStepId}")
	public String addStepRecipient(@ModelAttribute("authUserId") Long authUserId,
								   @PathVariable("id") Long id,
								   @PathVariable("workflowStepId") Long workflowStepId,
								   @RequestParam String recipientsEmails, RedirectAttributes redirectAttributes) {
		Workflow workflow = workflowService.getById(id);
		WorkflowStep workflowStep = workflowStepService.addStepRecipients(workflowStepId, recipientsEmails);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Participant ajouté"));
		return "redirect:/admin/workflows/" + workflow.getName() + "#" + workflowStep.getId();
	}

	@DeleteMapping(value = "/remove-step/{id}/{stepNumber}")
	public String addStep(@ModelAttribute("authUserId") Long authUserId,
						  @PathVariable("id") Long id,
						  @PathVariable("stepNumber") Integer stepNumber) {
		Workflow workflow = workflowService.getById(id);
		workflowStepService.removeStep(workflow, stepNumber);
		return "redirect:/admin/workflows/" + workflow.getName();
	}

	@PostMapping(value = "/add-params/{id}")
	public String addParams(@ModelAttribute("authUserId") Long authUserId,
							@PathVariable("id") Long id) {
		User authUser = userService.getUserById(authUserId);
		Workflow workflow = workflowService.getById(id);
		workflowService.setUpdateByAndUpdateDate(workflow, authUser.getEppn());
		return "redirect:/admin/workflows/" + workflow.getName();
	}

	@GetMapping(value = "/get-files-from-source/{id}")
	public String getFileFromSource(@ModelAttribute("authUserId") Long authUserId, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
		User authUser = userService.getUserById(authUserId);
		Workflow workflow = workflowService.getById(id);
		int nbImportedFiles = workflowService.importFilesFromSource(workflow, authUser, authUser);
		if(nbImportedFiles == 0) {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Aucun fichier à importer"));
		} else {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("info", nbImportedFiles + " ficher(s) importé(s)"));
		}
		return "redirect:/admin/workflows/" + workflow.getName();
	}

}
