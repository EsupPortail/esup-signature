package org.esupportail.esupsignature.service;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Resource;

import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.domain.SignBook;
import org.esupportail.esupsignature.domain.SignBook.DocumentIOType;
import org.esupportail.esupsignature.domain.SignRequest;
import org.esupportail.esupsignature.domain.SignRequest.SignRequestStatus;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.service.fs.FsAccessService;
import org.esupportail.esupsignature.service.fs.cifs.CifsAccessImpl;
import org.esupportail.esupsignature.service.fs.vfs.VfsAccessImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SignBookService {
	
	private static final Logger log = LoggerFactory.getLogger(SignBookService.class);
	
	@Resource
	FileService fileService;
	
	@Resource
	private CifsAccessImpl cifsAccessImpl;

	@Resource
	private VfsAccessImpl vfsAccessImpl;
	
	@Resource
	private SignRequestService signRequestService;
	
	@Resource
	private DocumentService documentService;
	
	
	public void importFilesFromSource(SignBook signBook, User user) {
		FsAccessService fsAccessService = null;
		if(signBook.getSourceType().equals(DocumentIOType.cifs)) {
			fsAccessService = cifsAccessImpl;
		} else if(signBook.getSourceType().equals(DocumentIOType.vfs)) {
			fsAccessService = vfsAccessImpl;
		}
        try {
        	List<File> files = fsAccessService.listFiles(signBook.getDocumentsSourceUri(), user);
        	Document documentToAdd = documentService.addFile(files.get(0), files.get(0).getName(), fileService.getContentType(files.get(0)));
            SignRequest signRequest = signRequestService.createSignRequest(user, documentToAdd, new HashMap<String, String>(signBook.getParams()), signBook.getRecipientEmail());
            signBook.getSignRequests().add(signRequest);
            signBook.persist();
            fsAccessService.remove("/" + signBook.getDocumentsSourceUri() + "/", user);
        } catch (Exception e) {
        	log.error("read cifs file error : ", e);
        }
	}
	
	public void importSignRequestInSignBook(SignRequest signRequest, SignBook signBook, User user) throws EsupSignatureException {
		if(!signBook.getSignRequests().contains(signRequest)) {
	    	signRequest.getOriginalFile().remove();
	    	signRequest.setOriginalFile(signRequest.getSignedFile());
	    	signRequest.setSignedFile(null);
	    	signRequest.setRecipientEmail(signBook.getRecipientEmail());
	    	signRequest.setParams(new HashMap<String, String>(signBook.getParams()));
	    	signRequest.setSignBookId(signBook.getId());
	    	signRequest.setStatus(SignRequestStatus.uploaded);
	    	signRequest.merge();
	    	signBook.getSignRequests().add(signRequest);
	    	signRequestService.updateInfo(signRequest, SignRequestStatus.pending, "importInSignBook", user, "SUCCESS");
	    	signBook.merge();
		} else {
			throw new EsupSignatureException("allready in this signbook");
		}
	}
	
	public void removeSignRequestFromSignBook(SignRequest signRequest, SignBook signBook, User user) throws EsupSignatureException {
		if(signBook.getSignRequests().contains(signRequest)) {
			signRequestService.updateInfo(signRequest, SignRequestStatus.completed, "removeFromSignBook", user, "SUCCESS");
			signRequest.setSignBookId(0);
	    	signRequest.merge();
	    	signBook.getSignRequests().remove(signRequest);
	    	signBook.merge();
		} else {
			throw new EsupSignatureException("not in this signbook");
		}
	}
}
