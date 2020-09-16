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
import org.esupportail.esupsignature.repository.*;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.file.FileService;
import org.esupportail.esupsignature.service.fs.FsFile;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.esupportail.esupsignature.service.prefill.PreFillService;
import org.esupportail.esupsignature.service.security.otp.OtpService;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@RequestMapping("/user/signrequests")
@Controller
@Transactional
@EnableConfigurationProperties(GlobalProperties.class)
public class SignRequestController {

    private static final Logger logger = LoggerFactory.getLogger(SignRequestController.class);

    @ModelAttribute("activeMenu")
    public String getActiveMenu() {
        return "signrequests";
    }

    @Resource
    private GlobalProperties globalProperties;

    @Resource
    private UserService userService;

    @Resource
    private UserRepository userRepository;

    @Resource
    private PreFillService preFillService;

    @Resource
    private SignRequestRepository signRequestRepository;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private SignRequestParamsRepository signRequestParamsRepository;

    @Resource
    private DataRepository dataRepository;

    @Resource
    private FormService formService;

    @Resource
    private WorkflowRepository workflowRepository;

    @Resource
    private WorkflowService workflowService;

    @Resource
    private SignBookService signBookService;

    @Resource
    private SignBookRepository signBookRepository;

    @Resource
    private LogRepository logRepository;

    @Resource
    private DocumentRepository documentRepository;

    @Resource
    private PdfService pdfService;

    @Resource
    private FileService fileService;

    @Resource
    private OtpService otpService;
//
//    @Resource
//    private SedaExportService sedaExportService;

    @GetMapping
    public String list(@ModelAttribute(value = "user", binding = false) User user, @ModelAttribute(value = "authUser", binding = false) User authUser,
                       @RequestParam(value = "statusFilter", required = false) String statusFilter,
                       @RequestParam(value = "signBookId", required = false) Long signBookId,
                       @RequestParam(value = "messageError", required = false) String messageError,
                       @SortDefault(value = "createDate", direction = Direction.DESC) @PageableDefault(size = 10) Pageable pageable, Model model) {
        if(user.equals(authUser) || userService.getSignShare(user, authUser)) {
            List<SignRequest> signRequests;
            if (statusFilter != null) {
                if (statusFilter.equals("tosign")) {
                    signRequests = signRequestService.getToSignRequests(user);
                } else if (statusFilter.equals("signedByMe")) {
                    signRequests = signRequestService.getSignRequestsSignedByUser(user);
                } else if (statusFilter.equals("refusedByMe")) {
                    signRequests = signRequestService.getSignRequestsRefusedByUser(user);
                } else if (statusFilter.equals("sharedSign")) {
                    signRequests = signRequestService.getSignRequestsSharedSign(user);
                } else {
                    signRequests = signRequestRepository.findByCreateByAndStatus(user, SignRequestStatus.valueOf(statusFilter));
                }
                model.addAttribute("statusFilter", statusFilter);
            } else {
                signRequests = signRequestRepository.findByCreateBy(user);
                for(SignRequest signRequest : signRequestService.getToSignRequests(user)) {
                    if(!signRequests.contains(signRequest)) {
                        signRequests.add(signRequest);
                    }
                }
                for(SignRequest signRequest : signRequestService.getSignRequestsSignedByUser(user)) {
                    if(!signRequests.contains(signRequest)) {
                        signRequests.add(signRequest);
                    }
                }
                for(SignRequest signRequest : signRequestService.getSignRequestsRefusedByUser(user)) {
                    if(!signRequests.contains(signRequest)) {
                        signRequests.add(signRequest);
                    }
                }
            }

            model.addAttribute("signRequests", signRequestService.getSignRequestsPageGrouped(signRequests, pageable));
            if (user.getKeystore() != null) {
                model.addAttribute("keystore", user.getKeystore().getFileName());
            }
        } else {
            model.addAttribute("signRequests", signRequestService.getSignRequestsPageGrouped(new ArrayList<>(), pageable));
        }

        model.addAttribute("mydocs", "active");
        model.addAttribute("signBookId", signBookId);
        model.addAttribute("statuses", SignRequestStatus.values());
        model.addAttribute("messageError", messageError);
        model.addAttribute("forms", formService.getFormsByUser(user, authUser));
        model.addAttribute("workflows", workflowService.getWorkflowsForUser(user, authUser));
        return "user/signrequests/list";
    }

