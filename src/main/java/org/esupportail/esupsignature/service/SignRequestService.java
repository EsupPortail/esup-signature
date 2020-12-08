package org.esupportail.esupsignature.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import eu.europa.esig.dss.token.Pkcs12SignatureToken;
import org.apache.commons.codec.binary.Base64;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dss.model.AbstractSignatureForm;
import org.esupportail.esupsignature.dss.model.SignatureDocumentForm;
import org.esupportail.esupsignature.dss.model.SignatureMultipleDocumentsForm;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.*;
import org.esupportail.esupsignature.exception.*;
import org.esupportail.esupsignature.repository.*;
import org.esupportail.esupsignature.service.event.EventService;
import org.esupportail.esupsignature.service.file.FileService;
import org.esupportail.esupsignature.service.fs.FsAccessFactory;
import org.esupportail.esupsignature.service.fs.FsAccessService;
import org.esupportail.esupsignature.service.fs.FsFile;
import org.esupportail.esupsignature.service.mail.MailService;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.esupportail.esupsignature.service.prefill.PreFillService;
import org.esupportail.esupsignature.service.security.otp.OtpService;
import org.esupportail.esupsignature.service.sign.SignService;
import org.esupportail.esupsignature.service.utils.CustomMetricsService;
import org.esupportail.esupsignature.web.controller.ws.json.JsonMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.support.MutableSortDefinition;
import org.springframework.beans.support.PropertyComparator;
import org.springframework.beans.support.SortDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.sql.SQLException;
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

	@Autowired(required=false)
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
	private LiveWorkflowService liveWorkflowService;

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

	@Resource
	private OtpService otpService;

	@Resource
	private PreFillService preFillService;

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
		return signRequestList.stream().sorted(Comparator.comparing(SignRequest::getId)).collect(Collectors.toList());
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

	public SignRequest initCreateSignRequest(User user, MultipartFile[] multipartFiles, SignType signType) throws EsupSignatureException {
		if (checkSignTypeDocType(signType, multipartFiles[0])) {
			try {
				SignBook signBook = addDocsInSignBook(user, "", "Signature simple", multipartFiles);
				signBook.getLiveWorkflow().getWorkflowSteps().add(liveWorkflowService.createWorkflowStep(false, signType, user.getEmail()));
				signBook.getLiveWorkflow().setCurrentStep(signBook.getLiveWorkflow().getWorkflowSteps().get(0));
				signBookService.pendingSignBook(signBook, user);
				return signBook.getSignRequests().get(0);
			} catch (EsupSignatureUserException | EsupSignatureIOException e) {
				TransactionInterceptor.currentTransactionStatus().setRollbackOnly();
				throw new EsupSignaturePdfException("Impossible de charger le document : documents corrompu", e);
			}
		} else {
			throw new EsupSignatureException("Impossible de demander une signature visuelle sur un document du type " + multipartFiles[0].getContentType());
		}
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

	public void pendingSignRequest(SignRequest signRequest) {
		for (Recipient recipient : signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients()) {
			Action action = new Action();
			actionRepository.save(action);
			signRequest.getRecipientHasSigned().put(recipient, action);
		}
		updateStatus(signRequest, SignRequestStatus.pending, "Envoyé pour signature", "SUCCESS", null, null, null);
		customMetricsService.incValue("esup-signature.signrequests", "new");
	}

	public boolean initSign(User user, Long id, String sseId, String signRequestParamsJsonString, String comment, String formData, Boolean visual, String password) {
		SignRequest signRequest = getSignRequestsFullById(id, user, user);
		Map<String, String> formDataMap = null;
		List<String> toRemoveKeys = new ArrayList<>();
		ObjectMapper objectMapper = new ObjectMapper();
		if(formData != null) {
			try {
				formDataMap = objectMapper.readValue(formData, Map.class);
				formDataMap.remove("_csrf");
				Data data = dataService.getDataFromSignBook(signRequest.getParentSignBook());
				if(data != null) {
					List<Field> fields = data.getForm().getFields();
					for(Map.Entry<String, String> entry : formDataMap.entrySet()) {
						List<Field> formfields = fields.stream().filter(f -> f.getName().equals(entry.getKey())).collect(Collectors.toList());
						if(formfields.size() > 0) {
							if(!formfields.get(0).getExtValueType().equals("system")) {
								List<String> steps = Arrays.asList(formfields.get(0).getStepNumbers().split(" "));
								if (!data.getDatas().containsKey(entry.getKey()) || steps.contains(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber().toString())) {
									data.getDatas().put(entry.getKey(), entry.getValue());
								}
							} else {
								toRemoveKeys.add(entry.getKey());
							}
						}
					}
				}
			} catch (IOException e) {
				logger.error("form datas error", e);
			}
		}
		for (String toRemoveKey : toRemoveKeys) {
			formDataMap.remove(toRemoveKey);
		}
		try {
			List<SignRequestParams> signRequestParamses = Arrays.asList(objectMapper.readValue(signRequestParamsJsonString, SignRequestParams[].class));
			if (signRequest.getCurrentSignType().equals(SignType.nexuSign)) {
				eventService.publishEvent(new JsonMessage("initNexu", "Démarrage de l'application NexU", null), "sign", sseId);
				return true;
			}
			eventService.publishEvent(new JsonMessage("step", "Démarrage de la signature", null), "sign", sseId);
			signRequest.setComment(comment);
			sign(signRequest, user, password, visual, signRequestParamses, formDataMap, sseId);
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
				public void afterCommit(){
					eventService.publishEvent(new JsonMessage("end", "Signature terminée", null), "sign", sseId);
				}
			});
			return true;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			eventService.publishEvent(new JsonMessage("sign_system_error", e.getMessage(), null), "sign", sseId);
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
		}
		return false;
	}

	public void sign(SignRequest signRequest, User user, String password, boolean visual, List<SignRequestParams> signRequestParamses, Map<String, String> formDataMap, String sseId) throws EsupSignatureException, IOException, InterruptedException {
		List<Document> toSignDocuments = signRequest.getToSignDocuments();
		SignType signType = signRequest.getCurrentSignType();
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
			eventService.publishEvent(new JsonMessage("step", "Remplissage du document", null), "sign", sseId);
			filledInputStream = pdfService.fill(toSignDocuments.get(0).getInputStream(), formDataMap);
		} else {
			filledInputStream = toSignDocuments.get(0).getInputStream();
		}

		if(signType.equals(SignType.visa) || signType.equals(SignType.pdfImageStamp)) {
			InputStream signedInputStream = filledInputStream;
			String fileName = toSignDocuments.get(0).getFileName();

			if (toSignDocuments.size() == 1 && toSignDocuments.get(0).getContentType().equals("application/pdf") && visual) {
				eventService.publishEvent(new JsonMessage("step", "Apposition de la signature", null), "sign", sseId);
				for(SignRequestParams signRequestParams : signRequestParamses) {
					signedInputStream = pdfService.stampImage(signedInputStream, signRequest, signRequestParams, user);
					updateStatus(signRequest, signRequest.getStatus(), "Apposition de la signature",  "SUCCESS", signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos(), signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber());
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
			certSign(signRequest, user, password, visual, sseId);
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
		eventService.publishEvent(new JsonMessage("step", "Paramétrage de la prochaine étape", null), "sign", sseId);
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
		signRequestParams.setAddExtra(signRequestParamses.get(0).isAddExtra());
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

	public void certSign(SignRequest signRequest, User user, String password, boolean visual, String sseId) throws EsupSignatureException, InterruptedException {
		SignatureForm signatureForm;
		List<Document> toSignDocuments = new ArrayList<>();
		for(Document document : signRequest.getToSignDocuments()) {
			toSignDocuments.add(document);
		}
		eventService.publishEvent(new JsonMessage("step", "Initialisation de la procédure", null), "sign", sseId);
		Pkcs12SignatureToken pkcs12SignatureToken = null;
		try {
			eventService.publishEvent(new JsonMessage("mail", "Déverouillage du keystore", null), "sign", sseId);
			pkcs12SignatureToken = userKeystoreService.getPkcs12Token(user.getKeystore().getInputStream(), password);
			CertificateToken certificateToken = userKeystoreService.getCertificateToken(pkcs12SignatureToken);
			CertificateToken[] certificateTokenChain = userKeystoreService.getCertificateTokenChain(pkcs12SignatureToken);
			eventService.publishEvent(new JsonMessage("step", "Formatage des documents", null), "sign", sseId);
			AbstractSignatureForm signatureDocumentForm = signService.getSignatureDocumentForm(toSignDocuments, signRequest, visual);
			signatureForm = signatureDocumentForm.getSignatureForm();
			signatureDocumentForm.setEncryptionAlgorithm(EncryptionAlgorithm.RSA);

			eventService.publishEvent(new JsonMessage("step", "Préparation de la signature", null), "sign", sseId);

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
				eventService.publishEvent(new JsonMessage("step", "Signature du document", null), "sign", sseId);
			} else {
				eventService.publishEvent(new JsonMessage("step", "Signature des documents", null), "sign", sseId);
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
			eventService.publishEvent(new JsonMessage("step", "Enregistrement du/des documents(s)", null), "sign", sseId);
			addSignedFile(signRequest, signedPdfDocument.openStream(), fileService.getNameOnly(signRequest.getTitle()) + "." + fileService.getExtension(dssDocument.getName()), signedPdfDocument.getMimeType().getMimeTypeString());
		} catch (EsupSignatureKeystoreException e) {
			eventService.publishEvent(new JsonMessage("sign_system_error", "Mauvais mot de passe", null), "sign", sseId);
			if(pkcs12SignatureToken != null) pkcs12SignatureToken.close();
			throw new EsupSignatureKeystoreException(e.getMessage(), e);
		} catch (Exception e) {
			eventService.publishEvent(new JsonMessage("sign_system_error", e.getMessage(), null), "sign", sseId);
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
			completeSignRequest(signRequest, user);
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
				sendSignRequestEmailAlert(recipientUser, signRequest);
			}
		}
	}

	public List<User> getTempUsersFromRecipientList(List<String> recipientsEmails) {
		List<User> tempUsers = new ArrayList<>();
		for (String recipientEmail : recipientsEmails) {
			if(recipientEmail.contains("*")) {
				recipientEmail = recipientEmail.split("\\*")[1];
			}
			User recipientUser = userRepository.findByEmail(recipientEmail).get(0);
			if(recipientUser.getUserType().equals(UserType.external)) {
				tempUsers.add(recipientUser);
			}
		}
		return tempUsers;
	}

	public boolean isTempUsers(SignRequest signRequest) {
		boolean isTempUsers = false;
		if(getTempUsers(signRequest).size() > 0) {
			isTempUsers = true;
		}
		return isTempUsers;
	}

	public List<User> getTempUsers(Long id) {
		SignRequest signRequest = getSignRequestsById(id);
		return getTempUsers(signRequest);
	}


	public List<User> getTempUsers(SignRequest signRequest, List<String> recipientsEmails) {
		Set<User> users = new HashSet<>();
		users.addAll(getTempUsers(signRequest));
		if(recipientsEmails != null) {
			users.addAll(getTempUsersFromRecipientList(recipientsEmails));
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

	public void completeSignRequest(Long id, User user) {
		SignRequest signRequest = getSignRequestsById(id);
		completeSignRequest(signRequest, user);
	}

	private void completeSignRequest(SignRequest signRequest, User user) {
		if (signRequest.getCreateBy().equals(user.getEppn()) && (signRequest.getStatus().equals(SignRequestStatus.signed) || signRequest.getStatus().equals(SignRequestStatus.checked))) {
			completeSignRequests(Arrays.asList(signRequest));
		} else {
			logger.warn(user.getEppn() + " try to complete " + signRequest.getId() + " without rights");
		}
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
					Document signedFile = signRequest.getLastSignedDocument();
					documentService.exportDocument(documentIOType, targetUrl, signedFile);
					updateStatus(signRequest, SignRequestStatus.exported, "Exporté vers " + targetUrl, "SUCCESS");
				}
			}
		}
	}

	public void archiveSignRequests(List<SignRequest> signRequests) throws EsupSignatureException {
		if(globalProperties.getArchiveUri() != null) {
			for(SignRequest signRequest : signRequests) {
				Document signedFile = signRequest.getLastSignedDocument();
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
		Document lastSignedDocument = signRequest.getToSignDocuments().get(0);
		return new FsFile(lastSignedDocument.getInputStream(), lastSignedDocument.getFileName(), lastSignedDocument.getContentType());
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

	public SignRequest getSignRequestsFullById(long id, User user, User authUser) {
		SignRequest signRequest = getSignRequestsById(id);
		if (signRequest.getStatus().equals(SignRequestStatus.pending)
				&& checkUserSignRights(user, authUser, signRequest) && signRequest.getOriginalDocuments().size() > 0
				&& needToSign(signRequest, user)) {
			signRequest.setSignable(true);
		}
		return signRequest;
	}

	public SignRequest getSignRequestsById(long id) {
		return signRequestRepository.findById(id).get();
	}

	public List<SignRequest> getSignRequestsByToken(String token) {
		return signRequestRepository.findByToken(token);
	}

	public boolean checkTempUsers(Long id, List<String> recipientEmails, String[] names, String[] firstnames, String[] phones) throws MessagingException {
		SignRequest signRequest = getSignRequestsById(id);
		List<User> tempUsers = getTempUsers(signRequest, recipientEmails);
		if(tempUsers.size() > 0) {
			if (names != null && tempUsers.size() == names.length) {
				int userNumber = 0;
				for (User tempUser : tempUsers) {
					if (tempUser.getUserType().equals(UserType.shib)) {
						logger.warn("TODO Envoi Mail SHIBBOLETH ");
						//TODO envoi mail spécifique
					} else if (tempUser.getUserType().equals(UserType.external)) {
						tempUser.setFirstname(firstnames[userNumber]);
						tempUser.setName(names[userNumber]);
						tempUser.setEppn(phones[userNumber]);
						otpService.generateOtpForSignRequest(id, tempUser);
					}
					userNumber++;
				}
			} else {
				return false;
			}
		}
		return true;
	}

	public List<Field> prefillSignRequestFields(SignRequest signRequest, User user) {
		List<Field> prefilledFields = new ArrayList<>();
		Data data = dataService.getDataFromSignBook(signRequest.getParentSignBook());
		if(data != null) {
			if(data.getForm() != null) {
				List<Field> fields = data.getForm().getFields();
				prefilledFields = preFillService.getPreFilledFieldsByServiceName(data.getForm().getPreFillType(), fields, user);
				for (Field field : prefilledFields) {
					if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() == null || !field.getStepNumbers().contains(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().toString())) {
						field.setDefaultValue("");
					}
					if(data.getDatas().get(field.getName()) != null && !data.getDatas().get(field.getName()).isEmpty()) {
						field.setDefaultValue(data.getDatas().get(field.getName()));
					}
				}
			}
		}
		return prefilledFields;
	}

	public InputStream getLastFileBase64(Long id) throws SQLException, EsupSignatureException {
		SignRequest signRequest = getSignRequestsById(id);
		InputStream inputStream = null;
		if (!signRequest.getStatus().equals(SignRequestStatus.exported)) {
			List<Document> documents = signRequest.getToSignDocuments();
			if (documents.size() == 1) {
				inputStream = documents.get(0).getBigFile().getBinaryFile().getBinaryStream();
			}
		} else {
			FsFile fsFile = getLastSignedFsFile(signRequest);
			inputStream = fsFile.getInputStream();
		}
		return inputStream;
	}

	public void addAttachement(MultipartFile[] multipartFiles, String link, SignRequest signRequest) throws EsupSignatureIOException {
		if(multipartFiles != null && multipartFiles.length > 0) {
			for (MultipartFile multipartFile : multipartFiles) {
				if(multipartFile.getSize() > 0) {
					addAttachmentToSignRequest(signRequest, multipartFile);
				}
			}
		}
		if(link != null && !link.isEmpty()) {
			signRequest.getLinks().add(link);
		}
	}

	public void removeAttachement(Long id, Long attachementId, RedirectAttributes redirectAttributes) {
		SignRequest signRequest = getSignRequestsById(id);
		Document attachement = documentService.getDocumentById(attachementId);
		if (!attachement.getParentId().equals(signRequest.getId())) {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Pièce jointe non trouvée ..."));
		} else {
			signRequest.getAttachments().remove(attachement);
			documentService.delete(attachement);
		}
	}

	public void removeLink(Long id, Integer linkId) {
		SignRequest signRequest = getSignRequestsById(id);
		String toRemove = signRequest.getLinks().get(linkId);
		signRequest.getLinks().remove(toRemove);
	}

	public void addComment(Long id, String comment, Integer commentPageNumber, Integer commentPosX, Integer commentPosY) {
		SignRequest signRequest = getSignRequestsById(id);
		signRequest.setComment(comment);
		updateStatus(signRequest, null, "Ajout d'un commentaire", "SUCCESS", commentPageNumber, commentPosX, commentPosY);
	}

	public void addStep(Long id, String[] recipientsEmails, SignType signType, Boolean allSignToComplete) {
		SignRequest signRequest = getSignRequestsById(id);
		addRecipients(signRequest, recipientsEmails);
		signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().setSignType(signType);
		if (allSignToComplete != null && allSignToComplete) {
			signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().setAllSignToComplete(true);
		} else {
			signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().setAllSignToComplete(false);
		}
	}


	public SignRequest getNextSignRequest(SignRequest signRequest, User user, User authUser) {
		List<SignRequest> toSignRequests = getSignRequestsForCurrentUserByStatus(user, authUser, "tosign");
		if(toSignRequests.size() > 0) {
			if (!toSignRequests.contains(signRequest)) {
				return toSignRequests.get(0);
			} else {
				if(toSignRequests.size() > 1) {
					int indexOfCurrentSignRequest = toSignRequests.indexOf(signRequest);
					if (indexOfCurrentSignRequest == 0) {
						return toSignRequests.get(indexOfCurrentSignRequest + 1);
					} else if (indexOfCurrentSignRequest == toSignRequests.size() - 1) {
						return toSignRequests.get(0);
					} else {
						return toSignRequests.get(indexOfCurrentSignRequest + 1);
					}
				}
			}
		}
		return null;
	}

	public SignRequest getPreviousSignRequest(SignRequest signRequest, User user, User authUser) {
		List<SignRequest> toSignRequests = getSignRequestsForCurrentUserByStatus(user, authUser, "tosign");
		if(toSignRequests.size() > 0) {
			if(toSignRequests.size() > 1) {
				int indexOfCurrentSignRequest = toSignRequests.indexOf(signRequest);
				if (indexOfCurrentSignRequest == 0) {
					return toSignRequests.get(toSignRequests.size() - 1);
				} else if (indexOfCurrentSignRequest == toSignRequests.size() - 1) {
					return toSignRequests.get(indexOfCurrentSignRequest - 1);
				} else {
					return toSignRequests.get(indexOfCurrentSignRequest - 1);
				}
			}
		}
		return null;
	}

	public List<String> getSignImageForSignRequest(SignRequest signRequest, User user, User authUser) throws EsupSignatureUserException, IOException {
		List<String> signImages = new ArrayList<>();
		if (signRequest.getSignedDocuments().size() > 0 || signRequest.getOriginalDocuments().size() > 0) {
			List<Document> toSignDocuments = signRequest.getToSignDocuments();
			if (toSignDocuments.size() == 1 && toSignDocuments.get(0).getContentType().equals("application/pdf")) {
				if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().equals(SignType.visa)) {
					signImages.add(fileService.getBase64Image(SignRequestService.class.getResourceAsStream("/sceau.png"), "sceau.png"));
				} else {
					if (user.getSignImages().size() > 0 && user.getSignImages().get(0) != null && user.getSignImages().get(0).getSize() > 0) {
						if (checkUserSignRights(user, authUser, signRequest) && user.getKeystore() == null && signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().equals(SignType.certSign)) {
							signRequest.setSignable(false);
							throw new EsupSignatureUserException("Pour signer ce document merci d’ajouter un certificat à votre profil");
						}
						for (Document signImage : user.getSignImages()) {
							signImages.add(fileService.getBase64Image(signImage));
						}
					} else {
						if (signRequest.getSignable() && signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType() != null && (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().equals(SignType.pdfImageStamp) || signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().equals(SignType.certSign))) {
							signRequest.setSignable(false);
							throw new EsupSignatureUserException("Pour signer ce document merci d'ajouter une image de votre signature dans <a href='user/users' target='_blank'>Mes paramètres</a>");

						}
					}
				}
			}
		}
		return signImages;
	}

}

