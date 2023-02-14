package org.esupportail.esupsignature.web.wssecure;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.export.SedaExportService;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.esupportail.esupsignature.web.ws.json.JsonExternalUserInfo;
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
import org.hibernate.HibernateException;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/ws-secure/signrequests")
@SessionAttributes("signBookId")
public class SignRequestWsSecureController {

    private static final Logger logger = LoggerFactory.getLogger(SignRequestWsSecureController.class);

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private SignBookService signBookService;

    @Resource
    private DocumentService documentService;

    @Resource
    private PreAuthorizeService preAuthorizeService;

    @Resource
    private SedaExportService sedaExportService;

    @Resource
    private CommentService commentService;

    @Resource
    private UserService userService;

    @Resource
    private WorkflowService workflowService;

    @PreAuthorize("@preAuthorizeService.signRequestSign(#id, #userEppn, #authUserEppn)")
    @ResponseBody
    @PostMapping(value = "/sign/{id}")
    public ResponseEntity<String> sign(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id,
                                       @RequestParam(value = "signRequestParams") String signRequestParamsJsonString,
                                       @RequestParam(value = "comment", required = false) String comment,
                                       @RequestParam(value = "formData", required = false) String formData,
                                       @RequestParam(value = "password", required = false) String password,
                                       @RequestParam(value = "certType", required = false) String certType,
                                       HttpSession httpSession) {
        Object userShareString = httpSession.getAttribute("userShareId");
        Long userShareId = null;
        if(userShareString != null) userShareId = Long.valueOf(userShareString.toString());
        try {
            boolean result = signBookService.initSign(id, signRequestParamsJsonString, comment, formData, password, certType, userShareId, userEppn, authUserEppn);
            if(!result) {
                return ResponseEntity.status(HttpStatus.OK).body("initNexu");
            }
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @GetMapping(value = "/get-last-file/{id}")
    public ResponseEntity<Void> getLastFile(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletResponse httpServletResponse) {
        try {
            signRequestService.getToSignFileResponse(id, "attachment", httpServletResponse);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            logger.error("get file error", e);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @GetMapping(value = "/get-file/{id}")
    public ResponseEntity<Void> getFile(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletResponse httpServletResponse) throws IOException {
        Document document = documentService.getById(id);
        if(signRequestService.getById(document.getParentId()) != null) {
            if(preAuthorizeService.signRequestView(document.getParentId(), userEppn, authUserEppn)) {
                signRequestService.getFileResponse(id, httpServletResponse);
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                logger.warn(userEppn + " try access document " + id + " without permission");
            }
        } else {
            logger.warn("document is not present in signResquest");
        }
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }


    @GetMapping(value = "/get-last-file-inline/{id}")
    public ResponseEntity<Void> getLastFileFromSignRequestInLine(@PathVariable("id") Long id, HttpServletResponse httpServletResponse) {
        try {
            signRequestService.getToSignFileResponse(id, "inline", httpServletResponse);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @GetMapping(value = "/get-last-file-report/{id}")
    public ResponseEntity<Void> getLastFileReport(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        try {
            signBookService.getToSignFileReportResponse(id, httpServletRequest, httpServletResponse);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            logger.error("get file error", e);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @PreAuthorize("@preAuthorizeService.signBookView(#id, #userEppn, #authUserEppn)")
    @GetMapping(value = "/get-last-files/{id}", produces = "application/zip")
    @ResponseBody
    public ResponseEntity<Void> getLastFiles(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletResponse httpServletResponse) throws IOException, EsupSignatureFsException {
        httpServletResponse.setContentType("application/zip");
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        httpServletResponse.setHeader("Content-Disposition", "attachment; filename=\"download.zip\"");
        signBookService.getMultipleSignedDocuments(Collections.singletonList(id), httpServletResponse);
        httpServletResponse.flushBuffer();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResponseBody
    @PostMapping(value = "/remove-doc/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String removeDocument(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id) throws JSONException {
        logger.info("remove document " + id);
        JSONObject result = new JSONObject();
        Document document = documentService.getById(id);
        SignRequest signRequest = signRequestService.getById(document.getParentId());
        if(signRequest.getCreateBy().getEppn().equals(authUserEppn)) {
            signRequest.getOriginalDocuments().remove(document);
        } else {
            result.put("error", "Non autorisé");
        }
        return result.toString();
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @GetMapping(value = "/get-seda/{id}")
    public ResponseEntity<Void> getSeda(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletResponse httpServletResponse) throws IOException {
        SignRequest signRequest = signRequestService.getById(id);
        InputStream inputStream = sedaExportService.generateSip(id);
        httpServletResponse.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode(signRequest.getTitle() + ".zip", StandardCharsets.UTF_8.toString()));
        httpServletResponse.setContentType("application/zip");
        IOUtils.copy(inputStream, httpServletResponse.getOutputStream());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserEppn)")
    @DeleteMapping(value = "/delete-comment/{id}/{commentId}")
    public ResponseEntity<Void> deleteComments(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @PathVariable("commentId") Long commentId,  RedirectAttributes redirectAttributes) {
        commentService.deleteComment(commentId);
        redirectAttributes.addFlashAttribute("message", new JsonMessage("success", "Le commentaire à bien été supprimé"));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping(value = "/warning-readed")
    @ResponseBody
    public void warningReaded(@ModelAttribute("authUserEppn") String authUserEppn) {
        signRequestService.warningReaded(authUserEppn);
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @GetMapping(value = "/print-with-code/{id}")
    public ResponseEntity<Void> printWithCode(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletResponse httpServletResponse) {
        try {
            signRequestService.getToSignFileResponseWithCode(id, httpServletResponse);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @PreAuthorize("@preAuthorizeService.signBookCreator(#signBookId, #userEppn)")
    @ResponseBody
    @PostMapping(value = "/finish-signbook")
    public ResponseEntity<Long> initSignBook(@SessionAttribute("signBookId") Long signBookId, HttpSession session, @ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn) {
        logger.info("start add documents");
        signBookService.finishSignBookUpload(signBookId, userEppn);
        session.removeAttribute("signBookId");
        return new ResponseEntity<>(signBookId, HttpStatus.OK);
    }

    @PreAuthorize("@preAuthorizeService.signBookCreator(#signBookId, #userEppn)")
    @ResponseBody
    @PostMapping(value = "/add-docs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> addDocumentToNewSignRequest(@SessionAttribute("signBookId") Long signBookId,  @ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @RequestParam("multipartFiles") MultipartFile[] multipartFiles, @RequestParam(required = false) Boolean separated) throws EsupSignatureIOException {
        logger.info("start add documents");
        try {
            signBookService.addDocumentsToSignBook(signBookId, multipartFiles, authUserEppn);
            return new ResponseEntity<>("{}", HttpStatus.OK);
        } catch (HibernateException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("@preAuthorizeService.notInShare(#userEppn, #authUserEppn) && hasRole('ROLE_USER')")
    @ResponseBody
    @PostMapping(value = "/create-sign-book")
    public ResponseEntity<Long> createSignFastRequest(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, Model model) {
        SignBook signBook = signBookService.createSignBook(null, null, "Auto signature", userEppn, false);
        signBookService.initSignBookWorkflow(signBook.getId(), SignType.pdfImageStamp, userEppn);
        model.addAttribute("signBookId", signBook.getId());
        return new ResponseEntity<>(signBook.getId(), HttpStatus.OK);
    }

    @PreAuthorize("@preAuthorizeService.notInShare(#userEppn, #authUserEppn) && hasRole('ROLE_USER')")
    @ResponseBody
    @PostMapping(value = "/create-full-sign-book")
    public ResponseEntity<Long> createFullSignFastRequest(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
                                                          @RequestParam("signType") SignType signType,
                                                          @RequestParam(value = "recipientsEmails") List<String> recipientsEmails,
                                                          @RequestParam(value = "recipientsCCEmails", required = false) List<String> recipientsCCEmails,
                                                          @RequestParam(name = "allSignToComplete", required = false) Boolean allSignToComplete,
                                                          @RequestParam(name = "forceAllSign", required = false) Boolean forceAllSign,
                                                          @RequestParam(name = "userSignFirst", required = false) Boolean userSignFirst,
                                                          @RequestParam(value = "pending", required = false) Boolean pending,
                                                          @RequestParam(value = "comment", required = false) String comment,
                                                          @RequestParam(value = "emails", required = false) List<String> emails,
                                                          @RequestParam(value = "names", required = false) List<String> names,
                                                          @RequestParam(value = "firstnames", required = false) List<String> firstnames,
                                                          @RequestParam(value = "phones", required = false) List<String> phones,
                                                          @RequestParam(value = "forcesmses", required = false) List<String> forcesmses,
                                                          @RequestParam(value = "title", required = false) String title,
                                                          Model model) throws EsupSignatureRuntimeException {
        recipientsEmails = recipientsEmails.stream().distinct().collect(Collectors.toList());
        List<JsonExternalUserInfo> externalUsersInfos = userService.getJsonExternalUserInfos(emails, names, firstnames, phones, forcesmses);
        SignBook signBook = signBookService.createFullSignBook(title, signType, allSignToComplete, userSignFirst, pending, comment, recipientsCCEmails, recipientsEmails, externalUsersInfos, userEppn, authUserEppn, false, forceAllSign);
        model.addAttribute("signBookId", signBook.getId());
        return new ResponseEntity<>(signBook.getId(), HttpStatus.OK);
    }

    @PreAuthorize("@preAuthorizeService.notInShare(#userEppn, #authUserEppn) && hasRole('ROLE_USER')")
    @ResponseBody
    @PostMapping(value = "/start-workflow", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Long> addDocumentToNewSignRequestUnique(@ModelAttribute("userEppn") String userEppn,
                                                                  @ModelAttribute("authUserEppn") String authUserEppn,
                                                                  @RequestParam(value = "workflowId", required = false) Long workflowId,
                                                                  @RequestParam(value = "title", required = false) String title, Model model) {
        SignBook signBook;
        if(workflowId != null) {
            Workflow workflow = workflowService.getById(workflowId);
            signBook = signBookService.createSignBook(title, workflow, null, userEppn, false);
        } else {
            signBook = signBookService.createSignBook(title, null, "Demande personnalisée", userEppn, false);
        }
        model.addAttribute("signBookId", signBook.getId());
        return new ResponseEntity<>(signBook.getId(), HttpStatus.OK);
    }

}
