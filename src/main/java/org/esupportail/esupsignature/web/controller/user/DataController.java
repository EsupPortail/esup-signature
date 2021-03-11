package org.esupportail.esupsignature.web.controller.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Hidden;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Hidden
@Controller
@RequestMapping("/user/datas")
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
	private RecipientService recipientService;

	@Resource
	private PdfService pdfService;

	@Resource
	private UserService userService;

	@Resource
	private PreAuthorizeService preAuthorizeService;

	@Resource
	private FieldPropertieService fieldPropertieService;

	@GetMapping

	public String list(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @SortDefault(value = "createDate", direction = Direction.DESC) @PageableDefault(size = 10) Pageable pageable, Model model) {
		List<Data> datas = dataService.getDataDraftByOwner(userEppn);
		model.addAttribute("forms", formService.getFormsByUser(userEppn, authUserEppn));
		model.addAttribute("workflows", workflowService.getWorkflowsByUser(userEppn, authUserEppn));
		model.addAttribute("datas", dataService.getDatasPaged(datas, pageable, userEppn, authUserEppn));
		return "user/datas/list";
	}

	@PreAuthorize("@preAuthorizeService.dataUpdate(#id, #userEppn)")
	@GetMapping("{id}")
	public String show(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id, @RequestParam(required = false) Integer page, Model model) {
		Data data = dataService.getById(id);
		User user = userService.getByEppn(userEppn);
		model.addAttribute("data", data);
		if (user.equals(data.getOwner())) {
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
	public String updateData(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
							 @PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) throws EsupSignatureIOException, EsupSignatureException {
		User user = (User) model.getAttribute("user");
		User authUser = (User) model.getAttribute("authUser");
		if(formService.isFormAuthorized(userEppn, authUserEppn, id)) {
			Data data = dataService.addData(id, user, authUser);
			SignBook signBook = dataService.sendForSign(data, new ArrayList<>(), new ArrayList<>(), user, authUser);
			return "redirect:/user/signrequests/" + signBook.getSignRequests().get(0).getId();
		} else {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Formulaire non autorisé"));
			return "redirect:/user/";
		}

	}

	@PreAuthorize("@preAuthorizeService.dataUpdate(#id, #userEppn)")
	@GetMapping("{id}/update")
	public String updateData(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id, Model model) throws EsupSignatureException {
		Data data = dataService.getById(id);
		model.addAttribute("data", data);
		if(data.getStatus().equals(SignRequestStatus.draft)) {
			Form form = data.getForm();
			model.addAttribute("fields", dataService.setFieldsDefaultsValues(data, form, userService.getUserByEppn(userEppn)));
			if (data.getSignBook() != null && recipientService.needSign(data.getSignBook().getLiveWorkflow().getCurrentStep().getRecipients(), userEppn)) {
				model.addAttribute("toSign", true);
			}
			Workflow workflow = workflowService.computeWorkflow(data.getForm().getWorkflow().getId(), null, userEppn, true);
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
	public String addData(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
						  @PathVariable("id") Long id,
						  @RequestParam String dataId,
						  @RequestParam MultiValueMap<String, String> formData, Model model,
						  RedirectAttributes redirectAttributes) throws JsonProcessingException {
		User user = (User) model.getAttribute("user");
		User authUser = (User) model.getAttribute("authUser");
		ObjectMapper objectMapper = new ObjectMapper();
		Map<String, String> datas = objectMapper.readValue(formData.getFirst("formData"), Map.class);
		Long dataLongId = null;
		try {
			dataLongId = Long.valueOf(dataId);
		} catch (NumberFormatException e) {
			logger.debug("dataId is null");
		}
		Data data = dataService.addData(id, dataLongId , datas, user, authUser);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Données enregistrées"));
		return data.getId().toString();
	}

	@PreAuthorize("@preAuthorizeService.dataUpdate(#id, #userEppn)")
	@PostMapping("{id}/send")
	public String sendDataById(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
							   @PathVariable("id") Long id,
                               @RequestParam(required = false) List<String> recipientEmails,
							   @RequestParam(required = false) List<String> targetEmails,
							   Model model, RedirectAttributes redirectAttributes) throws EsupSignatureIOException {
		User user = (User) model.getAttribute("user");
		User authUser = (User) model.getAttribute("authUser");
		try {
			SignBook signBook = dataService.initSendData(id, user, recipientEmails, targetEmails, authUser);
			if(signBook.getLiveWorkflow().getWorkflow().getWorkflowSteps().get(0).getUsers().get(0).getEppn().equals(userEppn)) {
				redirectAttributes.addFlashAttribute("message", new JsonMessage("warn", "Vous devez maintenant signer cette demande"));
				return "redirect:/user/signrequests/" + signBook.getSignRequests().get(0).getId();
			} else {
				redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Demande envoyée"));
				return "redirect:/user/";
			}
		} catch (EsupSignatureException e) {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", e.getMessage()));
		}
		return "redirect:/user/datas/" + id + "/update";
	}

	@PreAuthorize("@preAuthorizeService.dataUpdate(#id, #userEppn)")
	@DeleteMapping("{id}")
	public String deleteData(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
		dataService.delete(id);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Suppression effectuée"));
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

	@PreAuthorize("@preAuthorizeService.dataUpdate(#id, #userEppn)")
	@GetMapping("{id}/clone")
	public String cloneData(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
		User user = (User) model.getAttribute("user");
		Data data = dataService.getById(id);
		Data cloneData = dataService.cloneData(data, user);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Le document a été cloné"));
		return "redirect:/user/datas/" + cloneData.getId() + "/update";
	}

	@PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserEppn)")
	@GetMapping("{id}/clone-from-signrequests")
	public String cloneDataFromSignRequest(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
		User authUser = (User) model.getAttribute("authUser");
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

	@GetMapping(value = "/get-favorites/{id}")
	@ResponseBody
	public List<String> getFavorites(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id) {
		return fieldPropertieService.getFavoritesValues(authUserEppn, id);
	}

	@PostMapping(value = "/delete-multiple", consumes = {"application/json"})
	@ResponseBody
	public ResponseEntity<Boolean> deleteMultiple(@ModelAttribute("authUserEppn") String authUserEppn, @RequestBody List<Long> ids, RedirectAttributes redirectAttributes) {
		for(Long id : ids) {
			if(preAuthorizeService.dataUpdate(id, authUserEppn)) {
				dataService.delete(id);
			}
		}
//		eventService.publishEvent(new JsonMessage("info", "Brouillon(s) supprimé(s)"), "user", eventService.getClientIdByEppn(authUserEppn));
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Suppression effectuée"));
		return new ResponseEntity<>(true, HttpStatus.OK);
	}

}