package org.esupportail.esupsignature.web.controller.user;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.Log;
import org.esupportail.esupsignature.entity.SignBook.SignBookType;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureKeystoreException;
import org.esupportail.esupsignature.exception.EsupSignatureSignException;
import org.esupportail.esupsignature.repository.*;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.file.FileService;
import org.esupportail.esupsignature.service.pdf.PdfParameters;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.esupportail.esupsignature.service.sign.SignService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

//import org.esupportail.esupsignature.service.export.SedaExportService;

@RequestMapping("/user/signrequests")
@Controller
@Transactional
@Scope(value = "session")
public class SignRequestController {

    private static final Logger logger = LoggerFactory.getLogger(SignRequestController.class);

    @Value("${baseUrl}")
    private String baseUrl;

    @Value("${nexuVersion}")
    private String nexuVersion;

    @Value("${nexuUrl}")
    private String nexuUrl;

    @ModelAttribute("userMenu")
    public String getActiveMenu() {
        return "active";
    }

    @ModelAttribute("user")
    public User getUser() {
        return userService.getUserFromAuthentication();
    }

    private String progress = "0";

    private String password;

    private SignRequestStatus statusFilter = null;

    long startTime;

    public void setPassword(String password) {
        startTime = System.currentTimeMillis();
        this.password = password;
    }

    @Resource
    private UserService userService;

    @Resource
    private SignRequestRepository signRequestRepository;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private WorkflowStepRepository workflowStepRepository;

    @Resource
    private UserRepository userRepository;

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
    private DocumentService documentService;

    @Resource
    private PdfService pdfService;

    @Resource
    private FileService fileService;

    @Resource
    private SignService signService;

//    @Resource
//    private SedaExportService sedaExportService;

    @RequestMapping(produces = "text/html")
    public String list(
            @RequestParam(value = "statusFilter", required = false) String statusFilter,
            @RequestParam(value = "signBookId", required = false) Long signBookId,
            @RequestParam(value = "messageError", required = false) String messageError,
            @SortDefault(value = "createDate", direction = Direction.DESC) @PageableDefault(size = 5) Pageable pageable, Model model) {
        User user = userService.getUserFromAuthentication();
        workflowService.initCreatorWorkflow();

        if (statusFilter != null) {
            if (!statusFilter.equals("all")) {
                this.statusFilter = SignRequestStatus.valueOf(statusFilter);
            } else {
                this.statusFilter = null;
            }
        }

        List<SignRequest> signRequestsToSign = signRequestRepository.findByCreateByAndStatus(user.getEppn(), SignRequestStatus.pending);

        signRequestsToSign = signRequestsToSign.stream().sorted(Comparator.comparing(SignRequest::getCreateDate).reversed()).collect(Collectors.toList());
        Page<SignRequest> signRequests;
        if(this.statusFilter != null) {
            signRequests = signRequestRepository.findByCreateByAndStatus(user.getEppn(), this.statusFilter, pageable);
        } else {
            signRequests = signRequestRepository.findByCreateBy(user.getEppn(), pageable);
        }

        if (user.getKeystore() != null) {
            model.addAttribute("keystore", user.getKeystore().getFileName());
        }
        model.addAttribute("mydocs", "active");
        model.addAttribute("signRequestsToSign", signRequestsToSign);
        model.addAttribute("signBookId", signBookId);
        model.addAttribute("signRequests", signRequests);
        model.addAttribute("statusFilter", this.statusFilter);
        model.addAttribute("statuses", SignRequestStatus.values());
        model.addAttribute("messageError", messageError);
        populateEditForm(model, new SignRequest());
        return "user/signrequests/list";
    }

