package org.esupportail.esupsignature.web.controller.user;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.repository.DataRepository;
import org.esupportail.esupsignature.repository.FormRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.repository.UserPropertieRepository;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.esupportail.esupsignature.service.prefill.PreFillService;
import org.esupportail.esupsignature.web.controller.ws.json.JsonMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import java.util.*;
import java.util.stream.Collectors;

@RequestMapping("/user/datas")
@Controller
@Transactional
public class DataController {

	private static final Logger logger = LoggerFactory.getLogger(DataController.class);

	@ModelAttribute("activeMenu")
	public String getActiveMenu() {
		return "datas";
	}

	@Resource
	private DataService dataService;

	@Resource
	private DataRepository dataRepository;

	@Resource
	private WorkflowService workflowService;

	@Resource
	private FormService formService;

	@Resource
	private SignRequestRepository signRequestRepository;

	@Resource
	private FormRepository formRepository;

	@Resource
	private UserShareService userShareService;

	@Resource
	private UserPropertieRepository userPropertieRepository;

	@Resource
	private PreFillService preFillService;

	@Resource
	private SignBookService signBookService;

	@Resource
	private RecipientService recipientService;

	@Resource
	private PdfService pdfService;

	@ModelAttribute("forms")
	public List<Form> getForms(@ModelAttribute("user") User user, User authUser) {
		return 	formService.getFormsByUser(user, authUser);
	}

	@GetMapping
	public String list(@ModelAttribute("user") User user, @ModelAttribute("authUser") User authUser, @SortDefault(value = "createDate", direction = Direction.DESC) @PageableDefault(size = 10) Pageable pageable, Model model) {
		List<Data> datas =  dataRepository.findByOwnerAndStatus(user.getEppn(), SignRequestStatus.draft);
		Page<Data> datasPage;
		if(!user.equals(authUser)) {
			List<Data> datasOk = new ArrayList<>();
			for(Data data : datas) {
				if(userShareService.checkFormShare(user, authUser, ShareType.create, data.getForm())) {
					datasOk.add(data);
				}
			}
			datasPage = new PageImpl<>(datasOk, pageable, datas.size());
		} else {
			datasPage = new PageImpl<>(datas, pageable, datas.size());
		}

		model.addAttribute("forms", formService.getFormsByUser(user, authUser));
		model.addAttribute("workflows", workflowService.getWorkflowsByUser(user, authUser));
		model.addAttribute("datas", datasPage);
		return "user/datas/list";
	}

