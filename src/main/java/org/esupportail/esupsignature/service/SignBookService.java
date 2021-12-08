package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureMailException;
import org.esupportail.esupsignature.repository.DataRepository;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.repository.WorkflowRepository;
import org.esupportail.esupsignature.service.interfaces.workflow.DefaultWorkflow;
import org.esupportail.esupsignature.service.mail.MailService;
import org.esupportail.esupsignature.service.security.otp.OtpService;
import org.esupportail.esupsignature.web.ws.json.JsonExternalUserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@EnableConfigurationProperties(GlobalProperties.class)
public class SignBookService {

    private static final Logger logger = LoggerFactory.getLogger(SignBookService.class);

    private final GlobalProperties globalProperties;

    @Resource
    private SignBookRepository signBookRepository;

    @Resource
    private UserService userService;

    @Resource
    private MailService mailService;

    @Resource
    private WorkflowStepService workflowStepService;

    @Resource
    private LiveWorkflowService liveWorkflowService;

    @Resource
    private LiveWorkflowStepService liveWorkflowStepService;

    @Resource
    private LogService logService;

    @Resource
    private TargetService targetService;

    @Resource
    private UserPropertieService userPropertieService;

    @Resource
    private CommentService commentService;

    @Resource
    private OtpService otpService;

    @Resource
    private SignRequestRepository signRequestRepository;

    @Resource
    private DataRepository dataRepository;

    @Resource
    private WorkflowRepository workflowRepository;

    public SignBookService(GlobalProperties globalProperties) {
        this.globalProperties = globalProperties;
    }

    public List<SignBook> getAllSignBooks() {
        List<SignBook> list = new ArrayList<>();
        signBookRepository.findAll().forEach(list::add);
        return list;
    }

    @Transactional
    public List<SignBook> getSignBooksByWorkflow(Long workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId).get();
        return signBookRepository.findByLiveWorkflowWorkflow(workflow);
    }

    @Transactional
    public int countSignBooksByWorkflow(Long workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId).get();
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
        String name = generateName2(signBook, title, workflowName, order, user, namingTemplate);
        signBook.setName(name);
        return signBook;
    }

    @Transactional
    public void initSignBook(Long signBookId, Long id, User user) {
        SignBook signBook = getById(signBookId);
        Workflow workflow = workflowRepository.findById(id).get();
        signBook.setName(workflow.getName() + "_" + new Date() + "_" + user.getEppn());
        signBook.setTitle(workflow.getDescription());
        signBook.getLiveWorkflow().setWorkflow(workflow);
        for(Target target : workflow.getTargets()) {
            signBook.getLiveWorkflow().getTargets().add(targetService.createTarget(target.getTargetUri()));
        }
    }

    public SignBook getById(Long id) {
        Optional<SignBook> signBook = signBookRepository.findById(id);
        if(signBook.isPresent()) {
            signBook.get().setLogs(getLogsFromSignBook(signBook.get()));
            return signBook.get();
        }
        return null;
    }

    public List<SignBook> getByWorkflowId(Long id) {
        return signBookRepository.findByWorkflowId(id);
    }

