package org.esupportail.esupsignature.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.beanutils.BeanUtils;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.*;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.*;
import org.esupportail.esupsignature.service.file.FileService;
import org.esupportail.esupsignature.service.fs.FsAccessFactory;
import org.esupportail.esupsignature.service.fs.FsAccessService;
import org.esupportail.esupsignature.service.fs.FsFile;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.esupportail.esupsignature.service.workflow.DefaultWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
    private WorkflowStepRepository workflowStepRepository;

    @Resource
    private LiveWorkflowRepository liveWorkflowRepository;

    @Resource
    private SignBookRepository signBookRepository;

    @Resource
    private UserRepository userRepository;

    @Resource
    private FsAccessFactory fsAccessFactory;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private SignBookService signBookService;

    @Resource
    private UserService userService;

    @Resource
    private UserShareRepository userShareRepository;

    @Resource
    public UserPropertieService userPropertieService;

    @Resource
    private FileService fileService;

    @Resource
    private PdfService pdfService;

    @Resource
    private LiveWorkflowService liveWorkflowService;

    @PostConstruct
    public void initCreatorWorkflow() {
        User creator;
        if (userRepository.countByEppn("creator") == 0) {
            creator = userService.createUser("creator", "Createur de la demande", "", "creator", UserType.system);
        } else {
            creator = userRepository.findByEppn("creator").get(0);
        }
        if (workflowRepository.countByName("Ma signature") == 0) {
            Workflow workflow = new Workflow();
            workflow.setName("Ma signature");
            workflow.setDescription("Signature du créateur de la demande");
            workflow.setCreateDate(new Date());
            workflow.setCreateBy(userService.getSystemUser());
            workflow.setSourceType(DocumentIOType.none);
            workflow.setTargetType(DocumentIOType.none);
            WorkflowStep workflowStep = new WorkflowStep();
            workflowStep.setName("Ma signature");
            workflowStep.setSignType(SignType.certSign);
            workflowStep.getUsers().add(creator);
            workflowStepRepository.save(workflowStep);
            workflow.getWorkflowSteps().add(workflowStep);
            workflowRepository.save(workflow);
        }
    }

    public boolean preAuthorizeOwner(String name, User user) {
        Workflow workflow = workflowRepository.findByName(name);
        return user.equals(workflow.getCreateBy()) || workflow.getCreateBy().equals(userService.getSystemUser());
    }

    public boolean preAuthorizeOwner(Long id, User user) {
        Workflow workflow = workflowRepository.findById(id).get();
        return user.equals(workflow.getCreateBy()) || workflow.getCreateBy().equals(userService.getSystemUser());
    }

    @Transactional
    public void init() throws EsupSignatureException {
        for (Workflow classWorkflow : getClassesWorkflows()) {
            logger.info("workflow class found : " + classWorkflow.getName());
            if (!isWorkflowExist(classWorkflow.getClass().getSimpleName())) {
                logger.info("create " + classWorkflow.getName() + " on database : ");
                Workflow newWorkflow = createWorkflow(classWorkflow.getClass().getSimpleName(), classWorkflow.getDescription(), userService.getSystemUser(), classWorkflow.getPublicUsage());
                newWorkflow.setFromCode(true);
            } else {
                logger.info("update " + classWorkflow.getName() + " on database");
                Workflow toUpdateWorkflow = workflowRepository.findByName(classWorkflow.getClass().getSimpleName());
                toUpdateWorkflow.setPublicUsage(classWorkflow.getPublicUsage());
                toUpdateWorkflow.setRole(classWorkflow.getRole());
                toUpdateWorkflow.setDescription(classWorkflow.getDescription());
                toUpdateWorkflow.setTitle(classWorkflow.getTitle());
                toUpdateWorkflow.setSourceType(classWorkflow.getSourceType());
                toUpdateWorkflow.setDocumentsSourceUri(classWorkflow.getDocumentsSourceUri());
                toUpdateWorkflow.setTargetType(classWorkflow.getTargetType());
                toUpdateWorkflow.setDocumentsTargetUri(classWorkflow.getDocumentsTargetUri());
                toUpdateWorkflow.setAuthorizedShareTypes(classWorkflow.getAuthorizedShareTypes());
                toUpdateWorkflow.setScanPdfMetadatas(classWorkflow.getScanPdfMetadatas());
                toUpdateWorkflow.setManagers(classWorkflow.getManagers());
            }
        }
        for (Workflow workflow : workflowRepository.findByFromCodeIsTrue()) {
            try {
                List<WorkflowStep> generatedWorkflowSteps = ((DefaultWorkflow) getWorkflowByClassName(workflow.getName())).generateWorkflowSteps(userService.getSystemUser(), null);
                int i = 0;
                for (WorkflowStep generatedWorkflowStep : generatedWorkflowSteps) {
                    if (workflow.getWorkflowSteps().size() > i) {
                        WorkflowStep toUdateWorkflowStep = workflow.getWorkflowSteps().get(i);
                        toUdateWorkflowStep.setDescription(generatedWorkflowStep.getDescription());
                        toUdateWorkflowStep.getUsers().clear();
                        toUdateWorkflowStep.getUsers().addAll(generatedWorkflowStep.getUsers());
                    } else {
                        WorkflowStep newWorkflowStep = createWorkflowStep(generatedWorkflowStep.getName(), generatedWorkflowStep.getAllSignToComplete(), generatedWorkflowStep.getSignType());
                        for (User user : generatedWorkflowStep.getUsers()) {
                            userRepository.save(user);
                            newWorkflowStep.getUsers().add(user);
                        }
                        newWorkflowStep.setDescription(generatedWorkflowStep.getDescription());
                        newWorkflowStep.setChangeable(generatedWorkflowStep.getChangeable());
                        workflow.getWorkflowSteps().add(newWorkflowStep);
                        workflowRepository.save(workflow);
                    }
                    i++;
                }
            } catch (EsupSignatureUserException e) {
                logger.warn("already exist");
            }
        }
    }

    public boolean isWorkflowExist(String name) {
        return workflowRepository.countByName(name) > 0;
    }

    public Workflow createWorkflow(User user) {
        Workflow workflow;
        workflow = new Workflow();
        workflow.setCreateDate(new Date());
        workflow.setCreateBy(user);
        workflowRepository.save(workflow);
        return workflow;
    }

    public Workflow createWorkflow(String title, String description, User user, boolean external) throws EsupSignatureException {
        String name;
        if (userService.getSystemUser().equals(user)) {
            name = title;
        } else {
            name = user.getEppn().split("@")[0] + title.substring(0, 1).toUpperCase() + title.toLowerCase().substring(1);
            name = name.replaceAll("[^a-zA-Z0-9]", "");
        }
        if (!isWorkflowExist(name)) {
            Workflow workflow = new Workflow();
            workflow.setName(name);
            workflow.setDescription(description);
            workflow.setTitle(title.replaceAll("[\\\\/:*?\"<>|]", "_").replace(" ", "_"));
            workflow.setCreateBy(user);
            workflow.setCreateDate(new Date());
            workflow.getManagers().removeAll(Collections.singleton(""));
            Document model = null;
            workflow.setSourceType(DocumentIOType.none);
            workflow.setTargetType(DocumentIOType.none);
            workflowRepository.save(workflow);
            if (model != null) {
                model.setParentId(workflow.getId());
            }
            return workflow;
        } else {
            throw new EsupSignatureException("already exist");
        }
    }

    public Set<Workflow> getWorkflowsByUser(User user, User authUser) {
        List<Workflow> authorizedWorkflows = workflowRepository.findAuthorizedWorkflowByUser(user);
        Set<Workflow> workflows = new HashSet<>();
        if (user.equals(authUser)) {
            workflows.addAll(workflowRepository.findByCreateBy(user));
            workflows.addAll(workflowRepository.findByManagersContains(user.getEmail()));
            workflows.addAll(authorizedWorkflows);
        } else {
            for (UserShare userShare : userShareRepository.findByUserAndToUsersInAndShareTypesContains(user, Arrays.asList(authUser), ShareType.create)) {
                if (userShare.getWorkflow() != null && authorizedWorkflows.contains(userShare.getWorkflow())) {
                    workflows.add(userShare.getWorkflow());
                }
            }
        }
        workflows = workflows.stream().sorted(Comparator.comparing(Workflow::getCreateDate)).collect(Collectors.toCollection(LinkedHashSet::new));
        return workflows;
    }

    public int importFilesFromSource(Workflow workflow, User user) {
        List<FsFile> fsFiles = new ArrayList<>();
        int nbImportedFiles = 0;
        if (workflow.getSourceType() != null && !workflow.getSourceType().equals(DocumentIOType.none)) {
            logger.debug("retrieve from " + workflow.getSourceType() + " in " + workflow.getDocumentsSourceUri());
            FsAccessService fsAccessService = fsAccessFactory.getFsAccessService(workflow.getSourceType());
            if (fsAccessService != null) {
                fsAccessService.open();
                if (fsAccessService.cd(workflow.getDocumentsSourceUri()) == null) {
                    logger.info("create non existing folders : " + workflow.getDocumentsSourceUri());
                    fsAccessService.createFile("/", workflow.getDocumentsSourceUri(), "folder");
                    if (fsAccessService.getFile("/" + workflow.getDocumentsSourceUri() + "/signed") == null) {
                        fsAccessService.createFile("/" + workflow.getDocumentsSourceUri() + "/", "signed", "folder");
                    }
                }
                try {
                    fsFiles.addAll(fsAccessService.listFiles(workflow.getDocumentsSourceUri() + "/"));
                    if (fsFiles.size() > 0) {
                        for (FsFile fsFile : fsFiles) {
                            logger.info("adding file : " + fsFile.getName());
                            ByteArrayOutputStream baos = fileService.copyInputStream(fsFile.getInputStream());
                            Map<String, String> metadatas = pdfService.readMetadatas(new ByteArrayInputStream(baos.toByteArray()));
                            String documentName = fsFile.getName();
                            if (metadatas.get("Title") != null && !metadatas.get("Title").isEmpty()) {
                                documentName = metadatas.get("Title");
                            }
                            SignBook signBook = signBookService.createSignBook(workflow.getTitle(), documentName + "_" + nbImportedFiles, user, false);
                            signBook.getLiveWorkflow().setTargetType(workflow.getTargetType());
                            signBook.getLiveWorkflow().setDocumentsTargetUri(workflow.getDocumentsTargetUri());
                            SignRequest signRequest = signRequestService.createSignRequest(documentName, user);
                            if (fsFile.getCreateBy() != null && userRepository.countByEppn(fsFile.getCreateBy()) > 0) {
                                user = userRepository.findByEppn(fsFile.getCreateBy()).get(0);
                                user.setIp("127.0.0.1");
                            }
                            List<String> workflowRecipientsEmails = new ArrayList<>();
                            workflowRecipientsEmails.add(user.getEmail());
                            signRequestService.addDocsToSignRequest(signRequest, fileService.toMultipartFile(new ByteArrayInputStream(baos.toByteArray()), fsFile.getName(), fsFile.getContentType()));
                            signRequest.setParentSignBook(signBook);
                            signBook.getSignRequests().add(signRequest);

                            if (workflow.getScanPdfMetadatas()) {
                                String signType = metadatas.get("sign_type_default_val");
                                User creator = userService.createUserWithEppn(metadatas.get("Creator"));
                                if (creator != null) {
                                    signRequest.setCreateBy(creator);
                                    signBook.setCreateBy(creator);
                                } else {
                                    signRequest.setCreateBy(userService.getSystemUser());
                                    signBook.setCreateBy(userService.getSystemUser());
                                }
                                for (String metadataKey : metadatas.keySet()) {
                                    String[] keySplit = metadataKey.split("_");
                                    if (keySplit[0].equals("sign") && keySplit[1].contains("step")) {
                                        ObjectMapper mapper = new ObjectMapper();
                                        List<String> recipientList = mapper.readValue(metadatas.get(metadataKey), List.class);
                                        LiveWorkflowStep liveWorkflowStep = liveWorkflowService.createWorkflowStep(false, SignType.valueOf(signType), recipientList.toArray(String[]::new));
                                        signBook.getLiveWorkflow().getWorkflowSteps().add(liveWorkflowStep);
                                    }
                                    if (keySplit[0].equals("sign") && keySplit[1].contains("target")) {
                                        String target = metadatas.get(metadataKey);
                                        if (target.contains("://")) {
                                            signBook.getLiveWorkflow().setDocumentsTargetUri(target.replace("\\", "/"));
                                        } else {
                                            signBook.getLiveWorkflow().setDocumentsTargetUri(workflow.getDocumentsTargetUri() + "/" + target.replace("\\", "/"));
                                        }
                                        logger.info("target set to : " + signBook.getLiveWorkflow().getDocumentsTargetUri());
                                    }
                                }
                            } else {
                                signBookService.importWorkflow(signBook, workflow);
                            }

                            signBookService.nextWorkFlowStep(signBook);
                            signBookService.pendingSignBook(signBook, user);
                            fsAccessService.remove(fsFile);
                            nbImportedFiles++;
                        }
                    } else {
                        logger.info("aucun fichier à importer depuis : " + workflow.getDocumentsSourceUri());
                    }
                } catch (Exception e) {
                    throw new EsupSignatureRuntimeException("error on import from " + workflow.getDocumentsSourceUri(), e);
                }
                fsAccessService.close();
            } else {
                logger.warn("aucun service de fichier n'est disponible");
            }
        }
        return nbImportedFiles;
    }

    public boolean checkUserManageRights(User user, Workflow workflow) {
        if ((workflow.getCreateBy().equals(user) || workflow.getManagers().contains(user.getEmail())) && !workflow.getCreateBy().equals(userService.getSystemUser())) {
            return true;
        } else {
            return false;
        }
    }

    public void changeSignType(WorkflowStep workflowStep, String name, SignType signType) {
        if (name != null) {
            workflowStep.setName(name);
        }
        setSignTypeForWorkflowStep(signType, workflowStep);
    }

    public Long setSignTypeForWorkflowStep(SignType signType, WorkflowStep workflowStep) {
        workflowStep.setSignType(signType);
        return workflowStep.getId();
    }

    public Long toggleAllSignToCompleteForWorkflowStep(WorkflowStep workflowStep) {
        if (workflowStep.getAllSignToComplete()) {
            workflowStep.setAllSignToComplete(false);
        } else {
            workflowStep.setAllSignToComplete(true);
        }
        return workflowStep.getId();
    }

    public void addRecipientsToWorkflowStep(WorkflowStep workflowStep, String... recipientsEmail) {
        recipientsEmail = Arrays.stream(recipientsEmail).distinct().toArray(String[]::new);
        for (String recipientEmail : recipientsEmail) {
            User recipientUser;
            if (userRepository.countByEmail(recipientEmail) == 0) {
                recipientUser = userService.createUserWithEmail(recipientEmail);
            } else {
                recipientUser = userRepository.findByEmail(recipientEmail).get(0);
            }
            if (workflowStep.getId() != null) {
                for (User user : workflowStep.getUsers()) {
                    if (user.equals(recipientUser)) {
                        return;
                    }
                }
            }
            workflowStep.getUsers().add(recipientUser);
        }
    }

    public WorkflowStep createWorkflowStep(String name, Boolean allSignToComplete, SignType signType, String... recipientEmails) throws EsupSignatureUserException {
        WorkflowStep workflowStep = new WorkflowStep();
        if (name != null) {
            workflowStep.setName(name);
        }
        if (allSignToComplete == null) {
            workflowStep.setAllSignToComplete(false);
        } else {
            workflowStep.setAllSignToComplete(allSignToComplete);
        }
        workflowStep.setSignType(signType);
        workflowStepRepository.save(workflowStep);
        if (recipientEmails != null && recipientEmails.length > 0) {
            addRecipientsToWorkflowStep(workflowStep, recipientEmails);
        }
        return workflowStep;
    }


    public void addStep(String signType, String description, String[] recipientsEmails, Boolean changeable, Boolean allSignToComplete, Workflow workflow) throws EsupSignatureUserException {
        WorkflowStep workflowStep = createWorkflowStep("", allSignToComplete, SignType.valueOf(signType), recipientsEmails);
        workflowStep.setDescription(description);
        workflowStep.setChangeable(changeable);
        workflow.getWorkflowSteps().add(workflowStep);
    }

    public boolean isWorkflowStepFullSigned(LiveWorkflowStep liveWorkflowStep) {
        for (Recipient recipient : liveWorkflowStep.getRecipients()) {
            if (!recipient.getSigned()) {
                return false;
            }
        }
        return true;
    }

    public List<Workflow> getClassesWorkflows() {
        return this.workflows;
    }

    public List<Workflow> getDatabaseWorkflows() {
        return workflowRepository.findAll();
    }

    public Set<Workflow> getWorkflowsBySystemUser() {
        User systemUser = userService.getSystemUser();
        return getWorkflowsByUser(systemUser, systemUser);

    }

    public List<Workflow> getSystemWorkflows() {
        List<Workflow> workflowTypes = new ArrayList<>();
        workflowTypes.addAll(getWorkflowsBySystemUser());
        return workflowTypes;
    }

    public List<Workflow> getAuthorizedToShareWorkflows() {
        return workflowRepository.findDistinctByAuthorizedShareTypesIsNotNull();
    }

    public List<Workflow> getAllWorkflows() {
        List<Workflow> allWorkflows = new ArrayList<>();
        allWorkflows.addAll(this.getClassesWorkflows());
        allWorkflows.addAll(this.getDatabaseWorkflows());
        return allWorkflows;
    }

    public Workflow getWorkflowByClassName(String className) {
        for (Workflow workflow : workflows) {
            if (workflow.getClass().getSimpleName().equals(className)) {
                return workflow;
            }
        }
        return null;
    }

    public Workflow getWorkflowClassByName(String name) {
        for (Workflow workflow : workflows) {
            if (workflow.getName().equals(name)) {
                return workflow;
            }
        }
        return null;
    }

    public Workflow getWorkflowById(Long id) {
        return workflowRepository.findById(id).get();
    }

    public Workflow getWorkflowByName(String name) {
        return workflowRepository.findByName(name);
    }

    public Workflow initWorkflow(User user, Long id, String name) {
        Workflow workflow = getWorkflowById(id);
        workflow.setSourceType(DocumentIOType.none);
        workflow.setTargetType(DocumentIOType.none);
        workflow.setCreateBy(user);
        workflow.setName(name);
        workflow.setDescription(name);
        workflow.setTitle(name.replaceAll("[\\\\/:*?\"<>|]", "_").replace(" ", "_"));
        return workflow;
    }

    public Workflow computeWorkflow(Workflow workflow, List<String> recipientEmails, User user, boolean computeForDisplay) throws EsupSignatureException {
        try {
            Workflow modelWorkflow = (Workflow) BeanUtils.cloneBean(workflow);
            if (modelWorkflow.getFromCode() != null && modelWorkflow.getFromCode()) {
                DefaultWorkflow defaultWorkflow = (DefaultWorkflow) getWorkflowByClassName(modelWorkflow.getName());
                defaultWorkflow.fillWorkflowSteps(modelWorkflow, user, recipientEmails);
            }
            int step = 1;
            for (WorkflowStep workflowStep : modelWorkflow.getWorkflowSteps()) {
                replaceStepSystemUsers(user, workflowStep);
                if (workflowStep.getChangeable() != null && workflowStep.getChangeable()) {
                    if(!computeForDisplay) {
                        workflowStep.getUsers().clear();
                    }
                    List<User> recipients = userPropertieService.getFavoriteRecipientEmail(step, workflowStep, recipientEmails, user, this);
                    if(recipients.size() > 0 ) {
                        workflowStep.getUsers().clear();
                    }
                    for (User oneUser : recipients) {
                        workflowStep.getUsers().add(oneUser);
                    }
                }
                entityManager.detach(workflowStep);
                step++;
            }
            entityManager.detach(modelWorkflow);
            return modelWorkflow;
        } catch (Exception e) {
            throw new EsupSignatureException("compute workflow error", e);
        }
    }

    private void replaceStepSystemUsers(User user, WorkflowStep workflowStep) {
        for (User oneUser : workflowStep.getUsers()) {
            if (oneUser.getEppn().equals("creator")) {
                workflowStep.getUsers().remove(oneUser);
                workflowStep.getUsers().add(user);
            }
            if (oneUser.getEppn().equals("generic")) {
                workflowStep.getUsers().remove(oneUser);
            }
        }
   }

    public void saveProperties(User user, Workflow modelWorkflow, Workflow computedWorkflow) {
        int i = 0;
        for (WorkflowStep workflowStep : modelWorkflow.getWorkflowSteps()) {
            List<User> recipients = computedWorkflow.getWorkflowSteps().get(i).getUsers();
            if(recipients.size() > 0 && workflowStep.getChangeable() != null && workflowStep.getChangeable()) {
                userPropertieService.createUserPropertie(user, workflowStep, recipients);
            }
            i++;
        }
    }

    public WorkflowStep addStepRecipients(User user, Long workflowStepId, String recipientsEmails, Workflow workflow) {
        WorkflowStep workflowStep = workflowStepRepository.findById(workflowStepId).get();
        if(user.equals(workflow.getCreateBy()) || workflow.getCreateBy().equals(userService.getSystemUser())) {
            addRecipientsToWorkflowStep(workflowStep, recipientsEmails);
        } else {
            logger.warn(user.getEppn() + " try to update " + workflow.getId() + " without rights");
        }
        return workflowStep;
    }

    public WorkflowStep removeStepRecipient(User user, Long workflowStepId, Long userId, Workflow workflow) {
        WorkflowStep workflowStep = workflowStepRepository.findById(workflowStepId).get();
        if(user.equals(workflow.getCreateBy()) || userService.getSystemUser().equals(workflow.getCreateBy())) {
            User recipientToRemove = userRepository.findById(userId).get();
            workflowStep.getUsers().remove(recipientToRemove);
        } else {
            logger.warn(user.getEppn() + " try to move " + workflow.getId() + " without rights");
        }
        return workflowStep;
    }


    public void updateStep(Integer step, SignType signType, String description, Boolean changeable, Boolean allSignToComplete, Workflow workflow) {
        WorkflowStep workflowStep = workflow.getWorkflowSteps().get(step);
        changeSignType(workflowStep, null, signType);
        workflowStep.setDescription(description);
        workflowStep.setChangeable(changeable);
        workflowStep.setAllSignToComplete(allSignToComplete);
    }

    public void removeStep(Integer stepNumber, Workflow workflow) {
        WorkflowStep workflowStep = workflow.getWorkflowSteps().get(stepNumber);
        workflow.getWorkflowSteps().remove(workflowStep);
        workflowRepository.save(workflow);
        workflowStepRepository.delete(workflowStep);
    }


    public void delete(Workflow workflow) {
        List<LiveWorkflow> liveWorkflows = liveWorkflowRepository.findByWorkflow(workflow);
        List<LiveWorkflow> deleteLiveWorkflows = liveWorkflows.stream().filter(l -> l.getWorkflowSteps().isEmpty()).collect(Collectors.toList());
        List<LiveWorkflow> noneDeleteLiveWorkflows = liveWorkflows.stream().filter(l -> !l.getWorkflowSteps().isEmpty()).collect(Collectors.toList());
        for (LiveWorkflow liveWorkflow : deleteLiveWorkflows) {
            List<SignBook> signBooks = signBookRepository.findByLiveWorkflowAndStatus(liveWorkflow, SignRequestStatus.draft);
            signBooks.forEach(s -> signBookRepository.delete(s));
        }
        deleteLiveWorkflows.forEach(l -> liveWorkflowRepository.delete(l));
        noneDeleteLiveWorkflows.forEach(l -> l.setWorkflow(null));
        workflowRepository.delete(workflow);
    }

    public List<Workflow> getWorkflowsByDisplayWorkflowType(DisplayWorkflowType displayWorkflowType) {
        if (displayWorkflowType == null) {
            displayWorkflowType = DisplayWorkflowType.SYSTEM;
        }
        List<Workflow> workflows = new ArrayList<>();
        if(DisplayWorkflowType.SYSTEM.equals(displayWorkflowType)) {
            workflows.addAll(getWorkflowsBySystemUser());
        } else if(DisplayWorkflowType.CLASSES.equals(displayWorkflowType)) {
            workflows.addAll(getClassesWorkflows());
        } else if(DisplayWorkflowType.ALL.equals(displayWorkflowType)) {
            workflows.addAll(getAllWorkflows());
        } else if(DisplayWorkflowType.USERS.equals(displayWorkflowType)) {
            workflows.addAll(getAllWorkflows());
            workflows.removeAll(getClassesWorkflows());
            workflows.removeAll(getWorkflowsBySystemUser());
        }
        return workflows;
    }

    public Workflow update(Workflow workflow, User user, String[] types, List<String> managers) {
        Workflow workflowToUpdate = getWorkflowById(workflow.getId());
        if(managers != null && managers.size() > 0) {
            workflowToUpdate.getManagers().clear();
            for(String manager : managers) {
                User managerUser = userService.checkUserByEmail(manager);
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
        List<UserShare> userShares = userShareRepository.findByWorkflowId(workflowToUpdate.getId());
        for(UserShare userShare : userShares) {
            userShare.getShareTypes().removeIf(shareType -> !shareTypes.contains(shareType));
        }
        workflowToUpdate.setSourceType(workflow.getSourceType());
        workflowToUpdate.setTargetType(workflow.getTargetType());
        workflowToUpdate.setDocumentsSourceUri(workflow.getDocumentsSourceUri());
        workflowToUpdate.setDocumentsTargetUri(workflow.getDocumentsTargetUri());
        workflowToUpdate.setDescription(workflow.getDescription());
        workflowToUpdate.setTitle(workflow.getTitle());
        workflowToUpdate.setPublicUsage(workflow.getPublicUsage());
        workflowToUpdate.setScanPdfMetadatas(workflow.getScanPdfMetadatas());
        workflowToUpdate.setRole(workflow.getRole());
        workflowToUpdate.setUpdateBy(user.getEppn());
        workflowToUpdate.setUpdateDate(new Date());
        workflowRepository.save(workflowToUpdate);
        return workflowToUpdate;
    }

    public void setUpdateByAndUpdateDate(Workflow workflow, String updateBy) {
        workflow.setUpdateBy(updateBy);
        workflow.setUpdateDate(new Date());
    }


    public String[] getTargetEmails(User user, Form form) {
        List<UserPropertie> userProperties = userPropertieService.getUserProperties(user, getWorkflowByName(form.getWorkflowType()).getWorkflowSteps().get(0));
        userProperties = userProperties.stream().sorted(Comparator.comparing(UserPropertie::getId).reversed()).collect(Collectors.toList());
        if(userProperties.size() > 0 ) {
            return userProperties.get(0).getTargetEmail().split(",");
        }
        return null;
    }
}
