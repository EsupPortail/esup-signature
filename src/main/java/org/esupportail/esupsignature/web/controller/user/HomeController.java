package org.esupportail.esupsignature.web.controller.user;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dto.ui.global.UiMessageDto;
import org.esupportail.esupsignature.dto.ui.global.UiSlimSelectDto;
import org.esupportail.esupsignature.dto.ui.global.UiSearchRequest;
import org.esupportail.esupsignature.dto.ui.global.UiSearchResult;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.UiParams;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.DataRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.ui.UiFetchService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RequestMapping("/user")
@Controller
public class HomeController {

    private final TagService tagService;
    private final UiFetchService uiFetchService;

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
    private final MessageService messageService;
    private final UserService userService;


    public HomeController(GlobalProperties globalProperties, SignRequestRepository signRequestRepository, FormService formService, WorkflowService workflowService, SignRequestService signRequestService, SignBookService signBookService, DataRepository dataRepository, MessageService messageService, UserService userService, TagService tagService, UiFetchService uiFetchService) {
        this.globalProperties = globalProperties;
        this.signRequestRepository = signRequestRepository;
        this.formService = formService;
        this.workflowService = workflowService;
        this.signRequestService = signRequestService;
        this.signBookService = signBookService;
        this.dataRepository = dataRepository;
        this.messageService = messageService;
        this.userService = userService;
        this.tagService = tagService;
        this.uiFetchService = uiFetchService;
    }

    @GetMapping(value = {"", "/"})
    public String home(@ModelAttribute("userEppn") String userEppn,
                       @ModelAttribute("authUserEppn") String authUserEppn,
                       @RequestParam(required = false, name = "formId") Long formId,
                       @RequestParam(required = false, name = "workflowId") Long workflowId,
                       Model model)
            throws EsupSignatureUserException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

        User authUser = userService.getByEppn(authUserEppn);
        if (authUser != null) {
            List<SignRequest> oldSignRequests = new ArrayList<>();
            if (globalProperties.getNbDaysBeforeWarning() > -1) {
                oldSignRequests = signRequestRepository.findByCreateByEppnAndOlderPending(authUser.getId(), globalProperties.getNbDaysBeforeWarning());
            }
            model.addAttribute("oldSignRequests", oldSignRequests);
//            List<SignRequest> recipientNotPresentSignRequests = signRequestService.getRecipientNotPresentSignRequests(userEppn);
//            model.addAttribute("recipientNotPresentSignRequests", recipientNotPresentSignRequests);
            List<Message> messages = new ArrayList<>();
            if (!authUserEppn.equals("system") && userEppn.equals(authUserEppn)) {
                messages.addAll(messageService.getByUserNeverRead(authUser));
            }
            model.addAttribute("messageNews", messages);
            List<Data> datas = dataRepository.findByCreateByAndStatus(authUser, SignRequestStatus.draft);
            model.addAttribute("datas", datas);
            List<Form> forms = formService.getFormsByUser(userEppn, authUserEppn);
            model.addAttribute("forms", forms);
            Set<Workflow> workflows = workflowService.getWorkflowsByUser(userEppn, authUserEppn);
            model.addAttribute("workflows", workflows);
            model.addAttribute("featuredForms", forms.stream().filter(Form::getIsFeatured).toList());
            model.addAttribute("featuredWorkflows", workflows.stream().filter(Workflow::getIsFeatured).toList());
            model.addAttribute("startFormId", formId != null && formService.isFormAuthorized(userEppn, authUserEppn, formId) ? formId : null);
            model.addAttribute("startWorkflowId", workflowId != null && workflowService.isWorkflowAuthorized(userEppn, authUserEppn, workflowId) ? workflowId : null);
            model.addAttribute("allTags", tagService.getAllTags(Pageable.unpaged()).getContent());
            model.addAttribute("nbFollowByMe", signRequestService.nbFollowedByMe(userEppn));
            model.addAttribute("selectedTags", new ArrayList<>());
            return "user/home/index";
        } else {
            throw new EsupSignatureUserException("not reconized user");
        }
    }

    @PreAuthorize("@preAuthorizeService.notInShare(#userEppn, #authUserEppn) && hasRole('ROLE_USER')")
    @GetMapping("/start-form/{formId}")
    public String startForm(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable Long formId, RedirectAttributes redirectAttributes) {
        if(!formService.isFormAuthorized(userEppn, authUserEppn, formId)) {
            redirectAttributes.addFlashAttribute("message", new UiMessageDto("error", "Formulaire non autorisé"));
            return "redirect:/user";
        }
        return "redirect:/user?formId=" + formId;
    }

    @PreAuthorize("@preAuthorizeService.notInShare(#userEppn, #authUserEppn) && hasRole('ROLE_USER')")
    @GetMapping("/start-workflow/{workflowId}")
    public String startWorkflow(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable Long workflowId, RedirectAttributes redirectAttributes) {
        if(!workflowService.isWorkflowAuthorized(userEppn, authUserEppn, workflowId)) {
            redirectAttributes.addFlashAttribute("message", new UiMessageDto("error", "Circuit non autorisé"));
            return "redirect:/user";
        }
        return "redirect:/user?workflowId=" + workflowId;
    }

    @GetMapping("/toggle-favorite-workflow/{workflowId}")
    @ResponseBody
    public Boolean toggleFavoriteWorkflow(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable Long workflowId) {
        return userService.toggleFavorite(authUserEppn, workflowId, UiParams.favoriteWorkflows);
   }

    @GetMapping("/toggle-favorite-form/{formId}")
    @ResponseBody
    public Boolean toggleFavoriteForm(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable Long formId) {
        return userService.toggleFavorite(authUserEppn, formId, UiParams.favoriteForms);
    }

    @PostMapping(value = "/search", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<UiSearchResult> search(@ModelAttribute("authUserEppn") String authUserEppn, @RequestBody List<UiSearchRequest> searchRequests) {
        return uiFetchService.buildHomeSearchResults(authUserEppn, searchRequests);
    }

    @GetMapping(value = "/search-titles")
    @ResponseBody
    public List<UiSlimSelectDto> searchDocTitles(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
                                                 @RequestParam(value = "searchString", required = false) String searchString) {
        List<UiSlimSelectDto> results = new ArrayList<>();
        if(!StringUtils.hasText(searchString)) return results;
        for(String docTitle : signBookService.getAllDocTitles(userEppn, searchString)) {
            results.add(new UiSlimSelectDto(docTitle, docTitle, "<i class=\"fi fi-rr-file \"></i> " + docTitle));

        }
        for(String workflowTile : workflowService.getWorkflowsByUser(userEppn, authUserEppn).stream().map(Workflow::getDescription).filter(s -> s !=null && s.toLowerCase().contains(searchString.toLowerCase())).toList()) {
            results.add(new UiSlimSelectDto(workflowTile, workflowTile, "<i class=\"fi fi-rr-diagram-project project-diagram-color\"></i> " + workflowTile));
        }
        for(String formTitle : formService.getFormsByUser(userEppn, authUserEppn).stream().map(Form::getTitle).filter(s -> s != null && s.toLowerCase().contains(searchString.toLowerCase())).toList()) {
            results.add(new UiSlimSelectDto(formTitle, formTitle, "<i class=\"fi fi-rr-poll-h file-alt-color\"></i> " + formTitle));
        }
        return results;
    }

}
