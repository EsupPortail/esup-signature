package org.esupportail.esupsignature.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.WriterException;
import eu.europa.esig.dss.enumerations.SignatureForm;
import eu.europa.esig.dss.validation.reports.Reports;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.sign.SignProperties;
import org.esupportail.esupsignature.dss.model.AbstractSignatureForm;
import org.esupportail.esupsignature.dss.model.DssMultipartFile;
import org.esupportail.esupsignature.dss.model.SignatureDocumentForm;
import org.esupportail.esupsignature.dss.model.SignatureMultipleDocumentsForm;
import org.esupportail.esupsignature.dss.service.FOPService;
import org.esupportail.esupsignature.dto.js.JsMessage;
import org.esupportail.esupsignature.dto.json.RecipientWsDto;
import org.esupportail.esupsignature.dto.json.RecipientsActionsDto;
import org.esupportail.esupsignature.dto.json.SignRequestStepsDto;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.*;
import org.esupportail.esupsignature.exception.*;
import org.esupportail.esupsignature.repository.NexuSignatureRepository;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.repository.WsAccessTokenRepository;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessFactoryService;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessService;
import org.esupportail.esupsignature.service.interfaces.fs.FsFile;
import org.esupportail.esupsignature.service.interfaces.prefill.PreFillService;
import org.esupportail.esupsignature.service.mail.MailService;
import org.esupportail.esupsignature.service.utils.StepStatus;
import org.esupportail.esupsignature.service.utils.WebUtilsService;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.esupportail.esupsignature.service.utils.metric.CustomMetricsService;
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.esupportail.esupsignature.service.utils.sign.SignService;
import org.esupportail.esupsignature.service.utils.sign.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@EnableConfigurationProperties(GlobalProperties.class)
public class SignRequestService {

	private static final Logger logger = LoggerFactory.getLogger(SignRequestService.class);

	private final GlobalProperties globalProperties;
	private final SignProperties signProperties;
	private final TargetService targetService;
	private final WebUtilsService webUtilsService;
	private final SignRequestRepository signRequestRepository;
	private final ActionService actionService;
	private final PdfService pdfService;
	private final DocumentService documentService;
	private final CustomMetricsService customMetricsService;
	private final SignService signService;
	private final SignTypeService signTypeService;
	private final UserService userService;
	private final DataService dataService;
	private final CommentService commentService;
	private final MailService mailService;
	private final AuditTrailService auditTrailService;
	private final UserShareService userShareService;
	private final RecipientService recipientService;
	private final FsAccessFactoryService fsAccessFactoryService;
	private final WsAccessTokenRepository wsAccessTokenRepository;
	private final FileService fileService;
	private final PreFillService preFillService;
	private final LogService logService;
	private final SignRequestParamsService signRequestParamsService;
	private final ValidationService validationService;
	private final FOPService fopService;
	private final ObjectMapper objectMapper;
	private final SignBookRepository signBookRepository;
	private final NexuSignatureRepository nexuSignatureRepository;

