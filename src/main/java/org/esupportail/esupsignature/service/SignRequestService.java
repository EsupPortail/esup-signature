package org.esupportail.esupsignature.service;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.codec.binary.Base64;
import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.domain.Log;
import org.esupportail.esupsignature.domain.SignBook;
import org.esupportail.esupsignature.domain.SignBook.DocumentIOType;
import org.esupportail.esupsignature.domain.SignBook.SignBookType;
import org.esupportail.esupsignature.domain.SignRequest;
import org.esupportail.esupsignature.domain.SignRequest.SignRequestStatus;
import org.esupportail.esupsignature.domain.SignRequestParams;
import org.esupportail.esupsignature.domain.SignRequestParams.NewPageType;
import org.esupportail.esupsignature.domain.SignRequestParams.SignType;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.dss.web.model.AbstractSignatureForm;
import org.esupportail.esupsignature.dss.web.model.SignatureDocumentForm;
import org.esupportail.esupsignature.dss.web.model.SignatureMultipleDocumentsForm;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureKeystoreException;
import org.esupportail.esupsignature.exception.EsupSignatureNexuException;
import org.esupportail.esupsignature.exception.EsupSignatureSignException;
import org.esupportail.esupsignature.service.fs.cifs.CifsAccessImpl;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import eu.europa.esig.dss.ASiCContainerType;
import eu.europa.esig.dss.AbstractSignatureParameters;
import eu.europa.esig.dss.DSSDocument;
import eu.europa.esig.dss.DSSUtils;
import eu.europa.esig.dss.EncryptionAlgorithm;
import eu.europa.esig.dss.InMemoryDocument;
import eu.europa.esig.dss.SignatureForm;
import eu.europa.esig.dss.asic.ASiCWithCAdESSignatureParameters;
import eu.europa.esig.dss.asic.ASiCWithXAdESSignatureParameters;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import eu.europa.esig.dss.x509.CertificateToken;

@Service
public class SignRequestService {

	private static final Logger logger = LoggerFactory.getLogger(SignRequestService.class);

	@Resource
	private UserKeystoreService userKeystoreService;

	@Resource
	private PdfService pdfService;

	@Resource
	private CifsAccessImpl cifsAccessImpl;

	@Resource
	private DocumentService documentService;

	@Resource
	private SignBookService signBookService;

	@Autowired
	private SigningService signingService;

	@Resource
	private FileService fileService;

	
	@Value("${sign.defaultSignatureForm}")
	private SignatureForm defaultSignatureForm;
	@Value("${sign.pades.xFirstPos}")
	private int xFirstPos;
	@Value("${sign.pades.yFirstPos}")
	private int yFirstPos;	
	
	private String step = "";
	
	public List<SignRequest> findSignRequestByUserAndStatusEquals(User user, SignRequestStatus status) {
		return findSignRequestByUserAndStatusEquals(user, true, status, null, null);
	}
	
	public List<SignRequest> findSignRequestByUserAndStatusEquals(User user, Boolean toSign, SignRequestStatus status, Integer page, Integer size) {
		List<String> recipientEmails = new ArrayList<>();
		recipientEmails.add(user.getEmail());
		List<SignBook> signBooks = SignBook.findSignBooksByRecipientEmailsEquals(recipientEmails).getResultList();
		List<SignRequest> signRequests = new ArrayList<>();
		for(SignBook signBook : signBooks) {
			for(SignRequest signRequest : signBook.getSignRequests()) {
				if(status == null || signRequest.getStatus().equals(status)) {
					signRequests.add(signRequest);							
				}
			}
		}
		List<Log> logs;
		if(toSign) {
			logs = Log.findLogsByEppnAndActionEquals(user.getEppn(), "sign").getResultList();
		} else {
			logs = Log.findLogsByEppnEquals(user.getEppn()).getResultList();
		}
		for(Log log : logs) {
			SignRequest signRequest = SignRequest.findSignRequest(log.getSignRequestId());
			if(signRequest != null && !signRequests.contains(signRequest) && (status == null || signRequest.getStatus().equals(status))) {
				signRequests.add(signRequest);
			}
		}
		signRequests = signRequests.stream().sorted(Comparator.comparing(SignRequest::getCreateDate).reversed()).collect(Collectors.toList());
		
		if(page != null) {
			return signRequests.stream().skip((page - 1) * size).limit(size).collect(Collectors.toList());
		} else {
			return signRequests;
		}
	}
	
