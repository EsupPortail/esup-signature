package org.esupportail.esupsignature.web.controller.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.ldap.PersonLdap;
import org.esupportail.esupsignature.repository.*;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.file.FileService;
import org.esupportail.esupsignature.service.fs.FsFile;
import org.esupportail.esupsignature.web.controller.ws.json.JsonDocuments;
import org.esupportail.esupsignature.web.controller.ws.json.JsonSignRequestStatus;
import org.esupportail.esupsignature.web.controller.ws.json.JsonWorkflowStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.persistence.NoResultException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
@Transactional
@RequestMapping("/ws/")
public class WsController {

    private static final Logger logger = LoggerFactory.getLogger(WsController.class);

    @Resource
    private SignRequestParamsRepository signRequestParamsRepository;

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
    private UserRepository userRepository;

    @Resource
    private UserService userService;

    @Resource
    private DocumentService documentService;

    @Resource
    private FileService fileService;

    @Resource
    private LogRepository logRepository;

    @ResponseBody
    @PostMapping(value = "/create-sign-book", produces = MediaType.APPLICATION_JSON_VALUE)
    public String createSignBook(@RequestParam String name, @RequestParam String createBy, HttpServletRequest httpServletRequest) throws EsupSignatureException {
        User user = userRepository.findByEppn(createBy).get(0);
        user.setIp(httpServletRequest.getRemoteAddr());
        SignBook signBook = signBookService.getSignBook(name, user);
        return signBook.getName();
    }

    @ResponseBody
    @PostMapping(value = "/create-sign-request", produces = MediaType.APPLICATION_JSON_VALUE)
    public String createSignRequest(@RequestParam String title,
                                    @RequestParam String createBy,
                                    @RequestParam("recipientsEmail") String recipientsEmail,
                                    @RequestParam("multipartFiles") MultipartFile[] multipartFiles,
                                    @RequestParam("signLevel") int signLevel, HttpServletRequest httpServletRequest) throws EsupSignatureIOException, IOException, EsupSignatureUserException {
        User user = userRepository.findByEppn(createBy).get(0);
        user.setIp(httpServletRequest.getRemoteAddr());
        ObjectMapper mapper = new ObjectMapper();
        SignRequest signRequest = signRequestService.createSignRequest(title, user);
        signRequestService.addDocsToSignRequest(signRequest, multipartFiles);
        signRequestService.addRecipients(signRequest, mapper.readValue(recipientsEmail, String[].class));
        signRequestService.pendingSignRequest(signRequest, signRequestService.getSignTypeByLevel(signLevel), false);
        logger.info("new signRequest created by " + user.getEppn());
        return signRequest.getToken();
    }

    @ResponseBody
    @PostMapping(value = "/add-docs/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object addDocument(User user, @PathVariable("id") Long id,
                              @RequestParam("multipartFiles") MultipartFile[] multipartFiles, HttpServletRequest httpServletRequest) throws EsupSignatureIOException {
        //User user = userService.getCurrentUser();
        user.setIp(httpServletRequest.getRemoteAddr());
        SignRequest signRequest = signRequestRepository.findById(id).get();
        signRequestService.addDocsToSignRequest(signRequest, multipartFiles);
        logger.info("documents added in signRequest : " + signRequest.getId());
        String[] ok = {"ok"};
        return ok;
    }

    @ResponseBody
    @PostMapping(value = "/add-docs-in-sign-book-group/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object addDocumentInSignBookGroup(User user, @PathVariable("name") String name,
                              @RequestParam("multipartFiles") MultipartFile[] multipartFiles, HttpServletRequest httpServletRequest) throws EsupSignatureException, EsupSignatureIOException {
        logger.info("start add documents in " + name);
        //User user = userService.getCurrentUser();
        user.setIp(httpServletRequest.getRemoteAddr());
        SignBook signBook = signBookService.getSignBook(name, user);
        SignRequest signRequest = signRequestService.createSignRequest(name, user);
        signRequestService.addDocsToSignRequest(signRequest, multipartFiles);
        signBookService.addSignRequest(signBook, signRequest);
        logger.info("signRequest : " + signRequest.getId() + " added to signBook" + signBook.getName() + " - " + signBook.getId());
        String[] ok = {"ok"};
        return ok;
    }

