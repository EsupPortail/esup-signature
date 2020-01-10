package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.SignBook.SignBookType;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.repository.*;
import org.esupportail.esupsignature.service.fs.FsAccessFactory;
import org.esupportail.esupsignature.service.fs.FsAccessService;
import org.esupportail.esupsignature.service.fs.UploadActionType;
import org.esupportail.esupsignature.service.mail.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.spel.spi.EvaluationContextExtension;
import org.springframework.data.spel.spi.Function;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SignBookService implements EvaluationContextExtension {

    private static final Logger logger = LoggerFactory.getLogger(SignBookService.class);

    @Resource
    private SignBookRepository signBookRepository;

    @Resource
    private SignRequestRepository signRequestRepository;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private UserRepository userRepository;

    @Resource
    private WorkflowRepository workflowRepository;

    @Resource
    private WorkflowStepRepository workflowStepRepository;

    @Resource
    private UserService userService;

    @Resource
    private FsAccessFactory fsAccessFactory;

    @Resource
    private MailService mailService;

    @Resource
    private DocumentService documentService;

    @Resource
    private LogRepository logRepository;

    @Resource
    private WorkflowService workflowService;

    public List<SignBook> getAllSignBooks() {
        List<SignBook> list = new ArrayList<SignBook>();
        signBookRepository.findAll().forEach(e -> list.add(e));
        return list;
    }

    public List<SignBook> getTosignRequests(User user) {
        List<SignBook> signBooksToSign = new ArrayList<>();
        List<WorkflowStep> workflowSteps = workflowStepRepository.findByRecipients(user.getId());
        for(WorkflowStep workflowStep : workflowSteps) {
            if(!workflowStep.getRecipients().get(user.getId()) && signBookRepository.findByWorkflowStepsContains(workflowStep).size() > 0) {
                SignBook signBook = signBookRepository.findByWorkflowStepsContains(workflowStep).get(0);
                if (getCurrentWorkflowStep(signBook).equals(workflowStep) && getStatus(signBook).equals(SignRequestStatus.pending)) {
                    signBooksToSign.add(signBook);
                }
            }
        }
        return signBooksToSign.stream().sorted(Comparator.comparing(SignBook::getCreateDate).reversed()).collect(Collectors.toList());
    }

    public void addRecipient(SignBook signBook, List<String> recipientEmails) {
        for (String recipientEmail : recipientEmails) {
            if (userRepository.countByEmail(recipientEmail) == 0) {
                userService.createUser(recipientEmail);
            }
            signBook.getRecipientEmails().add(recipientEmail);
        }
    }

    public void removeRecipient(SignBook signBook, String recipientEmail) {
        signBook.getRecipientEmails().remove(recipientEmail);
    }

    public SignBook createSignBook(String name, SignBookType signBookType, User user, boolean external) throws EsupSignatureException {
        if (signBookRepository.countByName(name) == 0) {
            SignBook signBook = new SignBook();
            signBook.setStatus(SignRequestStatus.draft);
            signBook.setName(name);
            signBook.setSignBookType(signBookType);
            signBook.setCreateBy(user.getEppn());
            signBook.setCreateDate(new Date());
            signBook.setExternal(external);
            signBookRepository.save(signBook);
            return signBook;
        } else {
            throw new EsupSignatureException("Un parapheur porte déjà ce nom");
        }
    }

    public SignBook getSignBook(String name, User user) throws EsupSignatureException {
        if (signBookRepository.countByName(name) == 0) {
            return createSignBook(name, SignBookType.workflow, user, false);
        } else {
            return signBookRepository.findByName(name).get(0);
        }
    }

    public void addSignRequest(SignBook signBook, SignRequest signRequest) {
        signBook.getSignRequests().add(signRequest);
        signBookRepository.save(signBook);
    }

    public void deleteSignBook(SignBook signBook) {
        signBookRepository.delete(signBook);
    }

    public void removeSignRequestFromSignBook(SignBook signBook, SignRequest signRequest) {
        signRequestRepository.save(signRequest);
        signBook.getSignRequests().remove(signRequest);
        signBookRepository.save(signBook);
    }

    public boolean isUserInWorkflow(SignBook signBook, User user) {
        if (getCurrentWorkflowStep(signBook) != null && getCurrentWorkflowStep(signBook).getRecipients().size() > 0) {
            for (Map.Entry<Long, Boolean> userId : getCurrentWorkflowStep(signBook).getRecipients().entrySet()) {
                if (userId.getKey().equals(user.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean checkUserManageRights(User user, SignBook signBook) {
        if (signBook.getCreateBy().equals(user.getEppn()) || signBook.getCreateBy().equals("System")) {
            return true;
        } else {
            return false;
        }
    }

    public void importWorkflow(SignBook signBook, Workflow workflow) {
        for (WorkflowStep workflowStep : workflow.getWorkflowSteps()) {
            WorkflowStep newWorkflowStep = new WorkflowStep();
            newWorkflowStep.setSignType(workflowStep.getSignType());
            newWorkflowStep.setAllSignToComplete(workflowStep.isAllSignToComplete());
            for(Long userId : workflowStep.getRecipients().keySet()){
                User recipient = userRepository.findById(userId).get();
                if(recipient.getEppn().equals("creator")) {
                    recipient = userRepository.findByEmail(userService.getUserFromAuthentication().getEmail()).get(0);
                }
                newWorkflowStep.getRecipients().put(recipient.getId(), false);
            }
            workflowStepRepository.save(newWorkflowStep);
            signBook.getWorkflowSteps().add(newWorkflowStep);
        }
        signBook.setTargetType(workflow.getTargetType());
        signBook.setDocumentsTargetUri(workflow.getDocumentsTargetUri());
        signBookRepository.save(signBook);
    }


    public void saveWorkflow(String name, User user, SignBook signBook) throws EsupSignatureException {
        Workflow workflow;
        workflow = workflowService.createWorkflow(name, user, null, false);
        for(WorkflowStep workflowStep : signBook.getWorkflowSteps()) {
            WorkflowStep toSaveWorkflowStep = new WorkflowStep();
            toSaveWorkflowStep.getRecipients().putAll(workflowStep.getRecipients());
            toSaveWorkflowStep.setSignType(workflowStep.getSignType());
            workflowStepRepository.save(toSaveWorkflowStep);
            workflow.getWorkflowSteps().add(toSaveWorkflowStep);
        }
        workflow.setCreateDate(new Date());
        workflow.setCreateBy(user.getEppn());
        workflowRepository.save(workflow);
    }

    public void exportFilesToTarget(SignBook signBook, User user) throws EsupSignatureException {
        logger.trace("export signRequest to : " + signBook.getTargetType() + "://" + signBook.getDocumentsTargetUri());
        if (getStatus(signBook).equals(SignRequestStatus.completed) /* && signRequestService.isSignRequestCompleted(signRequest)*/) {
            boolean exportOk = exportFileToTarget(signBook, user);
            if (exportOk) {
                if (!signBook.getTargetType().equals(DocumentIOType.mail)) {
                    clearAllDocuments(signBook);
                }
            }
        }
    }


    public boolean exportFileToTarget(SignBook signBook, User user) throws EsupSignatureException {
        if (signBook.getTargetType() != null && !signBook.getTargetType().equals(DocumentIOType.none)) {
            if (signBook.getTargetType().equals(DocumentIOType.mail)) {
                logger.info("send by email to " + signBook.getDocumentsTargetUri());
                mailService.sendFile(signBook);
                signBook.setExportedDocumentURI("mail://" + signBook.getDocumentsTargetUri());
                return true;
            } else {
                for(SignRequest signRequest : signBook.getSignRequests()) {
                    Document signedFile = signRequestService.getLastSignedDocument(signRequest);
                    try {
                        logger.info("send to " + signBook.getTargetType() + " in /" + signBook.getDocumentsTargetUri() + "/signed");
                        FsAccessService fsAccessService = fsAccessFactory.getFsAccessService(signBook.getTargetType());
                        InputStream inputStream = signedFile.getInputStream();
                        fsAccessService.createFile("/", signBook.getDocumentsTargetUri(), "folder");
                        fsAccessService.createFile("/" + signBook.getDocumentsTargetUri() + "/", "signed", "folder");
                        signBook.setExportedDocumentURI(fsAccessService.getUri() + "/" + signBook.getDocumentsTargetUri() + "/signed/" + signedFile.getFileName());
                        return fsAccessService.putFile("/" + signBook.getDocumentsTargetUri() + "/signed/", signedFile.getFileName(), inputStream, UploadActionType.OVERRIDE);
                    } catch (Exception e) {
                        throw new EsupSignatureException("write fsaccess error : ", e);
                    }
                }
            }
        } else {
            logger.debug("no target type for this signbook");
        }
        return false;
    }

    public List<Document> getLastSignedDocuments(SignBook signBook) {
        List<Document> documents = new ArrayList<>();
        for (SignRequest signRequest : signBook.getSignRequests()) {
            documents.add(signRequestService.getLastSignedDocument(signRequest));
        }
        return documents;
    }


    public void clearAllDocuments(SignBook signBook) {
        for (SignRequest signRequest : signBook.getSignRequests()) {
            logger.info("clear all documents from " + signRequest.getToken());
            List<Document> originalDocuments = new ArrayList<Document>();
            originalDocuments.addAll(signRequest.getOriginalDocuments());
            signRequest.getOriginalDocuments().clear();
            for (Document document : originalDocuments) {
                documentService.deleteDocument(document);
            }
            List<Document> signedDocuments = new ArrayList<Document>();
            signedDocuments.addAll(signRequest.getSignedDocuments());
            signRequest.getSignedDocuments().clear();
            for (Document document : signedDocuments) {
                documentService.deleteDocument(document);
            }
            signRequestRepository.save(signRequest);
        }
    }

    public boolean isSignBookCompleted(SignBook signBook) {
        if (getCurrentWorkflowStep(signBook).getRecipients() != null) {
            for (Map.Entry<Long, Boolean> userId : getCurrentWorkflowStep(signBook).getRecipients().entrySet()) {
                if (!getCurrentWorkflowStep(signBook).getRecipients().get(userId.getKey()) && getCurrentWorkflowStep(signBook).isAllSignToComplete()) {
                    return false;
                }
                if (getCurrentWorkflowStep(signBook).getRecipients().get(userId.getKey()) && !getCurrentWorkflowStep(signBook).isAllSignToComplete()) {
                    return true;
                }
            }
            if (getCurrentWorkflowStep(signBook).isAllSignToComplete()) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public void removeStep(SignBook signBook, int step) {
        WorkflowStep workflowStep = signBook.getWorkflowSteps().get(step);
        signBook.getWorkflowSteps().remove(step);
//        if(signBook.getWorkflowSteps().size() < signBook.getCurrentWorkflowStepNumber() && getStatus(signBook).equals(SignRequestStatus.pending)) {
//            signBook.setStatus(SignRequestStatus.completed);
//        }
        signBookRepository.save(signBook);
        workflowStepRepository.delete(workflowStep);
    }

    public void toggleNeedAllSign(SignBook signBook, int step, Boolean allSignToComplete) {
        WorkflowStep workflowStep = signBook.getWorkflowSteps().get(step);
        if(allSignToComplete != null && !allSignToComplete.equals(workflowStep.isAllSignToComplete())) {
            workflowService.toggleAllSignToCompleteForWorkflowStep(workflowStep);
        }
    }

    public void changeSignType(SignBook signBook, int step, String name, SignType signType) {
        WorkflowStep workflowStep = signBook.getWorkflowSteps().get(step);
        if(name != null) {
            workflowStep.setName(name);
        }
        workflowService.setSignTypeForWorkflowStep(signType, workflowStep);
    }

    public boolean isStepDone(SignBook signBook) {
        for (SignRequest signRequest : signBook.getSignRequests()) {
            if (signRequest.getStatus().equals(SignRequestStatus.pending)) {
                return false;
            }
        }
        return true;
    }

    public void nextWorkFlowStep(SignBook signBook, User user) {
        signBook.setCurrentWorkflowStepNumber(signBook.getCurrentWorkflowStepNumber() + 1);
    }

    public boolean checkUserViewRights(User user, SignBook signBook) {
        if (signBook.getCreateBy().equals(user.getEppn()) || isUserInWorkflow(signBook, user)) {
            return true;
        }
        return false;
    }

    public boolean checkUserSignRights(User user, SignBook signBook) {
        if ((getStatus(signBook).equals(SignRequestStatus.pending) || getStatus(signBook).equals(SignRequestStatus.draft))
                && getCurrentWorkflowStep(signBook).getRecipients().containsKey(user.getId()) &&  !getCurrentWorkflowStep(signBook).getRecipients().get(user.getId())) {
            return true;
        } else {
            return false;
        }
    }

    public void pendingSignRequest(SignBook signBook, User user) {
        if(!getStatus(signBook).equals(SignRequestStatus.pending)) {
            updateStatus(signBook, SignRequestStatus.pending, "Envoyé pour signature", user, "SUCCESS", signBook.getComment());
            for(SignRequest signRequest : signBook.getSignRequests()) {
                signRequest.setStatus(SignRequestStatus.pending);
            }
            for (Long recipientId : getCurrentWorkflowStep(signBook).getRecipients().keySet()) {
                User recipientUser = userRepository.findById(recipientId).get();
                if (recipientUser.getEmailAlertFrequency() == null || recipientUser.getEmailAlertFrequency().equals(User.EmailAlertFrequency.immediately) || userService.checkEmailAlert(recipientUser)) {
                    userService.sendEmailAlert(recipientUser);
                }
            }
        } else {
            logger.warn("already pending");
        }
    }

    public void updateStatus(SignBook signBook, SignRequestStatus signRequestStatus, String action, User user, String returnCode, String comment) {
        updateStatus(signBook, signRequestStatus, action, user, returnCode, comment, null, null, null);
    }

    public void updateStatus(SignBook signBook, SignRequestStatus signRequestStatus, String action, User user, String returnCode, String comment, Integer pageNumber, Integer posX, Integer posY ) {
        Log log = new Log();
        log.setSignRequestId(signBook.getId());
        log.setEppn(user.getEppn());
        log.setIp(user.getIp());
        log.setInitialStatus(getStatus(signBook).toString());
        log.setLogDate(new Date());
        log.setAction(action);
        log.setReturnCode(returnCode);
        log.setComment(comment);
        if(pageNumber != null) {
            log.setPageNumber(pageNumber);
            log.setPosX(posX);
            log.setPosY(posY);
        }
        if(signRequestStatus != null) {
            log.setFinalStatus(signRequestStatus.toString());
            signBook.setStatus(signRequestStatus);
        } else {
            log.setFinalStatus(getStatus(signBook).toString());
        }
        logRepository.save(log);
    }

    public WorkflowStep getCurrentWorkflowStep(SignBook signBook) {
        if(signBook.getWorkflowSteps().size() >= signBook.getCurrentWorkflowStepNumber()) {
            return signBook.getWorkflowSteps().get(signBook.getCurrentWorkflowStepNumber() - 1);
        } else {
            return new WorkflowStep();
        }
    }

    public SignRequestStatus getStatus(SignBook signBook) {
        //TODO move to signbookservice
        for(SignRequest signRequest : signBook.getSignRequests()) {
            if(signRequest.getStatus().equals(SignRequestStatus.refused)) {
                return SignRequestStatus.refused;
            }
        }
        if(signBook.getCurrentWorkflowStepNumber() <= signBook.getWorkflowSteps().size() && signBook.getRealStatus().equals(SignRequestStatus.pending) || signBook.getRealStatus().equals(SignRequestStatus.draft)) {
            return signBook.getRealStatus();
        }
        return SignRequestStatus.completed;
    }

    @Override
    public String getExtensionId() {
        return null;
    }

    @Override
    public Map<String, Object> getProperties() {
        return null;
    }

    @Override
    public Map<String, Function> getFunctions() {
        return null;
    }

    @Override
    public Object getRootObject() {
        return null;
    }
}
