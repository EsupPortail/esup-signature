package org.esupportail.esupsignature.web.controller.user;

import eu.europa.esig.dss.validation.reports.Reports;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.io.FileUtils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dss.service.XSLTService;
import org.esupportail.esupsignature.dto.js.JsMessage;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.entity.enums.SignWith;
import org.esupportail.esupsignature.entity.enums.UiParams;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureMailException;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.esupportail.esupsignature.service.security.otp.OtpService;
import org.esupportail.esupsignature.service.utils.sign.SignService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/user/signrequests")
@EnableConfigurationProperties(GlobalProperties.class)
public class SignRequestController {

    private static final Logger logger = LoggerFactory.getLogger(SignRequestController.class);

    @Resource
    private SignService signService;

    @Resource
    private SignWithService signWithService;

    @Resource
    private DataService dataService;

    @ModelAttribute("activeMenu")
    public String getActiveMenu() {
        return "signrequests";
    }

    @Resource
    private UserService userService;

    @Resource
    private CertificatService certificatService;

    @Resource
    private PreAuthorizeService preAuthorizeService;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private WorkflowService workflowService;

    @Resource
    private SignBookService signBookService;

    @Resource
    private LogService logService;

    @Resource
    private AuditTrailService auditTrailService;

    @Resource
    private OtpService otpService;

    @Resource
    private GlobalProperties globalProperties;

    @Resource
    private XSLTService xsltService;

    @GetMapping()
    public String show() {
        return "redirect:/user";
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @GetMapping(value = "/{id}")
    public String show(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @RequestParam(required = false) Boolean frameMode, Model model, HttpSession httpSession) throws IOException, EsupSignatureRuntimeException {
        SignRequest signRequest = signRequestService.getById(id);
        boolean displayNotif = false;
        if (signRequest.getParentSignBook().getStatus().equals(SignRequestStatus.pending) && signRequest.getCreateBy().getEppn().equals(userEppn) &&
            ((signRequest.getParentSignBook().getLastNotifDate() == null && Duration.between(signRequest.getParentSignBook().getCreateDate().toInstant(), new Date().toInstant()).toHours() > globalProperties.getHoursBeforeRefreshNotif()) ||
            (signRequest.getParentSignBook().getLastNotifDate() != null && Duration.between(signRequest.getParentSignBook().getLastNotifDate().toInstant(), new Date().toInstant()).toHours() > globalProperties.getHoursBeforeRefreshNotif()))) {
            displayNotif = true;
        }
        model.addAttribute("displayNotif", displayNotif);
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
        if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() != null && signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep() != null) {
            model.addAttribute("currentStepId", signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep().getId());
            model.addAttribute("currentStepMultiSign", signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getMultiSign());
        }
        model.addAttribute("nbSignRequestInSignBookParent", signRequest.getParentSignBook().getSignRequests().size());
        List<Document> toSignDocuments = signService.getToSignDocuments(signRequest.getId());
        if(toSignDocuments.size() == 1) {
            model.addAttribute("toSignDocument", toSignDocuments.get(0));
        }
        model.addAttribute("attachments", signRequestService.getAttachments(id));
        SignBook nextSignBook = signBookService.getNextSignBook(signRequest.getId(), userEppn, authUserEppn);
        model.addAttribute("nextSignBook", nextSignBook);
        model.addAttribute("nextSignRequest", signBookService.getNextSignRequest(signRequest.getId(), userEppn, authUserEppn, nextSignBook));
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
        if (reports != null) {
            model.addAttribute("signatureIds", reports.getSimpleReport().getSignatureIdList());
        }
        if(!signRequest.getStatus().equals(SignRequestStatus.draft) && !signRequest.getStatus().equals(SignRequestStatus.pending) && !signRequest.getStatus().equals(SignRequestStatus.refused) && !signRequest.getStatus().equals(SignRequestStatus.deleted)) {
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
        boolean signable = signBookService.checkSignRequestSignable(id, userEppn, authUserEppn);
        model.addAttribute("signWiths", signWithService.getAuthorizedSignWiths(userEppn, signRequest));
        model.addAttribute("sealCertOK", signWithService.checkSealCertificat(userEppn, true));
        model.addAttribute("allSignWiths", SignWith.values());
        model.addAttribute("certificats", certificatService.getCertificatByUser(userEppn));
        model.addAttribute("signable", signable);
        model.addAttribute("editable", signRequestService.isEditable(id, userEppn));
        model.addAttribute("isNotSigned", !signService.isSigned(signRequest, reports));
        model.addAttribute("isTempUsers", signRequestService.isTempUsers(signRequest.getParentSignBook().getId()));
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
        return "user/signrequests/show";
    }


    @PreAuthorize("@preAuthorizeService.signRequestSign(#id, #userEppn, #authUserEppn)")
    @PostMapping(value = "/refuse/{id}")
    public String refuse(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @RequestParam(value = "comment") String comment, @RequestParam(value = "redirect") String redirect, RedirectAttributes redirectAttributes) throws EsupSignatureMailException, EsupSignatureRuntimeException {
        signBookService.refuse(id, comment, userEppn, authUserEppn);
        redirectAttributes.addFlashAttribute("messageInfos", "La demandes a bien été refusée");
        if(redirect.equals("end")) {
            return "redirect:/user";
        } else {
            return "redirect:/user/signrequests/" + redirect;
        }
    }

    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserEppn)")
    @GetMapping(value = "/restore/{id}", produces = "text/html")
    public String restore(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        signRequestService.restore(id, authUserEppn);
        redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Restauration effectuée"));
        return "redirect:/user/signrequests/" + id;
    }

