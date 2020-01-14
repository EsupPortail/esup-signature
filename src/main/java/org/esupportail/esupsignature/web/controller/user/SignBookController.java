package org.esupportail.esupsignature.web.controller.user;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureKeystoreException;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//import org.esupportail.esupsignature.service.export.SedaExportService;

@RequestMapping("/user/signbooks")
@Controller
@Transactional
@Scope(value = "session")
public class SignBookController {

    private static final Logger logger = LoggerFactory.getLogger(SignBookController.class);

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
    public String list(@SortDefault(value = "createDate", direction = Direction.DESC) @PageableDefault(size = 5) Pageable pageable, Model model) {
        User user = userService.getUserFromAuthentication();
        workflowService.initCreatorWorkflow();
        List<SignBook> signBooksToSign = signBookService.getTosignRequests(user);
        model.addAttribute("signBooksToSign", signBooksToSign);
        model.addAttribute("signBooksCreateByCurrentUser", signBookRepository.findByCreateBy(user.getEppn(), pageable));
        model.addAttribute("statusFilter", this.statusFilter);
        model.addAttribute("statuses", SignRequestStatus.values());
        populateEditForm(model, new SignRequest());
        return "user/signbooks/list";
    }

    @RequestMapping(value = "/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable("id") Long id, Model model) {
        User user = userService.getUserFromAuthentication();
        SignBook signBook = signBookRepository.findById(id).get();
        workflowService.setWorkflowsLabels(signBook.getWorkflowSteps());
        model.addAttribute("signBook", signBook);
        model.addAttribute("signTypes", SignType.values());
        return "user/signbooks/update";

    }

