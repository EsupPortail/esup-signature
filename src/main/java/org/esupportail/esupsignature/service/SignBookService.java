package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.*;
import org.esupportail.esupsignature.exception.*;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.service.interfaces.workflow.DefaultWorkflow;
import org.esupportail.esupsignature.service.mail.MailService;
import org.esupportail.esupsignature.service.security.otp.OtpService;
import org.esupportail.esupsignature.service.utils.WebUtilsService;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.esupportail.esupsignature.service.utils.sign.SignService;
import org.esupportail.esupsignature.web.ws.json.JsonExternalUserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.text.DateFormat;
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
    private GlobalProperties globalProperties;

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
    private TargetService targetService;

    @Resource
    private UserPropertieService userPropertieService;

    @Resource
    private CommentService commentService;

    @Resource
    private OtpService otpService;

    @Resource
    private FileService fileService;

    public List<SignBook> getAllSignBooks() {
        List<SignBook> list = new ArrayList<>();
        signBookRepository.findAll().forEach(list::add);
        return list;
    }

    @Transactional
    public List<SignBook> getSignBooksByWorkflow(Long workflowId) {
        Workflow workflow = workflowService.getById(workflowId);
        return signBookRepository.findByLiveWorkflowWorkflow(workflow);
    }

    @Transactional
    public int countSignBooksByWorkflow(Long workflowId) {
        Workflow workflow = workflowService.getById(workflowId);
        return signBookRepository.countByLiveWorkflowWorkflow(workflow);
    }

    public SignBook createSignBook(String title, Workflow workflow, String prefix, String namingTemplate, User user, boolean external) {
        SignBook signBook = new SignBook();
        if(namingTemplate == null || namingTemplate.isEmpty()) {
            namingTemplate = globalProperties.getNamingTemplate();
            if(workflow != null && workflow.getNamingTemplate() != null && !workflow.getNamingTemplate().isEmpty()) {
                namingTemplate = workflow.getNamingTemplate();
            }
        }
        String workflowName = prefix;
        int order = 0;
        if(workflow != null) {
            workflowName = workflow.getName();
            order = signBookRepository.countByLiveWorkflowWorkflow(workflow);
        }
        signBook.setStatus(SignRequestStatus.draft);
        signBook.setTitle(workflowName);
        signBook.setCreateBy(user);
        signBook.setCreateDate(new Date());
        signBook.setExternal(external);
        signBook.setLiveWorkflow(liveWorkflowService.create());
        signBookRepository.save(signBook);
        String name = generateName2(signBook.getId(), title, workflowName, order, user, namingTemplate);
        signBook.setName(name);
        return signBook;
    }

    @Transactional
    public SignBook addFastSignRequestInNewSignBook(MultipartFile[] multipartFiles, SignType signType, User user, String authUserEppn) throws EsupSignatureException {
        if (signService.checkSignTypeDocType(signType, multipartFiles[0])) {
            try {
                SignBook signBook = addDocsInNewSignBookSeparated(fileService.getNameOnly(multipartFiles[0].getOriginalFilename()), "Auto signature", multipartFiles, user);
                signBook.getLiveWorkflow().getLiveWorkflowSteps().add(liveWorkflowStepService.createLiveWorkflowStep(null,false, true, false, signType, Collections.singletonList(user.getEmail()), null));
                signBook.getLiveWorkflow().setCurrentStep(signBook.getLiveWorkflow().getLiveWorkflowSteps().get(0));
                dispatchSignRequestParams(signBook);
                pendingSignBook(signBook, null, user.getEppn(), authUserEppn, false);
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
    public void delete(Long signBookId, String userEppn) {
        SignBook signBook = getById(signBookId);
        List<Long> signRequestsIds = signBook.getSignRequests().stream().map(SignRequest::getId).collect(Collectors.toList());
        for(Long signRequestId : signRequestsIds) {
            signRequestService.delete(signRequestId, userEppn);
        }
        for(LiveWorkflowStep liveWorkflowStep : signBook.getLiveWorkflow().getLiveWorkflowSteps()) {
            if(liveWorkflowStep.getSignRequestParams() != null) {
                liveWorkflowStep.getSignRequestParams().clear();
            }
        }
        signBook.setStatus(SignRequestStatus.deleted);
        signBook.setUpdateDate(new Date());
        signBook.setUpdateBy(userEppn);
        logger.info("delete signbook : " + signBookId);
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
        logger.info("definitive delete signbook : " + signBookId);
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

    public void importWorkflow(SignBook signBook, Workflow workflow, List<JsonExternalUserInfo> externalUsersInfos) {
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
            LiveWorkflowStep newWorkflowStep = liveWorkflowStepService.createLiveWorkflowStep(workflowStep, workflowStep.getRepeatable(), workflowStep.getMultiSign(), workflowStep.getAllSignToComplete(), workflowStep.getSignType(), recipientEmails, externalUsersInfos);
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

    public void completeSignBook(Long signBookId, String userEppn) throws EsupSignatureException {
        SignBook signBook = getById(signBookId);
        if (!signBook.getCreateBy().equals(userService.getSchedulerUser())) {
            try {
                mailService.sendCompletedMail(signBook, userEppn);
                mailService.sendCompletedCCMail(signBook);
            } catch (EsupSignatureMailException e) {
                throw new EsupSignatureException(e.getMessage());
            }
        }
        updateStatus(signBook, SignRequestStatus.completed, "Tous les documents sont signés", "SUCCESS", "", userEppn, userEppn);
        signRequestService.completeSignRequests(signBook.getSignRequests(), userEppn);
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
    public boolean startLiveWorkflow(SignBook signBook, String userEppn, String authUserEppn, Boolean start) throws EsupSignatureException {
        if(signBook.getLiveWorkflow().getLiveWorkflowSteps().size() >  0) {
            signBook.getLiveWorkflow().setCurrentStep(signBook.getLiveWorkflow().getLiveWorkflowSteps().get(0));
            if(start != null && start) {
                dispatchSignRequestParams(signBook);
                pendingSignBook(signBook, null, userEppn, authUserEppn, false);
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
        return isMoreWorkStep && signBook.getLiveWorkflow().getCurrentStepNumber() > -1;
    }

    public boolean isMoreWorkflowStep(SignBook signBook) {
        int test = signBook.getLiveWorkflow().getCurrentStepNumber();
        return signBook.getLiveWorkflow().getLiveWorkflowSteps().size() >= signBook.getLiveWorkflow().getCurrentStepNumber() + 1 && test > -1;
    }

    public boolean isNextWorkFlowStep(SignBook signBook) {
        return signBook.getLiveWorkflow().getLiveWorkflowSteps().size() >= signBook.getLiveWorkflow().getCurrentStepNumber() + 2;
    }

    public boolean checkUserViewRights(String userEppn, SignBook signBook) {
        List<Recipient> recipients = new ArrayList<>();
        for (LiveWorkflowStep liveWorkflowStep : signBook.getLiveWorkflow().getLiveWorkflowSteps()) {
            recipients.addAll(liveWorkflowStep.getRecipients());
        }
        if(signBook.getViewers().stream().anyMatch(user -> user.getEppn().equals(userEppn)) || signBook.getCreateBy().getEppn().equals(userEppn) || recipientService.recipientsContainsUser(recipients, userEppn) > 0) {
            return true;
        }
        return false;
    }

    @Transactional
    public void initWorkflowAndPendingSignBook(Long signRequestId, List<String> recipientsEmails, List<String> allSignToCompletes, List<JsonExternalUserInfo> externalUsersInfos, List<String> targetEmails, String userEppn, String authUserEppn) throws EsupSignatureException {
        SignRequest signRequest = signRequestService.getById(signRequestId);
        SignBook signBook = signRequest.getParentSignBook();
        if(signBook.getStatus().equals(SignRequestStatus.draft)) {
            if (signBook.getLiveWorkflow().getWorkflow() != null) {
                List<Target> targets = new ArrayList<>(workflowService.getById(signBook.getLiveWorkflow().getWorkflow().getId()).getTargets());
                Workflow workflow = workflowService.computeWorkflow(signBook.getLiveWorkflow().getWorkflow().getId(), recipientsEmails, allSignToCompletes, userEppn, false);
                importWorkflow(signBook, workflow, externalUsersInfos);
                nextWorkFlowStep(signBook);
                targetService.copyTargets(targets, signBook, targetEmails);
                if(recipientsEmails != null) {
                    for (String recipientEmail : recipientsEmails) {
                        userPropertieService.createUserPropertieFromMails(userService.getByEppn(authUserEppn), Collections.singletonList(recipientEmail.split("\\*")[1]));
                    }
                }
            }
            pendingSignBook(signBook, null, userEppn, authUserEppn, false);
        }
    }


    public void pendingSignBook(SignBook signBook, Data data, String userEppn, String authUserEppn, boolean forceSendEmail) throws EsupSignatureException {
        LiveWorkflowStep liveWorkflowStep = signBook.getLiveWorkflow().getCurrentStep();
        updateStatus(signBook, SignRequestStatus.pending, "Circuit envoyé pour signature de l'étape " + signBook.getLiveWorkflow().getCurrentStepNumber(), "SUCCESS", signBook.getComment(), userEppn, authUserEppn);
        boolean emailSended = false;
        for(SignRequest signRequest : signBook.getSignRequests()) {
            if(liveWorkflowStep != null) {
                signRequestService.pendingSignRequest(signRequest, userEppn);
                if (!emailSended) {
                    try {
                        signRequestService.sendEmailAlerts(signRequest, userEppn, data, forceSendEmail);
                        emailSended = true;
                    } catch (EsupSignatureMailException e) {
                        throw new EsupSignatureException(e.getMessage());
                    }
                }
                for (Recipient recipient : signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients()) {
//                    if(!signRequest.getCreateBy().getEppn().equals(userEppn)) {
//                        eventService.publishEvent(new JsonMessage("info", "Vous avez une nouvelle demande", null), "user", eventService.getClientIdByEppn(recipient.getUser().getEppn()));
//                    }
                    if(recipient.getUser().getUserType().equals(UserType.external)) {
                        try {
                            otpService.generateOtpForSignRequest(signRequest.getId(), recipient.getUser());
                        } catch (EsupSignatureMailException e) {
                            throw new EsupSignatureException(e.getMessage());
                        }
                    }
                }
                logger.info("Circuit " + signBook.getId() + " envoyé pour signature de l'étape " + signBook.getLiveWorkflow().getCurrentStepNumber());
            } else {
                completeSignBook(signBook.getId(), userEppn);
                logger.info("Circuit " + signBook.getId() + " terminé car ne contient pas d'étape");
                break;
            }
        }
    }

    @Transactional
    public void nextStepAndPending(Long signBookId, Data data, String userEppn, String authUserEppn) throws EsupSignatureException {
        SignBook signBook = getById(signBookId);
        nextWorkFlowStep(signBook);
        pendingSignBook(signBook, data, userEppn, authUserEppn, true);
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

    public void refuse(SignBook signBook, String comment, String userEppn, String authUserEppn) throws EsupSignatureMailException {
        mailService.sendRefusedMail(signBook, comment, userEppn);
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

    public String generateName2(long id, String title, String worflowName, int order, User user, String template) {
        if(template.contains("[id]")) {
            template = template.replace("[id]", id + "");
        }
        if(template.contains("[title]")) {
            template = template.replace("[title]", title.replaceAll("[\\\\/:*?\"<>|]", "-").replace("\t", ""));
        }
        if(template.contains("[worflowName]")) {
            template = template.replace("[worflowName]", worflowName.replaceAll("[\\\\/:*?\"<>|]", "-").replace("\t", ""));
        }
        if(template.contains("[user.eppn]")) {
            template = template.replace("[user.eppn]", user.getEppn());
        }
        if(template.contains("[user.name]")) {
            template = template.replace("[user.name]", user.getFirstname() + "-" + user.getName());
        }
        if(template.contains("[user.initials]")) {
            template = template.replace("[user.initials]", user.getName().substring(0,1).toUpperCase() + user.getFirstname().substring(0,1).toUpperCase());
        }
        if(template.contains("[UUID]")) {
            template = template.replace("[UUID]", UUID.randomUUID().toString());
        }
        if(template.contains("[order]")) {
            template = template.replace("[order]", order + "");
        }
        if(template.contains("[timestamp]")) {
            Date date = Calendar.getInstance().getTime();
            template = template.replace("[timestamp]", date.getTime() + "");
        }
        if(template.contains("[date-fr]")) {
            Date date = Calendar.getInstance().getTime();
            DateFormat dateFormat = new SimpleDateFormat("ddMMyyyyhhmm");
            String strDate = dateFormat.format(date);
            template = template.replace("[date-fr]", strDate);
        }
        if(template.contains("[date-en]")) {
            Date date = Calendar.getInstance().getTime();
            DateFormat dateFormat = new SimpleDateFormat("yyyyMMddhhmm");
            String strDate = dateFormat.format(date);
            template = template.replace("[date-en]", strDate);
        }
        return template;
    }

//    public String generateName(String prefix, String suffix) {
//        String signBookName = "";
//
//        if(!prefix.isEmpty()) {
//            signBookName += prefix.replaceAll("[\\\\/:*?\"<>|]", "-").replace(" ", "-");
//        }
//        if(!suffix.isEmpty()) {
//            if(!prefix.isEmpty()) {
//                signBookName += "-";
//            }
//            signBookName += suffix.replaceAll("[\\\\/:*?\"<>|]", "-");
//        }
//        return signBookName;
//    }

    public Map<SignBook, String> sendSignBook(SignBook signBook, SignType signType, Boolean allSignToComplete, Boolean userSignFirst, Boolean pending, String comment, List<String> recipientsEmails, List<JsonExternalUserInfo> externalUsersInfos, User user, User authUser, boolean forceSendEmail) throws EsupSignatureException {
        String message = null;
        if (allSignToComplete == null) {
            allSignToComplete = false;
        }
        if(userSignFirst != null && userSignFirst) {
            signBook.getLiveWorkflow().getLiveWorkflowSteps().add(liveWorkflowStepService.createLiveWorkflowStep(null,false, true,false, SignType.pdfImageStamp, Collections.singletonList(user.getEmail()), null));
        }
        signBook.getLiveWorkflow().getLiveWorkflowSteps().add(liveWorkflowStepService.createLiveWorkflowStep(null,false, true, allSignToComplete, signType, recipientsEmails, externalUsersInfos));
        signBook.getLiveWorkflow().setCurrentStep(signBook.getLiveWorkflow().getLiveWorkflowSteps().get(0));
        dispatchSignRequestParams(signBook);
        if (pending != null && pending) {
            pendingSignBook(signBook, null, user.getEppn(), authUser.getEppn(), forceSendEmail);
        } else {
            message = "Après vérification/annotation, vous devez cliquer sur 'Démarrer le circuit' pour transmettre la demande aux participants";
        }
        if (comment != null && !comment.isEmpty()) {
            for (SignRequest signRequest : signBook.getSignRequests()) {
                commentService.create(signRequest.getId(), comment, null, null, null, null, true, null, user.getEppn());
                signRequestService.updateStatus(signRequest, signRequest.getStatus(), "comment", comment, "SUCCES", null, null, null, 0, user.getEppn(), authUser.getEppn());
            }
        }
        Map<SignBook, String> signBookStringMap = new HashMap<>();
        signBookStringMap.put(signBook, message);
        if(recipientsEmails != null) {
            userPropertieService.createUserPropertieFromMails(userService.getByEppn(authUser.getEppn()), recipientsEmails);
        }
        return signBookStringMap;
    }


    public void addDocumentsToSignBook(SignBook signBook, MultipartFile[] multipartFiles, String authUserEppn) throws EsupSignatureIOException {
        int i = 0;
        for (MultipartFile multipartFile : multipartFiles) {
            SignRequest signRequest = signRequestService.createSignRequest(fileService.getNameOnly(multipartFile.getOriginalFilename()), signBook, authUserEppn, authUserEppn);
            signRequestService.addDocsToSignRequest(signRequest, true, i, multipartFile);
            i++;
        }
    }

    @Transactional
    public SignBook addDocsInNewSignBookSeparated(String name, String prefix, MultipartFile[] multipartFiles, User authUser) throws EsupSignatureIOException {
        SignBook signBook = createSignBook(name, null, prefix, "", authUser, true);
        addDocumentsToSignBook(signBook, multipartFiles, authUser.getEppn());
        return signBook;
    }

    @Transactional
    public SignBook addDocsInNewSignBookGrouped(String name, MultipartFile[] multipartFiles, String authUserEppn) throws EsupSignatureIOException {
        User authUser = userService.getByEppn(authUserEppn);
        SignBook signBook = createSignBook(name, null, "","", authUser, false);
        SignRequest signRequest = signRequestService.createSignRequest(null, signBook, authUserEppn, authUserEppn);
        signRequestService.addDocsToSignRequest(signRequest, true, 0, multipartFiles);
        logger.info("signRequest : " + signRequest.getId() + " added to signBook" + signBook.getName() + " - " + signBook.getId());
        return signBook;
    }

    @Transactional
    public void addWorkflowToSignBook(SignBook signBook, String authUserEppn, Long workflowSignBookId) throws EsupSignatureException {
        Workflow workflow = workflowService.getById(workflowSignBookId);
        importWorkflow(signBook, workflow, null);
        nextWorkFlowStep(signBook);
        pendingSignBook(signBook, null, authUserEppn, authUserEppn, false);
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
    public void addLiveStep(Long id, List<String> recipientsEmails, int stepNumber, Boolean allSignToComplete, SignType signType, boolean repeatable, boolean multiSign, String authUserEppn) throws EsupSignatureException {
        SignBook signBook = this.getById(id);
        int currentStepNumber = signBook.getLiveWorkflow().getCurrentStepNumber();
        LiveWorkflowStep liveWorkflowStep = liveWorkflowStepService.createLiveWorkflowStep(null, repeatable, multiSign, allSignToComplete, signType, recipientsEmails, null);
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
            userPropertieService.createUserPropertieFromMails(userService.getByEppn(authUserEppn), recipientsEmails);
        }
    }

    public void dispatchSignRequestParams(SignBook signBook) {
        for(SignRequest signRequest : signBook.getSignRequests()) {
            int i = 0;
            if(signRequest.getSignRequestParams().size() > 0) {
                for (LiveWorkflowStep liveWorkflowStep : signBook.getLiveWorkflow().getLiveWorkflowSteps()) {
                    if (!liveWorkflowStep.getSignType().equals(SignType.hiddenVisa)) {
                        if (signRequest.getSignRequestParams().size() >= i + 1) {
                            liveWorkflowStep.getSignRequestParams().add(signRequest.getSignRequestParams().get(i));
                        } else {
                            break;
                        }
                        i++;
                    }
                }
            } else {
                for (LiveWorkflowStep liveWorkflowStep : signBook.getLiveWorkflow().getLiveWorkflowSteps()) {
                    if(liveWorkflowStep.getWorkflowStep() != null) {
                        WorkflowStep workflowStep = workflowStepService.getById(liveWorkflowStep.getWorkflowStep().getId());
                        liveWorkflowStep.getSignRequestParams().addAll(workflowStep.getSignRequestParams());
                    }
                }
            }
        }
    }

    @Transactional
    public void sendCCEmail(Long signBookId, List<String> recipientsCCEmails) throws EsupSignatureMailException {
        SignBook signBook = getById(signBookId);
        if(recipientsCCEmails != null) {
            addViewers(signBookId, recipientsCCEmails);
        }
        mailService.sendCCtAlert(signBook.getViewers().stream().map(User::getEmail).collect(Collectors.toList()), signBook.getSignRequests().get(0));
    }

    @Transactional
    public void addViewers(Long signBookId, List<String> recipientsCCEmails) {
        SignBook signBook = getById(signBookId);
        if(recipientsCCEmails != null) {
            for (String recipientsEmail : recipientsCCEmails) {
                User user = userService.getUserByEmail(recipientsEmail);
                if(!signBook.getViewers().contains(user)) {
                    signBook.getViewers().add(user);
                }
            }
        }
    }

    @Transactional
    public List<SignRequest> getSignRequestByViewer(String userEppn) {
        Set<SignRequest> signRequests = new HashSet<>();
        List<SignBook> signBooks = signBookRepository.findByViewersContaining(userService.getUserByEppn(userEppn));
        for (SignBook signBook : signBooks) {
            signRequests.addAll(signBook.getSignRequests());
        }
        return new ArrayList<>(signRequests);
    }

}
