package org.esupportail.esupsignature.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.esupportail.esupsignature.dto.RecipientWsDto;
import org.esupportail.esupsignature.dto.WorkflowStepDto;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.*;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.FormRepository;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.repository.WorkflowRepository;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessFactoryService;
import org.esupportail.esupsignature.service.interfaces.listsearch.UserListService;
import org.esupportail.esupsignature.service.interfaces.workflow.DefaultWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.util.StringUtils;

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

    @Resource
    private FormRepository formRepository;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private RecipientService recipientService;

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
            WorkflowStep workflowStep = null;
            try {
                workflowStep = workflowStepService.createWorkflowStep("Ma signature", false, null, false, Collections.singletonList(new RecipientWsDto(creator.getEmail())).toArray(RecipientWsDto[]::new));
            } catch (EsupSignatureRuntimeException e) {
                logger.warn(e.getMessage());
            }
            workflow.getWorkflowSteps().add(workflowStep);
            workflowRepository.save(workflow);
        }
    }

    @Transactional
    public void copyClassWorkflowsIntoDatabase() throws EsupSignatureRuntimeException {
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
                            toUdateWorkflowStep.setSignType(generatedWorkflowStep.getSignType());
                        } else {
                            WorkflowStep newWorkflowStep = workflowStepService.createWorkflowStep(generatedWorkflowStep.getName(), generatedWorkflowStep.getAllSignToComplete(), generatedWorkflowStep.getSignType(), generatedWorkflowStep.getChangeable());
                            for (User user : generatedWorkflowStep.getUsers()) {
                                userService.save(user);
                                newWorkflowStep.getUsers().add(user);
                            }
                            newWorkflowStep.setDescription(generatedWorkflowStep.getDescription());
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
        workflowRepository.deleteAll(toRemoveWorkflows);
    }

    public boolean isWorkflowExist(String name, String userEppn) {
        return workflowRepository.countByNameAndCreateByEppn(name, userEppn) > 0;
    }

    public Workflow createWorkflow(User user) {
        Workflow workflow;
        workflow = new Workflow();
        workflow.setCreateDate(new Date());
        workflow.setCreateBy(user);
        workflowRepository.save(workflow);
        return workflow;
    }

    @Transactional
    public Workflow addStepToWorkflow(Long id, SignType signType, Boolean allSignToComplete, Boolean changeable, WorkflowStepDto step, User user) throws EsupSignatureRuntimeException {
        Workflow workflow;
        if (id != null && id != -1) {
            workflow = getById(id);
        } else {
            workflow = createWorkflow(user);
        }
        if(workflow.getCreateBy().getEppn().equals(user.getEppn())) {
            if(step.getRecipients() != null && !step.getRecipients().isEmpty()) {
                logger.info("add new workflow step to Workflow " + workflow.getId());
                WorkflowStep workflowStep = workflowStepService.createWorkflowStep("", allSignToComplete, signType, changeable, step.getRecipients().toArray(RecipientWsDto[]::new));
                workflow.getWorkflowSteps().add(workflowStep);
                userPropertieService.createUserPropertieFromMails(user, Collections.singletonList(step));
            }
        }
        return workflow;
    }

    @Transactional
    public Workflow createWorkflow(String title, String description, User user) throws EsupSignatureRuntimeException {
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
            throw new EsupSignatureRuntimeException("already exist");
        }
    }

    public Set<Workflow> getWorkflowsByUser(String userEppn, String authUserEppn) {
        User authUser = userService.getByEppn(authUserEppn);
        Set<Workflow> authorizedWorkflows = new HashSet<>();
        for (String role : userService.getRoles(userEppn)) {
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
        workflows = workflows.stream().filter(workflow -> !workflow.getAuthorizedShareTypes().isEmpty()).collect(Collectors.toList());
        return workflows;
    }

    @Transactional
    public List<Workflow> getAllWorkflows() {
        return workflowRepository.findAll();
    }

    @Transactional
    public String getAllWorkflowsJson() throws JsonProcessingException {
        return objectMapper.writeValueAsString(workflowRepository.findAllJson());
    }

    public Workflow getWorkflowByClassName(String className) {
        for (Workflow workflow : workflows) {
            if (className.equals(workflow.getName())) {
                return workflow;
            }
        }
        return null;
    }

    @Transactional
    public Workflow getById(Long id) {
        return workflowRepository.findById(id).orElse(null);
    }

    @Transactional
    public Workflow updateWorkflow(String userEppn, Long id, String name, List<String> recipientsCCEmails) {
        User user = userService.getByEppn(userEppn);
        Workflow workflow = getById(id);
        workflow.setCreateBy(user);
        workflow.setName(name);
        workflow.setDescription(name);
        workflow.setTitle(name.replaceAll("[\\\\/:*?\"<>|]", "_").replace(" ", "_"));
        addViewers(id, recipientsCCEmails);
        return workflow;
    }

    @Transactional
    public Workflow computeWorkflow(Long workflowId, List<WorkflowStepDto> steps, String userEppn, boolean computeForDisplay) throws EsupSignatureRuntimeException {
        try {
            Workflow modelWorkflow = getById(workflowId);
            if (modelWorkflow.getFromCode() != null && modelWorkflow.getFromCode()) {
                DefaultWorkflow defaultWorkflow = (DefaultWorkflow) getWorkflowByClassName(modelWorkflow.getName());
                List<RecipientWsDto> recipients = steps.stream().map(WorkflowStepDto::getRecipients).flatMap(List::stream).toList();
                defaultWorkflow.fillWorkflowSteps(modelWorkflow, recipients);
            }
            int stepNumber = 1;
            for (WorkflowStep workflowStep : modelWorkflow.getWorkflowSteps()) {
                entityManager.detach(workflowStep);
                replaceStepSystemUsers(userEppn, workflowStep);
                if (workflowStep.getChangeable() != null && workflowStep.getChangeable()) {
                    if(!computeForDisplay) {
                        int finalStep = stepNumber;
                        Optional<WorkflowStepDto> step = steps.stream().filter(s -> s.getStepNumber() == finalStep).findFirst();
                        if(step.isPresent()) {
                            List<RecipientWsDto> recipients = step.get().getRecipients();
                            List<User> users = this.getFavoriteRecipientEmail(stepNumber, recipients);
                            if (!recipients.isEmpty()) {
                                workflowStep.getUsers().clear();
                                for (User oneUser : users) {
                                    workflowStep.getUsers().add(oneUser);
                                }
                            }
                            if (step.get().getAllSignToComplete()) {
                                workflowStep.setAllSignToComplete(true);
                            }
                            if (step.get().getSignType() != null) {
                                workflowStep.setSignType(step.get().getSignType());
                            }
                        }
                    }
                }
                stepNumber++;
            }
            entityManager.detach(modelWorkflow);
            return modelWorkflow;
        } catch (Exception e) {
            throw new EsupSignatureRuntimeException("compute workflow error", e);
        }
    }

    public List<User> getFavoriteRecipientEmail(long stepNumber, List<RecipientWsDto> recipients) {
        List<User> users = new ArrayList<>();
        if (recipients != null && !recipients.isEmpty()) {
            recipients = recipients.stream().filter(r -> r.getStep().equals(stepNumber)).collect(Collectors.toList());
            for (RecipientWsDto recipient : recipients) {
                String userEmail = recipient.getEmail();
                for(String realUserEmail : recipientService.getCompleteRecipientList(Collections.singletonList(new RecipientWsDto(userEmail)))) {
                    User user = userService.getUserByEmail(realUserEmail);
                    if(StringUtils.hasText(recipient.getPhone())) {
                        user.setPhone(recipient.getPhone());
                    }
                    if(StringUtils.hasText(recipient.getName())) {
                        user.setName(recipient.getName());
                    }
                    if(StringUtils.hasText(recipient.getFirstName())) {
                        user.setFirstname(recipient.getFirstName());
                    }
                    users.add(user);
                }
            }
        }
        return users;
    }

    public void replaceStepSystemUsers(String userEppn, WorkflowStep workflowStep) throws EsupSignatureRuntimeException {
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

    @Transactional
    public void delete(Long workflowId) throws EsupSignatureRuntimeException {
        Workflow workflow = getById(workflowId);
        List<SignBook> signBooks = signBookRepository.findByLiveWorkflowWorkflow(workflow);
        List<Form> forms = formRepository.findByWorkflowIdEquals(workflow.getId());
        if(forms.isEmpty()) {
            if (signBooks.stream().allMatch(signBook -> signBook.getStatus() == SignRequestStatus.uploading || signBook.getStatus() == SignRequestStatus.draft || signBook.getStatus() == SignRequestStatus.deleted)) {
                List<LiveWorkflow> liveWorkflows = liveWorkflowService.getByWorkflow(workflow);
                for (LiveWorkflow liveWorkflow : liveWorkflows) {
                    liveWorkflow.setWorkflow(null);
                    liveWorkflow.getLiveWorkflowSteps().forEach(lws -> lws.setWorkflowStep(null));
                }
                for (WorkflowStep workflowStep : workflow.getWorkflowSteps()) {
                    List<Field> fields = fieldService.getFieldsByWorkflowStep(workflowStep);
                    for (Field field : fields) {
                        field.getWorkflowSteps().remove(workflowStep);
                    }
                }
                for(UserShare userShare : userShareService.getByWorkflowId(workflow.getId())) {
                    userShareService.delete(userShare);
                }
                workflowRepository.delete(workflow);
            } else {
                throw new EsupSignatureRuntimeException("Le circuit ne peut pas être supprimé car il est associé à des demandes");
            }
        } else {
            throw new EsupSignatureRuntimeException("Le circuit ne peut pas être supprimé car il associé au formulaire " + forms.get(0).getTitle());
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

    public Workflow update(Workflow workflow, User user, String[] types, Set<String> managers) {
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

    @Transactional
    public List<WorkflowStep> getWorkflowStepsFromSignRequest(SignRequest signRequest, String userEppn) throws EsupSignatureRuntimeException {
        List<WorkflowStep> workflowSteps = new ArrayList<>();
        if(signRequest.getParentSignBook().getLiveWorkflow().getWorkflow() != null) {
            Workflow workflow = computeWorkflow(signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getId(),null, userEppn, true);
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
        User user = userService.getByEppn(eppn);
        return workflowRepository.findWorkflowByManagersIn(Collections.singletonList(user.getEmail()));
    }

    @Transactional
    public InputStream getJsonWorkflowSetup(Long id) throws IOException {
        Workflow workflow = getById(id);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        objectMapper.writer().writeValue(outputStream, workflow);
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    @Transactional
    public void setWorkflowSetupFromJson(Long id, InputStream inputStream) throws IOException, EsupSignatureRuntimeException {
        Workflow workflow = getById(id);
        String savedName = workflow.getName();
        String savedTitle = workflow.getTitle();
        Workflow workflowSetup = objectMapper.readValue(inputStream.readAllBytes(), Workflow.class);
        workflow.getWorkflowSteps().clear();
        for(WorkflowStep workflowStepSetup : workflowSetup.getWorkflowSteps()) {
            Optional<WorkflowStep> optionalWorkflowStep = workflow.getWorkflowSteps().stream().filter(workflowStep1 -> workflowStep1.getId().equals(workflowStepSetup.getId())).findFirst();
            if(optionalWorkflowStep.isPresent()) {
                WorkflowStep workflowStep = optionalWorkflowStep.get();
                workflowStepService.updateStep(workflowStep.getId(), workflowStepSetup.getSignType(), workflowStepSetup.getDescription(), workflowStepSetup.getChangeable(), workflowStepSetup.getRepeatable(), workflowStepSetup.getMultiSign(), workflowStepSetup.getAllSignToComplete(), workflowStepSetup.getMaxRecipients(), workflowStepSetup.getAttachmentAlert(), workflowStepSetup.getAttachmentRequire(), false, null);
            } else {
                List<RecipientWsDto> recipients = new ArrayList<>();
                for(User user : workflowStepSetup.getUsers()) {
                    recipients.add(new RecipientWsDto(user.getEmail()));
                }
                WorkflowStepDto workflowStepDto = new WorkflowStepDto(workflowStepSetup.getSignType(), workflowStepSetup.getDescription(), recipients, workflowStepSetup.getChangeable(), workflowStepSetup.getMaxRecipients(), workflowStepSetup.getAllSignToComplete(), workflowStepSetup.getAttachmentRequire());

                WorkflowStep newWorkflowStep = workflowStepService.createWorkflowStep(workflowSetup.getName(), workflowStepSetup.getAllSignToComplete(), workflowStepSetup.getSignType(), workflowStepSetup.getChangeable(), recipients.toArray(RecipientWsDto[]::new));

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

    @Transactional
    public void anonymize(String userEppn, User anonymous) {
        User user = userService.getByEppn(userEppn);
        List<Workflow> workflows = workflowRepository.findAll();
        for(Workflow workflow : workflows) {
            if(workflow.getCreateBy().equals(user)) {
                workflow.setCreateBy(anonymous);
            }
            workflow.getViewers().removeIf(user1 -> user1.equals(user));
        }
    }

    @Transactional
    public String getByIdJson(Long id) throws JsonProcessingException {
        return objectMapper.writeValueAsString(workflowRepository.getByIdJson(id));
    }
}