    @RequestMapping(value = "/{id}/{signRequestId}", produces = "text/html")
    public String show(@PathVariable("id") Long id, @PathVariable("signRequestId") Long signRequestId, Model model) throws Exception {
        User user = userService.getUserFromAuthentication();
        SignBook signBook = signBookRepository.findById(id).get();
        workflowService.setWorkflowsLabels(signBook.getWorkflowSteps());
        model.addAttribute("signBook", signBook);
        SignRequest signRequest = signRequestRepository.findById(signRequestId).get();
        if (signBook.getSignRequests().contains(signRequest) && (signRequestService.checkUserViewRights(user, signRequest) || signRequestService.checkUserSignRights(user, signRequest))) {
            Document toDisplayDocument;
            if (signRequestService.getToSignDocuments(signRequest).size() == 1) {
                toDisplayDocument = signRequestService.getToSignDocuments(signRequest).get(0);
                if (toDisplayDocument.getContentType().equals("application/pdf")) {
                    PdfParameters pdfParameters = pdfService.getPdfParameters(toDisplayDocument.getInputStream());
                    model.addAttribute("pdfWidth", pdfParameters.getWidth());
                    model.addAttribute("pdfHeight", pdfParameters.getHeight());
                    model.addAttribute("imagePagesSize", pdfParameters.getTotalNumberOfPages());
                }
                model.addAttribute("documentType", fileService.getExtension(toDisplayDocument.getFileName()));
                model.addAttribute("documentId", toDisplayDocument.getId());
            }
            if (signBookService.getStatus(signRequest.getParentSignBook()).equals(SignRequestStatus.pending) && signRequest.getStatus().equals(SignRequestStatus.pending) && signRequestService.checkUserSignRights(user, signRequest) && signRequest.getOriginalDocuments().size() > 0) {
                model.addAttribute("signable", "ok");
                if(!signRequestService.getCurrentSignType(signRequest).equals(SignType.visa)
                        && user.getSignImage() != null
                        && user.getSignImage().getSize() > 0) {
                    model.addAttribute("signFile", fileService.getBase64Image(user.getSignImage()));
                    int[] size = pdfService.getSignSize(user.getSignImage().getInputStream());
                    model.addAttribute("signWidth", size[0]);
                    model.addAttribute("signHeight", size[1]);
                } else {
                    model.addAttribute("signWidth", 100);
                    model.addAttribute("signHeight", 75);
                }
            }
            if(signRequest.getSignRequestParams().size() < signRequest.getParentSignBook().getCurrentWorkflowStepNumber()) {
                signRequest.getSignRequestParams().add(signRequestService.getEmptySignRequestParams());
                signRequestRepository.save(signRequest);
            }
        }
        model.addAttribute("postits", logRepository.findBySignRequestIdAndPageNumberIsNotNull(signRequest.getId()));
        model.addAttribute("refusLogs", logRepository.findBySignRequestIdAndFinalStatus(signRequest.getId(), SignRequestStatus.refused.name()));
        model.addAttribute("signRequest", signRequest);
        model.addAttribute("signRequestId", signRequestId);
        return "user/signbooks/show";
    }

//    @RequestMapping(value = "/sign/{id}", method = RequestMethod.POST)
//    public void sign(
//            @PathVariable("id") Long id,
//            @RequestParam(value = "comment", required = false) String comment,
//            @RequestParam(value = "password", required = false) String password, HttpServletRequest request) throws IOException {
//        User user = userService.getUserFromAuthentication();
//        user.setIp(request.getRemoteAddr());
//        float nbSigned = 0;
//        progress = "0";
//        SignBook signBook = signBookRepository.findById(id).get();
//        for (SignRequest signRequest : signBook.getSignRequests()) {
//            if (signRequestService.checkUserSignRights(user, signRequest)) {
//                if (!"".equals(password)) {
//                    setPassword(password);
//                }
//                try {
//                    if (signBookService.getCurrentWorkflowStep(signBook).getSignType().equals(SignType.visa)) {
//                        signRequestService.updateStatus(signRequest, SignRequestStatus.checked, "Visa", user, "SUCCESS", comment);
//                    } else if (signBookService.getCurrentWorkflowStep(signBook).getSignType().equals(SignType.nexuSign)) {
//                        logger.error("no multiple nexu sign");
//                        progress = "not_autorized";
//                    } else {
//                        signRequestService.sign(signRequest, user, this.password, false, false);
//                    }
//                } catch (EsupSignatureKeystoreException e) {
//                    logger.error("keystore error", e);
//                    progress = "security_bad_password";
//                    break;
//                } catch (EsupSignatureException e) {
//                    logger.error(e.getMessage(), e);
//                }
//            } else {
//                logger.error("not autorized to sign");
//                progress = "not_autorized";
//            }
//            nbSigned++;
//            float percent = (nbSigned / signBook.getSignRequests().size()) * 100;
//            progress = String.valueOf((int) percent);
//        }
//    }

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
    public String refuse(@PathVariable("id") Long id, @RequestParam(value = "comment", required = true) String comment, RedirectAttributes redirectAttrs, HttpServletResponse response,
                         Model model, HttpServletRequest request) throws SQLException {
        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        SignRequest signRequest = signRequestRepository.findById(id).get();
        if (!signRequestService.checkUserSignRights(user, signRequest)) {
            redirectAttrs.addFlashAttribute("messageCustom", "not autorized");
            return "redirect:/user/signbooks/" + id;
        }
        signRequest.setComment(comment);
        signRequestService.refuse(signRequest, user);
        return "redirect:/user/signbooks/";
    }

