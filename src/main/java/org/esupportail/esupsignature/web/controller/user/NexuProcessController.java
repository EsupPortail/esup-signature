package org.esupportail.esupsignature.web.controller.user;

import eu.europa.esig.dss.AbstractSignatureParameters;
import eu.europa.esig.dss.model.ToBeSigned;
import org.esupportail.esupsignature.dss.model.*;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
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
	private SignRequestService signRequestService;

	@Resource
	private UserService userService;

	@Scope(value = "session")
	@PreAuthorize("@preAuthorizeService.signRequestSign(#id, #userEppn, #authUserEppn)")
	@GetMapping(value = "/{id}", produces = "text/html")
	public String showSignatureParameters(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
										  @PathVariable("id") Long id, HttpSession httpSession, Model model) {
		User user = userService.getByEppn(userEppn);
		User authUser = userService.getByEppn(authUserEppn);
		httpSession.removeAttribute("abstractSignatureForm");
		httpSession.removeAttribute("abstractSignatureParameters");
		SignRequest signRequest = signRequestService.getById(id);
		logger.info("init nexu sign by : " + user.getEppn() + " for signRequest : " + id);
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
											   @ModelAttribute("id") Long id, HttpSession httpSession) throws IOException, EsupSignatureException {
		User user = userService.getByEppn(userEppn);
		SignRequest signRequest = signRequestService.getById(id);
		logger.info("get data to sign for signRequest: " + id);
		AbstractSignatureForm abstractSignatureForm = signService.getAbstractSignatureForm(signRequest);
		abstractSignatureForm.setBase64Certificate(params.getSigningCertificate());
		abstractSignatureForm.setBase64CertificateChain(params.getCertificateChain());
		abstractSignatureForm.setEncryptionAlgorithm(params.getEncryptionAlgorithm());
		AbstractSignatureParameters<?> abstractSignatureParameters = signService.getSignatureParameters(signRequest, user, abstractSignatureForm);
		ToBeSigned dataToSign = signService.getDataToSign((SignatureDocumentForm) abstractSignatureForm, abstractSignatureParameters);
		GetDataToSignResponse responseJson = new GetDataToSignResponse();
		responseJson.setDataToSign(DatatypeConverter.printBase64Binary(dataToSign.getBytes()));
		httpSession.setAttribute("abstractSignatureForm", abstractSignatureForm);
		httpSession.setAttribute("abstractSignatureParameters", abstractSignatureParameters);
		return responseJson;
	}

	@Scope(value = "session")
	@PreAuthorize("@preAuthorizeService.signRequestSign(#id, #userEppn, #authUserEppn)")
	@PostMapping(value = "/sign-document")
	@ResponseBody
	public SignDocumentResponse signDocument(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
											 @RequestBody @Valid SignatureValueAsString signatureValue,
											 @ModelAttribute("id") Long id, HttpSession httpSession) throws EsupSignatureException {
		User user = userService.getByEppn(userEppn);
		User authUser = userService.getByEppn(authUserEppn);
		SignRequest signRequest = signRequestService.getById(id);
		AbstractSignatureForm abstractSignatureForm = (AbstractSignatureForm) httpSession.getAttribute("abstractSignatureForm");
		AbstractSignatureParameters<?> abstractSignatureParameters = (AbstractSignatureParameters<?>) httpSession.getAttribute("abstractSignatureParameters");
		SignDocumentResponse signDocumentResponse = signService.getSignDocumentResponse(signRequest, signatureValue, abstractSignatureForm, abstractSignatureParameters, user, authUser);
		httpSession.removeAttribute("abstractSignatureForm");
		httpSession.removeAttribute("abstractSignatureParameters");
		return signDocumentResponse;
	}

}
