package org.esupportail.esupsignature.web.otp;

import eu.europa.esig.dss.validation.reports.Reports;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dto.js.JsMessage;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.entity.enums.UiParams;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureMailException;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/otp/signrequests")
@EnableConfigurationProperties(GlobalProperties.class)
public class OtpSignRequestController {

    private static final Logger logger = LoggerFactory.getLogger(OtpSignRequestController.class);

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

    @GetMapping(value = "/signbook-redirect/{id}")
    public String redirect(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) throws IOException, EsupSignatureRuntimeException {
        SignBook signBook = signBookService.getById(id);
        if(!preAuthorizeService.signBookView(id, userEppn, authUserEppn)) {
            User user = userService.getByEppn(userEppn);
            redirectAttributes.addFlashAttribute("errorMsg", "Access non autorisé");
            if (signBook.getLiveWorkflow().getCurrentStep().getRecipients().stream().noneMatch(r -> r.getUser().getEmail().equals(user.getEmail()))) {
                redirectAttributes.addFlashAttribute("errorMsg",
                        "<p>L'adresse email liée à votre authentification ne correspond pas à celle indiquée dans la demande initiale.<br>\n" +
                                "Voici l'adresse transmise par votre fournisseur d'identité :"
                                + user.getEmail() +
                                "</p><p>Vous pouvez soit modifier votre adresse de contact auprès de votre fournisseur d'identité, soit contacter le gestionnaire de la demande afin qu’il mette à jour l’email de contact dans la demande de signature.</p>");
            }
            return "redirect:/otp-access/error";
        }
        return "redirect:/otp/signrequests/" + signBook.getSignRequests().get(0).getId();
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @GetMapping(value = "/{id}")
    public String show(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, Model model, HttpSession httpSession) throws IOException, EsupSignatureRuntimeException {
        SignRequest signRequest = signRequestService.getById(id);
        model.addAttribute("urlProfil", "otp");
        model.addAttribute("displayNotif", false);
        model.addAttribute("notifTime", 0);
        model.addAttribute("signRequest", signRequest);
        model.addAttribute("signBook", signRequest.getParentSignBook());
        Workflow workflow = signRequest.getParentSignBook().getLiveWorkflow().getWorkflow();
        model.addAttribute("workflow", workflow);
        model.addAttribute("postits", signRequest.getComments().stream().filter(Comment::getPostit).collect(Collectors.toList()));
        List<Comment> comments = signRequest.getComments().stream().filter(comment -> !comment.getPostit() && comment.getStepNumber() == null).collect(Collectors.toList());
        model.addAttribute("comments", comments);
        model.addAttribute("spots", signRequest.getComments().stream().filter(comment -> comment.getStepNumber() != null).collect(Collectors.toList()));
        boolean attachmentAlert = signRequestService.isAttachmentAlert(signRequest);
        model.addAttribute("attachmentAlert", attachmentAlert);
        boolean attachmentRequire = signRequestService.isAttachmentRequire(signRequest);
        model.addAttribute("attachmentRequire", attachmentRequire);
        model.addAttribute("isCurrentUserAsSigned", signRequestService.isCurrentUserAsSigned(signRequest, userEppn));
        model.addAttribute("currentSignType", signRequest.getCurrentSignType());
        model.addAttribute("currentStepNumber", signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber());
        model.addAttribute("currentStepMinSignLevel", signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getMinSignLevel());
        model.addAttribute("currentStepMultiSign", true);
        model.addAttribute("currentStepSingleSignWithAnnotation", true);
        if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() != null) {
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
        model.addAttribute("attachments", signRequestService.getAttachments(id));
        SignBook nextSignBook = signBookService.getNextSignBook(signRequest.getId(), userEppn, authUserEppn);
        model.addAttribute("nextSignBook", nextSignBook);
        model.addAttribute("nextSignRequest", signBookService.getNextSignRequest(signRequest.getId(), userEppn, authUserEppn, nextSignBook));
        model.addAttribute("fields", signRequestService.prefillSignRequestFields(id, userEppn));
        model.addAttribute("toUseSignRequestParams", signRequestService.getToUseSignRequestParams(id, userEppn));
        model.addAttribute("sealCertOK", signWithService.checkSealCertificat(userEppn, true));
        model.addAttribute("otp", true);
        if(!signRequest.getStatus().equals(SignRequestStatus.draft)) {
            try {
                Object userShareString = httpSession.getAttribute("userShareId");
                Long userShareId = null;
                if(userShareString != null) userShareId = Long.valueOf(userShareString.toString());
                List<String> signImages = signBookService.getSignImagesForSignRequest(id, userEppn, authUserEppn, userShareId);
                model.addAttribute("signImages", signImages);
            } catch (EsupSignatureUserException e) {
                model.addAttribute("message", new JsMessage("warn", e.getMessage()));
            }
        }
        model.addAttribute("signatureIds", new ArrayList<>());
        Reports reports = signRequestService.validate(id);
        if(reports != null) {
            model.addAttribute("signWiths", signWithService.getAuthorizedSignWiths(userEppn, signRequest, !reports.getSimpleReport().getSignatureIdList().isEmpty()));
            model.addAttribute("signatureIds", reports.getSimpleReport().getSignatureIdList());
            model.addAttribute("signatureIssue", false);
            for(String signatureId : reports.getSimpleReport().getSignatureIdList()) {
                if(!reports.getSimpleReport().isValid(signatureId)) {
                    model.addAttribute("signatureIssue", true);
                }
            }
        } else {
            model.addAttribute("signWiths", signWithService.getAuthorizedSignWiths(userEppn, signRequest, false));
        }
        model.addAttribute("certificats", certificatService.getCertificatByUser(userEppn));
        boolean signable = signBookService.checkSignRequestSignable(id, userEppn, authUserEppn);
        model.addAttribute("signable", signable);
        model.addAttribute("editable", signRequestService.isEditable(id, userEppn));
        model.addAttribute("isNotSigned", !signRequestService.isSigned(signRequest, reports));
        model.addAttribute("isTempUsers", false);
        if(signRequest.getStatus().equals(SignRequestStatus.draft)) {
            model.addAttribute("steps", workflowService.getWorkflowStepsFromSignRequest(signRequest, userEppn));
        }
        model.addAttribute("refuseLogs", logService.getRefuseLogs(signRequest.getId()));
        model.addAttribute("viewRight", preAuthorizeService.checkUserViewRights(signRequest, userEppn, authUserEppn));
        if(signRequest.getData() != null && signRequest.getData().getForm() != null) {
            model.addAttribute("action", signRequest.getData().getForm().getAction());
            model.addAttribute("supervisors", signRequest.getData().getForm().getWorkflow().getManagers());
        }
        List<Log> logs = logService.getBySignRequest(signRequest.getId());
        logs = logs.stream().sorted(Comparator.comparing(Log::getLogDate).reversed()).collect(Collectors.toList());
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
        model.addAttribute("logs", logs);
        return "user/signrequests/show";
    }

    @PreAuthorize("@preAuthorizeService.signRequestSign(#id, #userEppn, #authUserEppn)")
    @PostMapping(value = "/refuse/{id}")
    public String refuse(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @RequestParam(value = "comment") String comment, RedirectAttributes redirectAttributes) throws EsupSignatureMailException, EsupSignatureRuntimeException {
        signBookService.refuse(id, comment, userEppn, authUserEppn);
        redirectAttributes.addFlashAttribute("message", new JsMessage("info", "La demandes a bien été refusée"));
        return "redirect:/otp/signrequests/" + id;
    }

    @PreAuthorize("@preAuthorizeService.signRequestRecipient(#id, #authUserEppn)")
    @PostMapping(value = "/add-attachment/{id}")
    public String addAttachement(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                                 @RequestParam(value = "multipartFiles", required = false) MultipartFile[] multipartFiles,
                                 @RequestParam(value = "link", required = false) String link,
                                 RedirectAttributes redirectAttributes) throws EsupSignatureIOException {
        logger.info("start add attachment");
        signRequestService.addAttachement(multipartFiles, link, id, authUserEppn);
        redirectAttributes.addFlashAttribute("message", new JsMessage("info", "La piece jointe a bien été ajoutée"));
        return "redirect:/otp/signrequests/" + id;
    }

    @PreAuthorize("@preAuthorizeService.attachmentCreator(#attachementId, #userEppn, #authUserEppn)")
    @DeleteMapping(value = "/remove-attachment/{id}/{attachementId}")
    public String removeAttachement(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @PathVariable("attachementId") Long attachementId, RedirectAttributes redirectAttributes) {
        logger.info("start remove attachment");
        signRequestService.removeAttachement(id, attachementId, redirectAttributes);
        redirectAttributes.addFlashAttribute("message", new JsMessage("info", "La pieces jointe a été supprimée"));
        return "redirect:/otp/signrequests/" + id;
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @DeleteMapping(value = "/remove-link/{id}/{linkId}")
    public String removeLink(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @PathVariable("linkId") Integer linkId, RedirectAttributes redirectAttributes) {
        logger.info("start remove link");
        signRequestService.removeLink(id, linkId);
        redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Le lien a été supprimé"));
        return "redirect:/user/signrequests/" + id;
    }

    @PreAuthorize("@preAuthorizeService.signRequestRecipient(#id, #userEppn)")
    @PostMapping(value = "/comment/{id}")
    public String comment(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
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
        if(signRequestService.addComment(id, comment, commentPageNumber, commentPosX, commentPosY, commentWidth, commentHeight, postit, spotStepNumber, authUserEppn, userEppn, forceSend) != null) {
            model.addAttribute("message", new JsMessage("success", "Annotation ajoutée"));
        } else {
            model.addAttribute("message", new JsMessage("error", "Ajout d'emplacement non autorisé"));
        }
        return "redirect:/otp/signrequests/" + id;
    }

}