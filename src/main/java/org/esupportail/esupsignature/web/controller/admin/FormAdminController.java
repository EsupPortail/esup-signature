package org.esupportail.esupsignature.web.controller.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.dto.ui.global.UiMessageDto;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dto.page.admin.FormFieldUpdateDto;
import org.esupportail.esupsignature.dto.ui.global.SignatureUiConfigDto;
import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.Tag;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.FieldType;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.export.DataExportService;
import org.esupportail.esupsignature.service.interfaces.prefill.PreFillService;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.esupportail.esupsignature.dto.mapper.UiFetchService;
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping({"/manager/forms", "/admin/forms"})
public class FormAdminController {

	private static final Logger logger = LoggerFactory.getLogger(FormAdminController.class);

	@ModelAttribute("activeMenu")
	public String getActiveMenu() {
		return "forms";
	}

	private final FormService formService;
	private final WorkflowService workflowService;
	private final UserService userService;
	private final PreFillService preFillService;
	private final DataExportService dataExportService;
	private final FieldService fieldService;
	private final ObjectMapper objectMapper;
	private final PreAuthorizeService preAuthorizeService;
    private final TagService tagService;
	private final UiFetchService uiFetchService;
	private final GlobalProperties globalProperties;

	public FormAdminController(FormService formService, WorkflowService workflowService, UserService userService, PreFillService preFillService, DataExportService dataExportService, FieldService fieldService, ObjectMapper objectMapper, PreAuthorizeService preAuthorizeService, TagService tagService, UiFetchService uiFetchService, GlobalProperties globalProperties) {
		this.formService = formService;
		this.workflowService = workflowService;
		this.userService = userService;
		this.preFillService = preFillService;
		this.dataExportService = dataExportService;
		this.fieldService = fieldService;
		this.objectMapper = objectMapper;
		this.preAuthorizeService = preAuthorizeService;
        this.tagService = tagService;
						this.uiFetchService = uiFetchService;
		this.globalProperties = globalProperties;
    }

	@GetMapping()
	public String list(@ModelAttribute("authUserEppn") String authUserEppn,
                       @RequestParam(name = "selectedTags", required = false) List<Tag> selectedTags,
                       @RequestParam(name = "activeVersion", required = false) Boolean activeVersion,
                       Model model, HttpServletRequest httpServletRequest) {
		String path = httpServletRequest.getRequestURI();
		String workflowRole = path.startsWith("/admin") ? "admin" : "manager";
		var view = uiFetchService.buildAdminFormListView(authUserEppn, workflowRole, selectedTags, activeVersion);
		model.addAttribute("forms", view.forms());
		model.addAttribute("roles", view.roles());
		model.addAttribute("workflowTypes", view.workflowTypes());
		model.addAttribute("workflowRole", view.workflowRole());
		model.addAttribute("preFillTypes", view.preFillTypes());
        model.addAttribute("allTags", view.allTags());
		model.addAttribute("activeVersion", view.activeVersion());
        model.addAttribute("selectedTagIds", view.selectedTagIds());
		return "admin/forms/list";
	}

	@PostMapping()
	public String create(@ModelAttribute("authUserEppn") String authUserEppn, @RequestParam("name") String name,
						 @RequestParam("title") String title,
						 @RequestParam Long workflowId,
						 @RequestParam("fieldNames[]") String[] fieldNames,
						 @RequestParam("fieldTypes[]") String[] fieldTypes,
						 @RequestParam String prefillType,
						 @RequestParam(name = "managerRole", required = false) String managerRole,
						 @RequestParam(required = false) List<String> roleNames,
						 @RequestParam(required = false) Boolean publicUsage, RedirectAttributes redirectAttributes) throws IOException {
		try {
			Form form = formService.createForm(null, name, title, workflowId, prefillType, roleNames, publicUsage, fieldNames, fieldTypes, authUserEppn);
			if(!userService.getRoles(authUserEppn).contains("ROLE_ADMIN")) {
				form.setManagerRole(managerRole);
			}
			if(userService.getRoles(authUserEppn).contains("ROLE_ADMIN")) {
				return "redirect:/admin/forms/" + form.getId() + "/fields";
			} else {
				return "redirect:/manager/forms/" + form.getId() + "/fields";
			}
		} catch (EsupSignatureRuntimeException e) {
			logger.error(e.getMessage());
			redirectAttributes.addFlashAttribute("message", new UiMessageDto("error", e.getMessage()));
			return "redirect:/admin/forms";
		}
	}

