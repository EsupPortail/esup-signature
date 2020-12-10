package org.esupportail.esupsignature.web.controller.user;

import eu.europa.esig.dss.AbstractSignatureParameters;
import eu.europa.esig.dss.model.ToBeSigned;
import org.esupportail.esupsignature.dss.model.*;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.sign.SignService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;

@CrossOrigin(allowedHeaders = "Content-Type", origins = "*")
@Controller
@SessionAttributes(value = { "signatureDocumentForm", "parameters"})
@RequestMapping("/user/nexu-sign")
@Transactional
public class NexuProcessController {

	private static final Logger logger = LoggerFactory.getLogger(NexuProcessController.class);

	private AbstractSignatureForm signatureDocumentForm;
	private AbstractSignatureParameters<?> parameters;


	@ModelAttribute("activeMenu")
	public String getActiveMenu() {
		return "signrequests";
	}

	@Resource
	private SignService signService;

	@Resource
	private SignRequestService signRequestService;

	@PreAuthorize("@signRequestService.preAuthorizeSign(#id, #user, #authUser)")
	@GetMapping(value = "/{id}", produces = "text/html")
	public String showSignatureParameters(@ModelAttribute("user") User user, @ModelAttribute("authUser") User authUser, @PathVariable("id") Long id, Model model) throws IOException, EsupSignatureException {
		SignRequest signRequest = signRequestService.getById(id);
		logger.info("init nexu sign by : " + user.getEppn() + " for signRequest : " + id);
		this.signatureDocumentForm = signService.getAbstractSignatureForm(signRequest);
		model.addAttribute("id", signRequest.getId());
		return "user/signrequests/nexu-signature-process";
	}

	@PreAuthorize("@signRequestService.preAuthorizeSign(#id, #user, #authUser)")
	@PostMapping(value = "/get-data-to-sign")
	@ResponseBody
	public GetDataToSignResponse getDataToSign(@ModelAttribute("user") User user, @ModelAttribute("authUser") User authUser,
								@RequestBody @Valid DataToSignParams params, @ModelAttribute("id") Long id) throws IOException {
		SignRequest signRequest = signRequestService.getById(id);
		logger.info("get data to sign for signRequest: " + id);
		signatureDocumentForm.setBase64Certificate(params.getSigningCertificate());
		signatureDocumentForm.setBase64CertificateChain(params.getCertificateChain());
		signatureDocumentForm.setEncryptionAlgorithm(params.getEncryptionAlgorithm());
		GetDataToSignResponse responseJson = new GetDataToSignResponse();
		parameters = signService.getToBeSigned(signRequest, user, signatureDocumentForm);
		ToBeSigned dataToSign = signService.getDataToSign((SignatureDocumentForm) signatureDocumentForm, parameters);
		responseJson.setDataToSign(DatatypeConverter.printBase64Binary(dataToSign.getBytes()));
		return responseJson;
	}

	@PreAuthorize("@signRequestService.preAuthorizeSign(#id, #user, #authUser)")
	@PostMapping(value = "/sign-document")
	@ResponseBody
	public SignDocumentResponse signDocument(@ModelAttribute("user") User user, @ModelAttribute("authUser") User authUser, @RequestBody @Valid SignatureValueAsString signatureValue,
			@ModelAttribute("id") Long id) throws EsupSignatureException {
		SignRequest signRequest = signRequestService.getById(id);
		return signService.getSignDocumentResponse(signRequest, signatureValue, signatureDocumentForm, parameters, user);
	}

}
