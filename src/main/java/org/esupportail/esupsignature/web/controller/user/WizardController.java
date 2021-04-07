package org.esupportail.esupsignature.web.controller.user;

import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.event.EventService;
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
import org.esupportail.esupsignature.web.ws.json.JsonWorkflowStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
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
    private EventService eventService;

    @Resource
    private UserService userService;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private CommentService commentService;

    @GetMapping(value = "/wiz-start-by-docs", produces = "text/html")
    public String wiz2(@RequestParam(value = "workflowId", required = false) Long workflowId, Model model) {
        logger.debug("Choix des fichiers");
        if (workflowId != null) {
            Workflow workflow = workflowService.getById(workflowId);
            model.addAttribute("workflow", workflow);
        }
        return "user/wizard/wiz-start-by-docs";
    }

    @GetMapping(value = "/wiz-init-steps/{id}")
    public String wiz4(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id,
                       @RequestParam(value = "workflowId", required = false) Long workflowId,
                       @RequestParam(value = "recipientsCCEmailsWiz", required = false) List<String> recipientsCCEmailsWiz,
                       @RequestParam(value = "comment", required = false) String comment,
                       Model model) {
        User user = (User) model.getAttribute("user");
        SignBook signBook = signBookService.getById(id);
        if(comment != null && !comment.isEmpty()) {
            commentService.create(signBook.getSignRequests().get(0).getId(), comment, 0, 0, 0, null, true, null, userEppn);
        }
        signBookService.addViewers(id, recipientsCCEmailsWiz);
        if(signBook.getCreateBy().getEppn().equals(userEppn)) {
            model.addAttribute("signBook", signBook);
            if (workflowId != null) {
                signBookService.initSignBook(id, workflowId, user);
                model.addAttribute("isTempUsers", signRequestService.isTempUsers(signBook.getSignRequests().get(0).getId()));
                return "user/wizard/wiz-setup-workflow";
            }
            model.addAttribute("signTypes", SignType.values());
        }
        return "user/wizard/wiz-init-steps";
    }

    @PostMapping(value = "/wiz-add-step/{id}", produces = "text/html")
    public String wizX(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                       @RequestParam(name="addNew", required = false) Boolean addNew,
                       @RequestParam(name="userSignFirst", required = false) Boolean userSignFirst,
                       @RequestParam(name="end", required = false) Boolean end,
                       @RequestParam(name="close", required = false) Boolean close,
                       @RequestParam(name="start", required = false) Boolean start,
                       @RequestBody JsonWorkflowStep step,
                       Model model) throws EsupSignatureException {
        SignBook signBook = signBookService.getById(id);
        if(signBook.getCreateBy().getEppn().equals(userEppn)) {
            if(step.getRecipientsEmails() != null && step.getRecipientsEmails().size() > 0) {
                if (userSignFirst) {
                    liveWorkflowStepService.addNewStepToSignBook(id, SignType.pdfImageStamp, false, Collections.singletonList(userService.getByEppn(authUserEppn).getEmail()), null, authUserEppn);
                }
                liveWorkflowStepService.addNewStepToSignBook(id, SignType.valueOf(step.getSignType()), step.getAllSignToComplete(), step.getRecipientsEmails(), step.getExternalUsersInfos(), authUserEppn);
                if (addNew != null) {
                    model.addAttribute("signTypes", SignType.values());
                }
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

//    @GetMapping(value = "/wiz-save/{id}")
//    public String saveForm(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id, Model model) {
//        SignBook signBook = signBookService.getById(id);
//    }

    @PostMapping(value = "/wiz-save/{id}")
    public String saveWorkflow(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id,
                               @RequestParam(name="name") String name,
                               Model model) {
        User user = (User) model.getAttribute("user");
        SignBook signBook = signBookService.getById(id);
        model.addAttribute("signBook", signBook);
        try {
            signBookService.saveWorkflow(id, name, name, user);
        } catch (EsupSignatureException e) {
            eventService.publishEvent(new JsonMessage("error", "Un circuit de signature porte déjà ce nom"), "user", eventService.getClientIdByEppn(userEppn));
            return "user/wizard/wiz-save";
        }
        return "user/wizard/wizend";
    }


    @GetMapping(value = "/wiz-init-steps-workflow")
    public String wizWorkflow(@ModelAttribute("userEppn") String userEppn, Model model) {
        model.addAttribute("signTypes", SignType.values());
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
    public String wiz5Workflow(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id, Model model) {
        Workflow workflow = workflowService.getById(id);
        if(workflow.getCreateBy().getEppn().equals(userEppn)) {
            model.addAttribute("workflow", workflow);
        }
        return "user/wizard/wiz-save-workflow";
    }

    @PostMapping(value = "/wiz-save-workflow/{id}")
    public String wiz5Workflow(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id, @RequestParam(name="name") String name, Model model) {
        User user = (User) model.getAttribute("user");
        if(!workflowService.isWorkflowExist(name, userEppn)) {
            Workflow workflow = workflowService.initWorkflow(user, id, name);
            model.addAttribute("workflow", workflow);
            return "user/wizard/wizend";
        } else {
            Workflow workflow = workflowService.getById(id);
            model.addAttribute("workflow", workflow);
            eventService.publishEvent(new JsonMessage("error", "Un circuit de signature porte déjà ce nom"), "user", eventService.getClientIdByEppn(userEppn));
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

    @PostMapping(value = "/wizend/{id}")
    public String wizEnd(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id, @RequestParam(name="close") String close, Model model) throws EsupSignatureException {
        SignBook signBook = signBookService.getById(id);
        if(signBook.getCreateBy().getEppn().equals(userEppn)) {
            signBookService.sendCCEmail(id, null);
            model.addAttribute("signBook", signBook);
            model.addAttribute("close", close);
            return "user/wizard/wizend";
        } else {
            throw new EsupSignatureException("not authorized");
        }
    }

    @GetMapping(value = "/wizredirect/{id}")
    public String wizRedirect(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) throws EsupSignatureException {
        SignBook signBook = signBookService.getById(id);
        if(signBook.getCreateBy().getEppn().equals(userEppn)) {
//            signBookService.startLiveWorkflow(signBook, userEppn, userEppn, false);
            if(signBook.getLiveWorkflow().getCurrentStep() == null) {
                redirectAttributes.addFlashAttribute("message", new JsonMessage("warn", "Après vérification, vous devez confirmer l'envoi pour finaliser la demande"));
            }
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
            if(workflowService.delete(workflow)) {
                redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Circuit supprimé"));
            } else {
                redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Impossible de supprimer ce circuit car il contient des demandes"));
            }
        }
        return "redirect:/user/";
    }



}
