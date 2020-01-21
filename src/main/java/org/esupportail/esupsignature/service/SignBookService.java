package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.SignBook.SignBookType;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.repository.*;
import org.esupportail.esupsignature.service.fs.FsAccessFactory;
import org.esupportail.esupsignature.service.mail.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SignBookService {

    private static final Logger logger = LoggerFactory.getLogger(SignBookService.class);

    @Resource
    private SignBookRepository signBookRepository;

    @Resource
    private SignRequestRepository signRequestRepository;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private RecipientService recipientService;

    @Resource
    private RecipientRepository recipientRepository;

    @Resource
    private UserRepository userRepository;

    @Resource
    private WorkflowRepository workflowRepository;

    @Resource
    private WorkflowStepRepository workflowStepRepository;

    @Resource
    private UserService userService;

    @Resource
    private MailService mailService;

    @Resource
    private LogRepository logRepository;

    @Resource
    private WorkflowService workflowService;

    @Resource
    private WorkflowService workflowStepService;

    public List<SignBook> getAllSignBooks() {
        List<SignBook> list = new ArrayList<SignBook>();
        signBookRepository.findAll().forEach(e -> list.add(e));
        return list;
    }

    public List<SignBook> getTosignRequests(User user) {
        List<SignBook> signBooksToSign = new ArrayList<>();
        List<Recipient> recipients = recipientRepository.findByUser(user);
        List<WorkflowStep> workflowSteps = workflowStepRepository.findByRecipients(recipients);
        for(WorkflowStep workflowStep : workflowSteps) {
            if(!recipientService.findRecipientByUser(workflowStep.getRecipients(), user).getSigned() && signBookRepository.findByWorkflowStepsContains(workflowStep).size() > 0) {
                SignBook signBook = signBookRepository.findByWorkflowStepsContains(workflowStep).get(0);
                if (getCurrentWorkflowStep(signBook).equals(workflowStep) && signBook.getStatus().equals(SignRequestStatus.pending)) {
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
            logger.info("create new signBook : " + name);
            return createSignBook(name, SignBookType.workflow, user, false);
        } else {
            return signBookRepository.findByName(name).get(0);
        }
    }

    public void addSignRequest(SignBook signBook, SignRequest signRequest) {
        signBook.getSignRequests().add(signRequest);
        signBookRepository.save(signBook);
        signRequest.setParentSignBook(signBook);
        signRequestRepository.save(signRequest);
    }

    public void deleteSignBook(SignBook signBook) {
        signBookRepository.delete(signBook);
    }

    public void removeSignRequestFromSignBook(SignBook signBook, SignRequest signRequest) {
        signRequestRepository.save(signRequest);
        signBook.getSignRequests().remove(signRequest);
        signBookRepository.save(signBook);
    }

    public boolean checkUserManageRights(User user, SignBook signBook) {
        if (signBook.getCreateBy().equals(user.getEppn()) || signBook.getCreateBy().equals("System")) {
            return true;
        } else {
            return false;
        }
    }

    public void importWorkflow(SignBook signBook, Workflow workflow) {
        logger.info("import workflow steps in signBook " + signBook.getName() + " - " +signBook.getId());
        for (WorkflowStep workflowStep : workflow.getWorkflowSteps()) {
            WorkflowStep newWorkflowStep = new WorkflowStep();
            newWorkflowStep.setSignType(workflowStep.getSignType());
            newWorkflowStep.setAllSignToComplete(workflowStep.isAllSignToComplete());
            workflowStepRepository.save(newWorkflowStep);
            for(Recipient recipient : workflowStep.getRecipients()){
                User recipientUser;
                if(recipient.getUser().getEppn().equals("creator")) {
                    recipientUser = userRepository.findByEmail(userService.getUserFromAuthentication().getEmail()).get(0);
                    newWorkflowStep.getRecipients().add(recipientService.createRecipient(newWorkflowStep.getId(), recipientUser));
                } else {
                    newWorkflowStep.getRecipients().add(recipientService.createRecipient(newWorkflowStep.getId(), recipient.getUser()));
                }

            }
            signBook.getWorkflowSteps().add(newWorkflowStep);
        }
        signBook.setTargetType(workflow.getTargetType());
        signBook.setDocumentsTargetUri(workflow.getDocumentsTargetUri());
        signBookRepository.save(signBook);
    }

    public void saveWorkflow(String name, User user, SignBook signBook) throws EsupSignatureException {
        Workflow workflow;
        workflow = workflowService.createWorkflow(name, user, false);
        for(WorkflowStep workflowStep : signBook.getWorkflowSteps()) {
            WorkflowStep toSaveWorkflowStep = new WorkflowStep();
            for(Recipient recipient : workflowStep.getRecipients()) {
                toSaveWorkflowStep.getRecipients().add(recipientService.createRecipient(workflowStep.getId(), recipient.getUser()));
            }
            toSaveWorkflowStep.setSignType(workflowStep.getSignType());
            workflowStepRepository.save(toSaveWorkflowStep);
            workflow.getWorkflowSteps().add(toSaveWorkflowStep);
        }
        workflow.setCreateDate(new Date());
        workflow.setCreateBy(user.getEppn());
        workflowRepository.save(workflow);
    }

    public void completeSignBook(SignBook signBook, User user) throws EsupSignatureException {
        updateStatus(signBook, SignRequestStatus.completed, "Tous les documents sont signés", user, "SUCCESS", "");
        signRequestService.completeSignRequests(signBook.getSignRequests(), signBook.getTargetType(), signBook.getDocumentsTargetUri(), user);
    }

    public void exportFilesToTarget(SignBook signBook) throws EsupSignatureException {
        logger.trace("export signRequest to : " + signBook.getTargetType() + "://" + signBook.getDocumentsTargetUri());
        if (signBook.getStatus().equals(SignRequestStatus.completed)) {
           signRequestService.sendSignRequestsToTarget(signBook.getName(), signBook.getSignRequests(), signBook.getTargetType(), signBook.getDocumentsTargetUri());
        }
    }

    public void removeStep(SignBook signBook, int step) {
        WorkflowStep workflowStep = signBook.getWorkflowSteps().get(step);
        signBook.getWorkflowSteps().remove(step);
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
        WorkflowStep currentWorkflowStep = getCurrentWorkflowStep(signBook);
        for (SignRequest signRequest : signBook.getSignRequests()) {
            if (signRequest.getStatus().equals(SignRequestStatus.pending) || (currentWorkflowStep.isAllSignToComplete() && !workflowStepService.isWorkflowStepFullSigned(currentWorkflowStep))) {
                return false;
            }
        }
        return true;
    }

    public boolean nextWorkFlowStep(SignBook signBook) {
        signBook.setCurrentWorkflowStepNumber(signBook.getCurrentWorkflowStepNumber() + 1);
        signBookRepository.save(signBook);
        if(signBook.getWorkflowSteps().size() >= signBook.getCurrentWorkflowStepNumber()) {
            return true;
        } else {
            return false;
        }
    }

    public boolean checkUserViewRights(User user, SignBook signBook) {
        if(signBook.getCreateBy().equals(user.getEppn()) || recipientService.recipientsContainsUser(getCurrentWorkflowStep(signBook).getRecipients(), user) > 0) {
            return true;
        }
        return false;
    }

    public void pendingSignBook(SignBook signBook, User user) {
        logger.info("Parapheur " + signBook.getId() + " envoyé pour signature de l'étape " + signBook.getCurrentWorkflowStepNumber());
        WorkflowStep currentWorkflowStep = getCurrentWorkflowStep(signBook);
        updateStatus(signBook, SignRequestStatus.pending, "Parapheur envoyé pour signature de l'étape " + signBook.getCurrentWorkflowStepNumber(), user, "SUCCESS", signBook.getComment());
        for(SignRequest signRequest : signBook.getSignRequests()) {
            signRequestService.addRecipients(signRequest, currentWorkflowStep.getRecipients());
            signRequestService.pendingSignRequest(signRequest, currentWorkflowStep.getSignType(), user);
        }
        for (Recipient recipient : currentWorkflowStep.getRecipients()) {
            User recipientUser = recipient.getUser();
            if (recipientUser.getEmailAlertFrequency() == null || recipientUser.getEmailAlertFrequency().equals(User.EmailAlertFrequency.immediately) || userService.checkEmailAlert(recipientUser)) {
                if(!recipientUser.equals(user)) {
                    userService.sendEmailAlert(recipientUser);
                }
            }
        }
    }

    public void updateStatus(SignBook signBook, SignRequestStatus signRequestStatus, String action, User user, String returnCode, String comment) {
        Log log = new Log();
        log.setSignRequestId(signBook.getId());
        log.setEppn(user.getEppn());
        log.setIp(user.getIp());
        log.setInitialStatus(signBook.getStatus().toString());
        log.setLogDate(new Date());
        log.setAction(action);
        log.setReturnCode(returnCode);
        log.setComment(comment);
        if(signRequestStatus != null) {
            log.setFinalStatus(signRequestStatus.toString());
            signBook.setStatus(signRequestStatus);
        } else {
            log.setFinalStatus(signBook.getStatus().toString());
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

    public void delete(SignBook signBook) {
        List<WorkflowStep> workflowSteps = new ArrayList<>();
        workflowSteps.addAll(signBook.getWorkflowSteps());
        for(WorkflowStep workflowStep : workflowSteps) {
            signBook.getWorkflowSteps().remove(workflowStep);
            workflowStepRepository.delete(workflowStep);
        }
        signBookRepository.delete(signBook);
    }

    public void refuse(SignBook signBook, String comment, User user) {
        mailService.sendRefusedMail(signBook);
        updateStatus(signBook, SignRequestStatus.refused, "Au moins un document a été refusé", user, "SUCCESS", comment);
        signBookRepository.save(signBook);
        for(SignRequest signRequest : signBook.getSignRequests()) {
            signRequestService.updateStatus(signRequest, SignRequestStatus.refused, "Refusé", user, "SUCCESS", signRequest.getComment());
        }
    }
}
