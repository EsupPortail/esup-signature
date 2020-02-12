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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @RequestMapping(produces = "text/html")
    public String list(
            @RequestParam(value = "statusFilter", required = false) String statusFilter,
            @RequestParam(value = "signBookId", required = false) Long signBookId,
            @RequestParam(value = "messageError", required = false) String messageError,
            @SortDefault(value = "createDate", direction = Direction.DESC) @PageableDefault(size = 3) Pageable pageable, Model model) {
        User user = userService.getUserFromAuthentication();
        workflowService.initCreatorWorkflow();

        if (statusFilter != null) {
            if (!statusFilter.equals("all")) {
                this.statusFilter = SignRequestStatus.valueOf(statusFilter);
            } else {
                this.statusFilter = null;
            }
        }

        List<SignRequest> signRequests;
        if(this.statusFilter != null) {
            signRequests = signRequestRepository.findByCreateByAndStatus(user.getEppn(), this.statusFilter);
        } else {
            signRequests = signRequestRepository.findByCreateBy(user.getEppn());
        }
        model.addAttribute("signRequests", getSignRequestsPageGrouped(signRequests, pageable));

        if (user.getKeystore() != null) {
            model.addAttribute("keystore", user.getKeystore().getFileName());
        }
        model.addAttribute("mydocs", "active");
        model.addAttribute("signRequestsSignedByMe", signRequestService.getSignRequestsSignedByUser(user));
        model.addAttribute("signBookId", signBookId);
        model.addAttribute("statusFilter", this.statusFilter);
        model.addAttribute("statuses", SignRequestStatus.values());
        model.addAttribute("messageError", messageError);
        return "user/signrequests/list";
    }

    @GetMapping("/to-sign")
    public String listToSign(
            @RequestParam(value = "statusFilter", required = false) String statusFilter,
            @RequestParam(value = "signBookId", required = false) Long signBookId,
            @RequestParam(value = "messageError", required = false) String messageError,
            @SortDefault(value = "createDate", direction = Direction.DESC) @PageableDefault(size = 3) Pageable pageable, Model model) {
        User user = userService.getUserFromAuthentication();
        workflowService.initCreatorWorkflow();

        if (statusFilter != null) {
            if (!statusFilter.equals("all")) {
                this.statusFilter = SignRequestStatus.valueOf(statusFilter);
            } else {
                this.statusFilter = null;
            }
        }
        List<SignRequest> signRequestsToSign = signRequestService.getToSignRequests(user);
        model.addAttribute("signRequests", getSignRequestsPageGrouped(signRequestsToSign, pageable));
        model.addAttribute("statusFilter", this.statusFilter);
        model.addAttribute("statuses", SignRequestStatus.values());
        model.addAttribute("messageError", messageError);
        model.addAttribute("activeMenu", "tosign");
        return "user/signrequests/list-to-sign";
    }

    public Page<SignRequest> getSignRequestsPageGrouped(List<SignRequest> signRequests, Pageable pageable) {
        List<SignRequest> signRequestsGrouped = new ArrayList<>();
        Map<SignBook, List<SignRequest>> signBookSignRequestMap = signRequests.stream().filter(signRequest -> signRequest.getParentSignBook() != null).collect(Collectors.groupingBy(SignRequest::getParentSignBook, Collectors.toList()));
        for(Map.Entry<SignBook, List<SignRequest>> signBookListEntry : signBookSignRequestMap.entrySet()) {
            int last = signBookListEntry.getValue().size() - 1;
            signBookListEntry.getValue().get(last).setViewTitle("");
            for(SignRequest signRequest : signBookListEntry.getValue()) {
                signBookListEntry.getValue().get(last).setViewTitle(signBookListEntry.getValue().get(last).getViewTitle() + signRequest.getTitle() + "\n\r");
            }
            signRequestsGrouped.add(signBookListEntry.getValue().get(last));
        }
        for(SignRequest signRequest : signRequests.stream().filter(signRequest -> signRequest.getParentSignBook() == null).collect(Collectors.toList())) {
            signRequest.setViewTitle(signRequest.getTitle());
            signRequestsGrouped.add(signRequest);
        }
        return new PageImpl<>(signRequestsGrouped.stream().skip(pageable.getOffset()).limit(pageable.getPageSize()).collect(Collectors.toList()), pageable, signRequestsGrouped.size());
    }

    @GetMapping(value = "/{id}", params = "form")
    public String updateForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttrs) throws Exception {
        User user = userService.getUserFromAuthentication();
        SignRequest signRequest = signRequestRepository.findById(id).get();
        if (signRequestService.checkUserViewRights(user, signRequest) || signRequestService.checkUserSignRights(user, signRequest)) {
            model.addAttribute("signBooks", signBookService.getAllSignBooks());
            List<Log> logs = logRepository.findBySignRequestId(signRequest.getId());
            logs = logs.stream().sorted(Comparator.comparing(Log::getLogDate).reversed()).collect(Collectors.toList());
            model.addAttribute("logs", logs);
            model.addAttribute("comments", logs.stream().filter(log -> log.getComment() != null && !log.getComment().isEmpty()).collect(Collectors.toList()));
            List<Log> refuseLogs = logRepository.findBySignRequestIdAndFinalStatus(signRequest.getId(), SignRequestStatus.refused.name());
            model.addAttribute("refuseLogs", refuseLogs);
            if (user.getSignImage() != null) {
                model.addAttribute("signFile", fileService.getBase64Image(user.getSignImage()));
            }
            if (user.getKeystore() != null) {
                model.addAttribute("keystore", user.getKeystore().getFileName());
            }
            model.addAttribute("signRequest", signRequest);

            if (signRequest.getStatus().equals(SignRequestStatus.pending) && signRequestService.checkUserSignRights(user, signRequest) && signRequest.getOriginalDocuments().size() > 0) {
                model.addAttribute("signable", "ok");
            }
            model.addAttribute("signTypes", SignType.values());
            model.addAttribute("workflows", workflowRepository.findAll());
            return "user/signrequests/update";
        } else {
            logger.warn(user.getEppn() + " attempted to access signRequest " + id + " without write access");
            redirectAttrs.addFlashAttribute("messageCustom", "not authorized");
            return "redirect:/user/signrequests/";
        }
    }

    @GetMapping(value = "/{id}")
    public String show(@PathVariable("id") Long id, @RequestParam(required = false) Boolean frameMode, Model model) throws Exception {
        User user = userService.getUserFromAuthentication();
        SignRequest signRequest = signRequestRepository.findById(id).get();
        model.addAttribute("signRequest", signRequest);
        if ((signRequestService.checkUserViewRights(user, signRequest) || signRequestService.checkUserSignRights(user, signRequest))) {
            if (signRequest.getStatus().equals(SignRequestStatus.pending)
                    && signRequestService.checkUserSignRights(user, signRequest) && signRequest.getOriginalDocuments().size() > 0
                    && signRequestService.needToSign(signRequest, user)
            ) {
                model.addAttribute("signable", "ok");
                model.addAttribute("nexuUrl", nexuUrl);
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
                        model.addAttribute("signFile", fileService.getBase64Image(user.getSignImage()));
                        int[] size = pdfService.getSignSize(user.getSignImage().getInputStream());
                        model.addAttribute("signWidth", size[0]);
                        model.addAttribute("signHeight", size[1]);
                    } else {
                        model.addAttribute("signWidth", 100);
                        model.addAttribute("signHeight", 75);
                    }

                }
                model.addAttribute("documentType", fileService.getExtension(toDisplayDocument.getFileName()));
            } else if(signRequestService.getLastSignedFsFile(signRequest) != null) {
                FsFile fsFile = signRequestService.getLastSignedFsFile(signRequest);
                model.addAttribute("documentType", fileService.getExtension(fsFile.getName()));
            }
        }
        List<Log> refuseLogs = logRepository.findBySignRequestIdAndFinalStatus(signRequest.getId(), SignRequestStatus.refused.name());
        model.addAttribute("refuseLogs", refuseLogs);
        model.addAttribute("postits", logRepository.findBySignRequestIdAndPageNumberIsNotNull(signRequest.getId()));
        if(frameMode != null && frameMode) {
            return "user/signrequests/show-frame";
        } else {
            return "user/signrequests/show";
        }
    }

    @PostMapping(value = "/add-docs/{id}")
    public String addDocumentToNewSignRequest(@PathVariable("id") Long id,
                                              @RequestParam("multipartFiles") MultipartFile[] multipartFiles) throws EsupSignatureIOException {
        logger.info("start add documents");
        User user = userService.getUserFromAuthentication();
        SignBook signBook = signBookRepository.findById(id).get();
        if (signBookService.checkUserViewRights(user, signBook)) {
            for (MultipartFile multipartFile : multipartFiles) {
                SignRequest signRequest = signRequestService.createSignRequest(signBook.getName() + "_" + multipartFile.getOriginalFilename(), user);
                signRequestService.addDocsToSignRequest(signRequest, multipartFile);
                signBookService.addSignRequest(signBook, signRequest);
            }
        }
        return "redirect:/user/signbooks/" + id + "/?form";
    }

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
        if(signRequestService.checkUserSignRights(user, signRequest)) {
            return "redirect:/user/signrequests/" + signRequest.getId() + "/?frameMode=true";
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
            SignRequest signRequest = signRequestService.createSignRequest(multipartFiles[0].getOriginalFilename(),  user);
            signRequestService.addDocsToSignRequest(signRequest, multipartFiles);
            signRequestService.addRecipients(signRequest, user);
            signRequestService.pendingSignRequest(signRequest, signType, false, user);
            return "redirect:/user/signrequests/" + signRequest.getId();
        } else {
            logger.warn("no file to import");
        }
        return "redirect:/user/signrequests";
    }

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
        if (signRequestService.checkUserSignRights(user, signRequest)) {
            if (signPageNumber != null && xPos != null && yPos != null && visual) {
                SignRequestParams signRequestParams = signRequestService.getCurrentSignRequestParams(signRequest);
                signRequestParams.setSignPageNumber(signPageNumber);
                signRequestParams.setXPos(xPos);
                signRequestParams.setYPos(yPos);
                signRequestParamsRepository.save(signRequestParams);
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
        } else {
            signRequestService.setStep("not_authorized");
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
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

    @GetMapping(value = "/refuse/{id}")
    public String refuse(@PathVariable("id") Long id, @RequestParam(value = "comment") String comment, RedirectAttributes redirectAttrs, HttpServletRequest request) {
        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        SignRequest signRequest = signRequestRepository.findById(id).get();
        if (!signRequestService.checkUserSignRights(user, signRequest)) {
            redirectAttrs.addFlashAttribute("messageCustom", "not authorized");
            return "redirect:/";
        }
        signRequest.setComment(comment);
        signRequestService.refuse(signRequest, user);
        return "redirect:/user/signrequests/" + signRequest.getId();
    }

    @DeleteMapping(value = "/{id}", produces = "text/html")
    public String delete(@PathVariable("id") Long id, HttpServletRequest request) {
        SignRequest signRequest = signRequestRepository.findById(id).get();
        signRequestService.delete(signRequest);
        if(signRequest.getParentSignBook() != null) {
            return "redirect:" + request.getHeader("referer");
        } else {
            return "redirect:/user/signrequests/";
        }

    }

    @RequestMapping(value = "/get-last-file-seda/{id}", method = RequestMethod.GET)
    public void getLastFileSeda(@PathVariable("id") Long id, HttpServletResponse response, Model model) {
        SignRequest signRequest = signRequestRepository.findById(id).get();
        User user = userService.getUserFromAuthentication();
        if (signRequestService.checkUserViewRights(user, signRequest)) {
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
        } else {
            logger.warn(user.getEppn() + " try to access " + signRequest.getId() + " without view rights");
        }
    }

    @RequestMapping(value = "/get-last-file/{id}", method = RequestMethod.GET)
    public void getLastFile(@PathVariable("id") Long id, HttpServletResponse response) throws IOException, SQLException {
        SignRequest signRequest = signRequestRepository.findById(id).get();
        User user = userService.getUserFromAuthentication();
        if (signRequestService.checkUserViewRights(user, signRequest)) {
            InputStream inputStream = null;
            String contentType = "";
            String fileName = "";
            if(!signRequest.getStatus().equals(SignRequestStatus.exported)) {
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
        } else {
            logger.warn(user.getEppn() + " try to access " + signRequest.getId() + " without view rights");
        }
    }

    @RequestMapping(value = "/change-step-sign-type/{id}/{step}", method = RequestMethod.GET)
    public String changeStepSignType(@PathVariable("id") Long id, @PathVariable("step") Integer step, @RequestParam(name = "signType") SignType signType) {
        User user = userService.getUserFromAuthentication();
        SignRequest signRequest = signRequestRepository.findById(id).get();
        if(signRequest.getCreateBy().equals(user.getEppn())) {
            signRequest.setSignType(signType);
        }
        return "redirect:/user/signrequests/" + id + "/?form";
    }

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

    @RequestMapping(value = "/pending/{id}", method = RequestMethod.GET)
    public String pending(@PathVariable("id") Long id,
                          @RequestParam(value = "comment", required = false) String comment,
                          HttpServletRequest request) throws EsupSignatureIOException {
        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        SignRequest signRequest = signRequestRepository.findById(id).get();
        signRequest.setComment(comment);
        if (signRequestService.checkUserViewRights(user, signRequest) && (signRequest.getStatus().equals(SignRequestStatus.draft) || signRequest.getStatus().equals(SignRequestStatus.completed))) {
            signRequestService.updateStatus(signRequest, SignRequestStatus.pending, "Envoyé pour signature", user, "SUCCESS", signRequest.getComment());
        } else {
            logger.warn(user.getEppn() + " try to send for sign " + signRequest.getId() + " without rights");
        }
        return "redirect:/user/signrequests/" + id + "/?form";
    }

    @PostMapping(value = "/add-recipients/{id}")
    public String addRecipients(@PathVariable("id") Long id,
                          @RequestParam(value = "recipientsEmails", required = false) String[] recipientsEmails,
                          @RequestParam(name = "signType") SignType signType,
                          @RequestParam(name = "allSignToComplete", required = false) Boolean allSignToComplete) {
        User user = userService.getUserFromAuthentication();
        SignRequest signRequest = signRequestRepository.findById(id).get();
        if (signRequestService.checkUserViewRights(user, signRequest)) {
            signRequestService.addRecipients(signRequest, recipientsEmails);
            signRequest.setSignType(signType);
            if(allSignToComplete != null && allSignToComplete) {
                signRequest.setAllSignToComplete(true);
            } else {
                signRequest.setAllSignToComplete(false);
            }
        } else {
            logger.warn(user.getEppn() + " try to update signRiquets " + signRequest.getId() + " without rights");
        }
        return "redirect:/user/signrequests/" + id + "/?form";
    }

    @PostMapping(value = "/comment/{id}")
    public String comment(@PathVariable("id") Long id,
                          @RequestParam(value = "comment", required = false) String comment,
                          @RequestParam(value = "pageNumber", required = false) Integer pageNumber,
                          @RequestParam(value = "posX", required = false) Integer posX,
                          @RequestParam(value = "posY", required = false) Integer posY,
                          HttpServletResponse response, RedirectAttributes redirectAttrs, Model model, HttpServletRequest request) {
        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        SignRequest signRequest = signRequestRepository.findById(id).get();
        if (signRequestService.checkUserViewRights(user, signRequest)) {
            signRequestService.updateStatus(signRequest, null, "Ajout d'un commentaire", user, "SUCCESS", comment, pageNumber, posX, posY);
        } else {
            logger.warn(user.getEppn() + " try to add comment" + signRequest.getId() + " without rights");
        }
        return "redirect:/user/signrequests/" + signRequest.getId();
    }

}
