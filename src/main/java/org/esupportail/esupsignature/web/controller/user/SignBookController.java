package org.esupportail.esupsignature.web.controller.user;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.LogRepository;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.repository.WorkflowRepository;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.file.FileService;
import org.esupportail.esupsignature.web.controller.ws.json.JsonMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RequestMapping("/user/signbooks")
@Controller
@Transactional
public class SignBookController {

    private static final Logger logger = LoggerFactory.getLogger(SignBookController.class);

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private SignBookRepository signBookRepository;

    @Resource
    private WorkflowService workflowService;

    @Resource
    private WorkflowRepository workflowRepository;

    @Resource
    private SignBookService signBookService;

    @Resource
    private LogRepository logRepository;

    @Resource
    private FileService fileService;

    @Resource
    private LiveWorkflowService liveWorkflowService;

    @PreAuthorize("@signBookService.preAuthorizeView(#id, #user)")
    @GetMapping(value = "/{id}")
    public String show(@ModelAttribute("user") User user, @PathVariable("id") Long id) {
        SignBook signBook = signBookRepository.findById(id).get();
        return "redirect:/user/signrequests/" + signBook.getSignRequests().get(0).getId();
    }

    @PreAuthorize("@signBookService.preAuthorizeManage(#id, #authUser)")
    @DeleteMapping(value = "/{id}", produces = "text/html")
    public String delete(@ModelAttribute("authUser") User authUser, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        SignBook signBook = signBookRepository.findById(id).get();
        if(signBookService.delete(signBook)) {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Suppression effectuée"));
        } else {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Suppression interdite"));
        }
        return "redirect:/user/signrequests";
    }

    @PreAuthorize("@signBookService.preAuthorizeManage(#id, #authUser)")
    @GetMapping(value = "/{id}", params = "form")
    public String updateForm(@ModelAttribute("authUser") User authUser, @PathVariable("id") Long id, Model model) {
        SignBook signBook = signBookRepository.findById(id).get();
        List<Log> logs = new ArrayList<>();
        for (SignRequest signRequest : signBook.getSignRequests()) {
            logs.addAll(logRepository.findBySignRequestId(signRequest.getId()));
        }
        model.addAttribute("logs", logs);
        List<LiveWorkflowStep> allSteps = new ArrayList<>(signBook.getLiveWorkflow().getWorkflowSteps());
        if (allSteps.size() > 0) {
            allSteps.remove(0);
        }
        model.addAttribute("allSteps", allSteps);
        model.addAttribute("signBook", signBook);
        model.addAttribute("signTypes", SignType.values());
        model.addAttribute("workflows", workflowService.getWorkflowsByUser(authUser, authUser));
        return "user/signrequests/update-signbook";
    }

//    @PreAuthorize("@signBookService.preAuthorizeManage(#id, #authUser)")
//    @GetMapping(value = "/update-step/{id}/{step}")
//    public String changeStepSignType(@ModelAttribute("user") User authUser, @PathVariable("id") Long id,
//                                     @PathVariable("step") Integer step,
//                                     @RequestParam(name="name", required = false) String name,
//                                     @RequestParam(name="signType") SignType signType,
//                                     @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete) {
//        SignBook signBook = signBookRepository.findById(id).get();
//        if(user.equals(signBook.getCreateBy()) && signBook.getLiveWorkflow().getCurrentStepNumber() <= step + 1) {
//            if(allSignToComplete == null) {
//                allSignToComplete = false;
//            }
//            signBookService.changeSignType(signBook, step, signType);
//            signBookService.toggleNeedAllSign(signBook, step, allSignToComplete);
//            return "redirect:/user/signrequests/" + id + "/?form";
//        }
//        return "redirect:/user/signbooks/";
//    }

