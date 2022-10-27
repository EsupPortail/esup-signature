package org.esupportail.esupsignature.web.controller.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.FieldType;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.service.FieldService;
import org.esupportail.esupsignature.service.FormService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.esupportail.esupsignature.service.export.DataExportService;
import org.esupportail.esupsignature.service.interfaces.prefill.PreFill;
import org.esupportail.esupsignature.service.interfaces.prefill.PreFillService;
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
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

	@GetMapping()
	public String list(Model model) {
		List<Form> forms = formService.getAllForms();
		model.addAttribute("forms", forms);
		model.addAttribute("roles", userService.getAllRoles());
		model.addAttribute("targetTypes", DocumentIOType.values());
		model.addAttribute("workflowTypes", workflowService.getSystemWorkflows());
		model.addAttribute("preFillTypes", preFillService.getPreFillValues());
		return "admin/forms/list";
	}

	@GetMapping("{id}/fields")
	public String fields(@PathVariable("id") Long id, Model model) {
		Form form = formService.getById(id);
		model.addAttribute("form", form);
		model.addAttribute("workflow", form.getWorkflow());
		PreFill preFill = preFillService.getPreFillServiceByName(form.getPreFillType());
		if(preFill != null) {
			model.addAttribute("preFillTypes", preFill.getTypes());
		} else {
			model.addAttribute("preFillTypes", new HashMap<>());
		}
		model.addAttribute("document", form.getDocument());
		return "admin/forms/fields";
	}

	@GetMapping("{id}/signs")
	public String signs(@PathVariable("id") Long id, Model model) throws EsupSignatureIOException {
		Form form = formService.getById(id);
		Map<Long, Integer> srpMap = new HashMap<>();
		for(WorkflowStep workflowStep : form.getWorkflow().getWorkflowSteps()) {
			for(SignRequestParams signRequestParams : workflowStep.getSignRequestParams()) {
				srpMap.put(signRequestParams.getId(), form.getWorkflow().getWorkflowSteps().indexOf(workflowStep) + 1);
			}
		}
		if(form.getDocument() != null) {
			form.setTotalPageCount(formService.getTotalPagesCount(id));
		}
		model.addAttribute("form", form);
		model.addAttribute("srpMap", srpMap);
		model.addAttribute("workflow", form.getWorkflow());
		model.addAttribute("document", form.getDocument());
		return "admin/forms/signs";
	}

	@PostMapping("/update-signs-order/{id}")
	public ResponseEntity<String> updateSignsOrder(@PathVariable("id") Long id,
								   @RequestParam Map<String, String> values) throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		String[] stringStringMap = objectMapper.readValue(values.get("srpMap"), String[].class);
		Map<Long, Integer> signRequestParamsSteps = new HashMap<>();
		for (int i = 0; i < stringStringMap.length; i = i + 2) {
			signRequestParamsSteps.put(Long.valueOf(stringStringMap[i]), Integer.valueOf(stringStringMap[i + 1]));
		}
		formService.setSignRequestParamsSteps(id, signRequestParamsSteps);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/add-signrequestparams/{id}")
	public String addSignRequestParams(@PathVariable("id") Long id,
								   Integer step,
								   Integer signPageNumber,
								   Integer xPos,
								   Integer yPos,
								   RedirectAttributes redirectAttributes) throws JsonProcessingException {
		formService.addSignRequestParamsSteps(id, step, signPageNumber, xPos, yPos);
		return "redirect:/admin/forms/" + id + "/signs";
	}

	@DeleteMapping("/remove-signRequestParams/{formId}/{id}")
	public String removeSignRequestParams(@PathVariable("formId") Long formId,
									   @PathVariable("id") Long id,
									   RedirectAttributes redirectAttributes) throws JsonProcessingException {
		formService.removeSignRequestParamsSteps(formId, id);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Champ signature supprimé"));
		return "redirect:/admin/forms/" + formId + "/signs";
	}

	@PostMapping()
	public String postForm(@RequestParam("name") String name,
						   @RequestParam("title") String title,
						   @RequestParam Long workflowId,
						   @RequestParam("fieldNames[]") String[] fieldNames,
						   @RequestParam("fieldTypes[]") String[] fieldTypes,
						   @RequestParam(required = false) Boolean publicUsage, RedirectAttributes redirectAttributes) throws IOException {
		try {
			Form form = formService.createForm(null, name, title, workflowId, null, null, publicUsage, fieldNames, fieldTypes);
			return "redirect:/admin/forms/" + form.getId() + "/fields";
		} catch (EsupSignatureException e) {
			logger.error(e.getMessage());
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", e.getMessage()));
			return "redirect:/admin/forms/";
		}
	}

	@PostMapping("/add-field/{id}")
	public String addField(@PathVariable("id") long id,
						   @RequestParam("fieldNames[]") String[] fieldNames,
						   @RequestParam("fieldTypes[]") String[] fieldTypes) {
		formService.addField(id, fieldNames, fieldTypes);
		return "redirect:/admin/forms/" + id + "/fields";
	}

	@PostMapping("generate")
	public String generateForm(
			@RequestParam("multipartFile") MultipartFile multipartFile,
			@RequestParam String name,
			@RequestParam String title,
			@RequestParam Long workflowId,
			@RequestParam String prefillType,
			@RequestParam(required = false) List<String> roleNames,
			@RequestParam(required = false) Boolean publicUsage,
			RedirectAttributes redirectAttributes) throws IOException {
		try {
			Form form = formService.generateForm(multipartFile, name, title, workflowId, prefillType, roleNames, publicUsage);
			return "redirect:/admin/forms/" + form.getId() + "/fields";
		} catch (EsupSignatureException e) {
			logger.error(e.getMessage());
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", e.getMessage()));
			return "redirect:/admin/forms/";
		}
	}

	@GetMapping("update/{id}")
	public String updateForm(@PathVariable("id") long id, Model model) {
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

	@GetMapping("create")
	public String createForm(Model model) {
		model.addAttribute("form", new Form());
		return "admin/forms/create";
	}

	@PutMapping
	public String updateForm(@ModelAttribute Form updateForm,
							 @RequestParam(value = "types", required = false) String[] types,
							 RedirectAttributes redirectAttributes) {
		formService.updateForm(updateForm.getId(), updateForm, types, true);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Modifications enregistrées"));
		return "redirect:/admin/forms/update/" + updateForm.getId();
	}

	@PostMapping("/update-model/{id}")
	public String updateFormModel(@PathVariable("id") Long id,
								  @RequestParam(value = "multipartModel", required=false) MultipartFile multipartModel, RedirectAttributes redirectAttributes) {
		try {
			if(multipartModel.getSize() > 0) {
				formService.updateFormModel(id, multipartModel);
			}
		} catch (EsupSignatureException e) {
			logger.error(e.getMessage());
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", e.getMessage()));
			return "redirect:/admin/forms/";
		}
		redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Modifications enregistrées"));
		return "redirect:/admin/forms/update/" + id;
	}

	@DeleteMapping("{id}")
	public String deleteForm(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
		formService.deleteForm(id);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Le formulaire à bien été supprimé"));
		return "redirect:/admin/forms";
	}

	@GetMapping(value = "/{name}/datas/csv", produces="text/csv")
	public ResponseEntity<Void> getFormDatasCsv(@PathVariable String name, HttpServletResponse response) {
		List<Form> forms = formService.getFormByName(name);
		if (forms.size() > 0) {
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
			logger.warn("form " + name + " not found");
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ResponseBody
	@PostMapping("/fields/{id}/update")
	public ResponseEntity<String> updateField(@PathVariable("id") Long id,
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

	@DeleteMapping("{idForm}/fields/{id}/delete")
	public String updateField(@PathVariable("idForm") Long idForm, @PathVariable("id") Long id) {
		fieldService.deleteField(id, idForm);
		return "redirect:/admin/forms/" + idForm + "/fields";
	}

	@GetMapping(value = "/get-file/{id}")
	public void getFile(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletResponse httpServletResponse, RedirectAttributes redirectAttributes) throws IOException {
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
	public ResponseEntity<Void> exportFormSetup(@PathVariable("id") Long id, HttpServletResponse response) {
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
	public String importFormSetup(@PathVariable("id") Long id,
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

}