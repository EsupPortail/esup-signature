package org.esupportail.esupsignature.web.controller.user;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.repository.*;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RequestMapping("/user/datas")
@Controller
@Transactional
public class DataController {

	private static final Logger logger = LoggerFactory.getLogger(DataController.class);

	@ModelAttribute("userMenu")
	public String getActiveRole() {
		return "active";
	}

	@ModelAttribute("activeMenu")
	public String getActiveMenu() {
		return "datas";
	}


	@ModelAttribute(value = "user", binding = false)
	public User getUser() {
		return userService.getCurrentUser();
	}

	@ModelAttribute(value = "authUser", binding = false)
	public User getAuthUser() {
		return userService.getUserFromAuthentication();
	}

	@ModelAttribute(value = "suUsers", binding = false)
	public List<User> getSuUsers() {
		return userService.getSuUsers(getAuthUser());
	}

	@Resource
	private DataService dataService;

	@Resource
	private DataRepository dataRepository;

	@Resource
	private FormService formService;

	@Resource
	private SignRequestRepository signRequestRepository;

	@Resource
	private FormRepository formRepository;

	@Resource
	private UserService userService;

	@Resource
	private UserPropertieRepository userPropertieRepository;

	@Resource
	private PreFillService preFillService;

	@Resource
	private SignBookService signBookService;

	@Resource
	private RecipientService recipientService;

	@ModelAttribute("forms")
	public List<Form> getForms(User user, User authUser) {
		return 	formService.getFormsByUser(user, authUser);
	}

	@GetMapping
	public String list(User user, @SortDefault(value = "createDate", direction = Direction.DESC) @PageableDefault(size = 3) Pageable pageable, Model model) {
		//User user = userService.getCurrentUser();
		Page<Data> datas =  dataRepository.findByCreateByAndStatus(user.getEppn(), SignRequestStatus.draft, pageable);
		model.addAttribute("datas", datas);
		return "user/datas/list";
	}

	@GetMapping("{id}")
	public String show(User user, @PathVariable("id") Long id, @RequestParam(required = false) Integer page, Model model) {
		//User user = userService.getCurrentUser();
		Data data = dataService.getDataById(id);
		model.addAttribute("data", data);
		if (user.getEppn().equals(data.getOwner())) {
			if (page == null) {
				page = 1;
			}
			model.addAttribute("page", page);
			model.addAttribute("fields", data.getDatas());
			return "user/datas/show";
		} else {
			return "redirect:/";
		}
	}

	@GetMapping("form/{id}")
	public String updateData(User user, @PathVariable("id") Long id, @RequestParam(required = false) Integer page, Model model, RedirectAttributes redirectAttributes) {
		//User user = userService.getCurrentUser();
		List<Form> autorizedForms = formRepository.findAutorizedFormByUser(user);
		Form form = formService.getFormById(id);
		if(autorizedForms.contains(form) && userService.checkServiceShare(UserShare.ShareType.create, form)) {
			if (page == null) {
				page = 1;
			}
			model.addAttribute("form", form);
			if (form.getPreFillType() != null && !form.getPreFillType().isEmpty()) {
				Integer finalPage = page;
				List<Field> fields = form.getFields().stream().filter(field -> field.getPage() == finalPage).collect(Collectors.toList());
				List<Field> prefilledFields = preFillService.getPreFilledFieldsByServiceName(form.getPreFillType(), fields, user);
				for (Field field : prefilledFields) {
					if(!field.getStepNumbers().contains("0")) {
						field.setDefaultValue("");
					}
				}
				model.addAttribute("fields", prefilledFields);
			} else {
				model.addAttribute("fields", form.getFields());
			}

			model.addAttribute("data", new Data());
			model.addAttribute("activeForm", form.getName());
			model.addAttribute("page", page);
			if (form.getPdfDisplay()) {
				return "user/datas/create-pdf";
			} else {
				return "user/datas/create";
			}
		} else {
			redirectAttributes.addFlashAttribute("messageError", "Formulaire non autorisé");
			return "redirect:/user/";
		}

	}

	@PreAuthorize("@dataService.preAuthorizeUpdate(#id, #user)")
	@GetMapping("{id}/update")
	public String updateData(User user, @PathVariable("id") Long id, Model model) {
		//User user = userService.getCurrentUser();
		Data data = dataService.getDataById(id);
		model.addAttribute("data", data);
		if(data.getStatus().equals(SignRequestStatus.draft)) {
			Form form = data.getForm();
			if (!data.getStatus().equals(SignRequestStatus.draft) && user.getEmail().equals(data.getCreateBy())) {
				return "redirect:/user/" + user.getEppn() + "/data/" + data.getId();
			}
			List<Field> fields = form.getFields();
			for (Field field : fields) {
				field.setDefaultValue(data.getDatas().get(field.getName()));
			}
			List<UserPropertie> userProperties = userPropertieRepository.findByUserAndStepAndForm(user, 0, form);
			userProperties = userProperties.stream().sorted(Comparator.comparing(UserPropertie::getId).reversed()).collect(Collectors.toList());
			if(userProperties.size() > 0 ) {
				model.addAttribute("targetEmails", userProperties.get(0).getTargetEmail().split(","));
			}
			Workflow workflow = dataService.getWorkflowByDataAndUser(data, null, user);
			model.addAttribute("steps", workflow.getWorkflowSteps());
			model.addAttribute("fields", fields);
			model.addAttribute("form", form);
			model.addAttribute("activeForm", form.getName());
			model.addAttribute("document", form.getDocument());
			if (data.getSignBook() != null && recipientService.needSign(data.getSignBook().getSignRequests().get(0).getRecipients(), user)) {
				model.addAttribute("toSign", true);
			}
			return "user/datas/create-pdf";
		} else {
			return "redirect:/user/datas/" + data.getId();
		}
	}

