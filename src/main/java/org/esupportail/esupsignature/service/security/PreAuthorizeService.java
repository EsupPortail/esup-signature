package org.esupportail.esupsignature.service.security;

import org.apache.commons.collections.CollectionUtils;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.repository.UserShareRepository;
import org.esupportail.esupsignature.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PreAuthorizeService {

    @Resource
    private UserService userService;

    @Resource
    private UserShareService userShareService;

    @Resource
    private SignBookService signBookService;

    @Resource
    private WorkflowService workflowService;

    @Resource
    private FormService formService;

    @Resource
    private ReportService reportService;

    @Resource
    private UserShareRepository userShareRepository;

    @Resource
    private SignRequestRepository signRequestRepository;

    public boolean notInShare(String userEppn, String authUserEppn) {
        return userEppn.equals(authUserEppn);
    }

    public boolean isManager(String userEppn) {
        User user = userService.getUserByEppn(userEppn);
        return user.getManagersRoles().size() > 0;
    }

    public boolean formManage(Long id, String userEppn) {
        Form form = formService.getById(id);
        User user = userService.getByEppn(userEppn);
        return form.getManagers().contains(user.getEmail());
    }

    public boolean signBookView(Long id, String userEppn, String authUserEppn) {
        SignBook signBook = signBookService.getById(id);
        return userShareService.checkUserViewRights(userEppn, authUserEppn, signBook);
    }

    public boolean signBookManage(Long id, String userEppn) {
        SignBook signBook = signBookService.getById(id);
        if(signBook != null) {
            return signBookService.checkUserManageRights(userEppn, signBook);
        } else {
            return true;
        }
    }

    public boolean signRequestOwner(Long id, String userEppn) {
        SignRequest signRequest = signRequestRepository.findById(id).get();
        User user = userService.getUserByEppn(userEppn);
        boolean isManager = false;
        if(signRequest.getParentSignBook().getLiveWorkflow().getWorkflow() != null) {
            Workflow workflow = workflowService.getById(signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getId());
            isManager = workflow.getManagers().contains(user.getEmail());
        }
        return signRequest.getCreateBy().getEppn().equals(userEppn) || isManager;
    }

    public boolean signRequestRecipent(Long id, String userEppn) {
        SignRequest signRequest = signRequestRepository.findById(id).get();
        return (signRequest.getStatus().equals(SignRequestStatus.pending) &&
                (signBookService.isUserInRecipients(signRequest, userEppn) || signRequest.getCreateBy().getEppn().equals(userEppn)))
                || (signRequest.getStatus().equals(SignRequestStatus.draft) && signRequest.getCreateBy().getEppn().equals(userEppn));
    }

    public boolean reportOwner(Long id, String userEppn) {
        Report report = reportService.getById(id);
        return report.getUser().getEppn().equals(userEppn);
    }

    public boolean signRequestView(Long id, String userEppn, String authUserEppn) {
        Optional<SignRequest> signRequest = signRequestRepository.findById(id);
        if(signRequest.isPresent()) {
            return checkUserViewRights(signRequest.get(), userEppn, authUserEppn) || signBookService.checkUserSignRights(signRequest.get(), userEppn, authUserEppn);
        } else {
            return false;
        }
    }

    public boolean signRequestSign(Long id, String userEppn, String authUserEppn) {
        SignRequest signRequest = signRequestRepository.findById(id).get();
        return signBookService.checkUserSignRights(signRequest, userEppn, authUserEppn);
    }

    public boolean workflowOwner(Long id, String userEppn) {
        Workflow workflow = workflowService.getById(id);
        return userEppn.equals(workflow.getCreateBy().getEppn()) || workflow.getCreateBy().equals(userService.getSystemUser());
    }

    public boolean workflowManager(Long id, String userEppn) {
        Workflow workflow = workflowService.getById(id);
        User manager = userService.getByEppn(userEppn);
        return workflow.getCreateBy().getEppn().equals(manager.getEppn()) || CollectionUtils.containsAny(manager.getManagersRoles(), workflow.getRoles());
    }

    public boolean formManager(Long id, String userEppn) {
        Form form = formService.getById(id);
        User manager = userService.getByEppn(userEppn);
        return CollectionUtils.containsAny(manager.getManagersRoles(), form.getRoles());
    }

    public boolean roleManager(String role, String userEppn) {
        User manager = userService.getByEppn(userEppn);
        return manager.getManagersRoles().contains(role);
    }

    public User checkShareForSignRequest(SignRequest signRequest, String authUserEppn) {
        SignBook signBook = signRequest.getParentSignBook();
        if(signBook != null) {
            List<UserShare> userShares = userShareRepository.findByToUsersEppnInAndShareTypesContains(Collections.singletonList(authUserEppn), ShareType.sign);
            for (UserShare userShare : userShares) {
                Workflow workflow = signRequest.getParentSignBook().getLiveWorkflow().getWorkflow();
                if(userShare.getWorkflow().equals(workflow) && signBookService.checkUserSignRights(signRequest, userShare.getUser().getEppn(), authUserEppn)) {
                    return userShare.getUser();
                }
            }
            Data data = signBookService.getBySignBook(signBook);
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
        User user = userService.getUserByEppn(userEppn);
        if(userEppn.equals(authUserEppn) || userShareService.checkAllShareTypesForSignRequest(userEppn, authUserEppn, signRequest)) {
            List<SignRequest> signRequests = signRequestRepository.findByIdAndRecipient(signRequest.getId(), userEppn);
            Data data = signBookService.getBySignBook(signRequest.getParentSignBook());
            User authUser = userService.getUserByEppn(authUserEppn);
            if((data != null && (data.getForm() != null && data.getForm().getManagers().contains(authUser.getEmail())))
                    || signRequest.getCreateBy().getEppn().equals(userEppn)
                    || signRequest.getParentSignBook().getViewers().contains(userService.getUserByEppn(authUserEppn))
                    || signRequest.getParentSignBook().getLiveWorkflow().getLiveWorkflowSteps().stream().map(LiveWorkflowStep::getUsers).anyMatch(users -> users.contains(user))
                    || signRequests.size() > 0) {
                return true;
            }
        }
        return false;
    }
}
