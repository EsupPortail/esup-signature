package org.esupportail.esupsignature.web.controller.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.DataService;
import org.esupportail.esupsignature.service.FormService;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.esupportail.esupsignature.web.ws.json.JsonExternalUserInfo;
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.HashMap;
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
	private SignBookService signBookService;

	@Resource
	private FormService formService;

	@Resource
	private PdfService pdfService;

	@Resource
	private UserService userService;

	@Resource
	private ObjectMapper objectMapper;

	@PostMapping("/send-form/{id}")
	public String sendForm(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
						   @RequestParam(required = false) List<String> recipientEmails,
						   @RequestParam(required = false) List<String> signTypes,
						   @RequestParam(required = false) List<String> allSignToCompletes,
						   @RequestParam(required = false) List<String> targetEmails,
						   @RequestParam(value = "emails", required = false) List<String> emails,
						   @RequestParam(value = "names", required = false) List<String> names,
						   @RequestParam(value = "firstnames", required = false) List<String> firstnames,
						   @RequestParam(value = "phones", required = false) List<String> phones,
						   @RequestParam(value = "forcesmses", required = false) List<String> forcesmses,
						   @PathVariable("id") Long id, RedirectAttributes redirectAttributes) throws EsupSignatureRuntimeException {
		List<JsonExternalUserInfo> externalUsersInfos = userService.getJsonExternalUserInfos(emails, names, firstnames, phones, forcesmses);
		if(formService.isFormAuthorized(userEppn, authUserEppn, id)) {
			Data data = dataService.addData(id, userEppn);
			SignBook signBook = signBookService.sendForSign(data.getId(), recipientEmails, signTypes, allSignToCompletes, externalUsersInfos, targetEmails, null, userEppn, authUserEppn, false, null, null, null, null);
			return "redirect:/user/signrequests/" + signBook.getSignRequests().get(0).getId();
		} else {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Formulaire non autorisé"));
			return "redirect:/user";
		}

	}

	@PostMapping("/form/{id}")
	@ResponseBody
	public String addData(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
						  @PathVariable("id") Long id,
						  @RequestParam String dataId,
						  @RequestParam MultiValueMap<String, String> formData, Model model,
						  RedirectAttributes redirectAttributes) throws JsonProcessingException {
		User user = (User) model.getAttribute("user");
		User authUser = userService.getByEppn(authUserEppn);
		TypeReference<HashMap<String, String>> type = new TypeReference<>(){};
		Map<String, String> datas = objectMapper.readValue(formData.getFirst("formData"), type);
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

	@GetMapping("/forms/{id}/get-image")
	public ResponseEntity<Void> getImagePdfAsByteArray(@PathVariable("id") Long id, HttpServletResponse httpServletResponse) throws Exception {
		Form form = formService.getById(id);
		InputStream in = pdfService.pageAsInputStream(form.getDocument().getInputStream(), 0);
		httpServletResponse.setContentType(MediaType.IMAGE_PNG_VALUE);
		IOUtils.copy(in, httpServletResponse.getOutputStream());
		in.close();
		return new ResponseEntity<>(HttpStatus.OK);
	}

}