package org.esupportail.esupsignature.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.*;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.repository.WorkflowRepository;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessFactoryService;
import org.esupportail.esupsignature.service.interfaces.listsearch.UserListService;
import org.esupportail.esupsignature.service.interfaces.workflow.DefaultWorkflow;
import org.esupportail.esupsignature.web.ws.json.JsonExternalUserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkflowService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowService.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Resource
    private List<Workflow> workflows;

    @Resource
    private WorkflowRepository workflowRepository;

    @Resource
    private WorkflowStepService workflowStepService;

    @Resource
    private LiveWorkflowService liveWorkflowService;

    @Resource
    private LiveWorkflowStepService liveWorkflowStepService;

    @Resource
    private FsAccessFactoryService fsAccessFactoryService;

    @Resource
    private UserService userService;

    @Resource
    private UserShareService userShareService;

    @Resource
    public UserPropertieService userPropertieService;

    @Resource
    private TargetService targetService;

    @Resource
    private FieldService fieldService;

    @Resource
    private SignBookRepository signBookRepository;

    @Resource
    private UserListService userListService;

    @PostConstruct
    public void initCreatorWorkflow() {
        User creator= userService.getByEppn("creator");
        if (creator == null) {
            creator = userService.createUser("creator", "Createur de la demande", "", "creator", UserType.system, false);
        }
        if (workflowRepository.countByName("Ma signature") == 0) {
            Workflow workflow = new Workflow();
            workflow.setName("Ma signature");
            workflow.setDescription("Signature du créateur de la demande");
            workflow.setCreateDate(new Date());
            workflow.setCreateBy(userService.getSystemUser());
            WorkflowStep workflowStep = workflowStepService.createWorkflowStep("Ma signature", false, SignType.pdfImageStamp, creator.getEmail());
            workflow.getWorkflowSteps().add(workflowStep);
            workflowRepository.save(workflow);
        }
    }

    @Transactional
    public void copyClassWorkflowsIntoDatabase() throws EsupSignatureException {
        for (Workflow classWorkflow : getClassesWorkflows()) {
            logger.info("workflow class found : " + classWorkflow.getName());
            if (!isWorkflowExist(classWorkflow.getName(), "system")) {
                logger.info("create " + classWorkflow.getName() + " on database : ");
                Workflow newWorkflow = createWorkflow(classWorkflow.getName(), classWorkflow.getDescription(), userService.getSystemUser());
                newWorkflow.setFromCode(true);
            } else {
                logger.info("update " + classWorkflow.getName() + " on database");
                Workflow toUpdateWorkflow = workflowRepository.findByName(classWorkflow.getName());
                toUpdateWorkflow.setPublicUsage(classWorkflow.getPublicUsage());
                toUpdateWorkflow.getRoles().clear();
                toUpdateWorkflow.getRoles().addAll(classWorkflow.getRoles());
                toUpdateWorkflow.setDescription(classWorkflow.getDescription());
                toUpdateWorkflow.setTitle(classWorkflow.getTitle());
                toUpdateWorkflow.setDocumentsSourceUri(classWorkflow.getDocumentsSourceUri());
                toUpdateWorkflow.getTargets().addAll(classWorkflow.getTargets());
                toUpdateWorkflow.setAuthorizedShareTypes(classWorkflow.getAuthorizedShareTypes());
                toUpdateWorkflow.setScanPdfMetadatas(classWorkflow.getScanPdfMetadatas());
                toUpdateWorkflow.setManagers(classWorkflow.getManagers());
            }
        }
        List<Workflow> toRemoveWorkflows = new ArrayList<>();
        for (Workflow workflow : workflowRepository.findByFromCodeIsTrue()) {
            try {
                DefaultWorkflow defaultWorkflow = (DefaultWorkflow) getWorkflowByClassName(workflow.getName());
                if (defaultWorkflow != null) {
                    List<WorkflowStep> generatedWorkflowSteps = defaultWorkflow.generateWorkflowSteps(userService.getSystemUser(), null);
                    int i = 0;
                    for (WorkflowStep generatedWorkflowStep : generatedWorkflowSteps) {
                        if (workflow.getWorkflowSteps().size() > i) {
                            WorkflowStep toUdateWorkflowStep = workflow.getWorkflowSteps().get(i);
                            toUdateWorkflowStep.setDescription(generatedWorkflowStep.getDescription());
                            toUdateWorkflowStep.getUsers().clear();
                            toUdateWorkflowStep.getUsers().addAll(generatedWorkflowStep.getUsers());
                        } else {
                            WorkflowStep newWorkflowStep = workflowStepService.createWorkflowStep(generatedWorkflowStep.getName(), generatedWorkflowStep.getAllSignToComplete(), generatedWorkflowStep.getSignType());
                            for (User user : generatedWorkflowStep.getUsers()) {
                                userService.save(user);
                                newWorkflowStep.getUsers().add(user);
                            }
                            newWorkflowStep.setDescription(generatedWorkflowStep.getDescription());
                            newWorkflowStep.setChangeable(generatedWorkflowStep.getChangeable());
                            workflow.getWorkflowSteps().add(newWorkflowStep);
                            workflowRepository.save(workflow);
                        }
                        i++;
                    }
                } else {
                    toRemoveWorkflows.add(workflow);
                }
            } catch(EsupSignatureUserException e){
                logger.warn("already exist");
            }
        }
        for(Workflow workflow : toRemoveWorkflows) {
            workflowRepository.delete(workflow);
        }
    }

    public boolean isWorkflowExist(String name, String userEppn) {
        return workflowRepository.countByNameAndCreateByEppn(name, userEppn) > 0;
    }

