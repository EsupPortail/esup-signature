package org.esupportail.esupsignature.web.controller.user;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignBook.SignBookType;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.SignRequestParams.SignType;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.repository.WorkflowStepRepository;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.util.Arrays;

@RequestMapping("/user/signrequests/wizard")
@Controller
@Transactional
@Scope(value = "session")
public class SignRequestWizardController {

    private static final Logger logger = LoggerFactory.getLogger(SignRequestWizardController.class);

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
    private WorkflowStepRepository workflowStepRepository;

    @Resource
    private SignBookRepository signBookRepository;

    @RequestMapping(value = "/wiz1", produces = "text/html")
    public String wiz1() {
        //User user = userService.getUserFromAuthentication();
        return "user/signrequests/wizard/wiz1";
    }

    @PostMapping(value = "/wiz2", produces = "text/html")
    public String wiz2(@Valid SignRequest signRequest, Model model, HttpServletRequest request) {
        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        signRequest = signRequestService.createSignRequest(signRequest, user);
        model.addAttribute("signRequest", signRequest);
        return "user/signrequests/wizard/wiz2";
    }

    @RequestMapping(value = "/wiz3/{id}", produces = "text/html")
    public String wiz3(@PathVariable("id") Long id, Model model) throws IOException {
        User user = userService.getUserFromAuthentication();
        SignRequest signRequest = signRequestRepository.findById(id).get();
        if(signRequestService.checkUserViewRights(user, signRequest)) {
            model.addAttribute("signRequest", signRequest);
            model.addAttribute("signBooks", signBookRepository.findBySignBookType(SignBookType.workflow));
        }
        return "user/signrequests/wizard/wiz3";
    }



    @RequestMapping(value = "/wiz4/{id}", produces = "text/html")
    public String wiz4(@PathVariable("id") Long id, @RequestParam(value = "signBookId", required = false) Long signBookId,  Model model) throws IOException {
        User user = userService.getUserFromAuthentication();
        SignRequest signRequest = signRequestRepository.findById(id).get();
        if(signRequestService.checkUserViewRights(user, signRequest)) {
            model.addAttribute("signRequest", signRequest);
            if (signRequest.getOriginalDocuments().size() == 1) {
                signRequestService.scanSignatureFields(signRequest, PDDocument.load(signRequest.getOriginalDocuments().get(0).getInputStream()));
            }
            signRequestRepository.save(signRequest);
            if (signBookId != null) {
                SignBook signBook = signBookRepository.findById(signBookId).get();
                signRequestService.importWorkflow(signRequest, signBook);
                return "user/signrequests/wizard/wizend";
            }
            signRequestService.setSignBooksLabels(signRequest.getWorkflowSteps());
            model.addAttribute("workflowStep", signRequest.getWorkflowSteps().get(0));
            model.addAttribute("signTypes", SignType.values());
            model.addAttribute("step", 0);
            return "user/signrequests/wizard/wiz4";
        } else {
            return "user/signrequest/error";
        }

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
        SignRequest signRequest = signRequestRepository.findById(id).get();
        if(addNew != null){
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
            signRequestService.setSignBooksLabels(signRequest.getWorkflowSteps());
        }
        signRequestRepository.save(signRequest);
        model.addAttribute("signRequest", signRequest);
        if(signRequest.getWorkflowSteps().size() > step) {
            model.addAttribute("workflowStep", signRequest.getWorkflowSteps().get(step + 1));
        }
        model.addAttribute("signTypes", SignType.values());
        model.addAttribute("step", step + 1);
        return "user/signrequests/wizard/wiz4";
    }

}
