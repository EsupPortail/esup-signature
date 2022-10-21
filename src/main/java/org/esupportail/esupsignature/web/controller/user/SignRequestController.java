package org.esupportail.esupsignature.web.controller.user;

import eu.europa.esig.dss.validation.reports.Reports;
import org.apache.commons.io.FileUtils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dss.service.XSLTService;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.entity.enums.SignWith;
import org.esupportail.esupsignature.entity.enums.UiParams;
import org.esupportail.esupsignature.exception.*;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.esupportail.esupsignature.service.security.otp.OtpService;
import org.esupportail.esupsignature.service.utils.sign.SignService;
import org.esupportail.esupsignature.web.ws.json.JsonExternalUserInfo;
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/user/signrequests")
@EnableConfigurationProperties(GlobalProperties.class)
public class SignRequestController {

    private static final Logger logger = LoggerFactory.getLogger(SignRequestController.class);

    @Resource
    private SignService signService;

    @Resource
    private SignWithService signWithService;

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
        return "redirect:/user/";
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @GetMapping(value = "/{id}")
    public String show(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @RequestParam(required = false) Boolean frameMode, Model model, HttpSession httpSession) throws IOException, EsupSignatureException {
        SignRequest signRequest = signBookService.getSignRequestsFullById(id, userEppn, authUserEppn);
        boolean displayNotif = false;
        if (signRequest.getLastNotifDate() == null && Duration.between(signRequest.getCreateDate().toInstant(), new Date().toInstant()).toHours() > globalProperties.getHoursBeforeRefreshNotif()) {
            displayNotif = true;
        } else if (signRequest.getLastNotifDate() != null && Duration.between(signRequest.getLastNotifDate().toInstant(), new Date().toInstant()).toHours() > globalProperties.getHoursBeforeRefreshNotif()) {
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
        model.addAttribute("nextSignBook", signBookService.getNextSignBook(signRequest.getId(), userEppn));
        model.addAttribute("nextSignRequest", signBookService.getNextSignRequest(signRequest.getId(), userEppn));
        model.addAttribute("fields", signRequestService.prefillSignRequestFields(id, userEppn));
        model.addAttribute("toUseSignRequestParams", signRequestService.getToUseSignRequestParams(id, userEppn));
        model.addAttribute("uiParams", userService.getUiParams(authUserEppn));
        model.addAttribute("favoriteSignRequestParamsJson", userService.getFavoriteSignRequestParamsJson(userEppn));
        try {
            Object userShareString = httpSession.getAttribute("userShareId");
            Long userShareId = null;
            if(userShareString != null) userShareId = Long.valueOf(userShareString.toString());
            List<String> signImages = signBookService.getSignImagesForSignRequest(id, userEppn, authUserEppn, userShareId);
            model.addAttribute("signImages", signImages);
        } catch (EsupSignatureUserException e) {
            model.addAttribute("message", new JsonMessage("warn", e.getMessage()));
        }
        model.addAttribute("signatureIds", new ArrayList<>());
        Reports reports = signRequestService.validate(id);
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
        model.addAttribute("signWiths", signWithService.getAuthorizedSignWiths(userEppn, signRequest));
        model.addAttribute("sealCertOK", signWithService.checkSealCertificat(userEppn));
        model.addAttribute("allSignWiths", SignWith.values());
        model.addAttribute("certificats", certificatService.getCertificatByUser(userEppn));
        model.addAttribute("signable", signRequest.getSignable());
        model.addAttribute("editable", signRequest.getEditable());
        model.addAttribute("isNotSigned", signService.isNotSigned(signRequest));
        model.addAttribute("isTempUsers", signRequestService.isTempUsers(id));
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
        if(signRequest.getSignable()
                && signRequest.getParentSignBook().getLiveWorkflow().getWorkflow() != null && userService.getUiParams(authUserEppn) != null
                && (userService.getUiParams(authUserEppn).get(UiParams.workflowVisaAlert) == null || !Arrays.asList(userService.getUiParams(authUserEppn).get(UiParams.workflowVisaAlert).split(",")).contains(signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getId().toString()))
                && signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().equals(SignType.hiddenVisa)) {
            model.addAttribute("message", new JsonMessage("custom", "Vous êtes destinataire d'une demande de visa (et non de signature) sur ce document.\nSa validation implique que vous en acceptez le contenu.\nVous avez toujours la possibilité de ne pas donner votre accord en refusant cette demande de visa et en y adjoignant vos commentaires."));
            userService.setUiParams(authUserEppn, UiParams.workflowVisaAlert, signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getId().toString() + ",");

        }
        Data data = signBookService.getBySignBook(signRequest.getParentSignBook());
        if(data != null && data.getForm() != null) {
            model.addAttribute("form", data.getForm());
        }
        List<Log> logs = logService.getFullBySignRequest(signRequest.getId());
        model.addAttribute("logs", logs);
        return "user/signrequests/show";
    }

//    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserEppn)")
//    @ResponseBody
//    @PostMapping(value = "/add-docs/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
//    public Object addDocumentToNewSignRequest(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @RequestParam("multipartFiles") MultipartFile[] multipartFiles) throws EsupSignatureIOException {
//        logger.info("start add documents");
//        SignRequest signRequest = signRequestService.getById(id);
//        int i = 0;
//        for (MultipartFile multipartFile : multipartFiles) {
//            signRequestService.addDocsToSignRequest(signRequest, true, i, new ArrayList<>(), multipartFile);
//            i++;
//        }
//        return new String[]{"ok"};
//    }
//
//    @PreAuthorize("@preAuthorizeService.notInShare(#userEppn, #authUserEppn) && hasRole('ROLE_USER')")
//    @PostMapping(value = "/fast-sign-request")
//    public String createSignRequest(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @RequestParam("multipartFiles") MultipartFile[] multipartFiles,
//                                    @RequestParam("signType") SignType signType,
//                                    HttpServletRequest request, RedirectAttributes redirectAttributes) {
//        if (multipartFiles != null) {
//            try {
//                SignBook signBook = signBookService.addFastSignRequestInNewSignBook(multipartFiles, signType, userEppn, authUserEppn);
//                return "redirect:/user/signrequests/" + signBook.getSignRequests().get(0).getId();
//            } catch (EsupSignatureException e) {
//                redirectAttributes.addFlashAttribute("message", new JsonMessage("error", e.getMessage()));
//            }
//        } else {
//            logger.warn("no file to import");
//        }
//        return "redirect:" + request.getHeader(HttpHeaders.REFERER);
//    }

    @PreAuthorize("@preAuthorizeService.notInShare(#userEppn, #authUserEppn) && hasRole('ROLE_USER')")
    @PostMapping(value = "/send-sign-request")
    public String sendSignRequest(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
                                  @RequestParam("multipartFiles") MultipartFile[] multipartFiles,
                                  @RequestParam("signType") SignType signType,
                                  @RequestParam(value = "recipientsEmails", required = false) List<String> recipientsEmails,
                                  @RequestParam(value = "recipientsCCEmails", required = false) List<String> recipientsCCEmails,
                                  @RequestParam(name = "allSignToComplete", required = false) Boolean allSignToComplete,
                                  @RequestParam(name = "forceAllSign", required = false) Boolean forceAllSign,
                                  @RequestParam(name = "userSignFirst", required = false) Boolean userSignFirst,
                                  @RequestParam(value = "pending", required = false) Boolean pending,
                                  @RequestParam(value = "comment", required = false) String comment,
                                  @RequestParam(value = "emails", required = false) List<String> emails,
                                  @RequestParam(value = "names", required = false) List<String> names,
                                  @RequestParam(value = "firstnames", required = false) List<String> firstnames,
                                  @RequestParam(value = "phones", required = false) List<String> phones,
                                  @RequestParam(value = "title", required = false) String title,
                                  RedirectAttributes redirectAttributes, HttpServletRequest httpServletRequest) {
        String referer = httpServletRequest.getHeader(HttpHeaders.REFERER);
        recipientsEmails = recipientsEmails.stream().distinct().collect(Collectors.toList());
        logger.info(userEppn + " envoi d'une demande de signature à " + recipientsEmails);
        List<JsonExternalUserInfo> externalUsersInfos = userService.getJsonExternalUserInfos(emails, names, firstnames, phones);
        if (multipartFiles != null) {
            try {
                Map<SignBook, String> signBookStringMap = null;
                try {
                    signBookStringMap = signBookService.sendSignRequest(title, multipartFiles, signType, allSignToComplete, userSignFirst, pending, comment, recipientsCCEmails, recipientsEmails, externalUsersInfos, userEppn, authUserEppn, false, forceAllSign, null);
                } catch(EsupSignatureIOException e) {
                    logger.warn("error on send signrequest, redirect to home");
                    redirectAttributes.addFlashAttribute("message", new JsonMessage("error", e.getMessage()));
                    return "redirect:" + referer;
                }
                if (signBookStringMap.values().iterator().next() != null) {
                    redirectAttributes.addFlashAttribute("message", new JsonMessage("warn", signBookStringMap.values().toArray()[0].toString()));
                } else {
                    if(userSignFirst == null || !userSignFirst) {
                        redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Votre demande à bien été envoyée"));
                    }
                }
                long signRequestId = signBookStringMap.keySet().iterator().next().getSignRequests().get(0).getId();
                if(signRequestService.checkTempUsers(signRequestId, recipientsEmails, externalUsersInfos)) {
                    redirectAttributes.addFlashAttribute("message", new JsonMessage("warn", "Merci de compléter tous les utilisateurs externes"));
                }
                return "redirect:/user/signrequests/" + signRequestId;
            } catch (EsupSignatureException | MessagingException e) {
                redirectAttributes.addFlashAttribute("message", new JsonMessage("error", e.getMessage()));
            }
        } else {
            logger.warn("no file to import");
            redirectAttributes.addFlashAttribute("message", new JsonMessage("error","Pas de fichier à importer"));
        }
        return "redirect:" + referer;
    }

    @PreAuthorize("@preAuthorizeService.signRequestSign(#id, #userEppn, #authUserEppn)")
    @PostMapping(value = "/refuse/{id}")
    public String refuse(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @RequestParam(value = "comment") String comment, @RequestParam(value = "redirect") String redirect, RedirectAttributes redirectAttributes) throws EsupSignatureMailException, EsupSignatureException {
        signBookService.refuse(id, comment, userEppn, authUserEppn);
        redirectAttributes.addFlashAttribute("messageInfos", "La demandes à bien été refusée");
        if(redirect.equals("end")) {
            return "redirect:/user/signbooks/";
        } else {
            return "redirect:/user/signbooks/" + redirect;
        }
    }

    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserEppn)")
    @GetMapping(value = "/restore/{id}", produces = "text/html")
    public String restore(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        signRequestService.restore(id, authUserEppn);
        redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Restauration effectuée"));
        return "redirect:/user/signrequests/" + id;
    }

    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserEppn)")
    @DeleteMapping(value = "/{id}", produces = "text/html")
    public String delete(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletRequest httpServletRequest, RedirectAttributes redirectAttributes) {
        if(signRequestService.delete(id, authUserEppn)) {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Suppression définitive effectuée"));
            return "redirect:/user/signbooks";
        } else {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Suppression effectuée"));
            return "redirect:" + httpServletRequest.getHeader(HttpHeaders.REFERER);
        }
    }

    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserEppn)")
    @DeleteMapping(value = "/force-delete/{id}", produces = "text/html")
    public String forceDelete(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletRequest httpServletRequest, RedirectAttributes redirectAttributes) {
        SignRequest signRequest = signRequestService.getById(id);
        String referer = httpServletRequest.getHeader("referer");
        if(signRequest.getParentSignBook().getSignRequests().size() > 1) {
            signRequestService.deleteDefinitive(id);
            redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Suppression effectuée"));
            if(referer.contains("signrequests")) {
                return "redirect:/user/signbooks/";
            } else {
                return "redirect:" + referer;
            }
        } else {
            signBookService.deleteDefinitive(signRequest.getParentSignBook().getId(), authUserEppn);
            redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Suppression effectuée"));
            if(referer.contains("signrequests")) {
                return "redirect:/user/";
            } else {
                return "redirect:" + referer;
            }
        }
    }

    @PreAuthorize("@preAuthorizeService.signRequestRecipent(#id, #authUserEppn)")
    @PostMapping(value = "/add-attachment/{id}")
    public String addAttachement(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                                 @RequestParam(value = "multipartFiles", required = false) MultipartFile[] multipartFiles,
                                 @RequestParam(value = "link", required = false) String link,
                                 RedirectAttributes redirectAttributes) throws EsupSignatureIOException {
        logger.info("start add attachment");
        if(signRequestService.addAttachement(multipartFiles, link, id)) {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "La piece jointe à bien été ajoutée"));
        } else {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Aucune pièce jointe n'a été ajoutée. Merci de contrôle la validité du document"));
        }
        return "redirect:/user/signrequests/" + id;
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @GetMapping(value = "/remove-attachment/{id}/{attachementId}")
    public String removeAttachement(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @PathVariable("attachementId") Long attachementId, RedirectAttributes redirectAttributes) {
        logger.info("start remove attachment");
        signRequestService.removeAttachement(id, attachementId, redirectAttributes);
        redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "La pieces jointe a été supprimée"));
        return "redirect:/user/signrequests/" + id;
    }



    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @GetMapping(value = "/remove-link/{id}/{linkId}")
    public String removeLink(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @PathVariable("linkId") Integer linkId, RedirectAttributes redirectAttributes) {
        logger.info("start remove link");
        signRequestService.removeLink(id, linkId);
        redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Le lien a été supprimé"));
        return "redirect:/user/signrequests/" + id;
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @GetMapping(value = "/get-attachment/{id}/{attachementId}")
    public void getAttachment(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @PathVariable("attachementId") Long attachementId, HttpServletResponse httpServletResponse, RedirectAttributes redirectAttributes) {
        try {
            if (!signRequestService.getAttachmentResponse(id, attachementId, httpServletResponse)) {
                redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Pièce jointe non trouvée ..."));
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
        return "redirect:/user/signrequests/" + id + "/?form";
    }

    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserEppn)")
    @PostMapping(value = "/pending/{id}")
    public String pending(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                          @RequestParam(required = false) List<String> recipientEmails,
                          @RequestParam(required = false) List<String> allSignToCompletes,
                          @RequestParam(required = false) List<String> targetEmails,
                          @RequestParam(value = "comment", required = false) String comment,
                          @RequestParam(value = "emails", required = false) List<String> emails,
                          @RequestParam(value = "names", required = false) List<String> names,
                          @RequestParam(value = "firstnames", required = false) List<String> firstnames,
                          @RequestParam(value = "phones", required = false) List<String> phones,
                          @RequestParam(value = "draft", required = false) Boolean draft,
                          RedirectAttributes redirectAttributes) throws MessagingException, EsupSignatureException, EsupSignatureFsException {
        List<JsonExternalUserInfo> externalUsersInfos = userService.getJsonExternalUserInfos(emails, names, firstnames, phones);
        if(signRequestService.checkTempUsers(id, recipientEmails, externalUsersInfos)) {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Merci de compléter tous les utilisateurs externes"));
            return "redirect:/user/signrequests/" + id;
        }
        try {
            signBookService.initWorkflowAndPendingSignBook(id, recipientEmails, allSignToCompletes, externalUsersInfos, targetEmails, userEppn, authUserEppn, draft);
            if(comment != null && !comment.isEmpty()) {
                signRequestService.addPostit(id, comment, userEppn, authUserEppn);
            }
            redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Votre demande à bien été transmise"));
        } catch (EsupSignatureException e) {
            logger.error(e.getMessage(), e);
            redirectAttributes.addFlashAttribute("message", new JsonMessage("error", e.getMessage()));
        }
        return "redirect:/user/signrequests/" + id;
    }

    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserEppn)")
    @PostMapping(value = "/add-step/{id}")
    public String addRecipients(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                                @RequestParam(value = "recipientsEmails", required = false) List<String> recipientsEmails,
                                @RequestParam(name = "signType") SignType signType,
                                @RequestParam(name = "allSignToComplete", required = false) Boolean allSignToComplete) throws EsupSignatureException {
        signBookService.addStep(id, recipientsEmails, signType, allSignToComplete, authUserEppn);
        return "redirect:/user/signrequests/" + id + "/?form";
    }


    @PreAuthorize("@preAuthorizeService.signRequestRecipent(#id, #userEppn)")
    @PostMapping(value = "/comment/{id}")
    public String comment(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                          @RequestParam(value = "comment", required = false) String comment,
                          @RequestParam(value = "spotStepNumber", required = false) Integer spotStepNumber,
                          @RequestParam(value = "commentPageNumber", required = false) Integer commentPageNumber,
                          @RequestParam(value = "commentPosX", required = false) Integer commentPosX,
                          @RequestParam(value = "commentPosY", required = false) Integer commentPosY,
                          @RequestParam(value = "postit", required = false) String postit, Model model) {
        SignRequest signRequest = signRequestService.getById(id);
        if(spotStepNumber == null || userEppn.equals(signRequest.getCreateBy().getEppn())) {
            signRequestService.addComment(id, comment, commentPageNumber, commentPosX, commentPosY, postit, spotStepNumber, authUserEppn);
            model.addAttribute("message", new JsonMessage("success", "Annotation ajoutée"));
        } else {
            model.addAttribute("message", new JsonMessage("error", "Ajout d'emplacement non autorisé"));
        }
        return "redirect:/user/signrequests/" + id;
    }

    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserEppn)")
    @PostMapping(value = "/send-otp/{id}/{recipientId}")
    public String sendOtp(@ModelAttribute("authUserEppn") String authUserEppn,
                          @PathVariable("id") Long id,
                          @PathVariable("recipientId") Long recipientId,
                          @RequestParam("phone") String phone,
                          RedirectAttributes redirectAttributes) throws Exception {
        if(otpService.generateOtpForSignRequest(id, recipientId, phone)){
            redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Demande OTP envoyée"));
        } else {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Problème d'envoi OTP"));
        }
        return "redirect:/user/signrequests/" + id;
    }

    @PreAuthorize("@preAuthorizeService.signRequestRecipent(#id, #authUserEppn)")
    @PostMapping(value = "/replay-notif/{id}")
    public String replayNotif(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, RedirectAttributes redirectAttributes, HttpServletRequest httpServletRequest) throws EsupSignatureMailException {
        if(signRequestService.replayNotif(id)) {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Votre relance a bien été envoyée"));
        } else {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Votre relance n'a pas été envoyée car une autre relance a déjà été émise"));
        }
        return "redirect:" + httpServletRequest.getHeader(HttpHeaders.REFERER);
    }

}