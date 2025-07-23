package org.esupportail.esupsignature.web.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.dto.js.JsMessage;
import org.esupportail.esupsignature.dto.json.RecipientWsDto;
import org.esupportail.esupsignature.dto.json.WorkflowStepDto;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.*;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@RequestMapping({"/manager/workflows", "/admin/workflows"})
@Controller
public class WorkflowAdminController {

	private static final Logger logger = LoggerFactory.getLogger(WorkflowAdminController.class);

	@ModelAttribute("activeMenu")
	public String getActiveMenu() {
		return "workflows";
	}

	private final TargetService targetService;
	private final UserService userService;
	private final RecipientService recipientService;
	private final SignBookService signBookService;
	private final WorkflowService workflowService;
	private final WorkflowStepService workflowStepService;
	private final CertificatService certificatService;
	private final PreAuthorizeService preAuthorizeService;
	private final List<ExternalAuth> externalAuths;

	public WorkflowAdminController(TargetService targetService, UserService userService, RecipientService recipientService, SignBookService signBookService, WorkflowService workflowService, WorkflowStepService workflowStepService, CertificatService certificatService, PreAuthorizeService preAuthorizeService, List<ExternalAuth> externalAuths) {
		this.targetService = targetService;
		this.userService = userService;
		this.recipientService = recipientService;
		this.signBookService = signBookService;
		this.workflowService = workflowService;
		this.workflowStepService = workflowStepService;
		this.certificatService = certificatService;
		this.preAuthorizeService = preAuthorizeService;
        this.externalAuths = externalAuths;
    }

	@GetMapping
	public String list(@ModelAttribute("authUserEppn") String authUserEppn, @RequestParam(name = "displayWorkflowType", required = false) DisplayWorkflowType displayWorkflowType, Model model, HttpServletRequest httpServletRequest) {
		if (displayWorkflowType == null) {
			displayWorkflowType = DisplayWorkflowType.system;
		}
		model.addAttribute("displayWorkflowType", displayWorkflowType);
		String path = httpServletRequest.getRequestURI();
		if (path.startsWith("/admin")) {
			model.addAttribute("workflows", workflowService.getWorkflowsByDisplayWorkflowType(displayWorkflowType));
			model.addAttribute("workflowRole", "admin");
		} else {
			model.addAttribute("workflows", workflowService.getManagerWorkflows(authUserEppn));
			model.addAttribute("roles", userService.getManagersRoles(authUserEppn));
			model.addAttribute("workflowRole", "manager");
		}
		return "admin/workflows/list";
	}

	@GetMapping(value = "/steps/{id}")
	public String show(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
		if(preAuthorizeService.workflowManager(id, authUserEppn)) {
			model.addAttribute("workflowRole", "manager");
		} else if(userService.getRoles(authUserEppn).contains("ROLE_ADMIN")) {
			model.addAttribute("workflowRole", "admin");
		} else {
			redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Accès non autorisé"));
			return "redirect:/admin/workflows";
		}
		model.addAttribute("fromAdmin", true);
		Workflow workflow = workflowService.getById(id);
		model.addAttribute("workflow", workflow);
		model.addAttribute("certificats", certificatService.getAllCertificats());
		return "admin/workflows/steps";
	}

	@PostMapping(produces = "text/html")
	public String create(@ModelAttribute("authUserEppn") String authUserEppn,
						 @RequestParam(name = "title") String title,
						 @RequestParam(name = "description") String description,
						 @RequestParam(name = "managerRole", required = false) String managerRole, RedirectAttributes redirectAttributes) {
		if(title == null) {
			title = description;
		}
		Workflow workflow;
		try {
			if(userService.getRoles(authUserEppn).contains("ROLE_ADMIN")) {
				workflow = workflowService.createWorkflow(title, description, userService.getSystemUser());
				return "redirect:/admin/workflows/update/" + workflow.getId();
			} else {
				workflow = workflowService.createWorkflow(title, description, userService.getByEppn(authUserEppn));
				workflow.setManagerRole(managerRole);
				return "redirect:/manager/workflows/update/" + workflow.getId();
			}
		} catch (EsupSignatureRuntimeException e) {
			redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Un circuit possède déjà ce préfixe"));
			return "redirect:/admin/workflows";
		}
	}

