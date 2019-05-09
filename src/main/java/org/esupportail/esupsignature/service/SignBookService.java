package org.esupportail.esupsignature.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Resource;

import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.domain.SignBook;
import org.esupportail.esupsignature.domain.SignBook.DocumentIOType;
import org.esupportail.esupsignature.domain.SignBook.SignBookType;
import org.esupportail.esupsignature.domain.SignRequest;
import org.esupportail.esupsignature.domain.SignRequest.SignRequestStatus;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.service.fs.EsupStockException;
import org.esupportail.esupsignature.service.fs.FsAccessService;
import org.esupportail.esupsignature.service.fs.FsFile;
import org.esupportail.esupsignature.service.fs.UploadActionType;
import org.esupportail.esupsignature.service.fs.cifs.CifsAccessImpl;
import org.esupportail.esupsignature.service.fs.opencmis.CmisAccessImpl;
import org.esupportail.esupsignature.service.fs.vfs.VfsAccessImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.stereotype.Service;

@Service
public class SignBookService {

	private static final Logger logger = LoggerFactory.getLogger(SignBookService.class);

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

	@Resource
	private ReloadableResourceBundleMessageSource messageSource;
	
	@Value("${sign.defaultPositionX}")
	private int defaultPositionX;
	@Value("${sign.defaultPositionY}")
	private int defaultPositionY;
	
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

	public SignBook createUserSignBook(User user) {
		SignBook signbook = new SignBook();
		signbook.setName(user.getFirstname() + " " + user.getName());
		signbook.setDescription("Parapheur personnel de " + signbook.getName());
		signbook.setCreateBy(user.getEppn());
		signbook.setCreateDate(new Date());
		signbook.getRecipientEmails().add(user.getEmail());
		signbook.setSignRequestParams(null);
		signbook.setModelFile(null);
		signbook.setSignBookType(SignBookType.user);
		signbook.setSourceType(DocumentIOType.none);
		signbook.setTargetType(DocumentIOType.none);
		signbook.setSignRequestParams(signRequestService.getEmptySignRequestParams());
		signbook.persist();
		return signbook;
	}
	
	public void resetSignBookParams(SignBook signBook) {
		signBook.getSignRequestParams().setSignPageNumber(1);
		signBook.getSignRequestParams().setXPos(defaultPositionX);
		signBook.getSignRequestParams().setYPos(defaultPositionY);
		signBook.merge();
	}
	
	public void importFilesFromSource(SignBook signBook, User user) throws EsupSignatureIOException, EsupStockException {
		if (signBook.getSourceType() != null && !signBook.getSourceType().equals(DocumentIOType.none)) {
			logger.info("retrieve from " + signBook.getSourceType() + " in " + signBook.getDocumentsSourceUri());
			FsAccessService fsAccessService = getFsAccessService(signBook.getSourceType());
			try {
				List<FsFile> fsFiles = fsAccessService.listFiles(signBook.getDocumentsSourceUri());
				if (fsFiles.size() > 0) {
					for (FsFile fsFile : fsFiles) {
						logger.info("adding file : " + fsFile.getFile().getName());
						fsFile.setPath(signBook.getDocumentsSourceUri());
						Document documentToAdd = documentService.createDocument(fsFile.getFile(), fsFile.getName(), fsFile.getContentType());
						if (fsFile.getCreateBy() != null && User.countFindUsersByEppnEquals(fsFile.getCreateBy()) > 0) {
							user = User.findUsersByEppnEquals(fsFile.getCreateBy()).getSingleResult();
							user.setIp("127.0.0.1");
						}
						List<String> signBookRecipientsEmails = new ArrayList<>();
						signBookRecipientsEmails.add(user.getEmail());
						SignRequest signRequest = signRequestService.createSignRequest(new SignRequest(), user, documentToAdd, signBook.getSignRequestParams());
						signRequest.merge();
						fsAccessService.remove(fsFile);
					}
				} else {
					logger.info("no file to import in this folder : " + signBook.getDocumentsSourceUri());
					throw new EsupSignatureIOException("alert_no_file_to_import");
				}
			} catch (IOException e) {
				logger.error("read fsaccess error : ", e);
			}
		} else {
			logger.debug("no source type for signbook : " + signBook.getName());
		}
	}

	public void exportFilesToTarget(SignBook signBook, User user) throws EsupSignatureException {
		for (SignRequest signRequest : signBook.getSignRequests()) {
			if (signRequest.getStatus().equals(SignRequestStatus.signed) && signRequestService.isSignRequestCompleted(signRequest)) {
				exportFileToTarget(signBook, signRequest, user);
				//signRequestService.updateInfo(signRequest, SignRequestStatus.exported, "export to target " + signBook.getTargetType() + " : " + signBook.getDocumentsTargetUri(), user, "SUCCESS");
				removeSignRequestFromAllSignBooks(signRequest);
			}
		}
	}