	public SignRequest createSignRequest(SignRequest signRequest, User user, SignRequestParams signRequestParams, List<String> recipientEmails) {
			return createSignRequest(signRequest, user, new ArrayList<>(), signRequestParams, recipientEmails );
	}
	
	public SignRequest createSignRequest(SignRequest signRequest, User user, Document document, SignRequestParams signRequestParams, List<String> recipientEmails) {
		List<Document> documents = new ArrayList<Document>();
		documents.add(document);
		return createSignRequest(signRequest, user, documents, signRequestParams, recipientEmails );
	}
	
	public SignRequest createSignRequest(SignRequest signRequest, User user, List<Document> documents, SignRequestParams signRequestParams, List<String> recipientEmails) {
		signRequest.setName(String.valueOf(generateUniqueId()));
		signRequest.setCreateBy(user.getEppn());
		signRequest.setCreateDate(new Date());
		signRequest.setStatus(SignRequestStatus.draft);
		signRequest.setSignRequestParams(signRequestParams);
		for(String recipientEmail : recipientEmails) {
			List<String> recipientEmailsList = new ArrayList<>();
			recipientEmailsList.add(recipientEmail);
			SignBook signBook = SignBook.findSignBooksByRecipientEmailsAndSignBookTypeEquals(recipientEmailsList, SignBookType.user).getSingleResult();
			if(signBook.getSignBookType().equals(SignBookType.group)) {
				List<String> recipientsEmailsFromGroup = signBook.getRecipientEmails();
				for(String recipientEmailFromGroup : recipientsEmailsFromGroup) {
					List<String> recipientEmailFromGroupList = new ArrayList<>();
					recipientEmailFromGroupList.add(recipientEmailFromGroup);
					SignBook signBookFromGroup = SignBook.findSignBooksByRecipientEmailsAndSignBookTypeEquals(recipientEmailFromGroupList, SignBookType.user).getSingleResult();
					signRequest.getSignBooks().put(signBookFromGroup.getId(), false);
					signBookFromGroup.getSignRequests().add(signRequest);

				}
			} else {
				signRequest.getSignBooks().put(signBook.getId(), false);
				signBook.getSignRequests().add(signRequest);
			}
		}
		signRequest.persist();
		return signRequest;
	}
	
	public void addOriginalDocuments(SignRequest signRequest, List<Document> documents) {
		for(Document document : documents) {
			signRequest.getOriginalDocuments().add(document);
			document.setSignRequestId(signRequest.getId());
		}
	}
	
	public void addOriginalDocuments(SignRequest signRequest, Document document) {
		signRequest.getOriginalDocuments().add(document);
		document.setSignRequestId(signRequest.getId());
	}
	
	public void sign(SignRequest signRequest, User user, String password) throws EsupSignatureIOException, EsupSignatureSignException, EsupSignatureNexuException, EsupSignatureKeystoreException, IOException {
		step = "Demarrage de la signature";
		SignBook currentSignBook = signBookService.getSignBookBySignRequestAndUser(signRequest, user);
		if(!signRequest.isOverloadSignBookParams()) {
			signRequest.getSignRequestParams().setSignType(currentSignBook.getSignRequestParams().getSignType());
		}
		if(signRequest.getSignRequestParams().getSignType().equals(SignType.nexuSign)) {
			throw new EsupSignatureNexuException("redirect to nexuSign");
		}
		if(signRequest.countSignOk() == 0) {
			if(!SignRequestParams.NewPageType.none.equals(signRequest.getSignRequestParams().getNewPageType())) {
				signRequest.getSignRequestParams().setXPos(xFirstPos);
				signRequest.getSignRequestParams().setYPos(yFirstPos);
			}
		} else {
			signRequest.getSignRequestParams().setNewPageType(NewPageType.none);
		}
		File signedFile = null;
		List<Document> toSignDocuments = getToSignDocuments(signRequest);
		
		SignType signType = signRequest.getSignRequestParams().getSignType();		
		if (signType.equals(SignRequestParams.SignType.pdfImageStamp) || signType.equals(SignType.visa)) {
			File toSignFile = toSignDocuments.get(0).getJavaIoFile();
			signedFile = pdfService.stampImage(toSignFile, signRequest.getSignRequestParams(), user);
		} else {
			if (toSignDocuments.size() == 1 && fileService.getContentType(toSignDocuments.get(0).getJavaIoFile()).equals("application/pdf")) {
				signedFile = certSign(signRequest, user, password, SignatureForm.PAdES);
			} else {
				signedFile = certSign(signRequest, user, password, defaultSignatureForm);
			}
		}
		
		if (signedFile != null) {
			addSignedFile(signRequest, signedFile, user);
			applySignBookRules(signRequest, user);
			step = "end";
		} else {
			throw new EsupSignatureSignException("enable to sign document");
		}
	}

