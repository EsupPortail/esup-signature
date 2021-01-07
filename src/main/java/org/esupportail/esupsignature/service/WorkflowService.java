package org.esupportail.esupsignature.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.*;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.WorkflowRepository;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessFactory;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessService;
import org.esupportail.esupsignature.service.interfaces.fs.FsFile;
import org.esupportail.esupsignature.service.interfaces.workflow.DefaultWorkflow;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.hibernate.Session;
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

    @PostConstruct
    public void initCreatorWorkflow() {
        User creator= userService.getByEppn("creator");
        if (creator == null) {
            creator = userService.createUser("creator", "Createur de la demande", "", "creator", UserType.system);
        }
        if (workflowRepository.countByName("Ma signature") == 0) {
            Workflow workflow = new Workflow();
            workflow.setName("Ma signature");
            workflow.setDescription("Signature du créateur de la demande");
            workflow.setCreateDate(new Date());
            workflow.setCreateBy(userService.getSystemUser());
            workflow.setSourceType(DocumentIOType.none);
            workflow.setTargetType(DocumentIOType.none);
            WorkflowStep workflowStep = workflowStepService.createWorkflowStep("Ma signature", false, SignType.pdfImageStamp, creator.getEmail());
            workflow.getWorkflowSteps().add(workflowStep);
            workflowRepository.save(workflow);
        }
    }

    @Transactional
    public void copyClassWorkflowsIntoDatabase() throws EsupSignatureException {
        for (Workflow classWorkflow : getClassesWorkflows()) {
            logger.info("workflow class found : " + classWorkflow.getName());
            if (!isWorkflowExist(classWorkflow.getClass().getSimpleName())) {
                logger.info("create " + classWorkflow.getName() + " on database : ");
                Workflow newWorkflow = createWorkflow(classWorkflow.getClass().getSimpleName(), classWorkflow.getDescription(), userService.getSystemUser());
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

    public Workflow createWorkflow(String title, String description, User user) throws EsupSignatureException {
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

    public Set<Workflow> getWorkflowsByUser(String userEppn, String authUserEppn) {
        User user = userService.getByEppn(userEppn);
        User authUser = userService.getByEppn(authUserEppn);
        List<Workflow> authorizedWorkflows = workflowRepository.findAuthorizedWorkflowByRoles(user.getRoles());
        Set<Workflow> workflows = new HashSet<>();
        if (userEppn.equals(authUserEppn)) {
            workflows.addAll(workflowRepository.findByCreateByEppn(userEppn));
            workflows.addAll(workflowRepository.findByManagersContains(user.getEmail()));
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
    public int importFilesFromSource(Workflow workflow, User user, User authUser) {
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
                            SignRequest signRequest = signRequestService.createSignRequest(documentName, signBook, user.getEppn(), authUser.getEppn());
                            if (fsFile.getCreateBy() != null && userService.getByEppn(fsFile.getCreateBy()) != null) {
                                user = userService.getByEppn(fsFile.getCreateBy());
                            }
                            List<String> workflowRecipientsEmails = new ArrayList<>();
                            workflowRecipientsEmails.add(user.getEmail());
                            signRequestService.addDocsToSignRequest(signRequest, fileService.toMultipartFile(new ByteArrayInputStream(baos.toByteArray()), fsFile.getName(), fsFile.getContentType()));

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
                                        LiveWorkflowStep liveWorkflowStep = liveWorkflowStepService.createWorkflowStep(false, SignType.valueOf(signType), recipientList.toArray(String[]::new));
                                        signBook.getLiveWorkflow().getLiveWorkflowSteps().add(liveWorkflowStep);
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
                            signBookService.nextStepAndPending(signBook.getId(), null, user.getEppn(), authUser.getEppn());
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

    public Workflow getById(Long id) {
        return workflowRepository.findById(id).get();
    }

    public Workflow getWorkflowByName(String name) {
        return workflowRepository.findByName(name);
    }

    @Transactional
    public Workflow initWorkflow(User user, Long id, String name) {
        Workflow workflow = getById(id);
        workflow.setSourceType(DocumentIOType.none);
        workflow.setTargetType(DocumentIOType.none);
        workflow.setCreateBy(user);
        workflow.setName(name);
        workflow.setDescription(name);
        workflow.setTitle(name.replaceAll("[\\\\/:*?\"<>|]", "_").replace(" ", "_"));
        return workflow;
    }

    public Workflow computeWorkflow(Long workflowId, List<String> recipientEmails, String userEppn, boolean computeForDisplay) throws EsupSignatureException {
        Session session = entityManager.unwrap(Session.class);
        System.err.println(session.getFlushMode());
        System.err.println(session.getHibernateFlushMode());
        try {
            Workflow modelWorkflow = getById(workflowId);
            if (modelWorkflow.getFromCode() != null && modelWorkflow.getFromCode()) {
                DefaultWorkflow defaultWorkflow = (DefaultWorkflow) getWorkflowByClassName(modelWorkflow.getName());
                defaultWorkflow.fillWorkflowSteps(modelWorkflow, recipientEmails);
            }
            int step = 1;
            for (WorkflowStep workflowStep : modelWorkflow.getWorkflowSteps()) {
                replaceStepSystemUsers(userEppn, workflowStep.getId());
                if (workflowStep.getChangeable() != null && workflowStep.getChangeable()) {
                    List<User> recipients = userPropertieService.getFavoriteRecipientEmail(step, workflowStep, recipientEmails, userEppn);
                    if(recipients.size() > 0 && !computeForDisplay) {
                        workflowStep.getUsers().clear();
                        for (User oneUser : recipients) {
                            workflowStep.getUsers().add(oneUser);
                        }
                    }
                }
                step++;
            }
            return modelWorkflow;
        } catch (Exception e) {
            throw new EsupSignatureException("compute workflow error", e);
        }
    }

    public void replaceStepSystemUsers(String userEppn, Long workflowStepId) {
        User user = userService.getByEppn(userEppn);
        WorkflowStep workflowStep = workflowStepService.getById(workflowStepId);
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

    public void delete(Workflow workflow) {
        List<LiveWorkflow> liveWorkflows = liveWorkflowService.getByWorkflow(workflow);
        List<LiveWorkflow> deleteLiveWorkflows = liveWorkflows.stream().filter(l -> l.getLiveWorkflowSteps().isEmpty()).collect(Collectors.toList());
        List<LiveWorkflow> noneDeleteLiveWorkflows = liveWorkflows.stream().filter(l -> !l.getLiveWorkflowSteps().isEmpty()).collect(Collectors.toList());
        for (LiveWorkflow liveWorkflow : deleteLiveWorkflows) {
            List<SignBook> signBooks = signBookService.getByLiveWorkflowAndStatus(liveWorkflow, SignRequestStatus.draft);
            signBooks.forEach(s -> signBookService.delete(s));
        }
        deleteLiveWorkflows.forEach(l -> liveWorkflowService.delete(l));
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


    public String[] getTargetEmails(String userEppn, Form form) {
        List<UserPropertie> userProperties = userPropertieService.getUserProperties(userEppn, getWorkflowByName(form.getWorkflowType()).getWorkflowSteps().get(0).getId());
        userProperties = userProperties.stream().sorted(Comparator.comparing(UserPropertie::getId).reversed()).collect(Collectors.toList());
        if(userProperties.size() > 0 ) {
            if(userProperties.get(0).getTargetEmail() != null) {
                return userProperties.get(0).getTargetEmail().split(",");
            }
        }
        return null;
    }

    public List<WorkflowStep> getWorkflowStepsFromSignRequest(SignRequest signRequest, String userEppn) throws EsupSignatureException {
        List<WorkflowStep> workflowSteps = new ArrayList<>();
        if(signRequest.getParentSignBook().getLiveWorkflow().getWorkflow() != null) {
            Workflow workflow = computeWorkflow(getWorkflowByName(signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getName()).getId(), null, userEppn, true);
            workflowSteps.addAll(workflow.getWorkflowSteps());
        }
        return workflowSteps;
    }

}

