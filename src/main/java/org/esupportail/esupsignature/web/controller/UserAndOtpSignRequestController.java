package org.esupportail.esupsignature.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dto.page.user.signrequest.ShowSignRequestDto;
import org.esupportail.esupsignature.dto.page.user.signrequest.SignUiFrontDto;
import org.esupportail.esupsignature.dto.ui.global.UiMessageDto;
import org.esupportail.esupsignature.dto.ws.WorkflowStepDto;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.*;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.dto.mapper.UiFetchService;
import org.esupportail.esupsignature.dto.mapper.UiFetchService.ShowSignRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequestMapping(path = {"/user/signrequests", "/otp/signrequests"})
@EnableConfigurationProperties(GlobalProperties.class)
public class UserAndOtpSignRequestController {

    private static final Logger logger = LoggerFactory.getLogger(UserAndOtpSignRequestController.class);

    private final SignRequestService signRequestService;
    private final CommentService commentService;
    private final UserService userService;
    private final GlobalProperties globalProperties;
    private final SignBookService signBookService;
    private final UiFetchService uiFetchService;

    private final Map<String, Object> userLocks = new ConcurrentHashMap<>();
    private Object getLock(String authUserEppn) {
        return userLocks.computeIfAbsent(authUserEppn, k -> new Object());
    }

