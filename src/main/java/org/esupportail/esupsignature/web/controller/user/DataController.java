package org.esupportail.esupsignature.web.controller.user;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.repository.DataRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.repository.UserPropertieRepository;
import org.esupportail.esupsignature.repository.UserRepository;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.prefill.PreFillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/user")
public class DataController {

	private static final Logger logger = LoggerFactory.getLogger(DataController.class);

	@Resource
	private DataService dataService;

	@Resource
	private DataRepository dataRepository;

	@Resource
	private UserPropertieRepository userPropertieRepository;

	@Resource
	private FormService formService;

	@Resource
	private UserService userService;

	@Resource
	private SignRequestService signRequestService;

	@Resource
	private WorkflowService workflowService;

	@Resource
	private PreFillService preFillService;

	@Resource
	private UserRepository userRepository;

	@Resource
	private SignRequestRepository signRequestRepository;

	@ModelAttribute("user")
	public User getUser() {
		return userService.getUserFromAuthentication();
	}

	@ModelAttribute("suUsers")
	public List<User> getSuUsers() {
		return userService.getSuUsers();
	}

	@ModelAttribute("userMenu")
	public String getActiveRole() {
		return "active";
	}

	@ModelAttribute("activeMenu")
	public String getActiveMenu() {
		return "datas";
	}

	@ModelAttribute("forms")
	public List<Form> getForms() {
		return 	formService.getFormsByUser(userService.getUserFromAuthentication(), true);
	}

	@GetMapping("datas")
	public String list(@SortDefault(value = "createDate", direction = Direction.DESC) @PageableDefault(size = 3) Pageable pageable, Model model) {
		User user = userService.getUserFromAuthentication();
		Page<Data> datas =  dataRepository.findByCreateBy(user.getEppn(), pageable);
		model.addAttribute("datas", datas);
		return "user/datas/list";
	}

	@GetMapping("datas/{id}")
	public String show(@PathVariable("id") Long id, @RequestParam(required = false) Integer page, Model model) {
		User user = userService.getUserFromAuthentication();
		Data data = dataService.getDataById(id);
		if(page == null) {
			page = 1;
		}
		if(user.getEppn().equals(data.getOwner())) {
			Form form = data.getForm();
			Workflow workflow = workflowService.getWorkflowByClassName(form.getWorkflowType());
			List<UserPropertie> userProperties = userPropertieRepository.findByUserAndStepAndForm(user, 0, form);
			userProperties = userProperties.stream().sorted(Comparator.comparing(UserPropertie::getId).reversed()).collect(Collectors.toList());
			if(userProperties.size() > 0 ) {
				model.addAttribute("targetEmails", userProperties.get(0).getTargetEmail().split(","));
			}
			model.addAttribute("steps", workflow.getWorkflowSteps(data, new ArrayList<>()));
			model.addAttribute("data", data);
			model.addAttribute("form", form);
			model.addAttribute("activeForm", form.getName());
			Integer finalPage = page;
			List<Field> fields = form.getFields().stream().filter(field -> field.getPage() == finalPage).collect(Collectors.toList());
			model.addAttribute("page", page);
			model.addAttribute("fields", fields);
			model.addAttribute("document", form.getDocument());
			return "user/datas/show";
		} else {
			return "/";
		}
	}

	@GetMapping("datas/form/{id}")
	public String createData(@PathVariable("id") Long id, @RequestParam(required = false) Integer page, Model model) {
		User user = userService.getUserFromAuthentication();
		Form form = formService.getFormById(id);
		if(page == null) {
			page = 1;
		}
		model.addAttribute("form", form);
		if(form.getPreFillType() != null) {
			Integer finalPage = page;
			List<Field> fields = form.getFields().stream().filter(field -> field.getPage() == finalPage).collect(Collectors.toList());
			model.addAttribute("fields", preFillService.getPreFillServiceByName(form.getPreFillType(), fields, user));
		} else {
			model.addAttribute("fields", form.getFields());
		}

		model.addAttribute("data", new Data());
		model.addAttribute("activeForm", form.getName());
		model.addAttribute("page", page);
		if(form.isPdfDisplay()) {
			return "user/datas/create2";
		} else {
			return "user/datas/create";
		}
	}


	@GetMapping("datas/{id}/update")
	public String updateData(@PathVariable("id") Long id, Model model) {
		User user = userService.getUserFromAuthentication();
		Data data = dataService.getDataById(id);
		Form form = data.getForm();
		if(!data.getStatus().equals(SignRequestStatus.draft) && user.getEmail().equals(data.getCreateBy())) {
			return "redirect:/user/" + user.getEppn() + "/data/" + data.getId();
		}
		List<Field> fields = form.getFields();
		for(Field field : fields) {
			field.setDefaultValue(data.getDatas().get(field.getName()));
		}
		Workflow workflow = workflowService.getWorkflowByClassName(form.getWorkflowType());
		model.addAttribute("steps", workflow.getWorkflowSteps(data, new ArrayList<>()));
		model.addAttribute("fields", fields);
		model.addAttribute("data", data);
		model.addAttribute("form", form);
		model.addAttribute("activeForm", form.getName());
		model.addAttribute("document", form.getDocument());
		return "user/datas/create2";
	}

