package org.esupportail.esupsignature.web.controller.user;

import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import org.esupportail.esupsignature.dto.WorkflowStepDto;
import org.esupportail.esupsignature.dto.js.JsMessage;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.exception.EsupSignatureMailException;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.mail.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

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
    private LiveWorkflowStepService liveWorkflowStepService;

    @Resource
    private UserService userService;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private MailService mailService;

    @PreAuthorize("@preAuthorizeService.notInShare(#userEppn, #authUserEppn) && hasRole('ROLE_USER')")
    @GetMapping(value = "/wiz-start-sign/{type}", produces = "text/html")
    public String wizStartSign(@PathVariable String type, @ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
                               @RequestParam(value = "workflowId", required = false) Long workflowId, Model model) {
        String modalTile = "Création d'une nouvelle demande";
        if(workflowId != null) {
            Workflow workflow = workflowService.getById(workflowId);
            modalTile = "Création d'une nouvelle demande dans le circuit : " + workflow.getName();
            model.addAttribute("workflow", workflow);
        }
        model.addAttribute("modalTitle", modalTile);
        return "user/wizard/wiz-" + type + "-sign";
    }

    @PreAuthorize("@preAuthorizeService.notInShare(#userEppn, #authUserEppn) && hasRole('ROLE_USER')")
    @GetMapping(value = "/wiz-start-workflow", produces = "text/html")
    public String wizStartWorkflow( @ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn) {
        return "user/wizard/wiz-new-workflow-step";
    }

    @PreAuthorize("@preAuthorizeService.notInShare(#userEppn, #authUserEppn) && hasRole('ROLE_USER')")
    @GetMapping(value = "/wiz-start-form/{formId}", produces = "text/html")
    public String wizStartForm(@PathVariable("formId") Long formId, @ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, Model model) {
        model.addAttribute("form", formService.getById(formId));
        return "user/wizard/wiz-start-form";
    }

    @PreAuthorize("@preAuthorizeService.notInShare(#userEppn, #authUserEppn) && hasRole('ROLE_USER')")
    @ResponseBody
    @PostMapping(value = "/wiz-create-sign/{type}")
    public ResponseEntity<Long> wizCreateSign(@PathVariable String type, @ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn) {
        String workflowName = "Demande simple";
        if(type.equals("self")) {
            workflowName = "Auto signature";
        }
        SignBook signBook = signBookService.createSignBook(null, null, workflowName, userEppn, false);
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
    public ResponseEntity<Void> updateFastSign(@PathVariable("signBookId") Long signBookId,
                                         @ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
                                         @RequestBody List<WorkflowStepDto> steps,
                                         @RequestParam(value = "pending", required = false) Boolean pending) throws EsupSignatureRuntimeException {
        if(pending == null) pending = false;
        try {
            signBookService.startFastSignBook(signBookId, pending, steps, userEppn, authUserEppn, false);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@preAuthorizeService.notInShare(#userEppn, #authUserEppn) && hasRole('ROLE_USER')")
    @ResponseBody
    @PostMapping(value = "/wiz-create-workflow-sign")
    public ResponseEntity<Long> wizCreateWorkflowSign(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @RequestParam(value = "workflowId", required = false) Long workflowId) {
        Workflow workflow = null;
        String name = "Demande personnalisée";
        if (workflowId != null) {
            workflow = workflowService.getById(workflowId);
            name = workflow.getName();
        }
        SignBook signBook = signBookService.createSignBook("", workflow, name, userEppn, false);
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
                model.addAttribute("isTempUsers", signRequestService.isTempUsers(signBook.getId()));
                model.addAttribute("workflowId", workflowId);
                model.addAttribute("modalTitle", "Création d'une nouvelle demande dans le circuit : " + signBook.getLiveWorkflow().getWorkflow().getName());
                return "user/wizard/wiz-setup-workflow";
            }
        }
        return "user/wizard/wiz-new-step";
    }


    @PreAuthorize("@preAuthorizeService.signBookCreator(#signBookId, #userEppn)")
    @PostMapping(value = "/wiz-add-step-signbook/{signBookId}", produces = "text/html")
    public String wizWorkflowSignAddStep(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
                       @PathVariable("signBookId") Long signBookId,
                       @RequestParam(name="end", required = false) Boolean end,
                       @RequestParam(name="close", required = false) Boolean close,
                       @RequestParam(name="start", required = false) Boolean start,
                       @RequestBody List<WorkflowStepDto> steps,
                       Model model) throws EsupSignatureRuntimeException {
        SignBook signBook = signBookService.getById(signBookId);
        if(signBook.getCreateBy().getEppn().equals(userEppn)) {
            if(!steps.get(0).getRecipients().isEmpty()) {
                signBookService.importWorkflowFromWorkflowStepDto(signBookId, steps, userEppn);
            } else {
                end = true;
            }
            model.addAttribute("signBook", signBook);
            model.addAttribute("close", close);
            if(end != null && end) {
                if(signBookService.startLiveWorkflow(signBook, userEppn, authUserEppn, start)) {
                    return "user/wizard/wiz-save";
                } else {
                    return "user/wizard/wiz-end";
                }
            }
        }
        return "user/wizard/wiz-new-step";
    }

    @PostMapping(value = "/wiz-save-signbook/{id}")
    public String saveWorkflow(@ModelAttribute("userEppn") String userEppn,
                               @PathVariable("id") Long id,
                               @RequestParam(name="name") String name,
                               @RequestParam(required = false) List<String> viewers,
                               Model model) {
        User user = (User) model.getAttribute("user");
        SignBook signBook = signBookService.getById(id);
        model.addAttribute("signBook", signBook);
        try {
            signBookService.saveSignBookAsWorkflow(id, name, name, user);
            signBookService.addViewers(id, viewers);
        } catch (EsupSignatureRuntimeException e) {
            return "user/wizard/wiz-save";
        }
        return "user/wizard/wiz-end";
    }

    @PostMapping(value = "/wiz-add-step-workflow/{workflowId}", produces = "text/html")
    public String wizXWorkflow(@PathVariable("workflowId") Long workflowId,
                               @ModelAttribute("userEppn") String userEppn,
                               @RequestParam(name="end", required = false) Boolean end,
                               @RequestBody List<WorkflowStepDto> steps,
                               Model model) {
        User user = (User) model.getAttribute("user");
        try {
            Workflow workflow = workflowService.addStepToWorkflow(workflowId, steps.get(0).getSignType(), steps.get(0).getAllSignToComplete(), steps.get(0).getChangeable(), steps.get(0), user);
            model.addAttribute("workflow", workflow);
            if(end != null && end) {
                if(!workflow.getWorkflowSteps().isEmpty()) {
                    return "user/wizard/wiz-save-workflow";
                }else {
                    return "user/wizard/wiz-end";
                }
            }
        } catch (EsupSignatureRuntimeException e) {
            logger.warn(e.getMessage());
        }
        return "user/wizard/wiz-new-workflow-step";
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
                               @RequestParam(required = false) List<String> viewers,
                               Model model) {
        Workflow workflow = workflowService.updateWorkflow(userEppn, id, name, viewers);
        model.addAttribute("workflow", workflow);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@preAuthorizeService.signBookOwner(#signBookId, #authUserEppn)")
    @PostMapping(value = "/wiz-init-workflow/{signBookId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Long> pending(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
                                        @PathVariable("signBookId") Long signBookId,
                                        @RequestBody List<WorkflowStepDto> steps,
                                        @RequestParam(value = "comment", required = false) String comment,
                                        @RequestParam(value = "pending", required = false) Boolean pending,
                                        @RequestParam(value = "sendEmailAlert", required = false, defaultValue = "true") Boolean sendEmailAlert,
                                        RedirectAttributes redirectAttributes) throws MessagingException, EsupSignatureRuntimeException {
        if (sendEmailAlert == null) sendEmailAlert = true;
        try {
            signBookService.initSignBookWorkflow(signBookId, steps, null, userEppn, authUserEppn, pending, sendEmailAlert);
            if(comment != null && !comment.isEmpty()) {
                signRequestService.addPostit(signBookId, comment, userEppn, authUserEppn);
            }
            redirectAttributes.addFlashAttribute("message", new JsMessage("success", "Votre demande a bien été transmise"));
        } catch (EsupSignatureRuntimeException e) {
            logger.error(e.getMessage(), e);
            redirectAttributes.addFlashAttribute("message", new JsMessage("error", e.getMessage()));
        }
        return ResponseEntity.ok().body(signBookId);
    }

    @GetMapping(value = "/wiz-end-workflow/{id}")
    public String wizEndWorkflow(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id, Model model) throws EsupSignatureRuntimeException {
        Workflow workflow = workflowService.getById(id);
        if(workflow.getCreateBy().getEppn().equals(userEppn)) {
            model.addAttribute("workflow", workflow);
            return "user/wizard/wiz-end";
        } else {
            throw new EsupSignatureRuntimeException("not authorized");
        }
    }

    @PreAuthorize("@preAuthorizeService.signBookCreator(#signBookId, #userEppn)")
    @PostMapping(value = "/wiz-end/{signBookId}")
    public String wizEnd(@ModelAttribute("userEppn") String userEppn, @PathVariable("signBookId") Long signBookId, @RequestParam(name="close") String close, Model model) throws EsupSignatureRuntimeException, EsupSignatureMailException {
        SignBook signBook = signBookService.getById(signBookId);
        if(signBook.getCreateBy().getEppn().equals(userEppn)) {
            mailService.sendCCAlert(signBook, null);
            model.addAttribute("signBook", signBook);
            model.addAttribute("close", close);
            return "user/wizard/wiz-end";
        } else {
            throw new EsupSignatureRuntimeException("not authorized");
        }
    }

    @PreAuthorize("@preAuthorizeService.signBookCreator(#signBookId, #userEppn)")
    @GetMapping(value = "/wizredirect/{signBookId}")
    public String wizRedirect(@ModelAttribute("userEppn") String userEppn, @PathVariable("signBookId") Long signBookId, RedirectAttributes redirectAttributes) throws EsupSignatureRuntimeException {
        SignBook signBook = signBookService.getById(signBookId);
        if(signBook.getCreateBy().getEppn().equals(userEppn)) {
            if(signBook.getLiveWorkflow().getCurrentStep() == null) {
                redirectAttributes.addFlashAttribute("message", new JsMessage("warn", "Après vérification, vous devez confirmer l'envoi pour finaliser la demande"));
            }
            return "redirect:/user/signrequests/" + signBook.getSignRequests().get(0).getId();
        } else {
            throw new EsupSignatureRuntimeException("not authorized");
        }
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