    @ResponseBody
    @PostMapping(value = "/add-docs-in-sign-book-unique/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object addDocumentToNewSignRequest(User user, @PathVariable("name") String name,
                                              @RequestParam("multipartFiles") MultipartFile[] multipartFiles, HttpServletRequest httpServletRequest) throws EsupSignatureException, EsupSignatureIOException, IOException {
        logger.info("start add documents in " + name);
        //User user = userService.getCurrentUser();
        SignBook signBook = signBookService.getSignBook(name, user);
        user = userRepository.findByEppn(signBook.getCreateBy()).get(0);
        user.setIp(httpServletRequest.getRemoteAddr());
        for (MultipartFile multipartFile : multipartFiles) {
            SignRequest signRequest = signRequestService.createSignRequest(signBook.getName() + "_" + multipartFile.getOriginalFilename(), user);
            signRequestService.addDocsToSignRequest(signRequest, multipartFile);
            signBookService.addSignRequest(signBook, signRequest);
        }
        String[] ok = {"ok"};
        return ok;
    }

    @ResponseBody
    @PostMapping(value = "/add-workflow-step", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity addWorkflowStep(User user, @RequestParam String name,
                                  @RequestParam String jsonWorkflowStepString) throws IOException, EsupSignatureUserException {
        SignBook signBook = signBookRepository.findByName(name).get(0);
        SignType signType = null;
        ObjectMapper mapper = new ObjectMapper();
        JsonWorkflowStep jsonWorkflowStep = mapper.readValue(jsonWorkflowStepString, JsonWorkflowStep.class);
        int level = jsonWorkflowStep.getSignLevel();
        signType = signRequestService.getSignTypeByLevel(level);
        WorkflowStep workflowStep = workflowService.createWorkflowStep("", "signBook", signBook.getId(), jsonWorkflowStep.getAllSignToComplete(), signType, jsonWorkflowStep.getRecipientEmails().stream().toArray(String[]::new));
        signBook.getWorkflowSteps().add(workflowStep);
        return new ResponseEntity(HttpStatus.OK);
    }

    @ResponseBody
    @GetMapping(value = "/list-sign-requests", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<JsonDocuments> listSignedFiles(@RequestParam("recipientEmail") String recipientEmail) throws EsupSignatureUserException {
        List<JsonDocuments> signedFiles = new ArrayList<>();
        User user = userService.getUserByEmail(recipientEmail);
        List<SignRequest> signRequests = signRequestService.getSignRequestsSignedByUser(user);

        for (SignRequest signRequest : signRequests) {
            signedFiles.add(new JsonDocuments(signRequest.getTitle(), signRequest.getSignedDocuments().get(0).getContentType(), signRequest.getToken(), signRequest.getStatus().name(), signRequest.getCreateDate()));

        }
        return signedFiles;
    }

    @ResponseBody
    @PostMapping(value = "/pending-sign-book", produces = MediaType.APPLICATION_JSON_VALUE)
    public String pendingSignBook(@RequestParam String name) throws EsupSignatureIOException {
        SignBook signBook = signBookRepository.findByName(name).get(0);
        signBookService.nextWorkFlowStep(signBook);
        signBookService.pendingSignBook(signBook, userService.getSystemUser());
        return signBook.getSignRequests().get(0).getToken();
    }

    @ResponseBody
    @PostMapping(value = "/pending-sign-request", produces = MediaType.APPLICATION_JSON_VALUE)
    public void pendingSignRequest(@RequestParam String token,
                                   @RequestParam("recipientsEmail") String recipientsEmail,
                                   @RequestParam("signLevel") int signLevel) throws IOException, EsupSignatureUserException {
        if (signRequestRepository.countByToken(token) > 0) {
            SignRequest signRequest = signRequestRepository.findByToken(token).get(0);
            ObjectMapper mapper = new ObjectMapper();
            signRequestService.addRecipients(signRequest, mapper.readValue(recipientsEmail, String[].class));
            signRequestService.pendingSignRequest(signRequest, signRequestService.getSignTypeByLevel(signLevel), false);
        }
    }

    @ResponseBody
    @GetMapping(value = "/list-to-sign-requests", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<JsonDocuments> listToSignFiles(@RequestParam("recipientEmail") String recipientEmail, HttpServletRequest httpServletRequest) {
        List<JsonDocuments> signedFiles = new ArrayList<>();
        User user = userRepository.findByEmail(recipientEmail).get(0);
        user.setIp(httpServletRequest.getRemoteAddr());
        for (SignRequest signRequest : signRequestService.getToSignRequests(user)) {
            signedFiles.add(new JsonDocuments(signRequest.getTitle(), signRequestService.getToSignDocuments(signRequest).get(0).getContentType(), signRequest.getToken(), signRequest.getStatus().name(), signRequest.getCreateDate()));
        }
        return signedFiles;
    }

    @ResponseBody
    @PostMapping(value = "/create-workflow")
    public String createWorkflow(@RequestParam String workflowString, @RequestParam String signBookType, HttpServletRequest httpServletRequest) throws IOException, ParseException, EsupSignatureException {
        User user = userService.getSystemUser();
        user.setIp(httpServletRequest.getRemoteAddr());
        ObjectMapper mapper = new ObjectMapper();
        Workflow workflow = workflowService.createWorkflow(mapper.readValue(workflowString, Workflow.class).getName(), user, true);
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
                signBookService.delete(signBook);
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
            signRequestService.delete(signRequest);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

@ApiResponses( value = {
            @ApiResponse(code = 200, message = "OK", response = ByteArrayInputStream.class)
            }
    )@GetMapping(value = "/get-signed-file")
    public ResponseEntity<Void> getSignedFile(@RequestParam String signBookName, @RequestParam String name, HttpServletResponse response) {
        try {
            SignRequest signRequest = signRequestRepository.findByToken(signBookName).get(0);
            if (signRequest.getStatus().equals(SignRequestStatus.signed)) {
                try {
                    FsFile file = signRequestService.getLastSignedFsFile(signRequest);
                    if (file == null) {
                        response.setHeader("Content-Disposition", "inline;filename=\"" + file.getName() + "\"");
                        response.setContentType(signRequest.getOriginalDocuments().get(0).getContentType());
                        IOUtils.copy(signRequest.getOriginalDocuments().get(0).getInputStream(), response.getOutputStream());
                    } else {
                        response.setHeader("Content-Disposition", "inline;filename=\"" + file.getName() + "\"");
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

    @ApiResponses( value = {
            @ApiResponse(code = 200, message = "OK", response = ByteArrayInputStream.class)
            }
    )
    @GetMapping(value = "/get-last-file-by-token/{token}")
    public void getLastFileByToken(User user, @PathVariable("token") String token, HttpServletResponse response) {
        //User user = userService.getCurrentUser();
        SignRequest signRequest = signRequestRepository.findByToken(token).get(0);
        if (signRequestService.checkUserViewRights(user, signRequest)) {
            List<Document> documents = signRequestService.getToSignDocuments(signRequest);
            try {
                if (documents.size() > 1) {
                    response.sendRedirect("/user/signrequests/" + signRequest.getId());
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

    @ApiResponses( value = {
            @ApiResponse(code = 200, message = "OK", response = ByteArrayInputStream.class)
            }
    )@GetMapping(value = "/get-last-file-from-signrequest")
    public ResponseEntity<Void> getLastFileFromSignRequest(User user, @RequestParam String token, HttpServletResponse response) {
        try {
            if (signRequestRepository.countByToken(token) > 0) {
                SignRequest signRequest = signRequestRepository.findByToken(token).get(0);
                    FsFile file = signRequestService.getLastSignedFsFile(signRequest);
                    if (file != null) {
                        response.setHeader("Content-Disposition", "inline;filename=\"" + file.getName() + "\"");
                        response.setContentType(file.getContentType());
                        IOUtils.copy(file.getInputStream(), response.getOutputStream());
                    }
                    return new ResponseEntity<>(HttpStatus.OK);
            } else {
                logger.warn("no signRequest " + token);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (NoResultException | IOException e) {
            logger.error(e.getMessage(), e);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

@ApiResponses( value = {
            @ApiResponse(code = 200, message = "OK", response = ByteArrayInputStream.class)
            }
    )@GetMapping(value = "/get-last-file-from-signbook")
    public ResponseEntity<Void> getLastFileFromSignBook(@RequestParam String name, HttpServletResponse response) {
        try {
            SignBook signBook = signBookRepository.findByName(name).get(0);
            if (signBook != null) {
                try {
                    SignRequest signRequest = signBook.getSignRequests().get(0);
                    FsFile file = signRequestService.getLastSignedFsFile(signRequest);
                    if (file == null) {
                        response.setHeader("Content-Disposition", "inline;filename=\"" + file.getName() + "\"");
                        response.setContentType(signRequest.getOriginalDocuments().get(0).getContentType());
                        IOUtils.copy(signRequest.getOriginalDocuments().get(0).getInputStream(), response.getOutputStream());
                    } else {
                        response.setHeader("Content-Disposition", "inline;filename=\"" + file.getName() + "\"");
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

@ApiResponses( value = {
            @ApiResponse(code = 200, message = "OK", response = ByteArrayInputStream.class)
            }
    )@GetMapping(value = "/get-last-file")
    public ResponseEntity<Void> getLastFile(@RequestParam String token, HttpServletResponse response) {
        try {
            SignRequest signRequest = signRequestRepository.findByToken(token).get(0);
            if (signRequest != null) {
                try {
                    FsFile file = signRequestService.getLastSignedFsFile(signRequest);
                    if (file == null) {
                        response.setHeader("Content-Disposition", "inline;filename=\"" + file.getName() + "\"");
                        response.setContentType(signRequest.getOriginalDocuments().get(0).getContentType());
                        IOUtils.copy(signRequest.getOriginalDocuments().get(0).getInputStream(), response.getOutputStream());
                    } else {
                        response.setHeader("Content-Disposition", "inline;filename=\"" + file.getName() + "\"");
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
                for (Recipient recipient : signRequest.getRecipients()) {
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
                if(signRequest.getParentSignBook().getWorkflowSteps().size() > 0 ) {
                    for (Recipient recipient : signRequest.getRecipients()) {
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
        if (userRepository.countByEppn(eppn) > 0) {
            return new ObjectMapper().writeValueAsString(true);
        } else {
            return new ObjectMapper().writeValueAsString(false);
        }
    }

    @Transactional
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

    @Transactional
    @ResponseBody
    @PostMapping(value = "/complete-sign-request")
    public String completeSignRequest(@RequestParam String token,
                                                    @RequestParam(required = false) String documentIOTypeName,
                                                    @RequestParam(required = false) String targetUri,
                                                    HttpServletRequest httpServletRequest) {
        String result = "";
        try {
            SignRequest signRequest = signRequestRepository.findByToken(token).get(0);
            User user = userService.getSystemUser();
            user.setIp(httpServletRequest.getRemoteAddr());
            if (signRequest.getStatus().equals(SignRequestStatus.signed) || signRequest.getStatus().equals(SignRequestStatus.checked)) {
                result = signRequestService.completeSignRequests(Arrays.asList(signRequest), DocumentIOType.valueOf(documentIOTypeName), targetUri, user);
            } else {
                logger.warn("no signed version of signRequest : " + token);
            }
        } catch (NoResultException | EsupSignatureException e) {
            logger.error(e.getMessage(), e);
        }
        return result;
    }

//
//    @ResponseBody
//    @GetMapping(value = "/search-user")
//    public List<PersonLdap> searchUser(@RequestParam(value = "searchString") String searchString, @RequestParam(required = false) String ldapTemplateName) {
//        logger.debug("ldap search for : " + searchString);
//        return userService.getPersonLdaps(searchString, ldapTemplateName);
//    }

}
