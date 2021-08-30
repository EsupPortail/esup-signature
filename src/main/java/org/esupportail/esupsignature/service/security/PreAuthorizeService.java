package org.esupportail.esupsignature.service.security;

import org.apache.commons.collections.CollectionUtils;
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

    @Resource
    private FormService formService;

    @Resource
    private ReportService reportService;

    public boolean notInShare(String userEppn, String authUserEppn) {
        return userEppn.equals(authUserEppn);
    }

    public boolean formManage(Long id, String userEppn) {
        Form form = formService.getById(id);
        User user = userService.getByEppn(userEppn);
        return form.getManagers().contains(user.getEmail());
    }

    public boolean signBookView(Long id, String userEppn) {
        SignBook signBook = signBookService.getById(id);
        return signBookService.checkUserViewRights(userEppn, signBook);
    }

    public boolean signBookManage(Long id, String userEppn) {
        SignBook signBook = signBookService.getById(id);
        return signBookService.checkUserManageRights(userEppn, signBook);
    }

    public boolean signRequestOwner(Long id, String userEppn) {
        SignRequest signRequest = signRequestService.getById(id);
        return signRequest.getCreateBy().getEppn().equals(userEppn);
    }

    public boolean reportOwner(Long id, String userEppn) {
        Report report = reportService.getById(id);
        return report.getUser().getEppn().equals(userEppn);
    }

    public boolean signRequestView(Long id, String userEppn, String authUserEppn) {
        SignRequest signRequest = signRequestService.getById(id);
        if(signRequest != null) {
            return signRequestService.checkUserViewRights(signRequest, userEppn, authUserEppn) || signRequestService.checkUserSignRights(signRequest, userEppn, authUserEppn);
        } else {
            return false;
        }
    }

    public boolean signRequestSign(Long id, String userEppn, String authUserEppn) {
        SignRequest signRequest = signRequestService.getById(id);
        return signRequestService.checkUserSignRights(signRequest, userEppn, authUserEppn);
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
}
