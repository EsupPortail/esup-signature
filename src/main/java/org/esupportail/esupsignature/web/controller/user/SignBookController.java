package org.esupportail.esupsignature.web.controller.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.esupportail.esupsignature.dto.json.RecipientWsDto;
import org.esupportail.esupsignature.dto.json.WorkflowStepDto;
import org.esupportail.esupsignature.dto.js.JsMessage;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
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

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/user/signbooks")
public class SignBookController {

    private static final Logger logger = LoggerFactory.getLogger(SignBookController.class);

    @Resource
    private RecipientService recipientService;

    @Resource
    private SignWithService signWithService;

    @ModelAttribute("activeMenu")
    public String getActiveMenu() {
        return "signbooks";
    }

    @Resource
    private PreAuthorizeService preAuthorizeService;

    @Resource
    private WorkflowService workflowService;

    @Resource
    private SignBookService signBookService;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private FormService formService;

    @Resource
    private TemplateEngine templateEngine;

    @GetMapping
    @PreAuthorize("@preAuthorizeService.notInShare(#userEppn, #authUserEppn)")
    public String list(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
                       @RequestParam(value = "statusFilter", required = false) String statusFilter,
                       @RequestParam(value = "recipientsFilter", required = false) String recipientsFilter,
                       @RequestParam(value = "workflowFilter", required = false) String workflowFilter,
                       @RequestParam(value = "docTitleFilter", required = false) String docTitleFilter,
                       @RequestParam(value = "creatorFilter", required = false) String creatorFilter,
                       @RequestParam(value = "dateFilter", required = false) String dateFilter,
                       @SortDefault(value = "createDate", direction = Sort.Direction.DESC) @PageableDefault(size = 15) Pageable pageable, Model model) {
        if(statusFilter == null || statusFilter.equals("all")) statusFilter = "";
        if(workflowFilter != null && (workflowFilter.isEmpty() || workflowFilter.equals("all"))) {
            workflowFilter = null;
        }
        if(creatorFilter != null && (creatorFilter.isEmpty() || creatorFilter.equals("all"))) {
            creatorFilter = null;
        }
        if(docTitleFilter != null && (docTitleFilter.isEmpty() || docTitleFilter.equals("all"))) {
            docTitleFilter = null;
        }
        if(recipientsFilter != null && (recipientsFilter.isEmpty() || recipientsFilter.equals("all"))) {
            recipientsFilter = null;
        }
        model.addAttribute("statusFilter", statusFilter);
        Page<SignBook> signBooks = signBookService.getSignBooks(userEppn, authUserEppn, statusFilter, recipientsFilter, workflowFilter, docTitleFilter, creatorFilter, dateFilter, pageable);
        model.addAttribute("signBooks", signBooks);
        model.addAttribute("nbEmpty", signBookService.countEmpty(userEppn));
        model.addAttribute("statuses", SignRequestStatus.values());
        model.addAttribute("forms", formService.getFormsByUser(userEppn, authUserEppn));
        model.addAttribute("workflows", workflowService.getWorkflowsByUser(userEppn, authUserEppn));
        model.addAttribute("signWiths", signWithService.getAuthorizedSignWiths(userEppn));
        model.addAttribute("sealCertOK", signWithService.checkSealCertificat(userEppn, true));
        model.addAttribute("workflowFilter", workflowFilter);
        model.addAttribute("docTitleFilter", docTitleFilter);
        model.addAttribute("dateFilter", dateFilter);
        model.addAttribute("recipientsFilter", recipientsFilter);
        LinkedHashSet<String> workflowNames = new LinkedHashSet<>();
        LinkedHashSet<String> docTitles = new LinkedHashSet<>();
        if(statusFilter.isEmpty() && (workflowFilter == null || workflowFilter.equals("Hors circuit")) && docTitleFilter == null && recipientsFilter == null) {
            docTitles.addAll(signBookService.getAllDocTitles(userEppn));
            workflowNames.addAll(signBookService.getWorkflowNames(userEppn));
        } else {
            docTitles.addAll(signBooks.stream().map(SignBook::getSubject).toList());
            workflowNames.addAll(signBooks.stream().map(SignBook::getWorkflowName).toList());
        }
        List<User> creators = signBookService.getCreators(userEppn, workflowFilter, docTitleFilter, creatorFilter);
        model.addAttribute("creators", creators);
        model.addAttribute("docTitles", docTitles);
        model.addAttribute("workflowNames", workflowNames);
        model.addAttribute("signRequestRecipients", signBookService.getRecipientsNames(userEppn).stream().filter(Objects::nonNull).collect(Collectors.toList()));
        model.addAttribute("nbFollowByMe", signRequestService.nbFollowedByMe(userEppn));
        model.addAttribute("nbDraft", signRequestService.getNbDraftSignRequests(userEppn));
        return "user/signbooks/list";
    }

