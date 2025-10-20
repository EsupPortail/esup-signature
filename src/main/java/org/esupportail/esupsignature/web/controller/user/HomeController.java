package org.esupportail.esupsignature.web.controller.user;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dto.js.JsSlimSelect;
import org.esupportail.esupsignature.dto.json.SearchRequest;
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
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

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
    public String toggleFavoriteWorkflow(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable Long workflowId) {
        userService.toggleFavorite(authUserEppn, workflowId, UiParams.favoriteWorkflows);
        return "redirect:/user";
    }

    @GetMapping("/toggle-favorite-form/{formId}")
    public String toggleFavoriteForm(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable Long formId) {
        userService.toggleFavorite(authUserEppn, formId, UiParams.favoriteForms);
        return "redirect:/user";
    }

    @PostMapping(value = "search", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<SearchResult> search(@ModelAttribute("authUserEppn") String authUserEppn, @RequestBody List<SearchRequest> searchRequests) {
        List<SearchResult> searchResults = new ArrayList<>();
        if(!searchRequests.isEmpty()) {
            List<String> words = new ArrayList<>();
            List<String> types = new ArrayList<>();
            List<Tag> tags = new ArrayList<>();
            for (SearchRequest searchRequest : searchRequests) {
                if (searchRequest.getValue().startsWith("tag:")) {
                    tags.add(tagService.getById(Long.valueOf(searchRequest.getValue().split(":")[1])));
                } else if (searchRequest.getValue().startsWith("type:")) {
                    types.add(searchRequest.getValue().split(":")[1]);
                } else if (!searchRequest.getValue().contains(":")) {
                    words.add(searchRequest.getValue());
                }
            }
            if (types.isEmpty() || types.contains("workflow")) {
                List<Workflow> workflows = workflowService.getWorkflowsByUser(authUserEppn, authUserEppn)
                        .stream().filter(w -> (tags.isEmpty() || new HashSet<>(w.getTags()).containsAll(tags)) && (words.isEmpty() || words.stream().anyMatch(word -> w.getDescription().toLowerCase().contains(word.toLowerCase())))).toList();
                for (Workflow workflow : workflows) {
                    SearchResult searchResult = new SearchResult();
                    searchResult.setIcon("fa-solid fa-project-diagram project-diagram-color");
                    searchResult.setTitle(workflow.getDescription());
                    searchResult.setUrl(workflow.getId() + " ");
                    for(Tag tag : workflow.getTags()) {
                        searchResult.setTags(searchResult.getTags() +
                                "<span style=\"background-color: " + tag.getColor() + "\" class=\"badge\">" + tag.getName() + "</span> ");
                    }
                    searchResults.add(searchResult);
                }
            }
            if (types.isEmpty() || types.contains("form")) {
                List<Form> forms = formService.getFormsByUser(authUserEppn, authUserEppn)
                        .stream().filter(f -> (tags.isEmpty() || new HashSet<>(f.getTags()).containsAll(tags)) && (words.isEmpty() || words.stream().anyMatch(word -> f.getDescription().toLowerCase().contains(word.toLowerCase())))).toList();
                for (Form form : forms) {
                    SearchResult searchResult = new SearchResult();
                    searchResult.setIcon("fa-solid fa-file-alt file-alt-color");
                    searchResult.setTitle(form.getTitle());
                    searchResult.setUrl(form.getId() + " ");
                    for(Tag tag : form.getTags()) {
                        searchResult.setTags(searchResult.getTags() +
                                "<span style=\"background-color: " + tag.getColor() + "\" class=\"badge\">" + tag.getName() + "</span> ");
                    }
                    searchResults.add(searchResult);
                }
            }
            if((types.isEmpty() || types.contains("signBook")) && tags.isEmpty()) {
                Set<SignBook> signBooks = new HashSet<>();
                if(words.isEmpty()) {
                    signBooks.addAll(signBookService.getSignBooks(authUserEppn, authUserEppn, "all", null, null, null, null, null, Pageable.ofSize(20)).getContent());
                } else {
                    for (String word : words) {
                        signBooks.addAll(signBookService.getSignBooks(authUserEppn, authUserEppn, "all", null, null, word, null, null, Pageable.unpaged()).getContent());
                    }
                }
                for (SignBook signBook : signBooks) {
                    SearchResult searchResult = new SearchResult();
                    searchResult.setIcon("fa-solid fa-file");
                    searchResult.setTitle(signBook.getSubject());
                    searchResult.setUrl(signBook.getId() + " ");
                    searchResults.add(searchResult);
                }
            }
        }
        return searchResults;
    }

    @GetMapping(value = "/search-titles")
    @PreAuthorize("@preAuthorizeService.notInShare(#userEppn, #authUserEppn)")
    @ResponseBody
    public List<JsSlimSelect> searchDocTitles(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
                                              @RequestParam(value = "searchString", required = false) String searchString) {
        List<JsSlimSelect> results = new ArrayList<>();
        for(String docTitle : signBookService.getAllDocTitles(userEppn, searchString)) {
            results.add(new JsSlimSelect(docTitle, docTitle, "<i class=\"fa-regular fa-file \"></i> " + docTitle));

        }
        for(String workflowTile : workflowService.getWorkflowsByUser(userEppn, authUserEppn).stream().map(Workflow::getDescription).filter(s -> s.toLowerCase().contains(searchString.toLowerCase())).toList()) {
            results.add(new JsSlimSelect(workflowTile, workflowTile, "<i class=\"fa-solid fa-project-diagram project-diagram-color\"></i> " + workflowTile));
        }
        for(String formTitle : formService.getFormsByUser(userEppn, authUserEppn).stream().map(Form::getTitle).filter(s -> s.toLowerCase().contains(searchString.toLowerCase())).toList()) {
            results.add(new JsSlimSelect(formTitle, formTitle, "<i class=\"fa-solid fa-file-alt file-alt-color\"></i> " + formTitle));
        }
        return results;
    }

}
