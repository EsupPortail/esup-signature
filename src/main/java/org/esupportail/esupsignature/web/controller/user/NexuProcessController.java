package org.esupportail.esupsignature.web.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.esig.dss.AbstractSignatureParameters;
import eu.europa.esig.dss.enumerations.SignatureForm;
import eu.europa.esig.dss.model.ToBeSigned;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dss.model.*;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.sign.SignService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
@SessionAttributes(value = { "signatureDocumentForm", "signRequestId", "parameters"})
@RequestMapping("/user/nexu-sign")
@Transactional
@EnableConfigurationProperties(GlobalProperties.class)
public class NexuProcessController {

	private static final Logger logger = LoggerFactory.getLogger(NexuProcessController.class);

	@ModelAttribute("userMenu")
	public String getActiveRole() {
		return "active";
	}

	@ModelAttribute("activeMenu")
	public String getActiveMenu() {
		return "signrequests";
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

	@ModelAttribute(value = "globalProperties")
	public GlobalProperties getGlobalProperties() {
		return this.globalProperties;
	}

	@Resource
	private GlobalProperties globalProperties;

	@Resource
	private SignService signService;

	@Resource
	private SignRequestRepository signRequestRepository;

	@Resource
	private UserService userService;
	
	@Resource
	private SignRequestService signRequestService;

	private AbstractSignatureParameters parameters;
	
	@GetMapping(value = "/{id}", produces = "text/html")
	public String showSignatureParameters(@ModelAttribute User user, @PathVariable("id") Long id, Model model,
										  @RequestParam(value = "referer", required = false) String referer, RedirectAttributes redirectAttrs) throws IOException, EsupSignatureException {
    	//User user = userService.getCurrentUser();
		SignRequest signRequest = signRequestRepository.findById(id).get();
		logger.info("init nexu sign by : " + user.getEppn() + " for signRequest : " + id);
		if (signRequestService.checkUserSignRights(user, signRequest)) {
			AbstractSignatureForm signatureDocumentForm;
			List<Document> toSignFiles = new ArrayList<>();
			for(Document document : signRequestService.getToSignDocuments(signRequest)) {
				toSignFiles.add(document);
			}
			signatureDocumentForm = signService.getSignatureDocumentForm(toSignFiles, signRequest, true);
			model.addAttribute("signRequestId", signRequest.getId());
			model.addAttribute("signatureDocumentForm", signatureDocumentForm);
			model.addAttribute("digestAlgorithm", signatureDocumentForm.getDigestAlgorithm());
			model.addAttribute("rootUrl", globalProperties.getRootUrl());
			model.addAttribute("nexuUrl", globalProperties.getNexuUrl());
			model.addAttribute("nexuVersion", globalProperties.getNexuVersion());
			model.addAttribute("referer", referer);
			return "user/signrequests/nexu-signature-process";
		} else {
			redirectAttrs.addFlashAttribute("messageCustom", "not authorized");
			return "redirect:/user/signrequests/";
		}
	}

	@PostMapping(value = "/get-data-to-sign")
	@ResponseBody
	public GetDataToSignResponse getDataToSign(@ModelAttribute User user, Model model,
								@RequestBody @Valid DataToSignParams params,
								@ModelAttribute("signatureDocumentForm") @Valid AbstractSignatureForm signatureDocumentForm,
								@ModelAttribute("signRequestId") Long signRequestId) throws IOException {
		logger.info("get data to sign for : " + signRequestId);
		signatureDocumentForm.setBase64Certificate(params.getSigningCertificate());
		signatureDocumentForm.setBase64CertificateChain(params.getCertificateChain());
		signatureDocumentForm.setEncryptionAlgorithm(params.getEncryptionAlgorithm());
		SignRequest signRequest = signRequestRepository.findById(signRequestId).get();
		if (signRequestService.checkUserSignRights(user, signRequest)) {
			ToBeSigned dataToSign;
			if(signatureDocumentForm.getClass().equals(SignatureMultipleDocumentsForm.class)) {
				parameters = signService.fillParameters((SignatureMultipleDocumentsForm) signatureDocumentForm);
				dataToSign = signService.getDataToSign((SignatureMultipleDocumentsForm) signatureDocumentForm);
			} else {
				if(signatureDocumentForm.getSignatureForm().equals(SignatureForm.PAdES)) {
					SignatureDocumentForm documentForm = (SignatureDocumentForm) signatureDocumentForm;
					parameters = signService.fillVisibleParameters((SignatureDocumentForm) signatureDocumentForm, signRequest.getSignRequestParams().get(signRequest.getSignedDocuments().size()), documentForm.getDocumentToSign(), user, false);
				} else {
					parameters = signService.fillParameters((SignatureDocumentForm) signatureDocumentForm);
				}
				dataToSign = signService.getDataToSign((SignatureDocumentForm) signatureDocumentForm, parameters);
			}
				
			if (dataToSign == null) {
				return null;
			}
//			model.addAttribute("parameters", parameters);
			model.addAttribute("signatureDocumentForm", signatureDocumentForm);
			model.addAttribute("signRequestId", signRequest.getId());
			GetDataToSignResponse responseJson = new GetDataToSignResponse();
			responseJson.setDataToSign(DatatypeConverter.printBase64Binary(dataToSign.getBytes()));
			//"dataToSign"
//			String testjson = objectMapper.writeValueAsString(responseJson);
//			return request.getParameter("callback") + "(" + testjson + ")";
			return responseJson;
		} else {
//			return request.getParameter("callback") + "(" + objectMapper.writeValueAsString(new GetDataToSignResponse()) + ")";
			return new GetDataToSignResponse();
		}
	}

	@PostMapping(value = "/sign-document")
	@ResponseBody
	public SignDocumentResponse signDocument(@ModelAttribute User user,@RequestBody @Valid SignatureValueAsString signatureValue,
			@ModelAttribute("signatureDocumentForm") @Valid AbstractSignatureForm signatureDocumentForm, 
			@ModelAttribute("signRequestId") Long signRequestId) throws EsupSignatureException {
		SignRequest signRequest = signRequestRepository.findById(signRequestId).get();
		if (signRequestService.checkUserSignRights(user, signRequest)) {
			SignDocumentResponse signedDocumentResponse;
			signatureDocumentForm.setBase64SignatureValue(signatureValue.getSignatureValue());
			try {
				Document signedFile = signRequestService.nexuSign(signRequest, user, signatureDocumentForm, parameters);
				if(signedFile != null) {
					signRequestService.updateStatus(signRequest, SignRequestStatus.signed, "Signature", "SUCCESS");

					signRequestService.applyEndOfStepRules(signRequest, user);
				}
			} catch (IOException e) {
				throw new EsupSignatureException("unable to sign" , e);
			}
	        signedDocumentResponse = new SignDocumentResponse();
	        signedDocumentResponse.setUrlToDownload("download");
//			String testjson = objectMapper.writeValueAsString(signedDocumentResponse);
//			return request.getParameter("callback") + "(" + testjson + ")";
			return signedDocumentResponse;
		} else {
//			String testjson = objectMapper.writeValueAsString(new SignDocumentResponse());
//			return request.getParameter("callback") + "(" + testjson + ")";
			return new SignDocumentResponse();
		}
	}

}
