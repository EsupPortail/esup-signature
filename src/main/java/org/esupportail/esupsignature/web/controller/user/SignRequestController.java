package org.esupportail.esupsignature.web.controller.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.esig.dss.validation.reports.Reports;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.entity.enums.UiParams;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.exception.*;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.export.SedaExportService;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.esupportail.esupsignature.service.security.otp.OtpService;
import org.esupportail.esupsignature.web.ws.json.JsonExternalUserInfo;
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpHeaders;
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
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/user/signrequests")
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
    private CertificatService certificatService;

    @Resource
    private ValidationService validationService;

    @Resource
    private PreAuthorizeService preAuthorizeService;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private DataService dataService;

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
    private CommentService commentService;

    @Resource
    private OtpService otpService;

    @Resource
    private TemplateEngine templateEngine;

    @Resource
    private SedaExportService sedaExportService;

    @GetMapping
    public String list(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
                       @RequestParam(value = "statusFilter", required = false) String statusFilter,
                       @RequestParam(value = "recipientsFilter", required = false) String recipientsFilter,
                       @RequestParam(value = "workflowFilter", required = false) String workflowFilter,
                       @RequestParam(value = "docTitleFilter", required = false) String docTitleFilter,
                       @SortDefault(value = "createDate", direction = Direction.DESC) @PageableDefault(size = 10) Pageable pageable, Model model) {
        if(statusFilter == null) statusFilter = "all";
        if(statusFilter.equals("all")) statusFilter = "";
        List<SignRequest> signRequests = signRequestService.getSignRequests(userEppn, authUserEppn, statusFilter, recipientsFilter, workflowFilter, docTitleFilter, pageable);
        model.addAttribute("statusFilter", statusFilter);
        model.addAttribute("signBooks", signRequestToSignBookPages(pageable, signRequests));
        model.addAttribute("statuses", SignRequestStatus.values());
        model.addAttribute("forms", formService.getFormsByUser(userEppn, authUserEppn));
        model.addAttribute("workflows", workflowService.getWorkflowsByUser(userEppn, authUserEppn));
        model.addAttribute("recipientsFilter", recipientsFilter);
        model.addAttribute("signRequestRecipients", signRequestService.getRecipientsNameFromSignRequests(signRequests));
        model.addAttribute("docTitleFilter", docTitleFilter);
        model.addAttribute("docTitles", new HashSet<>(signRequests.stream().map(SignRequest::getTitle).collect(Collectors.toList())));
        model.addAttribute("workflowFilter", workflowFilter);
        model.addAttribute("signRequestWorkflow", new HashSet<>(signRequests.stream().map(s -> s.getParentSignBook().getTitle()).collect(Collectors.toList())));
        return "user/signrequests/list";
    }

    public Page<SignBook> signRequestToSignBookPages(@PageableDefault(size = 10) @SortDefault(value = "createDate", direction = Direction.DESC) Pageable pageable,List<SignRequest> signRequests) {
        List<SignBook> signBooks = signRequests.stream().map(SignRequest::getParentSignBook).distinct().collect(Collectors.toList());
        return new PageImpl<>(signBooks.stream().skip(pageable.getOffset()).limit(pageable.getPageSize()).collect(Collectors.toList()), pageable, signBooks.size());
    }

    @GetMapping(value = "/list-ws")
    @ResponseBody
    public String listWs(@ModelAttribute(name = "userEppn") String userEppn, @ModelAttribute(name = "authUserEppn") String authUserEppn,
                         @RequestParam(value = "statusFilter", required = false) String statusFilter,
                         @RequestParam(value = "recipientsFilter", required = false) String recipientsFilter,
                         @RequestParam(value = "workflowFilter", required = false) String workflowFilter,
                         @RequestParam(value = "docTitleFilter", required = false) String docTitleFilter,
                         @SortDefault(value = "createDate", direction = Direction.DESC) @PageableDefault(size = 5) Pageable pageable, HttpServletRequest httpServletRequest, Model model) {
        List<SignRequest> signRequests = signRequestService.getSignRequests(userEppn, authUserEppn, statusFilter, recipientsFilter, workflowFilter, docTitleFilter, pageable);
        model.addAttribute("signBooks", signRequestToSignBookPages(pageable, signRequests));
        CsrfToken token = new HttpSessionCsrfTokenRepository().loadToken(httpServletRequest);
        final Context ctx = new Context(Locale.FRENCH);
        ctx.setVariables(model.asMap());
        ctx.setVariable("token", token);
        return templateEngine.process("user/signrequests/includes/list-elem.html", ctx);
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @GetMapping(value = "/{id}")
    public String show(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @RequestParam(required = false) Boolean frameMode, Model model, HttpSession httpSession, RedirectAttributes redirectAttributes) throws IOException, EsupSignatureException {
        SignRequest signRequest = signRequestService.getSignRequestsFullById(id, userEppn, authUserEppn);
//        if(signRequest.getStatus().equals(SignRequestStatus.deleted)) {
//            redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Demande supprimée"));
//            return "redirect:/user/";
//        }
        if (signRequest.getLastNotifDate() == null) {
            model.addAttribute("notifTime", 0);
        } else {
            model.addAttribute("notifTime", Duration.between(signRequest.getLastNotifDate().toInstant(), new Date().toInstant()).toHours());
        }
        model.addAttribute("signRequest", signRequest);
        model.addAttribute("workflow", signRequest.getParentSignBook().getLiveWorkflow().getWorkflow());
        model.addAttribute("postits", signRequest.getComments().stream().filter(Comment::getPostit).collect(Collectors.toList()));
        model.addAttribute("comments", signRequest.getComments().stream().filter(comment -> !comment.getPostit() && comment.getStepNumber() == null).collect(Collectors.toList()));
        model.addAttribute("spots", signRequest.getComments().stream().filter(comment -> comment.getStepNumber() != null).collect(Collectors.toList()));
        boolean attachmentRequire = false;
        if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep() != null
                && signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep().getAttachmentRequire() != null
                && signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep().getAttachmentRequire()
                && signRequest.getAttachments().size() == 0) {
            attachmentRequire = true;
        }
        model.addAttribute("attachmentRequire", attachmentRequire);
        model.addAttribute("currentSignType", signRequest.getCurrentSignType());
        model.addAttribute("currentStepNumber", signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber());
        if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() != null && signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep() != null) {
            model.addAttribute("currentStepId", signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep().getId());
            model.addAttribute("currentStepMultiSign", signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getMultiSign());
        }
        model.addAttribute("nbSignRequestInSignBookParent", signRequest.getParentSignBook().getSignRequests().size());
        List<Document> toSignDocuments = signRequestService.getToSignDocuments(signRequest.getId());
        if(toSignDocuments.size() == 1) {
            model.addAttribute("toSignDocument", toSignDocuments.get(0));
        }
        model.addAttribute("attachments", signRequestService.getAttachments(id));
        model.addAttribute("nextSignRequest", signRequestService.getNextSignRequest(signRequest.getId(), userEppn, authUserEppn));
        model.addAttribute("prevSignRequest", signRequestService.getPreviousSignRequest(signRequest.getId(), userEppn, authUserEppn));
        model.addAttribute("fields", signRequestService.prefillSignRequestFields(id, userEppn));
        model.addAttribute("toUseSignRequestParams", signRequestService.getToUseSignRequestParams(id, userEppn));
        model.addAttribute("uiParams", userService.getUiParams(authUserEppn));
        if(!signRequest.getStatus().equals(SignRequestStatus.draft)) {
            try {
                Object userShareString = httpSession.getAttribute("userShareId");
                Long userShareId = null;
                if(userShareString != null) userShareId = Long.valueOf(userShareString.toString());
                List<String> signImages = signRequestService.getSignImagesForSignRequest(signRequest, userEppn, authUserEppn, userShareId);
                model.addAttribute("signImages", signImages);
            } catch (EsupSignatureUserException e) {
                model.addAttribute("message", new JsonMessage("warn", e.getMessage()));
            }
        }
        Reports reports = validationService.validate(id);
        if(reports != null) {
            model.addAttribute("signatureIds", reports.getSimpleReport().getSignatureIdList());
        }
        model.addAttribute("certificats", certificatService.getCertificatByUser(userEppn));
        model.addAttribute("signable", signRequest.getSignable());
        model.addAttribute("editable", signRequest.getEditable());
        model.addAttribute("isNotSigned", signRequestService.isNotSigned(signRequest));
        model.addAttribute("isTempUsers", signRequestService.isTempUsers(id));
        if(signRequest.getStatus().equals(SignRequestStatus.draft)) {
            model.addAttribute("steps", workflowService.getWorkflowStepsFromSignRequest(signRequest, userEppn));
        }
        model.addAttribute("refuseLogs", logService.getRefuseLogs(signRequest.getId()));
        model.addAttribute("viewRight", signRequestService.checkUserViewRights(signRequest, userEppn, authUserEppn));
        model.addAttribute("frameMode", frameMode);
        if(signRequest.getData() != null && signRequest.getData().getForm() != null) {
            model.addAttribute("action", signRequest.getData().getForm().getAction());
            model.addAttribute("supervisors", signRequest.getData().getForm().getManagers());
        }
        List<Log> logs = logService.getBySignRequest(signRequest.getId());
        logs = logs.stream().sorted(Comparator.comparing(Log::getLogDate).reversed()).collect(Collectors.toList());
        if(signRequest.getSignable()
                && (signRequest.getParentSignBook().getLiveWorkflow() == null || signRequest.getParentSignBook().getLiveWorkflow().getWorkflow() == null ||userService.getUiParams(authUserEppn) == null || userService.getUiParams(authUserEppn).get(UiParams.workflowVisaAlert) == null || !Arrays.asList(userService.getUiParams(authUserEppn).get(UiParams.workflowVisaAlert).split(",")).contains(signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getId().toString()))
                && signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().equals(SignType.hiddenVisa)
                && (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber() > 1 || !signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getUsers().contains(signRequest.getCreateBy()))) {
            model.addAttribute("message", new JsonMessage("custom", "Vous êtes destinataire d'une demande de visa (et non de signature) sur ce document.\nSa validation implique que vous en acceptez le contenu.\nVous avez toujours la possibilité de ne pas donner votre accord en refusant cette demande de visa et en y adjoignant vos commentaires."));
            userService.setUiParams(authUserEppn, UiParams.workflowVisaAlert, signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getId().toString() + ",");

        }
        Data data = dataService.getBySignBook(signRequest.getParentSignBook());
        if(data != null && data.getForm() != null) {
            String message = formService.getHelpMessage(userEppn, data.getForm());
            if(message != null) {
                model.addAttribute("form", data.getForm());
                model.addAttribute("message", new JsonMessage("help", message));
            }
        }
        model.addAttribute("logs", logs);
        return "user/signrequests/show";
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @GetMapping(value = "/details/{id}")
    public String details(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, Model model) throws Exception {
        User user = (User) model.getAttribute("user");
        SignRequest signRequest = signRequestService.getById(id);
        model.addAttribute("signBooks", signBookService.getAllSignBooks());
        List<Log> logs = logService.getBySignRequest(signRequest.getId());
        logs = logs.stream().sorted(Comparator.comparing(Log::getLogDate).reversed()).collect(Collectors.toList());
        model.addAttribute("logs", logs);
        model.addAttribute("comments", logService.getLogs(signRequest.getId()));
        model.addAttribute("refuseLogs", logService.getRefuseLogs(signRequest.getId()));
        if (user.getKeystore() != null) {
            model.addAttribute("keystore", user.getKeystore().getFileName());
        }
        model.addAttribute("signRequest", signRequest);
        model.addAttribute("toSignDocument", signRequestService.getToSignDocuments(id).get(0));
        model.addAttribute("signable", signRequest.getSignable());
        model.addAttribute("editable", signRequest.getEditable());
        model.addAttribute("workflows", workflowService.getAllWorkflows());
        return "user/signrequests/details";
    }

    @PreAuthorize("@preAuthorizeService.signRequestSign(#id, #userEppn, #authUserEppn)")
    @ResponseBody
    @PostMapping(value = "/sign/{id}")
    public ResponseEntity<String> sign(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                               @RequestParam(value = "signRequestParams") String signRequestParamsJsonString,
                               @RequestParam(value = "comment", required = false) String comment,
                               @RequestParam(value = "formData", required = false) String formData,
                               @RequestParam(value = "visual", required = false) Boolean visual,
                               @RequestParam(value = "password", required = false) String password,
                               @RequestParam(value = "certType", required = false) String certType,
                                       HttpSession httpSession) {
        if (visual == null) visual = true;
        ObjectMapper objectMapper = new ObjectMapper();
        Object[] signRequestParamses = new Object[0];
        try {
            signRequestParamses = objectMapper.readValue(signRequestParamsJsonString, Object[].class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        if(signRequestParamses.length == 0 && visual) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Il manque une signature !");
        }
        Object userShareString = httpSession.getAttribute("userShareId");
        Long userShareId = null;
        if(userShareString != null) userShareId = Long.valueOf(userShareString.toString());
        try {
            signRequestService.initSign(id, signRequestParamsJsonString, comment, formData, visual, password, certType, userShareId, userEppn, authUserEppn);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            logger.warn("message", e);
            return ResponseEntity.status(HttpStatus.OK).body(e.getMessage());
        }
    }

    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserEppn)")
    @ResponseBody
    @PostMapping(value = "/add-docs/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object addDocumentToNewSignRequest(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @RequestParam("multipartFiles") MultipartFile[] multipartFiles) throws EsupSignatureIOException {
        logger.info("start add documents");
        SignRequest signRequest = signRequestService.getById(id);
        int i = 0;
        for (MultipartFile multipartFile : multipartFiles) {
            signRequestService.addDocsToSignRequest(signRequest, true, i, multipartFile);
            i++;
        }
        return new String[]{"ok"};
    }

    @ResponseBody
    @PostMapping(value = "/remove-doc/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String removeDocument(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id) throws JSONException {
        logger.info("remove document " + id);
        JSONObject result = new JSONObject();
        Document document = documentService.getById(id);
        SignRequest signRequest = signRequestService.getById(document.getParentId());
        if(signRequest.getCreateBy().getEppn().equals(authUserEppn)) {
            signRequest.getOriginalDocuments().remove(document);
        } else {
            result.put("error", "Non autorisé");
        }
        return result.toString();
    }

