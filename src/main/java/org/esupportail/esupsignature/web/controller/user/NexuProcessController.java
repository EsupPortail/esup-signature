package org.esupportail.esupsignature.web.controller.user;

import eu.europa.esig.dss.AbstractSignatureParameters;
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
import org.esupportail.esupsignature.service.sign.SignService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;

@CrossOrigin(allowedHeaders = "Content-Type", origins = "*")
@Controller
@SessionAttributes(value = { "signatureDocumentForm", "signRequestId", "parameters"})
@RequestMapping("/user/nexu-sign")
@Transactional
@EnableConfigurationProperties(GlobalProperties.class)
public class NexuProcessController {

	private static final Logger logger = LoggerFactory.getLogger(NexuProcessController.class);

	@ModelAttribute("activeMenu")
	public String getActiveMenu() {
		return "signrequests";
	}

	@Resource
	private SignService signService;

	@Resource
	private SignRequestRepository signRequestRepository;

	private AbstractSignatureParameters parameters;

	@PreAuthorize("@signRequestService.preAuthorizeSign(#id, #user, #authUser)")
	@GetMapping(value = "/{id}", produces = "text/html")
	public String showSignatureParameters(@ModelAttribute("user") User user, @ModelAttribute("authUser") User authUser, @PathVariable("id") Long id, Model model,
										  @RequestParam(value = "referer", required = false) String referer) throws IOException, EsupSignatureException {
		SignRequest signRequest = signRequestRepository.findById(id).get();
		logger.info("init nexu sign by : " + user.getEppn() + " for signRequest : " + id);
		AbstractSignatureForm signatureDocumentForm = signService.getAbstractSignatureForm(signRequest);
		model.addAttribute("id", signRequest.getId());
		model.addAttribute("signatureDocumentForm", signatureDocumentForm);
		model.addAttribute("digestAlgorithm", signatureDocumentForm.getDigestAlgorithm());
		model.addAttribute("referer", referer);
		return "user/signrequests/nexu-signature-process";
	}

	@PreAuthorize("@signRequestService.preAuthorizeSign(#id, #user, #authUser)")
	@PostMapping(value = "/get-data-to-sign")
	@ResponseBody
	public GetDataToSignResponse getDataToSign(@ModelAttribute("user") User user, @ModelAttribute("authUser") User authUser, Model model,
								@RequestBody @Valid DataToSignParams params,
								@ModelAttribute("signatureDocumentForm") @Valid AbstractSignatureForm signatureDocumentForm,
								@ModelAttribute("id") Long id) throws IOException {
		SignRequest signRequest = signRequestRepository.findById(id).get();
		logger.info("get data to sign for signRequest: " + id);
		signatureDocumentForm.setBase64Certificate(params.getSigningCertificate());
		signatureDocumentForm.setBase64CertificateChain(params.getCertificateChain());
		signatureDocumentForm.setEncryptionAlgorithm(params.getEncryptionAlgorithm());
		model.addAttribute("signatureDocumentForm", signatureDocumentForm);
		model.addAttribute("signRequestId", signRequest.getId());
		GetDataToSignResponse responseJson = new GetDataToSignResponse();
		ToBeSigned dataToSign = signService.getToBeSigned(signRequest, user, signatureDocumentForm, this.parameters);
		responseJson.setDataToSign(DatatypeConverter.printBase64Binary(dataToSign.getBytes()));
		return responseJson;
	}

	@PreAuthorize("@signRequestService.preAuthorizeSign(#id, #user, #authUser)")
	@PostMapping(value = "/sign-document")
	@ResponseBody
	public SignDocumentResponse signDocument(@ModelAttribute("user") User user, @ModelAttribute("authUser") User authUser, @RequestBody @Valid SignatureValueAsString signatureValue,
			@ModelAttribute("signatureDocumentForm") @Valid AbstractSignatureForm signatureDocumentForm, 
			@ModelAttribute("id") Long id) throws EsupSignatureException {
		SignRequest signRequest = signRequestRepository.findById(id).get();
		SignDocumentResponse signedDocumentResponse = signService.getSignDocumentResponse(user, signatureValue, signatureDocumentForm, signRequest, this.parameters);
		return signedDocumentResponse;
	}

}
