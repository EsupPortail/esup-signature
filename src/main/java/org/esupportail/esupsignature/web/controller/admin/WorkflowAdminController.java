package org.esupportail.esupsignature.web.controller.admin;

import org.esupportail.esupsignature.entity.*;
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

	@Resource
	private UserShareRepository userShareRepository;

	@GetMapping(produces = "text/html")
	public String list(@RequestParam(name = "displayWorkflowType", required = false) String displayWorkflowType, Model model) {
		List<Workflow> workflows = new ArrayList<>();
		if("system".equals(displayWorkflowType) || displayWorkflowType == null) {
			displayWorkflowType = "system";
			workflows.addAll(workflowService.getWorkflowsBySystemUser());
		} else if("classes".equals(displayWorkflowType)) {
			workflows.addAll(workflowService.getClassesWorkflows());
		} else if("all".equals(displayWorkflowType)) {
			workflows.addAll(workflowService.getAllWorkflows());
		} else if("users".equals(displayWorkflowType)) {
			workflows.addAll(workflowService.getAllWorkflows());
			workflows.removeAll(workflowService.getClassesWorkflows());
			workflows.removeAll(workflowService.getWorkflowsBySystemUser());
		}
		model.addAttribute("displayWorkflowType", displayWorkflowType);
		model.addAttribute("workflows", workflows);
		return "admin/workflows/list";
	}

	@GetMapping(value = "/{name}", produces = "text/html")
	public String show(@PathVariable("name") String name, Model model, RedirectAttributes redirectAttributes) {
		model.addAttribute("fromAdmin", true);
		model.addAttribute("signTypes", SignType.values());
		Workflow workflow = workflowRepository.findByName(name);
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

    @GetMapping(value = "/{id}", params = "form")
    public String updateForm(@ModelAttribute("user") User user, @PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
		Workflow workflow = workflowRepository.findById(id).get();
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
		workflowToUpdate.getAuthorizedShareTypes().clear();
		List<ShareType> shareTypes = new ArrayList<>();
		if(types != null) {
			for (String type : types) {
				ShareType shareType = ShareType.valueOf(type);
				workflowToUpdate.getAuthorizedShareTypes().add(shareType);
				shareTypes.add(shareType);
			}
		}
		List<UserShare> userShares = userShareRepository.findByWorkflowId(workflowToUpdate.getId());
		for(UserShare userShare : userShares) {
			userShare.getShareTypes().removeIf(shareType -> !shareTypes.contains(shareType));
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
    public String delete(@ModelAttribute("user") User user, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) {

    	Workflow workflow = workflowRepository.findById(id).get();
//		if (!workflowService.checkUserManageRights(user, workflow)) {
//			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", Non autorisé");
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
		WorkflowStep workflowStep = workflowService.createWorkflowStep("", allSignToComplete, SignType.valueOf(signType), recipientsEmails);
		workflowStep.setDescription(description);
		workflowStep.setChangeable(changeable);
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
		if(user.equals(workflow.getCreateBy()) || workflow.getCreateBy().equals(userService.getSystemUser())) {
			WorkflowStep workflowStep = workflow.getWorkflowSteps().get(step);
			workflowService.changeSignType(workflowStep, null, signType);
			workflowStep.setDescription(description);
			workflowStep.setChangeable(changeable);
			workflowStep.setAllSignToComplete(allSignToComplete);
			return "redirect:/admin/workflows/" + workflow.getName();
		}
		return "redirect:/admin/workflows/";
	}

	@DeleteMapping(value = "/remove-step-recipent/{id}/{workflowStepId}")
	public String removeStepRecipient(@ModelAttribute("user") User user, @PathVariable("id") Long id,
									  @PathVariable("workflowStepId") Long workflowStepId,
									  @RequestParam(value = "userId") Long userId, RedirectAttributes redirectAttributes) {
		Workflow workflow = workflowRepository.findById(id).get();
		WorkflowStep workflowStep = workflowStepRepository.findById(workflowStepId).get();
		if(user.equals(workflow.getCreateBy()) || userService.getSystemUser().equals(workflow.getCreateBy())) {
			User recipientToRemove = userRepository.findById(userId).get();
			workflowStep.getUsers().remove(recipientToRemove);
		} else {
			logger.warn(user.getEppn() + " try to move " + workflow.getId() + " without rights");
		}
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Participant supprimé"));
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
		if(user.equals(workflow.getCreateBy()) || workflow.getCreateBy().equals(userService.getSystemUser())) {
			workflowService.addRecipientsToWorkflowStep(workflowStep, recipientsEmails);
		} else {
			logger.warn(user.getEppn() + " try to update " + workflow.getId() + " without rights");
		}
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Participant ajouté"));
		return "redirect:/admin/workflows/" + workflow.getName() + "#" + workflowStep.getId();
	}

	@DeleteMapping(value = "/remove-step/{id}/{stepNumber}")
	public String addStep(@ModelAttribute("user") User user,
						  @PathVariable("id") Long id,
						  @PathVariable("stepNumber") Integer stepNumber) {
		Workflow workflow = workflowRepository.findById(id).get();
		WorkflowStep workflowStep = workflow.getWorkflowSteps().get(stepNumber);
		workflow.getWorkflowSteps().remove(workflowStep);
		workflowRepository.save(workflow);
		workflowStepRepository.delete(workflowStep);
		return "redirect:/admin/workflows/" + workflow.getName();
	}

	@PostMapping(value = "/add-params/{id}")
	public String addParams(@ModelAttribute("user") User user,
							@PathVariable("id") Long id,
			RedirectAttributes redirectAttributes) {
		Workflow workflow = workflowRepository.findById(id).get();
		if (!workflow.getCreateBy().equals(user)) {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Accès refusé"));
			return "redirect:/admin/workflows/" + workflow.getName();
		}
		workflow.setUpdateBy(user.getEppn());
		workflow.setUpdateDate(new Date());
		return "redirect:/admin/workflows/" + workflow.getName();
	}

	@GetMapping(value = "/get-files-from-source/{id}")
	public String getFileFromSource(@ModelAttribute("user") User user, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) throws Exception {
		Workflow workflow = workflowRepository.findById(id).get();
		int nbImportedFiles = workflowService.importFilesFromSource(workflow, user);
		if(nbImportedFiles == 0) {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Aucun fichier à importer"));
		} else {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("info", nbImportedFiles + " ficher(s) importé(s)"));
		}
		return "redirect:/admin/workflows/" + workflow.getName();
	}

}
