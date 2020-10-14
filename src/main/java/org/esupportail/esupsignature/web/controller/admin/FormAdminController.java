package org.esupportail.esupsignature.web.controller.admin;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.Field;
import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.UserShare;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.FieldType;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.repository.FieldRepository;
import org.esupportail.esupsignature.repository.FormRepository;
import org.esupportail.esupsignature.repository.UserShareRepository;
import org.esupportail.esupsignature.service.DocumentService;
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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
	private FormRepository formRepository;

	@Resource
	private FieldRepository fieldRepository;

	@Resource
	private FormService formService;

	@Resource
	private WorkflowService workflowService;

	@Resource
	private PreFillService preFillService;

	@Resource
	private UserShareRepository userShareRepository;

	@Resource
	private DataExportService dataExportService;

	@PostMapping()
	public String postForm(@RequestParam("name") String name, @RequestParam(value = "targetType", required = false) String targetType, @RequestParam(value = "targetUri", required = false) String targetUri, @RequestParam("fieldNames[]") String[] fieldNames, @RequestParam("fieldTypes[]") String[] fieldTypes, Model model) {
		Form form = new Form();
		form.setDocument(null);
		form.setName(name);
		if(targetType != null) {
			form.setTargetType(DocumentIOType.valueOf(targetType));
			form.setTargetUri(targetUri);
		}
		for(String fieldName : fieldNames) {
			Field field = new Field();
			field.setName(fieldName);
			field.setLabel(fieldName);
			field.setType(FieldType.text);
			form.getFields().add(field);
		}
		//TODO check other version
		form.setVersion(1);
		form.setActiveVersion(true);
		formRepository.save(form);
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
		model.addAttribute("workflowTypes", workflowService.getAllWorkflows());
		List<PreFill> aaa = preFillService.getPreFillValues();
		model.addAttribute("preFillTypes", aaa);
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
		model.addAttribute("workflowTypes", workflowService.getAllWorkflows());
		model.addAttribute("preFillTypes", preFillService.getPreFillValues());
		return "admin/forms/list";
	}
	
	@PutMapping()
	public String updateForm(@ModelAttribute Form updateForm,
							 @RequestParam(required = false) List<String> managers,
							 @RequestParam(value = "types", required = false) String[] types,
							 RedirectAttributes redirectAttributes) {
		Form form = formService.getFormById(updateForm.getId());
		form.setPdfDisplay(updateForm.getPdfDisplay());
		form.getManagers().clear();
		form.getManagers().addAll(managers);
		form.setName(updateForm.getName());
		form.setTitle(updateForm.getTitle());
		form.setRole(updateForm.getRole());
		form.setPreFillType(updateForm.getPreFillType());
		form.setWorkflowType(updateForm.getWorkflowType());
		form.setTargetUri(updateForm.getTargetUri());
		form.setTargetType(updateForm.getTargetType());
		form.setDescription(updateForm.getDescription());
		form.setMessage(updateForm.getMessage());
		form.setPublicUsage(updateForm.getPublicUsage());
		form.setAction(updateForm.getAction());
		form.getAuthorizedShareTypes().clear();
		List<ShareType> shareTypes = new ArrayList<>();
		if(types != null) {
			for (String type : types) {
				ShareType shareType = ShareType.valueOf(type);
				form.getAuthorizedShareTypes().add(shareType);
				shareTypes.add(shareType);
			}
		}
		List<UserShare> userShares = userShareRepository.findByFormId(form.getId());
		for(UserShare userShare : userShares) {
			userShare.getShareTypes().removeIf(shareType -> !shareTypes.contains(shareType));
		}
		formRepository.save(form);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Modifications enregistrées"));
		return "redirect:/admin/forms/update/" + updateForm.getId();
	}
	
	@DeleteMapping("{id}")
	public String deleteForm(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
		Form form = formRepository.findById(id).get();
		List<UserShare> userShares = userShareRepository.findByForm(form);
		for(UserShare userShare : userShares) {
			userShareRepository.deleteById(userShare.getId());
		}
		formService.deleteForm(id);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Le formulaire à bien été supprimé"));
		return "redirect:/admin/forms";
	}

	@GetMapping(value = "/{name}/datas/csv", produces="text/csv")
	public ResponseEntity<Void> getFormDatasCsv(@PathVariable String name, HttpServletResponse response) {
		List<Form> forms = formRepository.findFormByNameAndActiveVersion(name, true);
		if (forms.size() > 0) {
			try {
				response.setContentType("text/csv; charset=utf-8");
				response.setHeader("Content-Disposition", "attachment;filename=\"" + forms.get(0).getName() + ".csv\"");
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

	@PutMapping("/{formId}/field/{fieldId}/update")
	public String updateField(@PathVariable("fieldId") Long id,
							  @PathVariable("formId") Long formId,
							  @RequestParam(value = "required", required = false) Boolean required,
							  @RequestParam(value = "extValueServiceName", required = false) String extValueServiceName,
							  @RequestParam(value = "extValueType", required = false) String extValueType,
							  @RequestParam(value = "extValueReturn", required = false) String extValueReturn,
							  @RequestParam(value = "searchServiceName", required = false) String searchServiceName,
							  @RequestParam(value = "searchType", required = false) String searchType,
							  @RequestParam(value = "searchReturn", required = false) String searchReturn,
							  @RequestParam(value = "stepNumbers", required = false) String stepNumbers,
							  RedirectAttributes redirectAttributes) {
		Field field = fieldRepository.findById(id).get();
		field.setRequired(required);
		field.setExtValueServiceName(extValueServiceName);
		field.setExtValueType(extValueType);
		field.setExtValueReturn(extValueReturn);
		field.setSearchServiceName(searchServiceName);
		field.setSearchType(searchType);
		field.setSearchReturn(searchReturn);
		field.setStepNumbers(stepNumbers);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Le champ à été mis à jour"));
		return "redirect:/admin/forms/" + formId;
	}

}