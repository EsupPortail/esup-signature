package org.esupportail.esupsignature.web.controller.user;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.repository.DataRepository;
import org.esupportail.esupsignature.service.FormService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

@RequestMapping("/user/")
@Controller
@Transactional
@EnableConfigurationProperties(GlobalProperties.class)
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    @Resource
    private GlobalProperties globalProperties;

    @ModelAttribute("userMenu")
    public String getRoleMenu() {
        return "active";
    }

    @ModelAttribute("activeMenu")
    public String getActiveMenu() {
        return "home";
    }

    @ModelAttribute(value = "user", binding = false)
    public User getUser() {
        return userService.getCurrentUser();
    }

    @ModelAttribute(value = "authUser", binding = false)
    public User getAuthUser() {
        return userService.getUserFromAuthentication();
    }

    @ModelAttribute(value = "suUsers", binding = false)
    public List<User> getSuUsers() {
        return userService.getSuUsers(getAuthUser());
    }

    @Resource
    private UserService userService;

    @Resource
    private FormService formService;

    @Resource
    private WorkflowService workflowService;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private DataRepository dataRepository;

    @GetMapping
    public String list(@ModelAttribute User user, User authUser, Model model, @SortDefault(value = "createDate", direction = Sort.Direction.DESC) @PageableDefault(size = 100) Pageable pageable) {
        List<SignRequest> signRequestsToSign = signRequestService.getToSignRequests(user);
        model.addAttribute("signRequests", signRequestService.getSignRequestsPageGrouped(signRequestsToSign, pageable));
        List<Data> datas =  dataRepository.findByCreateByAndStatus(user.getEppn(), SignRequestStatus.draft);
        model.addAttribute("globalProperties", globalProperties);
        model.addAttribute("datas", datas);
        model.addAttribute("forms", formService.getFormsByUser(user, authUser));
        model.addAttribute("workflows", workflowService.getWorkflowsForUser(user));
        return "user/home/index";
    }

}
