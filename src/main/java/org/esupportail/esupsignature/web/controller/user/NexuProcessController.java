package org.esupportail.esupsignature.web.controller.user;

import eu.europa.esig.dss.AbstractSignatureParameters;
import eu.europa.esig.dss.SignatureForm;
import eu.europa.esig.dss.ToBeSigned;
import org.esupportail.esupsignature.dss.web.model.*;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureKeystoreException;
import org.esupportail.esupsignature.exception.EsupSignatureSignException;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.sign.SignService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
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
@RequestMapping(value = "/user/nexu-sign")
@Transactional
@Scope("session")
public class NexuProcessController {

	private static final Logger logger = LoggerFactory.getLogger(NexuProcessController.class);

	@Value("${root.url}")
	private String rootUrl;

	@Value("${nexuUrl}")
	private String nexuUrl;

	@Value("${baseUrl}")
	private String downloadNexuUrl;

	@Autowired
	private SignService signService;

	@Autowired
	private SignRequestRepository signRequestRepository;

	@Resource
	private UserService userService;
	
	@Resource
	private SignRequestService signRequestService;

	private AbstractSignatureParameters parameters;
	
	@RequestMapping(value = "/{id}", produces = "text/html")
	public String showSignatureParameters(@PathVariable("id") Long id, Model model,
										  @RequestParam(value = "referer", required = false) String referer,
										  HttpServletRequest request, RedirectAttributes redirectAttrs) throws IOException, EsupSignatureSignException {
    	User user = userService.getUserFromAuthentication();
		SignRequest signRequest = signRequestRepository.findById(id).get();
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
			model.addAttribute("rootUrl", rootUrl);
			model.addAttribute("nexuUrl", nexuUrl);
			model.addAttribute("referer", referer);
			return "user/nexu-signature-process";
		} else {
			redirectAttrs.addFlashAttribute("messageCustom", "not autorized");
			return "redirect:/user/signrequests/";
		}
	}

	@RequestMapping(value = "/get-data-to-sign", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public GetDataToSignResponse getDataToSign(Model model, @RequestBody @Valid DataToSignParams params,
			@ModelAttribute("signatureDocumentForm") @Valid AbstractSignatureForm signatureDocumentForm, 
		@ModelAttribute("signRequestId") Long signRequestId, BindingResult result) throws IOException {
		signatureDocumentForm.setBase64Certificate(params.getSigningCertificate());
		signatureDocumentForm.setBase64CertificateChain(params.getCertificateChain());
		signatureDocumentForm.setEncryptionAlgorithm(params.getEncryptionAlgorithm());
    	User user = userService.getUserFromAuthentication();
		SignRequest signRequest = signRequestRepository.findById(signRequestId).get();
		if (signRequestService.checkUserSignRights(user, signRequest)) {
			ToBeSigned dataToSign;
			if(signatureDocumentForm.getClass().equals(SignatureMultipleDocumentsForm.class)) {
				parameters = signService.fillParameters((SignatureMultipleDocumentsForm) signatureDocumentForm);
				dataToSign = signService.getDataToSign((SignatureMultipleDocumentsForm) signatureDocumentForm);
			} else {
				if(signatureDocumentForm.getSignatureForm().equals(SignatureForm.PAdES)) {
					SignatureDocumentForm documentForm = (SignatureDocumentForm) signatureDocumentForm;
					parameters = signService.fillVisibleParameters((SignatureDocumentForm) signatureDocumentForm, signRequest.getSignRequestParams().get(signRequest.getParentSignBook().getCurrentWorkflowStepNumber()), documentForm.getDocumentToSign(), user, false);
				} else {
					parameters = signService.fillParameters((SignatureDocumentForm) signatureDocumentForm);
				}
				dataToSign = signService.getDataToSign((SignatureDocumentForm) signatureDocumentForm, parameters);
			}
				
			if (dataToSign == null) {
				return null;
			}
			model.addAttribute("parameters", parameters);
			model.addAttribute("signatureDocumentForm", signatureDocumentForm);
			model.addAttribute("signRequestId", signRequest.getId());
			
			GetDataToSignResponse responseJson = new GetDataToSignResponse();
			responseJson.setDataToSign(DatatypeConverter.printBase64Binary(dataToSign.getBytes()));
			return responseJson;
		} else {
			return new GetDataToSignResponse();
		}
	}

	@RequestMapping(value = "/sign-document", method = RequestMethod.POST)
	@ResponseBody
	public SignDocumentResponse signDocument(Model model, @RequestBody @Valid SignatureValueAsString signatureValue,
			@ModelAttribute("signatureDocumentForm") @Valid AbstractSignatureForm signatureDocumentForm, 
			@ModelAttribute("signRequestId") Long signRequestId, BindingResult result) throws EsupSignatureKeystoreException {
		User user = userService.getUserFromAuthentication();
		SignRequest signRequest = signRequestRepository.findById(signRequestId).get();
		if (signRequestService.checkUserSignRights(user, signRequest)) {
			SignDocumentResponse signedDocumentResponse;
			signatureDocumentForm.setBase64SignatureValue(signatureValue.getSignatureValue());
	        try {
	        	signRequestService.nexuSign(signRequest, user, signatureDocumentForm, parameters);
			} catch (EsupSignatureIOException | EsupSignatureSignException e) {
				logger.error(e.getMessage(), e);
			}
			signRequestService.updateStatus(signRequest, SignRequestStatus.signed, "Signature", user, "SUCCESS", signRequest.getComment());
			signRequestRepository.save(signRequest);
			signRequestService.applyEndOfStepRules(signRequest, user);
	        signedDocumentResponse = new SignDocumentResponse();
	        signedDocumentResponse.setUrlToDownload("download");
	        return signedDocumentResponse;
		} else {
			return new SignDocumentResponse();
		}
	}

}
