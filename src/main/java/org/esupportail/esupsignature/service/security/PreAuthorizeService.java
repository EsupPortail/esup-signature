package org.esupportail.esupsignature.service.security;

import org.apache.commons.collections.CollectionUtils;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PreAuthorizeService {

    private final UserService userService;

    private final SignBookService signBookService;

    private final DataService dataService;

    private final SignRequestService signRequestService;

    private final DocumentService documentService;

    private final CommentService commentService;

    private final WorkflowService workflowService;

    private final FormService formService;

    private final ReportService reportService;

    private final UserShareService userShareService;

    public PreAuthorizeService(UserService userService, SignBookService signBookService, DataService dataService, SignRequestService signRequestService, DocumentService documentService, CommentService commentService, WorkflowService workflowService, FormService formService, ReportService reportService, UserShareService userShareService) {
        this.userService = userService;
        this.signBookService = signBookService;
        this.dataService = dataService;
        this.signRequestService = signRequestService;
        this.documentService = documentService;
        this.commentService = commentService;
        this.workflowService = workflowService;
        this.formService = formService;
        this.reportService = reportService;
        this.userShareService = userShareService;
    }

    public boolean notInShare(String userEppn, String authUserEppn) {
        return userEppn.equals(authUserEppn);
    }

    public boolean isManager(String userEppn) {
        if(userEppn != null) {
            User user = userService.getByEppn(userEppn);
            return !user.getManagersRoles().isEmpty();
        }
        return false;
    }

    public boolean workflowManage(Long id, String userEppn) {
        if(userEppn != null) {
            return workflowService.checkWorkflowManageRights(id, userEppn);
        }
        return false;
    }

    public boolean signBookView(Long id, String userEppn, String authUserEppn) {
        if(userEppn != null && authUserEppn != null) {
            return signBookService.checkUserViewRights(userEppn, authUserEppn, id);
        }
        return false;
    }

    public boolean documentCreator(Long documentId, String authUserEppn) {
        Document document = documentService.getById(documentId);
        SignRequest signRequest = signRequestService.getById(document.getParentId());
        return signRequest.getCreateBy().getEppn().equals(authUserEppn);
    }

    public boolean documentView(Long documentId, String userEppn, String authUserEppn) {
        Document document = documentService.getById(documentId);
        if(signRequestService.getById(document.getParentId()) != null) {
            return signRequestView(document.getParentId(), userEppn, authUserEppn);
        }
        return false;
    }

    public boolean signBookCreator(Long id, String userEppn) {
        SignBook signBook = signBookService.getById(id);
        return signBook != null && signBook.getCreateBy().getEppn().equals(userEppn);
    }

    public boolean signBookSendOtp(Long id, String userEppn) {
        SignBook signBook = signBookService.getById(id);
        User user = userService.getByEppn(userEppn);
        return signBook != null && (signBook.getCreateBy().getEppn().equals(userEppn)
                || signBook.getLiveWorkflow().getWorkflow().getManagers().contains(user.getEmail())
                || signBook.getLiveWorkflow().getWorkflow().getDashboardRoles().stream().anyMatch(user.getRoles()::contains)
                || user.getRoles().contains("ROLE_ADMIN"));
    }

    public boolean signBookManage(Long id, String userEppn) {
        if(userEppn != null) {
            return signBookService.checkUserManageRights(id, userEppn);
        }
        return false;
    }

    public boolean signRequestCreator(Long id, String userEppn) {
        if(userEppn != null) {
            SignRequest signRequest = signRequestService.getById(id);
            if(signRequest != null) {
                return signRequest.getCreateBy().getEppn().equals(userEppn);
            }
        }
        return false;
    }

    public boolean signRequestOwner(Long id, String userEppn) {
        if(userEppn != null) {
            SignRequest signRequest = signRequestService.getById(id);
            if(signRequest != null) {
                boolean isManager = isManager(userEppn, signRequest.getParentSignBook());
                return signRequest.getCreateBy().getEppn().equals(userEppn) || isManager;
            }
        }
        return false;
    }

    public boolean signRequestDelete(Long id, String userEppn) {
        if(userEppn != null) {
            SignRequest signRequest = signRequestService.getById(id);
            if(signRequest != null) {
                boolean isManager = isManager(userEppn, signRequest.getParentSignBook());
                return signRequestService.isDeletetable(signRequest, userEppn) && (signRequest.getCreateBy().getEppn().equals(userEppn) || isManager);
            }
        }
        return false;
    }

    private boolean isManager(String userEppn, SignBook signRequest) {
        User user = userService.getByEppn(userEppn);
        boolean isManager = false;
        if (signRequest.getLiveWorkflow().getWorkflow() != null) {
            Workflow workflow = workflowService.getById(signRequest.getLiveWorkflow().getWorkflow().getId());
            isManager = workflow.getManagers().contains(user.getEmail());
        }
        return isManager;
    }

    public boolean signBookOwner(Long id, String userEppn) {
        if(userEppn != null) {
            SignBook signBook = signBookService.getById(id);
            boolean isManager = isManager(userEppn, signBook);
            return signBook.getCreateBy().getEppn().equals(userEppn) || isManager;
        }
        return false;
    }

    public boolean signRequestRecipient(Long id, String userEppn) {
        if(userEppn != null) {
            SignRequest signRequest = signRequestService.getById(id);
            return (signRequest.getStatus().equals(SignRequestStatus.pending) &&
                    (signRequestService.isUserInRecipients(signRequest, userEppn) || signRequest.getCreateBy().getEppn().equals(userEppn)))
                    || (signRequest.getStatus().equals(SignRequestStatus.draft) && signRequest.getCreateBy().getEppn().equals(userEppn));
        }
        return false;
    }

    public boolean signRequestRecipientAndViewers(Long id, String userEppn) {
        if(userEppn != null) {
            SignRequest signRequest = signRequestService.getById(id);
            return (signRequest.getStatus().equals(SignRequestStatus.pending) &&
                    (signRequestService.isUserInRecipients(signRequest, userEppn) || signRequest.getParentSignBook().getViewers().stream().anyMatch(u -> u.getEppn().equals(userEppn)) || signRequest.getCreateBy().getEppn().equals(userEppn)))
                    || (signRequest.getStatus().equals(SignRequestStatus.draft) && signRequest.getCreateBy().getEppn().equals(userEppn));
        }
        return false;
    }

    public boolean reportOwner(Long id, String userEppn) {
        if(userEppn != null) {
            Report report = reportService.getById(id);
            return report.getUser().getEppn().equals(userEppn);
        }
        return false;
    }

    public boolean signRequestView(Long id, String userEppn, String authUserEppn) {
        if(userEppn != null && authUserEppn != null) {
            SignRequest signRequest = signRequestService.getById(id);
            if (signRequest != null) {
                return checkUserViewRights(signRequest, userEppn, authUserEppn) || signBookService.checkUserViewRights(userEppn, authUserEppn, signRequest.getParentSignBook().getId());
            }
        }
        return false;
    }

    public boolean attachmentCreator(Long id, String userEppn, String authUserEppn) {
        if(userEppn != null && authUserEppn != null) {
            User user = userService.getByEppn(userEppn);
            Document document = documentService.getById(id);
            if(document.getCreateBy().equals(user)) {
                return true;
            }
        }
        return false;
    }

    public boolean signRequestSign(Long id, String userEppn, String authUserEppn) {
        if(userEppn != null && authUserEppn != null) {
            SignRequest signRequest = signRequestService.getById(id);
            if(signRequest != null) {
                return signBookService.checkUserSignRights(signRequest, userEppn, authUserEppn);
            }
        }
        return false;
    }

    public boolean workflowOwner(Long id, String userEppn) {
        if(userEppn != null) {
            Workflow workflow = workflowService.getById(id);
            if(workflow != null) {
                return userEppn.equals(workflow.getCreateBy().getEppn()) || workflow.getCreateBy().equals(userService.getSystemUser());
            }
        }
        return false;
    }

    public boolean workflowManager(Long id, String userEppn) {
        if(userEppn != null) {
            Workflow workflow = workflowService.getById(id);
            User manager = userService.getByEppn(userEppn);
            return workflow.getCreateBy().getEppn().equals(manager.getEppn()) || CollectionUtils.containsAny(manager.getManagersRoles(), workflow.getRoles());
        }
        return false;
    }

    public boolean formManager(Long id, String userEppn) {
        if(userEppn != null) {
            Form form = formService.getById(id);
            User manager = userService.getByEppn(userEppn);
            return form.getManagerRole() != null && manager.getManagersRoles().contains(form.getManagerRole());
        }
        return false;
    }

    public boolean roleManager(String role, String userEppn) {
        if(userEppn != null) {
            User manager = userService.getByEppn(userEppn);
            return manager.getManagersRoles().contains(role);
        }
        return false;
    }

    public User checkShareForSignRequest(SignRequest signRequest, String authUserEppn) {
        SignBook signBook = signRequest.getParentSignBook();
        if(signBook != null) {
            List<UserShare> userShares = userShareService.getByToUsersEppnInAndShareTypesContains(Collections.singletonList(authUserEppn), ShareType.sign);
            for (UserShare userShare : userShares) {
                Workflow workflow = signRequest.getParentSignBook().getLiveWorkflow().getWorkflow();
                if(userShare.getWorkflow().equals(workflow) && signBookService.checkUserSignRights(signRequest, userShare.getUser().getEppn(), authUserEppn)) {
                    return userShare.getUser();
                }
            }
            Data data = dataService.getBySignBook(signBook);
            if(data !=  null) {
                for (UserShare userShare : userShares) {
                    if (userShare.getForm().equals(data.getForm()) && signBookService.checkUserSignRights(signRequest, userShare.getUser().getEppn(), authUserEppn)) {
                        return userShare.getUser();
                    }
                }
            }
        }
        return null;
    }

    public boolean checkUserViewRights(SignRequest signRequest, String userEppn, String authUserEppn) {
        if(userEppn != null && authUserEppn != null) {
            User user = userService.getByEppn(userEppn);
            if (userEppn.equals(authUserEppn) || signBookService.checkAllShareTypesForSignRequest(userEppn, authUserEppn, signRequest.getParentSignBook().getId())) {
                List<SignRequest> signRequests = signRequestService.getByIdAndRecipient(signRequest.getId(), userEppn);
                Data data = dataService.getBySignBook(signRequest.getParentSignBook());
                User authUser = userService.getByEppn(authUserEppn);
                return (data != null && (data.getForm() != null && data.getForm().getWorkflow() != null && data.getForm().getWorkflow().getManagers().contains(authUser.getEmail())))
                        ||
                        (signRequest.getParentSignBook().getLiveWorkflow().getWorkflow() != null && signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getManagers().contains(authUser.getEmail()))
                        || signRequest.getCreateBy().getEppn().equals(userEppn)
                        || signRequest.getParentSignBook().getViewers().contains(userService.getByEppn(authUserEppn))
                        || signRequest.getParentSignBook().getLiveWorkflow().getLiveWorkflowSteps().stream().map(LiveWorkflowStep::getUsers).anyMatch(users -> users.contains(user))
                        || !signRequests.isEmpty();
            }
        }
        return false;
    }

    public boolean commentCreator(Long postitId, String userEppn) {
        Comment comment = commentService.getById(postitId);
        return comment.getCreateBy().getEppn().equals(userEppn);
    }
}
