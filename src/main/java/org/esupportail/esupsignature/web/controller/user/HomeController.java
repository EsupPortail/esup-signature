package org.esupportail.esupsignature.web.controller.user;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dto.json.SearchResult;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.UiParams;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.DataRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequestMapping("/user")
@Controller
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);
    private final TagService tagService;

    @ModelAttribute("activeMenu")
    public String getActiveMenu() {
        return "home";
    }

    private final GlobalProperties globalProperties;
    private final SignRequestRepository signRequestRepository;
    private final FormService formService;
    private final WorkflowService workflowService;
    private final SignRequestService signRequestService;
    private final SignBookService signBookService;
    private final DataRepository dataRepository;
    private final TemplateEngine templateEngine;
    private final MessageService messageService;
    private final UserService userService;

    public HomeController(GlobalProperties globalProperties, SignRequestRepository signRequestRepository, FormService formService, WorkflowService workflowService, SignRequestService signRequestService, SignBookService signBookService, DataRepository dataRepository, TemplateEngine templateEngine, MessageService messageService, UserService userService, TagService tagService) {
        this.globalProperties = globalProperties;
        this.signRequestRepository = signRequestRepository;
        this.formService = formService;
        this.workflowService = workflowService;
        this.signRequestService = signRequestService;
        this.signBookService = signBookService;
        this.dataRepository = dataRepository;
        this.templateEngine = templateEngine;
        this.messageService = messageService;
        this.userService = userService;
        this.tagService = tagService;
    }

    @GetMapping(value = {"", "/"})
    public String home(@ModelAttribute("userEppn") String userEppn,
                       @ModelAttribute("authUserEppn") String authUserEppn,
                       @RequestParam(required = false, name = "formId") Long formId,
                       Model model, @SortDefault(value = "createDate", direction = Sort.Direction.DESC) @PageableDefault(size = 100) Pageable pageable) throws EsupSignatureUserException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        User authUser = userService.getByEppn(authUserEppn);
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
                messages.addAll(messageService.getByUserNeverRead(authUser));
            }
            model.addAttribute("messageNews", messages);
            List<SignBook> signBooksToSign = signBookService.getSignBooks(userEppn, authUserEppn, "toSign", null, null, null, null, null, pageable).toList();
            model.addAttribute("signBooksToSign", signBooksToSign);
            List<SignBook> signBooksPending = new ArrayList<>();
            if(userEppn.equals(authUserEppn)) {
                signBooksPending = signBookService.getSignBooks(userEppn, authUserEppn, "pending", null, null, null, null, null, pageable).toList();
            }
            model.addAttribute("signBooksPending", signBooksPending);
            List<Data> datas = dataRepository.findByCreateByAndStatus(authUser, SignRequestStatus.draft);
            model.addAttribute("datas", datas);
            model.addAttribute("forms", formService.getFormsByUser(userEppn, authUserEppn));
            model.addAttribute("workflows", workflowService.getWorkflowsByUser(userEppn, authUserEppn));
            model.addAttribute("startFormId", formId);
            model.addAttribute("allTags", tagService.getAllTags(Pageable.unpaged()).getContent());
            model.addAttribute("selectedTags", new ArrayList<>());
            model.addAttribute("favoriteWorkflows", workflowService.getByIds(userEppn, authUserEppn));
            model.addAttribute("favoriteForms", formService.getByIds(userEppn, authUserEppn));

            return "user/home/index";
        } else {
            throw new EsupSignatureUserException("not reconized user");
        }
    }

    @GetMapping("/start-form/{formId}")
    public String startForm(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable Long formId) {
        return "redirect:/user?formId=" + formId;
    }

    @GetMapping("/toggle-favorite-workflow/{workflowId}")
    public String toggleFavoriteWorkflow( @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable Long workflowId) {
        userService.toggleFavorite(authUserEppn, workflowId, UiParams.favoriteWorkflows);
        return "redirect:/user";
    }

    @GetMapping("/toggle-favorite-form/{formId}")
    public String toggleFavoriteForm( @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable Long formId) {
        userService.toggleFavorite(authUserEppn, formId, UiParams.favoriteForms);
        return "redirect:/user";
    }

    @PostMapping("search")
    public SearchResult search(@RequestParam String search) {
        return new SearchResult();
    }

}
