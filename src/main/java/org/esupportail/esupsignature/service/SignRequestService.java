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
import org.esupportail.esupsignature.domain.Log;
import org.esupportail.esupsignature.domain.SignBook;
import org.esupportail.esupsignature.domain.SignBook.DocumentIOType;
import org.esupportail.esupsignature.domain.SignBook.SignType;
import org.esupportail.esupsignature.domain.SignRequest;
import org.esupportail.esupsignature.domain.SignRequest.SignRequestStatus;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureKeystoreException;
import org.esupportail.esupsignature.service.fs.cifs.CifsAccessImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SignRequestService {
	
	private static final Logger logger = LoggerFactory.getLogger(SignRequestService.class);
	
	@Resource
	PdfService pdfService;
	
	@Resource
	CifsAccessImpl cifsAccessImpl;
	
	@Resource
	DocumentService documentService;
	
	@Resource
	SignBookService signBookService;
	
	@Resource
	FileService fileService;
	
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
		if (signRequest.getSignBookId() != 0) {
			SignBook signBook = SignBook.findSignBook(signRequest.getSignBookId());
			try {
				signBookService.removeSignRequestFromSignBook(signRequest, signBook, user);
			} catch (EsupSignatureException e) {
			}
			if (signBook.getTargetType().equals(DocumentIOType.cifs)) {
				try {
					InputStream in = new FileInputStream(signRequest.getSignedFile().getBigFile().toJavaIoFile());
					cifsAccessImpl.putFile("/" + signBook.getDocumentsTargetUri() + "/",
							signRequest.getSignedFile().getFileName(), in, user, null);
				} catch (FileNotFoundException e) {
					logger.error("error on cifs copy", e);				}
			}

		}
			
	}
	
	public void sign(SignRequest signRequest, User user, String password) throws EsupSignatureKeystoreException, EsupSignatureIOException {
    	File signImage = user.getSignImage().getBigFile().toJavaIoFile();
        File toSignFile = signRequest.getOriginalFile().getBigFile().toJavaIoFile();
    	SignType signType = SignType.valueOf(signRequest.getParams().get("signType"));

    	if(fileService.getContentType(toSignFile).equals("application/pdf")) {
    		File signedFile = null;
            if(signType.equals(SignType.imageStamp)) {
            	logger.info("imageStamp signature ");
            	signedFile = pdfService.stampImageSign(toSignFile, signImage, signRequest.getParams());
            } else 
            if(signType.equals(SignType.certPAdES)) {
            	logger.info("cades signature");
              	signedFile = pdfService.padesSign(toSignFile, signImage, signRequest.getParams(), user, password);
            }

	        if(signedFile != null) {
	        	addSignedFile(signRequest, signedFile, user);
	        }
    	} else {
    		logger.warn("only pdf sign");
    	}

	}
	
	public void addSignedFile(SignRequest signRequest, File signedFile, User user) throws EsupSignatureIOException {
		try {
			signRequest.setSignedFile(documentService.addFile(signedFile, "signed_by_" + user.getEppn() + "_" + signRequest.getName(), "application/pdf"));
			updateInfo(signRequest, SignRequestStatus.signed, "sign", user, "SUCCESS");		
		} catch (IOException e) {
			throw new EsupSignatureIOException("error on save signed file", e);
		}
	}
	
}
