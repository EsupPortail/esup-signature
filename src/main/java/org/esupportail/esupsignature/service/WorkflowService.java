package org.esupportail.esupsignature.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.esupportail.esupsignature.dto.json.RecipientWsDto;
import org.esupportail.esupsignature.dto.json.WorkflowStepDto;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.*;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.FormRepository;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.repository.WorkflowRepository;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessFactoryService;
import org.esupportail.esupsignature.service.interfaces.listsearch.UserListService;
import org.esupportail.esupsignature.service.interfaces.workflow.DefaultWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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

    private final  List<Workflow> workflows;
    private final  WorkflowRepository workflowRepository;
    private final  WorkflowStepService workflowStepService;
    private final  LiveWorkflowService liveWorkflowService;
    private final  LiveWorkflowStepService liveWorkflowStepService;
    private final  FsAccessFactoryService fsAccessFactoryService;
    private final  UserService userService;
    private final  UserShareService userShareService;
    private final UserPropertieService userPropertieService;
    private final  TargetService targetService;
    private final  FieldService fieldService;
    private final  SignBookRepository signBookRepository;
    private final  UserListService userListService;
    private final  FormRepository formRepository;
    private final  ObjectMapper objectMapper;
    private final  RecipientService recipientService;

    public WorkflowService(List<Workflow> workflows, WorkflowRepository workflowRepository, WorkflowStepService workflowStepService, LiveWorkflowService liveWorkflowService, LiveWorkflowStepService liveWorkflowStepService, FsAccessFactoryService fsAccessFactoryService, UserService userService, UserShareService userShareService, UserPropertieService userPropertieService, TargetService targetService, FieldService fieldService, SignBookRepository signBookRepository, UserListService userListService, FormRepository formRepository, ObjectMapper objectMapper, RecipientService recipientService) {
        this.workflows = workflows;
        this.workflowRepository = workflowRepository;
        this.workflowStepService = workflowStepService;
        this.liveWorkflowService = liveWorkflowService;
        this.liveWorkflowStepService = liveWorkflowStepService;
        this.fsAccessFactoryService = fsAccessFactoryService;
        this.userService = userService;
        this.userShareService = userShareService;
        this.userPropertieService = userPropertieService;
        this.targetService = targetService;
        this.fieldService = fieldService;
        this.signBookRepository = signBookRepository;
        this.userListService = userListService;
        this.formRepository = formRepository;
        this.objectMapper = objectMapper;
        this.recipientService = recipientService;
    }

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
    public boolean checkWorkflowManageRights(Long id, String userEppn) {
        Workflow workflow = getById(id);
        User user = userService.getByEppn(userEppn);
        return workflow.getManagers().contains(user.getEmail()) || user.getRoles().stream().anyMatch(role -> workflow.getDashboardRoles().contains(role));
    }

    @Transactional
    public void copyClassWorkflowsIntoDatabase() throws EsupSignatureRuntimeException {
        logger.info("Checking Workflow classes...");
        for (Workflow classWorkflow : getClassesWorkflows()) {
            logger.debug("workflow class found : " + classWorkflow.getName());
            if (!isWorkflowExist(classWorkflow.getName(), "system")) {
                logger.info("create " + classWorkflow.getName() + " on database : ");
                Workflow newWorkflow = createWorkflow(classWorkflow.getName(), classWorkflow.getDescription(), userService.getSystemUser());
                newWorkflow.setFromCode(true);
            } else {
                logger.debug("update " + classWorkflow.getName() + " on database");
                Workflow toUpdateWorkflow = workflowRepository.findByName(classWorkflow.getName());
                toUpdateWorkflow.setPublicUsage(classWorkflow.getPublicUsage());
                toUpdateWorkflow.getRoles().clear();
                toUpdateWorkflow.getRoles().addAll(classWorkflow.getRoles());
                toUpdateWorkflow.setDescription(classWorkflow.getDescription());
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
        logger.info("Checking Workflow classes done");
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
    public Workflow addStepToWorkflow(Long id, WorkflowStepDto step, User user) {
        Workflow workflow;
        if (id != null && id != -1) {
            workflow = getById(id);
        } else {
            workflow = createWorkflow(user);
        }
        if(workflow.getCreateBy().getEppn().equals(user.getEppn())) {
            logger.info("add new workflow step to Workflow " + workflow.getId());
            WorkflowStep workflowStep = workflowStepService.createWorkflowStep("", step.getAllSignToComplete(), step.getSignType(), step.getChangeable(), step.getRecipients().toArray(RecipientWsDto[]::new));
            if(step.getMultiSign() != null) {
                workflowStep.setMultiSign(step.getMultiSign());
            }
            if(step.getSingleSignWithAnnotation() != null) {
                workflowStep.setSingleSignWithAnnotation(step.getSingleSignWithAnnotation());
            }
            workflowStep.setSealVisa(step.getSealVisa());
            workflow.getWorkflowSteps().add(workflowStep);
            userPropertieService.createUserPropertieFromMails(user, Collections.singletonList(step));
        }
        return workflow;
    }

    @Transactional
    public Workflow createWorkflow(String title, String description, User user) throws EsupSignatureRuntimeException {
        String name = user.getEppn() + description;
        if(StringUtils.hasText(title)) {
            if (userService.getSystemUser().equals(user)) {
                name = title;
            } else {
                name = user.getEppn() + title.substring(0, 1).toUpperCase() + title.toLowerCase().substring(1);
                name = name.replaceAll("[^a-zA-Z0-9]", "");
            }
        }
        if (!isWorkflowExist(name, user.getEppn())) {
            Workflow workflow = new Workflow();
            workflow.setName(name);
            workflow.setDescription(description);
            workflow.setToken(generateToken(title));
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
    public Workflow getByIdOrToken(String idStr) {
        Long idLong = null;
        try {
            idLong = Long.valueOf(idStr);
        } catch (NumberFormatException ignored) {}
        return workflowRepository.findByIdOrToken(idLong, idStr);
    }

    @Transactional
    public void updateWorkflow(String userEppn, Long id, String name, List<String> recipientsCCEmails) {
        User user = userService.getByEppn(userEppn);
        Workflow workflow = getById(id);
        if(workflow.getCreateBy().equals(user)) {
            workflow.setName(name);
            workflow.setDescription(name);
            workflow.setToken(generateToken(name));
            addViewers(id, recipientsCCEmails);
        } else {
            throw new EsupSignatureRuntimeException("You are not authorized to update this workflow");
        }
    }


    @Transactional
    public void computeWorkflow(List<WorkflowStepDto> steps, SignBook signBook) {
        for (WorkflowStepDto step : steps) {
            LiveWorkflowStep newWorkflowStep = liveWorkflowStepService.createLiveWorkflowStep(signBook, null, step);
            signBook.getLiveWorkflow().getLiveWorkflowSteps().add(newWorkflowStep);
        }
        signBook.getLiveWorkflow().setCurrentStep(signBook.getLiveWorkflow().getLiveWorkflowSteps().get(0));
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
                if(!computeForDisplay) {
                    int finalStep = stepNumber;
                    Optional<WorkflowStepDto> step = steps.stream().filter(s -> s.getStepNumber() == finalStep).findFirst();
                    if(step.isPresent()) {
                        if (workflowStep.getChangeable() != null && workflowStep.getChangeable()) {
                            List<RecipientWsDto> recipients = step.get().getRecipients();
                            List<User> users = this.getFavoriteRecipientEmail(stepNumber, recipients);
                            if (!recipients.isEmpty()) {
                                workflowStep.getUsers().clear();
                                for (User oneUser : users) {
                                    workflowStep.getUsers().add(oneUser);
                                }
                            }
                        }
                        if (step.get().getAllSignToComplete() != null) {
                            workflowStep.setAllSignToComplete(step.get().getAllSignToComplete());
                        }
                        if (step.get().getSignType() != null) {
                            workflowStep.setSignType(step.get().getSignType());
                        }
                        if (step.get().getChangeable() != null) {
                            workflowStep.setChangeable(step.get().getChangeable());
                        }
                        if (step.get().getRepeatable() != null) {
                            workflowStep.setRepeatable(step.get().getRepeatable());
                        }
                        if (step.get().getMultiSign() != null) {
                            workflowStep.setMultiSign(step.get().getMultiSign());
                        }
                        if (step.get().getSingleSignWithAnnotation() != null) {
                            workflowStep.setSingleSignWithAnnotation(step.get().getSingleSignWithAnnotation());
                        }
                        if (step.get().getAttachmentAlert() != null) {
                            workflowStep.setAttachmentAlert(step.get().getAttachmentAlert());
                        }
                        if (step.get().getAttachmentRequire() != null) {
                            workflowStep.setAttachmentRequire(step.get().getAttachmentRequire());
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

    public List<User> getFavoriteRecipientEmail(int stepNumber, List<RecipientWsDto> recipients) {
        List<User> users = new ArrayList<>();
        if (recipients != null && !recipients.isEmpty()) {
            recipients = recipients.stream().filter(r -> r.getStep().equals(stepNumber)).collect(Collectors.toList());
            for (RecipientWsDto recipient : recipients) {
                String userEmail = recipient.getEmail();
                for(String realUserEmail : recipientService.getAllRecipientsEmails(Collections.singletonList(new RecipientWsDto(userEmail)))) {
                    User user = userService.getUserByEmail(realUserEmail);
                    if(StringUtils.hasText(recipient.getPhone())) {
                        userService.updatePhone(user.getEppn(), recipient.getPhone());
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
                if(oneUser.getCurrentReplaceByUser() != null) {
                        workflowStep.getUsers().remove(oneUser);
                        workflowStep.getUsers().add(oneUser.getCurrentReplaceByUser());
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
            if (signBooks.stream().allMatch(signBook -> signBook.getStatus().equals(SignRequestStatus.uploading) || signBook.getStatus().equals(SignRequestStatus.draft) || signBook.getDeleted())) {
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

    @Transactional
    public List<Workflow> getWorkflowsByDisplayWorkflowType(DisplayWorkflowType displayWorkflowType) {
        if (displayWorkflowType == null) {
            displayWorkflowType = DisplayWorkflowType.system;
        }
        List<Workflow> workflows = new ArrayList<>();
        if(DisplayWorkflowType.system.equals(displayWorkflowType)) {
            workflows.addAll(getWorkflowsBySystemUser().stream().filter(workflow -> workflow.getFromCode() == null || !workflow.getFromCode()).toList());
        } else if(DisplayWorkflowType.classes.equals(displayWorkflowType)) {
            workflows.addAll(getClassesWorkflows());
        } else if(DisplayWorkflowType.all.equals(displayWorkflowType)) {
            workflows.addAll(getAllWorkflows());
        } else if(DisplayWorkflowType.users.equals(displayWorkflowType)) {
            workflows.addAll(getAllWorkflows());
            workflows.removeAll(getClassesWorkflows());
            workflows.removeAll(getWorkflowsBySystemUser());
        }
        return workflows.stream()
                .sorted(Comparator.comparing(
                        w -> Optional.ofNullable(w.getDescription()).orElse("").toLowerCase(),
                        Comparator.naturalOrder()))
                .collect(Collectors.toList());
    }

    @Transactional
    public Workflow update(Workflow workflow, User user, String[] types, Set<String> managers, String authUserEppn) {
        Workflow workflowToUpdate = getById(workflow.getId());
        if(managers != null && !managers.isEmpty()) {
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
        workflowToUpdate.setToken(generateToken(workflow.getToken()));
        workflowToUpdate.setDocumentsSourceUri(workflow.getDocumentsSourceUri());
        workflowToUpdate.setDescription(workflow.getDescription());
        workflowToUpdate.setNamingTemplate(workflow.getNamingTemplate());
        workflowToUpdate.setTargetNamingTemplate(workflow.getTargetNamingTemplate());
        workflowToUpdate.setSealAtEnd(workflow.getSealAtEnd());
        workflowToUpdate.setOwnerSystem(workflow.getOwnerSystem());
        workflowToUpdate.setDisableDeleteByCreator(workflow.getDisableDeleteByCreator());
        workflowToUpdate.setForbidDownloadsBeforeEnd(workflow.getForbidDownloadsBeforeEnd());
        workflowToUpdate.setScanPdfMetadatas(workflow.getScanPdfMetadatas());
        workflowToUpdate.setSendAlertToAllRecipients(workflow.getSendAlertToAllRecipients());
        workflowToUpdate.setExternalCanEdit(workflow.getExternalCanEdit());
        workflowToUpdate.setAuthorizeClone(workflow.getAuthorizeClone());
        User manager = userService.getByEppn(authUserEppn);
        if(!manager.getRoles().contains("ROLE_ADMIN") && workflowToUpdate.getManagerRole() != null && manager.getManagersRoles().contains(workflowToUpdate.getManagerRole())) {
            workflowToUpdate.setPublicUsage(false);
            workflowToUpdate.getRoles().clear();
            if(workflow.getRoles().size() == 1 && workflow.getRoles().contains(workflowToUpdate.getManagerRole())) {
                workflowToUpdate.getRoles().addAll(workflow.getRoles());
            }
        } else {
            workflowToUpdate.setPublicUsage(workflow.getPublicUsage());
            workflowToUpdate.getRoles().clear();
            workflowToUpdate.getRoles().addAll(workflow.getRoles());
        }
        workflowToUpdate.getDashboardRoles().clear();
        workflowToUpdate.getDashboardRoles().addAll(workflow.getDashboardRoles());
        workflowToUpdate.setUpdateBy(user.getEppn());
        workflowToUpdate.setUpdateDate(new Date());
        workflowToUpdate.setMessage(workflow.getMessage());
        workflowToUpdate.setMailFrom(workflow.getMailFrom());
        workflowToUpdate.setDisableEmailAlerts(workflow.getDisableEmailAlerts());
        workflowToUpdate.setSignRequestParamsDetectionPattern(workflow.getSignRequestParamsDetectionPattern());
        workflowToUpdate.setStartArchiveDate(workflow.getStartArchiveDate());
        workflowToUpdate.setArchiveTarget(workflow.getArchiveTarget());
        workflowToUpdate.getExternalAuths().clear();
        workflowToUpdate.setExternalAuths(workflow.getExternalAuths());
        workflowRepository.save(workflowToUpdate);
        return workflowToUpdate;
    }

    private String generateToken(String token) {
        token = token.replaceAll("[\\\\/:*?\"<>|]", "_").replace(" ", "_");
        String baseToken = token.replaceAll("_\\d+$", "");

        List<Workflow> workflows = workflowRepository.findByTokenStartingWith(baseToken);
        if (!workflows.isEmpty()) {
            return baseToken + "_" + workflows.size();
        }
        return baseToken;
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
    public boolean addTarget(Long id, String documentsTargetUri, Boolean sendDocument, Boolean sendRepport, Boolean sendAttachment, Boolean sendZip, Boolean cascade) throws EsupSignatureFsException {
        Workflow workflow = getById(id);
        Target target;
        if(documentsTargetUri.equals("mailto:")) {
            target = targetService.createTarget("mailto:", sendDocument, sendRepport, sendAttachment, sendZip);
            workflow.getTargets().add(target);
        } else {
            DocumentIOType targetType = fsAccessFactoryService.getPathIOType(documentsTargetUri);
            if (!targetType.equals("mail") || workflow.getTargets().stream().map(Target::getTargetUri).noneMatch(tt -> tt.contains("mailto"))) {
                target = targetService.createTarget(documentsTargetUri, sendDocument, sendRepport, sendAttachment, sendZip);
                workflow.getTargets().add(target);
            } else {
                return false;
            }
        }
        if(cascade) {
            List<SignBook> signBooks = signBookRepository.findByLiveWorkflowWorkflow(workflow);
            for (SignBook signBook : signBooks) {
                if (signBook.getArchiveStatus().equals(ArchiveStatus.none)) {
                    signBook.getLiveWorkflow().getTargets().add(targetService.createTarget(documentsTargetUri, sendDocument, sendRepport, sendAttachment, sendZip));
                }
            }
        }
        return true;

    }

    @Transactional
    public void deleteTarget(Long id, Long targetId, Boolean cascade) {
        Workflow workflow = getById(id);
        Target target = targetService.getById(targetId);
        workflow.getTargets().remove(target);
        targetService.delete(target);
        if(cascade) {
            List<SignBook> signBooks = signBookRepository.findByLiveWorkflowWorkflow(workflow);
            for (SignBook signBook : signBooks) {
                if (signBook.getArchiveStatus().equals(ArchiveStatus.none)) {
                    LiveWorkflow liveWorkflow = signBook.getLiveWorkflow();
                    if (liveWorkflow.getTargets() != null && !liveWorkflow.getTargets().isEmpty()) {
                        List<Target> targets = liveWorkflow.getTargets().stream().filter(t -> t.getTargetUri().equalsIgnoreCase(target.getTargetUri())).toList();
                        for(Target target1 : targets) {
                            liveWorkflow.getTargets().remove(target1);
                            targetService.delete(target1);
                        }
                    }
                }
            }
        }
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
        return workflowRepository.findWorkflowByManagersIn(user.getEmail(), user.getRoles());
    }

    @Transactional
    public InputStream getJsonWorkflowSetup(Long id) throws IOException {
        Workflow workflow = getById(id);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        objectMapper.writer().writeValue(outputStream, workflow);
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    @Transactional
    public void setWorkflowSetupFromJson(Long id, InputStream inputStream, String authUserEppn) throws IOException, EsupSignatureRuntimeException {
        Workflow workflow = getById(id);
        String savedName = workflow.getName();
        String savedDescription = workflow.getDescription();
        Workflow workflowSetup = objectMapper.readValue(inputStream.readAllBytes(), Workflow.class);
        workflowSetup.setId(id);
        workflow.getWorkflowSteps().clear();
        for(WorkflowStep workflowStepSetup : workflowSetup.getWorkflowSteps()) {
            Optional<WorkflowStep> optionalWorkflowStep = workflow.getWorkflowSteps().stream().filter(workflowStep1 -> workflowStep1.getId().equals(workflowStepSetup.getId())).findFirst();
            if(optionalWorkflowStep.isPresent()) {
                WorkflowStep workflowStep = optionalWorkflowStep.get();
                workflowStepService.updateStep(workflowStep.getId(), workflowStepSetup.getSignType(), workflowStepSetup.getDescription(), workflowStepSetup.getChangeable(), workflowStepSetup.getRepeatable(), workflowStepSetup.getMultiSign(), workflowStepSetup.getSingleSignWithAnnotation(), workflowStepSetup.getAllSignToComplete(), workflowStepSetup.getMaxRecipients(), workflowStepSetup.getAttachmentAlert(), workflowStepSetup.getAttachmentRequire(), false, null, workflowStepSetup.getMinSignLevel(), workflowStepSetup.getMaxSignLevel(), workflowStepSetup.getSealVisa());
            } else {
                List<RecipientWsDto> recipients = new ArrayList<>();
                for(User user : workflowStepSetup.getUsers()) {
                    recipients.add(new RecipientWsDto(user.getEmail()));
                }
                WorkflowStep newWorkflowStep = workflowStepService.createWorkflowStep(workflowSetup.getName(), workflowStepSetup.getAllSignToComplete(), workflowStepSetup.getSignType(), workflowStepSetup.getChangeable(), recipients.toArray(RecipientWsDto[]::new));
                workflowStepService.updateStep(newWorkflowStep.getId(), workflowStepSetup.getSignType(), workflowStepSetup.getDescription(), workflowStepSetup.getChangeable(), workflowStepSetup.getRepeatable(), workflowStepSetup.getMultiSign(), workflowStepSetup.getSingleSignWithAnnotation(), workflowStepSetup.getAllSignToComplete(), workflowStepSetup.getMaxRecipients(), workflowStepSetup.getAttachmentAlert(), workflowStepSetup.getAttachmentRequire(), false, null, workflowStepSetup.getMinSignLevel(), workflowStepSetup.getMaxSignLevel(), workflowStepSetup.getSealVisa());
                workflow.getWorkflowSteps().add(newWorkflowStep);
            }
        }
        workflow.getTargets().clear();
        update(workflowSetup, workflowSetup.getCreateBy(), null, workflowSetup.getManagers(), authUserEppn);
        for(Target target : workflowSetup.getTargets()) {
            Target newTarget = targetService.createTarget(target.getTargetUri(), target.getSendDocument(), target.getSendReport(), target.getSendAttachment(), target.getSendZip());
            workflow.getTargets().add(newTarget);
        }
        workflow.setName(savedName);
        workflow.setDescription(savedDescription);
    }

    @Transactional
    public void rename(Long id, String name) {
        Workflow workflow = getById(id);
        workflow.setDescription(name);
    }

    @Transactional
    public void addViewers(Long id, List<String> recipientsCCEmails) {
        Workflow workflow = getById(id);
        if(recipientsCCEmails != null && !recipientsCCEmails.isEmpty()) {
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
    public String getByIdJson(String idStr) throws JsonProcessingException {
        Long idLong = null;
        try {
            idLong = Long.valueOf(idStr);
        } catch (NumberFormatException ignored) {}
        return objectMapper.writeValueAsString(workflowRepository.getByIdJson(idLong, idStr));
    }

    @Transactional
    public String getSignRequestById(String idStr) throws JsonProcessingException {
        Long idLong = null;
        try {
            idLong = Long.valueOf(idStr);
        } catch (NumberFormatException ignored) {}
        List<SignBook> signBooks = signBookRepository.findByWorkflowIdOrToken(idLong, idStr);
        return objectMapper.writeValueAsString(signBooks.stream().map(SignBook::getSignRequests).collect(Collectors.toList()));
    }

    @Transactional
    public String getHelpMessage(String userEppn, Workflow workflow) {
        User user = userService.getByEppn(userEppn);
        String messsage = null;
        boolean sendMessage = true;
        if(user.getFormMessages() != null) {
            String[] formMessages = user.getFormMessages().split(" ");
            if(Arrays.asList(formMessages).contains(workflow.getId().toString())) {
                sendMessage = false;
            }
        }
        if(sendMessage && StringUtils.hasText(workflow.getMessage())) {
            messsage = workflow.getMessage();
        }
        return messsage;
    }

    @Async
    @EventListener(ContextRefreshedEvent.class)
    @Transactional
    public void onApplicationEvent(ContextRefreshedEvent event) {
        copyClassWorkflowsIntoDatabase();
    }

    @Transactional
    public void importWorkflow(SignBook signBook, Workflow workflow, List<WorkflowStepDto> steps) {
        logger.info("try import workflow steps in signBook " + signBook.getSubject() + " - " + signBook.getId());
        int i = 0;
        for (WorkflowStep workflowStep : workflow.getWorkflowSteps()) {
            i++;
            WorkflowStepDto step = new WorkflowStepDto();
            int finalI = i;
            Optional<WorkflowStepDto> optionalStep = steps.stream().filter(s -> s.getStepNumber() == finalI).findFirst();
            if(optionalStep.isPresent()) step = optionalStep.get();
            step.getRecipients().removeIf(r -> r.getEmail().equals("creator"));
            for (User user : workflowStep.getUsers()) {
                if (user.equals(userService.getCreatorUser())) {
                    user = signBook.getCreateBy();
                }
                recipientService.addRecipientInStep(step, user.getEmail());
            }
            step.setRepeatable(workflowStep.getRepeatable());
            step.setRepeatableSignType(workflowStep.getRepeatableSignType());
            step.setMultiSign(workflowStep.getMultiSign());
            step.setSingleSignWithAnnotation(workflowStep.getSingleSignWithAnnotation());
            step.setAutoSign(workflowStep.getAutoSign());
            step.setAllSignToComplete(workflowStep.getAllSignToComplete());
            step.setSignType(workflowStep.getSignType());
            step.setAttachmentAlert(workflowStep.getAttachmentAlert());
            step.setAttachmentRequire(workflowStep.getAttachmentRequire());
            step.setSignLevel(workflowStep.getMinSignLevel());
            LiveWorkflowStep newWorkflowStep = liveWorkflowStepService.createLiveWorkflowStep(signBook, workflowStep, step);
            signBook.getLiveWorkflow().getLiveWorkflowSteps().add(newWorkflowStep);
        }
        if(!(workflow instanceof DefaultWorkflow)) {
            signBook.getLiveWorkflow().setWorkflow(workflow);
        }
    }

    @Transactional
    public void updateSendAlertToAllRecipients(Long id, Boolean sendAlertToAllRecipients) {
        Workflow workflow = getById(id);
        workflow.setSendAlertToAllRecipients(sendAlertToAllRecipients);
    }
}