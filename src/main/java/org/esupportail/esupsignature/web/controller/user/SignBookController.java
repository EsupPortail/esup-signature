package org.esupportail.esupsignature.web.controller.user;

import io.swagger.v3.oas.annotations.Hidden;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.service.LogService;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
import org.esupportail.esupsignature.web.ws.json.JsonWorkflowStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;

@Hidden
@Controller
@RequestMapping("/user/signbooks")
public class SignBookController {

    private static final Logger logger = LoggerFactory.getLogger(SignBookController.class);

    @Resource
    private WorkflowService workflowService;

    @Resource
    private SignBookService signBookService;

    @Resource
    private LogService logService;

    @Resource
    private SignRequestService signRequestService;

    @PreAuthorize("@preAuthorizeService.signBookView(#id, #userEppn)")
    @GetMapping(value = "/{id}")
    public String show(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id) {
        SignBook signBook = signBookService.getById(id);
        return "redirect:/user/signrequests/" + signBook.getSignRequests().get(0).getId();
    }

    @PreAuthorize("@preAuthorizeService.signBookManage(#id, #authUserEppn)")
    @DeleteMapping(value = "/{id}", produces = "text/html")
    public String delete(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        signBookService.delete(id);
        redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "Suppression effectuée"));
        return "redirect:/user/signrequests";
    }

    @PreAuthorize("@preAuthorizeService.signBookManage(#id, #authUserEppn)")
    @DeleteMapping(value = "silent-delete/{id}", produces = "text/html")
    @ResponseBody
    public void silentDelete(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id) {
        signBookService.delete(id);
    }

    @PreAuthorize("@preAuthorizeService.signBookManage(#id, #authUserEppn)")
    @GetMapping(value = "/{id}", params = "form")
    public String updateForm(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, Model model) {
        SignBook signBook = signBookService.getById(id);
        model.addAttribute("signBook", signBook);
        SignRequest signRequest = signBook.getSignRequests().get(0);
        model.addAttribute("signRequest", signRequest);
        model.addAttribute("toSignDocument", signRequestService.getToSignDocuments(signRequest.getId()).get(0));
        model.addAttribute("signable", signRequest.getSignable());
        model.addAttribute("comments", logService.getLogs(signRequest.getId()));
        model.addAttribute("logs", signBook.getLogs());
        model.addAttribute("allSteps", signBookService.getAllSteps(signBook));
        model.addAttribute("signTypes", SignType.values());
        model.addAttribute("workflows", workflowService.getWorkflowsByUser(authUserEppn, authUserEppn));
        return "user/signrequests/update-signbook";
    }

    @PreAuthorize("@preAuthorizeService.signBookManage(#id, #authUserEppn)")
    @PostMapping(value = "/add-live-step/{id}")
    public String addStep(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                          @RequestParam("recipientsEmails") String[] recipientsEmails,
                          @RequestParam("stepNumber") int stepNumber,
                          @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete,
                          @RequestParam("signType") SignType signType, RedirectAttributes redirectAttributes) {
        try {
            signBookService.addLiveStep(id, recipientsEmails, stepNumber, allSignToComplete, signType, false, authUserEppn);
            redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Étape ajoutée"));
        } catch (EsupSignatureException e) {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("error", e.getMessage()));
        }

        return "redirect:/user/signbooks/" + id + "/?form";
    }

    @PreAuthorize("@preAuthorizeService.signRequestSign(#id, #userEppn, #authUserEppn)")
    @PostMapping(value = "/add-repeatable-step/{id}")
    @ResponseBody
    public ResponseEntity<String> addRepeatableStep(@ModelAttribute("authUserEppn") String authUserEppn, @ModelAttribute("userEppn") String userEppn,
                                                    @PathVariable("id") Long id,
                                                    @RequestBody JsonWorkflowStep step) {
        try {
            String[] recipientsEmailsArray = new String[step.getRecipientsEmails().size()];
            recipientsEmailsArray = step.getRecipientsEmails().toArray(recipientsEmailsArray);
            signBookService.addLiveStep(signRequestService.getById(id).getParentSignBook().getId(), recipientsEmailsArray, step.getStepNumber(), step.getAllSignToComplete(), SignType.valueOf(step.getSignType()), true, authUserEppn);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (EsupSignatureException e) {
            logger.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("@preAuthorizeService.signBookManage(#id, #authUserEppn)")
    @DeleteMapping(value = "/remove-live-step/{id}/{step}")
    public String removeStep(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @PathVariable("step") Integer step, RedirectAttributes redirectAttributes) {
        if (signBookService.removeStep(id, step)) {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("info", "L'étape a été supprimés"));
        } else {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("warn", "L'étape ne peut pas être supprimée"));
        }
        return "redirect:/user/signbooks/" + id + "/?form";
    }

    @PreAuthorize("@preAuthorizeService.signBookManage(#id, #authUserEppn)")
    @PostMapping(value = "/add-workflow/{id}")
    public String addWorkflow(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                          @RequestParam(value = "workflowSignBookId") Long workflowSignBookId) {
        SignBook signBook = signBookService.getById(id);
        signBookService.addWorkflowToSignBook(signBook, authUserEppn, workflowSignBookId);
        return "redirect:/user/signrequests/" + signBook.getSignRequests().get(0).getId() + "/?form";
    }

    @PreAuthorize("@preAuthorizeService.signBookManage(#id, #authUserEppn)")
    @PostMapping(value = "/add-docs/{id}")
    public String addDocumentToNewSignRequest(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                                              @RequestParam("multipartFiles") MultipartFile[] multipartFiles) throws EsupSignatureIOException {
        logger.info("start add documents");
        SignBook signBook = signBookService.getById(id);
        signBookService.addDocumentsToSignBook(signBook, signBook.getName(), multipartFiles, authUserEppn);
        return "redirect:/user/signrequests/" + signBook.getSignRequests().get(0).getId() + "/?form";
    }

    @PreAuthorize("@preAuthorizeService.signBookManage(#id, #authUserEppn)")
    @GetMapping(value = "/pending/{id}")
    public String pending(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id) {
        SignBook signBook = signBookService.getById(id);
        signBookService.pendingSignBook(signBook, null, authUserEppn, authUserEppn);
        return "redirect:/user/signrequests/" + signBook.getSignRequests().get(0).getId();
    }

    @PreAuthorize("@preAuthorizeService.signBookManage(#name, #authUserEppn)")
    @ResponseBody
    @PostMapping(value = "/add-docs-in-sign-book-group/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object addDocumentInSignBookGroup(@ModelAttribute("authUserEppn") String authUserEppn,
                                             @PathVariable("name") String name,
                                             @RequestParam("multipartFiles") MultipartFile[] multipartFiles) throws EsupSignatureIOException {
        logger.info("start add documents in " + name);
        SignBook signBook = signBookService.addDocsInNewSignBookGrouped(name, multipartFiles, authUserEppn);
        String[] ok = {"" + signBook.getId()};
        return ok;
    }

    @ResponseBody
    @PostMapping(value = "/add-docs-in-sign-book-unique/{workflowName}/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object addDocumentToNewSignRequestUnique(@ModelAttribute("authUserEppn") String authUserEppn,
                                                    @PathVariable("name") String name,
                                                    @PathVariable("workflowName") String workflowName,
                                                    @RequestParam("multipartFiles") MultipartFile[] multipartFiles, Model model) throws EsupSignatureIOException {
        User authUser = (User) model.getAttribute("authUser");
        logger.info("start add documents in " + name);
        SignBook signBook = signBookService.addDocsInNewSignBookSeparated(name, workflowName, multipartFiles, authUser);
        String[] ok = {"" + signBook.getId()};
        return ok;
    }


}