    @PreAuthorize("@signRequestService.preAuthorizeOwner(#id, #user)")
    @GetMapping(value = "/send-otp/{id}/{recipientId}")
    public String sendOtp(@ModelAttribute("user") User user,
                          @PathVariable("id") Long id,
                          @PathVariable("recipientId") Long recipientId,
                          RedirectAttributes redirectAttributes) throws Exception {
        SignRequest signRequest = signRequestRepository.findById(id).get();
        User newUser = userRepository.findById(recipientId).get();
        if(newUser.getUserType().equals(UserType.external)) {
            otpService.generateOtpForSignRequest(signRequest, newUser);
            redirectAttributes.addFlashAttribute("messageSuccess", "Demande OTP envoyée");
        } else {
            redirectAttributes.addFlashAttribute("messageError", "Problème d'envoi OTP");
        }
        return "redirect:/user/signrequests/" + id;
    }

    @PreAuthorize("@signRequestService.preAuthorizeView(#id, #user, #authUser)")
    @GetMapping(value = "/{id}")
    public String show(@ModelAttribute("user") User user, @ModelAttribute("authUser") User authUser, @PathVariable("id") Long id, @RequestParam(required = false) Boolean frameMode, Model model) throws Exception {
        SignRequest signRequest = signRequestRepository.findById(id).get();
        if (signRequest.getStatus().equals(SignRequestStatus.pending)
                && signRequestService.checkUserSignRights(user, signRequest) && signRequest.getOriginalDocuments().size() > 0
                && signRequestService.needToSign(signRequest, user)) {
            signRequest.setSignable(true);
            model.addAttribute("currentSignType", signRequestService.getCurrentSignType(signRequest));
            model.addAttribute("nexuUrl", globalProperties.getNexuUrl());
            model.addAttribute("nexuVersion", globalProperties.getNexuVersion());
            model.addAttribute("baseUrl", globalProperties.getNexuDownloadUrl());
        }
        if(signRequest.getParentSignBook() != null && dataRepository.countBySignBook(signRequest.getParentSignBook()) > 0) {
            Data data = dataRepository.findBySignBook(signRequest.getParentSignBook()).get(0);
            if(data != null && data.getForm() != null) {
                List<Field> fields = data.getForm().getFields();
                List<Field> prefilledFields = preFillService.getPreFilledFieldsByServiceName(data.getForm().getPreFillType(), fields, user);
                for (Field field : prefilledFields) {
                    if(!field.getStepNumbers().contains(signRequest.getCurrentStepNumber().toString())) {
                        field.setDefaultValue("");
                    }
                    if(data.getDatas().get(field.getName()) != null && !data.getDatas().get(field.getName()).isEmpty()) {
                        field.setDefaultValue(data.getDatas().get(field.getName()));
                    }
                }
                model.addAttribute("fields", prefilledFields);
            }
        }
        if (signRequest.getSignedDocuments().size() > 0 || signRequest.getOriginalDocuments().size() > 0) {
            List<Document> toSignDocuments = signRequestService.getToSignDocuments(signRequest);
            if (toSignDocuments.size() == 1 && toSignDocuments.get(0).getContentType().equals("application/pdf")) {
                Document toDisplayDocument = signRequestService.getToSignDocuments(signRequest).get(0);
                if (user.getSignImages().size() >  0 && user.getSignImages().get(0) != null && user.getSignImages().get(0).getSize() > 0) {
                    if(signRequestService.checkUserSignRights(user, signRequest) && user.getKeystore() == null && signRequest.getSignType().equals(SignType.certSign)) {
                        signRequest.setSignable(false);
                        model.addAttribute("messageWarn", "Pour signer ce document merci d’ajouter un certificat à votre profil");
                    }
                    List<String> signImages = new ArrayList<>();
                    for(Document signImage : user.getSignImages()) {
                        signImages.add(fileService.getBase64Image(signImage));
                    }
                    model.addAttribute("signImages", signImages);
                    int[] size = pdfService.getSignSize(user.getSignImages().get(0).getInputStream());
                    model.addAttribute("signWidth", size[0]);
                    model.addAttribute("signHeight", size[1]);
                } else {
                    if(signRequest.getSignable() && signRequest.getSignType() != null && (signRequest.getSignType().equals(SignType.pdfImageStamp) || signRequest.getSignType().equals(SignType.certSign))) {
                        model.addAttribute("messageWarn", "Pour signer ce document merci d'ajouter une image de votre signature dans <a href='user/users'>Mes paramètres</a>");
                        signRequest.setSignable(false);
                    }
                }
                model.addAttribute("documentType", fileService.getExtension(toDisplayDocument.getFileName()));
            } else {
                if(signRequest.getSignType() != null && (signRequest.getSignType().equals(SignType.certSign) || signRequest.getSignType().equals(SignType.nexuSign))) {
                    signRequest.setSignable(true);
                }
                model.addAttribute("documentType", "other");
            }

        } else if (signRequestService.getLastSignedFsFile(signRequest) != null) {
            FsFile fsFile = signRequestService.getLastSignedFsFile(signRequest);
            model.addAttribute("documentType", fileService.getExtension(fsFile.getName()));
        }
        boolean isTempUsers = false;
        if(signRequestService.getTempUsers(signRequest).size() > 0) {
            isTempUsers = true;
        }
        List<Log> refuseLogs = logRepository.findBySignRequestIdAndFinalStatus(signRequest.getId(), SignRequestStatus.refused.name());
        model.addAttribute("isTempUsers", isTempUsers);
        model.addAttribute("refuseLogs", refuseLogs);
        model.addAttribute("postits", logRepository.findBySignRequestIdAndPageNumberIsNotNull(signRequest.getId()));
        List<Log> globalPostits =logRepository.findBySignRequestIdAndStepNumberIsNotNull(signRequest.getId());
        model.addAttribute("globalPostits", globalPostits);
        model.addAttribute("signRequest", signRequest);
        model.addAttribute("viewRight", signRequestService.checkUserViewRights(user, signRequest));
        signRequestService.setStep("");
        if (frameMode != null && frameMode) {
            return "user/signrequests/show-frame";
        } else {
            return "user/signrequests/show";
        }
    }

