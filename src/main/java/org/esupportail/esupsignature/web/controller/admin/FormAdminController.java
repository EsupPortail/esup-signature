package org.esupportail.esupsignature.web.controller.admin;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.FieldType;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.repository.FieldRepository;
import org.esupportail.esupsignature.repository.FormRepository;
import org.esupportail.esupsignature.repository.UserShareRepository;
import org.esupportail.esupsignature.service.DocumentService;
import org.esupportail.esupsignature.service.FieldService;
import org.esupportail.esupsignature.service.FormService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.esupportail.esupsignature.service.export.DataExportService;
import org.esupportail.esupsignature.service.prefill.PreFill;
import org.esupportail.esupsignature.service.prefill.PreFillService;
import org.esupportail.esupsignature.web.controller.ws.json.JsonMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
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

@Controller
@RequestMapping("/admin/forms")
@Transactional
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
	private DocumentService documentService;

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
	public String postForm(@RequestParam("name") String name, @RequestParam(value = "targetType", required = false) String targetType, @RequestParam(value = "targetUri", required = false) String targetUri, @RequestParam("fieldNames[]") String[] fieldNames) throws IOException {
		DocumentIOType documentIOType = null;
		if(targetType != null) {
			documentIOType = DocumentIOType.valueOf(targetType);
		}
		Form form = formService.createForm(null, name, null, null, null, null, documentIOType, targetUri, fieldNames);
		return "redirect:/admin/forms/" + form.getId();
	}
	
	@GetMapping("{id}")
	public String getFormById(@PathVariable("id") Long id, Model model) {
		Form form = formService.getFormById(id);
		model.addAttribute("form", form);
		model.addAttribute("document", form.getDocument());
		return "admin/forms/show";
	}

	@PostMapping("generate")
	public String generateForm(@RequestParam("multipartFile") MultipartFile multipartFile, String name, String title, String workflowType, String prefillType, String roleName, DocumentIOType targetType, String targetUri, Model model) throws IOException {
		Document document = documentService.createDocument(multipartFile.getInputStream(), multipartFile.getOriginalFilename(), multipartFile.getContentType());
		Form form = formService.createForm(document, name, title, workflowType, prefillType, roleName, targetType, targetUri);
		return "redirect:/admin/forms/" + form.getId();
	}

	@GetMapping("update/{id}")
	public String updateForm(@PathVariable("id") long id, Model model) {
		Form form = formService.getFormById(id);
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
	@PostMapping("/{formId}/field/{fieldId}/update")
	public ResponseEntity<String> updateField(@PathVariable("fieldId") Long id,
							  @PathVariable("formId") Long formId,
							  @RequestParam(value = "required", required = false) String required,
							  @RequestParam(value = "readOnly", required = false) String readOnly,
							  @RequestParam(value = "extValueServiceName", required = false) String extValueServiceName,
							  @RequestParam(value = "extValueType", required = false) String extValueType,
							  @RequestParam(value = "extValueReturn", required = false) String extValueReturn,
							  @RequestParam(value = "searchServiceName", required = false) String searchServiceName,
							  @RequestParam(value = "searchType", required = false) String searchType,
							  @RequestParam(value = "searchReturn", required = false) String searchReturn,
							  @RequestParam(value = "stepNumbers", required = false) String stepNumbers) {
		Field field = fieldService.updateField(id, Boolean.valueOf(required), Boolean.valueOf(readOnly), extValueServiceName, extValueType, extValueReturn, searchServiceName, searchType, searchReturn, stepNumbers);
		fieldService.updateField(field);
		return new ResponseEntity<>(HttpStatus.OK);
	}

}