    @GetMapping(value = "/list-ws")
    @ResponseBody
    @PreAuthorize("@preAuthorizeService.notInShare(#userEppn, #authUserEppn)")
    public String listWs(@ModelAttribute(name = "userEppn") String userEppn, @ModelAttribute(name = "authUserEppn") String authUserEppn,
                         @RequestParam(value = "statusFilter", required = false) String statusFilter,
                         @RequestParam(value = "recipientsFilter", required = false) String recipientsFilter,
                         @RequestParam(value = "workflowFilter", required = false) String workflowFilter,
                         @RequestParam(value = "docTitleFilter", required = false) String docTitleFilter,
                         @RequestParam(value = "creatorFilter", required = false) String creatorFilter,
                         @RequestParam(value = "dateFilter", required = false) String dateFilter,
                         @SortDefault(value = "createDate", direction = Sort.Direction.DESC) @PageableDefault(size = 10) Pageable pageable, HttpServletRequest httpServletRequest, Model model) {
        if(statusFilter == null || statusFilter.equals("all")) statusFilter = "";
        if(workflowFilter != null && (workflowFilter.isEmpty() || workflowFilter.equals("all"))) {
            workflowFilter = null;
        }
        if(creatorFilter != null && (creatorFilter.isEmpty() || creatorFilter.equals("all"))) {
            creatorFilter = null;
        }
        if(docTitleFilter != null && (docTitleFilter.isEmpty() || docTitleFilter.equals("all"))) {
            docTitleFilter = null;
        }
        if(recipientsFilter != null && (recipientsFilter.isEmpty() || recipientsFilter.equals("all"))) {
            recipientsFilter = null;
        }
        Page<SignBook> signBooks = signBookService.getSignBooks(userEppn, authUserEppn, statusFilter, recipientsFilter, workflowFilter, docTitleFilter, creatorFilter, dateFilter, pageable);
        model.addAttribute("signBooks", signBooks);
        CsrfToken token = new HttpSessionCsrfTokenRepository().loadToken(httpServletRequest);
        final Context ctx = new Context(Locale.FRENCH);
        ctx.setVariables(model.asMap());
        ctx.setVariable("_csrf", token);
        return templateEngine.process("user/signbooks/includes/list-elem.html", ctx);
    }

    @PreAuthorize("@preAuthorizeService.signBookView(#id, #userEppn, #authUserEppn)")
    @GetMapping(value = "/{id}")
    public String show(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletRequest httpServletRequest, RedirectAttributes redirectAttributes, Model model) {
        SignBook signBook = signBookService.getById(id);
        if(!signBook.getSignRequests().isEmpty()) {
            Long signRequestId = signBook.getSignRequests().get(0).getId();
            if (signBook.getSignRequests().size() > 1) {
                if (signBook.getSignRequests().stream().anyMatch(s -> s.getStatus().equals(SignRequestStatus.pending))) {
                    signRequestId = signBook.getSignRequests().stream().filter(s -> s.getStatus().equals(SignRequestStatus.pending)).findFirst().get().getId();
                }
            }
            if(model.getAttribute("message") != null) {
                redirectAttributes.addFlashAttribute("message", model.getAttribute("message"));
            }
            return "redirect:/user/signrequests/" + signRequestId;
        } else {
            redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Cette demande de signature n'est pas conforme car elle est vide, elle peut être supprimée"));
            return "redirect:" + httpServletRequest.getHeader(HttpHeaders.REFERER);
        }
    }

