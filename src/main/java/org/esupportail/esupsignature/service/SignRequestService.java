package org.esupportail.esupsignature.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.zxing.WriterException;
import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.validation.reports.Reports;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dto.RecipientWsDto;
import org.esupportail.esupsignature.dto.js.JsMessage;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.*;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureMailException;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessFactoryService;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessService;
import org.esupportail.esupsignature.service.interfaces.fs.FsFile;
import org.esupportail.esupsignature.service.interfaces.prefill.PreFillService;
import org.esupportail.esupsignature.service.mail.MailService;
import org.esupportail.esupsignature.service.security.otp.OtpService;
import org.esupportail.esupsignature.service.utils.StepStatus;
import org.esupportail.esupsignature.service.utils.WebUtilsService;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.esupportail.esupsignature.service.utils.metric.CustomMetricsService;
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.esupportail.esupsignature.service.utils.sign.NexuService;
import org.esupportail.esupsignature.service.utils.sign.SignService;
import org.esupportail.esupsignature.service.utils.sign.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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

	@Resource
	private TargetService targetService;

	@Resource
	private NexuService nexuService;

	public SignRequestService(GlobalProperties globalProperties, SignBookRepository signBookRepository) {
		this.globalProperties = globalProperties;
		this.signBookRepository = signBookRepository;
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
	private SignTypeService signTypeService;

	@Resource
	private UserService userService;

	@Resource
	private DataService dataService;

	@Resource
	private CommentService commentService;

	@Resource
	private MailService mailService;

	@Resource
	private AuditTrailService auditTrailService;

	@Resource
	private UserShareService userShareService;

	@Resource
	private RecipientService recipientService;

	@Resource
	private FsAccessFactoryService fsAccessFactoryService;

	@Resource
	private OtpService otpService;

	@Resource
	private FileService fileService;

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

	private final SignBookRepository signBookRepository;

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
			if(!logs.isEmpty()) {
				return "fully-deleted";
			}
		}
		return null;
	}

	public Optional<SignRequest> getSignRequestByToken(String token) {
		return signRequestRepository.findByToken(token);
	}

	public SignRequest getNextSignRequest(Long signRequestId, String userEppn) {
		SignRequest currentSignRequest = getById(signRequestId);
		Optional<SignRequest> inSameSignBookSignRequest = currentSignRequest.getParentSignBook().getSignRequests().stream().filter(signRequest -> signRequest.getStatus().equals(SignRequestStatus.pending) && !signRequest.equals(currentSignRequest)).findAny();
		if(inSameSignBookSignRequest.isPresent()) {
			return inSameSignBookSignRequest.get();
		}
		List<SignRequest> signRequests = getToSignRequests(userEppn).stream().filter(signRequest -> signRequest.getStatus().equals(SignRequestStatus.pending) && !signRequest.getId().equals(signRequestId)).sorted(Comparator.comparingLong(SignRequest::getId)).collect(Collectors.toList());
		if(!signRequests.isEmpty()) {
			return signRequests.get(0);
		} else {
			return null;
		}
	}

	@Transactional
	public boolean isEditable(long id, String userEppn) {
		SignRequest signRequest = getById(id);
		User user = userService.getByEppn(userEppn);
		SignBook signBook = signRequest.getParentSignBook();
		if ((signRequest.getStatus().equals(SignRequestStatus.pending)
				&& (isUserInRecipients(signRequest, userEppn)
				|| signRequest.getCreateBy().getEppn().equals(userEppn)
				|| signBook.getViewers().contains(user)))
				|| (signRequest.getStatus().equals(SignRequestStatus.draft)
				&& signRequest.getCreateBy().getEppn().equals(user.getEppn()))
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
	public StepStatus sign(SignRequest signRequest, String password, String signWith, List<SignRequestParams> signRequestParamses, Map<String, String> formDataMap, String userEppn, String authUserEppn, Long userShareId, String comment) throws EsupSignatureRuntimeException, IOException {
		User user = userService.getByEppn(userEppn);
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
		List<Document> toSignDocuments = signService.getToSignDocuments(signRequest.getId());
		SignType signType = signRequest.getCurrentSignType();
		byte[] filledInputStream;
		boolean isForm = false;
		if(!isNextWorkFlowStep(signRequest.getParentSignBook())) {
			Data data = dataService.getBySignRequest(signRequest);
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
				throw new EsupSignatureRuntimeException("Il manque une signature !");
			}
			if (toSignDocuments.size() == 1 && toSignDocuments.get(0).getContentType().equals("application/pdf") && visual) {
				for(SignRequestParams signRequestParams : signRequestParamses) {
					signedInputStream = pdfService.stampImage(signedInputStream, signRequest, signRequestParams, 1, signerUser, date, userService.getRoles(userEppn).contains("ROLE_OTP"), false);
					if(signRequestParams.getSignImageNumber() < 0) {
						lastSignLogs.add(updateStatus(signRequest.getId(), signRequest.getStatus(), "Ajout d'un élément", null, "SUCCESS", signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos(), signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber(), userEppn, authUserEppn));
						auditTrailService.addAuditStep(signRequest.getToken(), userEppn, "Ajout d'un élément", "Pas de timestamp", date, isViewed, signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos());
					} else {
						lastSignLogs.add(updateStatus(signRequest.getId(), signRequest.getStatus(), "Apposition de la signature", null, "SUCCESS", signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos(), signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber(), userEppn, authUserEppn));
						auditTrailService.addAuditStep(signRequest.getToken(), userEppn, "Signature simple", "Pas de timestamp", date, isViewed, signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos());
					}
				}
			} else {
				auditTrailService.addAuditStep(signRequest.getToken(), userEppn, "Signature simple", "Pas de timestamp", date, isViewed, null, null, null);
			}
			if (isStepAllSignDone(signRequest.getParentSignBook()) && (reports == null || reports.getSimpleReport().getSignatureIdList().isEmpty())) {
				signedInputStream = pdfService.convertGS(pdfService.writeMetadatas(signedInputStream, fileName, signRequest, lastSignLogs));
			}
			byte[] signedBytes = signedInputStream;

			stepStatus = applyEndOfSignRules(signRequest.getId(), userEppn, authUserEppn, signType, comment);
			documentService.addSignedFile(signRequest, new ByteArrayInputStream(signedBytes), signRequest.getTitle() + "." + fileService.getExtension(toSignDocuments.get(0).getFileName()), toSignDocuments.get(0).getContentType(), user);
		} else {
			reports = validationService.validate(getToValidateFile(signRequest.getId()), null);
			DiagnosticData diagnosticData = reports.getDiagnosticData();
			if(diagnosticData.getAllSignatures().isEmpty()) {
				if (signRequestParamses.size() > 1) {
					for (SignRequestParams signRequestParams : signRequestParamses) {
						filledInputStream = pdfService.stampImage(filledInputStream, signRequest, signRequestParams, 1, signerUser, date, userService.getRoles(userEppn).contains("ROLE_OTP"), true);
						lastSignLogs.add(updateStatus(signRequest.getId(), signRequest.getStatus(), "Ajout d'un élément", null, "SUCCESS", signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos(), signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber(), userEppn, authUserEppn));
						auditTrailService.addAuditStep(signRequest.getToken(), userEppn, "Ajout d'un élément", "Pas de timestamp", date, isViewed, signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos());
					}
				}
			} else {
				logger.warn("skip add visuals because document already signed");
			}
			if (toSignDocuments.size() == 1 && toSignDocuments.get(0).getContentType().equals("application/pdf")) {
				signRequestParamsService.copySignRequestParams(signRequest, signRequestParamses);
				toSignDocuments.get(0).setTransientInputStream(new ByteArrayInputStream(filledInputStream));
			}
			Document signedDocument = signService.certSign(signRequest, signerUser.getEppn(), password, SignWith.valueOf(signWith));
			reports = validationService.validate(signedDocument.getInputStream(), null);
			diagnosticData = reports.getDiagnosticData();
			String certificat = new ArrayList<>(diagnosticData.getAllSignatures()).get(diagnosticData.getAllSignatures().size() - 1).getSigningCertificate().toString();
			String timestamp = "no timestamp found";
			if(!diagnosticData.getTimestampList().isEmpty()) {
				timestamp = diagnosticData.getTimestampList().get(0).getSigningCertificate().toString();
			}
			if (!signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams().isEmpty()) {
				SignRequestParams signRequestParams = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams().get(0);
				auditTrailService.addAuditStep(signRequest.getToken(), userEppn, certificat, timestamp, reports.getSimpleReport().getValidationTime(), isViewed, signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos());
			} else {
				auditTrailService.addAuditStep(signRequest.getToken(), userEppn, certificat, timestamp, reports.getSimpleReport().getValidationTime(), isViewed, 0, 0, 0);
			}
			stepStatus = applyEndOfSignRules(signRequest.getId(), userEppn, authUserEppn, SignType.certSign, comment);

		}
		customMetricsService.incValue("esup-signature.signrequests", "signed");
		return stepStatus;
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
	public void seal(Long signRequestId) {
		SignRequest signRequest = getById(signRequestId);
		Document document = signService.certSign(signRequest, "system", "", SignWith.sealCert);
		if(signRequest.getSignedDocuments().size() > 1) {
			signRequest.getSignedDocuments().remove(signRequest.getSignedDocuments().size() - 1);
		}
		if(signRequest.getSignedDocuments().size() > 1) {
			signRequest.getSignedDocuments().remove(signRequest.getSignedDocuments().size() - 2);
		}
		signRequest.getSignedDocuments().add(document);
	}

	public Long nbToSignSignRequests(String userEppn) {
		return signRequestRepository.countByRecipientUserToSign(userEppn);
	}

	public Long nbFollowedByMe(String userEppn) {
		return signBookRepository.countByViewersContaining(userEppn);
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
						if(signRequestParamses.isEmpty()) {
							toAddSignRequestParams = signRequestParamsService.scanSignatureFields(new ByteArrayInputStream(bytes), docNumber);
						} else {
							for (SignRequestParams signRequestParams : signRequestParamses) {
								toAddSignRequestParams.add(signRequestParamsService.createSignRequestParams(signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos(), signRequestParams.getSignWidth(), signRequestParams.getSignHeight()));
							}
						}
						signRequest.getSignRequestParams().addAll(toAddSignRequestParams);
						Reports reports = validationService.validate(new ByteArrayInputStream(bytes), null);
						if(reports == null || reports.getSimpleReport().getSignatureIdList().isEmpty()) {
							inputStream = pdfService.removeSignField(new ByteArrayInputStream(bytes));
						}
					} else if(contentType != null && contentType.contains("image")){
						bytes = pdfService.jpegToPdf(multipartFile.getInputStream(), multipartFile.getName()).readAllBytes();
						contentType = "application/pdf";
						inputStream = new ByteArrayInputStream(bytes);
					}
					Document document = documentService.createDocument(inputStream, signRequest.getCreateBy(), multipartFile.getOriginalFilename(), contentType);
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
			if (signService.isSigned(signRequest) && !signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().equals(SignType.hiddenVisa)) {
				signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().setSignType(signTypeService.getLessSignType(3));
			}
		}
		updateStatus(signRequest.getId(), SignRequestStatus.pending, "Envoyé pour signature", null, "SUCCESS", null, null, null, null, authUserEppn, authUserEppn);
		customMetricsService.incValue("esup-signature.signrequests", "new");
		for (Target target : signRequest.getParentSignBook().getLiveWorkflow().getTargets().stream().filter(t -> t != null && fsAccessFactoryService.getPathIOType(t.getTargetUri()).equals(DocumentIOType.rest)).collect(Collectors.toList())) {
			targetService.sendRest(target.getTargetUri(), signRequest.getId().toString(), "pending", signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber().toString());
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
		Calendar cal = Calendar.getInstance();
		if(cleanDate != null) {
			cal.setTime(cleanDate);
			cal.add(Calendar.DATE, globalProperties.getDelayBeforeCleaning());
			Date test = cal.getTime();
			Date now = new Date();
			if(signRequest.getExportedDocumentURI() != null
					&& test.getTime()< now.getTime()
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

	public Log updateStatus(Long signRequestId, SignRequestStatus signRequestStatus, String action, String comment, String returnCode, Integer pageNumber, Integer posX, Integer posY, Integer stepNumber, String userEppn, String authUserEppn) {
		SignBook signBook = getById(signRequestId).getParentSignBook();
		return logService.create(signRequestId, signBook.getSubject(), signBook.getWorkflowName(), signRequestStatus, action, comment, returnCode, pageNumber, posX, posY, stepNumber, userEppn, authUserEppn);
	}

	@Transactional
	public void restore(Long signRequestId, String userEppn) {
		SignRequest signRequest = getById(signRequestId);
		if(signRequest.getStatus().equals(SignRequestStatus.deleted)) {
			List<Log> logs = logService.getBySignRequest(signRequestId);
			logs = logs.stream().sorted(Comparator.comparing(Log::getLogDate).reversed()).toList();
			SignRequestStatus restoreStatus = SignRequestStatus.valueOf(logs.get(1).getFinalStatus());
			signRequest.setStatus(restoreStatus);
			signRequest.getParentSignBook().setStatus(restoreStatus);
			logService.create(signRequest.getId(), signRequest.getParentSignBook().getSubject(), signRequest.getParentSignBook().getWorkflowName(), restoreStatus, "Restauration par l'utilisateur", null, "SUCCESS", null, null, null, null, userEppn, userEppn);
		}
	}

	@Transactional
	public Long delete(Long signRequestId, String userEppn) {
		SignRequest signRequest = getById(signRequestId);
		if(signRequest.getStatus().equals(SignRequestStatus.deleted) || signRequest.getStatus().equals(SignRequestStatus.draft) || (signRequest.getParentSignBook().getSignRequests().size() > 1 && signRequest.getParentSignBook().getStatus().equals(SignRequestStatus.pending))) {
			return deleteDefinitive(signRequestId, false, userEppn);
		} else {
			if (signRequest.getStatus().equals(SignRequestStatus.exported) || signRequest.getStatus().equals(SignRequestStatus.archived)) {
				signRequest.getOriginalDocuments().clear();
				signRequest.getSignedDocuments().clear();
			}
			logService.create(signRequest.getId(), signRequest.getParentSignBook().getSubject(), signRequest.getParentSignBook().getWorkflowName(), SignRequestStatus.deleted, "Suppression du document par l'utilisateur", "", "SUCCESS", null, null, null, null, userEppn, userEppn);
			otpService.deleteOtpBySignRequestId(signRequestId);
			nexuService.delete(signRequestId);
			if(signRequest.getParentSignBook().getSignRequests().stream().allMatch(s -> s.getStatus().equals(SignRequestStatus.deleted))) {
				signRequest.getParentSignBook().setStatus(SignRequestStatus.deleted);
				signRequest.getParentSignBook().setUpdateDate(new Date());
				signRequest.getParentSignBook().setUpdateBy(userEppn);
				logService.create(signRequest.getId(), signRequest.getParentSignBook().getSubject(), signRequest.getParentSignBook().getWorkflowName(), SignRequestStatus.deleted, "Suppression de la demande par l'utilisateur", "", "SUCCESS", null, null, null, null, userEppn, userEppn);
			}
			signRequest.setStatus(SignRequestStatus.deleted);
			return signRequest.getParentSignBook().getId();
		}
	}

	@Transactional
	public Long deleteDefinitive(Long signRequestId, boolean force, String userEppn) {
		SignRequest signRequest = getById(signRequestId);
		if(!force && !signRequest.getStatus().equals(SignRequestStatus.deleted) && !signRequest.getRecipientHasSigned().values().stream().allMatch(a -> a.getActionType().equals(ActionType.none))) {
			throw new EsupSignatureRuntimeException("Suppression impossible, la demande est déjà démarrée");
		}
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
		signRequest.getParentSignBook().getSignRequests().remove(signRequest);
		signRequestRepository.delete(signRequest);
		long signBookId = 0;
		if(!signRequest.getParentSignBook().getSignRequests().isEmpty()) {
			signBookId = signRequest.getParentSignBook().getId();
		} else {
			signBookRepository.delete(signRequest.getParentSignBook());
		}
		nexuService.delete(signRequestId);
		if(signRequest.getParentSignBook().getSignRequests().stream().allMatch(s -> s.getStatus().equals(SignRequestStatus.signed) || s.getStatus().equals(SignRequestStatus.completed) || s.getStatus().equals(SignRequestStatus.refused))) {
			signRequest.getParentSignBook().setStatus(SignRequestStatus.completed);
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
				user.getRoles().contains("ROLE_ADMIN");
	}

	@Transactional
	public void deleteSignRequest(Long signRequestId, String userEppn) {
		SignRequest signRequest = signRequestRepository.findById(signRequestId).orElseThrow();
		if(!isDeletetable(signRequest, userEppn)) {
			throw new EsupSignatureRuntimeException("Interdiction de supprimer les demandes de ce circuit");
		}
		if(signRequest.getStatus().equals(SignRequestStatus.deleted)) {
			deleteDefinitive(signRequestId, false, userEppn);
		} else {
			if (signRequest.getStatus().equals(SignRequestStatus.exported) || signRequest.getStatus().equals(SignRequestStatus.archived)) {
				signRequest.getOriginalDocuments().clear();
				signRequest.getSignedDocuments().clear();
			}
			signRequest.setStatus(SignRequestStatus.deleted);
			logService.create(signRequest.getId(), signRequest.getParentSignBook().getSubject(), signRequest.getParentSignBook().getWorkflowName(), SignRequestStatus.deleted, "Suppression par l'utilisateur", "", "SUCCESS", null, null, null, null, userEppn, userEppn);
			otpService.deleteOtpBySignRequestId(signRequestId);
		}
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
						RecipientWsDto recipientWsDto = recipients.stream().filter(recipientWsDto1 -> recipientWsDto1.getEmail().equals(tempUser.getEmail())).findFirst().get();
						tempUser.setFirstname(recipientWsDto.getFirstName());
						tempUser.setName(recipientWsDto.getName());
						if(StringUtils.hasText(recipientWsDto.getPhone())) {
							tempUser.setPhone(PhoneNumberUtil.normalizeDiallableCharsOnly(recipientWsDto.getPhone()));
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

	public void removeLink(Long id, Integer linkId) {
		SignRequest signRequest = getById(id);
		String toRemove = new ArrayList<>(signRequest.getLinks()).get(linkId);
		signRequest.getLinks().remove(toRemove);
	}

	@Transactional
	public Long addComment(Long id, String commentText, Integer commentPageNumber, Integer commentPosX, Integer commentPosY, String postit, Integer spotStepNumber, String authUserEppn, String userEppn) {
		SignRequest signRequest = getById(id);
		if(spotStepNumber == null || userEppn.equals(signRequest.getCreateBy().getEppn())) {
			if (spotStepNumber != null && spotStepNumber > 0) {
				SignRequestParams signRequestParams = signRequestParamsService.createSignRequestParams(commentPageNumber, commentPosX, commentPosY, 150, 75);
				int docNumber = signRequest.getParentSignBook().getSignRequests().indexOf(signRequest);
				signRequestParams.setSignDocumentNumber(docNumber);
				signRequestParams.setComment(commentText);
				signRequest.getSignRequestParams().add(signRequestParams);
				signRequest.getParentSignBook().getLiveWorkflow().getLiveWorkflowSteps().get(spotStepNumber - 1).getSignRequestParams().add(signRequestParams);
			}
			Comment comment = commentService.create(id, commentText, commentPosX, commentPosY, commentPageNumber, spotStepNumber, "on".equals(postit), null, authUserEppn);
			if (!(spotStepNumber != null && spotStepNumber > 0)) {
				updateStatus(signRequest.getId(), null, "Ajout d'un commentaire", commentText, "SUCCESS", commentPageNumber, commentPosX, commentPosY, null, authUserEppn, authUserEppn);
				if (globalProperties.getSendPostitByEmail() && !authUserEppn.equals(signRequest.getCreateBy().getEppn())) {
					try {
						mailService.sendPostit(signRequest.getParentSignBook(), comment);
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
			List<Document> documents = signService.getToSignDocuments(signRequest.getId());
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
	public void getFileResponse(Long documentId, HttpServletResponse httpServletResponse) throws IOException {
		Document document = documentService.getById(documentId);
		webUtilsService.copyFileStreamToHttpResponse(document.getFileName(), document.getContentType(), "attachment", document.getInputStream(), httpServletResponse);
	}

	@Transactional
	public List<Document> getAttachments(Long signRequestId) {
		SignRequest signRequest = getById(signRequestId);
		return new ArrayList<>(signRequest.getAttachments());
	}

	@Transactional
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
		if(signRequest.getParentSignBook().getLastNotifDate() != null) {
			notifTime = Duration.between(signRequest.getParentSignBook().getLastNotifDate().toInstant(), new Date().toInstant()).toHours();
		}
		if(!recipientEmails.isEmpty() && notifTime >= globalProperties.getHoursBeforeRefreshNotif() && signRequest.getStatus().equals(SignRequestStatus.pending)) {
			mailService.sendSignRequestReplayAlert(recipientEmails, signRequest.getParentSignBook());
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
	public String getAllToJSon() throws JsonProcessingException {
		return objectMapper.writeValueAsString(signRequestRepository.findAllByForWs());
	}

	public boolean isAttachmentAlert(SignRequest signRequest) {
		boolean attachmentAlert = false;
		if (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() != null
			&& signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep() != null
			&& signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep().getAttachmentAlert() != null
			&& signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep().getAttachmentAlert()
			&& signRequest.getAttachments().isEmpty()) {
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
			&& signRequest.getAttachments().isEmpty()) {
			attachmentRequire = true;
		}
		return attachmentRequire;
	}

	@Transactional
	public Reports validate(long signRequestId) throws IOException {
		List<Document> documents = signService.getToSignDocuments(signRequestId);
		if(!documents.isEmpty()) {
			byte[] bytes = documents.get(0).getInputStream().readAllBytes();
			return validationService.validate(new ByteArrayInputStream(bytes), null);
		} else {
			return null;
		}
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
		return objectMapper.writeValueAsString(signRequest.getAuditTrail());
	}

	@Transactional
	public void updateComment(Long id, String text) {
		Comment comment = commentService.getById(id);
		comment.setText(text);
	}

	@Transactional
	public void deleteComment(Long id, Long postitId) {
		Comment comment = commentService.getById(postitId);
		SignRequest signRequest = signRequestRepository.findById(id).orElseThrow();
		signRequest.getComments().remove(comment);
		commentService.deleteComment(postitId, signRequest);
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
}
