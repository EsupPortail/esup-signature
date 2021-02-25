package org.esupportail.esupsignature.web.controller.admin;

import io.swagger.v3.oas.annotations.Hidden;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.Target;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.service.*;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Hidden
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
	private PreFillService preFillService;

	@Resource
	private DataExportService dataExportService;

	@Resource
	private FieldService fieldService;

	@Resource
	private TargetService targetService;

	@PostMapping()
	public String postForm(@RequestParam("name") String name, @RequestParam(value = "targetType", required = false) String targetType, @RequestParam(value = "targetUri", required = false) String targetUri, @RequestParam("fieldNames[]") String[] fieldNames, @RequestParam(required = false) Boolean publicUsage) throws IOException {
		DocumentIOType documentIOType = null;
		if(targetType != null) {
			documentIOType = DocumentIOType.valueOf(targetType);
		}
		List<Target> targets = new ArrayList<>();
		targets.add(targetService.createTarget(documentIOType, targetUri));
		Form form = formService.createForm(null, name, null, null, null, null, targets, publicUsage, fieldNames);
		return "redirect:/admin/forms/" + form.getId();
	}
	
	@GetMapping("{id}")
	public String getFormById(@PathVariable("id") Long id, Model model) {
		Form form = formService.getById(id);
		model.addAttribute("form", form);
		model.addAttribute("workflow", form.getWorkflow());
		PreFill preFill = preFillService.getPreFillServiceByName(form.getPreFillType());
		model.addAttribute("preFillTypes", preFill.getTypes());
		model.addAttribute("document", form.getDocument());
		return "admin/forms/show";
	}

	@PostMapping("generate")
	public String generateForm(@RequestParam("multipartFile") MultipartFile multipartFile, String name, String title, Workflow workflow, String prefillType, String roleName, DocumentIOType targetType, String targetUri, Boolean publicUsage) throws IOException {
		List<Target> targets = new ArrayList<>();
		targets.add(targetService.createTarget(targetType, targetUri));
		Form form = formService.generateForm(multipartFile, name, title, workflow, prefillType, roleName, targets, publicUsage);
		return "redirect:/admin/forms/" + form.getId();
	}

	@GetMapping("update/{id}")
	public String updateForm(@PathVariable("id") long id, Model model) {
		Form form = formService.getById(id);
		model.addAttribute("form", form);
		model.addAttribute("fields", form.getFields());
		model.addAttribute("document", form.getDocument());
		model.addAttribute("workflowTypes", workflowService.getSystemWorkflows());
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

	@GetMapping()
	public String list(Model model) {
		List<Form> forms = formService.getAllForms();
		model.addAttribute("forms", forms);
		model.addAttribute("targetTypes", DocumentIOType.values());
		model.addAttribute("workflowTypes", workflowService.getSystemWorkflows());
		model.addAttribute("preFillTypes", preFillService.getPreFillValues());
		return "admin/forms/list";
	}
	
	@PutMapping
	public String updateForm(@ModelAttribute Form updateForm,
							 @RequestParam(required = false) List<String> managers,
							 @RequestParam(value = "types", required = false) String[] types,
							 RedirectAttributes redirectAttributes) {
		formService.updateForm(updateForm.getId(), updateForm, managers, types);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Modifications enregistrées"));
		return "redirect:/admin/forms/update/" + updateForm.getId();
	}

	@PostMapping("/update-model/{id}")
	public String updateFormmodel(@PathVariable("id") Long id,
								  @RequestParam(value = "multipartModel", required=false) MultipartFile multipartModel, RedirectAttributes redirectAttributes) {
		formService.updateFormModel(id, multipartModel);
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
				response.setHeader("Content-disposition", "inline; filename=" + URLEncoder.encode(forms.get(0).getName(), StandardCharsets.UTF_8.toString()) + ".csv");
				InputStream csvInputStream = dataExportService.getCsvDatasFromForms(forms);
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
	@PostMapping("/field/{id}/update")
	public ResponseEntity<String> updateField(@PathVariable("id") Long id,
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
		fieldService.updateField(id, favorisable, required, readOnly, extValueServiceName, extValueType, extValueReturn, searchServiceName, searchType, searchReturn, stepZero, workflowStepsIds);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@GetMapping(value = "/get-file/{id}")
	public void getFile(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletResponse httpServletResponse, RedirectAttributes redirectAttributes) throws IOException {
		try {
			Map<String, Object> attachmentResponse = formService.getModel(id);
			if (attachmentResponse != null) {
				httpServletResponse.setContentType(attachmentResponse.get("contentType").toString());
				httpServletResponse.setHeader("Content-disposition", "inline; filename=" + URLEncoder.encode(attachmentResponse.get("fileName").toString(), StandardCharsets.UTF_8.toString()));
				IOUtils.copyLarge((InputStream) attachmentResponse.get("inputStream"), httpServletResponse.getOutputStream());
			} else {
				redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Modèle non trouvée ..."));
				httpServletResponse.sendRedirect("/user/signsignrequests/" + id);
			}
		} catch (Exception e) {
			logger.error("get file error", e);
		}
	}

}