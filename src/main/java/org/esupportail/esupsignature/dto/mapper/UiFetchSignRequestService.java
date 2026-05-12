package org.esupportail.esupsignature.dto.mapper;

import eu.europa.esig.dss.validation.reports.Reports;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.io.FileUtils;
import org.esupportail.esupsignature.config.certificat.SealCertificatProperties;
import org.esupportail.esupsignature.dto.page.admin.AdminSignRequestShowViewDto;
import org.esupportail.esupsignature.dto.page.user.signbook.SignBookLightDto;
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
    private final LogService logService;
    private final PreAuthorizeService preAuthorizeService;
    private final SignService signService;
    private final SignWithService signWithService;
    private final AuditTrailService auditTrailService;
    private final CertificatService certificatService;
    private final UiFetchSignRequestMapper mapper;

    public UiFetchSignRequestService(SignRequestService signRequestService, SignBookService signBookService, UserService userService, LogService logService, PreAuthorizeService preAuthorizeService, SignService signService, SignWithService signWithService, AuditTrailService auditTrailService, CertificatService certificatService, UiFetchSignRequestMapper mapper) {
        this.signRequestService = signRequestService;
        this.signBookService = signBookService;
        this.userService = userService;
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

        if (currentStep != null) {
            currentStepMinSignLevel = currentStep.getMinSignLevel();
            currentStepMaxSignLevel = currentStep.getMaxSignLevel();
            currentStepMultiSign = currentStep.getMultiSign();
            currentStepSingleSignWithAnnotation = currentStep.getSingleSignWithAnnotation();
            stepRepeatable = currentStep.getRepeatable();
            if (currentStep.getWorkflowStep() != null) {
                currentStepId = currentStep.getWorkflowStep().getId();
            }
        }

        int nbSignRequestInSignBookParent = signBook.getSignRequests().size();
        boolean lastStep = !liveWorkflow.getLiveWorkflowSteps().isEmpty() && currentStepNumber >= liveWorkflow.getLiveWorkflowSteps().size();
        List<Document> toSignDocuments = signRequestService.getToSignDocuments(signRequest.getId());
        Document toSignDocument = toSignDocuments.size() == 1 ? toSignDocuments.get(0) : null;
        boolean pdf = toSignDocument != null && "application/pdf".equals(toSignDocument.getContentType());
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

        User frontUser = userService.getFullUserByEppn(userEppn);
        User frontAuthUser = userService.getByEppn(authUserEppn);
        String action = null;
        Set<String> supervisors = null;
        if (signRequest.getData() != null && signRequest.getData().getForm() != null && signRequest.getData().getForm().getWorkflow() != null) {
            action = signRequest.getData().getForm().getAction();
            supervisors = signRequest.getData().getForm().getWorkflow().getManagers();
        }

        boolean notSigned = !signRequestService.isSigned(signRequest, reports);

        ShowSignRequestContextDto context = new ShowSignRequestContextDto();
        context.setUserEppn(userEppn);
        context.setAuthUserEppn(authUserEppn);
        context.setOtpView(isOtpView);
        context.setWorkflowId(workflow != null ? workflow.getId() : null);
        context.setCurrentStepSignType(currentStep != null ? currentStep.getSignType() : null);
        context.setSignRequest(signRequest);
        context.setSignBook(signBook);
        context.setLiveWorkflow(liveWorkflow);
        context.setCurrentStep(currentStep);
        context.setWorkflow(workflow);
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
        context.setStepRepeatable(stepRepeatable);
        context.setNbSignRequestInSignBookParent(nbSignRequestInSignBookParent);
        context.setLastStep(lastStep);
        context.setToSignDocuments(toSignDocuments);
        context.setToSignDocument(toSignDocument);
        context.setPdf(pdf);
        context.setReports(reports);
        context.setSignatureIds(signatureIds);
        context.setSignatureIssue(signatureIssue);
        context.setFields(fields);
        context.setCurrentSignRequestParamses(currentSignRequestParamses);
        context.setComments(comments);
        context.setSpots(spots);
        context.setSignImages(signImages);
        context.setSignImagesWarningMessage(signImagesWarningMessage);
        context.setFrontUser(frontUser);
        context.setFrontAuthUser(frontAuthUser);
        context.setAction(action);
        context.setSupervisors(supervisors);
        context.setNotSigned(notSigned);

        boolean updateAllowed = preAuthorizeService.signBookUpdate(signBook.getId(), authUserEppn);
        SignRequestFullDto common = mapper.toCommonDto(context, updateAllowed);
        ShowSignRequestDto showSignRequest = buildShowSignRequestBackDtoInternal(context, common);
        SignUiFrontDto signUiFront = buildSignUiFrontDtoInternal(context, common);
        context.setShowSignRequest(showSignRequest);
        context.setSignUiFront(signUiFront);
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

        List<Comment> comments = signRequestService.getComments(id);
        List<Log> logs = logService.getFullBySignRequest(signRequest.getId());

        AdminSignRequestShowViewDto dto = new AdminSignRequestShowViewDto();
        dto.setSignBookLight(mapper.toSignBookLightDto(signBook));
        dto.setSignRequestLight(mapper.toSignRequestLightDto(signRequest));
        dto.setSignRequestFull(mapper.toAdminSignRequestFullDto(signRequest, signBook, liveWorkflow, currentStep));
        dto.setWorkflow(mapper.toWorkflowMetaDto(workflow));
        dto.setOriginalDocuments(signRequest.getOriginalDocuments() == null ? List.of() : signRequest.getOriginalDocuments().stream().map(mapper::toDocumentDto).toList());
        dto.setSignedDocuments(signRequest.getSignedDocuments() == null ? List.of() : signRequest.getSignedDocuments().stream().map(mapper::toDocumentDto).toList());
        dto.setDocumentsHistory(signRequest.getDocumentsHistory() == null ? List.of() : List.of(mapper.toDocumentDto(signRequest.getDocumentsHistory())));
        dto.setSteps(liveWorkflow == null ? List.of() : liveWorkflow.getLiveWorkflowSteps().stream().map(mapper::toStepDto).toList());
        dto.setTargets(liveWorkflow == null ? List.of() : liveWorkflow.getTargets().stream().map(mapper::toTargetDto).toList());
        dto.setComments(comments.stream().map(mapper::toAdminCommentDto).toList());
        dto.setLogs(logs.stream().map(mapper::toAdminLogDto).toList());
        dto.setManager(true);
        return dto;
    }

    public ShowSignRequestDto buildShowSignRequestBackDto(ShowSignRequestContextDto context) {
        return context.getShowSignRequest();
    }

    private ShowSignRequestDto buildShowSignRequestBackDtoInternal(ShowSignRequestContextDto context, SignRequestFullDto common) {
        SignRequest signRequest = context.getSignRequest();
        SignBook signBook = context.getSignBook();
        String userEppn = context.getUserEppn();
        String authUserEppn = context.getAuthUserEppn();
        ShowSignRequestDto.SignRequestLightDto request = mapper.toSignRequestLightDto(signRequest);
        Workflow workflow = context.getWorkflow();
        SignBookLightDto signBookLight = mapper.toSignBookLightDto(signBook);
        ShowSignRequestDto.WorkflowMetaDto workflowMeta = mapper.toWorkflowMetaDto(workflow);

        boolean displayNotif = !context.isOtpView() && signRequestService.isDisplayNotif(signRequest, userEppn);
        boolean tempUsers = !context.isOtpView() && signBookService.isTempUsers(signBook.getId());
        String toSignDocumentContentType = context.getToSignDocument() != null ? context.getToSignDocument().getContentType() : null;
        List<Comment> postits = signRequestService.getPostits(signRequest.getId());
        List<ShowSignRequestDto.AttachmentDto> attachments = signRequestService.getAttachments(signRequest.getId()).stream()
                .map(mapper::toAttachmentDto)
                .toList();
        List<ShowSignRequestDto.DocumentDto> originalDocuments = signRequest.getOriginalDocuments().stream()
                .map(mapper::toDocumentDto)
                .toList();
        List<ShowSignRequestDto.DocumentDto> signedDocuments = signRequest.getSignedDocuments().stream()
                .map(mapper::toDocumentDto)
                .toList();
        String exportedDocumentURI = signRequest.getExportedDocumentURI();
        String lastSignedDocumentContentType = signRequest.getLastSignedDocument() != null
                ? signRequest.getLastSignedDocument().getContentType()
                : null;
        SignBook nextSignBook = signBookService.getNextSignBook(signRequest.getId(), userEppn, authUserEppn);
        SignRequest nextSignRequest = nextSignBook != null
                ? signBookService.getNextSignRequest(signRequest.getId(), nextSignBook.getId())
                : null;
        boolean hasNextSignBook = nextSignBook != null;
        Long nextSignRequestId = nextSignRequest != null ? nextSignRequest.getId() : null;
        List<SignWith> signWiths = new ArrayList<>();
        if (context.getReports() != null) {
            signWiths = signWithService.getAuthorizedSignWiths(userEppn, signRequest, !context.getSignatureIds().isEmpty());
        } else if (context.isSignable()) {
            signWiths = signWithService.getAuthorizedSignWiths(userEppn, signRequest, false);
        }

        AuditTrail auditTrail = null;
        String size = null;
        if (!signRequest.getStatus().equals(SignRequestStatus.draft)
                && !signRequest.getStatus().equals(SignRequestStatus.pending)
                && !signRequest.getStatus().equals(SignRequestStatus.refused)
                && !signRequest.getDeleted()) {
            auditTrail = auditTrailService.getAuditTrailByToken(signRequest.getToken());
            if (auditTrail != null && auditTrail.getDocumentSize() != null) {
                size = FileUtils.byteCountToDisplaySize(auditTrail.getDocumentSize());
            }
        }

        boolean sealCertOK = signWithService.checkSealCertificat(userEppn, true);
        List<SealCertificatProperties> sealCertificatPropertieses = certificatService.getAuthorizedSealCertificatProperties(userEppn);
        List<ShowSignRequestDto.StepDto> steps = context.getLiveWorkflow() != null
                ? context.getLiveWorkflow().getLiveWorkflowSteps().stream().map(mapper::toStepDto).toList()
                : new ArrayList<>();
        List<ShowSignRequestDto.TargetDto> targets = context.getLiveWorkflow() != null
                ? context.getLiveWorkflow().getTargets().stream().map(mapper::toTargetDto).toList()
                : new ArrayList<>();
        Map<Long, ShowSignRequestDto.RecipientActionDto> recipientActions = new LinkedHashMap<>();
        if (signRequest.getRecipientHasSigned() != null) {
            signRequest.getRecipientHasSigned().forEach((recipient, action) -> {
                if (recipient != null && recipient.getId() != null && action != null) {
                    ShowSignRequestDto.RecipientActionDto recipientActionDto = new ShowSignRequestDto.RecipientActionDto();
                    recipientActionDto.setActionType(action.getActionType());
                    recipientActionDto.setDate(action.getDate());
                    recipientActions.put(recipient.getId(), recipientActionDto);
                }
            });
        }
        List<ShowSignRequestDto.SignRequestTabDto> signRequestTabs = signBook.getSignRequests().stream()
                .map(mapper::toSignRequestTabDto)
                .toList();
        boolean viewedByCurrentUser = isViewedByUser(signRequest, userEppn);
        boolean viewRight = preAuthorizeService.checkUserViewRights(signRequest.getId(), userEppn, authUserEppn);
        List<Log> logs = logService.getFullBySignRequest(signRequest.getId());
        String pdfaCheck = !context.getToSignDocuments().isEmpty() ? context.getToSignDocuments().get(0).getPdfaCheck() : null;
        boolean auditTrailChecked = signBook.getStatus().equals(SignRequestStatus.completed) || signBook.getStatus().equals(SignRequestStatus.exported);
        List<RecipientWsDto> externalsRecipients = auditTrailChecked
                ? signRequestService.getExternalRecipients(signRequest.getId())
                : new ArrayList<>();

        ShowSignRequestDto dto = new ShowSignRequestDto();
        dto.setSignBookLight(signBookLight);
        dto.setSignRequestLight(request);
        dto.setSignRequestFull(common);
        dto.setWorkflow(workflowMeta);
        dto.setUrlProfil(context.isOtpView() ? "otp" : "user");
        dto.setDisplayNotif(displayNotif);
        dto.setTempUsers(tempUsers);
        dto.setLastStep(context.isLastStep());
        dto.setCurrentUserAsSigned(signRequestService.isCurrentUserAsSigned(signRequest, userEppn));
        dto.setSignatureIds(context.getSignatureIds());
        dto.setSignatureIssue(context.isSignatureIssue());
        dto.setSupervisors(context.getSupervisors());
        dto.setToSignDocumentContentType(toSignDocumentContentType);
        dto.setPostits(postits);
        dto.setAttachments(attachments);
        dto.setOriginalDocuments(originalDocuments);
        dto.setSignedDocuments(signedDocuments);
        dto.setExportedDocumentURI(exportedDocumentURI);
        dto.setLastSignedDocumentContentType(lastSignedDocumentContentType);
        dto.setHasNextSignBook(hasNextSignBook);
        dto.setNextSignRequestId(nextSignRequestId);
        dto.setSignWiths(signWiths);
        dto.setAuditTrail(auditTrail);
        dto.setSize(size);
        dto.setSealCertOK(sealCertOK);
        dto.setSealCertificatPropertieses(sealCertificatPropertieses);
        dto.setSteps(steps);
        dto.setTargets(targets);
        dto.setRecipientActions(recipientActions);
        dto.setSignRequestTabs(signRequestTabs);
        dto.setLiveWorkflowStepCount(steps.size());
        dto.setViewedByCurrentUser(viewedByCurrentUser);
        dto.setViewRight(viewRight);
        dto.setLogs(logs);
        dto.setPdfaCheck(pdfaCheck);
        dto.setAuditTrailChecked(auditTrailChecked);
        dto.setExternalsRecipients(externalsRecipients);
        return dto;
    }

    public SignUiFrontDto buildSignUiFrontDto(ShowSignRequestContextDto context) {
        return context.getSignUiFront();
    }

    private SignUiFrontDto buildSignUiFrontDtoInternal(ShowSignRequestContextDto context, SignRequestFullDto common) {
        User frontUser = context.getFrontUser();
        User frontAuthUser = context.getFrontAuthUser();

        SignUiFrontDto dto = new SignUiFrontDto();
        dto.setSignRequestId(common.getSignRequestId());
        dto.setDataId(common.getDataId());
        dto.setFormId(common.getFormId());
        dto.setSteps(context.getLiveWorkflow() != null
                ? context.getLiveWorkflow().getLiveWorkflowSteps().stream().map(mapper::toStepDto).toList()
                : List.of());
        dto.setCurrentSignRequestParamses(mapper.toSignRequestParamsFrontDtos(common.getSignRequestParams()));
        dto.setSignImageNumber(frontUser != null ? frontUser.getDefaultSignImageNumber() : null);
        dto.setCurrentSignType(common.getCurrentSignType());
        dto.setSignable(common.getSignable());
        dto.setEditable(common.getEditable());
        dto.setComments(mapper.toCommentFrontDtos(common.getComments()));
        dto.setSpots(mapper.toSignRequestParamsFrontDtos(common.getSpots()));
        dto.setPdf(common.getPdf());
        dto.setCurrentStepNumber(common.getCurrentStepNumber());
        dto.setCurrentStepMultiSign(common.getCurrentStepMultiSign());
        dto.setCurrentStepSingleSignWithAnnotation(common.getCurrentStepSingleSignWithAnnotation());
        dto.setCurrentStepMinSignLevel(common.getCurrentStepMinSignLevel());
        dto.setWorkflowAvailable(context.getWorkflow() != null);
        dto.setSignImages(common.getSignImages());
        dto.setUserName(mapper.toDisplayName(frontUser));
        dto.setAuthUserName(mapper.toDisplayName(frontAuthUser));
        dto.setFields(mapper.toFieldFrontDtos(common.getFields(), context.getWorkflow()));
        dto.setStepRepeatable(common.getStepRepeatable());
        dto.setStatus(common.getStatus());
        dto.setAction(common.getAction());
        dto.setNbSignRequests(common.getNbSignRequests());
        dto.setNotSigned(common.getNotSigned());
        dto.setAttachmentAlert(common.getAttachmentAlert());
        dto.setAttachmentRequire(common.getAttachmentRequire());
        dto.setOtp(context.isOtpView());
        dto.setRestore(frontUser == null || frontUser.getFavoriteSignRequestParams() == null);
        dto.setPhone(frontUser != null ? frontUser.getPhone() : null);
        dto.setReturnToHomeAfterSign(frontUser != null ? frontUser.getReturnToHomeAfterSign() : null);
        dto.setManager(common.getManager());
        return dto;
    }

    private boolean isViewedByUser(SignRequest signRequest, String userEppn) {
        return signRequest != null
                && userEppn != null
                && signRequest.getViewedBy() != null
                && signRequest.getViewedBy().stream().anyMatch(user -> user != null && userEppn.equals(user.getEppn()));
    }

    private List<String> fetchSignImagesForRequest(Long signRequestId, String userEppn, String authUserEppn, HttpSession httpSession) throws IOException, EsupSignatureUserException {
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


