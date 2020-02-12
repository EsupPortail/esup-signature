package org.esupportail.esupsignature.web.controller.user;

import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.service.FormService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

@RequestMapping("/user/new")
@Controller
@Transactional
public class NewController {

    @ModelAttribute("userMenu")
    public String getRoleMenu() {
        return "active";
    }

    @ModelAttribute("user")
    public User getUser() {
        return userService.getUserFromAuthentication();
    }

    @Resource
    private UserService userService;

    @Resource
    private FormService formService;

    @Resource
    private WorkflowService workflowService;

    @ModelAttribute("activeMenu")
    public String getActiveMenu() {
        return "new";
    }

    @GetMapping
    public String list(Model model) {
        User user = userService.getUserFromAuthentication();
        List<Form> forms = formService.getAllForms();
        model.addAttribute("forms", forms);
        model.addAttribute("workflows", workflowService.getWorkflowsForUser(user));
        model.addAttribute("signTypes", Arrays.asList(SignType.values()));
        return "user/new";
    }

}
