package org.esupportail.esupsignature.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.domain.SignBook;
import org.esupportail.esupsignature.domain.SignBook.DocumentIOType;
import org.esupportail.esupsignature.domain.SignRequest;
import org.esupportail.esupsignature.domain.SignRequest.SignRequestStatus;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.service.fs.FsAccessService;
import org.esupportail.esupsignature.service.fs.FsFile;
import org.esupportail.esupsignature.service.fs.UploadActionType;
import org.esupportail.esupsignature.service.fs.cifs.CifsAccessImpl;
import org.esupportail.esupsignature.service.fs.opencmis.CmisAccessImpl;
import org.esupportail.esupsignature.service.fs.vfs.VfsAccessImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	public void importFilesFromSource(SignBook signBook, User user) {
		if (signBook.getSourceType() != null && !signBook.getSourceType().equals(DocumentIOType.none)) {
			logger.info("retrieve from " + signBook.getSourceType() + " in " + signBook.getDocumentsSourceUri());
			FsAccessService fsAccessService = getFsAccessService(signBook.getSourceType());
			try {
				List<FsFile> fsFiles = fsAccessService.listFiles(signBook.getDocumentsSourceUri());
				if (fsFiles.size() > 0) {
					for (FsFile fsFile : fsFiles) {
						logger.info("adding file : " + fsFile.getFile().getName());
						fsFile.setPath(signBook.getDocumentsSourceUri());
						Document documentToAdd = documentService.addFile(fsFile.getFile(), fsFile.getName(), fsFile.getContentType());
						if (fsFile.getCreateBy() != null && User.countFindUsersByEppnEquals(fsFile.getCreateBy()) > 0) {
							user = User.findUsersByEppnEquals(fsFile.getCreateBy()).getSingleResult();
							user.setIp("127.0.0.1");
						}
						SignRequest signRequest = signRequestService.createSignRequest(new SignRequest(), user, documentToAdd, signBook.getSignRequestParams(), signBook.getRecipientEmail(), signBook.getId());
						signRequest.merge();
						signBook.getSignRequests().add(signRequest);
						signBook.merge();
						fsAccessService.remove(fsFile);

					}
				} else {
					logger.info("no file to import in this folder : " + signBook.getDocumentsSourceUri());
				}
			} catch (Exception e) {
				logger.error("read fsaccess error : ", e);
			}
		} else {
			logger.debug("no source type for this signbook");
		}
	}

	public void exportFilesToTarget(SignBook signBook, User user) throws EsupSignatureException {
		for (SignRequest signRequest : signBook.getSignRequests()) {
			if (signRequest.getStatus().equals(SignRequestStatus.signed) && signRequestService.isSignRequestCompleted(signRequest)) {
				exportFileToTarget(signBook, signRequestService.getLastDocument(signRequest).getJavaIoFile());
				signRequestService.updateInfo(signRequest, SignRequestStatus.exported, "export to target " + signBook.getTargetType() + " : " + signBook.getDocumentsTargetUri(), user, "SUCCESS");
				removeSignRequestFromSignBook(signRequest, signBook, user);
			}
		}
	}

	public void exportFileToTarget(SignBook signBook, File signedFile) throws EsupSignatureException {
		if (signBook.getTargetType() != null && !signBook.getTargetType().equals(DocumentIOType.none)) {
			logger.info("send to " + signBook.getTargetType() + " in " + signBook.getDocumentsTargetUri());
			FsAccessService fsAccessService = getFsAccessService(signBook.getSourceType());
			try {
				InputStream inputStream = new FileInputStream(signedFile);
				fsAccessService.putFile(signBook.getDocumentsTargetUri(), signedFile.getName(), inputStream, UploadActionType.OVERRIDE);
			} catch (Exception e) {
				throw new EsupSignatureException("write fsaccess error : ", e);
			}
		} else {
			logger.debug("no target type for this signbook");
		}
	}

	public void importSignRequestInSignBook(SignRequest signRequest, SignBook signBook, User user) throws EsupSignatureException {
		if (!signBook.getSignRequests().contains(signRequest)) {
			User testSignBookUser = User.findUsersByEmailEquals(signBook.getRecipientEmail()).getSingleResult();
			SignBook testSignBook = getSignBookByUser(signRequest, testSignBookUser);
			if(testSignBook == null) {
				signRequest.setSignRequestParams(signBook.getSignRequestParams());
				signRequest.getSignBooks().put(signBook.getId(), false);
				signRequest.merge();
				signBook.getSignRequests().add(signRequest);
				signRequestService.updateInfo(signRequest, SignRequestStatus.pending, "imported in signbook " + signBook.getId(), user, "SUCCESS");
				signBook.merge();
				logger.info("signRequest " + signRequest.getId() + " added to signBook : " + signBook.getId() + " by " + user.getEppn());
			} else {
				throw new EsupSignatureException("signRequest " + signRequest.getName() + " is already in signBook : " + testSignBook.getName() + " owned by " + user.getEppn());
			}
		} else {
			throw new EsupSignatureException(signRequest.getId() + " is already in signbook" + signBook.getId());
		}
	}

	public void removeSignRequestFromSignBook(SignRequest signRequest, SignBook signBook, User user) {
		if (signBook.getSignRequests().contains(signRequest)) {
			signRequestService.updateInfo(signRequest, SignRequestStatus.completed, "remove from signbook" + signBook.getId(), user, "SUCCESS");
			signRequest.getSignBooks().clear();
			signRequest.merge();
			signBook.getSignRequests().remove(signRequest);
			signBook.merge();
		} else {
			logger.error(signRequest.getId() + " not in this signbook : " + signBook.getName() + " " + signBook.getId());
		}
	}

	public SignBook getSignBookByUser(SignRequest signRequest, User user) {
		if (signRequest.getSignBooks() != null && signRequest.getSignBooks().size() > 0) {
			for (Map.Entry<Long, Boolean> signBookId : signRequest.getSignBooks().entrySet()) {
				SignBook signBook = SignBook.findSignBook(signBookId.getKey());
				if (user.getEmail().equals(signBook.getRecipientEmail())) {
					return signBook;
				}
			}
		}
		return null;
	}

}
