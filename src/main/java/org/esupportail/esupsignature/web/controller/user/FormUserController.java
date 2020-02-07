package org.esupportail.esupsignature.web.controller.user;

import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.repository.DataRepository;
import org.esupportail.esupsignature.repository.FormRepository;
import org.esupportail.esupsignature.service.DataService;
import org.esupportail.esupsignature.service.FormService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.sign.SignService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/user")
public class FormUserController {
	
	private static final Logger logger = LoggerFactory.getLogger(FormUserController.class);

	@Resource
	private FormRepository formRepository;

	@Resource
	private UserService userService;

	@Resource
	private FormService formService;

	@Resource
	private DataService dataService;

	@Resource
	private DataRepository dataRepository;

	@Resource
	private SignService signService;

	@ModelAttribute("user")
	public User getUser(@PathVariable String eppn) {
		return userService.getUserFromSu(eppn);
	}



	@ModelAttribute("forms")
	public List<Form> getForms(@PathVariable String eppn) {
		return 	formService.getFormsByUser(userService.getUserFromSu(eppn), true);
	}

	@ModelAttribute("suUsers")
	public List<User> getSuUsers() {
		return userService.getSuUsers();
	}

	@ModelAttribute("activeMenu")
	public String getActiveMenu() {
		return "apps";
	}

	@GetMapping("forms/{id}")
	public String getFormById(@PathVariable("id") Long id, Model model, @PathVariable String eppn) {
		Form form = formRepository.findById(id).get();
		model.addAttribute("form", form);
		model.addAttribute("activeForm", form.getName());
		model.addAttribute("fields", form.getFields());
		model.addAttribute("document", form.getDocument());
		return "user/forms/show";
	}

	@GetMapping("forms/{id}/list")
	public String getFormByIdList(@PathVariable("id") Long id, @SortDefault(value = "createDate", direction = Direction.DESC) @PageableDefault(size = 5) Pageable pageable, Model model, @PathVariable String eppn) throws RuntimeException {
		User user = userService.getUserFromSu(eppn);
		Form form = formRepository.findById(id).get();
		Form activeVersionForm = formRepository.findFormByNameAndActiveVersion(form.getName(), true).get(0);
		model.addAttribute("form", form);
		model.addAttribute("activeForm", form.getName());
		model.addAttribute("activeVersionForm", activeVersionForm);
		Page<Data> datas = dataRepository.findByFormNameAndOwner(form.getName(), user.getEppn(), pageable);
		model.addAttribute("datas", datas);
		return "user/forms/list";
	}
	
	@GetMapping("forms")
	public String getAllForms(Model model, @PathVariable String eppn) {
		List<Form> forms = formRepository.findFormByActiveVersion(true);
		model.addAttribute("forms", forms);
		return "user/forms/list";
	}

	@PostMapping("forms/{id}")
	public String addForm(@PathVariable("id") Long id, @RequestParam MultiValueMap<String, String> formData, RedirectAttributes redirectAttributes, @PathVariable String eppn) {
		User user = userService.getUserFromSu(eppn);
		Form form = formRepository.findById(id).get();
		Data data = new Data();
		data.setName(form.getName());
		data.setDatas(formData.toSingleValueMap());
		data.setForm(form);
		data.setStatus(SignRequestStatus.draft);
		data.setCreateBy(userService.getUserFromAuthentication().getEppn());
		data.setCreateDate(new Date());
		data.setOwner(user.getEppn());
		dataService.updateData(data);
		return "redirect:/user/data/" + data.getId();
	}
	
} 