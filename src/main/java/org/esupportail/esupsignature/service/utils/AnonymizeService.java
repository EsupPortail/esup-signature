package org.esupportail.esupsignature.service.utils;

import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.UserShare;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AnonymizeService {

    private final SignBookService signBookService;
    
    private final UserService userService;
    
    private final WorkflowService workflowService;
    
    private final WorkflowStepService workflowStepService;
    
    private final LogService logService;
    
    private final UserPropertieService userPropertieService;
    
    private final CommentService commentService;
    
    private final DataService dataService;
    
    private final UserShareService userShareService;
    
    private final RecipientService recipientService;
    
    private final ReportService reportService;
    
    private final FieldPropertieService fieldPropertieService;
    
    private final SignRequestService signRequestService;
    
    private final MessageService messageService;
    
    private final DocumentService documentService;

    public AnonymizeService(SignBookService signBookService, UserService userService, WorkflowService workflowService, WorkflowStepService workflowStepService, LogService logService, UserPropertieService userPropertieService, CommentService commentService, DataService dataService, UserShareService userShareService, RecipientService recipientService, ReportService reportService, FieldPropertieService fieldPropertieService, SignRequestService signRequestService, MessageService messageService, DocumentService documentService) {
        this.signBookService = signBookService;
        this.userService = userService;
        this.workflowService = workflowService;
        this.workflowStepService = workflowStepService;
        this.logService = logService;
        this.userPropertieService = userPropertieService;
        this.commentService = commentService;
        this.dataService = dataService;
        this.userShareService = userShareService;
        this.recipientService = recipientService;
        this.reportService = reportService;
        this.fieldPropertieService = fieldPropertieService;
        this.signRequestService = signRequestService;
        this.messageService = messageService;
        this.documentService = documentService;
    }

    @Transactional
    public void anonymize(Long id, Boolean force) throws EsupSignatureUserException {
        User user = userService.getById(id);
        User anonymous = userService.getAnonymousUser();
        List<SignBook> signBooks = signBookService.getSignBookForUsers(user.getEppn());
        if((force == null || !force) && signBooks.stream().anyMatch(signBook -> signBook.getStatus().equals(SignRequestStatus.pending))) {
            throw new EsupSignatureUserException("L'utilisateur possède des demandes en cours, merci de les transférer à un autre utilisateur");
        }
        for(SignBook signBook : signBooks) {
            if(signBook.getCreateBy().equals(user)) {
                signBook.setCreateBy(anonymous);
            }
            signBook.getViewers().remove(user);
            signBook.getTeam().remove(user);
        }
        signBookService.anonymize(user.getEppn(), anonymous);
        signRequestService.anonymize(user.getEppn(), anonymous);
        documentService.anoymize(user.getEppn(), anonymous);
        dataService.anonymize(user.getEppn(), anonymous);
        commentService.anonymizeComment(id);
        workflowStepService.anonymize(user.getEppn());
        workflowService.anonymize(user.getEppn(), anonymous);
        reportService.anonymize(user.getEppn());
        recipientService.anonymze(user, anonymous);
        logService.anonymize(user.getEppn());
        userPropertieService.deleteAll(user.getEppn());
        userShareService.deleteAll(user.getEppn());
        messageService.anonymize(user);
        List<UserShare> userShares = userShareService.getUserSharesToUser(user.getEppn());
        for(UserShare userShare : userShares) {
            userShareService.delete(userShare);
        }
        fieldPropertieService.deleteAll(user.getEppn());

        userService.anonymize(id);
        userService.delete(id);
    }

}
