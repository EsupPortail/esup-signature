package org.esupportail.esupsignature.web.controller.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.security.otp.OtpService;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.esupportail.esupsignature.web.controller.ws.json.JsonMessage;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RequestMapping("/user/signrequests")
@Controller

@EnableConfigurationProperties(GlobalProperties.class)
public class SignRequestController {

    private static final Logger logger = LoggerFactory.getLogger(SignRequestController.class);

    @ModelAttribute("activeMenu")
    public String getActiveMenu() {
        return "signrequests";
    }

    @Resource
    private UserService userService;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private FormService formService;

    @Resource
    private WorkflowService workflowService;

    @Resource
    private SignBookService signBookService;

    @Resource
    private LogService logService;

    @Resource
    private DocumentService documentService;

    @Resource
    private FileService fileService;

    @Resource
    private OtpService otpService;

    @Resource
    private TemplateEngine templateEngine;

    @Resource
    private LiveWorkflowStepService liveWorkflowStepService;

//
//    @Resource
//    private SedaExportService sedaExportService;

    @GetMapping
    public String list(@ModelAttribute(name = "userId") Long userId, @ModelAttribute(name = "authUserId") Long authUserId,
                       @RequestParam(value = "statusFilter", required = false) String statusFilter,
                       @SortDefault(value = "createDate", direction = Direction.DESC) @PageableDefault(size = 10) Pageable pageable, Model model) {
        model.addAttribute("statusFilter", statusFilter);
        model.addAttribute("signRequests", signRequestService.getSignRequestsPageGrouped(userId, authUserId, pageable));
        model.addAttribute("statuses", SignRequestStatus.values());
        model.addAttribute("forms", formService.getFormsByUser(userId, authUserId));
        model.addAttribute("workflows", workflowService.getWorkflowsByUser(userId, authUserId));
        return "user/signrequests/list";
    }

