package org.esupportail.esupsignature.web.controller.manager;

import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.Field;
import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.FieldType;
import org.esupportail.esupsignature.service.DocumentService;
import org.esupportail.esupsignature.service.FormService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("manager")
@Transactional
public class FormManagerController {
	
	private static final Logger logger = LoggerFactory.getLogger(FormManagerController.class);

	@ModelAttribute("managerMenu")
	public String getManagerMenu() {
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
	private UserService userService;

	@ModelAttribute("user")
	public User getUser() {
		return userService.getUserFromAuthentication();
	}

	@ModelAttribute("suUsers")
	public List<User> getSuUsers() {
		return userService.getSuUsers();
	}

	@PostMapping("forms")
	public String postForm(@RequestParam("name") String name, @RequestParam("targetType") String targetType, @RequestParam("targetUri") String targetUri, @RequestParam("fieldNames[]") String[] fieldNames, @RequestParam("fieldTypes[]") String[] fieldTypes, Model model) {
		Form form = new Form();
		form.setDocument(null);
		form.setName(name);
		form.setTargetType(DocumentIOType.valueOf(targetType));
		form.setTargetUri(targetUri);
		for(String fieldName : fieldNames) {
			Field field = new Field();
			field.setName(fieldName);
			field.setLabel(fieldName);
			field.setType(FieldType.text);
			form.getFields().add(field);
		}
		formService.updateForm(form);
		return "redirect:/manager/" + userService.getUserFromAuthentication().getEppn() + "/forms?";
	}
	
	@GetMapping("forms/{id}")
	public String getFormById(@PathVariable("id") Long id, Model model) {
		Form form = formService.getFormById(id);
		model.addAttribute("form", form);
		model.addAttribute("document", form.getDocument());
		return "manager/forms/show";
	}

	@PostMapping("forms/generate")
	public String generateForm(@RequestParam("multipartFile") MultipartFile multipartFile, String name, String workflowType, String code, DocumentIOType targetType, String targetUri, Model model) throws IOException {
		Document document = documentService.createDocument(multipartFile.getInputStream(), multipartFile.getOriginalFilename(), multipartFile.getContentType());
		Form form = formService.createForm(document, name, workflowType, code, targetType, targetUri);
		return "redirect:/manager/forms/" + form.getId();
	}

	@GetMapping("forms/update/{id}")
	public String updateForm(@PathVariable("id") long id, Model model) {
		Form form = formService.getFormById(id);
		model.addAttribute("form", form);
		model.addAttribute("fields", form.getFields());
		model.addAttribute("document", form.getDocument());
		model.addAttribute("workflowTypes", workflowService.getWorkflows());
		model.addAttribute("targetTypes", DocumentIOType.values());
		model.addAttribute("model", form.getDocument());
		return "manager/forms/update";
	}
	
	@GetMapping("forms")
	public String list(Model model) {
		List<Form> forms = formService.getAllForms();
		model.addAttribute("forms", forms);
		model.addAttribute("targetTypes", DocumentIOType.values());
		model.addAttribute("workflowTypes", workflowService.getWorkflows());
		return "manager/forms/list";
	}
	
	@PutMapping("forms")
	public String updateForm(@ModelAttribute Form updateForm, Model model) {
		Form form = formService.getFormById(updateForm.getId());
		form.setPdfDisplay(updateForm.isPdfDisplay());
		form.setName(updateForm.getName());
		form.setRole(updateForm.getRole());
		form.setPreFillType(updateForm.getPreFillType());
		form.setWorkflowType(updateForm.getWorkflowType());
		form.setTargetUri(updateForm.getTargetUri());
		form.setTargetType(updateForm.getTargetType());
		formService.updateForm(form);
		return "redirect:/manager/" + userService.getUserFromAuthentication().getEppn() + "/forms/" + form.getId();
	}
	
	@DeleteMapping("forms/{id}")
	public String deleteForm(@PathVariable("id") Long id, Model model) {
		formService.deleteForm(id);
		return "redirect:/manager/" + userService.getUserFromAuthentication().getEppn() + "/forms";
	}
	
} 