package org.esupportail.esupsignature.service;

import eu.europa.esig.dss.AbstractSignatureParameters;
import eu.europa.esig.dss.asic.cades.ASiCWithCAdESSignatureParameters;
import eu.europa.esig.dss.asic.xades.ASiCWithXAdESSignatureParameters;
import eu.europa.esig.dss.enumerations.ASiCContainerType;
import eu.europa.esig.dss.enumerations.EncryptionAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureForm;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import org.apache.commons.codec.binary.Base64;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dss.model.AbstractSignatureForm;
import org.esupportail.esupsignature.dss.model.SignatureDocumentForm;
import org.esupportail.esupsignature.dss.model.SignatureMultipleDocumentsForm;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.User.EmailAlertFrequency;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureKeystoreException;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.*;
import org.esupportail.esupsignature.service.event.EventService;
import org.esupportail.esupsignature.service.file.FileService;
import org.esupportail.esupsignature.service.fs.FsAccessFactory;
import org.esupportail.esupsignature.service.fs.FsAccessService;
import org.esupportail.esupsignature.service.fs.FsFile;
import org.esupportail.esupsignature.service.mail.MailService;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.esupportail.esupsignature.service.sign.SignService;
import org.esupportail.esupsignature.web.controller.ws.json.JsonMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.support.MutableSortDefinition;
import org.springframework.beans.support.PropertyComparator;
import org.springframework.beans.support.SortDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SignRequestService {

	private static final Logger logger = LoggerFactory.getLogger(SignRequestService.class);

	@Resource
	private GlobalProperties globalProperties;

	@Resource
	private SignRequestRepository signRequestRepository;

	@Resource
	private LogRepository logRepository;

	@Resource
	private RecipientService recipientService;

	@Resource
	private RecipientRepository recipientRepository;

	@Resource
	private UserKeystoreService userKeystoreService;

	@Resource
	private PdfService pdfService;

	@Resource
	private DocumentService documentService;

	@Resource
	private DocumentRepository documentRepository;

	@Resource
	private SignRequestParamsRepository signRequestParamsRepository;

	@Resource
	private WorkflowRepository workflowRepository;

	@Resource
	private BigFileService bigFileService;

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
	private UserShareService userShareService;

	@Resource
	private MailService mailService;

	@Resource
	private UserShareRepository userShareRepository;

	@Resource
	private DataRepository dataRepository;

	@Resource
	private FsAccessFactory fsAccessFactory;

	@Resource
	private DataService dataService;

	@Resource
	private EventService eventService;

	public List<SignRequest> getSignRequestsForCurrentUserByStatus(User user, User authUser, String statusFilter) {
		List<SignRequest> signRequestList = new ArrayList<>();
		List<SignRequest> signRequests = getSignRequestsByStatus(user, statusFilter);
		if(!user.equals(authUser)) {
			for(SignRequest signRequest: signRequests) {
				if(userShareService.checkShare(user, authUser, signRequest)) {
					signRequestList.add(signRequest);
				}
			}
		} else {
			signRequestList.addAll(signRequests);
		}
		return signRequestList;
	}

	public List<SignRequest> getSignRequestsByStatus(User user, String statusFilter) {
		List<SignRequest> signRequests;
		if (statusFilter != null) {
			if (statusFilter.equals("tosign")) {
				signRequests = getToSignRequests(user);
			} else if (statusFilter.equals("signedByMe")) {
				signRequests = getSignRequestsSignedByUser(user);
			} else if (statusFilter.equals("refusedByMe")) {
				signRequests = getSignRequestsRefusedByUser(user);
			} else if (statusFilter.equals("sharedSign")) {
				signRequests = getSignRequestsSharedSign(user);
			} else {
				signRequests = signRequestRepository.findByCreateByAndStatus(user, SignRequestStatus.valueOf(statusFilter));
			}
		} else {
			signRequests = signRequestRepository.findByCreateBy(user);
			for(SignRequest signRequest : getToSignRequests(user)) {
				if(!signRequests.contains(signRequest)) {
					signRequests.add(signRequest);
				}
			}
			for(SignRequest signRequest : getSignRequestsSignedByUser(user)) {
				if(!signRequests.contains(signRequest)) {
					signRequests.add(signRequest);
				}
			}
			for(SignRequest signRequest : getSignRequestsRefusedByUser(user)) {
				if(!signRequests.contains(signRequest)) {
					signRequests.add(signRequest);
				}
			}
		}
		for(SignRequest signRequest : signRequests.stream().filter(signRequest -> signRequest.getParentSignBook() == null).collect(Collectors.toList())) {
			signRequest.setViewTitle(signRequest.getTitle());
			signRequest.setData(dataService.getDataFromSignRequest(signRequest));
		}
		return signRequests;
	}

	public List<SignRequest> getToSignRequests(User user) {
		List<SignRequest> signRequestsToSign = signRequestRepository.findByRecipientUser(user);
		signRequestsToSign = signRequestsToSign.stream().filter(signRequest -> signRequest.getStatus().equals(SignRequestStatus.pending)).sorted(Comparator.comparing(SignRequest::getCreateDate).reversed()).collect(Collectors.toList());
		return  signRequestsToSign;
	}

	public List<SignRequest> getSignRequestsSignedByUser(User user) {
		List<Log> logs = new ArrayList<>();
		logs.addAll(logRepository.findByEppnForAndFinalStatus(user.getEppn(), SignRequestStatus.signed.name()));
		logs.addAll(logRepository.findByEppnForAndFinalStatus(user.getEppn(), SignRequestStatus.checked.name()));
		return getSignRequestsFromLogs(logs);

	}

	public List<SignRequest> getSignRequestsRefusedByUser(User user) {

		List<Log> logs = new ArrayList<>();
		logs.addAll(logRepository.findByEppnForAndFinalStatus(user.getEppn(), SignRequestStatus.refused.name()));
		return getSignRequestsFromLogs(logs);
	}

	private List<SignRequest> getSignRequestsFromLogs(List<Log> logs) {
		List<SignRequest> signRequests = new ArrayList<>();
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

	public List<SignRequest> getSignRequestsSharedSign(User user) {
		List<Log> logs = new ArrayList<>();
		logs.addAll(logRepository.findByEppnAndFinalStatus(user.getEppn(), SignRequestStatus.signed.name()));
		logs.addAll(logRepository.findByEppnAndFinalStatus(user.getEppn(), SignRequestStatus.checked.name()));
		logs.addAll(logRepository.findByEppnAndFinalStatus(user.getEppn(), SignRequestStatus.refused.name()));
		return getSignRequestsFromLogs(logs);
	}

	public List<Document> getToSignDocuments(SignRequest signRequest) {
		List<Document> documents = new ArrayList<>();
		if(signRequest.getSignedDocuments() != null && signRequest.getSignedDocuments().size() > 0 ) {
			documents.add(getLastSignedDocument(signRequest));
		} else {
			documents.addAll(signRequest.getOriginalDocuments());
		}
		return documents;
	}

	public SignRequest createSignRequest(String title, User user) {
		SignRequest signRequest = new SignRequest();
		signRequest.setTitle(title);
		signRequest.setToken(String.valueOf(generateUniqueId()));
		signRequest.setCreateBy(user);
		signRequest.setCreateDate(new Date());
		signRequest.setStatus(SignRequestStatus.draft);
		signRequestRepository.save(signRequest);
		updateStatus(signRequest, SignRequestStatus.draft, "Création de la demande " + title, "SUCCESS");
		return signRequest;
	}

	public boolean checkSignTypeDocType(SignType signType, MultipartFile multipartFile) {
		boolean check = true;
		if(!multipartFile.getContentType().toLowerCase().contains("pdf") && !multipartFile.getContentType().toLowerCase().contains("jpeg")) {
			if(signType.equals(SignType.pdfImageStamp)) {
				check = false;
			}
		}
		return check;
	}

	public void addDocsToSignRequest(SignRequest signRequest, MultipartFile... multipartFiles) throws EsupSignatureIOException {
		for(MultipartFile multipartFile : multipartFiles) {
			try {
				File file = fileService.inputStreamToTempFile(multipartFile.getInputStream(), multipartFile.getName());
				if (multipartFiles.length == 1 && multipartFiles[0].getContentType().equals("application/pdf")) {
					try {
						signRequest.getSignRequestParams().addAll(scanSignatureFields(new FileInputStream(file)));
					} catch (IOException e) {
						throw new EsupSignatureIOException("unable to open multipart inputStream", e);
					}
				}
				String docName = documentService.getFormatedName(multipartFile.getOriginalFilename(), signRequest.getOriginalDocuments().size());
				Document document = documentService.createDocument(new FileInputStream(file), docName, multipartFile.getContentType());
				signRequest.getOriginalDocuments().add(document);
				document.setParentId(signRequest.getId());
				documentRepository.save(document);
				file.delete();
			} catch (IOException e) {
				throw new EsupSignatureIOException("", e);
			}
		}
	}


	public SignBook addDocsInSignBook(User user, String name, String workflowName, MultipartFile[] multipartFiles) throws EsupSignatureException, EsupSignatureIOException {
		SignBook signBook = signBookService.createSignBook(workflowName, name, user, true);
		for (MultipartFile multipartFile : multipartFiles) {
			SignRequest signRequest = createSignRequest(workflowName + "_" + multipartFile.getOriginalFilename(), user);
			addDocsToSignRequest(signRequest, multipartFile);
			signBookService.addSignRequest(signBook, signRequest);
		}
		return signBook;
	}

	public void addAttachmentToSignRequest(SignRequest signRequest, MultipartFile... multipartFiles) throws EsupSignatureIOException {
		for(MultipartFile multipartFile : multipartFiles) {
			try {
				File file = fileService.inputStreamToTempFile(multipartFile.getInputStream(), multipartFile.getName());
				String docName = documentService.getFormatedName("attachement_" + multipartFile.getOriginalFilename(), signRequest.getOriginalDocuments().size());
				Document document = documentService.createDocument(new FileInputStream(file), docName, multipartFile.getContentType());
				signRequest.getAttachments().add(document);
				document.setParentId(signRequest.getId());
				documentRepository.save(document);
				file.delete();
			} catch (IOException e) {
				throw new EsupSignatureIOException("", e);
			}
		}
	}

	public List<SignRequestParams> scanSignatureFields(InputStream inputStream) throws EsupSignatureIOException {
		List<SignRequestParams> signRequestParamses = pdfService.scanSignatureFields(inputStream);
		if(signRequestParamses.size() == 0) {
			SignRequestParams signRequestParams = getEmptySignRequestParams();
			signRequestParamses.add(signRequestParams);
		}
		for(SignRequestParams signRequestParams : signRequestParamses) {
			signRequestParamsRepository.save(signRequestParams);
		}
		return signRequestParamses;
	}

	public void addRecipients(SignRequest signRequest, String... recipientsEmail) throws EsupSignatureUserException {
		for (String recipientEmail : recipientsEmail) {
			User recipientUser;
			if (userRepository.countByEmail(recipientEmail) == 0) {
				recipientUser = userService.createUserWithEmail(recipientEmail);
			} else {
				recipientUser = userRepository.findByEmail(recipientEmail).get(0);
			}
			if(recipientRepository.findByParentIdAndUser(signRequest.getId(), recipientUser).size() == 0) {
				Recipient recipient = recipientService.createRecipient(signRequest.getId(), recipientUser);
				recipientRepository.save(recipient);
				signRequest.getRecipients().add(recipient);
			}
		}

	}

	public void addRecipients(SignRequest signRequest, List<Recipient> recipients) {
		signRequest.getRecipients().clear();
		for(Recipient recipient : recipients) {
			Recipient newRecipient = null;
			try {
				newRecipient = recipientService.getRecipientByEmail(signRequest.getId(), recipient.getUser().getEmail());
				newRecipient.setParentType("signrequest");
				recipientRepository.save(newRecipient);
				signRequest.getRecipients().add(newRecipient);
			} catch (EsupSignatureUserException e) {
				logger.error("add recipient fail", e);
			}
		}
		//signRequestRepository.save(signRequest);
	}

	public void addRecipients(SignRequest signRequest, User user) {
		for(Recipient recipient : signRequest.getRecipients()) {
			if(recipient.getUser().equals(user)) {
				return;
			}
		}
		Recipient recipient = recipientService.createRecipient(signRequest.getId(), user);
		recipientRepository.save(recipient);
		signRequest.getRecipients().add(recipient);
	}

	public void pendingSignRequest(SignRequest signRequest, SignType signType, boolean allSignToComplete) {
		if(!signRequest.getStatus().equals(SignRequestStatus.pending)) {
			signRequest.setSignType(signType);
			signRequest.setAllSignToComplete(allSignToComplete);
			signRequest.setCurrentStepNumber(signRequest.getCurrentStepNumber() + 1);
			updateStatus(signRequest, SignRequestStatus.pending, "Envoyé pour signature", "SUCCESS", null, null, null);
		} else {
			logger.warn("already pending");
		}
	}

	public void sign(SignRequest signRequest, User user, String password, boolean visual, Map<String, String> formDataMap) throws EsupSignatureException, IOException, InterruptedException {
		List<Document> toSignDocuments = getToSignDocuments(signRequest);
		SignType signType = getCurrentSignType(signRequest);
		InputStream filledInputStream;
		if(formDataMap != null && formDataMap.size() > 0 && toSignDocuments.get(0).getContentType().equals("application/pdf")) {
			eventService.publishEvent(new JsonMessage("step", "Remplissage du document", null), "sign", user);
			filledInputStream = pdfService.fill(toSignDocuments.get(0).getInputStream(), formDataMap);
		} else {
			filledInputStream = toSignDocuments.get(0).getInputStream();
		}
		if(signType.equals(SignType.visa) || signType.equals(SignType.pdfImageStamp)) {
			InputStream signedInputStream = filledInputStream;
			String fileName = toSignDocuments.get(0).getFileName();
			if (signType.equals(SignType.visa)) {
				if (toSignDocuments.size() == 1 && toSignDocuments.get(0).getContentType().equals("application/pdf")) {
					if (visual) {
						eventService.publishEvent(new JsonMessage("step", "Apposition de la signature", null), "sign", user);
						signedInputStream = pdfService.stampImage(filledInputStream, getCurrentSignType(signRequest), signRequest.getCurrentSignRequestParams(), user);
					}
				}
			} else {
				if (toSignDocuments.size() == 1 && toSignDocuments.get(0).getContentType().equals("application/pdf") && visual) {
					for(SignRequestParams signRequestParams : signRequest.getSignRequestParams()) {
						signedInputStream = pdfService.stampImage(signedInputStream, getCurrentSignType(signRequest), signRequestParams, user);
					}
				}
			}
			if (signRequest.getParentSignBook() == null || (signBookService.isStepAllSignDone(signRequest.getParentSignBook()) && !signBookService.isNextWorkFlowStep(signRequest.getParentSignBook()))) {
				signedInputStream = pdfService.convertGS(pdfService.writeMetadatas(signedInputStream, fileName, signRequest));
			}
			addSignedFile(signRequest, signedInputStream, toSignDocuments.get(0).getFileName(), toSignDocuments.get(0).getContentType());
		} else {
			if (toSignDocuments.size() == 1 && toSignDocuments.get(0).getContentType().equals("application/pdf")) {
				bigFileService.setBinaryFileStream(toSignDocuments.get(0).getBigFile(), filledInputStream, filledInputStream.available());
			}
			certSign(signRequest, user, password, visual);
		}
		if (signType.equals(SignType.visa)) {
			if(signRequest.getComment() != null && !signRequest.getComment().isEmpty()) {
				updateStatus(signRequest, SignRequestStatus.checked, "Visa",  "SUCCESS", null, null, null, signRequest.getCurrentStepNumber());
			} else {
				updateStatus(signRequest, SignRequestStatus.checked, "Visa", "SUCCESS");
			}
		} else {
			if(signRequest.getComment() != null && !signRequest.getComment().isEmpty()) {
				updateStatus(signRequest, SignRequestStatus.signed, "Signature", "SUCCESS", null, null, null, signRequest.getCurrentStepNumber());
			} else {
				updateStatus(signRequest, SignRequestStatus.signed, "Signature", "SUCCESS");
			}
		}
		eventService.publishEvent(new JsonMessage("step", "Paramétrage de la prochaine étape", null), "sign", user);
		applyEndOfStepRules(signRequest, user);

	}

	public Document nexuSign(SignRequest signRequest, User user, AbstractSignatureForm signatureDocumentForm, AbstractSignatureParameters parameters) throws IOException {
		logger.info(user.getEppn() + " launch nexu signature for signRequest : " + signRequest.getId());
		DSSDocument dssDocument;

		if(signatureDocumentForm.getClass().equals(SignatureMultipleDocumentsForm.class)) {
			dssDocument = signService.signDocument((SignatureMultipleDocumentsForm) signatureDocumentForm);
		} else {
			dssDocument = signService.nexuSignDocument((SignatureDocumentForm) signatureDocumentForm, parameters);
		}

		InMemoryDocument signedDocument = new InMemoryDocument(DSSUtils.toByteArray(dssDocument), dssDocument.getName(), dssDocument.getMimeType());

		return addSignedFile(signRequest, signedDocument.openStream(), dssDocument.getName(), signedDocument.getMimeType().getMimeTypeString());
	}

//	public void serverSign(SignRequest signRequest) throws EsupSignatureException {
//		List<Document> toSignDocuments = new ArrayList<>();
//		for(Document document : getToSignDocuments(signRequest)) {
//			toSignDocuments.add(document);
//		}
//		eventService.publishEvent(new JsonMessage("step", "Initialisation de la procédure", signRequest), "sign", userService.getSystemUser());
//		try {
//			eventService.publishEvent(new JsonMessage("step", "Déverouillage du keystore", signRequest), "sign", userService.getSystemUser());
//
//			File serverKeyStore = new File(SignRequestService.class.getClassLoader().getResource("cert-esupdem.p12").getFile());
//
//			SignatureTokenConnection signatureTokenConnection = userKeystoreService.getSignatureTokenConnection(new FileInputStream(serverKeyStore), "chouthou");
//			CertificateToken certificateToken = userKeystoreService.getCertificateToken(new FileInputStream(serverKeyStore), "chouthou");
//			CertificateToken[] certificateTokenChain = userKeystoreService.getCertificateTokenChain(new FileInputStream(serverKeyStore), "chouthou");
//
//			step = "Formatage des documents";
//
//			AbstractSignatureForm signatureDocumentForm = signService.getSignatureDocumentForm(toSignDocuments, signRequest, false);
//			signatureDocumentForm.setEncryptionAlgorithm(EncryptionAlgorithm.RSA);
//
//			step = "Préparation de la signature";
//
//			signatureDocumentForm.setBase64Certificate(Base64.encodeBase64String(certificateToken.getEncoded()));
//			List<String> base64CertificateChain = new ArrayList<>();
//			for (CertificateToken token : certificateTokenChain) {
//				base64CertificateChain.add(Base64.encodeBase64String(token.getEncoded()));
//			}
//			signatureDocumentForm.setBase64CertificateChain(base64CertificateChain);
//			signatureDocumentForm.setSignWithExpiredCertificate(true);
//
//			ASiCWithXAdESSignatureParameters aSiCWithXAdESSignatureParameters = new ASiCWithXAdESSignatureParameters();
//			aSiCWithXAdESSignatureParameters.aSiC().setContainerType(ASiCContainerType.ASiC_E);
//			AbstractSignatureParameters parameters = aSiCWithXAdESSignatureParameters;
//
//			parameters.setSigningCertificate(certificateToken);
//			parameters.setCertificateChain(certificateTokenChain);
//			parameters.setSignatureLevel(signatureDocumentForm.getSignatureLevel());
//			DSSDocument dssDocument;
//			if(toSignDocuments.size() > 1) {
//				dssDocument = signService.certSignDocument((SignatureMultipleDocumentsForm) signatureDocumentForm, parameters, signatureTokenConnection);
//			} else {
//				dssDocument = signService.certSignDocument((SignatureDocumentForm) signatureDocumentForm, parameters, signatureTokenConnection);
//			}
//			InMemoryDocument signedPdfDocument = new InMemoryDocument(DSSUtils.toByteArray(dssDocument), dssDocument.getName(), dssDocument.getMimeType());
//			step = "Enregistrement du/des documents(s)";
//			addSignedFile(signRequest, signedPdfDocument.openStream(), dssDocument.getName(), signedPdfDocument.getMimeType().getMimeTypeString());
//			updateStatus(signRequest, SignRequestStatus.signed, "Signature", "SUCCESS");
//			applyEndOfStepRules(signRequest, userService.getSystemUser());
//		} catch (EsupSignatureKeystoreException e) {
//			step = "security_bad_password";
//			throw new EsupSignatureKeystoreException(e.getMessage(), e);
//		} catch (Exception e) {
//			step = "sign_system_error";
//			throw new EsupSignatureException(e.getMessage(), e);
//		}
//
//	}

	public void certSign(SignRequest signRequest, User user, String password, boolean visual) throws EsupSignatureException, InterruptedException {
		SignatureForm signatureForm;
		List<Document> toSignDocuments = new ArrayList<>();
		for(Document document : getToSignDocuments(signRequest)) {
			toSignDocuments.add(document);
		}
		eventService.publishEvent(new JsonMessage("step", "Initialisation de la procédure", null), "sign", user);
		try {
			eventService.publishEvent(new JsonMessage("step", "Déverouillage du keystore", null), "sign", user);
			SignatureTokenConnection signatureTokenConnection = userKeystoreService.getSignatureTokenConnection(user.getKeystore().getInputStream(), password);
			CertificateToken certificateToken = userKeystoreService.getCertificateToken(user.getKeystore().getInputStream(), password);
			CertificateToken[] certificateTokenChain = userKeystoreService.getCertificateTokenChain(user.getKeystore().getInputStream(), password);

			eventService.publishEvent(new JsonMessage("step", "Formatage des documents", null), "sign", user);
			AbstractSignatureForm signatureDocumentForm = signService.getSignatureDocumentForm(toSignDocuments, signRequest, visual);
			signatureForm = signatureDocumentForm.getSignatureForm();
			signatureDocumentForm.setEncryptionAlgorithm(EncryptionAlgorithm.RSA);

			eventService.publishEvent(new JsonMessage("step", "Préparation de la signature", null), "sign", user);

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
				parameters = signService.fillVisibleParameters((SignatureDocumentForm) signatureDocumentForm, signRequest.getCurrentSignRequestParams(), ((SignatureDocumentForm) signatureDocumentForm).getDocumentToSign(), user);
			}

			if(signatureForm.equals(SignatureForm.PAdES)) {
				eventService.publishEvent(new JsonMessage("step", "Signature du document", null), "sign", user);
			} else {
				eventService.publishEvent(new JsonMessage("step", "Signature des documents", null), "sign", user);
			}

			parameters.setSigningCertificate(certificateToken);
			parameters.setCertificateChain(certificateTokenChain);
			parameters.setSignatureLevel(signatureDocumentForm.getSignatureLevel());
			DSSDocument dssDocument;
			if(toSignDocuments.size() > 1) {
				dssDocument = signService.certSignDocument((SignatureMultipleDocumentsForm) signatureDocumentForm, parameters, signatureTokenConnection);
			} else {
				dssDocument = signService.certSignDocument((SignatureDocumentForm) signatureDocumentForm, parameters, signatureTokenConnection);
			}
			InMemoryDocument signedPdfDocument = new InMemoryDocument(DSSUtils.toByteArray(dssDocument), dssDocument.getName(), dssDocument.getMimeType());
			eventService.publishEvent(new JsonMessage("step", "Enregistrement du/des documents(s)", null), "sign", user);
			addSignedFile(signRequest, signedPdfDocument.openStream(), dssDocument.getName(), signedPdfDocument.getMimeType().getMimeTypeString());
		} catch (EsupSignatureKeystoreException e) {
			eventService.publishEvent(new JsonMessage("security_bad_password", "Mauvais mot de passe", null), "sign", user);
			throw new EsupSignatureKeystoreException(e.getMessage(), e);
		} catch (Exception e) {
			eventService.publishEvent(new JsonMessage("sign_system_error", e.getMessage(), null), "sign", user);
			throw new EsupSignatureException(e.getMessage(), e);
		}
	}

	public Document addSignedFile(SignRequest signRequest, InputStream signedInputStream, String originalName, String mimeType) throws IOException {
		String docName = documentService.getSignedName(originalName);
		Document document = documentService.createDocument(signedInputStream, docName, mimeType);
		document.setParentId(signRequest.getId());
		documentRepository.save(document);
		signRequest.getSignedDocuments().add(document);
		signRequestRepository.save(signRequest);
		return document;
	}

	public void applyEndOfStepRules(SignRequest signRequest, User user) throws InterruptedException {
		if(!user.getEppn().equals("system")) {
			recipientService.validateRecipient(signRequest.getRecipients(), user);
		}
		if(signRequest.getParentSignBook() != null) {
			if(!isSignRequestCompleted(signRequest)) {
				updateStatus(signRequest, SignRequestStatus.pending, "Demande incomplète", "SUCCESS");
			}
			if(signBookService.isUserSignAllDocs(signRequest.getParentSignBook(), user)) {
				WorkflowStep currentWorkflowStep = signBookService.getCurrentWorkflowStep(signRequest.getParentSignBook());
				recipientService.validateRecipient(currentWorkflowStep.getRecipients(), user);
			}
			if (signBookService.isStepAllDocsDone(signRequest.getParentSignBook())) {
				if (signBookService.isStepAllSignDone(signRequest.getParentSignBook())) {
					if (!signBookService.nextWorkFlowStep(signRequest.getParentSignBook())) {
						if (!signRequest.getParentSignBook().getCreateBy().equals("scheduler")) {
							mailService.sendCompletedMail(signRequest.getParentSignBook());
						}
						signBookService.completeSignBook(signRequest.getParentSignBook());
					} else {
						signBookService.pendingSignBook(signRequest.getParentSignBook(), user);
					}
				}
			}
		} else {
			if(isSignRequestCompleted(signRequest)) {
				completeSignRequest(signRequest);
			}
		}
	}

	public boolean isSignRequestCompleted(SignRequest signRequest) {
		long checkRecipients = recipientService.checkFalseRecipients(signRequest.getRecipients());
		return checkRecipients == 0 || !signRequest.getAllSignToComplete();
	}

	public void sendEmailAlerts(SignRequest signRequest, User user) {
		for (Recipient recipient : signRequest.getRecipients()) {
			User recipientUser = recipient.getUser();
			if (!recipientUser.equals(user) && (recipientUser.getEmailAlertFrequency() == null || recipientUser.getEmailAlertFrequency().equals(EmailAlertFrequency.immediately) || userService.checkEmailAlert(recipientUser))) {
				userService.sendSignRequestEmailAlert(recipientUser, signRequest);
			}
		}
	}

	public List<User> getTempUsers(SignRequest signRequest) {
		List<User> users = new ArrayList<>();
		if(signRequest.getParentSignBook() != null) {
			for (WorkflowStep workflowStep : signRequest.getParentSignBook().getWorkflowSteps()) {
				for (Recipient recipient : workflowStep.getRecipients()) {
					if (recipient.getUser().getEppn().equals(recipient.getUser().getEmail()) && recipient.getUser().getEppn().equals(recipient.getUser().getName())) {
						users.add(recipient.getUser());
					}
				}
			}
		}
		return users;
	}

	public void completeSignRequest(SignRequest signRequest) {
		completeSignRequests(Arrays.asList(signRequest));
	}

	public void completeSignRequests(List<SignRequest> signRequests) {
		for(SignRequest signRequest : signRequests) {
			updateStatus(signRequest, SignRequestStatus.completed, "Terminé", "SUCCESS");
		}
	}

	public void sendSignRequestsToTarget(String title, List<SignRequest> signRequests, DocumentIOType documentIOType, String targetUrl) throws EsupSignatureException {
		if(documentIOType != null && !documentIOType.equals(DocumentIOType.none)) {
			if (documentIOType.equals(DocumentIOType.mail)) {
				logger.info("send by email to " + targetUrl);
				try {
					mailService.sendFile(title, signRequests, targetUrl);
				} catch (MessagingException | IOException e) {
					throw new EsupSignatureException("unable to send mail", e);
				}
			} else {
				for (SignRequest signRequest : signRequests) {
					Document signedFile = getLastSignedDocument(signRequest);
					documentService.exportDocument(documentIOType, targetUrl, signedFile);
					updateStatus(signRequest, SignRequestStatus.exported, "Exporté vers " + targetUrl, "SUCCESS");
				}
			}
		}
	}

	public void archiveSignRequests(List<SignRequest> signRequests) throws EsupSignatureException {
		if(globalProperties.getArchiveUri() != null) {
			for(SignRequest signRequest : signRequests) {
				Document signedFile = getLastSignedDocument(signRequest);
				String subPath = "";
				if(signRequest.getParentSignBook() != null) {
					subPath = "/" + signRequest.getParentSignBook().getName().split("_")[0].replace(" ", "-") + "/";
				} else {
					subPath = "/simple/" + signRequest.getCreateBy().getEppn() + "/";
				}
				if(signRequest.getExportedDocumentURI() == null) {
					String documentUri = documentService.archiveDocument(signedFile, globalProperties.getArchiveUri(), subPath);
					signRequest.setExportedDocumentURI(documentUri);
					updateStatus(signRequest, SignRequestStatus.archived, "Exporté vers l'archivage", "SUCCESS");

				}
			}
		} else {
			logger.info("archive document was skipped");
		}
	}

	public void cleanDocuments(SignRequest signRequest) {
		Date cleanDate = getEndDate(signRequest);
		Calendar cal = Calendar.getInstance();
		cal.setTime(cleanDate);
		cal.add(Calendar.DATE, globalProperties.getDelayBeforeCleaning());
		cleanDate = cal.getTime();
		if (signRequest.getExportedDocumentURI() != null
				&& new Date().after(cleanDate) && signRequest.getSignedDocuments().size() > 0) {
			clearAllDocuments(signRequest);
			updateStatus(signRequest, SignRequestStatus.exported, "Fichiers nettoyés", "SUCCESS");
		} else {
			logger.debug("cleanning documents was skipped because date");
		}
	}


	public void clearAllDocuments(SignRequest signRequest) {
		if(signRequest.getExportedDocumentURI() != null && !signRequest.getExportedDocumentURI().isEmpty()) {
			logger.info("clear all documents from signRequest : " + signRequest.getId());
			List<Document> documents = new ArrayList<>();
			documents.addAll(signRequest.getOriginalDocuments());
			documents.addAll(signRequest.getSignedDocuments());
			signRequest.getOriginalDocuments().clear();
			signRequest.getSignedDocuments().clear();
			for(Document document : documents) {
				documentRepository.delete(document);
			}
			signRequestRepository.save(signRequest);


		}
	}

	public FsFile getLastSignedFsFile(SignRequest signRequest) throws EsupSignatureException {
		if(signRequest.getStatus().equals(SignRequestStatus.exported)) {
			if (signRequest.getExportedDocumentURI() != null && !signRequest.getExportedDocumentURI().startsWith("mail")) {
				FsAccessService fsAccessService = fsAccessFactory.getFsAccessService(signRequest.getExportedDocumentURI());
				return fsAccessService.getFileFromURI(signRequest.getExportedDocumentURI());
			}
		}
		Document lastSignedDocument = getToSignDocuments(signRequest).get(0);
		return new FsFile(lastSignedDocument.getInputStream(), lastSignedDocument.getFileName(), lastSignedDocument.getContentType());
	}

	public Document getLastSignedDocument(SignRequest signRequest) {
		if(signRequest.getSignedDocuments().size() > 0) {
			return signRequest.getSignedDocuments().get(signRequest.getSignedDocuments().size() - 1);
		} else {
			return getLastOriginalDocument(signRequest);
		}
	}

	public Document getLastOriginalDocument(SignRequest signRequest) {
		List<Document> documents = signRequest.getOriginalDocuments();
		if (documents.size() != 1) {
			return null;
		} else {
			return documents.get(0);
		}
	}

	public void updateStatus(SignRequest signRequest, SignRequestStatus signRequestStatus, String action, String returnCode) {
		updateStatus(signRequest, signRequestStatus, action, returnCode, null, null, null, null);
	}

	public void updateStatus(SignRequest signRequest, SignRequestStatus signRequestStatus, String action, String returnCode, Integer pageNumber, Integer posX, Integer posY) {
		updateStatus(signRequest, signRequestStatus, action, returnCode, pageNumber, posX, posY, null);
	}

	public void updateStatus(SignRequest signRequest, SignRequestStatus signRequestStatus, String action, String returnCode, Integer pageNumber, Integer posX, Integer posY, Integer stepNumber) {
		User user = userService.getUserFromAuthentication();
		Log log = new Log();
		log.setSignRequestId(signRequest.getId());
		if(user != null) {
			log.setEppn(user.getEppn());
			log.setEppnFor(userService.getCurrentUser().getEppn());
			log.setIp(user.getIp());
		}
		log.setInitialStatus(signRequest.getStatus().toString());
		log.setLogDate(new Date());
		log.setAction(action);
		log.setReturnCode(returnCode);
		log.setComment(signRequest.getComment());

		if(pageNumber != null) {
			log.setPageNumber(pageNumber);
			log.setPosX(posX);
			log.setPosY(posY);
		}
		if(stepNumber != null) {
			log.setStepNumber(stepNumber);
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
		if(signRequest.getParentSignBook() != null) {
			signBookService.refuse(signRequest.getParentSignBook(), signRequest.getComment(), user);
		} else {
			updateStatus(signRequest, SignRequestStatus.refused, "Refusé", "SUCCESS", null, null, null);
			for(Recipient recipient : signRequest.getRecipients()) {
				if(recipient.getUser().equals(user)) {
					recipient.setSigned(true);
				}
			}
		}
	}

	public boolean needToSign(SignRequest signRequest, User user) {
		return recipientService.needSign(signRequest.getRecipients(), user);
	}

	public boolean preAuthorizeOwner(Long id, User user) {
		SignRequest signRequest = signRequestRepository.findById(id).get();
		return signRequest.getCreateBy().equals(user);
	}

	public boolean preAuthorizeView(Long id, User user, User authUser) {
		SignRequest signRequest = signRequestRepository.findById(id).get();
		if (checkUserViewRights(user, authUser, signRequest) || checkUserSignRights(user, authUser, signRequest)) {
			return true;
		}
		return false;
	}

	public boolean preAuthorizeSign(Long id, User user, User authUser) {
		SignRequest signRequest = signRequestRepository.findById(id).get();
		if (checkUserSignRights(user, authUser, signRequest)) {
			return true;
		}
		return false;
	}

	public boolean checkUserSignRights(User user, User authUser, SignRequest signRequest) {
		if(user.equals(authUser) || userShareService.checkShare(user, authUser, signRequest, ShareType.sign)) {
			if ((signRequest.getStatus().equals(SignRequestStatus.pending) || signRequest.getStatus().equals(SignRequestStatus.draft))
					&& recipientService.recipientsContainsUser(signRequest.getRecipients(), user) > 0) {
				return true;
			}
		}
		return false;
	}

	public boolean checkUserViewRights(User user, User authUser, SignRequest signRequest) {
		if(user.equals(authUser) || userShareService.checkShare(user, authUser, signRequest)) {
			List<Log> log = logRepository.findByEppnAndSignRequestId(user.getEppn(), signRequest.getId());
			log.addAll(logRepository.findByEppnForAndSignRequestId(user.getEppn(), signRequest.getId()));
			if (signRequest.getCreateBy().equals(user) || log.size() > 0 || recipientService.recipientsContainsUser(signRequest.getRecipients(), user) > 0) {
				return true;
			}
		}
		return false;
	}

	public SignRequestParams getEmptySignRequestParams() {
		SignRequestParams signRequestParams = new SignRequestParams();
		signRequestParams.setSignImageNumber(0);
		signRequestParams.setSignPageNumber(1);
		signRequestParams.setxPos(0);
		signRequestParams.setyPos(0);
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
		if(signRequest.getParentSignBook() != null) {
			signRequest.getParentSignBook().getSignRequests().remove(signRequest);
		}
//		for (Recipient recipient : signRequest.getRecipients()) {
//			recipientRepository.delete(recipient);
//		}
		signRequest.getRecipients().clear();
		signRequestRepository.save(signRequest);
		signRequestRepository.delete(signRequest);
	}

	public SignType getCurrentSignType(SignRequest signRequest) {
		if(signRequest.getParentSignBook() != null) {
			WorkflowStep currentWorkflowStep = signBookService.getCurrentWorkflowStep(signRequest.getParentSignBook());
			if(currentWorkflowStep != null) {
				return signBookService.getCurrentWorkflowStep(signRequest.getParentSignBook()).getSignType();
			} else {
				return null;
			}
		} else {
			return signRequest.getSignType();
		}
	}

	public Page<SignRequest> getSignRequestsPageGrouped(List<SignRequest> signRequests, Pageable pageable) {
		List<SignRequest> signRequestsGrouped = new ArrayList<>();
		Map<SignBook, List<SignRequest>> signBookSignRequestMap = signRequests.stream().filter(signRequest -> signRequest.getParentSignBook() != null).collect(Collectors.groupingBy(SignRequest::getParentSignBook, Collectors.toList()));
		for(Map.Entry<SignBook, List<SignRequest>> signBookListEntry : signBookSignRequestMap.entrySet()) {
			int last = signBookListEntry.getValue().size() - 1;
			signBookListEntry.getValue().get(last).setViewTitle("");
			for(SignRequest signRequest : signBookListEntry.getValue()) {
				signBookListEntry.getValue().get(last).setViewTitle(signBookListEntry.getValue().get(last).getViewTitle() + signRequest.getTitle() + "\n\r");
			}
			signRequestsGrouped.add(signBookListEntry.getValue().get(last));
		}
		for(SignRequest signRequest : signRequests.stream().filter(signRequest -> signRequest.getParentSignBook() == null).collect(Collectors.toList())) {
			signRequestsGrouped.add(signRequest);
		}
		if(pageable.getSort().iterator().hasNext()) {
			Sort.Order order = pageable.getSort().iterator().next();
			SortDefinition sortDefinition = new MutableSortDefinition(order.getProperty(), true, order.getDirection().isAscending());
			Collections.sort(signRequestsGrouped, new PropertyComparator(sortDefinition));
		}
		for(SignRequest signRequest : signRequestsGrouped) {
			signRequest.setEndDate(getEndDate(signRequest));
		}
		return new PageImpl<>(signRequestsGrouped.stream().skip(pageable.getOffset()).limit(pageable.getPageSize()).collect(Collectors.toList()), pageable, signRequestsGrouped.size());
	}

	private Date getEndDate(SignRequest signRequest) {
		List<Log> endLog = logRepository.findBySignRequestIdAndFinalStatus(signRequest.getId(), SignRequestStatus.completed.name());
		endLog.addAll(logRepository.findBySignRequestIdAndFinalStatus(signRequest.getId(), SignRequestStatus.refused.name()));
		if(endLog .size() > 0) {
			return endLog.get(0).getLogDate();
		}
		return null;
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

	public User checkShare(SignRequest signRequest) {
		SignBook signBook = signRequest.getParentSignBook();
		if(signBook != null) {
			User toUser = userService.getUserFromAuthentication();
			List<UserShare> userShares = userShareRepository.findByToUsersInAndShareTypesContains(Collections.singletonList(toUser), ShareType.sign);
			for (UserShare userShare : userShares) {
				Workflow workflow = workflowRepository.findById(signBook.getWorkflowId()).get();
				if(userShare.getWorkflow().equals(workflow) && checkUserSignRights(userShare.getUser(), toUser, signRequest)) {
					return userShare.getUser();
				}
			}
			List<Data> datas = dataRepository.findBySignBook(signBook);
			if(datas.size() > 0) {
				for (UserShare userShare : userShares) {
					if (userShare.getForm().equals(datas.get(0).getForm()) && checkUserSignRights(userShare.getUser(), toUser, signRequest)) {
						return userShare.getUser();
					}
				}
			}
		}
		return null;
	}

}