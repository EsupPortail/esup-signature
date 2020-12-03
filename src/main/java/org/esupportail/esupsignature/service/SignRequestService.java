package org.esupportail.esupsignature.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.mail.MessagingException;

import eu.europa.esig.dss.token.Pkcs12SignatureToken;
import org.apache.commons.codec.binary.Base64;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dss.model.AbstractSignatureForm;
import org.esupportail.esupsignature.dss.model.SignatureDocumentForm;
import org.esupportail.esupsignature.dss.model.SignatureMultipleDocumentsForm;
import org.esupportail.esupsignature.entity.Action;
import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.Field;
import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.LiveWorkflowStep;
import org.esupportail.esupsignature.entity.Log;
import org.esupportail.esupsignature.entity.Recipient;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.UserShare;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.enums.ActionType;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.EmailAlertFrequency;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureKeystoreException;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.ActionRepository;
import org.esupportail.esupsignature.repository.DataRepository;
import org.esupportail.esupsignature.repository.DocumentRepository;
import org.esupportail.esupsignature.repository.LogRepository;
import org.esupportail.esupsignature.repository.RecipientRepository;
import org.esupportail.esupsignature.repository.SignRequestParamsRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.repository.UserRepository;
import org.esupportail.esupsignature.repository.UserShareRepository;
import org.esupportail.esupsignature.service.event.EventService;
import org.esupportail.esupsignature.service.file.FileService;
import org.esupportail.esupsignature.service.fs.FsAccessFactory;
import org.esupportail.esupsignature.service.fs.FsAccessService;
import org.esupportail.esupsignature.service.fs.FsFile;
import org.esupportail.esupsignature.service.mail.MailService;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.esupportail.esupsignature.service.sign.SignService;
import org.esupportail.esupsignature.service.utils.CustomMetricsService;
import org.esupportail.esupsignature.web.controller.ws.json.JsonMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.support.MutableSortDefinition;
import org.springframework.beans.support.PropertyComparator;
import org.springframework.beans.support.SortDefinition;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
	private ActionRepository actionRepository;

	@Resource
	private SignRequestParamsRepository signRequestParamsRepository;

	@Resource
	private CustomMetricsService customMetricsService;

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

    @Autowired(required=false)
	private MetricsEndpoint metricsEndpoint;

	public void init() {
		customMetricsService.registerValue("esup-signature.signrequests", "new");
		customMetricsService.registerValue("esup-signature.signrequests", "signed");
	}

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
		Set<SignRequest> signRequests = new HashSet<>();
		if (statusFilter != null) {
			if (statusFilter.equals("tosign")) {
				signRequests.addAll(getToSignRequests(user));
			} else if (statusFilter.equals("signedByMe")) {
				signRequests.addAll(getSignRequestsSignedByUser(user));
			} else if (statusFilter.equals("refusedByMe")) {
				signRequests.addAll(getSignRequestsRefusedByUser(user));
			} else if (statusFilter.equals("followByMe")) {
				signRequests.addAll(signRequestRepository.findByRecipientUser(user));
				signRequests.removeAll(getToSignRequests(user));
				signRequests.removeAll(getSignRequestsSignedByUser(user));
				signRequests.removeAll(getSignRequestsRefusedByUser(user));
			} else if (statusFilter.equals("sharedSign")) {
				signRequests.addAll(getSharedSignedSignRequests(user));
			} else {
				signRequests.addAll(signRequestRepository.findByCreateByAndStatus(user, SignRequestStatus.valueOf(statusFilter)));
			}
		} else {
			signRequests.addAll(signRequestRepository.findByCreateBy(user));
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
		return new ArrayList<>(signRequests);
	}

	public List<SignRequest> getToSignRequests(User user) {
		List<SignRequest> signRequestsToSign = signRequestRepository.findByRecipientUserToSign(user);
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
		List<Long> ids = new ArrayList<>();
		for (Log log : logs) {
			ids.add(log.getSignRequestId());
		}
		return signRequestRepository.findByIdIn(ids);
	}

	public List<SignRequest> getSharedToSignSignRequests(User user) {
		List<SignRequest> sharedSignRequests = new ArrayList<>();
		List<SignBook> sharedSignBooks = signBookService.getSharedSignBooks(user);
		for(SignBook signBook: sharedSignBooks) {
			sharedSignRequests.addAll(signBook.getSignRequests());
		}
		return sharedSignRequests;
	}

	public List<SignRequest> getSharedSignedSignRequests(User user) {
		List<Log> logs = logRepository.findByEppn(user.getEppn()).stream().filter(
				log -> !log.getEppn().equals(log.getEppnFor())
					&&
						(log.getFinalStatus().equals(SignRequestStatus.signed.name())
						||
						log.getFinalStatus().equals(SignRequestStatus.checked.name())
						||
						log.getFinalStatus().equals(SignRequestStatus.refused.name()))
				).collect(Collectors.toList());
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

	public SignBook addDocsInSignBook(User user, String name, String workflowName, MultipartFile[] multipartFiles) throws EsupSignatureIOException {
		SignBook signBook = signBookService.createSignBook(workflowName, name, user, true);
		for (MultipartFile multipartFile : multipartFiles) {
			SignRequest signRequest = createSignRequest(workflowName + "_" + multipartFile.getOriginalFilename(), user);
			signBookService.addSignRequest(signBook, signRequest);
			addDocsToSignRequest(signRequest, multipartFile);
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
			SignRequestParams signRequestParams = SignRequest.getEmptySignRequestParams();
			signRequestParamses.add(signRequestParams);
		}
		for(SignRequestParams signRequestParams : signRequestParamses) {
			signRequestParamsRepository.save(signRequestParams);
		}
		return signRequestParamses;
	}

	public void addRecipients(SignRequest signRequest, String... recipientsEmail) {
		for (String recipientEmail : recipientsEmail) {
			User recipientUser;
			if (userRepository.countByEmail(recipientEmail) == 0) {
				recipientUser = userService.createUserWithEmail(recipientEmail);
			} else {
				recipientUser = userRepository.findByEmail(recipientEmail).get(0);
			}
			if (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients().stream().anyMatch(r -> r.getUser().equals(recipientUser))) {
				Recipient recipient = recipientService.createRecipient(signRequest.getId(), recipientUser);
				recipientRepository.save(recipient);
				signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients().add(recipient);
			}
		}

	}

	public void addRecipients(SignRequest signRequest, List<Recipient> recipients) {
		signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients().clear();
		for(Recipient recipient : recipients) {
			Recipient newRecipient = null;
			try {
				newRecipient = recipientService.getRecipientByEmail(signRequest.getId(), recipient.getUser().getEmail());
				recipientRepository.save(newRecipient);
				signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients().add(newRecipient);
			} catch (EsupSignatureUserException e) {
				logger.error("add recipient fail", e);
			}
		}
		//signRequestRepository.save(signRequest);
	}

	public void addRecipients(SignRequest signRequest, User user) {
		for(Recipient recipient : signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients()) {
			if(recipient.getUser().equals(user)) {
				return;
			}
		}
		Recipient recipient = recipientService.createRecipient(signRequest.getId(), user);
		recipientRepository.save(recipient);
		signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients().add(recipient);
	}

	public void pendingSignRequest(SignRequest signRequest, SignType signType, boolean allSignToComplete) {
//		if(!signRequest.getStatus().equals(SignRequestStatus.pending)) {
			for (Recipient recipient : signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients()) {
				Action action = new Action();
				actionRepository.save(action);
				signRequest.getRecipientHasSigned().put(recipient, action);
			}
//			signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().setSignType(signType);
//			signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().setAllSignToComplete(allSignToComplete);
			updateStatus(signRequest, SignRequestStatus.pending, "Envoyé pour signature", "SUCCESS", null, null, null);
//		} else {
//			logger.warn("already pending");
//		}
		customMetricsService.incValue("esup-signature.signrequests", "new");
	}


	public void sign(SignRequest signRequest, User user, String password, boolean visual, List<SignRequestParams> signRequestParamses, Map<String, String> formDataMap) throws EsupSignatureException, IOException, InterruptedException {
		List<Document> toSignDocuments = getToSignDocuments(signRequest);
		SignType signType = getCurrentSignType(signRequest);
		InputStream filledInputStream;
		if(!signBookService.isNextWorkFlowStep(signRequest.getParentSignBook())) {
			Data data = dataService.getDataFromSignRequest(signRequest);
			if(data != null) {
				Form form = data.getForm();
				for (Field field : form.getFields()) {
					if ("default".equals(field.getExtValueServiceName()) && "system".equals(field.getExtValueType())) {
						if (field.getExtValueReturn().equals("id")) {
							List<SignBook> signBooks = signBookService.getSignBooksByWorkflowName(form.getWorkflowType());
							data.getDatas().put(field.getName(), "" + (signBooks.size() + 1));
							formDataMap.put(field.getName(), "" + (signBooks.size() + 1));
						}
					}
				}
			}
		}
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
						signedInputStream = pdfService.stampImage(filledInputStream, signRequest, signRequest.getCurrentSignRequestParams(), user);
					}
				}
			} else {
				if (toSignDocuments.size() == 1 && toSignDocuments.get(0).getContentType().equals("application/pdf") && visual) {
					for(SignRequestParams signRequestParams : signRequestParamses) {
						signedInputStream = pdfService.stampImage(signedInputStream, signRequest, signRequestParams, user);
						updateStatus(signRequest, signRequest.getStatus(), "Apposition de la signature",  "SUCCESS", signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos(), signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber());
					}
				}
			}
			if ((signBookService.isStepAllSignDone(signRequest.getParentSignBook()) && !signBookService.isNextWorkFlowStep(signRequest.getParentSignBook()))) {
				signedInputStream = pdfService.convertGS(pdfService.writeMetadatas(signedInputStream, fileName, signRequest));
			}
			addSignedFile(signRequest, signedInputStream, fileService.getNameOnly(signRequest.getTitle()) + "." + fileService.getExtension(toSignDocuments.get(0).getFileName()), toSignDocuments.get(0).getContentType());
		} else {
			if (toSignDocuments.size() == 1 && toSignDocuments.get(0).getContentType().equals("application/pdf")) {
				copySignRequestParams(signRequest, signRequestParamses);
				toSignDocuments.get(0).setTransientInputStream(filledInputStream);
			}
			certSign(signRequest, user, password, visual);
		}
		if (signType.equals(SignType.visa)) {
			if(signRequest.getComment() != null && !signRequest.getComment().isEmpty()) {
				updateStatus(signRequest, SignRequestStatus.checked, "Visa",  "SUCCESS", null, null, null, signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber());
			} else {
				updateStatus(signRequest, SignRequestStatus.checked, "Visa", "SUCCESS");
			}
		} else {
			if(signRequest.getComment() != null && !signRequest.getComment().isEmpty()) {
				updateStatus(signRequest, SignRequestStatus.signed, "Signature", "SUCCESS", null, null, null, signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber());
			} else {
				updateStatus(signRequest, SignRequestStatus.signed, "Signature", "SUCCESS");
			}
		}
		eventService.publishEvent(new JsonMessage("step", "Paramétrage de la prochaine étape", null), "sign", user);
		applyEndOfSignRules(signRequest, user);
		customMetricsService.incValue("esup-signature.signrequests", "signed");
	}

	private void copySignRequestParams(SignRequest signRequest, List<SignRequestParams> signRequestParamses) {
		SignRequestParams signRequestParams = signRequest.getCurrentSignRequestParams();
		signRequestParams.setSignPageNumber(signRequestParamses.get(0).getSignPageNumber());
		signRequestParams.setxPos(signRequestParamses.get(0).getxPos());
		signRequestParams.setyPos(signRequestParamses.get(0).getyPos());
		signRequestParams.setSignWidth(signRequestParamses.get(0).getSignWidth());
		signRequestParams.setSignHeight(signRequestParamses.get(0).getSignHeight());
		signRequestParams.setAddDate(signRequestParamses.get(0).isAddDate());
		signRequestParams.setAddName(signRequestParamses.get(0).isAddName());
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

		return addSignedFile(signRequest, signedDocument.openStream(), fileService.getNameOnly(signRequest.getTitle()) + "." + fileService.getExtension(signedDocument.getName()), signedDocument.getMimeType().getMimeTypeString());
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
		Pkcs12SignatureToken pkcs12SignatureToken = null;
		try {
			eventService.publishEvent(new JsonMessage("step", "Déverouillage du keystore", null), "sign", user);
			pkcs12SignatureToken = userKeystoreService.getPkcs12Token(user.getKeystore().getInputStream(), password);
			CertificateToken certificateToken = userKeystoreService.getCertificateToken(pkcs12SignatureToken);
			CertificateToken[] certificateTokenChain = userKeystoreService.getCertificateTokenChain(pkcs12SignatureToken);
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
				dssDocument = signService.certSignDocument((SignatureMultipleDocumentsForm) signatureDocumentForm, parameters, pkcs12SignatureToken);
			} else {
				dssDocument = signService.certSignDocument((SignatureDocumentForm) signatureDocumentForm, parameters, pkcs12SignatureToken);
			}
			pkcs12SignatureToken.close();
			InMemoryDocument signedPdfDocument = new InMemoryDocument(DSSUtils.toByteArray(dssDocument), dssDocument.getName(), dssDocument.getMimeType());
			eventService.publishEvent(new JsonMessage("step", "Enregistrement du/des documents(s)", null), "sign", user);
			addSignedFile(signRequest, signedPdfDocument.openStream(), fileService.getNameOnly(signRequest.getTitle()) + "." + fileService.getExtension(dssDocument.getName()), signedPdfDocument.getMimeType().getMimeTypeString());
		} catch (EsupSignatureKeystoreException e) {
			eventService.publishEvent(new JsonMessage("sign_system_error", "Mauvais mot de passe", null), "sign", user);
			if(pkcs12SignatureToken != null) pkcs12SignatureToken.close();
			throw new EsupSignatureKeystoreException(e.getMessage(), e);
		} catch (Exception e) {
			eventService.publishEvent(new JsonMessage("sign_system_error", e.getMessage(), null), "sign", user);
			if(pkcs12SignatureToken != null) pkcs12SignatureToken.close();
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

	public void applyEndOfSignRules(SignRequest signRequest, User user) throws EsupSignatureException {
		recipientService.validateRecipient(signRequest, user);
		if (isSignRequestCompleted(signRequest)) {
			completeSignRequest(signRequest);
			if (isCurrentStepCompleted(signRequest)) {
				for (Recipient recipient : signRequest.getRecipientHasSigned().keySet()) {
					recipient.setSigned(!signRequest.getRecipientHasSigned().get(recipient).getActionType().equals(ActionType.none));
				}
				if (signBookService.nextWorkFlowStep(signRequest.getParentSignBook())) {
					signBookService.pendingSignBook(signRequest.getParentSignBook(), user);
				} else {
					signBookService.completeSignBook(signRequest.getParentSignBook());
					if (!signRequest.getParentSignBook().getCreateBy().equals("scheduler")) {
						mailService.sendCompletedMail(signRequest.getParentSignBook());
					}
				}
			}
		} else {
			updateStatus(signRequest, SignRequestStatus.pending, "Demande incomplète", "SUCCESS");
		}
	}

	public boolean isCurrentStepCompleted(SignRequest signRequest) {
		return signRequest.getParentSignBook().getSignRequests().stream().allMatch(sr -> sr.getStatus().equals(SignRequestStatus.completed));
	}

	public boolean isSignRequestCompleted(SignRequest signRequest) {
		if (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getAllSignToComplete()) {
			return signRequest.getRecipientHasSigned().keySet().stream().filter(r -> signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients().contains(r)).allMatch(recipient -> !signRequest.getRecipientHasSigned().get(recipient).getActionType().equals(ActionType.none));
		} else {
			return signRequest.getRecipientHasSigned().keySet().stream().filter(r -> signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients().contains(r)).anyMatch(recipient -> !signRequest.getRecipientHasSigned().get(recipient).getActionType().equals(ActionType.none));
		}
	}

	public void sendEmailAlerts(SignRequest signRequest, User user) {
		for (Recipient recipient : signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients()) {
			User recipientUser = recipient.getUser();
			if (!recipientUser.getUserType().equals(UserType.external) && !recipientUser.equals(user) && (recipientUser.getEmailAlertFrequency() == null || recipientUser.getEmailAlertFrequency().equals(EmailAlertFrequency.immediately) || userService.checkEmailAlert(recipientUser))) {
				userService.signRequestService.sendSignRequestEmailAlert(recipientUser, signRequest);
			}
		}
	}

	public List<User> getTempUsersInRecipientList(List<String> recipientsEmails) {
		List<User> tempUsers = new ArrayList<>();
		for (String recipientEmail : recipientsEmails) {
			if(recipientEmail.contains("*")) {
				recipientEmail = recipientEmail.split("\\*")[1];
			}
			User recipientUser = userRepository.findByEmail(recipientEmail).get(0);
			if(recipientUser.getUserType().equals(UserType.external)) {
				tempUsers.add(recipientUser);
//				pending = false;
//				redirectAttributes.addFlashAttribute("message", new JsonMessage("warn", "La liste des destinataires contient des personnes externes.<br>Après vérification, vous devez confirmer l'envoi pour finaliser la demande"));
			}
		}
		return tempUsers;
	}

	public List<User> getTempUsers(SignRequest signRequest, List<String> recipientsEmails) {
		Set<User> users = new HashSet<>();
		users.addAll(getTempUsers(signRequest));
		if(recipientsEmails != null) {
			users.addAll(getTempUsersInRecipientList(recipientsEmails));
		}
		return new ArrayList<>(users);
	}

	public List<User> getTempUsers(SignRequest signRequest) {
		Set<User> users = new HashSet<>();
		if(signRequest.getParentSignBook().getLiveWorkflow().getWorkflowSteps().size() > 0) {
			for (LiveWorkflowStep liveWorkflowStep : signRequest.getParentSignBook().getLiveWorkflow().getWorkflowSteps()) {
				for (Recipient recipient : liveWorkflowStep.getRecipients()) {
					if (recipient.getUser().getUserType().equals(UserType.external) || (recipient.getUser().getEppn().equals(recipient.getUser().getEmail()) && recipient.getUser().getEppn().equals(recipient.getUser().getName()))) {
						users.add(recipient.getUser());
					}
				}
			}
		}
		return new ArrayList<>(users);
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
				subPath = "/" + signRequest.getParentSignBook().getName().split("_")[0].replace(" ", "-") + "/";
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
		log.setSignRequestToken(signRequest.getToken());
		if(user != null) {
			log.setEppn(user.getEppn());
			log.setEppnFor(userService.getCurrentUser().getEppn());
			log.setIp(user.getIp());
		}
		log.setInitialStatus(signRequest.getStatus().toString());
		log.setLogDate(new Date());
		log.setAction(action);
		log.setReturnCode(returnCode);
		if(signRequest.getComment() != null && !signRequest.getComment().isEmpty() && (signRequestStatus == null || signRequestStatus.equals(SignRequestStatus.pending) || signRequestStatus.equals(SignRequestStatus.refused) || pageNumber != null)) {
			log.setComment(signRequest.getComment());
		}
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
		signBookService.refuse(signRequest.getParentSignBook(), signRequest.getComment(), user);
	}

	public boolean needToSign(SignRequest signRequest, User user) {
		return recipientService.needSign(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients(), user);
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
			if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() != null) {
				Optional<Recipient> recipient = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients().stream().filter(r -> r.getUser().equals(user)).findFirst();
				if (recipient.isPresent() && (signRequest.getStatus().equals(SignRequestStatus.pending) || signRequest.getStatus().equals(SignRequestStatus.draft))
						&& !signRequest.getRecipientHasSigned().isEmpty() && signRequest.getRecipientHasSigned().get(recipient.get()).getActionType().equals(ActionType.none)) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean checkUserViewRights(User user, User authUser, SignRequest signRequest) {
		if(user.equals(authUser) || userShareService.checkShare(user, authUser, signRequest)) {
			List<Log> log = logRepository.findByEppnAndSignRequestId(user.getEppn(), signRequest.getId());
			log.addAll(logRepository.findByEppnForAndSignRequestId(user.getEppn(), signRequest.getId()));
			if (signRequest.getCreateBy().equals(user) || log.size() > 0 || recipientService.recipientsContainsUser(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients(), user) > 0) {
				return true;
			}
		}
		return false;
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

	public boolean delete(SignRequest signRequest) {
		List<Log> logs = logRepository.findBySignRequestId(signRequest.getId());
		for (Log log : logs) {
			logRepository.delete(log);
		}
		signRequest.getParentSignBook().getSignRequests().remove(signRequest);
		if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() != null) {
			signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients().clear();
		}
		signRequestRepository.save(signRequest);
		signRequestRepository.delete(signRequest);
		return true;
	}

	public SignType getCurrentSignType(SignRequest signRequest) {
		if(signRequest.getParentSignBook().getLiveWorkflow().getWorkflowSteps() != null) {
			return signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType();
		} else {
			return null;
		}
	}

	public Page<SignRequest> getSignRequestsPageGrouped(List<SignRequest> signRequests, Pageable pageable) {
		List<SignRequest> signRequestsGrouped = new ArrayList<>();
		Map<SignBook, List<SignRequest>> signBookSignRequestMap = signRequests.stream().collect(Collectors.groupingBy(SignRequest::getParentSignBook, Collectors.toList()));
		for(Map.Entry<SignBook, List<SignRequest>> signBookListEntry : signBookSignRequestMap.entrySet()) {
			int last = signBookListEntry.getValue().size() - 1;
			signBookListEntry.getValue().get(last).setViewTitle("");
//			for(SignRequest signRequest : signBookListEntry.getValue()) {
//				signBookListEntry.getValue().get(last).setViewTitle(signBookListEntry.getValue().get(last).getViewTitle() + signRequest.getTitle() + "\n\r");
//			}
			signRequestsGrouped.add(signBookListEntry.getValue().get(last));
		}
		if(pageable.getSort().iterator().hasNext()) {
			Sort.Order order = pageable.getSort().iterator().next();
			SortDefinition sortDefinition = new MutableSortDefinition(order.getProperty(), true, order.getDirection().isAscending());
			Collections.sort(signRequestsGrouped, new PropertyComparator(sortDefinition));
		}
		for(SignRequest signRequest : signRequestsGrouped) {
			if(signRequest.getEndDate() == null) {
				signRequest.setEndDate(getEndDate(signRequest));
			}
		}
		return new PageImpl<>(signRequestsGrouped.stream().skip(pageable.getOffset()).limit(pageable.getPageSize()).collect(Collectors.toList()), pageable, signRequestsGrouped.size());
	}

	private Date getEndDate(SignRequest signRequest) {
		List<Action> action = signRequest.getRecipientHasSigned().values().stream().filter(action1 -> !action1.getActionType().equals(ActionType.none)).sorted(Comparator.comparing(Action::getDate)).collect(Collectors.toList());
		if(action.size() > 0) {
			return action.get(0).getDate();
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
				Workflow workflow = signRequest.getParentSignBook().getLiveWorkflow().getWorkflow();
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

    public void sendSignRequestEmailAlert(User recipientUser, SignRequest signRequest) {
        Date date = new Date();
        Set<String> toEmails = new HashSet<>();
        toEmails.add(recipientUser.getEmail());
		SignBook signBook = signRequest.getParentSignBook();
		List<Data> datas = dataRepository.findBySignBook(signBook);
		Workflow workflow = signBook.getLiveWorkflow().getWorkflow();
		recipientUser.setLastSendAlertDate(date);
		for (UserShare userShare : userShareRepository.findByUser(recipientUser)) {
			if (userShare.getShareTypes().contains(ShareType.sign)) {
				if ((datas.size() > 0 && datas.get(0).getForm().equals(userShare.getForm()))
				|| (workflow != null && workflow.equals(userShare.getWorkflow()))) {
					for (User toUser : userShare.getToUsers()) {
						toEmails.add(toUser.getEmail());
					}
				}
			}
		}
        mailService.sendSignRequestAlert(new ArrayList<>(toEmails), signRequest);
        userRepository.save(recipientUser);
    }


	public void sendEmailAlertSummary(User recipientUser) {
		Date date = new Date();
		List<SignRequest> toSignSignRequests = getToSignRequests(recipientUser);
		toSignSignRequests.addAll(getSharedToSignSignRequests(recipientUser));
		if (toSignSignRequests.size() > 0) {
			recipientUser.setLastSendAlertDate(date);
			mailService.sendSignRequestSummaryAlert(Arrays.asList(recipientUser.getEmail()), toSignSignRequests);
			userRepository.save(recipientUser);
		}
	}
}
