package org.esupportail.esupsignature.web.controller.user;

import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.ToBeSigned;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.esupportail.esupsignature.dss.DssUtils;
import org.esupportail.esupsignature.dss.model.*;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.utils.StepStatus;
import org.esupportail.esupsignature.service.utils.sign.SignService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
		abstractSignatureForm.setCertificate(params.getSigningCertificate());
		abstractSignatureForm.setCertificateChain(params.getCertificateChain());
		abstractSignatureForm.setEncryptionAlgorithm(params.getEncryptionAlgorithm());
		if (abstractSignatureForm.isAddContentTimestamp()) {
			abstractSignatureForm.setContentTimestamp(DssUtils.fromTimestampToken(signService.getContentTimestamp((SignatureDocumentForm) abstractSignatureForm)));
		}
		httpSession.setAttribute("abstractSignatureForm", abstractSignatureForm);
		GetDataToSignResponse responseJson = new GetDataToSignResponse();
		try {
			ToBeSigned dataToSign = signService.getDataToSign(id, userEppn, (SignatureDocumentForm) abstractSignatureForm);
			responseJson.setDataToSign(dataToSign.getBytes());
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
											 @RequestBody @Valid SignResponse signatureValue,
											 @ModelAttribute("id") Long id, HttpSession httpSession) throws EsupSignatureRuntimeException {
		AbstractSignatureForm abstractSignatureForm = (AbstractSignatureForm) httpSession.getAttribute("abstractSignatureForm");
		abstractSignatureForm.setSignatureValue(signatureValue.getSignatureValue());
		SignDocumentResponse responseJson = signService.getSignDocumentResponse(id, signatureValue, abstractSignatureForm, userEppn, authUserEppn);
		signRequestService.updateStatus(id, SignRequestStatus.signed, "Signature", "SUCCESS", userEppn, authUserEppn);
		StepStatus stepStatus = signRequestService.applyEndOfSignRules(id, userEppn, authUserEppn, SignType.nexuSign, "");
		if(stepStatus.equals(StepStatus.last_end)) {
			signBookService.completeSignRequest(id, authUserEppn, "Tous les documents sont signés");
		} else if (stepStatus.equals(StepStatus.completed)){
			signBookService.pendingSignRequest(id, null, userEppn, authUserEppn, false);
		}
		httpSession.removeAttribute("abstractSignatureForm");
		return responseJson;
	}

	@Scope(value = "session")
	@PreAuthorize("@preAuthorizeService.signRequestSign(#id, #userEppn, #authUserEppn)")
	@PostMapping(value = "/error")
	@ResponseBody
	public void error(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @ModelAttribute("id") Long id, HttpSession httpSession) throws EsupSignatureRuntimeException {
		httpSession.removeAttribute("abstractSignatureForm");
	}

}
