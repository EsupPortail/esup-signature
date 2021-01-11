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
        return userEppn.equals(authUserEppn);
    }

    public boolean dataUpdate(Long id, String userEppn) {
        Data data = dataService.getById(id);
        return data.getCreateBy().equals(userEppn) || data.getOwner().equals(userEppn);
    }

    public boolean signBookView(Long id, String userEppn) {
        SignBook signBook = signBookService.getById(id);
        return signBookService.checkUserViewRights(userEppn, signBook);
    }

    public boolean signBookManage(Long id, String userEppn) {
        SignBook signBook = signBookService.getById(id);
        return signBookService.checkUserManageRights(userEppn, signBook);
    }

    public boolean signBookManage(String name, String userEppn) {
        SignBook signBook = signBookService.getByName(name);
        return signBookService.checkUserManageRights(userEppn, signBook);
    }

    public boolean signRequestOwner(Long id, String userEppn) {
        SignRequest signRequest = signRequestService.getById(id);
        return signRequest.getCreateBy().getEppn().equals(userEppn);
    }

    public boolean signRequestView(Long id, String userEppn, String authUserEppn) {
        SignRequest signRequest = signRequestService.getById(id);
        return signRequestService.checkUserViewRights(signRequest, userEppn, authUserEppn) || signRequestService.checkUserSignRights(signRequest, userEppn, authUserEppn);
    }

    public boolean signRequestSign(Long id, String userEppn, String authUserEppn) {
        SignRequest signRequest = signRequestService.getById(id);
        return signRequestService.checkUserSignRights(signRequest, userEppn, authUserEppn);
    }

    public boolean workflowOwner(Long id, String userEppn) {
        Workflow workflow = workflowService.getById(id);
        return userEppn.equals(workflow.getCreateBy().getEppn()) || workflow.getCreateBy().equals(userService.getSystemUser());
    }

}
