package org.esupportail.esupsignature.web.controller.user;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.SignBook.SignBookType;
import org.esupportail.esupsignature.entity.SignRequest.SignRequestStatus;
import org.esupportail.esupsignature.entity.SignRequestParams.NewPageType;
import org.esupportail.esupsignature.entity.SignRequestParams.SignType;
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
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*")
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
    private SignRequestParamsRepository signRequestParamsRepository;

    @Resource
    private SignBookRepository signBookRepository;

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

    @RequestMapping(produces = "text/html")
    public String list(
            @RequestParam(value = "statusFilter", required = false) String statusFilter,
            @RequestParam(value = "signBookId", required = false) Long signBookId,
            @RequestParam(value = "messageError", required = false) String messageError,
            @SortDefault(value = "createDate", direction = Direction.DESC) @PageableDefault(size = 5) Pageable pageable, RedirectAttributes redirectAttrs, Model model) {
        User user = userService.getUserFromAuthentication();
        if (user == null || !userService.isUserReady(user)) {
            return "redirect:/user/users/?form";
        }

        if (statusFilter != null) {
            if (!statusFilter.equals("all")) {
                this.statusFilter = SignRequestStatus.valueOf(statusFilter);
            } else {
                this.statusFilter = null;
            }
        }

        List<SignRequest> signRequestsToSign = signRequestService.getTosignRequests(user);

        signRequestsToSign = signRequestsToSign.stream().sorted(Comparator.comparing(SignRequest::getCreateDate).reversed()).collect(Collectors.toList());

        Page<SignRequest> signRequests = signRequestRepository.findBySignResquestByCreateByAndStatus(user.getEppn(), this.statusFilter, pageable);

        for (SignRequest signRequest : signRequests) {
            signRequestService.setSignBooksLabels(signRequest.getWorkflowSteps());
        }
        if (user.getKeystore() != null) {
            model.addAttribute("keystore", user.getKeystore().getFileName());
        }
        model.addAttribute("signType", signBookService.getUserSignBook(user).getSignRequestParams().getSignType());
        model.addAttribute("mydocs", "active");
        model.addAttribute("signRequestsToSign", signRequestsToSign);
        model.addAttribute("signBookId", signBookId);
        model.addAttribute("signRequests", signRequests);
        model.addAttribute("statusFilter", this.statusFilter);
        model.addAttribute("statuses", SignRequest.SignRequestStatus.values());
        model.addAttribute("messageError", messageError);

        return "user/signrequests/list";
    }

    @RequestMapping(value = "/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttrs) throws SQLException, IOException, Exception {
        User user = userService.getUserFromAuthentication();
        if (!userService.isUserReady(user)) {
            return "redirect:/user/users/?form";
        }
        SignRequest signRequest = signRequestRepository.findById(id).get();
        signRequest.setSignedDocuments(signRequest.getSignedDocuments().stream().sorted(Comparator.comparing(Document::getCreateDate)).collect(Collectors.toList()));
        if (signRequestService.checkUserViewRights(user, signRequest) || signRequestService.checkUserSignRights(user, signRequest)) {
            model.addAttribute("signBooks", signBookService.getAllSignBooks());
            Document toDisplayDocument = null;
            if (signRequestService.getToSignDocuments(signRequest).size() == 1) {
                toDisplayDocument = signRequestService.getToSignDocuments(signRequest).get(0);
                if (toDisplayDocument.getContentType().equals("application/pdf")) {
                    PdfParameters pdfParameters = pdfService.getPdfParameters(toDisplayDocument.getInputStream());
                    if (pdfParameters != null) {
                        model.addAttribute("pdfWidth", pdfParameters.getWidth());
                        model.addAttribute("pdfHeight", pdfParameters.getHeight());
                        model.addAttribute("imagePagesSize", pdfParameters.getTotalNumberOfPages());
                    }
                    if (user.getSignImage() != null) {
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
            List<Log> logs = logRepository.findBySignRequestId(signRequest.getId());
            model.addAttribute("logs", logs);
            model.addAttribute("comments", logs.stream().filter(log -> log.getComment() != null && !log.getComment().isEmpty()).collect(Collectors.toList()));
            if (user.getSignImage() != null) {
                model.addAttribute("signFile", fileService.getBase64Image(user.getSignImage()));
            }
            if (user.getKeystore() != null) {
                model.addAttribute("keystore", user.getKeystore().getFileName());
            }

           // signRequest.setOriginalSignBooks(signBookService.getOriginalSignBook(signRequest));
            //TODO choix signType
            //			if (toSignDocuments.size() == 1 && toSignDocuments.get(0).getContentType().equals("application/pdf")) {
            signRequestService.setSignBooksLabels(signRequest.getWorkflowSteps());

            model.addAttribute("signRequest", signRequest);
            model.addAttribute("itemId", id);
            if (signRequest.getStatus().equals(SignRequestStatus.pending) && signRequestService.checkUserSignRights(user, signRequest) && signRequest.getOriginalDocuments().size() > 0) {
                model.addAttribute("signable", "ok");
            }
            List<SignBook> firstOriginalSignBooks = signBookService.getSignBookBySignRequest(signRequest);
            if (firstOriginalSignBooks.size() > 0) {
                SignBook firstOriginalSignBook = signBookService.getSignBookBySignRequest(signRequest).get(0);
                if (firstOriginalSignBook.getSignBookType().equals(SignBookType.workflow)) {
                    model.addAttribute("firstOriginalSignBook", firstOriginalSignBook);
                }
                if (firstOriginalSignBook.getModelFile() != null) {
                    model.addAttribute("modelId", firstOriginalSignBook.getModelFile().getUrl());
                }
            }
            model.addAttribute("allSignBooks", signBookRepository.findByNotCreateBy("System"));
            model.addAttribute("nbSignOk", signRequest.countSignOk());
            model.addAttribute("baseUrl", baseUrl);
            model.addAttribute("nexuVersion", nexuVersion);
            model.addAttribute("nexuUrl", nexuUrl);

            return "user/signrequests/show";
        } else {
            logger.warn(user.getEppn() + " attempted to access signRequest " + id + " without write access");
            redirectAttrs.addFlashAttribute("messageCustom", "not autorized");
            return "redirect:/user/signrequests/";
        }
    }

    @RequestMapping(params = "form", produces = "text/html")
    public String createForm(Model model) {
        populateEditForm(model, new SignRequest());
        User user = userService.getUserFromAuthentication();
        model.addAttribute("mySignBook", signBookService.getUserSignBook(user));
        model.addAttribute("allSignBooks", signBookRepository.findByNotCreateBy("System"));
        return "user/signrequests/create";
    }

    @RequestMapping(method = RequestMethod.POST, produces = "text/html")
    public String create(@Valid SignRequest signRequest, BindingResult bindingResult,
                         @RequestParam(value = "signType", required = false) String signType,
                         @RequestParam(value = "signBookNames", required = false) String[] signBookNames,
                         @RequestParam(value = "newPageType", required = false) String newPageType,
                         Model model, HttpServletRequest request, RedirectAttributes redirectAttrs) throws EsupSignatureException, IOException {
        if (bindingResult.hasErrors()) {
            model.addAttribute("signRequest", signRequest);
            return "user/signrequests/create";
        }

        model.asMap().clear();
        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        SignRequestParams signRequestParams = null;
        if (signRequest.isOverloadSignBookParams()) {
            signRequestParams = new SignRequestParams();
            signRequestParams.setSignType(SignType.valueOf(signType));
            signRequestParams.setNewPageType(NewPageType.valueOf(newPageType));
            signRequestParams.setSignPageNumber(1);
            signRequestParamsRepository.save(signRequestParams);
            signRequest.getSignRequestParamsList().add(signRequestParams);
        }
        //TODO si signbook type workflow == 1 seul
        signRequest = signRequestService.createSignRequest(signRequest, user, signRequestParams);
        /*
        List<SignBook> signBooks = new ArrayList<>();
        if (signBookNames != null && signBookNames.length > 0) {
            for (String signBookName : signBookNames) {
                if (signBookRepository.countByName(signBookName) > 0) {
                    signBooks.add(signBookRepository.findByName(signBookName).get(0));
                } else {
                    signBooks.add(signBookService.getUserSignBookByRecipientEmail(signBookName));
                }
            }
        }
        //on traite les groups en premier
        signBooks = signBooks.stream().sorted(Comparator.comparing(SignBook::getSignBookType)).collect(Collectors.toList());
        for (SignBook signBook : signBooks) {
            signBookService.importSignRequestInSignBook(signRequest, signBook, user);
        }
        */
        return "redirect:/user/signrequests/" + signRequest.getId();
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
            try {
                if (signRequest.isOverloadSignBookParams()) {
                    if (signRequest.getOriginalDocuments().size() > 0
                            &&
                            (signRequest.getCurrentWorkflowStep().getSignRequestParams().getSignType().equals(SignType.pdfImageStamp) || signRequest.getCurrentWorkflowStep().getSignRequestParams().getSignType().equals(SignType.visa))) {
                        signRequest.getOriginalDocuments().remove(signRequest.getOriginalDocuments().get(0));
                    }
                }
                List<Document> documents = documentService.createDocuments(multipartFiles);
                signRequestService.addOriginalDocuments(signRequest, documents);
                signRequestRepository.save(signRequest);
            } catch (IOException e) {
                logger.error("error to add file : " + multipartFiles[0].getOriginalFilename(), e);
            }
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

    @RequestMapping(value = "/sign-by-token/{token}")
    public String signByToken(
            @RequestParam(value = "referer", required = false) String referer,
            @PathVariable("token") String token, RedirectAttributes redirectAttrs, HttpServletResponse response,
            Model model, HttpServletRequest request) throws IOException, SQLException {

        User user = userService.getUserFromAuthentication();
        if (!userService.isUserReady(user)) {
            return "redirect:/user/users/?form";
        }
        if (signRequestRepository.countByName(token) > 0) {
            SignRequest signRequest = signRequestRepository.findByName(token).get(0);
            if (signRequestService.checkUserViewRights(user, signRequest) || signRequestService.checkUserSignRights(user, signRequest)) {
                List<SignBook> originalSignBooks = signBookService.getSignBookBySignRequest(signRequest);
                if (originalSignBooks.size() > 0) {
                    if (signRequest.isOverloadSignBookParams()) {
                        SignRequestParams signRequestParams = signBookRepository.findByRecipientEmailsAndSignBookType(Arrays.asList(user.getEmail()), SignBookType.user).get(0).getSignRequestParams();
                        signRequest.getCurrentWorkflowStep().getSignRequestParams().setSignType(signRequestParams.getSignType());
                        signRequest.getCurrentWorkflowStep().getSignRequestParams().setNewPageType(signRequestParams.getNewPageType());
                    }
                }
                Document toDisplayDocument;
                if (signRequestService.getToSignDocuments(signRequest).size() == 1) {
                    toDisplayDocument = signRequestService.getToSignDocuments(signRequest).get(0);
                    if (toDisplayDocument.getContentType().equals("application/pdf")) {
                        PdfParameters pdfParameters = pdfService.getPdfParameters(toDisplayDocument.getInputStream());
                        model.addAttribute("pdfWidth", pdfParameters.getWidth());
                        model.addAttribute("pdfHeight", pdfParameters.getHeight());
                        model.addAttribute("imagePagesSize", pdfParameters.getTotalNumberOfPages());
                        if (user.getSignImage() != null) {
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
                if (user.getSignImage() != null) {
                    model.addAttribute("signFile", fileService.getBase64Image(user.getSignImage()));
                }
                if (user.getKeystore() != null) {
                    model.addAttribute("keystore", user.getKeystore().getFileName());
                }

                //signRequest.setOriginalSignBooks(signBookService.getOriginalSignBook(signRequest));

                signRequestService.setSignBooksLabels(signRequest.getWorkflowSteps());

                model.addAttribute("signRequest", signRequest);
                if (signRequest.getStatus().equals(SignRequestStatus.pending) && signRequestService.checkUserSignRights(user, signRequest) && signRequest.getOriginalDocuments().size() > 0) {
                    model.addAttribute("signable", "ok");
                }
                List<SignBook> firstOriginalSignBooks = signBookService.getSignBookBySignRequest(signRequest);
                if (firstOriginalSignBooks.size() > 0) {
                    SignBook firstOriginalSignBook = signBookService.getSignBookBySignRequest(signRequest).get(0);
                    if (firstOriginalSignBook.getSignBookType().equals(SignBookType.workflow)) {
                        model.addAttribute("firstOriginalSignBook", firstOriginalSignBook);
                    }
                    if (firstOriginalSignBook.getModelFile() != null) {
                        model.addAttribute("modelId", firstOriginalSignBook.getModelFile().getUrl());
                    }
                }


                model.addAttribute("baseUrl", baseUrl);
                model.addAttribute("nexuVersion", nexuVersion);
                model.addAttribute("nexuUrl", nexuUrl);
                if (referer != null && !"".equals(referer) && !"null".equals(referer)) {
                    String ref = request.getHeader("referer");
                    model.addAttribute("referer", ref);
                }
                return "user/signrequests/sign-only";
            } else {
                redirectAttrs.addAttribute("messageError", "Vous n'avez pas d'action à effectuer sur cette demande");
                return "redirect:/user/signrequests";
            }
        } else {
            redirectAttrs.addAttribute("messageError", "Cette demande de signature n'existe pas");
            return "redirect:/user/signrequests";
        }

    }

    @RequestMapping(value = "/sign/{id}", method = RequestMethod.POST)
    public String sign(@PathVariable("id") Long id,
                       @RequestParam(value = "xPos", required = false) Integer xPos,
                       @RequestParam(value = "yPos", required = false) Integer yPos,
                       @RequestParam(value = "comment", required = false) String comment,
                       @RequestParam(value = "addDate", required = false) Boolean addDate,
                       @RequestParam(value = "signonly", required = false) Boolean signonly,
                       @RequestParam(value = "signPageNumber", required = false) Integer signPageNumber,
                       @RequestParam(value = "password", required = false) String password,
                       @RequestParam(value = "referer", required = false) String referer,
                       RedirectAttributes redirectAttrs, HttpServletResponse response, Model model, HttpServletRequest request) {
        //TODO : choose xades cades
        if (addDate == null) {
            addDate = false;
        }
        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        SignRequest signRequest = signRequestRepository.findById(id).get();
        if (signRequestService.checkUserSignRights(user, signRequest)) {
            if (signRequest.getCurrentWorkflowStep().getSignRequestParams().getSignType().equals(SignType.nexuSign)) {
                return "redirect:/user/nexu-sign/" + id + "?referer=" + referer;
            }
            if (signRequest.isOverloadSignBookParams()) {
                SignBook signBook = signBookRepository.findByRecipientEmailsAndSignBookType(Arrays.asList(user.getEmail()), SignBookType.user).get(0);
                //signRequest.setSignRequestParams(signBook.getSignRequestParams().get(0));
                signRequest.getCurrentWorkflowStep().getSignRequestParams().setSignType(signBook.getSignRequestParams().getSignType());
                signRequestRepository.save(signRequest);
            }
            if (signPageNumber != null && xPos != null && yPos != null) {
                signRequest.getCurrentWorkflowStep().getSignRequestParams().setSignPageNumber(signPageNumber);
                signRequest.getCurrentWorkflowStep().getSignRequestParams().setXPos(xPos);
                signRequest.getCurrentWorkflowStep().getSignRequestParams().setYPos(yPos);
                signRequestRepository.save(signRequest);
            }
            if (!"".equals(password)) {
                setPassword(password);
            }
            try {
                signRequest.setComment(comment);
                signRequestService.sign(signRequest, user, this.password, addDate);
            } catch (EsupSignatureKeystoreException e) {
                logger.error("keystore error", e);
                redirectAttrs.addFlashAttribute("messageError", "security_bad_password");
                progress = "security_bad_password";
            } catch (EsupSignatureSignException e) {
                redirectAttrs.addFlashAttribute("messageCustom", e.getMessage());
                logger.error(e.getMessage(), e);
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
            if (signonly != null && signonly) {
                if (referer != null && !"".equals(referer) && !"null".equals(referer)) {
                    return "redirect:" + referer;
                } else {
                    return "redirect:/user/signrequests/sign-by-token/" + signRequest.getName();
                }
            } else {
                return "redirect:/user/signrequests/" + id;
            }
        } else {
            redirectAttrs.addFlashAttribute("messageCustom", "not autorized");
            progress = "not_autorized";
            return "redirect:/user/signrequests/";
        }
    }

    @RequestMapping(value = "/sign-multiple", method = RequestMethod.POST)
    public void signMultiple(
            @RequestParam(value = "ids", required = true) Long[] ids,
            @RequestParam(value = "comment", required = false) String comment,
            @RequestParam(value = "password", required = false) String password, RedirectAttributes redirectAttrs,
            HttpServletResponse response, Model model, HttpServletRequest request) throws JsonParseException, JsonMappingException, IOException {
        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        float totalToSign = ids.length;
        float nbSigned = 0;
        progress = "0";
        for (Long id : ids) {
            SignRequest signRequest = signRequestRepository.findById(id).get();
            SignBook currentSignBook = signBookService.getSignBookBySignRequestAndUser(signRequest, user);
            if (signRequestService.checkUserSignRights(user, signRequest)) {
                if (!"".equals(password)) {
                    setPassword(password);
                }
                try {
                    if (!signRequest.isOverloadSignBookParams()) {
                        signRequest.getCurrentWorkflowStep().getSignRequestParams().setSignType(currentSignBook.getSignRequestParams().getSignType());
                    }
                    if (signRequest.getCurrentWorkflowStep().getSignRequestParams().getSignType().equals(SignRequestParams.SignType.visa)) {
                        signRequestService.updateStatus(signRequest, SignRequestStatus.checked, "Visa", user, "SUCCESS", comment);
                    } else if (signRequest.getCurrentWorkflowStep().getSignRequestParams().getSignType().equals(SignRequestParams.SignType.nexuSign)) {
                        logger.error("no multiple nexu sign");
                        progress = "not_autorized";
                    } else {
                        signRequestService.sign(signRequest, user, this.password, false);
                    }
                } catch (EsupSignatureKeystoreException e) {
                    logger.error("keystore error", e);
                    progress = "security_bad_password";
                    break;
                } catch (EsupSignatureException e) {
                    logger.error(e.getMessage(), e);
                }
            } else {
                logger.error("not autorized to sign");
                progress = "not_autorized";
            }
            nbSigned++;
            float percent = (nbSigned / totalToSign) * 100;
            progress = String.valueOf((int) percent);
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
    public String refuse(@PathVariable("id") Long id, @RequestParam(value = "comment", required = true) String comment, RedirectAttributes redirectAttrs, HttpServletResponse response,
                         Model model, HttpServletRequest request) throws SQLException {
        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        SignRequest signRequest = signRequestRepository.findById(id).get();
        if (!signRequestService.checkUserSignRights(user, signRequest)) {
            redirectAttrs.addFlashAttribute("messageCustom", "not autorized");
            return "redirect:/user/signrequests/" + id;
        }
        signRequest.setComment(comment);
        signRequestService.refuse(signRequest, user);
        return "redirect:/user/signrequests/";
    }

    @DeleteMapping(value = "/{id}", produces = "text/html")
    public String delete(@PathVariable("id") Long id, Model model) {
        SignRequest signRequest = signRequestRepository.findById(id).get();
        signBookService.removeSignRequestFromAllSignBooks(signRequest);
        List<Log> logs = logRepository.findBySignRequestId(id);
        for (Log log : logs) {
            logRepository.delete(log);
        }
        signRequest.getSignRequestParamsList().clear();

        signRequestRepository.save(signRequest);
        signRequestRepository.delete(signRequest);
        model.asMap().clear();
        return "redirect:/user/signrequests/";
    }

    @RequestMapping(value = "/get-last-file-by-token/{token}", method = RequestMethod.GET)
    public void getLastFileByToken(@PathVariable("token") String token, HttpServletResponse response, Model model) {
        User user = userService.getUserFromAuthentication();
        SignRequest signRequest = signRequestRepository.findByName(token).get(0);
        if (signRequestService.checkUserViewRights(user, signRequest)) {
            getLastFile(signRequest.getId(), response, model);
        } else {
            logger.warn(user.getEppn() + " try to access " + signRequest.getId() + " without view rights");
        }
    }

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

    @RequestMapping(value = "/toggle-need-all-sign/{id}", method = RequestMethod.GET)
    public String toggleNeedAllSign(@PathVariable("id") Long id, HttpServletResponse response, Model model) {
        SignRequest signRequest = signRequestRepository.findById(id).get();
        signRequestService.toggleNeedAllSign(signRequest);
        return "redirect:/user/signrequests/" + id;
    }

    @RequestMapping(value = "/send-to-signbook/{id}", method = RequestMethod.GET)
    public String sendToSignBook(@PathVariable("id") Long id,
                                 @RequestParam(value = "signBookNames", required = true) String[] signBookNames,
                                 @RequestParam(value = "comment", required = false) String comment,
                                 HttpServletResponse response,
                                 RedirectAttributes redirectAttrs, Model model, HttpServletRequest request) {
        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        SignRequest signRequest = signRequestRepository.findById(id).get();
        if (signRequestService.checkUserViewRights(user, signRequest)) {
            if (signBookNames != null && signBookNames.length > 0) {
                for (String signBookName : signBookNames) {
                    SignBook signBook;
                    if (signBookRepository.countByName(signBookName) == 0 && signBookRepository.countByRecipientEmailsAndSignBookType(Arrays.asList(signBookName), SignBookType.user) == 0) {
                        User recipientUser = userService.createUser(signBookName);
                        signBook = signBookService.getUserSignBook(recipientUser);
                    } else {
                        if (signBookRepository.countByName(signBookName) > 0) {
                            signBook = signBookRepository.findByName(signBookName).get(0);
                        } else {
                            signBook = signBookService.getUserSignBookByRecipientEmail(signBookName);
                        }
                    }
                    signBookService.importSignRequestInSignBook(signRequest, signBook, user);
                }
            }
        } else {
            logger.warn(user.getEppn() + " try to move " + signRequest.getId() + " without rights");
        }
        return "redirect:/user/signrequests/" + id;
    }


    @RequestMapping(value = "/complete/{id}", method = RequestMethod.GET)
    public String complete(@PathVariable("id") Long id,
                           @RequestParam(value = "comment", required = false) String comment,
                           HttpServletResponse response, RedirectAttributes redirectAttrs, Model model, HttpServletRequest request) throws EsupSignatureException {
        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        SignRequest signRequest = signRequestRepository.findById(id).get();
        if (signRequest.getCreateBy().equals(user.getEppn()) && (signRequest.getStatus().equals(SignRequestStatus.signed) || signRequest.getStatus().equals(SignRequestStatus.checked))) {
            SignBook originalSignBook = signBookService.getSignBookBySignRequest(signRequest).get(0);
            signRequestService.completeSignRequest(signRequest, user);
        } else {
            logger.warn(user.getEppn() + " try to complete " + signRequest.getId() + " without rights");
        }
        return "redirect:/user/signrequests/" + id;
    }

    @RequestMapping(value = "/pending/{id}", method = RequestMethod.GET)
    public String pending(@PathVariable("id") Long id,
                          @RequestParam(value = "comment", required = false) String comment,
                          HttpServletResponse response, RedirectAttributes redirectAttrs, Model model, HttpServletRequest request) throws IOException {
        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        //TODO controle signType
        ////			if (toSignDocuments.size() == 1 && toSignDocuments.get(0).getContentType().equals("application/pdf")) {
        SignRequest signRequest = signRequestRepository.findById(id).get();
        signRequest.setComment(comment);
        if (signRequestService.checkUserViewRights(user, signRequest) && signRequest.getStatus().equals(SignRequestStatus.draft)) {
            signRequestService.pendingSignRequest(signRequest, user);
        } else {
            logger.warn(user.getEppn() + " try to send for sign " + signRequest.getId() + " without rights");
        }
        return "redirect:/user/signrequests/" + id;
    }

    @RequestMapping(value = "/comment/{id}", method = RequestMethod.GET)
    public String comment(@PathVariable("id") Long id,
                          @RequestParam(value = "comment", required = false) String comment,
                          HttpServletResponse response, RedirectAttributes redirectAttrs, Model model, HttpServletRequest request) {
        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        SignRequest signRequest = signRequestRepository.findById(id).get();
        if (signRequestService.checkUserViewRights(user, signRequest)) {
            signRequestService.updateStatus(signRequest, null, "Ajout d'un commentaire", user, "SUCCESS", comment);
        } else {
            logger.warn(user.getEppn() + " try to add comment" + signRequest.getId() + " without rights");
        }
        return "redirect:/user/signrequests/" + id;
    }

    void populateEditForm(Model model, SignRequest signRequest) {
        model.addAttribute("signRequest", signRequest);
        model.addAttribute("signTypes", Arrays.asList(SignRequestParams.SignType.values()));
        model.addAttribute("newPageTypes", Arrays.asList(SignRequestParams.NewPageType.values()));
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