	@PostMapping("generate")
	public String generate(@ModelAttribute("authUserEppn") String authUserEppn, @RequestParam("multipartFile") MultipartFile multipartFile,
						   @RequestParam String name,
						   @RequestParam String title,
						   @RequestParam Long workflowId,
						   @RequestParam String prefillType,
						   @RequestParam(name = "managerRole", required = false) String managerRole,
						   @RequestParam(required = false) List<String> roleNames,
						   @RequestParam(required = false) Boolean publicUsage, RedirectAttributes redirectAttributes) throws IOException {
		try {
			Form form = formService.generateForm(multipartFile, name, title, workflowId, prefillType, roleNames, publicUsage, authUserEppn);
			if(!userService.getRoles(authUserEppn).contains("ROLE_ADMIN")) {
				form.setManagerRole(managerRole);
			}
			return "redirect:/admin/forms/" + form.getId() + "/fields";
		} catch (EsupSignatureRuntimeException e) {
			logger.error(e.getMessage());
			redirectAttributes.addFlashAttribute("message", new UiMessageDto("error", e.getMessage()));
			return "redirect:/admin/forms";
		}
	}

	@GetMapping("{id}/fields")
	@PreAuthorize("@preAuthorizeService.formManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public String fields(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
		String workflowRole;
		if(preAuthorizeService.formManager(id, authUserEppn)) {
			workflowRole = "manager";
		}else if(userService.getRoles(authUserEppn).contains("ROLE_ADMIN")) {
			workflowRole = "admin";
		} else {
			redirectAttributes.addFlashAttribute("message", new UiMessageDto("error", "Accès non autorisé"));
			return "redirect:/admin/forms";
		}
		var view = uiFetchService.buildAdminFormFieldsView(authUserEppn, workflowRole, id);
		model.addAttribute("workflowRole", view.getWorkflowRole());
		model.addAttribute("form", view.getForm());
		model.addAttribute("workflow", view.getWorkflow());
		model.addAttribute("preFillTypes", view.getPreFillTypeOptions());
		model.addAttribute("document", view.getDocument());
		return "admin/forms/fields";
	}

	@GetMapping("{id}/signs")
	@PreAuthorize("@preAuthorizeService.formManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public String addSigns(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) throws EsupSignatureIOException {
		String workflowRole;
		if(preAuthorizeService.formManager(id, authUserEppn)) {
			workflowRole = "manager";
		}else if(userService.getRoles(authUserEppn).contains("ROLE_ADMIN")) {
			workflowRole = "admin";
		} else {
			redirectAttributes.addFlashAttribute("message", new UiMessageDto("error", "Accès non autorisé"));
			return "redirect:/admin/forms";
		}
		var view = uiFetchService.buildAdminFormSignsView(authUserEppn, workflowRole, id);
		model.addAttribute("workflowRole", view.getWorkflowRole());
		model.addAttribute("form", view.getForm());
		model.addAttribute("workflow", view.getWorkflow());
		model.addAttribute("document", view.getDocument());
		model.addAttribute("spots", view.getSpots());
		model.addAttribute("srpMap", view.getSrpMap());
		model.addAttribute("defaultSignImageNumber", view.getDefaultSignImageNumber());
		model.addAttribute("signatureUiConfig", SignatureUiConfigDto.fromGlobalProperties(globalProperties));
		return "admin/forms/signs";
	}

