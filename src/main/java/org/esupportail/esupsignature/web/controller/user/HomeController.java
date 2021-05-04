package org.esupportail.esupsignature.web.controller.user;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.Message;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.UiParams;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.DataRepository;
import org.esupportail.esupsignature.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequestMapping("/user/")
@Controller
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    @Resource
    private GlobalProperties globalProperties;

    @ModelAttribute("activeMenu")
    public String getActiveMenu() {
        return "home";
    }

    @Resource
    private FormService formService;

    @Resource
    private WorkflowService workflowService;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private DataRepository dataRepository;

    @Resource
    private TemplateEngine templateEngine;

    @Resource
    private MessageService messageService;

    @Resource UserService userService;

    @GetMapping
    public String list(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, Model model, @SortDefault(value = "createDate", direction = Sort.Direction.DESC) @PageableDefault(size = 100) Pageable pageable) throws EsupSignatureUserException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        User authUser = (User) model.getAttribute("authUser");
        if(authUser != null) {
            List<Message> messages = new ArrayList<>();
            if ((authUser.getUiParams().get(UiParams.homeHelp) == null) && globalProperties.getEnableSplash() && !authUser.getEppn().equals("system")) {
                Context ctx = new Context(Locale.FRENCH);
                ctx.setVariable("globalProperties", globalProperties);
                ctx.setVariable("splashMessage", true);
                Message splashMessage = new Message();
                splashMessage.setText(templateEngine.process("fragments/help.html", ctx));
                splashMessage.setId(0L);
                model.addAttribute("splashMessage", splashMessage);
            } else if (!authUserEppn.equals("system") && userEppn.equals(authUserEppn)) {
                messages.addAll(messageService.getByUser(authUser));
            }
            model.addAttribute("messageNews", messages);
            model.addAttribute("signRequests", new PageImpl<>(signRequestService.getToSignRequests(userEppn), pageable, signRequestService.getToSignRequests(userEppn).size()));
            List<Data> datas = dataRepository.findByCreateByAndStatus(authUser, SignRequestStatus.draft);
            model.addAttribute("datas", datas);
            model.addAttribute("forms", formService.getFormsByUser(userEppn, authUserEppn));
            model.addAttribute("workflows", workflowService.getWorkflowsByUser(userEppn, authUserEppn));
            model.addAttribute("uiParams", userService.getUiParams(authUserEppn));
            return "user/home/index";
        } else {
            throw new EsupSignatureUserException("not reconized user");
        }
    }

}