    @PreAuthorize("@preAuthorizeService.signRequestDelete(#id, #authUserEppn)")
    @DeleteMapping(value = "/{id}", produces = "text/html")
    public String delete(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @RequestParam(value = "definitive", required = false) Boolean definitive, HttpServletRequest httpServletRequest, RedirectAttributes redirectAttributes) {
        Long result;
        if(definitive != null && definitive) {
            result = signRequestService.deleteDefinitive(id, true, authUserEppn);
        } else {
            result = signRequestService.delete(id, authUserEppn);
        }
        if(result == 0L) {
            redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Suppression définitive effectuée"));
        } else if(result > 0L) {
            redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Suppression effectuée"));
            return "redirect:/user/signbooks/" + result;
        } else {
            redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Suppression impossible car la demande à démarrée et contient encore des documents en cours de signature"));
            return "redirect:/user/signrequests/" + id;
        }
        return "redirect:/user/signbooks";
    }

//    @PreAuthorize("@preAuthorizeService.signRequestDelete(#id, #authUserEppn)")
//    @DeleteMapping(value = "/force-delete/{id}", produces = "text/html")
//    public String forceDelete(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletRequest httpServletRequest, RedirectAttributes redirectAttributes) {
//        SignRequest signRequest = signRequestService.getById(id);
//        String referer = httpServletRequest.getHeader("referer");
//        if(signRequest.getParentSignBook().getSignRequests().size() > 1) {
//            try {
//                signRequestService.deleteDefinitive(id);
//                redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Suppression effectuée"));
//            } catch (EsupSignatureRuntimeException e) {
//                redirectAttributes.addFlashAttribute("message", new JsMessage("error", e.getMessage()));
//            }
//            if(referer.contains("signrequests")) {
//                return "redirect:/user/signbooks";
//            } else {
//                return "redirect:" + referer;
//            }
//        } else {
//            signBookService.deleteDefinitive(signRequest.getParentSignBook().getId(), authUserEppn);
//            redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Suppression effectuée"));
//            if(referer.contains("signrequests")) {
//                return "redirect:/user";
//            } else {
//                return "redirect:" + referer;
//            }
//        }
//    }

    @PreAuthorize("@preAuthorizeService.signRequestRecipient(#id, #authUserEppn)")
    @PostMapping(value = "/add-attachment/{id}")
    public String addAttachement(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                                 @RequestParam(value = "multipartFiles", required = false) MultipartFile[] multipartFiles,
                                 @RequestParam(value = "link", required = false) String link,
                                 RedirectAttributes redirectAttributes) throws EsupSignatureIOException {
        logger.info("start add attachment");
        if(signRequestService.addAttachement(multipartFiles, link, id, authUserEppn)) {
            redirectAttributes.addFlashAttribute("message", new JsMessage("info", "La piece jointe a bien été ajoutée"));
        } else {
            redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Aucune pièce jointe n'a été ajoutée. Merci de contrôle la validité du document"));
        }
        return "redirect:/user/signrequests/" + id;
    }

