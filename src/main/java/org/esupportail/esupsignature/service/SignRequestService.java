package org.esupportail.esupsignature.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.zxing.WriterException;
import eu.europa.esig.dss.validation.reports.Reports;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.ActionType;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureMailException;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.repository.DataRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessFactoryService;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessService;
import org.esupportail.esupsignature.service.interfaces.fs.FsFile;
import org.esupportail.esupsignature.service.interfaces.prefill.PreFillService;
import org.esupportail.esupsignature.service.mail.MailService;
import org.esupportail.esupsignature.service.security.otp.OtpService;
import org.esupportail.esupsignature.service.utils.WebUtilsService;
import org.esupportail.esupsignature.service.utils.metric.CustomMetricsService;
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.esupportail.esupsignature.service.utils.sign.SignService;
import org.esupportail.esupsignature.service.utils.sign.ValidationService;
import org.esupportail.esupsignature.web.ws.json.JsonExternalUserInfo;
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.support.MutableSortDefinition;
import org.springframework.beans.support.PropertyComparator;
import org.springframework.beans.support.SortDefinition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@EnableConfigurationProperties(GlobalProperties.class)
public class SignRequestService {

	private static final Logger logger = LoggerFactory.getLogger(SignRequestService.class);

	private final GlobalProperties globalProperties;

	public SignRequestService(GlobalProperties globalProperties) {
		this.globalProperties = globalProperties;
	}

	@Resource
	private WebUtilsService webUtilsService;

	@Resource
	private SignRequestRepository signRequestRepository;

	@Resource
	private ActionService actionService;

	@Resource
	private PdfService pdfService;

	@Resource
	private DocumentService documentService;

	@Resource
	private CustomMetricsService customMetricsService;

	@Resource
	private SignService signService;

	@Resource
	private UserService userService;

	@Resource
	private DataService dataService;

	@Resource
	private CommentService commentService;

	@Resource
	private MailService mailService;

	@Resource
	private FsAccessFactoryService fsAccessFactoryService;

	@Resource
	private DataRepository dataRepository;

	@Resource
	private OtpService otpService;

	@Resource
	private PreFillService preFillService;

	@Resource
	private LogService logService;

	@Resource
	private SignRequestParamsService signRequestParamsService;

	@Resource
	private ValidationService validationService;

	@Resource
	private ObjectMapper objectMapper;

	@PostConstruct
	public void initSignrequestMetrics() {
		customMetricsService.registerValue("esup-signature.signrequests", "new");
		customMetricsService.registerValue("esup-signature.signrequests", "signed");
	}

	public SignRequest getById(long id) {
		Optional<SignRequest> signRequest = signRequestRepository.findById(id);
		if(signRequest.isPresent()) {
			Data data = dataService.getBySignBook(signRequest.get().getParentSignBook());
			if (data != null) {
				signRequest.get().setData(data);
			}
			return signRequest.get();
		}
		return null;
	}

	public String getStatus(long id) {
		SignRequest signRequest = getById(id);
		if(signRequest != null){
			return signRequest.getStatus().name();
		} else {
			List<Log> logs = logService.getBySignRequest(id);
			if(logs.size() > 0) {
				return "fully-deleted";
			}
		}
		return null;
	}

	public SignRequest getSignRequestByToken(String token) {
		return signRequestRepository.findByToken(token);
	}

