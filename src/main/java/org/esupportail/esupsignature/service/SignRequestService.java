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
import org.esupportail.esupsignature.entity.User.EmailAlertFrequency;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
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
import org.springframework.data.spel.spi.EvaluationContextExtension;
import org.springframework.data.spel.spi.Function;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
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
public class SignRequestService implements EvaluationContextExtension {

	private static final Logger logger = LoggerFactory.getLogger(SignRequestService.class);

	@Resource
	private SignRequestRepository signRequestRepository;

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
	private WorkflowService workflowStepService;

	@Resource
	private MailService mailService;

	private String step = "";

	public List<SignRequest> getToSignRequests(User user) {
		List<SignRequest> signRequestsToSign = signRequestRepository.findByRecipientsContains(user.getId());
		signRequestsToSign = signRequestsToSign.stream().filter(signRequest -> signRequest.getStatus().equals(SignRequestStatus.pending)).collect(Collectors.toList());
		signRequestsToSign = signRequestsToSign.stream().sorted(Comparator.comparing(SignRequest::getCreateDate).reversed()).collect(Collectors.toList());
		return  signRequestsToSign;
	}


	public SignType getSignTypeByLevel(int level) {
		SignType signType = null;
		switch (level) {
			case 0:
				signType = SignType.visa;
				break;
			case 1:
				signType = SignType.pdfImageStamp;
				break;
			case 2:
				signType = SignType.certSign;
				break;
			case 3:
				signType = SignType.nexuSign;
				break;
		}
		return signType;
	}

	public SignRequest createSignRequest(String title, MultipartFile[] multipartFiles, List<String> recipientsEmail, SignType signType, User user) {
		SignRequest signRequest = createSignRequest(title, multipartFiles, user);
		pendingSignRequest(signRequest, recipientsEmail, signType, user);
		logger.info("adding new file into signRequest " + signRequest.getToken());
		return signRequest;
	}

	public SignRequest createSignRequest(String title, MultipartFile[] multipartFiles, User user) {
		List<Document> documentsToAdd = documentService.createDocuments(multipartFiles);
		SignRequest signRequest = createSignRequest(documentsToAdd, user);
		signRequest.setTitle(title);
		signRequestRepository.save(signRequest);
		logger.info("adding new file into signRequest " + signRequest.getToken());
		return signRequest;
	}

	public SignRequest createSignRequest(String title, SignBook signBook,  User user, List<Document> documents) {
		SignRequest signRequest = createSignRequest(documents, user);
		signRequest.setTitle(title);
		signBook.getSignRequests().add(signRequest);
		signBookRepository.save(signBook);
		signRequest.setParentSignBook(signBook);
		return signRequest;
	}

	public SignRequest createSignRequest(Document document, User user) {
		List<Document> documents = new ArrayList<>();
		documents.add(document);
		return createSignRequest(documents, user);
	}

	public SignRequest createSignRequest(List<Document> documents, User user) {
		SignRequest signRequest = new SignRequest();
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
		try {
			scanSignatureFields(signRequest);
		} catch (EsupSignatureIOException e) {
			logger.error("error on scan pdf", e);
		}
		return signRequest;
	}

	public List<SignRequest> getSignRequestsSignedByUser(User user) {
		List<SignRequest> signRequests = new ArrayList<>();
		List<Log> logs = new ArrayList<>();
		logs.addAll(logRepository.findByEppnAndFinalStatus(user.getEppn(), SignRequestStatus.signed.name()));
		logs.addAll(logRepository.findByEppnAndFinalStatus(user.getEppn(), SignRequestStatus.checked.name()));
		logs:
		for (Log log : logs) {
			logger.debug("find log : " + log.getSignRequestId() + ", " + log.getFinalStatus());
			try {
				SignRequest signRequest = signRequestRepository.findById(log.getSignRequestId()).get();
				if(!signRequests.contains(signRequest)) {
					signRequests.add(signRequest);
				}
			} catch (Exception e) {
				logger.debug(e.getMessage());
			}
		}
		return signRequests;
	}

