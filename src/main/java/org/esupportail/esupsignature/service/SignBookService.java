package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.SignBook.DocumentIOType;
import org.esupportail.esupsignature.entity.SignBook.SignBookType;
import org.esupportail.esupsignature.entity.SignRequest.SignRequestStatus;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.repository.*;
import org.esupportail.esupsignature.service.file.FileService;
import org.esupportail.esupsignature.service.fs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.*;

@Service
public class SignBookService {

    private static final Logger logger = LoggerFactory.getLogger(SignBookService.class);

    @Resource
    private SignBookRepository signBookRepository;

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
    private DocumentService documentService;

    @Resource
    private UserService userService;

    public List<SignBook> getAllSignBooks() {
        List<SignBook> list = new ArrayList<SignBook>();
        signBookRepository.findAll().forEach(e -> list.add(e));
        return list;
    }

    public void initCreatorSignBook() {
        if (signBookRepository.countByRecipientEmailsAndSignBookType(Arrays.asList("creator"), SignBookType.system) == 0) {
            SignBook signBook = new SignBook();
            signBook.setName("Créateur de la demande");
            signBook.setRecipientEmails(Arrays.asList("creator"));
            signBook.setCreateDate(new Date());
            signBook.setModelFile(null);
            signBook.setSignBookType(SignBookType.system);
            signBook.setSourceType(DocumentIOType.none);
            signBook.setTargetType(DocumentIOType.none);
            signBookRepository.save(signBook);

            SignBook signBookWorkflow = new SignBook();
            signBookWorkflow.setName("Ma signature");
            signBookWorkflow.setCreateDate(new Date());
            signBookWorkflow.setModelFile(null);
            signBookWorkflow.setSignBookType(SignBookType.workflow);
            signBookWorkflow.setSourceType(DocumentIOType.none);
            signBookWorkflow.setTargetType(DocumentIOType.none);

            WorkflowStep workflowStep = new WorkflowStep();
            workflowStep.setName("Ma signature");
            workflowStep.setSignRequestParams(signRequestService.getEmptySignRequestParams());
            workflowStep.getSignBooks().put(signBook.getId(), false);
            workflowStepRepository.save(workflowStep);

            signBookWorkflow.getWorkflowSteps().add(workflowStep);
            signBookRepository.save(signBookWorkflow);

        }

    }

    public SignBook createUserSignBook(User user) {
        SignBook signBook = new SignBook();
        signBook.setName(user.getFirstname() + " " + user.getName());
        signBook.setDescription("Parapheur personnel de " + signBook.getName());
        signBook.setCreateBy(user.getEppn());
        signBook.setCreateDate(new Date());
        signBook.getRecipientEmails().add(user.getEmail());
        signBook.setModelFile(null);
        signBook.setSignBookType(SignBookType.user);
        signBook.setSourceType(DocumentIOType.none);
        signBook.setTargetType(DocumentIOType.none);
        signBookRepository.save(signBook);
        return signBook;
    }

    public void updateSignBook(SignBook signBook, SignBook signBookToUpdate, MultipartFile multipartFile) throws EsupSignatureException {
        signBookToUpdate.getRecipientEmails().removeAll(signBook.getRecipientEmails());
        signBookToUpdate.getRecipientEmails().addAll(signBook.getRecipientEmails());
        signBookToUpdate.getModeratorEmails().removeAll(signBook.getModeratorEmails());
        signBookToUpdate.getModeratorEmails().addAll(signBook.getModeratorEmails());
        signBookToUpdate.setName(signBook.getName());
        signBookToUpdate.setDocumentsSourceUri(signBook.getDocumentsSourceUri());
        signBookToUpdate.setSourceType(signBook.getSourceType());
        signBookToUpdate.setDocumentsTargetUri(signBook.getDocumentsTargetUri());
        signBookToUpdate.setTargetType(signBook.getTargetType());
        if (multipartFile != null && !multipartFile.isEmpty()) {
            Document newModel;
            newModel = documentService.createDocument(multipartFile, multipartFile.getOriginalFilename());
            if (newModel != null) {
                Document oldModel = signBookToUpdate.getModelFile();
                signBookToUpdate.setModelFile(newModel);
                if (oldModel != null) {
                    documentRepository.delete(oldModel);
                }
            }
            newModel.setParentId(signBookToUpdate.getId());
        }
        signBookRepository.save(signBook);

    }

    public void addRecipient(SignBook signBook, List<String> recipientEmails) {
        for (String recipientEmail : recipientEmails) {
            if (signBookRepository.countByRecipientEmailsAndSignBookType(Arrays.asList(recipientEmail), SignBookType.user) == 0) {
                userService.createUser(recipientEmail);
            }
            signBook.getRecipientEmails().add(recipientEmail);
        }
    }

    public void removeRecipient(SignBook signBook, String recipientEmail) {
        signBook.getRecipientEmails().remove(recipientEmail);
    }

