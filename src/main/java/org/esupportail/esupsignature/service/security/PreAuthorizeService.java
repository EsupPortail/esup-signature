package org.esupportail.esupsignature.service.security;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Transactional
public class PreAuthorizeService {

    @Resource
    private UserService userService;

    @Resource
    private DataService dataService;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private SignBookService signBookService;

    @Resource
    private WorkflowService workflowService;

    public boolean notInShare(Long userId, Long authUserId) {
        User user = userService.getUserById(userId);
        User authUser = userService.getUserById(authUserId);
        return user.equals(authUser);
    }

    public boolean dataUpdate(Long id, Long userId) {
        User user = userService.getUserById(userId);
        Data data = dataService.getById(id);
        return data.getCreateBy().equals(user.getEppn()) || data.getOwner().equals(user.getEppn());
    }

    public boolean signBookView(Long id, Long userId) {
        User user = userService.getUserById(userId);
        SignBook signBook = signBookService.getById(id);
        return signBookService.checkUserViewRights(user, signBook);
    }

    public boolean signBookManage(Long id, Long userId) {
        User user = userService.getUserById(userId);
        SignBook signBook = signBookService.getById(id);
        return signBookService.checkUserManageRights(user, signBook);
    }

    public boolean signBookManage(String name, Long userId) {
        User user = userService.getUserById(userId);
        SignBook signBook = signBookService.getByName(name);
        return signBookService.checkUserManageRights(user, signBook);
    }

    public boolean signRequestOwner(Long id, Long userId) {
        User user = userService.getUserById(userId);
        SignRequest signRequest = signRequestService.getById(id);
        return signRequest.getCreateBy().equals(user);
    }

    public boolean signRequestView(Long id, Long userId, Long authUserId) {
        User user = userService.getUserById(userId);
        SignRequest signRequest = signRequestService.getById(id);
        return signRequestService.checkUserViewRights(signRequest, user, authUserId) || signRequestService.checkUserSignRights(signRequest, userId, authUserId);
    }

    public boolean signRequestSign(Long id, Long userId, Long authUserId) {
        SignRequest signRequest = signRequestService.getById(id);
        return signRequestService.checkUserSignRights(signRequest, userId, authUserId);
    }


    public boolean workflowOwner(String name, User user) {
        Workflow workflow = workflowService.getWorkflowByName(name);
        return user.equals(workflow.getCreateBy()) || workflow.getCreateBy().equals(userService.getSystemUser());
    }

    public boolean workflowOwner(Long id, User user) {
        Workflow workflow = workflowService.getById(id);
        return user.equals(workflow.getCreateBy()) || workflow.getCreateBy().equals(userService.getSystemUser());
    }

}
