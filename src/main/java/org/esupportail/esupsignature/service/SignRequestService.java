package org.esupportail.esupsignature.service;

import com.google.zxing.WriterException;
import eu.europa.esig.dss.validation.reports.Reports;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.ActionType;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureMailException;
import org.esupportail.esupsignature.repository.DataRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessFactoryService;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessService;
import org.esupportail.esupsignature.service.interfaces.fs.FsFile;
import org.esupportail.esupsignature.service.interfaces.prefill.PreFillService;
import org.esupportail.esupsignature.service.mail.MailService;
import org.esupportail.esupsignature.service.security.otp.OtpService;
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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@EnableConfigurationProperties(GlobalProperties.class)
public class SignRequestService {

	private static final Logger logger = LoggerFactory.getLogger(SignRequestService.class);

	@Resource
	private SignRequestRepository signRequestRepository;

	@Resource
	private ActionService actionService;

	private final GlobalProperties globalProperties;

	@Resource
	private PdfService pdfService;

	@Resource
	private DocumentService documentService;

	@Resource
	private CustomMetricsService customMetricsService;

	@Resource
	private SignService signService;

	@Resource
	private FileService fileService;

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

	public SignRequestService(GlobalProperties globalProperties) {
		this.globalProperties = globalProperties;
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

	public List<SignRequest> getSignRequestsByToken(String token) {
		return signRequestRepository.findByToken(token);
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

	public boolean isNextWorkFlowStep(SignBook signBook) {
		return signBook.getLiveWorkflow().getLiveWorkflowSteps().size() >= signBook.getLiveWorkflow().getCurrentStepNumber() + 2;
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

	public boolean isMoreWorkflowStep(SignBook signBook) {
		int test = signBook.getLiveWorkflow().getCurrentStepNumber();
		return signBook.getLiveWorkflow().getLiveWorkflowSteps().size() >= signBook.getLiveWorkflow().getCurrentStepNumber() + 1 && test > -1;
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
									String name = signRequest.getTitle().replaceAll("\\W+", "_");
									if(signRequest.getParentSignBook().getLiveWorkflow().getWorkflow() != null && signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getTargetNamingTemplate() != null) {
										String template = signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getTargetNamingTemplate();
										if(template.isEmpty()) {
											template = globalProperties.getNamingTemplate();
										}
										name = generateName(signRequest.getParentSignBook(), signRequest.getTitle(), signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getName(), 0, userService.getSystemUser(), template);
									}
									documentService.exportDocument(documentIOType, targetUrl, signedFile, name);
									target.setTargetOk(true);
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
			signRequests.get(0).getParentSignBook().setStatus(SignRequestStatus.exported);
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

	public void cleanFiles(SignBook signBook, String authUserEppn) {
		int nbDocOnDataBase = 0;
		for(SignRequest signRequest : signBook.getSignRequests()) {
			cleanDocuments(signRequest, authUserEppn);
			nbDocOnDataBase += signRequest.getSignedDocuments().size();
		}
		if(nbDocOnDataBase == 0) {
			logger.info(signBook.getName() + " cleaned");
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
		//TODO critères de suppression ou en conf (if deleteDefinitive)
		SignRequest signRequest = signRequestRepository.findById(signRequestId).get();
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
						if(jsonExternalUserInfo.getPhone() != null) {
							tempUser.setPhone(jsonExternalUserInfo.getPhone());
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
			signRequestParams.setComment(commentText);
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
	public Map<String, Object> getToSignFileResponseWithCode(Long signRequestId) throws SQLException, EsupSignatureFsException, IOException, EsupSignatureException, WriterException {
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
				InputStream inputStream = pdfService.addQrCode(signRequest, document.getBigFile().getBinaryFile().getBinaryStream());
				return fileService.getFileResponse(inputStream.readAllBytes(), document.getFileName(), document.getContentType());
			}
		} else {
			FsFile fsFile = getLastSignedFsFile(signRequest);
			return fileService.getFileResponse(fsFile.getInputStream().readAllBytes(), fsFile.getName(), fsFile.getContentType());
		}
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

	public boolean replayNotif(Long id) throws EsupSignatureMailException {
		SignRequest signRequest = this.getById(id);
		List<String> recipientEmails = new ArrayList<>();
		getCurrentRecipients(signRequest).forEach(r -> recipientEmails.add(r.getUser().getEmail()));
		long notifTime = Duration.between(signRequest.getLastNotifDate().toInstant(), new Date().toInstant()).toHours();
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
				if(userService.findPersonLdapByUser(user) != null || user.getUserType().equals(UserType.external)) {
					recipientNotPresentsignRequests.remove(signRequest);
				}
			}
		}
		return recipientNotPresentsignRequests;
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

	public String generateName(SignBook signBook, String title, String worflowName, int order, User user, String template) {
		if(template.isEmpty()) {
			template = globalProperties.getNamingTemplate();
		}
		if(template.contains("[id]")) {
			template = template.replace("[id]", signBook.getId() + "");
		}
		if(template.contains("[title]")) {
			template = template.replace("[title]", title);
		}
		if(template.contains("[worflowName]")) {
			template = template.replace("[worflowName]", worflowName);
		}
		if(template.contains("[user.eppn]")) {
			template = template.replace("[user.eppn]", user.getEppn().replace("@", "_"));
		}
		if(template.contains("[user.name]")) {
			template = template.replace("[user.name]", user.getFirstname() + "-" + user.getName());
		}
		if(template.contains("[user.initials]")) {
			template = template.replace("[user.initials]", user.getName().substring(0,1).toUpperCase() + user.getFirstname().substring(0,1).toUpperCase());
		}
		if(template.contains("[UUID]")) {
			template = template.replace("[UUID]", UUID.randomUUID().toString());
		}
		if(template.contains("[order]")) {
			template = template.replace("[order]", order + "");
		}
		if(template.contains("[timestamp]")) {
			Date date = Calendar.getInstance().getTime();
			template = template.replace("[timestamp]", date.getTime() + "");
		}
		if(template.contains("[date-fr]")) {
			Date date = Calendar.getInstance().getTime();
			DateFormat dateFormat = new SimpleDateFormat("ddMMyyyyhhmm");
			String strDate = dateFormat.format(date);
			template = template.replace("[date-fr]", strDate);
		}
		if(template.contains("[date-en]")) {
			Date date = Calendar.getInstance().getTime();
			DateFormat dateFormat = new SimpleDateFormat("yyyyMMddhhmm");
			String strDate = dateFormat.format(date);
			template = template.replace("[date-en]", strDate);
		}
		if(signBook.getSignRequests().size() == 1) {
			Data data = dataService.getBySignRequest(signBook.getSignRequests().get(0));
			if(data != null) {
				for(Map.Entry<String, String> entry: data.getDatas().entrySet()) {
					if(template.contains("[form." + entry.getKey() + "]")) {
						template = template.replace("[form." + entry.getKey() + "]", entry.getValue());
					}
				}

			}
		}
		return template.replaceAll("\\W+", "_");
	}

}
