package org.esupportail.esupsignature.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.codec.binary.Base64;
import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.domain.Log;
import org.esupportail.esupsignature.domain.SignBook;
import org.esupportail.esupsignature.domain.SignBook.DocumentIOType;
import org.esupportail.esupsignature.domain.SignBook.SignType;
import org.esupportail.esupsignature.domain.SignRequest;
import org.esupportail.esupsignature.domain.SignRequest.SignRequestStatus;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.dss.web.model.SignatureDocumentForm;
import org.esupportail.esupsignature.dss.web.service.SigningService;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureKeystoreException;
import org.esupportail.esupsignature.service.fs.cifs.CifsAccessImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.europa.esig.dss.DSSDocument;
import eu.europa.esig.dss.DSSUtils;
import eu.europa.esig.dss.EncryptionAlgorithm;
import eu.europa.esig.dss.FileDocument;
import eu.europa.esig.dss.InMemoryDocument;
import eu.europa.esig.dss.MimeType;
import eu.europa.esig.dss.asic.ASiCWithXAdESSignatureParameters;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.SignatureImageParameters;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import eu.europa.esig.dss.x509.CertificateToken;

@Service
public class SignRequestService {
	
	private static final Logger logger = LoggerFactory.getLogger(SignRequestService.class);
	
	@Resource
	UserKeystoreService userKeystoreService;
	
	@Resource
	PdfService pdfService;
	
	@Resource
	CifsAccessImpl cifsAccessImpl;
	
	@Resource
	DocumentService documentService;
	
	@Resource
	SignBookService signBookService;
	
	@Autowired
	SigningService signingService;
	
	@Resource
	FileService fileService;
	
	static int signWidth = 100;
	static int signHeight = 75;
	
	public SignRequest createSignRequest(User user, Document document, Map<String, String> params, String recipientEmail) {
		SignRequest signRequest = new SignRequest();
		signRequest.setName(document.getFileName());
    	signRequest.setCreateBy(user.getEppn());
    	signRequest.setCreateDate(new Date());
		signRequest.setOriginalFile(document);
		signRequest.setSignedFile(null);
		signRequest.setSignBookId(0);
		signRequest.setStatus(SignRequestStatus.uploaded);
		signRequest.setParams(params);
		signRequest.setRecipientEmail(recipientEmail);
        signRequest.persist();
		updateInfo(signRequest, SignRequestStatus.pending, "createSignRequest", user, "SUCCESS");
        return signRequest;
	}
	
	public void sign(SignRequest signRequest, User user, String password) throws EsupSignatureIOException, EsupSignatureException {
        File toSignFile = signRequest.getOriginalFile().getJavaIoFile();
    	SignType signType = SignType.valueOf(signRequest.getParams().get("signType"));
		File signedFile = null;
    	if(fileService.getContentType(toSignFile).equals("application/pdf")) {
            if(signType.equals(SignType.pdfImageStamp)) {
            	logger.info(user.getEppn() + " launch add imageStamp for signRequest : " + signRequest.getId());
            	signedFile = pdfService.stampImage(toSignFile, signRequest.getParams(), user);
            } else 
            if(signType.equals(SignType.certSign)) {
            	logger.info(user.getEppn() + " launch cades visible signature for signRequest : " + signRequest.getId());
              	signedFile = padesSign(signRequest, user, password);
            }
    	} else {
    		if(signType.equals(SignType.pdfImageStamp)) {
        		logger.warn("only pdf can get visible sign");
        	} else 
            if(signType.equals(SignType.certSign)) {
            	logger.info(user.getEppn() + " launch xades signature for signRequest : " + signRequest.getId());
              	signedFile = xadesSign(signRequest, user, password);
            }
    	}
        if(signedFile != null) {
        	addSignedFile(signRequest, signedFile, user);
        } else {
        	throw new EsupSignatureException("enable to sign document");
        }
	}

