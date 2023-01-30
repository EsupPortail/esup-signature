package org.esupportail.esupsignature.web.controller.admin;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.Certificat;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.DisplayWorkflowType;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

@RequestMapping("/admin/workflows")
@Controller

public class WorkflowAdminController {

	private static final Logger logger = LoggerFactory.getLogger(WorkflowAdminController.class);

	@Resource
	private SignRequestService signRequestService;

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
	private SignBookService signBookService;

	@Resource
	private WorkflowService workflowService;

	@Resource
	private WorkflowStepService workflowStepService;

	@Resource
	private CertificatService certificatService;

	@GetMapping
	public String list(@RequestParam(name = "displayWorkflowType", required = false) DisplayWorkflowType displayWorkflowType, Model model) {
		if(displayWorkflowType == null) {
			displayWorkflowType = DisplayWorkflowType.system;
		}
		model.addAttribute("displayWorkflowType", displayWorkflowType);
		model.addAttribute("workflows", workflowService.getWorkflowsByDisplayWorkflowType(displayWorkflowType));
		return "admin/workflows/list";
	}

	@GetMapping(value = "/{id}")
	public String show(@PathVariable("id") Long id, Model model) {
		model.addAttribute("fromAdmin", true);
		Workflow workflow = workflowService.getById(id);
		model.addAttribute("workflow", workflow);
		List<Certificat> certificats = certificatService.getAllCertificats();
		model.addAttribute("certificats", certificats);
		return "admin/workflows/show";
	}

	@PostMapping(produces = "text/html")
	public String create(@ModelAttribute("authUserEppn") String authUserEppn, @RequestParam(name = "title", required = false) String title, @RequestParam(name = "description") String description, RedirectAttributes redirectAttributes) {
		if(title == null) {
			title = description;
		}
		Workflow workflow;
		try {
			workflow = workflowService.createWorkflow(title, description, userService.getSystemUser());
		} catch (EsupSignatureException e) {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Un circuit possède déjà ce préfixe"));
			return "redirect:/admin/workflows/";
		}
		return "redirect:/admin/workflows/" + workflow.getId();
	}

    @GetMapping(value = "/update/{id}")
    public String updateForm(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, Model model) {
		Workflow workflow = workflowService.getById(id);
		model.addAttribute("workflow", workflow);
		model.addAttribute("nbWorkflowSignRequests", signBookService.countSignBooksByWorkflow(id));
		model.addAttribute("roles", userService.getAllRoles());
		model.addAttribute("sourceTypes", DocumentIOType.values());
		model.addAttribute("targetTypes", DocumentIOType.values());
		model.addAttribute("shareTypes", ShareType.values());
        return "admin/workflows/update";
    }

    @PostMapping(value = "/update")
    public String update(@ModelAttribute("authUserEppn") String authUserEppn,
						 @Valid Workflow workflow,
						 @RequestParam(value = "types", required = false) String[] types,
						 @RequestParam(required = false) List<String> viewersEmails,
						 @RequestParam(required = false) Set<String> managers) {
		User authUser = userService.getUserByEppn(authUserEppn);
		Workflow updateWorkflow = workflowService.update(workflow, authUser, types, managers);
		workflowService.addViewers(updateWorkflow.getId(), viewersEmails);
        return "redirect:/admin/workflows/update/" + updateWorkflow.getId();
    }

    @DeleteMapping(value = "/{id}", produces = "text/html")
    public String delete(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
    	Workflow workflow = workflowService.getById(id);
		try {
			workflowService.delete(workflow);
		} catch (EsupSignatureException e) {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", e.getMessage()));
		}
		return "redirect:/admin/workflows";
    }

	@PostMapping(value = "/add-step/{id}")
	public String addStep(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
						  @RequestParam("signType") String signType,
						  @RequestParam(name="description", required = false) String description,
						  @RequestParam(value = "recipientsEmails", required = false) String[] recipientsEmails,
						  @RequestParam(name="changeable", required = false) Boolean changeable,
						  @RequestParam(name="maxRecipients", required = false) Integer maxRecipients,
						  @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete,
						  @RequestParam(name="attachmentRequire", required = false) Boolean attachmentRequire) throws EsupSignatureException {
		workflowStepService.addStep(id, signType, description, recipientsEmails, changeable, allSignToComplete, maxRecipients, authUserEppn, false, attachmentRequire, false, null);
		return "redirect:/admin/workflows/" + id;
	}

	@PostMapping(value = "/add-auto-step/{id}")
	public String addAutoStep(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id,
							  @RequestParam(name="description", required = false) String description,
							  @RequestParam(name="certificatId", required = false) Long certificatId
	) throws EsupSignatureException {
		workflowStepService.addStep(id, SignType.certSign.name(), description, null, false, false, 1, userEppn, true, false, true, certificatId);

		return "redirect:/user/workflows/" + id;
	}

