package org.esupportail.esupsignature.web.controller.user;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.repository.*;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.esupportail.esupsignature.service.export.SedaExportService;
import org.esupportail.esupsignature.service.file.FileService;
import org.esupportail.esupsignature.service.fs.FsFile;
import org.esupportail.esupsignature.service.pdf.PdfParameters;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
public class SignRequestController {

    private static final Logger logger = LoggerFactory.getLogger(SignRequestController.class);

    @Value("${baseUrl}")
    private String baseUrl;

    @Value("${nexuVersion}")
    private String nexuVersion;

    @Value("${nexuUrl}")
    private String nexuUrl;

    @ModelAttribute("userMenu")
    public String getActiveRole() {
        return "active";
    }

    @ModelAttribute("activeMenu")
    public String getActiveMenu() {
        return "signrequests";
    }

    @ModelAttribute("user")
    public User getUser() {
        return userService.getUserFromAuthentication();
    }

    private String progress = "0";

    private SignRequestStatus statusFilter = null;

    @Resource
    private UserService userService;

    @Resource
    private SignRequestRepository signRequestRepository;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private SignRequestParamsRepository signRequestParamsRepository;

    @Resource
    private SignBookRepository signBookRepository;

    @Resource
    private WorkflowRepository workflowRepository;

    @Resource
    private WorkflowService workflowService;

    @Resource
    private SignBookService signBookService;

    @Resource
    private LogRepository logRepository;

    @Resource
    private DocumentRepository documentRepository;

    @Resource
    private PdfService pdfService;

    @Resource
    private FileService fileService;

    @Resource
    private SedaExportService sedaExportService;

    @GetMapping
    public String list(User user,
                       @RequestParam(value = "statusFilter", required = false) String statusFilter,
                       @RequestParam(value = "signBookId", required = false) Long signBookId,
                       @RequestParam(value = "messageError", required = false) String messageError,
                       @SortDefault(value = "createDate", direction = Direction.DESC) @PageableDefault(size = 5) Pageable pageable, Model model) {
        workflowService.initCreatorWorkflow();

        List<SignRequest> signRequests;
        if (statusFilter != null) {
            if (statusFilter.equals("tosign")) {
                signRequests = signRequestService.getToSignRequests(user);
                model.addAttribute("statusFilter", "tosign");
            } else {
                signRequests = signRequestRepository.findByCreateByAndStatus(user.getEppn(), SignRequestStatus.valueOf(statusFilter));
                model.addAttribute("statusFilter", SignRequestStatus.valueOf(statusFilter));
            }
        } else {
            signRequests = signRequestRepository.findByCreateBy(user.getEppn());
            for(SignRequest signRequest : signRequestService.getToSignRequests(user)) {
                if(!signRequests.contains(signRequest)) {
                    signRequests.add(signRequest);
                }
            }
        }

        model.addAttribute("signRequests", signRequestService.getSignRequestsPageGrouped(signRequests, pageable));

        if (user.getKeystore() != null) {
            model.addAttribute("keystore", user.getKeystore().getFileName());
        }
        model.addAttribute("mydocs", "active");
        model.addAttribute("signBookId", signBookId);
        model.addAttribute("statuses", SignRequestStatus.values());
        model.addAttribute("messageError", messageError);
        return "user/signrequests/list";
    }

