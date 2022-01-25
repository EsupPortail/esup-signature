package org.esupportail.esupsignature.web.controller.user;

import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.utils.sign.SignService;
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
import org.esupportail.esupsignature.web.ws.json.JsonWorkflowStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@Controller
@RequestMapping("/user/signbooks")
public class SignBookController {

    private static final Logger logger = LoggerFactory.getLogger(SignBookController.class);

    @Resource
    private WorkflowService workflowService;

    @Resource
    private SignBookService signBookService;

    @Resource
    private LogService logService;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private SignService signService;

    @Resource
    private UserService userService;

    @Resource
    private FormService formService;

    @Resource
    private TemplateEngine templateEngine;

    @GetMapping
    public String list(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
                       @RequestParam(value = "statusFilter", required = false) String statusFilter,
                       @RequestParam(value = "recipientsFilter", required = false) String recipientsFilter,
                       @RequestParam(value = "workflowFilter", required = false) String workflowFilter,
                       @RequestParam(value = "docTitleFilter", required = false) String docTitleFilter,
                       @SortDefault(value = "createDate", direction = Sort.Direction.DESC) @PageableDefault(size = 10) Pageable pageable, Model model) {
        if(statusFilter == null) statusFilter = "all";
        if(statusFilter.equals("all")) statusFilter = "";
        if(workflowFilter == null || workflowFilter.isEmpty() || workflowFilter.equals("all")) {
            workflowFilter = "%";
        }
        if(docTitleFilter == null || docTitleFilter.isEmpty() || docTitleFilter.equals("all")) {
            docTitleFilter = "%";
        }
        if(recipientsFilter == null || recipientsFilter.isEmpty() || recipientsFilter.equals("all")) {
            recipientsFilter = "%";
        }
        Page<SignBook> signBooks = signBookService.getSignBooks(userEppn, statusFilter, recipientsFilter, workflowFilter, docTitleFilter, pageable);
        model.addAttribute("statusFilter", statusFilter);
        model.addAttribute("signBooks", signBooks);
        model.addAttribute("nbEmpty", signBookService.countEmpty(userEppn));
        model.addAttribute("statuses", SignRequestStatus.values());
        model.addAttribute("forms", formService.getFormsByUser(userEppn, authUserEppn));
        model.addAttribute("workflows", workflowService.getWorkflowsByUser(userEppn, authUserEppn));
        model.addAttribute("workflowFilter", workflowFilter);
        model.addAttribute("docTitleFilter", docTitleFilter);
        model.addAttribute("recipientsFilter", recipientsFilter);
        Set<String> docTitles = new HashSet<>(signBookService.getDocTitles(userEppn));
        model.addAttribute("docTitles", docTitles);
        LinkedHashSet<String> workflowNames = new LinkedHashSet<>();
        if(workflowFilter.equals("%") || workflowFilter.equals("Hors circuit")) {
            workflowNames.add("Hors circuit");
        }
        workflowNames.addAll(signBookService.getWorkflowNames(userEppn));
        model.addAttribute("workflowNames", workflowNames);
        model.addAttribute("signRequestRecipients", signBookService.getRecipientsNames(userEppn));
        return "user/signbooks/list";
    }

    @GetMapping(value = "/list-ws")
    @ResponseBody
    public String listWs(@ModelAttribute(name = "userEppn") String userEppn, @ModelAttribute(name = "authUserEppn") String authUserEppn,
                         @RequestParam(value = "statusFilter", required = false) String statusFilter,
                         @RequestParam(value = "recipientsFilter", required = false) String recipientsFilter,
                         @RequestParam(value = "workflowFilter", required = false) String workflowFilter,
                         @RequestParam(value = "docTitleFilter", required = false) String docTitleFilter,
                         @SortDefault(value = "createDate", direction = Sort.Direction.DESC) @PageableDefault(size = 10) Pageable pageable, HttpServletRequest httpServletRequest, Model model) {
        if(statusFilter == null) statusFilter = "all";
        if(statusFilter.equals("all")) statusFilter = "";
        if(workflowFilter == null || workflowFilter.isEmpty() || workflowFilter.equals("all")) {
            workflowFilter = "%";
        }
        if(docTitleFilter == null || docTitleFilter.isEmpty() || docTitleFilter.equals("all")) {
            docTitleFilter = "%";
        }
        Page<SignBook> signBooks = signBookService.getSignBooks(userEppn, statusFilter, recipientsFilter, workflowFilter, docTitleFilter, pageable);
        model.addAttribute("signBooks", signBooks);
        CsrfToken token = new HttpSessionCsrfTokenRepository().loadToken(httpServletRequest);
        final Context ctx = new Context(Locale.FRENCH);
        ctx.setVariables(model.asMap());
        ctx.setVariable("token", token);
        return templateEngine.process("user/signbooks/includes/list-elem.html", ctx);
    }

