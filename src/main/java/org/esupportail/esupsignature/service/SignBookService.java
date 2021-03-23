package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.*;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignaturePdfException;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.service.event.EventService;
import org.esupportail.esupsignature.service.interfaces.workflow.DefaultWorkflow;
import org.esupportail.esupsignature.service.utils.WebUtilsService;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.esupportail.esupsignature.service.utils.mail.MailService;
import org.esupportail.esupsignature.service.utils.sign.SignService;
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

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

    @Resource
    private WebUtilsService webUtilsService;

    @Resource
    private ReportService reportService;

    @Resource
    private TargetService targetService;

    @Resource
    private UserPropertieService userPropertieService;

    @Resource
    private CommentService commentService;

    public List<SignBook> getAllSignBooks() {
        List<SignBook> list = new ArrayList<>();
        signBookRepository.findAll().forEach(list::add);
        return list;
    }

    public List<SignBook> getSignBooksByWorkflow(Workflow workflow) {
        return signBookRepository.findByLiveWorkflowWorkflow(workflow);
    }

    public SignBook createSignBook(String prefix,  String suffix, User user, boolean external) {
        String name = generateName(prefix, suffix, user.getEppn());
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
    public SignBook addFastSignRequestInNewSignBook(MultipartFile[] multipartFiles, SignType signType, User user, String authUserEppn) throws EsupSignatureException {
        if (signService.checkSignTypeDocType(signType, multipartFiles[0])) {
            try {
                SignBook signBook = addDocsInNewSignBookSeparated("", "Signature simple", multipartFiles, user);
                signBook.getLiveWorkflow().getLiveWorkflowSteps().add(liveWorkflowStepService.createLiveWorkflowStep(null,false, false, signType, user.getEmail()));
                signBook.getLiveWorkflow().setCurrentStep(signBook.getLiveWorkflow().getLiveWorkflowSteps().get(0));
                dispatchSignRequestParams(signBook);
                pendingSignBook(signBook, null, user.getEppn(), authUserEppn);
                return signBook;
            } catch (EsupSignatureIOException e) {
                throw new EsupSignaturePdfException("Impossible de charger le document : documents corrompu", e);
            }
        } else {
            throw new EsupSignatureException("Impossible de demander une signature visuelle sur un document du type " + multipartFiles[0].getContentType());
        }
    }

    @Transactional
    public void initSignBook(Long signBookId, Long id, User user) {
        SignBook signBook = getById(signBookId);
        Workflow workflow = workflowService.getById(id);
        signBook.setName(workflow.getName() + "_" + new Date() + "_" + user.getEppn());
        signBook.setTitle(workflow.getDescription());
        signBook.getLiveWorkflow().setWorkflow(workflow);
        for(Target target : workflow.getTargets()) {
            signBook.getLiveWorkflow().getTargets().add(targetService.createTarget(target.getTargetType(), target.getTargetUri()));
        }
    }

    public List<SignBook> getByCreateBy(String userEppn) {
        return signBookRepository.findByCreateByEppn(userEppn);
    }

    public SignBook getByName(String name) {
        return signBookRepository.findByName(name).get(0);
    }

    public SignBook getById(Long id) {
        SignBook signBook = signBookRepository.findById(id).get();
        signBook.setLogs(getLogsFromSignBook(signBook));
        return signBook;
    }

    public List<SignBook> getSharedSignBooks(String userEppn) {
        List<SignBook> sharedSignBook = new ArrayList<>();
        for(UserShare userShare : userShareService.getByToUsersInAndShareTypesContains(Collections.singletonList(userEppn), ShareType.sign)) {
            if(userShare.getWorkflow() != null) {
                sharedSignBook.addAll(signBookRepository.findByWorkflowId(userShare.getWorkflow().getId()));
            } else if(userShare.getForm() != null) {
                List<SignRequest> signRequests = signRequestService.getToSignRequests(userShare.getUser().getEppn());
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

//    public void addSignRequest(SignBook signBook, SignRequest signRequest) {
//        signBook.getSignRequests().add(signRequest);
//        signRequest.setParentSignBook(signBook);
//    }

    @Transactional
    public void delete(Long signBookId) {
        //TODO critères de suppression ou en conf (if deleteDefinitive)
        SignBook signBook = getById(signBookId);
        List<Long> signRequestsIds = signBook.getSignRequests().stream().map(SignRequest::getId).collect(Collectors.toList());
        for(Long signRequestId : signRequestsIds) {
            signRequestService.delete(signRequestId);
        }
        signBook.setStatus(SignRequestStatus.deleted);
    }

    @Transactional
    public void deleteDefinitive(Long signBookId) {
        SignBook signBook = getById(signBookId);
        List<Long> liveWorkflowStepIds = signBook.getLiveWorkflow().getLiveWorkflowSteps().stream().map(LiveWorkflowStep::getId).collect(Collectors.toList());
        for (Long liveWorkflowStepId : liveWorkflowStepIds) {
            liveWorkflowStepService.delete(liveWorkflowStepId);
        }
        List<Long> signRequestsIds = signBook.getSignRequests().stream().map(SignRequest::getId).collect(Collectors.toList());
        for(Long signRequestId : signRequestsIds) {
            signRequestService.deleteDefinitive(signRequestId);
        }
        dataService.nullifySignBook(signBook);
        signBookRepository.delete(signBook);
    }

    public void removeSignRequestFromSignBook(SignBook signBook, SignRequest signRequest) {
        signBook.getSignRequests().remove(signRequest);
    }

    public boolean checkUserManageRights(String userEppn, SignBook signBook) {
        if (signBook.getCreateBy().getEppn().equals(userEppn) || signBook.getCreateBy().getEppn().equals("system")) {
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
            LiveWorkflowStep newWorkflowStep = liveWorkflowStepService.createLiveWorkflowStep(workflowStep, workflowStep.getRepeatable(), workflowStep.getAllSignToComplete(), workflowStep.getSignType(), recipientEmails.toArray(String[]::new));
            signBook.getLiveWorkflow().getLiveWorkflowSteps().add(newWorkflowStep);
        }
        if(!(workflow instanceof DefaultWorkflow)) {
            signBook.getLiveWorkflow().setWorkflow(workflow);
        }
        dispatchSignRequestParams(signBook);
    }

    @Transactional
    public void saveWorkflow(Long signBookId, String title, String description, User user) throws EsupSignatureException {
        SignBook signBook = getById(signBookId);
        Workflow workflow = workflowService.createWorkflow(title, description, user);
        for(LiveWorkflowStep liveWorkflowStep : signBook.getLiveWorkflow().getLiveWorkflowSteps()) {
            List<String> recipientsEmails = new ArrayList<>();
            for (Recipient recipient : liveWorkflowStep.getRecipients()) {
                recipientsEmails.add(recipient.getUser().getEmail());
            }
            WorkflowStep toSaveWorkflowStep = workflowStepService.createWorkflowStep("" , liveWorkflowStep.getAllSignToComplete(), liveWorkflowStep.getSignType(), recipientsEmails.toArray(String[]::new));
            workflow.getWorkflowSteps().add(toSaveWorkflowStep);
        }
    }

    public void completeSignBook(SignBook signBook, String authUser) {
        updateStatus(signBook, SignRequestStatus.completed, "Tous les documents sont signés", "SUCCESS", "", authUser, authUser);
        signRequestService.completeSignRequests(signBook.getSignRequests(), authUser);
    }

    public void archivesFiles(SignBook signBook, String authUserEppn) throws EsupSignatureFsException {
        if(!signBook.getStatus().equals(SignRequestStatus.archived)) {
            signRequestService.archiveSignRequests(signBook.getSignRequests(), authUserEppn);
            signBook.setStatus(SignRequestStatus.archived);
        }
    }

    public void exportFilesToTarget(SignBook signBook, String authUserEppn) throws EsupSignatureException {
        if(signBook.getLiveWorkflow() != null && signBook.getLiveWorkflow().getTargets().size() > 0) {
            signRequestService.sendSignRequestsToTarget(signBook.getSignRequests(), signBook.getName(), signBook.getLiveWorkflow().getTargets(), authUserEppn);
            signBook.setStatus(SignRequestStatus.exported);
        }
    }

    public void cleanFiles(SignBook signBook, String authUserEppn) {
        int nbDocOnDataBase = 0;
        for(SignRequest signRequest : signBook.getSignRequests()) {
            signRequestService.cleanDocuments(signRequest, authUserEppn);
            nbDocOnDataBase += signRequest.getSignedDocuments().size();
        }
        if(nbDocOnDataBase == 0) {
            signBook.setStatus(SignRequestStatus.cleaned);

        }
    }

    @Transactional
    public boolean startLiveWorkflow(SignBook signBook, String userEppn, String authUserEppn, Boolean start) {
        if(signBook.getLiveWorkflow().getLiveWorkflowSteps().size() >  0) {
            signBook.getLiveWorkflow().setCurrentStep(signBook.getLiveWorkflow().getLiveWorkflowSteps().get(0));
            if(start != null && start) {
                dispatchSignRequestParams(signBook);
                pendingSignBook(signBook, null, userEppn, authUserEppn);
            }
            return true;
        }else {
            return false;
        }
    }

    @Transactional
    public boolean removeStep(Long signBookId, int step) {
        SignBook signBook = getById(signBookId);
        int currentStepNumber = signBook.getLiveWorkflow().getCurrentStepNumber();
        if(currentStepNumber <= step) {
            LiveWorkflowStep liveWorkflowStep = signBook.getLiveWorkflow().getLiveWorkflowSteps().get(step);
            signBook.getLiveWorkflow().getLiveWorkflowSteps().remove(liveWorkflowStep);
            for (Recipient recipient : liveWorkflowStep.getRecipients()) {
                for (SignRequest signRequest : signBook.getSignRequests()) {
                    signRequest.getRecipientHasSigned().remove(recipient);
                }
            }
            liveWorkflowStepService.delete(liveWorkflowStep);
            return true;
        }
        return false;
    }

    public boolean isStepAllSignDone(SignBook signBook) {
        LiveWorkflowStep liveWorkflowStep = signBook.getLiveWorkflow().getCurrentStep();
        return (!liveWorkflowStep.getAllSignToComplete() || workflowService.isWorkflowStepFullSigned(liveWorkflowStep)) && !isMoreWorkflowStep(signBook);
    }

    public boolean nextWorkFlowStep(SignBook signBook) {
        boolean isMoreWorkStep = isMoreWorkflowStep(signBook);
        if (isMoreWorkStep) {
            signBook.getLiveWorkflow().setCurrentStep(signBook.getLiveWorkflow().getLiveWorkflowSteps().get(signBook.getLiveWorkflow().getCurrentStepNumber()));
        }
        return isMoreWorkStep;
    }

    public boolean isMoreWorkflowStep(SignBook signBook) {
        return signBook.getLiveWorkflow().getLiveWorkflowSteps().size() >= signBook.getLiveWorkflow().getCurrentStepNumber() + 1;
    }

    public boolean isNextWorkFlowStep(SignBook signBook) {
        return signBook.getLiveWorkflow().getLiveWorkflowSteps().size() >= signBook.getLiveWorkflow().getCurrentStepNumber() + 2;
    }

    public boolean checkUserViewRights(String userEppn, SignBook signBook) {
        List<Recipient> recipients = new ArrayList<>();
        for (LiveWorkflowStep liveWorkflowStep : signBook.getLiveWorkflow().getLiveWorkflowSteps()) {
            recipients.addAll(liveWorkflowStep.getRecipients());
        }
        if(signBook.getCreateBy().getEppn().equals(userEppn) || recipientService.recipientsContainsUser(recipients, userEppn) > 0) {
            return true;
        }
        return false;
    }

    @Transactional
    public void initWorkflowAndPendingSignBook(Long signRequestId, List<String> recipientsEmails, List<String> targetEmails, String userEppn, String authUserEppn) throws EsupSignatureException {
        SignRequest signRequest = signRequestService.getById(signRequestId);
        SignBook signBook = signRequest.getParentSignBook();
        if(signBook.getStatus().equals(SignRequestStatus.draft)) {
            if (signBook.getLiveWorkflow().getWorkflow() != null) {
                Workflow workflow = workflowService.computeWorkflow(signBook.getLiveWorkflow().getWorkflow().getId(), recipientsEmails, userEppn, false);
                importWorkflow(signBook, workflow);
                nextWorkFlowStep(signBook);
                if(targetEmails != null && targetEmails.size() > 0) {
                    StringBuilder targetEmailsToAdd = new StringBuilder();
                    for (String targetEmail : targetEmails) {
                        targetEmailsToAdd.append(targetEmail).append(";");
                    }
                    signBook.getLiveWorkflow().getTargets().add(targetService.createTarget(DocumentIOType.mail, targetEmailsToAdd.toString()));
                }
                if(recipientsEmails != null) {
                    for (String recipientEmail : recipientsEmails) {
                        userPropertieService.createUserPropertieFromMails(userService.getByEppn(authUserEppn), Collections.singletonList(recipientEmail.split("\\*")[1]));
                    }
                }
            }
            pendingSignBook(signBook, null, userEppn, authUserEppn);
        }
    }

    public void pendingSignBook(SignBook signBook, Data data, String userEppn, String authUserEppn) {
        LiveWorkflowStep liveWorkflowStep = signBook.getLiveWorkflow().getCurrentStep();
        updateStatus(signBook, SignRequestStatus.pending, "Circuit envoyé pour signature de l'étape " + signBook.getLiveWorkflow().getCurrentStepNumber(), "SUCCESS", signBook.getComment(), userEppn, authUserEppn);
        boolean emailSended = false;
        for(SignRequest signRequest : signBook.getSignRequests()) {
            if(liveWorkflowStep != null) {
                signRequestService.pendingSignRequest(signRequest, userEppn);
                if (!emailSended) {
                    signRequestService.sendEmailAlerts(signRequest, userEppn, data);
                    emailSended = true;
                }
                for (Recipient recipient : signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients()) {
                    if(!signRequest.getCreateBy().getEppn().equals(userEppn)) {
                        eventService.publishEvent(new JsonMessage("info", "Vous avez une nouvelle demande", null), "user", eventService.getClientIdByEppn(recipient.getUser().getEppn()));
                    }
                }
                logger.info("Circuit " + signBook.getId() + " envoyé pour signature de l'étape " + signBook.getLiveWorkflow().getCurrentStepNumber());
            } else {
                completeSignBook(signBook, userEppn);
                logger.info("Circuit " + signBook.getId() + " terminé car ne contient pas d'étape");
                break;
            }
        }
    }

    @Transactional
    public void nextStepAndPending(Long signBookId, Data data, String userEppn, String authUserEppn) {
        SignBook signBook = getById(signBookId);
        nextWorkFlowStep(signBook);
        pendingSignBook(signBook, data, userEppn, authUserEppn);
    }

    public void updateStatus(SignBook signBook, SignRequestStatus signRequestStatus, String action, String returnCode, String comment, String userEppn, String authUserEppn) {
        Log log = logService.create(signBook.getId(), signBook.getStatus().name(), action, returnCode, comment, userEppn, authUserEppn);
        if(signRequestStatus != null) {
            log.setFinalStatus(signRequestStatus.toString());
            signBook.setStatus(signRequestStatus);
        } else {
            log.setFinalStatus(signBook.getStatus().toString());
        }
    }

    public void refuse(SignBook signBook, String comment, String userEppn, String authUserEppn) {
        mailService.sendRefusedMail(signBook, comment);
        for(SignRequest signRequest : signBook.getSignRequests()) {
            commentService.create(signRequest.getId(), comment, 0, 0, 0, null, true, "#FF7EB9", userEppn);
        }
        updateStatus(signBook, SignRequestStatus.refused, "Cette demande a été refusée, ceci annule toute la procédure", "SUCCESS", comment, userEppn, authUserEppn);
        for(SignRequest signRequest : signBook.getSignRequests()) {
            signRequestService.updateStatus(signRequest, SignRequestStatus.refused, "Refusé", "SUCCESS", null, null, null, signBook.getLiveWorkflow().getCurrentStepNumber(), userEppn, authUserEppn);
            for(Recipient recipient : signBook.getLiveWorkflow().getCurrentStep().getRecipients()) {
                if(recipient.getUser().getEppn().equals(userEppn)) {
                    Action action = signRequest.getRecipientHasSigned().get(recipient);
                    action.setActionType(ActionType.refused);
                    action.setUserIp(webUtilsService.getClientIp());
                    action.setDate(new Date());
                    recipient.setSigned(true);
                }
            }
        }
    }

    public String generateName(String prefix, String suffix, String userEppn) {
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
        signBookName += "_" + userEppn;
        return signBookName;
    }

    public Map<SignBook, String> sendSignBook(SignBook signBook, String[] recipientsEmails, Boolean allSignToComplete, Boolean userSignFirst, Boolean pending, String comment, SignType signType, User user, User authUser) throws EsupSignatureException {
        String message = null;
        if (allSignToComplete == null) {
            allSignToComplete = false;
        }
        if(userSignFirst != null && userSignFirst) {
            signBook.getLiveWorkflow().getLiveWorkflowSteps().add(liveWorkflowStepService.createLiveWorkflowStep(null,false, false, SignType.pdfImageStamp, user.getEmail()));
        }
        signBook.getLiveWorkflow().getLiveWorkflowSteps().add(liveWorkflowStepService.createLiveWorkflowStep(null,false, allSignToComplete, signType, recipientsEmails));
        signBook.getLiveWorkflow().setCurrentStep(signBook.getLiveWorkflow().getLiveWorkflowSteps().get(0));
        dispatchSignRequestParams(signBook);
        if(userService.getTempUsersFromRecipientList(Arrays.asList(recipientsEmails)) . size() > 0) {
            pending = false;
            message = "La liste des destinataires contient des personnes externes.<br>Après vérification, vous devez confirmer l'envoi pour finaliser la demande";
        }
        if (pending != null && pending) {
            pendingSignBook(signBook, null, user.getEppn(), authUser.getEppn());
            if (comment != null && !comment.isEmpty()) {
                for (SignRequest signRequest : signBook.getSignRequests()) {
                    signRequestService.updateStatus(signRequest, signRequest.getStatus(), "comment", comment, "SUCCES", null, null, null, 0, user.getEppn(), authUser.getEppn());
                }
            }
        } else {
            message = "Après vérification, vous devez confirmer l'envoi pour finaliser la demande";
        }
        Map<SignBook, String> signBookStringMap = new HashMap<>();
        signBookStringMap.put(signBook, message);
        if(recipientsEmails != null) {
            userPropertieService.createUserPropertieFromMails(userService.getByEppn(authUser.getEppn()), Arrays.asList(recipientsEmails));
        }
        return signBookStringMap;
    }


    public void addDocumentsToSignBook(SignBook signBook, String prefix, MultipartFile[] multipartFiles, String authUserEppn) throws EsupSignatureIOException {
        if(!prefix.isEmpty()) {
            prefix += "_";
        }
        for (MultipartFile multipartFile : multipartFiles) {
            SignRequest signRequest = signRequestService.createSignRequest(prefix + fileService.getNameOnly(multipartFile.getOriginalFilename()), signBook, authUserEppn, authUserEppn);
            signRequestService.addDocsToSignRequest(signRequest, multipartFile);
        }
    }

    @Transactional
    public SignBook addDocsInNewSignBookSeparated(String name, String workflowName, MultipartFile[] multipartFiles, User authUser) throws EsupSignatureIOException {
        SignBook signBook = createSignBook(workflowName, name, authUser, true);
        addDocumentsToSignBook(signBook, workflowName, multipartFiles, authUser.getEppn());
        return signBook;
    }

    @Transactional
    public SignBook addDocsInNewSignBookGrouped(String name, MultipartFile[] multipartFiles, String authUserEppn) throws EsupSignatureIOException {
        User authUser = userService.getByEppn(authUserEppn);
        SignBook signBook = createSignBook(name, "", authUser, false);
        SignRequest signRequest = signRequestService.createSignRequest(name, signBook, authUserEppn, authUserEppn);
        signRequestService.addDocsToSignRequest(signRequest, multipartFiles);
        logger.info("signRequest : " + signRequest.getId() + " added to signBook" + signBook.getName() + " - " + signBook.getId());
        return signBook;
    }

    @Transactional
    public void addWorkflowToSignBook(SignBook signBook, String authUserEppn, Long workflowSignBookId) {
        Workflow workflow = workflowService.getById(workflowSignBookId);
        importWorkflow(signBook, workflow);
        nextWorkFlowStep(signBook);
        pendingSignBook(signBook, null, authUserEppn, authUserEppn);
    }

    public List<Log> getLogsFromSignBook(SignBook signBook) {
        List<Log> logs = new ArrayList<>();
        for (SignRequest signRequest : signBook.getSignRequests()) {
            logs.addAll(logService.getBySignRequestId(signRequest.getId()));
        }
        return logs;
    }

    public List<LiveWorkflowStep> getAllSteps(SignBook signBook) {
        List<LiveWorkflowStep> allSteps = new ArrayList<>(signBook.getLiveWorkflow().getLiveWorkflowSteps());
        if (allSteps.size() > 0) {
            allSteps.remove(0);
        }
        return allSteps;
    }

    @Transactional
    public void addLiveStep(Long id, String[] recipientsEmails, int stepNumber, Boolean allSignToComplete, SignType signType, boolean repeatable, String authUserEppn) throws EsupSignatureException {
        SignBook signBook = this.getById(id);
        int currentStepNumber = signBook.getLiveWorkflow().getCurrentStepNumber();
        LiveWorkflowStep liveWorkflowStep = liveWorkflowStepService.createLiveWorkflowStep(null, repeatable, allSignToComplete, signType, recipientsEmails);
        if (stepNumber == -1) {
            signBook.getLiveWorkflow().getLiveWorkflowSteps().add(liveWorkflowStep);
        } else {
            if (stepNumber >= currentStepNumber) {
                signBook.getLiveWorkflow().getLiveWorkflowSteps().add(stepNumber, liveWorkflowStep);
            } else {
                throw new EsupSignatureException("L'étape ne peut pas être ajoutée");
            }
        }
        if(recipientsEmails != null) {
            userPropertieService.createUserPropertieFromMails(userService.getByEppn(authUserEppn), Arrays.asList(recipientsEmails));
        }
    }

    public List<SignBook> getByLiveWorkflowAndStatus(LiveWorkflow liveWorkflow, SignRequestStatus signRequestStatus) {
        return signBookRepository.findByLiveWorkflowAndStatus(liveWorkflow, signRequestStatus);
    }

    public Long nbToSignSignBooks(String userEppn) {
        return signBookRepository.countByRecipientUserToSign(userEppn);
    }

    public void dispatchSignRequestParams(SignBook signBook) {
        for(SignRequest signRequest : signBook.getSignRequests()) {
            int i = 0;
            for(LiveWorkflowStep liveWorkflowStep : signBook.getLiveWorkflow().getLiveWorkflowSteps()) {
                if(signRequest.getSignRequestParams().size() >= i + 1) {
                    liveWorkflowStep.getSignRequestParams().add(signRequest.getSignRequestParams().get(i));
                } else {
                    break;
                }
                i++;
            }
        }
    }
}