	public void sign(SignRequest signRequest, User user, String password, boolean addDate, boolean visual) throws EsupSignatureException {
		step = "Demarrage de la signature";
		Document signedFile = null;
		List<Document> toSignDocuments = getToSignDocuments(signRequest);
		SignType signType = getCurrentSignType(signRequest);
		if (signType.equals(SignType.visa)) {
			if (toSignDocuments.size() == 1 && toSignDocuments.get(0).getContentType().equals("application/pdf") && visual) {
				signedFile = pdfService.stampImage(toSignDocuments.get(0), signRequest, getCurrentSignType(signRequest), getCurrentSignRequestParams(signRequest), user, addDate);
			} else {
				signedFile = new Document();
			}
		} else if(signType.equals(SignType.pdfImageStamp)) {
			if (toSignDocuments.size() == 1 && toSignDocuments.get(0).getContentType().equals("application/pdf") && visual) {
				signedFile = pdfService.stampImage(toSignDocuments.get(0), signRequest, getCurrentSignType(signRequest), getCurrentSignRequestParams(signRequest), user, addDate);
			} else {
				signedFile = new Document();
			}
		} else {
			signedFile = certSign(signRequest, user, password, addDate, visual);
		}

		if (signedFile != null) {
			if(signedFile.getFileName() != null) {
				signRequest.getSignedDocuments().add(signedFile);
				signedFile.setParentId(signRequest.getId());
			}
			if(signType.equals(SignType.visa)) {
				updateStatus(signRequest, SignRequestStatus.checked, "Visa", user, "SUCCESS", signRequest.getComment());
			} else {
				updateStatus(signRequest, SignRequestStatus.signed, "Signature", user, "SUCCESS", signRequest.getComment());
			}
			signRequestRepository.save(signRequest);
			step = "Paramétrage de la prochaine étape";
			applyEndOfStepRules(signRequest, user);
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
	}

	public Document certSign(SignRequest signRequest, User user, String password, boolean addDate, boolean visual) throws EsupSignatureException {
		SignatureForm signatureForm;
		List<Document> toSignFiles = new ArrayList<>();
		for(Document document : getToSignDocuments(signRequest)) {
			toSignFiles.add(document);
		}
		step = "Initialisation de la procédure";
		try {
			step = "Déverouillage du keystore";

			SignatureTokenConnection signatureTokenConnection = userKeystoreService.getSignatureTokenConnection(user.getKeystore().getInputStream(), password);
			CertificateToken certificateToken = userKeystoreService.getCertificateToken(user.getKeystore().getInputStream(), password);
			CertificateToken[] certificateTokenChain = userKeystoreService.getCertificateTokenChain(user.getKeystore().getInputStream(), password);

			step = "Formatage des documents";

			AbstractSignatureForm signatureDocumentForm = signService.getSignatureDocumentForm(toSignFiles, signRequest, visual);
			signatureForm = signatureDocumentForm.getSignatureForm();
			signatureDocumentForm.setEncryptionAlgorithm(EncryptionAlgorithm.RSA);

			step = "Préparation de la signature";

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
				parameters = signService.fillVisibleParameters((SignatureDocumentForm) signatureDocumentForm, getCurrentSignRequestParams(signRequest), ((SignatureDocumentForm) signatureDocumentForm).getDocumentToSign(), user, addDate);
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
			throw new EsupSignatureException(e.getMessage(), e);
		}
	}

	public void addSignedFile(SignRequest signRequest, InputStream signedInputStream, String fileName, String mimeType, User user) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		Document document = documentService.createDocument(signedInputStream, signRequest.getTitle() + "_" + getCurrentSignType(signRequest) + "_" + user.getEppn() + "_" + simpleDateFormat.format(new Date()) + "." + fileService.getExtension(fileName), mimeType);
		signRequest.getSignedDocuments().add(document);
		document.setParentId(signRequest.getId());
	}