    @GetMapping(value = "/list-ws")
    @ResponseBody
    public String listWs(@ModelAttribute(name = "userId") Long userId, @ModelAttribute(name = "authUserId") Long authUserId,
                                    @RequestParam(value = "statusFilter", required = false) String statusFilter,
                                    @SortDefault(value = "createDate", direction = Direction.DESC) @PageableDefault(size = 5) Pageable pageable, HttpServletRequest httpServletRequest, Model model) {
        Page<SignRequest> signRequestPage = signRequestService.getSignRequestsPageGrouped(userId, authUserId, pageable);
        CsrfToken token = new HttpSessionCsrfTokenRepository().loadToken(httpServletRequest);
        final Context ctx = new Context(Locale.FRENCH);
        model.addAttribute("signRequests", signRequestPage);
        ctx.setVariables(model.asMap());
        ctx.setVariable("token", token);
        return templateEngine.process("user/signrequests/includes/list-elem.html", ctx);
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userId, #authUserId)")
    @GetMapping(value = "/{id}")
    public String show(@ModelAttribute("userId") Long userId, @ModelAttribute("authUserId") Long authUserId, @PathVariable("id") Long id, @RequestParam(required = false) Boolean frameMode, Model model) throws IOException, EsupSignatureException {
        User user = userService.getUserById(userId);
        SignRequest signRequest = signRequestService.getSignRequestsFullById(id, userId, authUserId);
        model.addAttribute("signRequest", signRequest);
        model.addAttribute("signable", signRequest.getSignable());
        model.addAttribute("currentSignType", signRequest.getCurrentSignType());
        model.addAttribute("nbSignRequestInSignBookParent", signRequest.getParentSignBook().getSignRequests().size());
        model.addAttribute("toSignDocument", signRequestService.getToSignDocuments(id).get(0));
        model.addAttribute("attachments", signRequestService.getAttachments(id));
        model.addAttribute("nextSignRequest", signRequestService.getNextSignRequest(signRequest, userId, authUserId));
        model.addAttribute("prevSignRequest", signRequestService.getPreviousSignRequest(signRequest, userId, authUserId));
        model.addAttribute("fields", signRequestService.prefillSignRequestFields(id, userId));
        try {
            model.addAttribute("signImages", signRequestService.getSignImageForSignRequest(id, userId, authUserId));
        } catch (EsupSignatureUserException e) {
            model.addAttribute("message", new JsonMessage("warn", e.getMessage()));
        }
        model.addAttribute("isTempUsers", userService.isTempUsers(signRequest));
        model.addAttribute("steps", getWorkflowStepsFromSignRequest(signRequest, user));
        model.addAttribute("refuseLogs", logService.getRefuseLogs(signRequest.getId()));
        model.addAttribute("comments", logService.getLogs(signRequest.getId()));
        model.addAttribute("globalPostits", logService.getGlobalLogs(signRequest.getId()));
        model.addAttribute("viewRight", signRequestService.checkUserViewRights(signRequest, user, authUserId));
        model.addAttribute("frameMode", frameMode);
        return "user/signrequests/show";
    }

    public List<WorkflowStep> getWorkflowStepsFromSignRequest(SignRequest signRequest, User user) throws EsupSignatureException {
        List<WorkflowStep> workflowSteps = new ArrayList<>();
        if(signRequest.getParentSignBook().getLiveWorkflow().getWorkflow() != null) {
            Workflow workflow = workflowService.computeWorkflow(workflowService.getWorkflowByName(signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getName()), null, user, true);
            workflowSteps.addAll(workflow.getWorkflowSteps());
        }
        return workflowSteps;
    }


    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userId, #authUserId)")
    @GetMapping(value = "/details/{id}")
    public String details(@ModelAttribute("userId") Long userId, @ModelAttribute("authUserId") Long authUserId, @PathVariable("id") Long id, Model model) throws Exception {
        User user = userService.getUserById(userId);
        User authUser = userService.getUserById(authUserId);
        SignRequest signRequest = signRequestService.getById(id);
        model.addAttribute("signBooks", signBookService.getAllSignBooks());
        List<Log> logs = logService.getById(signRequest.getId());
        logs = logs.stream().sorted(Comparator.comparing(Log::getLogDate).reversed()).collect(Collectors.toList());
        model.addAttribute("logs", logs);
        model.addAttribute("comments", logService.getLogs(signRequest.getId()));
        List<Log> refuseLogs = logService.getRefuseLogs(signRequest.getId());
        model.addAttribute("refuseLogs", refuseLogs);
        if (user.getSignImages().size() > 0 && user.getSignImages().get(0) != null) {
            model.addAttribute("signFile", fileService.getBase64Image(user.getSignImages().get(0)));
        }
        if (user.getKeystore() != null) {
            model.addAttribute("keystore", user.getKeystore().getFileName());
        }
        model.addAttribute("signRequest", signRequest);

        if (signRequest.getStatus().equals(SignRequestStatus.pending) && signRequestService.checkUserSignRights(signRequest, userId, authUserId) && signRequest.getOriginalDocuments().size() > 0) {
            signRequest.setSignable(true);
        }
        model.addAttribute("signTypes", SignType.values());
        model.addAttribute("workflows", workflowService.getAllWorkflows());
        return "user/signrequests/details";

    }

    @PreAuthorize("@preAuthorizeService.signRequestSign(#id, #userId, #authUserId)")
    @ResponseBody
    @PostMapping(value = "/sign/{id}")
    public ResponseEntity<String> sign(@ModelAttribute("userId") Long userId, @ModelAttribute("authUserId") Long authUserId, @PathVariable("id") Long id,
                               @RequestParam(value = "sseId") String sseId,
                               @RequestParam(value = "signRequestParams") String signRequestParamsJsonString,
                               @RequestParam(value = "comment", required = false) String comment,
                               @RequestParam(value = "formData", required = false) String formData,
                               @RequestParam(value = "visual", required = false) Boolean visual,
                               @RequestParam(value = "password", required = false) String password) {
        if (visual == null) visual = true;
        if(signRequestService.initSign(id, sseId, signRequestParamsJsonString, comment, formData, visual, password, userId, authUserId)) {
            new ResponseEntity<>(HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserId)")
    @ResponseBody
    @PostMapping(value = "/add-docs/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object addDocumentToNewSignRequest(@ModelAttribute("authUserId") Long authUserId, @PathVariable("id") Long id, @RequestParam("multipartFiles") MultipartFile[] multipartFiles) throws EsupSignatureIOException {
        logger.info("start add documents");
        SignRequest signRequest = signRequestService.getById(id);
        for (MultipartFile multipartFile : multipartFiles) {
            signRequestService.addDocsToSignRequest(signRequest, multipartFile);
        }
        return new String[]{"ok"};
    }

    @ResponseBody
    @PostMapping(value = "/remove-doc/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String removeDocument(@ModelAttribute("authUserId") Long authUserId, @PathVariable("id") Long id) throws JSONException {
        User authUser = userService.getUserById(authUserId);
        logger.info("remove document " + id);
        JSONObject result = new JSONObject();
        Document document = documentService.getById(id);
        SignRequest signRequest = signRequestService.getById(document.getParentId());
        if(signRequest.getCreateBy().equals(authUser)) {
            signRequest.getOriginalDocuments().remove(document);
        } else {
            result.put("error", "Non autorisé");
        }
        return result.toString();
    }

//    @GetMapping("/sign-by-token/{token}")
//    public String signByToken(@ModelAttribute("userId") Long userId, @ModelAttribute("authUserId") Long authUserId, @PathVariable("token") String token) {
//        SignRequest signRequest = signRequestService.getSignRequestsByToken(token).get(0);
//        if (signRequestService.checkUserSignRights(user, authUser, signRequest)) {
//            return "redirect:/user/signrequests/" + signRequest.getId();
//        } else {
//            return "redirect:/";
//        }
//    }

    @PreAuthorize("@preAuthorizeService.notInShare(#userId, #authUserId)")
    @PostMapping(value = "/fast-sign-request")
    public String createSignRequest(@ModelAttribute("userId") Long userId, @ModelAttribute("authUserId") Long authUserId, @RequestParam("multipartFiles") MultipartFile[] multipartFiles,
                                    @RequestParam("signType") SignType signType,
                                    HttpServletRequest request, RedirectAttributes redirectAttributes) {
        User user = userService.getUserById(userId);
        User authUser = userService.getUserById(authUserId);
        logger.info("création rapide demande de signature par " + user.getFirstname() + " " + user.getName());
        if (multipartFiles != null) {
            try {
                SignBook signBook = signBookService.addFastSignRequestInNewSignBook(user, multipartFiles, signType, authUser);
                return "redirect:/user/signrequests/" + signBook.getSignRequests().get(0).getId();
            } catch (EsupSignatureException e) {
                redirectAttributes.addFlashAttribute("message", new JsonMessage("error", e.getMessage()));
                return "redirect:" + request.getHeader("Referer");
            }
        } else {
            logger.warn("no file to import");
        }
        return "redirect:/user/signrequests";
    }

    @PreAuthorize("@preAuthorizeService.notInShare(#userId, #authUserId)")
    @PostMapping(value = "/send-sign-request")
    public String sendSignRequest(@ModelAttribute("userId") Long userId, @ModelAttribute("authUserId") Long authUserId, @RequestParam("multipartFiles") MultipartFile[] multipartFiles,
                                  @RequestParam(value = "recipientsEmails", required = false) String[] recipientsEmails,
                                  @RequestParam(name = "allSignToComplete", required = false) Boolean allSignToComplete,
                                  @RequestParam(name = "userSignFirst", required = false) Boolean userSignFirst,
                                  @RequestParam(value = "pending", required = false) Boolean pending,
                                  @RequestParam(value = "comment", required = false) String comment,
                                  @RequestParam("signType") SignType signType, RedirectAttributes redirectAttributes) throws EsupSignatureIOException {
        User user = userService.getUserById(userId);
        User authUser = userService.getUserById(authUserId);
        logger.info(user.getEmail() + " envoi d'une demande de signature à " + Arrays.toString(recipientsEmails));
        if (multipartFiles != null) {
            try {
                SignBook signBook = signBookService.addDocsInNewSignBookSeparated("", "Demande simple", multipartFiles, user);
                String message = sendSignRequest(user, signBook, recipientsEmails, allSignToComplete, userSignFirst, pending, comment, signType, authUser);
                if (message != null) {
                    redirectAttributes.addFlashAttribute("message", new JsonMessage("warn", message));
                } else {
                    redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Votre demande à bien été envoyée"));
                }
                return "redirect:/user/signrequests/" + signBook.getSignRequests().get(0).getId();
            } catch (EsupSignatureException e) {
                redirectAttributes.addFlashAttribute("message", new JsonMessage("error", e.getMessage()));
            }
        } else {
            logger.warn("no file to import");
            redirectAttributes.addFlashAttribute("message", new JsonMessage("error","Pas de fichier à importer"));
        }
        return "redirect:/user/signrequests";
    }

    public String sendSignRequest(User user, SignBook signBook, String[] recipientsEmails, Boolean allSignToComplete, Boolean userSignFirst, Boolean pending, String comment, SignType signType, User authUser) throws EsupSignatureException {
        String message = null;
        if (allSignToComplete == null) {
            allSignToComplete = false;
        }
        try {
            if(userSignFirst != null && userSignFirst) {
                signBook.getLiveWorkflow().getWorkflowSteps().add(liveWorkflowStepService.createWorkflowStep(false, SignType.pdfImageStamp, user.getEmail()));
            }
            signBook.getLiveWorkflow().getWorkflowSteps().add(liveWorkflowStepService.createWorkflowStep(allSignToComplete, signType, recipientsEmails));
            signBook.getLiveWorkflow().setCurrentStep(signBook.getLiveWorkflow().getWorkflowSteps().get(0));
        } catch (EsupSignatureUserException e) {
            logger.error("error with users on create signbook " + signBook.getId());
            throw new EsupSignatureException("Problème lors de l’envoi");
        }
        if(userService.getTempUsersFromRecipientList(Arrays.asList(recipientsEmails)) . size() > 0) {
            pending = false;
            message = "La liste des destinataires contient des personnes externes.<br>Après vérification, vous devez confirmer l'envoi pour finaliser la demande";
        }
        if (pending != null && pending) {
            signBookService.pendingSignBook(signBook, user, authUser);
            if (comment != null && !comment.isEmpty()) {
                for (SignRequest signRequest : signBook.getSignRequests()) {
                    signRequest.setComment(comment);
                    signRequestService.updateStatus(signRequest, signRequest.getStatus(), "comment", "SUCCES", null, null, null, 0, user, authUser);
                }
            }
        } else {
            message = "Après vérification, vous devez confirmer l'envoi pour finaliser la demande";
        }
        return message;
    }

    @PreAuthorize("@preAuthorizeService.signRequestSign(#id, #userId, #authUserId)")
    @PostMapping(value = "/refuse/{id}")
    public String refuse(@ModelAttribute("userId") Long userId, @ModelAttribute("authUserId") Long authUserId, @PathVariable("id") Long id, @RequestParam(value = "comment") String comment, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        User user = userService.getUserById(userId);
        User authUser = userService.getUserById(authUserId);
        SignRequest signRequest = signRequestService.getById(id);
        signRequest.setComment(comment);
        signRequestService.refuse(signRequest, user, authUser);
        redirectAttributes.addFlashAttribute("messageInfos", "La demandes à bien été refusée");
        return "redirect:/user/signrequests/" + signRequest.getId();
    }

    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserId)")
    @DeleteMapping(value = "/{id}", produces = "text/html")
    public String delete(@ModelAttribute("authUserId") Long authUserId, @PathVariable("id") Long id, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        SignRequest signRequest = signRequestService.getById(id);
        if(signRequestService.delete(signRequest)) {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Suppression effectuée"));
        } else {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Suppression interdite"));
        }
        return "redirect:" + request.getHeader("referer");
    }

//    @PostMapping(value = "delete-multiple", consumes = {"application/json"})
//    @ResponseBody
//    public ResponseEntity<Boolean> deleteMultiple(@ModelAttribute("authUserId") Long authUserId, @RequestBody List<Long> ids, RedirectAttributes redirectAttributes) {
//        for(Long id : ids) {
//            SignBook signBook = signBookService.getSignBookById(id);
//            if(signBook != null) {
//                if(signBookService.preAuthorizeManage(id, authUser)) {
//                    signBookService.delete(signBook);
//                }
//            } else if(signRequestService.getSignRequestsById(id) != null) {
//                SignRequest signRequest = signRequestService.getSignRequestsById(id);
//                if (signRequestService.preAuthorizeOwner(id, authUser)) {
//                    signRequestService.delete(signRequest);
//                }
//            }
//        }
//        redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Suppressions effectuées"));
//        redirectAttributes.addAttribute("messageInfo", "Suppressions effectuées");
//        return new ResponseEntity<>(true, HttpStatus.OK);
//    }

    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserId)")
    @PostMapping(value = "/add-attachment/{id}")
    public String addAttachement(@ModelAttribute("authUserId") Long authUserId, @PathVariable("id") Long id,
                                 @RequestParam(value = "multipartFiles", required = false) MultipartFile[] multipartFiles,
                                 @RequestParam(value = "link", required = false) String link,
                                 RedirectAttributes redirectAttributes) throws EsupSignatureIOException {
        logger.info("start add attachment");
        SignRequest signRequest = signRequestService.getById(id);
        signRequestService.addAttachement(multipartFiles, link, signRequest);
        redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "La pieces jointe à bien été ajoutée"));
        return "redirect:/user/signrequests/" + id;
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userId, #authUserId)")
    @GetMapping(value = "/remove-attachment/{id}/{attachementId}")
    public String removeAttachement(@ModelAttribute("userId") Long userId, @ModelAttribute("authUserId") Long authUserId, @PathVariable("id") Long id, @PathVariable("attachementId") Long attachementId, RedirectAttributes redirectAttributes) {
        logger.info("start remove attachment");
        signRequestService.removeAttachement(id, attachementId, redirectAttributes);
        redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "La pieces jointe a été supprimée"));
        return "redirect:/user/signrequests/" + id;
    }



    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userId, #authUserId)")
    @GetMapping(value = "/remove-link/{id}/{linkId}")
    public String removeLink(@ModelAttribute("userId") Long userId, @ModelAttribute("authUserId") Long authUserId, @PathVariable("id") Long id, @PathVariable("linkId") Integer linkId, RedirectAttributes redirectAttributes) {
        logger.info("start remove link");
        signRequestService.removeLink(id, linkId);
        redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Le lien a été supprimé"));
        return "redirect:/user/signrequests/" + id;
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userId, #authUserId)")
    @GetMapping(value = "/get-attachment/{id}/{attachementId}")
    public void getAttachment(@ModelAttribute("userId") Long userId, @ModelAttribute("authUserId") Long authUserId, @PathVariable("id") Long id, @PathVariable("attachementId") Long attachementId, HttpServletResponse httpServletResponse, RedirectAttributes redirectAttributes) {
        try {
            Map<String, Object> attachmentResponse = signRequestService.getAttachmentResponse(id, attachementId);
            if (attachmentResponse != null) {
                httpServletResponse.setContentType(attachmentResponse.get("contentType").toString());
                httpServletResponse.setHeader("Content-disposition", "inline; filename=" + URLEncoder.encode(attachmentResponse.get("fileName").toString(), StandardCharsets.UTF_8.toString()));
                IOUtils.copyLarge((InputStream) attachmentResponse.get("inputStream"), httpServletResponse.getOutputStream());
            } else {
                redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Pièce jointe non trouvée ..."));
                httpServletResponse.sendRedirect("/user/signsignrequests/" + id);
            }
        } catch (Exception e) {
            logger.error("get file error", e);
        }
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userId, #authUserId)")
    @GetMapping(value = "/get-last-file/{id}")
    public ResponseEntity<Void> getLastFile(@ModelAttribute("userId") Long userId, @ModelAttribute("authUserId") Long authUserId, @PathVariable("id") Long id, HttpServletResponse httpServletResponse) {
        try {
            Map<String, Object> fileResponse = signRequestService.getToSignFileResponse(id);
            httpServletResponse.setContentType(fileResponse.get("contentType").toString());
            httpServletResponse.setHeader("Content-disposition", "inline; filename=" + URLEncoder.encode(fileResponse.get("fileName").toString(), StandardCharsets.UTF_8.toString()));
            IOUtils.copyLarge((InputStream) fileResponse.get("inputStream"), httpServletResponse.getOutputStream());
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            logger.error("get file error", e);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userId, #authUserId)")
    @GetMapping(value = "/get-file/{id}")
    public ResponseEntity<Void> getFile(@ModelAttribute("userId") Long userId, @ModelAttribute("authUserId") Long authUserId, @PathVariable("id") Long id, HttpServletResponse httpServletResponse) throws IOException {
        Document document = documentService.getById(id);
        if(signRequestService.getById(document.getParentId()) != null) {
            httpServletResponse.setHeader("Content-disposition", "inline; filename=" + URLEncoder.encode(document.getFileName(), StandardCharsets.UTF_8.toString()));
            httpServletResponse.setContentType(document.getContentType());
            IOUtils.copy(document.getInputStream(), httpServletResponse.getOutputStream());
            return new ResponseEntity<>(HttpStatus.OK);        }
        logger.warn("document is not present in signResquest");
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserId)")
    @GetMapping(value = "/update-step/{id}/{step}")
    public String changeStepSignType(@ModelAttribute("authUserId") Long authUserId, @PathVariable("id") Long id, @PathVariable("step") Integer step, @RequestParam(name = "signType") SignType signType) {
        SignRequest signRequest = signRequestService.getById(id);
        signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().setSignType(signType);
        return "redirect:/user/signrequests/" + id + "/?form";
    }

    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserId)")
    @GetMapping(value = "/complete/{id}")
    public String complete(@ModelAttribute("userId") Long userId, @ModelAttribute("authUserId") Long authUserId, @PathVariable("id") Long id) {
        User user = userService.getUserById(userId);
        User authUser = userService.getUserById(authUserId);
        signRequestService.completeSignRequest(id, user, authUser);
        return "redirect:/user/signrequests/" + id + "/?form";
    }

    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserId)")
    @GetMapping(value = "/pending/{id}")
    public String pending(@ModelAttribute("userId") Long userId, @ModelAttribute("authUserId") Long authUserId, @PathVariable("id") Long id,
                          @RequestParam(required = false) List<String> recipientEmails,
                          @RequestParam(value = "comment", required = false) String comment,
                          @RequestParam(value = "names", required = false) String[] names,
                          @RequestParam(value = "firstnames", required = false) String[] firstnames,
                          @RequestParam(value = "phones", required = false) String[] phones,
                          RedirectAttributes redirectAttributes) throws MessagingException, EsupSignatureException {
        User user = userService.getUserById(userId);
        User authUser = userService.getUserById(authUserId);
        if(!signRequestService.checkTempUsers(id, recipientEmails, names, firstnames, phones)) {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Merci de compléter tous les utilisateurs externes"));
            return "redirect:/user/signrequests/" + id;
        }
        SignRequest signRequest = signRequestService.getById(id);
        signBookService.initWorkflowAndPendingSignBook(signRequest.getParentSignBook(), recipientEmails, user, authUser);
        if(comment != null && !comment.isEmpty()) {
            signRequestService.addPostit(signRequest, comment, user, authUser);
        }
        redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Votre demande à bien été transmise"));
        return "redirect:/user/signrequests/" + id;
    }

    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserId)")
    @PostMapping(value = "/add-step/{id}")
    public String addRecipients(@ModelAttribute("authUserId") Long authUserId, @PathVariable("id") Long id,
                                @RequestParam(value = "recipientsEmails", required = false) String[] recipientsEmails,
                                @RequestParam(name = "signType") SignType signType,
                                @RequestParam(name = "allSignToComplete", required = false) Boolean allSignToComplete) {
        signRequestService.addStep(id, recipientsEmails, signType, allSignToComplete);
        return "redirect:/user/signrequests/" + id + "/?form";
    }


    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userId, #authUserId)")
    @PostMapping(value = "/comment/{id}")
    public String comment(@ModelAttribute("userId") Long userId, @ModelAttribute("authUserId") Long authUserId, @PathVariable("id") Long id,
                          @RequestParam(value = "comment", required = false) String comment,
                          @RequestParam(value = "commentPageNumber", required = false) Integer commentPageNumber,
                          @RequestParam(value = "commentPosX", required = false) Integer commentPosX,
                          @RequestParam(value = "commentPosY", required = false) Integer commentPosY) {
        User authUser = userService.getUserById(authUserId);
        signRequestService.addComment(id, comment, commentPageNumber, commentPosX, commentPosY, authUser);
        return "redirect:/user/signrequests/" + id;
    }

    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserId)")
    @GetMapping(value = "/is-temp-users/{id}")
    @ResponseBody
    public List<User> isTempUsers(@ModelAttribute("authUserId") Long authUserId, @PathVariable("id") Long id,
                              @RequestParam(required = false) String recipientEmails) throws JsonProcessingException {
        SignRequest signRequest = signRequestService.getById(id);
        ObjectMapper objectMapper = new ObjectMapper();
        List<String> recipientList = objectMapper.readValue(recipientEmails, List.class);
        return userService.getTempUsers(signRequest, recipientList);
    }

    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserId)")
    @GetMapping(value = "/send-otp/{id}/{recipientId}")
    public String sendOtp(@ModelAttribute("authUserId") Long authUserId,
                          @PathVariable("id") Long id,
                          @PathVariable("recipientId") Long recipientId,
                          RedirectAttributes redirectAttributes) throws Exception {
        User newUser = userService.getUserById(recipientId);
        if(newUser.getUserType().equals(UserType.external)) {
            otpService.generateOtpForSignRequest(id, newUser);
            redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Demande OTP envoyée"));
        } else {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Problème d'envoi OTP"));
        }
        return "redirect:/user/signrequests/" + id;
    }

}