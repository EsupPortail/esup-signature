package org.esupportail.esupsignature.web.controller.user;

import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.ToBeSigned;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.esupportail.esupsignature.dss.model.*;
import org.esupportail.esupsignature.entity.NexuSignature;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.esupportail.esupsignature.service.utils.StepStatus;
import org.esupportail.esupsignature.service.utils.sign.NexuService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

@CrossOrigin(allowedHeaders = "Content-Type", origins = "*")
@Controller
@RequestMapping("/nexu-sign")
public class NexuProcessController implements Serializable {

	private static final Logger logger = LoggerFactory.getLogger(NexuProcessController.class);

	@Resource
	private PreAuthorizeService preAuthorizeService;

	@ModelAttribute("activeMenu")
	public String getActiveMenu() {
		return "signrequests";
	}

	@Resource
	private NexuService nexuService;

	@Resource
	private UserService userService;

	@Resource
	private SignBookService signBookService;

	@Resource
	private SignRequestService signRequestService;

	@GetMapping(value = "/start", produces = "text/html")
	public String showSignatureParameters(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
										  @RequestParam("ids") List<Long> ids, Model model) {
		logger.info("init nexu sign by : " + userEppn + " for signRequest : " + ids);
		for(Long id : ids) {
			if(!preAuthorizeService.signRequestSign(id, userEppn, authUserEppn)) throw new EsupSignatureRuntimeException("Vous n'avez pas les droits pour signer ce document");
			nexuService.deleteNexuSignature(id);
		}
		model.addAttribute("ids", ids);
		User user = userService.getByEppn(userEppn);
		if(user.getUserType().equals(UserType.external)) {
			model.addAttribute("urlProfil", "otp");
		} else {
			model.addAttribute("urlProfil", "user");
		}
		return "user/signrequests/nexu-signature-process";
	}

	@PreAuthorize("@preAuthorizeService.signRequestSign(#id, #userEppn, #authUserEppn)")
	@PostMapping(value = "/get-data-to-sign")
	@ResponseBody
	public GetDataToSignResponse getDataToSign(@ModelAttribute("userEppn") String userEppn,
											   @ModelAttribute("authUserEppn") String authUserEppn,
											   @RequestBody @Valid DataToSignParams dataToSignParams,
											   @RequestParam("id") Long id) throws IOException, EsupSignatureRuntimeException {
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

	@PreAuthorize("@preAuthorizeService.signRequestSign(#id, #userEppn, #authUserEppn)")
	@PostMapping(value = "/sign-document")
	@ResponseBody
	public SignDocumentResponse signDocument(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
											 @RequestBody @Valid SignResponse signatureValue,
											 @RequestParam("id") Long id) throws EsupSignatureRuntimeException, IOException {
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

	@PreAuthorize("@preAuthorizeService.signRequestSign(#id, #userEppn, #authUserEppn)")
	@PostMapping(value = "/error")
	@ResponseBody
	public void error(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @ModelAttribute("id") Long id) throws EsupSignatureRuntimeException {
		nexuService.deleteNexuSignature(id);
	}

}