	public void applyEndOfStepRules(SignRequest signRequest, User user) {
		if(signRequest.getParentSignBook() != null) {
			if (signBookService.isStepDone(signRequest.getParentSignBook())) {
				getCurrentRecipients(signRequest).put(user.getId(), true);
				signBookService.nextWorkFlowStep(signRequest.getParentSignBook(), user);
				if (signBookService.getStatus(signRequest.getParentSignBook()).equals(SignRequestStatus.completed)) {
					mailService.sendCompletedMail(signRequest.getParentSignBook());
				} else {
					for (SignRequest childSignRequest : signRequest.getParentSignBook().getSignRequests()) {
						updateStatus(childSignRequest, SignRequestStatus.pending, "Passage à l'étape " + signRequest.getParentSignBook().getCurrentWorkflowStepNumber(), user, "SUCCESS", "");
					}
				}
			}
		} else {
			getCurrentRecipients(signRequest).put(user.getId(), true);
			//TODO si tout a true ou si pas allsign
			//completeSignRequest(signRequest, user);
		}
	}

//
//	public void applyEndOfStepRules(SignRequest signRequest, User user) throws EsupSignatureException {
//		//SignBook recipientSignBook = signBookService.getSignBookBySignRequestAndUser(signRequest, user);
//		SignType signType = getCurrentSignType(signRequest);
//		signRequest.getParentSignBook().getCurrentWorkflowStep().getRecipients().put(user.getId(), true);
//		if (isSignRequestCompleted(signRequest)) {
//			if(signType.equals(SignType.visa)) {
//				updateStatus(signRequest, SignRequestStatus.checked, "Visa" , user, "SUCCESS", signRequest.getComment());
//			} else {
//				updateStatus(signRequest, SignRequestStatus.signed, "Signature", user, "SUCCESS", signRequest.getComment());
//			}
//			//signBookService.removeSignRequestFromSignBook(signRequest, recipientSignBook);
//			if(signRequest.getParentSignBook().getCurrentWorkflowStepNumber() == signRequest.getParentSignBook().getWorkflowSteps().size()) {
//				completeSignRequest(signRequest, user);
//				setWorkflowsLabels(signRequest.getParentSignBook().getWorkflowSteps());
//				mailService.sendCompletedMail(signRequest);
//			} else {
//				nextWorkFlowStep(signRequest.getParentSignBook(), user);
//				updateStatus(signRequest, SignRequestStatus.pending, "Passage à l'étape suivante", user, "SUCCESS", signRequest.getComment());
//			}
//		} else {
//			if(signType.equals(SignType.visa)) {
//				updateStatus(signRequest, SignRequestStatus.pending, "Visa", user, "SUCCESS", signRequest.getComment());
//			} else {
//				updateStatus(signRequest, SignRequestStatus.pending, "Signature", user, "SUCCESS", signRequest.getComment());
//			}
//		}
//		//signRequest.setNbSign(signRequest.getNbSign() + 1);
//	}

	public void addRecipients(SignRequest signRequest, List<String> recipientsEmail) {
		for (String recipientEmail : recipientsEmail) {
			User recipientUser;
			if (userRepository.countByEmail(recipientEmail) == 0) {
				recipientUser = userService.createUser(recipientEmail);
			} else {
				recipientUser = userRepository.findByEmail(recipientEmail).get(0);
			}
			signRequest.getRecipients().put(recipientUser.getId(), false);
		}
		signRequestRepository.save(signRequest);
	}

	public void pendingSignRequest(SignRequest signRequest, List<String> recipientsEmail, SignType signType, User user) {
		if(!signRequest.getStatus().equals(SignRequestStatus.pending)) {
			signRequest.setSignType(signType);
			signRequest.getRecipients().clear();
			signRequest.setCurrentStepNumber(signRequest.getCurrentStepNumber() + 1);
			for(String recipientEmail : recipientsEmail) {
				User recipientUser = userService.getUser(recipientEmail);
				signRequest.getRecipients().put(recipientUser.getId(), false);
			}
			signRequestRepository.save(signRequest);
			updateStatus(signRequest, SignRequestStatus.pending, "Envoyé pour signature", user, "SUCCESS", signRequest.getComment());
			sendEmailAlerts(signRequest);
		} else {
			logger.warn("already pending");
		}
	}

