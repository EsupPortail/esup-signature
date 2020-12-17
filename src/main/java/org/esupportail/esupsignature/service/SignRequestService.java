package org.esupportail.esupsignature.service;

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
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureKeystoreException;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.service.event.EventService;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessFactory;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessService;
import org.esupportail.esupsignature.service.interfaces.fs.FsFile;
import org.esupportail.esupsignature.service.interfaces.prefill.PreFillService;
import org.esupportail.esupsignature.service.security.otp.OtpService;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.esupportail.esupsignature.service.utils.mail.MailService;
import org.esupportail.esupsignature.service.utils.metric.CustomMetricsService;
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.esupportail.esupsignature.service.utils.sign.SignService;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.mail.MessagingException;
import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SignRequestService {

	private static final Logger logger = LoggerFactory.getLogger(SignRequestService.class);

	@Resource
	private SignRequestRepository signRequestRepository;

	@Resource
	private ObjectMapper objectMapper;

	@Resource
	private ActionService actionService;

	@Resource
	private GlobalProperties globalProperties;

	@Resource
	private RecipientService recipientService;

	@Autowired(required=false)
	private UserKeystoreService userKeystoreService;

	@Resource
	private PdfService pdfService;

	@Resource
	private DocumentService documentService;

	@Resource
	private CustomMetricsService customMetricsService;

	@Resource
	private SignBookService signBookService;

	@Resource
	private LiveWorkflowStepService liveWorkflowStepService;

	@Resource
	private SignService signService;

	@Resource
	private FileService fileService;

	@Resource
	private UserService userService;

	@Resource
	private UserShareService userShareService;

	@Resource
	private MailService mailService;

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

	@Resource
	private LogService logService;

	@Resource
	private SignRequestParamsService signRequestParamsService;

	@PostConstruct
	public void initSignrequestMetrics() {
		customMetricsService.registerValue("esup-signature.signrequests", "new");
		customMetricsService.registerValue("esup-signature.signrequests", "signed");
	}

	public SignRequest getById(long id) {
		return signRequestRepository.findById(id).get();
	}

	public List<SignRequest> getSignRequestsByToken(String token) {
		return signRequestRepository.findByToken(token);
	}

	public List<SignRequest> getSignRequestsForCurrentUserByStatus(Long userId, Long authUserId, String statusFilter) {
		List<SignRequest> signRequestList = new ArrayList<>();
		List<SignRequest> signRequests = getSignRequestsByStatus(userId, statusFilter);
		if(!userId.equals(authUserId)) {
			for(SignRequest signRequest: signRequests) {
				if(userShareService.checkShare(userId, authUserId, signRequest)) {
					signRequestList.add(signRequest);
				}
			}
		} else {
			signRequestList.addAll(signRequests);
		}
		return signRequestList.stream().sorted(Comparator.comparing(SignRequest::getId)).collect(Collectors.toList());
	}

	public List<SignRequest> getSignRequestsByStatus(Long userId, String statusFilter) {
		Set<SignRequest> signRequests = new HashSet<>();
		if (statusFilter != null) {
			switch (statusFilter) {
				case "tosign":
					signRequests.addAll(getToSignRequests(userId));
					break;
				case "signedByMe":
					signRequests.addAll(getSignRequestsSignedByUser(userId));
					break;
				case "refusedByMe":
					signRequests.addAll(getSignRequestsRefusedByUser(userId));
					break;
				case "followByMe":
					signRequests.addAll(signRequestRepository.findByRecipientUserId(userId));
					signRequests.removeAll(getToSignRequests(userId));
					signRequests.removeAll(getSignRequestsSignedByUser(userId));
					signRequests.removeAll(getSignRequestsRefusedByUser(userId));
					break;
				case "sharedSign":
					signRequests.addAll(getSharedSignedSignRequests(userId));
					break;
				default:
					signRequests.addAll(signRequestRepository.findByCreateByIdAndStatus(userId, SignRequestStatus.valueOf(statusFilter)));
					break;
			}
		} else {
			signRequests.addAll(signRequestRepository.findByCreateById(userId));
			signRequests.addAll(getToSignRequests(userId));
			signRequests.addAll(getSignRequestsSignedByUser(userId));
			signRequests.addAll(getSignRequestsRefusedByUser(userId));
		}
		return new ArrayList<>(signRequests);
	}

	public List<SignRequest> getToSignRequests(Long userId) {
		List<SignRequest> signRequestsToSign = signRequestRepository.findByRecipientUserToSign(userId);
		signRequestsToSign = signRequestsToSign.stream().filter(signRequest -> signRequest.getStatus().equals(SignRequestStatus.pending)).sorted(Comparator.comparing(SignRequest::getCreateDate).reversed()).collect(Collectors.toList());
		return  signRequestsToSign;
	}

	public List<SignRequest> getSignRequestsSignedByUser(Long userId) {
		User user = userService.getById(userId);
		List<Log> logs = new ArrayList<>();
		logs.addAll(logService.getByEppnForAndFinalStatus(user.getEppn(), SignRequestStatus.signed.name()));
		logs.addAll(logService.getByEppnForAndFinalStatus(user.getEppn(), SignRequestStatus.checked.name()));
		return getSignRequestsFromLogs(logs);

	}

	public List<SignRequest> getSignRequestsRefusedByUser(Long userId) {
		User user = userService.getById(userId);
		List<Log> logs = new ArrayList<>(logService.getByEppnForAndFinalStatus(user.getEppn(), SignRequestStatus.refused.name()));
		return getSignRequestsFromLogs(logs);
	}

	private List<SignRequest> getSignRequestsFromLogs(List<Log> logs) {
		List<Long> ids = new ArrayList<>();
		for (Log log : logs) {
			ids.add(log.getSignRequestId());
		}
		return signRequestRepository.findByIdIn(ids);
	}

	public List<SignRequest> getSharedToSignSignRequests(Long userId) {
		List<SignRequest> sharedSignRequests = new ArrayList<>();
		List<SignBook> sharedSignBooks = signBookService.getSharedSignBooks(userId);
		for(SignBook signBook: sharedSignBooks) {
			sharedSignRequests.addAll(signBook.getSignRequests());
		}
		return sharedSignRequests;
	}

	public List<SignRequest> getSharedSignedSignRequests(Long userId) {
		User user = userService.getById(userId);
		List<Log> logs = logService.getByEppn(user.getEppn()).stream().filter(
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

	public SignRequest createSignRequest(String title, SignBook signBook, User user, User authUser) {
		SignRequest signRequest = new SignRequest();
		signRequest.setTitle(title);
		signRequest.setToken(String.valueOf(generateUniqueId()));
		signRequest.setCreateBy(user);
		signRequest.setCreateDate(new Date());
		signRequest.setParentSignBook(signBook);
		signRequest.setStatus(SignRequestStatus.draft);
		signRequestRepository.save(signRequest);
		signBook.getSignRequests().add(signRequest);
		updateStatus(signRequest, SignRequestStatus.draft, "Création de la demande " + title, "SUCCESS", user, authUser);
		return signRequest;
	}

	public void addDocsToSignRequest(SignRequest signRequest, MultipartFile... multipartFiles) throws EsupSignatureIOException {
		for(MultipartFile multipartFile : multipartFiles) {
			try {
				File file = fileService.inputStreamToTempFile(multipartFile.getInputStream(), multipartFile.getName());
				if (multipartFiles.length == 1 && multipartFiles[0].getContentType().equals("application/pdf")) {
					try {
						signRequest.getSignRequestParams().addAll(signRequestParamsService.scanSignatureFields(new FileInputStream(file)));
					} catch (IOException e) {
						throw new EsupSignatureIOException("unable to open multipart inputStream", e);
					}
				}
				String docName = documentService.getFormatedName(multipartFile.getOriginalFilename(), signRequest.getOriginalDocuments().size());
				Document document = documentService.createDocument(new FileInputStream(file), docName, multipartFile.getContentType());
				signRequest.getOriginalDocuments().add(document);
				document.setParentId(signRequest.getId());
				file.delete();
			} catch (IOException e) {
				throw new EsupSignatureIOException("", e);
			}
		}
	}


	public void addAttachmentToSignRequest(SignRequest signRequest, MultipartFile... multipartFiles) throws EsupSignatureIOException {
		for(MultipartFile multipartFile : multipartFiles) {
			try {
				File file = fileService.inputStreamToTempFile(multipartFile.getInputStream(), multipartFile.getName());
				String docName = documentService.getFormatedName("attachement_" + multipartFile.getOriginalFilename(), signRequest.getOriginalDocuments().size());
				Document document = documentService.createDocument(new FileInputStream(file), docName, multipartFile.getContentType());
				signRequest.getAttachments().add(document);
				document.setParentId(signRequest.getId());
				file.delete();
			} catch (IOException e) {
				throw new EsupSignatureIOException("", e);
			}
		}
	}

	public void pendingSignRequest(SignRequest signRequest, User authUser) {
		for (Recipient recipient : signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients()) {
			signRequest.getRecipientHasSigned().put(recipient, actionService.getEmptyAction());
		}
		updateStatus(signRequest, SignRequestStatus.pending, "Envoyé pour signature", "SUCCESS", null, null, null, authUser, authUser);
		customMetricsService.incValue("esup-signature.signrequests", "new");
	}

	@Transactional
	public boolean initSign(Long signRequestId, String sseId, String signRequestParamsJsonString, String comment, String formData, Boolean visual, String password, Long userId, Long authUserId) {
		SignRequest signRequest = getSignRequestsFullById(signRequestId, userId, authUserId);
		Map<String, String> formDataMap = null;
		List<String> toRemoveKeys = new ArrayList<>();
		if(formData != null) {
			try {
				formDataMap = objectMapper.readValue(formData, Map.class);
				formDataMap.remove("_csrf");
				Data data = dataService.getBySignBook(signRequest.getParentSignBook());
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
			List<SignRequestParams> signRequestParamses = signRequestParamsService.getSignRequestParamsFromJson(signRequestParamsJsonString);
			if (signRequest.getCurrentSignType().equals(SignType.nexuSign)) {
				signRequestParamsService.copySignRequestParams(signRequest, signRequestParamses);
				eventService.publishEvent(new JsonMessage("initNexu", "Démarrage de l'application NexU", null), "sign", sseId);
				return true;
			}
			eventService.publishEvent(new JsonMessage("step", "Démarrage de la signature", null), "sign", sseId);
			signRequest.setComment(comment);
			User user = userService.getById(userId);
			User authUser = userService.getById(authUserId);
			sign(signRequest, user, password, visual, signRequestParamses, formDataMap, sseId, authUser);
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

	@Transactional
	public void sign(SignRequest signRequest, User user, String password, boolean visual, List<SignRequestParams> signRequestParamses, Map<String, String> formDataMap, String sseId, User authUser) throws EsupSignatureException, IOException, InterruptedException {
		List<Document> toSignDocuments = getToSignDocuments(signRequest.getId());
		SignType signType = signRequest.getCurrentSignType();
		InputStream filledInputStream;
		if(!signBookService.isNextWorkFlowStep(signRequest.getParentSignBook())) {
			Data data = dataService.getBySignRequest(signRequest);
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
					updateStatus(signRequest, signRequest.getStatus(), "Apposition de la signature",  "SUCCESS", signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos(), signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber(), user, authUser);
				}
			}
			if ((signBookService.isStepAllSignDone(signRequest.getParentSignBook()) && !signBookService.isNextWorkFlowStep(signRequest.getParentSignBook()))) {
				signedInputStream = pdfService.convertGS(pdfService.writeMetadatas(signedInputStream, fileName, signRequest));
			}
			documentService.addSignedFile(signRequest, signedInputStream, fileService.getNameOnly(signRequest.getTitle()) + "." + fileService.getExtension(toSignDocuments.get(0).getFileName()), toSignDocuments.get(0).getContentType());
		} else {
			if (toSignDocuments.size() == 1 && toSignDocuments.get(0).getContentType().equals("application/pdf")) {
				signRequestParamsService.copySignRequestParams(signRequest, signRequestParamses);
				toSignDocuments.get(0).setTransientInputStream(filledInputStream);
			}
			certSign(signRequest, user, password, visual, sseId);
		}
		if (signType.equals(SignType.visa)) {
			if(signRequest.getComment() != null && !signRequest.getComment().isEmpty()) {
				updateStatus(signRequest, SignRequestStatus.checked, "Visa",  "SUCCESS", null, null, null, signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber(), user, authUser);
			} else {
				updateStatus(signRequest, SignRequestStatus.checked, "Visa", "SUCCESS", user, authUser);
			}
		} else {
			if(signRequest.getComment() != null && !signRequest.getComment().isEmpty()) {
				updateStatus(signRequest, SignRequestStatus.signed, "Signature", "SUCCESS", null, null, null, signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber(), user, authUser);
			} else {
				updateStatus(signRequest, SignRequestStatus.signed, "Signature", "SUCCESS", user, authUser);
			}
		}
		eventService.publishEvent(new JsonMessage("step", "Paramétrage de la prochaine étape", null), "sign", sseId);
		applyEndOfSignRules(signRequest, user, authUser);
		customMetricsService.incValue("esup-signature.signrequests", "signed");
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
		for(Document document : getToSignDocuments(signRequest.getId())) {
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

			AbstractSignatureParameters<?> parameters;
			if(signatureForm.equals(SignatureForm.CAdES)) {
				ASiCWithCAdESSignatureParameters aSiCWithCAdESSignatureParameters = new ASiCWithCAdESSignatureParameters();
				aSiCWithCAdESSignatureParameters.aSiC().setContainerType(ASiCContainerType.ASiC_E);
				parameters = aSiCWithCAdESSignatureParameters;
			} else if(signatureForm.equals(SignatureForm.XAdES)) {
				ASiCWithXAdESSignatureParameters aSiCWithXAdESSignatureParameters = new ASiCWithXAdESSignatureParameters();
				aSiCWithXAdESSignatureParameters.aSiC().setContainerType(ASiCContainerType.ASiC_E);
				parameters = aSiCWithXAdESSignatureParameters;
			} else {
				parameters = signService.fillVisibleParameters((SignatureDocumentForm) signatureDocumentForm, signRequest.getCurrentSignRequestParams(), new ByteArrayInputStream(((SignatureDocumentForm) signatureDocumentForm).getDocumentToSign()), user);
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
			if(signatureDocumentForm instanceof SignatureMultipleDocumentsForm) {
				dssDocument = signService.certSignDocument((SignatureMultipleDocumentsForm) signatureDocumentForm, parameters, pkcs12SignatureToken);
			} else {
				dssDocument = signService.certSignDocument((SignatureDocumentForm) signatureDocumentForm, parameters, pkcs12SignatureToken);
			}
			pkcs12SignatureToken.close();
			InMemoryDocument signedPdfDocument = new InMemoryDocument(DSSUtils.toByteArray(dssDocument), dssDocument.getName(), dssDocument.getMimeType());
			eventService.publishEvent(new JsonMessage("step", "Enregistrement du/des documents(s)", null), "sign", sseId);
			documentService.addSignedFile(signRequest, signedPdfDocument.openStream(), fileService.getNameOnly(signRequest.getTitle()) + "." + fileService.getExtension(dssDocument.getName()), signedPdfDocument.getMimeType().getMimeTypeString());
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

	public void applyEndOfSignRules(SignRequest signRequest, User user, User authUser) throws EsupSignatureException {
		recipientService.validateRecipient(signRequest, user);
		if (isSignRequestCompleted(signRequest)) {
			completeSignRequests(Collections.singletonList(signRequest), authUser);
			if (isCurrentStepCompleted(signRequest)) {
				for (Recipient recipient : signRequest.getRecipientHasSigned().keySet()) {
					recipient.setSigned(!signRequest.getRecipientHasSigned().get(recipient).getActionType().equals(ActionType.none));
				}
				if (signBookService.nextWorkFlowStep(signRequest.getParentSignBook())) {
					signBookService.pendingSignBook(signRequest.getParentSignBook(), null, user, authUser);
				} else {
					signBookService.completeSignBook(signRequest.getParentSignBook(), authUser);
					if (!signRequest.getParentSignBook().getCreateBy().equals(userService.getSchedulerUser())) {
						mailService.sendCompletedMail(signRequest.getParentSignBook());
					}
				}
			}
		} else {
			updateStatus(signRequest, SignRequestStatus.pending, "Demande incomplète", "SUCCESS", user, authUser);
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

	public void sendEmailAlerts(SignRequest signRequest, User user, Data data) {
		for (Recipient recipient : signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients()) {
			User recipientUser = recipient.getUser();
			if (!recipientUser.getUserType().equals(UserType.external) && !recipientUser.equals(user) && (recipientUser.getEmailAlertFrequency() == null || recipientUser.getEmailAlertFrequency().equals(EmailAlertFrequency.immediately) || userService.checkEmailAlert(recipientUser))) {
				sendSignRequestEmailAlert(signRequest, recipientUser, data);
			}
		}
	}

	public void completeSignRequest(Long id, User user, User authUser) {
		SignRequest signRequest = getById(id);
		completeSignRequest(signRequest, user, authUser);
	}

	private void completeSignRequest(SignRequest signRequest, User user, User authUser) {
		if (signRequest.getCreateBy().equals(user) && (signRequest.getStatus().equals(SignRequestStatus.signed) || signRequest.getStatus().equals(SignRequestStatus.checked))) {
			completeSignRequests(Arrays.asList(signRequest), authUser);
		} else {
			logger.warn(user.getEppn() + " try to complete " + signRequest.getId() + " without rights");
		}
	}

	public void completeSignRequests(List<SignRequest> signRequests, User authUser) {
		for(SignRequest signRequest : signRequests) {
			updateStatus(signRequest, SignRequestStatus.completed, "Terminé", "SUCCESS", authUser, authUser);
		}
	}

	public void sendSignRequestsToTarget(List<SignRequest> signRequests, String title, DocumentIOType documentIOType, String targetUrl, User authUser) throws EsupSignatureException {
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
					updateStatus(signRequest, SignRequestStatus.exported, "Exporté vers " + targetUrl, "SUCCESS", authUser, authUser);
				}
			}
		}
	}

	public void addPostit(SignRequest signRequest, String comment, User user, User authUser) {
		if(comment != null && !comment.isEmpty()) {
			signRequest.setComment(comment);
			updateStatus(signRequest, signRequest.getStatus(), "comment", "SUCCES", null, null, null, 0, user, authUser);
		}
	}

	public void archiveSignRequests(List<SignRequest> signRequests, User authUser) throws EsupSignatureException {
		if(globalProperties.getArchiveUri() != null) {
			for(SignRequest signRequest : signRequests) {
				Document signedFile = signRequest.getLastSignedDocument();
				String subPath = "/" + signRequest.getParentSignBook().getName().split("_")[0].replace(" ", "-") + "/";
				if(signRequest.getExportedDocumentURI() == null) {
					String documentUri = documentService.archiveDocument(signedFile, globalProperties.getArchiveUri(), subPath);
					signRequest.setExportedDocumentURI(documentUri);
					updateStatus(signRequest, SignRequestStatus.archived, "Exporté vers l'archivage", "SUCCESS", authUser, authUser);

				}
			}
		} else {
			logger.info("archive document was skipped");
		}
	}

	public void cleanDocuments(SignRequest signRequest, User authUser) {
		Date cleanDate = getEndDate(signRequest);
		Calendar cal = Calendar.getInstance();
		cal.setTime(cleanDate);
		cal.add(Calendar.DATE, globalProperties.getDelayBeforeCleaning());
		cleanDate = cal.getTime();
		if (signRequest.getExportedDocumentURI() != null
				&& new Date().after(cleanDate) && signRequest.getSignedDocuments().size() > 0) {
			clearAllDocuments(signRequest);
			updateStatus(signRequest, SignRequestStatus.exported, "Fichiers nettoyés", "SUCCESS", authUser, authUser);
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
				documentService.delete(document);
			}
		}
	}

	public FsFile getLastSignedFsFile(SignRequest signRequest) throws EsupSignatureException {
		if(signRequest.getStatus().equals(SignRequestStatus.exported)) {
			if (signRequest.getExportedDocumentURI() != null && !signRequest.getExportedDocumentURI().startsWith("mail")) {
				FsAccessService fsAccessService = fsAccessFactory.getFsAccessService(signRequest.getExportedDocumentURI());
				return fsAccessService.getFileFromURI(signRequest.getExportedDocumentURI());
			}
		}
		Document lastSignedDocument = getToSignDocuments(signRequest.getId()).get(0);
		return new FsFile(lastSignedDocument.getInputStream(), lastSignedDocument.getFileName(), lastSignedDocument.getContentType());
	}

	public void updateStatus(SignRequest signRequest, SignRequestStatus signRequestStatus, String action, String returnCode, User user, User authUser) {
		updateStatus(signRequest, signRequestStatus, action, returnCode, null, null, null, null, user, authUser);
	}

	public void updateStatus(SignRequest signRequest, SignRequestStatus signRequestStatus, String action, String returnCode, Integer pageNumber, Integer posX, Integer posY, User user, User authUser) {
		updateStatus(signRequest, signRequestStatus, action, returnCode, pageNumber, posX, posY, null, user, authUser);
	}

	public void updateStatus(SignRequest signRequest, SignRequestStatus signRequestStatus, String action, String returnCode, Integer pageNumber, Integer posX, Integer posY, Integer stepNumber, User user, User authUser) {
		logService.create(signRequest, signRequestStatus, action, returnCode, pageNumber, posX, posY, stepNumber, user, authUser);
	}

	public void refuse(SignRequest signRequest, User user, User authUser) {
		signBookService.refuse(signRequest.getParentSignBook(), signRequest.getComment(), user, authUser);
	}

	public boolean needToSign(SignRequest signRequest, Long userId) {
		return recipientService.needSign(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients(), userId);
	}

	public boolean checkUserSignRights(SignRequest signRequest, Long userId, Long authUserId) {
		if(userId.equals(authUserId) || userShareService.checkShare(userId, authUserId, signRequest, ShareType.sign)) {
			if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() != null) {
				Optional<Recipient> recipient = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients().stream().filter(r -> r.getUser().getId().equals(userId)).findFirst();
				if (recipient.isPresent() && (signRequest.getStatus().equals(SignRequestStatus.pending) || signRequest.getStatus().equals(SignRequestStatus.draft))
						&& !signRequest.getRecipientHasSigned().isEmpty() && signRequest.getRecipientHasSigned().get(recipient.get()).getActionType().equals(ActionType.none)) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean checkUserViewRights(SignRequest signRequest, User user, Long authUserId) {
		if(user.getId().equals(authUserId) || userShareService.checkShare(user.getId(), authUserId, signRequest)) {
			List<Log> log = logService.getByEppnAndSignRequestId(user.getEppn(), signRequest.getId());
			log.addAll(logService.getByEppnAndSignRequestId(user.getEppn(), signRequest.getId()));
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
		List<Log> logs = logService.getBySignRequestId(signRequest.getId());
		for (Log log : logs) {
			logService.delete(log);
		}
		signRequest.getParentSignBook().getSignRequests().remove(signRequest);
		if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() != null) {
			signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients().clear();
		}
		signRequestRepository.save(signRequest);
		signRequestRepository.delete(signRequest);
		return true;
	}

	@Transactional
	public Page<SignRequest> getSignRequestsPageGrouped(Long userId, Long authUserId, String statusFilter, Pageable pageable) {
		List<SignRequest> signRequests = getSignRequestsForCurrentUserByStatus(userId, authUserId, statusFilter);
		List<SignRequest> signRequestsGrouped = new ArrayList<>();
		Map<SignBook, List<SignRequest>> signBookSignRequestMap = signRequests.stream().collect(Collectors.groupingBy(SignRequest::getParentSignBook, Collectors.toList()));
		for(Map.Entry<SignBook, List<SignRequest>> signBookListEntry : signBookSignRequestMap.entrySet()) {
			int last = signBookListEntry.getValue().size() - 1;
			signBookListEntry.getValue().get(last).setViewTitle("");
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

	public void sendSignRequestEmailAlert(SignRequest signRequest, User recipientUser, Data data) {
		Date date = new Date();
		Set<String> toEmails = new HashSet<>();
		toEmails.add(recipientUser.getEmail());
		SignBook signBook = signRequest.getParentSignBook();
		Workflow workflow = signBook.getLiveWorkflow().getWorkflow();
		recipientUser.setLastSendAlertDate(date);
		if(data != null) {
			for (UserShare userShare : userShareService.getUserSharesByUser(recipientUser.getId())) {
				if (userShare.getShareTypes().contains(ShareType.sign)) {
					if ((data.getForm().equals(userShare.getForm())) || (workflow != null && workflow.equals(userShare.getWorkflow()))) {
						for (User toUser : userShare.getToUsers()) {
							toEmails.add(toUser.getEmail());
						}
					}
				}
			}
		}
		mailService.sendSignRequestAlert(new ArrayList<>(toEmails), signRequest);
	}


	public void sendEmailAlertSummary(User recipientUser) {
		Date date = new Date();
		List<SignRequest> toSignSignRequests = getToSignRequests(recipientUser.getId());
		toSignSignRequests.addAll(getSharedToSignSignRequests(recipientUser.getId()));
		if (toSignSignRequests.size() > 0) {
			recipientUser.setLastSendAlertDate(date);
			mailService.sendSignRequestSummaryAlert(Arrays.asList(recipientUser.getEmail()), toSignSignRequests);
		}
	}

	@Transactional
	public SignRequest getSignRequestsFullById(long id, Long userId, Long authUserId) {
		SignRequest signRequest = getById(id);
		if (signRequest.getStatus().equals(SignRequestStatus.pending)
				&& checkUserSignRights(signRequest, userId, authUserId) && signRequest.getOriginalDocuments().size() > 0
				&& needToSign(signRequest, userId)) {
			signRequest.setSignable(true);
		}
		return signRequest;
	}

	public boolean checkTempUsers(Long id, List<String> recipientEmails, String[] names, String[] firstnames, String[] phones) throws MessagingException {
		SignRequest signRequest = getById(id);
		List<User> tempUsers = userService.getTempUsers(signRequest, recipientEmails);
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

	@Transactional
	public List<Field> prefillSignRequestFields(Long signRequestId, Long userId) {
		User user = userService.getById(userId);
		SignRequest signRequest = getById(signRequestId);
		List<Field> prefilledFields = new ArrayList<>();
		Data data = dataService.getBySignBook(signRequest.getParentSignBook());
		if(data != null) {
			if(data.getForm() != null) {
				List<Field> fields = data.getForm().getFields();
				if (!"".equals(data.getForm().getPreFillType())) {
					prefilledFields = preFillService.getPreFilledFieldsByServiceName(data.getForm().getPreFillType(), fields, user);
					for (Field field : prefilledFields) {
						if (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() == null || !field.getStepNumbers().contains(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().toString())) {
							field.setDefaultValue("");
						}
						if (data.getDatas().get(field.getName()) != null && !data.getDatas().get(field.getName()).isEmpty()) {
							field.setDefaultValue(data.getDatas().get(field.getName()));
						}
					}
				} else {
					prefilledFields = data.getForm().getFields();
				}
			}
		}
		return prefilledFields;
	}

	public InputStream getLastFileBase64(Long id) throws SQLException, EsupSignatureException {
		SignRequest signRequest = getById(id);
		InputStream inputStream = null;
		if (!signRequest.getStatus().equals(SignRequestStatus.exported)) {
			List<Document> documents = getToSignDocuments(id);
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
		SignRequest signRequest = getById(id);
		Document attachement = documentService.getById(attachementId);
		if (!attachement.getParentId().equals(signRequest.getId())) {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Pièce jointe non trouvée ..."));
		} else {
			signRequest.getAttachments().remove(attachement);
			documentService.delete(attachement);
		}
	}

	public void removeLink(Long id, Integer linkId) {
		SignRequest signRequest = getById(id);
		String toRemove = signRequest.getLinks().get(linkId);
		signRequest.getLinks().remove(toRemove);
	}

	public void addComment(Long id, String comment, Integer commentPageNumber, Integer commentPosX, Integer commentPosY, User authUser) {
		SignRequest signRequest = getById(id);
		signRequest.setComment(comment);
		updateStatus(signRequest, null, "Ajout d'un commentaire", "SUCCESS", commentPageNumber, commentPosX, commentPosY, authUser, authUser);
	}

	public void addStep(Long id, String[] recipientsEmails, SignType signType, Boolean allSignToComplete) {
		SignRequest signRequest = getById(id);
		liveWorkflowStepService.addRecipientsToWorkflowStep(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep(), recipientsEmails);
		signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().setSignType(signType);
		if (allSignToComplete != null && allSignToComplete) {
			signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().setAllSignToComplete(true);
		} else {
			signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().setAllSignToComplete(false);
		}
	}

	@Transactional
	public Map<SignBook, String> sendSignRequest(MultipartFile[] multipartFiles, String[] recipientsEmails, Boolean allSignToComplete, Boolean userSignFirst, Boolean pending, String comment, SignType signType, User user, User authUser) throws EsupSignatureException, EsupSignatureIOException {
		SignBook signBook = signBookService.addDocsInNewSignBookSeparated("", "Demande simple", multipartFiles, user);
		return signBookService.sendSignBook(user, signBook, recipientsEmails, allSignToComplete, userSignFirst, pending, comment, signType, authUser);
	}

	public SignRequest getNextSignRequest(Long signRequestId, Long userid, Long authUserId) {
		List<SignRequest> toSignRequests = getSignRequestsForCurrentUserByStatus(userid, authUserId, "tosign");
		Optional<SignRequest> signRequest = toSignRequests.stream().filter(signRequest1 -> signRequest1.getId().equals(signRequestId)).findFirst();
		if(signRequest.isPresent()) {
			if (toSignRequests.size() > 0) {
				if (!toSignRequests.contains(signRequest.get())) {
					return toSignRequests.get(0);
				} else {
					if (toSignRequests.size() > 1) {
						int indexOfCurrentSignRequest = toSignRequests.indexOf(signRequest.get());
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
		}
		return null;
	}

	public SignRequest getPreviousSignRequest(Long signRequestId, Long userId, Long authUserId) {
		List<SignRequest> toSignRequests = getSignRequestsForCurrentUserByStatus(userId, authUserId, "tosign");
		Optional<SignRequest> signRequest = toSignRequests.stream().filter(signRequest1 -> signRequest1.getId().equals(signRequestId)).findFirst();
		if(signRequest.isPresent()) {
			if (toSignRequests.size() > 0) {
				if (toSignRequests.size() > 1) {
					int indexOfCurrentSignRequest = toSignRequests.indexOf(signRequest.get());
					if (indexOfCurrentSignRequest > -1) {
						if (indexOfCurrentSignRequest == 0) {
							return toSignRequests.get(toSignRequests.size() - 1);
						} else if (indexOfCurrentSignRequest == toSignRequests.size() - 1) {
							return toSignRequests.get(indexOfCurrentSignRequest - 1);
						} else {
							return toSignRequests.get(indexOfCurrentSignRequest - 1);
						}
					}
				}
			}
		}
		return null;
	}

	@Transactional
	public List<String> getSignImageForSignRequest(SignRequest signRequestRef, Long userId, Long authUserId) throws EsupSignatureUserException, IOException {
		SignRequest signRequest = getSignRequestsFullById(signRequestRef.getId(), userId, authUserId);
		signRequestRef.setSignable(signRequest.getSignable());
		User user = userService.getById(userId);
		List<String> signImages = new ArrayList<>();
		if (signRequest.getSignedDocuments().size() > 0 || signRequest.getOriginalDocuments().size() > 0) {
			List<Document> toSignDocuments = getToSignDocuments(signRequest.getId());
			if (toSignDocuments.size() == 1 && toSignDocuments.get(0).getContentType().equals("application/pdf")) {
				if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().equals(SignType.visa)) {
					signImages.add(fileService.getBase64Image(SignRequestService.class.getResourceAsStream("/sceau.png"), "sceau.png"));
				} else {
					if (user.getSignImages().size() > 0 && user.getSignImages().get(0) != null && user.getSignImages().get(0).getSize() > 0) {
						if (checkUserSignRights(signRequest, userId, authUserId) && user.getKeystore() == null && signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().equals(SignType.certSign)) {
							signRequestRef.setSignable(false);
							throw new EsupSignatureUserException("Pour signer ce document merci d’ajouter un certificat à votre profil");
						}
						for (Document signImage : user.getSignImages()) {
							signImages.add(fileService.getBase64Image(signImage));
						}
					} else {
						if (signRequest.getSignable() && signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType() != null && (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().equals(SignType.pdfImageStamp) || signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().equals(SignType.certSign))) {
							signRequestRef.setSignable(false);
							throw new EsupSignatureUserException("Pour signer ce document merci d'ajouter une image de votre signature dans <a href='user/users' target='_blank'>Mes paramètres</a>");

						}
					}
				}
			}
		}
		return signImages;
	}

	public AbstractMap.SimpleEntry<List<User>, List<User>> checkUserResponse(SignRequest signRequest) {
		List<User> usersHasSigned = new ArrayList<>();
		List<User> usersHasRefused = new ArrayList<>();
		for(Map.Entry<Recipient, Action> recipientActionEntry : signRequest.getRecipientHasSigned().entrySet()) {
			if (recipientActionEntry.getValue().getActionType().equals(ActionType.signed)) {
				usersHasSigned.add(recipientActionEntry.getKey().getUser());
			}
			if (recipientActionEntry.getValue().getActionType().equals(ActionType.refused)) {
				usersHasRefused.add(recipientActionEntry.getKey().getUser());
			}
		}
		return new AbstractMap.SimpleEntry<>(usersHasRefused, usersHasSigned);
	}

	public int getNbByCreateAndStatus(Long userId) {
		return signRequestRepository.findByCreateByIdAndStatus(userId, SignRequestStatus.pending).size();
	}

	public Map<String, Object> getAttachmentResponse(Long signRequestId, Long attachementId) throws SQLException, IOException {
		SignRequest signRequest = getById(signRequestId);
		Document attachement = documentService.getById(attachementId);
		if (attachement != null && attachement.getParentId().equals(signRequest.getId())) {
			return fileService.getFileResponse(attachement.getBigFile().getBinaryFile().getBinaryStream().readAllBytes(), attachement.getFileName(), attachement.getContentType());
		}
		return null;
	}

	@Transactional
	public Map<String, Object> getToSignFileResponse(Long signRequestId) throws SQLException, EsupSignatureException, IOException {
		SignRequest signRequest = getById(signRequestId);
		if (!signRequest.getStatus().equals(SignRequestStatus.exported)) {
			List<Document> documents = getToSignDocuments(signRequest.getId());
			if (documents.size() > 1) {
				return null;
			} else {
				return fileService.getFileResponse(documents.get(0).getBigFile().getBinaryFile().getBinaryStream().readAllBytes(), documents.get(0).getFileName(), documents.get(0).getContentType());
			}
		} else {
			FsFile fsFile = getLastSignedFsFile(signRequest);
			return fileService.getFileResponse(fsFile.getInputStream().readAllBytes(), fsFile.getName(), fsFile.getContentType());
		}
	}

	@Transactional
	public List<Document> getToSignDocuments(Long signRequestId) {
		SignRequest signRequest = getById(signRequestId);
		List<Document> documents = new ArrayList<>();
		if(signRequest.getSignedDocuments() != null && signRequest.getSignedDocuments().size() > 0 ) {
			documents.add(signRequest.getLastSignedDocument());
		} else {
			documents.addAll(signRequest.getOriginalDocuments());
		}
		return documents;
	}

	@Transactional
	public List<Document> getAttachments(Long signRequestId) {
		SignRequest signRequest = getById(signRequestId);
		return new ArrayList<>(signRequest.getAttachments());
	}
}
