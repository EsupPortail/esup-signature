package org.esupportail.esupsignature.web.controller.admin;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.DisplayWorkflowType;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.*;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.esupportail.esupsignature.web.controller.ws.json.JsonMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RequestMapping("/admin/workflows")
@Controller
@Transactional
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
	public String create(@ModelAttribute("user") User user, @RequestParam(name = "title") String title, @RequestParam(name = "description") String description, RedirectAttributes redirectAttributes) {
		Workflow workflow;
		try {
			workflow = workflowService.createWorkflow(title, description, userService.getSystemUser(),false);
		} catch (EsupSignatureException e) {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Un circuit porte déjà ce nom"));
			return "redirect:/admin/workflows/";
		}
		return "redirect:/admin/workflows/" + workflow.getName();
	}

    @GetMapping(value = "/update/{id}")
    public String updateForm(@ModelAttribute("user") User user, @PathVariable("id") Long id, Model model) {
		Workflow workflow = workflowService.getWorkflowById(id);
		model.addAttribute("workflow", workflow);
		model.addAttribute("sourceTypes", DocumentIOType.values());
		model.addAttribute("targetTypes", DocumentIOType.values());
		model.addAttribute("shareTypes", ShareType.values());
		model.addAttribute("signTypes", Arrays.asList(SignType.values()));
        return "admin/workflows/update";
    }
	
    @PostMapping(value = "/update")
    public String update(@ModelAttribute("user") User user,
						 @Valid Workflow workflow,
						 @RequestParam(value = "types", required = false) String[] types,
						 @RequestParam(required = false) List<String> managers) {
		Workflow updateWorkflow = workflowService.update(workflow, user, types, managers);
        return "redirect:/admin/workflows/" + updateWorkflow.getName();
    }

    @DeleteMapping(value = "/{id}", produces = "text/html")
    public String delete(@ModelAttribute("user") User user, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) {

    	Workflow workflow = workflowService.getWorkflowById(id);
//		if (!workflowService.checkUserManageRights(user, workflow)) {
//			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", Non autorisé");
//			return "redirect:/admin/workflows/" + id;
//		}
		workflowService.delete(workflow);
        return "redirect:/admin/workflows";
    }

	@PostMapping(value = "/add-step/{id}")
	public String addStep(@ModelAttribute("user") User user, @PathVariable("id") Long id,
						  @RequestParam("signType") String signType,
						  @RequestParam(name="description", required = false) String description,
						  @RequestParam(value = "recipientsEmails", required = false) String[] recipientsEmails,
						  @RequestParam(name="changeable", required = false) Boolean changeable,
						  @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete) throws EsupSignatureUserException {
		Workflow workflow = workflowService.getWorkflowById(id);
		workflowService.addStep(signType, description, recipientsEmails, changeable, allSignToComplete, workflow);
		return "redirect:/admin/workflows/" + workflow.getName();
	}

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
		return "redirect:/admin/workflows/" + workflow.getName();
	}

	@DeleteMapping(value = "/remove-step-recipent/{id}/{workflowStepId}")
	public String removeStepRecipient(@ModelAttribute("user") User user, @PathVariable("id") Long id,
									  @PathVariable("workflowStepId") Long workflowStepId,
									  @RequestParam(value = "userId") Long userId, RedirectAttributes redirectAttributes) {
		Workflow workflow = workflowService.getWorkflowById(id);
		WorkflowStep workflowStep = workflowService.removeStepRecipient(user, workflowStepId, userId, workflow);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Participant supprimé"));
		return "redirect:/admin/workflows/" + workflow.getName() + "#" + workflowStep.getId();
	}

	@PostMapping(value = "/add-step-recipents/{id}/{workflowStepId}")
	public String addStepRecipient(@ModelAttribute("user") User user,
								   @PathVariable("id") Long id,
								   @PathVariable("workflowStepId") Long workflowStepId,
								   @RequestParam String recipientsEmails, RedirectAttributes redirectAttributes, HttpServletRequest httpServletRequest) {
		Workflow workflow = workflowService.getWorkflowById(id);
		WorkflowStep workflowStep = workflowService.addStepRecipients(user, workflowStepId, recipientsEmails, workflow);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Participant ajouté"));
		return "redirect:/admin/workflows/" + workflow.getName() + "#" + workflowStep.getId();
	}

	@DeleteMapping(value = "/remove-step/{id}/{stepNumber}")
	public String addStep(@ModelAttribute("user") User user,
						  @PathVariable("id") Long id,
						  @PathVariable("stepNumber") Integer stepNumber) {
		Workflow workflow = workflowService.getWorkflowById(id);
		workflowService.removeStep(stepNumber, workflow);
		return "redirect:/admin/workflows/" + workflow.getName();
	}

	@PostMapping(value = "/add-params/{id}")
	public String addParams(@ModelAttribute("user") User user,
							@PathVariable("id") Long id,
			RedirectAttributes redirectAttributes) {
		Workflow workflow = workflowService.getWorkflowById(id);
		if (!workflow.getCreateBy().equals(user)) {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Accès refusé"));
			return "redirect:/admin/workflows/" + workflow.getName();
		}
		workflowService.setUpdateByAndUpdateDate(workflow, user.getEppn());
		return "redirect:/admin/workflows/" + workflow.getName();
	}

	@GetMapping(value = "/get-files-from-source/{id}")
	public String getFileFromSource(@ModelAttribute("user") User user, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) throws Exception {
		Workflow workflow = workflowService.getWorkflowById(id);
		int nbImportedFiles = workflowService.importFilesFromSource(workflow, user);
		if(nbImportedFiles == 0) {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Aucun fichier à importer"));
		} else {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("info", nbImportedFiles + " ficher(s) importé(s)"));
		}
		return "redirect:/admin/workflows/" + workflow.getName();
	}

}