    public SignBook createSignBook(SignBook signBook, User user, MultipartFile multipartFile, boolean external) throws EsupSignatureException {
        if (signBookRepository.countByName(signBook.getName()) == 0) {
            signBook.setCreateBy(user.getEppn());
            signBook.setCreateDate(new Date());
            signBook.getRecipientEmails().removeAll(Collections.singleton(""));
            signBook.setExternal(external);
            for (String recipientEmail : signBook.getRecipientEmails()) {
                if (signBookRepository.countByRecipientEmailsAndSignBookType(Arrays.asList(recipientEmail), SignBookType.user) == 0) {
                    userService.createUser(recipientEmail);
                }
            }
            signBook.getModeratorEmails().removeAll(Collections.singleton(""));
            for (String moderatorEmail : signBook.getModeratorEmails()) {
                if (signBookRepository.countByRecipientEmails(Arrays.asList(moderatorEmail)) == 0) {
                    userService.createUser(moderatorEmail);
                }
            }
            Document model = null;
            if (multipartFile != null) {
                model = documentService.createDocument(multipartFile, multipartFile.getOriginalFilename());
                signBook.setModelFile(model);
            } else {
                signBook.setModelFile(null);
            }
            signBook.setSourceType(DocumentIOType.none);
            signBook.setTargetType(DocumentIOType.none);
            signBookRepository.save(signBook);
            if (model != null) {
                model.setParentId(signBook.getId());
            }
            return signBook;
        } else {
            throw new EsupSignatureException("all ready exist");
        }
    }

    public void deleteSignBook(SignBook signBook) {
        List<SignBook> signBooks = new ArrayList<>();
        signBookRepository.save(signBook);
        for (SignBook signBookStep : signBooks) {
            if (signBookStep.isExternal()) {
                deleteSignBook(signBookStep);
            }
        }

        signBookRepository.delete(signBook);
    }

    public List<FsFile> importFilesFromSource(SignBook signBook, User user) {
        List<FsFile> fsFiles = new ArrayList<>();
        if (signBook.getSourceType() != null && !signBook.getSourceType().equals(DocumentIOType.none)) {
            logger.info("retrieve from " + signBook.getSourceType() + " in " + signBook.getDocumentsSourceUri());
            FsAccessService fsAccessService = fsAccessFactory.getFsAccessService(signBook.getSourceType());
            if(fsAccessService.cd(signBook.getDocumentsSourceUri()) == null) {
                logger.info("create non existing folders : " + signBook.getDocumentsSourceUri());
                fsAccessService.createFile("/", signBook.getDocumentsSourceUri(), "folder");
                fsAccessService.createFile("/" + signBook.getDocumentsSourceUri() + "/", "signed", "folder");
            }
            try {
                fsFiles.addAll(fsAccessService.listFiles("/" + signBook.getDocumentsSourceUri() + "/"));
                if (fsFiles.size() > 0) {
                    for (FsFile fsFile : fsFiles) {
                        logger.info("adding file : " + fsFile.getName());
                        Document documentToAdd = documentService.createDocument(fsFile.getInputStream(), fsFile.getName(), fsFile.getContentType());
                        if (fsFile.getCreateBy() != null && userRepository.countByEppn(fsFile.getCreateBy()) > 0) {
                            user = userRepository.findByEppn(fsFile.getCreateBy()).get(0);
                            user.setIp("127.0.0.1");
                        }
                        List<String> signBookRecipientsEmails = new ArrayList<>();
                        signBookRecipientsEmails.add(user.getEmail());
                        SignRequest signRequest = signRequestService.createSignRequest(new SignRequest(), user, documentToAdd);
                        signRequest.setTitle(documentToAdd.getFileName());
                        signRequestRepository.save(signRequest);
                        signRequestService.importWorkflow(signRequest, signBook);
                        signRequestService.updateStatus(signRequest, SignRequestStatus.pending, "Import depuis " + signBook.getSourceType() + " : " + signBook.getDocumentsSourceUri(), user, "SUCCESS", null);
                        fsAccessService.remove(fsFile);
                    }
                }
            } catch (EsupSignatureFsException e) {
                throw new EsupSignatureRuntimeException("error on delete source file", e);
            }
        }
        return fsFiles;
    }

    public void removeSignRequestFromSignBook(SignRequest signRequest, SignBook signBook) {
        signRequestRepository.save(signRequest);
        signBookRepository.save(signBook);
    }

    public SignBook getSignBookBySignRequestAndUser(SignRequest signRequest, User user) {
        if (signRequest.getCurrentWorkflowStep() != null && signRequest.getCurrentWorkflowStep().getSignBooks().size() > 0) {
            for (Map.Entry<Long, Boolean> signBookId : signRequest.getCurrentWorkflowStep().getSignBooks().entrySet()) {
                SignBook signBook = signBookRepository.findById(signBookId.getKey()).get();
                if (signBook.getRecipientEmails().contains(user.getEmail()) && signRequest.getCurrentWorkflowStep().getSignBooks().containsKey(signBookId.getKey())) {
                    return signBook;
                }
            }
        }
        return null;
    }

    public SignBook getUserSignBook(User user) {
        return getUserSignBookByRecipientEmail(user.getEmail());
    }

    public SignBook getUserSignBookByRecipientEmail(String recipientEmail) {
        if (signBookRepository.countByRecipientEmailsAndSignBookType(Arrays.asList(recipientEmail), SignBookType.user) > 0) {
            return signBookRepository.findByRecipientEmailsAndSignBookType(Arrays.asList(recipientEmail), SignBookType.user).get(0);
        } else {
            User user;
            if (userRepository.findByEmail(recipientEmail).size() > 0) {
                user = userRepository.findByEmail(recipientEmail).get(0);
            } else {
                user = userService.createUser(recipientEmail);
            }
            return getUserSignBook(user);
        }
    }

    public boolean checkUserManageRights(User user, SignBook signBook) {
        if (signBook.getCreateBy().equals(user.getEppn()) || signBook.getModeratorEmails().contains(user.getEmail()) || signBook.getCreateBy().equals("System")) {
            return true;
        } else {
            return false;
        }
    }

}
