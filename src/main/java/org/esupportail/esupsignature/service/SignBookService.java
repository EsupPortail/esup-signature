package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.SignBook.SignBookType;
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
public class SignBookService {

    private static final Logger logger = LoggerFactory.getLogger(SignBookService.class);

    @Resource
    private SignBookRepository signBookRepository;

    @Resource
    private SignRequestRepository signRequestRepository;
    @Resource
    private UserRepository userRepository;

    @Resource
    private UserService userService;

    public List<SignBook> getAllSignBooks() {
        List<SignBook> list = new ArrayList<SignBook>();
        signBookRepository.findAll().forEach(e -> list.add(e));
        return list;
    }

    public void updateSignBook(SignBook signBook, SignBook signBookToUpdate) throws EsupSignatureException {
        signBookToUpdate.getRecipientEmails().removeAll(signBook.getRecipientEmails());
        signBookToUpdate.getRecipientEmails().addAll(signBook.getRecipientEmails());
        signBookToUpdate.setName(signBook.getName());
        signBookRepository.save(signBook);

    }

    public void addRecipient(SignBook signBook, List<String> recipientEmails) {
        for (String recipientEmail : recipientEmails) {
            if (userRepository.countByEmail(recipientEmail) == 0) {
                userService.createUser(recipientEmail);
            }
            signBook.getRecipientEmails().add(recipientEmail);
        }
    }

    public void removeRecipient(SignBook signBook, String recipientEmail) {
        signBook.getRecipientEmails().remove(recipientEmail);
    }

    public SignBook createSignBook(String name, SignBookType signBookType, User user, boolean external) throws EsupSignatureException {
        if (signBookRepository.countByName(name) == 0) {
            SignBook signBook = new SignBook();
            signBook.setName(name);
            signBook.setSignBookType(signBookType);
            signBook.setCreateBy(user.getEppn());
            signBook.setCreateDate(new Date());
            signBook.setExternal(external);
            Document model = null;
            signBookRepository.save(signBook);
            if (model != null) {
                model.setParentId(signBook.getId());
            }
            return signBook;
        } else {
            throw new EsupSignatureException("all ready exist");
        }
    }

    public void addSignRequest(SignBook signBook, SignRequest signRequest) {
        signBook.getSignRequests().add(signRequest);
        signBookRepository.save(signBook);
    }

    public void deleteSignBook(SignBook signBook) {
        signBookRepository.delete(signBook);
    }

    public void removeSignRequestFromSignBook(SignBook signBook, SignRequest signRequest) {
        signRequestRepository.save(signRequest);
        signBook.getSignRequests().remove(signRequest);
        signBookRepository.save(signBook);
    }

    public boolean isUserInWorkflow(SignRequest signRequest, User user) {
        if (signRequest.getCurrentWorkflowStep() != null && signRequest.getCurrentWorkflowStep().getRecipients().size() > 0) {
            for (Map.Entry<Long, Boolean> userId : signRequest.getCurrentWorkflowStep().getRecipients().entrySet()) {
                if (userId.getKey().equals(user.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean checkUserManageRights(User user, SignBook signBook) {
        if (signBook.getCreateBy().equals(user.getEppn()) || signBook.getCreateBy().equals("System")) {
            return true;
        } else {
            return false;
        }
    }
}
