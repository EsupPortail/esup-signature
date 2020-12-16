package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.*;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignaturePdfException;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.service.event.EventService;
import org.esupportail.esupsignature.service.interfaces.workflow.DefaultWorkflow;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.esupportail.esupsignature.service.utils.mail.MailService;
import org.esupportail.esupsignature.service.utils.sign.SignService;
import org.esupportail.esupsignature.web.controller.ws.json.JsonMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
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
    private UserService userService;

    @Resource
    private MailService mailService;

    @Resource
    private WorkflowService workflowService;

    @Resource
    private WorkflowStepService workflowStepService;

    @Resource
    private FileService fileService;

    @Resource
    private EventService eventService;

    @Resource
    private LiveWorkflowService liveWorkflowService;

    @Resource
    private UserShareService userShareService;

    @Resource
    private DataService dataService;

    @Resource
    private LiveWorkflowStepService liveWorkflowStepService;

    @Resource
    private LogService logService;

    @Resource
    private SignService signService;

    public List<SignBook> getAllSignBooks() {
        List<SignBook> list = new ArrayList<>();
        signBookRepository.findAll().forEach(list::add);
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
            signBook.setLiveWorkflow(liveWorkflowService.create());
            signBookRepository.save(signBook);
            return signBook;
        } else {
            return signBookRepository.findByName(name).get(0);
        }
    }

    @Transactional
    public SignBook addFastSignRequestInNewSignBook(User user, MultipartFile[] multipartFiles, SignType signType, User authUser) throws EsupSignatureException {
        if (signService.checkSignTypeDocType(signType, multipartFiles[0])) {
            try {
                SignBook signBook = addDocsInNewSignBookSeparated("", "Signature simple", multipartFiles, user);
                signBook.getLiveWorkflow().getWorkflowSteps().add(liveWorkflowStepService.createWorkflowStep(false, signType, user.getEmail()));
                signBook.getLiveWorkflow().setCurrentStep(signBook.getLiveWorkflow().getWorkflowSteps().get(0));
                pendingSignBook(signBook, user, authUser);
                return signBook;
            } catch (EsupSignatureUserException | EsupSignatureIOException e) {
                TransactionInterceptor.currentTransactionStatus().setRollbackOnly();
                throw new EsupSignaturePdfException("Impossible de charger le document : documents corrompu", e);
            }
        } else {
            throw new EsupSignatureException("Impossible de demander une signature visuelle sur un document du type " + multipartFiles[0].getContentType());
        }
    }

    public void initSignBook(User user, Long id, SignBook signBook) {
        Workflow workflow = workflowService.getById(id);
        signBook.setName(workflow.getName() + "_" + new Date() + "_" + user.getEppn());
        signBook.setTitle(workflow.getDescription());
        signBook.getLiveWorkflow().setWorkflow(workflow);
    }

    public List<SignBook> getByCreateBy(Long userId) {
        return signBookRepository.findByCreateById(userId);
    }

    public SignBook getByName(String name) {
        return signBookRepository.findByName(name).get(0);
    }

    public SignBook getById(Long id) {
        SignBook signBook = signBookRepository.findById(id).get();
        signBook.setLogs(getLogsFromSignBook(signBook));
        return signBook;
    }

    public List<SignBook> getSharedSignBooks(Long userId) {
        List<SignBook> sharedSignBook = new ArrayList<>();
        for(UserShare userShare : userShareService.getByToUsersInAndShareTypesContains(Collections.singletonList(userId), ShareType.sign)) {
            if(userShare.getWorkflow() != null) {
                sharedSignBook.addAll(signBookRepository.findByWorkflowId(userShare.getWorkflow().getId()));
            } else if(userShare.getForm() != null) {
                List<SignRequest> signRequests = signRequestService.getToSignRequests(userShare.getUser().getId());
                for (SignRequest signRequest : signRequests) {
                    Data data = dataService.getBySignBook(signRequest.getParentSignBook());
                    if(data.getForm().equals(userShare.getForm())) {
                        sharedSignBook.add(signRequest.getParentSignBook());
                        break;
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
        dataService.nullifySignBook(signBook);
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
                newWorkflowStep = liveWorkflowStepService.createWorkflowStep(workflowStep.getAllSignToComplete(), workflowStep.getSignType(), recipientEmails.toArray(String[]::new));
            } catch (EsupSignatureUserException e) {
                logger.error("error on import workflow", e);
            }
            signBook.getLiveWorkflow().getWorkflowSteps().add(newWorkflowStep);
        }
        if(!(workflow instanceof DefaultWorkflow)) {
            signBook.getLiveWorkflow().setWorkflow(workflow);
        }
        signBook.getLiveWorkflow().setTargetType(workflow.getTargetType());
        signBook.getLiveWorkflow().setDocumentsTargetUri(workflow.getDocumentsTargetUri());
    }

    public void saveWorkflow(SignBook signBook, String title, String description, User user) throws EsupSignatureException {
        Workflow workflow = workflowService.createWorkflow(title, description, user);
        for(LiveWorkflowStep liveWorkflowStep : signBook.getLiveWorkflow().getWorkflowSteps()) {
            List<String> recipientsEmails = new ArrayList<>();
            for (Recipient recipient : liveWorkflowStep.getRecipients()) {
                recipientsEmails.add(recipient.getUser().getEmail());
            }
            WorkflowStep toSaveWorkflowStep = workflowStepService.createWorkflowStep("" , liveWorkflowStep.getAllSignToComplete(), liveWorkflowStep.getSignType(), recipientsEmails.toArray(String[]::new));
            workflow.getWorkflowSteps().add(toSaveWorkflowStep);
        }
    }

    public void completeSignBook(SignBook signBook, User authUser) {
        updateStatus(signBook, SignRequestStatus.completed, "Tous les documents sont signés", "SUCCESS", "",authUser, authUser);
        signRequestService.completeSignRequests(signBook.getSignRequests(), authUser);
    }

    public void archivesFiles(SignBook signBook, User authUser) throws EsupSignatureException {
        if(!signBook.getStatus().equals(SignRequestStatus.archived)) {
            signRequestService.archiveSignRequests(signBook.getSignRequests(), authUser);
            signBook.setStatus(SignRequestStatus.archived);
        }
    }

    public void exportFilesToTarget(SignBook signBook, User authUser) throws EsupSignatureException {
        if(!signBook.getStatus().equals(SignRequestStatus.exported) && signBook.getLiveWorkflow().getDocumentsTargetUri() != null && !signBook.getLiveWorkflow().getTargetType().equals(DocumentIOType.none)) {
            signRequestService.sendSignRequestsToTarget(signBook.getSignRequests(), signBook.getName(), signBook.getLiveWorkflow().getTargetType(), signBook.getLiveWorkflow().getDocumentsTargetUri(), authUser);
            signBook.setStatus(SignRequestStatus.exported);
        }
    }

    public void cleanFiles(SignBook signBook, User authUser) {
        int nbDocOnDataBase = 0;
        for(SignRequest signRequest : signBook.getSignRequests()) {
            signRequestService.cleanDocuments(signRequest, authUser);
            nbDocOnDataBase += signRequest.getSignedDocuments().size();
        }
        if(nbDocOnDataBase == 0) {
            signBook.setStatus(SignRequestStatus.cleaned);

        }
    }

    public boolean startLiveWorkflow(SignBook signBook, User user, User authUser) {
        if(signBook.getLiveWorkflow().getWorkflowSteps().size() >  0) {
            signBook.getLiveWorkflow().setCurrentStep(signBook.getLiveWorkflow().getWorkflowSteps().get(0));
            pendingSignBook(signBook, user, authUser);
            return true;
        }else {
            return false;
        }
    }

    public void removeStep(SignBook signBook, int step) {
        LiveWorkflowStep liveWorkflowStep = signBook.getLiveWorkflow().getWorkflowSteps().get(step);
        signBook.getLiveWorkflow().getWorkflowSteps().remove(liveWorkflowStep);
        for(Recipient recipient : liveWorkflowStep.getRecipients()) {
            for(SignRequest signRequest : signBook.getSignRequests()) {
                signRequest.getRecipientHasSigned().remove(recipient);
            }
        }
        liveWorkflowStepService.delete(liveWorkflowStep);
    }

    public boolean isStepAllSignDone(SignBook signBook) {
        LiveWorkflowStep liveWorkflowStep = signBook.getLiveWorkflow().getCurrentStep();
        if (liveWorkflowStep.getAllSignToComplete() && !workflowService.isWorkflowStepFullSigned(liveWorkflowStep)) {
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

    public void initWorkflowAndPendingSignBook(SignBook signBook, List<String> recipientEmails, User user, User authUser) throws EsupSignatureException {
        if(signBook.getStatus().equals(SignRequestStatus.draft)) {
            if (signBook.getLiveWorkflow().getWorkflow() != null) {
                Workflow workflow = workflowService.computeWorkflow(signBook.getLiveWorkflow().getWorkflow(), recipientEmails, user, false);
                importWorkflow(signBook, workflow);
                nextWorkFlowStep(signBook);
            }
            pendingSignBook(signBook, user, authUser);
        }
    }

    public void pendingSignBook(SignBook signBook, User user, User authUser) {
        LiveWorkflowStep liveWorkflowStep = signBook.getLiveWorkflow().getCurrentStep();
        updateStatus(signBook, SignRequestStatus.pending, "Circuit envoyé pour signature de l'étape " + signBook.getLiveWorkflow().getCurrentStepNumber(), "SUCCESS", signBook.getComment(), user, authUser);
        boolean emailSended = false;
        for(SignRequest signRequest : signBook.getSignRequests()) {
            if(liveWorkflowStep != null) {
                signRequestService.pendingSignRequest(signRequest, user);
                if (!emailSended) {
                    signRequestService.sendEmailAlerts(signRequest, user);
                    emailSended = true;
                }
                for (Recipient recipient : signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients()) {
                    eventService.publishEvent(new JsonMessage("info", "Vous avez une nouvelle demande", null), "user", eventService.getClientIdByEppn(recipient.getUser().getEppn()));
                }
                logger.info("Circuit " + signBook.getId() + " envoyé pour signature de l'étape " + signBook.getLiveWorkflow().getCurrentStepNumber());
            } else {
                completeSignBook(signBook, user);
                logger.info("Circuit " + signBook.getId() + " terminé car ne contient pas d'étape");
                break;
            }
        }
    }

    public void updateStatus(SignBook signBook, SignRequestStatus signRequestStatus, String action, String returnCode, String comment, User user, User authUser) {
        Log log = logService.create(signBook.getId(), signBook.getStatus().name(), action, returnCode, comment, user, authUser);
        if(signRequestStatus != null) {
            log.setFinalStatus(signRequestStatus.toString());
            signBook.setStatus(signRequestStatus);
        } else {
            log.setFinalStatus(signBook.getStatus().toString());
        }
    }



    public void refuse(SignBook signBook, String comment, User user, User authUser) {
        mailService.sendRefusedMail(signBook, comment);
        updateStatus(signBook, SignRequestStatus.refused, "Un des documents du a été refusé, ceci annule toute la procédure", "SUCCESS", comment, user, authUser);
        for(SignRequest signRequest : signBook.getSignRequests()) {
            signRequest.setComment(comment);
            signRequestService.updateStatus(signRequest, SignRequestStatus.refused, "Refusé", "SUCCESS", null, null, null, signBook.getLiveWorkflow().getCurrentStepNumber(), user, authUser);
            for(Recipient recipient : signBook.getLiveWorkflow().getCurrentStep().getRecipients()) {
                if(recipient.getUser().equals(user)) {
                    Action action = signRequest.getRecipientHasSigned().get(recipient);
                    action.setActionType(ActionType.refused);
                    action.setDate(new Date());
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


    public void addDocumentsToSignBook(SignBook signBook, String prefix, MultipartFile[] multipartFiles, User authUser) throws EsupSignatureIOException {
        if(!prefix.isEmpty()) {
            prefix += "_";
        }
        for (MultipartFile multipartFile : multipartFiles) {
            SignRequest signRequest = signRequestService.createSignRequest(prefix + fileService.getNameOnly(multipartFile.getOriginalFilename()), authUser, authUser);
            addSignRequest(signBook, signRequest);
            signRequestService.addDocsToSignRequest(signRequest, multipartFile);
        }
    }

    public SignBook addDocsInNewSignBookSeparated(String name, String workflowName, MultipartFile[] multipartFiles, User authUser) throws EsupSignatureIOException {
        SignBook signBook = createSignBook("workflowName", "name", authUser, true);
        addDocumentsToSignBook(signBook, workflowName, multipartFiles, authUser);
        return signBook;
    }

    public void addDocsInNewSignBookGrouped(String name, MultipartFile[] multipartFiles, User authUser) throws EsupSignatureIOException {
        SignBook signBook = createSignBook(name, "", authUser, false);
        SignRequest signRequest = signRequestService.createSignRequest(name, authUser, authUser);
        signRequestService.addDocsToSignRequest(signRequest, multipartFiles);
        addSignRequest(signBook, signRequest);
        logger.info("signRequest : " + signRequest.getId() + " added to signBook" + signBook.getName() + " - " + signBook.getId());
    }

    public void addWorkflowToSignBook(SignBook signBook, User authUser, Long workflowSignBookId) {
        Workflow workflow = workflowService.getById(workflowSignBookId);
        importWorkflow(signBook, workflow);
        nextWorkFlowStep(signBook);
        pendingSignBook(signBook, authUser, authUser);
    }

    public List<Log> getLogsFromSignBook(SignBook signBook) {
        List<Log> logs = new ArrayList<>();
        for (SignRequest signRequest : signBook.getSignRequests()) {
            logs.addAll(logService.getBySignRequestId(signRequest.getId()));
        }
        return logs;
    }

    public List<LiveWorkflowStep> getAllSteps(SignBook signBook) {
        List<LiveWorkflowStep> allSteps = new ArrayList<>(signBook.getLiveWorkflow().getWorkflowSteps());
        if (allSteps.size() > 0) {
            allSteps.remove(0);
        }
        return allSteps;
    }

    public void addLiveStep(SignBook signBook, String[] recipientsEmails, int stepNumber, Boolean allSignToComplete, String signType, User authUser) throws EsupSignatureException {
        int currentSetNumber = signBook.getLiveWorkflow().getCurrentStepNumber();
        if(stepNumber + 1 >= currentSetNumber) {
            try {
                LiveWorkflowStep liveWorkflowStep = liveWorkflowStepService.createWorkflowStep(allSignToComplete, SignType.valueOf(signType), recipientsEmails);
                if (stepNumber == -1) {
                    signBook.getLiveWorkflow().getWorkflowSteps().add(liveWorkflowStep);
                } else {
                    signBook.getLiveWorkflow().getWorkflowSteps().add(stepNumber, liveWorkflowStep);
                }
                signBook.getLiveWorkflow().setCurrentStep(signBook.getLiveWorkflow().getWorkflowSteps().get(currentSetNumber - 1));
                pendingSignBook(signBook, authUser, authUser);
            } catch (EsupSignatureUserException e) {
                logger.error("error on add step", e);
                throw new EsupSignatureException("Erreur lors de l'ajout des participants");
            }
        } else {
            throw new EsupSignatureException("L'étape ne peut pas être ajoutée");
        }
    }

    public List<SignBook> getByLiveWorkflowAndStatus(LiveWorkflow liveWorkflow, SignRequestStatus signRequestStatus) {
        return signBookRepository.findByLiveWorkflowAndStatus(liveWorkflow, signRequestStatus);
    }


}
