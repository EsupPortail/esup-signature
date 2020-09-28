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
    private RecipientService recipientService;

    @Resource
    private WorkflowStepRepository workflowStepRepository;

    @Resource
    private RecipientRepository recipientRepository;

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
            workflow.setCreateBy("system");
            workflow.setSourceType(DocumentIOType.none);
            workflow.setTargetType(DocumentIOType.none);
            WorkflowStep workflowStep = new WorkflowStep();
            workflowStep.setName("Ma signature");
            workflowStep.setSignType(SignType.certSign);
            workflowStep.setParentType("system");
            Recipient recipient = recipientService.createRecipient(workflowStep.getId(), creator);
            recipientRepository.save(recipient);
            workflowStep.getRecipients().add(recipient);
            workflowStepRepository.save(workflowStep);
            workflow.getWorkflowSteps().add(workflowStep);
            workflowRepository.save(workflow);
        }
    }

    public Workflow createWorkflow(String name, User user, boolean external) throws EsupSignatureException {
        if (workflowRepository.countByName(name) == 0) {
            Workflow workflow = new Workflow();
            workflow.setName(name);
            workflow.setDescription(name);
            workflow.setCreateBy(user.getEppn());
            workflow.setCreateDate(new Date());
            workflow.setExternal(external);
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

    public Set<Workflow> getWorkflowsForUser(User user, User authUser) {
        Set<Workflow> workflows = new HashSet<>();
        if(user.equals(authUser)) {
            workflows.addAll(workflowRepository.findByCreateBy(user.getEppn()));
            workflows.addAll(workflowRepository.findByManagersContains(user.getEmail()));
            workflows.addAll(workflowRepository.findAutorizedWorkflowByUser(user));
        } else {
            for(UserShare userShare : userShareRepository.findByUserAndToUsersInAndShareType(user, Arrays.asList(authUser), ShareType.create)) {
                workflows.addAll(userShare.getWorkflows());
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
            fsAccessService.open();
            if(fsAccessService.cd(workflow.getDocumentsSourceUri()) == null) {
                logger.info("create non existing folders : " + workflow.getDocumentsSourceUri());
                fsAccessService.createFile("/", workflow.getDocumentsSourceUri(), "folder");
                if(fsAccessService.getFile("/" + workflow.getDocumentsSourceUri() + "/signed") == null) {
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
                        if(metadatas.get("Title") != null && !metadatas.get("Title").isEmpty()) {
                            documentName = metadatas.get("Title");
                        }
                        SignBook signBook = signBookService.createSignBook(workflow.getTitle() , documentName  + "_" + nbImportedFiles, user, false);
                        signBook.setTargetType(workflow.getTargetType());
                        signBook.setDocumentsTargetUri(workflow.getDocumentsTargetUri());
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

                        if(workflow.getScanPdfMetadatas()) {
                            String signType = metadatas.get("sign_type_default_val");
                            User creator = userService.createUserWithEppn(metadatas.get("Creator"));
                            if(creator != null) {
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
                                    WorkflowStep workflowStep = createWorkflowStep(workflow.getName() + " - " + stepSplit[1], "signbook", signBook.getId(), false, SignType.valueOf(signType), recipientList.toArray(String[]::new));
                                    signBook.getWorkflowSteps().add(workflowStep);
                                }
                                if (keySplit[0].equals("sign") && keySplit[1].contains("target")) {
                                    String target = metadatas.get(metadataKey);
                                    if(target.contains("://")) {
                                        signBook.setDocumentsTargetUri(target.replace("\\", "/"));
                                    } else {
                                        signBook.setDocumentsTargetUri(workflow.getDocumentsTargetUri() + "/" + target.replace("\\", "/"));
                                    }
                                    logger.info("target set to : " + signBook.getDocumentsTargetUri());
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
        }
        return nbImportedFiles;
    }

    public boolean checkUserManageRights(User user, Workflow workflow) {
        if ((workflow.getCreateBy().equals(user.getEppn()) || workflow.getManagers().contains(user.getEmail())) && !workflow.getCreateBy().equals("system")) {
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
                for (Recipient recipient : workflowStep.getRecipients()) {
                    if (recipient.getUser().equals(recipientUser)) {
                        return;
                    }
                }
            }
            Recipient recipient = recipientService.createRecipient(workflowStep.getId(), recipientUser);
            recipient.setParentId(workflowStep.getId());
            recipient.setParentType("workflow");
            recipientRepository.save(recipient);
            workflowStep.getRecipients().add(recipient);
        }
    }

    public WorkflowStep createWorkflowStep(String name, String parentType, Long parentId, Boolean allSignToComplete, SignType signType, String... recipientEmails) throws EsupSignatureUserException {
        WorkflowStep workflowStep = new WorkflowStep();
        if(name != null) {
            workflowStep.setName(name);
        }
        workflowStep.setParentType(parentType);
        workflowStep.setParentId(parentId);
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

    public boolean isWorkflowStepFullSigned(WorkflowStep workflowStep) {
        for(Recipient recipient : workflowStep.getRecipients()) {
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
        for(Workflow workflow : this.workflows ) {
            if(workflow.getClass().getSimpleName().equals(name)) {
                return workflow;
            }
        }
        List<Workflow> dataBaseWorkflows =  workflowRepository.findByName(name);
        if(dataBaseWorkflows.size() > 0) {
            return dataBaseWorkflows.get(0);
        }
        return null;
    }

    public List<Recipient> getFavoriteRecipientEmail(int step, Form form, List<String> recipientEmails, User user) throws EsupSignatureUserException {
        List<Recipient> recipients = new ArrayList<>();
        if(recipientEmails != null && recipientEmails.size() > 0) {
            recipientEmails = recipientEmails.stream().filter(r -> r.startsWith(String.valueOf(step))).collect(Collectors.toList());
            for(String recipientEmail : recipientEmails) {
                recipients.add(recipientService.getRecipientByEmail(form.getId(), recipientEmail.substring(recipientEmail.indexOf("*") + 1)));
            }
        } else {
            List<String> favoritesEmail = userPropertieService.getFavoritesEmails(user, step, form);
            for(String email : favoritesEmail) {
                User recipientUser = userService.checkUserByEmail(email);
                recipients.add(recipientService.createRecipient(null, recipientUser));
            }
        }
        return recipients;
    }

    public Workflow getWorkflowByDataAndUser(Data data, List<String> recipientEmails, User user) throws EsupSignatureException {
        Workflow workflow;
        List<WorkflowStep> workflowSteps = new ArrayList<>();
        Workflow modelWorkflow = getWorkflowByName(data.getForm().getWorkflowType());
        try {
            if (modelWorkflow instanceof DefaultWorkflow) {
                DefaultWorkflow defaultWorkflow = (DefaultWorkflow) BeanUtils.cloneBean(modelWorkflow);
                workflowSteps.addAll(((DefaultWorkflow) modelWorkflow).generateWorkflowSteps(user, data, recipientEmails));
                defaultWorkflow.initWorkflowSteps();
                defaultWorkflow.getWorkflowSteps().addAll(workflowSteps);
                workflow = defaultWorkflow;
                workflowSteps.addAll(workflow.getWorkflowSteps());
            } else {
                workflow = (Workflow) BeanUtils.cloneBean(modelWorkflow);
                int step = 1;
                for (WorkflowStep workflowStep : workflow.getWorkflowSteps()) {
                    replaceStepCreatorByUser(user, workflowStep);
                    entityManager.detach(workflowStep);
                    if (workflowStep.getChangeable() != null && workflowStep.getChangeable()) {
                        workflowStep.getRecipients().clear();
                        List<Recipient> recipients = getFavoriteRecipientEmail(workflowStep.getStepNumber(), data.getForm(), recipientEmails, user);
                        for (Recipient recipient : recipients) {
                            workflowStep.getRecipients().add(recipient);
                            entityManager.detach(recipient);
                        }
                        if(recipientEmails != null) {
                            userPropertieService.createUserPropertie(user, step, workflowStep, data.getForm());
                        }
                    }
                    step++;

                }
                workflowSteps.addAll(workflow.getWorkflowSteps());
                entityManager.detach(workflow);
            }
            return workflow;
        } catch (Exception e) {
            logger.error("workflow not found", e);
            throw new EsupSignatureException("workflow not found", e);
        }
    }

    private void replaceStepCreatorByUser(User user, WorkflowStep workflowStep) {
        for(Recipient recipient : workflowStep.getRecipients()) {
            entityManager.detach(recipient);
            if (recipient.getUser().getEppn().equals("creator")) {
                recipient.setUser(user);
            }
        }
    }

}