	@PostMapping("/update-signs-order/{id}")
	@PreAuthorize("@preAuthorizeService.formManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public ResponseEntity<String> updateSignsOrder(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
								   @RequestParam Map<String, String> values) throws JsonProcessingException {
		String[] stringStringMap = objectMapper.readValue(values.get("srpMap"), String[].class);
		Map<Long, Integer> signRequestParamsSteps = new HashMap<>();
		for (int i = 0; i < stringStringMap.length; i = i + 2) {
			if(signRequestParamsSteps.containsValue(Integer.valueOf(stringStringMap[i + 1]))) {
				logger.warn("step " + stringStringMap[i + 1] + " already affected");
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
			signRequestParamsSteps.put(Long.valueOf(stringStringMap[i]), Integer.valueOf(stringStringMap[i + 1]));
		}
		formService.setSignRequestParamsSteps(id, signRequestParamsSteps);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@DeleteMapping("/remove-signRequestParams/{formId}/{signRequestParamsId}")
	@PreAuthorize("@preAuthorizeService.formManager(#formId, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public String removeSignRequestParams(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("formId") Long formId,
									   @PathVariable("signRequestParamsId") Long signRequestParamsId,
									   RedirectAttributes redirectAttributes) {
		formService.removeSignRequestParamsSteps(formId, signRequestParamsId);
		redirectAttributes.addFlashAttribute("message", new UiMessageDto("info", "Champ signature supprimé"));
		return "redirect:/admin/forms/" + formId + "/signs";
	}

	@DeleteMapping("/delete-spot/{formId}/{id}")
	@ResponseBody
	@PreAuthorize("@preAuthorizeService.formManager(#formId, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public void deleteSpot(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("formId") Long formId,
										  @PathVariable("id") Long id,
										  RedirectAttributes redirectAttributes) {
		formService.removeSignRequestParamsSteps(formId, id);
		redirectAttributes.addFlashAttribute("message", new UiMessageDto("info", "Champ signature supprimé"));
	}

	@PostMapping("/add-field/{id}")
	@PreAuthorize("@preAuthorizeService.formManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public String addField(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") long id,
						   @RequestParam("fieldNames[]") String[] fieldNames,
						   @RequestParam("fieldTypes[]") String[] fieldTypes) {
		formService.addField(id, fieldNames, fieldTypes);
		return "redirect:/admin/forms/" + id + "/fields";
	}

	@GetMapping("/update/{id}")
	@PreAuthorize("@preAuthorizeService.formManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public String updateForm(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") long id, Model model, RedirectAttributes redirectAttributes) {
		String workflowRole;
		if(preAuthorizeService.formManager(id, authUserEppn)) {
			workflowRole = "manager";
		} else if(userService.getRoles(authUserEppn).contains("ROLE_ADMIN")) {
			workflowRole = "admin";
		} else {
			redirectAttributes.addFlashAttribute("message", new UiMessageDto("error", "Accès non autorisé"));
			return "redirect:/user";
		}
		var view = uiFetchService.buildAdminFormUpdateView(authUserEppn, workflowRole, id);
		model.addAttribute("workflowRole", view.getWorkflowRole());
		model.addAttribute("form", view.getForm());
		model.addAttribute("roles", view.getRoles());
		model.addAttribute("document", view.getDocument());
		model.addAttribute("workflowTypes", view.getWorkflowTypes());
		model.addAttribute("preFillTypes", view.getPreFillTypes());
		model.addAttribute("shareTypes", ShareType.values());
		model.addAttribute("targetTypes", DocumentIOType.values());
		model.addAttribute("model", view.getDocument());
	        model.addAttribute("allTags", view.getAllTags());
	        model.addAttribute("selectedTagIds", view.getSelectedTagIds());
        return "admin/forms/update";
	}


