package org.esupportail.esupsignature.service.ui;
import eu.europa.esig.dss.validation.reports.Reports;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.io.FileUtils;
import org.esupportail.esupsignature.config.certificat.SealCertificatProperties;
import org.esupportail.esupsignature.dto.mapper.UiFetchSignRequestMapper;
import org.esupportail.esupsignature.dto.page.admin.AdminSignRequestShowViewDto;
import org.esupportail.esupsignature.dto.page.user.signrequest.ShowSignRequestContextDto;
import org.esupportail.esupsignature.dto.page.user.signrequest.ShowSignRequestDto;
import org.esupportail.esupsignature.dto.page.user.signrequest.SignRequestFullDto;
import org.esupportail.esupsignature.dto.page.user.signrequest.SignUiFrontDto;
import org.esupportail.esupsignature.dto.ws.RecipientWsDto;
import org.esupportail.esupsignature.entity.AuditTrail;
import org.esupportail.esupsignature.entity.Comment;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.Field;
import org.esupportail.esupsignature.entity.LiveWorkflow;
import org.esupportail.esupsignature.entity.LiveWorkflowStep;
import org.esupportail.esupsignature.entity.Log;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.enums.SignLevel;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.entity.enums.SignWith;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.AuditTrailService;
import org.esupportail.esupsignature.service.CertificatService;
import org.esupportail.esupsignature.service.CommentService;
import org.esupportail.esupsignature.service.LogService;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.SignWithService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.esupportail.esupsignature.service.utils.sign.SignService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class UiFetchSignRequestService {
    private final SignRequestService signRequestService;
    private final SignBookService signBookService;
    private final UserService userService;
    private final CommentService commentService;
    private final LogService logService;
    private final PreAuthorizeService preAuthorizeService;
    private final SignService signService;
    private final SignWithService signWithService;
    private final AuditTrailService auditTrailService;
    private final CertificatService certificatService;
    private final UiFetchSignRequestMapper mapper;
    public UiFetchSignRequestService(SignRequestService signRequestService, SignBookService signBookService, UserService userService, CommentService commentService, LogService logService, PreAuthorizeService preAuthorizeService, SignService signService, SignWithService signWithService, AuditTrailService auditTrailService, CertificatService certificatService, UiFetchSignRequestMapper mapper) {
        this.signRequestService = signRequestService;
        this.signBookService = signBookService;
        this.userService = userService;
        this.commentService = commentService;
        this.logService = logService;
        this.preAuthorizeService = preAuthorizeService;
        this.signService = signService;
        this.signWithService = signWithService;
        this.auditTrailService = auditTrailService;
        this.certificatService = certificatService;
        this.mapper = mapper;
    }
    @Transactional(readOnly = true)
    public ShowSignRequestContextDto buildShowSignRequestContext(Long id, String userEppn, String authUserEppn, HttpSession httpSession, boolean isOtpView) throws IOException {
        SignRequest signRequest = signRequestService.getByIdWithShowContext(id);
        SignBook signBook = signRequest.getParentSignBook();
        LiveWorkflow liveWorkflow = signBook.getLiveWorkflow();
        LiveWorkflowStep currentStep = liveWorkflow.getCurrentStep();
        Workflow workflow = liveWorkflow.getWorkflow();
        boolean signable = signBookService.checkSignRequestSignable(id, userEppn, authUserEppn);
        boolean editable = signRequestService.isEditable(id, userEppn);
        boolean manager = signBookService.checkUserManageRights(signBook.getId(), userEppn);
        boolean updateAllowed = preAuthorizeService.signBookUpdate(signBook.getId(), authUserEppn);
        boolean attachmentAlert = signRequestService.isAttachmentAlert(signRequest);
        boolean attachmentRequire = signRequestService.isAttachmentRequire(signRequest);
        SignType currentSignType = signRequest.getCurrentSignType();
        Integer currentStepNumber = liveWorkflow.getCurrentStepNumber();
        boolean currentStepMultiSign = true;
        boolean currentStepSingleSignWithAnnotation = true;
        Long currentStepId = null;
        SignLevel currentStepMinSignLevel = SignLevel.simple;
        SignLevel currentStepMaxSignLevel = SignLevel.qualified;
        Boolean stepRepeatable = null;
        SignType currentStepSignType = null;
        SignType currentStepRepeatableSignType = null;
        if (currentStep != null) {
            currentStepMinSignLevel = currentStep.getMinSignLevel();
            currentStepMaxSignLevel = currentStep.getMaxSignLevel();
            currentStepMultiSign = currentStep.getMultiSign();
            currentStepSingleSignWithAnnotation = currentStep.getSingleSignWithAnnotation();
            stepRepeatable = currentStep.getRepeatable();
            currentStepSignType = currentStep.getSignType();
            currentStepRepeatableSignType = currentStep.getRepeatableSignType();
            if (currentStep.getWorkflowStep() != null) {
                currentStepId = currentStep.getWorkflowStep().getId();
            }
        }
        List<Document> toSignDocuments = signRequestService.getToSignDocuments(signRequest.getId());
        Document toSignDocument = toSignDocuments.size() == 1 ? toSignDocuments.get(0) : null;
        boolean pdf = toSignDocument != null && "application/pdf".equals(toSignDocument.getContentType());
        String toSignDocumentContentType = toSignDocument != null ? toSignDocument.getContentType() : null;
        String pdfaCheck = !toSignDocuments.isEmpty() ? toSignDocuments.get(0).getPdfaCheck() : null;
        if (toSignDocuments.stream().anyMatch(document -> !document.isPdf()) && currentStepMinSignLevel.getValue() < 3) {
            currentStepMinSignLevel = SignLevel.advanced;
        }
        Reports reports = signService.validate(id);
        List<String> signatureIds = new ArrayList<>();
        boolean signatureIssue = false;
        if (reports != null) {
            signatureIds = reports.getSimpleReport().getSignatureIdList();
            for (String signatureId : signatureIds) {
                if (!reports.getSimpleReport().isValid(signatureId)) {
                    signatureIssue = true;
                    break;
                }
            }
            if (!signatureIds.isEmpty() && currentStepMinSignLevel.getValue() < 3) {
                currentStepMinSignLevel = SignLevel.advanced;
            }
        }
        List<Field> fields = signRequestService.prefillSignRequestFields(id, userEppn);
        List<SignRequestParams> currentSignRequestParamses = signRequestService.getToUseSignRequestParams(id, userEppn);
        List<Comment> comments = signRequestService.getComments(id);
        List<SignRequestParams> spots = signRequestService.getSpots(id);
        List<String> signImages = new ArrayList<>();
        String signImagesWarningMessage = null;
        try {
            signImages = fetchSignImagesForRequest(id, userEppn, authUserEppn, httpSession);
        } catch (EsupSignatureUserException e) {
            signImagesWarningMessage = e.getMessage();
        }
        String action = null;
        Set<String> supervisors = null;
        if (signRequest.getData() != null
                && signRequest.getData().getForm() != null
                && signRequest.getData().getForm().getWorkflow() != null) {
            action = signRequest.getData().getForm().getAction();
            supervisors = signRequest.getData().getForm().getWorkflow().getManagers();
        }
        boolean notSigned = !signRequestService.isSigned(signRequest, reports);
        Long dataId = signRequest.getData() != null ? signRequest.getData().getId() : null;
        Long formId = signRequest.getData() != null && signRequest.getData().getForm() != null
                ? signRequest.getData().getForm().getId() : null;
        boolean hasDocumentsHistory = signRequest.getDocumentsHistory() != null;
        String exportedDocumentURI = signRequest.getExportedDocumentURI();
        Long workflowId = workflow != null ? workflow.getId() : null;
        SignRequestStatus signBookStatus = signBook.getStatus();
        boolean auditTrailChecked = signBookStatus.equals(SignRequestStatus.completed)
                || signBookStatus.equals(SignRequestStatus.exported);
        List<ShowSignRequestDto.SignBookViewerDto> signBookViewers = signBookService.getSignBookViewerDtos(signBook.getId());
        var signBookLight = mapper.toSignBookLightDto(signBook, signBookViewers);
        var signRequestLight = mapper.toSignRequestLightDto(signRequest);
        var workflowMeta = mapper.toWorkflowMetaDto(workflow);
        List<ShowSignRequestDto.StepDto> steps = signBookService.getLiveWorkflowStepDtos(signBook.getId());
        List<ShowSignRequestDto.TargetDto> targets = signBookService.getLiveWorkflowTargetDtos(signBook.getId());
        List<ShowSignRequestDto.SignRequestLightDto> clonedSignRequests = signRequest.getStatus() == SignRequestStatus.refused
                ? signRequestService.getCloneSignRequestLightProjections(id).stream().map(mapper::toSignRequestLightDto).toList()
                : List.of();
        List<ShowSignRequestDto.AttachmentDto> attachments = signRequestService.getAttachmentProjections(id).stream().map(mapper::toAttachmentDto).toList();
        var signedDocProj = signRequestService.getSignedDocumentProjections(id);
        List<ShowSignRequestDto.DocumentDto> originalDocuments = signRequestService.getOriginalDocumentProjections(id).stream().map(mapper::toDocumentDto).toList();
        List<ShowSignRequestDto.DocumentDto> signedDocuments = signedDocProj.stream().map(mapper::toDocumentDto).toList();
        String lastSignedDocumentContentType = !signedDocProj.isEmpty()
                ? signedDocProj.get(signedDocProj.size() - 1).getContentType() : null;
        List<ShowSignRequestDto.SignRequestTabDto> signRequestTabs = signRequestService
                .getSignRequestTabProjections(signBook.getId(), userEppn).stream().map(mapper::toSignRequestTabDto).toList();
        int nbSignRequestInSignBookParent = signRequestTabs.size();
        boolean lastStep = !steps.isEmpty() && currentStepNumber != null && currentStepNumber >= steps.size();
        Map<Long, ShowSignRequestDto.RecipientActionDto> recipientActions = new LinkedHashMap<>();
        if (signRequest.getRecipientHasSigned() != null) {
            signRequest.getRecipientHasSigned().forEach((recipient, recipientAction) -> {
                if (recipient != null && recipient.getId() != null && recipientAction != null) {
                    ShowSignRequestDto.RecipientActionDto d = new ShowSignRequestDto.RecipientActionDto();
                    d.setActionType(recipientAction.getActionType());
                    d.setDate(recipientAction.getDate());
                    recipientActions.put(recipient.getId(), d);
                }
            });
        }
        boolean viewedByCurrentUser = signRequest.getViewedBy() != null
                && signRequest.getViewedBy().stream().anyMatch(u -> u != null && userEppn.equals(u.getEppn()));
        boolean displayNotif = !isOtpView && signRequestService.isDisplayNotif(signRequest, userEppn);
        boolean tempUsers = !isOtpView && signBookService.isTempUsers(signBook.getId());
        boolean currentUserAsSigned = signRequestService.isCurrentUserAsSigned(signRequest, userEppn);
        boolean viewRight = preAuthorizeService.checkUserViewRights(id, userEppn, authUserEppn);
        AuditTrail auditTrail = null;
        String auditTrailSize = null;
        if (!signRequest.getStatus().equals(SignRequestStatus.draft)
                && !signRequest.getStatus().equals(SignRequestStatus.pending)
                && !signRequest.getStatus().equals(SignRequestStatus.refused)
                && !signRequest.getDeleted()) {
            auditTrail = auditTrailService.getAuditTrailByToken(signRequest.getToken());
            if (auditTrail != null && auditTrail.getDocumentSize() != null) {
                auditTrailSize = FileUtils.byteCountToDisplaySize(auditTrail.getDocumentSize());
            }
        }
        List<SignWith> signWiths = new ArrayList<>();
        if (reports != null) {
            signWiths = signWithService.getAuthorizedSignWiths(userEppn, signRequest, !signatureIds.isEmpty());
        } else if (signable) {
            signWiths = signWithService.getAuthorizedSignWiths(userEppn, signRequest, false);
        }
        boolean sealCertOK = signWithService.checkSealCertificat(userEppn, true);
        List<SealCertificatProperties> sealCertificatPropertieses = certificatService.getAuthorizedSealCertificatProperties(userEppn);
        SignBook nextSignBook = signBookService.getNextSignBook(id, userEppn, authUserEppn);
        SignRequest nextSignRequest = nextSignBook != null ? signBookService.getNextSignRequest(id, nextSignBook.getId()) : null;
        boolean hasNextSignBook = nextSignBook != null;
        Long nextSignRequestId = nextSignRequest != null ? nextSignRequest.getId() : null;
        List<RecipientWsDto> externalsRecipients = auditTrailChecked ? signRequestService.getExternalRecipients(id) : new ArrayList<>();
        List<Log> logs = logService.getFullBySignRequest(id);
        List<Comment> postits = signRequestService.getPostits(id);
        User frontUser = userService.getFullUserByEppn(userEppn);
        User frontAuthUser = userService.getByEppn(authUserEppn);
        Integer signImageNumber = frontUser != null ? frontUser.getDefaultSignImageNumber() : null;
        boolean restore = frontUser == null || frontUser.getFavoriteSignRequestParams() == null;
        String phone = frontUser != null ? frontUser.getPhone() : null;
        Boolean returnToHomeAfterSign = frontUser != null ? frontUser.getReturnToHomeAfterSign() : null;
        String userName = mapper.toDisplayName(frontUser);
        String authUserName = mapper.toDisplayName(frontAuthUser);
        var fieldFrontDtos = mapper.toFieldFrontDtos(fields, workflow);
        ShowSignRequestContextDto context = new ShowSignRequestContextDto();
        context.setUserEppn(userEppn);
        context.setAuthUserEppn(authUserEppn);
        context.setOtpView(isOtpView);
        context.setSignImagesWarningMessage(signImagesWarningMessage);
        context.setWorkflowId(workflowId);
        context.setCurrentStepSignType(currentStepSignType);
        context.setSignRequestId(id);
        context.setSignRequestStatus(signRequest.getStatus());
        context.setSignRequestDeleted(signRequest.getDeleted());
        context.setSignRequestToken(signRequest.getToken());
        context.setDataId(dataId);
        context.setFormId(formId);
        context.setHasDocumentsHistory(hasDocumentsHistory);
        context.setExportedDocumentURI(exportedDocumentURI);
        context.setPdfaCheck(pdfaCheck);
        context.setSignBookId(signBook.getId());
        context.setSignBookStatus(signBookStatus);
        context.setSignBookLight(signBookLight);
        context.setSignRequestLight(signRequestLight);
        context.setWorkflowMeta(workflowMeta);
        context.setWorkflowAvailable(workflow != null);
        context.setSignable(signable);
        context.setEditable(editable);
        context.setManager(manager);
        context.setAttachmentAlert(attachmentAlert);
        context.setAttachmentRequire(attachmentRequire);
        context.setCurrentSignType(currentSignType);
        context.setCurrentStepNumber(currentStepNumber);
        context.setCurrentStepId(currentStepId);
        context.setCurrentStepMultiSign(currentStepMultiSign);
        context.setCurrentStepSingleSignWithAnnotation(currentStepSingleSignWithAnnotation);
        context.setCurrentStepMinSignLevel(currentStepMinSignLevel);
        context.setCurrentStepMaxSignLevel(currentStepMaxSignLevel);
        context.setCurrentStepRepeatableSignType(currentStepRepeatableSignType);
        context.setStepRepeatable(stepRepeatable);
        context.setNbSignRequestInSignBookParent(nbSignRequestInSignBookParent);
        context.setLastStep(lastStep);
        context.setSteps(steps);
        context.setTargets(targets);
        context.setClonedSignRequests(clonedSignRequests);
        context.setAttachments(attachments);
        context.setOriginalDocuments(originalDocuments);
        context.setSignedDocuments(signedDocuments);
        context.setLastSignedDocumentContentType(lastSignedDocumentContentType);
        context.setSignRequestTabs(signRequestTabs);
        context.setRecipientActions(recipientActions);
        context.setToSignDocumentContentType(toSignDocumentContentType);
        context.setPdf(pdf);
        context.setReports(reports);
        context.setSignatureIds(signatureIds);
        context.setSignatureIssue(signatureIssue);
        context.setFieldFrontDtos(fieldFrontDtos);
        context.setCurrentSignRequestParamses(currentSignRequestParamses);
        context.setComments(comments);
        context.setSpots(spots);
        context.setSignImages(signImages);
        context.setAction(action);
        context.setSupervisors(supervisors);
        context.setNotSigned(notSigned);
        context.setViewedByCurrentUser(viewedByCurrentUser);
        context.setDisplayNotif(displayNotif);
        context.setTempUsers(tempUsers);
        context.setCurrentUserAsSigned(currentUserAsSigned);
        context.setViewRight(viewRight);
        context.setAuditTrailChecked(auditTrailChecked);
        context.setHasNextSignBook(hasNextSignBook);
        context.setNextSignRequestId(nextSignRequestId);
        context.setAuditTrail(auditTrail);
        context.setAuditTrailSize(auditTrailSize);
        context.setSignWiths(signWiths);
        context.setSealCertOK(sealCertOK);
        context.setSealCertificatPropertieses(sealCertificatPropertieses);
        context.setExternalsRecipients(externalsRecipients);
        context.setLogs(logs);
        context.setPostits(postits);
        context.setSignImageNumber(signImageNumber);
        context.setRestore(restore);
        context.setPhone(phone);
        context.setReturnToHomeAfterSign(returnToHomeAfterSign);
        context.setUserName(userName);
        context.setAuthUserName(authUserName);
        SignRequestFullDto common = mapper.toCommonDto(context, updateAllowed);
        context.setShowSignRequest(buildShowSignRequestBackDtoInternal(context, common));
        context.setSignUiFront(buildSignUiFrontDtoInternal(context, common));
        return context;
    }
    @Transactional(readOnly = true)
    public AdminSignRequestShowViewDto buildAdminSignRequestShowView(Long id) {
        SignRequest signRequest = signRequestService.getByIdWithShowContext(id);
        if (signRequest == null) {
            return null;
        }
        SignBook signBook = signRequest.getParentSignBook();
        LiveWorkflow liveWorkflow = signBook != null ? signBook.getLiveWorkflow() : null;
        LiveWorkflowStep currentStep = liveWorkflow != null ? liveWorkflow.getCurrentStep() : null;
        Workflow workflow = liveWorkflow != null ? liveWorkflow.getWorkflow() : null;
        List<ShowSignRequestDto.DocumentDto> originalDocuments = signRequestService.getOriginalDocumentProjections(id).stream().map(mapper::toDocumentDto).toList();
        List<ShowSignRequestDto.DocumentDto> signedDocuments = signRequestService.getSignedDocumentProjections(id).stream().map(mapper::toDocumentDto).toList();
        List<ShowSignRequestDto.DocumentDto> documentsHistory = signRequestService.getDocumentsHistoryProjections(id).stream().map(mapper::toDocumentDto).toList();
        var comments = commentService.getAdminCommentsBySignRequest(id);
        var logs = logService.getAdminLogsBySignRequest(signRequest.getId());
        AdminSignRequestShowViewDto dto = new AdminSignRequestShowViewDto();
        dto.setSignBookLight(mapper.toSignBookLightDto(signBook));
        dto.setSignRequestLight(mapper.toSignRequestLightDto(signRequest));
        dto.setSignRequestFull(mapper.toAdminSignRequestFullDto(signRequest, signBook, liveWorkflow, currentStep));
        dto.setWorkflow(mapper.toWorkflowMetaDto(workflow));
        dto.setOriginalDocuments(originalDocuments);
        dto.setSignedDocuments(signedDocuments);
        dto.setDocumentsHistory(documentsHistory);
        dto.setSteps(liveWorkflow == null ? List.of() : liveWorkflow.getLiveWorkflowSteps().stream().map(mapper::toStepDto).toList());
        dto.setTargets(liveWorkflow == null ? List.of() : liveWorkflow.getTargets().stream().map(mapper::toTargetDto).toList());
        dto.setComments(comments.stream().map(mapper::toAdminCommentDto).toList());
        dto.setLogs(logs.stream().map(mapper::toAdminLogDto).toList());
        dto.setManager(true);
        return dto;
    }

    private ShowSignRequestDto buildShowSignRequestBackDtoInternal(ShowSignRequestContextDto context, SignRequestFullDto common) {
        ShowSignRequestDto dto = new ShowSignRequestDto();
        dto.setSignBookLight(context.getSignBookLight());
        dto.setSignRequestLight(context.getSignRequestLight());
        dto.setSignRequestFull(common);
        dto.setWorkflow(context.getWorkflowMeta());
        dto.setUrlProfil(context.isOtpView() ? "otp" : "user");
        dto.setDisplayNotif(context.isDisplayNotif());
        dto.setTempUsers(context.isTempUsers());
        dto.setLastStep(context.isLastStep());
        dto.setCurrentUserAsSigned(context.isCurrentUserAsSigned());
        dto.setSignatureIds(context.getSignatureIds());
        dto.setSignatureIssue(context.isSignatureIssue());
        dto.setSupervisors(context.getSupervisors());
        dto.setToSignDocumentContentType(context.getToSignDocumentContentType());
        dto.setPostits(context.getPostits());
        dto.setAttachments(context.getAttachments());
        dto.setOriginalDocuments(context.getOriginalDocuments());
        dto.setSignedDocuments(context.getSignedDocuments());
        dto.setExportedDocumentURI(context.getExportedDocumentURI());
        dto.setLastSignedDocumentContentType(context.getLastSignedDocumentContentType());
        dto.setHasNextSignBook(context.isHasNextSignBook());
        dto.setNextSignRequestId(context.getNextSignRequestId());
        dto.setSignWiths(context.getSignWiths());
        dto.setAuditTrail(context.getAuditTrail());
        dto.setSize(context.getAuditTrailSize());
        dto.setSealCertOK(context.isSealCertOK());
        dto.setSealCertificatPropertieses(context.getSealCertificatPropertieses());
        dto.setSteps(context.getSteps());
        dto.setTargets(context.getTargets());
        dto.setClonedSignRequests(context.getClonedSignRequests());
        dto.setRecipientActions(context.getRecipientActions());
        dto.setSignRequestTabs(context.getSignRequestTabs());
        dto.setLiveWorkflowStepCount(context.getSteps() != null ? context.getSteps().size() : 0);
        dto.setViewedByCurrentUser(context.isViewedByCurrentUser());
        dto.setViewRight(context.isViewRight());
        dto.setLogs(context.getLogs());
        dto.setPdfaCheck(context.getPdfaCheck());
        dto.setAuditTrailChecked(context.isAuditTrailChecked());
        dto.setExternalsRecipients(context.getExternalsRecipients());
        return dto;
    }
    private SignUiFrontDto buildSignUiFrontDtoInternal(ShowSignRequestContextDto context, SignRequestFullDto common) {
        SignUiFrontDto dto = new SignUiFrontDto();
        dto.setSignRequestId(common.getSignRequestId());
        dto.setDataId(common.getDataId());
        dto.setFormId(common.getFormId());
        dto.setSteps(context.getSteps() != null ? context.getSteps() : List.of());
        dto.setCurrentSignRequestParamses(mapper.toSignRequestParamsFrontDtos(common.getSignRequestParams()));
        dto.setSignImageNumber(context.getSignImageNumber());
        dto.setCurrentSignType(common.getCurrentSignType());
        dto.setSignable(common.getSignable());
        dto.setEditable(common.getEditable());
        dto.setComments(mapper.toCommentFrontDtos(common.getComments(), context));
        dto.setSpots(mapper.toSignRequestParamsFrontDtos(common.getSpots()));
        dto.setPdf(common.getPdf());
        dto.setCurrentStepNumber(common.getCurrentStepNumber());
        dto.setCurrentStepMultiSign(common.getCurrentStepMultiSign());
        dto.setCurrentStepSingleSignWithAnnotation(common.getCurrentStepSingleSignWithAnnotation());
        dto.setCurrentStepMinSignLevel(common.getCurrentStepMinSignLevel());
        dto.setWorkflowAvailable(context.isWorkflowAvailable());
        dto.setSignImages(common.getSignImages());
        dto.setUserName(context.getUserName());
        dto.setAuthUserName(context.getAuthUserName());
        dto.setFields(context.getFieldFrontDtos());
        dto.setStepRepeatable(common.getStepRepeatable());
        dto.setStatus(common.getStatus());
        dto.setAction(common.getAction());
        dto.setNbSignRequests(common.getNbSignRequests());
        dto.setNotSigned(common.getNotSigned());
        dto.setAttachmentAlert(common.getAttachmentAlert());
        dto.setAttachmentRequire(common.getAttachmentRequire());
        dto.setOtp(context.isOtpView());
        dto.setRestore(context.isRestore());
        dto.setPhone(context.getPhone());
        dto.setReturnToHomeAfterSign(context.getReturnToHomeAfterSign());
        dto.setManager(common.getManager());
        return dto;
    }
    private List<String> fetchSignImagesForRequest(Long signRequestId, String userEppn, String authUserEppn, HttpSession httpSession)
            throws IOException, EsupSignatureUserException {
        Long userShareId = getUserShareId(httpSession);
        return signBookService.getSignImagesForSignRequest(signRequestId, userEppn, authUserEppn, userShareId);
    }
    private Long getUserShareId(HttpSession httpSession) {
        if (httpSession == null) {
            return null;
        }
        Object userShareString = httpSession.getAttribute("userShareId");
        if (userShareString == null) {
            return null;
        }
        return Long.valueOf(userShareString.toString());
    }
}
