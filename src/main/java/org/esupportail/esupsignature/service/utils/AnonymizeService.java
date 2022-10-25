package org.esupportail.esupsignature.service.utils;

import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
public class AnonymizeService {

    @Resource
    private SignBookService signBookService;

    @Resource
    private UserService userService;

    @Resource
    private WorkflowService workflowService;

    @Resource
    private WorkflowStepService workflowStepService;

    @Resource
    private LogService logService;

    @Resource
    private UserPropertieService userPropertieService;

    @Resource
    private CommentService commentService;

    @Resource
    private DataService dataService;

    @Resource
    private UserShareService userShareService;

    @Resource
    private RecipientService recipientService;

    @Resource
    private ReportService reportService;

    @Resource
    private FieldPropertieService fieldPropertieService;

    @Resource
    private SignRequestService signRequestService;

    @Transactional
    public void anonymize(Long id, Boolean force) throws EsupSignatureUserException {
        User user = userService.getById(id);
        User anonymous = userService.getAnonymousUser();
        List<SignBook> signBooks = signBookService.getSignBookForUsers(user.getEppn());
        if((force == null || !force) && signBooks.stream().anyMatch(signBook -> signBook.getStatus().equals(SignRequestStatus.pending))) {
            throw new EsupSignatureUserException("L'utilisateur possède des demandes en cours, merci de les transférer à un autre utilisateur");
        }
        for(SignBook signBook : signBooks) {
            signBook.getTeam().removeIf(user1 -> user1.equals(user));
            if(signBook.getCreateBy().equals(user)) {
                signBook.setCreateBy(anonymous);
            }
        }
        signBookService.anonymize(user.getEppn(), anonymous);
        signRequestService.anonymize(user.getEppn(), anonymous);
        dataService.anonymize(user.getEppn(), anonymous);
        commentService.anonymizeComment(id);
        workflowStepService.anonymize(user.getEppn());
        workflowService.anonymize(user.getEppn(), anonymous);
        reportService.anonymize(user.getEppn());
        recipientService.anonymze(user, anonymous);
        logService.anonymize(user.getEppn());
        userPropertieService.deleteAll(user.getEppn());
        userShareService.deleteAll(user.getEppn());
        fieldPropertieService.deleteAll(user.getEppn());

        userService.anonymize(id);
        userService.delete(id);
    }

}
