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
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
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
	private CommentService commentService;

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

	public List<SignRequest> getSignRequestsForCurrentUserByStatus(String userEppn, String authUserEppn, String statusFilter) {
		List<SignRequest> signRequestList = new ArrayList<>();
		List<SignRequest> signRequests = getSignRequestsByStatus(userEppn, statusFilter);
		if(!userEppn.equals(authUserEppn)) {
			for(SignRequest signRequest: signRequests) {
				if(userShareService.checkShare(userEppn, authUserEppn, signRequest) || getSharedSignedSignRequests(authUserEppn).contains(signRequest)) {
					signRequestList.add(signRequest);
				}
			}
		} else {
			signRequestList.addAll(signRequests);
		}
		return signRequestList.stream().sorted(Comparator.comparing(SignRequest::getId)).collect(Collectors.toList());
	}

	public boolean isUserInRecipients(SignRequest signRequest, String userEppn) {
		boolean isInRecipients = false;
		Set<Recipient> recipients = signRequest.getRecipientHasSigned().keySet();
		for(Recipient recipient : recipients) {
			if (recipient.getUser().getEppn().equals(userEppn)) {
				isInRecipients = true;
				break;
			}
		}
		return isInRecipients;
	}

	public List<SignRequest> getSignRequestsByStatus(String userEppn, String statusFilter) {
		Set<SignRequest> signRequests = new HashSet<>();
		if (statusFilter != null) {
			switch (statusFilter) {
				case "tosign":
					signRequests.addAll(getToSignRequests(userEppn));
					break;
				case "signedByMe":
					signRequests.addAll(getSignRequestsSignedByUser(userEppn));
					break;
				case "refusedByMe":
					signRequests.addAll(getSignRequestsRefusedByUser(userEppn));
					break;
				case "followByMe":
					signRequests.addAll(signRequestRepository.findByRecipient(userEppn));
					break;
				case "sharedSign":
					signRequests.addAll(getSharedSignedSignRequests(userEppn));
					break;
				default:
					signRequests.addAll(signRequestRepository.findByCreateByEppnAndStatus(userEppn, SignRequestStatus.valueOf(statusFilter)));
					break;
			}
		} else {
			signRequests.addAll(signRequestRepository.findByCreateByEppn(userEppn));
			signRequests.addAll(getToSignRequests(userEppn));
			signRequests.addAll(getSignRequestsSignedByUser(userEppn));
			signRequests.addAll(getSignRequestsRefusedByUser(userEppn));
		}
		return new ArrayList<>(signRequests);
	}

	public Long nbToSignSignRequests(String userEppn) {
		return signRequestRepository.countByRecipientUserToSign(userEppn);
	}

	public List<SignRequest> getToSignRequests(String userEppn) {
		List<SignRequest> signRequestsToSign = signRequestRepository.findByRecipientUserToSign(userEppn);
		signRequestsToSign = signRequestsToSign.stream().sorted(Comparator.comparing(SignRequest::getCreateDate).reversed()).collect(Collectors.toList());
		return  signRequestsToSign;
	}

	private List<SignRequest> getSignRequestsFromLogs(List<Log> logs) {
		List<Long> ids = new ArrayList<>();
		for (Log log : logs) {
			ids.add(log.getSignRequestId());
		}
		return signRequestRepository.findByIdIn(ids);
	}

	public List<SignRequest> getSharedToSignSignRequests(String userEppn) {
		List<SignRequest> sharedSignRequests = new ArrayList<>();
		List<SignBook> sharedSignBooks = signBookService.getSharedSignBooks(userEppn);
		for(SignBook signBook: sharedSignBooks) {
			sharedSignRequests.addAll(signBook.getSignRequests());
		}
		return sharedSignRequests;
	}

	public List<SignRequest> getSharedSignedSignRequests(String userEppn) {
		User user = userService.getByEppn(userEppn);
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

	public SignRequest createSignRequest(String title, SignBook signBook, String userEppn, String authUserEppn) {
		User user = userService.getUserByEppn(userEppn);
		SignRequest signRequest = new SignRequest();
		signRequest.setTitle(title);
		signRequest.setToken(String.valueOf(generateUniqueId()));
		signRequest.setCreateBy(user);
		signRequest.setCreateDate(new Date());
		signRequest.setParentSignBook(signBook);
		signRequest.setStatus(SignRequestStatus.draft);
		signRequestRepository.save(signRequest);
		signBook.getSignRequests().add(signRequest);
		updateStatus(signRequest, SignRequestStatus.draft, "Création de la demande " + title, "SUCCESS", userEppn, authUserEppn);
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

	public void pendingSignRequest(SignRequest signRequest, String authUserEppn) {
		for (Recipient recipient : signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients()) {
			signRequest.getRecipientHasSigned().put(recipient, actionService.getEmptyAction());
		}
		updateStatus(signRequest, SignRequestStatus.pending, "Envoyé pour signature", "SUCCESS", null, null, null, authUserEppn, authUserEppn);
		customMetricsService.incValue("esup-signature.signrequests", "new");
	}

	@Transactional
	public boolean initSign(Long signRequestId, String sseId, String signRequestParamsJsonString, String comment, String formData, Boolean visual, String password, Long userShareId, String userEppn, String authUserEppn) {
		SignRequest signRequest = getSignRequestsFullById(signRequestId, userEppn, authUserEppn);
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
			List<SignRequestParams> signRequestParamses;
			if (signRequestParamsJsonString == null) {
				signRequestParamses = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams();
			} else {
				signRequestParamses = signRequestParamsService.getSignRequestParamsFromJson(signRequestParamsJsonString);
			}
			if (signRequest.getCurrentSignType().equals(SignType.nexuSign)) {
				signRequestParamsService.copySignRequestParams(signRequest, signRequestParamses);
				eventService.publishEvent(new JsonMessage("initNexu", "Démarrage de l'application NexU", null), "sign", sseId);
				return true;
			}
			eventService.publishEvent(new JsonMessage("step", "Démarrage de la signature", null), "sign", sseId);
			User user = userService.getByEppn(userEppn);
			User authUser = userService.getByEppn(authUserEppn);
			sign(signRequest, password, visual, signRequestParamses, formDataMap, sseId, user, authUser, userShareId, comment);
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
				public void afterCommit(){
					eventService.publishEvent(new JsonMessage("end", "Signature terminée", null), "sign", sseId);
				}
			});
			return true;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			eventService.publishEvent(new JsonMessage("sign_system_error", e.getMessage(), null), "sign", sseId);
//			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
		}
		return false;
	}

	@Transactional
	public void sign(SignRequest signRequest, String password, boolean visual, List<SignRequestParams> signRequestParamses, Map<String, String> formDataMap, String sseId, User user, User authUser, Long userShareId, String comment) throws EsupSignatureException, IOException, InterruptedException {
		User signerUser = user;
		if(userShareId != null) {
			UserShare userShare = userShareService.getById(userShareId);
			if (userShare.getUser().getEppn().equals(user.getEppn()) && userShare.getSignWithOwnSign() != null && userShare.getSignWithOwnSign()) {
				signerUser = userService.getByEppn(authUser.getEppn());
			}
		}
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
					signedInputStream = pdfService.stampImage(signedInputStream, signRequest, signRequestParams, signerUser);
					updateStatus(signRequest, signRequest.getStatus(), "Apposition de la signature",  "SUCCESS", signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos(), signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber(), user.getEppn(), authUser.getEppn());
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
			certSign(signRequest, signerUser, password, visual, sseId);
		}
		if (signType.equals(SignType.visa)) {
			if(comment != null && !comment.isEmpty()) {
				commentService.create(signRequest.getId(), comment, 0, 0, 0, null, true, user.getEppn());
				updateStatus(signRequest, SignRequestStatus.checked, "Visa",  "SUCCESS", null, null, null, signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber(), user.getEppn(), authUser.getEppn());
			} else {
				updateStatus(signRequest, SignRequestStatus.checked, "Visa", "SUCCESS", user.getEppn(), authUser.getEppn());
			}
		} else {
			if(comment != null && !comment.isEmpty()) {
				commentService.create(signRequest.getId(), comment, 0, 0, 0, null, true, user.getEppn());
				updateStatus(signRequest, SignRequestStatus.signed, "Signature", "SUCCESS", null, null, null, signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber(), user.getEppn(), authUser.getEppn());
			} else {
				updateStatus(signRequest, SignRequestStatus.signed, "Signature", "SUCCESS", user.getEppn(), authUser.getEppn());
			}
		}
		eventService.publishEvent(new JsonMessage("step", "Paramétrage de la prochaine étape", null), "sign", sseId);
		applyEndOfSignRules(signRequest, user.getEppn(), authUser.getEppn());
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
			eventService.publishEvent(new JsonMessage("step", "Déverouillage du keystore", null), "sign", sseId);
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
				parameters = signService.fillVisibleParameters((SignatureDocumentForm) signatureDocumentForm, signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams().get(0), new ByteArrayInputStream(((SignatureDocumentForm) signatureDocumentForm).getDocumentToSign()), user);
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

	public void applyEndOfSignRules(SignRequest signRequest, String userEppn, String authUserEppn) throws EsupSignatureException {
		recipientService.validateRecipient(signRequest, userEppn);
		if (isSignRequestCompleted(signRequest)) {
			completeSignRequests(Collections.singletonList(signRequest), authUserEppn);
			if (isCurrentStepCompleted(signRequest)) {
				for (Recipient recipient : signRequest.getRecipientHasSigned().keySet()) {
					recipient.setSigned(!signRequest.getRecipientHasSigned().get(recipient).getActionType().equals(ActionType.none));
				}
				if (signBookService.nextWorkFlowStep(signRequest.getParentSignBook())) {
					signBookService.pendingSignBook(signRequest.getParentSignBook(), null, userEppn, authUserEppn);
				} else {
					signBookService.completeSignBook(signRequest.getParentSignBook(), authUserEppn);
					if (!signRequest.getParentSignBook().getCreateBy().equals(userService.getSchedulerUser())) {
						mailService.sendCompletedMail(signRequest.getParentSignBook());
					}
				}
			}
		} else {
			updateStatus(signRequest, SignRequestStatus.pending, "Demande incomplète", "SUCCESS", userEppn, authUserEppn);
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

	public void sendEmailAlerts(SignRequest signRequest, String userEppn, Data data) {
		for (Recipient recipient : signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients()) {
			User recipientUser = recipient.getUser();
			if (!UserType.external.equals(recipientUser.getUserType()) 
			&& !recipientUser.getEppn().equals(userEppn) 
			&& (recipientUser.getEmailAlertFrequency() == null 
			|| recipientUser.getEmailAlertFrequency().equals(EmailAlertFrequency.immediately) 
			|| userService.checkEmailAlert(recipientUser))) {
				sendSignRequestEmailAlert(signRequest, recipientUser, data);
			}
		}
	}

	public void completeSignRequest(Long id, String userEppn, String authUserEppn) {
		SignRequest signRequest = getById(id);
		completeSignRequest(signRequest, userEppn, authUserEppn);
	}

	private void completeSignRequest(SignRequest signRequest, String userEppn, String authUserEppn) {
		if (signRequest.getCreateBy().getEppn().equals(userEppn) && (signRequest.getStatus().equals(SignRequestStatus.signed) || signRequest.getStatus().equals(SignRequestStatus.checked))) {
			completeSignRequests(Arrays.asList(signRequest), authUserEppn);
		} else {
			logger.warn(userEppn + " try to complete " + signRequest.getId() + " without rights");
		}
	}

	public void completeSignRequests(List<SignRequest> signRequests, String authUserEppn) {
		for(SignRequest signRequest : signRequests) {
			updateStatus(signRequest, SignRequestStatus.completed, "Terminé", "SUCCESS", authUserEppn, authUserEppn);
		}
	}

	public void sendSignRequestsToTarget(List<SignRequest> signRequests, String title, DocumentIOType documentIOType, String targetUrl, String authUserEppn) throws EsupSignatureException {
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
					if(signRequest.getAttachments().size() > 0) {
						targetUrl += "/" + signRequest.getTitle();
						for(Document attachment : signRequest.getAttachments()) {
							documentService.exportDocument(documentIOType, targetUrl, attachment);
						}
					}
					documentService.exportDocument(documentIOType, targetUrl, signedFile);
					updateStatus(signRequest, SignRequestStatus.exported, "Exporté vers " + targetUrl, "SUCCESS", authUserEppn, authUserEppn);
				}
			}
		}
	}

	public void addPostit(Long signRequestId, String comment, String userEppn, String authUserEppn) {
		SignRequest signRequest = getById(signRequestId);
				if(comment != null && !comment.isEmpty()) {
			updateStatus(signRequest, signRequest.getStatus(), "comment", comment, "SUCCES", null, null, null, 0, userEppn, authUserEppn);
		}
	}

	public void archiveSignRequests(List<SignRequest> signRequests, String authUserEppn) throws EsupSignatureException {
		if(globalProperties.getArchiveUri() != null) {
			for(SignRequest signRequest : signRequests) {
				Document signedFile = signRequest.getLastSignedDocument();
				String subPath = "/" + signRequest.getParentSignBook().getName().split("_")[0].replace(" ", "-") + "/";
				if(signRequest.getExportedDocumentURI() == null) {
					String documentUri = documentService.archiveDocument(signedFile, globalProperties.getArchiveUri(), subPath);
					signRequest.setExportedDocumentURI(documentUri);
					updateStatus(signRequest, SignRequestStatus.archived, "Exporté vers l'archivage", "SUCCESS", authUserEppn, authUserEppn);

				}
			}
		} else {
			logger.info("archive document was skipped");
		}
	}

	public void cleanDocuments(SignRequest signRequest, String authUserEppn) {
		Date cleanDate = getEndDate(signRequest);
		Calendar cal = Calendar.getInstance();
		cal.setTime(cleanDate);
		cal.add(Calendar.DATE, globalProperties.getDelayBeforeCleaning());
		cleanDate = cal.getTime();
		if (signRequest.getExportedDocumentURI() != null
				&& new Date().after(cleanDate) && signRequest.getSignedDocuments().size() > 0) {
			clearAllDocuments(signRequest);
			updateStatus(signRequest, SignRequestStatus.exported, "Fichiers nettoyés", "SUCCESS", authUserEppn, authUserEppn);
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

	public void updateStatus(SignRequest signRequest, SignRequestStatus signRequestStatus, String action, String returnCode, String userEppn, String authUserEppn) {
		updateStatus(signRequest, signRequestStatus, action, returnCode, null, null, null, null, userEppn, authUserEppn);
	}

	public void updateStatus(SignRequest signRequest, SignRequestStatus signRequestStatus, String action, String returnCode, Integer pageNumber, Integer posX, Integer posY, String userEppn, String authUserEppn) {
		updateStatus(signRequest, signRequestStatus, action, returnCode, pageNumber, posX, posY, null, userEppn, authUserEppn);
	}

	public void updateStatus(SignRequest signRequest, SignRequestStatus signRequestStatus, String action, String returnCode, Integer pageNumber, Integer posX, Integer posY, Integer stepNumber, String userEppn, String authUserEppn) {
		logService.create(signRequest, signRequestStatus, action, null, returnCode, pageNumber, posX, posY, stepNumber, userEppn, authUserEppn);
	}

	public void updateStatus(SignRequest signRequest, SignRequestStatus signRequestStatus, String action, String comment, String returnCode, Integer pageNumber, Integer posX, Integer posY, Integer stepNumber, String userEppn, String authUserEppn) {
		logService.create(signRequest, signRequestStatus, action, comment, returnCode, pageNumber, posX, posY, stepNumber, userEppn, authUserEppn);
	}

	@Transactional
	public void refuse(Long signRequestId, String comment, String userEppn, String authUserEppn) {
		SignRequest signRequest = getById(signRequestId);
		signBookService.refuse(signRequest.getParentSignBook(), comment, userEppn, authUserEppn);
	}

	public boolean needToSign(SignRequest signRequest, String userEppn) {
		return recipientService.needSign(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients(), userEppn);
	}

	public boolean checkUserSignRights(SignRequest signRequest, String userEppn, String authUserEppn) {
		if(userEppn.equals(authUserEppn) || userShareService.checkShare(userEppn, authUserEppn, signRequest, ShareType.sign)) {
			if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() != null) {
				Optional<Recipient> recipient = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients().stream().filter(r -> r.getUser().getEppn().equals(userEppn)).findFirst();
				if (recipient.isPresent()
					&& (signRequest.getStatus().equals(SignRequestStatus.pending) || signRequest.getStatus().equals(SignRequestStatus.draft))
					&& !signRequest.getRecipientHasSigned().isEmpty()
					&& signRequest.getRecipientHasSigned().get(recipient.get()).getActionType().equals(ActionType.none)) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean checkUserViewRights(SignRequest signRequest, String userEppn, String authUserEppn) {
		if(userEppn.equals(authUserEppn) || userShareService.checkShare(userEppn, authUserEppn, signRequest)) {
			List<SignRequest> signRequests = signRequestRepository.findByIdAndRecipient(signRequest.getId(), userEppn);
			if (signRequest.getCreateBy().getEppn().equals(userEppn) || signRequests.size() > 0 ) {
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

	@Transactional
	public boolean delete(Long signRequestId) {
		SignRequest signRequest = getById(signRequestId);
//		List<Log> logs = logService.getBySignRequestId(signRequest.getId());
//		for (Log log : logs) {
//			logService.delete(log);
//		}
		List<Comment> comments = signRequest.getComments();
		for( Comment comment : comments) {
			commentService.deleteComment(comment.getId());
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
	public Page<SignRequest> getSignRequestsPageGrouped(String userEppn, String authUserEppn, String statusFilter, String recipientsFilter, String workflowFilter, String docTitleFilter, Pageable pageable) {
		List<SignRequest> signRequests = getSignRequestsForCurrentUserByStatus(userEppn, authUserEppn, statusFilter);
		if (recipientsFilter != null) {
			List<SignRequest> signRequestByRecipients = signRequestRepository.findByRecipient(recipientsFilter);
			signRequests.retainAll(signRequestByRecipients);
		}
		if (workflowFilter != null) {
			List<SignRequest> signRequestByWorkflow = signRequestRepository.findByParentSignBookTitle(workflowFilter);
			signRequests.retainAll(signRequestByWorkflow);
		}
		if (docTitleFilter != null) {
			List<SignRequest> signRequestByTitle = signRequestRepository.findByTitle(docTitleFilter);
			signRequests.retainAll(signRequestByTitle);
		}
		List<SignRequest> signRequestsGrouped = new ArrayList<>();
		Map<SignBook, List<SignRequest>> signBookSignRequestMap = signRequests.stream().collect(Collectors.groupingBy(SignRequest::getParentSignBook, Collectors.toList()));
		for(Map.Entry<SignBook, List<SignRequest>> signBookListEntry : signBookSignRequestMap.entrySet()) {
			int last = signBookListEntry.getValue().size() - 1;
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
			for (UserShare userShare : userShareService.getUserSharesByUser(recipientUser.getEppn())) {
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
		List<SignRequest> toSignSignRequests = getToSignRequests(recipientUser.getEppn());
		toSignSignRequests.addAll(getSharedToSignSignRequests(recipientUser.getEppn()));
		if (toSignSignRequests.size() > 0) {
			recipientUser.setLastSendAlertDate(date);
			mailService.sendSignRequestSummaryAlert(Arrays.asList(recipientUser.getEmail()), toSignSignRequests);
		}
	}

	@Transactional
	public SignRequest getSignRequestsFullById(long id, String userEppn, String authUserEppn) {
		SignRequest signRequest = getById(id);
		if (signRequest.getStatus().equals(SignRequestStatus.pending)
				&& checkUserSignRights(signRequest, userEppn, authUserEppn)
				&& signRequest.getOriginalDocuments().size() > 0
				&& needToSign(signRequest, userEppn)) {
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
	public List<Field> prefillSignRequestFields(Long signRequestId, String userEppn) {
		User user = userService.getByEppn(userEppn);
		SignRequest signRequest = getById(signRequestId);
		List<Field> prefilledFields = new ArrayList<>();
		Data data = dataService.getBySignBook(signRequest.getParentSignBook());
		if(data != null) {
			if(data.getForm() != null) {
				List<Field> fields = data.getForm().getFields();
				if (!"".equals(data.getForm().getPreFillType())) {
					prefilledFields = preFillService.getPreFilledFieldsByServiceName(data.getForm().getPreFillType(), fields, user);
					for (Field field : prefilledFields) {
						if (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() == null || !field.getStepNumbers().contains(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber().toString())) {
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

	@Transactional
	public Document getLastSignedFile(Long signRequestId) {
		SignRequest signRequest = getById(signRequestId);
		return signRequest.getSignedDocuments().get(signRequest.getSignedDocuments().size() - 1);
	}

	@Transactional
	public void addAttachement(MultipartFile[] multipartFiles, String link, Long signRequestId) throws EsupSignatureIOException {
		SignRequest signRequest = getById(signRequestId);
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

	@Transactional
	public void addComment(Long id, String commentText, Integer commentPageNumber, Integer commentPosX, Integer commentPosY, Integer spotStepNumber, String authUserEppn) {
		SignRequest signRequest = getById(id);
		if(spotStepNumber != null && spotStepNumber > 0) {
			SignRequestParams signRequestParams = signRequestParamsService.createSignRequestParams(commentPageNumber, commentPosX, commentPosY);
			signRequest.getSignRequestParams().add(signRequestParams);
			List<SignRequestParams> signRequestParamsList = new ArrayList<>();
			signRequestParamsList.add(signRequestParams);
			signRequest.getParentSignBook().getLiveWorkflow().getLiveWorkflowSteps().get(spotStepNumber - 1).setSignRequestParams(signRequestParamsList);
		}
		commentService.create(id, commentText, commentPosX, commentPosY, commentPageNumber, spotStepNumber, false, authUserEppn);
		if(!(spotStepNumber != null && spotStepNumber > 0)) {
			updateStatus(signRequest, null, "Ajout d'un commentaire", commentText, "SUCCESS", commentPageNumber, commentPosX, commentPosY, null, authUserEppn, authUserEppn);
		} else {
			updateStatus(signRequest, null, "Ajout d'un emplacement de signature", commentText, "SUCCESS", commentPageNumber, commentPosX, commentPosY, null, authUserEppn, authUserEppn);
		}
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
		return signBookService.sendSignBook(signBook, recipientsEmails, allSignToComplete, userSignFirst, pending, comment, signType, user, authUser);
	}

	public SignRequest getNextSignRequest(Long signRequestId, String userEppn, String authUserEppn) {
		List<SignRequest> toSignRequests = getSignRequestsForCurrentUserByStatus(userEppn, authUserEppn, "tosign");
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

	public SignRequest getPreviousSignRequest(Long signRequestId, String userEppn, String authUserEppn) {
		List<SignRequest> toSignRequests = getSignRequestsForCurrentUserByStatus(userEppn, authUserEppn, "tosign");
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
	public List<String> getSignImagesForSignRequest(SignRequest signRequestRef, String userEppn, String authUserEppn, Long userShareId) throws EsupSignatureUserException, IOException {
		SignRequest signRequest = getSignRequestsFullById(signRequestRef.getId(), userEppn, authUserEppn);
		signRequestRef.setSignable(signRequest.getSignable());
		List<String> signImages = new ArrayList<>();
		if (signRequest.getSignedDocuments().size() > 0 || signRequest.getOriginalDocuments().size() > 0) {
			List<Document> toSignDocuments = getToSignDocuments(signRequest.getId());
			if (toSignDocuments.size() == 1 && toSignDocuments.get(0).getContentType().equals("application/pdf")) {
				if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().equals(SignType.visa)) {
					signImages.add(fileService.getBase64Image(SignRequestService.class.getResourceAsStream("/sceau.png"), "sceau.png"));
				} else {
					User user = userService.getByEppn(userEppn);
					if(userShareId != null) {
						UserShare userShare = userShareService.getById(userShareId);
						if (userShare.getUser().getEppn().equals(userEppn) && userShare.getSignWithOwnSign() != null && userShare.getSignWithOwnSign()) {
							user = userService.getByEppn(authUserEppn);
						}
					}
					if (user.getSignImages().size() > 0 && user.getSignImages().get(0) != null && user.getSignImages().get(0).getSize() > 0) {
						if (checkUserSignRights(signRequest, userEppn, authUserEppn) && user.getKeystore() == null && signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().equals(SignType.certSign)) {
							signRequestRef.setSignable(false);
							throw new EsupSignatureUserException("Pour signer ce document merci d’ajouter un certificat à votre profil <a href='user/users' target='_blank'>Mes paramètres</a>");
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

	public Long getNbByCreateAndStatus(String userEppn) {
		return signRequestRepository.countByCreateByEppnAndStatus(userEppn, SignRequestStatus.pending);
	}

	@Transactional
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

	public List<SignRequest> getSignRequestsSignedByUser(String eppn) {
		return signRequestRepository.findByRecipientAndActionType(eppn, ActionType.signed);
	}

	private List<SignRequest> getSignRequestsRefusedByUser(String userEppn) {
		return signRequestRepository.findByRecipientAndActionType(userEppn, ActionType.refused);
	}

	public void replayNotif(Long id) {
		SignRequest signRequest = this.getById(id);
		List<String> recipientEmails = new ArrayList<>();
		signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients().stream().filter(r -> !r.getSigned()).collect(Collectors.toList()).forEach(r -> recipientEmails.add(r.getUser().getEmail()));
		mailService.sendSignRequestAlert(recipientEmails, signRequest);
	}

	public SignRequest getSignRequestByComment(Comment comment) {
		return signRequestRepository.findSignRequestByCommentsContains(comment);
	}

	public List<Recipient> getRecipientsNameFromSignRequestPage(Page<SignRequest> signRequests) {
		List<Recipient> recipientNames = new ArrayList<>();
		for (SignRequest signRequest : signRequests) {
			recipientNames.addAll(signRequest.getRecipientHasSigned().keySet());
		}
		return recipientNames.stream().filter(distinctByKey(r -> r.getUser().getId())).collect( Collectors.toList() );
	}

	public static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor)
	{
		Map<Object, Boolean> map = new ConcurrentHashMap<>();
		return t -> map.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
	}
}