    @GetMapping(value = "/update/{id}")
    public String updateForm(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
		if(preAuthorizeService.workflowManager(id, authUserEppn)) {
			model.addAttribute("workflowRole", "manager");
			model.addAttribute("roles", userService.getManagersRoles(authUserEppn));
		} else if(userService.getRoles(authUserEppn).contains("ROLE_ADMIN")) {
			model.addAttribute("workflowRole", "admin");
			model.addAttribute("roles", userService.getAllRoles());
		} else {
			redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Accès non autorisé"));
			return "redirect:/user";
		}
		Workflow workflow = workflowService.getById(id);
		model.addAttribute("workflow", workflow);
		model.addAttribute("externalAuths", externalAuths);
		model.addAttribute("nbWorkflowSignRequests", signBookService.countSignBooksByWorkflow(id));
		model.addAttribute("sourceTypes", DocumentIOType.values());
		model.addAttribute("targetTypes", DocumentIOType.values());
		model.addAttribute("shareTypes", ShareType.values());
		return "admin/workflows/update";
    }

    @PostMapping(value = "/update")
    public String update(@ModelAttribute("authUserEppn") String authUserEppn,
						 @ModelAttribute Workflow workflow,
						 @RequestParam(value = "types", required = false) String[] types,
						 @RequestParam(required = false) List<String> viewersEmails,
						 @RequestParam(required = false) Set<String> managers, RedirectAttributes redirectAttributes) {
		User user = userService.getByEppn(authUserEppn);
		if(preAuthorizeService.workflowManager(workflow.getId(), authUserEppn) || userService.getRoles(authUserEppn).contains("ROLE_ADMIN")) {
			Workflow updateWorkflow = workflowService.update(workflow, user, types, managers, authUserEppn);
			workflowService.addViewers(updateWorkflow.getId(), viewersEmails);
			if(preAuthorizeService.workflowManager(workflow.getId(), authUserEppn)) {
				return "redirect:/manager/workflows/update/" + updateWorkflow.getId();
			} else if(userService.getRoles(authUserEppn).contains("ROLE_ADMIN")) {
				return "redirect:/admin/workflows/update/" + updateWorkflow.getId();
			}
		}
		redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Accès non autorisé"));
		return "redirect:/user";
    }

    @DeleteMapping(value = "/{id}", produces = "text/html")
	@PreAuthorize("@preAuthorizeService.workflowManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public String delete(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
		try {
			workflowService.delete(id);
		} catch (EsupSignatureRuntimeException e) {
			redirectAttributes.addFlashAttribute("message", new JsMessage("error", e.getMessage()));
		}
		return "redirect:/admin/workflows";
    }