    @PreAuthorize("@signRequestService.preAuthorizeView(authentication.name, #id)")
    @GetMapping(value = "/{id}")
    public String show(@PathVariable("id") Long id, @RequestParam(required = false) Boolean frameMode, Model model) throws Exception {
        User user = userService.getUserFromAuthentication();
        SignRequest signRequest = signRequestRepository.findById(id).get();
        model.addAttribute("signRequest", signRequest);

        if (signRequest.getStatus().equals(SignRequestStatus.pending)
                && signRequestService.checkUserSignRights(user, signRequest) && signRequest.getOriginalDocuments().size() > 0
                && signRequestService.needToSign(signRequest, user)
        ) {
            signRequest.setSignable(true);
            //model.addAttribute("signable", true);
            model.addAttribute("nexuUrl", nexuUrl);
            model.addAttribute("nexuVersion", nexuVersion);
            model.addAttribute("baseUrl", baseUrl);
        }
        Document toDisplayDocument;
        if (signRequest.getSignedDocuments().size() > 0 || signRequest.getOriginalDocuments().size() > 0) {
            toDisplayDocument = signRequestService.getToSignDocuments(signRequest).get(0);
            if (toDisplayDocument.getContentType().equals("application/pdf")) {
                PdfParameters pdfParameters = pdfService.getPdfParameters(toDisplayDocument.getInputStream());
                model.addAttribute("pdfWidth", pdfParameters.getWidth());
                model.addAttribute("pdfHeight", pdfParameters.getHeight());
                model.addAttribute("imagePagesSize", pdfParameters.getTotalNumberOfPages());
                if (user.getSignImage() != null && user.getSignImage().getSize() > 0) {
                    if(user.getKeystore() == null && signRequest.getSignType().equals(SignType.certSign)) {
                        //model.addAttribute("signable", false);
                        model.addAttribute("messageWarn", "Pour signer ce document merci d'ajouter un keystore à votre profil");
                    }
                    model.addAttribute("signFile", fileService.getBase64Image(user.getSignImage()));
                    int[] size = pdfService.getSignSize(user.getSignImage().getInputStream());
                    model.addAttribute("signWidth", size[0]);
                    model.addAttribute("signHeight", size[1]);
                } else {
                    if(signRequest.getSignType().equals(SignType.pdfImageStamp) || signRequest.getSignType().equals(SignType.certSign)) {
                        //model.addAttribute("signable", false);
                        model.addAttribute("messageWarn", "Pour signer ce document merci d'ajouter une image de votre signature");
                    }
                    model.addAttribute("signWidth", 100);
                    model.addAttribute("signHeight", 75);
                }

            }
            model.addAttribute("documentType", fileService.getExtension(toDisplayDocument.getFileName()));
        } else if (signRequestService.getLastSignedFsFile(signRequest) != null) {
            FsFile fsFile = signRequestService.getLastSignedFsFile(signRequest);
            model.addAttribute("documentType", fileService.getExtension(fsFile.getName()));
        }
        model.addAttribute("currentSignType", signRequestService.getCurrentSignType(signRequest).name());
        List<Log> refuseLogs = logRepository.findBySignRequestIdAndFinalStatus(signRequest.getId(), SignRequestStatus.refused.name());
        model.addAttribute("refuseLogs", refuseLogs);
        model.addAttribute("postits", logRepository.findBySignRequestIdAndPageNumberIsNotNull(signRequest.getId()));
        if (frameMode != null && frameMode) {
            return "user/signrequests/show-frame";
        } else {
            return "user/signrequests/show";
        }
    }

    @PreAuthorize("@signRequestService.preAuthorizeView(authentication.name, #id)")
    @GetMapping(value = "/{id}", params = "form")
    public String updateForm(@PathVariable("id") Long id, Model model) throws Exception {
        User currentUser = userService.getUserFromAuthentication();
        SignRequest signRequest = signRequestRepository.findById(id).get();
        model.addAttribute("signBooks", signBookService.getAllSignBooks());
        List<Log> logs = logRepository.findBySignRequestId(signRequest.getId());
        logs = logs.stream().sorted(Comparator.comparing(Log::getLogDate).reversed()).collect(Collectors.toList());
        model.addAttribute("logs", logs);
        model.addAttribute("comments", logs.stream().filter(log -> log.getComment() != null && !log.getComment().isEmpty()).collect(Collectors.toList()));
        List<Log> refuseLogs = logRepository.findBySignRequestIdAndFinalStatus(signRequest.getId(), SignRequestStatus.refused.name());
        model.addAttribute("refuseLogs", refuseLogs);
        if (currentUser.getSignImage() != null) {
            model.addAttribute("signFile", fileService.getBase64Image(currentUser.getSignImage()));
        }
        if (currentUser.getKeystore() != null) {
            model.addAttribute("keystore", currentUser.getKeystore().getFileName());
        }
        model.addAttribute("signRequest", signRequest);

        if (signRequest.getStatus().equals(SignRequestStatus.pending) && signRequestService.checkUserSignRights(currentUser, signRequest) && signRequest.getOriginalDocuments().size() > 0) {
            signRequest.setSignable(true);
            //model.addAttribute("signable", true);
        }
        model.addAttribute("signTypes", SignType.values());
        model.addAttribute("workflows", workflowRepository.findAll());
        return "user/signrequests/update";

    }

//
//    @PreAuthorize("@signRequestService.preAuthorizeOwner(authentication.name, #id)")
//    @PostMapping(value = "/add-docs/{id}")
//    public String addDocumentToNewSignRequest(@PathVariable("id") Long id,
//                                              @RequestParam("multipartFiles") MultipartFile[] multipartFiles) throws EsupSignatureIOException {
//        logger.info("start add documents");
//        User user = userService.getUserFromAuthentication();
//        SignBook signBook = signBookRepository.findById(id).get();
//        for (MultipartFile multipartFile : multipartFiles) {
//            SignRequest signRequest = signRequestService.createSignRequest(signBook.getName() + "_" + multipartFile.getOriginalFilename(), user);
//            signRequestService.addDocsToSignRequest(signRequest, multipartFile);
//            signBookService.addSignRequest(signBook, signRequest);
//        }
//        return "redirect:/user/signbooks/" + id + "/?form";
//    }

