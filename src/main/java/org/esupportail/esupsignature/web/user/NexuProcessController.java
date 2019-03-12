package org.esupportail.esupsignature.web.user;

import java.io.File;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.xml.bind.DatatypeConverter;

import org.esupportail.esupsignature.domain.SignRequest;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.dss.web.model.DataToSignParams;
import org.esupportail.esupsignature.dss.web.model.GetDataToSignResponse;
import org.esupportail.esupsignature.dss.web.model.SignDocumentResponse;
import org.esupportail.esupsignature.dss.web.model.SignatureDocumentForm;
import org.esupportail.esupsignature.dss.web.model.SignatureValueAsString;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureKeystoreException;
import org.esupportail.esupsignature.service.FileService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.SigningService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.pdf.PdfService;
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

import eu.europa.esig.dss.ToBeSigned;

@Controller
@SessionAttributes(value = { "signaturePdfForm", "signedPdfDocument", "signRequest" })
@RequestMapping(value = "/user/nexu-sign")
@Transactional
public class NexuProcessController {

	private static final Logger logger = LoggerFactory.getLogger(NexuProcessController.class);

	@Value("${nexuUrl}")
	private String nexuUrl;

	@Value("${baseUrl}")
	private String downloadNexuUrl;

	@Autowired
	private SigningService signingService;

	@Resource
	private PdfService pdfService;
	
	@Resource
	private FileService fileService;
	
	@Resource
	private UserService userService;
	
	@Resource
	private SignRequestService signRequestService;

	@RequestMapping(value = "/{id}", produces = "text/html")
	public String showSignatureParameters(@PathVariable("id") Long id, Model model, HttpServletRequest request, RedirectAttributes redirectAttrs) {
    	User user = userService.getEppnFromAuthentication();
		SignRequest signRequest = SignRequest.findSignRequest(id);
		if (signRequestService.checkUserSignRights(user, signRequest)) {
    		SignatureDocumentForm signatureDocumentForm = signingService.getXadesSignatureDocumentForm();
    		File toSignFile = signRequestService.getLastDocument(signRequest).getJavaIoFile();
    		if(fileService.getContentType(toSignFile).equals("application/pdf")) {
    			signatureDocumentForm = signingService.getPadesSignatureDocumentForm();
    			toSignFile = pdfService.formatPdf(toSignFile, signRequest.getSignRequestParams());
    		}
    		signatureDocumentForm.setDocumentToSign(fileService.toMultipartFile(toSignFile, "application/pdf"));
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
			@ModelAttribute("signaturePdfForm") @Valid SignatureDocumentForm signaturePdfForm, @ModelAttribute("signRequest") SignRequest signRequest, BindingResult result) throws EsupSignatureKeystoreException {
		SignDocumentResponse signedDocumentResponse;
		signaturePdfForm.setBase64SignatureValue(signatureValue.getSignatureValue());
    	User user = userService.getEppnFromAuthentication();
        try {
        	signRequestService.nexuSign(signRequest, user, signaturePdfForm);
		} catch (EsupSignatureIOException e) {
			logger.error(e.getMessage(), e);
		}
        signedDocumentResponse = new SignDocumentResponse();
        signedDocumentResponse.setUrlToDownload("download");
        return signedDocumentResponse;
	}

}