    @PreAuthorize("@preAuthorizeService.signBookView(#id, #userEppn, #authUserEppn)")
    @GetMapping(value = "/{id}")
    public String show(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id) {
        SignBook signBook = signBookService.getById(id);
        return "redirect:/user/signrequests/" + signBook.getSignRequests().get(0).getId();
    }

    @PreAuthorize("@preAuthorizeService.signBookManage(#id, #authUserEppn)")
    @DeleteMapping(value = "/{id}", produces = "text/html")
    public String delete(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletRequest httpServletRequest, RedirectAttributes redirectAttributes) {
        boolean isDefinitive = signBookService.delete(id, authUserEppn);
        if(isDefinitive) {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Le document a été supprimé définitivement"));
        } else {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Le document a été placé dans la corbeille"));
        }
        return "redirect:" + httpServletRequest.getHeader(HttpHeaders.REFERER);
    }

    @PreAuthorize("@preAuthorizeService.signBookManage(#id, #authUserEppn)")
    @DeleteMapping(value = "/force-delete/{id}", produces = "text/html")
    public String forceDelete(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletRequest httpServletRequest, RedirectAttributes redirectAttributes) {
        signBookService.deleteDefinitive(id);
        redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Le document a été supprimé définitivement"));
        return "redirect:" + httpServletRequest.getHeader(HttpHeaders.REFERER);
    }

    @PreAuthorize("@preAuthorizeService.signBookManage(#id, #authUserEppn)")
    @DeleteMapping(value = "silent-delete/{id}", produces = "text/html")
    @ResponseBody
    public void silentDelete(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id) {
        signBookService.deleteDefinitive(id);
    }

    @PreAuthorize("@preAuthorizeService.signBookManage(#id, #authUserEppn)")
    @GetMapping(value = "/{id}", params = "form")
    public String updateForm(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, Model model) {
        SignBook signBook = signBookService.getById(id);
        if((signBook.getLiveWorkflow().getWorkflow() == null || signBook.getCreateBy().equals(signBook.getLiveWorkflow().getWorkflow().getCreateBy())) && (signBook.getStatus().equals(SignRequestStatus.draft) || signBook.getStatus().equals(SignRequestStatus.pending))) {
            model.addAttribute("signBook", signBook);
            SignRequest signRequest = signBook.getSignRequests().get(0);
            model.addAttribute("signRequest", signRequest);
            List<Document> toSignDocuments = signService.getToSignDocuments(signRequest.getId());
            if(toSignDocuments.size() == 1) {
                model.addAttribute("toSignDocument", toSignDocuments.get(0));
            }
            model.addAttribute("signable", signRequest.getSignable());
            model.addAttribute("comments", logService.getLogs(signRequest.getId()));
            model.addAttribute("logs", signBook.getLogs());
            model.addAttribute("allSteps", signBookService.getAllSteps(signBook));
            model.addAttribute("workflows", workflowService.getWorkflowsByUser(authUserEppn, authUserEppn));
            return "user/signrequests/update";
        } else {
            return "redirect:/user/signrequests/" + signBook.getSignRequests().get(0).getId();
        }
    }

    @PreAuthorize("@preAuthorizeService.signBookManage(#id, #authUserEppn)")
    @PostMapping(value = "/add-live-step/{id}")
    public String addStep(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                          @RequestParam("recipientsEmails") List<String> recipientsEmails,
                          @RequestParam("stepNumber") int stepNumber,
                          @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete,
                          @RequestParam(name="autoSign", required = false) Boolean autoSign,
                          @RequestParam("signType") SignType signType,
                          RedirectAttributes redirectAttributes) {
        try {
            signBookService.addLiveStep(id, recipientsEmails, stepNumber, allSignToComplete, signType, false, null, true, autoSign, authUserEppn);
            redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Étape ajoutée"));
        } catch (EsupSignatureException e) {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("error", e.getMessage()));
        }

