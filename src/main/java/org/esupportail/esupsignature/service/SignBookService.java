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
import org.esupportail.esupsignature.service.fs.opencmis.CmisAccessImpl;
import org.esupportail.esupsignature.service.fs.vfs.VfsAccessImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SignBookService {
	
	private static final Logger log = LoggerFactory.getLogger(SignBookService.class);
	
	@Resource
	private FileService fileService;
	
	@Resource
	private CifsAccessImpl cifsAccessImpl;

	@Resource
	private VfsAccessImpl vfsAccessImpl;

	@Resource
	private CmisAccessImpl cmisAccessImpl;
	
	@Resource
	private SignRequestService signRequestService;
	
	@Resource
	private DocumentService documentService;
	
	
	public void importFilesFromSource(SignBook signBook, User user) {
		FsAccessService fsAccessService = getFsAccessService(signBook.getSourceType());
        try {
        	List<File> files = fsAccessService.listFiles(signBook.getDocumentsSourceUri(), user);
        	if(files.size() > 0) {
	        	for(File file : files) {
	            	Document documentToAdd = documentService.addFile(file, file.getName(), fileService.getContentType(file));
	                SignRequest signRequest = signRequestService.createSignRequest(user, documentToAdd, new HashMap<String, String>(signBook.getParams()), signBook.getRecipientEmail());
	                signBook.getSignRequests().add(signRequest);
	                signBook.persist();
	                fsAccessService.remove(signBook.getDocumentsSourceUri() + "/" + file.getName(), user);
	        	}
        	} else {
        		log.info("no file to import in this folder : " + signBook.getDocumentsSourceUri());
        	}
        } catch (Exception e) {
        	log.error("read fsaccess error : ", e);
        }
	}
	
	private FsAccessService getFsAccessService(DocumentIOType type) {
		FsAccessService fsAccessService = null;
		switch (type) {
			case cifs:
				fsAccessService = cifsAccessImpl;
				break;
			case vfs:
				fsAccessService = vfsAccessImpl;
				break;
			case cmis:
				fsAccessService = cmisAccessImpl;
				break;
			default:
				break;
		}
		return fsAccessService;
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
