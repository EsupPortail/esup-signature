package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.SignBook.DocumentIOType;
import org.esupportail.esupsignature.entity.SignBook.SignBookType;
import org.esupportail.esupsignature.entity.SignRequest.SignRequestStatus;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.repository.*;
import org.esupportail.esupsignature.service.fs.*;
import org.esupportail.esupsignature.service.mail.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
    private SignRequestParamsRepository signRequestParamsRepository;

    @Resource
    private FsAccessFactory fsAccessFactory;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private DocumentService documentService;

    @Resource
    private UserService userService;

    @Resource
    private MailService mailService;

    public List<SignBook> getAllSignBooks() {
        List<SignBook> list = new ArrayList<SignBook>();
        signBookRepository.findAll().forEach(e -> list.add(e));
        return list;
    }

    public void creatorSignBook() {
        SignBook signBook;
        if (signBookRepository.countByRecipientEmailsAndSignBookType(Arrays.asList("creator"), SignBookType.system) == 0) {
            signBook = new SignBook();
            signBook.setName("Créateur de la demande");
            signBook.setRecipientEmails(Arrays.asList("creator"));
            signBook.setCreateDate(new Date());
            signBook.setSignRequestParams(null);
            signBook.setModelFile(null);
            signBook.setSignBookType(SignBookType.system);
            signBook.setSourceType(DocumentIOType.none);
            signBook.setTargetType(DocumentIOType.none);
            signBook.setSignRequestParams(signRequestService.getEmptySignRequestParams());
            signBookRepository.save(signBook);
        }

    }

    //@Transactional
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
        signBook.setSignRequestParams(signRequestService.getEmptySignRequestParams());
        signBookRepository.save(signBook);
        return signBook;
    }

    public void updateSignBook(SignBook signBook, SignBook signBookToUpdate, SignRequestParams signRequestParams, MultipartFile multipartFile) throws EsupSignatureException {
        signBookToUpdate.getRecipientEmails().removeAll(signBook.getRecipientEmails());
        signBookToUpdate.getRecipientEmails().addAll(signBook.getRecipientEmails());
        signBookToUpdate.getModeratorEmails().removeAll(signBook.getModeratorEmails());
        signBookToUpdate.getModeratorEmails().addAll(signBook.getModeratorEmails());
        signBookToUpdate.setName(signBook.getName());
        signBookToUpdate.setDocumentsSourceUri(signBook.getDocumentsSourceUri());
        signBookToUpdate.setSourceType(signBook.getSourceType());
        signBookToUpdate.setDocumentsTargetUri(signBook.getDocumentsTargetUri());
        signBookToUpdate.setTargetType(signBook.getTargetType());
        signBookToUpdate.getSignRequestParams().setSignType(signRequestParams.getSignType());
        signBookToUpdate.getSignRequestParams().setNewPageType(signRequestParams.getNewPageType());
        signBookToUpdate.setAutoRemove(signBook.isAutoRemove());
        if (!multipartFile.isEmpty()) {
            Document newModel;
            try {
                newModel = documentService.createDocument(multipartFile, multipartFile.getOriginalFilename());
            } catch (IOException e) {
                logger.error("enable to add model", e);
                throw new EsupSignatureException(e.getMessage(), e);
            }
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

    public void createSignBook(SignBook signBook, User user, SignRequestParams signRequestParams, MultipartFile multipartFile, boolean external) throws EsupSignatureException {
        if (signBookRepository.countByName(signBook.getName()) == 0) {
            signRequestParamsRepository.save(signRequestParams);
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
                try {
                    model = documentService.createDocument(multipartFile, multipartFile.getOriginalFilename());
                } catch (IOException e) {
                    logger.error("enable to add model", e);
                    throw new EsupSignatureException(e.getMessage(), e);
                }
                signBook.setModelFile(model);
            } else {
                signBook.setModelFile(null);
            }
            signBook.setSignRequestParams(signRequestParams);
            signBook.setSourceType(DocumentIOType.none);
            signBook.setTargetType(DocumentIOType.none);
            signBookRepository.save(signBook);
            if (model != null) {
                model.setParentId(signBook.getId());
            }
        } else {
            throw new EsupSignatureException("all ready exist");
        }
    }

    public void deleteSignBook(SignBook signBook) {
            List<SignBook> signBooks = new ArrayList<>();
            signBook.setSignRequestParams(null);
            signBookRepository.save(signBook);
            for (SignBook signBookStep : signBooks) {
                if (signBookStep.isExternal()) {
                    deleteSignBook(signBookStep);
                }
            }

            signBookRepository.delete(signBook);
    }

    public void resetSignBookParams(SignBook signBook) {
        signBook.getSignRequestParams().setSignPageNumber(1);
        signBook.getSignRequestParams().setXPos(0);
        signBook.getSignRequestParams().setYPos(0);
        //signBookRepository.save(signBook);
    }

    public void importFilesFromSource(SignBook signBook, User user) throws EsupSignatureIOException, EsupStockException {
        if (signBook.getSourceType() != null && !signBook.getSourceType().equals(DocumentIOType.none)) {
            logger.info("retrieve from " + signBook.getSourceType() + " in " + signBook.getDocumentsSourceUri());
            FsAccessService fsAccessService = fsAccessFactory.getFsAccessService(signBook.getSourceType());
            try {
                List<FsFile> fsFiles = new ArrayList<FsFile>();
                try {
                    fsFiles.addAll(fsAccessService.listFiles("/" + signBook.getDocumentsSourceUri() + "/"));
                } catch (EsupStockException e) {
                    logger.warn("create non existing folder because", e);
                    fsAccessService.createFile("/", signBook.getSignBookType().toString(), "folder");
                    fsAccessService.createFile("/" + signBook.getSignBookType().toString(), signBook.getName(), "folder");
                    fsAccessService.createFile("/" + signBook.getSignBookType().toString() + "/" + signBook.getName(), "signed", "folder");
                }
                if (fsFiles.size() > 0) {
                    for (FsFile fsFile : fsFiles) {
                        logger.info("adding file : " + fsFile.getName());
                        fsFile.setPath(signBook.getDocumentsSourceUri());
                        Document documentToAdd = documentService.createDocument(fsFile.getFile(), fsFile.getName(), fsFile.getContentType());
                        if (fsFile.getCreateBy() != null && userRepository.countByEppn(fsFile.getCreateBy()) > 0) {
                            user = userRepository.findByEppn(fsFile.getCreateBy()).get(0);
                            user.setIp("127.0.0.1");
                        }
                        List<String> signBookRecipientsEmails = new ArrayList<>();
                        signBookRecipientsEmails.add(user.getEmail());
                        SignRequest signRequest = signRequestService.createSignRequest(new SignRequest(), user, documentToAdd, signBook.getSignRequestParams());
                        signRequest.setTitle("Import depuis " + signBook.getSourceType() + " : " + signBook.getDocumentsSourceUri());
                        importSignRequestInSignBook(signRequest, signBook, user);
                        signRequestService.updateStatus(signRequest, SignRequestStatus.pending, "Import depuis " + signBook.getSourceType() + " : " + signBook.getDocumentsSourceUri(), user, "SUCCESS", null);
                        fsAccessService.remove(fsFile);
                    }
                } else {
                    throw new EsupSignatureIOException("no file to import in this folder : " + signBook.getDocumentsSourceUri());
                }
            } catch (IOException e) {
                logger.error("read fsaccess error : ", e);
            } catch (EsupSignatureException e) {
                logger.error("import in signBook error", e);
            }
        } else {
            logger.trace("no source type for signbook : " + signBook.getName());
        }
    }

    public void exportFilesToTarget(SignBook signBook, User user) throws EsupSignatureException {
        logger.trace("export signRequest from " + signBook.getName());
        List<SignRequest> signRequests = new ArrayList<>();
        signRequests.addAll(signBook.getSignRequests());
        for (SignRequest signRequest : signRequests) {
            if (signRequest.getStatus().equals(SignRequestStatus.completed) /* && signRequestService.isSignRequestCompleted(signRequest)*/) {
                boolean exportOk = exportFileToTarget(signBook, signRequest, user);
                if(exportOk) {
                    removeSignRequestFromAllSignBooks(signRequest);
                    signRequestService.updateStatus(signRequest, SignRequestStatus.exported, "Copié vers la destination " + signBook.getTargetType() + " : " + signBook.getDocumentsTargetUri(), user, "SUCCESS", "");
                    if (!signBook.getTargetType().equals(DocumentIOType.mail)) {
                        signRequestService.clearAllDocuments(signRequest);
                    }
                }
            }
        }

        if (signBook.isExternal() && signBook.getSignRequests().size() == 0) {
            deleteSignBook(signBook);
        }
    }

    public boolean exportFileToTarget(SignBook signBook, SignRequest signRequest, User user) throws EsupSignatureException {
        if (signBook.getTargetType() != null && !signBook.getTargetType().equals(DocumentIOType.none)) {
            Document signedFile = signRequestService.getLastSignedDocument(signRequest);
            if(signBook.getTargetType().equals(DocumentIOType.mail)) {
                logger.info("send by email to " + signBook.getDocumentsTargetUri());
                mailService.sendFile(signBook.getDocumentsTargetUri(), signedFile, signRequest);
                signRequest.setExportedDocumentURI("mail://" + signBook.getDocumentsTargetUri());
                return true;
            } else {
                try {
                    logger.info("send to " + signBook.getTargetType() + " in /" + signBook.getSignBookType().toString() + "/" + signBook.getDocumentsTargetUri());
                    FsAccessService fsAccessService = fsAccessFactory.getFsAccessService(signBook.getTargetType());
                    InputStream inputStream = signedFile.getInputStream();
                    fsAccessService.createFile("/", signBook.getSignBookType().toString(), "folder");
                    fsAccessService.createFile("/" + signBook.getSignBookType().toString(), signBook.getDocumentsTargetUri(), "folder");
                    signRequest.setExportedDocumentURI(fsAccessService.getUri() + "/" + signBook.getSignBookType().toString() + "/" + signBook.getDocumentsTargetUri() + "/" + signedFile.getFileName());
                    return fsAccessService.putFile("/" + signBook.getSignBookType().toString() + "/" + signBook.getDocumentsTargetUri() + "/", signedFile.getFileName(), inputStream, UploadActionType.OVERRIDE);
                } catch (Exception e) {
                    throw new EsupSignatureException("write fsaccess error : ", e);
                }
            }
        } else {
            logger.debug("no target type for this signbook");
        }
        return false;
    }

    public void importSignRequestInSignBook(SignRequest signRequest, SignBook signBook, User user) throws EsupSignatureException, IOException {
        if (!signBook.getSignRequests().contains(signRequest)) {
            signBook.getSignRequests().add(signRequest);
            if (signBook.getSignBookType().equals(SignBookType.workflow)) {
                //TODO a réparer
                List<String> recipients = new ArrayList<>();
                for(Long signBookId : signRequest.getCurrentWorkflowStep().getSignBooks().keySet()){
                    recipients.addAll(signBookRepository.findById(signBookId).get().getRecipientEmails());
                }
                importSignRequestByRecipients(signRequest, recipients, user);
                signRequestService.updateStatus(signRequest, SignRequestStatus.draft, "Envoyé dans le parapheur " + signRequest.getCurrentWorkflowStep().getSignBooks().toString(), user, "SUCCESS", "");
                if (signRequest.getSignBooksWorkflowStep() > 1) {
                    signRequestService.pendingSignRequest(signRequest, user);
                }
            } else {
                importSignRequestByRecipients(signRequest, signBook.getRecipientEmails(), user);
                signRequestService.updateStatus(signRequest, SignRequestStatus.draft, "Envoyé dans le parapheur " + signBook.getName(), user, "SUCCESS", "");
            }
        } else {
            logger.warn(signRequest.getId() + " is already in signbook" + signBook.getName());
        }
    }

    private void importSignRequestByRecipients(SignRequest signRequest, List<String> recipientEmails, User user) {
        for (String recipientEmail : recipientEmails) {
            SignBook signBook = getUserSignBookByRecipientEmail(recipientEmail);
            if (signBook.getRecipientEmails().contains("creator")) {
                signBook = getUserSignBook(user);
            }
            if(signRequest.getWorkflowSteps().size() < signRequest.getSignBooksWorkflowStep()) {
                WorkflowStep workflowStep = new WorkflowStep();
                workflowStep.setSignRequestParams(signBook.getSignRequestParams());
                workflowStepRepository.save(workflowStep);
                signRequest.getWorkflowSteps().add(workflowStep);
            }
            if (!signRequest.getCurrentWorkflowStep().getSignBooks().containsKey(signBook.getId())) {
                signRequest.getCurrentWorkflowStep().getSignBooks().put(signBook.getId(), false);
            } else {
                logger.warn(signRequest.getId() + " is already in signbook" + signBook.getName());
            }
        }
    }

    public void removeSignRequestFromAllSignBooks(SignRequest signRequest) {
        logger.info("remove from all signBook " + signRequest.getName());
        List<SignBook> signBooks = getSignBookBySignRequest(signRequest);
        for (SignBook signBook : signBooks) {
                signBook.getSignRequests().remove(signRequest);
				signBookRepository.save(signBook);
        }
        //signRequest.getCurrentWorkflowStep().getSignBooks().clear();
        signRequestRepository.save(signRequest);
    }

    public void removeSignRequestFromSignBook(SignRequest signRequest, SignBook signBook) {
        //signRequest.getCurrentWorkflowStep().getSignBooks().remove(signBook.getId());
        signBook.getSignRequests().remove(signRequest);
        //signRequest.getCurrentWorkflowStep().getSignBooks().clear();
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

    public List<SignBook> getOriginalSignBook(SignRequest signRequest) {
        return signBookRepository.findBySignRequests(Arrays.asList(signRequest));
    }

    public SignBook getUserSignBook(User user) {
        return getUserSignBookByRecipientEmail(user.getEmail());
    }

    public SignBook getUserSignBookByRecipientEmail(String recipientEmail) {
        if (signBookRepository.countByRecipientEmailsAndSignBookType(Arrays.asList(recipientEmail), SignBookType.user) > 0) {
            return signBookRepository.findByRecipientEmailsAndSignBookType(Arrays.asList(recipientEmail), SignBookType.user).get(0);
        } else {
            /*
            if (signBookRepository.countByRecipientEmailsAndSignBookType(Arrays.asList(recipientEmail), SignBookType.system) > 0) {
                return signBookRepository.findByRecipientEmailsAndSignBookType(Arrays.asList(recipientEmail), SignBookType.system).get(0);
            }
             */
            User user;
            if(userRepository.findByEmail(recipientEmail).size() > 0) {
                user = userRepository.findByEmail(recipientEmail).get(0);
            } else {
                user = userService.createUser(recipientEmail);
            }
            return getUserSignBook(user);
        }
    }

    public List<SignBook> getSignBookBySignRequest(SignRequest signRequest) {
        List<SignBook> signBooks = signBookRepository.findBySignRequests(Collections.singletonList(signRequest));
        return signBooks;
    }

    public boolean checkUserManageRights(User user, SignBook signBook) {
        if (signBook.getCreateBy().equals(user.getEppn()) || signBook.getModeratorEmails().contains(user.getEmail()) || signBook.getCreateBy().equals("System")) {
            return true;
        } else {
            return false;
        }
    }

}
