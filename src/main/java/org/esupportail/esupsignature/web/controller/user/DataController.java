package org.esupportail.esupsignature.web.controller.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.esupportail.esupsignature.web.controller.ws.json.JsonMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RequestMapping("/user/datas")
@Controller

public class DataController {

	private static final Logger logger = LoggerFactory.getLogger(DataController.class);

	@ModelAttribute("activeMenu")
	public String getActiveMenu() {
		return "datas";
	}

	@Resource
	private DataService dataService;

	@Resource
	private WorkflowService workflowService;

	@Resource
	private FormService formService;

	@Resource
	private SignRequestService signRequestService;

	@Resource
	private UserShareService userShareService;

	@Resource
	private RecipientService recipientService;

	@Resource
	private PdfService pdfService;

	@Resource
	private UserService userService;

	@GetMapping

	public String list(@ModelAttribute("userId") Long userId, @ModelAttribute("authUserId") Long authUserId, @SortDefault(value = "createDate", direction = Direction.DESC) @PageableDefault(size = 10) Pageable pageable, Model model) {
		User user = userService.getById(userId);
		User authUser = userService.getById(authUserId);
		List<Data> datas = dataService.getDataDraftByOwner(userId);
		model.addAttribute("forms", formService.getFormsByUser(userId, authUserId));
		model.addAttribute("workflows", workflowService.getWorkflowsByUser(userId, authUserId));
		model.addAttribute("datas", dataService.getDatasPaged(datas, pageable, userId, authUserId));
		return "user/datas/list";
	}