    @RequestMapping(value = "/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttrs) throws SQLException, IOException, Exception {
        User user = userService.getUserFromAuthentication();
        SignRequest signRequest = signRequestRepository.findById(id).get();
        signRequest.setSignedDocuments(signRequest.getSignedDocuments().stream().sorted(Comparator.comparing(Document::getCreateDate)).collect(Collectors.toList()));
        if (signRequestService.checkUserViewRights(user, signRequest) || signRequestService.checkUserSignRights(user, signRequest)) {
            model.addAttribute("signBooks", signBookService.getAllSignBooks());
            Document toDisplayDocument = null;
            List<Log> logs = logRepository.findBySignRequestId(signRequest.getId());
            logs = logs.stream().sorted(Comparator.comparing(Log::getLogDate).reversed()).collect(Collectors.toList());

            model.addAttribute("logs", logs);
            model.addAttribute("comments", logs.stream().filter(log -> log.getComment() != null && !log.getComment().isEmpty()).collect(Collectors.toList()));
            if (user.getSignImage() != null) {
                model.addAttribute("signFile", fileService.getBase64Image(user.getSignImage()));
            }
            if (user.getKeystore() != null) {
                model.addAttribute("keystore", user.getKeystore().getFileName());
            }
            //workflowService.setWorkflowsLabels(signRequest.getParentSignBook().getWorkflowSteps());
            model.addAttribute("signRequest", signRequest);

            if (signRequest.getStatus().equals(SignRequestStatus.pending) && signRequestService.checkUserSignRights(user, signRequest) && signRequest.getOriginalDocuments().size() > 0) {
                model.addAttribute("signable", "ok");
            }
            model.addAttribute("signTypes", SignType.values());
            model.addAttribute("allSignBooks", signBookRepository.findBySignBookType(SignBookType.group));
            model.addAttribute("workflows", workflowRepository.findAll());
            model.addAttribute("baseUrl", baseUrl);
            model.addAttribute("nexuVersion", nexuVersion);
            model.addAttribute("nexuUrl", nexuUrl);
            return "user/signrequests/update";
        } else {
            logger.warn(user.getEppn() + " attempted to access signRequest " + id + " without write access");
            redirectAttrs.addFlashAttribute("messageCustom", "not autorized");
            return "redirect:/user/signrequests/";
        }
    }

    @RequestMapping(value = "/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model model) throws Exception {
        User user = userService.getUserFromAuthentication();
        SignRequest signRequest = signRequestRepository.findById(id).get();
        model.addAttribute("signRequest", signRequest);
        if ((signRequestService.checkUserViewRights(user, signRequest) || signRequestService.checkUserSignRights(user, signRequest))) {
            if (signRequest.getStatus().equals(SignRequestStatus.pending) && signRequestService.checkUserSignRights(user, signRequest) && signRequest.getOriginalDocuments().size() > 0) {
                model.addAttribute("signable", "ok");
            }
            Document toDisplayDocument;
            if (signRequestService.getToSignDocuments(signRequest).size() == 1) {
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
                model.addAttribute("documentId", toDisplayDocument.getId());
            }
        }
        List<Log> logs = logRepository.findBySignRequestIdAndPageNumberIsNotNull(id);
        model.addAttribute("postits", logRepository.findBySignRequestIdAndPageNumberIsNotNull(signRequest.getId()));
        return "user/signrequests/show";
    }

    @PostMapping(produces = "text/html")
    public String create(@Valid SignRequest signRequest, HttpServletRequest request) {
        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        signRequest = signRequestService.createSignRequest(signRequest, user);
        return "redirect:/user/signrequests/" + signRequest.getId() + "/?form";
    }

    @ResponseBody
    @RequestMapping(value = "/add-doc/{id}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Object addDocument(@PathVariable("id") Long id,
                              @RequestParam("multipartFiles") MultipartFile[] multipartFiles, HttpServletRequest request) {
        logger.info("start add documents");
        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        SignRequest signRequest = signRequestRepository.findById(id).get();
        if (signRequestService.checkUserViewRights(user, signRequest)) {
            List<Document> documents = documentService.createDocuments(multipartFiles);
            signRequestService.addOriginalDocuments(signRequest, documents);
            signRequestRepository.save(signRequest);
        }
        String[] ok = {"ok"};
        return ok;
    }

    @ResponseBody
    @RequestMapping(value = "/remove-doc/{id}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Object removeDocument(@PathVariable("id") Long id, HttpServletRequest request) {
        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        Document document = documentRepository.findById(id).get();
        SignRequest signRequest = signRequestRepository.findById(document.getParentId()).get();
        if (signRequest.getCreateBy().equals(user.getEppn())) {
            signRequest.getOriginalDocuments().remove(document);
            documentService.deleteDocument(document);
        }
        String[] ok = {"ok"};
        return ok;
    }

