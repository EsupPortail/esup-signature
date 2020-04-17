package org.esupportail.esupsignature.web.controller.user;

import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.repository.WorkflowRepository;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@RequestMapping("/user/wizard")
@Controller
@Transactional
public class WizardController {

    private static final Logger logger = LoggerFactory.getLogger(WizardController.class);

    @ModelAttribute("userMenu")
    public String getActiveMenu() {
        return "active";
    }

    @ModelAttribute(value = "user", binding = false)
    public User getUser() {
        return userService.getCurrentUser();
    }

    @Resource
    private UserService userService;

    @Resource
    private WorkflowRepository workflowRepository;

    @Resource
    private WorkflowService workflowService;

    @Resource
    private SignBookRepository signBookRepository;

    @Resource
    private SignBookService signBookService;

    @GetMapping(value = "/wiz1")
    public String wiz1(@RequestParam(value = "workflowId", required = false) Long workflowId, Model model) {
        logger.info("Saisie du nom");
        if (workflowId != null) {
            Workflow workflow = workflowRepository.findById(workflowId).get();
            model.addAttribute("workflow", workflow);
        }
        return "user/wizard/wiz1";
    }

    @PostMapping(value = "/wiz2", produces = "text/html")
    public String wiz2(@RequestParam("name") String name, @RequestParam(value = "workflowId", required = false) Long workflowId, Model model, RedirectAttributes redirectAttributes) {
        logger.info("Choix des fichiers");
        if(signBookRepository.countByName(name) > 0) {
            redirectAttributes.addFlashAttribute("messageError", "Un circuit portant ce nom existe déjà");
            return "redirect:/user/wizard/wiz1";
        }
        logger.info("init new signBook : " + name);
        model.addAttribute("name", name);
        if (workflowId != null) {
            Workflow workflow = workflowRepository.findById(workflowId).get();
            model.addAttribute("workflow", workflow);
        }
        return "user/wizard/wiz2";
    }

    @PostMapping(value = "/wiz3", produces = "text/html")
    public ModelAndView wiz3(@ModelAttribute User user, @RequestParam("name") String name, @RequestParam(value = "workflowId", required = false) Long workflowId, Model model) throws EsupSignatureException, IOException, EsupSignatureIOException {
        logger.info("Choix d'un workflow");
        SignBook signBook = signBookService.getSignBook(name, user);
        model.addAttribute("signBook", signBook);
        model.addAttribute("workflows", workflowService.getWorkflowsForUser(user));
        if (workflowId != null) {
            Workflow workflow = workflowRepository.findById(workflowId).get();
            ModelAndView modelAndView = new ModelAndView("redirect:/user/wizard/wiz4/" + signBook.getId());
            modelAndView.addObject("name", name);
            modelAndView.addObject("workflowId", workflow.getId());
            return modelAndView;
        }
        return new ModelAndView("user/wizard/wiz3");
    }

    //TODO preauthorize
    @GetMapping(value = "/wiz4/{id}")
    public String wiz4(@ModelAttribute User user, @PathVariable("id") Long id,
                       @RequestParam(value = "workflowId", required = false) Long workflowId,
                       @RequestParam(value = "selfSign", required = false) Boolean selfSign,
                       Model model) {
        //User user = userService.getCurrentUser();
        SignBook signBook = signBookRepository.findById(id).get();
        if(signBook.getCreateBy().equals(user.getEppn())) {
            model.addAttribute("signBook", signBook);
            if (workflowId != null) {
                Workflow workflow = workflowRepository.findById(workflowId).get();
                signBookService.importWorkflow(signBook, workflow);
                signBookService.nextWorkFlowStep(signBook);
                signBookService.pendingSignBook(signBook, user);
                return "redirect:/user/wizard/wizend/" + signBook.getId();
            } else if(selfSign != null) {
                Workflow workflow = workflowRepository.findByName("Ma signature").get(0);
                signBookService.importWorkflow(signBook, workflow);
                signBookService.nextWorkFlowStep(signBook);
                signBookService.pendingSignBook(signBook, user);
                return "redirect:/user/wizard/wizend/" + signBook.getId();
            }
            model.addAttribute("workflowStepForm", true);
            model.addAttribute("signTypes", SignType.values());
        }
        return "user/wizard/wiz4";
    }

    @PostMapping(value = "/wizX/{id}", produces = "text/html")
    public String wizX(@ModelAttribute User user, @PathVariable("id") Long id,
                       @RequestParam(name="signType", required = false) SignType signType,
                       @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete,
                       @RequestParam(value = "recipientsEmail", required = false) String[] recipientsEmail,
                       @RequestParam(name="addNew", required = false) Boolean addNew,
                       @RequestParam(name="end", required = false) Boolean end,
                       Model model) throws EsupSignatureUserException {
        //User user = userService.getCurrentUser();
        SignBook signBook = signBookRepository.findById(id).get();
        if(signBook.getCreateBy().equals(user.getEppn())) {
            if(recipientsEmail != null && recipientsEmail.length > 0) {
                logger.info("add new workflow step to signBook " + signBook.getName() + " - " + signBook.getId());
                WorkflowStep workflowStep = workflowService.createWorkflowStep("", "signBook", signBook.getId(), allSignToComplete, signType, recipientsEmail);
                signBook.getWorkflowSteps().add(workflowStep);
                if (addNew != null) {
                    model.addAttribute("workflowStepForm", true);
                    model.addAttribute("signTypes", SignType.values());
                }
            } else {
                end = true;
            }
            if(end != null) {
                if(signBook.getWorkflowSteps().size() >  0) {
                    signBookService.nextWorkFlowStep(signBook);
                    signBookService.pendingSignBook(signBook, user);
                    return "redirect:/user/wizard/wiz5/" + signBook.getId();
                }else {
                    return "redirect:/user/wizard/wizend/" + signBook.getId();
                }
            }
            model.addAttribute("signBook", signBook);
        }
        return "user/wizard/wiz4";
    }

    @GetMapping(value = "/wiz5/{id}")
    public String saveForm(@ModelAttribute User user, @PathVariable("id") Long id, Model model) {
        //User user = userService.getCurrentUser();
        SignBook signBook = signBookRepository.findById(id).get();
        if(signBook.getCreateBy().equals(user.getEppn())) {
            model.addAttribute("signBook", signBook);
        }
        return "user/wizard/wiz5";
    }

    @PostMapping(value = "/wiz5/{id}")
    public String saveWorkflow(@ModelAttribute User user, @PathVariable("id") Long id, @RequestParam(name="name") String name, Model model) {
        //User user = userService.getCurrentUser();
        SignBook signBook = signBookRepository.findById(id).get();
        try {
            signBookService.saveWorkflow(name, user, signBook);
        } catch (EsupSignatureException e) {
            model.addAttribute("signBook", signBook);
            model.addAttribute("messageError", "Un circuit de signature porte déjà ce nom");
            return "redirect:/user/wizard/wiz5/" + signBook.getId();
        }
        return "redirect:/user/wizard/wizend/" + signBook.getId();
    }

    @GetMapping(value = "/wizend/{id}")
    public String wizEnd(@ModelAttribute User user, @PathVariable("id") Long id, Model model) throws EsupSignatureException {
        //User user = userService.getCurrentUser();
        SignBook signBook = signBookRepository.findById(id).get();
        if(signBook.getCreateBy().equals(user.getEppn())) {
            model.addAttribute("signBook", signBook);
            return "user/wizard/wizend";
        } else {
            throw new EsupSignatureException("not authorized");
        }
    }

}
