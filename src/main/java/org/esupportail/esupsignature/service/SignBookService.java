package org.esupportail.esupsignature.service;

import java.util.HashMap;

import javax.annotation.Resource;

import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.domain.SignBook;
import org.esupportail.esupsignature.domain.SignRequest;
import org.esupportail.esupsignature.domain.SignRequest.SignRequestStatus;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.service.fs.cifs.CifsAccessImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jcifs.smb.SmbFile;

@Service
public class SignBookService {
	
	private static final Logger log = LoggerFactory.getLogger(SignBookService.class);

	
	@Resource
	private CifsAccessImpl cifsAccessImpl;

	@Resource
	private SignRequestService signRequestService;
	
	@Resource
	private DocumentService documentService;
	
	
	public void importFilesFromCIFS(String source, SignBook signBook, User user) {
        try {
        	SmbFile[] files = cifsAccessImpl.listFiles(source, user);
        	Document documentToAdd = documentService.addFile(files[0].getInputStream(), files[0].getName(), files[0].getContentLengthLong(), files[0].getContentType());
            SignRequest signRequest = signRequestService.createSignRequest(user.getEppn(), documentToAdd, new HashMap<String, String>(signBook.getParams()), signBook.getRecipientEmail());
            signBook.getSignRequests().add(signRequest);
            signBook.persist();
            files[0].getInputStream().close();
            files[0].close();
            cifsAccessImpl.remove("/" + signBook.getDocumentsSourceUri() + "/", user);
        } catch (Exception e) {
        	log.error("read cifs file error : ", e);
        }
	}
	
	public void importSignRequest(SignRequest signRequest, SignBook signBook) throws EsupSignatureException {
    	if(signRequest.getSignedFile() != null) {
    		if(!signBook.getSignRequests().contains(signRequest)) {
		    	signRequest.getOriginalFile().remove();
		    	signRequest.setOriginalFile(signRequest.getSignedFile());
		    	signRequest.setSignedFile(null);
		    	signRequest.setStatus(SignRequestStatus.pending);
		    	signRequest.setRecipientEmail(signBook.getRecipientEmail());
		    	signRequest.setParams(new HashMap<String, String>(signBook.getParams()));
		    	signRequest.merge();
		    	signBook.getSignRequests().add(signRequest);
		    	signBook.merge();
    		} else {
    			throw new EsupSignatureException("allready in this signbook");
    		}
    	} else {
    		throw new EsupSignatureException("no signed file found");
    	}
	}
}
