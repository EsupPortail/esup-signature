package org.esupportail.esupsignature.web.controller.manager;

import org.esupportail.esupsignature.entity.Recipient;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.repository.*;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.esupportail.esupsignature.service.fs.FsFile;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RequestMapping("/manager/workflows")
@Controller
@Transactional
public class WorkflowManagerController {

	private static final Logger logger = LoggerFactory.getLogger(WorkflowManagerController.class);
	
	@ModelAttribute("managerMenu")
	public String getManagerMenu() {
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

	@ModelAttribute("user")
	public User getUser() {
		return userService.getUserFromAuthentication();
	}


	@GetMapping(value = "/{id}", produces = "text/html")
	public String show(@PathVariable("id") Long id, Model uiModel) {
		Workflow workflow = workflowRepository.findById(id).get();
		uiModel.addAttribute("workflow", workflow);
		uiModel.addAttribute("signTypes", SignType.values());
		uiModel.addAttribute("itemId", id);
		return "manager/workflows/show";
	}

	@GetMapping(produces = "text/html")
	public String list(Model model) {
		User user = userService.getUserFromAuthentication();
    	List<Workflow> workflows = new ArrayList<>();
    	workflows.addAll(workflowService.getWorkflows());
    	workflows.addAll(workflowRepository.findAll());
		model.addAttribute("workflows", workflows);
		return "manager/workflows/list";
	}

	@PostMapping(produces = "text/html")
	public String create(@RequestParam(name = "name") String name, RedirectAttributes redirectAttrs) {
		User user = userService.getUserFromAuthentication();
		Workflow newWorkflow = new Workflow();
		newWorkflow.setName(name);
		Workflow workflow;
		try {
			workflow = workflowService.createWorkflow(name, user,false);
		} catch (EsupSignatureException e) {
			redirectAttrs.addAttribute("messageError", "Ce parapheur existe déjà");
			return "redirect:/manager/workflows/";
		}
		return "redirect:/manager/workflows/" + workflow.getId();
	}

    @RequestMapping(value = "/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable("id") Long id, Model uiModel, RedirectAttributes redirectAttrs) {
		User user = userService.getUserFromAuthentication();
		Workflow workflow = workflowRepository.findById(id).get();
		if (!workflowService.checkUserManageRights(user, workflow)) {
			redirectAttrs.addFlashAttribute("messageCustom", "access error");
			return "redirect:/manager/workflows/" + id;
		}
		uiModel.addAttribute("workflow", workflow);
		uiModel.addAttribute("users", userRepository.findAll());
		uiModel.addAttribute("sourceTypes", Arrays.asList(DocumentIOType.values()));
		uiModel.addAttribute("targetTypes", Arrays.asList(DocumentIOType.values()));
		uiModel.addAttribute("signTypes", Arrays.asList(SignType.values()));
		uiModel.addAttribute("signrequests", signRequestRepository.findAll());
        return "manager/workflows/update";
    }
	
    @PostMapping(value = "/update/{id}")
    public String update(@PathVariable("id") Long id, @Valid Workflow workflow, @RequestParam(required = false) List<String> managers) {
		User user = userService.getUserFromAuthentication();
		Workflow workflowToUpdate = workflowRepository.findById(workflow.getId()).get();
		if(workflowToUpdate.getCreateBy().equals(user.getEppn())) {
			if(managers != null && managers.size() > 0) {
				workflowToUpdate.getManagers().clear();
				for(String manager : managers) {
					User managerUser = userService.getUser(manager);
					if(!workflowToUpdate.getManagers().contains(managerUser.getEmail())) {
						workflowToUpdate.getManagers().add(managerUser.getEmail());
					}
				}
			}
			workflowToUpdate.setSourceType(workflow.getSourceType());
			workflowToUpdate.setTargetType(workflow.getTargetType());
			workflowToUpdate.setDocumentsSourceUri(workflow.getDocumentsSourceUri());
			workflowToUpdate.setDocumentsTargetUri(workflow.getDocumentsTargetUri());
			workflowToUpdate.setDescription(workflow.getDescription());
			workflowToUpdate.setUpdateBy(user.getEppn());
			workflowToUpdate.setUpdateDate(new Date());
			workflowRepository.save(workflowToUpdate);
		}
        return "redirect:/manager/workflows/" + workflow.getId();

    }

    @DeleteMapping(value = "/{id}", produces = "text/html")
    public String delete(@PathVariable("id") Long id, RedirectAttributes redirectAttrs) {
    	User user = userService.getUserFromAuthentication();
    	Workflow workflow = workflowRepository.findById(id).get();
		if (!workflowService.checkUserManageRights(user, workflow)) {
			redirectAttrs.addFlashAttribute("messageCustom", "Non autorisé");
			return "redirect:/manager/workflows/" + id;
		}
		workflowRepository.delete(workflow);
        return "redirect:/manager/workflows";
    }

	@PostMapping(value = "/add-step/{id}")
	public String addStep(@PathVariable("id") Long id,
						  @RequestParam("recipientsEmails") String[] recipientsEmails,
						  @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete,
						  @RequestParam("signType") String signType,
						  RedirectAttributes redirectAttrs) {
		User user = userService.getUserFromAuthentication();
		Workflow workflow = workflowRepository.findById(id).get();
		if (!workflow.getCreateBy().equals(user.getEppn())) {
			redirectAttrs.addFlashAttribute("messageCustom", "access error");
			return "redirect:/manager/workflows/" + id;
		}
		WorkflowStep workflowStep = workflowService.createWorkflowStep("", allSignToComplete, SignType.valueOf(signType), recipientsEmails);
		workflow.getWorkflowSteps().add(workflowStep);
		return "redirect:/manager/workflows/" + id;
	}

	@DeleteMapping(value = "/remove-step-recipent/{id}/{workflowStepId}")
	public String removeStepRecipient(@PathVariable("id") Long id,
									  @PathVariable("workflowStepId") Long workflowStepId,
									  @RequestParam(value = "recipientId") Long recipientId, HttpServletRequest request) {
		User user = userService.getUserFromAuthentication();
		user.setIp(request.getRemoteAddr());
		Workflow workflow = workflowRepository.findById(id).get();
		WorkflowStep workflowStep = workflowStepRepository.findById(workflowStepId).get();
		if(user.getEppn().equals(workflow.getCreateBy())) {
			Recipient recipientToRemove = recipientRepository.findById(recipientId).get();
			workflowStep.getRecipients().remove(recipientToRemove);
		} else {
			logger.warn(user.getEppn() + " try to move " + workflow.getId() + " without rights");
		}
		return "redirect:/manager/workflows/" + id + "#" + workflowStep.getId();
	}

	@PostMapping(value = "/add-step-recipents/{id}/{workflowStepId}")
	public String addStepRecipient(@PathVariable("id") Long id,
									  @PathVariable("workflowStepId") Long workflowStepId,
									  @RequestParam String recipientsEmails,
									  RedirectAttributes redirectAttrs, HttpServletRequest request) {
		User user = userService.getUserFromAuthentication();
		user.setIp(request.getRemoteAddr());
		Workflow workflow = workflowRepository.findById(id).get();
		WorkflowStep workflowStep = workflowStepRepository.findById(workflowStepId).get();
		if(user.getEppn().equals(workflow.getCreateBy())) {
			workflowService.addRecipientsToWorkflowStep(workflowStep, recipientsEmails);
		} else {
			logger.warn(user.getEppn() + " try to update " + workflow.getId() + " without rights");
		}
		return "redirect:/manager/workflows/" + id + "#" + workflowStep.getId();
	}


	@RequestMapping(value = "/toggle-need-all-sign/{id}/{step}", method = RequestMethod.GET)
	public String toggleNeedAllSign(@PathVariable("id") Long id,@PathVariable("step") Integer step) {
		User user = userService.getUserFromAuthentication();
		Workflow workflow = workflowRepository.findById(id).get();
		if(user.getEppn().equals(workflow.getCreateBy())) {
			Long stepId = workflowService.toggleNeedAllSign(workflow, step);
			return "redirect:/manager/workflows/" + id + "#" + stepId;
		}
		return "redirect:/manager/workflows/";
	}

	@RequestMapping(value = "/change-step-sign-type/{id}/{step}", method = RequestMethod.GET)
	public String changeStepSignType(@PathVariable("id") Long id, @PathVariable("step") Integer step, @RequestParam(name="signType") SignType signType) {
		User user = userService.getUserFromAuthentication();
		Workflow workflow = workflowRepository.findById(id).get();
		if(user.getEppn().equals(workflow.getCreateBy())) {
			workflowService.changeSignType(workflow, step, null, signType);
			return "redirect:/manager/workflows/" + id;
		}
		return "redirect:/manager/workflows/";
	}

	@DeleteMapping(value = "/remove-step/{id}/{stepNumber}")
	public String addStep(@PathVariable("id") Long id, @PathVariable("stepNumber") Integer stepNumber, RedirectAttributes redirectAttrs) {
		User user = userService.getUserFromAuthentication();
		Workflow workflow = workflowRepository.findById(id).get();
		if (!workflow.getCreateBy().equals(user.getEppn())) {
			redirectAttrs.addFlashAttribute("messageCustom", "access error");
			return "redirect:/manager/workflows/" + id;
		}
		WorkflowStep workflowStep = workflow.getWorkflowSteps().get(stepNumber);
		workflow.getWorkflowSteps().remove(workflowStep);
		workflowRepository.save(workflow);
		workflowStepRepository.delete(workflowStep);
		return "redirect:/manager/workflows/" + id;
	}

	@RequestMapping(value = "/add-params/{id}", method = RequestMethod.POST)
	public String addParams(@PathVariable("id") Long id, 
			RedirectAttributes redirectAttrs) {
		User user = userService.getUserFromAuthentication();
		Workflow workflow = workflowRepository.findById(id).get();

		if (!workflow.getCreateBy().equals(user.getEppn())) {
			redirectAttrs.addFlashAttribute("messageCustom", "access error");
			return "redirect:/manager/workflows/" + id;
		}
		workflow.setUpdateBy(user.getEppn());
		workflow.setUpdateDate(new Date());
		return "redirect:/manager/workflows/" + id;
	}

	@RequestMapping(value = "/get-files-from-source/{id}", produces = "text/html")
	public String getFileFromSource(@PathVariable("id") Long id, RedirectAttributes redirectAttrs) throws Exception {
		User user = userService.getUserFromAuthentication();
		Workflow workflow = workflowRepository.findById(id).get();

		if (!workflow.getCreateBy().equals(user.getEppn())) {
			redirectAttrs.addFlashAttribute("messageCustom", "access error");
			return "redirect:/manager/workflows/" + id;
		}
		List<FsFile> fsFiles = workflowService.importFilesFromSource(workflow, user);
		if(fsFiles.size() == 0) {
			redirectAttrs.addFlashAttribute("messageError", "Aucun fichier à importer");
		} else {
			redirectAttrs.addFlashAttribute("messageInfo", fsFiles.size() + " ficher(s) importé(s)");
		}
		return "redirect:/manager/workflows/" + id;
	}

}
