package org.esupportail.esupsignature.web.controller.user;

import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.service.LiveWorkflowStepService;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.esupportail.esupsignature.service.event.EventService;
import org.esupportail.esupsignature.web.controller.ws.json.JsonMessage;
import org.esupportail.esupsignature.web.controller.ws.json.JsonWorkflowStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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

    @GetMapping(value = "/wiz2", produces = "text/html")
    public String wiz2(@RequestParam(value = "workflowId", required = false) Long workflowId, Model model) {
        logger.debug("Choix des fichiers");
        if (workflowId != null) {
            Workflow workflow = workflowService.getById(workflowId);
            model.addAttribute("workflow", workflow);
        }
        return "user/wizard/wiz2";
    }

    @PostMapping(value = "/wiz3", produces = "text/html")
    public ModelAndView wiz3(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @RequestParam(value = "workflowId", required = false) Long workflowId, Model model) {
        logger.debug("Choix d'un workflow");
        List<SignBook> signBooks = signBookService.getByCreateBy(userEppn);
        SignBook signBook = signBooks.stream().sorted(Comparator.comparing(SignBook::getCreateDate).reversed()).collect(Collectors.toList()).get(0);
        model.addAttribute("signBook", signBook);
        model.addAttribute("workflows", workflowService.getWorkflowsByUser(userEppn, authUserEppn));
        ModelAndView modelAndView = new ModelAndView("redirect:/user/wizard/wiz4/" + signBook.getId());
        if (workflowId != null) {
            Workflow workflow = workflowService.getById(workflowId);
            modelAndView.addObject("workflowId", workflow.getId());
        }
        return modelAndView;
    }

    @GetMapping(value = "/wiz4Workflow")
    public String wizWorkflow(@ModelAttribute("userEppn") String userEppn, Model model) {
        model.addAttribute("signTypes", SignType.values());
        return "user/wizard/wiz4Workflow";
    }

    @PostMapping(value = "/wizXWorkflow", produces = "text/html")
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
                return "user/wizard/wiz5Workflow";
            }else {
                return "user/wizard/wizendWorkflow";
            }
        }
        return "user/wizard/wiz4Workflow";
    }

    @GetMapping(value = "/wiz5Workflow/{id}")
    public String wiz5Workflow(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id, Model model) {
        Workflow workflow = workflowService.getById(id);
        if(workflow.getCreateBy().getEppn().equals(userEppn)) {
            model.addAttribute("workflow", workflow);
        }
        return "user/wizard/wiz5Workflow";
    }

    @PostMapping(value = "/wiz5Workflow/{id}")
    public String wiz5Workflow(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id, @RequestParam(name="name") String name, Model model) {
        User user = (User) model.getAttribute("user");
        Workflow workflow = workflowService.initWorkflow(user, id, name);
        return "redirect:/user/wizard/wizendWorkflow/" + workflow.getId();
    }

    @GetMapping(value = "/wiz4/{id}")
    public String wiz4(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id,
                       @RequestParam(value = "workflowId", required = false) Long workflowId,
                       Model model) {
        User user = (User) model.getAttribute("user");
        SignBook signBook = signBookService.getById(id);
        if(signBook.getCreateBy().getEppn().equals(userEppn)) {
            model.addAttribute("signBook", signBook);
            if (workflowId != null) {
                signBookService.initSignBook(id, workflowId, user);
                return "user/wizard/wizend";
            }
            model.addAttribute("signTypes", SignType.values());
        }
        return "user/wizard/wiz4";
    }

    @PostMapping(value = "/wizX/{id}", produces = "text/html")
    public String wizX(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                       @RequestParam(name="addNew", required = false) Boolean addNew,
                       @RequestParam(name="end", required = false) Boolean end,
                       @RequestBody JsonWorkflowStep step,
                       Model model) {
        SignBook signBook = signBookService.getById(id);
        if(signBook.getCreateBy().getEppn().equals(userEppn)) {
            if(step.getRecipientsEmails() != null && step.getRecipientsEmails().size() > 0) {
                String[] recipientsEmailsArray = new String[step.getRecipientsEmails().size()];
                recipientsEmailsArray = step.getRecipientsEmails().toArray(recipientsEmailsArray);
                liveWorkflowStepService.addNewStepToSignBook(SignType.valueOf(step.getSignType()), step.getAllSignToComplete(), recipientsEmailsArray, id);
                if (addNew != null) {
                    model.addAttribute("signTypes", SignType.values());
                }
            } else {
                end = true;
            }
            model.addAttribute("signBook", signBook);
            if(end != null && end) {
                if (signBookService.startLiveWorkflow(signBook, userEppn, authUserEppn)) {
                    return "user/wizard/wiz5";
                } else {
                    return "user/wizard/wizend/";
                }
            }
        }
        return "user/wizard/wiz4";
    }

//    @GetMapping(value = "/wiz5/{id}")
//    public String saveForm(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id, Model model) {
//        SignBook signBook = signBookService.getById(id);
//    }

    @PostMapping(value = "/wiz5/{id}")
    public String saveWorkflow(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id, @RequestParam(name="name") String name, Model model, RedirectAttributes redirectAttributes) {
        User user = (User) model.getAttribute("user");
        SignBook signBook = signBookService.getById(id);
        model.addAttribute("signBook", signBook);
        try {
            signBookService.saveWorkflow(id, name, name, user);
        } catch (EsupSignatureException e) {
            eventService.publishEvent(new JsonMessage("error", "Un circuit de signature porte déjà ce nom"), "user", eventService.getClientIdByEppn(userEppn));
            return "user/wizard/wiz5";
        }
        return "user/wizard/wizend";
    }

    @GetMapping(value = "/wizendWorkflow/{id}")
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
    public String wizEnd(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id, Model model) throws EsupSignatureException {
        SignBook signBook = signBookService.getById(id);
        if(signBook.getCreateBy().getEppn().equals(userEppn)) {
            model.addAttribute("signBook", signBook);
            return "user/wizard/wizend";
        } else {
            throw new EsupSignatureException("not authorized");
        }
    }

    @GetMapping(value = "/wizredirect/{id}")
    public String wizRedirect(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) throws EsupSignatureException {
        SignBook signBook = signBookService.getById(id);
        if(signBook.getCreateBy().getEppn().equals(userEppn)) {
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
            workflowService.delete(workflow);
        }
        return "redirect:/user/";
    }



}
