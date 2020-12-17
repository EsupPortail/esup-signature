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

    public boolean notInShare(String userEppn, String authUserEppn) {
        User user = userService.getByEppn(userEppn);
        User authUser = userService.getByEppn(authUserEppn);
        return user.equals(authUser);
    }

    public boolean dataUpdate(Long id, String userEppn) {
        User user = userService.getByEppn(userEppn);
        Data data = dataService.getById(id);
        return data.getCreateBy().equals(user.getEppn()) || data.getOwner().equals(user.getEppn());
    }

    public boolean signBookView(Long id, String userEppn) {
        User user = userService.getByEppn(userEppn);
        SignBook signBook = signBookService.getById(id);
        return signBookService.checkUserViewRights(user, signBook);
    }

    public boolean signBookManage(Long id, String userEppn) {
        User user = userService.getByEppn(userEppn);
        SignBook signBook = signBookService.getById(id);
        return signBookService.checkUserManageRights(user, signBook);
    }

    public boolean signBookManage(String name, String userEppn) {
        User user = userService.getByEppn(userEppn);
        SignBook signBook = signBookService.getByName(name);
        return signBookService.checkUserManageRights(user, signBook);
    }

    public boolean signRequestOwner(Long id, String userEppn) {
        User user = userService.getByEppn(userEppn);
        SignRequest signRequest = signRequestService.getById(id);
        return signRequest.getCreateBy().equals(user);
    }

    public boolean signRequestView(Long id, String userEppn, String authUserEppn) {
        User user = userService.getByEppn(userEppn);
        SignRequest signRequest = signRequestService.getById(id);
        return signRequestService.checkUserViewRights(signRequest, user, authUserEppn) || signRequestService.checkUserSignRights(signRequest, userEppn, authUserEppn);
    }

    public boolean signRequestSign(Long id, String userEppn, String authUserEppn) {
        SignRequest signRequest = signRequestService.getById(id);
        return signRequestService.checkUserSignRights(signRequest, userEppn, authUserEppn);
    }

    public boolean workflowOwner(Long id, User user) {
        Workflow workflow = workflowService.getById(id);
        return user.equals(workflow.getCreateBy()) || workflow.getCreateBy().equals(userService.getSystemUser());
    }

}
