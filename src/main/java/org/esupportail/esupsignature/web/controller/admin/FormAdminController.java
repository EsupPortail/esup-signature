package org.esupportail.esupsignature.web.controller.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.FieldType;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.FieldService;
import org.esupportail.esupsignature.service.FormService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.esupportail.esupsignature.service.export.DataExportService;
import org.esupportail.esupsignature.service.interfaces.prefill.PreFill;
import org.esupportail.esupsignature.service.interfaces.prefill.PreFillService;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
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

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/forms")
public class FormAdminController {

	private static final Logger logger = LoggerFactory.getLogger(FormAdminController.class);

	@ModelAttribute("adminMenu")
	public String getAdminMenu() {
		return "active";
	}

	@ModelAttribute("activeMenu")
	public String getActiveMenu() {
		return "forms";
	}

	@Resource
	private FormService formService;

	@Resource
	private WorkflowService workflowService;

	@Resource
	private UserService userService;

	@Resource
	private PreFillService preFillService;

	@Resource
	private DataExportService dataExportService;

	@Resource
	private FieldService fieldService;

	@Resource
	private ObjectMapper objectMapper;

	@Resource
	private PreAuthorizeService preAuthorizeService;

	@GetMapping()
	public String list(@ModelAttribute("authUserEppn") String authUserEppn, Model model) {
		Set<Form> forms = new HashSet<>();
		User user = userService.getByEppn(authUserEppn);
		if(user.getRoles().contains("ROLE_ADMIN")) {
			forms.addAll(formService.getAllForms());
			model.addAttribute("roles", userService.getAllRoles());
			model.addAttribute("workflowTypes", workflowService.getSystemWorkflows());
		} else {
			forms.addAll(formService.getManagerForms(authUserEppn));
			model.addAttribute("roles", userService.getManagersRoles(authUserEppn));
			model.addAttribute("workflowTypes", workflowService.getManagerWorkflows(authUserEppn));
		}
		model.addAttribute("forms", forms.stream().sorted(Comparator.comparing(Form::getTitle)).collect(Collectors.toList()));
		model.addAttribute("targetTypes", DocumentIOType.values());
		model.addAttribute("preFillTypes", preFillService.getPreFillValues());
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
			Form form = formService.createForm(null, name, title, workflowId, prefillType, roleNames, publicUsage, fieldNames, fieldTypes);
			User user = userService.getByEppn(authUserEppn);
			if(!user.getRoles().contains("ROLE_ADMIN")) {
				form.setManagerRole(managerRole);
			}
			return "redirect:/admin/forms/" + form.getId() + "/fields";
		} catch (EsupSignatureRuntimeException e) {
			logger.error(e.getMessage());
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", e.getMessage()));
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
			Form form = formService.generateForm(multipartFile, name, title, workflowId, prefillType, roleNames, publicUsage);
			User user = userService.getByEppn(authUserEppn);
			if(!user.getRoles().contains("ROLE_ADMIN")) {
				form.setManagerRole(managerRole);
			}
			return "redirect:/admin/forms/" + form.getId() + "/fields";
		} catch (EsupSignatureRuntimeException e) {
			logger.error(e.getMessage());
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", e.getMessage()));
			return "redirect:/admin/forms";
		}
	}

	@GetMapping("{id}/fields")
	@PreAuthorize("@preAuthorizeService.formManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public String fields(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
		User user = userService.getByEppn(authUserEppn);
		if(preAuthorizeService.formManager(id, authUserEppn) || user.getRoles().contains("ROLE_ADMIN")) {
			Form form = formService.getById(id);
			model.addAttribute("form", form);
			model.addAttribute("workflow", form.getWorkflow());
			PreFill preFill = preFillService.getPreFillServiceByName(form.getPreFillType());
			if (preFill != null) {
				model.addAttribute("preFillTypes", preFill.getTypes());
			} else {
				model.addAttribute("preFillTypes", new HashMap<>());
			}
			model.addAttribute("document", form.getDocument());
			return "admin/forms/fields";
		}
		redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Accès non autorisé"));
		return "redirect:/admin/forms";
	}

	@GetMapping("{id}/signs")
	@PreAuthorize("@preAuthorizeService.formManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public String addSigns(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, Model model) throws EsupSignatureIOException {
		Form form = formService.getById(id);
		if (form.getWorkflow() != null) {
			model.addAttribute("spots", formService.getSpots(id));
			model.addAttribute("srpMap", formService.getSrpMap(form));
		}
		if (form.getDocument() != null) {
			form.setTotalPageCount(formService.getTotalPagesCount(id));
		}
		model.addAttribute("form", form);
		model.addAttribute("workflow", form.getWorkflow());
		model.addAttribute("document", form.getDocument());
		return "admin/forms/signs";
	}

	@PostMapping("/update-signs-order/{id}")
	@PreAuthorize("@preAuthorizeService.formManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public ResponseEntity<String> updateSignsOrder(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
								   @RequestParam Map<String, String> values) throws JsonProcessingException {
		String[] stringStringMap = objectMapper.readValue(values.get("srpMap"), String[].class);
		Map<Long, Integer> signRequestParamsSteps = new HashMap<>();
		for (int i = 0; i < stringStringMap.length; i = i + 2) {
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
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Champ signature supprimé"));
		return "redirect:/admin/forms/" + formId + "/signs";
	}

	@DeleteMapping("/delete-spot/{formId}/{id}")
	@ResponseBody
	@PreAuthorize("@preAuthorizeService.formManager(#formId, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public void deleteSpot(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("formId") Long formId,
										  @PathVariable("id") Long id,
										  RedirectAttributes redirectAttributes) {
		formService.removeSignRequestParamsSteps(formId, id);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Champ signature supprimé"));
	}

	@PostMapping("/add-field/{id}")
	@PreAuthorize("@preAuthorizeService.formManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public String addField(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") long id,
						   @RequestParam("fieldNames[]") String[] fieldNames,
						   @RequestParam("fieldTypes[]") String[] fieldTypes) {
		formService.addField(id, fieldNames, fieldTypes);
		return "redirect:/admin/forms/" + id + "/fields";
	}

	@GetMapping("update/{id}")
	@PreAuthorize("@preAuthorizeService.formManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public String updateForm(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") long id, Model model) {
		Form form = formService.getById(id);
		model.addAttribute("form", form);
		model.addAttribute("fields", form.getFields());
		model.addAttribute("roles", userService.getAllRoles());
		model.addAttribute("document", form.getDocument());
		List<Workflow> workflows = workflowService.getSystemWorkflows();
		workflows.add(form.getWorkflow());
		model.addAttribute("workflowTypes", workflows);
		List<PreFill> preFillTypes = preFillService.getPreFillValues();
		model.addAttribute("preFillTypes", preFillTypes);
		model.addAttribute("shareTypes", ShareType.values());
		model.addAttribute("targetTypes", DocumentIOType.values());
		model.addAttribute("model", form.getDocument());
		return "admin/forms/update";
	}


	@PutMapping
	public String updateForm(@ModelAttribute("authUserEppn") String authUserEppn, @ModelAttribute Form updateForm,
							 @RequestParam(value = "types", required = false) String[] types,
							 RedirectAttributes redirectAttributes) {
		User user = userService.getByEppn(authUserEppn);
		if(preAuthorizeService.formManager(updateForm.getId(), authUserEppn) || user.getRoles().contains("ROLE_ADMIN")) {
			formService.updateForm(updateForm.getId(), updateForm, types, true);
			redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Modifications enregistrées"));
			return "redirect:/admin/forms/update/" + updateForm.getId();
		}
		redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Accès non autorisé"));
		return "redirect:/";
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
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", e.getMessage()));
			return "redirect:/admin/forms";
		}
		redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Modifications enregistrées"));
		return "redirect:/admin/forms/update/" + id;
	}

	@DeleteMapping("{id}")
	@PreAuthorize("@preAuthorizeService.formManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public String deleteForm(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
		formService.deleteForm(id);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Le formulaire a bien été supprimé"));
		return "redirect:/admin/forms";
	}

	@GetMapping(value = "/{name}/datas/csv", produces="text/csv")
	public ResponseEntity<Void> getFormDatasCsv(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable String name, HttpServletResponse response) {
		List<Form> forms = formService.getFormByName(name);
		if (forms.size() > 0) {
			User user = userService.getByEppn(authUserEppn);
			if(preAuthorizeService.formManager(forms.get(0).getId(), authUserEppn) || user.getRoles().contains("ROLE_ADMIN")) {
				try {
					response.setContentType("text/csv; charset=utf-8");
					response.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode(forms.get(0).getName(), StandardCharsets.UTF_8.toString()) + ".csv");
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
	@PostMapping("{formId}/fields/{id}/update")
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

	@DeleteMapping("{formId}/fields/{id}/delete")
	@PreAuthorize("@preAuthorizeService.formManager(#formId, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public String updateField(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("formId") Long formId, @PathVariable("id") Long id) {
		fieldService.deleteField(id, formId);
		return "redirect:/admin/forms/" + formId + "/fields";
	}

	@GetMapping(value = "/get-file/{id}")
	@PreAuthorize("@preAuthorizeService.formManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public void getFile(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletResponse httpServletResponse, RedirectAttributes redirectAttributes) throws IOException {
		try {
			if (!formService.getModel(id, httpServletResponse)) {
				redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Modèle non trouvée ..."));
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
				formService.setFormSetupFromJson(id, multipartFormSetup.getInputStream());
			}
		} catch (IOException e) {
			logger.error(e.getMessage());
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", e.getMessage()));
		}
		return "redirect:/admin/forms/update/" + id;
	}

	@PostMapping(value = "/add-spot/{id}")
	@ResponseBody
	@PreAuthorize("@preAuthorizeService.formManager(#id, #authUserEppn) || hasRole('ROLE_ADMIN')")
	public void addSpot(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
						  @RequestParam(value = "spotStepNumber", required = false) Integer spotStepNumber,
						  @RequestParam(value = "commentPageNumber", required = false) Integer commentPageNumber,
						  @RequestParam(value = "commentPosX", required = false) Integer commentPosX,
						  @RequestParam(value = "commentPosY", required = false) Integer commentPosY) {
		if(spotStepNumber != null) {
			formService.addSignRequestParamsSteps(id, spotStepNumber, commentPageNumber, commentPosX, commentPosY);
		}
	}

}