    @DeleteMapping(value = "/{id}", produces = "text/html")
    public String delete(@PathVariable("id") Long id, Model model) {
        SignBook signBook = signBookRepository.findById(id).get();
        List<SignRequest>  signRequests = new ArrayList<>();
        signRequests.addAll(signBook.getSignRequests());
        signBook.getSignRequests().clear();
        for(SignRequest signRequest : signRequests) {
            signRequestService.delete(signRequest);
        }
        signBook.getSignRequests().clear();
        signBookRepository.delete(signBook);
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

//    @RequestMapping(value = "/get-last-file-seda/{id}", method = RequestMethod.GET)
//    public void getLastFileSeda(@PathVariable("id") Long id, HttpServletResponse response, Model model) {
//        SignRequest signRequest = signRequestRepository.findById(id).get();
//        User user = userService.getUserFromAuthentication();
//        if (signRequestService.checkUserViewRights(user, signRequest)) {
//            List<Document> documents = signRequestService.getToSignDocuments(signRequest);
//            try {
//                if (documents.size() > 1) {
//                    response.sendRedirect("/user/signbooks/" + id);
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
                    response.sendRedirect("/user/signbooks/" + id);
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

    @RequestMapping(value = "/change-step-sign-type/{id}/{step}", method = RequestMethod.GET)
    public String changeStepSignType(@PathVariable("id") Long id, @PathVariable("step") Integer step, @RequestParam(name="signType") SignType signType) {
        User user = userService.getUserFromAuthentication();
        SignBook signBook = signBookRepository.findById(id).get();
        if(user.getEppn().equals(signBook.getCreateBy()) && signBook.getCurrentWorkflowStepNumber() <= step + 1) {
            signBookService.changeSignType(signBook, step, null, signType);
            return "redirect:/user/signbooks/" + id + "/?form";
        }
        return "redirect:/user/signbooks/";
    }

    @RequestMapping(value = "/update-step/{id}/{step}", method = RequestMethod.GET)
    public String changeStepSignType(@PathVariable("id") Long id,
                                     @PathVariable("step") Integer step,
                                     @RequestParam(name="name", required = false) String name,
                                     @RequestParam(name="signType") SignType signType,
                                     @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete) {
        User user = userService.getUserFromAuthentication();
        SignBook signBook = signBookRepository.findById(id).get();
        if(user.getEppn().equals(signBook.getCreateBy()) && signBook.getCurrentWorkflowStepNumber() <= step + 1) {
            if(allSignToComplete == null) {
                allSignToComplete = false;
            }
            signBookService.changeSignType(signBook, step, name, signType);
            signBookService.toggleNeedAllSign(signBook, step, allSignToComplete);
            return "redirect:/user/signbooks/" + id + "/?form";
        }
        return "redirect:/user/signbooks/";
    }

    @PostMapping(value = "/add-step/{id}")
    public String addStep(@PathVariable("id") Long id,
                          @RequestParam(value = "name", required = false) String name,
                          @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete,
                          @RequestParam("signType") String signType) {
        User user = userService.getUserFromAuthentication();
        SignBook signBook = signBookRepository.findById(id).get();
        if (signBookService.checkUserViewRights(user, signBook)) {
            WorkflowStep workflowStep = workflowService.createWorkflowStep(null, name, allSignToComplete, SignType.valueOf(signType));
            signBook.getWorkflowSteps().add(workflowStep);
        }
        return "redirect:/user/signbooks/" + id + "/?form";
    }

    @DeleteMapping(value = "/remove-step/{id}/{step}")
    public String removeStep(@PathVariable("id") Long id, @PathVariable("step") Integer step) {
        User user = userService.getUserFromAuthentication();
        SignBook signBook = signBookRepository.findById(id).get();
        if(user.getEppn().equals(signBook.getCreateBy()) && signBook.getCurrentWorkflowStepNumber() <= step + 1) {
            signBookService.removeStep(signBook, step);
        }
        return "redirect:/user/signbooks/" + id + "/?form";
    }

    @PostMapping(value = "/add-workflow/{id}")
    public String addWorkflow(@PathVariable("id") Long id,
                          @RequestParam(value = "workflowSignBookId") Long workflowSignBookId) {
        User user = userService.getUserFromAuthentication();
        SignBook signBook = signBookRepository.findById(id).get();
        if (signBookService.checkUserViewRights(user, signBook)) {
            Workflow workflow = workflowRepository.findById(workflowSignBookId).get();
            signBookService.importWorkflow(signBook, workflow, user);
        }
        return "redirect:/user/signbooks/" + id + "/?form";
    }

    @RequestMapping(value = "/send-to-signbook/{id}/{workflowStepId}", method = RequestMethod.GET)
    public String sendToSignBook(@PathVariable("id") Long id,
                                 @PathVariable("workflowStepId") Long workflowStepId,
                                 @RequestParam(value = "signBookNames", required = true) String[] signBookNames,
                                 RedirectAttributes redirectAttrs, HttpServletRequest request) {
        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        SignBook signBook = signBookRepository.findById(id).get();
        if (signBookService.checkUserViewRights(user, signBook)) {
            WorkflowStep workflowStep = workflowStepRepository.findById(workflowStepId).get();
            if (signBookNames != null && signBookNames.length > 0) {
                workflowService.addRecipientsToWorkflowStep(Arrays.asList(signBookNames), workflowStep, user);
            }
        } else {
            logger.warn(user.getEppn() + " try to move " + signBook.getId() + " without rights");
        }
        return "redirect:/user/signbooks/" + id + "/?form";
    }

    @DeleteMapping(value = "/remove-step-recipent/{id}/{workflowStepId}")
    public String removeStepRecipient(@PathVariable("id") Long id,
                                 @PathVariable("workflowStepId") Long workflowStepId,
                                 @RequestParam(value = "recipientName") String recipientEmail,
                                 RedirectAttributes redirectAttrs, HttpServletRequest request) {
        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        SignRequest signRequest = signRequestRepository.findById(id).get();
        WorkflowStep workflowStep = workflowStepRepository.findById(workflowStepId).get();
        if (signRequestService.checkUserViewRights(user, signRequest)) {
            User userToRemove = userRepository.findByEmail(recipientEmail).get(0);
            workflowStep.getRecipients().remove(userToRemove.getId());
            workflowStepRepository.save(workflowStep);
        } else {
            logger.warn(user.getEppn() + " try to move " + signRequest.getId() + " without rights");
        }
        return "redirect:/user/signbooks/" + id + "/?form";
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
        return "redirect:/user/signbooks/" + id + "/?form";
    }

//    @RequestMapping(value = "/pending/{id}", method = RequestMethod.GET)
//    public String pending(@PathVariable("id") Long id,
//                          @RequestParam(value = "comment", required = false) String comment,
//                          HttpServletResponse response, RedirectAttributes redirectAttrs, Model model, HttpServletRequest request) throws IOException {
//        logger.info("démarrage de la demande");
//        User user = userService.getUserFromAuthentication();
//        user.setIp(request.getRemoteAddr());
//        //TODO controle signType
//        SignRequest signRequest = signRequestRepository.findById(id).get();
//        signRequest.setComment(comment);
//        if (signRequestService.checkUserViewRights(user, signRequest) && (signRequest.getStatus().equals(SignRequestStatus.draft) || signRequest.getStatus().equals(SignRequestStatus.completed))) {
//            signRequestService.pendingSignRequest(signRequest, user);
//        } else {
//            logger.warn(user.getEppn() + " try to send for sign " + signRequest.getId() + " without rights");
//        }
//        return "redirect:/user/signbooks/" + id + "/?form";
//    }

    @RequestMapping(value = "/scan-pdf-sign/{id}", method = RequestMethod.GET)
    public String scanPdfSign(@PathVariable("id") Long id,
                          RedirectAttributes redirectAttrs, HttpServletRequest request) throws IOException {
        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        SignBook signBook = signBookRepository.findById(id).get();
        for(SignRequest signRequest : signBook.getSignRequests()) {
            if (signBook.getCurrentWorkflowStepNumber() == 1 && signRequest.getOriginalDocuments().size() == 1 && signRequest.getOriginalDocuments().get(0).getContentType().contains("pdf")) {
                try {
                    int nbSignFound = signRequestService.scanSignatureFields(signRequest);
                    redirectAttrs.addFlashAttribute("messageInfo", "Scan terminé, " + nbSignFound + " signature(s) trouvée(s)");
                } catch (EsupSignatureIOException e) {
                    logger.error("unable to scan the pdf document from " + signRequest.getId(), e);
                }
            }
        }
        return "redirect:/user/signbooks/" + id + "/?form";
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
        return "redirect:/user/signbooks/" + signRequest.getParentSignBook().getId() + "/" + signRequest.getParentSignBook().getSignRequests().indexOf(signRequest);
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