	@PostMapping("datas/form/{id}")
	public String addData(@PathVariable("id") Long id, @RequestParam Long dataId, @RequestParam String name, @RequestParam MultiValueMap<String, String> formData, RedirectAttributes redirectAttributes) {
		User user = userService.getUserFromAuthentication();
		Form form = formService.getFormById(id);
		Data data;
		if(dataId != null) {
			data = dataRepository.findById(dataId).get();
		} else {
			data = new Data();
		}
		data.setName(name);
		data.getDatas().putAll(formData.toSingleValueMap());
		data.setForm(form);
		data.setFormName(form.getName());
		data.setFormVersion(form.getVersion());
		data.setStatus(SignRequestStatus.draft);
		data.setCreateBy(userService.getUserFromAuthentication().getEppn());
		data.setOwner(user.getEppn());
		data.setCreateDate(new Date());
		dataService.updateData(data);
		return "redirect:/user/datas/" + data.getId() + "/update";
	}
	

	@PutMapping("datas/{id}")
	public String updateData(@PathVariable("id") Long id, @RequestParam String name, @RequestParam(required = false) String navPage, @RequestParam(required = false) Integer page, @RequestParam MultiValueMap<String, String> formData, RedirectAttributes redirectAttributes) {
		User user = userService.getUserFromAuthentication();
		Data data = dataService.getDataById(id);
		if(page == null) {
			page = 1;
		}
		if("next".equals(navPage)) {
			page++;
		} else if("prev".equals(navPage)) {
			page--;
		}
		data.setName(name);
		for(String key : formData.keySet()) {
			data.getDatas().put(key, formData.get(key).get(0));
		}
		//data.setDatas(formData.toSingleValueMap());
		data.setUpdateDate(new Date());
		if(user.getEppn().equals(data.getCreateBy())) {
			dataService.updateData(data);
		}
		redirectAttributes.addAttribute("page", page);
		if(navPage != null && !navPage.isEmpty()) {
			return "redirect:/user/" + user.getEppn() + "/data/" + data.getId() + "/update?page=" + page;
		} else {
			return "redirect:/user/" + user.getEppn() + "/data/" + data.getId();
		}
	}

	@PostMapping("datas/{id}/send")
	public String sendDataById(@PathVariable("id") Long id,
                               @RequestParam(required = false) List<String> recipientEmails, @RequestParam(required = false) List<String> targetEmails) throws EsupSignatureIOException, EsupSignatureException {
		User user = userService.getUserFromAuthentication();
		Data data = dataService.getDataById(id);
//		todo create signrequest
		dataService.sendForSign(data, recipientEmails, targetEmails, user);
		return "redirect:/user/" + user.getEppn() + "/data/" + id + "/update";
	}

	@PostMapping("datas/{id}/next-step")
	public String nextStep(@PathVariable("id") Long id,
                           @RequestParam(required = false) List<String> recipientEmails, @RequestParam(required = false) List<String> targetEmails, @PathVariable String eppn) {
		User user = userService.getUserFromSu(eppn);
		Data data = dataService.getDataById(id);
		dataService.nextStep(data, recipientEmails, user);
		return "redirect:/user/" + user.getEppn() + "/data/" + id;
	}

	@DeleteMapping("datas/{id}")
	public String deleteData(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
		User user = userService.getUserFromAuthentication();
		Data data = dataRepository.findById(id).get();
		if(user.getEppn().equals(data.getCreateBy())) {
			if(signRequestRepository.countByToken(data.getSignRequestToken()) > 0) {
				SignRequest signRequest = signRequestRepository.findByToken(data.getSignRequestToken()).get(0);
				signRequestService.delete(signRequest);
			}
			dataService.deleteData(id);
			redirectAttributes.addFlashAttribute("messageInfo", "Suppression effectuée");
		} else {
			redirectAttributes.addFlashAttribute("messageError", "Suppression impossible");
		}
		return "redirect:/user/datas/";
	}

	@GetMapping("/datas/{id}/export-pdf")
	public ResponseEntity exportToPdf(@PathVariable("id") Long id, HttpServletResponse response) {
		try {
			Data data = dataService.getDataById(id);
			InputStream exportPdf = dataService.generateFile(data);
			response.setHeader("Content-Disposition", "inline;filename=\"" + data.getName() + "\"");
			response.setContentType("application/pdf");
			IOUtils.copy(exportPdf, response.getOutputStream());
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (Exception e) {
			logger.error("get file error", e);
		}
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
	}

	@GetMapping("datas/{id}/reset")
	public String resetData(@PathVariable("id") Long id) {
		User user = userService.getUserFromAuthentication();
		Data data = dataService.getDataById(id);
		if(user.getEmail().equals(data.getCreateBy())) {
			SignRequest signRequest = signRequestRepository.findByToken(data.getSignRequestToken()).get(0);
			signRequestService.delete(signRequest);
			dataService.reset(data);
			return "redirect:/user/" + user.getEppn() + "/data/" + id;
		} else {
			return "";
		}

	}

	@GetMapping("datas/{id}/clone")
	public String cloneData(@PathVariable("id") Long id) {
		User user = userService.getUserFromAuthentication();
		Data data = dataService.getDataById(id);
		long nbDatas = dataRepository.countByNameStartsWith(data.getName());
		if(user.getEmail().equals(data.getCreateBy())) {
			Data cloneData = new Data();
			cloneData.setName(data.getName() + "(" + nbDatas + ")");
			cloneData.setStatus(SignRequestStatus.draft);
			cloneData.setCreateBy(userService.getUserFromAuthentication().getEppn());
			cloneData.setCreateDate(new Date());
			cloneData.setOwner(data.getOwner());
			cloneData.getDatas().putAll(data.getDatas());
			cloneData.setForm(data.getForm());
			dataService.updateData(cloneData);
			return "redirect:/user/" + user.getEppn() + "/data/" + cloneData.getId();
		} else {
			return "";
		}
	}
}