	public void sendEmailAlerts(SignRequest signRequest) {
		for (Long recipientId : signRequest.getRecipients().keySet()) {
			User recipientUser = userRepository.findById(recipientId).get();
			if (recipientUser.getEmailAlertFrequency() == null || recipientUser.getEmailAlertFrequency().equals(EmailAlertFrequency.immediately) || userService.checkEmailAlert(recipientUser)) {
				userService.sendEmailAlert(recipientUser);
			}
		}
	}

	public int scanSignatureFields(SignRequest signRequest) throws EsupSignatureIOException {
		int nbSignFound = 0;
		try {
			PDDocument pdDocument = PDDocument.load(signRequest.getOriginalDocuments().get(0).getInputStream());
			if (signRequest.getOriginalDocuments().size() == 1 && signRequest.getOriginalDocuments().get(0).getContentType().contains("pdf")) {
				List<SignRequestParams> signRequestParamsList = pdfService.pdSignatureFieldsToSignRequestParams(pdDocument);
				for(SignRequestParams signRequestParams : signRequestParamsList) {
					signRequestParamsRepository.save(signRequestParams);
				}
				signRequest.getSignRequestParams().addAll(signRequestParamsList);
				nbSignFound = signRequestParamsList.size();
			}
		} catch (IOException e) {
			throw new EsupSignatureIOException("unable to open pdf document");
		}
		logger.info(nbSignFound + " signature fields found on " + signRequest.getOriginalDocuments().get(0).getFileName() + " from signrequest " + signRequest.getId());
		if(nbSignFound == 0) {
			SignRequestParams signRequestParams = getEmptySignRequestParams();
			signRequestParamsRepository.save(signRequestParams);
			signRequest.getSignRequestParams().add(signRequestParams);
		}
		return nbSignFound;
	}

	public String completeSignRequest(SignRequest signRequest, DocumentIOType documentIOType, String targetUri, User user) throws EsupSignatureException {
		String result = "";
		if(documentIOType != null) {
			result = sendSignRequestsToTarget(signRequest.getTitle(), Arrays.asList(signRequest), documentIOType, targetUri);
		}
		updateStatus(signRequest, SignRequestStatus.completed, "Terminé automatiquement", user, "SUCCESS", signRequest.getComment());
		return result;
	}

