package org.esupportail.esupsignature.web.controller.user;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.repository.DataRepository;
import org.esupportail.esupsignature.service.FormService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.esupportail.esupsignature.service.file.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RequestMapping("/user/")
@Controller
@Transactional
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    @ModelAttribute("userMenu")
    public String getRoleMenu() {
        return "active";
    }

    @ModelAttribute("activeMenu")
    public String getActiveMenu() {
        return "home";
    }

    @ModelAttribute("user")
    public User getUser(@RequestParam(required = false) String suEppn) {
        return userService.getUserFromSu(suEppn);
    }

    @ModelAttribute("suUsers")
    public List<User> getSuUsers() {
        return userService.getSuUsers();
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
    public String list(User user, Model model, @SortDefault(value = "createDate", direction = Sort.Direction.DESC) @PageableDefault(size = 100) Pageable pageable) {
        List<SignRequest> signRequestsToSign = signRequestService.getToSignRequests(user);
        model.addAttribute("signRequests", signRequestService.getSignRequestsPageGrouped(signRequestsToSign, pageable));
        List<Data> datas =  dataRepository.findByCreateByAndStatus(user.getEppn(), SignRequestStatus.draft);
        model.addAttribute("datas", datas);
        model.addAttribute("forms", formService.getFormsByUser(user));
        model.addAttribute("workflows", workflowService.getWorkflowsForUser(user));
        model.addAttribute("signTypes", Arrays.asList(SignType.values()));
        return "user/home/index";
    }

}
