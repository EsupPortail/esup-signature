package org.esupportail.esupsignature.service;

import eu.europa.esig.dss.*;
import eu.europa.esig.dss.asic.ASiCWithCAdESSignatureParameters;
import eu.europa.esig.dss.asic.ASiCWithXAdESSignatureParameters;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import eu.europa.esig.dss.x509.CertificateToken;
import org.apache.commons.codec.binary.Base64;
import org.esupportail.esupsignature.dss.web.model.AbstractSignatureForm;
import org.esupportail.esupsignature.dss.web.model.SignatureDocumentForm;
import org.esupportail.esupsignature.dss.web.model.SignatureMultipleDocumentsForm;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.SignBook.DocumentIOType;
import org.esupportail.esupsignature.entity.SignBook.SignBookType;
import org.esupportail.esupsignature.entity.SignRequest.SignRequestStatus;
import org.esupportail.esupsignature.entity.SignRequestParams.NewPageType;
import org.esupportail.esupsignature.entity.SignRequestParams.SignType;
import org.esupportail.esupsignature.entity.User.EmailAlertFrequency;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureKeystoreException;
import org.esupportail.esupsignature.exception.EsupSignatureSignException;
import org.esupportail.esupsignature.repository.*;
import org.esupportail.esupsignature.service.fs.FsAccessFactory;
import org.esupportail.esupsignature.service.fs.FsAccessService;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class SignRequestService {

	private static final Logger logger = LoggerFactory.getLogger(SignRequestService.class);

	@Autowired
	private SignRequestRepository signRequestRepository;

	@Autowired
	private LogRepository logRepository;
	
	@Resource
	private UserKeystoreService userKeystoreService;

	@Resource
	private PdfService pdfService;

	@Resource
	private DocumentService documentService;

	@Autowired
	private SignRequestParamsRepository signRequestParamsRepository; 
	
	@Autowired
	private SignBookRepository signBookRepository;
	
	@Resource
	private FsAccessFactory fsAccessFactory;
	
	@Resource
	private SignBookService signBookService;
	
	@Autowired
	private SigningService signingService;

	@Resource
	private FileService fileService;

	@Resource
	private BigFileService bigFileService;
	
	@Autowired
	private UserRepository userRepository;
	
	@Resource
	private UserService userService;
	
	@Value("${sign.defaultSignatureForm}")
	private SignatureForm defaultSignatureForm;
	@Value("${sign.firstPosX}")
	private int firstPosX;
	@Value("${sign.firstPosY}")
	private int firstPosY;	
	
	private String step = "";
	
	public List<SignRequest> getAllSignRequests() {
		List<SignRequest> list = new ArrayList<SignRequest>();
		signRequestRepository.findAll().forEach(e -> list.add(e));
		return list;
	}

	public List<SignRequest> findSignRequestByUserAndStatusEquals(User user, SignRequestStatus status) {
		return findSignRequestByUserAndStatusEquals(user, true, status);
	}
	
	public List<SignRequest> findSignRequestByUserAndStatusEquals(User user, Boolean toSign, SignRequestStatus status) {
		List<SignBook> signBooks = signBookRepository.findByRecipientEmails(Arrays.asList(user.getEmail()));
		List<SignRequest> signRequests = new ArrayList<>();
		for(SignBook signBook : signBooks) {
			for(SignRequest signRequest : getAllSignRequests()) {
				if(signRequest.getSignBooks().containsKey(signBook.getId()) && (status == null || signRequest.getStatus().equals(status))) {
					signRequests.add(signRequest);							
				}
			}
		}
		List<Log> logs;
		if(toSign) {
			logs = logRepository.findByEppnAndAction(user.getEppn(), "sign");
		} else {
			logs = logRepository.findByEppn(user.getEppn());
		}
		for(Log log : logs) {
			SignRequest signRequest = signRequestRepository.findById(log.getSignRequestId()).get();
			if(signRequest != null && !signRequests.contains(signRequest) && (status == null || signRequest.getStatus().equals(status))) {
				signRequests.add(signRequest);
			}
		}
		return signRequests;
	}
	
	public SignRequest createSignRequest(SignRequest signRequest, User user, SignRequestParams signRequestParams) {
			return createSignRequest(signRequest, user, new ArrayList<>(), signRequestParams);
	}
	
	public SignRequest createSignRequest(SignRequest signRequest, User user, Document document, SignRequestParams signRequestParams) {
		List<Document> documents = new ArrayList<Document>();
		documents.add(document);
		return createSignRequest(signRequest, user, documents, signRequestParams);
	}
	
	public SignRequest createSignRequest(SignRequest signRequest, User user, List<Document> documents, SignRequestParams signRequestParams) {
		signRequest.setName(String.valueOf(generateUniqueId()));
		signRequest.setCreateBy(user.getEppn());
		signRequest.setCreateDate(new Date());
		signRequest.setStatus(SignRequestStatus.draft);
		signRequest.getSignRequestParamsList().add(signRequestParams);
		signRequest.setOriginalDocuments(documents);
		signRequestRepository.save(signRequest);
		for(Document document : documents) {
			document.setParentId(signRequest.getId());
		}
		return signRequest;
	}
	
	public void addOriginalDocuments(SignRequest signRequest, List<Document> documents) {
		for(Document document : documents) {
			signRequest.getOriginalDocuments().add(document);
			document.setParentId(signRequest.getId());
		}
	}

	public void sign(SignRequest signRequest, User user, String password, boolean addDate) throws EsupSignatureIOException, EsupSignatureSignException, EsupSignatureKeystoreException, IOException {
		step = "Demarrage de la signature";
		SignBook currentSignBook = signBookService.getSignBookBySignRequestAndUser(signRequest, user);
		if(!signRequest.isOverloadSignBookParams()) {
			signRequest.getSignRequestParams().setSignType(currentSignBook.getSignRequestParams().get(0).getSignType());
		}
		boolean addPage = false;
		if(!SignRequestParams.NewPageType.none.equals(signRequest.getSignRequestParams().getNewPageType())) {
			int nbSignOk = signRequest.countSignOk();
			//TODO or get next signature field
			signRequest.getSignRequestParams().setXPos(firstPosX + ((nbSignOk - (Math.abs(nbSignOk / 3) * 3)) * 150));
			signRequest.getSignRequestParams().setYPos(firstPosY +(Math.abs(nbSignOk / 3) * 100));
			if(nbSignOk == 0) {
				addPage = true;
			}
		}
		File signedFile = null;
		List<Document> toSignDocuments = getToSignDocuments(signRequest);
		
		SignType signType = signRequest.getSignRequestParams().getSignType();		
		if (signType.equals(SignRequestParams.SignType.pdfImageStamp) || signType.equals(SignType.visa)) {
			File toSignFile = toSignDocuments.get(0).getJavaIoFile();
			if (toSignDocuments.size() == 1 && fileService.getContentType(toSignDocuments.get(0).getJavaIoFile()).equals("application/pdf")) {
				signedFile = pdfService.stampImage(toSignFile, signRequest.getSignRequestParams(), user, addPage, addDate);
			} else {
				signedFile = toSignFile;
			}
		} else {
			if (toSignDocuments.size() == 1 && fileService.getContentType(toSignDocuments.get(0).getJavaIoFile()).equals("application/pdf")) {
				signedFile = certSign(signRequest, user, password, SignatureForm.PAdES);
			} else {
				signedFile = certSign(signRequest, user, password, defaultSignatureForm);
			}
		}
		
		if (signedFile != null) {
			addSignedFile(signRequest, signedFile, user);
			try {
				applySignBookRules(signRequest, user);
			} catch (EsupSignatureException e) {
				throw new EsupSignatureSignException("error on apply signBook rules", e);
			}
			step = "end";
		} else {
			throw new EsupSignatureSignException("enable to sign document");
		}
	}

	public void nexuSign(SignRequest signRequest, User user, AbstractSignatureForm signatureDocumentForm, AbstractSignatureParameters parameters) throws EsupSignatureKeystoreException, EsupSignatureIOException, EsupSignatureSignException {
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
				try {
				applySignBookRules(signRequest, user);
				} catch (EsupSignatureException e) {
					throw new EsupSignatureSignException("error on apply signBook rules", e);
				}
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
				boolean addPage = false;
				if(signRequest.countSignOk() == 0) {
					addPage = true;
				}
				
				File toSignFile = toSignFiles.get(0);
				pdfService.formatPdf(toSignFile, signRequest.getSignRequestParams(), addPage, user);
				if(signRequest.getNbSign() == 0) {
					toSignFile = pdfService.convertGS(pdfService.writeMetadatas(toSignFile, user));
				}
				
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
		} catch (EsupSignatureException e) {
			step = "sign_system_error";
			throw new EsupSignatureKeystoreException(e.getMessage(), e);
		} catch (Exception e) {
			step = "sign_system_error";
			throw new EsupSignatureKeystoreException(e.getMessage(), e);
		}
		return null;
	}
	
	public void addSignedFile(SignRequest signRequest, File signedFile, User user) throws EsupSignatureIOException {
		try {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
			Document document = documentService.createDocument(signedFile, signRequest.getTitle() + "_" + signRequest.getSignRequestParams().getSignType() + "_" + signRequest.getCreateBy() + "_" + simpleDateFormat.format(new Date()) + "." + fileService.getExtension(signedFile), fileService.getContentType(signedFile));
			signRequest.getSignedDocuments().add(document);
			document.setParentId(signRequest.getId());
		} catch (IOException e) {
			throw new EsupSignatureIOException("error on save signed file", e);
		}
	}

	public void applySignBookRules(SignRequest signRequest, User user) throws EsupSignatureException {
		SignBook recipientSignBook = signBookService.getSignBookBySignRequestAndUser(signRequest, user);
		List<SignBook> signBooks = signBookService.getSignBookBySignRequest(signRequest);
		SignBook signBook = signBooks.get(0);
		SignRequestParams.SignType signType = signRequest.getSignRequestParams().getSignType();
		signRequest.getSignBooks().put(recipientSignBook.getId(), true);
		if (isSignRequestCompleted(signRequest)) {
			if(recipientSignBook.getSignBookType().equals(SignBookType.user)) {
				signBookService.resetSignBookParams(recipientSignBook);
			}
			if(signType.equals(SignType.visa)) {
				updateStatus(signRequest, SignRequestStatus.checked, "Visa" , user, "SUCCESS", signRequest.getComment());
			} else {
				updateStatus(signRequest, SignRequestStatus.signed, "Signature", user, "SUCCESS", signRequest.getComment());
			}
			if(signBook.isAutoRemove()) {
				if(signBook.getSignBookType().equals(SignBookType.workflow) && signRequest.getSignBooksWorkflowStep() < signBook.getSignBooks().size()) {
					signRequest.setSignBooksWorkflowStep(signRequest.getSignBooksWorkflowStep() + 1);
					signBookService.removeSignRequestFromSignBook(signRequest, signBook, user);
					signBookService.importSignRequestInSignBook(signRequest, signBook, user);
				} else {
					completeSignRequest(signRequest, signBook, user);
				}
			}
		} else {
			if(signType.equals(SignType.visa)) {
				updateStatus(signRequest, SignRequestStatus.pending, "Visa", user, "SUCCESS", signRequest.getComment());
			} else {
				updateStatus(signRequest, SignRequestStatus.pending, "Signature", user, "SUCCESS", signRequest.getComment());
			}
		}
		signRequest.setNbSign(signRequest.getNbSign() + 1);
	}
	
	public void pendingSignRequest(SignRequest signRequest, User user) {
		updateStatus(signRequest, SignRequestStatus.pending, "Envoyé pour signature", user, "SUCCESS", signRequest.getComment());
		for(Long signBookId : signRequest.getSignBooks().keySet()) {
			SignBook signBook = signBookRepository.findById(signBookId).get();
			for(String emailRecipient : signBook.getRecipientEmails()) {
				User recipient = userRepository.findByEmail(emailRecipient).get(0);
				//TODO : add force email alert

				if(signRequest.getNbSign() == 0 && signRequest.getOriginalDocuments().size() == 1 && signRequest.getOriginalDocuments().get(0).getContentType().contains("pdf")) {
					int numSign = 0;
					File toSignFile = signRequest.getOriginalDocuments().get(0).getJavaIoFile();
					while(true) {
						int[] pos = pdfService.getSignFieldCoord(toSignFile, numSign);
						if(pos == null) {
							break;
						}
						SignRequestParams signRequestParams;
						if(signRequest.getSignRequestParamsList().size() < numSign + 1) {
							signRequestParams = getEmptySignRequestParams();
							signRequest.getSignRequestParamsList().add(signRequestParams);
						} else {
							signRequestParams = signRequest.getSignRequestParamsList().get(numSign); 
						}
						signRequestParams.setXPos(pos[0]);
						signRequestParams.setYPos(pos[1]);
						signRequestParams.setPdSignatureFieldName(pdfService.getPDSignatureFieldName(toSignFile, numSign).getPartialName());
						signRequestParamsRepository.save(signRequestParams);
						numSign++;
					}
				}
				if(user.getEmailAlertFrequency() == null|| user.getEmailAlertFrequency().equals(EmailAlertFrequency.immediately) || userService.checkEmailAlert(user)) {
					userService.sendEmailAlert(recipient);
				}
			}
		}
	}
	
	public void completeSignRequest(SignRequest signRequest, SignBook signBook, User user) {
		updateStatus(signRequest, SignRequestStatus.completed, "Terminé automatiquement", user, "SUCCESS", signRequest.getComment());
		signBookService.removeSignRequestFromAllSignBooks(signRequest);
		if(signBook.isExternal()) {
			signBookService.deleteSignBook(signBook);
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
	
	public File getLastSignedFile(SignRequest signRequest) throws Exception {
		if(signRequest.getStatus().equals(SignRequestStatus.exported)) {
			FsAccessService fsAccessService = null;
			if(signRequest.getExportedDocumentURI().startsWith("smb")) {
				fsAccessService = fsAccessFactory.getFsAccessService(DocumentIOType.smb);
			}
			return fsAccessService.getFileFromURI(signRequest.getExportedDocumentURI()).getFile();
		} else {
			return getLastSignedDocument(signRequest).getJavaIoFile();
		}
	}
	
	public Document getLastSignedDocument(SignRequest signRequest) {
		List<Document> documents = signRequest.getSignedDocuments();
		if(documents.size() > 0) {
			return documents.get(documents.size() - 1);
		} else {
			return getLastOriginalDocument(signRequest);
		}
	}

	public Document getLastOriginalDocument(SignRequest signRequest) {
		List<Document> documents = signRequest.getOriginalDocuments();
		if (documents.size() > 1) {
			return null;
		} else {
			return documents.get(0);
		}
	}

	public void clearAllDocuments(SignRequest signRequest) {
		logger.info("clear all documents from " + signRequest.getName());
		List<Document> originalDocuments = new ArrayList<Document>();
		originalDocuments.addAll(signRequest.getOriginalDocuments());
		signRequest.getOriginalDocuments().clear();
		for(Document document : originalDocuments) {
			documentService.deleteDocument(document);
		}
		List<Document> signedDocuments = new ArrayList<Document>();
		signedDocuments.addAll(signRequest.getSignedDocuments());
		signRequest.getSignedDocuments().clear();
		for(Document document : signedDocuments) {
			documentService.deleteDocument(document);
		}
		signRequestRepository.save(signRequest);
	}
	
	public void updateStatus(SignRequest signRequest, SignRequestStatus signRequestStatus, String action, User user, String returnCode, String comment) {
		Log log = new Log();
		log.setSignRequestId(signRequest.getId());
		log.setEppn(user.getEppn());
		log.setIp(user.getIp());
		log.setInitialStatus(signRequest.getStatus().toString());
		log.setLogDate(new Date());
		log.setAction(action);
		log.setReturnCode(returnCode);
		log.setComment(comment);
		if(signRequestStatus != null) {
			log.setFinalStatus(signRequestStatus.toString());		
			signRequest.setStatus(signRequestStatus);
		} else {
			log.setFinalStatus(signRequest.getStatus().toString());
		}
		logRepository.save(log);
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
		updateStatus(signRequest, SignRequestStatus.refused, "Refusé", user, "SUCCESS", signRequest.getComment());
	}
	
	public void toggleNeedAllSign(SignRequest signRequest) {
		if(signRequest.isAllSignToComplete()) {
			signRequest.setAllSignToComplete(false);
		} else {
			signRequest.setAllSignToComplete(true);
		}
		signRequestRepository.save(signRequest);
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
		if(signRequest != null) {
			List<Log> log = logRepository.findByEppnAndSignRequestId(user.getEppn(), signRequest.getId());
			SignBook signBook = signBookService.getSignBookBySignRequestAndUser(signRequest, user);
			if (signRequest.getCreateBy().equals(user.getEppn()) || log.size() > 0 || signBook != null) {
				return true;
			}
		}
		return false;
	}
	
	public SignRequestParams getEmptySignRequestParams() {
		SignRequestParams signRequestParams = new SignRequestParams();
		signRequestParams.setSignPageNumber(1);
		signRequestParams.setNewPageType(NewPageType.none);
		signRequestParams.setSignType(SignType.visa);
		signRequestParamsRepository.save(signRequestParams);
		return signRequestParams;
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
	
	public void setSignBooksLabels(SignRequest signRequest) {
		Map<String, Boolean> signBookNames = new HashMap<>();
		for(Map.Entry<Long, Boolean> signBookMap : signRequest.getSignBooks().entrySet()) {
			signBookNames.put(signBookRepository.findById(signBookMap.getKey()).get().getName(), signBookMap.getValue());
		}
		signRequest.setSignBooksLabels(signBookNames);
	}
	
	public String getStep() {
		return step;
	}
	
}