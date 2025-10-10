package org.esupportail.esupsignature.web.controller;

import eu.europa.esig.dss.validation.reports.Reports;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.io.FileUtils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dss.service.XSLTService;
import org.esupportail.esupsignature.dto.js.JsMessage;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private final WorkflowService workflowService;
    private final SignBookService signBookService;
    private final LogService logService;
    private final AuditTrailService auditTrailService;
    private final XSLTService xsltService;
    private final SignService signService;

    public UserAndOtpSignRequestController(SignRequestService signRequestService, CommentService commentService, SignWithService signWithService, DataService dataService, UserService userService, CertificatService certificatService, PreAuthorizeService preAuthorizeService, WorkflowService workflowService, SignBookService signBookService, LogService logService, AuditTrailService auditTrailService, XSLTService xsltService, SignService signService) {
        this.signRequestService = signRequestService;
        this.commentService = commentService;
        this.signWithService = signWithService;
        this.dataService = dataService;
        this.userService = userService;
        this.certificatService = certificatService;
        this.preAuthorizeService = preAuthorizeService;
        this.workflowService = workflowService;
        this.signBookService = signBookService;
        this.logService = logService;
        this.auditTrailService = auditTrailService;
        this.xsltService = xsltService;
        this.signService = signService;
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @GetMapping(value = "/{id}")
    public String show(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @RequestParam(required = false) Boolean frameMode, Model model, HttpSession httpSession, HttpServletRequest httpServletRequest) throws IOException, EsupSignatureRuntimeException {
        String urlProfil = "user";
        String path = httpServletRequest.getRequestURI();
        SignRequest signRequest = signRequestService.getById(id);
        if(path.startsWith("/otp")) {
            urlProfil = "otp";
            model.addAttribute("displayNotif", false);
            model.addAttribute("isTempUsers", false);
        } else {
            model.addAttribute("displayNotif", signRequestService.isDisplayNotif(signRequest, userEppn));
            model.addAttribute("isTempUsers", signBookService.isTempUsers(signRequest.getParentSignBook().getId()));
        }
        boolean signable = signBookService.checkSignRequestSignable(id, userEppn, authUserEppn);
        model.addAttribute("signable", signable);
        model.addAttribute("urlProfil", urlProfil);
        model.addAttribute("isManager", signBookService.checkUserManageRights(signRequest.getParentSignBook().getId(), userEppn));
        model.addAttribute("signRequest", signRequest);
        model.addAttribute("signBook", signRequest.getParentSignBook());
        Workflow workflow = signRequest.getParentSignBook().getLiveWorkflow().getWorkflow();
        model.addAttribute("workflow", workflow);
        model.addAttribute("postits", signRequestService.getPostits(id));
        model.addAttribute("comments", signRequestService.getComments(id));
        model.addAttribute("spots", signRequestService.getSpots(id));
        boolean attachmentAlert = signRequestService.isAttachmentAlert(signRequest);
        model.addAttribute("attachmentAlert", attachmentAlert);
        boolean attachmentRequire = signRequestService.isAttachmentRequire(signRequest);
        model.addAttribute("attachmentRequire", attachmentRequire);
        model.addAttribute("currentSignType", signRequest.getCurrentSignType());
        model.addAttribute("currentStepNumber", signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber());
        model.addAttribute("currentStepMultiSign", true);
        model.addAttribute("currentStepSingleSignWithAnnotation", true);
        SignLevel currentStepMinSignLevel = SignLevel.simple;
        if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() != null) {
            currentStepMinSignLevel = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getMinSignLevel();
            if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep() != null) {
                model.addAttribute("currentStepId", signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep().getId());
            }
            model.addAttribute("currentStepMultiSign", signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getMultiSign());
            model.addAttribute("currentStepSingleSignWithAnnotation", signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSingleSignWithAnnotation());
        }
        model.addAttribute("nbSignRequestInSignBookParent", signRequest.getParentSignBook().getSignRequests().size());
        List<Document> toSignDocuments = signRequestService.getToSignDocuments(signRequest.getId());
        if(toSignDocuments.size() == 1) {
            model.addAttribute("toSignDocument", toSignDocuments.get(0));
        }
        if(toSignDocuments.stream().anyMatch(d -> !d.getContentType().equals("application/pdf")) && currentStepMinSignLevel.getValue() < 3) {
            currentStepMinSignLevel = SignLevel.advanced;
        }
        model.addAttribute("sealCertificatPropertieses", certificatService.getCheckedSealCertificates());
        model.addAttribute("currentStepMinSignLevel", currentStepMinSignLevel);
        model.addAttribute("attachments", signRequestService.getAttachments(id));
        SignBook nextSignBook = signBookService.getNextSignBook(signRequest.getId(), userEppn, authUserEppn);
        model.addAttribute("nextSignBook", nextSignBook);
        model.addAttribute("nextSignRequest", signBookService.getNextSignRequest(signRequest.getId(), nextSignBook));
        model.addAttribute("fields", signRequestService.prefillSignRequestFields(id, userEppn));
        model.addAttribute("toUseSignRequestParams", signRequestService.getToUseSignRequestParams(id, userEppn));
        model.addAttribute("favoriteSignRequestParamsJson", userService.getFavoriteSignRequestParamsJson(userEppn));
        try {
            Object userShareString = httpSession.getAttribute("userShareId");
            Long userShareId = null;
            if(userShareString != null) {
                userShareId = Long.valueOf(userShareString.toString());
            }
            List<String> signImages = signBookService.getSignImagesForSignRequest(id, userEppn, authUserEppn, userShareId);
            model.addAttribute("signImages", signImages);
        } catch (EsupSignatureUserException e) {
            model.addAttribute("message", new JsMessage("warn", e.getMessage()));
        }
        model.addAttribute("signatureIds", new ArrayList<>());
        Reports reports = signService.validate(id);
        if(reports != null) {
            model.addAttribute("signatureIds", reports.getSimpleReport().getSignatureIdList());
            List<SignWith> signWiths = signWithService.getAuthorizedSignWiths(userEppn, signRequest, !reports.getSimpleReport().getSignatureIdList().isEmpty());
            if(signable) model.addAttribute("signWiths", signWiths);
            model.addAttribute("signatureIssue", false);
            for(String signatureId : reports.getSimpleReport().getSignatureIdList()) {
                if(!reports.getSimpleReport().isValid(signatureId)) {
                    model.addAttribute("signatureIssue", true);
                }
            }
        } else {
            if(signable)  model.addAttribute("signWiths", signWithService.getAuthorizedSignWiths(userEppn, signRequest, false));
        }
        if(!signRequest.getStatus().equals(SignRequestStatus.draft) && !signRequest.getStatus().equals(SignRequestStatus.pending) && !signRequest.getStatus().equals(SignRequestStatus.refused) && !signRequest.getDeleted()) {
            if (reports != null) {
                model.addAttribute("simpleReport", xsltService.generateShortReport(reports.getXmlSimpleReport()));
            }
            AuditTrail auditTrail = auditTrailService.getAuditTrailByToken(signRequest.getToken());
            if(auditTrail != null) {
                model.addAttribute("auditTrail", auditTrail);
                if(auditTrail.getDocumentSize() != null) {
                    model.addAttribute("size", FileUtils.byteCountToDisplaySize(auditTrail.getDocumentSize()));
                }
            }
        }
        model.addAttribute("sealCertOK", signWithService.checkSealCertificat(userEppn, true));
        model.addAttribute("allSignWiths", SignWith.values());
        model.addAttribute("certificats", certificatService.getCertificatByUser(userEppn));
        model.addAttribute("editable", signRequestService.isEditable(id, userEppn));
        model.addAttribute("isNotSigned", !signRequestService.isSigned(signRequest, reports));
        model.addAttribute("isCurrentUserAsSigned", signRequestService.isCurrentUserAsSigned(signRequest, userEppn));
        if(signRequest.getStatus().equals(SignRequestStatus.draft)) {
            model.addAttribute("steps", workflowService.getWorkflowStepsFromSignRequest(signRequest, userEppn));
        }
        model.addAttribute("refuseLogs", logService.getRefuseLogs(signRequest.getId()));
        model.addAttribute("viewRight", preAuthorizeService.checkUserViewRights(signRequest, userEppn, authUserEppn));
        model.addAttribute("frameMode", frameMode);
        if(signRequest.getData() != null && signRequest.getData().getForm() != null && signRequest.getData().getForm().getWorkflow() != null) {
            model.addAttribute("action", signRequest.getData().getForm().getAction());
            model.addAttribute("supervisors", signRequest.getData().getForm().getWorkflow().getManagers());
        }
        if(signable
                && signRequest.getParentSignBook().getLiveWorkflow().getWorkflow() != null && userService.getUiParams(authUserEppn) != null
                && (userService.getUiParams(authUserEppn).get(UiParams.workflowVisaAlert) == null || !Arrays.asList(userService.getUiParams(authUserEppn).get(UiParams.workflowVisaAlert).split(",")).contains(signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getId().toString()))
                && signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().equals(SignType.hiddenVisa)) {
            model.addAttribute("message", new JsMessage("custom", "Vous êtes destinataire d'une demande de visa (et non de signature) sur ce document.\nSa validation implique que vous en acceptez le contenu.\nVous avez toujours la possibilité de ne pas donner votre accord en refusant cette demande de visa et en y adjoignant vos commentaires."));
            userService.setUiParams(authUserEppn, UiParams.workflowVisaAlert, signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getId().toString() + ",");

        }
        Data data = dataService.getBySignBook(signRequest.getParentSignBook());
        if(data != null && data.getForm() != null) {
            model.addAttribute("form", data.getForm());
        }
        List<Log> logs = logService.getFullBySignRequest(signRequest.getId());
        model.addAttribute("logs", logs);
        if(!toSignDocuments.isEmpty()) {
            model.addAttribute("pdfaCheck", toSignDocuments.get(0).getPdfaCheck());
        }
        if(signRequest.getParentSignBook().getStatus().equals(SignRequestStatus.completed) || signRequest.getParentSignBook().getStatus().equals(SignRequestStatus.exported)) {
            model.addAttribute("auditTrailChecked", true);
            model.addAttribute("externalsRecipients", signRequestService.getExternalRecipients(signRequest.getId()));
        }
        return "user/signrequests/show";
    }

    @PreAuthorize("@preAuthorizeService.signRequestRecipientAndViewers(#id, #userEppn)")
    @PostMapping(value = "/postit/{id}")
    public String postit(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                         @RequestParam(value = "comment", required = false) String comment,
                         @RequestParam(value = "postit", required = false) String postit,
                         @RequestParam(value = "forceSend", required = false, defaultValue = "false") Boolean forceSend, Model model, HttpServletRequest httpServletRequest) {
        Long commentId = signRequestService.addComment(id, comment, null, null, null, null, null, postit, null, authUserEppn, userEppn, forceSend);
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
        if(signRequestService.addAttachement(multipartFiles, link, id, authUserEppn)) {
            redirectAttributes.addFlashAttribute("message", new JsMessage("info", "La piece jointe a bien été ajoutée"));
        } else {
            redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Aucune pièce jointe n'a été ajoutée. Merci de contrôle la validité du document"));
        }
        String path = httpServletRequest.getRequestURI();
        String basePath = path.startsWith("/otp") ? "/otp/signrequests/" : "/user/signrequests/";
        return "redirect:" + basePath + id;    }

    @PreAuthorize("@preAuthorizeService.attachmentCreator(#attachementId, #userEppn, #authUserEppn)")
    @DeleteMapping(value = "/remove-attachment/{id}/{attachementId}")
    public String removeAttachement(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @PathVariable("attachementId") Long attachementId, RedirectAttributes redirectAttributes, HttpServletRequest httpServletRequest) {
        logger.info("start remove attachment");
        signRequestService.removeAttachement(id, attachementId, redirectAttributes);
        redirectAttributes.addFlashAttribute("message", new JsMessage("info", "La pieces jointe a été supprimée"));
        String path = httpServletRequest.getRequestURI();
        String basePath = path.startsWith("/otp") ? "/otp/signrequests/" : "/user/signrequests/";
        return "redirect:" + basePath + id;    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @DeleteMapping(value = "/remove-link/{id}/{linkId}")
    public String removeLink(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @PathVariable("linkId") Integer linkId, RedirectAttributes redirectAttributes, HttpServletRequest httpServletRequest) {
        logger.info("start remove link");
        signRequestService.removeLink(id, linkId);
        redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Le lien a été supprimé"));
        String path = httpServletRequest.getRequestURI();
        String basePath = path.startsWith("/otp") ? "/otp/signrequests/" : "/user/signrequests/";
        return "redirect:" + basePath + id;
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @GetMapping(value = "/get-attachment/{id}/{attachementId}")
    public void getAttachment(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @PathVariable("attachementId") Long attachementId, HttpServletResponse httpServletResponse, RedirectAttributes redirectAttributes, HttpServletRequest httpServletRequest) {
        try {
            if (!signRequestService.getAttachmentResponse(id, attachementId, httpServletResponse)) {
                redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Pièce jointe non trouvée ..."));
                httpServletResponse.sendRedirect("/user/signsignrequests/" + id);
            }
        } catch (Exception e) {
            logger.error("get file error", e);
        }
    }

    @PreAuthorize("@preAuthorizeService.signRequestRecipientAndViewers(#id, #userEppn)")
    @PostMapping(value = "/comment/{id}")
    @ResponseBody
    public ResponseEntity<Long> comment(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                                        @RequestParam(value = "comment", required = false) String comment,
                                        @RequestParam(value = "spotStepNumber", required = false) Integer spotStepNumber,
                                        @RequestParam(value = "commentPageNumber", required = false) Integer commentPageNumber,
                                        @RequestParam(value = "commentPosX", required = false) Integer commentPosX,
                                        @RequestParam(value = "commentPosY", required = false) Integer commentPosY,
                                        @RequestParam(value = "commentWidth", required = false) Integer commentWidth,
                                        @RequestParam(value = "commentHeight", required = false) Integer commentHeight,
                                        @RequestParam(value = "postit", required = false) String postit,
                                        @RequestParam(value = "forceSend", required = false, defaultValue = "false") Boolean forceSend,
                                        Model model) {
        Long commentId = signRequestService.addComment(id, comment, commentPageNumber, commentPosX, commentPosY, commentWidth, commentHeight, postit, spotStepNumber, authUserEppn, userEppn, forceSend);
        if(commentId != null) {
            return ResponseEntity.ok().body(commentId);
        } else {
            return ResponseEntity.badRequest().body(null);
        }
    }

}