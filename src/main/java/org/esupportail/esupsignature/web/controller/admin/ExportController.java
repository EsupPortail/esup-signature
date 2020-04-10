package org.esupportail.esupsignature.web.controller.admin;


import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.repository.FormRepository;
import org.esupportail.esupsignature.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import java.util.List;

@Controller
@RequestMapping(value = "/admin/export")
public class ExportController {
	
	private static final Logger logger = LoggerFactory.getLogger(ExportController.class);

	@ModelAttribute("userMenu")
	public String getRoleMenu() {
		return "active";
	}

	@ModelAttribute("activeMenu")
	public String getActiveMenu() {
		return "validation";
	}

	@ModelAttribute("user")
	public User getUser() {
		return userService.getCurrentUser();
	}

	@Resource
	private UserService userService;

	@Resource
	private FormRepository formRepository;

	@GetMapping
	public String list(Model model) {
		List<Form> forms = formRepository.findAutorizedFormByUser(getUser());
		model.addAttribute("forms", forms);
		return "admin/export/list";
	}
}