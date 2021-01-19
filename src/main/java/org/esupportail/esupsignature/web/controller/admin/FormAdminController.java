package org.esupportail.esupsignature.web.controller.admin;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.service.FieldService;
import org.esupportail.esupsignature.service.FormService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.esupportail.esupsignature.service.export.DataExportService;
import org.esupportail.esupsignature.service.interfaces.prefill.PreFill;
import org.esupportail.esupsignature.service.interfaces.prefill.PreFillService;
import org.esupportail.esupsignature.web.controller.ws.json.JsonMessage;
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
import java.util.List;

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

	@PostMapping()
	public String postForm(@RequestParam("name") String name, @RequestParam(value = "targetType", required = false) String targetType, @RequestParam(value = "targetUri", required = false) String targetUri, @RequestParam("fieldNames[]") String[] fieldNames, @RequestParam(required = false) Boolean publicUsage) throws IOException {
		DocumentIOType documentIOType = null;
		if(targetType != null) {
			documentIOType = DocumentIOType.valueOf(targetType);
		}
		Form form = formService.createForm(null, name, null, null, null, null, documentIOType, targetUri, publicUsage, fieldNames);
		return "redirect:/admin/forms/" + form.getId();
	}
	
	@GetMapping("{id}")
	public String getFormById(@PathVariable("id") Long id, Model model) {
		Form form = formService.getById(id);
		model.addAttribute("form", form);
		PreFill preFill = preFillService.getPreFillServiceByName(form.getPreFillType());
		model.addAttribute("preFillTypes", preFill.getTypes());
		model.addAttribute("document", form.getDocument());
		return "admin/forms/show";
	}

	@PostMapping("generate")
	public String generateForm(@RequestParam("multipartFile") MultipartFile multipartFile, String name, String title, String workflowType, String prefillType, String roleName, DocumentIOType targetType, String targetUri, Boolean publicUsage) throws IOException {
		Form form = formService.generateForm(multipartFile, name, title, workflowType, prefillType, roleName, targetType, targetUri, publicUsage);
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
	
	@PutMapping()
	public String updateForm(@ModelAttribute Form updateForm,
							 @RequestParam(required = false) List<String> managers,
							 @RequestParam(value = "types", required = false) String[] types,
							 RedirectAttributes redirectAttributes) {
		formService.updateForm(updateForm.getId(), updateForm, managers, types);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Modifications enregistrées"));
		return "redirect:/admin/forms/update/" + updateForm.getId();
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
											  @RequestParam(value = "required", required = false) Boolean required,
											  @RequestParam(value = "readOnly", required = false) Boolean readOnly,
											  @RequestParam(value = "prefill", required = false) Boolean prefill,
											  @RequestParam(value = "search", required = false) Boolean search,
											  @RequestParam(value = "valueServiceName", required = false) String valueServiceName,
											  @RequestParam(value = "valueType", required = false) String valueType,
											  @RequestParam(value = "valueReturn", required = false) String valueReturn,
											  @RequestParam(value = "stepNumbers", required = false) String stepNumbers) {

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
		fieldService.updateField(id, Boolean.valueOf(required), Boolean.valueOf(readOnly), extValueServiceName, extValueType, extValueReturn, searchServiceName, searchType, searchReturn, stepNumbers);
		return new ResponseEntity<>(HttpStatus.OK);
	}

}