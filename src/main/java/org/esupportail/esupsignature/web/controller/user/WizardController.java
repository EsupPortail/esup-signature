package org.esupportail.esupsignature.web.controller.user;

import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureMailException;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
import org.esupportail.esupsignature.web.ws.json.JsonWorkflowStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.List;

@RequestMapping("/user/wizard")
@Controller
public class WizardController {

    private static final Logger logger = LoggerFactory.getLogger(WizardController.class);

    @Resource
    private WorkflowService workflowService;

    @Resource
    private SignBookService signBookService;

    @Resource
    private LiveWorkflowStepService liveWorkflowStepService;

    @Resource
    private UserService userService;

    @Resource
    private SignRequestService signRequestService;

    @GetMapping(value = "/wiz-start-by-docs", produces = "text/html")
    public String wiz2(@RequestParam(value = "workflowId", required = false) Long workflowId, Model model) {
        if (workflowId != null) {
            Workflow workflow = workflowService.getById(workflowId);
            model.addAttribute("workflow", workflow);
        }
        return "user/wizard/wiz-start-by-docs";
    }

    @GetMapping(value = "/wiz-init-steps")
    public String wiz4(@ModelAttribute("userEppn") String userEppn,
                       @SessionAttribute("signBookId") Long signBookId,
                       @RequestParam(value = "workflowId", required = false) Long workflowId,
                       @RequestParam(value = "forceAllSign", required = false) Boolean forceAllSign,
                       @RequestParam(value = "recipientsCCEmailsWiz", required = false) List<String> recipientsCCEmailsWiz,
                       @RequestParam(value = "comment", required = false) String comment,
                       Model model) {
        SignBook signBook = signBookService.getById(signBookId);
        signBookService.finishSignBookUpload(signBookId, userEppn);
        signBook.setDescription(comment);
        signBook.setForceAllDocsSign(forceAllSign);
        signBookService.addViewers(signBookId, recipientsCCEmailsWiz);
        if(signBook.getCreateBy().getEppn().equals(userEppn)) {
            model.addAttribute("signBook", signBook);
            if (workflowId != null && workflowId != 0) {
                signBookService.initSignBook(signBookId, workflowId, userEppn);
                model.addAttribute("isTempUsers", signRequestService.isTempUsers(signBook.getSignRequests().get(0).getId()));
                return "user/wizard/wiz-setup-workflow";
            }
        }
        return "user/wizard/wiz-init-steps";
    }

    @PostMapping(value = "/wiz-add-step", produces = "text/html")
    public String wizX(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
                       @SessionAttribute("signBookId") Long signBookId,
                       @RequestParam(name="addNew", required = false) Boolean addNew,
                       @RequestParam(name="userSignFirst", required = false) Boolean userSignFirst,
                       @RequestParam(name="end", required = false) Boolean end,
                       @RequestParam(name="close", required = false) Boolean close,
                       @RequestParam(name="start", required = false) Boolean start,
                       @RequestBody JsonWorkflowStep step,
                       Model model) throws EsupSignatureException {
        SignBook signBook = signBookService.getById(signBookId);
        if(signBook.getCreateBy().getEppn().equals(userEppn)) {
            if(step.getRecipientsEmails() != null && step.getRecipientsEmails().size() > 0) {
                if (userSignFirst) {
                    liveWorkflowStepService.addNewStepToSignBook(signBookId, SignType.pdfImageStamp, false, Collections.singletonList(userService.getByEppn(authUserEppn).getEmail()), null, authUserEppn);
                }
                liveWorkflowStepService.addNewStepToSignBook(signBookId, SignType.valueOf(step.getSignType()), step.getAllSignToComplete(), step.getRecipientsEmails(), step.getExternalUsersInfos(), authUserEppn);
            } else {
                end = true;
            }
            model.addAttribute("signBook", signBook);
            model.addAttribute("close", close);
            if(end != null && end) {
                if(signBookService.startLiveWorkflow(signBook, userEppn, authUserEppn, start)) {
                    return "user/wizard/wiz-save";
                } else {
                    return "user/wizard/wizend";
                }
            }
        }
        return "user/wizard/wiz-init-steps";
    }

    @PostMapping(value = "/wiz-save/{id}")
    public String saveWorkflow(@ModelAttribute("userEppn") String userEppn,
                               @PathVariable("id") Long id,
                               @RequestParam(name="name") String name,
                               @RequestParam(required = false) List<String> viewers,
                               Model model) {
        User user = (User) model.getAttribute("user");
        SignBook signBook = signBookService.getById(id);
        model.addAttribute("signBook", signBook);
        try {
            signBookService.saveWorkflow(id, name, name, user);
            signBookService.addViewers(id, viewers);
        } catch (EsupSignatureException e) {
            return "user/wizard/wiz-save";
        }
        return "user/wizard/wizend";
    }


