package org.esupportail.esupsignature.web.controller.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.esupportail.esupsignature.web.ws.json.JsonExternalUserInfo;
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
import java.util.List;
import java.util.Map;

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

	@PostMapping("sendForm/{id}")
	public String updateData(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
							 @RequestParam(required = false) List<String> recipientEmails,
							 @RequestParam(required = false) List<String> targetEmails,
							 @RequestParam(value = "emails", required = false) List<String> emails,
							 @RequestParam(value = "names", required = false) List<String> names,
							 @RequestParam(value = "firstnames", required = false) List<String> firstnames,
							 @RequestParam(value = "phones", required = false) List<String> phones,
							 @PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) throws EsupSignatureIOException, EsupSignatureException {
		User user = (User) model.getAttribute("user");
		User authUser = (User) model.getAttribute("authUser");
		List<JsonExternalUserInfo> externalUsersInfos = userService.getJsonExternalUserInfos(emails, names, firstnames, phones);
		if(formService.isFormAuthorized(userEppn, authUserEppn, id)) {
			Data data = dataService.addData(id, user, authUser);
			SignBook signBook = dataService.sendForSign(data, recipientEmails, externalUsersInfos, targetEmails, user, authUser);
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
		return "redirect:/user/signrequests/" + data.getSignBook().getSignRequests().get(0).getId();
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
		try {
			SignBook signBook = dataService.initSendData(id, userEppn, recipientEmails, targetEmails, authUserEppn);
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

	@PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserEppn)")
	@PostMapping("{id}/clone-from-signrequests")
	public String cloneDataFromSignRequest(@ModelAttribute("serEppn") String userEppn,
										   @ModelAttribute("authUserEppn") String authUserEppn,
										   @PathVariable("id") Long id,
										   @RequestParam(required = false) List<String> recipientEmails,
										   @RequestParam(required = false) List<String> targetEmails, RedirectAttributes redirectAttributes) throws EsupSignatureIOException, EsupSignatureException {
		SignRequest signRequest = signRequestService.getById(id);
		SignBook signBook = dataService.cloneFromSignRequest(signRequest, userEppn, authUserEppn, recipientEmails, targetEmails);
		redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Le document a été cloné"));
		return "redirect:/user/signrequests/" + signBook.getSignRequests().get(0);
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
			response.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode(modelResponse.get("fileName").toString(), StandardCharsets.UTF_8.toString()));
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

}