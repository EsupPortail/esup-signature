package org.esupportail.esupsignature.web.user;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.xml.bind.DatatypeConverter;

import org.esupportail.esupsignature.domain.SignRequest;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.dss.web.model.DataToSignParams;
import org.esupportail.esupsignature.dss.web.model.GetDataToSignResponse;
import org.esupportail.esupsignature.dss.web.model.SignDocumentResponse;
import org.esupportail.esupsignature.dss.web.model.SignatureDocumentForm;
import org.esupportail.esupsignature.dss.web.model.SignatureValueAsString;
import org.esupportail.esupsignature.dss.web.service.SigningService;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.service.FileService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import eu.europa.esig.dss.DSSDocument;
import eu.europa.esig.dss.DSSUtils;
import eu.europa.esig.dss.FileDocument;
import eu.europa.esig.dss.InMemoryDocument;
import eu.europa.esig.dss.MimeType;
import eu.europa.esig.dss.ToBeSigned;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.SignatureImageParameters;

@Controller
@SessionAttributes(value = { "signaturePdfForm", "signedPdfDocument", "signRequest" })
@RequestMapping(value = "/user/nexu-sign")
@Transactional
public class NexuProcessController {

	private static final Logger log = LoggerFactory.getLogger(NexuProcessController.class);

	@Value("${nexuUrl}")
	private String nexuUrl;

	@Value("${baseUrl}")
	private String downloadNexuUrl;

	@Autowired
	private SigningService signingService;
	
	@Resource
	private FileService fileService;
	
	@Resource
	private UserService userService;
	
	@Resource
	private SignRequestService signRequestService;
	
	private int signWidth = 100;
	private int signHeight = 75;

	@RequestMapping(value = "/{id}", produces = "text/html")
	public String showSignatureParameters(@PathVariable("id") Long id, Model model, HttpServletRequest request, RedirectAttributes redirectAttrs) {
		String eppn = userService.getEppnFromAuthentication();
    	User user = User.findUsersByEppnEquals(eppn).getSingleResult();
		SignRequest signRequest = SignRequest.findSignRequest(id);
		if (signRequestService.checkUserSignRights(user, signRequest)) {
    		SignatureDocumentForm signatureDocumentForm = signingService.getPadesSignatureDocumentForm();
			signatureDocumentForm.setDocumentToSign(fileService.toMultipartFile(signRequest.getOriginalFile().getBigFile().toJavaIoFile(), "application/pdf"));
			model.addAttribute("signRequest", signRequest);
			model.addAttribute("signaturePdfForm", signatureDocumentForm);
			model.addAttribute("digestAlgorithm", signatureDocumentForm.getDigestAlgorithm());
			model.addAttribute("rootUrl", "nexu-sign");
			model.addAttribute("nexuUrl", nexuUrl);
			return "user/nexu-signature-process";
		} else {
			redirectAttrs.addFlashAttribute("messageCustom", "not autorized");
			return "redirect:/user/signrequests/";
		}
	}

	@RequestMapping(value = "/get-data-to-sign", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public GetDataToSignResponse getDataToSign(Model model, @RequestBody @Valid DataToSignParams params,
			@ModelAttribute("signaturePdfForm") @Valid SignatureDocumentForm signaturePdfForm, @ModelAttribute("signRequest") SignRequest signRequest, BindingResult result) {

		signaturePdfForm.setBase64Certificate(params.getSigningCertificate());
		signaturePdfForm.setBase64CertificateChain(params.getCertificateChain());
		signaturePdfForm.setEncryptionAlgorithm(params.getEncryptionAlgorithm());

		model.addAttribute("signaturePdfForm", signaturePdfForm);
		model.addAttribute("signRequest", signRequest);

		ToBeSigned dataToSign = signingService.getDataToSign(signaturePdfForm);
		if (dataToSign == null) {
			return null;
		}

		GetDataToSignResponse responseJson = new GetDataToSignResponse();
		responseJson.setDataToSign(DatatypeConverter.printBase64Binary(dataToSign.getBytes()));
		return responseJson;
	}

	@RequestMapping(value = "/sign-document", method = RequestMethod.POST)
	@ResponseBody
	public SignDocumentResponse signDocument(Model model, @RequestBody @Valid SignatureValueAsString signatureValue,
			@ModelAttribute("signaturePdfForm") @Valid SignatureDocumentForm signaturePdfForm, @ModelAttribute("signRequest") SignRequest signRequest, BindingResult result) {
		signaturePdfForm.setBase64SignatureValue(signatureValue.getSignatureValue());
		String eppn = userService.getEppnFromAuthentication();
    	User user = User.findUsersByEppnEquals(eppn).getSingleResult();
    	
    	File signImage = user.getSignImage().getBigFile().toJavaIoFile();

    	Map<String, String> params = signRequest.getParams();
		SignatureImageParameters imageParameters = new SignatureImageParameters();
		imageParameters.setPage(Integer.valueOf(params.get("signPageNumber")));
		imageParameters.setxAxis(Integer.valueOf(params.get("xPos")));
		imageParameters.setyAxis(Integer.valueOf(params.get("yPos")));
		FileDocument fileDocumentImage = new FileDocument(signImage);
		fileDocumentImage.setMimeType(MimeType.PNG);
		imageParameters.setImage(fileDocumentImage);
		imageParameters.setWidth(signWidth);
		imageParameters.setHeight(signHeight);
		
		PAdESSignatureParameters parameters = new PAdESSignatureParameters();
		parameters.setSignatureImageParameters(imageParameters);
		parameters.setSignatureSize(100000);
    	
		DSSDocument document = signingService.padesSignDocument(signaturePdfForm, parameters);
		
		InMemoryDocument signedPdfDocument = new InMemoryDocument(DSSUtils.toByteArray(document), document.getName(), document.getMimeType());
        try {
        	File signedFile = fileService.inputStreamToFile(signedPdfDocument.openStream(), signedPdfDocument.getName(), "pdf");
        	signRequestService.addSignedFile(signRequest, signedFile, user);
        	signRequest.merge();
        	model.addAttribute("signRequest", signRequest);
    		model.addAttribute("signedPdfDocument", signedPdfDocument);
    		SignDocumentResponse signedDocumentResponse = new SignDocumentResponse();
    		signedDocumentResponse.setUrlToDownload("download");
    		return signedDocumentResponse;
		} catch (IOException e) {
			log.error("error to read signed file", e);
		} catch (EsupSignatureIOException e) {
			log.error(e.getMessage(), e);
		}
        return null;
	}

	@RequestMapping(value = "/end", method = RequestMethod.GET)
	public String end(@ModelAttribute("signedPdfDocument") InMemoryDocument signedDocument, @ModelAttribute("signRequest") SignRequest signRequest, HttpServletResponse response) {
		return "redirect:/user/signrequests/" + signRequest.getId(); 
	}
	
}