	@PostMapping(value = "/update-step/{id}/{step}")
	public String updateStep(@ModelAttribute("authUserEppn") String authUserEppn,
							 @PathVariable("id") Long id,
							 @PathVariable("step") Integer step,
							 @RequestParam(name="signType") SignType signType,
							 @RequestParam(name="description") String description,
							 @RequestParam(name="maxRecipients", required = false) Integer maxRecipients,
							 @RequestParam(name="repeatable", required = false) Boolean repeatable,
							 @RequestParam(name="multiSign", required = false) Boolean multiSign,
							 @RequestParam(name="changeable", required = false) Boolean changeable,
							 @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete,
							 @RequestParam(name="attachmentAlert", required = false) Boolean attachmentAlert,
							 @RequestParam(name="attachmentRequire", required = false) Boolean attachmentRequire,
							 @RequestParam(name="autoSign", required = false) Boolean autoSign,
							 @RequestParam(name="certificatId", required = false) Long certificatId,
							 RedirectAttributes redirectAttributes) {
		Workflow workflow = workflowService.getById(id);
		try {
			workflowStepService.updateStep(workflow.getWorkflowSteps().get(step).getId(), signType, description, changeable, repeatable, multiSign, allSignToComplete, maxRecipients, attachmentAlert, attachmentRequire, autoSign, certificatId);
		} catch (EsupSignatureException e) {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Type de signature impossible pour une étape infinie"));
		}
		return "redirect:/admin/workflows/" + id;
	}

	@DeleteMapping(value = "/remove-step-recipent/{id}/{workflowStepId}")
	public String removeStepRecipient(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
									  @PathVariable("workflowStepId") Long workflowStepId,
									  @RequestParam(value = "userToRemoveEppn") String userToRemoveEppn, RedirectAttributes redirectAttributes) {
		WorkflowStep workflowStep = workflowStepService.removeStepRecipient(workflowStepId, userToRemoveEppn);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Participant supprimé"));
		return "redirect:/admin/workflows/" + id + "#" + workflowStep.getId();
	}

	@PostMapping(value = "/add-step-recipents/{id}/{workflowStepId}")
	public String addStepRecipient(@ModelAttribute("authUserEppn") String authUserEppn,
								   @PathVariable("id") Long id,
								   @PathVariable("workflowStepId") Long workflowStepId,
								   @RequestParam String[] recipientsEmails, RedirectAttributes redirectAttributes) throws EsupSignatureException {
		WorkflowStep workflowStep = workflowStepService.addStepRecipients(workflowStepId, recipientsEmails);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Participant ajouté"));
		return "redirect:/admin/workflows/" + id + "#" + workflowStep.getId();
	}

	@DeleteMapping(value = "/remove-step/{id}/{stepNumber}")
	public String addStep(@ModelAttribute("authUserEppn") String authUserEppn,
						  @PathVariable("id") Long id,
						  @PathVariable("stepNumber") Integer stepNumber) {
		Workflow workflow = workflowService.getById(id);
		workflowStepService.removeStep(workflow, stepNumber);
		return "redirect:/admin/workflows/" + id;
	}

	@GetMapping(value = "/get-files-from-source/{id}")
	public String getFileFromSource(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) throws EsupSignatureException {
		User authUser = userService.getUserByEppn(authUserEppn);
		int nbImportedFiles = signBookService.importFilesFromSource(id, authUser, authUser);
		if(nbImportedFiles == 0) {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Aucun fichier à importer"));
		} else {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("info", nbImportedFiles + " ficher(s) importé(s)"));
		}
		return "redirect:/admin/workflows/" + id;
	}

	@PostMapping(value = "/add-target/{id}")
	public String addTarget(@PathVariable("id") Long id,
							@RequestParam("documentsTargetUri") String documentsTargetUri,
							RedirectAttributes redirectAttributes) throws EsupSignatureFsException {
		if(workflowService.addTarget(id, documentsTargetUri)) {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Destination ajoutée"));
		} else {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("warn", "Une destination mail existe déjà"));
		}
		return "redirect:/admin/workflows/update/" + id;
	}

	@GetMapping(value = "/delete-target/{id}/{targetId}")
	public String deleteTarget(@PathVariable("id") Long id,
							   @PathVariable("targetId") Long targetId,
							   RedirectAttributes redirectAttributes) {
		workflowService.deleteTarget(id, targetId);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Destination supprimée"));
		return "redirect:/admin/workflows/update/" + id;
	}

	@GetMapping(value = "/export/{id}", produces="text/json")
	public ResponseEntity<Void> exportFormSetup(@PathVariable("id") Long id, HttpServletResponse response) {
		Workflow workflow = workflowService.getById(id);
		try {
			response.setContentType("text/json; charset=utf-8");
			response.setHeader("Content-Disposition", "attachment; filename=" + workflow.getName() + ".json");
			InputStream csvInputStream = workflowService.getJsonWorkflowSetup(id);
			IOUtils.copy(csvInputStream, response.getOutputStream());
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (Exception e) {
			logger.error("get file error", e);
		}
		return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@PostMapping("/import/{id}")
	public String importWorkflowSetup(@PathVariable("id") Long id,
								  @RequestParam(value = "multipartFormSetup", required=false) MultipartFile multipartFormSetup, RedirectAttributes redirectAttributes) {
		try {
			if(multipartFormSetup.getSize() > 0) {
				workflowService.setWorkflowSetupFromJson(id, multipartFormSetup.getInputStream());
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", e.getMessage()));
		}
		return "redirect:/admin/workflows/update/" + id;
	}

}
