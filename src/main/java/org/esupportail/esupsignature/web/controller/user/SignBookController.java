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
    private UserService userService;

    @Resource
    private SignRequestRepository signRequestRepository;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private SignBookRepository signBookRepository;

    @Resource
    private WorkflowRepository workflowRepository;

    @Resource
    private WorkflowService workflowService;

    @Resource
    private SignBookService signBookService;

    @Resource
    private FileService fileService;

    @Resource
    private LogRepository logRepository;

    @Resource
    private LiveWorkflowService liveWorkflowService;

    @PreAuthorize("@signBookService.preAuthorizeView(#id, #user)")
    @GetMapping(value = "/{id}")
    public String show(@ModelAttribute("user") User user, @PathVariable("id") Long id) {
        SignBook signBook = signBookRepository.findById(id).get();
        return "redirect:/user/signrequests/" + signBook.getSignRequests().get(0).getId();
    }

    @PreAuthorize("@signBookService.preAuthorizeManage(#id, #user)")
    @GetMapping(value = "/{id}", params = "form", produces = "text/html")
    public String updateForm(@ModelAttribute("user") User user, @PathVariable("id") Long id, Model model) {
        User authUser = userService.getUserFromAuthentication();
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
        model.addAttribute("workflows", workflowService.getWorkflowsByUser(user, authUser));
        return "user/signbooks/update";
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

    @PreAuthorize("@signBookService.preAuthorizeManage(#id, #user)")
    @GetMapping(value = "/get-last-file/{id}")
    public void getLastFile(@ModelAttribute("user") User user, @PathVariable("id") Long id, HttpServletResponse response) {
        SignRequest signRequest = signRequestRepository.findById(id).get();
        List<Document> documents = signRequestService.getToSignDocuments(signRequest);
        try {
            if (documents.size() > 1) {
                response.sendRedirect("/user/signbooks/" + id);
            } else {
                Document document = documents.get(0);
                response.setHeader("Content-disposition", "inline; filename=" + URLEncoder.encode(document.getFileName(), StandardCharsets.UTF_8.toString()));
                response.setContentType(document.getContentType());
                IOUtils.copy(document.getBigFile().getBinaryFile().getBinaryStream(), response.getOutputStream());
            }
        } catch (Exception e) {
            logger.error("get file error", e);
        }
    }
//
//    @GetMapping(value = "/update-step/{id}/{step}")
//    public String changeStepSignType(@PathVariable("id") Long id, @PathVariable("step") Integer step, @RequestParam(name="signType") SignType signType) {
//
//        SignBook signBook = signBookRepository.findById(id).get();
//        if(user.getEppn().equals(signBook.getCreateBy()) && signBook.getCurrentWorkflowStepNumber() <= step + 1) {
//            signBookService.changeSignType(signBook, step, signType);
//            return "redirect:/user/signbooks/" + id + "/?form";
//        }
//        return "redirect:/user/signbooks/";
//    }

    @PreAuthorize("@signBookService.preAuthorizeManage(#id, #user)")
    @GetMapping(value = "/update-step/{id}/{step}")
    public String changeStepSignType(@ModelAttribute("user") User user, @PathVariable("id") Long id,
                                     @PathVariable("step") Integer step,
                                     @RequestParam(name="name", required = false) String name,
                                     @RequestParam(name="signType") SignType signType,
                                     @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete) {
        SignBook signBook = signBookRepository.findById(id).get();
        if(user.equals(signBook.getCreateBy()) && signBook.getLiveWorkflow().getCurrentStepNumber() <= step + 1) {
            if(allSignToComplete == null) {
                allSignToComplete = false;
            }
            signBookService.changeSignType(signBook, step, signType);
            signBookService.toggleNeedAllSign(signBook, step, allSignToComplete);
            return "redirect:/user/signbooks/" + id + "/?form";
        }
        return "redirect:/user/signbooks/";
    }

    @PreAuthorize("@signBookService.preAuthorizeManage(#id, #user)")
    @PostMapping(value = "/add-step/{id}")
    public String addStep(@ModelAttribute("user") User user, @PathVariable("id") Long id,
                          @RequestParam("recipientsEmails") String[] recipientsEmails,
                          @RequestParam("stepNumber") int stepNumber,
                          @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete,
                          @RequestParam("signType") String signType, RedirectAttributes redirectAttributes) {
        SignBook signBook = signBookRepository.findById(id).get();
        LiveWorkflowStep liveWorkflowStep = null;
        try {
            liveWorkflowStep = liveWorkflowService.createWorkflowStep("", "signBook", signBook.getId(), allSignToComplete, SignType.valueOf(signType), recipientsEmails);
        } catch (EsupSignatureUserException e) {
            logger.error("error on add step", e);
            redirectAttributes.addFlashAttribute("message", new JsonMessage("error", "Erreur lors de l'ajout des participants"));
        }
        if (stepNumber == - 1) {
            signBook.getLiveWorkflow().getWorkflowSteps().add(liveWorkflowStep);
        } else {
            signBook.getLiveWorkflow().getWorkflowSteps().add(stepNumber, liveWorkflowStep);
        }
        signBookService.pendingSignBook(signBook, user);
        redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Étape ajoutée"));
        return "redirect:/user/signbooks/" + id + "/?form";
    }

    @PreAuthorize("@signBookService.preAuthorizeManage(#id, #user)")
    @DeleteMapping(value = "/remove-step/{id}/{step}")
    public String removeStep(@ModelAttribute("user") User user, @PathVariable("id") Long id, @PathVariable("step") Integer step) {
        SignBook signBook = signBookRepository.findById(id).get();
        if(signBook.getLiveWorkflow().getCurrentStepNumber() <= step + 1) {
            signBookService.removeStep(signBook, step);
        }
        return "redirect:/user/signbooks/" + id + "/?form";
    }

    @PreAuthorize("@signBookService.preAuthorizeManage(#id, #user)")
    @PostMapping(value = "/add-workflow/{id}")
    public String addWorkflow(@ModelAttribute("user") User user, @PathVariable("id") Long id,
                          @RequestParam(value = "workflowSignBookId") Long workflowSignBookId) throws InterruptedException {
        SignBook signBook = signBookRepository.findById(id).get();
        if (signBookService.checkUserViewRights(user, signBook)) {
            Workflow workflow = workflowRepository.findById(workflowSignBookId).get();
            signBookService.importWorkflow(signBook, workflow);
            signBookService.nextWorkFlowStep(signBook);
            signBookService.pendingSignBook(signBook, user);
        }
        return "redirect:/user/signbooks/" + id + "/?form";
    }

    @PreAuthorize("@signBookService.preAuthorizeManage(#id, #user)")
    @PostMapping(value = "/add-docs/{id}")
    public String addDocumentToNewSignRequest(@ModelAttribute("user") User user, @PathVariable("id") Long id,
                                              @RequestParam("multipartFiles") MultipartFile[] multipartFiles) throws EsupSignatureIOException {
        logger.info("start add documents");
        SignBook signBook = signBookRepository.findById(id).get();
        for (MultipartFile multipartFile : multipartFiles) {
            SignRequest signRequest = signRequestService.createSignRequest(signBook.getName() + "_" + multipartFile.getOriginalFilename(), user);
            signRequestService.addDocsToSignRequest(signRequest, multipartFile);
            signBookService.addSignRequest(signBook, signRequest);
            LiveWorkflowStep liveWorkflowStep = signBook.getLiveWorkflow().getCurrentStep();
            signRequestService.pendingSignRequest(signRequest, liveWorkflowStep.getSignType(), liveWorkflowStep.getAllSignToComplete());
        }
        return "redirect:/user/signbooks/" + id + "/?form";
    }

    @PreAuthorize("@signBookService.preAuthorizeManage(#id, #user)")
    @DeleteMapping(value = "/remove-step-recipent/{id}/{step}")
    public String removeStepRecipient(@ModelAttribute("user") User user, @PathVariable("id") Long id,
                                 @PathVariable("step") Integer step,
                                 @RequestParam(value = "recipientId") Long recipientId, RedirectAttributes redirectAttributes) {
        SignBook signBook = signBookRepository.findById(id).get();
        signBookService.removeStepRecipient(signBook, step, recipientId);
        redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Le destinataire à été supprimé"));
        return "redirect:/user/signbooks/" + id + "/?form";
    }

    @PreAuthorize("@signBookService.preAuthorizeManage(#id, #user)")
    @GetMapping(value = "/pending/{id}")
    public String pending(@ModelAttribute("user") User user, @PathVariable("id") Long id) throws InterruptedException {
        SignBook signBook = signBookRepository.findById(id).get();
        signBookService.pendingSignBook(signBook, user);
        return "redirect:/user/signrequests/" + signBook.getSignRequests().get(0).getId();
    }

    @PreAuthorize("@signBookService.preAuthorizeView(#id, #user)")
    @PostMapping(value = "/comment/{id}")
    public String comment(@ModelAttribute("user") User user, @PathVariable("id") Long id,
                          @RequestParam(value = "comment", required = false) String comment,
                          @RequestParam(value = "pageNumber", required = false) Integer pageNumber,
                          @RequestParam(value = "posX", required = false) Integer posX,
                          @RequestParam(value = "posY", required = false) Integer posY,
                          HttpServletRequest request) {
        SignRequest signRequest = signRequestRepository.findById(id).get();
        signRequest.setComment(comment);
        signRequestService.updateStatus(signRequest, null, "Ajout d'un commentaire", "SUCCESS", pageNumber, posX, posY);
        return "redirect:/user/signbooks/" + signRequest.getParentSignBook().getId() + "/" + signRequest.getParentSignBook().getSignRequests().indexOf(signRequest);
    }

    @PreAuthorize("@signBookService.preAuthorizeManage(#id, #user)")
    @ResponseBody
    @PostMapping(value = "/add-docs-in-sign-book-group/{workflowName}/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object addDocumentInSignBookGroup(@ModelAttribute("user") User user,
                                             @PathVariable("name") String name,
                                             @PathVariable("workflowName") String workflowName,
                                             @RequestParam("multipartFiles") MultipartFile[] multipartFiles, HttpServletRequest httpServletRequest) throws EsupSignatureException, EsupSignatureIOException {
        logger.info("start add documents in " + name);
        SignBook signBook = signBookService.createSignBook(workflowName, name, user, false);
        SignRequest signRequest = signRequestService.createSignRequest(name, user);
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

        List<Workflow> workflows = workflowRepository.findByName(workflowName);
        if(workflows.size() > 0) {
            name = workflows.get(0).getTitle() + "_" + name;
        }
        SignBook signBook = signBookService.createSignBook(name, "", user, false);
        for (MultipartFile multipartFile : multipartFiles) {
            SignRequest signRequest = signRequestService.createSignRequest(signBook.getName() + "_" + fileService.getNameOnly(multipartFile.getOriginalFilename()), user);
            signRequestService.addDocsToSignRequest(signRequest, multipartFile);
            signBookService.addSignRequest(signBook, signRequest);
        }
        String[] ok = {"ok"};
        return ok;
    }

}
