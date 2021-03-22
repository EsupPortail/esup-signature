package org.esupportail.esupsignature.web.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.interfaces.fs.FsFile;
import org.esupportail.esupsignature.service.utils.barcode.DdDocService;
import org.esupportail.esupsignature.web.ws.json.JsonDocuments;
import org.esupportail.esupsignature.web.ws.json.JsonSignRequestStatus;
import org.esupportail.esupsignature.web.ws.json.JsonWorkflowStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.persistence.NoResultException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller

@RequestMapping("/ws/")
public class WsController {

    private static final Logger logger = LoggerFactory.getLogger(WsController.class);

    @Resource
    private SignRequestRepository signRequestRepository;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private SignBookRepository signBookRepository;

    @Resource
    private WorkflowService workflowService;

    @Resource
    private SignBookService signBookService;

    @Resource
    private UserService userService;

    @Resource
    private DdDocService ddDocService;

    @Resource
    private LiveWorkflowStepService liveWorkflowStepService;

    @ResponseBody
    @PostMapping(value = "/create-sign-book", produces = MediaType.APPLICATION_JSON_VALUE)
    public String createSignBook(@RequestParam String name, @RequestParam String createBy, HttpServletRequest httpServletRequest) {
        User user = userService.getByEppn(createBy);
        user.setIp(httpServletRequest.getRemoteAddr());
        SignBook signBook = signBookService.createSignBook("", name, user, true);
        return signBook.getName();
    }