//    public boolean isWorkflowExist(String name) {
//        return workflowRepository.countByName(name) > 0;
//    }

    public Workflow createWorkflow(User user) {
        Workflow workflow;
        workflow = new Workflow();
        workflow.setCreateDate(new Date());
        workflow.setCreateBy(user);
        workflowRepository.save(workflow);
        return workflow;
    }

    @Transactional
    public Workflow addStepToWorkflow(Long id, SignType signType, Boolean allSignToComplete, String[] recipientsEmails, User user) {
        Workflow workflow;
        if (id != null) {
            workflow = getById(id);
        } else {
            workflow = createWorkflow(user);
        }
        if(workflow.getCreateBy().getEppn().equals(user.getEppn())) {
            if(recipientsEmails != null) {
                logger.info("add new workflow step to Workflow " + workflow.getId());
                WorkflowStep workflowStep = workflowStepService.createWorkflowStep("", allSignToComplete, signType, recipientsEmails);
                workflow.getWorkflowSteps().add(workflowStep);
                userPropertieService.createUserPropertieFromMails(user, Arrays.asList(recipientsEmails));
            }
        }
        return workflow;
    }

    @Transactional
    public Workflow createWorkflow(String title, String description, User user) throws EsupSignatureException {
        String name;
        if (userService.getSystemUser().equals(user)) {
            name = title;
        } else {
            name = user.getEppn().split("@")[0] + title.substring(0, 1).toUpperCase() + title.toLowerCase().substring(1);
            name = name.replaceAll("[^a-zA-Z0-9]", "");
        }
        if (!isWorkflowExist(name, user.getEppn())) {
            Workflow workflow = new Workflow();
            workflow.setName(name);
            workflow.setDescription(description);
            workflow.setTitle(title.replaceAll("[\\\\/:*?\"<>|]", "_").replace(" ", "_"));
            workflow.setCreateBy(user);
            workflow.setCreateDate(new Date());
            workflow.getManagers().removeAll(Collections.singleton(""));
            workflowRepository.save(workflow);
            return workflow;
        } else {
            throw new EsupSignatureException("already exist");
        }
    }

    public Set<Workflow> getWorkflowsByUser(String userEppn, String authUserEppn) {
        User user = userService.getByEppn(userEppn);
        User authUser = userService.getByEppn(authUserEppn);
        Set<Workflow> authorizedWorkflows = new HashSet<>();
        for (String role : user.getRoles()) {
            authorizedWorkflows.addAll(workflowRepository.findAuthorizedForms(role));
        }
        Set<Workflow> workflows = new HashSet<>();
        if (userEppn.equals(authUserEppn)) {
            workflows.addAll(workflowRepository.findByCreateByEppn(userEppn).stream().filter(workflow -> workflow.getManagerRole() == null || workflow.getManagerRole().isEmpty()).collect(Collectors.toList()));
//            workflows.addAll(workflowRepository.findByManagersContains(user.getEmail()));
            workflows.addAll(authorizedWorkflows);
        } else {
            for (UserShare userShare : userShareService.getByUserAndToUsersInAndShareTypesContains(userEppn, authUser, ShareType.create)) {
                if (userShare.getWorkflow() != null && authorizedWorkflows.contains(userShare.getWorkflow())) {
                    workflows.add(userShare.getWorkflow());
                }
            }
        }
        workflows = workflows.stream().sorted(Comparator.comparing(Workflow::getCreateDate)).collect(Collectors.toCollection(LinkedHashSet::new));
        return workflows;
    }

    public List<Workflow> getClassesWorkflows() {
        return this.workflows;
    }

    public List<Workflow> getDatabaseWorkflows() {
        return workflowRepository.findAll();
    }

    public Set<Workflow> getWorkflowsBySystemUser() {
        User systemUser = userService.getSystemUser();
        return getWorkflowsByUser(systemUser.getEppn(), systemUser.getEppn());

    }

    public List<Workflow> getSystemWorkflows() {
        List<Workflow> workflowTypes = new ArrayList<>(workflowRepository.findNotInForm());
        return workflowTypes;
    }

    public List<Workflow> getAuthorizedToShareWorkflows() {
        List<Workflow> workflows = workflowRepository.findDistinctByAuthorizedShareTypesIsNotNull();
        workflows = workflows.stream().filter(workflow -> workflow.getAuthorizedShareTypes().size() > 0).collect(Collectors.toList());
        return workflows;
    }

    public List<Workflow> getAllWorkflows() {
        List<Workflow> allWorkflows = new ArrayList<>();
        allWorkflows.addAll(this.getDatabaseWorkflows());
        return allWorkflows;
    }

    public Workflow getWorkflowByClassName(String className) {
        for (Workflow workflow : workflows) {
            if (className.equals(workflow.getName())) {
                return workflow;
            }
        }
        return null;
    }

    public Workflow getById(Long id) {
        return workflowRepository.findById(id).get();
    }