    @GetMapping(value = "/wiz-init-steps-workflow")
    public String wizWorkflow(@ModelAttribute("userEppn") String userEppn, Model model) {
        return "user/wizard/wiz-init-steps-workflow";
    }

    @PostMapping(value = "/wiz-add-step-workflow", produces = "text/html")
    public String wizXWorkflow(@ModelAttribute("userEppn") String userEppn,
                               @RequestParam(name="end", required = false) Boolean end,
                               @RequestBody JsonWorkflowStep step,
                               Model model) {
        User user = (User) model.getAttribute("user");
        String[] recipientsEmailsArray = new String[step.getRecipientsEmails().size()];
        recipientsEmailsArray = step.getRecipientsEmails().toArray(recipientsEmailsArray);
        Workflow  workflow = workflowService.addStepToWorkflow(step.getWorkflowId(), SignType.valueOf(step.getSignType()), step.getAllSignToComplete(), recipientsEmailsArray, user);
        model.addAttribute("workflow", workflow);
        if(end != null && end) {
            if(workflow.getWorkflowSteps().size() >  0) {
                return "user/wizard/wiz-save-workflow";
            }else {
                return "user/wizard/wizend";
            }
        }
        return "user/wizard/wiz-init-steps-workflow";
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
    public String wiz5Workflow(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id, @RequestParam(name="name") String name,
                               @RequestParam(required = false) List<String> viewers,
                               Model model) {
        User user = (User) model.getAttribute("user");
        if(!workflowService.isWorkflowExist(name, userEppn)) {
            Workflow workflow = workflowService.initWorkflow(user, id, name);
            workflowService.addViewers(id, viewers);
            model.addAttribute("workflow", workflow);
            return "user/wizard/wizend";
        } else {
            Workflow workflow = workflowService.getById(id);
            model.addAttribute("workflow", workflow);
            return "user/wizard/wiz-save-workflow";
        }
    }

    @GetMapping(value = "/wizend-workflow/{id}")
    public String wizEndWorkflow(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id, Model model) throws EsupSignatureException {
        Workflow workflow = workflowService.getById(id);
        if(workflow.getCreateBy().getEppn().equals(userEppn)) {
            model.addAttribute("workflow", workflow);
            return "user/wizard/wizend";
        } else {
            throw new EsupSignatureException("not authorized");
        }
    }

    @PostMapping(value = "/wizend")
    public String wizEnd(@ModelAttribute("userEppn") String userEppn, @SessionAttribute("signBookId") Long signBookId, @RequestParam(name="close") String close, Model model) throws EsupSignatureException, EsupSignatureMailException {
        SignBook signBook = signBookService.getById(signBookId);
        if(signBook.getCreateBy().getEppn().equals(userEppn)) {
            signBookService.sendCCEmail(signBookId, null);
            model.addAttribute("signBook", signBook);
            model.addAttribute("close", close);
            return "user/wizard/wizend";
        } else {
            throw new EsupSignatureException("not authorized");
        }
    }

    @GetMapping(value = "/wizredirect")
    public String wizRedirect(@ModelAttribute("userEppn") String userEppn, @SessionAttribute("signBookId") Long signBookId, HttpSession session, RedirectAttributes redirectAttributes) throws EsupSignatureException {
        SignBook signBook = signBookService.getById(signBookId);
        if(signBook.getCreateBy().getEppn().equals(userEppn)) {
            if(signBook.getLiveWorkflow().getCurrentStep() == null) {
                redirectAttributes.addFlashAttribute("message", new JsonMessage("warn", "Après vérification, vous devez confirmer l'envoi pour finaliser la demande"));
            }
            session.removeAttribute("signBookId");
            return "redirect:/user/signrequests/" + signBook.getSignRequests().get(0).getId();
        } else {
            throw new EsupSignatureException("not authorized");
        }
    }

    @DeleteMapping(value = "/delete-workflow/{id}", produces = "text/html")
    public String delete(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Workflow workflow = workflowService.getById(id);
        if (!workflow.getCreateBy().getEppn().equals(userEppn)) {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Non autorisé"));
		} else {
            try {
                workflowService.delete(workflow);
            } catch (EsupSignatureException e) {
                redirectAttributes.addFlashAttribute("message", new JsonMessage("error", e.getMessage()));
            }
        }
        return "redirect:/user/";
    }

}
