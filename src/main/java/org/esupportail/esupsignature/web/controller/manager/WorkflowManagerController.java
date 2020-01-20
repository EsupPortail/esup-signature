package org.esupportail.esupsignature.web.controller.manager;

import com.google.common.collect.ImmutableList;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.repository.*;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.esupportail.esupsignature.service.fs.FsFile;
import org.esupportail.esupsignature.service.pdf.PdfParameters;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RequestMapping("/manager/workflows")
@Controller
@Transactional
@Scope(value = "session")
public class WorkflowManagerController {

	private static final Logger logger = LoggerFactory.getLogger(WorkflowManagerController.class);
	
	@ModelAttribute("managerMenu")
	public String getActiveMenu() {
		return "active";
	}

	@Resource
	private UserRepository userRepository;
	
	@Resource
	private UserService userService;
	
	@Resource
	private WorkflowRepository workflowRepository;

	@Resource
	private WorkflowStepRepository workflowStepRepository;

	@Resource
	private WorkflowService workflowService;

	@Resource
	private SignRequestRepository signRequestRepository;
	
	@Resource
	private SignRequestService signRequestService;
	
	@Resource
	private SignRequestParamsRepository signRequestParamsRepository; 

	@Resource
	private PdfService pdfService;

	@ModelAttribute("user")
	public User getUser() {
		return userService.getUserFromAuthentication();
	}


	@RequestMapping(value = "/{id}", produces = "text/html")
	public String show(@PathVariable("id") Long id, Model uiModel) throws IOException {
		Workflow workflow = workflowRepository.findById(id).get();
		workflowService.setWorkflowsLabels(workflow.getWorkflowSteps());
		uiModel.addAttribute("workflow", workflow);
		uiModel.addAttribute("signTypes", SignType.values());
		uiModel.addAttribute("itemId", id);
		return "manager/workflows/show";
	}

	void populateEditForm(Model uiModel, Workflow workflow) {
		uiModel.addAttribute("workflow", workflow);
		uiModel.addAttribute("users", userRepository.findAll());
		uiModel.addAttribute("sourceTypes", Arrays.asList(DocumentIOType.values()));
		uiModel.addAttribute("targetTypes", Arrays.asList(DocumentIOType.values()));
		uiModel.addAttribute("signTypes", Arrays.asList(SignType.values()));
		uiModel.addAttribute("signrequests", signRequestRepository.findAll());
	}

	@RequestMapping(produces = "text/html")
	public String list(@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size,
			@RequestParam(value = "sortFieldName", required = false) String sortFieldName,
			@RequestParam(value = "sortOrder", required = false) String sortOrder, Model uiModel) {
		User user = userService.getUserFromAuthentication();
    	if(sortFieldName == null) {
    		sortFieldName = "workflowType";
    	}

    	List<Workflow> workflows = ImmutableList.copyOf(workflowRepository.findAll());

		for (Workflow workflow : workflows) {
			workflowService.setWorkflowsLabels(workflow.getWorkflowSteps());
		}
		uiModel.addAttribute("workflows", workflows);
		return "manager/workflows/list";
	}

	@PostMapping(produces = "text/html")
	public String create(@RequestParam(name = "name") String name,
						 Model uiModel, RedirectAttributes redirectAttrs) {
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
		populateEditForm(uiModel, workflow);
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

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = "text/html")
    public String delete(@PathVariable("id") Long id, @RequestParam(value = "page", required = false) Integer page, @RequestParam(value = "size", required = false) Integer size, Model uiModel, RedirectAttributes redirectAttrs) {
    	User user = userService.getUserFromAuthentication();
    	Workflow workflow = workflowRepository.findById(id).get();
		populateEditForm(uiModel, workflow);
		if (!workflowService.checkUserManageRights(user, workflow)) {
			redirectAttrs.addFlashAttribute("messageCustom", "Non autorisé");
			return "redirect:/manager/workflows/" + id;
		}
		workflowService.deleteWorkflow(workflow);
        uiModel.asMap().clear();
        uiModel.addAttribute("page", (page == null) ? "1" : page.toString());
        uiModel.addAttribute("size", (size == null) ? "10" : size.toString());
        return "redirect:/manager/workflows";
    }
	