	public void nexuSign(SignRequest signRequest, User user, AbstractSignatureForm signatureDocumentForm, AbstractSignatureParameters parameters) throws EsupSignatureKeystoreException, EsupSignatureIOException {
		logger.info(user.getEppn() + " launch nexu signature for signRequest : " + signRequest.getId());
		DSSDocument dssDocument;
		
		if(signatureDocumentForm.getClass().equals(SignatureMultipleDocumentsForm.class)) {
			dssDocument = signingService.signDocument((SignatureMultipleDocumentsForm) signatureDocumentForm);
		} else {
			dssDocument = signingService.nexuSignDocument((SignatureDocumentForm) signatureDocumentForm, parameters);
		}
		
		InMemoryDocument signedDocument = new InMemoryDocument(DSSUtils.toByteArray(dssDocument), dssDocument.getName(), dssDocument.getMimeType());

		try {
			File signedFile = fileService.inputStreamToFile(signedDocument.openStream(), signedDocument.getName());
			if (signedFile != null) {
				addSignedFile(signRequest, signedFile, user);
				applySignBookRules(signRequest, user);
			}
		} catch (IOException e) {
			logger.error("error to read signed file", e);
		}
	}

	public File certSign(SignRequest signRequest, User user, String password, SignatureForm signatureForm) throws EsupSignatureKeystoreException, IOException {
		List<File> toSignFiles = new ArrayList<>();
		for(Document document : getToSignDocuments(signRequest)) {
			toSignFiles.add(document.getJavaIoFile());
		}
		step = "Préparation de la signature";
		try {
			AbstractSignatureForm signatureDocumentForm = signingService.getSignatureDocumentForm(toSignFiles, signatureForm);
			signatureDocumentForm.setEncryptionAlgorithm(EncryptionAlgorithm.RSA);
			
			File keyStoreFile = user.getKeystore().getJavaIoFile();
			SignatureTokenConnection signatureTokenConnection = userKeystoreService.getSignatureTokenConnection(keyStoreFile, password);
			CertificateToken certificateToken = userKeystoreService.getCertificateToken(keyStoreFile, password);
			CertificateToken[] certificateTokenChain = userKeystoreService.getCertificateTokenChain(keyStoreFile, password);
	
			signatureDocumentForm.setBase64Certificate(Base64.encodeBase64String(certificateToken.getEncoded()));
			List<String> base64CertificateChain = new ArrayList<>();
			for (CertificateToken token : certificateTokenChain) {
				base64CertificateChain.add(Base64.encodeBase64String(token.getEncoded()));
			}
			signatureDocumentForm.setBase64CertificateChain(base64CertificateChain);
			
			AbstractSignatureParameters parameters = null;
			if(signatureForm.equals(SignatureForm.CAdES)) {
				ASiCWithCAdESSignatureParameters aSiCWithCAdESSignatureParameters = new ASiCWithCAdESSignatureParameters();
				aSiCWithCAdESSignatureParameters.aSiC().setContainerType(ASiCContainerType.ASiC_E);
				parameters = aSiCWithCAdESSignatureParameters;
			} else if(signatureForm.equals(SignatureForm.XAdES)) {
				ASiCWithXAdESSignatureParameters aSiCWithXAdESSignatureParameters = new ASiCWithXAdESSignatureParameters();
				aSiCWithXAdESSignatureParameters.aSiC().setContainerType(ASiCContainerType.ASiC_E);
				parameters = aSiCWithXAdESSignatureParameters;
			} else if(signatureForm.equals(SignatureForm.PAdES)) {
				step = "Formatage du PDF";
				File toSignFile = pdfService.formatPdf(toSignFiles.get(0), signRequest.getSignRequestParams());
				parameters = signingService.fillVisibleParameters((SignatureDocumentForm) signatureDocumentForm, signRequest.getSignRequestParams(), fileService.toMultipartFile(toSignFile, "pdf"), user);
				SignatureDocumentForm documentForm = (SignatureDocumentForm) signatureDocumentForm;
				documentForm.setDocumentToSign(fileService.toMultipartFile(toSignFile, "application/pdf"));
				signatureDocumentForm = documentForm;
			}
			step = "Signature du/des documents(s)";
			
			parameters.setSigningCertificate(certificateToken);
			parameters.setCertificateChain(certificateTokenChain);
			parameters.setSignatureLevel(signatureDocumentForm.getSignatureLevel());
			DSSDocument dssDocument;
			if(toSignFiles.size() > 1) {
				dssDocument = signingService.certSignDocument((SignatureMultipleDocumentsForm) signatureDocumentForm, parameters, signatureTokenConnection);
			} else {
				dssDocument = signingService.certSignDocument((SignatureDocumentForm) signatureDocumentForm, parameters, signatureTokenConnection);
			}
			InMemoryDocument signedPdfDocument = new InMemoryDocument(DSSUtils.toByteArray(dssDocument), dssDocument.getName(), dssDocument.getMimeType());
	
			try {
				step = "Enregistrement du/des documents(s)";
				return fileService.inputStreamToFile(signedPdfDocument.openStream(), signedPdfDocument.getName());
			} catch (IOException e) {
				logger.error("error to read signed file", e);
		}
		} catch (EsupSignatureKeystoreException e) {
			step = "security_bad_password";
			throw new EsupSignatureKeystoreException(e.getMessage(), e);
		}
		return null;
	}
	