	@PreAuthorize("@dataService.preAuthorizeUpdate(#id, #user)")
	@GetMapping("{id}")
	public String show(@ModelAttribute("user") User user, @PathVariable("id") Long id, @RequestParam(required = false) Integer page, Model model) {
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
	public String updateData(@ModelAttribute("user") User user, @ModelAttribute("authUser") User authUser,
							 @PathVariable("id") Long id,
							 @RequestParam(required = false) Integer page, Model model, RedirectAttributes redirectAttributes) {
		List<Form> authorizedForms = formRepository.findAuthorizedFormByUser(user);
		Form form = formService.getFormById(id);
		if(authorizedForms.contains(form) && userShareService.checkFormShare(user, authUser, ShareType.create, form)) {
			if (page == null) {
				page = 1;
			}
			model.addAttribute("form", form);
			if (form.getPreFillType() != null && !form.getPreFillType().isEmpty()) {
				Integer finalPage = page;
				List<Field> fields = form.getFields().stream().filter(field -> field.getPage() == null || field.getPage().equals(finalPage)).collect(Collectors.toList());
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
			boolean sendMessage = true;
			if(user.getFormMessages() != null) {
				String[] formMessages = user.getFormMessages().split(" ");
				if(Arrays.asList(formMessages).contains(form.getId().toString())) {
					sendMessage = false;
				}
			}
			if(sendMessage) model.addAttribute("message", new JsonMessage("help", form.getMessage()));
			if (form.getDocument() != null) {
				return "user/datas/create-pdf";
			} else {
				return "user/datas/create";
			}
		} else {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Formulaire non autorisé"));
			return "redirect:/user/";
		}

	}

	@PreAuthorize("@dataService.preAuthorizeUpdate(#id, #user)")
	@GetMapping("{id}/update")
	public String updateData(@ModelAttribute("user") User user, @PathVariable("id") Long id, Model model) throws EsupSignatureException {
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
			Workflow workflow = workflowService.getWorkflowByDataAndUser(data, null, user);
			model.addAttribute("steps", workflow.getWorkflowSteps());
			model.addAttribute("fields", fields);
			model.addAttribute("form", form);
			model.addAttribute("activeForm", form.getName());
			model.addAttribute("document", form.getDocument());
			if (data.getSignBook() != null && recipientService.needSign(data.getSignBook().getSignRequests().get(0).getRecipients(), user)) {
				model.addAttribute("toSign", true);
			}
			if(form.getDocument() != null) {
				return "user/datas/create-pdf";
			} else {
				return "user/datas/create";
			}
		} else {
			return "redirect:/user/datas/" + data.getId();
		}
	}

	@PostMapping("form/{id}")
	public String addData(@ModelAttribute("user") User user, @PathVariable("id") Long id,
						  @RequestParam Long dataId,
						  @RequestParam MultiValueMap<String, String> formData,
						  RedirectAttributes redirectAttributes) {
		Form form = formService.getFormById(id);
		formData.remove("_csrf");
		formData.remove("dataId");
		Data data;
		if(dataId != null) {
			data = dataRepository.findById(dataId).get();
		} else {
			data = new Data();
		}
		data = dataService.updateData(formData, user, form, data);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Données enregistrées"));
		return "redirect:/user/datas/" + data.getId() + "/update";
	}

	@PutMapping("{id}")
	public String updateData(@ModelAttribute("user") User user, @PathVariable("id") Long id, @RequestParam String name, @RequestParam(required = false) String navPage, @RequestParam(required = false) Integer page, @RequestParam MultiValueMap<String, String> formData, RedirectAttributes redirectAttributes) {
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
	public String sendDataById(@ModelAttribute("user") User user, @PathVariable("id") Long id,
                               @RequestParam(required = false) List<String> recipientEmails, @RequestParam(required = false) List<String> targetEmails, RedirectAttributes redirectAttributes) throws EsupSignatureIOException{
		Data data = dataService.getDataById(id);
			if(data.getStatus().equals(SignRequestStatus.draft)) {
			try {
				SignBook signBook = dataService.sendForSign(data, recipientEmails, targetEmails, user);
				redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "La procédure est démarrée"));
				return "redirect:/user/signrequests/" + signBook.getSignRequests().get(0).getId();
			} catch (EsupSignatureException | InterruptedException e) {
				logger.error(e.getMessage(), e);
				redirectAttributes.addFlashAttribute("message", new JsonMessage("error", e.getMessage()));
				return "redirect:/user/datas/" + id;
			}
		} else {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Attention, la procédure est déjà démarée"));
		}
		return "redirect:/user/signrequests/";
	}

	@PreAuthorize("@dataService.preAuthorizeUpdate(#id, #user)")
	@DeleteMapping("{id}")
	public String deleteData(@ModelAttribute("user") User user, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
		Data data = dataRepository.findById(id).get();
		if(user.getEppn().equals(data.getCreateBy()) || user.getEppn().equals(data.getOwner())) {
			dataService.delete(data);
			redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Suppression effectuée"));
		} else {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Suppression impossible"));
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
	public String resetData(@ModelAttribute("user") User user, @PathVariable("id") Long id) {
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
	public String cloneData(@ModelAttribute("user") User user, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
		Data data = dataService.getDataById(id);
		Data cloneData = dataService.cloneData(data);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Le document a été cloné"));
		return "redirect:/user/datas/" + cloneData.getId() + "/update";
	}

	@PreAuthorize("@signRequestService.preAuthorizeOwner(#id, #authUser)")
	@GetMapping("{id}/clone-from-signrequests")
	public String cloneDataFromSignRequest(@ModelAttribute("authUser") User authUser, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
		SignRequest signRequest = signRequestRepository.findById(id).get();
		Data data = dataService.getDataFromSignRequest(signRequest);
		Data cloneData = dataService.cloneData(data);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Le document a été cloné"));
		return "redirect:/user/datas/" + cloneData.getId() + "/update";
	}

	@GetMapping("forms/{id}/get-image")
	public ResponseEntity<Void> getImagePdfAsByteArray(@PathVariable("id") Long id, HttpServletResponse response) throws Exception {
		Form form = formRepository.findById(id).get();
		InputStream in = pdfService.pageAsInputStream(form.getDocument().getInputStream(), 0);
		response.setContentType(MediaType.IMAGE_PNG_VALUE);
		IOUtils.copy(in, response.getOutputStream());
		in.close();
		return new ResponseEntity<>(HttpStatus.OK);
	}

}