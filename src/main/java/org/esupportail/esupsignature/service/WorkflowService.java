package org.esupportail.esupsignature.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.beanutils.BeanUtils;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.entity.enums.UserType;
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
    private UserPropertieService userPropertieService;

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
        if(workflowRepository.countByName("Ma signature") == 0) {
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

    @Transactional
    public void init() {
        for (Workflow workflow : workflowRepository.findByFromCodeIsTrue()) {
            workflow.getWorkflowSteps().clear();
            workflowRepository.save(workflow);
        }
        for (Workflow workflow : getClassesWorkflows()) {
            try {
                Workflow workflow1 = createWorkflow(workflow.getClass().getSimpleName(), workflow.getDescription(), userService.getSystemUser(), false);
                workflow1.setFromCode(true);
            } catch (EsupSignatureException e) {
                logger.warn("already exist");
            }
        }
        for (Workflow workflow : workflowRepository.findByFromCodeIsTrue()) {
            List<WorkflowStep> workflowSteps = null;
            try {
                workflowSteps = ((DefaultWorkflow) getWorkflowByClassName(workflow.getName())).generateWorkflowSteps(userService.getSystemUser(), null, false);
                for (WorkflowStep workflowStep : workflowSteps) {
                    WorkflowStep workflowStep1 = createWorkflowStep(workflowStep.getName(), workflowStep.getAllSignToComplete(), workflowStep.getSignType());
                    workflowStep1.setDescription(workflowStep.getDescription());
                    workflowStep1.setChangeable(workflowStep.getChangeable());
                    workflow.getWorkflowSteps().add(workflowStep1);
                    workflowRepository.save(workflow);
                }
            } catch (EsupSignatureUserException e) {
                logger.warn("already exist");
            }
        }
    }

    public Workflow createWorkflow(String title, String description, User user, boolean external) throws EsupSignatureException {
        String name;
        if (userService.getSystemUser().equals(user)) {
            name = title;
        } else {
            name = user.getEppn().split("@")[0] + title.substring(0, 1).toUpperCase() + title.toLowerCase().substring(1);
            name = name.replaceAll("[^a-zA-Z0-9]", "");
        }
        if (workflowRepository.countByName(name) == 0) {
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
        if(user.equals(authUser)) {
            workflows.addAll(workflowRepository.findByCreateBy(user));
            workflows.addAll(workflowRepository.findByManagersContains(user.getEmail()));
            workflows.addAll(authorizedWorkflows);
        } else {
            for(UserShare userShare : userShareRepository.findByUserAndToUsersInAndShareTypesContains(user, Arrays.asList(authUser), ShareType.create)) {
                if(userShare.getWorkflow() != null && authorizedWorkflows.contains(userShare.getWorkflow())) {
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
            if(fsAccessService != null) {
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
                                        String[] stepSplit = keySplit[1].split("#");
                                        ObjectMapper mapper = new ObjectMapper();
                                        List<String> recipientList = mapper.readValue(metadatas.get(metadataKey), List.class);
                                        LiveWorkflowStep liveWorkflowStep = liveWorkflowService.createWorkflowStep(workflow.getName() + " - " + stepSplit[1], "signbook", signBook.getId(), false, SignType.valueOf(signType), recipientList.toArray(String[]::new));
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
        if(name != null) {
            workflowStep.setName(name);
        }
        setSignTypeForWorkflowStep(signType, workflowStep);
    }

    public Long setSignTypeForWorkflowStep(SignType signType, WorkflowStep workflowStep) {
        workflowStep.setSignType(signType);
        return workflowStep.getId();
    }

    public Long toggleAllSignToCompleteForWorkflowStep(WorkflowStep workflowStep) {
        if(workflowStep.getAllSignToComplete()) {
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
            if(workflowStep.getId() != null) {
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
        if(name != null) {
            workflowStep.setName(name);
        }
        if(allSignToComplete ==null) {
            workflowStep.setAllSignToComplete(false);
        } else {
            workflowStep.setAllSignToComplete(allSignToComplete);
        }
        workflowStep.setSignType(signType);
        workflowStepRepository.save(workflowStep);
        if(recipientEmails != null && recipientEmails.length > 0) {
            addRecipientsToWorkflowStep(workflowStep, recipientEmails);
        }
        return workflowStep;
    }

    public boolean isWorkflowStepFullSigned(LiveWorkflowStep liveWorkflowStep) {
        for(Recipient recipient : liveWorkflowStep.getRecipients()) {
            if(!recipient.getSigned()) {
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
        for(Workflow workflow : workflows ) {
            if(workflow.getClass().getSimpleName().equals(className)) {
                return workflow;
            }
        }
        return null;
    }

    public Workflow getWorkflowByName(String name) {
        return workflowRepository.findByName(name);
    }

    public List<User> getFavoriteRecipientEmail(int stepNumber, WorkflowStep workflowStep, List<String> recipientEmails, User user) {
        List<User> users = new ArrayList<>();
        if(recipientEmails != null && recipientEmails.size() > 0) {
            recipientEmails = recipientEmails.stream().filter(r -> r.startsWith(String.valueOf(stepNumber))).collect(Collectors.toList());
            for(String recipientEmail : recipientEmails) {
                String userEmail = recipientEmail.split("\\*")[1];
                users.add(userService.checkUserByEmail(userEmail));
            }
            userPropertieService.createUserPropertie(user, workflowStep, users);
        } else {
            List<User> favoritesEmail = userPropertieService.getFavoritesEmails(user, workflowStep);
            users.addAll(favoritesEmail);
        }
        return users;
    }

    public Workflow getWorkflowByDataAndUser(Workflow workflow, List<String> recipientEmails, User user) throws EsupSignatureException {
        List<WorkflowStep> workflowSteps = new ArrayList<>();
        Workflow modelWorkflow = getWorkflowByName(workflow.getName());
        try {
            if (modelWorkflow.getFromCode()) {
                DefaultWorkflow defaultWorkflow = (DefaultWorkflow) getWorkflowByClassName(modelWorkflow.getName());
                defaultWorkflow.fillWorkflowSteps(modelWorkflow, user, recipientEmails);
            } else {
                workflow = (Workflow) BeanUtils.cloneBean(modelWorkflow);
                int step = 1;
                for (WorkflowStep workflowStep : workflow.getWorkflowSteps()) {
                    replaceStepCreatorByUser(user, workflowStep);
                    if (workflowStep.getChangeable() != null && workflowStep.getChangeable()) {
                        workflowStep.getUsers().clear();
                        List<User> recipients = getFavoriteRecipientEmail(step, workflowStep, recipientEmails, user);
                        for (User oneUser : recipients) {
                            workflowStep.getUsers().add(oneUser);
                        }
                        userPropertieService.createUserPropertie(user, workflowStep, recipients);
                    }
                    step++;
                }
                workflowSteps.addAll(workflow.getWorkflowSteps());
            }
            return workflow;
        } catch (Exception e) {
            logger.error("workflow not found", e);
            throw new EsupSignatureException("workflow not found", e);
        }
    }

    private void replaceStepCreatorByUser(User user, WorkflowStep workflowStep) {
        List<User> users = new ArrayList<>();
        for(User oneUser : workflowStep.getUsers()) {
            if (oneUser.getEppn().equals("creator")) {
                users.add(user);
            } else {
                users.add(oneUser);
            }
        }
        workflowStep.getUsers().clear();
        workflowStep.getUsers().addAll(users);
    }

}
