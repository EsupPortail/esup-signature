package org.esupportail.esupsignature.web.controller.user;

import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.ToBeSigned;
import org.esupportail.esupsignature.dss.DssUtils;
import org.esupportail.esupsignature.dss.model.*;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.utils.sign.SignService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.Serializable;

@CrossOrigin(allowedHeaders = "Content-Type", origins = "*")
@Controller
@RequestMapping("/user/nexu-sign")
public class NexuProcessController implements Serializable {

	private static final Logger logger = LoggerFactory.getLogger(NexuProcessController.class);

	@ModelAttribute("activeMenu")
	public String getActiveMenu() {
		return "signrequests";
	}

	@Resource
	private SignService signService;

	@Resource
	private SignBookService signBookService;

	@Resource
	private SignRequestService signRequestService;

	@Scope(value = "session")
	@PreAuthorize("@preAuthorizeService.signRequestSign(#id, #userEppn, #authUserEppn)")
	@GetMapping(value = "/{id}", produces = "text/html")
	public String showSignatureParameters(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
										  @PathVariable("id") Long id, HttpSession httpSession, Model model) {
		httpSession.removeAttribute("abstractSignatureForm");
		SignRequest signRequest = signRequestService.getById(id);
		logger.info("init nexu sign by : " + userEppn + " for signRequest : " + id);
		model.addAttribute("id", signRequest.getId());
		return "user/signrequests/nexu-signature-process";
	}

	@Scope(value = "session")
	@PreAuthorize("@preAuthorizeService.signRequestSign(#id, #userEppn, #authUserEppn)")
	@PostMapping(value = "/get-data-to-sign")
	@ResponseBody
	public GetDataToSignResponse getDataToSign(@ModelAttribute("userEppn") String userEppn,
											   @ModelAttribute("authUserEppn") String authUserEppn,
											   @RequestBody @Valid DataToSignParams params,
											   @ModelAttribute("id") Long id, HttpSession httpSession) throws IOException, EsupSignatureRuntimeException {
		logger.info("get data to sign for signRequest: " + id);
		AbstractSignatureForm abstractSignatureForm = signService.getAbstractSignatureForm(id, userEppn);
		abstractSignatureForm.setBase64Certificate(params.getSigningCertificate());
		abstractSignatureForm.setBase64CertificateChain(params.getCertificateChain());
		abstractSignatureForm.setEncryptionAlgorithm(params.getEncryptionAlgorithm());
		if (abstractSignatureForm.isAddContentTimestamp()) {
			abstractSignatureForm.setContentTimestamp(DssUtils.fromTimestampToken(signService.getContentTimestamp((SignatureDocumentForm) abstractSignatureForm)));
		}
		httpSession.setAttribute("abstractSignatureForm", abstractSignatureForm);
		GetDataToSignResponse responseJson = new GetDataToSignResponse();
		try {
			ToBeSigned dataToSign = signService.getDataToSign(id, userEppn, (SignatureDocumentForm) abstractSignatureForm);
			responseJson.setDataToSign(DatatypeConverter.printBase64Binary(dataToSign.getBytes()));
			return responseJson;
		} catch (DSSException e) {
			throw new EsupSignatureRuntimeException(e.getMessage());
		}
	}

	@Scope(value = "session")
	@PreAuthorize("@preAuthorizeService.signRequestSign(#id, #userEppn, #authUserEppn)")
	@PostMapping(value = "/sign-document")
	@ResponseBody
	public SignDocumentResponse signDocument(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
											 @RequestBody @Valid SignatureValueAsString signatureValue,
											 @ModelAttribute("id") Long id, HttpSession httpSession) throws EsupSignatureRuntimeException, IOException {
		AbstractSignatureForm abstractSignatureForm = (AbstractSignatureForm) httpSession.getAttribute("abstractSignatureForm");
		abstractSignatureForm.setBase64SignatureValue(signatureValue.getSignatureValue());
		SignDocumentResponse signDocumentResponse = signService.getSignDocumentResponse(id, signatureValue, abstractSignatureForm, userEppn, authUserEppn);
		signRequestService.updateStatus(id, SignRequestStatus.signed, "Signature", "SUCCESS", userEppn, authUserEppn);
		signBookService.applyEndOfSignRules(id, userEppn, authUserEppn, SignType.nexuSign, "");
		httpSession.removeAttribute("abstractSignatureForm");
		return signDocumentResponse;
	}

}