	public void addSignedFile(SignRequest signRequest, File signedFile, User user) throws EsupSignatureIOException {
		try {
			Document document = documentService.createDocument(signedFile, "signed_" + signRequest.getSignRequestParams().getSignType().toString() + "_" + user.getEppn() + "_" + signedFile.getName(), fileService.getContentType(signedFile));
			signRequest.getSignedDocuments().add(document);
			document.setSignRequestId(signRequest.getId());
		} catch (IOException e) {
			throw new EsupSignatureIOException("error on save signed file", e);
		}
	}

	public void applySignBookRules(SignRequest signRequest, User user) {
		SignBook signBook = signBookService.getSignBookBySignRequestAndUser(signRequest, user);
		SignRequestParams.SignType signType = signRequest.getSignRequestParams().getSignType();
		signRequest.getSignBooks().put(signBook.getId(), true);
		if (isSignRequestCompleted(signRequest)) {
			if(signBook.getSignBookType().equals(SignBookType.user)) {
				signBookService.resetSignBookParams(signBook);
			}
			if(signType.equals(SignType.visa)) {
				updateInfo(signRequest, SignRequestStatus.checked, "visa", user, "SUCCESS", signRequest.getComment());
			} else {
				updateInfo(signRequest, SignRequestStatus.signed, "sign", user, "SUCCESS", signRequest.getComment());
			}
			if (!signBook.getTargetType().equals(DocumentIOType.none)) {
				try {
					signBookService.exportFileToTarget(signBook, signRequest, user);
					updateInfo(signRequest, SignRequestStatus.exported, "export to target " + signBook.getTargetType() + " : " + signBook.getDocumentsTargetUri(), user, "SUCCESS", signRequest.getComment());
				} catch (EsupSignatureException e) {
					logger.error("error on export file to fs", e);
				}
			}
			if(signBook.isAutoRemove()) {
				signBookService.removeSignRequestFromSignBook(signRequest, signBook, user);
				updateInfo(signRequest, SignRequestStatus.completed, "auto remove", user, "SUCCESS", signRequest.getComment());
			}
		} else {
			updateInfo(signRequest, SignRequestStatus.pending, "sign", user, "SUCCESS", signRequest.getComment());
		}
	}
	
	public List<Document> getToSignDocuments(SignRequest signRequest) {
		List<Document> documents = new ArrayList<>();
		if(signRequest.getSignedDocuments() != null && signRequest.getSignedDocuments().size() > 0 ) {
			documents.add(signRequest.getSignedDocuments().get(signRequest.getSignedDocuments().size() - 1));
		} else {
			documents.addAll(signRequest.getOriginalDocuments());
		}
		return documents;
	}
	
