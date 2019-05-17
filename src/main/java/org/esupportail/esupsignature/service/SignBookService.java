package org.esupportail.esupsignature.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import org.esupportail.esupsignature.domain.SignRequestParams;
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
import org.springframework.web.multipart.MultipartFile;

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
	private UserService userService;
	
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
	
	public void creatorSignBook(User user) {
		SignBook signBook;
		if(SignBook.countFindSignBooksByRecipientEmailsAndSignBookTypeEquals(Arrays.asList("creator"), SignBookType.system) == 0) {
			signBook = new SignBook();
			signBook.setName("Créateur de la demande");
			signBook.setRecipientEmails(Arrays.asList("creator"));
			signBook.setCreateDate(new Date());
			signBook.setSignRequestParams(null);
			signBook.setModelFile(null);
			signBook.setSignBookType(SignBookType.system);
			signBook.setSourceType(DocumentIOType.none);
			signBook.setTargetType(DocumentIOType.none);
			signBook.setSignRequestParams(new ArrayList<>());
			signBook.getSignRequestParams().add(signRequestService.getEmptySignRequestParams());
			signBook.persist();
		}
		
	}
	
	public SignBook createUserSignBook(User user) {
		SignBook signBook = new SignBook();
		signBook.setName(user.getFirstname() + " " + user.getName());
		signBook.setDescription("Parapheur personnel de " + signBook.getName());
		signBook.setCreateBy(user.getEppn());
		signBook.setCreateDate(new Date());
		signBook.getRecipientEmails().add(user.getEmail());
		signBook.setSignRequestParams(null);
		signBook.setModelFile(null);
		signBook.setSignBookType(SignBookType.user);
		signBook.setSourceType(DocumentIOType.none);
		signBook.setTargetType(DocumentIOType.none);
		signBook.setSignRequestParams(new ArrayList<>());
		signBook.getSignRequestParams().add(signRequestService.getEmptySignRequestParams());
		signBook.persist();
		return signBook;
	}
	
	public void updateSignBook(SignBook signBook, SignBook signBookToUpdate, SignRequestParams signRequestParams, MultipartFile multipartFile) throws EsupSignatureException {
		signBookToUpdate.getRecipientEmails().removeAll(signBook.getRecipientEmails());
		signBookToUpdate.getRecipientEmails().addAll(signBook.getRecipientEmails());
		signBookToUpdate.getModeratorEmails().removeAll(signBook.getModeratorEmails());
		signBookToUpdate.getModeratorEmails().addAll(signBook.getModeratorEmails());
		signBookToUpdate.setName(signBook.getName());
		signBookToUpdate.setDocumentsSourceUri(signBook.getDocumentsSourceUri());
		signBookToUpdate.setSourceType(signBook.getSourceType());
		signBookToUpdate.setDocumentsTargetUri(signBook.getDocumentsTargetUri());
		signBookToUpdate.setTargetType(signBook.getTargetType());
		signBookToUpdate.getSignRequestParams().get(0).setSignType(signRequestParams.getSignType());
		signBookToUpdate.getSignRequestParams().get(0).setNewPageType(signRequestParams.getNewPageType());
		signBookToUpdate.setAutoRemove(signBook.isAutoRemove());
		if(!multipartFile.isEmpty()) {
			Document newModel;
			try {
				newModel = documentService.createDocument(multipartFile, multipartFile.getOriginalFilename());
			} catch (IOException e) {
				logger.error("enable to add model", e);
				throw new EsupSignatureException(e.getMessage(), e);
			}
			if(newModel != null) {
				Document oldModel = signBookToUpdate.getModelFile();
				signBookToUpdate.setModelFile(newModel);
				oldModel.remove();
			}
			newModel.setParentId(signBookToUpdate.getId());
		}
		signBookToUpdate.merge();
		
	}
	
	public void createGroupSignBook(SignBook signBook, User user, SignRequestParams signRequestParams, MultipartFile multipartFile) throws EsupSignatureException {
		if(SignBook.countFindSignBooksByNameEquals(signBook.getName()) == 0) {
			signRequestParams.persist();
			signBook.setSignBookType(SignBookType.group);
			signBook.setCreateBy(user.getEppn());
			signBook.setCreateDate(new Date());
			signBook.getRecipientEmails().removeAll(Collections.singleton(""));
			for(String recipientEmail : signBook.getRecipientEmails()) {
				if(SignBook.countFindSignBooksByRecipientEmailsAndSignBookTypeEquals(Arrays.asList(recipientEmail), SignBookType.user) == 0) {
					userService.createUser(recipientEmail);
				}
			}
			signBook.getModeratorEmails().removeAll(Collections.singleton(""));
			for(String moderatorEmail : signBook.getModeratorEmails()) {
				if(SignBook.countFindSignBooksByRecipientEmailsEquals(Arrays.asList(moderatorEmail)) == 0) {
					userService.createUser(moderatorEmail);
				}
			}
			Document model = null;
			if(multipartFile != null) {
				try {
					model = documentService.createDocument(multipartFile, multipartFile.getOriginalFilename());
				} catch (IOException e) {
					logger.error("enable to add model", e);
					throw new EsupSignatureException(e.getMessage(), e);
				}
				signBook.setModelFile(model);
			}
			signBook.getSignRequestParams().add(signRequestParams);
			signBook.persist();
			if(model != null) {
				model.setParentId(signBook.getId());
			}
		} else {
			throw new EsupSignatureException("all ready exist");
		}
	}
	
	public void createWorkflowSignBook(SignBook signBook, User user, SignRequestParams signRequestParams, MultipartFile multipartFile) throws EsupSignatureException {
		if(SignBook.countFindSignBooksByNameEquals(signBook.getName()) == 0) {
			signRequestParams.persist();
			signBook.setSignBookType(SignBookType.workflow);
			signBook.setCreateBy(user.getEppn());
			signBook.setCreateDate(new Date());
			signBook.getRecipientEmails().removeAll(Collections.singleton(""));
			signBook.getModeratorEmails().removeAll(Collections.singleton(""));
			for(String moderatorEmail : signBook.getModeratorEmails()) {
				if(SignBook.countFindSignBooksByRecipientEmailsEquals(Arrays.asList(moderatorEmail)) == 0) {
					userService.createUser(moderatorEmail);
				}
			}
			Document model = null;
			if(multipartFile != null) {
				try {
					model = documentService.createDocument(multipartFile, multipartFile.getOriginalFilename());
				} catch (IOException e) {
					logger.error("enable to add model", e);
					throw new EsupSignatureException(e.getMessage(), e);
				}
				signBook.setModelFile(model);
			}
			signBook.getSignRequestParams().add(signRequestParams);
			signBook.persist();
			if(model != null) {
				model.setParentId(signBook.getId());
			}
		} else {
			throw new EsupSignatureException("all ready exist");
		}
	}
	
	public void resetSignBookParams(SignBook signBook) {
		signBook.getSignRequestParams().get(0).setSignPageNumber(1);
		signBook.getSignRequestParams().get(0).setXPos(defaultPositionX);
		signBook.getSignRequestParams().get(0).setYPos(defaultPositionY);
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
						SignRequest signRequest = signRequestService.createSignRequest(new SignRequest(), user, documentToAdd, signBook.getSignRequestParams().get(0));
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
			if(signBook.getSignBookType().equals(SignBookType.workflow)) {
				importSignRequestByRecipients(signRequest, signBook.getSignBooks().get(signRequest.getSignBooksWorkflowStep() - 1).getRecipientEmails(), user);
				signRequestService.updateStatus(signRequest, SignRequestStatus.draft, messageSource.getMessage("updateinfo_sendtosignbook", null, Locale.FRENCH) + " " + signBook.getSignBooks().get(signRequest.getSignBooksWorkflowStep() - 1).getName(), user, "SUCCESS", "");
				if(signRequest.getSignBooksWorkflowStep() > 1) {
					signRequestService.pendingSignRequest(signRequest, user);
				}
				//signRequest.getOriginalSignBookNames().clear();
			} else {
				importSignRequestByRecipients(signRequest, signBook.getRecipientEmails(), user);
				signRequestService.updateStatus(signRequest, SignRequestStatus.draft, messageSource.getMessage("updateinfo_sendtosignbook", null, Locale.FRENCH) + " " + signBook.getName(), user, "SUCCESS", "");
			}
			//signRequest.getOriginalSignBookNames().add(signBook.getName());
		} else {
			logger.warn(signRequest.getId() + " is already in signbook" + signBook.getName());
		}
	}

	private void importSignRequestByRecipients(SignRequest signRequest,  List<String> recipientEmails, User user) {
		for(String recipientEmail : recipientEmails) {
			List<String> recipientEmailsList = new ArrayList<>();
			recipientEmailsList.add(recipientEmail);
			SignBook signBook = getUserSignBookByRecipientEmail(recipientEmail);
			if(signBook.getRecipientEmails().contains("creator")) {
				signBook = getUserSignBook(user);
			}
			if(!signRequest.getSignBooks().containsKey(signBook.getId())) {
				signRequest.getSignBooks().put(signBook.getId(), false);
			} else {
				//throw new EsupSignatureException(signRequest.getId() + " is already in signbook" + signBook.getName());
				logger.warn(signRequest.getId() + " is already in signbook" + signBook.getName());
			}
		}
	}
	
	public void removeSignRequestFromAllSignBooks(SignRequest signRequest) {
		List<SignBook> signBooks = getSignBookBySignRequest(signRequest);
		for(SignBook signBook : signBooks) {
			List<SignRequest> signRequests = new ArrayList<>();
			signRequests.addAll(signBook.getSignRequests());
			signRequests.remove(signRequest);
			signBook.setSignRequests(signRequests);
			signBook.merge();
		}
		signRequest.getSignBooks().clear();
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


	public SignBook getUserSignBook(User user) {
		return getUserSignBookByRecipientEmail(user.getEmail());
	}
	
	public SignBook getUserSignBookByRecipientEmail(String recipientEmail) {
		if(SignBook.countFindSignBooksByRecipientEmailsAndSignBookTypeEquals(Arrays.asList(recipientEmail), SignBookType.user) > 0) {
			return SignBook.findSignBooksByRecipientEmailsAndSignBookTypeEquals(Arrays.asList(recipientEmail), SignBookType.user).getSingleResult();
		} else {
			return SignBook.findSignBooksByRecipientEmailsAndSignBookTypeEquals(Arrays.asList(recipientEmail), SignBookType.system).getSingleResult();
		}
	}
	
	public List<SignBook> getSignBookBySignRequest(SignRequest signRequest) {
		List<SignBook> signBooks = SignBook.findSignBooksBySignRequestsEquals(Arrays.asList(signRequest)).getResultList();
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