	@PostMapping("form/{id}")
	public String addData(User user, @PathVariable("id") Long id, @RequestParam Long dataId, @RequestParam MultiValueMap<String, String> formData, RedirectAttributes redirectAttributes) {
		//User user = userService.getCurrentUser();
		Form form = formService.getFormById(id);
		formData.remove("_csrf");
		Data data;
		if(dataId != null) {
			data = dataRepository.findById(dataId).get();
		} else {
			data = new Data();
		}
		data = dataService.updateData(formData, user, form, data);
		redirectAttributes.addFlashAttribute("messageSuccess", "Données enregistrées");
		return "redirect:/user/datas/" + data.getId() + "/update";
	}

	@PutMapping("{id}")
	public String updateData(User user, @PathVariable("id") Long id, @RequestParam String name, @RequestParam(required = false) String navPage, @RequestParam(required = false) Integer page, @RequestParam MultiValueMap<String, String> formData, RedirectAttributes redirectAttributes) {
		//User user = userService.getCurrentUser();
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
//		if(user.getEppn().equals(data.getCreateBy())) {
//			dataService.updateData(data);
//		}
		redirectAttributes.addAttribute("page", page);
		if(navPage != null && !navPage.isEmpty()) {
			return "redirect:/user/" + user.getEppn() + "/data/" + data.getId() + "/update?page=" + page;
		} else {
			return "redirect:/user/" + user.getEppn() + "/data/" + data.getId();
		}
	}

	@PreAuthorize("@dataService.preAuthorizeUpdate(#id, #user)")
	@PostMapping("{id}/send")
	public String sendDataById(User user, @PathVariable("id") Long id,
                               @RequestParam(required = false) List<String> recipientEmails, @RequestParam(required = false) List<String> targetEmails, RedirectAttributes redirectAttributes) throws EsupSignatureIOException{
		//User user = userService.getCurrentUser();
		Data data = dataService.getDataById(id);
		try {
			SignBook signBook = dataService.sendForSign(data, recipientEmails, targetEmails, user);
			return "redirect:/user/signrequests/" + signBook.getSignRequests().get(0).getId();
		} catch (EsupSignatureException e) {
			logger.error(e.getMessage(), e);
			redirectAttributes.addFlashAttribute("messageError", e.getMessage());
		}
		redirectAttributes.addFlashAttribute("messageSuccess", "La procédure est démarrée");
		return "redirect:/user/datas/" + id + "/update";
	}

	@DeleteMapping("{id}")
	public String deleteData(User user, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
		//User user = userService.getCurrentUser();
		Data data = dataRepository.findById(id).get();
		if(user.getEppn().equals(data.getCreateBy())) {
			dataService.delete(data);
			redirectAttributes.addFlashAttribute("messageInfo", "Suppression effectuée");
		} else {
			redirectAttributes.addFlashAttribute("messageError", "Suppression impossible");
		}
		return "redirect:/user/datas/";
	}

	@GetMapping("{id}/export-pdf")
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

	@GetMapping("{id}/reset")
	public String resetData(User user, @PathVariable("id") Long id) {
		//User user = userService.getCurrentUser();
		Data data = dataService.getDataById(id);
		if(user.getEmail().equals(data.getCreateBy())) {
			signBookService.delete(data.getSignBook());
			dataService.reset(data);
			return "redirect:/user/" + user.getEppn() + "/data/" + id;
		} else {
			return "";
		}

	}

	@PreAuthorize("@dataService.preAuthorizeUpdate(#id, #user)")
	@GetMapping("{id}/clone")
	public String cloneData(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
		//User user = userService.getCurrentUser();
		Data data = dataService.getDataById(id);
		Data cloneData = dataService.cloneData(data);
		redirectAttributes.addFlashAttribute("messageInfo", "Le document a été cloné");
		return "redirect:/user/datas/" + cloneData.getId() + "/update";
	}

	@PreAuthorize("@signRequestService.preAuthorizeOwner(#id, #authUser)")
	@GetMapping("{id}/clone-from-signrequests")
	public String cloneDataFromSignRequest(User authUser, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
		//User user = userService.getCurrentUser();
		SignRequest signRequest = signRequestRepository.findById(id).get();
		Data data = dataService.getDataFromSignRequest(signRequest);
		Data cloneData = dataService.cloneData(data);
		redirectAttributes.addFlashAttribute("messageInfo", "Le document a été cloné");
		return "redirect:/user/datas/" + cloneData.getId() + "/update";
	}

}