    @PreAuthorize("@signRequestService.preAuthorizeView(#id, #user, #authUser)")
    @GetMapping(value = "/{id}", params = "form")
    public String updateForm(@ModelAttribute("user") User user, @ModelAttribute("authUser") User authUser, @PathVariable("id") Long id, Model model) throws Exception {
        SignRequest signRequest = signRequestRepository.findById(id).get();
        model.addAttribute("signBooks", signBookService.getAllSignBooks());
        List<Log> logs = logRepository.findBySignRequestId(signRequest.getId());
        logs = logs.stream().sorted(Comparator.comparing(Log::getLogDate).reversed()).collect(Collectors.toList());
        model.addAttribute("logs", logs);
        model.addAttribute("comments", logs.stream().filter(log -> log.getComment() != null && !log.getComment().isEmpty()).collect(Collectors.toList()));
        List<Log> refuseLogs = logRepository.findBySignRequestIdAndFinalStatus(signRequest.getId(), SignRequestStatus.refused.name());
        model.addAttribute("refuseLogs", refuseLogs);
        if (user.getSignImages().get(0) != null) {
            model.addAttribute("signFile", fileService.getBase64Image(user.getSignImages().get(0)));
        }
        if (user.getKeystore() != null) {
            model.addAttribute("keystore", user.getKeystore().getFileName());
        }
        model.addAttribute("signRequest", signRequest);

        if (signRequest.getStatus().equals(SignRequestStatus.pending) && signRequestService.checkUserSignRights(user, signRequest) && signRequest.getOriginalDocuments().size() > 0) {
            signRequest.setSignable(true);
        }
        model.addAttribute("signTypes", SignType.values());
        model.addAttribute("workflows", workflowRepository.findAll());
        return "user/signrequests/update";

    }