    @ResponseBody
    @PostMapping(value = "/remove-doc/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object removeDocument(@PathVariable("id") Long id, HttpServletRequest request) {
        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        Document document = documentRepository.findById(id).get();
        SignRequest signRequest = signRequestRepository.findById(document.getParentId()).get();
        if (signRequest.getCreateBy().equals(user.getEppn())) {
            signRequest.getOriginalDocuments().remove(document);
        }
        String[] ok = {"ok"};
        return ok;
    }

    @GetMapping("/sign-by-token/{token}")
    public String signByToken(@PathVariable("token") String token) {
        User user = userService.getUserFromAuthentication();
        SignRequest signRequest = signRequestRepository.findByToken(token).get(0);
        if (signRequestService.checkUserSignRights(user, signRequest)) {
            return "redirect:/user/signrequests/" + signRequest.getId();
        } else {
            return "redirect:/";
        }
    }

    @PostMapping(value = "/fast-sign-request")
    public String createSignRequest(@RequestParam("multipartFiles") MultipartFile[] multipartFiles,
                                    @RequestParam("signType") SignType signType) throws EsupSignatureIOException {
        logger.info("création rapide demande de signature");
        User user = userService.getUserFromAuthentication();
        if (multipartFiles != null) {
            SignRequest signRequest = signRequestService.createSignRequest(multipartFiles[0].getOriginalFilename(), user);
            signRequestService.addDocsToSignRequest(signRequest, multipartFiles);
            signRequestService.addRecipients(signRequest, user);

            signRequestService.pendingSignRequest(signRequest, signType, false, user);
            return "redirect:/user/signrequests/" + signRequest.getId();
        } else {
            logger.warn("no file to import");
        }
        return "redirect:/user/signrequests";
    }

    @PreAuthorize("@signRequestService.preAuthorizeSign(authentication.name, #id)")
    @ResponseBody
    @PostMapping(value = "/sign/{id}")
    public ResponseEntity sign(@PathVariable("id") Long id,
                               @RequestParam(value = "xPos", required = false) Integer xPos,
                               @RequestParam(value = "yPos", required = false) Integer yPos,
                               @RequestParam(value = "comment", required = false) String comment,
                               @RequestParam(value = "addDate", required = false) Boolean addDate,
                               @RequestParam(value = "visual", required = false) Boolean visual,
                               @RequestParam(value = "signPageNumber", required = false) Integer signPageNumber,
                               @RequestParam(value = "password", required = false) String password,
                               HttpServletRequest request) {
        if (addDate == null) {
            addDate = false;
        }
        if (visual == null) {
            visual = false;
        }

        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        SignRequest signRequest = signRequestRepository.findById(id).get();
        if (signPageNumber != null && xPos != null && yPos != null && visual) {
            SignRequestParams signRequestParams = signRequest.getCurrentSignRequestParams();
            signRequestParams.setSignPageNumber(signPageNumber);
            signRequestParams.setxPos(xPos);
            signRequestParams.setyPos(yPos);
            signRequestParamsRepository.save(signRequestParams);
            if(!signRequest.getSignRequestParams().contains(signRequestParams)) {
                signRequest.getSignRequestParams().add(signRequestParams);
            }
        }
        if (signRequestService.getCurrentSignType(signRequest).equals(SignType.nexuSign)) {
            signRequestService.setStep("Démarrage de l'application NexU");
            signRequestService.setStep("initNexu");
            return new ResponseEntity(HttpStatus.OK);
        }
        try {
            signRequest.setComment(comment);
            signRequestService.sign(signRequest, user, password, addDate, visual);
            signRequestService.setStep("end");
        } catch (EsupSignatureException | IOException e) {
            logger.error(e.getMessage(), e);
        }
        return new ResponseEntity(HttpStatus.OK);
    }