//    @GetMapping("/sign-by-token/{token}")
//    public String signByToken(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("token") String token) {
//        SignRequest signRequest = signRequestService.getSignRequestsByToken(token).get(0);
//        if (signRequestService.checkUserSignRights(user, authUser, signRequest)) {
//            return "redirect:/user/signrequests/" + signRequest.getId();
//        } else {
//            return "redirect:/";
//        }
//    }

    @PreAuthorize("@preAuthorizeService.notInShare(#userEppn, #authUserEppn)")
    @PostMapping(value = "/fast-sign-request")
    public String createSignRequest(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @RequestParam("multipartFiles") MultipartFile[] multipartFiles,
                                    @RequestParam("signType") SignType signType,
                                    HttpServletRequest request, Model model, RedirectAttributes redirectAttributes) {
        User user = (User) model.getAttribute("user");
        logger.info("création rapide demande de signature par " + user.getFirstname() + " " + user.getName());
        if (multipartFiles != null) {
            try {
                SignBook signBook = signBookService.addFastSignRequestInNewSignBook(multipartFiles, signType, user, authUserEppn);
                return "redirect:/user/signrequests/" + signBook.getSignRequests().get(0).getId();
            } catch (EsupSignatureException e) {
                redirectAttributes.addFlashAttribute("message", new JsonMessage("error", e.getMessage()));
                return "redirect:" + request.getHeader(HttpHeaders.REFERER);
            }
        } else {
            logger.warn("no file to import");
        }
        return "redirect:/user/signrequests";
    }

    @PreAuthorize("@preAuthorizeService.notInShare(#userEppn, #authUserEppn)")
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
                                  Model model, RedirectAttributes redirectAttributes) throws EsupSignatureIOException {
        User user = (User) model.getAttribute("user");
        User authUser = (User) model.getAttribute("authUser");
        recipientsEmails = recipientsEmails.stream().distinct().collect(Collectors.toList());
        logger.info(user.getEmail() + " envoi d'une demande de signature à " + recipientsEmails);
        List<JsonExternalUserInfo> externalUsersInfos = userService.getJsonExternalUserInfos(emails, names, firstnames, phones);
        if (multipartFiles != null) {
            try {
                Map<SignBook, String> signBookStringMap = signRequestService.sendSignRequest(multipartFiles, signType, allSignToComplete, userSignFirst, pending, comment, recipientsCCEmails, recipientsEmails, externalUsersInfos, user, authUser, false, forceAllSign);
                if (signBookStringMap.values().iterator().next() != null) {
                    redirectAttributes.addFlashAttribute("message", new JsonMessage("warn", signBookStringMap.values().toArray()[0].toString()));
                } else {
                    if(userSignFirst == null || !userSignFirst) {
                        redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Votre demande à bien été envoyée"));
                    }
                }
                long signRequestId = signBookStringMap.keySet().iterator().next().getSignRequests().get(0).getId();
                if(!signRequestService.checkTempUsers(signRequestId, recipientsEmails, externalUsersInfos)) {
                    redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Merci de compléter tous les utilisateurs externes"));
                }
                return "redirect:/user/signrequests/" + signRequestId;
            } catch (EsupSignatureException | MessagingException e) {
                redirectAttributes.addFlashAttribute("message", new JsonMessage("error", e.getMessage()));
            }
        } else {
            logger.warn("no file to import");
            redirectAttributes.addFlashAttribute("message", new JsonMessage("error","Pas de fichier à importer"));
        }
        return "redirect:/user/signrequests";
    }

    @PreAuthorize("@preAuthorizeService.signRequestSign(#id, #userEppn, #authUserEppn)")
    @PostMapping(value = "/refuse/{id}")
    public String refuse(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @RequestParam(value = "comment") String comment, RedirectAttributes redirectAttributes) throws EsupSignatureMailException {
        signRequestService.refuse(id, comment, userEppn, authUserEppn);
        redirectAttributes.addFlashAttribute("messageInfos", "La demandes à bien été refusée");
        return "redirect:/user/signrequests/" + id;
    }

    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserEppn)")
    @GetMapping(value = "/restore/{id}", produces = "text/html")
    public String restore(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletRequest httpServletRequest, RedirectAttributes redirectAttributes) {
        signRequestService.restore(id, authUserEppn);
        redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Restauration effectuée"));
        return "redirect:/user/signrequests/" + id;
    }

    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserEppn)")
    @DeleteMapping(value = "/{id}", produces = "text/html")
    public String delete(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletRequest httpServletRequest, RedirectAttributes redirectAttributes) {
        signRequestService.delete(id, authUserEppn);
        redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Suppression effectuée"));
        return "redirect:" + httpServletRequest.getHeader(HttpHeaders.REFERER);
    }

    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserEppn)")
    @DeleteMapping(value = "/force-delete/{id}", produces = "text/html")
    public String forceDelete(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        SignRequest signRequest = signRequestService.getById(id);
        if(signRequest.getParentSignBook().getSignRequests().size() > 1) {
            signRequestService.deleteDefinitive(id);
            redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Suppression effectuée"));
            return "redirect:/user/signbooks/" + signRequest.getParentSignBook().getId();

        } else {
            signBookService.deleteDefinitive(signRequest.getParentSignBook().getId());
            redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Suppression effectuée"));
            return "redirect:/user/";
        }
    }

    @PostMapping(value = "/delete-multiple", consumes = {"application/json"})
    @ResponseBody
    public ResponseEntity<Boolean> deleteMultiple(@ModelAttribute("authUserEppn") String authUserEppn, @RequestBody List<Long> ids, RedirectAttributes redirectAttributes) {
        for(Long id : ids) {
            if(preAuthorizeService.signBookManage(id, authUserEppn)) {
                signBookService.delete(id, authUserEppn);
            }
        }
        redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Suppression effectuée"));
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    @GetMapping(value = "/download-multiple", produces = "application/zip")
    @ResponseBody
    public void downloadMultiple(@ModelAttribute("authUserEppn") String authUserEppn, @RequestParam List<Long> ids, HttpServletResponse httpServletResponse) throws IOException {
        httpServletResponse.setContentType("application/zip");
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        httpServletResponse.setHeader("Content-Disposition", "attachment; filename=\"download.zip\"");
        signRequestService.getMultipleSignedDocuments(ids, httpServletResponse);
        httpServletResponse.flushBuffer();
    }

    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserEppn)")
    @PostMapping(value = "/add-attachment/{id}")
    public String addAttachement(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                                 @RequestParam(value = "multipartFiles", required = false) MultipartFile[] multipartFiles,
                                 @RequestParam(value = "link", required = false) String link,
                                 RedirectAttributes redirectAttributes) throws EsupSignatureIOException {
        logger.info("start add attachment");
        signRequestService.addAttachement(multipartFiles, link, id);
        redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "La piece jointe à bien été ajoutée"));
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
            Map<String, Object> attachmentResponse = signRequestService.getAttachmentResponse(id, attachementId);
            if (attachmentResponse != null) {
                httpServletResponse.setContentType(attachmentResponse.get("contentType").toString());
                httpServletResponse.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode(attachmentResponse.get("fileName").toString(), StandardCharsets.UTF_8.toString()));
                IOUtils.copyLarge((InputStream) attachmentResponse.get("inputStream"), httpServletResponse.getOutputStream());
            } else {
                redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Pièce jointe non trouvée ..."));
                httpServletResponse.sendRedirect("/user/signsignrequests/" + id);
            }
        } catch (Exception e) {
            logger.error("get file error", e);
        }
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @GetMapping(value = "/get-last-file/{id}")
    public ResponseEntity<Void> getLastFile(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletResponse httpServletResponse) {
        try {
            Map<String, Object> fileResponse = signRequestService.getToSignFileResponse(id);
            if(fileResponse != null) {
                httpServletResponse.setContentType(fileResponse.get("contentType").toString());
                httpServletResponse.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode(fileResponse.get("fileName").toString(), StandardCharsets.UTF_8.toString()));
                IOUtils.copyLarge((InputStream) fileResponse.get("inputStream"), httpServletResponse.getOutputStream());
            }
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            logger.error("get file error", e);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @PreAuthorize("@preAuthorizeService.signBookView(#id, #userEppn, #authUserEppn)")
    @GetMapping(value = "/get-last-files/{id}", produces = "application/zip")
    @ResponseBody
    public ResponseEntity<Void> getLastFiles(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletResponse httpServletResponse) throws IOException {
        httpServletResponse.setContentType("application/zip");
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        httpServletResponse.setHeader("Content-Disposition", "attachment; filename=\"download.zip\"");
        signRequestService.getMultipleSignedDocuments(Collections.singletonList(id), httpServletResponse);
        httpServletResponse.flushBuffer();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @GetMapping(value = "/get-last-file-report/{id}")
    public ResponseEntity<Void> getLastFileReport(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletResponse httpServletResponse) {
        try {
            signRequestService.getToSignFileReportResponse(id, httpServletResponse);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            logger.error("get file error", e);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @GetMapping(value = "/get-file/{id}")
    public ResponseEntity<Void> getFile(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletResponse httpServletResponse) throws IOException, SQLException, EsupSignatureFsException {
        Document document = documentService.getById(id);
        if(signRequestService.getById(document.getParentId()) != null) {
            if(preAuthorizeService.signRequestView(document.getParentId(), userEppn, authUserEppn)) {
                Map<String, Object> fileResponse = signRequestService.getFileResponse(id);
                if(fileResponse != null) {
                    httpServletResponse.setContentType(fileResponse.get("contentType").toString());
                    httpServletResponse.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode(fileResponse.get("fileName").toString(), StandardCharsets.UTF_8.toString()));
                    IOUtils.copyLarge((InputStream) fileResponse.get("inputStream"), httpServletResponse.getOutputStream());
                }
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                logger.warn(userEppn + " try access document " + id + " without permission");
            }
        } else {
            logger.warn("document is not present in signResquest");
        }
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserEppn)")
    @GetMapping(value = "/update-step/{id}/{step}")
    public String changeStepSignType(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @PathVariable("step") Integer step, @RequestParam(name = "signType") SignType signType) {
        SignRequest signRequest = signRequestService.getById(id);
        signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().setSignType(signType);
        return "redirect:/user/signrequests/" + id + "/?form";
    }

    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserEppn)")
    @GetMapping(value = "/complete/{id}")
    public String complete(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id) {
        signRequestService.completeSignRequest(id, userEppn, authUserEppn);
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
                          RedirectAttributes redirectAttributes) throws MessagingException, EsupSignatureException {
        List<JsonExternalUserInfo> externalUsersInfos = userService.getJsonExternalUserInfos(emails, names, firstnames, phones);
        if(!signRequestService.checkTempUsers(id, recipientEmails, externalUsersInfos)) {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Merci de compléter tous les utilisateurs externes"));
            return "redirect:/user/signrequests/" + id;
        }
        signBookService.initWorkflowAndPendingSignBook(id, recipientEmails, allSignToCompletes, externalUsersInfos, targetEmails, userEppn, authUserEppn);
        if(comment != null && !comment.isEmpty()) {
            signRequestService.addPostit(id, comment, userEppn, authUserEppn);
        }
        redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Votre demande à bien été transmise"));
        return "redirect:/user/signrequests/" + id;
    }

    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserEppn)")
    @PostMapping(value = "/add-step/{id}")
    public String addRecipients(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                                @RequestParam(value = "recipientsEmails", required = false) List<String> recipientsEmails,
                                @RequestParam(name = "signType") SignType signType,
                                @RequestParam(name = "allSignToComplete", required = false) Boolean allSignToComplete) throws EsupSignatureException {
        signRequestService.addStep(id, recipientsEmails, signType, allSignToComplete, authUserEppn);
        return "redirect:/user/signrequests/" + id + "/?form";
    }


    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #userEppn)")
    @PostMapping(value = "/comment/{id}")
    public String comment(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                          @RequestParam(value = "comment", required = false) String comment,
                          @RequestParam(value = "spotStepNumber", required = false) Integer spotStepNumber,
                          @RequestParam(value = "commentPageNumber", required = false) Integer commentPageNumber,
                          @RequestParam(value = "commentPosX", required = false) Integer commentPosX,
                          @RequestParam(value = "commentPosY", required = false) Integer commentPosY,
                          @RequestParam(value = "postit", required = false) String postit) {
        signRequestService.addComment(id, comment, commentPageNumber, commentPosX, commentPosY, postit, spotStepNumber, authUserEppn);
        return "redirect:/user/signrequests/" + id;
    }

    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserEppn)")
    @GetMapping(value = "/is-temp-users/{id}")
    @ResponseBody
    public List<User> isTempUsers(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                              @RequestParam(required = false) String recipientEmails) throws JsonProcessingException {
        SignRequest signRequest = signRequestService.getById(id);
        ObjectMapper objectMapper = new ObjectMapper();
        List<String> recipientList = objectMapper.readValue(recipientEmails, List.class);
        return userService.getTempUsers(signRequest, recipientList);
    }

    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserEppn)")
    @GetMapping(value = "/send-otp/{id}/{recipientId}")
    public String sendOtp(@ModelAttribute("authUserEppn") String authUserEppn,
                          @PathVariable("id") Long id,
                          @PathVariable("recipientId") Long recipientId,
                          RedirectAttributes redirectAttributes) throws Exception {
        User newUser = userService.getById(recipientId);
        if(newUser.getUserType().equals(UserType.external)) {
            otpService.generateOtpForSignRequest(id, newUser);
            redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Demande OTP envoyée"));
        } else {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Problème d'envoi OTP"));
        }
        return "redirect:/user/signrequests/" + id;
    }

    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserEppn)")
    @PostMapping(value = "/replay-notif/{id}")
    public String replayNotif(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,  RedirectAttributes redirectAttributes) throws EsupSignatureMailException {
        signRequestService.replayNotif(id);
        redirectAttributes.addFlashAttribute("message", new JsonMessage ("success", "Votre relance a bien été envoyée"));
        return "redirect:/user/signrequests/" + id;
    }

    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserEppn)")
    @DeleteMapping(value = "/delete-comment/{id}/{commentId}")
    public ResponseEntity<Void> deleteComments(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @PathVariable("commentId") Long commentId,  RedirectAttributes redirectAttributes) {
        commentService.deleteComment(commentId);
        redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Le commentaire à bien été supprimé"));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResponseBody
    @PostMapping(value = "/mass-sign")
    public ResponseEntity<String> massSign(@ModelAttribute("userEppn") String userEppn,
                                           @ModelAttribute("authUserEppn") String authUserEppn,
                                           @RequestParam String ids,
                                           @RequestParam(value = "password", required = false) String password,
                                           @RequestParam(value = "certType", required = false) String certType,
                                           HttpSession httpSession) throws InterruptedException, EsupSignatureMailException, EsupSignatureException, IOException {
        String error = signRequestService.initMassSign(userEppn, authUserEppn, ids, httpSession, password, certType);
        if(error == null) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @GetMapping(value = "/get-seda/{id}")
    public ResponseEntity<Void> getSeda(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletResponse httpServletResponse) throws IOException {
        SignRequest signRequest = signRequestService.getById(id);
        InputStream inputStream = sedaExportService.generateSip(id);
        httpServletResponse.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode(signRequest.getTitle() + ".zip", StandardCharsets.UTF_8.toString()));
        httpServletResponse.setContentType("application/zip");
        IOUtils.copy(inputStream, httpServletResponse.getOutputStream());
        return new ResponseEntity<>(HttpStatus.OK);
    }

}