	public String sendSignRequestsToTarget(String title, List<SignRequest> signRequests, DocumentIOType documentIOType, String targetUrl) throws EsupSignatureException {
		if (documentIOType.equals(DocumentIOType.mail)) {
			logger.info("send by email to " + targetUrl);
			mailService.sendFile(title, signRequests, targetUrl);
			return "mail://" + targetUrl;
		} else {
			for(SignRequest signRequest : signRequests) {
				Document signedFile = getLastSignedDocument(signRequest);
				try {
					logger.info("send to " + documentIOType.name() + " in /" + targetUrl + "/signed");
					FsAccessService fsAccessService = fsAccessFactory.getFsAccessService(documentIOType);
					InputStream inputStream = signedFile.getInputStream();
					fsAccessService.createFile("/", targetUrl, "folder");
					fsAccessService.createFile("/" + targetUrl + "/", "signed", "folder");
					if(fsAccessService.putFile("/" + targetUrl + "/signed/", signedFile.getFileName(), inputStream, UploadActionType.OVERRIDE)){
						return fsAccessService.getUri() + "/" + targetUrl + "/signed/" + signedFile.getFileName();
					}
				} catch (Exception e) {
					throw new EsupSignatureException("write fsaccess error : ", e);
				}
			}
		}
		return null;
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

	public FsFile getLastSignedFsFile(SignRequest signRequest) {
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

	public void refuse(SignRequest signRequest, User user) {
		updateStatus(signRequest, SignRequestStatus.refused, "Refusé", user, "SUCCESS", signRequest.getComment());
		mailService.sendRefusedMail(signRequest.getParentSignBook());
	}

	public boolean checkUserSignRights(User user, SignRequest signRequest) {
		if ((signRequest.getStatus().equals(SignRequestStatus.pending) || signRequest.getStatus().equals(SignRequestStatus.draft))
				&& getCurrentRecipients(signRequest).containsKey(user.getId())) {
			return true;
		} else {
			return false;
		}
	}

	public boolean checkUserViewRights(User user, SignRequest signRequest) {
		if(signRequest != null) {
			List<Log> log = logRepository.findByEppnAndSignRequestId(user.getEppn(), signRequest.getId());
			if (signRequest.getCreateBy().equals(user.getEppn()) || log.size() > 0 || getCurrentRecipients(signRequest).containsKey(user.getId())) {
				return true;
			}
		}
		return false;
	}

	public SignRequestParams getEmptySignRequestParams() {
		SignRequestParams signRequestParams = new SignRequestParams();
		signRequestParams.setSignPageNumber(1);
		signRequestParams.setXPos(0);
		signRequestParams.setYPos(0);
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

	public void delete(SignRequest signRequest) {
		List<Log> logs = logRepository.findBySignRequestId(signRequest.getId());
		for (Log log : logs) {
			logRepository.delete(log);
		}
		for(SignRequestParams signRequestParams: signRequest.getSignRequestParams()) {
			signRequestParamsRepository.delete(signRequestParams);
		}
		for(Document document : signRequest.getOriginalDocuments()) {
			documentService.deleteDocument(document);
		}
		signRequest.getOriginalDocuments().clear();
		signRequestRepository.delete(signRequest);
	}

	public String getStep() {
		return step;
	}

	public void setStep(String step) {
		this.step = step;
	}

	public Map<String, Boolean> getCurrentRecipientsNames(SignRequest signRequest) {
		if(signRequest.getParentSignBook() != null) {
			workflowStepService.setWorkflowsLabels(signRequest.getParentSignBook().getWorkflowSteps());
			return signBookService.getCurrentWorkflowStep(signRequest.getParentSignBook()).getRecipientsNames();
		} else {
			Map<String, Boolean> signBookNames = new HashMap<>();
			for (Map.Entry<Long, Boolean> userMap : signRequest.getRecipients().entrySet()) {
				signBookNames.put(userRepository.findById(userMap.getKey()).get().getFirstname() + " " + userRepository.findById(userMap.getKey()).get().getName(), userMap.getValue());
			}
			return signBookNames;
		}
	}

	public Map<Long, Boolean> getCurrentRecipients(SignRequest signRequest) {
		if(signRequest.getParentSignBook() != null) {
			return signBookService.getCurrentWorkflowStep(signRequest.getParentSignBook()).getRecipients();
		} else {
			return signRequest.getRecipients();
		}
	}

	public SignType getCurrentSignType(SignRequest signRequest) {
		if(signRequest.getParentSignBook() != null) {
			return signBookService.getCurrentWorkflowStep(signRequest.getParentSignBook()).getSignType();
		} else {
			return signRequest.getSignType();
		}
	}

	public SignRequestParams getCurrentSignRequestParams(SignRequest signRequest) {
		if(signRequest.getParentSignBook() != null && signRequest.getSignRequestParams().size() > signRequest.getParentSignBook().getCurrentWorkflowStepNumber() - 1) {
			return signRequest.getSignRequestParams().get(signRequest.getParentSignBook().getCurrentWorkflowStepNumber() - 1);
		} else {
			if(signRequest.getCurrentStepNumber()> 0 && signRequest.getSignRequestParams().size() > signRequest.getCurrentStepNumber() - 1) {
				return signRequest.getSignRequestParams().get(signRequest.getCurrentStepNumber() - 1);
			} else {
				return getEmptySignRequestParams();
			}
		}
	}

	@Override
	public String getExtensionId() {
		return null;
	}

	@Override
	public Map<String, Object> getProperties() {
		return null;
	}

	@Override
	public Map<String, Function> getFunctions() {
		return null;
	}

	@Override
	public Object getRootObject() {
		return null;
	}
}