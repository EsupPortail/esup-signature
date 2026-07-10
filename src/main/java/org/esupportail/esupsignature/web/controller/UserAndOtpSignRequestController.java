package org.esupportail.esupsignature.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dto.projection.jpa.AttachmentProjectionDto;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.esupportail.esupsignature.service.ui.UiFetchSignRequestService;
import org.esupportail.esupsignature.dto.page.user.signrequest.ShowSignRequestDto;
import org.esupportail.esupsignature.dto.page.user.signrequest.ShowSignRequestContextDto;
import org.esupportail.esupsignature.dto.page.user.signrequest.SignUiFrontDto;
import org.esupportail.esupsignature.dto.ui.global.SignatureUiConfigDto;
import org.esupportail.esupsignature.dto.ui.global.UiMessageDto;
import org.esupportail.esupsignature.dto.ws.WorkflowStepDto;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.*;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.ui.UiFetchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
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
import java.util.*;
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
    private final UiFetchSignRequestService uiFetchSignRequestService;
    private final PreAuthorizeService preAuthorizeService;
    private final MobileSignTokenService mobileSignTokenService;
    private final DataService dataService;
    private final ObjectMapper objectMapper;

    private Object getLock(String authUserEppn) {
        return userLocks.computeIfAbsent(authUserEppn, k -> new Object());
    }

    public UserAndOtpSignRequestController(SignRequestService signRequestService, CommentService commentService, UserService userService, GlobalProperties globalProperties, SignBookService signBookService, UiFetchService uiFetchService, UiFetchSignRequestService uiFetchSignRequestService, PreAuthorizeService preAuthorizeService, MobileSignTokenService mobileSignTokenService, DataService dataService, ObjectMapper objectMapper) {
        this.signRequestService = signRequestService;
        this.commentService = commentService;
        this.userService = userService;
        this.globalProperties = globalProperties;
        this.signBookService = signBookService;
        this.uiFetchService = uiFetchService;
        this.uiFetchSignRequestService = uiFetchSignRequestService;
        this.preAuthorizeService = preAuthorizeService;
        this.mobileSignTokenService = mobileSignTokenService;
        this.dataService = dataService;
        this.objectMapper = objectMapper;
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
        ShowSignRequestContextDto context = uiFetchSignRequestService.buildShowSignRequestContext(id, userEppn, authUserEppn, httpSession, isOtpView);
        if(context.getSignImagesWarningMessage() != null) {
            model.addAttribute("message", new UiMessageDto("warn", context.getSignImagesWarningMessage()));
        }

        if(context.isSignable()
                && context.getWorkflowId() != null && userService.getUiParams(authUserEppn) != null
                && (userService.getUiParams(authUserEppn).get(UiParams.workflowVisaAlert) == null || !Arrays.asList(userService.getUiParams(authUserEppn).get(UiParams.workflowVisaAlert).split(",")).contains(context.getWorkflowId().toString()))
                && SignType.hiddenVisa.equals(context.getCurrentStepSignType())) {
            model.addAttribute("message", new UiMessageDto("custom", "Vous êtes destinataire d'une demande de visa (et non de signature) sur ce document.\nSa validation implique que vous en acceptez le contenu.\nVous avez toujours la possibilité de ne pas donner votre accord en refusant cette demande de visa et en y adjoignant vos commentaires."));
            userService.setUiParams(authUserEppn, UiParams.workflowVisaAlert, context.getWorkflowId().toString() + ",");
        }
        ShowSignRequestDto showSignRequest = context.getShowSignRequest();
        SignatureUiConfigDto signatureUiConfig = SignatureUiConfigDto.fromGlobalProperties(globalProperties);
        model.addAttribute("favoriteSignRequestParamsJson", favoriteSignRequestParamsJson);
        model.addAttribute("signatureUiConfig", signatureUiConfig);
        model.addAttribute("signatureUiConfigJson", objectMapper.writeValueAsString(signatureUiConfig));
        model.addAttribute("originalDocumentsJson", objectMapper.writeValueAsString(showSignRequest.getOriginalDocuments()));
        model.addAttribute("signedDocumentsJson", objectMapper.writeValueAsString(showSignRequest.getSignedDocuments()));
        model.addAttribute("signImagesJson", objectMapper.writeValueAsString(showSignRequest.signRequestFull().getSignImages()));
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
        ShowSignRequestContextDto context = uiFetchSignRequestService.buildShowSignRequestContext(id, userEppn, authUserEppn, httpSession, isOtpView);
        SignUiFrontDto frontDto = context.getSignUiFront();
        return ResponseEntity.ok(frontDto);
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @GetMapping(value = "/{id}/form-action.js", produces = "application/javascript")
    @ResponseBody
    public ResponseEntity<String> getFormActionScript(@ModelAttribute("userEppn") String userEppn,
                                                      @ModelAttribute("authUserEppn") String authUserEppn,
                                                      @PathVariable("id") Long id) {
        String action = dataService.getFormActionBySignRequestId(id);
        logger.debug("Serving form action script signRequestId={} actionLength={}", id, action.length());
        String script = "console.debug(\"Executing form action script\", {signRequestId: " + id + ", actionLength: " + action.length() + "});\n" + action;
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/javascript"))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(script);
    }

    @PreAuthorize("@preAuthorizeService.signRequestRecipientAndViewers(#id, #userEppn)")
    @PostMapping(value = "/postit/{id}")
    public Object postit(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                         @RequestParam(value = "comment", required = false) String comment,
                         @RequestParam(value = "postit", required = false) String postit,
                         @RequestParam(value = "forceSend", required = false, defaultValue = "false") Boolean forceSend, Model model, HttpServletRequest httpServletRequest) {
        boolean ajaxRequest = isAjaxRequest(httpServletRequest);
        Long commentId = null;
        try {
            commentId = signRequestService.addComment(id, comment, null, null, null, null, null, postit, null, authUserEppn, userEppn, forceSend);
        } catch (EsupSignatureException e) {
            if (ajaxRequest) {
                return internalServerErrorResponse("Problème lors de l'ajout du post-it");
            }
            model.addAttribute("message", new UiMessageDto("error", "Problème lors de l'ajout du post-it"));
        }
        if(commentId != null) {
            if (ajaxRequest) {
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("success", true);
                response.put("message", "Post-it ajouté");
                response.put("postit", toPostitResponse(commentService.getById(commentId), id, userEppn));
                return ResponseEntity.ok(response);
            }
            model.addAttribute("message", new UiMessageDto("success", "Post-it ajouté"));
        } else {
            if (ajaxRequest) {
                return badRequestResponse("Problème lors de l'ajout du post-it");
            }
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
            redirectAttributes.addFlashAttribute("message", new UiMessageDto("success", "Postit modifié"));

        } catch (EsupSignatureException e) {
            redirectAttributes.addFlashAttribute("message", new UiMessageDto("error", e.getMessage()));
        }
        String path = httpServletRequest.getRequestURI();
        String basePath = path.startsWith("/otp") ? "/otp/signrequests/" : "/user/signrequests/";
        return "redirect:" + basePath + signRequestId;
    }

    // AJAX-friendly endpoint to update a postit via fetch (avoid relying on _method override)
    @PreAuthorize("@preAuthorizeService.commentCreator(#postitId, #userEppn)")
    @PostMapping(value = "/comment-ajax/{signRequestId}/update/{postitId}", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> commentUpdateAjax(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
                                                                  @PathVariable("signRequestId") Long signRequestId,
                                                                  @PathVariable("postitId") Long postitId,
                                                                  @RequestParam(value = "comment", required = false) String comment) {
        Map<String, Object> response = new LinkedHashMap<>();
        try {
            commentService.updateComment(signRequestId, postitId, comment);
            response.put("success", true);
            response.put("message", "Postit modifié");
            return ResponseEntity.ok(response);
        } catch (EsupSignatureException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
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

    // AJAX-friendly endpoint to delete a postit via fetch (avoid relying on _method override)
    @PreAuthorize("@preAuthorizeService.commentCreator(#postitId, #userEppn)")
    @PostMapping(value = "/comment-ajax/{signRequestId}/delete/{postitId}", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> commentDeleteAjax(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
                                                                  @PathVariable("signRequestId") Long signRequestId,
                                                                  @PathVariable("postitId") Long postitId) {
        Map<String, Object> response = new LinkedHashMap<>();
        try {
            commentService.deletePostit(signRequestId, postitId);
            response.put("success", true);
            response.put("message", "Postit supprimé");
            return ResponseEntity.ok(response);
        } catch (EsupSignatureException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PreAuthorize("@preAuthorizeService.signRequestRecipient(#id, #authUserEppn)")
    @PostMapping(value = "/add-attachment/{id}")
    public Object addAttachement(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                                 @RequestParam(value = "multipartFiles", required = false) MultipartFile[] multipartFiles,
                                 @RequestParam(value = "link", required = false) String link,
                                 RedirectAttributes redirectAttributes, HttpServletRequest httpServletRequest) {
        logger.info("start add attachment");
        boolean ajaxRequest = isAjaxRequest(httpServletRequest);
        Set<Long> attachmentIdsBefore = signRequestService.getAttachmentProjections(id).stream()
                .map(AttachmentProjectionDto::getId)
                .filter(Objects::nonNull)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        Set<String> linksBefore = new HashSet<>(signRequestService.getById(id).getLinks());
        try {
            if(StringUtils.hasText(link)) {
                new URI(link);
            }
            if(signRequestService.addAttachement(multipartFiles, link, id, authUserEppn)) {
                if (ajaxRequest) {
                    return ResponseEntity.ok(buildAddAttachmentResponse(id, attachmentIdsBefore, linksBefore, link));
                }
                redirectAttributes.addFlashAttribute("message", new UiMessageDto("info", "La piece jointe a bien été ajoutée"));
            } else {
                if (ajaxRequest) {
                    return badRequestResponse("Aucune pièce jointe n'a été ajoutée. Merci de contrôler la validité du document");
                }
                redirectAttributes.addFlashAttribute("message", new UiMessageDto("error", "Aucune pièce jointe n'a été ajoutée. Merci de contrôle la validité du document"));
            }
        } catch (EsupSignatureIOException e) {
            logger.warn("error adding attachment", e);
            if (ajaxRequest) {
                return internalServerErrorResponse(e.getMessage());
            }
            redirectAttributes.addFlashAttribute("message", new UiMessageDto("error", e.getMessage()));
        } catch (Exception e) {
            if (ajaxRequest) {
                return badRequestResponse("Le lien fourni n'est pas valide");
            }
            redirectAttributes.addFlashAttribute("message", new UiMessageDto("error", "Le lien fourni n'est pas valide"));
        }
        return "redirect:" + getBasePath(httpServletRequest) + id + "?attachment=true";
    }

    @PreAuthorize("@preAuthorizeService.attachmentCreator(#attachementId, #userEppn, #authUserEppn)")
    @DeleteMapping(value = "/remove-attachment/{id}/{attachementId}")
    public Object removeAttachement(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @PathVariable("attachementId") Long attachementId, RedirectAttributes redirectAttributes, HttpServletRequest httpServletRequest) {
        logger.info("start remove attachment");
        boolean ajaxRequest = isAjaxRequest(httpServletRequest);
        try {
            signRequestService.removeAttachement(id, attachementId);
            if (ajaxRequest) {
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("success", true);
                response.put("message", "La pièce jointe a été supprimée");
                response.put("attachmentId", attachementId);
                return ResponseEntity.ok(response);
            }
            redirectAttributes.addFlashAttribute("message", new UiMessageDto("info", "La pieces jointe a été supprimée"));
        } catch (EsupSignatureRuntimeException e) {
            if (ajaxRequest) {
                return badRequestResponse(e.getMessage());
            }
            redirectAttributes.addFlashAttribute("message", new UiMessageDto("error", e.getMessage()));
        }
        return "redirect:" + getBasePath(httpServletRequest) + id + "?attachment=true";
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @DeleteMapping(value = "/remove-link/{id}/{linkId}")
    public Object removeLink(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @PathVariable("linkId") Integer linkId, RedirectAttributes redirectAttributes, HttpServletRequest httpServletRequest) {
        logger.info("start remove link");
        boolean ajaxRequest = isAjaxRequest(httpServletRequest);
        try {
            signRequestService.removeLink(id, linkId);
            if (ajaxRequest) {
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("success", true);
                response.put("message", "Le lien a été supprimé");
                response.put("links", new ArrayList<>(signRequestService.getById(id).getLinks()));
                return ResponseEntity.ok(response);
            }
            redirectAttributes.addFlashAttribute("message", new UiMessageDto("info", "Le lien a été supprimé"));
        } catch (EsupSignatureRuntimeException e) {
            if (ajaxRequest) {
                return badRequestResponse(e.getMessage());
            }
            redirectAttributes.addFlashAttribute("message", new UiMessageDto("error", e.getMessage()));
        }
        return "redirect:" + getBasePath(httpServletRequest) + id + "?attachment=true";
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

    @PreAuthorize("@preAuthorizeService.signRequestManager(#id, #userEppn)")
    @PostMapping(value = "/add-spot/{id}")
    @ResponseBody
    public ResponseEntity<String> addSpot(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                                          @RequestParam(value = "spotStepNumber", required = false) Integer spotStepNumber,
                                          @RequestParam(value = "commentPageNumber", required = false) Integer commentPageNumber,
                                          @RequestParam(value = "commentPosX", required = false) Integer commentPosX,
                                          @RequestParam(value = "commentPosY", required = false) Integer commentPosY,
                                          @RequestParam(value = "commentScale", required = false, defaultValue = "1") Float commentScale,
                                          @RequestParam(value = "recipientId", required = false) Long recipientId) {
        Long spotId;
        try {
            spotId = signRequestService.addSpot(id, commentPageNumber, commentPosX, commentPosY, Math.round(200 * commentScale), Math.round(100 * commentScale), spotStepNumber, recipientId);
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
                if(preAuthorizeService.checkUserViewRights(signRequestId, authUserEppn, authUserEppn)) {
                    String path = httpServletRequest.getRequestURI();
                    String basePath = path.startsWith("/otp") ? "/otp-access/transferred" : "/user/signrequests/" + signRequestId;
                    return "redirect:" + basePath;
                } else {
                    redirectAttributes.addFlashAttribute("message", new UiMessageDto("info", "Demande transférée, vous n'y avez plus accès"));
                    return "redirect:/user";
                }
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

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @GetMapping(value = "/{id}/generate-mobile-token")
    @ResponseBody
    public ResponseEntity<Map<String, String>> generateToken(@ModelAttribute("userEppn") String userEppn,
                                                             @ModelAttribute("authUserEppn") String authUserEppn,
                                                             @PathVariable("id") Long id) {
        try {
            String token = mobileSignTokenService.createToken(authUserEppn);
            String mobileSignUrl = buildMobileSignUrl(token);

            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("url", mobileSignUrl);
            response.put("qrcodeUrl", buildMobileSignQrCodeUrl(token));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error generating mobile sign token: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String buildMobileSignUrl(String token) {
        String baseUrl = globalProperties.getRootUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "http://" + globalProperties.getDomain();
        }
        // Make sure baseUrl doesn't end with /
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/public/mobile-sign/" + token;
    }

    private String buildMobileSignQrCodeUrl(String token) {
        return "/public/mobile-sign/" + token + "/qrcode.png";
    }

    private boolean isAjaxRequest(HttpServletRequest httpServletRequest) {
        return "XMLHttpRequest".equalsIgnoreCase(httpServletRequest.getHeader("X-Requested-With"));
    }

    private String getBasePath(HttpServletRequest httpServletRequest) {
        String path = httpServletRequest.getRequestURI();
        return path.startsWith("/otp") ? "/otp/signrequests/" : "/user/signrequests/";
    }

    private ResponseEntity<Map<String, Object>> badRequestResponse(String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("message", message);
        return ResponseEntity.badRequest().body(response);
    }

    private ResponseEntity<Map<String, Object>> internalServerErrorResponse(String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("message", message);
        return ResponseEntity.internalServerError().body(response);
    }

    private Map<String, Object> buildAddAttachmentResponse(Long id, Set<Long> attachmentIdsBefore, Set<String> linksBefore, String submittedLink) {
        List<AttachmentProjectionDto> attachments = signRequestService.getAttachmentProjections(id);
        SignRequest signRequest = signRequestService.getById(id);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "La pièce jointe a bien été ajoutée");

        List<Map<String, Object>> addedAttachments = new ArrayList<>();
        for (AttachmentProjectionDto attachment : attachments) {
            if (attachment.getId() != null && !attachmentIdsBefore.contains(attachment.getId())) {
                addedAttachments.add(toAttachmentResponse(attachment));
            }
        }
        response.put("addedAttachments", addedAttachments);

        List<String> links = new ArrayList<>(signRequest.getLinks());
        response.put("links", links);
        if (StringUtils.hasText(submittedLink) && !linksBefore.contains(submittedLink)) {
            int addedLinkIndex = links.indexOf(submittedLink);
            if (addedLinkIndex >= 0) {
                Map<String, Object> addedLink = new LinkedHashMap<>();
                addedLink.put("index", addedLinkIndex);
                addedLink.put("value", submittedLink);
                response.put("addedLink", addedLink);
            }
        }
        return response;
    }

    private Map<String, Object> toAttachmentResponse(AttachmentProjectionDto attachment) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", attachment.getId());
        response.put("fileName", attachment.getFileName());
        if (attachment.getCreateByEppn() != null || attachment.getCreateByFirstname() != null || attachment.getCreateByName() != null) {
            Map<String, Object> createBy = new LinkedHashMap<>();
            createBy.put("eppn", attachment.getCreateByEppn());
            createBy.put("firstname", attachment.getCreateByFirstname());
            createBy.put("name", attachment.getCreateByName());
            response.put("createBy", createBy);
        }
        return response;
    }

    private Map<String, Object> toPostitResponse(Comment comment, Long signRequestId, String userEppn) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", comment.getId());
        response.put("text", comment.getText());
        response.put("postitColor", comment.getPostitColor());
        response.put("refuse", Boolean.TRUE.equals(comment.getRefuse()));
        response.put("createDate", comment.getCreateDate() != null ? comment.getCreateDate().getTime() : null);
        response.put("canEdit", comment.getCreateBy() != null && Objects.equals(comment.getCreateBy().getEppn(), userEppn) && !Boolean.TRUE.equals(comment.getRefuse()));
        response.put("signRequestId", signRequestId);
        if (comment.getCreateBy() != null) {
            Map<String, Object> createBy = new LinkedHashMap<>();
            createBy.put("eppn", comment.getCreateBy().getEppn());
            createBy.put("firstname", comment.getCreateBy().getFirstname());
            createBy.put("name", comment.getCreateBy().getName());
            response.put("createBy", createBy);
        }
        return response;
    }

}