    @ResponseBody
    @GetMapping("/wait-for-sign/{token}")
    public DeferredResult<String> waitForSign(@PathVariable("token") String token) {
        DeferredResult<String> output = new DeferredResult<>();
        User user = userService.getUserFromAuthentication();
        ForkJoinPool.commonPool().submit(() -> {
            try {
                int tryNumber = 0;
                while (true) {
                    logger.info(user.getEppn() + " is waiting for sign " + token);
                    tryNumber++;
                    if(signRequestRepository.findByToken(token).get(0).getStatus().equals(SignRequestStatus.completed) || tryNumber > 30){
                        break;
                    }
                    Thread.sleep(2000);
                }
            } catch (InterruptedException e) {
                logger.info(e.getMessage());
            }
            output.setResult("ok");
        });
        return output;
    }

    @RequestMapping(value = "/fast-sign-request", method = RequestMethod.POST)
    public String createSignRequest(@RequestParam("multipartFile") MultipartFile multipartFile, @RequestParam("signType") SignType signType) throws EsupSignatureIOException {
        logger.info("création rapide demande de signature");
        User user = userService.getUserFromAuthentication();
        if (multipartFile != null) {
            Document documentToAdd = documentService.createDocument(multipartFile, multipartFile.getOriginalFilename());
            SignRequest signRequest = signRequestService.createSignRequest(new SignRequest(), user, documentToAdd);
            signRequest.setTitle(fileService.getNameOnly(documentToAdd.getFileName()));
            signRequest.setSignType(signType);
            signRequest.getRecipients().put(user.getId(), false);
            signRequestRepository.save(signRequest);
            signRequestService.pendingSignRequest(signRequest, user);
            logger.info("adding new file into signRequest " + signRequest.getToken());
            return "redirect:/user/signrequests/" + signRequest.getId();
        } else {
            logger.warn("no file to import");
        }
        return "redirect:/user/signrequests";
    }