	@PutMapping
	public String updateForm(@ModelAttribute("authUserEppn") String authUserEppn, @ModelAttribute Form updateForm,
							 @RequestParam(value = "types", required = false) String[] types,
							 RedirectAttributes redirectAttributes) {

		redirectAttributes.addFlashAttribute("message", new UiMessageDto("success", "Modifications enregistrées"));
		if(userService.getRoles(authUserEppn).contains("ROLE_ADMIN")) {
			formService.updateForm(updateForm.getId(), updateForm, types, true, authUserEppn);
			return "redirect:/admin/forms/update/" + updateForm.getId();
		} else if(preAuthorizeService.formManager(updateForm.getId(), authUserEppn)) {
			formService.updateForm(updateForm.getId(), updateForm, types, true, authUserEppn);
			return "redirect:/manager/forms/update/" + updateForm.getId();
		} else {
			redirectAttributes.addFlashAttribute("message", new UiMessageDto("success", "Accès non autorisé"));
			return "redirect:/";
		}
	}

	@PostMapping("/update-model/{id}")
	@PreAuthorize("@preAuthorizeService.formManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public String updateFormModel(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
								  @RequestParam(value = "multipartModel", required=false) MultipartFile multipartModel, RedirectAttributes redirectAttributes) {
		try {
			if(multipartModel.getSize() > 0) {
				formService.updateFormModel(id, multipartModel);
			}
		} catch (EsupSignatureRuntimeException e) {
			logger.error(e.getMessage());
			redirectAttributes.addFlashAttribute("message", new UiMessageDto("error", e.getMessage()));
			return "redirect:/admin/forms";
		}
		redirectAttributes.addFlashAttribute("message", new UiMessageDto("success", "Modifications enregistrées"));
		return "redirect:/admin/forms/update/" + id;
	}

	@DeleteMapping("{id}")
	@PreAuthorize("@preAuthorizeService.formManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public String deleteForm(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
		formService.deleteForm(id);
		redirectAttributes.addFlashAttribute("message", new UiMessageDto("info", "Le formulaire a bien été supprimé"));
		return "redirect:/admin/forms";
	}