	public void exportFileToTarget(SignBook signBook, SignRequest signRequest, User user) throws EsupSignatureException {
		if (signBook.getTargetType() != null && !signBook.getTargetType().equals(DocumentIOType.none)) {
			logger.info("send to " + signBook.getTargetType() + " in " + signBook.getDocumentsTargetUri());
			FsAccessService fsAccessService = getFsAccessService(signBook.getSourceType());
			try {
				File signedFile = signRequestService.getLastSignedDocument(signRequest).getJavaIoFile();
				InputStream inputStream = new FileInputStream(signedFile);
				fsAccessService.putFile(signBook.getDocumentsTargetUri(), signedFile.getName(), inputStream, UploadActionType.OVERRIDE);
				signRequestService.updateStatus(signRequest, SignRequestStatus.exported, messageSource.getMessage("updateinfo_exporttotarget", null, Locale.FRENCH) + " " + signBook.getTargetType() + " : " + signBook.getDocumentsTargetUri(), user, "SUCCESS", "");
			} catch (Exception e) {
				throw new EsupSignatureException("write fsaccess error : ", e);
			}
		} else {
			logger.debug("no target type for this signbook");
		}
	}

	public void importSignRequestInSignBook(SignRequest signRequest, SignBook signBook, User user) throws EsupSignatureException {

		if (!signBook.getSignRequests().contains(signRequest)) {
			signBook.getSignRequests().add(signRequest);
			//signBook.merge();
			importSignRequestByRecipients(signRequest, signBook.getRecipientEmails(), user);
			signRequest.getOriginalSignBookNames().add(signBook.getName());
			signRequestService.updateStatus(signRequest, SignRequestStatus.draft, messageSource.getMessage("updateinfo_sendtosignbook", null, Locale.FRENCH) + " " + signBook.getName(), user, "SUCCESS", "");
		} else {
			//throw new EsupSignatureException(signRequest.getId() + " is already in signbook" + signBook.getName());
			logger.warn(signRequest.getId() + " is already in signbook" + signBook.getName());
		}
	}

	public void importSignRequestByRecipients(SignRequest signRequest,  List<String> recipientEmails, User user) {
		for(String recipientEmail : recipientEmails) {
			List<String> recipientEmailsList = new ArrayList<>();
			recipientEmailsList.add(recipientEmail);
			SignBook signBook = SignBook.findSignBooksByRecipientEmailsAndSignBookTypeEquals(recipientEmailsList, SignBookType.user).getSingleResult();
			/*
			if(signBook.getSignBookType().equals(SignBookType.group)) {
				if(!signRequest.getSignBooks().containsKey(signBook.getId())) {
					List<String> recipientsEmailsFromGroup = signBook.getRecipientEmails();
					for(String recipientEmailFromGroup : recipientsEmailsFromGroup) {
						List<String> recipientEmailFromGroupList = new ArrayList<>();
						recipientEmailFromGroupList.add(recipientEmailFromGroup);
						SignBook signBookFromGroup = SignBook.findSignBooksByRecipientEmailsAndSignBookTypeEquals(recipientEmailFromGroupList, SignBookType.user).getSingleResult();
						signRequest.getSignBooks().put(signBookFromGroup.getId(), false);
						signBookFromGroup.getSignRequests().add(signRequest);
						//signRequestService.updateInfo(signRequest, SignRequestStatus.pending, "imported in signbook " + signBook.getName(), user, "SUCCESS", "");
						logger.info("signRequest " + signRequest.getId() + " added to signBook : " + signBook.getId() + " by " + user.getEppn());
					}
				} else {
					throw new EsupSignatureException(signRequest.getId() + " is already in signbook" + signBook.getName());
				}
			} else {
				*/
				if(!signRequest.getSignBooks().containsKey(signBook.getId())) {
					signRequest.getSignBooks().put(signBook.getId(), false);
				} else {
					//throw new EsupSignatureException(signRequest.getId() + " is already in signbook" + signBook.getName());
					logger.warn(signRequest.getId() + " is already in signbook" + signBook.getName());
				}
			//}
		}
	}
	
	public void removeSignRequestFromAllSignBooks(SignRequest signRequest) {
		List<SignBook> signBooks = signRequestService.getSignBooksList(signRequest);
		for(SignBook signBook : signBooks) {
			List<SignRequest> signRequests = new ArrayList<>();
			signRequests.addAll(signBook.getSignRequests());
			signRequests.remove(signRequest);
			signBook.setSignRequests(signRequests);
			signBook.merge();
		}
		signRequest.getSignBooks().clear();
		signRequest.getOriginalSignBookNames().clear();
	}

	public void removeSignRequestFromSignBook(SignRequest signRequest, SignBook signBook, User user) {
		signRequest.getSignBooks().remove(signBook.getId());
		signBook.getSignRequests().remove(signRequest);
		signBook.merge();
	}
	
	public SignBook getSignBookBySignRequestAndUser(SignRequest signRequest, User user) {
		if (signRequest.getSignBooks().size() > 0) {
			for(Map.Entry<Long, Boolean> signBookId : signRequest.getSignBooks().entrySet()) {
				SignBook signBook = SignBook.findSignBook(signBookId.getKey());
				if(signBook.getRecipientEmails().contains(user.getEmail()) && signRequest.getSignBooks().containsKey(signBookId.getKey())) {
					return signBook;
				}
			}
		}
		return null;
	}

	public List<SignBook> getSignBookBySignRequest(SignRequest signRequest) {
		List<SignRequest> signRequests = Arrays.asList(signRequest);
		List<SignBook> signBooks = SignBook.findSignBooksBySignRequestsEquals(signRequests).getResultList();
		return signBooks;
	}
	
	public boolean checkUserManageRights(User user, SignBook signBook) {
		if (signBook.getCreateBy().equals(user.getEppn()) || signBook.getModeratorEmails().contains(user.getEmail())) {
			return true;
		} else {
			return false;
		}
	}
	
}