    @PreAuthorize("@preAuthorizeService.signBookOwner(#id, #authUserEppn)")
    @GetMapping(value = "/restore/{id}", produces = "text/html")
    public String restore(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        signBookService.restore(id, authUserEppn);
        redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Restauration effectuée"));
        return "redirect:/user/signbooks/" + id;
    }

    @PreAuthorize("@preAuthorizeService.signBookManage(#id, #authUserEppn)")
    @DeleteMapping(value = "/{id}", produces = "text/html")
    public String delete(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletRequest httpServletRequest, RedirectAttributes redirectAttributes) {
        Boolean isDefinitive = signBookService.delete(id, authUserEppn);
        if(isDefinitive != null) {
            if (isDefinitive) {
                redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Le document a été supprimé définitivement"));
                if (httpServletRequest.getHeader(HttpHeaders.REFERER).contains("signrequests")) {
                    return "redirect:/user/signbooks";
                } else {
                    return "redirect:" + httpServletRequest.getHeader(HttpHeaders.REFERER);
                }
            } else {
                redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Le document a été placé dans la corbeille"));
                return "redirect:" + httpServletRequest.getHeader(HttpHeaders.REFERER);
            }
        } else {
            redirectAttributes.addFlashAttribute("message", new JsMessage("warn", "Ce document ne pas être supprimé de la corbeille"));
            return "redirect:" + httpServletRequest.getHeader(HttpHeaders.REFERER);
        }
    }

    @PreAuthorize("@preAuthorizeService.signBookManage(#id, #authUserEppn)")
    @DeleteMapping(value = "/force-delete/{id}", produces = "text/html")
    public String forceDelete(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletRequest httpServletRequest, RedirectAttributes redirectAttributes) {
        if(signBookService.deleteDefinitive(id, authUserEppn)) {
            redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Le document a été supprimé définitivement"));
            return "redirect:/user/signbooks";

        } else {
            redirectAttributes.addFlashAttribute("message", new JsMessage("warn", "Le document ne peut pas être supprimé définitivement"));
            return "redirect:" + httpServletRequest.getHeader(HttpHeaders.REFERER);

        }
    }

    @PreAuthorize("@preAuthorizeService.signBookManage(#id, #authUserEppn)")
    @GetMapping(value = "/update/{id}")
    public String updateForm(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        SignBook signBook = signBookService.getById(id);
        if(signBook != null && signBook.getCreateBy().getEppn().equals(authUserEppn) && (signBook.getStatus().equals(SignRequestStatus.draft) || signBook.getStatus().equals(SignRequestStatus.pending))) {
            model.addAttribute("signBook", signBook);
            model.addAttribute("logs", signBookService.getLogsFromSignBook(signBook));
            model.addAttribute("allSteps", signBookService.getAllSteps(signBook));
            model.addAttribute("workflows", workflowService.getWorkflowsByUser(authUserEppn, authUserEppn));
            return "user/signbooks/update";
        } else {
            redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Demande non trouvée"));
            return "redirect:/user/signbooks";
        }
    }

    @PreAuthorize("@preAuthorizeService.signBookManage(#id, #authUserEppn)")
    @PutMapping(value = "/update/{id}")
    public String update(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                         @RequestParam String subject,
                         @RequestParam String description,
                         @RequestParam(required = false) List<String> viewers,
                         RedirectAttributes redirectAttributes) {
        signBookService.updateSignBook(id, subject, description, viewers);
        redirectAttributes.addFlashAttribute("message", new JsMessage("success", "Modifications enregistrées"));
        return "redirect:/user/signbooks/update/" + id;
    }

