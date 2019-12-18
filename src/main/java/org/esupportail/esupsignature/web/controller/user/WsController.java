package org.esupportail.esupsignature.web.controller.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.SignBook.SignBookType;
import org.esupportail.esupsignature.entity.SignRequest.SignRequestStatus;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.ldap.PersonLdap;
import org.esupportail.esupsignature.repository.LogRepository;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.repository.UserRepository;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.file.FileService;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@Transactional
@RequestMapping(value = "/ws/")
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
    private UserRepository userRepository;

    @Resource
    private UserService userService;

    @Resource
    private FileService fileService;

    @Resource
    private DocumentService documentService;

    @Resource
    private LogRepository logRepository;

    @Autowired(required = false)
    private LdapPersonService ldapPersonService;

    // TODO creation / recupération de demandes par WS + declenchement
    // d'evenements + multidocs
    @ResponseBody
    @RequestMapping(value = "/create-sign-request", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String createSignRequest(@RequestParam("file") MultipartFile file,
                                    @RequestParam String creatorEmail,
                                    HttpServletRequest httpServletRequest) throws IOException {
        User user;
        if(userRepository.findByEmail(creatorEmail).size() > 0) {
            user = userRepository.findByEmail(creatorEmail).get(0);
        } else {
            user = userService.createUser(creatorEmail);
        }
        user.setIp(httpServletRequest.getRemoteAddr());
        if (file != null) {
            Document documentToAdd = documentService.createDocument(file, file.getOriginalFilename());
            SignRequest signRequest = signRequestService.createSignRequest(new SignRequest(), user, documentToAdd);
            signRequest.setTitle(fileService.getNameOnly(documentToAdd.getFileName()));
            signRequestRepository.save(signRequest);
            logger.info("adding new file into signRequest " + signRequest.getToken());
            return signRequest.getToken();
        } else {
            logger.warn("no file to import");
        }
        return null;
    }

    @ResponseBody
    @RequestMapping(value = "/list-sign-requests", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Doc> listSignedFiles(@RequestParam("recipientEmail") String recipientEmail, HttpServletRequest httpServletRequest) throws IOException, EsupSignatureException {
        List<Doc> signedFiles = new ArrayList<>();
        List<User> users = userRepository.findByEmail(recipientEmail);
        User user;
        if(users.size() > 0){
            user = users.get(0);
        } else {
            user = userService.createUser(recipientEmail);
        }
        user.setIp(httpServletRequest.getRemoteAddr());
        List<Log> logs = new ArrayList<>();
        logs.addAll(logRepository.findByEppnAndFinalStatus(user.getEppn(), SignRequestStatus.signed.name()));
		logs.addAll(logRepository.findByEppnAndFinalStatus(user.getEppn(), SignRequestStatus.checked.name()));
        logs:
		for (Log log : logs) {
            logger.debug("find log : " + log.getSignRequestId() + ", " + log.getFinalStatus());
            try {
                SignRequest signRequest = signRequestRepository.findById(log.getSignRequestId()).get();
                for(Doc doc : signedFiles) {
                    if (doc.getToken().equals(signRequest.getToken())) {
                        continue logs;
                    }
                }
                signedFiles.add(new Doc(signRequest.getTitle(), signRequest.getSignedDocuments().get(0).getContentType(), signRequest.getToken(), signRequest.getStatus().name(), log.getLogDate()));
            } catch (Exception e) {
                logger.debug(e.getMessage());
            }
        }
        return signedFiles;
    }

    @ResponseBody
    @RequestMapping(value = "/pending-sign-request", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String pendingSignRequest(@RequestParam String token) throws IOException {
        SignRequest signRequest = signRequestRepository.findByToken(token).get(0);
        signRequestService.pendingSignRequest(signRequest, userService.getSystemUser());
        return null;
    }

    @ResponseBody
    @RequestMapping(value = "/add-workflow-step", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String addWorkflowStep(@RequestParam String token,
                                  @RequestParam String jsonWorkflowStepString) throws IOException {
        SignRequest signRequest = signRequestRepository.findByToken(token).get(0);
        SignRequestParams.SignType signType = null;
        ObjectMapper mapper = new ObjectMapper();
        JsonWorkflowStep jsonWorkflowStep = mapper.readValue(jsonWorkflowStepString, JsonWorkflowStep.class);
        switch (jsonWorkflowStep.getSignLevel()) {
            case 0:
                signType = SignRequestParams.SignType.visa;
                break;
            case 1:
                signType = SignRequestParams.SignType.pdfImageStamp;
                break;
            case 2:
                signType = SignRequestParams.SignType.certSign;
                break;
            case 3:
                signType = SignRequestParams.SignType.nexuSign;
                break;
        }
        signRequestService.addWorkflowStep(jsonWorkflowStep.getRecipientEmails(), "name", jsonWorkflowStep.getAllSignToComplete(), signType, signRequest);
        return null;
    }

    @ResponseBody
    @RequestMapping(value = "/list-to-sign-requests", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Doc> listToSignedFiles(@RequestParam("recipientEmail") String recipientEmail, HttpServletRequest httpServletRequest) {
        List<Doc> signedFiles = new ArrayList<>();
        User user = userRepository.findByEmail(recipientEmail).get(0);
        user.setIp(httpServletRequest.getRemoteAddr());
        for (SignRequest signRequest : signRequestService.getTosignRequests(user)) {
            signedFiles.add(new Doc(signRequest.getTitle(), signRequestService.getToSignDocuments(signRequest).get(0).getContentType(), signRequest.getToken(), signRequest.getStatus().name(), signRequest.getCreateDate()));
        }
        return signedFiles;
    }

    @ResponseBody
    @RequestMapping(value = "/create-workflow", method = RequestMethod.POST)
    public String createSignBook(@RequestParam String workflowString, @RequestParam String signBookType, HttpServletRequest httpServletRequest) throws IOException, ParseException, EsupSignatureException {
        User user = userService.getSystemUser();
        user.setIp(httpServletRequest.getRemoteAddr());
        ObjectMapper mapper = new ObjectMapper();
        Workflow workflow = workflowService.createWorkflow(mapper.readValue(workflowString, Workflow.class), user, null, true);
        return workflow.getName();
    }

    @ResponseBody
    @RequestMapping(value = "/delete-sign-book", method = RequestMethod.POST)
    public ResponseEntity<String> deleteSignBook(@RequestParam String signBookName, HttpServletRequest httpServletRequest) throws IOException, ParseException, EsupSignatureException {
        User user = userService.getSystemUser();
        user.setIp(httpServletRequest.getRemoteAddr());
        if (signBookRepository.countByName(signBookName) > 0) {
            SignBook signBook = signBookRepository.findByName(signBookName).get(0);
            if(signBook.isExternal()) {
                signBookService.deleteSignBook(signBook);
            }
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
                    FsFile file = signRequestService.getLastSignedFile(signRequest);
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
    public ResponseEntity<Void> getLastFile(@RequestParam String name, HttpServletResponse response) {
        try {
            //TODO add user to check right
            SignRequest signRequest = signRequestRepository.findByToken(name).get(0);
            if (signRequest != null) {
                try {
                    FsFile file = signRequestService.getLastSignedFile(signRequest);
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

    @ResponseBody
    @RequestMapping(value = "/check-sign-request", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonSignInfoMessage checkSignRequest(@RequestParam String fileToken) {
        try {
            if (signRequestRepository.countByToken(fileToken) > 0) {
                SignRequest signRequest = signRequestRepository.findByToken(fileToken).get(0);
                JsonSignInfoMessage jsonSignInfoMessage = new JsonSignInfoMessage();
                jsonSignInfoMessage.setStatus(signRequest.getStatus().toString());
                if(signRequest.getWorkflowSteps().size() > 0 ) {
                    signRequestService.setWorkflowsLabels(signRequest.getWorkflowSteps());
                    for (Long userId : signRequest.getCurrentWorkflowStep().getRecipients().keySet()) {
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
    @RequestMapping(value = "/delete-sign-request", method = RequestMethod.GET)
    public void deleteSignRequest(@RequestParam String fileToken, HttpServletResponse response, Model model) {
        SignRequest signRequest = signRequestRepository.findByToken(fileToken).get(0);
        signRequestRepository.delete(signRequest);
    }

    @RequestMapping(value = "/sign-by-token/{token}")
    public String signByToken(@PathVariable("token") String token, HttpServletRequest request) {
        return "redirect:/user/signrequests/sign-by-token/" + token;
    }

    @ResponseBody
    @RequestMapping(value = "/check-user-status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String checkUserStatus(@RequestParam String eppn, HttpServletResponse response, Model model) throws JsonProcessingException {
        if (userRepository.countByEppn(eppn) > 0) {
            User user = userRepository.findByEppn(eppn).get(0);
            return new ObjectMapper().writeValueAsString(true);
        } else {
            return new ObjectMapper().writeValueAsString(false);
        }
    }

    @Transactional
    @RequestMapping(value = "/complete-sign-request", method = RequestMethod.POST)
    public ResponseEntity<Void> completeSignRequest(@RequestParam String signBookName, @RequestParam String name, HttpServletRequest httpServletRequest, HttpServletResponse response, Model model) {
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

    //TODO refactor with UserController
    @RequestMapping(value = "/searchLdap")
    @ResponseBody
    public List<PersonLdap> searchLdap(@RequestParam(value = "searchString") String searchString, @RequestParam(required = false) String ldapTemplateName) {
        logger.debug("ldap search for : " + searchString);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json; charset=utf-8");
        List<PersonLdap> ldapList = new ArrayList<PersonLdap>();
        if (ldapPersonService != null && !searchString.trim().isEmpty() && searchString.length() > 3) {
            List<PersonLdap> ldapSearchList = ldapPersonService.search(searchString, ldapTemplateName);
            ldapList.addAll(ldapSearchList.stream().sorted(Comparator.comparing(PersonLdap::getDisplayName)).collect(Collectors.toList()));

        }
        return ldapList;
    }

    @RequestMapping(value = "/search-signbook")
    @ResponseBody
    public List<PersonLdap> searchSignBook(@RequestParam(value = "searchString") String searchString, @RequestParam(required = false) String ldapTemplateName) {
        logger.debug("signBook search for : " + searchString);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json; charset=utf-8");
        List<PersonLdap> ldapList = new ArrayList<PersonLdap>();
        if (ldapPersonService != null && !searchString.trim().isEmpty() && searchString.length() > 3) {
            List<PersonLdap> ldapSearchList = ldapSearchList = ldapPersonService.search(searchString, ldapTemplateName);
            ldapList.addAll(ldapSearchList.stream().sorted(Comparator.comparing(PersonLdap::getDisplayName)).collect(Collectors.toList()));

        }

        List<SignBook> signBooks = new ArrayList<>();
        signBooks.addAll(signBookRepository.findBySignBookType(SignBookType.system));
        signBooks.addAll(signBookRepository.findBySignBookType(SignBookType.group));

        for (SignBook signBook : signBooks) {
            PersonLdap personLdap = new PersonLdap();
            personLdap.setUid(signBook.getSignBookType().toString());
            personLdap.setMail(signBook.getName());
            personLdap.setDisplayName(signBook.getName());
            ldapList.add(personLdap);
        }
        return ldapList;
    }

}
