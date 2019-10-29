package org.esupportail.esupsignature.service;

import eu.europa.esig.dss.*;
import eu.europa.esig.dss.asic.ASiCWithCAdESSignatureParameters;
import eu.europa.esig.dss.asic.ASiCWithXAdESSignatureParameters;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import eu.europa.esig.dss.x509.CertificateToken;
import org.apache.commons.codec.binary.Base64;
import org.apache.pdfbox.pdmodel.PDDocument;
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
import org.esupportail.esupsignature.service.file.FileService;
import org.esupportail.esupsignature.service.fs.FsAccessFactory;
import org.esupportail.esupsignature.service.fs.FsAccessService;
import org.esupportail.esupsignature.service.fs.FsFile;
import org.esupportail.esupsignature.service.mail.MailService;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.esupportail.esupsignature.service.sign.SignService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SignRequestService {

	private static final Logger logger = LoggerFactory.getLogger(SignRequestService.class);

	@Resource
	private SignRequestRepository signRequestRepository;

	@Resource
	private WorkflowStepRepository workflowStepRepository;

	@Resource
	private LogRepository logRepository;
	
	@Resource
	private UserKeystoreService userKeystoreService;

	@Resource
	private PdfService pdfService;

	@Resource
	private DocumentService documentService;

	@Resource
	private SignRequestParamsRepository signRequestParamsRepository; 
	
	@Resource
	private SignBookRepository signBookRepository;
	
	@Resource
	private FsAccessFactory fsAccessFactory;
	
	@Resource
	private SignBookService signBookService;
	
	@Resource
	private SignService signService;

	@Resource
	private FileService fileService;

	@Resource
	private UserRepository userRepository;

	@Resource
	private UserService userService;

	@Resource
	private MailService mailService;

	private String step = "";
	
	public List<SignRequest> getAllSignRequests() {
		List<SignRequest> list = new ArrayList<SignRequest>();
		signRequestRepository.findAll().forEach(e -> list.add(e));
		return list;
	}

	public List<SignRequest> getTosignRequests(User user) {
		List<SignRequest> signRequestsToSign = new ArrayList<>();
		List<SignBook> signBooksGroup = signBookRepository.findByRecipientEmailsContainAndSignBookType(user.getEmail(), SignBookType.group);
		signBooksGroup.addAll(signBookRepository.findByRecipientEmailsContainAndSignBookType(user.getEmail(), SignBookType.user));
		SignBook signBook = signBookRepository.findByName(user.getFirstname() + " " + user.getName()).get(0);
		for(SignBook signBookGroup : signBooksGroup) {
			for(SignRequest signRequest : signBookGroup.getSignRequests()) {
				if(!signRequestsToSign.contains(signRequest) && signRequest.getStatus().equals(SignRequestStatus.pending)) {
					signRequestsToSign.add(signRequest);
				}
			}

			List<SignBook> signBooksWorkflows = signBookRepository.findBySignBookContain(signBookGroup);
			for(SignBook signBookWorkflow : signBooksWorkflows) {
				for(SignRequest signRequest : signBookWorkflow.getSignRequests()) {
					if(!signRequestsToSign.contains(signRequest) && signRequest.getStatus().equals(SignRequestStatus.pending) && signRequest.getCurrentWorkflowStep().getSignBooks().containsKey(signBook.getId()) && !signRequest.getCurrentWorkflowStep().getSignBooks().get(signBook.getId())) {
						signRequestsToSign.add(signRequest);
					}
				}
			}
		}

		return signRequestsToSign.stream().sorted(Comparator.comparing(SignRequest::getCreateDate).reversed()).collect(Collectors.toList());

	}

	public List<SignRequest> findSignRequestByUserAndStatusEquals(User user, Boolean toSign, SignRequestStatus status) {
		List<SignBook> signBooks = signBookRepository.findByRecipientEmails(Arrays.asList(user.getEmail()));
		List<SignRequest> signRequests = new ArrayList<>();
		for(SignBook signBook : signBooks) {
			for(SignRequest signRequest : getAllSignRequests()) {
				if(signRequest.getCurrentWorkflowStep().getSignBooks().containsKey(signBook.getId()) && (status == null || signRequest.getStatus().equals(status))) {
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
			signRequest.getCurrentWorkflowStep().getSignRequestParams().setSignType(currentSignBook.getSignRequestParams().getSignType());
		}
		boolean addPage = false;
		if(!SignRequestParams.NewPageType.none.equals(signRequest.getCurrentWorkflowStep().getSignRequestParams().getNewPageType())) {
			int nbSignOk = signRequest.countSignOk();
			//TODO or get next signature field
			signRequest.getCurrentWorkflowStep().getSignRequestParams().setXPos(95 + ((nbSignOk - (Math.abs(nbSignOk / 3) * 3)) * 150));
			signRequest.getCurrentWorkflowStep().getSignRequestParams().setYPos(188 +(Math.abs(nbSignOk / 3) * 100));
			if(nbSignOk == 0) {
				addPage = true;
			}
		}

		Document signedFile;

		List<Document> toSignDocuments = getToSignDocuments(signRequest);

		SignType signType = signRequest.getCurrentWorkflowStep().getSignRequestParams().getSignType();
		if (signType.equals(SignRequestParams.SignType.pdfImageStamp) || signType.equals(SignType.visa)) {
			if (toSignDocuments.size() == 1 && toSignDocuments.get(0).getContentType().equals("application/pdf")) {
				signedFile = pdfService.stampImage(toSignDocuments.get(0), signRequest, user, addPage, addDate);
			} else {
				logger.error("no visual sign for non pdf file");
				throw new EsupSignatureSignException("no visual sign for non pdf file");
			}
		} else {
			signedFile = certSign(signRequest, user, password);
		}
		
		if (signedFile != null) {
			//addSignedFile(signRequest, signedFile, signedFile.getName(), Files.probeContentType(signedFile.toPath()) , user);
			signRequest.getSignedDocuments().add(signedFile);
			signedFile.setParentId(signRequest.getId());
			//signedFile.delete();
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
			dssDocument = signService.signDocument((SignatureMultipleDocumentsForm) signatureDocumentForm);
		} else {
			dssDocument = signService.nexuSignDocument((SignatureDocumentForm) signatureDocumentForm, parameters);
		}
		
		InMemoryDocument signedDocument = new InMemoryDocument(DSSUtils.toByteArray(dssDocument), dssDocument.getName(), dssDocument.getMimeType());

		addSignedFile(signRequest, signedDocument.openStream(), signedDocument.getName(), signedDocument.getMimeType().getMimeTypeString(), user);
		try {
			applySignBookRules(signRequest, user);
		} catch (EsupSignatureException | IOException e) {
			throw new EsupSignatureSignException("error on apply signBook rules", e);
		}
	}

	public Document certSign(SignRequest signRequest, User user, String password) throws EsupSignatureKeystoreException, IOException {
		SignatureForm signatureForm;
		List<Document> toSignFiles = new ArrayList<>();
		for(Document document : getToSignDocuments(signRequest)) {
			toSignFiles.add(document);
		}
		step = "Préparation de la signature";
		try {
			AbstractSignatureForm signatureDocumentForm = signService.getSignatureDocumentForm(toSignFiles, signRequest);
			signatureForm = signatureDocumentForm.getSignatureForm();
			signatureDocumentForm.setEncryptionAlgorithm(EncryptionAlgorithm.RSA);
			
			SignatureTokenConnection signatureTokenConnection = userKeystoreService.getSignatureTokenConnection(user.getKeystore().getInputStream(), password);
			CertificateToken certificateToken = userKeystoreService.getCertificateToken(user.getKeystore().getInputStream(), password);
			CertificateToken[] certificateTokenChain = userKeystoreService.getCertificateTokenChain(user.getKeystore().getInputStream(), password);
	
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
				InputStream toSignInputStream;
				Document toSignFile = toSignFiles.get(0);
				toSignInputStream = pdfService.formatPdf(toSignFile.getInputStream(), signRequest.getCurrentWorkflowStep().getSignRequestParams(), addPage);
				if(signRequest.getSignBooksWorkflowStep() == 1) {
					toSignInputStream = pdfService.convertGS(pdfService.writeMetadatas(toSignFile.getInputStream(), toSignFile.getFileName(), signRequest));
				}
				MultipartFile multipartFile = fileService.toMultipartFile(toSignInputStream, toSignFile.getFileName(), "application/pdf");
				parameters = signService.fillVisibleParameters((SignatureDocumentForm) signatureDocumentForm, signRequest.getCurrentWorkflowStep().getSignRequestParams(), multipartFile, user);
				SignatureDocumentForm documentForm = (SignatureDocumentForm) signatureDocumentForm;
				documentForm.setDocumentToSign(multipartFile);
				signatureDocumentForm = documentForm;
			}
			step = "Signature du/des documents(s)";

			parameters.setSigningCertificate(certificateToken);
			parameters.setCertificateChain(certificateTokenChain);
			parameters.setSignatureLevel(signatureDocumentForm.getSignatureLevel());
			DSSDocument dssDocument;
			if(toSignFiles.size() > 1) {
				dssDocument = signService.certSignDocument((SignatureMultipleDocumentsForm) signatureDocumentForm, parameters, signatureTokenConnection);
			} else {
				dssDocument = signService.certSignDocument((SignatureDocumentForm) signatureDocumentForm, parameters, signatureTokenConnection);
			}
			InMemoryDocument signedPdfDocument = new InMemoryDocument(DSSUtils.toByteArray(dssDocument), dssDocument.getName(), dssDocument.getMimeType());
			step = "Enregistrement du/des documents(s)";
			return documentService.createDocument(signedPdfDocument.openStream(), signedPdfDocument.getName(), signedPdfDocument.getMimeType().getMimeTypeString());
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
	}
	
	public void addSignedFile(SignRequest signRequest, InputStream signedInputStream, String fileName, String mimeType, User user) throws EsupSignatureIOException {
		try {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
			Document document = documentService.createDocument(signedInputStream, signRequest.getTitle() + "_" + signRequest.getCurrentWorkflowStep().getSignRequestParams().getSignType() + "_" + user.getEppn() + "_" + simpleDateFormat.format(new Date()) + "." + fileService.getExtension(fileName), mimeType);
			signRequest.getSignedDocuments().add(document);
			document.setParentId(signRequest.getId());
		} catch (IOException e) {
			throw new EsupSignatureIOException("error on save signed file", e);
		}
	}

	public void applySignBookRules(SignRequest signRequest, User user) throws EsupSignatureException, IOException {
		SignBook recipientSignBook = signBookService.getSignBookBySignRequestAndUser(signRequest, user);
		List<SignBook> signBooks = signBookService.getSignBookBySignRequest(signRequest);
		SignBook signBook = signBooks.get(0);
		SignRequestParams.SignType signType = signRequest.getCurrentWorkflowStep().getSignRequestParams().getSignType();
		signRequest.getCurrentWorkflowStep().getSignBooks().put(recipientSignBook.getId(), true);
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
				signBookService.removeSignRequestFromSignBook(signRequest, signBook, user);
				signRequest.setSignBooksWorkflowStep(signRequest.getSignBooksWorkflowStep() + 1);
				if(signBook.getSignBookType().equals(SignBookType.workflow) && signRequest.getSignBooksWorkflowStep() < signBook.getSignBooks().size()) {
					signBookService.importSignRequestInSignBook(signRequest, signBook, user);
				} else {
					completeSignRequest(signRequest, signBook, user);
					mailService.sendCompletedMail(signRequest);
				}
			}
		} else {
			if(signType.equals(SignType.visa)) {
				updateStatus(signRequest, SignRequestStatus.pending, "Visa", user, "SUCCESS", signRequest.getComment());
			} else {
				updateStatus(signRequest, SignRequestStatus.pending, "Signature", user, "SUCCESS", signRequest.getComment());
			}
		}
		//signRequest.setNbSign(signRequest.getNbSign() + 1);
	}
	
	public void pendingSignRequest(SignRequest signRequest, User user) {
		for(Long signBookId : signRequest.getCurrentWorkflowStep().getSignBooks().keySet()) {
			SignBook signBook = signBookRepository.findById(signBookId).get();
			for(String emailRecipient : signBook.getRecipientEmails()) {
				User recipient;
				List<User> recipients = userRepository.findByEmail(emailRecipient);
				if(recipients.size() > 0) {
					recipient = recipients.get(0);
				} else {
					recipient = userService.createUser(emailRecipient);
				}
				if(signRequest.getSignBooksWorkflowStep() == 1 && signRequest.getOriginalDocuments().size() == 1 && signRequest.getOriginalDocuments().get(0).getContentType().contains("pdf")) {
					Document toSignDocument = signRequest.getOriginalDocuments().get(0);
					try {
						scanSignatureFields(signRequest, PDDocument.load(toSignDocument.getInputStream()));
					} catch (IOException e) {
						logger.error("unable to scan the pdf document", e);
					}
				}
				if(recipient.getEmailAlertFrequency() == null|| recipient.getEmailAlertFrequency().equals(EmailAlertFrequency.immediately) || userService.checkEmailAlert(recipient)) {
					userService.sendEmailAlert(recipient);
				}
			}
		}
		updateStatus(signRequest, SignRequestStatus.pending, "Envoyé pour signature", user, "SUCCESS", signRequest.getComment());
	}

	private void scanSignatureFields(SignRequest signRequest, PDDocument pdDocument) {
		LinkedList<SignRequestParams> signRequestParamsList = pdfService.pdSignatureFieldsToSignRequestParams(pdDocument);
		if(signRequestParamsList.size() > 0) {
			int stepNumber = 0;
			for (SignRequestParams signRequestParams : signRequestParamsList) {
				signRequestParams.setNewPageType(NewPageType.none);
				signRequestParams.setSignType(SignType.visa);
				signRequestParamsRepository.save(signRequestParams);
				if(signRequest.getWorkflowSteps().size() > stepNumber){
					signRequest.getWorkflowSteps().get(stepNumber).setSignRequestParams(signRequestParams);
				} else {
					addWorkflowStep(signRequest, signRequestParams);
				}
				stepNumber++;
			}
		}
	}

	public void addWorkflowStep(SignRequest signRequest, SignRequestParams signRequestParams) {
		WorkflowStep workflowStep = new WorkflowStep();
		workflowStep.setSignRequestParams(signRequestParams);
		workflowStepRepository.save(workflowStep);
		signRequest.getWorkflowSteps().add(workflowStep);
	}

	public void completeSignRequest(SignRequest signRequest, SignBook signBook, User user) throws EsupSignatureException {
		updateStatus(signRequest, SignRequestStatus.completed, "Terminé automatiquement", user, "SUCCESS", signRequest.getComment());
	}

	public List<Document> getToSignDocuments(SignRequest signRequest) {
		List<Document> documents = new ArrayList<>();
		if(signRequest.getSignedDocuments() != null && signRequest.getSignedDocuments().size() > 0 ) {
			signRequest.setSignedDocuments(signRequest.getSignedDocuments().stream().sorted(Comparator.comparing(Document::getCreateDate).reversed()).collect(Collectors.toList()));
			documents.add(signRequest.getSignedDocuments().get(0));
		} else {
			documents.addAll(signRequest.getOriginalDocuments());
		}
		return documents;
	}
	
	public FsFile getLastSignedFile(SignRequest signRequest) throws Exception {
		if(signRequest.getStatus().equals(SignRequestStatus.exported)) {
			FsAccessService fsAccessService = null;
			if (signRequest.getExportedDocumentURI().startsWith("smb")) {
				fsAccessService = fsAccessFactory.getFsAccessService(DocumentIOType.smb);
				return fsAccessService.getFileFromURI(signRequest.getExportedDocumentURI());
			}
		}
		Document lastSignedDocument = getLastSignedDocument(signRequest);
		return new FsFile(lastSignedDocument.getInputStream(), lastSignedDocument.getFileName(), lastSignedDocument.getContentType());
	}
	
	public Document getLastSignedDocument(SignRequest signRequest) {
		if(signRequest.getSignedDocuments().size() > 0) {
			signRequest.setSignedDocuments(signRequest.getSignedDocuments().stream().sorted(Comparator.comparing(Document::getCreateDate).reversed()).collect(Collectors.toList()));
			return signRequest.getSignedDocuments().get(0);
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
		if (signRequest.getCurrentWorkflowStep().getSignBooks() != null) {
			for (Map.Entry<Long, Boolean> signBookId : signRequest.getCurrentWorkflowStep().getSignBooks().entrySet()) {
				if (!signRequest.getCurrentWorkflowStep().getSignBooks().get(signBookId.getKey()) && signRequest.getCurrentWorkflowStep().isAllSignToComplete()) {
					return false;
				}
				if (signRequest.getCurrentWorkflowStep().getSignBooks().get(signBookId.getKey()) && !signRequest.getCurrentWorkflowStep().isAllSignToComplete()) {
					return true;
				}
			}
			if (signRequest.getCurrentWorkflowStep().isAllSignToComplete()) {
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
		if(signRequest.getCurrentWorkflowStep().isAllSignToComplete()) {
			signRequest.getCurrentWorkflowStep().setAllSignToComplete(false);
		} else {
			signRequest.getCurrentWorkflowStep().setAllSignToComplete(true);
		}
		signRequestRepository.save(signRequest);
	}
	
	public boolean checkUserSignRights(User user, SignRequest signRequest) {
		SignBook signBook = signBookService.getSignBookBySignRequestAndUser(signRequest, user);
		if ((signRequest.getStatus().equals(SignRequestStatus.pending) || signRequest.getStatus().equals(SignRequestStatus.draft)) 
				&& signBook != null
				&& !signRequest.getCurrentWorkflowStep().getSignBooks().get(signBook.getId())) {
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
		for (Map.Entry<Long, Boolean> signBookMap : signRequest.getCurrentWorkflowStep().getSignBooks().entrySet()) {
			signBookNames.put(signBookRepository.findById(signBookMap.getKey()).get().getName(), signBookMap.getValue());
		}
		signRequest.setSignBooksLabels(signBookNames);
	}

	public String getStep() {
		return step;
	}
	
}