        return "redirect:/user/signbooks/" + id + "/?form";
    }

    @PreAuthorize("@preAuthorizeService.signRequestSign(#id, #userEppn, #authUserEppn)")
    @PostMapping(value = "/add-repeatable-step/{id}")
    @ResponseBody
    public ResponseEntity<String> addRepeatableStep(@ModelAttribute("authUserEppn") String authUserEppn, @ModelAttribute("userEppn") String userEppn,
                                                    @PathVariable("id") Long id,
                                                    @RequestBody JsonWorkflowStep step) {
        try {
            signBookService.addLiveStep(signRequestService.getById(id).getParentSignBook().getId(), step.getRecipientsEmails(), step.getStepNumber(), step.getAllSignToComplete(), SignType.valueOf(step.getSignType()), true, SignType.valueOf(step.getSignType()), true, step.getAutoSign(), authUserEppn);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (EsupSignatureException e) {
            logger.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("@preAuthorizeService.signBookManage(#id, #authUserEppn)")
    @DeleteMapping(value = "/remove-live-step/{id}/{step}")
    public String removeStep(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @PathVariable("step") Integer step, RedirectAttributes redirectAttributes) {
        if (signBookService.removeStep(id, step)) {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "L'étape a été supprimés"));
        } else {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("warn", "L'étape ne peut pas être supprimée"));
        }
        return "redirect:/user/signbooks/" + id + "/?form";
    }

    @PreAuthorize("@preAuthorizeService.signBookManage(#id, #authUserEppn)")
    @PostMapping(value = "/add-workflow/{id}")
    public String addWorkflow(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                          @RequestParam(value = "workflowSignBookId") Long workflowSignBookId) throws EsupSignatureException {
        SignBook signBook = signBookService.getById(id);
        signRequestService.addWorkflowToSignBook(signBook, authUserEppn, workflowSignBookId);
        return "redirect:/user/signrequests/" + signBook.getSignRequests().get(0).getId() + "/?form";
    }

    @PreAuthorize("@preAuthorizeService.signBookManage(#id, #authUserEppn)")
    @PostMapping(value = "/add-docs/{id}")
    public String addDocumentToNewSignRequest(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                                              @RequestParam("multipartFiles") MultipartFile[] multipartFiles) throws EsupSignatureIOException {
        logger.info("start add documents");
        signRequestService.addDocumentsToSignBook(id, multipartFiles, authUserEppn);
        return "redirect:/user/signrequests/" + id + "/?form";
    }

    @PreAuthorize("@preAuthorizeService.signBookManage(#id, #authUserEppn)")
    @GetMapping(value = "/pending/{id}")
    public String pending(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id) throws EsupSignatureException {
        signRequestService.pendingSignBook(id, null, authUserEppn, authUserEppn, false);
        return "redirect:/user/signrequests/" + id;
    }

    @ResponseBody
    @PostMapping(value = "/add-docs-in-sign-book-group/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object addDocumentInSignBookGroup(@ModelAttribute("authUserEppn") String authUserEppn,
                                             @PathVariable("name") String name,
                                             @RequestParam("multipartFiles") MultipartFile[] multipartFiles) throws EsupSignatureIOException {
        logger.info("start add documents in " + name);
        SignBook signBook = signRequestService.addDocsInNewSignBookGrouped(name, multipartFiles, authUserEppn);
        String[] ok = {"" + signBook.getId()};
        return ok;
    }

    @ResponseBody
    @PostMapping(value = "/add-docs-in-sign-book-unique/{workflowName}/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object addDocumentToNewSignRequestUnique(@ModelAttribute("authUserEppn") String authUserEppn,
                                                    @PathVariable("name") String name,
                                                    @PathVariable("workflowName") String workflowName,
                                                    @RequestParam("multipartFiles") MultipartFile[] multipartFiles, Model model) throws EsupSignatureIOException {
        User authUser = userService.getUserByEppn(authUserEppn);
        logger.info("start add documents in " + name);
        SignBook signBook = signRequestService.addDocsInNewSignBookSeparated(name, name, workflowName, multipartFiles, authUser);
        return new String[]{"" + signBook.getId()};
    }

    @PreAuthorize("@preAuthorizeService.signBookView(#id, #authUserEppn, #authUserEppn)")
    @GetMapping(value = "/toggle/{id}", produces = "text/html")
    public String toggle(@ModelAttribute("authUserEppn") String authUserEppn,
                         @PathVariable("id") Long id, HttpServletRequest httpServletRequest, RedirectAttributes redirectAttributes) {
        if(signBookService.toggle(id, authUserEppn)) {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "La demande à été masquée"));
        } else {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "La demande est de nouveau visible"));
        }
        return "redirect:" + httpServletRequest.getHeader(HttpHeaders.REFERER);
    }

    @GetMapping(value = "/download-multiple", produces = "application/zip")
    @ResponseBody
    public void downloadMultiple(@ModelAttribute("authUserEppn") String authUserEppn, @RequestParam List<Long> ids, HttpServletResponse httpServletResponse) throws IOException {
        httpServletResponse.setContentType("application/zip");
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        httpServletResponse.setHeader("Content-Disposition", "attachment; filename=\"download.zip\"");
        try {
            signRequestService.getMultipleSignedDocuments(ids, httpServletResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
        httpServletResponse.flushBuffer();
    }

    @GetMapping(value = "/download-multiple-with-report", produces = "application/zip")
    @ResponseBody
    public void downloadMultipleWithReport(@ModelAttribute("authUserEppn") String authUserEppn, @RequestParam List<Long> ids, HttpServletResponse httpServletResponse) throws IOException {
        httpServletResponse.setContentType("application/zip");
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        httpServletResponse.setHeader("Content-Disposition", "attachment; filename=\"download.zip\"");
        try {
            signRequestService.getMultipleSignedDocumentsWithReport(ids, httpServletResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
        httpServletResponse.flushBuffer();
    }
}
