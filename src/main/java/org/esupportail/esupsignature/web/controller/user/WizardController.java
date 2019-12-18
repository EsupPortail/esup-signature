package org.esupportail.esupsignature.web.controller.user;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.SignRequestParams.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.repository.WorkflowRepository;
import org.esupportail.esupsignature.repository.WorkflowStepRepository;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RequestMapping("/user/wizard")
@Controller
@Transactional
@Scope(value = "session")
public class WizardController {

    private static final Logger logger = LoggerFactory.getLogger(WizardController.class);

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
    private WorkflowStepRepository workflowStepRepository;

    @Resource
    private SignBookRepository signBookRepository;

    @Resource
    private SignBookService signBookService;

    @RequestMapping(value = "/wiz1", produces = "text/html")
    public String wiz1() {
        //User user = userService.getUserFromAuthentication();
        return "user/wizard/wiz1";
    }

    @PostMapping(value = "/wiz1", produces = "text/html")
    public String wiz1Post(@RequestParam("title") String title, Model model, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        try {
            SignBook signBook = signBookService.createSignBook(title, SignBook.SignBookType.workflow, user, false);
            return "redirect:/user/wizard/wiz2/" + signBook.getId();
        } catch (EsupSignatureException e) {
            redirectAttributes.addFlashAttribute("messageError", e.getMessage());
        }
        return "redirect:/user/wizard/wiz1";
    }

    @GetMapping(value = "/wiz2/{id}", produces = "text/html")
    public String wiz2(@PathVariable("id") Long id, Model model, HttpServletRequest request) {
        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        SignBook signBook = signBookRepository.findById(id).get();
        model.addAttribute("signBook", signBook);
        return "user/wizard/wiz2";
    }

    @RequestMapping(value = "/wiz3/{id}", produces = "text/html")
    public String wiz3(@PathVariable("id") Long id, Model model) throws IOException {
        User user = userService.getUserFromAuthentication();
        SignBook signBook = signBookRepository.findById(id).get();
        model.addAttribute("signBook", signBook);
        List<Workflow> workflows = new ArrayList<>();
        workflows.add(workflowRepository.findByName("Ma signature").get(0));
        for(Workflow workflow : workflowRepository.findAll()) {
            if(!workflows.contains(workflow)) {
                workflows.add(workflow);
            }
        }
        model.addAttribute("workflows", workflows);
        return "user/wizard/wiz3";
    }



    @RequestMapping(value = "/wiz4/{id}", produces = "text/html")
    public String wiz4(@PathVariable("id") Long id, @RequestParam(value = "workflowId", required = false) Long workflowId,  Model model) throws IOException {
        User user = userService.getUserFromAuthentication();
        SignBook signBook = signBookRepository.findById(id).get();
        model.addAttribute("signBook", signBook);
        if (workflowId != null) {
            Workflow workflow = workflowRepository.findById(workflowId).get();
            for(SignRequest signRequest : signBook.getSignRequests()) {
                signRequestService.importWorkflow(signRequest, workflow);
            }
            return "redirect:/user/wizard/wizend/" + signBook.getId();
        }

        for(SignRequest signRequest : signBook.getSignRequests()) {
            if (signRequest.getWorkflowSteps() == null || signRequest.getWorkflowSteps().size() == 0) {
                WorkflowStep workflowStep = new WorkflowStep();
                workflowStep.setSignRequestParams(signRequestService.getEmptySignRequestParams());
                workflowStepRepository.save(workflowStep);
                signRequest.getWorkflowSteps().add(workflowStep);
            }
        }
        //model.addAttribute("allSignBooks", signBookRepository.findByRecipientEmailsAndSignBookType(Arrays.asList(user.getEmail()), SignBookType.user));
        //model.addAttribute("workflowStep", signBook.getWorkflowSteps().get(0));
        model.addAttribute("signTypes", SignType.values());
        model.addAttribute("step", 0);
        return "user/wizard/wiz4";
    }

    @PostMapping(value = "/wizX/{id}", produces = "text/html")
    public String wizX(@PathVariable("id") Long id,
                       @RequestParam("step") Integer step,
                       @RequestParam(name="name", required = false) String name,
                       @RequestParam(name="signType", required = false) SignType signType,
                       @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete,
                       @RequestParam(value = "signBookNames", required = false) String[] signBookNames,
                       @RequestParam(name="addNew", required = false) Boolean addNew,
                       Model model) {
        User user = userService.getUserFromAuthentication();
        SignBook signBook = signBookRepository.findById(id).get();
        for(SignRequest signRequest : signBook.getSignRequests()) {
            if (addNew != null) {
                WorkflowStep workflowStep = new WorkflowStep();
                workflowStep.setSignRequestParams(signRequestService.getEmptySignRequestParams());
                workflowStepRepository.save(workflowStep);
                signRequest.getWorkflowSteps().add(workflowStep);
            } else {
                if (user.getEppn().equals(signRequest.getCreateBy())) {
                    if (signRequest.getWorkflowSteps().size() < step) {
                        signRequestService.addWorkflowStep(Arrays.asList(signBookNames), name, allSignToComplete, signType, signRequest);
                    } else {
                        if (allSignToComplete == null) {
                            allSignToComplete = false;
                        }
                        signRequestService.changeSignType(signRequest, step, name, signType);
                        signRequestService.toggleNeedAllSign(signRequest, step, allSignToComplete);
                        WorkflowStep workflowStep = signRequest.getWorkflowSteps().get(step);
                        if (signBookNames != null && signBookNames.length > 0) {
                            signRequestService.addRecipientsToWorkflowStep(signRequest, Arrays.asList(signBookNames), workflowStep, user);
                        }
                    }
                }
                signRequestService.setWorkflowsLabels(signRequest.getWorkflowSteps());
            }
            signRequestRepository.save(signRequest);
        }
        model.addAttribute("signBook", signBook);
        if(signBook.getSignRequests().get(0).getWorkflowSteps().size() > step + 1) {
            model.addAttribute("workflowStep", signBook.getSignRequests().get(0).getWorkflowSteps().get(step + 1));
            model.addAttribute("signTypes", SignType.values());
            model.addAttribute("step", step + 1);
        } else {
            model.addAttribute("step", step);
        }
        return "user/wizard/wiz4";
    }


    @RequestMapping(value = "/wizend/{id}", produces = "text/html")
    public String wizEnd(@PathVariable("id") Long id, Model model) {
        User user = userService.getUserFromAuthentication();
        SignBook signBook = signBookRepository.findById(id).get();
        boolean mySign = false;
        for(SignRequest signRequest : signBook.getSignRequests())
        if(signRequestService.checkUserViewRights(user, signRequest)) {
            if(signRequest.getCurrentWorkflowStep().getRecipients().containsKey(user.getId())) {
                mySign = true;
            }
            signRequestService.pendingSignRequest(signRequest, user);
        }
        model.addAttribute("mySign", mySign);
        return "user/wizard/wizend";
    }

}
