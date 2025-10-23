package org.esupportail.esupsignature.web.controller.user;

import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.esupportail.esupsignature.dto.js.JsMessage;
import org.esupportail.esupsignature.dto.json.RecipientWsDto;
import org.esupportail.esupsignature.dto.json.WorkflowStepDto;
import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.FormService;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.servlet.IServletWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.util.List;
import java.util.Locale;

@RequestMapping("/user/wizard")
@Controller
public class WizardController {

    private static final Logger logger = LoggerFactory.getLogger(WizardController.class);

    @Resource
    private WorkflowService workflowService;

    @Resource
    private FormService formService;

    @Resource
    private SignBookService signBookService;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private TemplateEngine templateEngine;

    @PreAuthorize("@preAuthorizeService.notInShare(#userEppn, #authUserEppn) && hasRole('ROLE_USER')")
    @GetMapping(value = "/wiz-start-sign/{type}", produces = "text/html")
    public String wizStartSign(@PathVariable String type, @ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
                               @RequestParam(value = "workflowId", required = false) Long workflowId, Model model) {
        String modalTile = "Création d'une nouvelle demande personnalisée";
        if(workflowId != null) {
            Workflow workflow = workflowService.getById(workflowId);
            workflow.setMessageToDisplay(workflowService.getHelpMessage(userEppn, workflow));
            modalTile = "Création d'une nouvelle demande dans le circuit : " + workflow.getDescription();
            model.addAttribute("workflow", workflow);
        }
        model.addAttribute("modalTitle", modalTile);
        return "user/wizard/wiz-" + type + "-sign";
    }

    @PreAuthorize("@preAuthorizeService.notInShare(#userEppn, #authUserEppn) && hasRole('ROLE_USER')")
    @GetMapping(value = "/wiz-start-workflow", produces = "text/html")
    public String wizStartWorkflow(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn) {
        return "user/wizard/wiz-new-workflow-step";
    }

    @PreAuthorize("@preAuthorizeService.notInShare(#userEppn, #authUserEppn) && hasRole('ROLE_USER')")
    @GetMapping(value = "/wiz-start-form/{formId}", produces = "text/html")
    public String wizStartForm(@PathVariable("formId") Long formId, @ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, Model model) {
        if(formService.isFormAuthorized(userEppn, authUserEppn, formId)) {
            Form form = formService.getById(formId);
            form.setMessageToDisplay(formService.getHelpMessage(userEppn, form));
            model.addAttribute("form", form);
            return "user/wizard/wiz-start-form";
        } else {
            return "user/wizard/wiz-not-autorized";
        }
    }

    @PreAuthorize("@preAuthorizeService.notInShare(#userEppn, #authUserEppn) && hasRole('ROLE_USER')")
    @ResponseBody
    @PostMapping(value = "/wiz-create-sign/{type}")
    public ResponseEntity<Long> wizCreateSign(@PathVariable String type, @ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn) {
        String workflowName = "Demande simple";
        if(type.equals("self")) {
            workflowName = "Auto signature";
        }
        SignBook signBook = signBookService.createSignBook(null, null, workflowName, userEppn, false, null);
        return ResponseEntity.ok().body(signBook.getId());
    }

    @PreAuthorize("@preAuthorizeService.signBookCreator(#signBookId, #userEppn)")
    @ResponseBody
    @PostMapping(value = "/start-self-sign/{signBookId}")
    public void startSelfSign(@PathVariable("signBookId") Long signBookId, @ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn) {
        signBookService.createSelfSignBook(signBookId, userEppn);
    }