    public UserAndOtpSignRequestController(SignRequestService signRequestService, CommentService commentService, UserService userService, GlobalProperties globalProperties, SignBookService signBookService, UiFetchService uiFetchService) {
        this.signRequestService = signRequestService;
        this.commentService = commentService;
        this.userService = userService;
        this.globalProperties = globalProperties;
        this.signBookService = signBookService;
        this.uiFetchService = uiFetchService;
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @GetMapping(value = "/{id}")
    public String show(@ModelAttribute("userEppn") String userEppn,
                       @ModelAttribute("authUserEppn") String authUserEppn,
                       @PathVariable("id") Long id,
                       Model model, HttpSession httpSession, HttpServletRequest httpServletRequest) throws IOException, EsupSignatureRuntimeException {
        String path = httpServletRequest.getRequestURI();
        boolean isOtpView = path.startsWith("/otp");
        String favoriteSignRequestParamsJson = userService.getFavoriteSignRequestParamsJson(userEppn);
        ShowSignRequestContext context = uiFetchService.buildShowSignRequestContext(id, userEppn, authUserEppn, httpSession, isOtpView);
        Workflow workflow = context.workflow();
        LiveWorkflowStep currentStep = context.currentStep();

        if(context.signImagesWarningMessage() != null) {
            model.addAttribute("message", new UiMessageDto("warn", context.signImagesWarningMessage()));
        }

        if(context.signable()
                && workflow != null && userService.getUiParams(authUserEppn) != null
                && (userService.getUiParams(authUserEppn).get(UiParams.workflowVisaAlert) == null || !Arrays.asList(userService.getUiParams(authUserEppn).get(UiParams.workflowVisaAlert).split(",")).contains(workflow.getId().toString()))
                && currentStep != null && currentStep.getSignType().equals(SignType.hiddenVisa)) {
            model.addAttribute("message", new UiMessageDto("custom", "Vous êtes destinataire d'une demande de visa (et non de signature) sur ce document.\nSa validation implique que vous en acceptez le contenu.\nVous avez toujours la possibilité de ne pas donner votre accord en refusant cette demande de visa et en y adjoignant vos commentaires."));
            userService.setUiParams(authUserEppn, UiParams.workflowVisaAlert, workflow.getId().toString() + ",");
        }
        ShowSignRequestDto showSignRequest = uiFetchService.buildShowSignRequestBackDto(context);
        model.addAttribute("favoriteSignRequestParamsJson", favoriteSignRequestParamsJson);
        model.addAttribute("showSignRequest", showSignRequest);
        model.addAttribute("signRequestFull", showSignRequest.signRequestFull());
        model.addAttribute("signRequestLight", showSignRequest.signRequestLight());
        return "user/signrequests/show";
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @GetMapping(value = "/{id}/front", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<SignUiFrontDto> getFront(@ModelAttribute("userEppn") String userEppn,
                                                   @ModelAttribute("authUserEppn") String authUserEppn,
                                                   @PathVariable("id") Long id,
                                                   HttpSession httpSession,
                                                   HttpServletRequest httpServletRequest) throws IOException {
        String path = httpServletRequest.getRequestURI();
        boolean isOtpView = path.startsWith("/otp");
        ShowSignRequestContext context = uiFetchService.buildShowSignRequestContext(id, userEppn, authUserEppn, httpSession, isOtpView);
        SignUiFrontDto frontDto = uiFetchService.buildSignUiFrontDto(context);
        return ResponseEntity.ok(frontDto);
    }

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
            model.addAttribute("message", new UiMessageDto("error", "Problème lors de l'ajout du post-it"));
        }
        if(commentId != null) {
            model.addAttribute("message", new UiMessageDto("success", "Post-it ajouté"));
        } else {
            model.addAttribute("message", new UiMessageDto("error", "Problème lors de l'ajout du post-it"));
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
            redirectAttributes.addFlashAttribute("message", new UiMessageDto("success", "Postit modifiée"));

        } catch (EsupSignatureException e) {
            redirectAttributes.addFlashAttribute("message", new UiMessageDto("error", e.getMessage()));
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
            redirectAttributes.addFlashAttribute("message", new UiMessageDto("info", "Postit supprimé"));

        } catch (EsupSignatureException e) {
            redirectAttributes.addFlashAttribute("message", new UiMessageDto("error", e.getMessage()));
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
                redirectAttributes.addFlashAttribute("message", new UiMessageDto("info", "La piece jointe a bien été ajoutée"));
            } else {
                redirectAttributes.addFlashAttribute("message", new UiMessageDto("error", "Aucune pièce jointe n'a été ajoutée. Merci de contrôle la validité du document"));
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", new UiMessageDto("error", "Le lien fourni n'est pas valide"));
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
        redirectAttributes.addFlashAttribute("message", new UiMessageDto("info", "La pieces jointe a été supprimée"));
        String path = httpServletRequest.getRequestURI();
        String basePath = path.startsWith("/otp") ? "/otp/signrequests/" : "/user/signrequests/";
        return "redirect:" + basePath + id + "?attachment=true";
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @DeleteMapping(value = "/remove-link/{id}/{linkId}")
    public String removeLink(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @PathVariable("linkId") Integer linkId, RedirectAttributes redirectAttributes, HttpServletRequest httpServletRequest) {
        logger.info("start remove link");
        signRequestService.removeLink(id, linkId);
        redirectAttributes.addFlashAttribute("message", new UiMessageDto("info", "Le lien a été supprimé"));
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
                    redirectAttributes.addFlashAttribute("message", new UiMessageDto("error", "Pièce jointe non trouvée ..."));
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
                    redirectAttributes.addFlashAttribute("message", new UiMessageDto("error", "Pièce jointe non trouvée ..."));
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
            redirectAttributes.addFlashAttribute("message", new UiMessageDto("error", "Opération interdite"));
            return "redirect:/user/signrequests/" + signRequestId;
        }
        if(keepFollow == null) keepFollow = false;
        try {
            signBookService.transfertSignRequest(signRequestId, authUserEppn, transfertRecipientsEmails, phones, names, firstnames, keepFollow);
            redirectAttributes.addFlashAttribute("message", new UiMessageDto("success", "Demande transférée"));
            if(keepFollow) {
                return "redirect:/user/signrequests/" + signRequestId;
            } else {
                String path = httpServletRequest.getRequestURI();
                String basePath = path.startsWith("/otp") ? "/otp-access/transferred" : "/user/signrequests/" + signRequestId;
                return "redirect:" + basePath;
            }
        } catch (EsupSignatureRuntimeException e) {
            redirectAttributes.addFlashAttribute("message", new UiMessageDto("error", "Demande non transférée : " + e.getMessage()));
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