package org.esupportail.esupsignature.web.controller;

import eu.europa.esig.dss.validation.reports.Reports;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.io.FileUtils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dto.js.JsMessage;
import org.esupportail.esupsignature.dto.json.RecipientWsDto;
import org.esupportail.esupsignature.dto.json.WorkflowStepDto;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.*;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.esupportail.esupsignature.service.utils.sign.SignService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequestMapping(path = {"/user/signrequests", "/otp/signrequests"})
@EnableConfigurationProperties(GlobalProperties.class)
public class UserAndOtpSignRequestController {

    private static final Logger logger = LoggerFactory.getLogger(UserAndOtpSignRequestController.class);

    private final SignRequestService signRequestService;
    private final CommentService commentService;
    private final SignWithService signWithService;
    private final DataService dataService;
    private final UserService userService;
    private final CertificatService certificatService;
    private final PreAuthorizeService preAuthorizeService;
    private final GlobalProperties globalProperties;
    private final SignBookService signBookService;
    private final LogService logService;
    private final AuditTrailService auditTrailService;
    private final SignService signService;

    private final Map<String, Object> userLocks = new ConcurrentHashMap<>();
    private Object getLock(String authUserEppn) {
        return userLocks.computeIfAbsent(authUserEppn, k -> new Object());
    }

