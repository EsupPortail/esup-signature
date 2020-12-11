package org.esupportail.esupsignature.web.controller.user;

import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.WorkflowService;
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

@RequestMapping("/user/signbooks")
@Controller
@Transactional
public class SignBookController {

    private static final Logger logger = LoggerFactory.getLogger(SignBookController.class);

    @Resource
    private WorkflowService workflowService;

    @Resource
    private SignBookService signBookService;

    @PreAuthorize("@signBookService.preAuthorizeView(#id, #user)")
    @GetMapping(value = "/{id}")
    public String show(@ModelAttribute("user") User user, @PathVariable("id") Long id) {
        SignBook signBook = signBookService.getById(id);
        return "redirect:/user/signrequests/" + signBook.getSignRequests().get(0).getId();
    }

    @PreAuthorize("@signBookService.preAuthorizeManage(#id, #authUser)")
    @DeleteMapping(value = "/{id}", produces = "text/html")
    public String delete(@ModelAttribute("authUser") User authUser, @PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        SignBook signBook = signBookService.getById(id);
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
        SignBook signBook = signBookService.getById(id);
        model.addAttribute("signBook", signBook);
        model.addAttribute("logs", signBook.getLogs());
        model.addAttribute("allSteps", signBookService.getAllSteps(signBook));
        model.addAttribute("signTypes", SignType.values());
        model.addAttribute("workflows", workflowService.getWorkflowsByUser(authUser, authUser));
        return "user/signrequests/update-signbook";
    }

    @PreAuthorize("@signBookService.preAuthorizeManage(#id, #authUser)")
    @PostMapping(value = "/add-live-step/{id}")
    public String addStep(@ModelAttribute("authUser") User authUser, @PathVariable("id") Long id,
                          @RequestParam("recipientsEmails") String[] recipientsEmails,
                          @RequestParam("stepNumber") int stepNumber,
                          @RequestParam(name="allSignToComplete", required = false) Boolean allSignToComplete,
                          @RequestParam("signType") String signType, RedirectAttributes redirectAttributes) {
        SignBook signBook = signBookService.getById(id);
        try {
            signBookService.addLiveStep(signBook, authUser, recipientsEmails, stepNumber, allSignToComplete, signType);
            redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Étape ajoutée"));
        } catch (EsupSignatureException e) {
            redirectAttributes.addFlashAttribute("message", new JsonMessage("error", e.getMessage()));
        }

        return "redirect:/user/signbooks/" + id + "/?form";
    }

    @PreAuthorize("@signBookService.preAuthorizeManage(#id, #authUser)")
    @DeleteMapping(value = "/remove-live-step/{id}/{step}")
    public String removeStep(@ModelAttribute("authUser") User authUser, @PathVariable("id") Long id, @PathVariable("step") Integer step, RedirectAttributes redirectAttributes) {
        SignBook signBook = signBookService.getById(id);
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
        SignBook signBook = signBookService.getById(id);
        signBookService.addWorkflowToSignBook(authUser, workflowSignBookId, signBook);
        return "redirect:/user/signrequests/" + signBook.getSignRequests().get(0).getId() + "/?form";
    }

    @PreAuthorize("@signBookService.preAuthorizeManage(#id, #authUser)")
    @PostMapping(value = "/add-docs/{id}")
    public String addDocumentToNewSignRequest(@ModelAttribute("authUser") User authUser, @PathVariable("id") Long id,
                                              @RequestParam("multipartFiles") MultipartFile[] multipartFiles) throws EsupSignatureIOException {
        logger.info("start add documents");
        SignBook signBook = signBookService.getById(id);
        signBookService.addDocumentsToSignBook(authUser, multipartFiles, signBook);
        return "redirect:/user/signrequests/" + signBook.getSignRequests().get(0).getId() + "/?form";
    }

    @PreAuthorize("@signBookService.preAuthorizeManage(#id, #authUser)")
    @GetMapping(value = "/pending/{id}")
    public String pending(@ModelAttribute("authUser") User authUser, @PathVariable("id") Long id) {
        SignBook signBook = signBookService.getById(id);
        signBookService.pendingSignBook(signBook, authUser);
        return "redirect:/user/signrequests/" + signBook.getSignRequests().get(0).getId();
    }

    @PreAuthorize("@signBookService.preAuthorizeManage(#id, #authUser)")
    @ResponseBody
    @PostMapping(value = "/add-docs-in-sign-book-group/{workflowName}/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object addDocumentInSignBookGroup(@ModelAttribute("authUser") User authUser,
                                             @PathVariable("name") String name,
                                             @PathVariable("workflowName") String workflowName,
                                             @RequestParam("multipartFiles") MultipartFile[] multipartFiles) throws EsupSignatureIOException {
        logger.info("start add documents in " + name);
        signBookService.addDocsInSignBook(authUser, name, multipartFiles);
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
        signBookService.addDocsInSignBookUnique(user, name, multipartFiles);
        String[] ok = {"ok"};
        return ok;
    }


}
