package org.esupportail.esupsignature.web.controller.user;

import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.ToBeSigned;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.esupportail.esupsignature.dss.model.*;
import org.esupportail.esupsignature.entity.NexuSignature;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.utils.StepStatus;
import org.esupportail.esupsignature.service.utils.sign.NexuService;
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
	private NexuService nexuService;

	@Resource
	private SignBookService signBookService;

	@Resource
	private SignRequestService signRequestService;

	@Scope(value = "session")
	@PreAuthorize("@preAuthorizeService.signRequestSign(#id, #userEppn, #authUserEppn)")
	@GetMapping(value = "/{id}", produces = "text/html")
	public String showSignatureParameters(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
										  @PathVariable("id") Long id, Model model) {
		logger.info("init nexu sign by : " + userEppn + " for signRequest : " + id);
		nexuService.deleteNexuSignature(id);
		SignRequest signRequest = signRequestService.getById(id);
		model.addAttribute("id", signRequest.getId());
		return "user/signrequests/nexu-signature-process";
	}

	@Scope(value = "session")
	@PreAuthorize("@preAuthorizeService.signRequestSign(#id, #userEppn, #authUserEppn)")
	@PostMapping(value = "/get-data-to-sign")
	@ResponseBody
	public GetDataToSignResponse getDataToSign(@ModelAttribute("userEppn") String userEppn,
											   @ModelAttribute("authUserEppn") String authUserEppn,
											   @RequestBody @Valid DataToSignParams dataToSignParams,
											   @ModelAttribute("id") Long id) throws IOException, EsupSignatureRuntimeException {
		logger.info("get data to sign for signRequest: " + id);
		AbstractSignatureForm abstractSignatureForm = nexuService.getSignatureForm(id);
		abstractSignatureForm.setCertificate(dataToSignParams.getSigningCertificate());
		abstractSignatureForm.setCertificateChain(dataToSignParams.getCertificateChain());
		abstractSignatureForm.setEncryptionAlgorithm(dataToSignParams.getEncryptionAlgorithm());
		NexuSignature nexuSignature = nexuService.saveNexuSignature(id, abstractSignatureForm);
		GetDataToSignResponse responseJson = new GetDataToSignResponse();
		try {
			ToBeSigned dataToSign = nexuService.getDataToSign(id, userEppn, (SignatureDocumentForm) abstractSignatureForm, nexuSignature.getDocumentToSign());
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
											 @ModelAttribute("id") Long id) throws EsupSignatureRuntimeException, IOException {
		NexuSignature nexuSignature = nexuService.getNexuSignature(id);
		AbstractSignatureForm abstractSignatureForm = nexuService.getAbstractSignatureFormFromNexuSignature(nexuSignature);
		abstractSignatureForm.setSignatureValue(signatureValue.getSignatureValue());
		SignDocumentResponse responseJson = nexuService.getSignDocumentResponse(id, signatureValue, abstractSignatureForm, userEppn, nexuSignature.getDocumentToSign());
		signRequestService.updateStatus(id, SignRequestStatus.signed, "Signature", "SUCCESS", userEppn, authUserEppn);
		StepStatus stepStatus = signRequestService.applyEndOfSignRules(id, userEppn, authUserEppn, SignType.nexuSign, "");
		if(stepStatus.equals(StepStatus.last_end)) {
			signBookService.completeSignRequest(id, authUserEppn, "Tous les documents sont signés");
		} else if (stepStatus.equals(StepStatus.completed)){
			signBookService.pendingSignRequest(id, null, userEppn, authUserEppn, false);
		}
		nexuService.deleteNexuSignature(id);
		return responseJson;
	}

	@Scope(value = "session")
	@PreAuthorize("@preAuthorizeService.signRequestSign(#id, #userEppn, #authUserEppn)")
	@PostMapping(value = "/error")
	@ResponseBody
	public void error(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @ModelAttribute("id") Long id, HttpSession httpSession) throws EsupSignatureRuntimeException {
		nexuService.deleteNexuSignature(id);
	}

}