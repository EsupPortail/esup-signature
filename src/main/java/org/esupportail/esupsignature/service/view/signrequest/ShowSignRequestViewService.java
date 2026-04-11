package org.esupportail.esupsignature.service.view.signrequest;

import eu.europa.esig.dss.validation.reports.Reports;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.io.FileUtils;
import org.esupportail.esupsignature.config.certificat.SealCertificatProperties;
import org.esupportail.esupsignature.dto.json.RecipientWsDto;
import org.esupportail.esupsignature.dto.view.signrequest.CommentFrontDto;
import org.esupportail.esupsignature.dto.view.signrequest.FieldFrontDto;
import org.esupportail.esupsignature.dto.view.signrequest.ShowSignRequestBackDto;
import org.esupportail.esupsignature.dto.view.signrequest.SignRequestParamsFrontDto;
import org.esupportail.esupsignature.dto.view.signrequest.SignRequestUiCommonDto;
import org.esupportail.esupsignature.dto.view.signrequest.SignUiFrontDto;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignLevel;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.entity.enums.SignWith;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.esupportail.esupsignature.service.utils.sign.SignService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ShowSignRequestViewService {

    private final SignRequestService signRequestService;
    private final SignBookService signBookService;
    private final DataService dataService;
    private final UserService userService;
    private final SignWithService signWithService;
    private final CertificatService certificatService;
    private final AuditTrailService auditTrailService;
    private final LogService logService;
    private final PreAuthorizeService preAuthorizeService;
    private final SignService signService;

    public ShowSignRequestViewService(SignRequestService signRequestService,
                                      SignBookService signBookService,
                                      DataService dataService,
                                      UserService userService,
                                      SignWithService signWithService,
                                      CertificatService certificatService,
                                      AuditTrailService auditTrailService,
                                      LogService logService,
                                      PreAuthorizeService preAuthorizeService,
                                      SignService signService) {
        this.signRequestService = signRequestService;
        this.signBookService = signBookService;
        this.dataService = dataService;
        this.userService = userService;
        this.signWithService = signWithService;
        this.certificatService = certificatService;
        this.auditTrailService = auditTrailService;
        this.logService = logService;
        this.preAuthorizeService = preAuthorizeService;
        this.signService = signService;
    }

    public ShowSignRequestContext buildContext(Long id,
                                               String userEppn,
                                               String authUserEppn,
                                               HttpSession httpSession,
                                               boolean isOtpView) throws IOException {
        SignRequest signRequest = signRequestService.getById(id);
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

        if(currentStep != null) {
            currentStepMinSignLevel = currentStep.getMinSignLevel();
            currentStepMaxSignLevel = currentStep.getMaxSignLevel();
            currentStepMultiSign = currentStep.getMultiSign();
            currentStepSingleSignWithAnnotation = currentStep.getSingleSignWithAnnotation();
            stepRepeatable = currentStep.getRepeatable();
            if(currentStep.getWorkflowStep() != null) {
                currentStepId = currentStep.getWorkflowStep().getId();
            }
        }

        int nbSignRequestInSignBookParent = signBook.getSignRequests().size();
        boolean lastStep = !liveWorkflow.getLiveWorkflowSteps().isEmpty() && currentStepNumber >= liveWorkflow.getLiveWorkflowSteps().size();
        List<Document> toSignDocuments = signRequestService.getToSignDocuments(signRequest.getId());
        Document toSignDocument = toSignDocuments.size() == 1 ? toSignDocuments.get(0) : null;
        boolean pdf = toSignDocument != null && "application/pdf".equals(toSignDocument.getContentType());
        if(toSignDocuments.stream().anyMatch(document -> !document.isPdf()) && currentStepMinSignLevel.getValue() < 3) {
            currentStepMinSignLevel = SignLevel.advanced;
        }

        Reports reports = signService.validate(id);
        List<String> signatureIds = new ArrayList<>();
        boolean signatureIssue = false;
        if(reports != null) {
            signatureIds = reports.getSimpleReport().getSignatureIdList();
            for (String signatureId : signatureIds) {
                if (!reports.getSimpleReport().isValid(signatureId)) {
                    signatureIssue = true;
                    break;
                }
            }
            if(!signatureIds.isEmpty() && currentStepMinSignLevel.getValue() < 3) {
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
            signImages = getSignImagesForRequest(id, userEppn, authUserEppn, httpSession);
        } catch (EsupSignatureUserException e) {
            signImagesWarningMessage = e.getMessage();
        }

        User frontUser = userService.getFullUserByEppn(userEppn);
        User frontAuthUser = userService.getByEppn(authUserEppn);
        String action = null;
        Set<String> supervisors = null;
        if(signRequest.getData() != null && signRequest.getData().getForm() != null && signRequest.getData().getForm().getWorkflow() != null) {
            action = signRequest.getData().getForm().getAction();
            supervisors = signRequest.getData().getForm().getWorkflow().getManagers();
        }

        boolean notSigned = !signRequestService.isSigned(signRequest, reports);

        return new ShowSignRequestContext(
                userEppn,
                authUserEppn,
                isOtpView,
                signRequest,
                signBook,
                liveWorkflow,
                currentStep,
                workflow,
                signable,
                editable,
                manager,
                attachmentAlert,
                attachmentRequire,
                currentSignType,
                currentStepNumber,
                currentStepId,
                currentStepMultiSign,
                currentStepSingleSignWithAnnotation,
                currentStepMinSignLevel,
                currentStepMaxSignLevel,
                stepRepeatable,
                nbSignRequestInSignBookParent,
                lastStep,
                toSignDocuments,
                toSignDocument,
                pdf,
                reports,
                signatureIds,
                signatureIssue,
                fields,
                currentSignRequestParamses,
                comments,
                spots,
                signImages,
                signImagesWarningMessage,
                frontUser,
                frontAuthUser,
                action,
                supervisors,
                notSigned
        );
    }

    public ShowSignRequestBackDto buildBackDto(ShowSignRequestContext context,
                                               Boolean frameMode,
                                               String annotation) {
        SignRequestUiCommonDto common = buildCommonDto(context);
        SignRequest signRequest = context.signRequest();
        SignBook signBook = context.signBook();
        String userEppn = context.userEppn();
        String authUserEppn = context.authUserEppn();

        boolean displayNotif = !context.isOtpView() && signRequestService.isDisplayNotif(signRequest, userEppn);
        boolean tempUsers = !context.isOtpView() && signBookService.isTempUsers(signBook.getId());
        List<Comment> postits = signRequestService.getPostits(signRequest.getId());
        List<Document> attachments = signRequestService.getAttachments(signRequest.getId());
        SignBook nextSignBook = signBookService.getNextSignBook(signRequest.getId(), userEppn, authUserEppn);
        SignRequest nextSignRequest = signBookService.getNextSignRequest(signRequest.getId(), nextSignBook);
        List<SignWith> signWiths = new ArrayList<>();
        if(context.reports() != null) {
            signWiths = signWithService.getAuthorizedSignWiths(userEppn, signRequest, !context.signatureIds().isEmpty());
        } else if(context.signable()) {
            signWiths = signWithService.getAuthorizedSignWiths(userEppn, signRequest, false);
        }

        AuditTrail auditTrail = null;
        String size = null;
        if(!signRequest.getStatus().equals(SignRequestStatus.draft)
                && !signRequest.getStatus().equals(SignRequestStatus.pending)
                && !signRequest.getStatus().equals(SignRequestStatus.refused)
                && !signRequest.getDeleted()) {
            auditTrail = auditTrailService.getAuditTrailByToken(signRequest.getToken());
            if(auditTrail != null && auditTrail.getDocumentSize() != null) {
                size = FileUtils.byteCountToDisplaySize(auditTrail.getDocumentSize());
            }
        }

        boolean sealCertOK = signWithService.checkSealCertificat(userEppn, true);
        List<SealCertificatProperties> sealCertificatPropertieses = certificatService.getCheckedSealCertificates();
        SignWith[] allSignWiths = SignWith.values();
        List<Certificat> certificats = certificatService.getCertificatByUser(userEppn);
        List<LiveWorkflowStep> steps = signRequest.getStatus().equals(SignRequestStatus.draft)
                ? context.liveWorkflow().getLiveWorkflowSteps()
                : new ArrayList<>();
        List<Log> refuseLogs = logService.getRefuseLogs(signRequest.getId());
        boolean viewRight = preAuthorizeService.checkUserViewRights(signRequest, userEppn, authUserEppn);
        Data data = dataService.getBySignBook(signBook);
        Form form = data != null ? data.getForm() : null;
        List<Log> logs = logService.getFullBySignRequest(signRequest.getId());
        String pdfaCheck = !context.toSignDocuments().isEmpty() ? context.toSignDocuments().get(0).getPdfaCheck() : null;
        boolean auditTrailChecked = signBook.getStatus().equals(SignRequestStatus.completed) || signBook.getStatus().equals(SignRequestStatus.exported);
        List<RecipientWsDto> externalsRecipients = auditTrailChecked
                ? signRequestService.getExternalRecipients(signRequest.getId())
                : new ArrayList<>();

        return new ShowSignRequestBackDto(
                signRequest,
                signBook,
                context.workflow(),
                common.signRequestId(),
                signBook.getId(),
                common.dataId(),
                common.formId(),
                context.isOtpView() ? "otp" : "user",
                displayNotif,
                tempUsers,
                common.signable(),
                common.editable(),
                common.manager(),
                common.status(),
                common.currentSignType(),
                common.currentStepNumber(),
                context.currentStepId(),
                common.currentStepMultiSign(),
                common.currentStepSingleSignWithAnnotation(),
                common.currentStepMinSignLevel(),
                context.currentStepMaxSignLevel(),
                common.stepRepeatable(),
                context.lastStep(),
                common.pdf(),
                common.attachmentAlert(),
                common.attachmentRequire(),
                common.notSigned(),
                signRequestService.isCurrentUserAsSigned(signRequest, userEppn),
                context.signatureIds(),
                context.signatureIssue(),
                common.nbSignRequests(),
                common.action(),
                context.supervisors(),
                context.toSignDocument(),
                postits,
                common.comments(),
                common.spots(),
                attachments,
                nextSignBook,
                nextSignRequest,
                common.fields(),
                common.signRequestParams(),
                common.signImages(),
                signWiths,
                auditTrail,
                size,
                sealCertOK,
                sealCertificatPropertieses,
                allSignWiths,
                certificats,
                annotation,
                steps,
                refuseLogs,
                viewRight,
                frameMode,
                form,
                logs,
                pdfaCheck,
                auditTrailChecked,
                externalsRecipients
        );
    }

    public SignUiFrontDto buildFrontDto(ShowSignRequestContext context) {
        SignRequestUiCommonDto common = buildCommonDto(context);
        User frontUser = context.frontUser();
        User frontAuthUser = context.frontAuthUser();

        return new SignUiFrontDto(
                common.signRequestId(),
                common.dataId(),
                common.formId(),
                toSignRequestParamsFrontDtos(common.signRequestParams()),
                frontUser != null ? frontUser.getDefaultSignImageNumber() : null,
                common.currentSignType(),
                common.signable(),
                common.editable(),
                toCommentFrontDtos(common.comments()),
                toSignRequestParamsFrontDtos(common.spots()),
                common.pdf(),
                common.currentStepNumber(),
                common.currentStepMultiSign(),
                common.currentStepSingleSignWithAnnotation(),
                common.currentStepMinSignLevel(),
                context.workflow() != null,
                common.signImages(),
                toDisplayName(frontUser),
                toDisplayName(frontAuthUser),
                toFieldFrontDtos(common.fields(), context.workflow()),
                common.stepRepeatable(),
                common.status(),
                common.action(),
                common.nbSignRequests(),
                common.notSigned(),
                common.attachmentAlert(),
                common.attachmentRequire(),
                context.isOtpView(),
                frontUser == null || frontUser.getFavoriteSignRequestParams() == null,
                frontUser != null ? frontUser.getPhone() : null,
                frontUser != null ? frontUser.getReturnToHomeAfterSign() : null,
                common.manager()
        );
    }

    private List<CommentFrontDto> toCommentFrontDtos(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) {
            return List.of();
        }
        return comments.stream()
                .map(comment -> new CommentFrontDto(
                        comment.getId(),
                        comment.getPageNumber(),
                        comment.getStepNumber(),
                        comment.getPosX(),
                        comment.getPosY()
                ))
                .toList();
    }

    private List<SignRequestParamsFrontDto> toSignRequestParamsFrontDtos(List<SignRequestParams> signRequestParamses) {
        if (signRequestParamses == null || signRequestParamses.isEmpty()) {
            return List.of();
        }
        return signRequestParamses.stream()
                .map(signRequestParams -> new SignRequestParamsFrontDto(
                        signRequestParams.getId(),
                        signRequestParams.getPdSignatureFieldName(),
                        signRequestParams.getStepNumber(),
                        signRequestParams.getSignImageNumber(),
                        signRequestParams.getSignPageNumber(),
                        signRequestParams.getSignDocumentNumber(),
                        signRequestParams.getSignWidth(),
                        signRequestParams.getSignHeight(),
                        signRequestParams.getxPos(),
                        signRequestParams.getyPos(),
                        signRequestParams.getExtraText(),
                        signRequestParams.getIsExtraText(),
                        signRequestParams.getAddWatermark(),
                        signRequestParams.getAllPages(),
                        signRequestParams.getAddImage(),
                        signRequestParams.getAddExtra(),
                        signRequestParams.getExtraType(),
                        signRequestParams.getExtraName(),
                        signRequestParams.getExtraDate(),
                        signRequestParams.getExtraOnTop(),
                        signRequestParams.getTextPart(),
                        signRequestParams.getSignScale(),
                        signRequestParams.getRed(),
                        signRequestParams.getGreen(),
                        signRequestParams.getBlue(),
                        signRequestParams.getFontSize()
                ))
                .toList();
    }

    private List<FieldFrontDto> toFieldFrontDtos(List<Field> fields, Workflow workflow) {
        if (fields == null || fields.isEmpty()) {
            return List.of();
        }
        return fields.stream()
                .map(field -> new FieldFrontDto(
                        field.getId(),
                        field.getName(),
                        field.getDescription(),
                        field.getPage(),
                        field.getRequired(),
                        field.getReadOnly(),
                        field.getEditable(),
                        toWorkflowStepNumbers(field, workflow),
                        field.getDefaultValue(),
                        field.getSearchServiceName(),
                        field.getSearchType(),
                        field.getSearchReturn(),
                        field.getType() != null ? field.getType().name().toLowerCase() : null,
                        field.getFavorisable()
                ))
                .toList();
    }

    private List<Integer> toWorkflowStepNumbers(Field field, Workflow workflow) {
        if (field == null || workflow == null || field.getWorkflowSteps() == null || field.getWorkflowSteps().isEmpty()) {
            return List.of();
        }
        List<WorkflowStep> workflowSteps = workflow.getWorkflowSteps();
        if (workflowSteps == null || workflowSteps.isEmpty()) {
            return List.of();
        }

        List<Integer> stepNumbers = new ArrayList<>();
        for (WorkflowStep fieldWorkflowStep : field.getWorkflowSteps()) {
            Long workflowStepId = fieldWorkflowStep != null ? fieldWorkflowStep.getId() : null;
            for (int i = 0; i < workflowSteps.size(); i++) {
                WorkflowStep workflowStep = workflowSteps.get(i);
                if (workflowStepId != null && workflowStep != null && workflowStepId.equals(workflowStep.getId())) {
                    stepNumbers.add(i + 1);
                    break;
                }
            }
        }
        return stepNumbers;
    }

    private SignRequestUiCommonDto buildCommonDto(ShowSignRequestContext context) {
        SignRequest signRequest = context.signRequest();
        return new SignRequestUiCommonDto(
                signRequest.getId(),
                signRequest.getData() != null ? signRequest.getData().getId() : null,
                signRequest.getData() != null && signRequest.getData().getForm() != null ? signRequest.getData().getForm().getId() : null,
                context.currentSignRequestParamses(),
                context.currentSignType(),
                context.signable(),
                context.editable(),
                context.comments(),
                context.spots(),
                context.pdf(),
                context.currentStepNumber(),
                context.currentStepMultiSign(),
                context.currentStepSingleSignWithAnnotation(),
                context.currentStepMinSignLevel(),
                context.signImages(),
                context.fields(),
                context.stepRepeatable(),
                signRequest.getStatus(),
                context.action(),
                context.nbSignRequestInSignBookParent(),
                context.notSigned(),
                context.attachmentAlert(),
                context.attachmentRequire(),
                context.manager()
        );
    }

    private String toDisplayName(User user) {
        if(user == null) {
            return null;
        }
        return user.getFirstname() + " " + user.getName();
    }

    private List<String> getSignImagesForRequest(Long signRequestId, String userEppn, String authUserEppn, HttpSession httpSession) throws IOException, EsupSignatureUserException {
        Long userShareId = getUserShareId(httpSession);
        return signBookService.getSignImagesForSignRequest(signRequestId, userEppn, authUserEppn, userShareId);
    }

    private Long getUserShareId(HttpSession httpSession) {
        Object userShareString = httpSession.getAttribute("userShareId");
        if(userShareString == null) {
            return null;
        }
        return Long.valueOf(userShareString.toString());
    }

    public record ShowSignRequestContext(
            String userEppn,
            String authUserEppn,
            boolean isOtpView,
            SignRequest signRequest,
            SignBook signBook,
            LiveWorkflow liveWorkflow,
            LiveWorkflowStep currentStep,
            Workflow workflow,
            boolean signable,
            boolean editable,
            boolean manager,
            boolean attachmentAlert,
            boolean attachmentRequire,
            SignType currentSignType,
            Integer currentStepNumber,
            Long currentStepId,
            boolean currentStepMultiSign,
            boolean currentStepSingleSignWithAnnotation,
            SignLevel currentStepMinSignLevel,
            SignLevel currentStepMaxSignLevel,
            Boolean stepRepeatable,
            int nbSignRequestInSignBookParent,
            boolean lastStep,
            List<Document> toSignDocuments,
            Document toSignDocument,
            boolean pdf,
            Reports reports,
            List<String> signatureIds,
            boolean signatureIssue,
            List<Field> fields,
            List<SignRequestParams> currentSignRequestParamses,
            List<Comment> comments,
            List<SignRequestParams> spots,
            List<String> signImages,
            String signImagesWarningMessage,
            User frontUser,
            User frontAuthUser,
            String action,
            Set<String> supervisors,
            boolean notSigned
    ) {}
}




