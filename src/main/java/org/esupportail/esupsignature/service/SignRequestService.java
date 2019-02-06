package org.esupportail.esupsignature.service;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

import javax.annotation.Resource;

import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.domain.Log;
import org.esupportail.esupsignature.domain.SignBook.NewPageType;
import org.esupportail.esupsignature.domain.SignBook.SignType;
import org.esupportail.esupsignature.domain.SignRequest;
import org.esupportail.esupsignature.domain.SignRequest.SignRequestStatus;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.exception.EsupSignatureKeystoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SignRequestService {
	
	private static final Logger log = LoggerFactory.getLogger(SignRequestService.class);
	
	@Resource
	PdfService pdfService;
	
	@Resource
	DocumentService documentService;
	
	@Resource
	FileService fileService;
	
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
	}
	
	public void sign(SignRequest signRequest, User user, String password) throws EsupSignatureKeystoreException {
    	File signImage = user.getSignImage().getBigFile().toJavaIoFile();
        File toSignFile = signRequest.getOriginalFile().getBigFile().toJavaIoFile();
    	NewPageType newPageType = NewPageType.valueOf(signRequest.getParams().get("newPageType"));
    	SignType signType = SignType.valueOf(signRequest.getParams().get("signType"));
    	int signPageNumber = Integer.valueOf(signRequest.getParams().get("signPageNumber"));
    	int xPos = Integer.valueOf(signRequest.getParams().get("xPos"));
    	int yPos = Integer.valueOf(signRequest.getParams().get("yPos"));
        try {
        	if(fileService.getContentType(toSignFile).equals("application/pdf")) {
	        	File signedFile = pdfService.signPdf(toSignFile, signImage, signType, signPageNumber, xPos, yPos, newPageType, user, password);
		        if(signedFile != null) {
					signRequest.setSignedFile(documentService.addFile(signedFile, "signed_by_" + user.getEppn() + "_" + signRequest.getName(), "application/pdf"));
					updateInfo(signRequest, SignRequestStatus.signed, "sign", user, "SUCCESS");
		        }
        	}
        } catch (IOException e) {
        	log.error("file to sign or sign image opening error", e);
		}
	}
}