    @PreAuthorize("@preAuthorizeService.signBookManage(#id, #authUserEppn)")
    @PostMapping(value = "/add-viewers/{id}")
    public String addViewers(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                         @RequestParam(required = false) List<String> viewers,
                         RedirectAttributes redirectAttributes) {
        signBookService.addViewers(id, viewers);
        redirectAttributes.addFlashAttribute("message", new JsMessage("success", "Observateurs ajoutés"));
        return "redirect:/user/signbooks/" + id;
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
            WorkflowStepDto workflowStepDto = recipientService.convertRecipientEmailsToStep(recipientsEmails).get(0);
            workflowStepDto.setAutoSign(autoSign);
            workflowStepDto.setAllSignToComplete(allSignToComplete);
            workflowStepDto.setSignType(signType);
            signBookService.addLiveStep(id, workflowStepDto , stepNumber, authUserEppn);
            redirectAttributes.addFlashAttribute("message", new JsMessage("success", "Étape ajoutée"));
        } catch (EsupSignatureRuntimeException e) {
            redirectAttributes.addFlashAttribute("message", new JsMessage("error", e.getMessage()));
        }

        return "redirect:/user/signbooks/update/" + id;
    }

    @PreAuthorize("@preAuthorizeService.signRequestSign(#id, #userEppn, #authUserEppn)")
    @PostMapping(value = "/add-repeatable-step/{id}")
    @ResponseBody
    public ResponseEntity<String> addRepeatableStep(@ModelAttribute("authUserEppn") String authUserEppn, @ModelAttribute("userEppn") String userEppn,
                                                    @PathVariable("id") Long id,
                                                    @RequestBody WorkflowStepDto step) {
        try {
            signBookService.addLiveStep(signRequestService.getById(id).getParentSignBook().getId(), step, step.getStepNumber(), authUserEppn);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (EsupSignatureRuntimeException e) {
            logger.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("@preAuthorizeService.signBookManage(#id, #authUserEppn)")
    @DeleteMapping(value = "/remove-live-step/{id}/{step}")
    public String removeStep(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @PathVariable("step") Integer step, RedirectAttributes redirectAttributes) {
        if (signBookService.removeStep(id, step)) {
            redirectAttributes.addFlashAttribute("message", new JsMessage("info", "L'étape a été supprimés"));
        } else {
            redirectAttributes.addFlashAttribute("message", new JsMessage("warn", "L'étape ne peut pas être supprimée"));
        }
        return "redirect:/user/signbooks/update/" + id;
    }

    @PreAuthorize("@preAuthorizeService.signBookManage(#id, #authUserEppn)")
    @PostMapping(value = "/add-workflow/{id}")
    public String addWorkflow(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                          @RequestParam(value = "workflowSignBookId") Long workflowSignBookId) throws EsupSignatureRuntimeException {
        SignBook signBook = signBookService.getById(id);
        signBookService.addWorkflowToSignBook(signBook, authUserEppn, workflowSignBookId);
        return "redirect:/user/signrequests/" + signBook.getSignRequests().get(0).getId() + "?form";
    }

    @PreAuthorize("@preAuthorizeService.signBookManage(#id, #authUserEppn)")
    @PostMapping(value = "/add-docs/{id}")
    public String addDocumentToNewSignRequest(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                                              @RequestParam("multipartFiles") MultipartFile[] multipartFiles, RedirectAttributes redirectAttributes) {
        try {
            signBookService.addDocumentsToSignBook(id, multipartFiles, authUserEppn);
            redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Ajout effectuée"));
        } catch(Exception e) {
            redirectAttributes.addFlashAttribute("message", new JsMessage("error", e.getMessage()));
        }
        return "redirect:/user/signbooks/update/" + id;
    }

    @PreAuthorize("@preAuthorizeService.signBookOwner(#id, #authUserEppn)")
    @PostMapping(value = "/add-step/{id}")
    public String addRecipients(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                                @RequestParam(value = "recipientsEmails", required = false) List<String> recipientsEmails,
                                @RequestParam(name = "signType") SignType signType,
                                @RequestParam(name = "allSignToComplete", required = false) Boolean allSignToComplete) throws EsupSignatureRuntimeException {
        WorkflowStepDto step = recipientService.convertRecipientEmailsToStep(recipientsEmails).get(0);
        step.setSignType(signType);
        step.setAllSignToComplete(allSignToComplete);
        signBookService.addLiveStep(id, step, null, authUserEppn);
        return "redirect:/user/signrequests/" + id + "?form";
    }

    @PreAuthorize("@preAuthorizeService.signBookManage(#id, #authUserEppn)")
    @PostMapping(value = "/pending/{id}")
    public String pending(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id) throws EsupSignatureRuntimeException {
        signBookService.pendingSignBook(authUserEppn, id);
        return "redirect:/user/signbooks/" + id;
    }

    @PreAuthorize("@preAuthorizeService.signBookView(#id, #authUserEppn, #authUserEppn)")
    @GetMapping(value = "/toggle/{id}", produces = "text/html")
    public String toggle(@ModelAttribute("authUserEppn") String authUserEppn,
                         @PathVariable("id") Long id, HttpServletRequest httpServletRequest, RedirectAttributes redirectAttributes) {
        if(signBookService.toggle(id, authUserEppn)) {
            redirectAttributes.addFlashAttribute("message", new JsMessage("info", "La demande à été masquée"));
        } else {
            redirectAttributes.addFlashAttribute("message", new JsMessage("info", "La demande est de nouveau visible"));
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
            for(Long id : ids) {
                if(!preAuthorizeService.signBookView(id, authUserEppn, authUserEppn)) throw new EsupSignatureException("access denied");
            }
            signBookService.getMultipleSignedDocuments(ids, httpServletResponse);
        } catch (Exception e) {
            logger.error("error while downloading multiple documents", e);
        }
        httpServletResponse.flushBuffer();
    }

    @ResponseBody
    @PostMapping(value = "/mass-sign")
    public ResponseEntity<String> massSign(@ModelAttribute("userEppn") String userEppn,
                                           @ModelAttribute("authUserEppn") String authUserEppn,
                                           @RequestParam String ids,
                                           @RequestParam(value = "password", required = false) String password,
                                           @RequestParam(value = "signWith", required = false) String signWith,
                                           HttpSession httpSession) throws EsupSignatureRuntimeException, IOException {
        String error = signBookService.initMassSign(userEppn, authUserEppn, ids, httpSession, password, signWith);
        if(error == null) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(value = "/download-multiple-with-report", produces = "application/zip")
    @ResponseBody
    public void downloadMultipleWithReport(@ModelAttribute("authUserEppn") String authUserEppn, @RequestParam List<Long> ids, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        httpServletResponse.setContentType("application/zip");
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        httpServletResponse.setHeader("Content-Disposition", "attachment; filename=\"download.zip\"");
        try {
            signBookService.getMultipleSignedDocumentsWithReport(ids, httpServletRequest, httpServletResponse);
        } catch (Exception e) {
            logger.error("error while downloading multiple documents", e);
        }
        httpServletResponse.flushBuffer();
    }

    @PostMapping(value = "/delete-multiple", consumes = {"application/json"})
    @ResponseBody
    public ResponseEntity<Boolean> deleteMultiple(@ModelAttribute("authUserEppn") String authUserEppn, @RequestBody List<Long> ids, RedirectAttributes redirectAttributes) {
        for(Long id : ids) {
            if(preAuthorizeService.signBookManage(id, authUserEppn)) {
                signBookService.delete(id, authUserEppn);
            }
        }
        redirectAttributes.addFlashAttribute("message", new JsMessage("info", "Suppression effectuée"));
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    @PostMapping(value = "/update-recipients/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Modifier les destinataires d'une étape de demande de signature")
    @PreAuthorize("@preAuthorizeService.signBookManage(#id, #authUserEppn)")
    public String updateRecipients(@PathVariable Long id,
                                   @ModelAttribute("authUserEppn") String authUserEppn,
                                   @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey,
                                   @RequestParam("recipientsEmails") List<String> recipientsEmails,
                                   @RequestParam Integer stepNumber, RedirectAttributes redirectAttributes) throws JsonProcessingException {
        List<RecipientWsDto> recipientWsDtos = new ArrayList<>();
        for(String recipientsEmail: recipientsEmails) {
            recipientWsDtos.add(new RecipientWsDto(recipientsEmail));
        }
        try {
            signRequestService.replaceRecipientsToWorkflowStep(id, stepNumber, recipientWsDtos);
        } catch (EsupSignatureException e) {
            redirectAttributes.addFlashAttribute("message", new JsMessage("error", e.getMessage()));
        }
        return "redirect:/user/signbooks/update/" + id;
    }
}