    @PreAuthorize("@signBookService.preAuthorizeManage(#id, #authUser)")
    @PostMapping(value = "/add-live-step/{id}")
    public String addStep(@ModelAttribute("authUser") User authUser, @PathVariable("id") Long id,
                          @RequestParam("recipientsEmails") String[] recipientsEmails,
                          @RequestParam("stepNumber") int stepNumber,
                          @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete,
                          @RequestParam("signType") String signType, RedirectAttributes redirectAttributes) {
        SignBook signBook = signBookRepository.findById(id).get();
        LiveWorkflowStep liveWorkflowStep = null;
        int currentSetNumber = signBook.getLiveWorkflow().getCurrentStepNumber();
        if(stepNumber + 1 >= currentSetNumber) {
            try {
                liveWorkflowStep = liveWorkflowService.createWorkflowStep("", "signBook", signBook.getId(), allSignToComplete, SignType.valueOf(signType), recipientsEmails);
            } catch (EsupSignatureUserException e) {
                logger.error("error on add step", e);
                redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Erreur lors de l'ajout des participants"));
            }
            if (stepNumber == -1) {
                signBook.getLiveWorkflow().getWorkflowSteps().add(liveWorkflowStep);
            } else {
                signBook.getLiveWorkflow().getWorkflowSteps().add(stepNumber, liveWorkflowStep);
            }
            signBook.getLiveWorkflow().setCurrentStep(signBook.getLiveWorkflow().getWorkflowSteps().get(currentSetNumber - 1));
            signBookService.pendingSignBook(signBook, authUser);
            redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Étape ajoutée"));
        } else {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "L'étape ne peut pas être ajoutée"));
        }
        return "redirect:/user/signbooks/" + id + "/?form";
    }

    @PreAuthorize("@signBookService.preAuthorizeManage(#id, #authUser)")
    @DeleteMapping(value = "/remove-live-step/{id}/{step}")
    public String removeStep(@ModelAttribute("authUser") User authUser, @PathVariable("id") Long id, @PathVariable("step") Integer step, RedirectAttributes redirectAttributes) {
        SignBook signBook = signBookRepository.findById(id).get();
        int currentStepNumber = signBook.getLiveWorkflow().getCurrentStepNumber();
        if(currentStepNumber <= step) {
            signBookService.removeStep(signBook, step);
            redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "L'étape a été supprimés"));
        } else {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("warn", "L'étape ne peut pas être supprimée"));
        }
        return "redirect:/user/signbooks/" + id + "/?form";
    }

    @PreAuthorize("@signBookService.preAuthorizeManage(#id, #authUser)")
    @PostMapping(value = "/add-workflow/{id}")
    public String addWorkflow(@ModelAttribute("authUser") User authUser, @PathVariable("id") Long id,
                          @RequestParam(value = "workflowSignBookId") Long workflowSignBookId) {
        SignBook signBook = signBookRepository.findById(id).get();
        Workflow workflow = workflowRepository.findById(workflowSignBookId).get();
        signBookService.importWorkflow(signBook, workflow);
        signBookService.nextWorkFlowStep(signBook);
        signBookService.pendingSignBook(signBook, authUser);
        return "redirect:/user/signrequests/" + signBook.getSignRequests().get(0).getId() + "/?form";
    }

    @PreAuthorize("@signBookService.preAuthorizeManage(#id, #authUser)")
    @PostMapping(value = "/add-docs/{id}")
    public String addDocumentToNewSignRequest(@ModelAttribute("authUser") User authUser, @PathVariable("id") Long id,
                                              @RequestParam("multipartFiles") MultipartFile[] multipartFiles) throws EsupSignatureIOException {
        logger.info("start add documents");
        SignBook signBook = signBookRepository.findById(id).get();
        for (MultipartFile multipartFile : multipartFiles) {
            SignRequest signRequest = signRequestService.createSignRequest(signBook.getName() + "_" + multipartFile.getOriginalFilename(), authUser);
            signRequestService.addDocsToSignRequest(signRequest, multipartFile);
            signBookService.addSignRequest(signBook, signRequest);
            LiveWorkflowStep liveWorkflowStep = signBook.getLiveWorkflow().getCurrentStep();
            signRequestService.pendingSignRequest(signRequest, liveWorkflowStep.getSignType(), liveWorkflowStep.getAllSignToComplete());
        }
        return "redirect:/user/signrequests/" + signBook.getSignRequests().get(0).getId() + "/?form";
    }

    @PreAuthorize("@signBookService.preAuthorizeManage(#id, #authUser)")
    @DeleteMapping(value = "/remove-live-step-recipent/{id}/{step}")
    public String removeStepRecipient(@ModelAttribute("authUser") User authUser, @PathVariable("id") Long id,
                                 @PathVariable("step") Integer step,
                                 @RequestParam(value = "recipientId") Long recipientId, RedirectAttributes redirectAttributes) {
        SignBook signBook = signBookRepository.findById(id).get();
        signBookService.removeStepRecipient(signBook, step, recipientId);
        redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Le destinataire a été supprimé"));
        return "redirect:/user/signrequests/" + signBook.getSignRequests().get(0).getId() + "/?form";
    }

    @PreAuthorize("@signBookService.preAuthorizeManage(#id, #authUser)")
    @GetMapping(value = "/pending/{id}")
    public String pending(@ModelAttribute("authUser") User authUser, @PathVariable("id") Long id) {
        SignBook signBook = signBookRepository.findById(id).get();
        signBookService.pendingSignBook(signBook, authUser);
        return "redirect:/user/signrequests/" + signBook.getSignRequests().get(0).getId();
    }

    @PreAuthorize("@signBookService.preAuthorizeManage(#id, #authUser)")
    @ResponseBody
    @PostMapping(value = "/add-docs-in-sign-book-group/{workflowName}/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object addDocumentInSignBookGroup(@ModelAttribute("authUser") User authUser,
                                             @PathVariable("name") String name,
                                             @PathVariable("workflowName") String workflowName,
                                             @RequestParam("multipartFiles") MultipartFile[] multipartFiles, HttpServletRequest httpServletRequest) throws EsupSignatureException, EsupSignatureIOException {
        logger.info("start add documents in " + name);
        SignBook signBook = signBookService.createSignBook(name, "", authUser, false);
        SignRequest signRequest = signRequestService.createSignRequest(name, authUser);
        signRequestService.addDocsToSignRequest(signRequest, multipartFiles);
        signBookService.addSignRequest(signBook, signRequest);
        logger.info("signRequest : " + signRequest.getId() + " added to signBook" + signBook.getName() + " - " + signBook.getId());
        String[] ok = {"ok"};
        return ok;
    }

    @ResponseBody
    @PostMapping(value = "/add-docs-in-sign-book-unique/{workflowName}/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object addDocumentToNewSignRequestUnique(@ModelAttribute("user") User user,
                                                    @PathVariable("name") String name,
                                                    @PathVariable("workflowName") String workflowName,
                                              @RequestParam("multipartFiles") MultipartFile[] multipartFiles) throws EsupSignatureIOException {
        logger.info("start add documents in " + name);
        SignBook signBook = signBookService.createSignBook(name, "", user, false);
        for (MultipartFile multipartFile : multipartFiles) {
            SignRequest signRequest = signRequestService.createSignRequest(fileService.getNameOnly(multipartFile.getOriginalFilename()), user);
            signRequestService.addDocsToSignRequest(signRequest, multipartFile);
            signBookService.addSignRequest(signBook, signRequest);
        }
        String[] ok = {"ok"};
        return ok;
    }

}