    @PreAuthorize("@signRequestService.preAuthorizeSign(#id, #user)")
    @ResponseBody
    @PostMapping(value = "/sign/{id}")
    public ResponseEntity sign(@ModelAttribute("user") User user, @PathVariable("id") Long id,
                               @RequestParam(value = "signRequestParams") String signRequestParamsJsonString,
                               @RequestParam(value = "comment", required = false) String comment,
                               @RequestParam(value = "formData", required = false) String formData,
                               @RequestParam(value = "visual", required = false) Boolean visual,
                               @RequestParam(value = "password", required = false) String password) throws JsonProcessingException {
        if (visual == null) visual = true;
        ObjectMapper objectMapper = new ObjectMapper();
        SignRequest signRequest = signRequestRepository.findById(id).get();

        Map<String, String> formDataMap = null;
        if(formData != null) {
            try {
                formDataMap = objectMapper.readValue(formData, Map.class);
                formDataMap.remove("_csrf");
                if(signRequest.getParentSignBook() != null && dataRepository.countBySignBook(signRequest.getParentSignBook()) > 0) {
                    Data data = dataRepository.findBySignBook(signRequest.getParentSignBook()).get(0);
                    List<Field> fields = data.getForm().getFields();
                    for(Map.Entry<String, String> entry : formDataMap.entrySet()) {
                        List<Field> formfields = fields.stream().filter(f -> f.getName().equals(entry.getKey())).collect(Collectors.toList());
                        if(formfields.size()> 0 ) {
                            List<String> steps = Arrays.asList(formfields.get(0).getStepNumbers().split("#"));
                            if (!data.getDatas().containsKey(entry.getKey()) || steps.contains(signRequest.getCurrentStepNumber().toString())) {
                                data.getDatas().put(entry.getKey(), entry.getValue());
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        List<SignRequestParams> signRequestParamses = Arrays.asList(objectMapper.readValue(signRequestParamsJsonString, SignRequestParams[].class));
        signRequest.getSignRequestParams().clear();
        for(SignRequestParams signRequestParams : signRequestParamses) {
            signRequestParamsRepository.save(signRequestParams);
            signRequest.getSignRequestParams().add(signRequestParams);
        }

        if (signRequestService.getCurrentSignType(signRequest).equals(SignType.nexuSign)) {
            signRequestService.setStep("Démarrage de l'application NexU");
            signRequestService.setStep("initNexu");
            return new ResponseEntity(HttpStatus.OK);
        }
        try {
            signRequest.setComment(comment);
            signRequestService.sign(signRequest, user, password, visual, formDataMap);
            signRequestService.setStep("end");
            return new ResponseEntity(HttpStatus.OK);
        } catch (EsupSignatureException | IOException e) {
            logger.error(e.getMessage());
        }
        return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @PreAuthorize("@signRequestService.preAuthorizeOwner(#id, #authUser)")
    @ResponseBody
    @PostMapping(value = "/add-docs/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object addDocumentToNewSignRequest(@ModelAttribute("authUser") User authUser, @PathVariable("id") Long id, @RequestParam("multipartFiles") MultipartFile[] multipartFiles) throws EsupSignatureIOException {
        logger.info("start add documents");
        SignRequest signRequest = signRequestRepository.findById(id).get();
        for (MultipartFile multipartFile : multipartFiles) {
            signRequestService.addDocsToSignRequest(signRequest, multipartFile);
        }
        return new String[]{"ok"};
    }

    @ResponseBody
    @PostMapping(value = "/remove-doc/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String removeDocument(@ModelAttribute("user") User user, @PathVariable("id") Long id) throws JSONException {
        logger.info("remove document " + id);
        JSONObject result = new JSONObject();
        Document document = documentRepository.findById(id).get();
        SignRequest signRequest = signRequestRepository.findById(document.getParentId()).get();
        if(signRequest.getCreateBy().equals(user)) {
            signRequest.getOriginalDocuments().remove(document);
        } else {
            result.put("error", "Non autorisé");
        }
        return result.toString();
    }

    @GetMapping("/sign-by-token/{token}")
    public String signByToken(@ModelAttribute("user") User user, @PathVariable("token") String token) {
        //User user = userService.getCurrentUser();
        SignRequest signRequest = signRequestRepository.findByToken(token).get(0);
        if (signRequestService.checkUserSignRights(user, signRequest)) {
            return "redirect:/user/signrequests/" + signRequest.getId();
        } else {
            return "redirect:/";
        }
    }

    @PostMapping(value = "/fast-sign-request")
    public String createSignRequest(@ModelAttribute("user") User user, @RequestParam("multipartFiles") MultipartFile[] multipartFiles,
                                    @RequestParam("signType") SignType signType, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        logger.info("création rapide demande de signature par " + user.getFirstname() + " " + user.getName());
        if (multipartFiles != null) {
            if(signRequestService.checkSignTypeDocType(signType, multipartFiles[0])) {
                SignRequest signRequest = signRequestService.createSignRequest(multipartFiles[0].getOriginalFilename(), user);
                try {
                    signRequestService.addDocsToSignRequest(signRequest, multipartFiles);
                } catch (EsupSignatureIOException e) {
                    redirectAttributes.addFlashAttribute("messageError", "Impossible de charger le document : documents corrompu");
                    return "redirect:" + request.getHeader("Referer");
                }
                signRequestService.addRecipients(signRequest, user);

                signRequestService.pendingSignRequest(signRequest, signType, false);
                return "redirect:/user/signrequests/" + signRequest.getId();
            } else {
                redirectAttributes.addFlashAttribute("messageError", "Impossible de demander une signature visuelle sur un document du type " + multipartFiles[0].getContentType());
                return "redirect:" + request.getHeader("Referer");
            }
        } else {
            logger.warn("no file to import");
        }
        return "redirect:/user/signrequests";
    }

    @PostMapping(value = "/send-sign-request")
    public String sendSignRequest(@ModelAttribute("user") User user, @RequestParam("multipartFiles") MultipartFile[] multipartFiles,
                                  @RequestParam(value = "recipientsEmails", required = false) String[] recipientsEmails,
                                  @RequestParam(name = "allSignToComplete", required = false) Boolean allSignToComplete,
                                  @RequestParam("signType") SignType signType, RedirectAttributes redirectAttributes) throws EsupSignatureIOException, EsupSignatureException {
        logger.info(user.getEmail() + " envoi d'une demande de signature à " + Arrays.toString(recipientsEmails));
        if (multipartFiles != null) {
            if(allSignToComplete == null) {
                allSignToComplete = false;
            }

            SignBook signBook = signRequestService.addDocsInSignBook(user, "Demande simple", "", multipartFiles);
            signBook.setCurrentWorkflowStepNumber(1);
            try {
                signBookRepository.save(signBook);
                signBook.getWorkflowSteps().add(workflowService.createWorkflowStep(multipartFiles[0].getOriginalFilename(), "signbook", signBook.getId(), allSignToComplete, signType, recipientsEmails));
            } catch (EsupSignatureUserException e) {
                logger.error("error with users on create signbook " + signBook.getId());
                redirectAttributes.addFlashAttribute("messageError", "Problème lors de l’envoi");
            }
//            signBookService.addSignRequest(signBook, signRequest);
            //enable for auto pending
//          signBookService.pendingSignBook(signBook, user);
//          if(!comment.isEmpty()) {
//              signRequest.setComment(comment);
//              signRequestService.updateStatus(signRequest, signRequest.getStatus(), "comment", "SUCCES", null, null, null, 0);
//          }
            redirectAttributes.addFlashAttribute("messageWarn", "Après vérification, vous devez confirmer l'envoi pour finaliser la demande");
            return "redirect:/user/signrequests/" + signBook.getSignRequests().get(0).getId();
        } else {
            logger.warn("no file to import");
            redirectAttributes.addFlashAttribute("messageError", "Pas de fichier à importer");
        }
        return "redirect:/user/signrequests";
    }


    @ResponseBody
    @GetMapping(value = "/get-step")
    public String getStep() {
        logger.debug("getStep : " + signRequestService.getStep());
        return signRequestService.getStep();
    }

    @PreAuthorize("@signRequestService.preAuthorizeSign(#id, #user)")
    @GetMapping(value = "/refuse/{id}")
    public String refuse(@ModelAttribute("user") User user, @PathVariable("id") Long id, @RequestParam(value = "comment") String comment, RedirectAttributes redirectAttrs, HttpServletRequest request) {
        SignRequest signRequest = signRequestRepository.findById(id).get();
        signRequest.setComment(comment);
        signRequestService.refuse(signRequest, user);
        redirectAttrs.addFlashAttribute("messageInfos", "La demandes à bien été refusée");
        return "redirect:/user/signrequests/?statusFilter=tosign";
    }

    @PreAuthorize("@signRequestService.preAuthorizeOwner(#id, #authUser)")
    @DeleteMapping(value = "/{id}", produces = "text/html")
    public String delete(@ModelAttribute("authUser") User authUser, @PathVariable("id") Long id, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        SignRequest signRequest = signRequestRepository.findById(id).get();
        signRequestService.delete(signRequest);
        redirectAttributes.addFlashAttribute("messageInfo", "Suppression effectuée");
        if (signRequest.getParentSignBook() != null) {
            return "redirect:" + request.getHeader("referer");
        } else {
            return "redirect:/user/signrequests/";
        }
    }

    @PreAuthorize("@signRequestService.preAuthorizeOwner(#id, #authUser)")
    @PostMapping(value = "/add-attachment/{id}")
    public String addAttachement(@ModelAttribute("authUser") User authUser, @PathVariable("id") Long id,
                                 @RequestParam(value = "multipartFiles", required = false) MultipartFile[] multipartFiles,
                                 @RequestParam(value = "link", required = false) String link,
                                 RedirectAttributes redirectAttributes) throws EsupSignatureIOException {
        logger.info("start add attachment");
        SignRequest signRequest = signRequestRepository.findById(id).get();
        if(multipartFiles != null && multipartFiles.length > 0) {
            for (MultipartFile multipartFile : multipartFiles) {
                if(multipartFile.getSize() > 0) {
                    signRequestService.addAttachmentToSignRequest(signRequest, multipartFile);
                }
            }
        }
        if(link != null && !link.isEmpty()) {
            signRequest.getLinks().add(link);
        }
        redirectAttributes.addFlashAttribute("messageInfo", "La pieces jointe à bien été ajoutée");
        return "redirect:/user/signrequests/" + id;
    }

    @PreAuthorize("@signRequestService.preAuthorizeView(#id, #user, #authUser)")
    @GetMapping(value = "/remove-attachment/{id}/{attachementId}")
    public String removeAttachement(@ModelAttribute("user") User user, @ModelAttribute("authUser") User authUser, @PathVariable("id") Long id, @PathVariable("attachementId") Long attachementId, RedirectAttributes redirectAttributes) {
        logger.info("start remove attachment");
        SignRequest signRequest = signRequestRepository.findById(id).get();
        Document attachement = documentRepository.findById(attachementId).get();
        if (!attachement.getParentId().equals(signRequest.getId())) {
            redirectAttributes.addFlashAttribute("messageError", "Pièce jointe non trouvée ...");
        } else {
            signRequest.getAttachments().remove(attachement);
            signRequestRepository.save(signRequest);
            documentRepository.delete(attachement);
        }
        redirectAttributes.addFlashAttribute("messageInfo", "La pieces jointe à été supprimée");
        return "redirect:/user/signrequests/" + id;
    }

    @PreAuthorize("@signRequestService.preAuthorizeView(#id, #user, #authUser)")
    @GetMapping(value = "/remove-link/{id}/{linkId}")
    public String removeLink(@ModelAttribute("user") User user, @ModelAttribute("authUser") User authUser, @PathVariable("id") Long id, @PathVariable("linkId") Integer linkId, RedirectAttributes redirectAttributes) {
        logger.info("start remove link");
        SignRequest signRequest = signRequestRepository.findById(id).get();
        String toRemove = signRequest.getLinks().get(linkId);
        signRequest.getLinks().remove(toRemove);
        signRequestRepository.save(signRequest);
        redirectAttributes.addFlashAttribute("messageInfo", "Le lien à été supprimé");
        return "redirect:/user/signrequests/" + id;
    }

    @PreAuthorize("@signRequestService.preAuthorizeView(#id, #user, #authUser)")
    @GetMapping(value = "/get-attachment/{id}/{attachementId}")
    public void getAttachment(@ModelAttribute("user") User user, @ModelAttribute("authUser") User authUser, @PathVariable("id") Long id, @PathVariable("attachementId") Long attachementId, HttpServletResponse response, RedirectAttributes redirectAttributes) {
        SignRequest signRequest = signRequestRepository.findById(id).get();
        Document attachement = documentRepository.findById(attachementId).get();
        try {
            if (!attachement.getParentId().equals(signRequest.getId())) {
                redirectAttributes.addFlashAttribute("messageError", "Pièce jointe non trouvée ...");
                response.sendRedirect("/user/signsignrequests/" + id);
            } else {
                response.setHeader("Content-Disposition", "inline;filename=\"" + attachement.getFileName() + "\"");
                response.setContentType(attachement.getContentType());
                IOUtils.copy(attachement.getInputStream(), response.getOutputStream());
            }
        } catch (Exception e) {
            logger.error("get file error", e);
        }
    }

    @PreAuthorize("@signRequestService.preAuthorizeView(#id, #user, #authUser)")
    @GetMapping(value = "/get-last-file/{id}")
    public void getLastFile(@ModelAttribute("user") User user, @ModelAttribute("authUser") User authUser, @PathVariable("id") Long id, HttpServletResponse response) throws IOException, SQLException, EsupSignatureException {
        SignRequest signRequest = signRequestRepository.findById(id).get();
        InputStream inputStream = null;
        String contentType = "";
        String fileName = "";
        if (!signRequest.getStatus().equals(SignRequestStatus.exported)) {
            List<Document> documents = signRequestService.getToSignDocuments(signRequest);
            if (documents.size() > 1) {
                response.sendRedirect("/user/signrequests/" + signRequest.getId());
            } else {
                inputStream = documents.get(0).getBigFile().getBinaryFile().getBinaryStream();
                fileName = documents.get(0).getFileName();
                contentType = documents.get(0).getContentType();
            }
        } else {
            FsFile fsFile = signRequestService.getLastSignedFsFile(signRequest);
            inputStream = fsFile.getInputStream();
            fileName = fsFile.getName();
            contentType = fsFile.getContentType();
        }
        try {
            response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");
            response.setContentType(contentType);
            IOUtils.copy(inputStream, response.getOutputStream());
        } catch (Exception e) {
            logger.error("get file error", e);
        }
    }

    @PreAuthorize("@signRequestService.preAuthorizeOwner(#id, #authUser)")
    @GetMapping(value = "/update-step/{id}/{step}")
    public String changeStepSignType(@ModelAttribute("authUser") User authUser, @PathVariable("id") Long id, @PathVariable("step") Integer step, @RequestParam(name = "signType") SignType signType) {
        SignRequest signRequest = signRequestRepository.findById(id).get();
        signRequest.setSignType(signType);
        return "redirect:/user/signrequests/" + id + "/?form";
    }

    @PreAuthorize("@signRequestService.preAuthorizeOwner(#id, #authUser)")
    @GetMapping(value = "/complete/{id}")
    public String complete(@ModelAttribute("user") User user, User authUser, @PathVariable("id") Long id, HttpServletRequest request) throws EsupSignatureException {
        SignRequest signRequest = signRequestRepository.findById(id).get();
        if (signRequest.getCreateBy().equals(user.getEppn()) && (signRequest.getStatus().equals(SignRequestStatus.signed) || signRequest.getStatus().equals(SignRequestStatus.checked))) {
            signRequestService.completeSignRequest(signRequest);
        } else {
            logger.warn(user.getEppn() + " try to complete " + signRequest.getId() + " without rights");
        }
        return "redirect:/user/signrequests/" + id + "/?form";
    }

    @PreAuthorize("@signRequestService.preAuthorizeOwner(#id, #authUser)")
    @GetMapping(value = "/pending/{id}")
    public String pending(@ModelAttribute("user") User user, User authUser, @PathVariable("id") Long id,
                          @RequestParam(value = "comment", required = false) String comment,
                          @RequestParam(value = "emails", required = false) String emails[],
                          @RequestParam(value = "names", required = false) String names[],
                          @RequestParam(value = "firstnames", required = false) String firstnames[],
                          @RequestParam(value = "phones", required = false) String phones[],
                          RedirectAttributes redirectAttributes) throws MessagingException {
        SignRequest signRequest = signRequestRepository.findById(id).get();
        List<User> tempUsers = signRequestService.getTempUsers(signRequest);
        int countExternalUsers = 0;
        if(tempUsers.size() > 0) {
            for (User tempUser : tempUsers) {
                if (tempUser.getUserType().equals(UserType.external)) countExternalUsers++;
            }
            if (countExternalUsers == names.length) {
                int userNumber = 0;
                for (User tempUser : tempUsers) {
                    if (tempUser.getUserType().equals(UserType.shib)) {
                        logger.warn("TODO Envoi Mail SHIBBOLETH ");
                        //TODO envoi mail spécifique
                    } else if (tempUser.getUserType().equals(UserType.external)) {
                        tempUser.setFirstname(firstnames[userNumber]);
                        tempUser.setName(names[userNumber]);
                        tempUser.setEppn(phones[userNumber]);
                        otpService.generateOtpForSignRequest(signRequest, tempUser);
                    }
                    userRepository.save(tempUser);
                    userNumber++;
                }
            } else {
                redirectAttributes.addFlashAttribute("messageError", "Merci de compléter tous les utilisateurs externes");
                return "redirect:/user/signrequests/" + signRequest.getId();
            }
        }
        if(signRequest.getParentSignBook() != null) {
            if(signRequest.getParentSignBook().getStatus().equals(SignRequestStatus.draft)) {
                signBookService.pendingSignBook(signRequest.getParentSignBook(), user);
            }
        } else {
            if (signRequest.getStatus().equals(SignRequestStatus.draft)) {
                signRequestService.updateStatus(signRequest, SignRequestStatus.pending, "Envoyé pour signature", "SUCCESS");

            }
        }
        if(!comment.isEmpty()) {
            signRequest.setComment(comment);
            signRequestService.updateStatus(signRequest, signRequest.getStatus(), "comment", "SUCCES", null, null, null, 0);
        }
        redirectAttributes.addFlashAttribute("messageSuccess", "Votre demande à bien été transmise");
        return "redirect:/user/signrequests/";
    }

    @PreAuthorize("@signRequestService.preAuthorizeOwner(#id, #authUser)")
    @PostMapping(value = "/add-recipients/{id}")
    public String addRecipients(@ModelAttribute("authUser") User authUser, @PathVariable("id") Long id,
                                @RequestParam(value = "recipientsEmails", required = false) String[] recipientsEmails,
                                @RequestParam(name = "signType") SignType signType,
                                @RequestParam(name = "allSignToComplete", required = false) Boolean allSignToComplete) throws EsupSignatureUserException {
        SignRequest signRequest = signRequestRepository.findById(id).get();
        signRequestService.addRecipients(signRequest, recipientsEmails);
        signRequest.setSignType(signType);
        if (allSignToComplete != null && allSignToComplete) {
            signRequest.setAllSignToComplete(true);
        } else {
            signRequest.setAllSignToComplete(false);
        }
        return "redirect:/user/signrequests/" + id + "/?form";
    }

    @PreAuthorize("@signRequestService.preAuthorizeView(#id, #user, #authUser)")
    @PostMapping(value = "/comment/{id}")
    public String comment(@ModelAttribute("user") User user, @ModelAttribute("authUser") User authUser, @PathVariable("id") Long id,
                          @RequestParam(value = "comment", required = false) String comment,
                          @RequestParam(value = "commentPageNumber", required = false) Integer commentPageNumber,
                          @RequestParam(value = "commentPosX", required = false) Integer commentPosX,
                          @RequestParam(value = "commentPosY", required = false) Integer commentPosY) {
        SignRequest signRequest = signRequestRepository.findById(id).get();
        signRequest.setComment(comment);
        signRequestService.updateStatus(signRequest, null, "Ajout d'un commentaire", "SUCCESS", commentPageNumber, commentPosX, commentPosY);
        return "redirect:/user/signrequests/" + signRequest.getId();
    }

}
