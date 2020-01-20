package org.esupportail.esupsignature.web.controller.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.SignBook.SignBookType;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.ldap.PersonLdap;
import org.esupportail.esupsignature.repository.*;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.fs.FsFile;
import org.esupportail.esupsignature.service.ldap.LdapPersonService;
import org.esupportail.esupsignature.web.JsonSignInfoMessage;
import org.esupportail.esupsignature.web.JsonWorkflowStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
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
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@Transactional
@RequestMapping(value = "/ws/")
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
    private LogRepository logRepository;

    @ResponseBody
    @RequestMapping(value = "/create-sign-book", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String createSignBook(@RequestParam String name, @RequestParam String createBy, HttpServletRequest httpServletRequest) throws EsupSignatureException {
        User user = userRepository.findByEppn(createBy).get(0);
        user.setIp(httpServletRequest.getRemoteAddr());
        SignBook signBook = signBookService.getSignBook(name, user);
        return signBook.getName();
    }

    @ResponseBody
    @RequestMapping(value = "/create-sign-request", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String createSignRequest(@RequestParam String title,
                                    @RequestParam String createBy,
                                    @RequestParam("recipientsEmail") String recipientsEmail,
                                    @RequestParam("multipartFiles") MultipartFile[] multipartFiles,
                                    @RequestParam("signLevel") int signLevel, HttpServletRequest httpServletRequest) throws EsupSignatureIOException, IOException, EsupSignatureException {
        User user = userRepository.findByEppn(createBy).get(0);
        user.setIp(httpServletRequest.getRemoteAddr());
        logger.info("new signRequest created by " + user.getEppn());
        ObjectMapper mapper = new ObjectMapper();
        SignRequest signRequest = signRequestService.createSignRequest(title, user);
        signRequestService.addDocsToSignRequest(signRequest, multipartFiles);
        List<String> recipientsEmailList = mapper.readValue(recipientsEmail, List.class);
        signRequestService.pendingSignRequest(signRequest, recipientsEmailList, signRequestService.getSignTypeByLevel(signLevel), user);
        logger.info("new signRequest created by " + user.getEppn());
        return signRequest.getToken();
    }

    @ResponseBody
    @RequestMapping(value = "/add-docs-in-sign-book-group/{name}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Object addDocument(@PathVariable("name") String name,
                              @RequestParam("multipartFiles") MultipartFile[] multipartFiles, HttpServletRequest httpServletRequest) throws EsupSignatureException, EsupSignatureIOException {
        logger.info("start add documents");
        User user = userService.getUserFromAuthentication();
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
    @RequestMapping(value = "/add-docs-in-sign-book-unique/{name}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Object addDocumentToNewSignRequest(@PathVariable("name") String name,
                                              @RequestParam("multipartFiles") MultipartFile[] multipartFiles, HttpServletRequest httpServletRequest) throws EsupSignatureException, EsupSignatureIOException, IOException {
        logger.info("start add documents");
        User user = userService.getUserFromAuthentication();
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
    @RequestMapping(value = "/add-workflow-step", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity addWorkflowStep(@RequestParam String name,
                                  @RequestParam String jsonWorkflowStepString) throws IOException {
        SignBook signBook = signBookRepository.findByName(name).get(0);
        SignType signType = null;
        ObjectMapper mapper = new ObjectMapper();
        JsonWorkflowStep jsonWorkflowStep = mapper.readValue(jsonWorkflowStepString, JsonWorkflowStep.class);
        int level = jsonWorkflowStep.getSignLevel();
        signType = signRequestService.getSignTypeByLevel(level);
        WorkflowStep workflowStep = workflowService.createWorkflowStep(jsonWorkflowStep.getRecipientEmails(), "", jsonWorkflowStep.getAllSignToComplete(), signType);
        signBook.getWorkflowSteps().add(workflowStep);
        signBookRepository.save(signBook);
        return new ResponseEntity(HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(value = "/list-sign-requests", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Doc> listSignedFiles(@RequestParam("recipientEmail") String recipientEmail, HttpServletRequest httpServletRequest) throws IOException, EsupSignatureException {
        List<Doc> signedFiles = new ArrayList<>();
        User user = userService.getUser(recipientEmail);
        List<SignRequest> signRequests = signRequestService.getSignRequestsSignedByUser(user);

        for (SignRequest signRequest : signRequests) {
            signedFiles.add(new Doc(signRequest.getTitle(), signRequest.getSignedDocuments().get(0).getContentType(), signRequest.getToken(), signRequest.getStatus().name(), signRequest.getCreateDate()));

        }
        return signedFiles;
    }

//    private void getSignRequestSignedByUser(List<Doc> signedFiles, User user) {
//        List<Log> logs = new ArrayList<>();
//        logs.addAll(logRepository.findByEppnAndFinalStatus(user.getEppn(), SignRequestStatus.signed.name()));
//        logs.addAll(logRepository.findByEppnAndFinalStatus(user.getEppn(), SignRequestStatus.checked.name()));
//        logs:
//		for (Log log : logs) {
//            logger.debug("find log : " + log.getSignRequestId() + ", " + log.getFinalStatus());
//            try {
//                SignRequest signRequest = signRequestRepository.findById(log.getSignRequestId()).get();
//                for(Doc doc : signedFiles) {
//                    if (doc.getToken().equals(signRequest.getToken())) {
//                        continue logs;
//                    }
//                }
//                signedFiles.add(new Doc(signRequest.getTitle(), signRequest.getSignedDocuments().get(0).getContentType(), signRequest.getToken(), signRequest.getStatus().name(), log.getLogDate()));
//            } catch (Exception e) {
//                logger.debug(e.getMessage());
//            }
//        }
//    }

    @ResponseBody
    @RequestMapping(value = "/pending-sign-book", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String pendingSignBook(@RequestParam String name) throws EsupSignatureIOException {
        SignBook signBook = signBookRepository.findByName(name).get(0);
//        for (SignRequest signRequest : signBook.getSignRequests()) {
//            if(signRequest.getOriginalDocuments().size() == 1 && signRequest.getOriginalDocuments().get(0).getContentType().equals("application/pdf")) {
//                signRequestService.scanSignatureFields(signRequest.getOriginalDocuments().get(0).getInputStream());
//            }
//        }
        signBookService.pendingSignBook(signBook, userService.getSystemUser());
        return signBook.getSignRequests().get(0).getToken();
    }

    @ResponseBody
    @RequestMapping(value = "/pending-sign-request", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public void pendingSignRequest(@RequestParam String token,
                                   @RequestParam("recipientsEmail") String recipientsEmail,
                                   @RequestParam("signLevel") int signLevel) throws EsupSignatureIOException, IOException {
        if (signRequestRepository.countByToken(token) > 0) {
            SignRequest signRequest = signRequestRepository.findByToken(token).get(0);
            ObjectMapper mapper = new ObjectMapper();
            List<String> recipientsEmailList = mapper.readValue(recipientsEmail, List.class);
            signRequestService.pendingSignRequest(signRequest, recipientsEmailList, signRequestService.getSignTypeByLevel(signLevel), userService.getSystemUser());
        }
    }

    @ResponseBody
    @RequestMapping(value = "/list-to-sign-requests", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Doc> listToSignedFiles(@RequestParam("recipientEmail") String recipientEmail, HttpServletRequest httpServletRequest) {
        List<Doc> signedFiles = new ArrayList<>();
        User user = userRepository.findByEmail(recipientEmail).get(0);
        user.setIp(httpServletRequest.getRemoteAddr());
        for (SignRequest signRequest : signRequestService.getToSignRequests(user)) {
            signedFiles.add(new Doc(signRequest.getTitle(), signRequestService.getToSignDocuments(signRequest).get(0).getContentType(), signRequest.getToken(), signRequest.getStatus().name(), signRequest.getCreateDate()));
        }
        return signedFiles;
    }

    @ResponseBody
    @RequestMapping(value = "/create-workflow", method = RequestMethod.POST)
    public String createWorkflow(@RequestParam String workflowString, @RequestParam String signBookType, HttpServletRequest httpServletRequest) throws IOException, ParseException, EsupSignatureException {
        User user = userService.getSystemUser();
        user.setIp(httpServletRequest.getRemoteAddr());
        ObjectMapper mapper = new ObjectMapper();
        Workflow workflow = workflowService.createWorkflow(mapper.readValue(workflowString, Workflow.class).getName(), user, true);
        return workflow.getName();
    }

    @ResponseBody
    @RequestMapping(value = "/delete-sign-book", method = RequestMethod.POST)
    public ResponseEntity<String> deleteSignBook(@RequestParam String name, HttpServletRequest httpServletRequest) {
        User user = userService.getSystemUser();
        user.setIp(httpServletRequest.getRemoteAddr());
        if (signBookRepository.countByName(name) > 0) {
            SignBook signBook = signBookRepository.findByName(name).get(0);
            if(signBook.isExternal()) {
                signBookService.deleteSignBook(signBook);
            }
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(value = "/delete-sign-request", method = RequestMethod.POST)
    public ResponseEntity<String> deleteSignRequest(@RequestParam String token, HttpServletRequest httpServletRequest) {
        User user = userService.getSystemUser();
        user.setIp(httpServletRequest.getRemoteAddr());
        if (signRequestRepository.countByToken(token) > 0) {
            SignRequest signRequest = signRequestRepository.findByToken(token).get(0);
            signRequestRepository.delete(signRequest);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Transactional
    @RequestMapping(value = "/get-signed-file", method = RequestMethod.GET)
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

    @Transactional
    @RequestMapping(value = "/get-last-file-from-signrequest", method = RequestMethod.GET)
    public ResponseEntity<Void> getLastFileFromSignRequest(@RequestParam String token, HttpServletResponse response) {
        try {
            //TODO add user to check right
            if (signRequestRepository.countByToken(token) > 0) {
                SignRequest signRequest = signRequestRepository.findByToken(token).get(0);
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
            } else {
                logger.warn("no signRequest " + token);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (NoResultException | IOException e) {
            logger.error(e.getMessage(), e);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Transactional
    @RequestMapping(value = "/get-last-file-from-signbook", method = RequestMethod.GET)
    public ResponseEntity<Void> getLastFileFromSignBook(@RequestParam String name, HttpServletResponse response) {
        try {
            //TODO add user to check right
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

    @Transactional
    @RequestMapping(value = "/get-last-file", method = RequestMethod.GET)
    public ResponseEntity<Void> getLastFile(@RequestParam String token, HttpServletResponse response) {
        try {
            //TODO add user to check right
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
    @RequestMapping(value = "/check-sign-request", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonSignInfoMessage checkSignRequest(@RequestParam String fileToken) {
        try {
            if (signRequestRepository.countByToken(fileToken) > 0) {
                SignRequest signRequest = signRequestRepository.findByToken(fileToken).get(0);
                JsonSignInfoMessage jsonSignInfoMessage = new JsonSignInfoMessage();
                jsonSignInfoMessage.setStatus(signRequest.getStatus().toString());
                for (Long userId : signRequestService.getCurrentRecipients(signRequest).keySet()) {
                    User user = userRepository.findById(userId).get();
                    jsonSignInfoMessage.getNextRecipientNames().add(user.getName());
                    jsonSignInfoMessage.getNextRecipientEppns().add(user.getEppn());
                }
                return jsonSignInfoMessage;
            }
        } catch (NoResultException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @ResponseBody
    @RequestMapping(value = "/check-sign-book", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonSignInfoMessage checkSignBook(@RequestParam String fileToken) {
        try {
            if (signRequestRepository.countByToken(fileToken) > 0) {
                SignRequest signRequest = signRequestRepository.findByToken(fileToken).get(0);
                JsonSignInfoMessage jsonSignInfoMessage = new JsonSignInfoMessage();
                jsonSignInfoMessage.setStatus(signRequest.getStatus().toString());
                if(signRequest.getParentSignBook().getWorkflowSteps().size() > 0 ) {
                    workflowService.setWorkflowsLabels(signRequest.getParentSignBook().getWorkflowSteps());
                    for (Long userId : signRequestService.getCurrentRecipients(signRequest).keySet()) {
                        User user = userRepository.findById(userId).get();
                        jsonSignInfoMessage.getNextRecipientNames().add(user.getName());
                        jsonSignInfoMessage.getNextRecipientEppns().add(user.getEppn());
                    }
                }
                return jsonSignInfoMessage;
            }
        } catch (NoResultException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @ResponseBody
    @RequestMapping(value = "/check-user-status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String checkUserStatus(@RequestParam String eppn) throws JsonProcessingException {
        if (userRepository.countByEppn(eppn) > 0) {
            User user = userRepository.findByEppn(eppn).get(0);
            return new ObjectMapper().writeValueAsString(true);
        } else {
            return new ObjectMapper().writeValueAsString(false);
        }
    }

    @Transactional
    @RequestMapping(value = "/complete-sign-book", method = RequestMethod.POST)
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
    @RequestMapping(value = "/complete-sign-request", method = RequestMethod.POST)
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

    //TODO refactor with UserController
    @ResponseBody
    @RequestMapping(value = "/searchUser")
    public List<PersonLdap> searchUser(@RequestParam(value = "searchString") String searchString, @RequestParam(required = false) String ldapTemplateName) {
        logger.debug("ldap search for : " + searchString);
        return userService.getPersonLdaps(searchString, ldapTemplateName);
    }


//    @RequestMapping(value = "/search-signbook")
//    @ResponseBody
//    public List<PersonLdap> searchSignBook(@RequestParam(value = "searchString") String searchString, @RequestParam(required = false) String ldapTemplateName) {
//        logger.debug("signBook search for : " + searchString);
//        HttpHeaders headers = new HttpHeaders();
//        headers.add("Content-Type", "application/json; charset=utf-8");
//        List<PersonLdap> ldapList = new ArrayList<PersonLdap>();
//        if (ldapPersonService != null && !searchString.trim().isEmpty() && searchString.length() > 3) {
//            List<PersonLdap> ldapSearchList = ldapSearchList = ldapPersonService.search(searchString, ldapTemplateName);
//            ldapList.addAll(ldapSearchList.stream().sorted(Comparator.comparing(PersonLdap::getDisplayName)).collect(Collectors.toList()));
//
//        }
//
//        List<SignBook> signBooks = new ArrayList<>();
//        signBooks.addAll(signBookRepository.findBySignBookType(SignBookType.system));
//        signBooks.addAll(signBookRepository.findBySignBookType(SignBookType.group));
//
//        for (SignBook signBook : signBooks) {
//            PersonLdap personLdap = new PersonLdap();
//            personLdap.setUid(signBook.getSignBookType().toString());
//            personLdap.setMail(signBook.getName());
//            personLdap.setDisplayName(signBook.getName());
//            ldapList.add(personLdap);
//        }
//        return ldapList;
//    }

}
