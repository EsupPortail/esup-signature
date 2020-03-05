package org.esupportail.esupsignature.web.controller.user;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.repository.*;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Arrays;
import java.util.List;

@RequestMapping("/user/signbooks")
@Controller
@Transactional
public class SignBookController {

    private static final Logger logger = LoggerFactory.getLogger(SignBookController.class);

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

    @Resource
    private WorkflowRepository workflowRepository;

    @Resource
    private WorkflowService workflowService;

    @Resource
    private SignBookService signBookService;

    @Resource
    private LogRepository logRepository;

    @PreAuthorize("@signBookService.preAuthorizeManage(authentication.name, #id)")
    @GetMapping(value = "/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable("id") Long id, Model model) {
        User user = userService.getUserFromAuthentication();
        SignBook signBook = signBookRepository.findById(id).get();
        List<Log> logs = logRepository.findBySignRequestId(signBook.getId());
        model.addAttribute("logs", logs);
        model.addAttribute("signBook", signBook);
        model.addAttribute("signTypes", SignType.values());
        model.addAttribute("workflows", workflowService.getWorkflowsForUser(user));
        return "user/signbooks/update";
    }

    @PreAuthorize("@signBookService.preAuthorizeManage(authentication.name, #id)")
    @DeleteMapping(value = "/{id}", produces = "text/html")
    public String delete(@PathVariable("id") Long id) {
        SignBook signBook = signBookRepository.findById(id).get();
        signBookService.delete(signBook);
        return "redirect:/user/signrequests/";
    }

    @GetMapping(value = "/get-last-file/{id}")
    public void getLastFile(@PathVariable("id") Long id, HttpServletResponse response) {
        SignRequest signRequest = signRequestRepository.findById(id).get();
        User user = userService.getUserFromAuthentication();
        if (signRequestService.checkUserViewRights(user, signRequest)) {
            List<Document> documents = signRequestService.getToSignDocuments(signRequest);
            try {
                if (documents.size() > 1) {
                    response.sendRedirect("/user/signbooks/" + id);
                } else {
                    Document document = documents.get(0);
                    response.setHeader("Content-Disposition", "inline;filename=\"" + document.getFileName() + "\"");
                    response.setContentType(document.getContentType());
                    IOUtils.copy(document.getBigFile().getBinaryFile().getBinaryStream(), response.getOutputStream());
                }
            } catch (Exception e) {
                logger.error("get file error", e);
            }
        } else {
            logger.warn(user.getEppn() + " try to access " + signRequest.getId() + " without view rights");
        }
    }

    @GetMapping(value = "/change-step-sign-type/{id}/{step}")
    public String changeStepSignType(@PathVariable("id") Long id, @PathVariable("step") Integer step, @RequestParam(name="signType") SignType signType) {
        User user = userService.getUserFromAuthentication();
        SignBook signBook = signBookRepository.findById(id).get();
        if(user.getEppn().equals(signBook.getCreateBy()) && signBook.getCurrentWorkflowStepNumber() <= step + 1) {
            signBookService.changeSignType(signBook, step, signType);
            return "redirect:/user/signbooks/" + id + "/?form";
        }
        return "redirect:/user/signbooks/";
    }

    @GetMapping(value = "/update-step/{id}/{step}")
    public String changeStepSignType(@PathVariable("id") Long id,
                                     @PathVariable("step") Integer step,
                                     @RequestParam(name="name", required = false) String name,
                                     @RequestParam(name="signType") SignType signType,
                                     @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete) {
        User user = userService.getUserFromAuthentication();
        SignBook signBook = signBookRepository.findById(id).get();
        if(user.getEppn().equals(signBook.getCreateBy()) && signBook.getCurrentWorkflowStepNumber() <= step + 1) {
            if(allSignToComplete == null) {
                allSignToComplete = false;
            }
            signBookService.changeSignType(signBook, step, signType);
            signBookService.toggleNeedAllSign(signBook, step, allSignToComplete);
            return "redirect:/user/signbooks/" + id + "/?form";
        }
        return "redirect:/user/signbooks/";
    }

    @PostMapping(value = "/add-step/{id}")
    public String addStep(@PathVariable("id") Long id,
                          @RequestParam("recipientsEmails") String[] recipientsEmails,
                          @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete,
                          @RequestParam("signType") String signType) {
        User user = userService.getUserFromAuthentication();
        SignBook signBook = signBookRepository.findById(id).get();
        if (signBookService.checkUserViewRights(user, signBook)) {
            WorkflowStep workflowStep = workflowService.createWorkflowStep("", allSignToComplete, SignType.valueOf(signType), recipientsEmails);
            signBook.getWorkflowSteps().add(workflowStep);
        }
        return "redirect:/user/signbooks/" + id + "/?form";
    }

    @DeleteMapping(value = "/remove-step/{id}/{step}")
    public String removeStep(@PathVariable("id") Long id, @PathVariable("step") Integer step) {
        User user = userService.getUserFromAuthentication();
        SignBook signBook = signBookRepository.findById(id).get();
        if(user.getEppn().equals(signBook.getCreateBy()) && signBook.getCurrentWorkflowStepNumber() <= step + 1) {
            signBookService.removeStep(signBook, step);
        }
        return "redirect:/user/signbooks/" + id + "/?form";
    }

    @PostMapping(value = "/add-workflow/{id}")
    public String addWorkflow(@PathVariable("id") Long id,
                          @RequestParam(value = "workflowSignBookId") Long workflowSignBookId) {
        User user = userService.getUserFromAuthentication();
        SignBook signBook = signBookRepository.findById(id).get();
        if (signBookService.checkUserViewRights(user, signBook)) {
            Workflow workflow = workflowRepository.findById(workflowSignBookId).get();
            signBookService.importWorkflow(signBook, workflow);
            signBookService.nextWorkFlowStep(signBook);
            signBookService.pendingSignBook(signBook, user);
        }
        return "redirect:/user/signbooks/" + id + "/?form";
    }