	@PreAuthorize("@preAuthorizeService.dataUpdate(#id, #userId)")
	@GetMapping("{id}")
	public String show(@ModelAttribute("userId") Long userId, @PathVariable("id") Long id, @RequestParam(required = false) Integer page, Model model) {
		User user = userService.getById(userId);
		Data data = dataService.getById(id);
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
	public String updateData(@ModelAttribute("userId") Long userId, @ModelAttribute("authUserId") Long authUserId,
							 @PathVariable("id") Long id,
							 @RequestParam(required = false) Integer page, Model model, RedirectAttributes redirectAttributes) {
		User user = userService.getById(userId);
		if(formService.isFormAuthorized(userId, authUserId, id)) {
			if (page == null) {
				page = 1;
			}
			Form form = formService.getById(id);
			model.addAttribute("form", form);
			model.addAttribute("fields", dataService.getPrefilledFields(user, page, form));
			model.addAttribute("data", new Data());
			model.addAttribute("activeForm", form.getName());
			model.addAttribute("page", page);
			String message = formService.getHelpMessage(user, form);
			if(message != null) {
				model.addAttribute("message", new JsonMessage("help", message));
			}
			return "user/datas/create";
		} else {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Formulaire non autorisé"));
			return "redirect:/user/";
		}

	}

	@PreAuthorize("@preAuthorizeService.dataUpdate(#id, #userId)")
	@GetMapping("{id}/update")
	public String updateData(@ModelAttribute("userId") Long userId, @PathVariable("id") Long id, Model model) throws EsupSignatureException {
		User user = userService.getById(userId);
		Data data = dataService.getById(id);
		model.addAttribute("data", data);
		if(data.getStatus().equals(SignRequestStatus.draft)) {
			Form form = data.getForm();
			model.addAttribute("fields", dataService.setFieldsDefaultsValues(data, form));
			model.addAttribute("targetEmails", workflowService.getTargetEmails(user.getId(), form));
			if (data.getSignBook() != null && recipientService.needSign(data.getSignBook().getLiveWorkflow().getCurrentStep().getRecipients(), userId)) {
				model.addAttribute("toSign", true);
			}
			Workflow workflow = workflowService.computeWorkflow(workflowService.getWorkflowByName(data.getForm().getWorkflowType()), null, user, true);
			model.addAttribute("steps", workflow.getWorkflowSteps());
			model.addAttribute("form", form);
			model.addAttribute("activeForm", form.getName());
			model.addAttribute("document", form.getDocument());
			return "user/datas/create";
		} else {
			return "redirect:/user/datas/" + data.getId();
		}
	}

	@PostMapping("form/{id}")
	@ResponseBody
	public String addData(@ModelAttribute("userId") Long userId, @ModelAttribute("authUserId") Long authUserId, @PathVariable("id") Long id,
						  @RequestParam Long dataId,
						  @RequestParam MultiValueMap<String, String> formData,
						  RedirectAttributes redirectAttributes) throws JsonProcessingException {
		User user = userService.getById(userId);
		User authUser = userService.getById(authUserId);
		ObjectMapper objectMapper = new ObjectMapper();
		Map<String, String> datas = objectMapper.readValue(formData.getFirst("formData"), Map.class);
		Data data = dataService.addData(user, id, dataId, datas, authUser);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Données enregistrées"));
		return "" + data.getId();
	}

//	@PutMapping("{id}")
//	public String updateData(@ModelAttribute("userId") Long userId, @PathVariable("id") Long id, @RequestParam String name, @RequestParam(required = false) String navPage, @RequestParam(required = false) Integer page, @RequestParam MultiValueMap<String, String> formData, RedirectAttributes redirectAttributes) {
//		User user = userService.getUserById(userId);
//		Data data = dataService.getById(id);
//		if(page == null) {
//			page = 1;
//		}
//		if("next".equals(navPage)) {
//			page++;
//		} else if("prev".equals(navPage)) {
//			page--;
//		}
//		dataService.setDatas(name, formData, data);
//		redirectAttributes.addAttribute("page", page);
//		if(navPage != null && !navPage.isEmpty()) {
//			return "redirect:/user/" + user.getEppn() + "/data/" + data.getId() + "/update?page=" + page;
//		} else {
//			return "redirect:/user/" + user.getEppn() + "/data/" + data.getId();
//		}
//	}

	@PreAuthorize("@preAuthorizeService.dataUpdate(#id, #userId)")
	@PostMapping("{id}/send")
	public String sendDataById(@ModelAttribute("userId") Long userId, @ModelAttribute("authUserId") Long authUserId, @PathVariable("id") Long id,
                               @RequestParam(required = false) List<String> recipientEmails, @RequestParam(required = false) List<String> targetEmails, RedirectAttributes redirectAttributes) throws EsupSignatureIOException{
		User user = userService.getById(userId);
		User authUser = userService.getById(authUserId);
		try {
			SignBook signBook = dataService.initSendData(id, user, recipientEmails, targetEmails, authUser);
			redirectAttributes.addFlashAttribute("message", new JsonMessage("success", signBook.getComment()));
			return "redirect:/user/signrequests/" + signBook.getSignRequests().get(0).getId();

		} catch (EsupSignatureException e) {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", e.getMessage()));
		}
		return "redirect:/user/signrequests/";
	}

	@PreAuthorize("@preAuthorizeService.dataUpdate(#id, #userId)")
	@DeleteMapping("{id}")
	public String deleteData(@ModelAttribute("userId") Long userId, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
		User user = userService.getById(userId);
		Data data = dataService.getById(id);
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
			Data data = dataService.getById(id);
			InputStream exportPdf = dataService.generateFile(data);
			response.setHeader("Content-disposition", "inline; filename=" + URLEncoder.encode(data.getName(), StandardCharsets.UTF_8.toString()));
			response.setContentType("application/pdf");
			IOUtils.copy(exportPdf, response.getOutputStream());
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (Exception e) {
			logger.error("get file error", e);
		}
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
	}

	@PreAuthorize("@preAuthorizeService.dataUpdate(#id, #userId)")
	@GetMapping("{id}/clone")
	public String cloneData(@ModelAttribute("userId") Long userId, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
		User user = userService.getById(userId);
		Data data = dataService.getById(id);
		Data cloneData = dataService.cloneData(data, user);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Le document a été cloné"));
		return "redirect:/user/datas/" + cloneData.getId() + "/update";
	}

	@PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserId)")
	@GetMapping("{id}/clone-from-signrequests")
	public String cloneDataFromSignRequest(@ModelAttribute("authUserId") Long authUserId, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
		User authUser = userService.getById(authUserId);
		SignRequest signRequest = signRequestService.getById(id);
		Data cloneData = dataService.cloneFromSignRequest(signRequest, authUser);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Le document a été cloné"));
		return "redirect:/user/datas/" + cloneData.getId() + "/update";
	}

	@GetMapping("forms/{id}/get-image")
	public ResponseEntity<Void> getImagePdfAsByteArray(@PathVariable("id") Long id, HttpServletResponse httpServletResponse) throws Exception {
		Form form = formService.getById(id);
		InputStream in = pdfService.pageAsInputStream(form.getDocument().getInputStream(), 0);
		httpServletResponse.setContentType(MediaType.IMAGE_PNG_VALUE);
		IOUtils.copy(in, httpServletResponse.getOutputStream());
		in.close();
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@GetMapping(value = "/get-model/{id}")

	public ResponseEntity<Void> getFile(@PathVariable("id") Long id, HttpServletResponse response) {
		try {
			Map<String, Object> modelResponse = dataService.getModelResponse(id);
			response.setHeader("Content-disposition", "inline; filename=" + URLEncoder.encode(modelResponse.get("fileName").toString(), StandardCharsets.UTF_8.toString()));
			response.setContentType(modelResponse.get("contentType").toString());
			IOUtils.copy((InputStream) modelResponse.get("inputStream"), response.getOutputStream());
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (Exception e) {
			logger.error("get file error", e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}