    @PreAuthorize("@preAuthorizeService.attachmentCreator(#attachementId, #userEppn, #authUserEppn)")
    @DeleteMapping(value = "/remove-attachment/{id}/{attachementId}")
    public String removeAttachement(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @PathVariable("attachementId") Long attachementId, RedirectAttributes redirectAttributes) {
        logger.info("start remove attachment");
        signRequestService.removeAttachement(id, attachementId, redirectAttributes);
        redirectAttributes.addFlashAttribute("message", new JsMessage("info", "La pieces jointe a été supprimée"));
        return "redirect:/user/signrequests/" + id;
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @DeleteMapping(value = "/remove-link/{id}/{linkId}")
    public String removeLink(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @PathVariable("linkId") Integer linkId, RedirectAttributes redirectAttributes) {
        logger.info("start remove link");
        signRequestService.removeLink(id, linkId);
        redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Le lien a été supprimé"));
        return "redirect:/user/signrequests/" + id;
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @GetMapping(value = "/get-attachment/{id}/{attachementId}")
    public void getAttachment(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @PathVariable("attachementId") Long attachementId, HttpServletResponse httpServletResponse, RedirectAttributes redirectAttributes) {
        try {
            if (!signRequestService.getAttachmentResponse(id, attachementId, httpServletResponse)) {
                redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Pièce jointe non trouvée ..."));
                httpServletResponse.sendRedirect("/user/signsignrequests/" + id);
            }
        } catch (Exception e) {
            logger.error("get file error", e);
        }
    }

    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserEppn)")
    @GetMapping(value = "/update-step/{id}/{step}")
    public String changeStepSignType(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @PathVariable("step") Integer step, @RequestParam(name = "signType") SignType signType) {
        SignRequest signRequest = signRequestService.getById(id);
        signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().setSignType(signType);
        return "redirect:/user/signrequests/" + id + "?form";
    }

    @PreAuthorize("@preAuthorizeService.signRequestRecipient(#id, #authUserEppn)")
    @PostMapping(value = "/transfert/{id}")
    public String transfer(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                           @RequestParam(value = "transfertRecipientsEmails") List<String> transfertRecipientsEmails,
                           @RequestParam(value = "keepFollow", required = false) Boolean keepFollow,
                           RedirectAttributes redirectAttributes) throws EsupSignatureRuntimeException {
        if(keepFollow == null) keepFollow = false;
        try {
            signBookService.transfertSignRequest(id, authUserEppn, transfertRecipientsEmails.get(0), keepFollow);
            redirectAttributes.addFlashAttribute("message", new JsMessage("success", "Demande transférée"));
            if(keepFollow) {
                return "redirect:/user/signrequests/" + id;
            } else {
                return "redirect:/user";
            }
        } catch (EsupSignatureRuntimeException e) {
            redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Demande non transférée"));
            return "redirect:/user/signrequests/" + id;
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
                                          @RequestParam(value = "postit", required = false) String postit, Model model) {
        Long commentId = signRequestService.addComment(id, comment, commentPageNumber, commentPosX, commentPosY, postit, spotStepNumber, authUserEppn, userEppn);
        if(commentId != null) {
            return ResponseEntity.ok().body(commentId);
        } else {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PreAuthorize("@preAuthorizeService.commentCreator(#postitId, #userEppn)")
    @PutMapping(value = "/comment/{signRequestId}/update/{postitId}")
    public String commentUpdate(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
                                @PathVariable("signRequestId") Long signRequestId,
                                @PathVariable("postitId") Long postitId,
                                @RequestParam(value = "comment", required = false) String comment, RedirectAttributes redirectAttributes) {
        signRequestService.updateComment(postitId, comment);
        redirectAttributes.addFlashAttribute("message", new JsMessage("success", "Annotation modifiée"));
        return "redirect:/user/signrequests/" + signRequestId;
    }

    @PreAuthorize("@preAuthorizeService.commentCreator(#postitId, #userEppn)")
    @DeleteMapping(value = "/comment/{signRequestId}/delete/{postitId}")
    public String commentDelete(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
                                @PathVariable("signRequestId") Long signRequestId,
                                @PathVariable("postitId") Long postitId, RedirectAttributes redirectAttributes) {
        signRequestService.deleteComment(signRequestId, postitId);
        redirectAttributes.addFlashAttribute("message", new JsMessage("success", "Postit supprimé"));
        return "redirect:/user/signrequests/" + signRequestId;
    }
    
    @PreAuthorize("@preAuthorizeService.signRequestRecipientAndViewers(#id, #userEppn)")
    @PostMapping(value = "/postit/{id}")
    public String postit(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                                        @RequestParam(value = "comment", required = false) String comment,
                                        @RequestParam(value = "postit", required = false) String postit, Model model) {
        Long commentId = signRequestService.addComment(id, comment, null, null, null, postit, null, authUserEppn, userEppn);
        if(commentId != null) {
            model.addAttribute("message", new JsMessage("success", "Post-it ajouté"));
        } else {
            model.addAttribute("message", new JsMessage("error", "Problème lors de l'ajout du post-it"));
        }
        return "redirect:/user/signrequests/" + id;

    }

    @PreAuthorize("@preAuthorizeService.signBookSendOtp(#id, #authUserEppn)")
    @PostMapping(value = "/send-otp/{id}/{recipientId}")
    public String sendOtp(@ModelAttribute("authUserEppn") String authUserEppn,
                          @PathVariable("id") Long id,
                          @PathVariable("recipientId") Long recipientId,
                          @RequestParam(value = "phone", required = false) String phone,
                          RedirectAttributes redirectAttributes) {
        if(otpService.generateOtpForSignRequest(id, recipientId, phone)){
            redirectAttributes.addFlashAttribute("message", new JsMessage("success", "Demande OTP envoyée"));
        } else {
            redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Problème d'envoi OTP"));
        }
        return "redirect:/user/signbooks/" + id;
    }

    @PreAuthorize("@preAuthorizeService.signRequestRecipient(#id, #authUserEppn)")
    @PostMapping(value = "/replay-notif/{id}")
    public String replayNotif(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, RedirectAttributes redirectAttributes, HttpServletRequest httpServletRequest) throws EsupSignatureMailException {
        if(signRequestService.replayNotif(id, authUserEppn)) {
            redirectAttributes.addFlashAttribute("message", new JsMessage("success", "Votre relance a bien été envoyée"));
        } else {
            redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Votre relance n'a pas été envoyée car une autre relance a déjà été émise"));
        }
        return "redirect:" + httpServletRequest.getHeader(HttpHeaders.REFERER);
    }

}
