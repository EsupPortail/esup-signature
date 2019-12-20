package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.repository.*;
import org.esupportail.esupsignature.service.fs.FsAccessFactory;
import org.esupportail.esupsignature.service.fs.FsAccessService;
import org.esupportail.esupsignature.service.fs.FsFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.*;

@Service
public class WorkflowService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowService.class);

    @Resource
    private WorkflowRepository workflowRepository;

    @Resource
    private WorkflowStepRepository workflowStepRepository;

    @Resource
    private SignRequestRepository signRequestRepository;

    @Resource
    private DocumentRepository documentRepository;

    @Resource
    private UserRepository userRepository;

    @Resource
    private FsAccessFactory fsAccessFactory;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private SignBookService signBookService;

    @Resource
    private DocumentService documentService;

    @Resource
    private UserService userService;

    public List<Workflow> getAllWorkflows() {
        List<Workflow> list = new ArrayList<Workflow>();
        workflowRepository.findAll().forEach(e -> list.add(e));
        return list;
    }

    public void initCreatorWorkflow() {
        if (userRepository.countByEppn("creator") == 0) {
            User creator = userService.createUser("creator", "Createur de la demande", "", "");

            Workflow workflowWorkflow = new Workflow();
            workflowWorkflow.setName("Ma signature");
            workflowWorkflow.setCreateDate(new Date());
            workflowWorkflow.setModelFile(null);
            workflowWorkflow.setSourceType(DocumentIOType.none);
            workflowWorkflow.setTargetType(DocumentIOType.none);

            WorkflowStep workflowStep = new WorkflowStep();
            workflowStep.setName("Ma signature");
            workflowStep.setSignRequestParams(signRequestService.getEmptySignRequestParams());
            workflowStep.getRecipients().put(creator.getId(), false);
            workflowStepRepository.save(workflowStep);

            workflowWorkflow.getWorkflowSteps().add(workflowStep);
            workflowRepository.save(workflowWorkflow);

        }

    }

    public void updateWorkflow(Workflow workflow, Workflow workflowToUpdate, MultipartFile multipartFile) throws EsupSignatureException {
        workflowToUpdate.getModeratorEmails().removeAll(workflow.getModeratorEmails());
        workflowToUpdate.getModeratorEmails().addAll(workflow.getModeratorEmails());
        workflowToUpdate.setName(workflow.getName());
        workflowToUpdate.setDocumentsSourceUri(workflow.getDocumentsSourceUri());
        workflowToUpdate.setSourceType(workflow.getSourceType());
        workflowToUpdate.setDocumentsTargetUri(workflow.getDocumentsTargetUri());
        workflowToUpdate.setTargetType(workflow.getTargetType());
        if (multipartFile != null && !multipartFile.isEmpty()) {
            Document newModel;
            newModel = documentService.createDocument(multipartFile, multipartFile.getOriginalFilename());
            if (newModel != null) {
                Document oldModel = workflowToUpdate.getModelFile();
                workflowToUpdate.setModelFile(newModel);
                if (oldModel != null) {
                    documentRepository.delete(oldModel);
                }
            }
            newModel.setParentId(workflowToUpdate.getId());
        }
        workflowRepository.save(workflow);

    }

    public Workflow createWorkflow(Workflow workflow, User user, MultipartFile multipartFile, boolean external) throws EsupSignatureException {
        if (workflowRepository.countByName(workflow.getName()) == 0) {
            workflow.setCreateBy(user.getEppn());
            workflow.setCreateDate(new Date());
            workflow.setExternal(external);
            workflow.getModeratorEmails().removeAll(Collections.singleton(""));
            Document model = null;
            if (multipartFile != null) {
                model = documentService.createDocument(multipartFile, multipartFile.getOriginalFilename());
                workflow.setModelFile(model);
            } else {
                workflow.setModelFile(null);
            }
            workflow.setSourceType(DocumentIOType.none);
            workflow.setTargetType(DocumentIOType.none);
            workflowRepository.save(workflow);
            if (model != null) {
                model.setParentId(workflow.getId());
            }
            return workflow;
        } else {
            throw new EsupSignatureException("all ready exist");
        }
    }

    public void deleteWorkflow(Workflow workflow) {
        List<Workflow> workflows = new ArrayList<>();
        workflowRepository.save(workflow);
        for (Workflow workflowStep : workflows) {
            if (workflowStep.isExternal()) {
                deleteWorkflow(workflowStep);
            }
        }

        workflowRepository.delete(workflow);
    }

    public List<FsFile> importFilesFromSource(Workflow workflow, User user) {
        List<FsFile> fsFiles = new ArrayList<>();
        if (workflow.getSourceType() != null && !workflow.getSourceType().equals(DocumentIOType.none)) {
            logger.info("retrieve from " + workflow.getSourceType() + " in " + workflow.getDocumentsSourceUri());
            FsAccessService fsAccessService = fsAccessFactory.getFsAccessService(workflow.getSourceType());
            if(fsAccessService.cd(workflow.getDocumentsSourceUri()) == null) {
                logger.info("create non existing folders : " + workflow.getDocumentsSourceUri());
                fsAccessService.createFile("/", workflow.getDocumentsSourceUri(), "folder");
                fsAccessService.createFile("/" + workflow.getDocumentsSourceUri() + "/", "signed", "folder");
            }
            try {
                fsFiles.addAll(fsAccessService.listFiles("/" + workflow.getDocumentsSourceUri() + "/"));
                if (fsFiles.size() > 0) {
                    for (FsFile fsFile : fsFiles) {
                        logger.info("adding file : " + fsFile.getName());
                        Document documentToAdd = documentService.createDocument(fsFile.getInputStream(), fsFile.getName(), fsFile.getContentType());
                        if (fsFile.getCreateBy() != null && userRepository.countByEppn(fsFile.getCreateBy()) > 0) {
                            user = userRepository.findByEppn(fsFile.getCreateBy()).get(0);
                            user.setIp("127.0.0.1");
                        }
                        List<String> workflowRecipientsEmails = new ArrayList<>();
                        workflowRecipientsEmails.add(user.getEmail());
                        SignBook signBook = signBookService.createSignBook("Auto import : " + workflow.getName(), SignBook.SignBookType.workflow, user, false);
                        SignRequest signRequest = signRequestService.createSignRequest(new SignRequest(), user, documentToAdd);
                        signBook.getSignRequests().add(signRequest);
                        signRequest.setParentSignBook(signBook);
                        signRequest.setTitle(documentToAdd.getFileName());
                        signRequestRepository.save(signRequest);
                        signRequestService.importWorkflow(signRequest, workflow);
                        signRequestService.updateStatus(signRequest, SignRequestStatus.pending, "Import depuis " + workflow.getSourceType() + " : " + workflow.getDocumentsSourceUri(), user, "SUCCESS", null);
                        fsAccessService.remove(fsFile);
                    }
                }
            } catch (EsupSignatureFsException | EsupSignatureException e) {
                throw new EsupSignatureRuntimeException("error on delete source file", e);
            }
        }
        return fsFiles;
    }

    public void removeSignRequestFromWorkflow(SignRequest signRequest, Workflow workflow) {
        signRequestRepository.save(signRequest);
        workflowRepository.save(workflow);
    }

    public boolean checkUserManageRights(User user, Workflow workflow) {
        if (workflow.getCreateBy().equals(user.getEppn()) || workflow.getModeratorEmails().contains(user.getEmail()) || workflow.getCreateBy().equals("System")) {
            return true;
        } else {
            return false;
        }
    }

}