    @ResponseBody
    @PostMapping(value = "/create-sign-request", produces = MediaType.APPLICATION_JSON_VALUE)
    public String createSignRequest(@RequestParam String title,
                                    @RequestParam String createBy,
                                    @RequestParam("recipientsEmail") String recipientsEmail,
                                    @RequestParam("multipartFiles") MultipartFile[] multipartFiles) throws EsupSignatureIOException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        //TODO create signbook
        SignBook signBook = null;
        SignRequest signRequest = signRequestService.createSignRequest(title, signBook, createBy, createBy);
        signRequestService.addDocsToSignRequest(signRequest, multipartFiles);
        liveWorkflowStepService.addRecipients(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep(), mapper.readValue(recipientsEmail, String[].class));
        signRequestService.pendingSignRequest(signRequest, createBy);
        logger.info("new signRequest created by " + createBy);
        return signRequest.getToken();
    }

    @ResponseBody
    @PostMapping(value = "/add-docs/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object addDocument(@PathVariable("id") Long id,
                              @RequestParam("multipartFiles") MultipartFile[] multipartFiles) throws EsupSignatureIOException {
        SignRequest signRequest = signRequestRepository.findById(id).get();
        signRequestService.addDocsToSignRequest(signRequest, multipartFiles);
        logger.info("documents added in signRequest : " + signRequest.getId());
        String[] ok = {"ok"};
        return ok;
    }

    @ResponseBody
    @PostMapping(value = "/add-docs-in-sign-book-group/{workflowName}/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object addDocumentInSignBookGroup(@PathVariable("name") String name,
                                             @PathVariable("workflowName") String workflowName,
                                             @RequestParam("multipartFiles") MultipartFile[] multipartFiles, HttpServletRequest httpServletRequest) throws EsupSignatureIOException {
        logger.info("start add documents in " + name);
        User systemUser = userService.getSystemUser();
        systemUser.setIp(httpServletRequest.getRemoteAddr());
        SignBook signBook = signBookService.createSignBook(workflowName, name, systemUser, true);
        SignRequest signRequest = signRequestService.createSignRequest(name, signBook, systemUser.getEppn(), systemUser.getEppn());
        signRequestService.addDocsToSignRequest(signRequest, multipartFiles);
        logger.info("signRequest : " + signRequest.getId() + " added to signBook" + signBook.getName() + " - " + signBook.getId());
        String[] ok = {"ok"};
        return ok;
    }

    @ResponseBody
    @PostMapping(value = "/add-docs-in-sign-book-unique/{workflowName}/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object addDocumentToNewSignRequest(@PathVariable("name") String name,
                                              @PathVariable("workflowName") String workflowName,
                                              @RequestParam("multipartFiles") MultipartFile[] multipartFiles, HttpServletRequest httpServletRequest) throws EsupSignatureIOException {
        logger.info("start add documents in " + name);
        User systemUser = userService.getSystemUser();
        systemUser.setIp(httpServletRequest.getRemoteAddr());
        signBookService.addDocsInNewSignBookSeparated(name, workflowName, multipartFiles, systemUser);
        String[] ok = {"ok"};
        return ok;
    }

    @ResponseBody
    @PostMapping(value = "/add-workflow-step", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HttpStatus> addWorkflowStep(@ModelAttribute("userEppn") String userEppn, @RequestParam String name,
                                  @RequestParam String jsonWorkflowStepString) throws IOException {
        SignBook signBook = signBookRepository.findByName(name).get(0);
        ObjectMapper mapper = new ObjectMapper();
        JsonWorkflowStep jsonWorkflowStep = mapper.readValue(jsonWorkflowStepString, JsonWorkflowStep.class);
        int level = jsonWorkflowStep.getSignLevel();
        SignType signType = signRequestService.getSignTypeByLevel(level);
        LiveWorkflowStep liveWorkflowStep = liveWorkflowStepService.createLiveWorkflowStep(null, false, jsonWorkflowStep.getAllSignToComplete(), signType, jsonWorkflowStep.getRecipientsEmails().stream().toArray(String[]::new));
        signBook.getLiveWorkflow().getLiveWorkflowSteps().add(liveWorkflowStep);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResponseBody
    @GetMapping(value = "/list-sign-requests", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<JsonDocuments> listSignedFiles(@RequestParam("recipientEmail") String recipientEmail) {
        List<JsonDocuments> signedFiles = new ArrayList<>();
        User user = userService.getUserByEmail(recipientEmail);
        List<SignRequest> signRequests = signRequestService.getSignRequestsSignedByUser(user.getEppn());

        for (SignRequest signRequest : signRequests) {
            signedFiles.add(new JsonDocuments(signRequest.getTitle(), signRequest.getSignedDocuments().get(0).getContentType(), signRequest.getToken(), signRequest.getStatus().name(), signRequest.getCreateDate()));

        }
        return signedFiles;
    }

    @ResponseBody
    @PostMapping(value = "/pending-sign-book", produces = MediaType.APPLICATION_JSON_VALUE)
    public String pendingSignBook(@RequestParam String name) {
        SignBook signBook = signBookRepository.findByName(name).get(0);
        signBookService.nextStepAndPending(signBook.getId(), null, "system", "system");
        return signBook.getSignRequests().get(0).getToken();
    }

    @ResponseBody
    @PostMapping(value = "/pending-sign-request", produces = MediaType.APPLICATION_JSON_VALUE)
    public void pendingSignRequest(@RequestParam String token,
                                   @RequestParam("recipientsEmail") String recipientsEmail) throws IOException {
        if (signRequestRepository.countByToken(token) > 0) {
            SignRequest signRequest = signRequestRepository.findByToken(token).get(0);
            ObjectMapper mapper = new ObjectMapper();
            liveWorkflowStepService.addRecipients(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep(), mapper.readValue(recipientsEmail, String[].class));
            signRequestService.pendingSignRequest(signRequest, "system");
        }
    }

    @ResponseBody
    @GetMapping(value = "/list-to-sign-requests", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<JsonDocuments> listToSignFiles(@RequestParam("recipientEmail") String recipientEmail, HttpServletRequest httpServletRequest) {
        List<JsonDocuments> signedFiles = new ArrayList<>();
        User user = userService.getUserByEmail(recipientEmail);
        user.setIp(httpServletRequest.getRemoteAddr());
        for (SignRequest signRequest : signRequestService.getToSignRequests(user.getEppn())) {
            signedFiles.add(new JsonDocuments(signRequest.getTitle(), signRequestService.getToSignDocuments(signRequest.getId()).get(0).getContentType(), signRequest.getToken(), signRequest.getStatus().name(), signRequest.getCreateDate()));
        }
        return signedFiles;
    }

    @ResponseBody
    @PostMapping(value = "/create-workflow")
    public String createWorkflow(@RequestParam String workflowString, HttpServletRequest httpServletRequest) throws IOException, EsupSignatureException {
        User user = userService.getSystemUser();
        user.setIp(httpServletRequest.getRemoteAddr());
        ObjectMapper mapper = new ObjectMapper();
        String name = mapper.readValue(workflowString, Workflow.class).getName();
        Workflow workflow = workflowService.createWorkflow(name, name, user);
        return workflow.getName();
    }

    @ResponseBody
    @PostMapping(value = "/delete-sign-book")
    public ResponseEntity<String> deleteSignBook(@RequestParam String name, HttpServletRequest httpServletRequest) {
        User user = userService.getSystemUser();
        user.setIp(httpServletRequest.getRemoteAddr());
        if (signBookRepository.countByName(name) > 0) {
            SignBook signBook = signBookRepository.findByName(name).get(0);
            if(signBook.getExternal()) {
                signBookService.delete(signBook.getId());
            }
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResponseBody
    @PostMapping(value = "/delete-sign-request")
    public ResponseEntity<String> deleteSignRequest(@RequestParam String token, HttpServletRequest httpServletRequest) {
        User user = userService.getSystemUser();
        user.setIp(httpServletRequest.getRemoteAddr());
        if (signRequestRepository.countByToken(token) > 0) {
            SignRequest signRequest = signRequestRepository.findByToken(token).get(0);
            signRequestService.delete(signRequest.getId());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiResponses( value = {
            @ApiResponse(responseCode = "200")
            }
    )
    @GetMapping(value = "/get-signed-file")
    public ResponseEntity<Void> getSignedFile(@RequestParam String signBookName, @RequestParam String name, HttpServletResponse response) {
        try {
            SignRequest signRequest = signRequestRepository.findByToken(signBookName).get(0);
            if (signRequest.getStatus().equals(SignRequestStatus.signed)) {
                try {
                    FsFile file = signRequestService.getLastSignedFsFile(signRequest);
                    if (file == null) {
                        response.setHeader("Content-disposition", "inline; filename=" + URLEncoder.encode(file.getName(), StandardCharsets.UTF_8.toString()));
                        response.setContentType(signRequest.getOriginalDocuments().get(0).getContentType());
                        IOUtils.copy(signRequest.getOriginalDocuments().get(0).getInputStream(), response.getOutputStream());
                    } else {
                        response.setHeader("Content-disposition", "inline; filename=" + URLEncoder.encode(file.getName(), StandardCharsets.UTF_8.toString()));
                        response.setContentType(file.getContentType());
                        IOUtils.copy(file.getInputStream(), response.getOutputStream());
                    }
                    return new ResponseEntity<>(HttpStatus.OK);
                } catch (Exception e) {
                    logger.error("get file error", e);
                }
            } else {
                logger.warn("no signed version of " + name);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (NoResultException e) {
            logger.error(e.getMessage(), e);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @GetMapping(value = "/get-last-file-by-token/{token}")
    public void getLastFileByToken(@PathVariable("token") String token, HttpServletResponse response) {
        SignRequest signRequest = signRequestRepository.findByToken(token).get(0);
        List<Document> documents = signRequestService.getToSignDocuments(signRequest.getId());
        try {
            if (documents.size() > 1) {
                response.sendRedirect("/user/signrequests/" + signRequest.getId());
            } else {
                Document document = documents.get(0);
                response.setHeader("Content-disposition", "inline; filename=" + URLEncoder.encode(document.getFileName(), StandardCharsets.UTF_8.toString()));
                response.setContentType(document.getContentType());
                IOUtils.copy(document.getBigFile().getBinaryFile().getBinaryStream(), response.getOutputStream());
            }
        } catch (Exception e) {
            logger.error("get file error", e);
        }
    }

    @GetMapping(value = "/get-last-file-from-signrequest")
    public ResponseEntity<Void> getLastFileFromSignRequest(@ModelAttribute("userEppn") String userEppn, @RequestParam String token, HttpServletResponse response) {
        try {
            if (signRequestRepository.countByToken(token) > 0) {
                SignRequest signRequest = signRequestRepository.findByToken(token).get(0);
                    FsFile file = signRequestService.getLastSignedFsFile(signRequest);
                    if (file != null) {
                        response.setHeader("Content-disposition", "inline; filename=" + URLEncoder.encode(file.getName(), StandardCharsets.UTF_8.toString()));
                        response.setContentType(file.getContentType());
                        IOUtils.copy(file.getInputStream(), response.getOutputStream());
                    }
                    return new ResponseEntity<>(HttpStatus.OK);
            } else {
                logger.warn("no signRequest " + token);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (NoResultException | IOException | EsupSignatureException e) {
            logger.error(e.getMessage(), e);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @GetMapping(value = "/get-last-file-from-signbook")
    public ResponseEntity<Void> getLastFileFromSignBook(@RequestParam String name, HttpServletResponse response) {
        try {
            SignBook signBook = signBookRepository.findByName(name).get(0);
            if (signBook != null) {
                try {
                    SignRequest signRequest = signBook.getSignRequests().get(0);
                    FsFile file = signRequestService.getLastSignedFsFile(signRequest);
                    if (file == null) {
                        response.setHeader("Content-disposition", "inline; filename=" + URLEncoder.encode(file.getName(), StandardCharsets.UTF_8.toString()));
                        response.setContentType(signRequest.getOriginalDocuments().get(0).getContentType());
                        IOUtils.copy(signRequest.getOriginalDocuments().get(0).getInputStream(), response.getOutputStream());
                    } else {
                        response.setHeader("Content-disposition", "inline; filename=" + URLEncoder.encode(file.getName(), StandardCharsets.UTF_8.toString()));
                        response.setContentType(file.getContentType());
                        IOUtils.copy(file.getInputStream(), response.getOutputStream());
                    }
                    return new ResponseEntity<>(HttpStatus.OK);
                } catch (Exception e) {
                    logger.error("get file error", e);
                }
            } else {
                logger.warn("no signed version of " + name);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (NoResultException e) {
            logger.error(e.getMessage(), e);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @GetMapping(value = "/get-last-file")
    public ResponseEntity<Void> getLastFile(@RequestParam String token, HttpServletResponse response) {
        try {
            SignRequest signRequest = signRequestRepository.findByToken(token).get(0);
            if (signRequest != null) {
                try {
                    FsFile file = signRequestService.getLastSignedFsFile(signRequest);
                    if (file == null) {
                        response.setHeader("Content-disposition", "inline; filename=" + URLEncoder.encode(file.getName(), StandardCharsets.UTF_8.toString()));
                        response.setContentType(signRequest.getOriginalDocuments().get(0).getContentType());
                        IOUtils.copy(signRequest.getOriginalDocuments().get(0).getInputStream(), response.getOutputStream());
                    } else {
                        response.setHeader("Content-disposition", "inline; filename=" + URLEncoder.encode(file.getName(), StandardCharsets.UTF_8.toString()));
                        response.setContentType(file.getContentType());
                        IOUtils.copy(file.getInputStream(), response.getOutputStream());
                    }
                    return new ResponseEntity<>(HttpStatus.OK);
                } catch (Exception e) {
                    logger.error("get file error", e);
                }
            } else {
                logger.warn("no signed version of " + token);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (NoResultException e) {
            logger.error(e.getMessage(), e);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ResponseBody
    @GetMapping(value = "/check-sign-request", produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonSignRequestStatus checkSignRequest(@RequestParam String fileToken) {
        try {
            if (signRequestRepository.countByToken(fileToken) > 0) {
                SignRequest signRequest = signRequestRepository.findByToken(fileToken).get(0);
                JsonSignRequestStatus jsonSignRequestStatus = new JsonSignRequestStatus();
                jsonSignRequestStatus.setStatus(signRequest.getStatus().toString());
                for (Recipient recipient : signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients()) {
                    User user = recipient.getUser();
                    jsonSignRequestStatus.getNextRecipientNames().add(user.getName());
                    jsonSignRequestStatus.getNextRecipientEppns().add(user.getEppn());
                }
                return jsonSignRequestStatus;
            }
        } catch (NoResultException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @ResponseBody
    @GetMapping(value = "/check-sign-book", produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonSignRequestStatus checkSignBook(@RequestParam String fileToken) {
        try {
            if (signRequestRepository.countByToken(fileToken) > 0) {
                SignRequest signRequest = signRequestRepository.findByToken(fileToken).get(0);
                JsonSignRequestStatus jsonSignRequestStatus = new JsonSignRequestStatus();
                jsonSignRequestStatus.setStatus(signRequest.getStatus().toString());
                if(signRequest.getParentSignBook().getLiveWorkflow().getLiveWorkflowSteps().size() > 0 ) {
                    for (Recipient recipient : signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients()) {
                        User user = recipient.getUser();
                        jsonSignRequestStatus.getNextRecipientNames().add(user.getName());
                        jsonSignRequestStatus.getNextRecipientEppns().add(user.getEppn());
                    }
                }
                return jsonSignRequestStatus;
            }
        } catch (NoResultException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @ResponseBody
    @PostMapping(value = "/check-user-status", produces = MediaType.APPLICATION_JSON_VALUE)
    public String checkUserStatus(@RequestParam String eppn) throws JsonProcessingException {
        if (userService.getByEppn(eppn) != null) {
            return new ObjectMapper().writeValueAsString(true);
        } else {
            return new ObjectMapper().writeValueAsString(false);
        }
    }


    @PostMapping(value = "/complete-sign-book")
    public ResponseEntity<Void> completeSignBook(@RequestParam String signBookName, @RequestParam String name, HttpServletRequest httpServletRequest, HttpServletResponse response, Model model) {
        try {
            SignBook signBook = signBookRepository.findByName(signBookName).get(0);
            SignRequest signRequest = signRequestRepository.findByToken(name).get(0);
            User user = userService.getSystemUser();
            user.setIp(httpServletRequest.getRemoteAddr());
            if (signRequest.getStatus().equals(SignRequestStatus.signed)) {
                try {
                    signBookService.removeSignRequestFromSignBook(signBook, signRequest);
                    return new ResponseEntity<>(HttpStatus.OK);
                } catch (Exception e) {
                    logger.error("get file error", e);
                }
            } else {
                logger.warn("no signed version of " + name);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (NoResultException e) {
            logger.error(e.getMessage(), e);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @ResponseBody
    @PostMapping(value = "/complete-sign-request")
    public void completeSignRequest(@RequestParam String token,
                                                    HttpServletRequest httpServletRequest) {
        try {
            SignRequest signRequest = signRequestRepository.findByToken(token).get(0);
            if (signRequest.getStatus().equals(SignRequestStatus.signed) || signRequest.getStatus().equals(SignRequestStatus.checked)) {
                signRequestService.completeSignRequests(Arrays.asList(signRequest), "system");
            } else {
                logger.warn("no signed version of signRequest : " + token);
            }
        } catch (NoResultException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @GetMapping(value = "/2ddoc/{barcode}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<String> genetare2DDoc(@PathVariable("barcode") String barcode, HttpServletResponse response) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "image/svg+xml");
        headers.add("Content-Type", "image/png");
        MatrixToImageWriter.writeToStream(ddDocService.getQrCodeSvg(barcode), "PNG", response.getOutputStream());
        return null;
    }


//
//    @ResponseBody
//    @GetMapping(value = "/search-user")
//    public List<PersonLdap> searchUser(@RequestParam(value = "searchString") String searchString, @RequestParam(required = false) String ldapTemplateName) {
//        logger.debug("ldap search for : " + searchString);
//        return userService.getPersonLdaps(searchString, ldapTemplateName);
//    }

}