    @RequestMapping(value = "/sign/{id}", method = RequestMethod.POST)
    public String sign(@PathVariable("id") Long id,
                       @RequestParam(value = "xPos", required = false) Integer xPos,
                       @RequestParam(value = "yPos", required = false) Integer yPos,
                       @RequestParam(value = "comment", required = false) String comment,
                       @RequestParam(value = "addDate", required = false) Boolean addDate,
                       @RequestParam(value = "visual", required = false) Boolean visual,
                       @RequestParam(value = "signPageNumber", required = false) Integer signPageNumber,
                       @RequestParam(value = "password", required = false) String password,
                       @RequestParam(value = "referer", required = false) String referer,
                       RedirectAttributes redirectAttrs, HttpServletRequest request) {
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
            if (signRequestService.getCurrentSignType(signRequest).equals(SignType.nexuSign)) {
                return "redirect:/user/nexu-sign/" + id + "?referer=" + referer;
            }
            if (signPageNumber != null && xPos != null && yPos != null) {
                signRequestService.getCurrentSignRequestParams(signRequest).setSignPageNumber(signPageNumber);
                signRequestService.getCurrentSignRequestParams(signRequest).setXPos(xPos);
                signRequestService.getCurrentSignRequestParams(signRequest).setYPos(yPos);
                signRequestRepository.save(signRequest);
            }
            if (!"".equals(password)) {
                setPassword(password);
            }
            try {
                signRequest.setComment(comment);
                signRequestService.sign(signRequest, user, this.password, addDate, visual);
            } catch (EsupSignatureKeystoreException e) {
                logger.error("keystore error", e);
                redirectAttrs.addFlashAttribute("messageError", "security_bad_password");
                progress = "security_bad_password";
            } catch (EsupSignatureSignException e) {
                redirectAttrs.addFlashAttribute("messageCustom", e.getMessage());
                logger.error(e.getMessage(), e);
            } catch (IOException e) {
                logger.error(e.getMessage());
            } catch (EsupSignatureException e) {
                redirectAttrs.addFlashAttribute("messageCustom", e.getMessage());
                logger.error(e.getMessage(), e);
            }
            if (referer != null && !"".equals(referer) && !"null".equals(referer)) {
                return "redirect:" + referer;
            } else {
                return "redirect:/user/signrequests/" + signRequest.getId();
            }
        } else {
            redirectAttrs.addFlashAttribute("messageCustom", "not autorized");
            progress = "not_autorized";
            return "redirect:/user/signrequests/";
        }
    }

    @ResponseBody
    @RequestMapping(value = "/get-step")
    public String getStep() {
        logger.debug("getStep : " + signRequestService.getStep());
        return signRequestService.getStep();
    }

    @ResponseBody
    @RequestMapping(value = "/get-progress", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getProgress() {
        logger.debug("getProgress : " + progress);
        return progress;
    }

    @RequestMapping(value = "/refuse/{id}")
    public String refuse(@PathVariable("id") Long id, @RequestParam(value = "comment") String comment, RedirectAttributes redirectAttrs, HttpServletResponse response,
                         Model model, HttpServletRequest request) throws SQLException {
        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        SignRequest signRequest = signRequestRepository.findById(id).get();
        if (!signRequestService.checkUserSignRights(user, signRequest)) {
            redirectAttrs.addFlashAttribute("messageCustom", "not autorized");
            return "redirect:/";
        }
        signRequest.setComment(comment);
        signRequestService.refuse(signRequest, user);
        return "redirect:/user/signbooks/" + signRequest.getParentSignBook().getId() + "/0";
    }

    @DeleteMapping(value = "/{id}", produces = "text/html")
    public String delete(@PathVariable("id") Long id, Model model) {
        SignRequest signRequest = signRequestRepository.findById(id).get();
        List<Log> logs = logRepository.findBySignRequestId(id);
        for (Log log : logs) {
            logRepository.delete(log);
        }
        signRequestRepository.save(signRequest);
        signRequestRepository.delete(signRequest);
        model.asMap().clear();
        return "redirect:/user/signrequests/";
    }

    @RequestMapping(value = "/get-last-file-by-token/{token}", method = RequestMethod.GET)
    public void getLastFileByToken(@PathVariable("token") String token, HttpServletResponse response, Model model) {
        User user = userService.getUserFromAuthentication();
        SignRequest signRequest = signRequestRepository.findByToken(token).get(0);
        if (signRequestService.checkUserViewRights(user, signRequest)) {
            getLastFile(signRequest.getId(), response, model);
        } else {
            logger.warn(user.getEppn() + " try to access " + signRequest.getId() + " without view rights");
        }
    }
//
//    @RequestMapping(value = "/get-last-file-seda/{id}", method = RequestMethod.GET)
//    public void getLastFileSeda(@PathVariable("id") Long id, HttpServletResponse response, Model model) {
//        SignRequest signRequest = signRequestRepository.findById(id).get();
//        User user = userService.getUserFromAuthentication();
//        if (signRequestService.checkUserViewRights(user, signRequest)) {
//            List<Document> documents = signRequestService.getToSignDocuments(signRequest);
//            try {
//                if (documents.size() > 1) {
//                    response.sendRedirect("/user/signrequests/" + id);
//                } else {
//                    Document document = documents.get(0);
//                    response.setHeader("Content-Disposition", "inline;filename=test-seda.zip");
//                    response.setContentType("application/zip");
//                    IOUtils.copy(sedaExportService.generateSip(signRequest), response.getOutputStream());
//                }
//            } catch (Exception e) {
//                logger.error("get file error", e);
//            }
//        } else {
//            logger.warn(user.getEppn() + " try to access " + signRequest.getId() + " without view rights");
//        }
//    }


    @RequestMapping(value = "/get-last-file/{id}", method = RequestMethod.GET)
    public void getLastFile(@PathVariable("id") Long id, HttpServletResponse response, Model model) {
        SignRequest signRequest = signRequestRepository.findById(id).get();
        User user = userService.getUserFromAuthentication();
        if (signRequestService.checkUserViewRights(user, signRequest)) {
            List<Document> documents = signRequestService.getToSignDocuments(signRequest);
            try {
                if (documents.size() > 1) {
                    response.sendRedirect("/user/signrequests/" + id);
                } else {
                    Document document = documents.get(0);
                    response.setHeader("Content-Disposition", "inline;filename=\"" + document.getFileName() + "\"");
                    response.setContentType(document.getContentType());
                    IOUtils.copy(document.getBigFile().getBinaryFile().getBinaryStream(), response.getOutputStream());
                }
            } catch (Exception e) {
                logger.error("get file error", e);
            }
        } else {
            logger.warn(user.getEppn() + " try to access " + signRequest.getId() + " without view rights");
        }
    }

    /*
        @RequestMapping(value = "/toggle-need-all-sign/{id}/{step}", method = RequestMethod.GET)
        public String toggleNeedAllSign(@PathVariable("id") Long id,@PathVariable("step") Integer step) {
            User user = userService.getUserFromAuthentication();
            SignRequest signRequest = signRequestRepository.findById(id).get();
            if(user.getEppn().equals(signRequest.getCreateBy())) {
                signRequestService.toggleNeedAllSign(signRequest, step);
            }
            return "redirect:/user/signrequests/" + id;
        }
    */

    @RequestMapping(value = "/change-step-sign-type/{id}/{step}", method = RequestMethod.GET)
    public String changeStepSignType(@PathVariable("id") Long id, @PathVariable("step") Integer step, @RequestParam(name = "signType") SignType signType) {
        User user = userService.getUserFromAuthentication();
        SignRequest signRequest = signRequestRepository.findById(id).get();
        if(signRequest.getCreateBy().equals(user.getEppn())) {
            signRequest.setSignType(signType);
            signRequestRepository.save(signRequest);
        }
        return "redirect:/user/signrequests/" + id + "/?form";
    }

    @RequestMapping(value = "/complete/{id}", method = RequestMethod.GET)
    public String complete(@PathVariable("id") Long id,
                           @RequestParam(value = "comment", required = false) String comment,
                           HttpServletResponse response, RedirectAttributes redirectAttrs, HttpServletRequest request) throws EsupSignatureException {
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
                          HttpServletResponse response, RedirectAttributes redirectAttrs, Model model, HttpServletRequest request) throws IOException, EsupSignatureIOException {
        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        //TODO controle signType
        SignRequest signRequest = signRequestRepository.findById(id).get();
        signRequest.setComment(comment);
        if (signRequestService.checkUserViewRights(user, signRequest) && (signRequest.getStatus().equals(SignRequestStatus.draft) || signRequest.getStatus().equals(SignRequestStatus.completed))) {
            signRequestService.pendingSignRequest(signRequest, user);
        } else {
            logger.warn(user.getEppn() + " try to send for sign " + signRequest.getId() + " without rights");
        }
        return "redirect:/user/signrequests/" + id + "/?form";
    }

    @PostMapping(value = "/add-recipients/{id}")
    public String pending(@PathVariable("id") Long id,
                          @RequestParam(value = "recipientsEmail", required = false) String[] recipientsEmail,
                          @RequestParam(name = "signType") SignType signType,
                          @RequestParam(name = "allSignToComplete", required = false) Boolean allSignToComplete) {
        System.err.println(allSignToComplete);
        User user = userService.getUserFromAuthentication();
        SignRequest signRequest = signRequestRepository.findById(id).get();
        if (signRequestService.checkUserViewRights(user, signRequest)) {
            signRequest.getRecipients().clear();
            signRequestService.addRecipients(signRequest, Arrays.asList(recipientsEmail));
            signRequest.setSignType(signType);
            signRequest.setAllSignToComplete(false);
        } else {
            logger.warn(user.getEppn() + " try to update signRiquets " + signRequest.getId() + " without rights");
        }
        return "redirect:/user/signrequests/" + id + "/?form";
    }

    @RequestMapping(value = "/scan-pdf-sign/{id}", method = RequestMethod.GET)
    public String scanPdfSign(@PathVariable("id") Long id,
                              RedirectAttributes redirectAttrs, HttpServletRequest request) throws IOException, EsupSignatureIOException {
        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        SignRequest signRequest = signRequestRepository.findById(id).get();
        int nbSignFound = signRequestService.scanSignatureFields(signRequest);
        redirectAttrs.addFlashAttribute("messageInfo", "Scan terminé, " + nbSignFound + " signature(s) trouvée(s)");
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
        return "redirect:/user/signrequest/" + signRequest.getId();
    }

    void populateEditForm(Model model, SignRequest signRequest) {
        model.addAttribute("signRequest", signRequest);
        model.addAttribute("signTypes", Arrays.asList(SignType.values()));
    }

    @Scheduled(fixedDelay = 5000)
    public void clearPassword() {
        password = "";
        if (startTime > 0) {
            if (System.currentTimeMillis() - startTime > signService.getPasswordTimeout()) {
                password = "";
                startTime = 0;
            }
        }
    }
}