//    public Workflow getWorkflowByName(String name) {
//        return workflowRepository.findByName(name);
//    }

    @Transactional
    public Workflow initWorkflow(User user, Long id, String name) {
        Workflow workflow = getById(id);
        workflow.setCreateBy(user);
        workflow.setName(name);
        workflow.setDescription(name);
        workflow.setTitle(name.replaceAll("[\\\\/:*?\"<>|]", "_").replace(" ", "_"));
        return workflow;
    }

    public Workflow computeWorkflow(Long workflowId, List<String> recipientEmails, List<String> allSignToCompletes, String userEppn, boolean computeForDisplay) throws EsupSignatureException {
        try {
            Workflow modelWorkflow = getById(workflowId);
            if (modelWorkflow.getFromCode() != null && modelWorkflow.getFromCode()) {
                DefaultWorkflow defaultWorkflow = (DefaultWorkflow) getWorkflowByClassName(modelWorkflow.getName());
                defaultWorkflow.fillWorkflowSteps(modelWorkflow, recipientEmails);
            }
            int step = 1;
            for (WorkflowStep workflowStep : modelWorkflow.getWorkflowSteps()) {
                entityManager.detach(workflowStep);
                replaceStepSystemUsers(userEppn, workflowStep);
                if (workflowStep.getChangeable() != null && workflowStep.getChangeable()) {
                    List<User> recipients = this.getFavoriteRecipientEmail(step, recipientEmails);
                    if(recipients.size() > 0 && !computeForDisplay) {
                        workflowStep.getUsers().clear();
                        for (User oneUser : recipients) {
                            workflowStep.getUsers().add(oneUser);
                        }
                    }
                    if(allSignToCompletes != null && allSignToCompletes.contains(step + "")) {
                        workflowStep.setAllSignToComplete(true);
                    }
                }
                step++;
            }
            entityManager.detach(modelWorkflow);
            return modelWorkflow;
        } catch (Exception e) {
            throw new EsupSignatureException("compute workflow error", e);
        }
    }

    public List<User> getFavoriteRecipientEmail(int stepNumber, List<String> recipientEmails) {
        List<User> users = new ArrayList<>();
        if (recipientEmails != null && recipientEmails.size() > 0) {
            recipientEmails = recipientEmails.stream().filter(r -> r.startsWith(String.valueOf(stepNumber))).collect(Collectors.toList());
            for (String recipientEmail : recipientEmails) {
                String userEmail = recipientEmail.split("\\*")[1];
                users.add(userService.getUserByEmail(userEmail));
            }
        }
        return users;
    }

    public void replaceStepSystemUsers(String userEppn, WorkflowStep workflowStep) {
        User user = userService.getByEppn(userEppn);
        if(TransactionSynchronizationManager.isActualTransactionActive()) {
            List<User> users = new ArrayList<>(workflowStep.getUsers());
            for (User oneUser : users) {
                if (oneUser.getEppn().equals("creator")) {
                    workflowStep.getUsers().remove(oneUser);
                    workflowStep.getUsers().add(user);
                }
                if (oneUser.getEppn().equals("generic")) {
                    workflowStep.getUsers().remove(oneUser);
                }
                if(oneUser.getUserType().equals(UserType.group)) {
                    workflowStep.getUsers().remove(oneUser);
                    List<String> emails = userListService.getUsersEmailFromList(oneUser.getEmail());
                    for (String email : emails) {
                        workflowStep.getUsers().add(userService.getUserByEmail(email));
                    }
                }
            }
        }
   }

    public void delete(Workflow workflow) throws EsupSignatureException {
        List<SignBook> signBooks = signBookRepository.findByLiveWorkflowWorkflow(workflow);
        if(signBooks.stream().allMatch(signBook -> signBook.getStatus() == SignRequestStatus.draft || signBook.getStatus() == SignRequestStatus.deleted)) {
            List<LiveWorkflow> liveWorkflows = liveWorkflowService.getByWorkflow(workflow);
            for(LiveWorkflow liveWorkflow : liveWorkflows) {
                liveWorkflow.setWorkflow(null);
                liveWorkflow.getLiveWorkflowSteps().forEach(lws -> lws.setWorkflowStep(null));
            }
            for (WorkflowStep workflowStep : workflow.getWorkflowSteps()) {
                List<Field> fields = fieldService.getFieldsByWorkflowStep(workflowStep);
                for(Field field : fields) {
                    field.getWorkflowSteps().remove(workflowStep);
                }
            }
            workflowRepository.delete(workflow);
        } else {
            throw new EsupSignatureException("Le circuit ne peut pas être supprimé car il est en court d'utilisation");
        }
    }

    public List<Workflow> getWorkflowsByDisplayWorkflowType(DisplayWorkflowType displayWorkflowType) {
        if (displayWorkflowType == null) {
            displayWorkflowType = DisplayWorkflowType.system;
        }
        List<Workflow> workflows = new ArrayList<>();
        if(DisplayWorkflowType.system.equals(displayWorkflowType)) {
            workflows.addAll(getWorkflowsBySystemUser().stream().filter(workflow -> workflow.getFromCode() == null || !workflow.getFromCode()).collect(Collectors.toList()));
        } else if(DisplayWorkflowType.classes.equals(displayWorkflowType)) {
            workflows.addAll(getClassesWorkflows());
        } else if(DisplayWorkflowType.all.equals(displayWorkflowType)) {
            workflows.addAll(getAllWorkflows());
        } else if(DisplayWorkflowType.users.equals(displayWorkflowType)) {
            workflows.addAll(getAllWorkflows());
            workflows.removeAll(getClassesWorkflows());
            workflows.removeAll(getWorkflowsBySystemUser());
        }
        return workflows;
    }

    public Workflow update(Workflow workflow, User user, String[] types, List<String> managers) {
        Workflow workflowToUpdate = getById(workflow.getId());
        if(managers != null && managers.size() > 0) {
            workflowToUpdate.getManagers().clear();
            for(String manager : managers) {
                User managerUser = userService.getUserByEmail(manager);
                if(!workflowToUpdate.getManagers().contains(managerUser.getEmail())) {
                    workflowToUpdate.getManagers().add(managerUser.getEmail());
                }
            }
        } else {
            workflowToUpdate.getManagers().clear();
        }
        workflowToUpdate.getAuthorizedShareTypes().clear();
        List<ShareType> shareTypes = new ArrayList<>();
        if(types != null) {
            for (String type : types) {
                ShareType shareType = ShareType.valueOf(type);
                workflowToUpdate.getAuthorizedShareTypes().add(shareType);
                shareTypes.add(shareType);
            }
        }
        List<UserShare> userShares = userShareService.getByWorkflowId(workflowToUpdate.getId());
        for(UserShare userShare : userShares) {
            userShare.getShareTypes().removeIf(shareType -> !shareTypes.contains(shareType));
        }
        workflowToUpdate.getTargets().addAll(workflow.getTargets());
        workflowToUpdate.setDocumentsSourceUri(workflow.getDocumentsSourceUri());
        workflowToUpdate.setDescription(workflow.getDescription());
        workflowToUpdate.setTitle(workflow.getTitle());
        workflowToUpdate.setNamingTemplate(workflow.getNamingTemplate());
        workflowToUpdate.setTargetNamingTemplate(workflow.getTargetNamingTemplate());
        workflowToUpdate.setPublicUsage(workflow.getPublicUsage());
        workflowToUpdate.setSealAtEnd(workflow.getSealAtEnd());
        workflowToUpdate.setVisibility(workflow.getVisibility());
        workflowToUpdate.setOwnerSystem(workflow.getOwnerSystem());
        workflowToUpdate.setScanPdfMetadatas(workflow.getScanPdfMetadatas());
        workflowToUpdate.setSendAlertToAllRecipients(workflow.getSendAlertToAllRecipients());
        workflowToUpdate.getRoles().clear();
        workflowToUpdate.getRoles().addAll(workflow.getRoles());
        workflowToUpdate.setUpdateBy(user.getEppn());
        workflowToUpdate.setUpdateDate(new Date());
        workflowRepository.save(workflowToUpdate);
        return workflowToUpdate;
    }

    public List<WorkflowStep> getWorkflowStepsFromSignRequest(SignRequest signRequest, String userEppn) throws EsupSignatureException {
        List<WorkflowStep> workflowSteps = new ArrayList<>();
        if(signRequest.getParentSignBook().getLiveWorkflow().getWorkflow() != null) {
            Workflow workflow = computeWorkflow(signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getId(), null, null, userEppn, true);
            workflowSteps.addAll(workflow.getWorkflowSteps());
        }
        return workflowSteps;
    }

    @Transactional
    public boolean addTarget(Long id, String documentsTargetUri) throws EsupSignatureFsException {
        Workflow workflow = getById(id);
        DocumentIOType targetType = fsAccessFactoryService.getPathIOType(documentsTargetUri);
        if(!targetType.equals("mail") || workflow.getTargets().stream().map(Target::getTargetUri).noneMatch(tt -> tt.contains("mailto"))) {
            Target target = targetService.createTarget(documentsTargetUri);
            workflow.getTargets().add(target);
            return true;
        }
        return false;
    }

    @Transactional
    public void deleteTarget(Long id, Long targetId) {
        Workflow workflow = getById(id);
        Target target = targetService.getById(targetId);
        workflow.getTargets().remove(target);
        targetService.delete(target);
    }

    public List<Workflow> getManagerWorkflows(String userEppn) {
        User manager = userService.getByEppn(userEppn);
        Set<Workflow> workflowsManaged = new HashSet<>();
        for (String role : manager.getManagersRoles()) {
            workflowsManaged.addAll(workflowRepository.findByManagerRole(role));
        }
        return new ArrayList<>(workflowsManaged);
    }

    @Transactional
    public List<Workflow> getWorkflowByManagersContains(String eppn) {
        User user = userService.getUserByEppn(eppn);
        return workflowRepository.findWorkflowByManagersIn(Collections.singletonList(user.getEmail()));
    }

    @Transactional
    public InputStream getJsonWorkflowSetup(Long id) throws IOException {
        Workflow workflow = getById(id);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writer().writeValue(outputStream, workflow);
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    @Transactional
    public void setWorkflowSetupFromJson(Long id, InputStream inputStream) throws IOException, EsupSignatureException, EsupSignatureFsException {
        Workflow workflow = getById(id);
        String savedName = workflow.getName();
        String savedTitle = workflow.getTitle();
        ObjectMapper objectMapper = new ObjectMapper();
        Workflow workflowSetup = objectMapper.readValue(inputStream.readAllBytes(), Workflow.class);
        workflow.getWorkflowSteps().clear();
        for(WorkflowStep workflowStepSetup : workflowSetup.getWorkflowSteps()) {
            Optional<WorkflowStep> optionalWorkflowStep = workflow.getWorkflowSteps().stream().filter(workflowStep1 -> workflowStep1.getId().equals(workflowStepSetup.getId())).findFirst();
            if(optionalWorkflowStep.isPresent()) {
                WorkflowStep workflowStep = optionalWorkflowStep.get();
                workflowStepService.updateStep(workflowStep.getId(), workflowStepSetup.getSignType(), workflowStepSetup.getDescription(), workflowStepSetup.getChangeable(), workflowStepSetup.getRepeatable(), workflowStepSetup.getMultiSign(), workflowStepSetup.getAllSignToComplete(), workflowStepSetup.getMaxRecipients(), workflowStepSetup.getAttachmentAlert(), workflowStepSetup.getAttachmentRequire(), false, null);
            } else {
                WorkflowStep newWorkflowStep = workflowStepService.createWorkflowStep(workflowSetup.getName(), workflowStepSetup.getAllSignToComplete(), workflowStepSetup.getSignType(), workflowStepSetup.getUsers().stream().map(User::getEmail).collect(Collectors.toList()).toArray(String[]::new));
                workflowStepService.updateStep(newWorkflowStep.getId(), workflowStepSetup.getSignType(), workflowStepSetup.getDescription(), workflowStepSetup.getChangeable(), workflowStepSetup.getRepeatable(), workflowStepSetup.getMultiSign(), workflowStepSetup.getAllSignToComplete(), workflowStepSetup.getMaxRecipients(), workflowStepSetup.getAttachmentAlert(), workflowStepSetup.getAttachmentRequire(), false, null);
                workflow.getWorkflowSteps().add(newWorkflowStep);
            }
        }
        workflow.getTargets().clear();
        update(workflow, workflowSetup.getCreateBy(), null, workflowSetup.getManagers());
        for(Target target : workflowSetup.getTargets()) {
            Target newTarget = targetService.createTarget(target.getTargetUri());
            workflow.getTargets().add(newTarget);
        }
        workflow.setName(savedName);
        workflow.setTitle(savedTitle);
        return;
    }


    public void importWorkflow(SignBook signBook, Workflow workflow, List<JsonExternalUserInfo> externalUsersInfos) {
        logger.info("import workflow steps in signBook " + signBook.getSubject() + " - " + signBook.getId());
        for (WorkflowStep workflowStep : workflow.getWorkflowSteps()) {
            List<String> recipientEmails = new ArrayList<>();
            for (User user : workflowStep.getUsers()) {
                if (user.equals(userService.getCreatorUser())) {
                    recipientEmails.add(signBook.getCreateBy().getEmail());
                } else {
                    recipientEmails.add(user.getEmail());
                }
            }
            LiveWorkflowStep newWorkflowStep = liveWorkflowStepService.createLiveWorkflowStep(signBook, workflowStep, workflowStep.getRepeatable(), workflowStep.getRepeatableSignType(), workflowStep.getMultiSign(), workflowStep.getAutoSign(), workflowStep.getAllSignToComplete(), workflowStep.getSignType(), recipientEmails, externalUsersInfos);
            signBook.getLiveWorkflow().getLiveWorkflowSteps().add(newWorkflowStep);
        }
        if(!(workflow instanceof DefaultWorkflow)) {
            signBook.getLiveWorkflow().setWorkflow(workflow);
        }
        dispatchSignRequestParams(signBook);
    }

    public void dispatchSignRequestParams(SignBook signBook) {
        for(SignRequest signRequest : signBook.getSignRequests()) {
            if(signRequest.getSignRequestParams().size() > 0) {
                int i = 0;
                for (LiveWorkflowStep liveWorkflowStep : signBook.getLiveWorkflow().getLiveWorkflowSteps()) {
                    if (liveWorkflowStep.getWorkflowStep() != null) {
                        WorkflowStep workflowStep = workflowStepService.getById(liveWorkflowStep.getWorkflowStep().getId());
                        if (!liveWorkflowStep.getSignType().equals(SignType.hiddenVisa)) {
                            if(workflowStep.getSignRequestParams().size() > 0) {
                                for (SignRequestParams signRequestParams : signRequest.getSignRequestParams()) {
                                        for(SignRequestParams signRequestParams1 : workflowStep.getSignRequestParams()) {
                                            if(signRequestParams1.getSignPageNumber().equals(signRequestParams.getSignPageNumber())
                                                    && signRequestParams1.getxPos().equals(signRequestParams.getxPos())
                                                    && signRequestParams1.getyPos().equals(signRequestParams.getyPos())) {
                                                liveWorkflowStep.getSignRequestParams().add(signRequestParams);
                                            }
                                        }
                                }
                            } else {
                                if(signRequest.getSignRequestParams().size() > i) {
                                    liveWorkflowStep.getSignRequestParams().add(signRequest.getSignRequestParams().get(i));
                                }
                            }
                        }
                    } else {
                        signRequest.getParentSignBook().getLiveWorkflow().getLiveWorkflowSteps().get(0).getSignRequestParams().addAll(signRequest.getSignRequestParams());
                    }
                    i++;
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

    public void rename(Long id, String name) {
        Workflow workflow = getById(id);
        workflow.setDescription(name);
    }

    @Transactional
    public void addViewers(Long id, List<String> recipientsCCEmails) {
        Workflow workflow = getById(id);
        if(recipientsCCEmails != null && recipientsCCEmails.size() > 0) {
            workflow.getViewers().clear();
            for (String recipientsEmail : recipientsCCEmails) {
                User user = userService.getUserByEmail(recipientsEmail);
                workflow.getViewers().add(user);
            }
        } else {
            workflow.getViewers().clear();
        }
    }
}

