package org.esupportail.esupsignature.web.wssecure;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dto.js.JsMessage;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.export.SedaExportService;
import org.esupportail.esupsignature.service.utils.StepStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/ws-secure/global")
@SessionAttributes("signBookId")
public class GlobalWsSecureController {

    private static final Logger logger = LoggerFactory.getLogger(GlobalWsSecureController.class);

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private SignBookService signBookService;

    @Resource
    private DocumentService documentService;

    @Resource
    private SedaExportService sedaExportService;

    @Resource
    private CommentService commentService;

    @Resource
    private WorkflowService workflowService;

    private final GlobalProperties globalProperties;

    public GlobalWsSecureController(GlobalProperties globalProperties) {
        this.globalProperties = globalProperties;
    }

    @PreAuthorize("@preAuthorizeService.signRequestSign(#signRequestId, #userEppn, #authUserEppn)")
    @ResponseBody
    @PostMapping(value = "/sign/{signRequestId}")
    public ResponseEntity<String> sign(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("signRequestId") Long signRequestId,
                                       @RequestParam(value = "signRequestParams") String signRequestParamsJsonString,
                                       @RequestParam(value = "comment", required = false) String comment,
                                       @RequestParam(value = "formData", required = false) String formData,
                                       @RequestParam(value = "password", required = false) String password,
                                       @RequestParam(value = "certType", required = false) String certType,
                                       @RequestParam(value = "sealCertificat", required = false) String sealCertificat,
                                       HttpSession httpSession) throws IOException {
        Object userShareString = httpSession.getAttribute("userShareId");
        Long userShareId = null;
        if(userShareString != null) userShareId = Long.valueOf(userShareString.toString());
        try {
            StepStatus stepStatus = signBookService.initSign(signRequestId, signRequestParamsJsonString, comment, formData, password, certType, sealCertificat, userShareId, userEppn, authUserEppn);
            if(stepStatus.equals(StepStatus.nexu_redirect)) {
                return ResponseEntity.ok().body("initNexu");
            }
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#signRequestId, #userEppn, #authUserEppn)")
    @ResponseBody
    @PostMapping(value = "/viewed/{signRequestId}")
    public ResponseEntity<Void> viewedBy(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("signRequestId") Long signRequestId) {
        signRequestService.viewedBy(signRequestId, userEppn);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @GetMapping(value = "/get-last-file/{id}")
    public ResponseEntity<Void> getLastFile(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletResponse httpServletResponse) {
        try {
            signRequestService.getToSignFileResponse(id, "attachment", httpServletResponse, false);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("get file error", e);
        }
        return ResponseEntity.notFound().build();
    }

    @PreAuthorize("@preAuthorizeService.signRequestCreator(#id, #authUserEppn)")
    @GetMapping(value = "/get-original-file/{id}")
    public ResponseEntity<Void> getOriginalFile(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletResponse httpServletResponse) {
        try {
            signRequestService.getOriginalFileResponse(id, httpServletResponse);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("get file error", e);
        }
        return ResponseEntity.notFound().build();
    }

    @PreAuthorize("@preAuthorizeService.documentView(#documentId, #userEppn, #authUserEppn)")
    @GetMapping(value = "/get-file/{documentId}")
    public ResponseEntity<Void> getFile(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("documentId") Long documentId, HttpServletResponse httpServletResponse) throws IOException {
        try {
            signRequestService.getFileResponse(documentId, httpServletResponse);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @GetMapping(value = "/get-last-file-inline/{id}")
    public ResponseEntity<Void> getLastFileFromSignRequestInLine(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletResponse httpServletResponse) {
        try {
            signRequestService.getToSignFileResponse(id, "inline", httpServletResponse, false);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return ResponseEntity.notFound().build();
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @GetMapping(value = "/get-last-file-pdf/{id}")
    public ResponseEntity<Void> getLastFileFromSignRequestPdf(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletResponse httpServletResponse) {
        try {
            signRequestService.getToSignFileResponse(id, "form-data", httpServletResponse, false);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return ResponseEntity.notFound().build();
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @GetMapping(value = "/get-last-file-report/{id}")
    public ResponseEntity<Void> getLastFileReport(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        try {
            signRequestService.getSignedFileAndReportResponse(id, httpServletRequest, httpServletResponse, false);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.warn("get file error :" + e.getMessage());
        }
        return ResponseEntity.notFound().build();
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
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@preAuthorizeService.documentCreator(#documentId, #authUserEppn)")
    @ResponseBody
    @PostMapping(value = "/remove-doc/{documentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String removeDocument(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("documentId") Long documentId) {
        logger.info("remove document " + documentId);
        Document document = documentService.getById(documentId);
        SignRequest signRequest = signRequestService.getById(document.getParentId());
        signRequest.getOriginalDocuments().remove(document);
        return "{}";
    }

    @PreAuthorize("@preAuthorizeService.signRequestView(#id, #userEppn, #authUserEppn)")
    @GetMapping(value = "/get-seda/{id}")
    public ResponseEntity<Void> getSeda(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, HttpServletResponse httpServletResponse) throws IOException {
        SignRequest signRequest = signRequestService.getById(id);
        InputStream inputStream = sedaExportService.generateSip(id);
        httpServletResponse.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode(signRequest.getTitle() + ".zip", StandardCharsets.UTF_8.toString()));
        httpServletResponse.setContentType("application/zip");
        IOUtils.copy(inputStream, httpServletResponse.getOutputStream());
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@preAuthorizeService.signRequestOwner(#id, #authUserEppn)")
    @DeleteMapping(value = "/delete-comment/{id}/{commentId}")
    public ResponseEntity<Void> deleteComments(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id, @PathVariable("commentId") Long commentId,  RedirectAttributes redirectAttributes) {
        commentService.deleteComment(commentId, null);
        redirectAttributes.addFlashAttribute("message", new JsMessage("success", "Le commentaire a bien été supprimé"));
        return ResponseEntity.ok().build();
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
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return ResponseEntity.notFound().build();
    }

    @PreAuthorize("@preAuthorizeService.signBookCreator(#signBookId, #userEppn)")
    @ResponseBody
    @PostMapping(value = "/add-docs/{signBookId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> addDocumentToNewSignRequest(@PathVariable("signBookId") Long signBookId,  @ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn, @RequestParam("multipartFiles") MultipartFile[] multipartFiles) throws EsupSignatureIOException {
        logger.info("start add documents");
        if(globalProperties.getPdfOnly() && Arrays.stream(multipartFiles).anyMatch(m -> !Objects.equals(m.getContentType(), "application/pdf"))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Seul les fichiers PDF sont autorisés");
        }
        try {
            signBookService.addDocumentsToSignBook(signBookId, multipartFiles, authUserEppn);
        } catch(Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
        return ResponseEntity.ok().body(signBookId.toString());
    }

    @PreAuthorize("@preAuthorizeService.signBookManage(#id, #authUserEppn)")
    @DeleteMapping(value = "/silent-delete-signbook/{id}", produces = "text/html")
    @ResponseBody
    public void silentDeleteSignBook(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id) {
        signBookService.deleteDefinitive(id, authUserEppn);
    }

    @DeleteMapping(value = "/silent-delete-workflow/{id}", produces = "text/html")
    @PreAuthorize("@preAuthorizeService.workflowOwner(#id, #userEppn)")
    @ResponseBody
    public void silentDeleteWorkflow(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id) throws EsupSignatureRuntimeException {
        workflowService.delete(id);
    }

}