    @ResponseBody
    @GetMapping(value = "/get-step")
    public String getStep() {
        logger.debug("getStep : " + signRequestService.getStep());
        return signRequestService.getStep();
    }

    @ResponseBody
    @GetMapping(value = "/get-progress", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getProgress() {
        logger.debug("getProgress : " + progress);
        return progress;
    }

    @PreAuthorize("@signRequestService.preAuthorizeSign(authentication.name, #id)")
    @GetMapping(value = "/refuse/{id}")
    public String refuse(@PathVariable("id") Long id, @RequestParam(value = "comment") String comment, RedirectAttributes redirectAttrs, HttpServletRequest request) {
        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        SignRequest signRequest = signRequestRepository.findById(id).get();
        signRequest.setComment(comment);
        signRequestService.refuse(signRequest, user);
        return "redirect:/user/signrequests/" + signRequest.getId();
    }

    @PreAuthorize("@signRequestService.preAuthorizeOwner(authentication.name, #id)")
    @DeleteMapping(value = "/{id}", produces = "text/html")
    public String delete(@PathVariable("id") Long id, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        SignRequest signRequest = signRequestRepository.findById(id).get();
        signRequestService.delete(signRequest);
        redirectAttributes.addFlashAttribute("messageInfo", "Suppression effectuée");
        if (signRequest.getParentSignBook() != null) {
            return "redirect:" + request.getHeader("referer");
        } else {
            return "redirect:/user/signrequests/";
        }

    }

    @PreAuthorize("@signRequestService.preAuthorizeView(authentication.name, #id)")
    @RequestMapping(value = "/get-last-file-seda/{id}", method = RequestMethod.GET)
    public void getLastFileSeda(@PathVariable("id") Long id, HttpServletResponse response, Model model) {
        SignRequest signRequest = signRequestRepository.findById(id).get();
        List<Document> documents = signRequestService.getToSignDocuments(signRequest);
        try {
            if (documents.size() > 1) {
                response.sendRedirect("/user/signsignrequests/" + id);
            } else {
                response.setHeader("Content-Disposition", "attachment;filename=test-seda.zip");
                response.setContentType("application/zip");
                IOUtils.copy(sedaExportService.generateSip(signRequest), response.getOutputStream());
            }
        } catch (Exception e) {
            logger.error("get file error", e);
        }
    }


    @PreAuthorize("@signRequestService.preAuthorizeOwner(authentication.name, #id)")
    @PostMapping(value = "/add-attachment/{id}")
    public String addAttachement(@PathVariable("id") Long id, @RequestParam("multipartFiles") MultipartFile[] multipartFiles, RedirectAttributes redirectAttributes) throws EsupSignatureIOException {
        logger.info("start add attachment");
        SignRequest signRequest = signRequestRepository.findById(id).get();
        for (MultipartFile multipartFile : multipartFiles) {
            signRequestService.addAttachmentToSignRequest(signRequest, multipartFile);
        }
        redirectAttributes.addFlashAttribute("messageInfo", "La pieces jointe à bien été ajoutée");
        return "redirect:/user/signrequests/" + id;
    }

    @PreAuthorize("@signRequestService.preAuthorizeOwner(authentication.name, #id)")
    @GetMapping(value = "/remove-attachment/{id}/{attachementId}")
    public String removeAttachement(@PathVariable("id") Long id, @PathVariable("attachementId") Long attachementId, RedirectAttributes redirectAttributes) {
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

    @PreAuthorize("@signRequestService.preAuthorizeView(authentication.name, #id)")
    @GetMapping(value = "/get-attachment/{id}/{attachementId}")
    public void getAttachment(@PathVariable("id") Long id, @PathVariable("attachementId") Long attachementId, HttpServletResponse response, RedirectAttributes redirectAttributes) {
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

    @PreAuthorize("@signRequestService.preAuthorizeView(authentication.name, #id)")
    @RequestMapping(value = "/get-last-file/{id}", method = RequestMethod.GET)
    public void getLastFile(@PathVariable("id") Long id, HttpServletResponse response) throws IOException, SQLException {
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

    @PreAuthorize("@signRequestService.preAuthorizeOwner(authentication.name, #id)")
    @RequestMapping(value = "/change-step-sign-type/{id}/{step}", method = RequestMethod.GET)
    public String changeStepSignType(@PathVariable("id") Long id, @PathVariable("step") Integer step, @RequestParam(name = "signType") SignType signType) {
        SignRequest signRequest = signRequestRepository.findById(id).get();
        signRequest.setSignType(signType);
        return "redirect:/user/signrequests/" + id + "/?form";
    }

    @PreAuthorize("@signRequestService.preAuthorizeOwner(authentication.name, #id)")
    @RequestMapping(value = "/complete/{id}", method = RequestMethod.GET)
    public String complete(@PathVariable("id") Long id, HttpServletRequest request) throws EsupSignatureException {
        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        SignRequest signRequest = signRequestRepository.findById(id).get();
        if (signRequest.getCreateBy().equals(user.getEppn()) && (signRequest.getStatus().equals(SignRequestStatus.signed) || signRequest.getStatus().equals(SignRequestStatus.checked))) {
            signRequestService.completeSignRequest(signRequest, user);
        } else {
            logger.warn(user.getEppn() + " try to complete " + signRequest.getId() + " without rights");
        }
        return "redirect:/user/signrequests/" + id + "/?form";
    }

    @PreAuthorize("@signRequestService.preAuthorizeOwner(authentication.name, #id)")
    @RequestMapping(value = "/pending/{id}", method = RequestMethod.GET)
    public String pending(@PathVariable("id") Long id,
                          @RequestParam(value = "comment", required = false) String comment,
                          HttpServletRequest request) {
        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        SignRequest signRequest = signRequestRepository.findById(id).get();
        signRequest.setComment(comment);
        if (signRequest.getStatus().equals(SignRequestStatus.draft) || signRequest.getStatus().equals(SignRequestStatus.completed)) {
            signRequestService.updateStatus(signRequest, SignRequestStatus.pending, "Envoyé pour signature", user, "SUCCESS", signRequest.getComment());
        } else {
            logger.warn(user.getEppn() + " try to send for sign " + signRequest.getId() + " without rights");
        }
        return "redirect:/user/signrequests/" + id + "/?form";
    }

    @PreAuthorize("@signRequestService.preAuthorizeOwner(authentication.name, #id)")
    @PostMapping(value = "/add-recipients/{id}")
    public String addRecipients(@PathVariable("id") Long id,
                                @RequestParam(value = "recipientsEmails", required = false) String[] recipientsEmails,
                                @RequestParam(name = "signType") SignType signType,
                                @RequestParam(name = "allSignToComplete", required = false) Boolean allSignToComplete) {
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

    @PreAuthorize("@signRequestService.preAuthorizeView(authentication.name, #id)")
    @PostMapping(value = "/comment/{id}")
    public String comment(@PathVariable("id") Long id,
                          @RequestParam(value = "comment", required = false) String comment,
                          @RequestParam(value = "pageNumber", required = false) Integer pageNumber,
                          @RequestParam(value = "posX", required = false) Integer posX,
                          @RequestParam(value = "posY", required = false) Integer posY,
                          HttpServletRequest request) {
        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        SignRequest signRequest = signRequestRepository.findById(id).get();
        signRequestService.updateStatus(signRequest, null, "Ajout d'un commentaire", user, "SUCCESS", comment, pageNumber, posX, posY);
        return "redirect:/user/signrequests/" + signRequest.getId();
    }

}
