package org.esupportail.esupsignature.web.controller.user;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.repository.WorkflowRepository;
import org.esupportail.esupsignature.repository.WorkflowStepRepository;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RequestMapping("/user/signbooks/wizard")
@Controller
@Transactional
@Scope(value = "session")
public class SignBookWizardController {

    private static final Logger logger = LoggerFactory.getLogger(SignBookWizardController.class);

    @ModelAttribute("userMenu")
    public String getActiveMenu() {
        return "active";
    }

    @ModelAttribute("user")
    public User getUser() {
        return userService.getUserFromAuthentication();
    }

    @Resource
    private UserService userService;

    @Resource
    private SignRequestRepository signRequestRepository;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private WorkflowRepository workflowRepository;

    @Resource
    private WorkflowService workflowService;

    @Resource
    private WorkflowStepRepository workflowStepRepository;

    @Resource
    private SignBookRepository signBookRepository;

    @Resource
    private SignBookService signBookService;

    @GetMapping(value = "/wiz1")
    public String wiz1() {
        //User user = userService.getUserFromAuthentication();
        return "user/signbooks/wizard/wiz1";
    }

    @PostMapping(value = "/wiz2", produces = "text/html")
    public String wiz2(@RequestParam("name") String name, Model model, RedirectAttributes redirectAttributes) {
        if(signBookRepository.countByName(name) > 0) {
            redirectAttributes.addFlashAttribute("messageError", "Un parapheur portant ce nom existe déjà");
            return "redirect:/user/signbooks/wizard/wiz1";
        }
        model.addAttribute("name", name);
        return "user/signbooks/wizard/wiz2";
    }

    @PostMapping(value = "/wiz3", produces = "text/html")
    public String wiz3(@RequestParam("name") String name, Model model) throws EsupSignatureException, IOException, EsupSignatureIOException {
        User user = userService.getUserFromAuthentication();
        SignBook signBook = signBookService.getSignBook(name, user);
        for (SignRequest signRequest : signBook.getSignRequests()) {
            if(signRequest.getOriginalDocuments().size() == 1 && signRequest.getOriginalDocuments().get(0).getContentType().equals("application/pdf")) {
                signRequestService.scanSignatureFields(signRequest);
            }
        }
        model.addAttribute("signBook", signBook);
        List<Workflow> workflows = new ArrayList<>();
        for(Workflow workflow : workflowRepository.findByCreateBy(user.getEppn())) {
            if(!workflows.contains(workflow)) {
                workflows.add(workflow);
            }
        }
        model.addAttribute("workflows", workflows);
        return "user/signbooks/wizard/wiz3";
    }



    @RequestMapping(value = "/wiz4/{id}", produces = "text/html")
    public String wiz4(@PathVariable("id") Long id, @RequestParam(value = "workflowId", required = false) Long workflowId,  Model model) throws IOException {
        User user = userService.getUserFromAuthentication();
        SignBook signBook = signBookRepository.findById(id).get();
        //TODO check right
        model.addAttribute("signBook", signBook);
        if (workflowId != null) {
            Workflow workflow = workflowRepository.findById(workflowId).get();
            signBookService.importWorkflow(signBook, workflow);
            return "redirect:/user/signbooks/wizard/wizend/" + signBook.getId();
        }
        //model.addAttribute("allSignBooks", signBookRepository.findByRecipientEmailsAndSignBookType(Arrays.asList(user.getEmail()), SignBookType.user));
        model.addAttribute("workflowStepForm", true);
        model.addAttribute("signTypes", SignType.values());
        return "user/signbooks/wizard/wiz4";
    }

    @PostMapping(value = "/wizX/{id}", produces = "text/html")
    public String wizX(@PathVariable("id") Long id,
                       @RequestParam(name="signType", required = false) SignType signType,
                       @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete,
                       @RequestParam(value = "recipientsEmail", required = false) String[] recipientsEmail,
                       @RequestParam(name="addNew", required = false) Boolean addNew,
                       @RequestParam(name="end", required = false) Boolean end,
                       Model model) throws EsupSignatureException {
        User user = userService.getUserFromAuthentication();
        SignBook signBook = signBookRepository.findById(id).get();

        if (user.getEppn().equals(signBook.getCreateBy())) {
            WorkflowStep workflowStep = workflowService.createWorkflowStep(Arrays.asList(recipientsEmail), "", allSignToComplete, signType);
            signBook.getWorkflowSteps().add(workflowStep);
            signBookRepository.save(signBook);
        }
        if (addNew != null) {
            model.addAttribute("workflowStepForm", true);
            model.addAttribute("signTypes", SignType.values());
        }
        if(end != null) {
            return "redirect:/user/signbooks/wizard/wiz5/" + signBook.getId();
        }
        model.addAttribute("signBook", signBook);
        return "user/signbooks/wizard/wiz4";
    }

    @GetMapping(value = "/wiz5/{id}")
    public String saveForm(@PathVariable("id") Long id, Model model) {
        SignBook signBook = signBookRepository.findById(id).get();
        model.addAttribute("signBook", signBook);
        return "user/signbooks/wizard/wiz5";
    }

    @PostMapping(value = "/wiz5/{id}")
    public String saveWorkflow(@PathVariable("id") Long id, @RequestParam(name="name") String name, Model model) throws EsupSignatureException {
        User user = userService.getUserFromAuthentication();
        SignBook signBook = signBookRepository.findById(id).get();
        Workflow workflow;
        if(workflowRepository.countByName(name) > 0 ) {
            model.addAttribute("signBook", signBook);
            model.addAttribute("messageError", "Un circuit de signature porte déjà ce nom");
            return "redirect:/user/signbooks/wizard/wiz5/" + signBook.getId();
        } else {
            signBookService.saveWorkflow(name, user, signBook);
        }
        return "redirect:/user/signbooks/wizard/wizend/" + signBook.getId();
    }

    @RequestMapping(value = "/wizend/{id}", produces = "text/html")
    public String wizEnd(@PathVariable("id") Long id, Model model) {
        User user = userService.getUserFromAuthentication();
        SignBook signBook = signBookRepository.findById(id).get();
        model.addAttribute("signBook", signBook);
        boolean mySign = false;
        if(signBookService.getCurrentWorkflowStep(signBook).getRecipients().containsKey(user.getId())) {
            mySign = true;
        }
        signBookService.pendingSignBook(signBook, user);
        model.addAttribute("mySign", mySign);
        return "user/signbooks/wizard/wizend";
    }

}
