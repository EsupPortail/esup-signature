package org.esupportail.esupsignature.web.controller.user;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.xml.bind.DatatypeConverter;

import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.esupportail.esupsignature.dss.web.model.AbstractSignatureForm;
import org.esupportail.esupsignature.dss.web.model.DataToSignParams;
import org.esupportail.esupsignature.dss.web.model.GetDataToSignResponse;
import org.esupportail.esupsignature.dss.web.model.SignDocumentResponse;
import org.esupportail.esupsignature.dss.web.model.SignatureDocumentForm;
import org.esupportail.esupsignature.dss.web.model.SignatureMultipleDocumentsForm;
import org.esupportail.esupsignature.dss.web.model.SignatureValueAsString;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureKeystoreException;
import org.esupportail.esupsignature.exception.EsupSignatureSignException;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.service.FileService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.SigningService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.pdf.PdfService;
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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import eu.europa.esig.dss.AbstractSignatureParameters;
import eu.europa.esig.dss.SignatureForm;
import eu.europa.esig.dss.ToBeSigned;

@Controller
@SessionAttributes(value = { "signatureDocumentForm", "signRequestId", "parameters"})
@RequestMapping(value = "/user/nexu-sign")
@Transactional
@Scope("session")
public class NexuProcessController {

	private static final Logger logger = LoggerFactory.getLogger(NexuProcessController.class);

	@Value("${nexuUrl}")
	private String nexuUrl;

	@Value("${baseUrl}")
	private String downloadNexuUrl;

	@Autowired
	private SigningService signingService;

	@Autowired
	private SignRequestRepository signRequestRepository;
	
	@Resource
	private PdfService pdfService;
	
	@Resource
	private FileService fileService;
	
	@Resource
	private UserService userService;
	
	@Resource
	private SignRequestService signRequestService;

	@Value("${sign.defaultSignatureForm}")
	private SignatureForm defaultSignatureForm;
	
	private AbstractSignatureParameters parameters;
	
	@RequestMapping(value = "/{id}", produces = "text/html")
	public String showSignatureParameters(@PathVariable("id") Long id, Model model, HttpServletRequest request, RedirectAttributes redirectAttrs) {
    	User user = userService.getUserFromAuthentication();
		SignRequest signRequest = signRequestRepository.findById(id).get();
		if (signRequestService.checkUserSignRights(user, signRequest)) {
			AbstractSignatureForm signatureDocumentForm = null;
    		List<Document> documents = signRequestService.getToSignDocuments(signRequest);
    		if(documents.size() == 1){
        		File toSignFile = documents.get(0).getJavaIoFile();
        		if(fileService.getContentType(toSignFile).equals("application/pdf")) {
    				boolean addPage = false;
    				if(signRequest.countSignOk() == 0) {
    					addPage = true;
    				}
        			try {
						toSignFile = pdfService.formatPdf(toSignFile, signRequest.getSignRequestParams(), addPage, user);
					} catch (IOException e) {
						logger.error("error on format pdf", e);
					}
        			signatureDocumentForm = signingService.getSignatureDocumentForm(Arrays.asList(toSignFile), SignatureForm.PAdES);
        		} else {
        			signatureDocumentForm = signingService.getSignatureDocumentForm(Arrays.asList(toSignFile), defaultSignatureForm);
        		}
    		} else {
    			List<File> toSignFiles = new ArrayList<>();
    			for(Document document : signRequestService.getToSignDocuments(signRequest)) {
    				toSignFiles.add(document.getJavaIoFile());
    			}
				signatureDocumentForm = signingService.getSignatureDocumentForm(toSignFiles, defaultSignatureForm);
    		}
			model.addAttribute("signRequestId", signRequest.getId());
			model.addAttribute("signatureDocumentForm", signatureDocumentForm);
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
				parameters = signingService.fillParameters((SignatureMultipleDocumentsForm) signatureDocumentForm);
				dataToSign = signingService.getDataToSign((SignatureMultipleDocumentsForm) signatureDocumentForm);
			} else {
				if(signatureDocumentForm.getSignatureForm().equals(SignatureForm.PAdES)) {
					SignatureDocumentForm documentForm = (SignatureDocumentForm) signatureDocumentForm;
					PDSignatureField pdSignatureField = pdfService.getPDSignatureFieldName(signRequestService.getLastSignedDocument(signRequest).getJavaIoFile(), signRequest.getNbSign());
					parameters = signingService.fillVisibleParameters((SignatureDocumentForm) signatureDocumentForm, signRequest.getSignRequestParams(), documentForm.getDocumentToSign(), user, pdSignatureField);
				} else {
					parameters = signingService.fillParameters((SignatureDocumentForm) signatureDocumentForm);
				}
				dataToSign = signingService.getDataToSign((SignatureDocumentForm) signatureDocumentForm, parameters);
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
	        signRequestRepository.save(signRequest);
	        signedDocumentResponse = new SignDocumentResponse();
	        signedDocumentResponse.setUrlToDownload("download");
	        return signedDocumentResponse;
		} else {
			return new SignDocumentResponse();
		}
	}

}