	@RequestMapping(value = "/update-params/{id}", method = RequestMethod.POST)
	public String updateParams(@PathVariable("id") Long id, @RequestParam(value = "xPos", required = true) int xPos,
			@RequestParam(value = "yPos", required = true) int yPos,
			@RequestParam(value = "signPageNumber", required = true) int signPageNumber,
			RedirectAttributes redirectAttrs, HttpServletResponse response, Model model) {
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

	@RequestMapping(value = "/delete-params/{id}", method = RequestMethod.POST)
	public String deleteParams(@PathVariable("id") Long id,
			@RequestParam(value = "workflowId", required = true) Long workflowId,
			RedirectAttributes redirectAttrs, HttpServletResponse response, Model model) {
		User user = userService.getUserFromAuthentication();
		SignRequestParams signRequestParams = signRequestParamsRepository.findById(id).get(); 
		Workflow workflow = workflowRepository.findById(workflowId).get();
		if (!workflow.getCreateBy().equals(user.getEppn())) {
			redirectAttrs.addFlashAttribute("messageCustom", "access error");
			return "redirect:/manager/workflows/" + id;
		}
		//TODO with workflowsteps
		
		return "redirect:/manager/workflows/" + workflowId;
	}

	@PostMapping(value = "/add-step/{id}")
	public String addStep(@PathVariable("id") Long id,
						  @RequestParam("recipientsEmail") List<String> recipientsEmail,
						  @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete,
						  @RequestParam("signType") String signType,
						  RedirectAttributes redirectAttrs) {
		User user = userService.getUserFromAuthentication();
		Workflow workflow = workflowRepository.findById(id).get();
		if (!workflow.getCreateBy().equals(user.getEppn())) {
			redirectAttrs.addFlashAttribute("messageCustom", "access error");
			return "redirect:/manager/workflows/" + id;
		}
		WorkflowStep workflowStep = new WorkflowStep();
		for(String recipientEmail : recipientsEmail) {
			if(userRepository.countByEmail(recipientEmail) > 0) {
				User recipientUserToAdd = userRepository.findByEmail(recipientEmail).get(0);
				workflowStep.getRecipients().put(recipientUserToAdd.getId(), false);
			}
		}
		if(allSignToComplete ==null) {
			workflowStep.setAllSignToComplete(false);
		} else {
			workflowStep.setAllSignToComplete(allSignToComplete);
		}
		workflowStep.setSignType(SignType.valueOf(signType));
		workflowStepRepository.save(workflowStep);
		workflow.getWorkflowSteps().add(workflowStep);
		return "redirect:/manager/workflows/" + id;
	}

	@DeleteMapping(value = "/remove-step-recipent/{id}/{workflowStepId}")
	public String removeStepRecipient(@PathVariable("id") Long id,
									  @PathVariable("workflowStepId") Long workflowStepId,
									  @RequestParam(value = "recipientEmail") String recipientEmail,
									  RedirectAttributes redirectAttrs, HttpServletRequest request) {
		User user = userService.getUserFromAuthentication();
		user.setIp(request.getRemoteAddr());
		Workflow workflow = workflowRepository.findById(id).get();
		WorkflowStep workflowStep = workflowStepRepository.findById(workflowStepId).get();
		if(user.getEppn().equals(workflow.getCreateBy())) {
			User recipientUserToRemove = userRepository.findByEmail(recipientEmail).get(0);
			workflowStep.getRecipients().remove(recipientUserToRemove.getId());
			workflowStepRepository.save(workflowStep);
		} else {
			logger.warn(user.getEppn() + " try to move " + workflow.getId() + " without rights");
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