	public Document getLastSignedDocument(SignRequest signRequest) {
		List<Document> documents = signRequest.getSignedDocuments();
		return documents.get(documents.size() - 1);
	}

	public Document getLastOriginalDocument(SignRequest signRequest) {
		List<Document> documents = signRequest.getOriginalDocuments();
		if (documents.size() > 1) {
			return null;
		} else {
			return documents.get(0);
		}
	}

	public void updateInfo(SignRequest signRequest, SignRequestStatus signRequestStatus, String action, User user, String returnCode, String comment) {
		Log log = new Log();
		log.setSignRequestId(signRequest.getId());
		log.setEppn(user.getEppn());
		log.setIp(user.getIp());
		log.setInitialStatus(signRequest.getStatus().toString());
		log.setLogDate(new Date());
		log.setFinalStatus(signRequestStatus.toString());
		log.setAction(action);
		log.setReturnCode(returnCode);
		log.setComment(comment);
		log.persist();
		signRequest.setStatus(signRequestStatus);
		//signRequest.merge();
	}

	public boolean isSignRequestCompleted(SignRequest signRequest) {
		if (signRequest.getSignBooks() != null) {
			for (Map.Entry<Long, Boolean> signBookId : signRequest.getSignBooks().entrySet()) {
				if (!signRequest.getSignBooks().get(signBookId.getKey()) && signRequest.isAllSignToComplete()) {
					return false;
				}
				if (signRequest.getSignBooks().get(signBookId.getKey()) && !signRequest.isAllSignToComplete()) {
					return true;
				}
			}
			if (signRequest.isAllSignToComplete()) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	public void refuse(SignRequest signRequest, User user) {
		signBookService.removeSignRequestFromAllSignBooks(signRequest);
		updateInfo(signRequest, SignRequestStatus.refused, "refuse", user, "SUCCESS", signRequest.getComment());
	}
	
	public void toggleNeedAllSign(SignRequest signRequest) {
		if(signRequest.isAllSignToComplete()) {
			signRequest.setAllSignToComplete(false);
		} else {
			signRequest.setAllSignToComplete(true);
		}
		signRequest.merge();
	}
	
	public boolean checkUserSignRights(User user, SignRequest signRequest) {
		SignBook signBook = signBookService.getSignBookBySignRequestAndUser(signRequest, user);
		if ((signRequest.getStatus().equals(SignRequestStatus.pending) || signRequest.getStatus().equals(SignRequestStatus.draft)) 
				&& signBook != null
				&& !signRequest.getSignBooks().get(signBook.getId())) {
			return true;
		} else {
			return false;
		}
	}

	public boolean checkUserViewRights(User user, SignRequest signRequest) {
		List<Log> log = Log.findLogsByEppnAndSignRequestIdEquals(user.getEppn(), signRequest.getId()).getResultList();
		SignBook signBook = signBookService.getSignBookBySignRequestAndUser(signRequest, user);
		if (signRequest.getCreateBy().equals(user.getEppn()) || log.size() > 0 || signBook != null) {
			return true;
		} else {
			return false;
		}
	}
	
	public SignRequestParams getEmptySignRequestParams() {
		SignRequestParams signRequestParams = new SignRequestParams();
		signRequestParams.setSignPageNumber(1);
		//signRequestParams.setXPos(0);
		//signRequestParams.setYPos(0);
		signRequestParams.setNewPageType(NewPageType.none);
		signRequestParams.setSignType(SignType.visa);
		signRequestParams.persist();
		return signRequestParams;
	}

	public List<SignBook> getSignBooksList(SignRequest signRequest) {
		List<SignBook> signBooks = new ArrayList<>();
		Set<Long> signBookIds = signRequest.getSignBooks().keySet();
		for(long signBookId : signBookIds) {
			signBooks.add(SignBook.findSignBook(signBookId));
		}
		return signBooks;
	}
	
	public long generateUniqueId() {
        long val = -1;
        while (val < 0) {
        	final UUID uid = UUID.randomUUID();
            final ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
            buffer.putLong(uid.getLeastSignificantBits());
            buffer.putLong(uid.getMostSignificantBits());
            final BigInteger bi = new BigInteger(buffer.array());
            val = bi.longValue();
        } 
        return val;
    }
	public String getStep() {
		return step;
	}
	
}

