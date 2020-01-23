package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.SignBook.SignBookType;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.repository.*;
import org.esupportail.esupsignature.service.mail.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class SignBookService {

    private static final Logger logger = LoggerFactory.getLogger(SignBookService.class);

    @Resource
    private SignBookRepository signBookRepository;

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
        List<SignBook> list = new ArrayList<>();
        signBookRepository.findAll().forEach(e -> list.add(e));
        return list;
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
        signRequest.setParentSignBook(signBook);

    }

    public void deleteSignBook(SignBook signBook) {
        signBookRepository.delete(signBook);
    }

    public void removeSignRequestFromSignBook(SignBook signBook, SignRequest signRequest) {
        signBook.getSignRequests().remove(signRequest);
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
            WorkflowStep newWorkflowStep = workflowService.createWorkflowStep(workflowStep.getRecipients(), "", workflowStep.isAllSignToComplete(), workflowStep.getSignType());
            signBook.getWorkflowSteps().add(newWorkflowStep);
        }
        signBook.setTargetType(workflow.getTargetType());
        signBook.setDocumentsTargetUri(workflow.getDocumentsTargetUri());
    }

    public void saveWorkflow(String name, User user, SignBook signBook) throws EsupSignatureException {
        Workflow workflow = workflowService.createWorkflow(name, user, false);
        for(WorkflowStep workflowStep : signBook.getWorkflowSteps()) {
            WorkflowStep toSaveWorkflowStep = workflowService.createWorkflowStep(workflowStep.getRecipients(), "", workflowStep.isAllSignToComplete(), workflowStep.getSignType());
            workflow.getWorkflowSteps().add(toSaveWorkflowStep);
        }
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

    public boolean isUserSignAllDocs(SignBook signBook, User user) {
        for (SignRequest signRequest : signBook.getSignRequests()) {
            if (recipientService.needSign(signRequest.getRecipients(), user)) {
                return false;
            }
        }
        return true;
    }

    public boolean isStepAllDocsDone(SignBook signBook) {
        for (SignRequest signRequest : signBook.getSignRequests()) {
            if (signRequest.getStatus().equals(SignRequestStatus.pending)) {
                return false;
            }
        }
        return true;
    }

    public boolean isStepAllSignDone(SignBook signBook) {
        WorkflowStep currentWorkflowStep = getCurrentWorkflowStep(signBook);
        if (currentWorkflowStep.isAllSignToComplete() && !workflowStepService.isWorkflowStepFullSigned(currentWorkflowStep)) {
            return false;
        }
        return true;
    }

    public boolean nextWorkFlowStep(SignBook signBook) {
        signBook.setCurrentWorkflowStepNumber(signBook.getCurrentWorkflowStepNumber() + 1);
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
            signRequestService.pendingSignRequest(signRequest, currentWorkflowStep.getSignType(), currentWorkflowStep.isAllSignToComplete(), user);
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
        if(signBook.getCurrentWorkflowStepNumber() > 0 && signBook.getWorkflowSteps().size() >= signBook.getCurrentWorkflowStepNumber()) {
            return signBook.getWorkflowSteps().get(signBook.getCurrentWorkflowStepNumber() - 1);
        } else {
            return null;
        }
    }

    public void delete(SignBook signBook) {
        List<WorkflowStep> workflowSteps = new ArrayList<>();
        workflowSteps.addAll(signBook.getWorkflowSteps());
        for(WorkflowStep workflowStep : workflowSteps) {
            signBook.getWorkflowSteps().remove(workflowStep);
            for(Recipient recipient : workflowStep.getRecipients()) {
                recipientRepository.delete(recipient);
            }
            workflowStepRepository.delete(workflowStep);
        }
        List<SignRequest>  signRequests = new ArrayList<>();
        signRequests.addAll(signBook.getSignRequests());
        for(SignRequest signRequest : signRequests) {
            signRequestService.delete(signRequest);
        }
        signBook.getSignRequests().clear();
        signBookRepository.delete(signBook);
    }

    public void refuse(SignBook signBook, String comment, User user) {
        mailService.sendRefusedMail(signBook);
        updateStatus(signBook, SignRequestStatus.refused, "Au moins un document a été refusé", user, "SUCCESS", comment);
        for(SignRequest signRequest : signBook.getSignRequests()) {
            signRequestService.updateStatus(signRequest, SignRequestStatus.refused, "Refusé", user, "SUCCESS", signRequest.getComment());
        }
    }
}