	public File padesSign(SignRequest signRequest, User user, String password) throws EsupSignatureKeystoreException {
		File signImage = user.getSignImage().getJavaIoFile();
		
		File toSignFile = pdfService.formatPdf(signRequest.getOriginalFile().getJavaIoFile(), signRequest.getParams());
		
        SignatureDocumentForm signatureDocumentForm = signingService.getPadesSignatureDocumentForm();
		signatureDocumentForm.setEncryptionAlgorithm(EncryptionAlgorithm.RSA);
		signatureDocumentForm.setDocumentToSign(fileService.toMultipartFile(toSignFile, "application/pdf"));
        
		File keyStoreFile = user.getKeystore().getJavaIoFile();
		SignatureTokenConnection signatureTokenConnection = userKeystoreService.getSignatureTokenConnection(keyStoreFile, password);
		CertificateToken certificateToken = userKeystoreService.getCertificateToken(keyStoreFile, password);
		CertificateToken[] certificateTokenChain = userKeystoreService.getCertificateTokenChain(keyStoreFile, password);

        signatureDocumentForm.setBase64Certificate(Base64.encodeBase64String(certificateToken.getEncoded()));
		List<String> base64CertificateChain = new ArrayList<>();
		for(CertificateToken token : certificateTokenChain) {
			base64CertificateChain.add(Base64.encodeBase64String(token.getEncoded()));
		}
		signatureDocumentForm.setBase64CertificateChain(base64CertificateChain);

		SignatureImageParameters imageParameters = new SignatureImageParameters();
		imageParameters.setPage(Integer.valueOf(signRequest.getParams().get("signPageNumber")));
		imageParameters.setxAxis(Integer.valueOf(signRequest.getParams().get("xPos")));
		imageParameters.setyAxis(Integer.valueOf(signRequest.getParams().get("yPos")));
		FileDocument fileDocumentImage = new FileDocument(signImage);
		fileDocumentImage.setMimeType(MimeType.PNG);
		imageParameters.setImage(fileDocumentImage);
		imageParameters.setWidth(signWidth);
		imageParameters.setHeight(signHeight);
		
		PAdESSignatureParameters parameters = new PAdESSignatureParameters();
		parameters.setSigningCertificate(certificateToken);
		parameters.setCertificateChain(certificateTokenChain);
		parameters.setSignatureImageParameters(imageParameters);
		//TODO ajuster signatue size
		parameters.setSignatureSize(100000);

		DSSDocument dssDocument = signingService.certSignDocument(signatureDocumentForm, parameters, signatureTokenConnection);
        InMemoryDocument signedPdfDocument = new InMemoryDocument(DSSUtils.toByteArray(dssDocument), dssDocument.getName(), dssDocument.getMimeType());
        
        try {
			return fileService.inputStreamToFile(signedPdfDocument.openStream(), signedPdfDocument.getName());
		} catch (IOException e) {
			logger.error("error to read signed file", e);
		}
        return null;
	}

	public File xadesSign(SignRequest signRequest, User user, String password) throws EsupSignatureKeystoreException {
		File toSignFile = signRequest.getOriginalFile().getJavaIoFile();
		
        SignatureDocumentForm signatureDocumentForm = signingService.getXadesSignatureDocumentForm();
		signatureDocumentForm.setEncryptionAlgorithm(EncryptionAlgorithm.RSA);
		signatureDocumentForm.setDocumentToSign(fileService.toMultipartFile(toSignFile, fileService.getContentType(toSignFile)));
        
		File keyStoreFile = user.getKeystore().getJavaIoFile();
		SignatureTokenConnection signatureTokenConnection = userKeystoreService.getSignatureTokenConnection(keyStoreFile, password);
		CertificateToken certificateToken = userKeystoreService.getCertificateToken(keyStoreFile, password);
		CertificateToken[] certificateTokenChain = userKeystoreService.getCertificateTokenChain(keyStoreFile, password);

        signatureDocumentForm.setBase64Certificate(Base64.encodeBase64String(certificateToken.getEncoded()));
		List<String> base64CertificateChain = new ArrayList<>();
		for(CertificateToken token : certificateTokenChain) {
			base64CertificateChain.add(Base64.encodeBase64String(token.getEncoded()));
		}
		signatureDocumentForm.setBase64CertificateChain(base64CertificateChain);
		
		ASiCWithXAdESSignatureParameters parameters = new ASiCWithXAdESSignatureParameters();
		parameters.setSigningCertificate(certificateToken);
		parameters.setCertificateChain(certificateTokenChain);

		DSSDocument dssDocument = signingService.certSignDocument(signatureDocumentForm, parameters, signatureTokenConnection);
        InMemoryDocument signedPdfDocument = new InMemoryDocument(DSSUtils.toByteArray(dssDocument), dssDocument.getName(), dssDocument.getMimeType());
        
        try {
			return fileService.inputStreamToFile(signedPdfDocument.openStream(), signedPdfDocument.getName());
		} catch (IOException e) {
			logger.error("error to read signed file", e);
		}
        return null;
	}

	
	public void nexuSign(SignRequest signRequest, User user, SignatureDocumentForm signatureDocumentForm) throws EsupSignatureKeystoreException, EsupSignatureIOException {
		logger.info(user.getEppn() + " launch cades nexu signature for signRequest : " + signRequest.getId());
		DSSDocument dssDocument = signingService.signDocument(signatureDocumentForm);
        InMemoryDocument signedPdfDocument = new InMemoryDocument(DSSUtils.toByteArray(dssDocument), dssDocument.getName(), dssDocument.getMimeType());
        
        try {
        	File signedFile = fileService.inputStreamToFile(signedPdfDocument.openStream(), signedPdfDocument.getName());
        	if(signedFile != null){
            	addSignedFile(signRequest, signedFile, user);        		
        	}
		} catch (IOException e) {
			logger.error("error to read signed file", e);
		}
	}
	