	@GetMapping(value = "/{name}/datas/csv", produces="text/csv")
	public ResponseEntity<Void> getFormDatasCsv(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable String name, HttpServletResponse response) {
		List<Form> forms = formService.getFormByName(name);
		if (!forms.isEmpty()) {
			if(preAuthorizeService.formManager(forms.get(0).getId(), authUserEppn) || userService.getRoles(authUserEppn).contains("ROLE_ADMIN")) {
				try {
					response.setContentType("text/csv; charset=utf-8");
					response.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode(forms.get(0).getName(), StandardCharsets.UTF_8) + ".csv");
					InputStream csvInputStream = dataExportService.getCsvDatasFromForms(forms.stream().map(Form::getWorkflow).collect(Collectors.toList()));
					IOUtils.copy(csvInputStream, response.getOutputStream());
					return new ResponseEntity<>(HttpStatus.OK);
				} catch (Exception e) {
					logger.error("get file error", e);
				}
			} else {
				return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
			}
		} else {
			logger.warn("form " + name + " not found");
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ResponseBody
	@PutMapping("{formId}/fields/{id}/update")
	@PreAuthorize("@preAuthorizeService.formManager(#formId, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public ResponseEntity<String> updateField(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("formId") Long formId, @PathVariable("id") Long id,
											  @RequestParam(value = "description", required = false) String description,
											  @RequestParam(value = "fieldType", required = false, defaultValue = "text") FieldType fieldType,
											  @RequestParam(value = "required", required = false, defaultValue = "false") Boolean required,
											  @RequestParam(value = "favorisable", required = false, defaultValue = "false") Boolean favorisable,
											  @RequestParam(value = "readOnly", required = false, defaultValue = "false") Boolean readOnly,
											  @RequestParam(value = "prefill", required = false, defaultValue = "false") Boolean prefill,
											  @RequestParam(value = "search", required = false, defaultValue = "false") Boolean search,
											  @RequestParam(value = "valueServiceName", required = false) String valueServiceName,
											  @RequestParam(value = "valueType", required = false) String valueType,
											  @RequestParam(value = "valueReturn", required = false) String valueReturn,
											  @RequestParam(value = "stepZero", required = false, defaultValue = "false") Boolean stepZero,
											  @RequestParam(value = "workflowStepsIds", required = false) List<Long> workflowStepsIds) {

		String extValueServiceName = "";
		String extValueType = "";
		String extValueReturn = "";
		String searchServiceName = "";
		String searchType = "";
		String searchReturn = "";
		if(prefill) {
			extValueServiceName = valueServiceName;
			extValueType = valueType;
			extValueReturn = valueReturn;
		}
		if(search) {
			searchServiceName = valueServiceName;
			searchType = valueType;
			searchReturn = valueReturn;
		}
		fieldService.updateField(id, description, fieldType, favorisable, required, readOnly, extValueServiceName, extValueType, extValueReturn, searchServiceName, searchType, searchReturn, stepZero, workflowStepsIds);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@ResponseBody
	@PutMapping("{formId}/fields/update-all")
	@PreAuthorize("@preAuthorizeService.formManager(#formId, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public ResponseEntity<String> updateAllFields(@ModelAttribute("authUserEppn") String authUserEppn,
										@PathVariable("formId") Long formId,
										@RequestBody(required = false) List<FormFieldUpdateDto> fieldUpdates) {
		fieldService.updateFields(formId, fieldUpdates == null ? List.of() : fieldUpdates);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@DeleteMapping("{formId}/fields/{id}/delete")
	@PreAuthorize("@preAuthorizeService.formManager(#formId, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public String deleteField(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("formId") Long formId, @PathVariable("id") Long id) {
		fieldService.deleteField(id, formId);
		return "redirect:/admin/forms/" + formId + "/fields";
	}

	@GetMapping(value = "/get-file/{id}")
	@PreAuthorize("@preAuthorizeService.formManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public void getFile(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletResponse httpServletResponse, RedirectAttributes redirectAttributes) throws IOException {
		try {
			if (!formService.getModel(id, httpServletResponse)) {
				redirectAttributes.addFlashAttribute("message", new UiMessageDto("error", "Modèle non trouvée ..."));
				httpServletResponse.sendRedirect("/user/signsignrequests/" + id);
			}
		} catch (Exception e) {
			logger.error("get file error", e);
		}
	}

	@GetMapping(value = "/export/{id}", produces="text/json")
	@PreAuthorize("@preAuthorizeService.formManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public ResponseEntity<Void> exportFormSetup(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletResponse response) {
		Form form = formService.getById(id);
		try {
			response.setContentType("text/json; charset=utf-8");
			response.setHeader("Content-Disposition", "attachment; filename=" + form.getName() + ".json");
			InputStream csvInputStream = formService.getJsonFormSetup(id);
			IOUtils.copy(csvInputStream, response.getOutputStream());
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (Exception e) {
			logger.error("get file error", e);
		}
		return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@PostMapping("/import/{id}")
	@PreAuthorize("@preAuthorizeService.formManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public String importFormSetup(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
								  @RequestParam(value = "multipartFormSetup", required=false) MultipartFile multipartFormSetup, RedirectAttributes redirectAttributes) {
		try {
			if(multipartFormSetup.getSize() > 0) {
				formService.setFormSetupFromJson(id, multipartFormSetup.getInputStream(), authUserEppn);
			}
		} catch (IOException e) {
			logger.error(e.getMessage());
			redirectAttributes.addFlashAttribute("message", new UiMessageDto("error", e.getMessage()));
		}
		return "redirect:/admin/forms/update/" + id;
	}

	@PostMapping(value = "/add-spot/{id}")
	@ResponseBody
	@PreAuthorize("@preAuthorizeService.formManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public Long addSpot(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
						  @RequestParam(value = "spotStepNumber", required = false) Integer spotStepNumber,
						  @RequestParam(value = "commentPageNumber", required = false) Integer commentPageNumber,
                          @RequestParam(value = "commentScale", required = false) Float commentScale,
						  @RequestParam(value = "commentPosX", required = false) Integer commentPosX,
						  @RequestParam(value = "commentPosY", required = false) Integer commentPosY
						) {
		return formService.addSignRequestParamsSteps(id, spotStepNumber, commentPageNumber, commentPosX, commentPosY, Math.round(200 * commentScale), Math.round(100 * commentScale));
	}

}
