package org.esupportail.esupsignature.web.controller.user;

import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.repository.WorkflowRepository;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.esupportail.esupsignature.web.controller.ws.json.JsonMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
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
@Transactional
public class WizardController {

    private static final Logger logger = LoggerFactory.getLogger(WizardController.class);

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
    public String wiz2(@ModelAttribute("user") User user, @RequestParam("name") String name, @RequestParam(value = "workflowId", required = false) Long workflowId, Model model) {
        logger.info(user.getEppn() + " init new signBook : " + name);
        logger.debug("Choix des fichiers");
        model.addAttribute("name", name);
        if (workflowId != null) {
            Workflow workflow = workflowRepository.findById(workflowId).get();
            model.addAttribute("workflow", workflow);
        }
        return "user/wizard/wiz2";
    }

    @PostMapping(value = "/wiz3", produces = "text/html")
    public ModelAndView wiz3(@ModelAttribute("user") User user, @ModelAttribute("authUser") User authUser, @RequestParam(value = "workflowId", required = false) Long workflowId, Model model) {
        logger.debug("Choix d'un workflow");
        List<SignBook> signBooks = signBookRepository.findByCreateBy(user);
        SignBook signBook = signBooks.stream().sorted(Comparator.comparing(SignBook::getCreateDate).reversed()).collect(Collectors.toList()).get(0);
        model.addAttribute("signBook", signBook);
        model.addAttribute("workflows", workflowService.getWorkflowsByUser(user, authUser));
        if (workflowId != null) {
            Workflow workflow = workflowRepository.findById(workflowId).get();
            ModelAndView modelAndView = new ModelAndView("redirect:/user/wizard/wiz4/" + signBook.getId());
            modelAndView.addObject("workflowId", workflow.getId());
            return modelAndView;
        }
        return new ModelAndView("user/wizard/wiz3");
    }

    //TODO preauthorize
    @GetMapping(value = "/wiz4/{id}")
    public String wiz4(@ModelAttribute("user") User user, @PathVariable("id") Long id,
                       @RequestParam(value = "workflowId", required = false) Long workflowId,
                       Model model) {
        SignBook signBook = signBookRepository.findById(id).get();
        if(signBook.getCreateBy().equals(user)) {
            model.addAttribute("signBook", signBook);
            if (workflowId != null) {
                Workflow workflow = workflowRepository.findById(workflowId).get();
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
    public String wizX(@ModelAttribute("user") User user, @PathVariable("id") Long id,
                       @RequestParam(name="signType", required = false) SignType signType,
                       @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete,
                       @RequestParam(value = "recipientsEmail", required = false) String[] recipientsEmail,
                       @RequestParam(name="addNew", required = false) Boolean addNew,
                       @RequestParam(name="end", required = false) Boolean end,
                       Model model) throws EsupSignatureUserException, InterruptedException {
        SignBook signBook = signBookRepository.findById(id).get();
        if(signBook.getCreateBy().equals(user)) {
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
    public String saveForm(@ModelAttribute("user") User user, @PathVariable("id") Long id, Model model) {

        SignBook signBook = signBookRepository.findById(id).get();
        if(signBook.getCreateBy().equals(user)) {
            model.addAttribute("signBook", signBook);
        }
        return "user/wizard/wiz5";
    }

    @PostMapping(value = "/wiz5/{id}")
    public String saveWorkflow(@ModelAttribute("user") User user, @PathVariable("id") Long id, @RequestParam(name="name") String name, Model model, RedirectAttributes redirectAttributes) {
        SignBook signBook = signBookRepository.findById(id).get();
        try {
            signBookService.saveWorkflow(name, name, user, signBook);
        } catch (EsupSignatureException e) {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Un circuit de signature porte déjà ce nom"));
            return "redirect:/user/wizard/wiz5/" + signBook.getId();
        }
        return "redirect:/user/wizard/wizend/" + signBook.getId();
    }

    @GetMapping(value = "/wizend/{id}")
    public String wizEnd(@ModelAttribute("user") User user, @PathVariable("id") Long id, Model model) throws EsupSignatureException {
        SignBook signBook = signBookRepository.findById(id).get();
        if(signBook.getCreateBy().equals(user)) {
            model.addAttribute("signBook", signBook);
            return "user/wizard/wizend";
        } else {
            throw new EsupSignatureException("not authorized");
        }
    }

    @DeleteMapping(value = "/delete-workflow/{id}", produces = "text/html")
    public String delete(@ModelAttribute("user") User user, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Workflow workflow = workflowRepository.findById(id).get();
		if (!workflow.getCreateBy().equals(user.getEppn())) {
			redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Non autorisé"));
		} else {
            workflowRepository.delete(workflow);
        }
        return "redirect:/user/";
    }

}
