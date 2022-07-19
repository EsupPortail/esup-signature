package org.esupportail.esupsignature.web.wssecure;

import com.google.zxing.WriterException;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.export.SedaExportService;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.esupportail.esupsignature.web.ws.json.JsonMessage;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.persistence.NoResultException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Collections;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/ws-secure/signrequests")
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
    private AuditTrailService auditTrailService;


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
            signRequestService.getToSignFileResponse(id, httpServletResponse);
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
        } catch (NoResultException | IOException | EsupSignatureFsException | SQLException | EsupSignatureException | WriterException e) {
            logger.error(e.getMessage(), e);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @ResponseBody
    @PostMapping(value = "/start-workflow/{workflowName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object addDocumentToNewSignRequestUnique(@ModelAttribute("authUserEppn") String authUserEppn,
                                                    @PathVariable("workflowName") String workflowName,
                                                    @RequestParam(value = "title", required = false) String title,
                                                    @RequestParam(value = "unique", required = false) Boolean unique,
                                                    @RequestParam(value = "multipartFiles", required = false) MultipartFile[] multipartFiles) {
        logger.info("start add documents in " + workflowName);
        try {
            SignBook signBook;
            if(unique != null && unique) {
                signBook = signBookService.addDocsInNewSignBookGrouped(title, multipartFiles, authUserEppn);
            } else {
                signBook = signBookService.addDocsInNewSignBookSeparated(title, workflowName, multipartFiles, authUserEppn);
            }
            return new String[]{"" + signBook.getId()};
        } catch(EsupSignatureIOException e) {
            logger.warn("signbook not created");
        }
        return new ResponseEntity<>("Un problème est survenu car le document est corrompu. Merci d'essayer avec un document valide", HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