	public SignRequestService(GlobalProperties globalProperties, SignProperties signProperties, TargetService targetService, WebUtilsService webUtilsService, SignRequestRepository signRequestRepository, ActionService actionService, PdfService pdfService, DocumentService documentService, CustomMetricsService customMetricsService, SignService signService, SignTypeService signTypeService, UserService userService, DataService dataService, CommentService commentService, MailService mailService, AuditTrailService auditTrailService, UserShareService userShareService, RecipientService recipientService, FsAccessFactoryService fsAccessFactoryService, WsAccessTokenRepository wsAccessTokenRepository, FileService fileService, PreFillService preFillService, LogService logService, SignRequestParamsService signRequestParamsService, ValidationService validationService, FOPService fopService, ObjectMapper objectMapper, SignBookRepository signBookRepository, NexuSignatureRepository nexuSignatureRepository) {
        this.globalProperties = globalProperties;
        this.signProperties = signProperties;
        this.targetService = targetService;
        this.webUtilsService = webUtilsService;
        this.signRequestRepository = signRequestRepository;
        this.actionService = actionService;
        this.pdfService = pdfService;
        this.documentService = documentService;
        this.customMetricsService = customMetricsService;
        this.signService = signService;
        this.signTypeService = signTypeService;
        this.userService = userService;
        this.dataService = dataService;
        this.commentService = commentService;
        this.mailService = mailService;
        this.auditTrailService = auditTrailService;
        this.userShareService = userShareService;
        this.recipientService = recipientService;
        this.fsAccessFactoryService = fsAccessFactoryService;
        this.wsAccessTokenRepository = wsAccessTokenRepository;
        this.fileService = fileService;
        this.preFillService = preFillService;
        this.logService = logService;
        this.signRequestParamsService = signRequestParamsService;
        this.validationService = validationService;
        this.fopService = fopService;
        this.objectMapper = objectMapper;
        this.signBookRepository = signBookRepository;
		this.nexuSignatureRepository = nexuSignatureRepository;
	}

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
		if (signRequest != null) {
			return signRequest.getStatus().name();
		} else {
			List<Log> logs = logService.getBySignRequest(id);
			if (!logs.isEmpty()) {
				return "fully-deleted";
			}
		}
		return null;
	}

	public Optional<SignRequest> getSignRequestByToken(String token) {
		return signRequestRepository.findByToken(token);
	}

	@Transactional
	public boolean isEditable(long id, String userEppn) {
		SignRequest signRequest = getById(id);
		User user = userService.getByEppn(userEppn);
		SignBook signBook = signRequest.getParentSignBook();
		if (
			(
				!signRequest.getDeleted() && signRequest.getStatus().equals(SignRequestStatus.pending)
				&&
				(!user.getUserType().equals(UserType.external) || (signBook.getLiveWorkflow().getWorkflow() != null && signBook.getLiveWorkflow().getWorkflow().getExternalCanEdit()) || (signBook.getLiveWorkflow().getWorkflow() == null && globalProperties.getExternalCanEdit()))
				&&
				(
					isUserInRecipients(signRequest, userEppn)
					|| signRequest.getCreateBy().getEppn().equals(userEppn)
					|| signBook.getViewers().contains(user)
				)
			)
			||
			(signRequest.getStatus().equals(SignRequestStatus.draft) && signRequest.getCreateBy().getEppn().equals(user.getEppn()))
		) {
			return true;
		}
		return false;
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

	@Transactional
	public List<Document> getToSignDocuments(Long signRequestId) {
		SignRequest signRequest = signRequestRepository.findById(signRequestId).get();
		List<Document> documents = new ArrayList<>();
		if(signRequest.getSignedDocuments() != null && !signRequest.getSignedDocuments().isEmpty()) {
			documents.add(signRequest.getLastSignedDocument());
		} else {
			documents.addAll(signRequest.getOriginalDocuments());
		}
		return documents;
	}

	@Transactional
	public StepStatus sign(SignRequest signRequest, String password, String signWith, List<SignRequestParams> signRequestParamses, Data data, Map<String, String> formDataMap, String userEppn, String authUserEppn, Long userShareId, String comment) throws EsupSignatureRuntimeException, IOException {
		User user = userService.getByEppn(userEppn);
		if(signRequest.getAuditTrail() == null) {
			signRequest.setAuditTrail(auditTrailService.create(signRequest.getToken()));
		}
		boolean isViewed = signRequest.getViewedBy().contains(user);
		StepStatus stepStatus;
		Date date = new Date();
		List<Log> lastSignLogs = new ArrayList<>();
		User signerUser = userService.getByEppn(userEppn);
		if(userShareId != null) {
			UserShare userShare = userShareService.getById(userShareId);
			if (userShare.getUser().getEppn().equals(userEppn) && userShare.getSignWithOwnSign() != null && userShare.getSignWithOwnSign()) {
				signerUser = userService.getByEppn(authUserEppn);
			}
		}
		List<Document> toSignDocuments = getToSignDocuments(signRequest.getId());
		SignType signType = signRequest.getCurrentSignType();
		byte[] filledInputStream;
		boolean isForm = false;
		if(!isNextWorkFlowStep(signRequest.getParentSignBook())) {
			if(data != null && data.getForm() != null) {
				Form form = data.getForm();
				for (Field field : form.getFields()) {
					if ("default".equals(field.getExtValueServiceName()) && "system".equals(field.getExtValueType())) {
						if (field.getExtValueReturn().equals("id")) {
							data.getDatas().put(field.getName(), signRequest.getToken());
							formDataMap.put(field.getName(), signRequest.getToken());
						}
					}
				}
				isForm = true;
			}
		}
		byte[] bytes = toSignDocuments.get(0).getInputStream().readAllBytes();
		Reports reports = validationService.validate(new ByteArrayInputStream(bytes), null);
		if(formDataMap != null && !formDataMap.isEmpty() && toSignDocuments.get(0).getContentType().equals("application/pdf")
				&& (reports == null || reports.getSimpleReport().getSignatureIdList().isEmpty())) {
			filledInputStream = pdfService.fill(toSignDocuments.get(0).getInputStream(), formDataMap, isStepAllSignDone(signRequest.getParentSignBook()), isForm);
		} else {
			filledInputStream = toSignDocuments.get(0).getInputStream().readAllBytes();
		}
		boolean visual = true;
		if(signWith == null || SignWith.valueOf(signWith).equals(SignWith.imageStamp)) {
			byte[] signedInputStream = filledInputStream;
			String fileName = toSignDocuments.get(0).getFileName();
			if(signType.equals(SignType.hiddenVisa)) visual = false;
			if(signRequestParamses.isEmpty() && visual) {
				throw new EsupSignatureRuntimeException("Il faut apposer au moins un élément visuel");
			}
			int nbSign = 0;
			if (toSignDocuments.size() == 1 && toSignDocuments.get(0).getContentType().equals("application/pdf") && visual) {
				for(SignRequestParams signRequestParams : signRequestParamses) {
					if((signRequestParams.getSignImageNumber() < 0 || StringUtils.hasText(signRequestParams.getTextPart())) && (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getMultiSign() || signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSingleSignWithAnnotation())) {
						signedInputStream = pdfService.stampImage(signedInputStream, signRequest, signRequestParams, 1, signerUser, date, userService.getRoles(userEppn).contains("ROLE_OTP"), false);
						lastSignLogs.add(updateStatus(signRequest.getId(), signRequest.getStatus(), "Ajout d'un élément", null, "SUCCESS", signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos(), signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber(), userEppn, authUserEppn));
						auditTrailService.addAuditStep(signRequest.getToken(), userEppn, "Ajout d'un élément", "Pas de timestamp", "", "", date, isViewed, signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos());
					} else if(signRequestParams.getSignImageNumber() >= 0 && !StringUtils.hasText(signRequestParams.getTextPart()) && (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getMultiSign() || nbSign == 0)) {
						signedInputStream = pdfService.stampImage(signedInputStream, signRequest, signRequestParams, 1, signerUser, date, userService.getRoles(userEppn).contains("ROLE_OTP"), false);
						lastSignLogs.add(updateStatus(signRequest.getId(), signRequest.getStatus(), "Apposition de la signature", null, "SUCCESS", signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos(), signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber(), userEppn, authUserEppn));
						auditTrailService.addAuditStep(signRequest.getToken(), userEppn, "Signature simple", "Pas de timestamp", "", "", date, isViewed, signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos());
						nbSign++;
					}
				}
			} else {
				auditTrailService.addAuditStep(signRequest.getToken(), userEppn, "Visa", "Pas de timestamp", "", "", date, isViewed, null, null, null);
			}
			if (isStepAllSignDone(signRequest.getParentSignBook()) && (reports == null || reports.getSimpleReport().getSignatureIdList().isEmpty()) && BooleanUtils.isNotFalse(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getConvertToPDFA())) {
				signedInputStream = pdfService.convertToPDFA(pdfService.writeMetadatas(signedInputStream, fileName, signRequest, lastSignLogs));
			}
			byte[] signedBytes = signedInputStream;

			stepStatus = applyEndOfSignRules(signRequest.getId(), userEppn, authUserEppn, signType, comment);
			documentService.addSignedFile(signRequest, new ByteArrayInputStream(signedBytes), signRequest.getTitle() + "." + fileService.getExtension(toSignDocuments.get(0).getFileName()), toSignDocuments.get(0).getContentType(), user);
		} else {
			SignRequestParams lastSignRequestParams = findLastSignRequestParams(signRequestParamses);
			reports = validationService.validate(getToValidateFile(signRequest.getId()), null);
			if (reports == null || reports.getDiagnosticData().getAllSignatures().isEmpty()) {
				filledInputStream = stampImagesOnFirstSign(signRequest, signRequestParamses, userEppn, authUserEppn, filledInputStream, date, lastSignLogs, lastSignRequestParams);
			} else {
				logger.warn("skip add visuals because document already signed");
			}
			if (toSignDocuments.size() == 1 && toSignDocuments.get(0).getContentType().equals("application/pdf") && lastSignRequestParams != null) {
				signRequestParamsService.copySignRequestParams(signRequest, Collections.singletonList(lastSignRequestParams));
				toSignDocuments.get(0).setTransientInputStream(new ByteArrayInputStream(filledInputStream));
			}
			SignatureDocumentForm signatureDocumentForm = getAbstractSignatureForm(toSignDocuments, signRequest, true);
			Document signedDocument = signService.certSign(signatureDocumentForm, signRequest, signerUser.getEppn(), password, SignWith.valueOf(signWith), lastSignRequestParams);
			auditTrailService.createSignAuditStep(signRequest, userEppn, signedDocument, isViewed);
			stepStatus = applyEndOfSignRules(signRequest.getId(), userEppn, authUserEppn, SignType.signature, comment);

		}
		customMetricsService.incValue("esup-signature.signrequests", "signed");
		return stepStatus;
	}

	public SignRequestParams findLastSignRequestParams(List<SignRequestParams> signRequestParamses) {
		SignRequestParams lastSignRequestParams = null;
		for (SignRequestParams signRequestParams : signRequestParamses) {
			if (signRequestParams.getSignImageNumber() >= 0 && !StringUtils.hasText(signRequestParams.getTextPart())) {
				lastSignRequestParams = signRequestParams;
			}
		}
		return lastSignRequestParams;
	}

	public byte[] stampImagesOnFirstSign(SignRequest signRequest, List<SignRequestParams> signRequestParamses, String userEppn, String authUserEppn, byte[] filledInputStream, Date date, List<Log> lastSignLogs, SignRequestParams lastSignRequestParams) {
		User signerUser = userService.getByEppn(userEppn);
		boolean isViewed = signRequest.getViewedBy().contains(signerUser);
		if (signRequestParamses.size() > 1) {
			for (SignRequestParams signRequestParams : signRequestParamses) {
				if(signRequestParams.equals(lastSignRequestParams)) continue;
				filledInputStream = pdfService.stampImage(filledInputStream, signRequest, signRequestParams, 1, signerUser, date, userService.getRoles(userEppn).contains("ROLE_OTP"), true);
				Log log = updateStatus(signRequest.getId(), signRequest.getStatus(), "Ajout d'un élément", null, "SUCCESS", signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos(), signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber(), userEppn, authUserEppn);
				if(lastSignLogs != null) {
					lastSignLogs.add(log);
				}
				auditTrailService.addAuditStep(signRequest.getToken(), userEppn, "Ajout d'un élément", "Pas de timestamp", "", "", date, isViewed, signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos());
			}
		}
		return filledInputStream;
	}

	@Transactional
	public StepStatus applyEndOfSignRules(Long signRequestId, String userEppn, String authUserEppn, SignType signType, String comment) throws EsupSignatureRuntimeException {
		SignRequest signRequest = getById(signRequestId);
		if (signType.equals(SignType.visa) || signType.equals(SignType.hiddenVisa)) {
			if(comment != null && !comment.isEmpty()) {
				commentService.create(signRequest.getId(), comment, 0, 0, 0, null, true, null, userEppn);
			}
			updateStatus(signRequest.getId(), SignRequestStatus.checked, "Visa",  comment, "SUCCESS", null, null, null, signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber(), userEppn, authUserEppn);
		} else {
			if(comment != null && !comment.isEmpty()) {
				commentService.create(signRequest.getId(), comment, 0, 0, 0, null,true, null, userEppn);
			}
			updateStatus(signRequest.getId(), SignRequestStatus.signed, "Signature", comment, "SUCCESS", null, null, null, signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber(), userEppn, authUserEppn);
		}
		recipientService.validateRecipient(signRequest, userEppn);
		if (isSignRequestStepCompleted(signRequest)) {
			completeSignRequests(Collections.singletonList(signRequest), authUserEppn);
			if (isCurrentStepCompleted(signRequest)) {
				for (Recipient recipient : signRequest.getRecipientHasSigned().keySet()) {
					recipient.setSigned(!signRequest.getRecipientHasSigned().get(recipient).getActionType().equals(ActionType.none));
				}
				if (nextWorkFlowStep(signRequest.getParentSignBook())) {
					return StepStatus.completed;
				} else {
					return StepStatus.last_end;
				}
			}
		} else {
			updateStatus(signRequest.getId(), SignRequestStatus.pending, "Demande incomplète", null, "SUCCESS", null, null, null, null,  userEppn, authUserEppn);
		}
		return StepStatus.not_completed;
	}

	@Transactional
	public void seal(Long signRequestId) throws IOException {
		SignRequest signRequest = getById(signRequestId);
		SignatureDocumentForm signatureDocumentForm = getAbstractSignatureForm(getToSignDocuments(signRequestId), signRequest, true);
		Document document = signService.certSign(signatureDocumentForm, signRequest, "system", "", SignWith.sealCert, null);
		if(signRequest.getSignedDocuments().size() > 1) {
			signRequest.getSignedDocuments().remove(signRequest.getSignedDocuments().size() - 1);
		}
		if(signRequest.getSignedDocuments().size() > 1) {
			signRequest.getSignedDocuments().remove(signRequest.getSignedDocuments().size() - 2);
		}
		signRequest.getSignedDocuments().add(document);
	}

	public Long nbFollowedByMe(String userEppn) {
		return signBookRepository.countByViewersContaining(userEppn);
	}

	@Transactional
	public List<SignRequest> getToSignRequests(String userEppn) {
		User user = userService.getByEppn(userEppn);
		List<SignRequest> signRequestsToSign = signBookRepository.findToSign(user, null, null, null, new Date(0), new Date(), Pageable.unpaged()).getContent()
				.stream().map(SignBook::getSignRequests).flatMap(Collection::stream).collect(Collectors.toList());
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

	@Transactional
	public SignRequest createSignRequest(String name, SignBook signBook, String userEppn, String authUserEppn) {
		String token = UUID.randomUUID().toString();
		while (signRequestRepository.findByToken(token).isPresent()) {
			token = UUID.randomUUID().toString();
		}
		User user = userService.getByEppn(userEppn);
		SignRequest signRequest = new SignRequest();
		if(name == null || name.isEmpty()) {
			if (signBook.getSignRequests().isEmpty()) {
				signRequest.setTitle(signBook.getSubject());
			} else {
				signRequest.setTitle(signBook.getSubject() + "_" + signBook.getSignRequests().size());
			}
		} else {
			signRequest.setTitle(name);
		}
		signRequest.setToken(token);
		signRequest.setCreateBy(user);
		signRequest.setCreateDate(new Date());
		signRequest.setParentSignBook(signBook);
		signRequest.setStatus(SignRequestStatus.draft);
		signRequestRepository.save(signRequest);
		signBook.getSignRequests().add(signRequest);
		updateStatus(signRequest.getId(), SignRequestStatus.draft, "Création de la demande " + signBook.getId(), null, "SUCCESS", null, null, null, null, userEppn, authUserEppn);
		return signRequest;
	}

	@Transactional
	public void addDocsToSignRequest(SignRequest signRequest, boolean scanSignatureFields, int docNumber, List<SignRequestParams> signRequestParamses, MultipartFile... multipartFiles) throws EsupSignatureIOException {
		for(MultipartFile multipartFile : multipartFiles) {
			try {
				byte[] bytes = multipartFile.getInputStream().readAllBytes();
				String pdfaCheck = smallCheckPDFA(bytes);
				String contentType = multipartFile.getContentType();
				InputStream inputStream = new ByteArrayInputStream(bytes);
				if (multipartFiles.length == 1 && bytes.length > 0) {
					if("application/pdf".equals(multipartFiles[0].getContentType()) && (scanSignatureFields || (signRequest.getParentSignBook().getLiveWorkflow().getWorkflow() != null && StringUtils.hasText(signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getSignRequestParamsDetectionPattern())))) {
						bytes = pdfService.normalizePDF(bytes);
						List<SignRequestParams> toAddSignRequestParams = new ArrayList<>();
						if(signRequestParamses.isEmpty()) {
							toAddSignRequestParams = signRequestParamsService.scanSignatureFields(new ByteArrayInputStream(bytes), docNumber, signRequest.getParentSignBook().getLiveWorkflow().getWorkflow(), true);
						} else {
							for (SignRequestParams signRequestParams : signRequestParamses) {
								toAddSignRequestParams.add(signRequestParamsService.createSignRequestParams(signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos()));
							}
						}
						addAllSignRequestParamsToSignRequest(signRequest, toAddSignRequestParams);
						Reports reports = validationService.validate(new ByteArrayInputStream(bytes), null);
						if(reports == null || reports.getSimpleReport().getSignatureIdList().isEmpty()) {
							inputStream = pdfService.removeSignField(new ByteArrayInputStream(bytes), signRequest.getParentSignBook().getLiveWorkflow().getWorkflow());
						}
					} else if(contentType != null && contentType.contains("image")){
						bytes = pdfService.jpegToPdf(multipartFile.getInputStream(), multipartFile.getName()).readAllBytes();
						contentType = "application/pdf";
						inputStream = new ByteArrayInputStream(bytes);
					}
					Document document = documentService.createDocument(inputStream, signRequest.getCreateBy(), multipartFile.getOriginalFilename(), contentType);
					document.setPdfaCheck(pdfaCheck);
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

	public void addAllSignRequestParamsToSignRequest(SignRequest signRequest, List<SignRequestParams> toAddSignRequestParams) {
		signRequest.getSignRequestParams().addAll(toAddSignRequestParams);
		int step = 1;
		for(SignRequestParams signRequestParams : toAddSignRequestParams) {
			commentService.create(signRequest.getId(), "", signRequestParams.getxPos(), signRequestParams.getyPos(), signRequestParams.getSignPageNumber(), step, false, null, "system");
			step++;
		}
	}

	public void addAttachmentToSignRequest(SignRequest signRequest, String authUserEppn, MultipartFile... multipartFiles) throws EsupSignatureIOException {
		User user = userService.getByEppn(authUserEppn);
		for(MultipartFile multipartFile : multipartFiles) {
			try {
				Document document = documentService.createDocument(multipartFile.getInputStream(), user, "attachement_" + signRequest.getAttachments().size() + "_" + multipartFile.getOriginalFilename(), multipartFile.getContentType());
				signRequest.getAttachments().add(document);
				document.setParentId(signRequest.getId());
			} catch (IOException e) {
				throw new EsupSignatureIOException(e.getMessage(), e);
			}
		}
	}

	@Transactional
	public void pendingSignRequest(SignRequest signRequest, String authUserEppn) {
		for (Recipient recipient : signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients()) {
			signRequest.getRecipientHasSigned().put(recipient, actionService.getEmptyAction());
			if (isSigned(signRequest, null) && !signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().equals(SignType.hiddenVisa)) {
				if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().getValue() < 3) {
					signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().setSignType(SignType.signature);
				}
			}
		}
		updateStatus(signRequest.getId(), SignRequestStatus.pending, "Envoyé pour signature", null, "SUCCESS", null, null, null, null, authUserEppn, authUserEppn);
		customMetricsService.incValue("esup-signature.signrequests", "new");
		for (Target target : signRequest.getParentSignBook().getLiveWorkflow().getTargets().stream().filter(t -> t != null && fsAccessFactoryService.getPathIOType(t.getTargetUri()).equals(DocumentIOType.rest)).toList()) {
			targetService.sendRest(target.getTargetUri(), signRequest.getId().toString(), "pending", signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber().toString(), authUserEppn, "");
		}
	}

	public boolean isNextWorkFlowStep(SignBook signBook) {
		return signBook.getLiveWorkflow().getLiveWorkflowSteps().size() >= signBook.getLiveWorkflow().getCurrentStepNumber() + 2;
	}

	public boolean isMoreWorkflowStep(SignBook signBook) {
		return signBook.getLiveWorkflow().getLiveWorkflowSteps().size() >= signBook.getLiveWorkflow().getCurrentStepNumber() + 1 && signBook.getLiveWorkflow().getCurrentStepNumber() > -1;
	}

	public boolean isMoreWorkflowStepAndNotAutoSign(SignBook signBook) {
		return signBook.getLiveWorkflow().getLiveWorkflowSteps().size() >= signBook.getLiveWorkflow().getCurrentStepNumber() + 1 && signBook.getLiveWorkflow().getCurrentStepNumber() > -1 && !signBook.getLiveWorkflow().getLiveWorkflowSteps().get(signBook.getLiveWorkflow().getCurrentStepNumber()).getAutoSign();
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

	public boolean isCurrentStepCompleted(SignRequest signRequest) {
		return signRequest.getParentSignBook().getSignRequests().stream().allMatch(sr -> sr.getStatus().equals(SignRequestStatus.completed) || sr.getStatus().equals(SignRequestStatus.refused));
	}

	public boolean isSignRequestStepCompleted(SignRequest signRequest) {
		if (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getAllSignToComplete()) {
			return signRequest.getRecipientHasSigned().keySet().stream().filter(r -> signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients().contains(r)).noneMatch(recipient -> signRequest.getRecipientHasSigned().get(recipient).getActionType().equals(ActionType.none));
		} else {
			return signRequest.getRecipientHasSigned().keySet().stream().filter(r -> signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients().contains(r)).anyMatch(recipient -> !signRequest.getRecipientHasSigned().get(recipient).getActionType().equals(ActionType.none));
		}
	}

	@Transactional
	public void completeSignRequests(List<SignRequest> signRequests, String authUserEppn) {
		for(SignRequest signRequest : signRequests) {
			if(!signRequest.getStatus().equals(SignRequestStatus.refused)) {
				updateStatus(signRequest.getId(), SignRequestStatus.completed, "Terminé", null,"SUCCESS", null, null, null, null, authUserEppn, authUserEppn);
			}
		}
	}

	public void addPostit(Long signBookId, String comment, String userEppn, String authUserEppn) {
		SignBook signBook = signBookRepository.findById(signBookId).get();
		for(SignRequest signRequest : signBook.getSignRequests()) {
			if (comment != null && !comment.isEmpty()) {
				updateStatus(signRequest.getId(), signRequest.getStatus(), "comment", comment, "SUCCES", null, null, null, 0, userEppn, authUserEppn);
			}
		}
	}

	@Transactional
	public void cleanDocuments(SignRequest signRequest, String authUserEppn) {
		Date cleanDate = getEndDate(signRequest);
		if(cleanDate != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(cleanDate);
			cal.add(Calendar.DATE, globalProperties.getDelayBeforeCleaning());
			Date test = cal.getTime();
			Date now = new Date();
			if(signRequest.getExportedDocumentURI() != null
					&& test.getTime() < now.getTime()
					&& !signRequest.getSignedDocuments().isEmpty()) {
				clearAllDocuments(signRequest);
				updateStatus(signRequest.getId(), SignRequestStatus.exported, "Fichiers nettoyés", null, "SUCCESS", null, null, null, null, authUserEppn, authUserEppn);
			} else {
				logger.debug("cleanning documents was skipped because date");
			}
		} else {
			logger.error("no end date for signrequest " + signRequest.getId());
		}
	}

	@Transactional
	public void clearAllDocuments(SignRequest signRequest) {
		if(signRequest.getExportedDocumentURI() != null && !signRequest.getExportedDocumentURI().isEmpty()) {
			logger.info("clear all documents from signRequest : " + signRequest.getId());
			List<Document> documents = new ArrayList<>();
			documents.addAll(signRequest.getOriginalDocuments());
			documents.addAll(signRequest.getSignedDocuments());
			signRequest.getOriginalDocuments().clear();
			signRequest.getSignedDocuments().clear();
			deleteNexu(signRequest.getId());
			for(Document document : documents) {
				documentService.delete(document);
			}
		}
	}

	@Transactional
	public void deleteNexu(Long id) {
		SignRequest signRequest = signRequestRepository.findById(id).orElseThrow();
		List<NexuSignature> nexuSignatures = nexuSignatureRepository.findBySignRequestId(id);
		for(NexuSignature nexuSignature : nexuSignatures) {
			signRequest.getSignedDocuments().removeAll(nexuSignature.getDocumentToSign());
			nexuSignatureRepository.delete(nexuSignature);
		}
	}

	public FsFile getLastSignedFsFile(SignRequest signRequest) throws EsupSignatureFsException {
		if(signRequest.getStatus().equals(SignRequestStatus.exported)) {
			if (signRequest.getExportedDocumentURI() != null && !signRequest.getExportedDocumentURI().startsWith("mail")) {
				FsAccessService fsAccessService = fsAccessFactoryService.getFsAccessService(signRequest.getExportedDocumentURI());
				return fsAccessService.getFileFromURI(signRequest.getExportedDocumentURI());
			}
		}
		Document lastSignedDocument = getToSignDocuments(signRequest.getId()).get(0);
		return new FsFile(lastSignedDocument.getInputStream(), lastSignedDocument.getFileName(), lastSignedDocument.getContentType());
	}

	public Log updateStatus(Long signRequestId, SignRequestStatus signRequestStatus, String action, String comment, String returnCode, Integer pageNumber, Integer posX, Integer posY, Integer stepNumber, String userEppn, String authUserEppn) {
		SignBook signBook = getById(signRequestId).getParentSignBook();
		return logService.create(signRequestId, signBook.getSubject(), signBook.getWorkflowName(), signRequestStatus, action, comment, returnCode, pageNumber, posX, posY, stepNumber, userEppn, authUserEppn);
	}

	@Transactional
	public Long delete(Long signRequestId, String userEppn) {
		logger.info("start delete of signrequest " + signRequestId);
		SignRequest signRequest = getById(signRequestId);
		if(!isDeletetable(signRequest, userEppn)) {
			throw new EsupSignatureRuntimeException("Interdiction de supprimer les demandes de ce circuit");
		}
		if(signRequest.getDeleted() || signRequest.getStatus().equals(SignRequestStatus.draft)) {
			return deleteDefinitive(signRequestId, userEppn);
		} else {
			logService.create(signRequest.getId(), signRequest.getParentSignBook().getSubject(), signRequest.getParentSignBook().getWorkflowName(), SignRequestStatus.deleted, "Mise à la corbeille du document par l'utilisateur", "", "SUCCESS", null, null, null, null, userEppn, userEppn);
			if (signRequest.getStatus().equals(SignRequestStatus.exported) || signRequest.getArchiveStatus().equals(ArchiveStatus.archived)) {
				logger.info("nettoyage des documents archivés ou exportés");
				signRequest.getOriginalDocuments().clear();
				signRequest.getSignedDocuments().clear();
				logService.create(signRequest.getId(), signRequest.getParentSignBook().getSubject(), signRequest.getParentSignBook().getWorkflowName(), SignRequestStatus.deleted, "Nettoyage des documents déjà archivés", "", "SUCCESS", null, null, null, null, userEppn, userEppn);
			}
			if(signRequest.getParentSignBook().getSignRequests().stream().allMatch(SignRequest::getDeleted)) {
				signRequest.getParentSignBook().setDeleted(true);
				signRequest.getParentSignBook().setUpdateDate(new Date());
				signRequest.getParentSignBook().setUpdateBy(userEppn);
				logService.create(signRequest.getId(), signRequest.getParentSignBook().getSubject(), signRequest.getParentSignBook().getWorkflowName(), SignRequestStatus.deleted, "Mise à la corbeille de la demande par l'utilisateur", "", "SUCCESS", null, null, null, null, userEppn, userEppn);
			}
			signRequest.setDeleted(true);
			for (Target target : signRequest.getParentSignBook().getLiveWorkflow().getTargets().stream().filter(t -> t != null && fsAccessFactoryService.getPathIOType(t.getTargetUri()).equals(DocumentIOType.rest)).toList()) {
				try {
					targetService.sendRest(target.getTargetUri(), signRequest.getId().toString(), "deleted", signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber().toString(), userEppn, "");
				} catch (Exception e) {
					logger.error("error on sending deleted on rest " + target.getTargetUri());
				}

			}
			return signRequest.getParentSignBook().getId();
		}
	}

	@Transactional
	public Long deleteDefinitive(Long signRequestId, String userEppn) {
		logger.info("start definitive delete of signrequest " + signRequestId);
		SignRequest signRequest = getById(signRequestId);
		SignBook signBook = signRequest.getParentSignBook();
//		boolean testAllNone = signRequest.getRecipientHasSigned().values().stream().allMatch(a -> a.getActionType().equals(ActionType.none));
//		int step = signBook.getLiveWorkflow().getLiveWorkflowSteps().indexOf(signBook.getLiveWorkflow().getCurrentStep());
//		if(testAllNone && !signRequest.getDeleted() && step > 0) {
//			return -1L;
//		}
		deleteNexu(signRequestId);
		logService.create(signRequestId, signRequest.getParentSignBook().getSubject(), signRequest.getParentSignBook().getWorkflowName(), SignRequestStatus.deleted, "Suppression définitive", null, "SUCCESS", null, null, null,null, userEppn, userEppn);
		signRequest.getRecipientHasSigned().clear();
		signRequestRepository.save(signRequest);
		if (signRequest.getData() != null) {
			Long dataId = signRequest.getData().getId();
			signRequest.setData(null);
			dataService.deleteOnlyData(dataId);
		}
		List<Long> commentsIds = signRequest.getComments().stream().map(Comment::getId).toList();
		for (Long commentId : commentsIds) {
			commentService.deleteComment(commentId, signRequest);
		}
		signBook.getSignRequests().remove(signRequest);
		for(SignRequestParams signRequestParams : signRequest.getSignRequestParams()) {
			for(LiveWorkflowStep liveWorkflowStep : signBook.getLiveWorkflow().getLiveWorkflowSteps()) {
				liveWorkflowStep.getSignRequestParams().remove(signRequestParams);
			}
		}
		signRequestRepository.delete(signRequest);
		for (Target target : signRequest.getParentSignBook().getLiveWorkflow().getTargets().stream().filter(t -> t != null && fsAccessFactoryService.getPathIOType(t.getTargetUri()).equals(DocumentIOType.rest)).toList()) {
			try {
				targetService.sendRest(target.getTargetUri(), signRequest.getId().toString(), "cleaned", signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber().toString(), userEppn, "");
			} catch (Exception e) {
				logger.error("error on sending deleted on rest " + target.getTargetUri());
			}
		}
		long signBookId = 0;
		if(!signBook.getSignRequests().isEmpty()) {
			signBookId = signBook.getId();
		} else {
			signBookRepository.delete(signBook);
		}
		if(!signBook.getDeleted() && signBook.getStatus().equals(SignRequestStatus.pending) && signBook.getSignRequests().stream().allMatch(s -> s.getStatus().equals(SignRequestStatus.signed) || s.getStatus().equals(SignRequestStatus.completed) || s.getStatus().equals(SignRequestStatus.refused))) {
			boolean isNextWorkflow = nextWorkFlowStep(signBook);
			if(isNextWorkflow) {
				for(SignRequest signRequest1 : signRequest.getParentSignBook().getSignRequests()) {
					if(!signRequest1.equals(signRequest) && !signRequest1.getStatus().equals(SignRequestStatus.refused)) {
						pendingSignRequest(signRequest1, userEppn);
					}
				}
			} else {
				if(signBook.getSignRequests().stream().allMatch(s -> s.getStatus().equals(SignRequestStatus.refused))) {
					signBook.setStatus(SignRequestStatus.refused);
				} else {
					signBook.setStatus(SignRequestStatus.completed);
				}
			}
		}
		if(signRequest.getParentSignBook().getSignRequests().stream().allMatch(s -> s.getStatus().equals(SignRequestStatus.refused))) {
			signRequest.getParentSignBook().setStatus(SignRequestStatus.refused);
		}
		return signBookId;
	}

	public Date getEndDate(SignRequest signRequest) {
		List<Action> action = signRequest.getRecipientHasSigned().values().stream().filter(action1 -> !action1.getActionType().equals(ActionType.none)).sorted(Comparator.comparing(Action::getDate)).collect(Collectors.toList());
		if(!action.isEmpty()) {
			return action.get(0).getDate();
		}
		return null;
	}

	public boolean isDeletetable(SignRequest signRequest, String userEppn) {
		User user = userService.getByEppn(userEppn);
		return signRequest.getParentSignBook().getLiveWorkflow().getWorkflow() == null
				||
				signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getDisableDeleteByCreator() == null
				||
				!signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getDisableDeleteByCreator()
				||
				signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getManagers().contains(user.getEmail())
				||
				user.getRoles().contains("ROLE_ADMIN");
	}

	@Transactional
	public boolean isTempUsers(Long signBookId) {
		SignBook signBook = signBookRepository.findById(signBookId).get();
		return !userService.getTempUsers(signBook).isEmpty();
	}

	@Transactional
	public boolean checkTempUsers(Long id, List<RecipientWsDto> recipients) throws EsupSignatureRuntimeException {
		SignBook signBook = signBookRepository.findById(id).get();
		List<User> tempUsers = userService.getTempUsers(signBook, recipients);
		if(!tempUsers.isEmpty()) {
			if (recipients != null && tempUsers.size() <= recipients.size()) {
				for (User tempUser : tempUsers) {
					if (tempUser.getUserType().equals(UserType.shib)) {
						logger.warn("TODO Envoi Mail SHIBBOLETH ");
						//TODO envoi mail spécifique
					} else if (tempUser.getUserType().equals(UserType.external)) {
						RecipientWsDto recipientWsDto = recipients.stream().filter(recipientWsDto1 -> recipientWsDto1.getEmail().toLowerCase().equals(tempUser.getEmail().toLowerCase(Locale.ROOT))).findFirst().get();
						tempUser.setFirstname(recipientWsDto.getFirstName());
						tempUser.setName(recipientWsDto.getName());
						if(StringUtils.hasText(recipientWsDto.getPhone())) {
							userService.updatePhone(tempUser.getEppn(), recipientWsDto.getPhone());
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
				&& !data.getDatas().isEmpty()
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
		if(!signRequest.getSignedDocuments().isEmpty()) {
			return signRequest.getSignedDocuments().get(signRequest.getSignedDocuments().size() - 1);
		} else {
			return null;
		}
	}


	@Transactional
	public boolean isSigned(SignRequest signRequest, Reports reports) {
		try {
			if(reports == null) {
				reports = validate(signRequest.getId());
			}
			List<Document> documents = getToSignDocuments(signRequest.getId());
			if (!documents.isEmpty() && (signRequest.getParentSignBook().getLiveWorkflow() != null && signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() != null)) {
				return (reports != null && !reports.getSimpleReport().getSignatureIdList().isEmpty());
			}
		} catch (Exception e) {
			logger.error("error while checking if signRequest is signed", e);
		}
		return false;
	}

	@Transactional
	public boolean isSigned(SignBook signBook, Reports reports) {
		for (SignRequest signRequest : signBook.getSignRequests()) {
			if (isSigned(signRequest, reports)) {
				return true;
			}
		}
		return false;
	}

	@Transactional
	public Reports validate(long signRequestId) throws IOException {
		List<Document> documents = getToSignDocuments(signRequestId);
		if(!documents.isEmpty()) {
			byte[] bytes = documents.get(0).getInputStream().readAllBytes();
			return validationService.validate(new ByteArrayInputStream(bytes), null);
		} else {
			return null;
		}
	}

	@Transactional
	public boolean addAttachement(MultipartFile[] multipartFiles, String link, Long signRequestId, String authUserEppn) throws EsupSignatureIOException {
		SignRequest signRequest = getById(signRequestId);
		int nbAttachmentAdded = 0;
		if(multipartFiles != null) {
			for (MultipartFile multipartFile : multipartFiles) {
				if(multipartFile.getSize() > 0) {
					addAttachmentToSignRequest(signRequest, authUserEppn, multipartFile);
					nbAttachmentAdded++;
					logService.create(signRequestId, signRequest.getParentSignBook().getSubject(), signRequest.getParentSignBook().getWorkflowName(), signRequest.getStatus(), "Ajout d'une pièce jointe", multipartFile.getOriginalFilename(), "SUCCESS", null, null, null, null, authUserEppn, authUserEppn);
				}
			}
		}
		if(link != null && !link.isEmpty()) {
			signRequest.getLinks().add(link);
			nbAttachmentAdded++;
		}
		return nbAttachmentAdded > 0;
	}

	@Transactional
	public void removeAttachement(Long id, Long attachementId, RedirectAttributes redirectAttributes) {
		SignRequest signRequest = getById(id);
		Document attachement = documentService.getById(attachementId);
		if (!attachement.getParentId().equals(signRequest.getId())) {
			redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Pièce jointe non trouvée ..."));
		} else {
			signRequest.getAttachments().remove(attachement);
			documentService.delete(attachement);
		}
	}

	@Transactional
	public void removeLink(Long id, Integer linkId) {
		SignRequest signRequest = getById(id);
		String toRemove = new ArrayList<>(signRequest.getLinks()).get(linkId);
		signRequest.getLinks().remove(toRemove);
	}

	@Transactional
	public Long addComment(Long id, String commentText, Integer commentPageNumber, Integer commentPosX, Integer commentPosY, Integer commentWidth, Integer commentHeight, String postit, Integer spotStepNumber, String authUserEppn, String userEppn, boolean forceSend) {
		SignRequest signRequest = getById(id);
		User user = userService.getByEppn(userEppn);
		if(spotStepNumber == null || signRequest.getCreateBy().equals(user) || signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getManagers().contains(user.getEmail())) {
			if (spotStepNumber != null && spotStepNumber > 0) {
				SignRequestParams signRequestParams = signRequestParamsService.createSignRequestParams(commentPageNumber, commentPosX, commentPosY);
				if(commentWidth != null && commentHeight != null) {
					signRequestParams.setSignWidth(commentWidth);
					signRequestParams.setSignHeight(commentHeight);
				}
				int docNumber = signRequest.getParentSignBook().getSignRequests().indexOf(signRequest);
				signRequestParams.setSignDocumentNumber(docNumber);
				signRequestParams.setComment(commentText);
				signRequest.getSignRequestParams().add(signRequestParams);
				signRequest.getParentSignBook().getLiveWorkflow().getLiveWorkflowSteps().get(spotStepNumber - 1).getSignRequestParams().add(signRequestParams);
			}
			Comment comment = commentService.create(id, commentText, commentPosX, commentPosY, commentPageNumber, spotStepNumber, "on".equals(postit), null, authUserEppn);
			if(commentWidth != null && commentHeight != null) {
				comment.setSignWidth(commentWidth);
				comment.setSignHeight(commentHeight);
			}
			if (!(spotStepNumber != null && spotStepNumber > 0)) {
				updateStatus(signRequest.getId(), null, "Ajout d'un commentaire", commentText, "SUCCESS", commentPageNumber, commentPosX, commentPosY, null, authUserEppn, authUserEppn);
				if ((globalProperties.getSendPostitByEmail() || forceSend)) {
					try {
						mailService.sendPostit(signRequest.getParentSignBook(), comment, userEppn, forceSend);
					} catch (EsupSignatureMailException e) {
						logger.warn("postit not sended", e);
					}
				}
			} else {
				updateStatus(signRequest.getId(), null, "Ajout d'un emplacement de signature", commentText, "SUCCESS", commentPageNumber, commentPosX, commentPosY, null, authUserEppn, authUserEppn);
			}
			return comment.getId();
		} else {
			return null;
		}
	}

	public Long getNbPendingSignRequests(String userEppn) {
		return signRequestRepository.countByCreateByEppnAndStatus(userEppn, SignRequestStatus.pending);
	}

	public Long getNbDraftSignRequests(String userEppn) {
		return signRequestRepository.countByCreateByEppnAndStatus(userEppn, SignRequestStatus.draft);
	}

	@Transactional
	public boolean getOriginalFileResponse(Long signRequestId, HttpServletResponse httpServletResponse) throws IOException {
		SignRequest signRequest = getById(signRequestId);
		webUtilsService.copyFileStreamToHttpResponse(signRequest.getOriginalDocuments().get(0).getFileName(), signRequest.getOriginalDocuments().get(0).getContentType(), "attachment", signRequest.getOriginalDocuments().get(0).getInputStream(), httpServletResponse);
		return true;
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
	public void getToSignFileResponse(Long signRequestId, String disposition, HttpServletResponse httpServletResponse, boolean force) throws IOException, EsupSignatureRuntimeException, EsupSignatureException {
		SignRequest signRequest = getById(signRequestId);
		if(!force && !disposition.equals("form-data")
				&& signRequest.getParentSignBook().getLiveWorkflow().getWorkflow() != null
				&&  BooleanUtils.isTrue(signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getForbidDownloadsBeforeEnd())
				&& !signRequest.getStatus().equals(SignRequestStatus.completed)
				&& !signRequest.getStatus().equals(SignRequestStatus.refused)
				&& !signRequest.getArchiveStatus().equals(ArchiveStatus.archived)
				&& !signRequest.getArchiveStatus().equals(ArchiveStatus.cleaned)
				&& !signRequest.getStatus().equals(SignRequestStatus.exported)) {
			throw new EsupSignatureException("Téléchargement interdit avant la fin du circuit");
		}
		if (!signRequest.getStatus().equals(SignRequestStatus.exported)) {
			List<Document> documents = getToSignDocuments(signRequest.getId());
			Document document;
			if(!documents.isEmpty()) {
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
			List<Document> documents = getToSignDocuments(signRequest.getId());
			Document document;
			if(!documents.isEmpty()) {
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
	public void getSignedFileAndReportResponse(Long signRequestId, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, boolean force) throws Exception {
		SignRequest signRequest = getById(signRequestId);
		if(!force
			&& signRequest.getParentSignBook().getLiveWorkflow().getWorkflow() != null
			&& BooleanUtils.isTrue(signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getForbidDownloadsBeforeEnd())
			&& (!signRequest.getStatus().equals(SignRequestStatus.completed)
			&& !signRequest.getStatus().equals(SignRequestStatus.refused)
			&& !signRequest.getStatus().equals(SignRequestStatus.exported)
			&& !signRequest.getArchiveStatus().equals(ArchiveStatus.archived)
			&& !signRequest.getArchiveStatus().equals(ArchiveStatus.cleaned))) {
			throw new EsupSignatureException("Téléchargement interdit avant la fin du circuit");
		}
		webUtilsService.copyFileStreamToHttpResponse(signRequest.getParentSignBook().getSubject() + "-avec_rapport.zip", "application/zip; charset=utf-8", "attachment", new ByteArrayInputStream(getZipWithDocAndReport(signRequest, httpServletRequest, httpServletResponse)), httpServletResponse);
	}

	@Transactional
	public byte[] getZipWithDocAndReport(SignRequest signRequest, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
		Map<InputStream, String> inputStreams = new HashMap<>();
		String name = "";
		InputStream inputStream = null;
		if (signRequest.getStatus().equals(SignRequestStatus.completed) || signRequest.getStatus().equals(SignRequestStatus.refused)) {
			List<Document> documents = getToSignDocuments(signRequest.getId());
			if(documents.size() == 1) {
				name = documents.get(0).getFileName();
				inputStream = documents.get(0).getInputStream();
			}
		} else if (signRequest.getStatus().equals(SignRequestStatus.exported) || signRequest.getArchiveStatus().equals(ArchiveStatus.archived) || signRequest.getArchiveStatus().equals(ArchiveStatus.cleaned)) {
			FsFile fsFile = getLastSignedFsFile(signRequest);
			name = fsFile.getName();
			inputStream = fsFile.getInputStream();
		} else {
			throw new EsupSignatureException("Impossible de générer le zip, la demande n'est pas signée");
		}
		if(inputStream != null) {
			byte[] fileByte = inputStream.readAllBytes();
			inputStreams.put(new ByteArrayInputStream(fileByte), name);
			int i = 0;
			for(Document document : signRequest.getAttachments()) {
				inputStreams.put(document.getInputStream(), i + "_" + document.getFileName());
				i++;
			}

			ByteArrayOutputStream auditTrail = auditTrailService.generateAuditTrailPdf(signRequest, httpServletRequest, httpServletResponse);
			inputStreams.put(new ByteArrayInputStream(auditTrail.toByteArray()), "dossier-de-preuve.pdf");

			Reports reports = validationService.validate(new ByteArrayInputStream(fileByte), null);
			if(reports != null) {
				ByteArrayOutputStream reportByteArrayOutputStream = new ByteArrayOutputStream();
				fopService.generateSimpleReport(reports.getXmlSimpleReport(), reportByteArrayOutputStream);
				inputStreams.put(new ByteArrayInputStream(reportByteArrayOutputStream.toByteArray()), "rapport-signature.pdf");

			}
			return fileService.zipDocuments(inputStreams);
		}
		return null;
	}

	@Transactional
	public void getFileResponse(Long documentId, HttpServletResponse httpServletResponse) throws IOException {
		Document document = documentService.getById(documentId);
		SignRequest signRequest = getById(document.getParentId());
		if(SecurityContextHolder.getContext().getAuthentication().getAuthorities()
				.stream()
				.noneMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"))
			&&
				signRequest.getParentSignBook().getLiveWorkflow().getWorkflow() != null && BooleanUtils.isTrue(signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getForbidDownloadsBeforeEnd()) && !signRequest.getStatus().equals(SignRequestStatus.completed)) {
			throw new EsupSignatureRuntimeException("Téléchargement interdit avant la fin du circuit");
		}
		webUtilsService.copyFileStreamToHttpResponse(document.getFileName(), document.getContentType(), "attachment", document.getInputStream(), httpServletResponse);
	}

	@Transactional
	public List<Document> getAttachments(Long signRequestId) {
		SignRequest signRequest = getById(signRequestId);
		return new ArrayList<>(signRequest.getAttachments());
	}

	@Transactional
	public boolean replayNotif(Long id, String userEppn) throws EsupSignatureMailException {
		SignRequest signRequest = this.getById(id);
		if (isDisplayNotif(signRequest, userEppn)) {
			List<String> recipientEmails = new ArrayList<>();
			List<Recipient> recipients = getCurrentRecipients(signRequest);
			for (Recipient recipient : recipients) {
				if (recipient.getUser() != null && recipient.getUser().getEmail() != null) {
					recipientEmails.add(recipient.getUser().getEmail());
				}
			}
			long notifTime = Long.MAX_VALUE;
			if (signRequest.getParentSignBook().getLastNotifDate() != null) {
				notifTime = Duration.between(signRequest.getParentSignBook().getLastNotifDate().toInstant(), new Date().toInstant()).toHours();
			}
			if (!recipientEmails.isEmpty() && notifTime >= globalProperties.getHoursBeforeRefreshNotif() && signRequest.getStatus().equals(SignRequestStatus.pending)) {
				mailService.sendSignRequestReplayAlert(recipientEmails, signRequest.getParentSignBook());
				return true;
			}
		}
		return false;
	}

	public boolean isDisplayNotif(SignRequest signRequest, String userEppn) {
		User user = userService.getByEppn(userEppn);
		boolean displayNotif = false;
		if (signRequest.getParentSignBook().getStatus().equals(SignRequestStatus.pending) &&
				(signRequest.getCreateBy().getEppn().equals(userEppn)
						|| (signRequest.getParentSignBook().getLiveWorkflow().getWorkflow() != null &&
						(signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getManagers().contains(user.getEmail())
						|| !Collections.disjoint(signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getDashboardRoles(), user.getRoles())))
				)
				&&
				((signRequest.getParentSignBook().getLastNotifDate() == null && Duration.between(signRequest.getParentSignBook().getCreateDate().toInstant(), new Date().toInstant()).toHours() >= globalProperties.getHoursBeforeRefreshNotif()) ||
						(signRequest.getParentSignBook().getLastNotifDate() != null && Duration.between(signRequest.getParentSignBook().getLastNotifDate().toInstant(), new Date().toInstant()).toHours() >= globalProperties.getHoursBeforeRefreshNotif()))) {
			displayNotif = true;
		}
		return displayNotif;
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
				if(userService.findPersonLdapLightByUser(user) != null
						|| user.getUserType().equals(UserType.external)
						|| user.getUserType().equals(UserType.shib)) {
					recipientNotPresentsignRequests.remove(signRequest);
				}
			}
		}
		return recipientNotPresentsignRequests;
	}

	@Transactional
	public List<SignRequestParams> getToUseSignRequestParams(Long id, String userEppn) {
		User user = userService.getByEppn(userEppn);
		List<SignRequestParams> toUserSignRequestParams = new ArrayList<>();
		SignRequest signRequest = getById(id);
		int signOrderNumber = signRequest.getParentSignBook().getSignRequests().indexOf(signRequest);
		if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() != null) {
			if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getAllSignToComplete()) {
				for (Recipient recipient : signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients()) {
					if (!signRequest.getRecipientHasSigned().isEmpty() && signRequest.getRecipientHasSigned().get(recipient) != null && !signRequest.getRecipientHasSigned().get(recipient).getActionType().equals(ActionType.none)) {
						return toUserSignRequestParams;
					}
				}
			}
			List<SignRequestParams> signRequestParamsForCurrentStep = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams().stream().filter(signRequestParams -> signRequestParams.getSignDocumentNumber().equals(signOrderNumber)).toList();
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
		if(signRequest != null) {
			Document toValideDocument = signRequest.getLastSignedDocument();
			if (toValideDocument != null) {
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				IOUtils.copy(toValideDocument.getInputStream(), outputStream);
				outputStream.close();
				return new ByteArrayInputStream(outputStream.toByteArray());
			}
		}
		return null;
	}

	@Transactional
	public String getAllToJSon(String xApikey) throws JsonProcessingException {
		WsAccessToken wsAccessToken = wsAccessTokenRepository.findByToken(xApikey);
		if(wsAccessToken == null || wsAccessToken.getWorkflows().isEmpty()) {
			return objectMapper.writeValueAsString(signRequestRepository.findAllForWs());
		}
		return objectMapper.writeValueAsString(signRequestRepository.findAllByToken(wsAccessToken));
	}

	public boolean isAttachmentAlert(SignRequest signRequest) {
		boolean attachmentAlert = false;
		if (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() != null
			&& (
			(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep() != null
				&& signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep().getAttachmentAlert() != null
				&& signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep().getAttachmentAlert())
				||
				(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getAttachmentAlert() != null
				&& signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getAttachmentAlert())
			)
		&& signRequest.getAttachments().isEmpty()) {
			attachmentAlert = true;
		}
		return attachmentAlert;
	}
	public boolean isAttachmentRequire(SignRequest signRequest) {
		boolean attachmentRequire = false;
		if (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() != null
			&& (
			(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep() != null
				&& signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep().getAttachmentRequire() != null
				&& signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep().getAttachmentRequire())
				||
				(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getAttachmentRequire() != null
				&& signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getAttachmentRequire())
			)
		&& signRequest.getAttachments().isEmpty()) {
			attachmentRequire = true;
		}
		return attachmentRequire;
	}

	@Transactional
	public void warningReaded(String authUserEppn) {
		User authUser = userService.getByEppn(authUserEppn);
		List<SignRequest> oldSignRequests = signRequestRepository.findByCreateByEppnAndOlderPending(authUser.getId(), globalProperties.getNbDaysBeforeWarning());
		for (SignRequest signRequest : oldSignRequests) {
			signRequest.setWarningReaded(true);
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
		if(signRequest != null) {
			return objectMapper.writeValueAsString(signRequest.getAuditTrail());
		} else {
			logger.warn("audit trail not found for " + id);
			return "";
		}
	}

	@Transactional
	public void updateComment(Long signRequestId, Long postitId, String text) throws EsupSignatureException {
		Comment comment = commentService.getById(postitId);
		SignRequest signRequest = signRequestRepository.findById(signRequestId).orElseThrow();
		if(!signRequest.getStatus().equals(SignRequestStatus.refused)) {
			comment.setText(text);
		} else {
			throw new EsupSignatureException("Demande refusé, modification impossible");
		}
	}

	@Transactional
	public void deleteComment(Long signRequestId, Long postitId) throws EsupSignatureException {
		Comment comment = commentService.getById(postitId);
		SignRequest signRequest = signRequestRepository.findById(signRequestId).orElseThrow();
		if(!signRequest.getStatus().equals(SignRequestStatus.refused)) {
			signRequest.getComments().remove(comment);
			commentService.deleteComment(postitId, signRequest);
		} else {
			throw new EsupSignatureException("Demande refusé, modification impossible");
		}
	}

	public List<SignRequest> getByIdAndRecipient(Long id, String userEppn) {
		return signRequestRepository.findByIdAndRecipient(id, userEppn);
	}

	@Transactional
	public void viewedBy(Long signRequestId, String userEppn) {
		User user = userService.getByEppn(userEppn);
		SignRequest signRequest = getById(signRequestId);
		signRequest.getViewedBy().add(user);
	}

	@Transactional
	public String smallCheckPDFA(Long signRequestId) throws IOException {
		SignRequest signRequest = getById(signRequestId);
		byte[] bytes = signRequest.getOriginalDocuments().get(0).getInputStream().readAllBytes();
		return smallCheckPDFA(bytes);
	}

	private String smallCheckPDFA(byte[] bytes) {
		List<String> results = pdfService.checkPDFA(bytes, true);
		StringBuilder pdfaCheck = new StringBuilder();
		for(String result : results) {
			if(result.startsWith("6") && !pdfaCheck.toString().contains(result.substring(0, 5))) {
				pdfaCheck.append(result, 0, 5).append(",");
			}
		}
		return pdfaCheck.toString();
	}

    public List<RecipientWsDto> getExternalRecipients(Long signRequestId) {
		SignRequest signRequest = getById(signRequestId);
		return signRequest.getParentSignBook().getTeam().stream().filter(user -> user.getUserType().equals(UserType.external)).map(user -> new RecipientWsDto(user.getId(), user.getEmail())).collect(Collectors.toList());
    }

	@Transactional
	public List<SignRequestStepsDto> getStepsDto(Long id) {
		List<SignRequestStepsDto> signRequestStepsDtos = new ArrayList<>();
		SignRequest signRequest = getById(id);
		int i = 1;
		for(LiveWorkflowStep liveWorkflowStep : signRequest.getParentSignBook().getLiveWorkflow().getLiveWorkflowSteps()) {
			SignRequestStepsDto signRequestStepsDto = new SignRequestStepsDto();
			signRequestStepsDto.setStepNumber(i);
			signRequestStepsDto.setSignType(liveWorkflowStep.getSignType().name());
			signRequestStepsDto.setAllSignToComplete(liveWorkflowStep.getAllSignToComplete());
			for(Recipient recipient : liveWorkflowStep.getRecipients()) {
				RecipientsActionsDto recipientsActionsDto = new RecipientsActionsDto();
				recipientsActionsDto.setStepNumber(i);
				Action action = signRequest.getRecipientHasSigned().get(recipient);
				if(action != null) {
					recipientsActionsDto.setActionDate(action.getDate());
					recipientsActionsDto.setActionType(action.getActionType().name());
					if (action.getActionType().equals(ActionType.refused)) {
						Optional<Log> refuseLog = logService.getRefuseLogs(signRequest.getParentSignBook().getId()).stream().filter(l -> l.getComment() != null).findAny();
						refuseLog.ifPresent(l -> recipientsActionsDto.setRefuseComment(l.getComment()));
					}
				}
				User user = recipient.getUser();
				recipientsActionsDto.setUserEppn(user.getEppn());
				recipientsActionsDto.setUserName(user.getName());
				recipientsActionsDto.setUserFirstname(user.getFirstname());
				recipientsActionsDto.setUserEmail(user.getEmail());
				AuditTrail auditTrail = auditTrailService.getAuditTrailByToken(signRequest.getToken());
				if(auditTrail != null && auditTrail.getAuditSteps().size() >= i) {
					AuditStep auditStep = auditTrailService.getAuditTrailByToken(signRequest.getToken()).getAuditSteps().get(i - 1);
					recipientsActionsDto.setSignPageNumber(auditStep.getPage());
					recipientsActionsDto.setSignPosX(auditStep.getPosX());
					recipientsActionsDto.setSignPosY(auditStep.getPosY());
				}
				signRequestStepsDto.getRecipientsActions().add(recipientsActionsDto);
			}
			signRequestStepsDtos.add(signRequestStepsDto);
			i++;
		}
		return signRequestStepsDtos;
	}

	@Transactional
	public SignatureDocumentForm getAbstractSignatureForm(List<Document> documents, SignRequest signRequest, boolean includeDocuments) throws IOException, EsupSignatureRuntimeException {
		SignatureForm signatureForm;
		AbstractSignatureForm abstractSignatureForm;
		if(documents.size() > 1) {
			signatureForm = signProperties.getDefaultSignatureForm();
			abstractSignatureForm = new SignatureMultipleDocumentsForm();
			if(includeDocuments) {
				List<DssMultipartFile> multipartFiles = new ArrayList<>();
				for (Document toSignFile : documents) {
					multipartFiles.add(toSignFile.getMultipartFile());
				}
				((SignatureMultipleDocumentsForm) abstractSignatureForm).setDocumentsToSign(multipartFiles);
			}
			((SignatureMultipleDocumentsForm) abstractSignatureForm).setContainerType(signProperties.getContainerType());
		} else {
			InputStream inputStream;
			byte[] bytes;
			Document toSignFile = documents.get(0);
			if(toSignFile.getContentType().equals("application/pdf")) {
				signatureForm = SignatureForm.PAdES;
				inputStream = toSignFile.getTransientInputStream();
				bytes = inputStream.readAllBytes();
				if(!isSigned(signRequest, null) && !pdfService.isPdfAComplient(bytes)) {
					bytes = pdfService.convertToPDFA(pdfService.writeMetadatas(bytes, toSignFile.getFileName(), signRequest, new ArrayList<>()));
				}
			} else {
				signatureForm = signProperties.getDefaultSignatureForm();
				bytes = toSignFile.getTransientInputStream().readAllBytes();
			}
			abstractSignatureForm = new SignatureDocumentForm();
			((SignatureDocumentForm) abstractSignatureForm).setDocumentToSign(new DssMultipartFile(toSignFile.getFileName(), toSignFile.getFileName(), toSignFile.getContentType(), bytes));
			if(!signatureForm.equals(SignatureForm.PAdES)) {
				((SignatureDocumentForm) abstractSignatureForm).setContainerType(signProperties.getContainerType());
			}
		}
		abstractSignatureForm.setSignatureForm(signatureForm);
		if(signatureForm.equals(SignatureForm.PAdES)) {
			abstractSignatureForm.setSignatureLevel(signProperties.getPadesSignatureLevel());
			abstractSignatureForm.setDigestAlgorithm(signProperties.getPadesDigestAlgorithm());
		} else if(signatureForm.equals(SignatureForm.CAdES)) {
			abstractSignatureForm.setSignatureLevel(signProperties.getCadesSignatureLevel());
			abstractSignatureForm.setDigestAlgorithm(signProperties.getCadesDigestAlgorithm());
		} else if(signatureForm.equals(SignatureForm.XAdES)) {
			abstractSignatureForm.setSignatureLevel(signProperties.getXadesSignatureLevel());
			abstractSignatureForm.setDigestAlgorithm(signProperties.getXadesDigestAlgorithm());
		}
		abstractSignatureForm.setSigningDate(new Date());
        assert abstractSignatureForm instanceof SignatureDocumentForm;
        return (SignatureDocumentForm) abstractSignatureForm;
	}

	@Transactional
	public void cleanSignRequestParams(Long id) throws IOException {
		SignRequest signRequest = getById(id);
		Reports reports = validate(id);
		if(reports.getSimpleReport().getSignatureIdList().isEmpty() && signRequest.getParentSignBook().getLiveWorkflow().getWorkflow() == null) {
			signRequest.getSignRequestParams().clear();
			signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams().clear();
		}
	}

	public boolean isCurrentUserAsSigned(SignRequest signRequest, String userEppn) {
		User user = userService.getByEppn(userEppn);
		return signRequest.getRecipientHasSigned().entrySet().stream().anyMatch(rhs -> rhs.getValue().getActionType().equals(ActionType.signed) && rhs.getKey().getUser().getEppn().equals(user.getEppn()));
	}
}