//    public void addSignRequest(SignBook signBook, SignRequest signRequest) {
//        signBook.getSignRequests().add(signRequest);
//        signRequest.setParentSignBook(signBook);
//    }

    @Transactional
    public boolean delete(Long signBookId, String userEppn) {
        SignBook signBook = getById(signBookId);
        if(signBook.getStatus().equals(SignRequestStatus.deleted)) {
            deleteDefinitive(signBookId);
            return true;
        }
        List<Long> signRequestsIds = signBook.getSignRequests().stream().map(SignRequest::getId).collect(Collectors.toList());
        for(Long signRequestId : signRequestsIds) {
            deleteSignRequest(signRequestId, userEppn);
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
        return false;
    }

    @Transactional
    public void deleteSignRequest(Long signRequestId, String userEppn) {
        //TODO critères de suppression ou en conf (if deleteDefinitive)
        SignRequest signRequest = signRequestRepository.findById(signRequestId).get();
        if(signRequest.getStatus().equals(SignRequestStatus.deleted)) {
            deleteDefinitive(signRequestId);
        } else {
            if (signRequest.getStatus().equals(SignRequestStatus.exported) || signRequest.getStatus().equals(SignRequestStatus.archived)) {
                signRequest.getOriginalDocuments().clear();
                signRequest.getSignedDocuments().clear();
            }
            signRequest.setStatus(SignRequestStatus.deleted);
            logService.create(signRequest.getId(), SignRequestStatus.deleted, "Suppression par l'utilisateur", "", "SUCCESS", null, null, null, null, userEppn, userEppn);
            otpService.deleteOtpBySignRequestId(signRequestId);
        }
    }

    public void nullifySignBook(SignBook signBook) {
        Data data = getBySignBook(signBook);
        if(data != null) data.setSignBook(null);
    }

    @Transactional
    public void deleteDefinitive(Long signBookId) {
        SignBook signBook = getById(signBookId);
        signBook.getLiveWorkflow().setCurrentStep(null);
        List<Long> liveWorkflowStepIds = signBook.getLiveWorkflow().getLiveWorkflowSteps().stream().map(LiveWorkflowStep::getId).collect(Collectors.toList());
        signBook.getLiveWorkflow().getLiveWorkflowSteps().clear();
        for (Long liveWorkflowStepId : liveWorkflowStepIds) {
            liveWorkflowStepService.delete(liveWorkflowStepId);
        }
        List<Long> signRequestsIds = signBook.getSignRequests().stream().map(SignRequest::getId).collect(Collectors.toList());
        for(Long signRequestId : signRequestsIds) {
            deleteDefinitiveSignRequest(signRequestId);
        }
        nullifySignBook(signBook);
        signBookRepository.delete(signBook);
        logger.info("definitive delete signbook : " + signBookId);
    }

    public void deleteOnlyData(Long id) {
        Data data = dataRepository.findById(id).get();
        data.setForm(null);
        dataRepository.delete(data);
    }

    @Transactional
    public void deleteDefinitiveSignRequest(Long signRequestId) {
        SignRequest signRequest = signRequestRepository.findById(signRequestId).get();
        signRequest.getRecipientHasSigned().clear();
        signRequestRepository.save(signRequest);
        if (signRequest.getData() != null) {
            Long dataId = signRequest.getData().getId();
            signRequest.setData(null);
            deleteOnlyData(dataId);
        }
        List<Long> commentsIds = signRequest.getComments().stream().map(Comment::getId).collect(Collectors.toList());
        for (Long commentId : commentsIds) {
            commentService.deleteComment(commentId);
        }
        signRequest.getParentSignBook().getSignRequests().remove(signRequest);
        signRequestRepository.delete(signRequest);
    }

    public boolean checkUserManageRights(String userEppn, SignBook signBook) {
        if(signBook.getSignRequests().size() == 1) {
            User user = userService.getUserByEppn(userEppn);
            Data data = getBySignBook(signBook);
            if(data != null && data.getForm() != null && !data.getForm().getManagers().isEmpty()) {
                if (data.getForm().getManagers().contains(user.getEmail())) {
                    return true;
                }
            }
        }
        return signBook.getCreateBy().getEppn().equals(userEppn);
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
            LiveWorkflowStep newWorkflowStep = liveWorkflowStepService.createLiveWorkflowStep(workflowStep, workflowStep.getRepeatable(), workflowStep.getMultiSign(), workflowStep.getAutoSign(), workflowStep.getAllSignToComplete(), workflowStep.getSignType(), recipientEmails, externalUsersInfos);
            signBook.getLiveWorkflow().getLiveWorkflowSteps().add(newWorkflowStep);
        }
        if(!(workflow instanceof DefaultWorkflow)) {
            signBook.getLiveWorkflow().setWorkflow(workflow);
        }
        dispatchSignRequestParams(signBook);
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

    public boolean isWorkflowStepFullSigned(LiveWorkflowStep liveWorkflowStep) {
        for (Recipient recipient : liveWorkflowStep.getRecipients()) {
            if (!recipient.getSigned()) {
                return false;
            }
        }
        return true;
    }

    public boolean isStepAllSignDone(SignBook signBook) {
        LiveWorkflowStep liveWorkflowStep = signBook.getLiveWorkflow().getCurrentStep();
        return (!liveWorkflowStep.getAllSignToComplete() || isWorkflowStepFullSigned(liveWorkflowStep)) && !isMoreWorkflowStep(signBook);
    }

    public boolean nextWorkFlowStep(SignBook signBook) {
        if (isMoreWorkflowStep(signBook)) {
            signBook.getLiveWorkflow().setCurrentStep(signBook.getLiveWorkflow().getLiveWorkflowSteps().get(signBook.getLiveWorkflow().getCurrentStepNumber()));
            return signBook.getLiveWorkflow().getCurrentStepNumber() > -1;
        }
        return false;
    }

    public boolean isMoreWorkflowStep(SignBook signBook) {
        int test = signBook.getLiveWorkflow().getCurrentStepNumber();
        return signBook.getLiveWorkflow().getLiveWorkflowSteps().size() >= signBook.getLiveWorkflow().getCurrentStepNumber() + 1 && test > -1;
    }

    public boolean isNextWorkFlowStep(SignBook signBook) {
        return signBook.getLiveWorkflow().getLiveWorkflowSteps().size() >= signBook.getLiveWorkflow().getCurrentStepNumber() + 2;
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

    public String generateName2(SignBook signBook, String title, String worflowName, int order, User user, String template) {
        if(template.contains("[id]")) {
            template = template.replace("[id]", signBook.getId() + "");
        }
        if(template.contains("[title]")) {
            template = template.replace("[title]", title);
        }
        if(template.contains("[worflowName]")) {
            template = template.replace("[worflowName]", worflowName);
        }
        if(template.contains("[user.eppn]")) {
            template = template.replace("[user.eppn]", user.getEppn().replace("@", "_"));
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
        if(signBook.getSignRequests().size() == 1) {
            Data data = getBySignRequest(signBook.getSignRequests().get(0));
            if(data != null) {
                for(Map.Entry<String, String> entry: data.getDatas().entrySet()) {
                    if(template.contains("[form." + entry.getKey() + "]")) {
                        template = template.replace("[form." + entry.getKey() + "]", entry.getValue());
                    }
                }

            }
        }
        return template.replaceAll("\\W+", "_");
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
    public void addLiveStep(Long id, List<String> recipientsEmails, int stepNumber, Boolean allSignToComplete, SignType signType, boolean repeatable, boolean multiSign, boolean autoSign, String authUserEppn) throws EsupSignatureException {
        SignBook signBook = this.getById(id);
        int currentStepNumber = signBook.getLiveWorkflow().getCurrentStepNumber();
        LiveWorkflowStep liveWorkflowStep = liveWorkflowStepService.createLiveWorkflowStep(null, repeatable, multiSign, autoSign, allSignToComplete, signType, recipientsEmails, null);
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
            if(signRequest.getSignRequestParams().size() > 0) {
                for (LiveWorkflowStep liveWorkflowStep : signBook.getLiveWorkflow().getLiveWorkflowSteps()) {
                    if (liveWorkflowStep.getWorkflowStep() != null) {
                        WorkflowStep workflowStep = workflowStepService.getById(liveWorkflowStep.getWorkflowStep().getId());
                        if (!liveWorkflowStep.getSignType().equals(SignType.hiddenVisa)) {
                            for (SignRequestParams signRequestParams : signRequest.getSignRequestParams()) {
                                for (SignRequestParams signRequestParams1 : workflowStep.getSignRequestParams()) {
                                    if (signRequestParams1.getSignPageNumber().equals(signRequestParams.getSignPageNumber())
                                            && signRequestParams1.getxPos().equals(signRequestParams.getxPos())
                                            && signRequestParams1.getyPos().equals(signRequestParams.getyPos())) {
                                        liveWorkflowStep.getSignRequestParams().add(signRequestParams);
                                    }
                                }
//                            if(signRequestParams.getSignStepNumber() != null && signRequestParams.getSignStepNumber().equals(i)) {
//                                liveWorkflowStep.getSignRequestParams().add(signRequestParams);
//                            } else {
//                                logger.warn("no signrequestparams found for step " + i + " please update signrequestparams / steps relation");
//                            }
                            }
                        }
//                    i++;
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

    public Data getBySignRequest(SignRequest signRequest) {
        return getBySignBook(signRequest.getParentSignBook());
    }

    public Data getBySignBook(SignBook signBook) {
        return dataRepository.findBySignBook(signBook);
    }


}
