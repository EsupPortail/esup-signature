package org.esupportail.esupsignature.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.esig.dss.validation.reports.Reports;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dss.service.FOPService;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.*;
import org.esupportail.esupsignature.exception.*;
import org.esupportail.esupsignature.repository.DataRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessFactoryService;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessService;
import org.esupportail.esupsignature.service.interfaces.fs.FsFile;
import org.esupportail.esupsignature.service.interfaces.prefill.PreFillService;
import org.esupportail.esupsignature.service.mail.MailService;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.esupportail.esupsignature.service.security.otp.OtpService;
import org.esupportail.esupsignature.service.utils.WebUtilsService;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.esupportail.esupsignature.service.utils.metric.CustomMetricsService;
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.esupportail.esupsignature.service.utils.sign.SignService;
import org.esupportail.esupsignature.web.ws.json.JsonExternalUserInfo;
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.support.MutableSortDefinition;
import org.springframework.beans.support.PropertyComparator;
import org.springframework.beans.support.SortDefinition;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

	@Resource
	private PdfService pdfService;

	@Resource
	private DocumentService documentService;

	@Resource
	private CustomMetricsService customMetricsService;

	@Resource
	private SignBookService signBookService;

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
	private CertificatService certificatService;

	@Resource
	private ReportService reportService;

	@Resource
	private MessageSource messageSource;

	@Resource
	private ValidationService validationService;

	@Resource
	private FOPService fopService;

	@Resource
	private WebUtilsService webUtilsService;

	@Resource
	private TargetService targetService;

	@Resource
	private LiveWorkflowStepService liveWorkflowStepService;

	@Resource
	private WorkflowService workflowService;

	@Resource
	private UserPropertieService userPropertieService;

	@Resource
	private PreAuthorizeService preAuthorizeService;

	@PostConstruct
	public void initSignrequestMetrics() {
		customMetricsService.registerValue("esup-signature.signrequests", "new");
		customMetricsService.registerValue("esup-signature.signrequests", "signed");
	}

	public SignRequest getById(long id) {
		Optional<SignRequest> signRequest = signRequestRepository.findById(id);
		if(signRequest.isPresent()) {
			Data data = signBookService.getBySignBook(signRequest.get().getParentSignBook());
			if (data != null) {
				signRequest.get().setData(data);
			}
			return signRequest.get();
		}
		return null;
	}

	public List<SignRequest> getSignRequestsByToken(String token) {
		return signRequestRepository.findByToken(token);
	}

	public List<SignRequest> getSignRequestsForCurrentUserByStatus(String userEppn, String authUserEppn, String statusFilter) {
		List<SignRequest> signRequestList = new ArrayList<>();
		List<SignRequest> signRequests = getSignRequestsByStatus(userEppn, statusFilter);
		if(!userEppn.equals(authUserEppn)) {
			for(SignRequest signRequest: signRequests) {
				if(userShareService.checkAllShareTypesForSignRequest(userEppn, authUserEppn, signRequest) || getSharedSignedSignRequests(authUserEppn).contains(signRequest)) {
					signRequestList.add(signRequest);
				}
			}
		} else {
			signRequestList.addAll(signRequests);
		}
		return signRequestList.stream().sorted(Comparator.comparing(SignRequest::getId)).collect(Collectors.toList());
	}

	public List<SignRequest> getSignRequestsByStatus(String userEppn, String statusFilter) {
		Set<SignRequest> signRequests = new HashSet<>();
		if (statusFilter != null && !statusFilter.isEmpty()) {
			switch (statusFilter) {
				case "hided":
					return signRequestRepository.findByHidedByEppn(userEppn);
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
					signRequests.addAll(signBookService.getSignRequestByViewer(userEppn));
					break;
				case "sharedSign":
					signRequests.addAll(getSharedSignedSignRequests(userEppn));
					break;
				case "completed":
					signRequests.addAll(signRequestRepository.findByCreateByEppnAndStatus(userEppn, SignRequestStatus.completed));
					signRequests.addAll(signRequestRepository.findByCreateByEppnAndStatus(userEppn, SignRequestStatus.exported));
					signRequests.addAll(signRequestRepository.findByCreateByEppnAndStatus(userEppn, SignRequestStatus.archived));
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
			signRequests.addAll(signBookService.getSignRequestByViewer(userEppn));
			signRequests.addAll(getSharedSignedSignRequests(userEppn));
			signRequestRepository.findByCreateByEppnAndStatus(userEppn, SignRequestStatus.deleted).forEach(signRequests::remove);
		}
		signRequestRepository.findByHidedByEppn(userEppn).forEach(signRequests::remove);
		return new ArrayList<>(signRequests);
	}

	public Page<SignRequest> getSignRequestsByForm(Form form, Pageable pageable) {
		List<SignRequest> signRequests = new ArrayList<>();
		List<Data> datas = dataRepository.findByFormId(form.getId());
		for(Data data : datas) {
			if(data.getSignBook() != null && data.getSignBook().getSignRequests().size() > 0) {
				signRequests.add(data.getSignBook().getSignRequests().get(0));
			}
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
		Long nbTosign = signRequestRepository.countByRecipientUserToSign(userEppn);
		return nbTosign;
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
		List<SignBook> sharedSignBooks = getSharedSignBooks(userEppn);
		for(SignBook signBook: sharedSignBooks) {
			sharedSignRequests.addAll(signBook.getSignRequests());
		}
		return sharedSignRequests;
	}

	public List<SignBook> getSharedSignBooks(String userEppn) {
		List<SignBook> sharedSignBook = new ArrayList<>();
		for(UserShare userShare : userShareService.getByToUsersInAndShareTypesContains(Collections.singletonList(userEppn), ShareType.sign)) {
			if(userShare.getWorkflow() != null) {
				sharedSignBook.addAll(signBookService.getByWorkflowId(userShare.getWorkflow().getId()));
			} else if(userShare.getForm() != null) {
				List<SignRequest> signRequests = getToSignRequests(userShare.getUser().getEppn());
				for (SignRequest signRequest : signRequests) {
					Data data = signBookService.getBySignBook(signRequest.getParentSignBook());
					if(data.getForm().equals(userShare.getForm())) {
						sharedSignBook.add(signRequest.getParentSignBook());
						break;
					}
				}
			}
		}
		return sharedSignBook;
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

	public SignRequest createSignRequest(String name, Long signBookId, String userEppn, String authUserEppn) {
		User user = userService.getUserByEppn(userEppn);
		SignBook signBook = signBookService.getById(signBookId);
		SignRequest signRequest = new SignRequest();
		if(name == null || name.isEmpty()) {
			if (signBook.getSignRequests().size() == 0) {
				signRequest.setTitle(signBook.getName());
			} else {
				signRequest.setTitle(signBook.getName() + "_" + signBook.getSignRequests().size());
			}
		} else {
			signRequest.setTitle(name);
		}
		signRequest.setToken(String.valueOf(generateUniqueId()));
		signRequest.setCreateBy(user);
		signRequest.setCreateDate(new Date());
		signRequest.setParentSignBook(signBook);
		signRequest.setStatus(SignRequestStatus.draft);
		signRequestRepository.save(signRequest);
		signBook.getSignRequests().add(signRequest);
		updateStatus(signRequest.getId(), SignRequestStatus.draft, "Création de la demande " + signBook.getTitle(), "SUCCESS", userEppn, authUserEppn);
		return signRequest;
	}

	public void addDocsToSignRequest(SignRequest signRequest, boolean scanSignatureFields, int docNumber, List<SignRequestParams> signRequestParamses, MultipartFile... multipartFiles) throws EsupSignatureIOException {
		for(MultipartFile multipartFile : multipartFiles) {
			try {
				byte[] bytes = multipartFile.getInputStream().readAllBytes();
				String contentType = multipartFile.getContentType();
				InputStream inputStream = new ByteArrayInputStream(bytes);
				if (multipartFiles.length == 1) {
					if("application/pdf".equals(multipartFiles[0].getContentType()) && scanSignatureFields) {
						bytes = pdfService.normalizeGS(new ByteArrayInputStream(bytes)).readAllBytes();
						List<SignRequestParams> toAddSignRequestParams = new ArrayList<>();
						if(signRequestParamses.size() == 0) {
							toAddSignRequestParams = signRequestParamsService.scanSignatureFields(new ByteArrayInputStream(bytes), docNumber);

						} else {
							for (SignRequestParams signRequestParams : signRequestParamses) {
								toAddSignRequestParams.add(signRequestParamsService.createSignRequestParams(signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos()));
							}
						}
						signRequest.getSignRequestParams().addAll(toAddSignRequestParams);
						if(validationService.validate(new ByteArrayInputStream(bytes), null).getSimpleReport().getSignatureIdList().size() == 0) {
							inputStream = pdfService.removeSignField(new ByteArrayInputStream(bytes));
						}
					} else if(multipartFiles[0].getContentType() != null && multipartFiles[0].getContentType().contains("image")){
						bytes = pdfService.jpegToPdf(multipartFile.getInputStream(), multipartFile.getName()).readAllBytes();
						contentType = "application/pdf";
						inputStream = new ByteArrayInputStream(bytes);
					}
				}
				Document document = documentService.createDocument(inputStream, multipartFile.getOriginalFilename(), contentType);
				signRequest.getOriginalDocuments().add(document);
				document.setParentId(signRequest.getId());
			} catch (IOException e) {
				logger.error("error on adding files");
				throw new EsupSignatureIOException("Erreur lors de l'ajout des fichiers", e);
			} catch (EsupSignatureException e) {
				logger.error("error on converting files");
				throw new EsupSignatureIOException("Erreur lors de la conversion du document", e);
			}
		}
	}

	public void addAttachmentToSignRequest(SignRequest signRequest, MultipartFile... multipartFiles) throws EsupSignatureIOException {
		for(MultipartFile multipartFile : multipartFiles) {
			try {
				File file = fileService.inputStreamToTempFile(multipartFile.getInputStream(), multipartFile.getName());
				Document document = documentService.createDocument(new FileInputStream(file), "attachement_" + signRequest.getAttachments().size() + "_" + multipartFile.getOriginalFilename(), multipartFile.getContentType());
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
		updateStatus(signRequest.getId(), SignRequestStatus.pending, "Envoyé pour signature", "SUCCESS", null, null, null, authUserEppn, authUserEppn);
		customMetricsService.incValue("esup-signature.signrequests", "new");
	}

	@Transactional
	public void pendingSignBook(Long signBookId, Data data, String userEppn, String authUserEppn, boolean forceSendEmail) throws EsupSignatureException {
		SignBook signBook = signBookService.getById(signBookId);
		LiveWorkflowStep liveWorkflowStep = signBook.getLiveWorkflow().getCurrentStep();
		signBookService.updateStatus(signBook, SignRequestStatus.pending, "Circuit envoyé pour signature de l'étape " + signBook.getLiveWorkflow().getCurrentStepNumber(), "SUCCESS", signBook.getComment(), userEppn, authUserEppn);
		boolean emailSended = false;
		for(SignRequest signRequest : signBook.getSignRequests()) {
			if(signBook.getLiveWorkflow() != null && signBook.getLiveWorkflow().getCurrentStep() != null && signBook.getLiveWorkflow().getCurrentStep().getAutoSign()) {
				signBook.getLiveWorkflow().getCurrentStep().setSignType(SignType.certSign);
				signBook.getLiveWorkflow().getCurrentStep().getRecipients().add(recipientService.createRecipient(userService.getSystemUser()));
			}
			if(!signRequest.getStatus().equals(SignRequestStatus.refused)) {
				if (liveWorkflowStep != null) {
					pendingSignRequest(signRequest, userEppn);
					if (!emailSended) {
						try {
							sendEmailAlerts(signRequest, userEppn, data, forceSendEmail);
							emailSended = true;
						} catch (EsupSignatureMailException e) {
							throw new EsupSignatureException(e.getMessage());
						}
					}
					for (Recipient recipient : signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients()) {
						if (recipient.getUser().getUserType().equals(UserType.external)) {
							try {
								otpService.generateOtpForSignRequest(signRequest.getId(), recipient.getUser());
							} catch (EsupSignatureMailException e) {
								throw new EsupSignatureException(e.getMessage());
							}
						}
					}
					logger.info("Circuit " + signBook.getId() + " envoyé pour signature de l'étape " + signBook.getLiveWorkflow().getCurrentStepNumber());
					if(signBook.getLiveWorkflow().getCurrentStep().getAutoSign()) {
						for(SignRequest signRequest1 : signBook.getSignRequests()) {
							List<SignRequestParams> signRequestParamses = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams();
							signRequestParamses.get(0).setExtraDate(true);
							signRequestParamses.get(0).setAddExtra(true);
							signRequestParamses.get(0).setExtraOnTop(true);
							signRequestParamses.get(0).setAddWatermark(true);
							signRequestParamses.get(0).setSignWidth(200);
							signRequestParamses.get(0).setSignHeight(100);
							signRequestParamses.get(0).setExtraText(signBook.getLiveWorkflow().getCurrentStep().getWorkflowStep().getCertificat().getKeystore().getFileName().replace(",", "\n"));
							if(signRequestParamses.size() > 0) {
								signRequest1.setSignable(true);
								try {
									sign(signRequest1, "", "auto", signRequestParamses, null, userService.getSystemUser(), userService.getSystemUser(), null, "");
								} catch (IOException | InterruptedException | EsupSignatureMailException e) {
									logger.error("auto sign fail", e);
								}
							} else {
								try {
									refuse(signRequest1.getId(), "Signature refusée par le système automatique",  "system", "system");
								} catch (EsupSignatureMailException e) {
									logger.error("auto refuse fail", e);
								}
							}
						}
					}
				} else {
					completeSignBook(signBook.getId(), userEppn);
					logger.info("Circuit " + signBook.getId() + " terminé car ne contient pas d'étape");
					break;
				}
			}
		}
	}

	@Transactional
	public boolean initSign(Long signRequestId, String signRequestParamsJsonString, String comment, String formData, String password, String certType, Long userShareId, String userEppn, String authUserEppn) throws EsupSignatureMailException, IOException, InterruptedException, EsupSignatureException {
		SignRequest signRequest = getSignRequestsFullById(signRequestId, userEppn, authUserEppn);
		Map<String, String> formDataMap = null;
		List<String> toRemoveKeys = new ArrayList<>();
		if(formData != null) {
			try {
				TypeReference<Map<String, String>> type = new TypeReference<>(){};
				formDataMap = objectMapper.readValue(formData, type);
				formDataMap.remove("_csrf");
				Data data = signBookService.getBySignBook(signRequest.getParentSignBook());
				if(data != null && data.getForm() != null) {
					List<Field> fields = preFillService.getPreFilledFieldsByServiceName(data.getForm().getPreFillType(), data.getForm().getFields(), userService.getUserByEppn(userEppn), signRequest);
					for(Map.Entry<String, String> entry : formDataMap.entrySet()) {
						Optional<Field> formfield = fields.stream().filter(f -> f.getName().equals(entry.getKey())).findFirst();
						if(formfield.isPresent()) {
							if(formfield.get().getWorkflowSteps().contains(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep())) {
								if(formfield.get().getExtValueType() == null || !formfield.get().getExtValueType().equals("system")) {
									data.getDatas().put(entry.getKey(), entry.getValue());
								} else {
									if(!formfield.get().getDefaultValue().isEmpty()) {
										data.getDatas().put(entry.getKey(), formfield.get().getDefaultValue());
									}
								}
							}
						} else {
							toRemoveKeys.add(entry.getKey());
						}
					}
					for (String toRemoveKey : toRemoveKeys) {
						formDataMap.remove(toRemoveKey);
					}
				}
			} catch (IOException e) {
				logger.error("form datas error", e);
			}
		}
		List<SignRequestParams> signRequestParamses;
		if (signRequestParamsJsonString == null) {
			signRequestParamses = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams();
		} else {
			signRequestParamses = signRequestParamsService.getSignRequestParamsFromJson(signRequestParamsJsonString);
		}
		if (signRequest.getCurrentSignType().equals(SignType.nexuSign)) {
			signRequestParamsService.copySignRequestParams(signRequest, signRequestParamses);
			return false;
		} else {
			User user = userService.getByEppn(userEppn);
			User authUser = userService.getByEppn(authUserEppn);
			sign(signRequest, password, certType, signRequestParamses, formDataMap, user, authUser, userShareId, comment);
			return true;
		}
	}

	@Transactional
	public String initMassSign(String userEppn, String authUserEppn, String ids, HttpSession httpSession, String password, String certType) throws IOException, InterruptedException, EsupSignatureMailException, EsupSignatureException {
		String error = null;
		TypeReference<List<String>> type = new TypeReference<>(){};
		List<String> idsString = objectMapper.readValue(ids, type);
		List<Long> idsLong = new ArrayList<>();
		idsString.forEach(s -> idsLong.add(Long.parseLong(s)));
		Object userShareString = httpSession.getAttribute("userShareId");
		Report report = reportService.createReport(authUserEppn);
		Long userShareId = null;
		if(userShareString != null) {
			userShareId = Long.valueOf(userShareString.toString());
		}
		for (Long id : idsLong) {
			SignRequest signRequest = getById(id);
			if (!signRequest.getStatus().equals(SignRequestStatus.pending)) {
				reportService.addSignRequestToReport(report.getId(), signRequest, ReportStatus.badStatus);
			} else if (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().equals(SignType.nexuSign)) {
				reportService.addSignRequestToReport(report.getId(), signRequest, ReportStatus.signTypeNotCompliant);
			} else if (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients().stream().noneMatch(r -> r.getUser().getEppn().equals(authUserEppn))) {
				reportService.addSignRequestToReport(report.getId(), signRequest, ReportStatus.userNotInCurrentStep);
				error = messageSource.getMessage("report.reportstatus." + ReportStatus.userNotInCurrentStep, null, Locale.FRENCH);
			} else if (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams().isEmpty()) {
				reportService.addSignRequestToReport(report.getId(), signRequest, ReportStatus.noSignField);
				error = messageSource.getMessage("report.reportstatus." + ReportStatus.noSignField, null, Locale.FRENCH);
			}
			else if (signRequest.getStatus().equals(SignRequestStatus.pending) && initSign(id,null, null, null, password, certType, userShareId, userEppn, authUserEppn)) {
				reportService.addSignRequestToReport(report.getId(), signRequest, ReportStatus.signed);
				error = null;
			}
			else {
				reportService.addSignRequestToReport(report.getId(), signRequest, ReportStatus.error);
			}
		}
		return error;
	}

	public void sign(SignRequest signRequest, String password, String certType, List<SignRequestParams> signRequestParamses, Map<String, String> formDataMap, User user, User authUser, Long userShareId, String comment) throws EsupSignatureException, IOException, InterruptedException, EsupSignatureMailException {
		User signerUser = user;
		if(userShareId != null) {
			UserShare userShare = userShareService.getById(userShareId);
			if (userShare.getUser().getEppn().equals(user.getEppn()) && userShare.getSignWithOwnSign() != null && userShare.getSignWithOwnSign()) {
				signerUser = userService.getByEppn(authUser.getEppn());
			}
		}
		List<Document> toSignDocuments = signService.getToSignDocuments(signRequest.getId());
		SignType signType = signRequest.getCurrentSignType();
		InputStream filledInputStream;
		if(!signBookService.isNextWorkFlowStep(signRequest.getParentSignBook())) {
			Data data = signBookService.getBySignRequest(signRequest);
			if(data != null && data.getForm() != null) {
				Form form = data.getForm();
				for (Field field : form.getFields()) {
					if ("default".equals(field.getExtValueServiceName()) && "system".equals(field.getExtValueType())) {
						if (field.getExtValueReturn().equals("id")) {
							data.getDatas().put(field.getName(), "" + signRequest.getToken());
							formDataMap.put(field.getName(), "" + signRequest.getToken());
						}
					}
				}
			}
		}
		byte[] bytes = toSignDocuments.get(0).getInputStream().readAllBytes();
		if(formDataMap != null && formDataMap.size() > 0 && toSignDocuments.get(0).getContentType().equals("application/pdf") && validationService.validate(new ByteArrayInputStream(bytes), null).getSimpleReport().getSignatureIdList().size() == 0) {
			filledInputStream = pdfService.fill(toSignDocuments.get(0).getInputStream(), formDataMap, signBookService.isStepAllSignDone(signRequest.getParentSignBook()));
		} else {
			filledInputStream = toSignDocuments.get(0).getInputStream();
		}
		boolean visual = true;
		if( signType.equals(SignType.visa) || signType.equals(SignType.hiddenVisa)  || signType.equals(SignType.pdfImageStamp)) {
			InputStream signedInputStream = filledInputStream;
			String fileName = toSignDocuments.get(0).getFileName();
			if(signType.equals(SignType.hiddenVisa)) visual = false;
			if(signRequestParamses.size() == 0 && visual) {
				throw new EsupSignatureException("Il manque une signature !");
			}
			List<Log> lastSignLogs = new ArrayList<>();
			if (toSignDocuments.size() == 1 && toSignDocuments.get(0).getContentType().equals("application/pdf") && visual) {
				for(SignRequestParams signRequestParams : signRequestParamses) {
					signedInputStream = pdfService.stampImage(signedInputStream, signRequest, signRequestParams, 1, signerUser);
					lastSignLogs.add(updateStatus(signRequest.getId(), signRequest.getStatus(), "Apposition de la signature",  "SUCCESS", signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos(), signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber(), user.getEppn(), authUser.getEppn()));
				}
			}
			if ((signBookService.isStepAllSignDone(signRequest.getParentSignBook()))) {
				signedInputStream = pdfService.convertGS(pdfService.writeMetadatas(signedInputStream, fileName, signRequest, lastSignLogs), signRequest.getToken());
			}
			applyEndOfSignRules(signRequest.getId(), user.getEppn(), authUser.getEppn(), signType, comment);
			documentService.addSignedFile(signRequest, signedInputStream, signRequest.getTitle() + "." + fileService.getExtension(toSignDocuments.get(0).getFileName()), toSignDocuments.get(0).getContentType());
		} else {
			if (toSignDocuments.size() == 1 && toSignDocuments.get(0).getContentType().equals("application/pdf")) {
				signRequestParamsService.copySignRequestParams(signRequest, signRequestParamses);
				toSignDocuments.get(0).setTransientInputStream(pdfService.addOutLine(signRequest, filledInputStream, user, new Date(), new SimpleDateFormat()));
			} else {
				visual = false;
			}
			if(signRequestParamses.size() == 0 && visual) {
				throw new EsupSignatureException("Il manque une signature !");
			}
			signService.certSign(signRequest, signerUser, password, certType, visual);
			applyEndOfSignRules(signRequest.getId(), user.getEppn(), authUser.getEppn(), SignType.certSign, comment);
		}
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

	public void completeSignBook(Long signBookId, String userEppn) throws EsupSignatureException {
		SignBook signBook = signBookService.getById(signBookId);
		if (!signBook.getCreateBy().equals(userService.getSchedulerUser())) {
			try {
				mailService.sendCompletedMail(signBook, userEppn);
				mailService.sendCompletedCCMail(signBook);
			} catch (EsupSignatureMailException e) {
				throw new EsupSignatureException(e.getMessage());
			}
		}
		signBookService.updateStatus(signBook, SignRequestStatus.completed, "Tous les documents sont signés", "SUCCESS", "", userEppn, userEppn);
		completeSignRequests(signBook.getSignRequests(), userEppn);
	}

	@Transactional
	public void applyEndOfSignRules(Long signRequestId, String userEppn, String authUserEppn, SignType signType, String comment) throws EsupSignatureException {
		SignRequest signRequest = getById(signRequestId);
		if ( signType.equals(SignType.visa) || signType.equals(SignType.hiddenVisa) ) {
			if(comment != null && !comment.isEmpty()) {
				commentService.create(signRequest.getId(), comment, 0, 0, 0, null, true, null, userEppn);
				updateStatus(signRequest.getId(), SignRequestStatus.checked, "Visa",  "SUCCESS", null, null, null, signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber(), userEppn, authUserEppn);
			} else {
				updateStatus(signRequest.getId(), SignRequestStatus.checked, "Visa", "SUCCESS", userEppn, authUserEppn);
			}
		} else {
			if(comment != null && !comment.isEmpty()) {
				commentService.create(signRequest.getId(), comment, 0, 0, 0, null,true, null, userEppn);
				updateStatus(signRequest.getId(), SignRequestStatus.signed, "Signature", "SUCCESS", null, null, null, signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber(), userEppn, authUserEppn);
			} else {
				updateStatus(signRequest.getId(), SignRequestStatus.signed, "Signature", "SUCCESS", userEppn, authUserEppn);
			}
		}
		recipientService.validateRecipient(signRequest, userEppn);
		if (isSignRequestCompleted(signRequest)) {
			completeSignRequests(Collections.singletonList(signRequest), authUserEppn);
			if (isCurrentStepCompleted(signRequest)) {
				for (Recipient recipient : signRequest.getRecipientHasSigned().keySet()) {
					recipient.setSigned(!signRequest.getRecipientHasSigned().get(recipient).getActionType().equals(ActionType.none));
				}
				if (signBookService.nextWorkFlowStep(signRequest.getParentSignBook())) {
					pendingSignBook(signRequest.getParentSignBook().getId(), null, userEppn, authUserEppn, false);
				} else {
					completeSignBook(signRequest.getParentSignBook().getId(), authUserEppn);
				}
			}
		} else {
			updateStatus(signRequest.getId(), SignRequestStatus.pending, "Demande incomplète", "SUCCESS", userEppn, authUserEppn);
		}
	}

	public void archivesFiles(SignBook signBook, String authUserEppn) throws EsupSignatureFsException, EsupSignatureException {
		if(!signBook.getStatus().equals(SignRequestStatus.archived)) {
			archiveSignRequests(signBook.getSignRequests(), authUserEppn);
			signBook.setStatus(SignRequestStatus.archived);
		}
	}

	public void exportFilesToTarget(SignBook signBook, String authUserEppn) throws EsupSignatureException, EsupSignatureFsException {
		if(signBook.getLiveWorkflow() != null && signBook.getLiveWorkflow().getTargets().size() > 0) {
			sendSignRequestsToTarget(signBook.getSignRequests(), signBook.getName(), signBook.getLiveWorkflow().getTargets(), authUserEppn);
			signBook.setStatus(SignRequestStatus.exported);
		}
	}

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

	public void sendEmailAlerts(SignRequest signRequest, String userEppn, Data data, boolean forceSend) throws EsupSignatureMailException {
		for (Recipient recipient : signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients()) {
			User recipientUser = recipient.getUser();
			if (!UserType.external.equals(recipientUser.getUserType())
				&& (!recipientUser.getEppn().equals(userEppn) || forceSend)
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
			if(!signRequest.getStatus().equals(SignRequestStatus.refused)) {
				updateStatus(signRequest.getId(), SignRequestStatus.completed, "Terminé", "SUCCESS", authUserEppn, authUserEppn);
			}
		}
	}

	public void sendSignRequestsToTarget(List<SignRequest> signRequests, String title, List<Target> targets, String authUserEppn) throws EsupSignatureException, EsupSignatureFsException {
		boolean allTargetsDone = true;
		for(Target target : targets) {
			if(!target.getTargetOk()) {
				DocumentIOType documentIOType = fsAccessFactoryService.getPathIOType(target.getTargetUri());
				String targetUrl = target.getTargetUri();
				if (documentIOType != null && !documentIOType.equals(DocumentIOType.none)) {
					if (documentIOType.equals(DocumentIOType.mail)) {
						logger.info("send by email to " + targetUrl);
						try {
							for (SignRequest signRequest : signRequests) {
								for (String email : targetUrl.split(";")) {
									User user = userService.getUserByEmail(email);
									if(!signRequest.getParentSignBook().getViewers().contains(user)) {
										signRequest.getParentSignBook().getViewers().add(user);
									}
								}
							}
							mailService.sendFile(title, signRequests, targetUrl);
							target.setTargetOk(true);
						} catch (MessagingException | IOException e) {
							logger.error("unable to send mail to : " + target.getTargetUri(), e);
							allTargetsDone = false;
						}
					} else {
						for (SignRequest signRequest : signRequests) {
							if (fsAccessFactoryService.getPathIOType(target.getTargetUri()).equals(DocumentIOType.rest)) {
								RestTemplate restTemplate = new RestTemplate();
								SignRequestStatus status = SignRequestStatus.completed;
								if (signRequest.getRecipientHasSigned().values().stream().anyMatch(action -> action.getActionType().equals(ActionType.refused))) {
									status = SignRequestStatus.refused;
								}
								try {
									ResponseEntity<String> response = restTemplate.getForEntity(target.getTargetUri() + "?signRequestId=" + signRequest.getId() + "&status=" + status.name(), String.class);
									if (response.getStatusCode().equals(HttpStatus.OK)) {
										target.setTargetOk(true);
										updateStatus(signRequest.getId(), signRequest.getStatus(), "Exporté vers " + targetUrl, "SUCCESS", authUserEppn, authUserEppn);
									} else {
										logger.error("rest export fail : " + target.getTargetUri() + " return is : " + response.getStatusCode());
										allTargetsDone = false;
									}
								} catch (Exception e) {
									logger.error("rest export fail : " + target.getTargetUri(), e);
									allTargetsDone = false;
								}
							} else {
								try {
									Document signedFile = signRequest.getLastSignedDocument();
									if (signRequest.getAttachments().size() > 0) {
										targetUrl += "/" + signRequest.getTitle();
										for (Document attachment : signRequest.getAttachments()) {
											documentService.exportDocument(documentIOType, targetUrl, attachment, null);
										}
									}
									String name = signRequest.getTitle();
									if(signRequest.getParentSignBook().getLiveWorkflow().getWorkflow() != null && signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getTargetNamingTemplate() != null) {
										name = signBookService.generateName2(signRequest.getParentSignBook(), title, signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getName(), 0, userService.getSystemUser(), signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getTargetNamingTemplate());
									}
									documentService.exportDocument(documentIOType, targetUrl, signedFile, name);
									target.setTargetOk(true);
									updateStatus(signRequest.getId(), signRequest.getStatus(), "Exporté vers " + targetUrl, "SUCCESS", authUserEppn, authUserEppn);
								} catch (EsupSignatureFsException e) {
									logger.error("fs export fail : " + target.getProtectedTargetUri(), e);
									allTargetsDone = false;
								}
							}
						}
					}
				}
			}
		}
		if(allTargetsDone) {
			for (SignRequest signRequest : signRequests) {
				updateStatus(signRequest.getId(), SignRequestStatus.exported, "Exporté vers toutes les destinations", "SUCCESS", authUserEppn, authUserEppn);
			}
		} else {
			throw new EsupSignatureException("unable to send to all targets");
		}
	}

	public void addPostit(Long signRequestId, String comment, String userEppn, String authUserEppn) {
		SignRequest signRequest = getById(signRequestId);
				if(comment != null && !comment.isEmpty()) {
			updateStatus(signRequest.getId(), signRequest.getStatus(), "comment", comment, "SUCCES", null, null, null, 0, userEppn, authUserEppn);
		}
	}

	public void archiveSignRequests(List<SignRequest> signRequests, String authUserEppn) throws EsupSignatureFsException, EsupSignatureException {
		if(globalProperties.getArchiveUri() != null) {
			for(SignRequest signRequest : signRequests) {
				Document signedFile = signRequest.getLastSignedDocument();
				String subPath = "/" + signRequest.getParentSignBook().getName().split("_")[0].replace(" ", "-") + "/";
				if(signRequest.getExportedDocumentURI() == null) {
					String name = signRequest.getTitle();
					if(signRequest.getParentSignBook().getLiveWorkflow().getWorkflow() != null && signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getTargetNamingTemplate() != null) {
						name = signBookService.generateName2(signRequest.getParentSignBook(), signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getTitle(), signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getName(), 0, userService.getSystemUser(), signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getTargetNamingTemplate());
					}
					String documentUri = documentService.archiveDocument(signedFile, globalProperties.getArchiveUri(), subPath, name);
					signRequest.setExportedDocumentURI(documentUri);
					updateStatus(signRequest.getId(), SignRequestStatus.archived, "Exporté vers l'archivage", "SUCCESS", authUserEppn, authUserEppn);

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
			updateStatus(signRequest.getId(), SignRequestStatus.exported, "Fichiers nettoyés", "SUCCESS", authUserEppn, authUserEppn);
		} else {
			logger.debug("cleanning documents was skipped because date");
		}
	}

	public void cleanFiles(SignBook signBook, String authUserEppn) {
		int nbDocOnDataBase = 0;
		for(SignRequest signRequest : signBook.getSignRequests()) {
			cleanDocuments(signRequest, authUserEppn);
			nbDocOnDataBase += signRequest.getSignedDocuments().size();
		}
		if(nbDocOnDataBase == 0) {
			signBook.setStatus(SignRequestStatus.cleaned);

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
	public void refuse(Long signRequestId, String comment, String userEppn, String authUserEppn) throws EsupSignatureMailException {
		SignRequest signRequest = getById(signRequestId);
		SignBook signBook = signRequest.getParentSignBook();
		if(signBook.getSignRequests().size() > 1 && (signBook.getForceAllDocsSign() == null || !signBook.getForceAllDocsSign())) {
			commentService.create(signRequest.getId(), comment, 0, 0, 0, null, true, "#FF7EB9", userEppn);
			updateStatus(signRequest.getId(), SignRequestStatus.refused, "Refusé", "SUCCESS", null, null, null, signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber(), userEppn, authUserEppn);
			for (Recipient recipient : signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients()) {
				if (recipient.getUser().getEppn().equals(userEppn)) {
					Action action = signRequest.getRecipientHasSigned().get(recipient);
					action.setActionType(ActionType.refused);
					action.setUserIp(webUtilsService.getClientIp());
					action.setDate(new Date());
					recipient.setSigned(true);
				}
			}
		} else {
			refuseSignBook(signRequest.getParentSignBook(), comment, userEppn, authUserEppn);
		}
	}

	public void refuseSignBook(SignBook signBook, String comment, String userEppn, String authUserEppn) throws EsupSignatureMailException {
		mailService.sendRefusedMail(signBook, comment, userEppn);
		for(SignRequest signRequest : signBook.getSignRequests()) {
			commentService.create(signRequest.getId(), comment, 0, 0, 0, null, true, "#FF7EB9", userEppn);
		}
		signBookService.updateStatus(signBook, SignRequestStatus.refused, "Cette demande a été refusée, ceci annule toute la procédure", "SUCCESS", comment, userEppn, authUserEppn);
		for(SignRequest signRequest : signBook.getSignRequests()) {
			updateStatus(signRequest.getId(), SignRequestStatus.refused, "Refusé", "SUCCESS", null, null, null, signBook.getLiveWorkflow().getCurrentStepNumber(), userEppn, authUserEppn);
			for(Recipient recipient : signBook.getLiveWorkflow().getCurrentStep().getRecipients()) {
				if(recipient.getUser().getEppn().equals(userEppn)) {
					Action action = signRequest.getRecipientHasSigned().get(recipient);
					action.setActionType(ActionType.refused);
					action.setUserIp(webUtilsService.getClientIp());
					action.setDate(new Date());
					recipient.setSigned(true);
				}
			}
		}
	}

	public boolean needToSign(SignRequest signRequest, String userEppn) {
		boolean needSignInWorkflow = recipientService.needSign(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients(), userEppn);
		Recipient recipient = signRequest.getRecipientHasSigned().keySet().stream().filter(recipient1 -> recipient1.getUser().getEppn().equals(userEppn)).max(Comparator.comparing(Recipient::getId)).get();
		boolean needSign = signRequest.getRecipientHasSigned().get(recipient).getActionType().equals(ActionType.none);
		return needSign || needSignInWorkflow;
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
	public void delete(Long signRequestId, String userEppn) {
		//TODO critères de suppression ou en conf (if deleteDefinitive)
		SignRequest signRequest = getById(signRequestId);
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
	public void deleteDefinitive(Long signRequestId) {
		SignRequest signRequest = getById(signRequestId);
		signRequest.getRecipientHasSigned().clear();
		signRequestRepository.save(signRequest);
		if (signRequest.getData() != null) {
			Long dataId = signRequest.getData().getId();
			signRequest.setData(null);
			signBookService.deleteOnlyData(dataId);
		}
		List<Long> commentsIds = signRequest.getComments().stream().map(Comment::getId).collect(Collectors.toList());
		for (Long commentId : commentsIds) {
			commentService.deleteComment(commentId);
		}
		signRequest.getParentSignBook().getSignRequests().remove(signRequest);
		signRequestRepository.delete(signRequest);
	}

	@Transactional
	public List<SignRequest> getSignRequests(String userEppn, String authUserEppn, String statusFilter, String recipientsFilter, String workflowFilter, String docTitleFilter, Pageable pageable) {
		List<SignRequest> signRequests = getSignRequestsForCurrentUserByStatus(userEppn, authUserEppn, statusFilter);
		if (recipientsFilter != null && !recipientsFilter.equals("") && !recipientsFilter.equals("all")) {
			List<SignRequest> signRequestByRecipients = signRequestRepository.findByRecipient(recipientsFilter);
			signRequests.retainAll(signRequestByRecipients);
		}
		if (workflowFilter != null && !workflowFilter.equals("") && !workflowFilter.equals("all")) {
			Set<SignRequest> signRequestByWorkflow = new HashSet<>();
			if(workflowFilter.equals("Hors circuit")) {
				signRequestByWorkflow.addAll(signRequestRepository.findByByParentSignBookTitleEmptyAndWorflowIsNull());
			} else {
				signRequestByWorkflow.addAll(signRequestRepository.findByWorkflowDescription(workflowFilter));
				signRequestByWorkflow.addAll(signRequestRepository.findByParentSignBookTitle(workflowFilter));
			}
			signRequests.retainAll(signRequestByWorkflow);
		}
		if (docTitleFilter != null && !docTitleFilter.equals("") && !docTitleFilter.equals("all")) {
			List<SignRequest> signRequestByTitle = signRequestRepository.findByTitle(docTitleFilter);
			signRequests.retainAll(signRequestByTitle);
		}
		if(pageable.getSort().iterator().hasNext()) {
			Sort.Order order = pageable.getSort().iterator().next();
			SortDefinition sortDefinition = new MutableSortDefinition(order.getProperty(), true, order.getDirection().isAscending());
			PropertyComparator<SignRequest> propertyComparator = new PropertyComparator<>(sortDefinition);
			signRequests.sort(propertyComparator);
		}
		for(SignRequest signRequest : signRequests) {
			if(signRequest.getEndDate() == null) {
				signRequest.setEndDate(getEndDate(signRequest));
			}
		}
		return signRequests;
	}

	private Date getEndDate(SignRequest signRequest) {
		List<Action> action = signRequest.getRecipientHasSigned().values().stream().filter(action1 -> !action1.getActionType().equals(ActionType.none)).sorted(Comparator.comparing(Action::getDate)).collect(Collectors.toList());
		if(action.size() > 0) {
			return action.get(0).getDate();
		}
		return null;
	}

	public void sendSignRequestEmailAlert(SignRequest signRequest, User recipientUser, Data data) throws EsupSignatureMailException {
		Date date = new Date();
		Set<String> toEmails = new HashSet<>();
		toEmails.add(recipientUser.getEmail());
		SignBook signBook = signRequest.getParentSignBook();
		Workflow workflow = signBook.getLiveWorkflow().getWorkflow();
		recipientUser.setLastSendAlertDate(date);
		if(data != null && data.getForm() != null) {
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


	public void sendEmailAlertSummary(User recipientUser) throws EsupSignatureMailException {
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
				&& preAuthorizeService.checkUserSignRights(signRequest, userEppn, authUserEppn)
				&& signRequest.getOriginalDocuments().size() > 0
				&& needToSign(signRequest, userEppn)) {
			signRequest.setSignable(true);
		}
		User user = userService.getUserByEppn(userEppn);
		if ((signRequest.getStatus().equals(SignRequestStatus.pending)
				&& (preAuthorizeService.isUserInRecipients(signRequest, userEppn) || signRequest.getCreateBy().getEppn().equals(userEppn))) || (signRequest.getStatus().equals(SignRequestStatus.draft) && signRequest.getCreateBy().getEppn().equals(user.getEppn()))
		) {
			signRequest.setEditable(true);
		}
		return signRequest;
	}

	@Transactional
	public boolean isTempUsers(Long signRequestId) {
		SignRequest signRequest = getById(signRequestId);
		boolean isTempUsers = false;
		if(userService.getTempUsers(signRequest).size() > 0) {
			isTempUsers = true;
		}
		return isTempUsers;
	}

	public boolean checkTempUsers(Long id, List<String> recipientEmails, List<JsonExternalUserInfo> externalUsersInfos) throws MessagingException {
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
						tempUser.setEppn(jsonExternalUserInfo.getPhone());
					}
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
		Data data = signBookService.getBySignBook(signRequest.getParentSignBook());
		if(data != null) {
			if(data.getForm() != null) {
				List<Field> fields = data.getForm().getFields();
				if (!"".equals(data.getForm().getPreFillType()) && signRequest.getParentSignBook().getLiveWorkflow() != null && signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() != null && signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getUsers().contains(user)) {
					prefilledFields = preFillService.getPreFilledFieldsByServiceName(data.getForm().getPreFillType(), fields, user, signRequest);
					for (Field field : prefilledFields) {
						if (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() == null
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
			if (data.getDatas().get(field.getName()) != null
					&& !data.getDatas().get(field.getName()).isEmpty()) {
				field.setDefaultValue(data.getDatas().get(field.getName()));
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
	public void addComment(Long id, String commentText, Integer commentPageNumber, Integer commentPosX, Integer commentPosY, String postit, Integer spotStepNumber, String authUserEppn) {
			SignRequest signRequest = getById(id);
		if(spotStepNumber != null && spotStepNumber > 0) {
			SignRequestParams signRequestParams = signRequestParamsService.createSignRequestParams(commentPageNumber, commentPosX, commentPosY);
			int docNumber = signRequest.getParentSignBook().getSignRequests().indexOf(signRequest);
			signRequestParams.setSignDocumentNumber(docNumber);
			signRequest.getSignRequestParams().add(signRequestParams);
			signRequest.getParentSignBook().getLiveWorkflow().getLiveWorkflowSteps().get(spotStepNumber - 1).getSignRequestParams().add(signRequestParams);
		}
		commentService.create(id, commentText, commentPosX, commentPosY, commentPageNumber, spotStepNumber, "on".equals(postit), null, authUserEppn);
		if(!(spotStepNumber != null && spotStepNumber > 0)) {
			updateStatus(signRequest.getId(), null, "Ajout d'un commentaire", commentText, "SUCCESS", commentPageNumber, commentPosX, commentPosY, null, authUserEppn, authUserEppn);
		} else {
			updateStatus(signRequest.getId(), null, "Ajout d'un emplacement de signature", commentText, "SUCCESS", commentPageNumber, commentPosX, commentPosY, null, authUserEppn, authUserEppn);
		}
	}

	public void addStep(Long id, List<String> recipientsEmails, SignType signType, Boolean allSignToComplete, String authUserEppn) throws EsupSignatureException {
		SignRequest signRequest = getById(id);
		signBookService.addLiveStep(signRequest.getParentSignBook().getId(), recipientsEmails, signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber(), allSignToComplete, signType, false, true, false, authUserEppn);
	}

	@Transactional
	public Map<SignBook, String> sendSignRequest(MultipartFile[] multipartFiles, SignType signType, Boolean allSignToComplete, Boolean userSignFirst, Boolean pending, String comment, List<String> recipientsCCEmails, List<String> recipientsEmails, List<JsonExternalUserInfo> externalUsersInfos, User user, User authUser, boolean forceSendEmail, Boolean forceAllSign, String targetUrl) throws EsupSignatureException, EsupSignatureIOException, EsupSignatureFsException {
		if(forceAllSign == null) forceAllSign = false;
		if (!signService.checkSignTypeDocType(signType, multipartFiles[0])) {
			throw new EsupSignatureException("Impossible de demander une signature visuelle sur un document du type " + multipartFiles[0].getContentType());
		}
		SignBook signBook = addDocsInNewSignBookSeparated(fileService.getNameOnly(multipartFiles[0].getOriginalFilename()), "Demande simple", multipartFiles, user);
		signBook.setForceAllDocsSign(forceAllSign);
		try {
			signBookService.sendCCEmail(signBook.getId(), recipientsCCEmails);
		} catch (EsupSignatureMailException e) {
			throw new EsupSignatureException(e.getMessage());
		}
		if(targetUrl != null && !targetUrl.isEmpty()) {
			signBook.getLiveWorkflow().getTargets().add(targetService.createTarget(targetUrl));
		}
		return sendSignBook(signBook, signType, allSignToComplete, userSignFirst, pending, comment, recipientsEmails, externalUsersInfos, user, authUser, forceSendEmail);
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
		signRequestRef.setEditable(signRequest.getEditable());
		LinkedList<String> signImages = new LinkedList<>();
		if (signRequest.getSignedDocuments().size() > 0 || signRequest.getOriginalDocuments().size() > 0) {
			List<Document> toSignDocuments = signService.getToSignDocuments(signRequest.getId());
			if (toSignDocuments.size() == 1 && toSignDocuments.get(0).getContentType().equals("application/pdf")) {
				if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() != null && !signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().equals(SignType.visa) && !signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().equals(SignType.hiddenVisa)) {
					User user = userService.getByEppn(userEppn);
					if(userShareId != null) {
						UserShare userShare = userShareService.getById(userShareId);
						if (userShare.getUser().getEppn().equals(userEppn) && userShare.getSignWithOwnSign() != null && userShare.getSignWithOwnSign()) {
							user = userService.getByEppn(authUserEppn);
						}
					}
					if (user.getSignImages().size() > 0 && user.getSignImages().get(0) != null && user.getSignImages().get(0).getSize() > 0) {
						if (preAuthorizeService.checkUserSignRights(signRequest, userEppn, authUserEppn)
							&& user.getKeystore() == null
							&& certificatService.getCertificatByUser(userEppn).size() == 0
							&& signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().equals(SignType.certSign)) {
							signRequestRef.setSignable(false);
							throw new EsupSignatureUserException("Pour signer ce document merci d’ajouter un certificat à votre profil <a href='user/users' target='_blank'>Mes paramètres</a>");
						}
						for (Document signImage : user.getSignImages()) {
							signImages.add(fileService.getBase64Image(signImage));
						}
					} else {
						if (signRequest.getSignable() && signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType() != null && signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().equals(SignType.pdfImageStamp)) {
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

	public Long getNbPendingSignRequests(String userEppn) {
		return signRequestRepository.countByCreateByEppnAndStatus(userEppn, SignRequestStatus.pending);
	}

	public Long getNbDraftSignRequests(String userEppn) {
		return signRequestRepository.countByCreateByEppnAndStatus(userEppn, SignRequestStatus.draft);
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
	public Map<String, Object> getToSignFileResponse(Long signRequestId) throws SQLException, EsupSignatureFsException, IOException, EsupSignatureException {
		SignRequest signRequest = getById(signRequestId);
		if (!signRequest.getStatus().equals(SignRequestStatus.exported)) {
			List<Document> documents = signService.getToSignDocuments(signRequest.getId());
			if (documents.size() > 1) {
				return null;
			} else {
				Document document;
				if(documents.size() > 0) {
					document = documents.get(0);
				} else {
					document = signRequest.getOriginalDocuments().get(0);
				}
				return fileService.getFileResponse(document.getBigFile().getBinaryFile().getBinaryStream().readAllBytes(), document.getFileName(), document.getContentType());
			}
		} else {
			FsFile fsFile = getLastSignedFsFile(signRequest);
			return fileService.getFileResponse(fsFile.getInputStream().readAllBytes(), fsFile.getName(), fsFile.getContentType());
		}
	}

	@Transactional
	public void getToSignFileReportResponse(Long signRequestId, HttpServletResponse response) throws Exception {
		SignRequest signRequest = getById(signRequestId);
		response.setContentType("application/zip; charset=utf-8");
		response.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode(signRequest.getTitle() + "-avec_rapport", StandardCharsets.UTF_8.toString()) + ".zip");
		response.getOutputStream().write(getZipWithDocAndReport(signRequest));
	}

	private byte[] getZipWithDocAndReport(SignRequest signRequest) throws Exception {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
		String name = "";
		InputStream inputStream = null;
		if (!signRequest.getStatus().equals(SignRequestStatus.exported)) {
			if(signService.getToSignDocuments(signRequest.getId()).size() == 1) {
				List<Document> documents = signService.getToSignDocuments(signRequest.getId());
				name = documents.get(0).getFileName();
				inputStream = documents.get(0).getInputStream();
			}
		} else {
			FsFile fsFile = getLastSignedFsFile(signRequest);
			name = fsFile.getName();
			inputStream = fsFile.getInputStream();
		}

		if(inputStream != null) {
			int i = 0;
			for(Document document : signRequest.getAttachments()) {
				zipOutputStream.putNextEntry(new ZipEntry(i + "_" + document.getFileName()));
				IOUtils.copy(document.getInputStream(), zipOutputStream);
				zipOutputStream.write(document.getInputStream().readAllBytes());
				zipOutputStream.closeEntry();
				i++;
			}

			byte[] fileBytes = inputStream.readAllBytes();

			zipOutputStream.putNextEntry(new ZipEntry(name));
			IOUtils.copy(new ByteArrayInputStream(fileBytes), zipOutputStream);
			zipOutputStream.closeEntry();
			File reportFile = fileService.getTempFile("report.pdf");

			Reports reports = validationService.validate(new ByteArrayInputStream(fileBytes), null);

			fopService.generateSimpleReport(reports.getXmlSimpleReport(), new FileOutputStream(reportFile));
			zipOutputStream.putNextEntry(new ZipEntry("rapport-signature.pdf"));
			IOUtils.copy(new FileInputStream(reportFile), zipOutputStream);
			zipOutputStream.closeEntry();
			reportFile.delete();
		}
		zipOutputStream.close();
		return outputStream.toByteArray();
	}

	@Transactional
	public Map<String, Object> getFileResponse(Long documentId) throws SQLException, EsupSignatureFsException, IOException {
		Document document = documentService.getById(documentId);
		return fileService.getFileResponse(document.getBigFile().getBinaryFile().getBinaryStream().readAllBytes(), document.getFileName(), document.getContentType());
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

	public void replayNotif(Long id) throws EsupSignatureMailException {
		SignRequest signRequest = this.getById(id);
		List<String> recipientEmails = new ArrayList<>();
		signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients().stream().filter(r -> !r.getSigned()).collect(Collectors.toList()).forEach(r -> recipientEmails.add(r.getUser().getEmail()));
		if(recipientEmails.size() > 0) {
			mailService.sendSignRequestAlert(recipientEmails, signRequest);
		}
	}

	public List<Recipient> getRecipientsNameFromSignRequests(List<SignRequest> signRequests) {
		List<Recipient> recipientNames = new ArrayList<>();
		for (SignRequest signRequest : signRequests) {
			recipientNames.addAll(signRequest.getRecipientHasSigned().keySet());
		}
		return recipientNames.stream().filter(distinctByKey(r -> r.getUser().getId())).collect( Collectors.toList() );
	}

	public static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
		Map<Object, Boolean> map = new ConcurrentHashMap<>();
		return t -> map.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
	}

	@Transactional
	public List<SignRequestParams> getToUseSignRequestParams(long id, String userEppn) {
		User user = userService.getUserByEppn(userEppn);
		List<SignRequestParams> toUserSignRequestParams = new ArrayList<>();
		SignRequest signRequest = getById(id);
		int signOrderNumber = signRequest.getParentSignBook().getSignRequests().indexOf(signRequest);
		if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() != null) {
			List<SignRequestParams> signRequestParamsForCurrentStep = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams().stream().filter(signRequestParams -> signRequestParams.getSignDocumentNumber().equals(signOrderNumber)).collect(Collectors.toList());
			for(SignRequestParams signRequestParams : signRequestParamsForCurrentStep) {
				if(signRequest.getSignRequestParams().contains(signRequestParams) && signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients().stream().anyMatch(recipient -> recipient.getUser().equals(user))) {
					toUserSignRequestParams.add(signRequestParams);
				}
			}
		}
		return toUserSignRequestParams;
	}

	@Transactional
	public File getToValidateFile(long id) throws IOException {
		SignRequest signRequest = getById(id);
		Document toValideDocument = signRequest.getLastSignedDocument();
		File file = fileService.getTempFile(toValideDocument.getFileName());
		OutputStream outputStream = new FileOutputStream(file);
		IOUtils.copy(toValideDocument.getInputStream(), outputStream);
		outputStream.close();
		return file;
	}

	public List<SignRequest> getAll() {
		return (List<SignRequest>) signRequestRepository.findAll();
	}

	@Transactional
	public void getMultipleSignedDocuments(List<Long> ids, HttpServletResponse response) throws IOException {
		response.setContentType("application/zip; charset=utf-8");
		response.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode("alldocs", StandardCharsets.UTF_8.toString()) + ".zip");
		List<Document> documents = new ArrayList<>();
		for(Long id : ids) {
			SignBook signBook = signBookService.getById(id);
			for (SignRequest signRequest : signBook.getSignRequests()) {
				if(signRequest.getStatus().equals(SignRequestStatus.completed) || signRequest.getStatus().equals(SignRequestStatus.exported) || signRequest.getStatus().equals(SignRequestStatus.archived))
				documents.add(signRequest.getLastSignedDocument());
			}
		}
		ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());
		int i = 0;
		for(Document document : documents) {
			zipOutputStream.putNextEntry(new ZipEntry(i + "_" + document.getFileName()));
			IOUtils.copy(document.getInputStream(), zipOutputStream);
			zipOutputStream.write(document.getInputStream().readAllBytes());
			zipOutputStream.closeEntry();
			i++;
		}
		zipOutputStream.close();
	}

	@Transactional
	public void getMultipleSignedDocumentsWithReport(List<Long> ids, HttpServletResponse response) throws Exception {
		response.setContentType("application/zip; charset=utf-8");
		response.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode("alldocs", StandardCharsets.UTF_8.toString()) + ".zip");
		Map<byte[], String> documents = new HashMap<>();
		for(Long id : ids) {
			SignBook signBook = signBookService.getById(id);
			for (SignRequest signRequest : signBook.getSignRequests()) {
				if(signRequest.getStatus().equals(SignRequestStatus.completed) || signRequest.getStatus().equals(SignRequestStatus.exported) || signRequest.getStatus().equals(SignRequestStatus.archived))
					documents.put(getZipWithDocAndReport(signRequest), signBook.getName());
			}
		}
		ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());
		int i = 0;
		for(Map.Entry<byte[], String> document : documents.entrySet()) {
			zipOutputStream.putNextEntry(new ZipEntry(i + "_" + document.getValue() + ".zip"));
			IOUtils.copy(new ByteArrayInputStream(document.getKey()), zipOutputStream);
			zipOutputStream.write(document.getKey());
			zipOutputStream.closeEntry();
			i++;
		}
		zipOutputStream.close();
	}

	public int transfer(String authUserEppn) {
		int i = 0;
		User user = userService.getUserByEppn(authUserEppn);
		User replacedByUser = user.getCurrentReplaceUser();
		if(replacedByUser != null) {
			List<SignRequest> signRequests = getToSignRequests(authUserEppn).stream().filter(signRequest -> signRequest.getStatus().equals(SignRequestStatus.pending)).collect(Collectors.toList());
			for(SignRequest signRequest : signRequests) {
				for(LiveWorkflowStep liveWorkflowStep : signRequest.getParentSignBook().getLiveWorkflow().getLiveWorkflowSteps()) {
					for(Recipient recipient : liveWorkflowStep.getRecipients()) {
						if(recipient.getUser().getEppn().equals(authUserEppn)) {
							recipient.setUser(replacedByUser);
						}
					}
					for(Recipient recipient : signRequest.getRecipientHasSigned().keySet()) {
						if(recipient.getUser().getEppn().equals(authUserEppn)) {
							recipient.setUser(replacedByUser);
						}
					}
				}
				i++;
			}
		}
		return i;
	}

	public boolean isAttachmentAlert(SignRequest signRequest) {
		boolean attachmentAlert = false;
		if (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep() != null
				&& signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep().getAttachmentAlert() != null
				&& signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep().getAttachmentAlert()
				&& signRequest.getAttachments().size() == 0) {
			attachmentAlert = true;
		}
		return attachmentAlert;
	}
	public boolean isAttachmentRequire(SignRequest signRequest) {
		boolean attachmentRequire = false;
		if (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep() != null
				&& signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep().getAttachmentRequire() != null
				&& signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep().getAttachmentRequire()
				&& signRequest.getAttachments().size() == 0) {
			attachmentRequire = true;
		}
		return attachmentRequire;
	}

	@Transactional
	public void toggle(Long id, String userEpppn) {
		SignRequest signRequest = getById(id);
		User user = userService.getUserByEppn(userEpppn);
		if(signRequest.getHidedBy().contains(user)) {
			signRequest.getHidedBy().remove(user);
		} else {
			signRequest.getHidedBy().add(user);
		}
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
	public void addDocumentsToSignBook(Long signBookId, MultipartFile[] multipartFiles, String authUserEppn) throws EsupSignatureIOException {
		int i = 0;
		for (MultipartFile multipartFile : multipartFiles) {
			SignRequest signRequest = createSignRequest(fileService.getNameOnly(multipartFile.getOriginalFilename()), signBookId, authUserEppn, authUserEppn);
			try {
				addDocsToSignRequest(signRequest, true, i, new ArrayList<>(), multipartFile);
			} catch (EsupSignatureIOException e) {
				logger.error("revert signbook creation due to error : " + e.getMessage());
				signBookService.deleteDefinitive(signBookId);
				throw new EsupSignatureIOException(e.getMessage(), e);
			}
			i++;
		}
	}

	public SignBook addDocsInNewSignBookSeparated(String name, String prefix, MultipartFile[] multipartFiles, User authUser) throws EsupSignatureIOException {
		SignBook signBook = signBookService.createSignBook(name, null, prefix, "", authUser, true);
		addDocumentsToSignBook(signBook.getId(), multipartFiles, authUser.getEppn());
		return signBook;
	}

	@Transactional
	public SignBook addDocsInNewSignBookGrouped(String name, MultipartFile[] multipartFiles, String authUserEppn) throws EsupSignatureIOException {
		User authUser = userService.getByEppn(authUserEppn);
		SignBook signBook = signBookService.createSignBook(name, null, "","", authUser, false);
		SignRequest signRequest = createSignRequest(null, signBook.getId(), authUserEppn, authUserEppn);
		addDocsToSignRequest(signRequest, true, 0, new ArrayList<>(), multipartFiles);
		logger.info("signRequest : " + signRequest.getId() + " added to signBook" + signBook.getName() + " - " + signBook.getId());
		return signBook;
	}

	@Transactional
	public SignBook addFastSignRequestInNewSignBook(MultipartFile[] multipartFiles, SignType signType, User user, String authUserEppn) throws EsupSignatureException {
		if (signService.checkSignTypeDocType(signType, multipartFiles[0])) {
			try {
				SignBook signBook = addDocsInNewSignBookSeparated(fileService.getNameOnly(multipartFiles[0].getOriginalFilename()), "Auto signature", multipartFiles, user);
				signBook.getLiveWorkflow().getLiveWorkflowSteps().add(liveWorkflowStepService.createLiveWorkflowStep(null,false, true, false, false, signType, Collections.singletonList(user.getEmail()), null));
				signBook.getLiveWorkflow().setCurrentStep(signBook.getLiveWorkflow().getLiveWorkflowSteps().get(0));
				signBookService.dispatchSignRequestParams(signBook);
				pendingSignBook(signBook.getId(), null, user.getEppn(), authUserEppn, false);
				return signBook;
			} catch (EsupSignatureIOException e) {
				throw new EsupSignaturePdfException("Impossible de charger le document suite à une erreur interne", e);
			}
		} else {
			throw new EsupSignatureException("Impossible de demander une signature visuelle sur un document du type " + multipartFiles[0].getContentType());
		}
	}

	@Transactional
	public void initWorkflowAndPendingSignBook(Long signRequestId, List<String> recipientsEmails, List<String> allSignToCompletes, List<JsonExternalUserInfo> externalUsersInfos, List<String> targetEmails, String userEppn, String authUserEppn) throws EsupSignatureFsException, EsupSignatureException {
		SignRequest signRequest = signRequestRepository.findById(signRequestId).get();
		SignBook signBook = signRequest.getParentSignBook();
		if(signBook.getStatus().equals(SignRequestStatus.draft)) {
			if (signBook.getLiveWorkflow().getWorkflow() != null) {
				List<Target> targets = new ArrayList<>(workflowService.getById(signBook.getLiveWorkflow().getWorkflow().getId()).getTargets());
				Workflow workflow = workflowService.computeWorkflow(signBook.getLiveWorkflow().getWorkflow().getId(), recipientsEmails, allSignToCompletes, userEppn, false);
				signBookService.importWorkflow(signBook, workflow, externalUsersInfos);
				signBookService.nextWorkFlowStep(signBook);
				targetService.copyTargets(targets, signBook, targetEmails);
				if(recipientsEmails != null) {
					for (String recipientEmail : recipientsEmails) {
						userPropertieService.createUserPropertieFromMails(userService.getByEppn(authUserEppn), Collections.singletonList(recipientEmail.split("\\*")[1]));
					}
				}
			}
			pendingSignBook(signBook.getId(), null, userEppn, authUserEppn, false);
		}
	}

	@Transactional
	public void nextStepAndPending(Long signBookId, Data data, String userEppn, String authUserEppn) throws EsupSignatureException {
		SignBook signBook = signBookService.getById(signBookId);
		signBookService.nextWorkFlowStep(signBook);
		pendingSignBook(signBook.getId(), data, userEppn, authUserEppn, true);
	}

	@Transactional
	public boolean startLiveWorkflow(SignBook signBook, String userEppn, String authUserEppn, Boolean start) throws EsupSignatureException {
		if(signBook.getLiveWorkflow().getLiveWorkflowSteps().size() >  0) {
			signBook.getLiveWorkflow().setCurrentStep(signBook.getLiveWorkflow().getLiveWorkflowSteps().get(0));
			if(start != null && start) {
				signBookService.dispatchSignRequestParams(signBook);
				pendingSignBook(signBook.getId(), null, userEppn, authUserEppn, false);
			}
			return true;
		}else {
			return false;
		}
	}

	public Map<SignBook, String> sendSignBook(SignBook signBook, SignType signType, Boolean allSignToComplete, Boolean userSignFirst, Boolean pending, String comment, List<String> recipientsEmails, List<JsonExternalUserInfo> externalUsersInfos, User user, User authUser, boolean forceSendEmail) throws EsupSignatureException {
		String message = null;
		if (allSignToComplete == null) {
			allSignToComplete = false;
		}
		if(userSignFirst != null && userSignFirst) {
			signBook.getLiveWorkflow().getLiveWorkflowSteps().add(liveWorkflowStepService.createLiveWorkflowStep(null,false, true, false,false, SignType.pdfImageStamp, Collections.singletonList(user.getEmail()), null));
		}
		signBook.getLiveWorkflow().getLiveWorkflowSteps().add(liveWorkflowStepService.createLiveWorkflowStep(null,false, true, false, allSignToComplete, signType, recipientsEmails, externalUsersInfos));
		signBook.getLiveWorkflow().setCurrentStep(signBook.getLiveWorkflow().getLiveWorkflowSteps().get(0));
		signBookService.dispatchSignRequestParams(signBook);
		if (pending != null && pending) {
			pendingSignBook(signBook.getId(), null, user.getEppn(), authUser.getEppn(), forceSendEmail);
		} else {
			message = "Après vérification/annotation, vous devez cliquer sur 'Démarrer le circuit' pour transmettre la demande aux participants";
		}
		if (comment != null && !comment.isEmpty()) {
			signBook.setDescription(comment);
//            for (SignRequest signRequest : signBook.getSignRequests()) {
//                commentService.create(signRequest.getId(), comment, null, null, null, null, true, null, user.getEppn());
//                signRequestService.updateStatus(signRequest, signRequest.getStatus(), "comment", comment, "SUCCES", null, null, null, 0, user.getEppn(), authUser.getEppn());
//            }
		}
		Map<SignBook, String> signBookStringMap = new HashMap<>();
		signBookStringMap.put(signBook, message);
		if(recipientsEmails != null) {
			userPropertieService.createUserPropertieFromMails(userService.getByEppn(authUser.getEppn()), recipientsEmails);
		}
		return signBookStringMap;
	}

	@Transactional
	public void addWorkflowToSignBook(SignBook signBook, String authUserEppn, Long workflowSignBookId) throws EsupSignatureException {
		Workflow workflow = workflowService.getById(workflowSignBookId);
		signBookService.importWorkflow(signBook, workflow, null);
		signBookService.nextWorkFlowStep(signBook);
		pendingSignBook(signBook.getId(), null, authUserEppn, authUserEppn, false);
	}

	@Transactional
	public int importFilesFromSource(Long workflowId, User user, User authUser) throws EsupSignatureFsException {
		Workflow workflow = workflowService.getById(workflowId);
		int nbImportedFiles = 0;
		if (workflow.getDocumentsSourceUri() != null && !workflow.getDocumentsSourceUri().equals("")) {
			logger.info("retrieve from " + workflow.getProtectedDocumentsSourceUri());
			FsAccessService fsAccessService = fsAccessFactoryService.getFsAccessService(workflow.getDocumentsSourceUri());
			if (fsAccessService != null) {
				try {
					fsAccessService.open();
					fsAccessService.createURITree(workflow.getDocumentsSourceUri());
					List<FsFile> fsFiles = new ArrayList<>(fsAccessService.listFiles(workflow.getDocumentsSourceUri() + "/"));
					if (fsFiles.size() > 0) {
						int j = 0;
						for (FsFile fsFile : fsFiles) {
							logger.info("adding file : " + fsFile.getName());
							ByteArrayOutputStream baos = fileService.copyInputStream(fsFile.getInputStream());
							Map<String, String> metadatas = pdfService.readMetadatas(new ByteArrayInputStream(baos.toByteArray()));
							String documentName = fsFile.getName();
							if (metadatas.get("Title") != null && !metadatas.get("Title").isEmpty()) {
								documentName = metadatas.get("Title");
							}
							SignBook signBook = signBookService.createSignBook(fileService.getNameOnly(documentName), workflow, "",null, user, false);
							signBook.getLiveWorkflow().setWorkflow(workflow);
							SignRequest signRequest = createSignRequest(null, signBook.getId(), user.getEppn(), authUser.getEppn());
							if (fsFile.getCreateBy() != null && userService.getByEppn(fsFile.getCreateBy()) != null) {
								user = userService.getByEppn(fsFile.getCreateBy());
							}
							List<String> workflowRecipientsEmails = new ArrayList<>();
							workflowRecipientsEmails.add(user.getEmail());
							addDocsToSignRequest(signRequest, true, j, new ArrayList<>(), fileService.toMultipartFile(new ByteArrayInputStream(baos.toByteArray()), fsFile.getName(), fsFile.getContentType()));
							j++;
							if (workflow.getScanPdfMetadatas()) {
								String signType = metadatas.get("sign_type_default_val");
								User creator = userService.createUserWithEppn(metadatas.get("Creator"));
								if (creator != null) {
									signRequest.setCreateBy(creator);
									signBook.setCreateBy(creator);
								} else {
									signRequest.setCreateBy(userService.getSystemUser());
									signBook.setCreateBy(userService.getSystemUser());
								}
								int i = 0;
								for (String metadataKey : metadatas.keySet()) {
									String[] keySplit = metadataKey.split("_");
									if (keySplit[0].equals("sign") && keySplit[1].contains("step")) {
										ObjectMapper mapper = new ObjectMapper();
										TypeReference<List<String>> type = new TypeReference<>(){};
										List<String> recipientList = mapper.readValue(metadatas.get(metadataKey), type);
										WorkflowStep workflowStep = null;
										if(workflow.getWorkflowSteps().size() > i) {
											workflowStep = workflow.getWorkflowSteps().get(i);
										}
										LiveWorkflowStep liveWorkflowStep = liveWorkflowStepService.createLiveWorkflowStep(workflowStep, false, true, false, false, SignType.valueOf(signType), recipientList, null);
										signBook.getLiveWorkflow().getLiveWorkflowSteps().add(liveWorkflowStep);
										i++;
									}
									if (keySplit[0].equals("sign") && keySplit[1].contains("target")) {
										String metadataTarget = metadatas.get(metadataKey);
										for(Target target : workflow.getTargets()) {
											signBook.getLiveWorkflow().getTargets().add(targetService.createTarget(target.getTargetUri() + "/" + metadataTarget));
										}
										logger.info("target set to : " + signBook.getLiveWorkflow().getTargets().get(0).getTargetUri());
									}
								}
							} else {
								targetService.copyTargets(workflow.getTargets(), signBook, null);
								signBookService.importWorkflow(signBook, workflow, null);
							}
							nextStepAndPending(signBook.getId(), null, user.getEppn(), authUser.getEppn());
							fsAccessService.remove(fsFile);
							nbImportedFiles++;
						}
					} else {
						logger.info("aucun fichier à importer depuis : " + workflow.getProtectedDocumentsSourceUri());
					}
				} catch (Exception e) {
					logger.error("error on import from " + workflow.getProtectedDocumentsSourceUri(), e.getMessage());
				}
				fsAccessService.close();
			} else {
				logger.warn("aucun service de fichier n'est disponible");
			}
		}
		return nbImportedFiles;
	}

	@Transactional
	public void warningReaded(String authUserEppn) {
		User authUser = userService.getUserByEppn(authUserEppn);
		List<SignRequest> oldSignRequests = signRequestRepository.findByCreateByEppnAndOlderPending(authUser.getId(), globalProperties.getNbDaysBeforeWarning());
		for (SignRequest signRequest : oldSignRequests) {
			signRequest.setWarningReaded(true);
		}
	}

}