    public UserAndOtpSignRequestController(SignRequestService signRequestService, CommentService commentService, SignWithService signWithService, DataService dataService, UserService userService, CertificatService certificatService, PreAuthorizeService preAuthorizeService, GlobalProperties globalProperties, SignBookService signBookService, LogService logService, AuditTrailService auditTrailService, SignService signService) {
        this.signRequestService = signRequestService;
        this.commentService = commentService;
        this.signWithService = signWithService;
        this.dataService = dataService;
        this.userService = userService;
        this.certificatService = certificatService;
        this.preAuthorizeService = preAuthorizeService;
        this.globalProperties = globalProperties;
        this.signBookService = signBookService;
        this.logService = logService;
        this.auditTrailService = auditTrailService;
        this.signService = signService;
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @GetMapping(value = "/{id}")
    public String show(@ModelAttribute("userEppn") String userEppn,
                       @ModelAttribute("authUserEppn") String authUserEppn,
                       @PathVariable("id") Long id,
                       @RequestParam(required = false) Boolean frameMode,
                       @RequestParam(required = false) String annotation,
                       Model model, HttpSession httpSession, HttpServletRequest httpServletRequest) throws IOException, EsupSignatureRuntimeException {
        String path = httpServletRequest.getRequestURI();
        boolean isOtpView = path.startsWith("/otp");
        String urlProfil = isOtpView ? "otp" : "user";
        SignRequest signRequest = signRequestService.getById(id);
        SignBook signBook = signRequest.getParentSignBook();
        LiveWorkflow liveWorkflow = signBook.getLiveWorkflow();
        LiveWorkflowStep currentStep = liveWorkflow.getCurrentStep();
        Workflow workflow = liveWorkflow.getWorkflow();

        boolean displayNotif = !isOtpView && signRequestService.isDisplayNotif(signRequest, userEppn);
        boolean isTempUsers = !isOtpView && signBookService.isTempUsers(signBook.getId());

        boolean signable = signBookService.checkSignRequestSignable(id, userEppn, authUserEppn);
        boolean isManager = signBookService.checkUserManageRights(signBook.getId(), userEppn);
        List<Comment> postits = signRequestService.getPostits(id);
        List<Comment> comments = signRequestService.getComments(id);
        List<SignRequestParams> spots = signRequestService.getSpots(id);
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
        boolean isLastStep = !liveWorkflow.getLiveWorkflowSteps().isEmpty() && currentStepNumber >= liveWorkflow.getLiveWorkflowSteps().size();
        List<Document> toSignDocuments = signRequestService.getToSignDocuments(signRequest.getId());
        Document toSignDocument = toSignDocuments.size() == 1 ? toSignDocuments.get(0) : null;
        boolean isPdf = toSignDocument != null && "application/pdf".equals(toSignDocument.getContentType());
        if(toSignDocuments.stream().anyMatch(d -> !d.isPdf()) && currentStepMinSignLevel.getValue() < 3) {
            currentStepMinSignLevel = SignLevel.advanced;
        }

        List<Document> attachments = signRequestService.getAttachments(id);
        SignBook nextSignBook = signBookService.getNextSignBook(signRequest.getId(), userEppn, authUserEppn);
        List<Field> fields = signRequestService.prefillSignRequestFields(id, userEppn);
        List<SignRequestParams> toUseSignRequestParams = signRequestService.getToUseSignRequestParams(id, userEppn);
        String favoriteSignRequestParamsJson = userService.getFavoriteSignRequestParamsJson(userEppn);
        List<String> signImages = new ArrayList<>();
        try {
            Object userShareString = httpSession.getAttribute("userShareId");
            Long userShareId = null;
            if(userShareString != null) {
                userShareId = Long.valueOf(userShareString.toString());
            }
            signImages = signBookService.getSignImagesForSignRequest(id, userEppn, authUserEppn, userShareId);
        } catch (EsupSignatureUserException e) {
            model.addAttribute("message", new JsMessage("warn", e.getMessage()));
        }

        List<String> signatureIds = new ArrayList<>();
        boolean signatureIssue = false;
        List<SignWith> signWiths = new ArrayList<>();
        Reports reports = signService.validate(id);
        if(reports != null) {
            signatureIds = reports.getSimpleReport().getSignatureIdList();
            signWiths = signWithService.getAuthorizedSignWiths(userEppn, signRequest, !signatureIds.isEmpty());
            for (String signatureId : signatureIds) {
                if (!reports.getSimpleReport().isValid(signatureId)) {
                    signatureIssue = true;
                    break;
                }
            }
            if(!signatureIds.isEmpty() && currentStepMinSignLevel.getValue() < 3) {
                currentStepMinSignLevel = SignLevel.advanced;
            }
        } else if(signable) {
            signWiths = signWithService.getAuthorizedSignWiths(userEppn, signRequest, false);
        }

        boolean editable = signRequestService.isEditable(id, userEppn);
        boolean isNotSigned = !signRequestService.isSigned(signRequest, reports);
        boolean isCurrentUserAsSigned = signRequestService.isCurrentUserAsSigned(signRequest, userEppn);
        List<LiveWorkflowStep> steps = signRequest.getStatus().equals(SignRequestStatus.draft) ? liveWorkflow.getLiveWorkflowSteps() : new ArrayList<>();
        List<Log> refuseLogs = logService.getRefuseLogs(signRequest.getId());
        boolean viewRight = preAuthorizeService.checkUserViewRights(signRequest, userEppn, authUserEppn);
        String action = null;
        Set<String> supervisors = null;
        if(signRequest.getData() != null && signRequest.getData().getForm() != null && signRequest.getData().getForm().getWorkflow() != null) {
            action = signRequest.getData().getForm().getAction();
            supervisors = signRequest.getData().getForm().getWorkflow().getManagers();
        }

        User frontUser = userService.getFullUserByEppn(userEppn);
        User frontAuthUser = userService.getByEppn(authUserEppn);

        SignRequest nextSignRequest = signBookService.getNextSignRequest(signRequest.getId(), nextSignBook);
        AuditTrail auditTrail = null;
        String size = null;
        if(!signRequest.getStatus().equals(SignRequestStatus.draft) && !signRequest.getStatus().equals(SignRequestStatus.pending) && !signRequest.getStatus().equals(SignRequestStatus.refused) && !signRequest.getDeleted()) {
            auditTrail = auditTrailService.getAuditTrailByToken(signRequest.getToken());
            if(auditTrail != null) {
                if(auditTrail.getDocumentSize() != null) {
                    size = FileUtils.byteCountToDisplaySize(auditTrail.getDocumentSize());
                }
            }
        }

        if(annotation != null && !editable) {
            return "redirect:/user/signrequests/" + id;
        }

        if(signable
                && workflow != null && userService.getUiParams(authUserEppn) != null
                && (userService.getUiParams(authUserEppn).get(UiParams.workflowVisaAlert) == null || !Arrays.asList(userService.getUiParams(authUserEppn).get(UiParams.workflowVisaAlert).split(",")).contains(workflow.getId().toString()))
                && currentStep != null && currentStep.getSignType().equals(SignType.hiddenVisa)) {
            model.addAttribute("message", new JsMessage("custom", "Vous êtes destinataire d'une demande de visa (et non de signature) sur ce document.\nSa validation implique que vous en acceptez le contenu.\nVous avez toujours la possibilité de ne pas donner votre accord en refusant cette demande de visa et en y adjoignant vos commentaires."));
            userService.setUiParams(authUserEppn, UiParams.workflowVisaAlert, workflow.getId().toString() + ",");

        }
        Data data = dataService.getBySignBook(signRequest.getParentSignBook());
        Form form = data != null ? data.getForm() : null;
        List<Log> logs = logService.getFullBySignRequest(signRequest.getId());
        String pdfaCheck = !toSignDocuments.isEmpty() ? toSignDocuments.get(0).getPdfaCheck() : null;
        boolean auditTrailChecked = signRequest.getParentSignBook().getStatus().equals(SignRequestStatus.completed) || signRequest.getParentSignBook().getStatus().equals(SignRequestStatus.exported);
        List<RecipientWsDto> externalsRecipients = auditTrailChecked ? signRequestService.getExternalRecipients(signRequest.getId()) : new ArrayList<>();
        boolean sealCertOK = signWithService.checkSealCertificat(userEppn, true);
        SignWith[] allSignWiths = SignWith.values();
        List<Certificat> certificats = certificatService.getCertificatByUser(userEppn);

        model.addAttribute("favoriteSignRequestParamsJson", favoriteSignRequestParamsJson);

        ShowSignRequestDataFlowDto showDataFlow = new ShowSignRequestDataFlowDto(
                new ShowSignRequestBackDto(
                        signRequest,
                        signBook,
                        workflow,
                        signRequest.getId(),
                        signBook.getId(),
                        signRequest.getData() != null ? signRequest.getData().getId() : null,
                        signRequest.getData() != null && signRequest.getData().getForm() != null ? signRequest.getData().getForm().getId() : null,
                        urlProfil,
                        displayNotif,
                        isTempUsers,
                        signable,
                        editable,
                        isManager,
                        signRequest.getStatus(),
                        currentSignType,
                        currentStepNumber,
                        currentStepId,
                        currentStepMultiSign,
                        currentStepSingleSignWithAnnotation,
                        currentStepMinSignLevel,
                        currentStepMaxSignLevel,
                        stepRepeatable,
                        isLastStep,
                        isPdf,
                        attachmentAlert,
                        attachmentRequire,
                        isNotSigned,
                        isCurrentUserAsSigned,
                        signatureIds,
                        signatureIssue,
                        nbSignRequestInSignBookParent,
                        action,
                        supervisors,
                        toSignDocument,
                        postits,
                        comments,
                        spots,
                        attachments,
                        nextSignBook,
                        nextSignRequest,
                        fields,
                        toUseSignRequestParams,
                        signImages,
                        signWiths,
                        auditTrail,
                        size,
                        sealCertOK,
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
                ),
                new ShowSignRequestFrontDto(
                        toFrontUserDto(frontUser),
                        toFrontUserDto(frontAuthUser),
                        toFrontUserDto(signRequest.getCreateBy()),
                        currentStepNumber,
                        supervisors,
                        isLastStep,
                        new SignUiFrontDto(
                                signRequest.getId(),
                                signRequest.getData() != null ? signRequest.getData().getId() : null,
                                signRequest.getData() != null && signRequest.getData().getForm() != null ? signRequest.getData().getForm().getId() : null,
                                toUseSignRequestParams,
                                frontUser != null ? frontUser.getDefaultSignImageNumber() : null,
                                currentSignType,
                                signable,
                                editable,
                                comments,
                                spots,
                                isPdf,
                                currentStepNumber,
                                currentStepMultiSign,
                                currentStepSingleSignWithAnnotation,
                                currentStepMinSignLevel,
                                workflow != null,
                                signImages,
                                toDisplayName(frontUser),
                                toDisplayName(frontAuthUser),
                                fields,
                                stepRepeatable,
                                signRequest.getStatus(),
                                action,
                                nbSignRequestInSignBookParent,
                                isNotSigned,
                                attachmentAlert,
                                attachmentRequire,
                                isOtpView,
                                frontUser == null || frontUser.getFavoriteSignRequestParams() == null,
                                frontUser != null ? frontUser.getPhone() : null,
                                frontUser != null ? frontUser.getReturnToHomeAfterSign() : null,
                                isManager
                        )
                )
        );
        model.addAttribute("signRequestShowDataFlow", showDataFlow);

        return "user/signrequests/show";
    }

    private FrontUserDto toFrontUserDto(User user) {
        if(user == null) {
            return null;
        }
        return new FrontUserDto(
                user.getId(),
                user.getEppn(),
                user.getName(),
                user.getFirstname(),
                user.getEmail(),
                user.getDefaultSignImageNumber(),
                user.getPhone(),
                user.getReturnToHomeAfterSign()
        );
    }

    private String toDisplayName(User user) {
        if(user == null) {
            return null;
        }
        return user.getFirstname() + " " + user.getName();
    }

    public record ShowSignRequestDataFlowDto(ShowSignRequestBackDto back, ShowSignRequestFrontDto front) {}

    public record ShowSignRequestBackDto(
            SignRequest signRequest,
            SignBook signBook,
            Workflow workflow,
            Long signRequestId,
            Long signBookId,
            Long dataId,
            Long formId,
            String urlProfil,
            Boolean displayNotif,
            Boolean tempUsers,
            Boolean signable,
            Boolean editable,
            Boolean manager,
            SignRequestStatus status,
            SignType currentSignType,
            Integer currentStepNumber,
            Long currentStepId,
            Boolean currentStepMultiSign,
            Boolean currentStepSingleSignWithAnnotation,
            SignLevel currentStepMinSignLevel,
            SignLevel currentStepMaxSignLevel,
            Boolean stepRepeatable,
            Boolean lastStep,
            Boolean pdf,
            Boolean attachmentAlert,
            Boolean attachmentRequire,
            Boolean notSigned,
            Boolean currentUserAsSigned,
            List<String> signatureIds,
            Boolean signatureIssue,
            Integer nbSignRequests,
            String action,
            Set<String> supervisors,
            Document toSignDocument,
            List<Comment> postits,
            List<Comment> comments,
            List<SignRequestParams> spots,
            List<Document> attachments,
            SignBook nextSignBook,
            SignRequest nextSignRequest,
            List<Field> fields,
            List<SignRequestParams> signRequestParams,
            List<String> signImages,
            List<SignWith> signWiths,
            AuditTrail auditTrail,
            String size,
            Boolean sealCertOK,
            SignWith[] allSignWiths,
            List<Certificat> certificats,
            String annotation,
            List<LiveWorkflowStep> steps,
            List<Log> refuseLogs,
            Boolean viewRight,
            Boolean frameMode,
            Form form,
            List<Log> logs,
            String pdfaCheck,
            Boolean auditTrailChecked,
            List<RecipientWsDto> externalsRecipients
    ) {}

    public record ShowSignRequestFrontDto(
            FrontUserDto user,
            FrontUserDto authUser,
            FrontUserDto creator,
            Integer currentStepNumber,
            Set<String> supervisors,
            Boolean lastStep,
            SignUiFrontDto signUi
    ) {}

    public record SignUiFrontDto(
            Long signRequestId,
            Long dataId,
            Long formId,
            List<SignRequestParams> currentSignRequestParamses,
            Integer signImageNumber,
            SignType currentSignType,
            Boolean signable,
            Boolean editable,
            List<Comment> comments,
            List<SignRequestParams> spots,
            Boolean pdf,
            Integer currentStepNumber,
            Boolean currentStepMultiSign,
            Boolean currentStepSingleSignWithAnnotation,
            SignLevel currentStepMinSignLevel,
            Boolean workflowAvailable,
            List<String> signImages,
            String userName,
            String authUserName,
            List<Field> fields,
            Boolean stepRepeatable,
            SignRequestStatus status,
            String action,
            Integer nbSignRequests,
            Boolean notSigned,
            Boolean attachmentAlert,
            Boolean attachmentRequire,
            Boolean otp,
            Boolean restore,
            String phone,
            Boolean returnToHomeAfterSign,
            Boolean manager
    ) {}

    public record FrontUserDto(
            Long id,
            String eppn,
            String name,
            String firstname,
            String email,
            Integer defaultSignImageNumber,
            String phone,
            Boolean returnToHomeAfterSign
    ) {}

    @PreAuthorize("@preAuthorizeService.signRequestRecipientAndViewers(#id, #userEppn)")
    @PostMapping(value = "/postit/{id}")
    public String postit(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                         @RequestParam(value = "comment", required = false) String comment,
                         @RequestParam(value = "postit", required = false) String postit,
                         @RequestParam(value = "forceSend", required = false, defaultValue = "false") Boolean forceSend, Model model, HttpServletRequest httpServletRequest) {
        Long commentId = null;
        try {
            commentId = signRequestService.addComment(id, comment, null, null, null, null, null, postit, null, authUserEppn, userEppn, forceSend);
        } catch (EsupSignatureException e) {
            model.addAttribute("message", new JsMessage("error", "Problème lors de l'ajout du post-it"));
        }
        if(commentId != null) {
            model.addAttribute("message", new JsMessage("success", "Post-it ajouté"));
        } else {
            model.addAttribute("message", new JsMessage("error", "Problème lors de l'ajout du post-it"));
        }
        String path = httpServletRequest.getRequestURI();
        String basePath = path.startsWith("/otp") ? "/otp/signrequests/" : "/user/signrequests/";
        return "redirect:" + basePath + id;
    }

    @PreAuthorize("@preAuthorizeService.commentCreator(#postitId, #userEppn)")
    @PutMapping(value = "/comment/{signRequestId}/update/{postitId}")
    public String commentUpdate(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
                                @PathVariable("signRequestId") Long signRequestId,
                                @PathVariable("postitId") Long postitId,
                                @RequestParam(value = "comment", required = false) String comment, RedirectAttributes redirectAttributes, HttpServletRequest httpServletRequest) {
        try {
            commentService.updateComment(signRequestId, postitId, comment);
            redirectAttributes.addFlashAttribute("message", new JsMessage("success", "Postit modifiée"));

        } catch (EsupSignatureException e) {
            redirectAttributes.addFlashAttribute("message", new JsMessage("error", e.getMessage()));
        }
        String path = httpServletRequest.getRequestURI();
        String basePath = path.startsWith("/otp") ? "/otp/signrequests/" : "/user/signrequests/";
        return "redirect:" + basePath + signRequestId;
    }

    @PreAuthorize("@preAuthorizeService.commentCreator(#postitId, #userEppn)")
    @DeleteMapping(value = "/comment/{signRequestId}/delete/{postitId}")
    public String commentDelete(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
                                @PathVariable("signRequestId") Long signRequestId,
                                @PathVariable("postitId") Long postitId, RedirectAttributes redirectAttributes, HttpServletRequest httpServletRequest) {
        try {
            commentService.deletePostit(signRequestId, postitId);
            redirectAttributes.addFlashAttribute("message", new JsMessage("success", "Postit supprimé"));

        } catch (EsupSignatureException e) {
            redirectAttributes.addFlashAttribute("message", new JsMessage("error", e.getMessage()));
        }
        String path = httpServletRequest.getRequestURI();
        String basePath = path.startsWith("/otp") ? "/otp/signrequests/" : "/user/signrequests/";
        return "redirect:" + basePath + signRequestId;
    }

    @PreAuthorize("@preAuthorizeService.signRequestRecipient(#id, #authUserEppn)")
    @PostMapping(value = "/add-attachment/{id}")
    public String addAttachement(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                                 @RequestParam(value = "multipartFiles", required = false) MultipartFile[] multipartFiles,
                                 @RequestParam(value = "link", required = false) String link,
                                 RedirectAttributes redirectAttributes, HttpServletRequest httpServletRequest) throws EsupSignatureIOException {
        logger.info("start add attachment");
        try {
            if(StringUtils.hasText(link)) {
                new URI(link);
            }
            if(signRequestService.addAttachement(multipartFiles, link, id, authUserEppn)) {
                redirectAttributes.addFlashAttribute("message", new JsMessage("info", "La piece jointe a bien été ajoutée"));
            } else {
                redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Aucune pièce jointe n'a été ajoutée. Merci de contrôle la validité du document"));
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Le lien fourni n'est pas valide"));
        }
        String path = httpServletRequest.getRequestURI();
        String basePath = path.startsWith("/otp") ? "/otp/signrequests/" : "/user/signrequests/";
        return "redirect:" + basePath + id + "?attachment=true";
    }

    @PreAuthorize("@preAuthorizeService.attachmentCreator(#attachementId, #userEppn, #authUserEppn)")
    @DeleteMapping(value = "/remove-attachment/{id}/{attachementId}")
    public String removeAttachement(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @PathVariable("attachementId") Long attachementId, RedirectAttributes redirectAttributes, HttpServletRequest httpServletRequest) {
        logger.info("start remove attachment");
        signRequestService.removeAttachement(id, attachementId, redirectAttributes);
        redirectAttributes.addFlashAttribute("message", new JsMessage("info", "La pieces jointe a été supprimée"));
        String path = httpServletRequest.getRequestURI();
        String basePath = path.startsWith("/otp") ? "/otp/signrequests/" : "/user/signrequests/";
        return "redirect:" + basePath + id + "?attachment=true";
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @DeleteMapping(value = "/remove-link/{id}/{linkId}")
    public String removeLink(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @PathVariable("linkId") Integer linkId, RedirectAttributes redirectAttributes, HttpServletRequest httpServletRequest) {
        logger.info("start remove link");
        signRequestService.removeLink(id, linkId);
        redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Le lien a été supprimé"));
        String path = httpServletRequest.getRequestURI();
        String basePath = path.startsWith("/otp") ? "/otp/signrequests/" : "/user/signrequests/";
        return "redirect:" + basePath + id + "?attachment=true";
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @GetMapping(value = "/get-attachment/{id}/{attachementId}")
    public void getAttachment(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @PathVariable("attachementId") Long attachementId, HttpServletResponse httpServletResponse, RedirectAttributes redirectAttributes, HttpServletRequest httpServletRequest) {
        synchronized (getLock(authUserEppn)) {
            try {
                if (!signRequestService.getAttachmentResponse(id, attachementId, httpServletResponse)) {
                    redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Pièce jointe non trouvée ..."));
                    httpServletResponse.sendRedirect("/user/signsignrequests/" + id);
                }
            } catch (Exception e) {
                logger.error("get file error", e);
            }
        }
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @GetMapping(value = "/get-attachment-inline/{id}/{attachementId}")
    public void getAttachmentInline(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @PathVariable("attachementId") Long attachementId, HttpServletResponse httpServletResponse, RedirectAttributes redirectAttributes, HttpServletRequest httpServletRequest) {
        synchronized (getLock(authUserEppn)) {
            try {
                logger.info("get file attachment");
                if (!signRequestService.getAttachmentInlineResponse(id, attachementId, httpServletResponse)) {
                    redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Pièce jointe non trouvée ..."));
                    httpServletResponse.sendRedirect("/user/signsignrequests/" + id);
                }
            } catch (Exception e) {
                logger.error("get file error", e);
            }
        }
    }

    @PreAuthorize("@preAuthorizeService.signRequestRecipientAndViewers(#id, #userEppn)")
    @PostMapping(value = "/comment/{id}")
    @ResponseBody
    public ResponseEntity<String> comment(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                                        @RequestParam(value = "comment", required = false) String comment,
                                        @RequestParam(value = "spotStepNumber", required = false) Integer spotStepNumber,
                                        @RequestParam(value = "commentPageNumber", required = false) Integer commentPageNumber,
                                        @RequestParam(value = "commentPosX", required = false) Integer commentPosX,
                                        @RequestParam(value = "commentPosY", required = false) Integer commentPosY,
                                        @RequestParam(value = "commentScale", required = false, defaultValue = "1") Float commentScale,
                                        @RequestParam(value = "postit", required = false) String postit,
                                        @RequestParam(value = "forceSend", required = false, defaultValue = "false") Boolean forceSend) {
        Long commentId;
        try {
            commentId = signRequestService.addComment(id, comment, commentPageNumber, commentPosX, commentPosY, Math.round(200 * commentScale), Math.round(100 * commentScale), postit, spotStepNumber, authUserEppn, userEppn, forceSend);
        } catch (EsupSignatureException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
        if(commentId != null) {
            return ResponseEntity.ok().body(commentId.toString());
        } else {
            return ResponseEntity.badRequest().body("Problème lors de l'ajout du post-it");
        }
    }

    @PreAuthorize("@preAuthorizeService.signRequestCreator(#id, #userEppn)")
    @PostMapping(value = "/add-spot/{id}")
    @ResponseBody
    public ResponseEntity<String> addSpot(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                                          @RequestParam(value = "spotStepNumber", required = false) Integer spotStepNumber,
                                          @RequestParam(value = "commentPageNumber", required = false) Integer commentPageNumber,
                                          @RequestParam(value = "commentPosX", required = false) Integer commentPosX,
                                          @RequestParam(value = "commentPosY", required = false) Integer commentPosY,
                                          @RequestParam(value = "commentScale", required = false, defaultValue = "1") Float commentScale) {
        Long spotId;
        try {
            spotId = signRequestService.addSpot(id, commentPageNumber, commentPosX, commentPosY, Math.round(200 * commentScale), Math.round(100 * commentScale), spotStepNumber);
        } catch (EsupSignatureException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
        if(spotId != null) {
            return ResponseEntity.ok().body(spotId.toString());
        } else {
            return ResponseEntity.badRequest().body("Problème lors de l'ajout du post-it");
        }
    }

    @PreAuthorize("@preAuthorizeService.signRequestSign(#id, #userEppn, #authUserEppn)")
    @PostMapping(value = "/add-repeatable-step/{id}")
    @ResponseBody
    public ResponseEntity<String> addRepeatableStep(@ModelAttribute("authUserEppn") String authUserEppn, @ModelAttribute("userEppn") String userEppn,
                                                    @PathVariable("id") Long id,
                                                    @RequestBody WorkflowStepDto step) {
        try {
            signBookService.addLiveStep(signRequestService.getById(id).getParentSignBook().getId(), step, step.getStepNumber(), authUserEppn);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (EsupSignatureException e) {
            logger.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("@preAuthorizeService.signRequestRecipient(#signRequestId, #authUserEppn)")
    @PostMapping(value = "/transfert/{signRequestId}")
    public String transfer(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("signRequestId") Long signRequestId,
                           @RequestParam(value = "transfertRecipientsEmails") String transfertRecipientsEmails,
                           @RequestParam(value = "phones", required = false) String phones,
                           @RequestParam(value = "names", required = false) String names,
                           @RequestParam(value = "firstnames", required = false) String firstnames,
                           @RequestParam(value = "keepFollow", required = false) Boolean keepFollow, HttpServletRequest httpServletRequest,
                           RedirectAttributes redirectAttributes) {
        if(!globalProperties.getEnableTransfertForUsers()) {
            redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Opération interdite"));
            return "redirect:/user/signrequests/" + signRequestId;
        }
        if(keepFollow == null) keepFollow = false;
        try {
            signBookService.transfertSignRequest(signRequestId, authUserEppn, transfertRecipientsEmails, phones, names, firstnames, keepFollow);
            redirectAttributes.addFlashAttribute("message", new JsMessage("success", "Demande transférée"));
            if(keepFollow) {
                return "redirect:/user/signrequests/" + signRequestId;
            } else {
                String path = httpServletRequest.getRequestURI();
                String basePath = path.startsWith("/otp") ? "/otp-access/transferred" : "/user/signrequests/" + signRequestId;
                return "redirect:" + basePath;
            }
        } catch (EsupSignatureRuntimeException e) {
            redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Demande non transférée : " + e.getMessage()));
            return "redirect:/user/signrequests/" + signRequestId;
        }
    }

    @PreAuthorize("@preAuthorizeService.signRequestSign(#id, #userEppn, #authUserEppn)")
    @PostMapping(value = "/refuse/{id}")
    public String refuse(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @RequestParam(value = "comment") String comment, @RequestParam(value = "redirect") String redirect, HttpServletRequest httpServletRequest, RedirectAttributes redirectAttributes) throws EsupSignatureRuntimeException {
        signBookService.refuse(id, comment, userEppn, authUserEppn);
        redirectAttributes.addFlashAttribute("messageInfos", "La demandes a bien été refusée");
        String path = httpServletRequest.getRequestURI();
        String basePath = path.startsWith("/otp") ? "/otp/signrequests/" : "/user/signrequests/";
        if(redirect.equals("end")) {
            User user = userService.getByEppn(userEppn);
            if(!user.getReturnToHomeAfterSign()) {
                return "redirect:" + basePath + id;
            }
            return "redirect:/user";
        } else {
            return "redirect:" + basePath + redirect;
        }
    }

}