    @PreAuthorize("@preAuthorizeService.signBookCreator(#signBookId, #userEppn)")
    @ResponseBody
    @PostMapping(value = "/update-fast-sign/{signBookId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateFastSign(@PathVariable("signBookId") Long signBookId,
                                         @ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
                                         @RequestBody List<WorkflowStepDto> steps,
                                         @RequestParam(value = "multiSign", required = false) Boolean multiSign,
                                         @RequestParam(value = "singleSignWithAnnotation", required = false) Boolean singleSignWithAnnotation,
                                         @RequestParam(value = "pending", required = false) Boolean pending) throws EsupSignatureRuntimeException {
        if(pending == null) pending = false;
        try {
            signBookService.startFastSignBook(signBookId, pending, steps, userEppn, authUserEppn, multiSign, singleSignWithAnnotation);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@preAuthorizeService.signBookCreator(#signBookId, #userEppn)")
    @ResponseBody
    @PostMapping(value = "/check-users/{signBookId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> checkUsers(@PathVariable("signBookId") Long signBookId,
                                                 @ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
                                                 @RequestBody List<WorkflowStepDto> steps) throws EsupSignatureRuntimeException {
        try {
            List<RecipientWsDto> recipients = steps.stream().map(WorkflowStepDto::getRecipients).flatMap(List::stream).toList();
            signRequestService.checkTempUsers(signBookId, recipients);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@preAuthorizeService.notInShare(#userEppn, #authUserEppn) && hasRole('ROLE_USER')")
    @ResponseBody
    @PostMapping(value = "/wiz-create-workflow-sign")
    public ResponseEntity<Long> wizCreateWorkflowSign(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
                                                      @RequestParam(value = "workflowId", required = false) Long workflowId,
                                                      @RequestParam(value = "title", required = false, defaultValue = "") String title,
                                                      @RequestParam(value = "comment", required = false, defaultValue = "") String comment) {
        Workflow workflow = null;
        String name = "Demande personnalisée";
        if (workflowId != null) {
            workflow = workflowService.getById(workflowId);
            name = workflow.getDescription();
        }
        SignBook signBook = signBookService.createSignBook(title, workflow, name, userEppn, false, comment);
        return ResponseEntity.ok().body(signBook.getId());
    }

    @PreAuthorize("@preAuthorizeService.signBookCreator(#signBookId, #userEppn)")
    @PostMapping(value = "/wiz-new-step/{signBookId}")
    public String wizWorkflowSignNewStep(@ModelAttribute("userEppn") String userEppn,
                       @PathVariable("signBookId") Long signBookId,
                       @RequestParam(value = "workflowId", required = false) Long workflowId,
                       @RequestBody List<WorkflowStepDto> steps,
                       Model model) {
        SignBook signBook = signBookService.updateSignBookWithStep(signBookId, steps);
        signBookService.finishSignBookUpload(signBookId, userEppn);
        if(signBook.getCreateBy().getEppn().equals(userEppn)) {
            model.addAttribute("signBook", signBook);
            if (workflowId != null && workflowId != 0) {
                signBookService.initSignBook(signBookId, workflowId, userEppn);
                model.addAttribute("isTempUsers", signBookService.isTempUsers(signBook.getId()));
                model.addAttribute("workflowId", workflowId);
                model.addAttribute("modalTitle", "Création d'une nouvelle demande dans le circuit : " + signBook.getLiveWorkflow().getWorkflow().getDescription());
                return "user/wizard/wiz-setup-workflow";
            }
        }
        return "user/wizard/wiz-new-step";
    }


    @PreAuthorize("@preAuthorizeService.signBookCreator(#signBookId, #userEppn)")
    @PostMapping(value = "/wiz-add-step-signbook/{signBookId}", produces = "text/html")
    @ResponseBody
    public ResponseEntity<String> wizWorkflowSignAddStep(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
                                                         @PathVariable("signBookId") Long signBookId,
                                                         @RequestParam(name="end", required = false, defaultValue = "false") Boolean end,
                                                         @RequestParam(name="close", required = false) Boolean close,
                                                         @RequestParam(name="start", required = false) Boolean start,
                                                         @RequestBody List<WorkflowStepDto> steps,
                                                         Model model, HttpServletRequest request, HttpServletResponse response) throws EsupSignatureRuntimeException {
        JakartaServletWebApplication jakartaServletWebApplication = JakartaServletWebApplication.buildApplication(request.getServletContext());
        IServletWebExchange iServletWebExchange = jakartaServletWebApplication.buildExchange(request, response);
        final WebContext context = new WebContext(iServletWebExchange, Locale.FRENCH);
        SignBook signBook = signBookService.getById(signBookId);
        if(signBook.getCreateBy().getEppn().equals(userEppn)) {
            if(!end && !steps.isEmpty()) {
                if(steps.get(0).getUserSignFirst() != null && steps.get(0).getUserSignFirst()) {
                    signBookService.addUserSignFirstStep(signBookId, userEppn);
                }
                signBookService.addNewStepToSignBook(signBookId, steps, authUserEppn);
            }
            model.addAttribute("signBook", signBook);
            model.addAttribute("close", close);
            model.asMap().forEach(context::setVariable);
            if(end) {
                if(signBookService.startLiveWorkflow(signBookId, userEppn, authUserEppn, start)) {
                    return ResponseEntity.ok().body(templateEngine.process("user/wizard/wiz-save", context));
                } else {
                    return ResponseEntity.ok().body(templateEngine.process("user/wizard/wiz-end", context));
                }
            }
        }
        return ResponseEntity.ok().body(templateEngine.process("user/wizard/wiz-new-step", context));
    }

    @PostMapping(value = "/wiz-save-signbook/{id}")
    public String saveWorkflow(@ModelAttribute("userEppn") String userEppn,
                               @PathVariable("id") Long id,
                               @RequestParam(name="name") String name,
                               @RequestParam(required = false) List<String> viewers,
                               Model model) {
        SignBook signBook = signBookService.getById(id);
        model.addAttribute("signBook", signBook);
        try {
            signBookService.saveSignBookAsWorkflow(id, name, name, userEppn);
            signBookService.addViewers(id, viewers);
        } catch (EsupSignatureRuntimeException e) {
            return "user/wizard/wiz-save";
        }
        return "user/wizard/wiz-end";
    }

    @PostMapping(value = "/wiz-add-step-workflow/{workflowId}", produces = "text/html")
    @ResponseBody
    public ResponseEntity<String> wizXWorkflow(@PathVariable("workflowId") Long workflowId,
                               @ModelAttribute("userEppn") String userEppn,
                               @RequestParam(name="end", required = false) Boolean end,
                               @RequestBody List<WorkflowStepDto> steps,
                               Model model, HttpServletRequest request, HttpServletResponse response) {
        JakartaServletWebApplication jakartaServletWebApplication = JakartaServletWebApplication.buildApplication(request.getServletContext());
        IServletWebExchange iServletWebExchange = jakartaServletWebApplication.buildExchange(request, response);
        final WebContext context = new WebContext(iServletWebExchange, Locale.FRENCH);
        Workflow workflow;
        workflow = workflowService.addStepToWorkflow(workflowId, steps.get(0), userEppn);
        model.addAttribute("workflow", workflow);
        model.asMap().forEach(context::setVariable);
        if(end != null && end) {
            if(workflow!=null && !workflow.getWorkflowSteps().isEmpty()) {
                return ResponseEntity.ok().body(templateEngine.process("user/wizard/wiz-save-workflow", context));
            }else {
                return ResponseEntity.ok().body(templateEngine.process("user/wizard/wiz-end", context));
            }
        }
        return ResponseEntity.ok().body(templateEngine.process("user/wizard/wiz-new-workflow-step", context));
    }

    @GetMapping(value = "/wiz-save-workflow/{id}")
    public String wiz5Workflow(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id,
                               Model model) {
        Workflow workflow = workflowService.getById(id);
        if(workflow.getCreateBy().getEppn().equals(userEppn)) {
            model.addAttribute("workflow", workflow);
        }
        return "user/wizard/wiz-save-workflow";
    }

    @PostMapping(value = "/wiz-save-workflow/{id}")
    @ResponseBody
    public ResponseEntity<Void> wizSaveWorkflow(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id, @RequestParam(name="name") String name,
                               @RequestParam(required = false) List<String> viewers) {
        if(!StringUtils.hasText(name)) return ResponseEntity.badRequest().build();
        workflowService.updateWorkflow(userEppn, id, name, viewers);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@preAuthorizeService.signBookOwner(#signBookId, #authUserEppn)")
    @PostMapping(value = "/wiz-init-workflow/{signBookId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> pending(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
                                        @PathVariable("signBookId") Long signBookId,
                                        @RequestBody List<WorkflowStepDto> steps,
                                        @RequestParam(value = "comment", required = false) String comment,
                                        @RequestParam(value = "pending", required = false) Boolean pending,
                                        @RequestParam(value = "sendEmailAlert", required = false, defaultValue = "true") Boolean sendEmailAlert) throws MessagingException, EsupSignatureRuntimeException {
        if (sendEmailAlert == null) sendEmailAlert = true;
        try {
            signBookService.initSignBookWorkflow(signBookId, steps, steps.get(0).getTargetEmails(), userEppn, authUserEppn, pending, sendEmailAlert);
            if(comment != null && !comment.isEmpty()) {
                signRequestService.addPostit(signBookId, comment, userEppn, authUserEppn);
            }
        } catch (EsupSignatureRuntimeException e) {
            if(e instanceof EsupSignatureFsException) {
                signBookService.deleteDefinitive(signBookId, authUserEppn);
                logger.error(e.getMessage());
            } else {
                logger.warn(e.getMessage());
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return ResponseEntity.ok().body(signBookId);
    }

    @DeleteMapping(value = "/delete-workflow/{id}", produces = "text/html")
    public String delete(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Workflow workflow = workflowService.getById(id);
        if (!workflow.getCreateBy().getEppn().equals(userEppn)) {
			redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Non autorisé"));
		} else {
            try {
                workflowService.delete(id);
            } catch (EsupSignatureRuntimeException e) {
                redirectAttributes.addFlashAttribute("message", new JsMessage("error", e.getMessage()));
            }
        }
        return "redirect:/user";
    }

}
