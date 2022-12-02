package org.esupportail.esupsignature.web.controller.user;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.UiParams;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.DataRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@RequestMapping("/user/")
@Controller
@EnableConfigurationProperties(GlobalProperties.class)
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    private final GlobalProperties globalProperties;

    @Resource
    private SignRequestRepository signRequestRepository;

    public HomeController(GlobalProperties globalProperties) {
        this.globalProperties = globalProperties;
    }

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
    private SignBookService signBookService;

    @Resource
    private DataRepository dataRepository;

    @Resource
    private TemplateEngine templateEngine;

    @Resource
    private MessageService messageService;

    @Resource
    private UserService userService;

    @Resource
    private UserShareService userShareService;

    @GetMapping
    public String home(@ModelAttribute("userEppn") String userEppn,
                       @ModelAttribute("authUserEppn") String authUserEppn,
                       @RequestParam(required = false, name = "formId") Long formId,
                       Model model, @SortDefault(value = "createDate", direction = Sort.Direction.DESC) @PageableDefault(size = 100) Pageable pageable) throws EsupSignatureUserException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        User authUser = userService.getUserByEppn(authUserEppn);
        if(authUser != null) {
            List<SignRequest> oldSignRequests = new ArrayList<>();
            if(globalProperties.getNbDaysBeforeWarning() > - 1) {
                oldSignRequests = signRequestRepository.findByCreateByEppnAndOlderPending(authUser.getId(), globalProperties.getNbDaysBeforeWarning());
            }
            model.addAttribute("oldSignRequests", oldSignRequests);
            List<SignRequest> recipientNotPresentSignRequests = signRequestService.getRecipientNotPresentSignRequests(userEppn);
            model.addAttribute("recipientNotPresentSignRequests", recipientNotPresentSignRequests);
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
            List<SignBook> signBooksToSign = signBookService.getSignBooks(userEppn, "toSign", null, null, null, null, null, pageable).toList();
            List<UserShare> userShares = userShareService.getUserSharesByUser(userEppn);
            List<Workflow> workflows = userShares.stream().map(UserShare::getWorkflow).collect(Collectors.toList());
            if(!userEppn.equals(authUserEppn)) {
                signBooksToSign = signBooksToSign.stream().filter(sb -> sb.getSignRequests().size() > 0 && userShareService.checkAllShareTypesForSignRequest(userEppn, authUserEppn, sb.getSignRequests().get(0))).collect(Collectors.toList());
                if (userShares.stream().noneMatch(us -> us.getAllSignRequests() != null && us.getAllSignRequests())) {
                    signBooksToSign = signBooksToSign.stream().filter(signBook -> workflows.contains(signBook.getLiveWorkflow().getWorkflow())).collect(Collectors.toList());
                }
            }
            model.addAttribute("signBooksToSign", signBooksToSign);
            List<SignBook> signBooksPending = new ArrayList<>();
            if(userEppn.equals(authUserEppn)) {
                signBooksPending = signBookService.getSignBooks(userEppn, "pending", null, null, null, null, null, pageable).toList();
            }
            model.addAttribute("signBooksPending", signBooksPending);
            List<Data> datas = dataRepository.findByCreateByAndStatus(authUser, SignRequestStatus.draft);
            model.addAttribute("datas", datas);
            model.addAttribute("forms", formService.getFormsByUser(userEppn, authUserEppn));
            model.addAttribute("workflows", workflowService.getWorkflowsByUser(userEppn, authUserEppn));
            model.addAttribute("uiParams", userService.getUiParams(authUserEppn));
            model.addAttribute("startFormId", formId);
            return "user/home/index";
        } else {
            throw new EsupSignatureUserException("not reconized user");
        }
    }

    @GetMapping("/start-form/{formId}")
    public String startForm(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable Long formId) {
        return "redirect:/user/?formId=" + formId;
    }

}
