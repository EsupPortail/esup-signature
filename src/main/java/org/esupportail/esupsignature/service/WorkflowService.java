package org.esupportail.esupsignature.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.*;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.WorkflowRepository;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessFactory;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessService;
import org.esupportail.esupsignature.service.interfaces.fs.FsFile;
import org.esupportail.esupsignature.service.interfaces.workflow.DefaultWorkflow;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.*;
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
    private SignBookService signBookService;

    @Resource
    private FsAccessFactory fsAccessFactory;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private UserService userService;

    @Resource
    private UserShareService userShareService;

    @Resource
    public UserPropertieService userPropertieService;

    @Resource
    private FileService fileService;

    @Resource
    private PdfService pdfService;

    @Resource
    private LiveWorkflowStepService liveWorkflowStepService;

    @Resource
    private TargetService targetService;

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
            workflow.setSourceType(DocumentIOType.none);
//            workflow.setTargetType(DocumentIOType.none);
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
                toUpdateWorkflow.setSourceType(classWorkflow.getSourceType());
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
            workflow.setSourceType(DocumentIOType.none);
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
            workflows.addAll(workflowRepository.findByCreateByEppn(userEppn));
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

    @Transactional
    public int importFilesFromSource(Long workflowId, User user, User authUser) {
        Workflow workflow = getById(workflowId);
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
                }
                try {
                    fsFiles.addAll(fsAccessService.listFiles(workflow.getDocumentsSourceUri() + "/"));
                    if (fsFiles.size() > 0) {
                        int j = 0;
                        for (FsFile fsFile : fsFiles) {
                            logger.info("adding file : " + fsFile.getName());
                            ByteArrayOutputStream baos = fileService.copyInputStream(fsFile.getInputStream());
                            Map<String, String> metadatas = pdfService.readMetadatas(new ByteArrayInputStream(baos.toByteArray()));
                            String documentName = fsFile.getName();
                            if (metadatas.get("Title") != null && !metadatas.get("Title").isEmpty()) {
                                documentName = metadatas.get("Title");
                            }
                            SignBook signBook = signBookService.createSignBook(fileService.getNameOnly(documentName), workflow, "",null, user, false);
                            signBook.getLiveWorkflow().setWorkflow(workflow);
                            SignRequest signRequest = signRequestService.createSignRequest(null, signBook, user.getEppn(), authUser.getEppn());
                            if (fsFile.getCreateBy() != null && userService.getByEppn(fsFile.getCreateBy()) != null) {
                                user = userService.getByEppn(fsFile.getCreateBy());
                            }
                            List<String> workflowRecipientsEmails = new ArrayList<>();
                            workflowRecipientsEmails.add(user.getEmail());
                            signRequestService.addDocsToSignRequest(signRequest, true, j, fileService.toMultipartFile(new ByteArrayInputStream(baos.toByteArray()), fsFile.getName(), fsFile.getContentType()));
                            j++;
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
                                int i = 0;
                                for (String metadataKey : metadatas.keySet()) {
                                    String[] keySplit = metadataKey.split("_");
                                    if (keySplit[0].equals("sign") && keySplit[1].contains("step")) {
                                        ObjectMapper mapper = new ObjectMapper();
                                        List<String> recipientList = mapper.readValue(metadatas.get(metadataKey), List.class);
                                        WorkflowStep workflowStep = null;
                                        if(workflow.getWorkflowSteps().size() > i) {
                                            workflowStep = workflow.getWorkflowSteps().get(i);
                                        }
                                        LiveWorkflowStep liveWorkflowStep = liveWorkflowStepService.createLiveWorkflowStep(workflowStep, false, true, false, SignType.valueOf(signType), recipientList, null);
                                        signBook.getLiveWorkflow().getLiveWorkflowSteps().add(liveWorkflowStep);
                                        i++;
                                    }
                                    if (keySplit[0].equals("sign") && keySplit[1].contains("target")) {
                                        String metadataTarget = metadatas.get(metadataKey);
                                        for(Target target : workflow.getTargets()) {
                                            signBook.getLiveWorkflow().getTargets().add(targetService.createTarget(target.getTargetType(), target.getTargetUri() + "/" + metadataTarget));
                                        }
                                        logger.info("target set to : " + signBook.getLiveWorkflow().getTargets().get(0).getTargetUri());
                                    }
                                }
                            } else {
                                targetService.copyTargets(workflow.getTargets(), signBook, null);
                                signBookService.importWorkflow(signBook, workflow, null);
                            }
                            signBookService.nextStepAndPending(signBook.getId(), null, user.getEppn(), authUser.getEppn());
                            fsAccessService.remove(fsFile);
                            nbImportedFiles++;
                        }
                    } else {
                        logger.info("aucun fichier à importer depuis : " + workflow.getDocumentsSourceUri());
                    }
                } catch (Exception e) {
                    logger.error("error on import from " + workflow.getDocumentsSourceUri(), e);
                }
                fsAccessService.close();
            } else {
                logger.warn("aucun service de fichier n'est disponible");
            }
        }
        return nbImportedFiles;
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
        return getWorkflowsByUser(systemUser.getEppn(), systemUser.getEppn());

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
        workflow.setSourceType(DocumentIOType.none);
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
   }

    public void delete(Workflow workflow) throws EsupSignatureException {
        List<SignBook> signBooks = signBookService.getSignBooksByWorkflow(workflow.getId());
        if(signBooks.stream().allMatch(signBook -> signBook.getStatus() == SignRequestStatus.draft || signBook.getStatus() == SignRequestStatus.deleted)) {
            List<LiveWorkflow> liveWorkflows = liveWorkflowService.getByWorkflow(workflow);
            for(LiveWorkflow liveWorkflow : liveWorkflows) {
                liveWorkflow.setWorkflow(null);
                liveWorkflow.getLiveWorkflowSteps().forEach(lws -> lws.setWorkflowStep(null));
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
        workflowToUpdate.setSourceType(workflow.getSourceType());
        workflowToUpdate.getTargets().addAll(workflow.getTargets());
        workflowToUpdate.setDocumentsSourceUri(workflow.getDocumentsSourceUri());
        workflowToUpdate.setDescription(workflow.getDescription());
        workflowToUpdate.setTitle(workflow.getTitle());
        workflowToUpdate.setNamingTemplate(workflow.getNamingTemplate());
        workflowToUpdate.setPublicUsage(workflow.getPublicUsage());
        workflowToUpdate.setVisibility(workflow.getVisibility());
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
    public boolean addTarget(Long id, String targetType, String documentsTargetUri) {
        Workflow workflow = getById(id);
        if(workflow.getTargets().stream().map(Target::getTargetType).noneMatch(tt -> tt.equals(DocumentIOType.mail))) {
            Target target = targetService.createTarget(DocumentIOType.valueOf(targetType), documentsTargetUri);
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

    public List<Workflow> getWorkflowsByRoles(String role) {
        return workflowRepository.findByRolesIn(Collections.singletonList(role));
    }

    public Set<Workflow> getManagerWorkflows(String userEppn) {
        User manager = userService.getByEppn(userEppn);
        Set<Workflow> workflowsManaged = new HashSet<>();
        for (String role : manager.getManagersRoles()) {
            workflowsManaged.addAll(this.getWorkflowsByRoles(role));
        }
        workflowsManaged.addAll(this.getWorkflowsByUser(manager.getEppn(), manager.getEppn()));
        return workflowsManaged;
    }

    @Transactional
    public InputStream getJsonWorkflowSetup(Long id) throws IOException {
        Workflow workflow = getById(id);
        File jsonFile = fileService.getTempFile("json");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writer().writeValue(jsonFile, workflow);
        return new FileInputStream(jsonFile);
    }

    @Transactional
    public void setWorkflowSetupFromJson(Long id, InputStream inputStream) throws IOException {
        Workflow workflow = getById(id);
        String savedName = workflow.getName();
        String savedTitle = workflow.getTitle();
        ObjectMapper objectMapper = new ObjectMapper();
        Workflow workflowSetup = objectMapper.readValue(inputStream.readAllBytes(), Workflow.class);
        for(WorkflowStep workflowStepSetup : workflowSetup.getWorkflowSteps()) {
            Optional<WorkflowStep> optionalWorkflowStep = workflow.getWorkflowSteps().stream().filter(workflowStep1 -> workflowStep1.getId().equals(workflowStepSetup.getId())).findFirst();
            if(optionalWorkflowStep.isPresent()) {
                WorkflowStep workflowStep = optionalWorkflowStep.get();
                workflowStepService.updateStep(workflowStep.getId(), workflowStepSetup.getSignType(), workflowStepSetup.getDescription(), workflowStepSetup.getChangeable(), workflowStepSetup.getRepeatable(), workflowStepSetup.getMultiSign(), workflowStepSetup.getAllSignToComplete(), workflowStepSetup.getMaxRecipients());
            } else {
                WorkflowStep newWorkflowStep = workflowStepService.createWorkflowStep(workflowSetup.getName(), workflowStepSetup.getAllSignToComplete(), workflowStepSetup.getSignType(), workflowStepSetup.getUsers().stream().map(User::getEmail).collect(Collectors.toList()).toArray(String[]::new));
                workflowStepService.updateStep(newWorkflowStep.getId(), workflowStepSetup.getSignType(), workflowStepSetup.getDescription(), workflowStepSetup.getChangeable(), workflowStepSetup.getRepeatable(), workflowStepSetup.getMultiSign(), workflowStepSetup.getAllSignToComplete(), workflowStepSetup.getMaxRecipients());
                workflow.getWorkflowSteps().add(newWorkflowStep);
            }
        }
        update(workflow, workflowSetup.getCreateBy(), workflowSetup.getManagers().toArray(String[]::new), workflowSetup.getManagers());
        workflow.setName(savedName);
        workflow.setTitle(savedTitle);
        return;
    }

}