	public Page<SignRequest> getSignRequestsByForm(Form form, String statusFilter, String recipientsFilter, String docTitleFilter, String creatorFilter, String dateFilter, Pageable pageable) {
		List<SignRequest> signRequests = new ArrayList<>();
		List<Data> datas = dataRepository.findByFormId(form.getId());
		for(Data data : datas) {
			if(data.getSignBook() != null && data.getSignBook().getSignRequests().size() > 0) {
				signRequests.add(data.getSignBook().getSignRequests().get(0));
			}
		}
		if(!statusFilter.equals("%")) {
			signRequests = signRequests.stream().filter(signRequest -> signRequest.getStatus().equals(SignRequestStatus.valueOf(statusFilter))).collect(Collectors.toList());
		}
		if(!creatorFilter.equals("%")) {
			signRequests = signRequests.stream().filter(signRequest -> signRequest.getCreateBy().getEppn().equals(creatorFilter)).collect(Collectors.toList());
		}
		if(!recipientsFilter.equals("%")) {
			signRequests = signRequests.stream().filter(signRequest -> signRequest.getRecipientHasSigned().keySet().stream().anyMatch(recipient -> recipient.getUser().getEppn().equals(recipientsFilter))).collect(Collectors.toList());
		}
		if(dateFilter != null && !dateFilter.isEmpty()) {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			Date formattedDate = null;
			try {
				formattedDate = formatter.parse(dateFilter);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			LocalDateTime nowLocalDateTime = formattedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			LocalDateTime startLocalDateTime = nowLocalDateTime.with(LocalTime.of(0, 0, 0));
			LocalDateTime endLocalDateTime = nowLocalDateTime.with(LocalTime.of(23, 59, 59));
			Date startDateFilter = Timestamp.valueOf(startLocalDateTime);
			Date endDateFilter = Timestamp.valueOf(endLocalDateTime);
			signRequests = signRequests.stream().filter(signRequest -> signRequest.getCreateDate().after(startDateFilter) && signRequest.getCreateDate().before(endDateFilter)).collect(Collectors.toList());
		}
		if(pageable.getSort().iterator().hasNext()) {
			Sort.Order order = pageable.getSort().iterator().next();
			SortDefinition sortDefinition = new MutableSortDefinition(order.getProperty(), true, order.getDirection().isAscending());
			PropertyComparator<SignRequest> propertyComparator = new PropertyComparator<>(sortDefinition);
			signRequests.sort(propertyComparator);
		}
		return new PageImpl<>(signRequests.stream().skip(pageable.getOffset()).limit(pageable.getPageSize()).collect(Collectors.toList()), pageable, signRequests.size());
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

	public SignRequest createSignRequest(String name, SignBook signBook, String userEppn, String authUserEppn) {
		User user = userService.getUserByEppn(userEppn);
		SignRequest signRequest = new SignRequest();
		if(name == null || name.isEmpty()) {
			if (signBook.getSignRequests().size() == 0) {
				signRequest.setTitle(signBook.getSubject());
			} else {
				signRequest.setTitle(signBook.getSubject() + "_" + signBook.getSignRequests().size());
			}
		} else {
			signRequest.setTitle(name);
		}
		signRequest.setToken(UUID.randomUUID().toString());
		signRequest.setCreateBy(user);
		signRequest.setCreateDate(new Date());
		signRequest.setParentSignBook(signBook);
		signRequest.setStatus(SignRequestStatus.draft);
		signRequestRepository.save(signRequest);
		signBook.getSignRequests().add(signRequest);
		updateStatus(signRequest.getId(), SignRequestStatus.draft, "Création de la demande " + signBook.getId(), "SUCCESS", userEppn, authUserEppn);
		return signRequest;
	}

	public void addDocsToSignRequest(SignRequest signRequest, boolean scanSignatureFields, int docNumber, List<SignRequestParams> signRequestParamses, MultipartFile... multipartFiles) throws EsupSignatureIOException {
		for(MultipartFile multipartFile : multipartFiles) {
			try {
				byte[] bytes = multipartFile.getInputStream().readAllBytes();
				String contentType = multipartFile.getContentType();
				InputStream inputStream = new ByteArrayInputStream(bytes);
				if (multipartFiles.length == 1 && bytes.length > 0) {
					if("application/pdf".equals(multipartFiles[0].getContentType()) && scanSignatureFields) {
						bytes = pdfService.normalizeGS(bytes);
						List<SignRequestParams> toAddSignRequestParams = new ArrayList<>();
						if(signRequestParamses.size() == 0) {
							toAddSignRequestParams = signRequestParamsService.scanSignatureFields(new ByteArrayInputStream(bytes), docNumber);

						} else {
							for (SignRequestParams signRequestParams : signRequestParamses) {
								toAddSignRequestParams.add(signRequestParamsService.createSignRequestParams(signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos()));
							}
						}
						signRequest.getSignRequestParams().addAll(toAddSignRequestParams);
						Reports reports = validationService.validate(new ByteArrayInputStream(bytes), null);
						if(reports == null || reports.getSimpleReport().getSignatureIdList().size() == 0) {
							inputStream = pdfService.removeSignField(new ByteArrayInputStream(bytes));
						}
					} else if(contentType != null && contentType.contains("image")){
						bytes = pdfService.jpegToPdf(multipartFile.getInputStream(), multipartFile.getName()).readAllBytes();
						contentType = "application/pdf";
						inputStream = new ByteArrayInputStream(bytes);
					}
					Document document = documentService.createDocument(inputStream, multipartFile.getOriginalFilename(), contentType);
					signRequest.getOriginalDocuments().add(document);
					document.setParentId(signRequest.getId());
				} else {
					logger.warn("file size is 0");
					throw new EsupSignatureIOException("Erreur lors de l'ajout des fichiers");
				}
			} catch (IOException e) {
				logger.warn("error on adding files", e);
				throw new EsupSignatureIOException("Erreur lors de l'ajout des fichiers", e);
			} catch (EsupSignatureRuntimeException e) {
				logger.warn("error on converting files", e);
				throw new EsupSignatureIOException("Erreur lors de la conversion du document", e);
			}
		}
	}

	public void addAttachmentToSignRequest(SignRequest signRequest, MultipartFile... multipartFiles) throws EsupSignatureIOException {
		for(MultipartFile multipartFile : multipartFiles) {
			try {
				Document document = documentService.createDocument(multipartFile.getInputStream(), "attachement_" + signRequest.getAttachments().size() + "_" + multipartFile.getOriginalFilename(), multipartFile.getContentType());
				signRequest.getAttachments().add(document);
				document.setParentId(signRequest.getId());
			} catch (IOException e) {
				throw new EsupSignatureIOException(e.getMessage(), e);
			}
		}
	}

	public void pendingSignRequest(SignRequest signRequest, String authUserEppn) {
		for (Recipient recipient : signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients()) {
			signRequest.getRecipientHasSigned().put(recipient, actionService.getEmptyAction());
			if(!signRequest.getParentSignBook().getTeam().contains(recipient.getUser())) {
				signRequest.getParentSignBook().getTeam().add(recipient.getUser());
			}
		}
		updateStatus(signRequest.getId(), SignRequestStatus.pending, "Envoyé pour signature", "SUCCESS", null, null, null, authUserEppn, authUserEppn);
		customMetricsService.incValue("esup-signature.signrequests", "new");
		for (Target target : signRequest.getParentSignBook().getLiveWorkflow().getTargets().stream().filter(t -> t != null && fsAccessFactoryService.getPathIOType(t.getTargetUri()).equals(DocumentIOType.rest)).collect(Collectors.toList())) {
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.getForEntity(target.getTargetUri() + "?signRequestId=" + signRequest.getId() + "&status=pending&step=" + signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber(), String.class);
		}
	}

	public boolean isNextWorkFlowStep(SignBook signBook) {
		return signBook.getLiveWorkflow().getLiveWorkflowSteps().size() >= signBook.getLiveWorkflow().getCurrentStepNumber() + 2;
	}

	public boolean isMoreWorkflowStep(SignBook signBook) {
		return signBook.getLiveWorkflow().getLiveWorkflowSteps().size() >= signBook.getLiveWorkflow().getCurrentStepNumber() + 1 && signBook.getLiveWorkflow().getCurrentStepNumber() > -1;
	}

	public boolean isStepAllSignDone(SignBook signBook) {
		LiveWorkflowStep liveWorkflowStep = signBook.getLiveWorkflow().getCurrentStep();
		return (!liveWorkflowStep.getAllSignToComplete() || isWorkflowStepFullSigned(liveWorkflowStep)) && !isMoreWorkflowStep(signBook);
	}

	public boolean isWorkflowStepFullSigned(LiveWorkflowStep liveWorkflowStep) {
		for (Recipient recipient : liveWorkflowStep.getRecipients()) {
			if (!recipient.getSigned()) {
				return false;
			}
		}
		return true;
	}

	public boolean nextWorkFlowStep(SignBook signBook) {
		if (isMoreWorkflowStep(signBook)) {
			signBook.getLiveWorkflow().setCurrentStep(signBook.getLiveWorkflow().getLiveWorkflowSteps().get(signBook.getLiveWorkflow().getCurrentStepNumber()));
			return signBook.getLiveWorkflow().getCurrentStepNumber() > -1;
		}
		return false;
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

	public boolean isCurrentStepCompleted(SignRequest signRequest) {
		return signRequest.getParentSignBook().getSignRequests().stream().allMatch(sr -> sr.getStatus().equals(SignRequestStatus.completed) || sr.getStatus().equals(SignRequestStatus.refused));
	}

	public boolean isSignRequestCompleted(SignRequest signRequest) {
		if (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getAllSignToComplete()) {
			return signRequest.getRecipientHasSigned().keySet().stream().filter(r -> signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients().contains(r)).allMatch(recipient -> !signRequest.getRecipientHasSigned().get(recipient).getActionType().equals(ActionType.none));
		} else {
			return signRequest.getRecipientHasSigned().keySet().stream().filter(r -> signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients().contains(r)).anyMatch(recipient -> !signRequest.getRecipientHasSigned().get(recipient).getActionType().equals(ActionType.none));
		}
	}

	public void completeSignRequests(List<SignRequest> signRequests, String authUserEppn) {
		for(SignRequest signRequest : signRequests) {
			if(!signRequest.getStatus().equals(SignRequestStatus.refused)) {
				updateStatus(signRequest.getId(), SignRequestStatus.completed, "Terminé", "SUCCESS", authUserEppn, authUserEppn);
			}
		}
	}

	public void addPostit(Long signRequestId, String comment, String userEppn, String authUserEppn) {
		SignRequest signRequest = getById(signRequestId);
				if(comment != null && !comment.isEmpty()) {
			updateStatus(signRequest.getId(), signRequest.getStatus(), "comment", comment, "SUCCES", null, null, null, 0, userEppn, authUserEppn);
		}
	}

	public void cleanDocuments(SignRequest signRequest, String authUserEppn) {
		Date cleanDate = getEndDate(signRequest);
		Calendar cal = Calendar.getInstance();
		if(cleanDate != null) {
			cal.setTime(cleanDate);
			cal.add(Calendar.DATE, globalProperties.getDelayBeforeCleaning());
			Date test = cal.getTime();
			Date now = new Date();
			if(signRequest.getExportedDocumentURI() != null
					&& test.getTime()< now.getTime()
					&& signRequest.getSignedDocuments().size() > 0) {
				clearAllDocuments(signRequest);
				updateStatus(signRequest.getId(), SignRequestStatus.exported, "Fichiers nettoyés", "SUCCESS", authUserEppn, authUserEppn);
			} else {
				logger.debug("cleanning documents was skipped because date");
			}
		} else {
			logger.error("no end date for signrequest " + signRequest.getId());
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

	public FsFile getLastSignedFsFile(SignRequest signRequest) throws EsupSignatureFsException {
		if(signRequest.getStatus().equals(SignRequestStatus.exported)) {
			if (signRequest.getExportedDocumentURI() != null && !signRequest.getExportedDocumentURI().startsWith("mail")) {
				FsAccessService fsAccessService = fsAccessFactoryService.getFsAccessService(signRequest.getExportedDocumentURI());
				return fsAccessService.getFileFromURI(signRequest.getExportedDocumentURI());
			}
		}
		Document lastSignedDocument = signService.getToSignDocuments(signRequest.getId()).get(0);
		return new FsFile(lastSignedDocument.getInputStream(), lastSignedDocument.getFileName(), lastSignedDocument.getContentType());
	}

	@Transactional
	public void updateStatus(Long signRequestId, SignRequestStatus signRequestStatus, String action, String returnCode, String userEppn, String authUserEppn) {
		updateStatus(signRequestId, signRequestStatus, action, returnCode, null, null, null, null, userEppn, authUserEppn);
	}

	public void updateStatus(Long signRequestId, SignRequestStatus signRequestStatus, String action, String returnCode, Integer pageNumber, Integer posX, Integer posY, String userEppn, String authUserEppn) {
		updateStatus(signRequestId, signRequestStatus, action, returnCode, pageNumber, posX, posY, null, userEppn, authUserEppn);
	}

	public Log updateStatus(Long signRequestId, SignRequestStatus signRequestStatus, String action, String returnCode, Integer pageNumber, Integer posX, Integer posY, Integer stepNumber, String userEppn, String authUserEppn) {
		return logService.create(signRequestId, signRequestStatus, action, null, returnCode, pageNumber, posX, posY, stepNumber, userEppn, authUserEppn);
	}

	public void updateStatus(Long signRequestId, SignRequestStatus signRequestStatus, String action, String comment, String returnCode, Integer pageNumber, Integer posX, Integer posY, Integer stepNumber, String userEppn, String authUserEppn) {
		logService.create(signRequestId, signRequestStatus, action, comment, returnCode, pageNumber, posX, posY, stepNumber, userEppn, authUserEppn);
	}

	@Transactional
	public void restore(Long signRequestId, String userEppn) {
		SignRequest signRequest = getById(signRequestId);
		if(signRequest.getStatus().equals(SignRequestStatus.deleted)) {
			List<Log> logs = logService.getBySignRequest(signRequestId);
			logs = logs.stream().sorted(Comparator.comparing(Log::getLogDate).reversed()).collect(Collectors.toList());
			SignRequestStatus restoreStatus = SignRequestStatus.valueOf(logs.get(1).getFinalStatus());
			signRequest.setStatus(restoreStatus);
			signRequest.getParentSignBook().setStatus(restoreStatus);
			logService.create(signRequest.getId(), restoreStatus, "Restauration par l'utilisateur", "", "SUCCESS", null, null, null, null, userEppn, userEppn);
		}
	}

	@Transactional
	public boolean delete(Long signRequestId, String userEppn) {
		SignRequest signRequest = getById(signRequestId);
		if(signRequest.getStatus().equals(SignRequestStatus.deleted)) {
			deleteDefinitive(signRequestId);
			return true;
		} else {
			if (signRequest.getStatus().equals(SignRequestStatus.exported) || signRequest.getStatus().equals(SignRequestStatus.archived)) {
				signRequest.getOriginalDocuments().clear();
				signRequest.getSignedDocuments().clear();
			}
			signRequest.setStatus(SignRequestStatus.deleted);
			logService.create(signRequest.getId(), SignRequestStatus.deleted, "Suppression par l'utilisateur", "", "SUCCESS", null, null, null, null, userEppn, userEppn);
			otpService.deleteOtpBySignRequestId(signRequestId);
			return false;
		}
	}

	@Transactional
	public void deleteDefinitive(Long signRequestId) {
		SignRequest signRequest = getById(signRequestId);
		signRequest.getRecipientHasSigned().clear();
		signRequestRepository.save(signRequest);
		if (signRequest.getData() != null) {
			Long dataId = signRequest.getData().getId();
			signRequest.setData(null);
			dataService.deleteOnlyData(dataId);
		}
		List<Long> commentsIds = signRequest.getComments().stream().map(Comment::getId).collect(Collectors.toList());
		for (Long commentId : commentsIds) {
			commentService.deleteComment(commentId);
		}
		signRequest.getParentSignBook().getSignRequests().remove(signRequest);
		signRequestRepository.delete(signRequest);
	}

	public Date getEndDate(SignRequest signRequest) {
		List<Action> action = signRequest.getRecipientHasSigned().values().stream().filter(action1 -> !action1.getActionType().equals(ActionType.none)).sorted(Comparator.comparing(Action::getDate)).collect(Collectors.toList());
		if(action.size() > 0) {
			return action.get(0).getDate();
		}
		return null;
	}

	@Transactional
	public void deleteSignRequest(Long signRequestId, String userEppn) {
		SignRequest signRequest = signRequestRepository.findById(signRequestId).orElseThrow();
		if(signRequest.getStatus().equals(SignRequestStatus.deleted)) {
			deleteDefinitive(signRequestId);
		} else {
			if (signRequest.getStatus().equals(SignRequestStatus.exported) || signRequest.getStatus().equals(SignRequestStatus.archived)) {
				signRequest.getOriginalDocuments().clear();
				signRequest.getSignedDocuments().clear();
			}
			signRequest.setStatus(SignRequestStatus.deleted);
			logService.create(signRequest.getId(), SignRequestStatus.deleted, "Suppression par l'utilisateur", "", "SUCCESS", null, null, null, null, userEppn, userEppn);
			otpService.deleteOtpBySignRequestId(signRequestId);
		}
	}

	@Transactional
	public boolean isTempUsers(Long signRequestId) {
		SignRequest signRequest = getById(signRequestId);
		return userService.getTempUsers(signRequest).size() > 0;
	}

	public boolean checkTempUsers(Long id, List<String> recipientEmails, List<JsonExternalUserInfo> externalUsersInfos) throws MessagingException, EsupSignatureRuntimeException {
		SignRequest signRequest = getById(id);
		List<User> tempUsers = userService.getTempUsers(signRequest, recipientEmails);
		if(tempUsers.size() > 0) {
			if (externalUsersInfos != null && tempUsers.size() == externalUsersInfos.size()) {
				for (User tempUser : tempUsers) {
					if (tempUser.getUserType().equals(UserType.shib)) {
						logger.warn("TODO Envoi Mail SHIBBOLETH ");
						//TODO envoi mail spécifique
					} else if (tempUser.getUserType().equals(UserType.external)) {
						JsonExternalUserInfo jsonExternalUserInfo = externalUsersInfos.stream().filter(jsonExternalUserInfo1 -> jsonExternalUserInfo1.getEmail().equals(tempUser.getEmail())).findFirst().get();
						tempUser.setFirstname(jsonExternalUserInfo.getFirstname());
						tempUser.setName(jsonExternalUserInfo.getName());
						if(StringUtils.hasText(jsonExternalUserInfo.getPhone())) {
							tempUser.setPhone(PhoneNumberUtil.normalizeDiallableCharsOnly(jsonExternalUserInfo.getPhone()));
						}
					}
				}
			} else {
				return true;
			}
		}
		return false;
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
				if (!"".equals(data.getForm().getPreFillType()) && signRequest.getParentSignBook().getLiveWorkflow() != null && signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() != null && signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getUsers().contains(user)) {
					prefilledFields = preFillService.getPreFilledFieldsByServiceName(data.getForm().getPreFillType(), fields, user, signRequest);
					for (Field field : prefilledFields) {
						if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() == null
								|| !field.getWorkflowSteps().contains(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep())) {
							field.setDefaultValue("");
						}
					}
				} else {
					prefilledFields = data.getForm().getFields();
				}
			}
		}
		for (Field field : prefilledFields) {
			if (field.getName() != null
				&& data.getDatas().size() > 0
				&& data.getDatas().get(field.getName()) != null
				&& !data.getDatas().get(field.getName()).isEmpty()) {
				field.setDefaultValue(data.getDatas().get(field.getName()));
			}
			for(WorkflowStep workflowStep : field.getWorkflowSteps()) {
				Optional<LiveWorkflowStep> liveWorkflowStep = signRequest.getParentSignBook().getLiveWorkflow().getLiveWorkflowSteps().stream().filter(l -> workflowStep.equals(l.getWorkflowStep())).findFirst();
				if(liveWorkflowStep.isPresent()) {
					if(liveWorkflowStep.get().getRecipients().stream().anyMatch(recipient -> recipient.getUser().getEppn().equals(userEppn))
						&& liveWorkflowStep.get().equals(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep())) {
						field.setEditable(true);
						break;
					} else{
						field.setEditable(false);
					}
				} else {
					field.setEditable(false);
				}
			}
		}
		return prefilledFields;
	}

	@Transactional
	public Document getLastSignedFile(Long signRequestId) {
		SignRequest signRequest = getById(signRequestId);
		if(signRequest.getSignedDocuments().size() > 0) {
			return signRequest.getSignedDocuments().get(signRequest.getSignedDocuments().size() - 1);
		} else {
			return null;
		}
	}

	@Transactional
	public boolean addAttachement(MultipartFile[] multipartFiles, String link, Long signRequestId) throws EsupSignatureIOException {
		SignRequest signRequest = getById(signRequestId);
		int nbAttachmentAdded = 0;
		if(multipartFiles != null && multipartFiles.length > 0) {
			for (MultipartFile multipartFile : multipartFiles) {
				if(multipartFile.getSize() > 0) {
					addAttachmentToSignRequest(signRequest, multipartFile);
					nbAttachmentAdded++;
				}
			}
		}
		if(link != null && !link.isEmpty()) {
			signRequest.getLinks().add(link);
			nbAttachmentAdded++;
		}
		return nbAttachmentAdded > 0;
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
		String toRemove = new ArrayList<>(signRequest.getLinks()).get(linkId);
		signRequest.getLinks().remove(toRemove);
	}

	@Transactional
	public void addComment(Long id, String commentText, Integer commentPageNumber, Integer commentPosX, Integer commentPosY, String postit, Integer spotStepNumber, String authUserEppn) {
			SignRequest signRequest = getById(id);
		if(spotStepNumber != null && spotStepNumber > 0) {
			SignRequestParams signRequestParams = signRequestParamsService.createSignRequestParams(commentPageNumber, commentPosX, commentPosY);
			int docNumber = signRequest.getParentSignBook().getSignRequests().indexOf(signRequest);
			signRequestParams.setSignDocumentNumber(docNumber);
			signRequestParams.setComment(commentText);
			signRequest.getSignRequestParams().add(signRequestParams);
			signRequest.getParentSignBook().getLiveWorkflow().getLiveWorkflowSteps().get(spotStepNumber - 1).getSignRequestParams().add(signRequestParams);
		}
		Comment comment = commentService.create(id, commentText, commentPosX, commentPosY, commentPageNumber, spotStepNumber, "on".equals(postit), null, authUserEppn);
		if(!(spotStepNumber != null && spotStepNumber > 0)) {
			updateStatus(signRequest.getId(), null, "Ajout d'un commentaire", commentText, "SUCCESS", commentPageNumber, commentPosX, commentPosY, null, authUserEppn, authUserEppn);
			if(globalProperties.getSendPostitByEmail() && !authUserEppn.equals(signRequest.getCreateBy().getEppn())) {
				try {
					mailService.sendPostit(signRequest.getParentSignBook(), comment);
				} catch (EsupSignatureMailException e) {
					logger.warn("postit not sended", e);
				}
			}
		} else {
			updateStatus(signRequest.getId(), null, "Ajout d'un emplacement de signature", commentText, "SUCCESS", commentPageNumber, commentPosX, commentPosY, null, authUserEppn, authUserEppn);
		}
	}

	public List<User> checkUserResponseSigned(SignRequest signRequest) {
		List<User> usersHasSigned = new ArrayList<>();
		for(Map.Entry<Recipient, Action> recipientActionEntry : signRequest.getRecipientHasSigned().entrySet()) {
			if (recipientActionEntry.getValue().getActionType().equals(ActionType.signed)) {
				usersHasSigned.add(recipientActionEntry.getKey().getUser());
			}
		}
		return usersHasSigned;
	}

	public List<User> checkUserResponseRefused(SignRequest signRequest) {
		List<User> usersHasRefused = new ArrayList<>();
		for(Map.Entry<Recipient, Action> recipientActionEntry : signRequest.getRecipientHasSigned().entrySet()) {
			if (recipientActionEntry.getValue().getActionType().equals(ActionType.refused)) {
				usersHasRefused.add(recipientActionEntry.getKey().getUser());
			}
		}
		return usersHasRefused;
	}

	public Long getNbPendingSignRequests(String userEppn) {
		return signRequestRepository.countByCreateByEppnAndStatus(userEppn, SignRequestStatus.pending);
	}

	public Long getNbDraftSignRequests(String userEppn) {
		return signRequestRepository.countByCreateByEppnAndStatus(userEppn, SignRequestStatus.draft);
	}

	@Transactional
	public boolean getAttachmentResponse(Long signRequestId, Long attachementId, HttpServletResponse httpServletResponse) throws IOException {
		SignRequest signRequest = getById(signRequestId);
		Document attachement = documentService.getById(attachementId);
		if (attachement != null && attachement.getParentId().equals(signRequest.getId())) {
			webUtilsService.copyFileStreamToHttpResponse(attachement.getFileName(), attachement.getContentType(), "attachment", attachement.getInputStream(), httpServletResponse);
			return true;
		}
		return false;
	}

	@Transactional
	public void getToSignFileResponse(Long signRequestId, String disposition, HttpServletResponse httpServletResponse) throws IOException, EsupSignatureRuntimeException {
		SignRequest signRequest = getById(signRequestId);
		if (!signRequest.getStatus().equals(SignRequestStatus.exported)) {
			List<Document> documents = signService.getToSignDocuments(signRequest.getId());
			Document document;
			if(documents.size() > 0) {
				document = documents.get(0);
			} else {
				document = signRequest.getOriginalDocuments().get(0);
			}
			webUtilsService.copyFileStreamToHttpResponse(document.getFileName(), document.getContentType(), disposition, document.getInputStream(), httpServletResponse);
		} else {
			FsFile fsFile = getLastSignedFsFile(signRequest);
			webUtilsService.copyFileStreamToHttpResponse(fsFile.getName(), fsFile.getContentType(), disposition, fsFile.getInputStream(), httpServletResponse);
		}
	}

	@Transactional
	public void getToSignFileResponseWithCode(Long signRequestId, HttpServletResponse httpServletResponse) throws IOException, EsupSignatureRuntimeException, WriterException {
		SignRequest signRequest = getById(signRequestId);
		if (!signRequest.getStatus().equals(SignRequestStatus.exported)) {
			List<Document> documents = signService.getToSignDocuments(signRequest.getId());
			Document document;
			if(documents.size() > 0) {
				document = documents.get(0);
			} else {
				document = signRequest.getOriginalDocuments().get(0);
			}
			InputStream inputStream = pdfService.addQrCode(signRequest, document.getInputStream());
			webUtilsService.copyFileStreamToHttpResponse(document.getFileName(), document.getContentType(), "attachment", inputStream, httpServletResponse);
		} else {
			FsFile fsFile = getLastSignedFsFile(signRequest);
			InputStream inputStream = pdfService.addQrCode(signRequest, fsFile.getInputStream());
			webUtilsService.copyFileStreamToHttpResponse(fsFile.getName(), fsFile.getContentType(), "attachment", inputStream, httpServletResponse);
		}
	}

	@Transactional
	public void getFileResponse(Long documentId, HttpServletResponse httpServletResponse) throws IOException {
		Document document = documentService.getById(documentId);
		webUtilsService.copyFileStreamToHttpResponse(document.getFileName(), document.getContentType(), "attachment", document.getInputStream(), httpServletResponse);
	}

	@Transactional
	public List<Document> getAttachments(Long signRequestId) {
		SignRequest signRequest = getById(signRequestId);
		return new ArrayList<>(signRequest.getAttachments());
	}

	public boolean replayNotif(Long id) throws EsupSignatureMailException {
		SignRequest signRequest = this.getById(id);
		List<String> recipientEmails = new ArrayList<>();
		List<Recipient> recipients = getCurrentRecipients(signRequest);
		for(Recipient recipient : recipients) {
			if(recipient.getUser() != null  && recipient.getUser().getEmail() != null) {
				recipientEmails.add(recipient.getUser().getEmail());
			}
		}
		long notifTime = Long.MAX_VALUE;
		if(signRequest.getLastNotifDate() != null) {
			notifTime = Duration.between(signRequest.getLastNotifDate().toInstant(), new Date().toInstant()).toHours();
		}
		if(recipientEmails.size() > 0 && notifTime >= globalProperties.getHoursBeforeRefreshNotif() && signRequest.getStatus().equals(SignRequestStatus.pending)) {
			mailService.sendSignRequestReplayAlert(recipientEmails, signRequest);
			return true;
		}
		return false;
	}

	private List<Recipient> getCurrentRecipients(SignRequest signRequest) {
		return signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients().stream().filter(r -> !r.getSigned()).collect(Collectors.toList());
	}

	@Transactional
	public List<SignRequest> getRecipientNotPresentSignRequests(String eppn) {
		List<SignRequest> signRequests = signRequestRepository.findByCreateByEppnAndStatus(eppn, SignRequestStatus.pending);
		List<SignRequest> recipientNotPresentsignRequests = new ArrayList<>(signRequests);
		for(SignRequest signRequest : signRequests) {
			List<Recipient> recipients = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients();
			for(Recipient recipient : recipients) {
				User user = recipient.getUser();
				if(userService.findPersonLdapLightByUser(user) != null || user.getUserType().equals(UserType.external) || user.getUserType().equals(UserType.shib)) {
					recipientNotPresentsignRequests.remove(signRequest);
				}
			}
		}
		return recipientNotPresentsignRequests;
	}

	@Transactional
	public List<SignRequestParams> getToUseSignRequestParams(Long id, String userEppn) {
		User user = userService.getUserByEppn(userEppn);
		List<SignRequestParams> toUserSignRequestParams = new ArrayList<>();
		SignRequest signRequest = getById(id);
		int signOrderNumber = signRequest.getParentSignBook().getSignRequests().indexOf(signRequest);
		if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() != null) {
			List<SignRequestParams> signRequestParamsForCurrentStep = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams().stream().filter(signRequestParams -> signRequestParams.getSignDocumentNumber().equals(signOrderNumber)).collect(Collectors.toList());
			for(SignRequestParams signRequestParams : signRequestParamsForCurrentStep) {
				if(signRequest.getSignRequestParams().contains(signRequestParams)
					&& signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients().stream().anyMatch(recipient -> recipient.getUser().equals(user))) {
					toUserSignRequestParams.add(signRequestParams);
				}
			}
		}
		return toUserSignRequestParams;
	}

	@Transactional
	public InputStream getToValidateFile(long id) throws IOException {
		SignRequest signRequest = getById(id);
		Document toValideDocument = signRequest.getLastSignedDocument();
		if(toValideDocument != null) {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			IOUtils.copy(toValideDocument.getInputStream(), outputStream);
			outputStream.close();
			return new ByteArrayInputStream(outputStream.toByteArray());
		} else {
			return null;
		}
	}

	public List<SignRequest> getAll() {
		return (List<SignRequest>) signRequestRepository.findAll();
	}

	public boolean isAttachmentAlert(SignRequest signRequest) {
		boolean attachmentAlert = false;
		if (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() != null
			&& signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep() != null
			&& signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep().getAttachmentAlert() != null
			&& signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep().getAttachmentAlert()
			&& signRequest.getAttachments().size() == 0) {
			attachmentAlert = true;
		}
		return attachmentAlert;
	}
	public boolean isAttachmentRequire(SignRequest signRequest) {
		boolean attachmentRequire = false;
		if (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() != null
			&&signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep() != null
			&& signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep().getAttachmentRequire() != null
			&& signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep().getAttachmentRequire()
			&& signRequest.getAttachments().size() == 0) {
			attachmentRequire = true;
		}
		return attachmentRequire;
	}

	@Transactional
	public Reports validate(long signRequestId) throws IOException {
		List<Document> documents = signService.getToSignDocuments(signRequestId);
		if(documents.size() > 0) {
			byte[] bytes = documents.get(0).getInputStream().readAllBytes();
			return validationService.validate(new ByteArrayInputStream(bytes), null);
		} else {
			return null;
		}
	}

	@Transactional
	public void warningReaded(String authUserEppn) {
		User authUser = userService.getUserByEppn(authUserEppn);
		List<SignRequest> oldSignRequests = signRequestRepository.findByCreateByEppnAndOlderPending(authUser.getId(), globalProperties.getNbDaysBeforeWarning());
		for (SignRequest signRequest : oldSignRequests) {
			signRequest.setWarningReaded(true);
		}
	}

    public SignRequest getByLastOtp(String urlId) {
		return signRequestRepository.findByLastOtp(urlId);
    }

	@Transactional
	public void renewOtp(String urlId) {
		SignRequest signRequest = getByLastOtp(urlId);
		if(signRequest != null) {
			List<Recipient> recipients = signRequest.getRecipientHasSigned().keySet().stream().filter(r -> r.getUser().getUserType().equals(UserType.external)).collect(Collectors.toList());
			for(Recipient recipient : recipients) {
				try {
					otpService.generateOtpForSignRequest(signRequest.getId(), recipient.getUser().getId(), recipient.getUser().getPhone());
				} catch (EsupSignatureMailException e) {
					logger.error(e.getMessage());
				}
			}
		}
	}

	@Transactional
	public void anonymize(String userEppn, User anonymous) {
		for(SignRequest signRequest : signRequestRepository.findByCreateByEppn(userEppn)) {
			signRequest.setCreateBy(anonymous);
		}
	}

	@Transactional
	public List<Comment> getPostits(Long id) {
		SignRequest signRequest = getById(id);
		return signRequest.getComments().stream().filter(Comment::getPostit).collect(Collectors.toList());
	}

	@Transactional
	public List<Comment> getComments(Long id) {
		SignRequest signRequest = getById(id);
		return signRequest.getComments().stream().filter(comment -> !comment.getPostit() && comment.getStepNumber() == null).collect(Collectors.toList());
	}

	@Transactional
	public List<Comment> getSpots(Long id) {
		SignRequest signRequest = getById(id);
		return signRequest.getComments().stream().filter(comment -> comment.getStepNumber() != null).collect(Collectors.toList());
	}

	@Transactional
	public String getJson(Long id) throws JsonProcessingException {
		SignRequest signRequest = getById(id);
		return objectMapper.writeValueAsString(signRequest);
	}

	@Transactional
	public Long getParentIdIfSignRequestUnique(Long id) {
		SignRequest signRequest = getById(id);
		if(signRequest.getParentSignBook().getSignRequests().size() == 1) {
			return signRequest.getParentSignBook().getId();
		} else {
			return null;
		}
	}

	@Transactional
	public String getAuditTrailJson(Long id) throws JsonProcessingException {
		SignRequest signRequest = getById(id);
		return objectMapper.writeValueAsString(signRequest.getAuditTrail());
	}
}
