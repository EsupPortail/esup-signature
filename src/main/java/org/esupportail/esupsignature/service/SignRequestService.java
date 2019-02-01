package org.esupportail.esupsignature.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;

import javax.annotation.Resource;

import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.domain.SignBook.NewPageType;
import org.esupportail.esupsignature.domain.SignBook.SignType;
import org.esupportail.esupsignature.domain.SignRequest;
import org.esupportail.esupsignature.domain.SignRequest.SignRequestStatus;
import org.esupportail.esupsignature.domain.User;
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
	
	public SignRequest createSignRequest(String eppn, Document document, Map<String, String> params, String recipientEmail) {
		
		SignRequest signRequest = new SignRequest();
		signRequest.setName(document.getFileName());
    	signRequest.setCreateBy(eppn);
    	signRequest.setCreateDate(new Date());
		signRequest.setOriginalFile(document);
		signRequest.setSignedFile(null);
		signRequest.setSignBookId(0);
		signRequest.setStatus(SignRequestStatus.pending);
		signRequest.setParams(params);
		signRequest.setRecipientEmail(recipientEmail);
        signRequest.persist();
        return signRequest;
	}
	
	public void updateInfo(SignRequest signRequest, SignRequestStatus signRequestStatus, User user) {
		signRequest.setStatus(signRequestStatus);
    	signRequest.setUpdateBy(user.getEppn());
    	signRequest.setUpdateDate(new Date());
	}
	
	public InputStream sign(SignRequest signRequest, User user, String base64PemCert, int signPageNumber, int xPos, int yPos) throws FileNotFoundException {
		InputStream in = null;
		Map<String, String> params = signRequest.getParams();    	
    	File signImage = user.getSignImage().getBigFile().toJavaIoFile();
        File toSignFile = signRequest.getOriginalFile().getBigFile().toJavaIoFile();
    	NewPageType newPageType = NewPageType.valueOf(params.get("newPageType"));
    	SignType signType = SignType.valueOf(params.get("signType"));
        try {
        	File signedFile = pdfService.signPdf(toSignFile, signImage, signType, base64PemCert, signPageNumber, xPos, yPos, newPageType);
        	in = new FileInputStream(signedFile);
	        if(signedFile != null) {
	        	params.put("signPageNumber", String.valueOf(signPageNumber));
				params.put("xPos", String.valueOf(xPos));
				params.put("yPos", String.valueOf(yPos));
				signRequest.setParams(params);
				signRequest.setSignedFile(documentService.addFile(signedFile, "signed_by_" + user.getEppn() + "_" + signRequest.getName(), "application/pdf"));
				updateInfo(signRequest, SignRequestStatus.signed, user);
	        }
        } catch (IOException e) {
        	log.error("file to sign or sign image opening error", e);
		}
        return in;
	}
}
