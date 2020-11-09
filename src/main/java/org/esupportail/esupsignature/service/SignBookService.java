package org.esupportail.esupsignature.service;

import ch.rasc.sse.eventbus.SseEvent;
import ch.rasc.sse.eventbus.SseEventBus;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.*;
import org.esupportail.esupsignature.service.event.EventService;
import org.esupportail.esupsignature.service.mail.MailService;
import org.esupportail.esupsignature.service.workflow.DefaultWorkflow;
import org.esupportail.esupsignature.web.controller.ws.json.JsonMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
    private DataRepository dataRepository;

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

    @Resource
    private RecipientRepository recipientRepository;

    @Resource
    private UserShareRepository userShareRepository;

    @Resource
    private EventService eventService;

    @Resource
    private LiveWorkflowService liveWorkflowService;

    @Resource
    private LiveWorkflowStepRepository liveWorkflowStepRepository;

    public List<SignBook> getAllSignBooks() {
        List<SignBook> list = new ArrayList<>();
        signBookRepository.findAll().forEach(e -> list.add(e));
        return list;
    }

    public List<SignBook> getSignBooksByWorkflowName(String workFlowName) {
        return signBookRepository.findByWorkflowNameAndStatus(workFlowName, SignRequestStatus.completed);
    }

    public SignBook createSignBook(String prefix,  String suffix, User user, boolean external) {
        String name = generateName(prefix, suffix, user);
        if (signBookRepository.countByName(name) == 0) {
            SignBook signBook = new SignBook();
            signBook.setStatus(SignRequestStatus.draft);
            signBook.setName(name);
            signBook.setTitle(prefix);
            signBook.setCreateBy(user);
            signBook.setCreateDate(new Date());
            signBook.setExternal(external);
            signBookRepository.save(signBook);
            return signBook;
        } else {
            return signBookRepository.findByName(name).get(0);
        }
    }

    public SignBook getSignBook(String name) throws EsupSignatureException {
        if (signBookRepository.countByName(name) > 0) {
            return signBookRepository.findByName(name).get(0);
        }
        return null;
    }

    public List<SignBook> getSharedSignBooks(User user) {
        List<SignBook> sharedSignBook = new ArrayList<>();
        for(UserShare userShare : userShareRepository.findByToUsersInAndShareTypesContains(Arrays.asList(user), ShareType.sign)) {
            if(userShare.getWorkflow() != null) {
                sharedSignBook.addAll(signBookRepository.findByWorkflowId(userShare.getWorkflow().getId()));
            } else if(userShare.getForm() != null) {
                List<SignRequest> signRequests = signRequestService.getToSignRequests(userShare.getUser());
                for (SignRequest signRequest : signRequests) {
                    List<Data> datas = dataRepository.findBySignBook(signRequest.getParentSignBook());
                    for (Data data : datas) {
                        if(data.getForm().equals(userShare.getForm())) {
                            sharedSignBook.add(signRequest.getParentSignBook());
                            break;
                        }
                    }
                }
            }
        }
        return sharedSignBook;
    }

    public void addSignRequest(SignBook signBook, SignRequest signRequest) {
        signBook.getSignRequests().add(signRequest);
        signRequest.setParentSignBook(signBook);
    }

    public boolean delete(SignBook signBook) {
        //TODO critères de suppresion ou en conf
//        if(signBook.getCurrentWorkflowStepNumber() > 0) {
//            return false;
//        }
        List<Data> datas = dataRepository.findBySignBook(signBook);
        for (Data data : datas) {
            data.setSignBook(null);
            dataRepository.save(data);
        }
        signBookRepository.delete(signBook);
        return true;
    }

    public void removeSignRequestFromSignBook(SignBook signBook, SignRequest signRequest) {
        signBook.getSignRequests().remove(signRequest);
    }

    public boolean checkUserManageRights(User user, SignBook signBook) {
        if (signBook.getCreateBy().equals(user) || signBook.getCreateBy().getEppn().equals("system")) {
            return true;
        } else {
            return false;
        }
    }

    public boolean preAuthorizeView(Long id, User user) {
        SignBook signBook = signBookRepository.findById(id).get();
        return checkUserViewRights(user, signBook);
    }

    public boolean preAuthorizeManage(Long id, User user) {
        SignBook signBook = signBookRepository.findById(id).get();
        return checkUserManageRights(user, signBook);
    }

    public boolean preAuthorizeManage(String name, User user) throws EsupSignatureException {
        SignBook signBook = getSignBook(name);
        return checkUserManageRights(user, signBook);
    }

    public void importWorkflow(SignBook signBook, Workflow workflow){
        logger.info("import workflow steps in signBook " + signBook.getName() + " - " +signBook.getId());
        for (WorkflowStep workflowStep : workflow.getWorkflowSteps()) {
            List<String> recipientEmails = new ArrayList<>();
            for (User user : workflowStep.getUsers()) {
                if (user.equals(userService.getCreatorUser())) {
                    recipientEmails.add(signBook.getCreateBy().getEmail());
                } else {
                    recipientEmails.add(user.getEmail());
                }
            }
            LiveWorkflowStep newWorkflowStep = null;
            try {
                newWorkflowStep = liveWorkflowService.createWorkflowStep("", "signBook", signBook.getId(), workflowStep.getAllSignToComplete(), workflowStep.getSignType(), recipientEmails.toArray(String[]::new));
            } catch (EsupSignatureUserException e) {
                logger.error("error on import workflow", e);
            }
            signBook.getLiveWorkflow().getWorkflowSteps().add(newWorkflowStep);
        }
        if(!(workflow instanceof DefaultWorkflow)) {
            signBook.getLiveWorkflow().setWorkflow(workflow);
        }
        signBook.getLiveWorkflow().setName("Workflow_" + signBook.getName());
        signBook.getLiveWorkflow().setTargetType(workflow.getTargetType());
        signBook.getLiveWorkflow().setDocumentsTargetUri(workflow.getDocumentsTargetUri());
    }

    public void saveWorkflow(String title, String description, User user, SignBook signBook) throws EsupSignatureException {
        Workflow workflow = workflowService.createWorkflow(title, description, user, false);
        for(LiveWorkflowStep liveWorkflowStep : signBook.getLiveWorkflow().getWorkflowSteps()) {
            List<String> recipientsEmails = new ArrayList<>();
            for (Recipient recipient : liveWorkflowStep.getRecipients()) {
                recipientsEmails.add(recipient.getUser().getEmail());
            }
            WorkflowStep toSaveWorkflowStep = null;
            try {
                toSaveWorkflowStep = workflowService.createWorkflowStep("", "signBook", signBook.getId(), liveWorkflowStep.getAllSignToComplete(), liveWorkflowStep.getSignType(), recipientsEmails.toArray(String[]::new));
            } catch (EsupSignatureUserException e) {
                logger.error("error on save workflow", e);
            }
            workflow.getWorkflowSteps().add(toSaveWorkflowStep);
        }
    }

    public void completeSignBook(SignBook signBook) {
        updateStatus(signBook, SignRequestStatus.completed, "Tous les documents sont signés", "SUCCESS", "");
        signRequestService.completeSignRequests(signBook.getSignRequests());
    }

    public void archivesFiles(SignBook signBook) throws EsupSignatureException {
        if(!signBook.getStatus().equals(SignRequestStatus.archived)) {
            signRequestService.archiveSignRequests(signBook.getSignRequests());
            signBook.setStatus(SignRequestStatus.archived);
        }
    }

    public void exportFilesToTarget(SignBook signBook) throws EsupSignatureException {
        if(!signBook.getStatus().equals(SignRequestStatus.exported) && signBook.getLiveWorkflow().getDocumentsTargetUri() != null && !signBook.getLiveWorkflow().getTargetType().equals(DocumentIOType.none)) {
            signRequestService.sendSignRequestsToTarget(signBook.getName(), signBook.getSignRequests(), signBook.getLiveWorkflow().getTargetType(), signBook.getLiveWorkflow().getDocumentsTargetUri());
            signBook.setStatus(SignRequestStatus.exported);
        }
    }

    public void cleanFiles(SignBook signBook) {
        int nbDocOnDataBase = 0;
        for(SignRequest signRequest : signBook.getSignRequests()) {
            signRequestService.cleanDocuments(signRequest);
            nbDocOnDataBase += signRequest.getSignedDocuments().size();
        }
        if(nbDocOnDataBase == 0) {
            signBook.setStatus(SignRequestStatus.cleaned);

        }
    }

    public void removeStep(SignBook signBook, int step) {
        LiveWorkflowStep liveWorkflowStep = signBook.getLiveWorkflow().getWorkflowSteps().get(step);
        signBook.getLiveWorkflow().getWorkflowSteps().remove(step);
        liveWorkflowStepRepository.delete(liveWorkflowStep);
    }

    public void removeStepRecipient(SignBook signBook, int step, Long recipientId) {
        Recipient recipientToRemove = recipientRepository.findById(recipientId).get();
        LiveWorkflowStep liveWorkflowStep = signBook.getLiveWorkflow().getWorkflowSteps().get(step);
        liveWorkflowStep.getRecipients().remove(recipientToRemove);
        for(SignRequest signRequest : signBook.getSignRequests()) {
            if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber() == step + 1) {
                signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients().removeIf(recipient -> recipient.getUser().equals(recipientToRemove.getUser()));
            }
        }
    }

    public void toggleNeedAllSign(SignBook signBook, int step, Boolean allSignToComplete) {
        LiveWorkflowStep liveWorkflowStep = signBook.getLiveWorkflow().getWorkflowSteps().get(step);
        if(allSignToComplete != null && !allSignToComplete.equals(liveWorkflowStep.getAllSignToComplete())) {
            liveWorkflowService.toggleAllSignToCompleteForWorkflowStep(liveWorkflowStep);
        }
    }

    public void changeSignType(SignBook signBook, int step, SignType signType) {
        LiveWorkflowStep liveWorkflowStep = signBook.getLiveWorkflow().getWorkflowSteps().get(step);
        liveWorkflowService.setSignTypeForWorkflowStep(signType, liveWorkflowStep);
    }

    public boolean isUserSignAllDocs(SignBook signBook, User user) {
        for (SignRequest signRequest : signBook.getSignRequests()) {
            if (recipientService.needSign(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients(), user)) {
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
        LiveWorkflowStep liveWorkflowStep = signBook.getLiveWorkflow().getCurrentStep();
        if (liveWorkflowStep.getAllSignToComplete() && !workflowStepService.isWorkflowStepFullSigned(liveWorkflowStep)) {
            return false;
        }
        return true;
    }

    public boolean nextWorkFlowStep(SignBook signBook) {
        boolean isMoreWorkStep = isMoreWorkflowStep(signBook);
        if (isMoreWorkStep) {
            signBook.getLiveWorkflow().setCurrentStep(signBook.getLiveWorkflow().getWorkflowSteps().get(signBook.getLiveWorkflow().getCurrentStepNumber()));
        }
        return isMoreWorkStep;
    }

    public boolean isMoreWorkflowStep(SignBook signBook) {
        return signBook.getLiveWorkflow().getWorkflowSteps().size() >= signBook.getLiveWorkflow().getCurrentStepNumber() + 1;
    }

    public boolean isNextWorkFlowStep(SignBook signBook) {
        return signBook.getLiveWorkflow().getWorkflowSteps().size() >= signBook.getLiveWorkflow().getCurrentStepNumber() + 2;
    }

    public boolean checkUserViewRights(User user, SignBook signBook) {
        List<Recipient> recipients = new ArrayList<>();
        for (LiveWorkflowStep liveWorkflowStep : signBook.getLiveWorkflow().getWorkflowSteps()) {
            recipients.addAll(liveWorkflowStep.getRecipients());
        }
        if(signBook.getCreateBy().equals(user) || recipientService.recipientsContainsUser(recipients, user) > 0) {
            return true;
        }
        return false;
    }

    public void pendingSignBook(SignBook signBook, User user) {
        LiveWorkflowStep liveWorkflowStep = signBook.getLiveWorkflow().getCurrentStep();
        updateStatus(signBook, SignRequestStatus.pending, "Circuit envoyé pour signature de l'étape " + signBook.getLiveWorkflow().getCurrentStepNumber(), "SUCCESS", signBook.getComment());
        boolean emailSended = false;
        for(SignRequest signRequest : signBook.getSignRequests()) {
            if(liveWorkflowStep != null) {
                signRequestService.pendingSignRequest(signRequest, liveWorkflowStep.getSignType(), liveWorkflowStep.getAllSignToComplete());
                if (!emailSended) {
                    signRequestService.sendEmailAlerts(signRequest, user);
                    emailSended = true;
                }
                for (Recipient recipient : signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients()) {
                    eventService.publishEvent(new JsonMessage("info", "Vous avez une nouvelle demande", null), "user", recipient.getUser());
                }
                logger.info("Circuit " + signBook.getId() + " envoyé pour signature de l'étape " + signBook.getLiveWorkflow().getCurrentStepNumber());
            } else {
                completeSignBook(signBook);
                logger.info("Circuit " + signBook.getId() + " terminé car ne contient pas d'étape");
                break;
            }
        }
    }

    public void updateStatus(SignBook signBook, SignRequestStatus signRequestStatus, String action, String returnCode, String comment) {
        Log log = new Log();
        log.setSignRequestId(signBook.getId());
        if(userService.getUserFromAuthentication() != null) {
            log.setEppn(userService.getUserFromAuthentication().getEppn());
            log.setEppnFor(userService.getSuEppn());
            log.setIp(userService.getUserFromAuthentication().getIp());
        }
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

    public void refuse(SignBook signBook, String comment, User user) {
        mailService.sendRefusedMail(signBook, comment);
        updateStatus(signBook, SignRequestStatus.refused, "Un des documents du a été refusé, ceci annule toute la procédure", "SUCCESS", comment);
        for(SignRequest signRequest : signBook.getSignRequests()) {
            signRequest.setComment(comment);
            signRequestService.updateStatus(signRequest, SignRequestStatus.refused, "Refusé", "SUCCESS", null, null, null);
            for(Recipient recipient : signBook.getLiveWorkflow().getCurrentStep().getRecipients()) {
                if(recipient.getUser().equals(user)) {
                    recipient.setSigned(true);
                }
            }
        }
    }

    public String generateName(String prefix, String suffix, User user) {
        String signBookName = "";

        if(!prefix.isEmpty()) {
            signBookName += prefix.replaceAll("[\\\\/:*?\"<>|]", "-").replace(" ", "-");
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        signBookName += "_" + format.format(new Date());

        if(!suffix.isEmpty()) {
            signBookName += "_";
            signBookName += suffix.replaceAll("[\\\\/:*?\"<>|]", "-");
        }
        signBookName += "_" + user.getEppn();
        return signBookName;
    }
}