    @PostMapping(value = "/add-docs/{id}")
    public String addDocumentToNewSignRequest(@PathVariable("id") Long id,
                                              @RequestParam("multipartFiles") MultipartFile[] multipartFiles) throws EsupSignatureIOException {
        logger.info("start add documents");
        User user = userService.getUserFromAuthentication();
        SignBook signBook = signBookRepository.findById(id).get();
        if (signBookService.checkUserViewRights(user, signBook)) {
            for (MultipartFile multipartFile : multipartFiles) {
                SignRequest signRequest = signRequestService.createSignRequest(signBook.getName() + "_" + multipartFile.getOriginalFilename(), user);
                signRequestService.addDocsToSignRequest(signRequest, multipartFile);
                signBookService.addSignRequest(signBook, signRequest);
            }
        }
        return "redirect:/user/signbooks/" + id + "/?form";
    }

    @RequestMapping(value = "/send-to-signbook/{id}/{workflowStepId}", method = RequestMethod.GET)
    public String sendToSignBook(@PathVariable("id") Long id,
                                 @PathVariable("workflowStepId") Long workflowStepId,
                                 @RequestParam(value = "signBookNames") String[] signBookNames,
                                 RedirectAttributes redirectAttrs, HttpServletRequest request) {
        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        SignBook signBook = signBookRepository.findById(id).get();
        if (signBookService.checkUserViewRights(user, signBook)) {
            WorkflowStep workflowStep = workflowStepRepository.findById(workflowStepId).get();
            if (signBookNames != null && signBookNames.length > 0) {
                workflowService.addRecipientsToWorkflowStep(workflowStep, signBookNames);
            }
        } else {
            logger.warn(user.getEppn() + " try to move " + signBook.getId() + " without rights");
        }
        return "redirect:/user/signbooks/" + id + "/?form";
    }

    @DeleteMapping(value = "/remove-step-recipent/{id}/{step}")
    public String removeStepRecipient(@PathVariable("id") Long id,
                                 @PathVariable("step") Integer step,
                                 @RequestParam(value = "recipientId") Long recipientId, HttpServletRequest request) {
        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        SignBook signBook = signBookRepository.findById(id).get();
        if (signBookService.checkUserViewRights(user, signBook)) {
            signBookService.removeStepRecipient(signBook, step, recipientId);
        } else {
            logger.warn(user.getEppn() + " try to move " + signBook.getId() + " without rights");
        }
        return "redirect:/user/signbooks/" + id + "/?form";
    }

    @RequestMapping(value = "/pending/{id}", method = RequestMethod.GET)
    public String pending(@PathVariable("id") Long id,
                          @RequestParam(value = "comment", required = false) String comment,
                          HttpServletRequest request) throws EsupSignatureIOException {
        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        SignBook signBook = signBookRepository.findById(id).get();
        if (signBookService.checkUserViewRights(user, signBook)) {
            signBookService.nextWorkFlowStep(signBook);
            signBookService.pendingSignBook(signBook, user);
        }
        return "redirect:/user/signrequests/" + signBook.getSignRequests().get(0).getId();
    }

    @RequestMapping(value = "/scan-pdf-sign/{id}", method = RequestMethod.GET)
    public String scanPdfSign(@PathVariable("id") Long id,
                          RedirectAttributes redirectAttrs, HttpServletRequest request) throws IOException {
        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        SignBook signBook = signBookRepository.findById(id).get();
        for(SignRequest signRequest : signBook.getSignRequests()) {
            if (signBook.getCurrentWorkflowStepNumber() == 1 && signRequest.getOriginalDocuments().size() == 1 && signRequest.getOriginalDocuments().get(0).getContentType().contains("pdf")) {
                try {
                    List<SignRequestParams> signRequestParamses = signRequestService.scanSignatureFields(signRequest.getOriginalDocuments().get(0).getInputStream());
                    redirectAttrs.addFlashAttribute("messageInfo", "Scan terminé, " + signRequestParamses.size() + " signature(s) trouvée(s)");
                } catch (EsupSignatureIOException e) {
                    logger.error("unable to scan the pdf document from " + signRequest.getId(), e);
                }
            }
        }
        return "redirect:/user/signbooks/" + id + "/?form";
    }

    @PostMapping(value = "/comment/{id}")
    public String comment(@PathVariable("id") Long id,
                          @RequestParam(value = "comment", required = false) String comment,
                          @RequestParam(value = "pageNumber", required = false) Integer pageNumber,
                          @RequestParam(value = "posX", required = false) Integer posX,
                          @RequestParam(value = "posY", required = false) Integer posY,
                          HttpServletRequest request) {
        User user = userService.getUserFromAuthentication();
        user.setIp(request.getRemoteAddr());
        SignRequest signRequest = signRequestRepository.findById(id).get();
        if (signRequestService.checkUserViewRights(user, signRequest)) {
            signRequestService.updateStatus(signRequest, null, "Ajout d'un commentaire", user, "SUCCESS", comment, pageNumber, posX, posY);
        } else {
            logger.warn(user.getEppn() + " try to add comment" + signRequest.getId() + " without rights");
        }
        return "redirect:/user/signbooks/" + signRequest.getParentSignBook().getId() + "/" + signRequest.getParentSignBook().getSignRequests().indexOf(signRequest);
    }

}
