package org.esupportail.esupsignature.web.controller.admin;

import org.esupportail.esupsignature.entity.Recipient;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.*;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	private UserRepository userRepository;

	@Resource
	private UserService userService;

	@Resource
	private RecipientRepository recipientRepository;

	@Resource
	private WorkflowRepository workflowRepository;

	@Resource
	private WorkflowStepRepository workflowStepRepository;

	@Resource
	private WorkflowService workflowService;

	@Resource
	private SignRequestRepository signRequestRepository;

	@GetMapping(produces = "text/html")
	public String list(@RequestParam(name = "displayWorkflowType", required = false) String displayWorkflowType, Model model) {
		List<Workflow> workflows = new ArrayList<>();
		if("system".equals(displayWorkflowType)) {
			User systemUser = userService.getSystemUser();
			workflows.addAll(workflowService.getWorkflowsForUser(systemUser, systemUser));
		} else if("classes".equals(displayWorkflowType)) {
			workflows.addAll(workflowService.getClassesWorkflows());
		} else {
			workflows.addAll(workflowService.getAllWorkflows());
		}
		model.addAttribute("displayWorkflowType", displayWorkflowType);
		model.addAttribute("workflows", workflows);
		return "admin/workflows/list";
	}

	@GetMapping(value = "/{name}", produces = "text/html")
	public String show(@PathVariable("name") String name, Model uiModel, RedirectAttributes redirectAttributes) {
		uiModel.addAttribute("signTypes", SignType.values());
		List<Workflow> workflows = workflowRepository.findByName(name);
		if(workflows.size() > 0) {
			uiModel.addAttribute("workflow", workflows.get(0));
			return "admin/workflows/show";
		} else {
			Workflow workflow = workflowService.getWorkflowByClassName(name);
			if (workflow != null) {
				uiModel.addAttribute("workflow", workflow);
				return "admin/workflows/show-class";
			}
		}
		redirectAttributes.addFlashAttribute("Workflow introuvable");
		return "redirect:/admin/workflows";
	}

	@PostMapping(produces = "text/html")
	public String create(@ModelAttribute("user") User user, @RequestParam(name = "name") String name, RedirectAttributes redirectAttrs) {
		Workflow newWorkflow = new Workflow();
		newWorkflow.setName(name);
		Workflow workflow;
		try {
			workflow = workflowService.createWorkflow(name, userService.getSystemUser(),false);
		} catch (EsupSignatureException e) {
			redirectAttrs.addFlashAttribute("messageError", "Un circuit porte déjà ce nom");
			return "redirect:/admin/workflows/";
		}
		return "redirect:/admin/workflows/" + workflow.getName();
	}

    @GetMapping(value = "/{id}", params = "form")
    public String updateForm(@ModelAttribute("user") User user, @PathVariable("id") Long id, Model uiModel, RedirectAttributes redirectAttrs) {
		//User user = userService.getCurrentUser();
		Workflow workflow = workflowRepository.findById(id).get();
//		if (!workflowService.checkUserManageRights(user, workflow)) {
//			redirectAttrs.addFlashAttribute("messageCustom", "access error");
//			return "redirect:/admin/workflows/" + workflow.getName();
//		}
		uiModel.addAttribute("workflow", workflow);
		uiModel.addAttribute("users", userRepository.findAll());
		uiModel.addAttribute("sourceTypes", Arrays.asList(DocumentIOType.values()));
		uiModel.addAttribute("targetTypes", Arrays.asList(DocumentIOType.values()));
		uiModel.addAttribute("signTypes", Arrays.asList(SignType.values()));
		uiModel.addAttribute("signrequests", signRequestRepository.findAll());
        return "admin/workflows/update";
    }
	
    @PostMapping(value = "/update/{id}")
    public String update(@ModelAttribute("user") User user,
						 @Valid Workflow workflow,
						 @RequestParam(required = false) List<String> managers) {
		Workflow workflowToUpdate = workflowRepository.findById(workflow.getId()).get();
		if(managers != null && managers.size() > 0) {
			workflowToUpdate.getManagers().clear();
			for(String manager : managers) {
				User managerUser = userService.checkUserByEmail(manager);
				if(!workflowToUpdate.getManagers().contains(managerUser.getEmail())) {
					workflowToUpdate.getManagers().add(managerUser.getEmail());
				}
			}
		} else {
			workflowToUpdate.getManagers().clear();
		}
		workflowToUpdate.setSourceType(workflow.getSourceType());
		workflowToUpdate.setTargetType(workflow.getTargetType());
		workflowToUpdate.setDocumentsSourceUri(workflow.getDocumentsSourceUri());
		workflowToUpdate.setDocumentsTargetUri(workflow.getDocumentsTargetUri());
		workflowToUpdate.setDescription(workflow.getDescription());
		workflowToUpdate.setTitle(workflow.getTitle());
		workflowToUpdate.setPublicUsage(workflow.getPublicUsage());
		workflowToUpdate.setScanPdfMetadatas(workflow.getScanPdfMetadatas());
		workflowToUpdate.setRole(workflow.getRole());
		workflowToUpdate.setUpdateBy(user.getEppn());
		workflowToUpdate.setUpdateDate(new Date());
		workflowRepository.save(workflowToUpdate);
        return "redirect:/admin/workflows/" + workflowToUpdate.getName();

    }

    @DeleteMapping(value = "/{id}", produces = "text/html")
    public String delete(@ModelAttribute("user") User user, @PathVariable("id") Long id, RedirectAttributes redirectAttrs) {
    	//User user = userService.getCurrentUser();
    	Workflow workflow = workflowRepository.findById(id).get();
//		if (!workflowService.checkUserManageRights(user, workflow)) {
//			redirectAttrs.addFlashAttribute("messageError", "Non autorisé");
//			return "redirect:/admin/workflows/" + id;
//		}
		workflowRepository.delete(workflow);
        return "redirect:/admin/workflows";
    }

	@PostMapping(value = "/add-step/{id}")
	public String addStep(@ModelAttribute("user") User user, @PathVariable("id") Long id,
						  @RequestParam("signType") String signType,
						  @RequestParam(name="description", required = false) String description,
						  @RequestParam(value = "recipientsEmails", required = false) String[] recipientsEmails,
						  @RequestParam(name="changeable", required = false) Boolean changeable,
						  @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete) throws EsupSignatureUserException {
		Workflow workflow = workflowRepository.findById(id).get();
		WorkflowStep workflowStep = workflowService.createWorkflowStep("", "workflow", workflow.getId(), allSignToComplete, SignType.valueOf(signType), recipientsEmails);
		workflowStep.setDescription(description);
		workflowStep.setChangeable(changeable);
		workflowStep.setStepNumber(workflow.getWorkflowSteps().size() + 1);
		workflow.getWorkflowSteps().add(workflowStep);
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
		Workflow workflow = workflowRepository.findById(id).get();
		if(user.getEppn().equals(workflow.getCreateBy()) || workflow.getCreateBy().equals("system")) {
			WorkflowStep workflowStep = workflow.getWorkflowSteps().get(step);
			workflowService.changeSignType(workflowStep, null, signType);
			workflowStep.setDescription(description);
			workflowStep.setStepNumber(workflow.getWorkflowSteps().indexOf(workflowStep) + 1);
			workflowStep.setChangeable(changeable);
			workflowStep.setAllSignToComplete(allSignToComplete);
			return "redirect:/admin/workflows/" + workflow.getName();
		}
		return "redirect:/admin/workflows/";
	}

	@DeleteMapping(value = "/remove-step-recipent/{id}/{workflowStepId}")
	public String removeStepRecipient(@ModelAttribute("user") User user, @PathVariable("id") Long id,
									  @PathVariable("workflowStepId") Long workflowStepId,
									  @RequestParam(value = "recipientId") Long recipientId) {
		Workflow workflow = workflowRepository.findById(id).get();
		WorkflowStep workflowStep = workflowStepRepository.findById(workflowStepId).get();
		if(user.getEppn().equals(workflow.getCreateBy()) || "system".equals(workflow.getCreateBy())) {
			Recipient recipientToRemove = recipientRepository.findById(recipientId).get();
			workflowStep.getRecipients().remove(recipientToRemove);
		} else {
			logger.warn(user.getEppn() + " try to move " + workflow.getId() + " without rights");
		}
		return "redirect:/admin/workflows/" + workflow.getName() + "#" + workflowStep.getId();
	}

	@PostMapping(value = "/add-step-recipents/{id}/{workflowStepId}")
	public String addStepRecipient(@ModelAttribute("user") User user,
								   @PathVariable("id") Long id,
								   @PathVariable("workflowStepId") Long workflowStepId,
								   @RequestParam String recipientsEmails, RedirectAttributes redirectAttributes, HttpServletRequest httpServletRequest) throws EsupSignatureUserException {
		user.setIp(httpServletRequest.getRemoteAddr());
		Workflow workflow = workflowRepository.findById(id).get();
		WorkflowStep workflowStep = workflowStepRepository.findById(workflowStepId).get();
		if(user.getEppn().equals(workflow.getCreateBy())) {
			workflowService.addRecipientsToWorkflowStep(workflowStep, recipientsEmails);
		} else {
			logger.warn(user.getEppn() + " try to update " + workflow.getId() + " without rights");
		}
		redirectAttributes.addFlashAttribute("messageInfo", "Participet ajouté");
		return "redirect:/admin/workflows/" + workflow.getName() + "#" + workflowStep.getId();
	}

	@DeleteMapping(value = "/remove-step/{id}/{stepNumber}")
	public String addStep(@ModelAttribute("user") User user,
						  @PathVariable("id") Long id,
						  @PathVariable("stepNumber") Integer stepNumber) {
		Workflow workflow = workflowRepository.findById(id).get();
		WorkflowStep workflowStep = workflow.getWorkflowSteps().get(stepNumber);
		workflow.getWorkflowSteps().remove(workflowStep);
		for(int i = 0; i < workflow.getWorkflowSteps().size(); i++) {
			workflow.getWorkflowSteps().get(i).setStepNumber(i + 1);
		}
		workflowRepository.save(workflow);
		workflowStepRepository.delete(workflowStep);
		return "redirect:/admin/workflows/" + workflow.getName();
	}

	@PostMapping(value = "/add-params/{id}")
	public String addParams(@ModelAttribute("user") User user,
							@PathVariable("id") Long id,
			RedirectAttributes redirectAttrs) {
		Workflow workflow = workflowRepository.findById(id).get();
		if (!workflow.getCreateBy().equals(user.getEppn())) {
			redirectAttrs.addFlashAttribute("messageCustom", "access error");
			return "redirect:/admin/workflows/" + workflow.getName();
		}
		workflow.setUpdateBy(user.getEppn());
		workflow.setUpdateDate(new Date());
		return "redirect:/admin/workflows/" + workflow.getName();
	}

	@GetMapping(value = "/get-files-from-source/{id}")
	public String getFileFromSource(@ModelAttribute("user") User user, @PathVariable("id") Long id, RedirectAttributes redirectAttrs) throws Exception {
		Workflow workflow = workflowRepository.findById(id).get();
		int nbImportedFiles = workflowService.importFilesFromSource(workflow, user);
		if(nbImportedFiles == 0) {
			redirectAttrs.addFlashAttribute("messageError", "Aucun fichier à importer");
		} else {
			redirectAttrs.addFlashAttribute("messageInfo", nbImportedFiles + " ficher(s) importé(s)");
		}
		return "redirect:/admin/workflows/" + workflow.getName();
	}

}