	public void addSignedFile(SignRequest signRequest, File signedFile, User user) throws EsupSignatureIOException {
		try {
			signRequest.setSignedFile(documentService.addFile(signedFile, "signed_" + signRequest.getSignTypeLabel() + "_" + user.getEppn() + "_" + signedFile.getName(), fileService.getContentType(signedFile)));
			updateInfo(signRequest, SignRequestStatus.signed, "sign", user, "SUCCESS");		
		} catch (IOException e) {
			throw new EsupSignatureIOException("error on save signed file", e);
		}
	}
	
	public void updateInfo(SignRequest signRequest, SignRequestStatus signRequestStatus, String action, User user, String returnCode) {
		Log log = new Log();
		log.setSignRequestId(signRequest.getId());
		log.setEppn(user.getEppn());
		log.setInitialStatus(signRequest.getStatus().toString());
		log.setLogDate(new Date());
		log.setFinalStatus(signRequestStatus.toString());
		log.setIp(user.getIp());
		log.setAction(action);
		log.setReturnCode(returnCode);
		log.persist();
		signRequest.setStatus(signRequestStatus);
		if (signRequest.getSignBookId() != 0) {
			SignBook signBook = SignBook.findSignBook(signRequest.getSignBookId());
			try {
				signBookService.removeSignRequestFromSignBook(signRequest, signBook, user);
			} catch (EsupSignatureException e) {
			}
			if (signBook.getTargetType().equals(DocumentIOType.cifs)) {
				try {
					InputStream in = new FileInputStream(signRequest.getSignedFile().getJavaIoFile());
					cifsAccessImpl.putFile("/" + signBook.getDocumentsTargetUri() + "/",
							signRequest.getSignedFile().getFileName(), in, user, null);
				} catch (FileNotFoundException e) {
					logger.error("error on cifs copy", e);				}
			}
		}
	}
	
	public boolean checkUserSignRights(User user, SignRequest signRequest) {
    	if(
    			signRequest.getStatus().equals(SignRequestStatus.pending) 
    			&&
    			(
    			(signRequest.getCreateBy().equals(user.getEppn()) && signRequest.getRecipientEmail() == null) 
    			||
    			(signRequest.getRecipientEmail() != null && signRequest.getRecipientEmail().equals(user.getEmail())))) 
    	{
    	    return true;
    	} else {
    		return false;
    	}
	}
	
	public boolean checkUserViewRights(User user, SignRequest signRequest) {
    	if(
    			signRequest.getCreateBy().equals(user.getEppn())
    			||
    			(signRequest.getRecipientEmail() != null && signRequest.getRecipientEmail().equals(user.getEmail()))) 
    	{
    	    return true;
    	} else {
    		return false;
    	}
	}
	
}