	@PreAuthorize("@preAuthorizeService.workflowManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	@PostMapping(value = "/add-step/{id}")
	public String addStep(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
						  @RequestParam("signType") String signType,
						  @RequestParam(name="description", required = false) String description,
						  @RequestParam(name="recipientsEmails", required = false) String[] recipientsEmails,
						  @RequestParam(name="changeable", required = false) Boolean changeable,
						  @RequestParam(name="maxRecipients", required = false) Integer maxRecipients,
						  @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete,
						  @RequestParam(name="attachmentRequire", required = false) Boolean attachmentRequire, RedirectAttributes redirectAttributes) throws EsupSignatureRuntimeException {
		List<RecipientWsDto> recipientWsDtos = new ArrayList<>();
		if(recipientsEmails != null) {
			recipientWsDtos = recipientService.convertRecipientEmailsToRecipientDto(List.of(recipientsEmails));
		}
		WorkflowStepDto workflowStepDto = new WorkflowStepDto(SignType.fromString(signType), description, recipientWsDtos, changeable, maxRecipients, allSignToComplete, attachmentRequire);
		workflowStepService.addStep(id, workflowStepDto, authUserEppn, false, false, null);
		if(preAuthorizeService.workflowManager(id, authUserEppn)) {
			return "redirect:/manager/workflows/steps/" + id;
		} else if(userService.getRoles(authUserEppn).contains("ROLE_ADMIN")) {
			return "redirect:/admin/workflows/steps/" + id;
		} else {
			redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Accès non autorisé"));
			return "redirect:/user";
		}
	}

	@PostMapping(value = "/add-auto-step/{id}")
	@PreAuthorize("@preAuthorizeService.workflowManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public String addAutoStep(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
							  @RequestParam(name="description", required = false) String description,
							  @RequestParam(name="certificatId", required = false) Long certificatId) throws EsupSignatureRuntimeException {
		WorkflowStepDto workflowStepDto = new WorkflowStepDto(SignType.signature, description, null, false, 1, false, false);
		workflowStepService.addStep(id, workflowStepDto, authUserEppn, false, true, certificatId);
		return "redirect:/admin/workflows/steps/" + id;
	}

	@PostMapping(value = "/update-step/{id}/{step}")
	@PreAuthorize("@preAuthorizeService.workflowManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public String updateStep(@ModelAttribute("authUserEppn") String authUserEppn,
							 @PathVariable("id") Long id,
							 @PathVariable("step") Integer step,
							 @RequestParam(name="signType") SignType signType,
							 @RequestParam(name="description") String description,
							 @RequestParam(name="maxRecipients", required = false) Integer maxRecipients,
							 @RequestParam(name="repeatable", required = false) Boolean repeatable,
							 @RequestParam(name="multiSign", required = false) Boolean multiSign,
							 @RequestParam(name="singleSignWithAnnotation", required = false) Boolean singleSignWithAnnotation,
							 @RequestParam(name="changeable", required = false) Boolean changeable,
							 @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete,
							 @RequestParam(name="attachmentAlert", required = false) Boolean attachmentAlert,
							 @RequestParam(name="attachmentRequire", required = false) Boolean attachmentRequire,
							 @RequestParam(name="autoSign", required = false) Boolean autoSign,
							 @RequestParam(name="certificatId", required = false) Long certificatId,
							 @RequestParam(name="minSignLevel", required = false) SignLevel minSignLevel,
							 @RequestParam(name="maxSignLevel", required = false) SignLevel maxSignLevel,
							 @RequestParam(name="sealVisa", required = false) Boolean sealVisa,
							 RedirectAttributes redirectAttributes) {
		Workflow workflow = workflowService.getById(id);
		try {
			workflowStepService.updateStep(workflow.getWorkflowSteps().get(step).getId(), signType, description, changeable, repeatable, multiSign, singleSignWithAnnotation, allSignToComplete, maxRecipients, attachmentAlert, attachmentRequire, autoSign, certificatId, minSignLevel, maxSignLevel, sealVisa);
		} catch (EsupSignatureRuntimeException e) {
			redirectAttributes.addFlashAttribute("message", new JsMessage("error", e.getMessage()));
		}
		if(preAuthorizeService.workflowManager(id, authUserEppn)) {
			return "redirect:/manager/workflows/steps/" + id;
		} else if(userService.getRoles(authUserEppn).contains("ROLE_ADMIN")) {
			return "redirect:/admin/workflows/steps/" + id;
		}
		redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Accès non autorisé"));
		return "redirect:/user";
	}

	@DeleteMapping(value = "/remove-step-recipent/{id}/{workflowStepId}")
	@PreAuthorize("@preAuthorizeService.workflowManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public String removeStepRecipient(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
									  @PathVariable("workflowStepId") Long workflowStepId,
									  @RequestParam(value = "userToRemoveEppn") String userToRemoveEppn, RedirectAttributes redirectAttributes) {
		workflowStepService.removeStepRecipient(workflowStepId, userToRemoveEppn);
		redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Participant supprimé"));
		if(preAuthorizeService.workflowManager(id, authUserEppn)) {
			return "redirect:/manager/workflows/steps/" + id;
		} else if(userService.getRoles(authUserEppn).contains("ROLE_ADMIN")) {
			return "redirect:/admin/workflows/steps/" + id;
		} else {
			redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Accès non autorisé"));
			return "redirect:/user";
		}
	}

	@PostMapping(value = "/add-step-recipents/{id}/{workflowStepId}")
	@PreAuthorize("@preAuthorizeService.workflowManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public String addStepRecipient(@ModelAttribute("authUserEppn") String authUserEppn,
								   @PathVariable("id") Long id,
								   @PathVariable("workflowStepId") Long workflowStepId,
								   @RequestParam String recipientsEmails, RedirectAttributes redirectAttributes) throws EsupSignatureRuntimeException {
		WorkflowStep workflowStep = workflowStepService.addStepRecipients(workflowStepId, recipientService.convertRecipientEmailsToRecipientDto(Arrays.stream(recipientsEmails.split(",")).toList()));
		redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Participant ajouté"));
		return "redirect:/admin/workflows/steps/" + id + "#" + workflowStep.getId();
	}

	@DeleteMapping(value = "/remove-step/{id}/{stepNumber}")
	@PreAuthorize("@preAuthorizeService.workflowManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public String addStep(@ModelAttribute("authUserEppn") String authUserEppn,
						  @PathVariable("id") Long id,
						  @PathVariable("stepNumber") Integer stepNumber, RedirectAttributes redirectAttributes) {
		Workflow workflow = workflowService.getById(id);
		workflowStepService.removeStep(workflow, stepNumber);
		if(preAuthorizeService.workflowManager(id, authUserEppn)) {
			return "redirect:/manager/workflows/steps/" + id;
		} else if(userService.getRoles(authUserEppn).contains("ROLE_ADMIN")) {
			return "redirect:/admin/workflows/steps/" + id;
		}
		redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Accès non autorisé"));
		return "redirect:/user";
	}

	@GetMapping(value = "/get-files-from-source/{id}")
	@PreAuthorize("@preAuthorizeService.workflowManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public String getFileFromSource(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) throws EsupSignatureRuntimeException {
		User user = userService.getByEppn(authUserEppn);
		int nbImportedFiles = signBookService.importFilesFromSource(id, user, user);
		if (nbImportedFiles == 0) {
			redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Aucun fichier à importer"));
		} else {
			redirectAttributes.addFlashAttribute("message", new JsMessage("info", nbImportedFiles + " ficher(s) importé(s)"));
		}
		return "redirect:/admin/workflows";
	}

	@PostMapping(value = "/add-target/{id}")
	@PreAuthorize("@preAuthorizeService.workflowManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public String addTarget(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
							@RequestParam("documentsTargetUri") String documentsTargetUri,
							@RequestParam(value = "sendDocument", required = false) Boolean sendDocument,
							@RequestParam(value = "sendReport", required = false) Boolean sendReport,
							@RequestParam(value = "sendDocument", required = false) Boolean sendAttachment,
							@RequestParam(value = "sendZip", required = false) Boolean sendZip,
							RedirectAttributes redirectAttributes) throws EsupSignatureFsException {
		if (workflowService.addTarget(id, documentsTargetUri, sendDocument, sendReport, sendAttachment, sendZip)) {
			redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Destination ajoutée"));
		} else {
			redirectAttributes.addFlashAttribute("message", new JsMessage("warn", "Une destination mail existe déjà"));
		}
		return "redirect:/admin/workflows/update/" + id;
	}

	@GetMapping(value = "/delete-target/{id}/{targetId}")
	@PreAuthorize("@preAuthorizeService.workflowManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public String deleteTarget(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
							   @PathVariable("targetId") Long targetId,
							   RedirectAttributes redirectAttributes) {
		workflowService.deleteTarget(id, targetId);
		redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Destination supprimée"));
		return "redirect:/admin/workflows/update/" + id;

	}

	@GetMapping(value = "/export/{id}", produces="text/json")
	@PreAuthorize("@preAuthorizeService.workflowManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public ResponseEntity<Void> exportFormSetup(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletResponse response) {
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
	@PreAuthorize("@preAuthorizeService.workflowManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public String importWorkflowSetup(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
								  @RequestParam(value = "multipartFormSetup", required=false) MultipartFile multipartFormSetup, RedirectAttributes redirectAttributes) {
		try {
			if (multipartFormSetup.getSize() > 0) {
				workflowService.setWorkflowSetupFromJson(id, multipartFormSetup.getInputStream(), authUserEppn);
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			redirectAttributes.addFlashAttribute("message", new JsMessage("error", e.getMessage()));
		}
		return "redirect:/admin/workflows/update/" + id;
	}

	@PutMapping("toggle-send-document/{id}/{targetId}")
	@PreAuthorize("@preAuthorizeService.workflowManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public String toggleSendDocument(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @PathVariable("targetId") Long targetId, RedirectAttributes redirectAttributes) {
		targetService.toggleSendDocument(targetId);
		redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Envoi des documents modifié"));
		return "redirect:/admin/workflows/update/" + id + "#targets";
	}

	@PutMapping("toggle-send-report/{id}/{targetId}")
	@PreAuthorize("@preAuthorizeService.workflowManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public String toggleSendReport(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @PathVariable("targetId") Long targetId, RedirectAttributes redirectAttributes) {
		targetService.toggleSendReport(targetId);
		redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Envoi du rapport modifié"));
		return "redirect:/admin/workflows/update/" + id + "#targets";
	}

	@PutMapping("toggle-send-attachment/{id}/{targetId}")
	@PreAuthorize("@preAuthorizeService.workflowManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public String toggleSendAttachment(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @PathVariable("targetId") Long targetId, RedirectAttributes redirectAttributes) {
		targetService.toggleSendAttachment(targetId);
		redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Envoi des pièces jointes modifié"));
		return "redirect:/admin/workflows/update/" + id + "#targets";
	}

	@PutMapping("toggle-send-zip/{id}/{targetId}")
	@PreAuthorize("@preAuthorizeService.workflowManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public String toggleSendZip(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @PathVariable("targetId") Long targetId, RedirectAttributes redirectAttributes) {
		targetService.toggleSendZip(targetId);
		redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Envoi par ZIP"));
		return "redirect:/admin/workflows/update/" + id + "#targets";
	}
}
