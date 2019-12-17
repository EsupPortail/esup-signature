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
import org.esupportail.esupsignature.service.fs.UploadActionType;
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

	public List<SignRequest> getTosignRequests(User user) {
		List<SignRequest> signRequestsToSign = new ArrayList<>();
		List<WorkflowStep> workflowSteps = workflowStepRepository.findByRecipients(user.getId());
		for(WorkflowStep workflowStep : workflowSteps) {
			if(!workflowStep.getRecipients().get(user.getId()) && signRequestRepository.findByWorkflowSteps(Arrays.asList(workflowStep)).size() > 0) {
				SignRequest signRequest = signRequestRepository.findByWorkflowSteps(Arrays.asList(workflowStep)).get(0);
				if (signRequest.getCurrentWorkflowStep().equals(workflowStep) && signRequest.getStatus().equals(SignRequestStatus.pending)) {
					signRequestsToSign.add(signRequest);
				}
			}
		}
		return signRequestsToSign.stream().sorted(Comparator.comparing(SignRequest::getCreateDate).reversed()).collect(Collectors.toList());

	}

	public SignRequest createSignRequest(SignRequest signRequest, User user) {
			return createSignRequest(signRequest, user, new ArrayList<>());
	}
	
	public SignRequest createSignRequest(SignRequest signRequest, User user, Document document) {
		List<Document> documents = new ArrayList<Document>();
		documents.add(document);
		return createSignRequest(signRequest, user, documents);
	}
	
	public SignRequest createSignRequest(SignRequest signRequest, User user, List<Document> documents) {
		signRequest.setToken(String.valueOf(generateUniqueId()));
		signRequest.setCreateBy(user.getEppn());
		signRequest.setCreateDate(new Date());
		signRequest.setStatus(SignRequestStatus.draft);
		signRequest.setOriginalDocuments(documents);
		signRequestRepository.save(signRequest);
		updateStatus(signRequest, SignRequestStatus.draft, "Création de la demande", user, "SUCCESS", "");
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

	public void sign(SignRequest signRequest, User user, String password, boolean addDate, boolean visual) throws EsupSignatureException, IOException {
		step = "Demarrage de la signature";
		boolean addPage = false;
		if(!SignRequestParams.NewPageType.none.equals(signRequest.getCurrentWorkflowStep().getSignRequestParams().getNewPageType())) {
			int nbSignOk = signRequest.countSignOk();
			signRequest.getCurrentWorkflowStep().getSignRequestParams().setXPos(95 + ((nbSignOk - (Math.abs(nbSignOk / 3) * 3)) * 150));
			signRequest.getCurrentWorkflowStep().getSignRequestParams().setYPos(188 +(Math.abs(nbSignOk / 3) * 100));
			if(nbSignOk == 0) {
				addPage = true;
			}
		}
		Document signedFile = null;
		List<Document> toSignDocuments = getToSignDocuments(signRequest);
		SignType signType = signRequest.getCurrentWorkflowStep().getSignRequestParams().getSignType();
		if (signType.equals(SignType.visa)) {
			if (toSignDocuments.size() == 1 && toSignDocuments.get(0).getContentType().equals("application/pdf") && visual) {
				signedFile = pdfService.stampImage(toSignDocuments.get(0), signRequest, user, addPage, addDate);
			} else {
				signedFile = toSignDocuments.get(0);
			}
		} else if(signType.equals(SignRequestParams.SignType.pdfImageStamp)) {
			signedFile = pdfService.stampImage(toSignDocuments.get(0), signRequest, user, addPage, addDate);
		} else {
			signedFile = certSign(signRequest, user, password, addDate, visual);
		}
		
		if (signedFile != null) {
			//addSignedFile(signRequest, signedFile, signedFile.getName(), Files.probeContentType(signedFile.toPath()) , user);
			signRequest.getSignedDocuments().add(signedFile);
			signedFile.setParentId(signRequest.getId());
			//signedFile.delete();
			applyEndOfStepRules(signRequest, user);
			step = "end";
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
			applyEndOfStepRules(signRequest, user);
		} catch (EsupSignatureException | IOException e) {
			throw new EsupSignatureSignException("error on apply signBook rules", e);
		}
	}

	public Document certSign(SignRequest signRequest, User user, String password, boolean addDate, boolean visual) throws EsupSignatureKeystoreException, IOException {
		SignatureForm signatureForm;
		List<Document> toSignFiles = new ArrayList<>();
		for(Document document : getToSignDocuments(signRequest)) {
			toSignFiles.add(document);
		}
		step = "Préparation de la signature";
		try {
			AbstractSignatureForm signatureDocumentForm = signService.getSignatureDocumentForm(toSignFiles, signRequest, visual);
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
				//TODO fix problème png (premier upload ?)
				step = "Formatage du PDF";
				boolean addPage = false;
				if(signRequest.countSignOk() == 0) {
					addPage = true;
				}
				InputStream toSignInputStream;
				Document toSignFile = toSignFiles.get(0);
				toSignInputStream = pdfService.formatPdf(toSignFile.getInputStream(), signRequest.getCurrentWorkflowStep().getSignRequestParams(), addPage);
				if(signRequest.getCurrentWorkflowStepNumber() == 1) {
					toSignInputStream = pdfService.convertGS(pdfService.writeMetadatas(toSignFile.getInputStream(), toSignFile.getFileName(), signRequest));
				}
				MultipartFile multipartFile = fileService.toMultipartFile(toSignInputStream, toSignFile.getFileName(), "application/pdf");
				parameters = signService.fillVisibleParameters((SignatureDocumentForm) signatureDocumentForm, signRequest.getCurrentWorkflowStep().getSignRequestParams(), multipartFile, user, addDate);
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
		} catch (Exception e) {
			step = "sign_system_error";
			throw new EsupSignatureKeystoreException(e.getMessage(), e);
		}
	}
	
	public void addSignedFile(SignRequest signRequest, InputStream signedInputStream, String fileName, String mimeType, User user) throws EsupSignatureIOException {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		Document document = documentService.createDocument(signedInputStream, signRequest.getTitle() + "_" + signRequest.getCurrentWorkflowStep().getSignRequestParams().getSignType() + "_" + user.getEppn() + "_" + simpleDateFormat.format(new Date()) + "." + fileService.getExtension(fileName), mimeType);
		signRequest.getSignedDocuments().add(document);
		document.setParentId(signRequest.getId());
	}

	public void applyEndOfStepRules(SignRequest signRequest, User user) throws EsupSignatureException, IOException {
		//SignBook recipientSignBook = signBookService.getSignBookBySignRequestAndUser(signRequest, user);
		SignRequestParams.SignType signType = signRequest.getCurrentWorkflowStep().getSignRequestParams().getSignType();
		signRequest.getCurrentWorkflowStep().getRecipients().put(user.getId(), true);
		if (isSignRequestCompleted(signRequest)) {
			if(signType.equals(SignType.visa)) {
				updateStatus(signRequest, SignRequestStatus.checked, "Visa" , user, "SUCCESS", signRequest.getComment());
			} else {
				updateStatus(signRequest, SignRequestStatus.signed, "Signature", user, "SUCCESS", signRequest.getComment());
			}
			//signBookService.removeSignRequestFromSignBook(signRequest, recipientSignBook);
			if(signRequest.getCurrentWorkflowStepNumber() == signRequest.getWorkflowSteps().size()) {
				completeSignRequest(signRequest, user);
				setWorkflowsLabels(signRequest.getWorkflowSteps());
				mailService.sendCompletedMail(signRequest);
			} else {
				nextWorkFlowStep(signRequest, user);
				updateStatus(signRequest, SignRequestStatus.pending, "Passage à l'étape suivante", user, "SUCCESS", signRequest.getComment());
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

	public void addWorkflowStep(List<String> recipientEmails, String name, Boolean allSignToComplete, SignType signType, SignRequest signRequest) {
		WorkflowStep workflowStep = new WorkflowStep();
		if(name != null) {
			workflowStep.setName(name);
		}
		if(allSignToComplete ==null) {
			workflowStep.setAllSignToComplete(false);
		} else {
			workflowStep.setAllSignToComplete(allSignToComplete);
		}
		workflowStep.setSignRequestParams(getEmptySignRequestParams());
		workflowStep.getSignRequestParams().setSignType(signType);
		workflowStepRepository.save(workflowStep);
		signRequest.getWorkflowSteps().add(workflowStep);
		if(recipientEmails != null) {
			addRecipientsToWorkflowStep(signRequest, recipientEmails, workflowStep, userService.getSystemUser());
		}
	}

	public void addRecipientsToWorkflowStep(SignRequest signRequest, List<String> recipientsEmail, WorkflowStep workflowStep, User user) {
		for (String recipientEmail : recipientsEmail) {
			User recipientUser;
			if (userRepository.countByEmail(recipientEmail) == 0) {
				recipientUser = userService.createUser(recipientEmail);
			} else {
				recipientUser = userRepository.findByEmail(recipientEmail).get(0);
			}
			workflowStep.getRecipients().put(recipientUser.getId(), false);
			updateStatus(signRequest, signRequest.getStatus(), "Envoyé dans le parapheur " + recipientUser.getName(), user, "SUCCESS", "");
		}
	}

	public void nextWorkFlowStep(SignRequest signRequest, User user) {
		signRequest.setCurrentWorkflowStepNumber(signRequest.getCurrentWorkflowStepNumber() + 1);
	}

	public void pendingSignRequest(SignRequest signRequest, User user) {
		if(!signRequest.getStatus().equals(SignRequestStatus.pending)) {
			updateStatus(signRequest, SignRequestStatus.pending, "Envoyé pour signature", user, "SUCCESS", signRequest.getComment());
			for (Long recipientId : signRequest.getCurrentWorkflowStep().getRecipients().keySet()) {
				User recipientUser = userRepository.findById(recipientId).get();
				if (recipientUser.getEmailAlertFrequency() == null || recipientUser.getEmailAlertFrequency().equals(EmailAlertFrequency.immediately) || userService.checkEmailAlert(recipientUser)) {
					userService.sendEmailAlert(recipientUser);
				}
			}
		} else {
			logger.warn("allready pending");
		}
	}

	public int scanSignatureFields(SignRequest signRequest, PDDocument pdDocument) {
		List<SignRequestParams> signRequestParamsList = pdfService.pdSignatureFieldsToSignRequestParams(pdDocument);
		int stepNumber = 0;
		if(signRequestParamsList.size() > 0) {
			for (SignRequestParams signRequestParams : signRequestParamsList) {
				signRequestParams.setNewPageType(NewPageType.none);
				signRequestParams.setSignType(SignType.visa);
				signRequestParamsRepository.save(signRequestParams);
				if(signRequest.getWorkflowSteps().size() > stepNumber){
					signRequest.getWorkflowSteps().get(stepNumber).getSignRequestParams().setXPos(signRequestParams.getXPos());
					signRequest.getWorkflowSteps().get(stepNumber).getSignRequestParams().setYPos(signRequestParams.getYPos());
				} else {
					WorkflowStep workflowStep = new WorkflowStep();
					workflowStep.setSignRequestParams(signRequestParams);
					workflowStepRepository.save(workflowStep);
					signRequest.getWorkflowSteps().add(workflowStep);
				}
				stepNumber++;
			}
		}
		return stepNumber;
	}

	public void completeSignRequest(SignRequest signRequest, User user) throws EsupSignatureException {
		signRequest.setCurrentWorkflowStepNumber(signRequest.getCurrentWorkflowStepNumber() + 1);
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
		logger.info("clear all documents from " + signRequest.getToken());
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


	public void exportFilesToTarget(SignRequest signRequest, User user) throws EsupSignatureException {
		logger.trace("export signRequest to : " + signRequest.getTargetType() + "://" + signRequest.getDocumentsTargetUri());
		if (signRequest.getStatus().equals(SignRequestStatus.completed) /* && signRequestService.isSignRequestCompleted(signRequest)*/) {
			setWorkflowsLabels(signRequest.getWorkflowSteps());
			boolean exportOk = exportFileToTarget(signRequest, user);
			if (exportOk) {
					updateStatus(signRequest, SignRequestStatus.exported, "Copié vers la destination " + signRequest.getExportedDocumentURI(), user, "SUCCESS", "");
				if (!signRequest.getTargetType().equals(DocumentIOType.mail)) {
					clearAllDocuments(signRequest);
				}
			}
		}
	}

	public boolean exportFileToTarget(SignRequest signRequest, User user) throws EsupSignatureException {
		if (signRequest.getTargetType() != null && !signRequest.getTargetType().equals(DocumentIOType.none)) {
			Document signedFile = getLastSignedDocument(signRequest);
			if (signRequest.getTargetType().equals(DocumentIOType.mail)) {
				logger.info("send by email to " + signRequest.getDocumentsTargetUri());
				mailService.sendFile(signRequest);
				signRequest.setExportedDocumentURI("mail://" + signRequest.getDocumentsTargetUri());
				return true;
			} else {
				try {
					logger.info("send to " + signRequest.getTargetType() + " in /" + signRequest.getDocumentsTargetUri() + "/signed");
					FsAccessService fsAccessService = fsAccessFactory.getFsAccessService(signRequest.getTargetType());
					InputStream inputStream = signedFile.getInputStream();
					fsAccessService.createFile("/", signRequest.getDocumentsTargetUri(), "folder");
					fsAccessService.createFile("/" + signRequest.getDocumentsTargetUri() + "/", "signed", "folder");
					signRequest.setExportedDocumentURI(fsAccessService.getUri() + "/" + signRequest.getDocumentsTargetUri() + "/signed/" + signedFile.getFileName());
					return fsAccessService.putFile( "/" + signRequest.getDocumentsTargetUri() + "/signed/", signedFile.getFileName(), inputStream, UploadActionType.OVERRIDE);
				} catch (Exception e) {
					throw new EsupSignatureException("write fsaccess error : ", e);
				}
			}
		} else {
			logger.debug("no target type for this signbook");
		}
		return false;
	}

	public void updateStatus(SignRequest signRequest, SignRequestStatus signRequestStatus, String action, User user, String returnCode, String comment) {
		updateStatus(signRequest, signRequestStatus, action, user, returnCode, comment, null, null, null);
	}

	public void updateStatus(SignRequest signRequest, SignRequestStatus signRequestStatus, String action, User user, String returnCode, String comment, Integer pageNumber, Integer posX, Integer posY ) {
		Log log = new Log();
		log.setSignRequestId(signRequest.getId());
		log.setEppn(user.getEppn());
		log.setIp(user.getIp());
		log.setInitialStatus(signRequest.getStatus().toString());
		log.setLogDate(new Date());
		log.setAction(action);
		log.setReturnCode(returnCode);
		log.setComment(comment);
		if(pageNumber != null) {
			log.setPageNumber(pageNumber);
			log.setPosX(posX);
			log.setPosY(posY);
		}
		if(signRequestStatus != null) {
			log.setFinalStatus(signRequestStatus.toString());		
			signRequest.setStatus(signRequestStatus);
		} else {
			log.setFinalStatus(signRequest.getStatus().toString());
		}
		logRepository.save(log);
	}

	public boolean isSignRequestCompleted(SignRequest signRequest) {
		if (signRequest.getCurrentWorkflowStep().getRecipients() != null) {
			for (Map.Entry<Long, Boolean> userId : signRequest.getCurrentWorkflowStep().getRecipients().entrySet()) {
				if (!signRequest.getCurrentWorkflowStep().getRecipients().get(userId.getKey()) && signRequest.getCurrentWorkflowStep().isAllSignToComplete()) {
					return false;
				}
				if (signRequest.getCurrentWorkflowStep().getRecipients().get(userId.getKey()) && !signRequest.getCurrentWorkflowStep().isAllSignToComplete()) {
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
		updateStatus(signRequest, SignRequestStatus.refused, "Refusé", user, "SUCCESS", signRequest.getComment());
	}

	public Long toggleNeedAllSign(Workflow workflow, int step) {
		WorkflowStep workflowStep = workflow.getWorkflowSteps().get(step);
		return toggleAllSignToCompleteForWorkflowStep(workflowStep);
	}

	public void toggleNeedAllSign(SignRequest signRequest, int step, Boolean allSignToComplete) {
		WorkflowStep workflowStep = signRequest.getWorkflowSteps().get(step);
		if(allSignToComplete != null && !allSignToComplete.equals(workflowStep.isAllSignToComplete())) {
			toggleAllSignToCompleteForWorkflowStep(workflowStep);
		}
	}

	public void removeStep(SignRequest signRequest, int step) {
		WorkflowStep workflowStep = signRequest.getWorkflowSteps().get(step);
		signRequest.getWorkflowSteps().remove(step);
		if(signRequest.getWorkflowSteps().size() < signRequest.getCurrentWorkflowStepNumber() && signRequest.getStatus().equals(SignRequestStatus.pending)) {
			signRequest.setStatus(SignRequestStatus.completed);
		}
		signRequestRepository.save(signRequest);
		workflowStepRepository.delete(workflowStep);

	}

	private Long toggleAllSignToCompleteForWorkflowStep(WorkflowStep workflowStep) {
		if(workflowStep.isAllSignToComplete()) {
			workflowStep.setAllSignToComplete(false);
		} else {
			workflowStep.setAllSignToComplete(true);
		}
		workflowStepRepository.save(workflowStep);
		return workflowStep.getId();
	}

	public void changeSignType(Workflow workflow, int step, String name, SignType signType) {
		WorkflowStep workflowStep = workflow.getWorkflowSteps().get(step);
		if(name != null) {
			workflowStep.setName(name);
		}
		setSignTypeForWorkflowStep(signType, workflowStep);
		workflowStepRepository.save(workflowStep);
	}

	public void changeSignType(SignRequest signRequest, int step, String name, SignType signType) {
		WorkflowStep workflowStep = signRequest.getWorkflowSteps().get(step);
		if(name != null) {
			workflowStep.setName(name);
		}
		setSignTypeForWorkflowStep(signType, workflowStep);
		workflowStepRepository.save(workflowStep);
	}

	private Long setSignTypeForWorkflowStep(SignType signType, WorkflowStep workflowStep) {
		workflowStep.getSignRequestParams().setSignType(signType);
		return workflowStep.getId();
	}

	public boolean checkUserSignRights(User user, SignRequest signRequest) {
		if ((signRequest.getStatus().equals(SignRequestStatus.pending) || signRequest.getStatus().equals(SignRequestStatus.draft))
				&& signRequest.getCurrentWorkflowStep().getRecipients().containsKey(user.getId()) &&  !signRequest.getCurrentWorkflowStep().getRecipients().get(user.getId())) {
			return true;
		} else {
			return false;
		}
	}

	public boolean checkUserViewRights(User user, SignRequest signRequest) {
		if(signRequest != null) {
			List<Log> log = logRepository.findByEppnAndSignRequestId(user.getEppn(), signRequest.getId());
			if (signRequest.getCreateBy().equals(user.getEppn()) || log.size() > 0 || signBookService.isUserInWorkflow(signRequest, user)) {
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
	
	public void setWorkflowsLabels(List<WorkflowStep> workflowSteps) {
		for(WorkflowStep workflowStep : workflowSteps) {
			Map<String, Boolean> signBookNames = new HashMap<>();
			for (Map.Entry<Long, Boolean> userMap : workflowStep.getRecipients().entrySet()) {
				signBookNames.put(userRepository.findById(userMap.getKey()).get().getFirstname() + " " + userRepository.findById(userMap.getKey()).get().getName(), userMap.getValue());
			}
			workflowStep.setRecipientsNames(signBookNames);
		}
	}


	public void importWorkflow(SignRequest signRequest, Workflow workflow) {
		int i = 0;
		for (WorkflowStep workflowStep : workflow.getWorkflowSteps()) {
			WorkflowStep newWorkflowStep;
			if(signRequest.getWorkflowSteps().size() >= i) {
				newWorkflowStep = signRequest.getWorkflowSteps().get(i);
			} else {
				newWorkflowStep = new WorkflowStep();
			}
			newWorkflowStep.setSignRequestParams(workflowStep.getSignRequestParams());
			newWorkflowStep.setAllSignToComplete(workflowStep.isAllSignToComplete());
			for(Long userId : workflowStep.getRecipients().keySet()){
				User recipient = userRepository.findById(userId).get();
				if(recipient.getName().equals("creator")) {
					recipient = userRepository.findByEmail(userService.getUserFromAuthentication().getEmail()).get(0);
				}
				newWorkflowStep.getRecipients().put(recipient.getId(), false);
			}
			if(newWorkflowStep.getId() == null) {
				workflowStepRepository.save(newWorkflowStep);
				signRequest.getWorkflowSteps().add(newWorkflowStep);
			}
			i++;
		}
		signRequest.setTargetType(workflow.getTargetType());
		signRequest.setDocumentsTargetUri(workflow.getDocumentsTargetUri());
		signRequestRepository.save(signRequest);
	}

	public String getStep() {
		return step;
	}
	
}