package org.esupportail.esupsignature.web.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.esupportail.esupsignature.dto.WorkflowStepDto;
import org.esupportail.esupsignature.entity.AuditTrail;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.RecipientService;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ws/signrequests")
public class SignRequestWsController {

    private static final Logger logger = LoggerFactory.getLogger(SignRequestWsController.class);

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private RecipientService recipientService;

    @Resource
    private SignBookService signBookService;

    @CrossOrigin
    @PostMapping(value ="/new", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(description = "Création d'une demande de signature")
    @PreAuthorize("@wsAccessTokenService.isAllAccess(#wsAccessToken)")
    public ResponseEntity<?> create(@Parameter(description = "Multipart stream du fichier à signer") @RequestParam MultipartFile[] multipartFiles,
                                    @RequestParam(required = false) @Parameter(description = "Paramètres des étapes (objet json)", array = @ArraySchema(schema = @Schema( implementation = WorkflowStepDto.class)), example = "[{\n" +
                                            "  \"title\": \"string\",\n" +
                                            "  \"workflowId\": 0,\n" +
                                            "  \"stepNumber\": 0,\n" +
                                            "  \"description\": \"string\",\n" +
                                            "  \"recipientsCCEmails\": [\n" +
                                            "    \"string\"\n" +
                                            "  ],\n" +
                                            "  \"recipients\": [\n" +
                                            "    {\n" +
                                            "      \"step\": 0,\n" +
                                            "      \"email\": \"string\",\n" +
                                            "      \"phone\": \"string\",\n" +
                                            "      \"name\": \"string\",\n" +
                                            "      \"firstName\": \"string\",\n" +
                                            "      \"forceSms\": true\n" +
                                            "    }\n" +
                                            "  ],\n" +
                                            "  \"changeable\": true,\n" +
                                            "  \"signLevel\": 0,\n" +
                                            "  \"signType\": \"hiddenVisa\",\n" +
                                            "  \"repeatable\": true,\n" +
                                            "  \"repeatableSignType\": \"hiddenVisa\",\n" +
                                            "  \"allSignToComplete\": true,\n" +
                                            "  \"userSignFirst\": true,\n" +
                                            "  \"multiSign\": true,\n" +
                                            "  \"autoSign\": true,\n" +
                                            "  \"forceAllSign\": true,\n" +
                                            "  \"comment\": \"string\",\n" +
                                            "  \"attachmentRequire\": true,\n" +
                                            "  \"maxRecipients\": 0\n" +
                                            "}]") String stepsJsonString,
                                    @RequestParam(required = false) @Parameter(description = "EPPN du créateur/propriétaire de la demande") String createByEppn,
                                    @RequestParam(required = false) @Parameter(description = "Titre (facultatif)") String title,
                                    @RequestParam(required = false) @Parameter(description = "Liste des personnes en copie (emails). Ne prend pas en charge les groupes") List<String> recipientsCCEmails,
                                    @RequestParam(required = false) @Parameter(description = "Commentaire") String comment,
                                    @RequestParam(required = false) @Parameter(description = "Envoyer la demande automatiquement") Boolean pending,
                                    @RequestParam(required = false) @Parameter(description = "Emplacement final", example = "smb://drive.univ-ville.fr/forms-archive/") String targetUrl,
                                    @RequestParam(required = false) @Parameter(description = "Retour au format json (facultatif, false par défaut)") Boolean json,
                                    @ModelAttribute("wsAccessToken") String wsAccessToken,
                                    @RequestParam(required = false) @Parameter(deprecated = true, description = "Liste des participants") List<String> recipientsEmails,
                                    @RequestParam(required = false) @Parameter(deprecated = true, description = "Liste des participants (ancien nom)") List<String> recipientEmails,
                                    @RequestParam(required = false) @Parameter(deprecated = true, description = "Tout les participants doivent-ils signer ?") Boolean allSignToComplete,
                                    @RequestParam(required = false) @Parameter(deprecated = true, description = "Le créateur doit-il signer en premier ?") Boolean userSignFirst,
                                    @RequestParam(required = false) @Parameter(deprecated = true, description = "Forcer la signature de tous les documents") Boolean forceAllSign,
                                    @RequestParam(required = false) @Parameter(deprecated = true, description = "Type de signature", schema = @Schema(allowableValues = {"visa", "pdfImageStamp", "certSign", "nexuSign"}), examples = {@ExampleObject(value = "visa"), @ExampleObject(value = "pdfImageStamp"), @ExampleObject(value = "certSign"), @ExampleObject(value = "nexuSign")}) String signType,
                                    @RequestParam(required = false) @Parameter(deprecated = true, description = "EPPN du créateur/propriétaire de la demande (ancien nom)") String eppn
                       ) {
        if(json == null) {
            json = false;
        }
        if(createByEppn == null && StringUtils.hasText(eppn)) {
            createByEppn = eppn;
        }
        if(createByEppn == null) {
            throw new EsupSignatureRuntimeException("Required request parameter 'createByEppn' for method parameter type String is not present");
        }
        if(recipientEmails == null && recipientsEmails != null && !recipientsEmails.isEmpty()) {
            recipientEmails = recipientsEmails;
        }
        List<WorkflowStepDto> workflowStepDtos;
        if(stepsJsonString == null && recipientEmails != null) {
            workflowStepDtos = recipientService.convertRecipientEmailsToStep(recipientEmails);
            workflowStepDtos.forEach(workflowStepDto -> {
                workflowStepDto.setSignType(SignType.valueOf(signType));
                workflowStepDto.setAllSignToComplete(allSignToComplete);
                workflowStepDto.setUserSignFirst(userSignFirst);
                workflowStepDto.setComment(comment);
                workflowStepDto.setRecipientsCCEmails(recipientsCCEmails);
            });
        } else {
            workflowStepDtos = recipientService.convertRecipientJsonStringToWorkflowStepDtos(stepsJsonString);
        }
        if(workflowStepDtos != null) {
            try {
                Map<SignBook, String> signBookStringMap = signBookService.createAndSendSignBook(title, multipartFiles, pending, workflowStepDtos, createByEppn, true, forceAllSign, targetUrl, wsAccessToken);
                List<String> signRequestIds = signBookStringMap.keySet().stream().flatMap(sb -> sb.getSignRequests().stream().map(signRequest -> signRequest.getId().toString())).toList();
                if(json) {
                    return ResponseEntity.ok(signRequestIds);
                } else {
                    return ResponseEntity.ok(org.apache.commons.lang.StringUtils.join(signRequestIds, ","));
                }
            } catch (EsupSignatureException e) {
                logger.error(e.getMessage(), e);
                return ResponseEntity.ok("-1");
            }
        }
        return ResponseEntity.ok("-1");
    }

    @CrossOrigin
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(description = "Récupération d'une demande de signature", responses = @ApiResponse(description = "SignRequest", content = @Content(schema = @Schema(implementation = SignRequest.class))))
    @PreAuthorize("@wsAccessTokenService.readWorkflowAccess(#id, #wsAccessToken)")
    public String get(@Parameter(description = "Identifiant de la demande") @PathVariable Long id,
                      @ModelAttribute("wsAccessToken") String wsAccessToken) throws JsonProcessingException {
        return signRequestService.getJson(id);
    }

    @CrossOrigin
    @GetMapping(value = "/status/{id}")
    @Operation(description = "Récupération du statut d'une demande de signature")
    @ResponseBody
    @PreAuthorize("@wsAccessTokenService.readWorkflowAccess(#id, #wsAccessToken)")
    public String getStatus(@Parameter(description = "Identifiant de la demande") @PathVariable Long id,
                            @ModelAttribute("wsAccessToken") String wsAccessToken) {
        return signRequestService.getStatus(id);
    }

    @CrossOrigin
    @GetMapping(value = "/audit-trail/{id}")
    @Operation(description = "Récupération du dossier de preuve de la demande", responses = @ApiResponse(description = "AuditTrail", content = @Content(schema = @Schema(implementation = AuditTrail.class))))
    @ResponseBody
    @PreAuthorize("@wsAccessTokenService.readWorkflowAccess(#id, #wsAccessToken)")
    public String getAuditTail(@Parameter(description = "Dossier de preuve de la demande") @PathVariable Long id,
                               @ModelAttribute("wsAccessToken") String wsAccessToken) throws JsonProcessingException {
        return signRequestService.getAuditTrailJson(id);
    }

    @CrossOrigin
    @DeleteMapping("/{id}")
    @Operation(description = "Supprimer une demande de signature définitivement")
    @PreAuthorize("@wsAccessTokenService.deleteWorkflowAccess(#id, #wsAccessToken)")
    public ResponseEntity<String> delete(@PathVariable Long id,
                                         @ModelAttribute("wsAccessToken") String wsAccessToken) {
        Long signBookId = signRequestService.getParentIdIfSignRequestUnique(id);
        if(signBookId != null) {
            signBookService.deleteDefinitive(signBookId, "system");
        } else {
            signRequestService.deleteDefinitive(id, true, "system");
        }
        return ResponseEntity.ok().build();
    }

    @CrossOrigin
    @DeleteMapping("/soft/{id}")
    @Operation(description = "Supprimer une demande de signature")
    @PreAuthorize("@wsAccessTokenService.deleteWorkflowAccess(#id, #wsAccessToken)")
    public ResponseEntity<String> softDelete(@PathVariable Long id,
                                             @ModelAttribute("wsAccessToken") String wsAccessToken) {
        Long signBookId = signRequestService.getParentIdIfSignRequestUnique(id);
        if(signBookId != null) {
            signBookService.delete(signBookId, "system");
        } else {
            signRequestService.delete(id, "system");
        }
        return ResponseEntity.ok().build();
    }

    @CrossOrigin
    @DeleteMapping("/{id}/signbook")
    @Operation(description = "Supprimer le parapheur dans lequel se trouve la demande ciblée")
    @PreAuthorize("@wsAccessTokenService.deleteWorkflowAccess(#id, #wsAccessToken)")
    public ResponseEntity<String> deleteSignBook(@PathVariable Long id,
                                                 @ModelAttribute("wsAccessToken") String wsAccessToken) {
        SignRequest signRequest = signRequestService.getById(id);
        signBookService.deleteDefinitive(signRequest.getParentSignBook().getId(), "system");
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/get-last-file/{id}")
    @ResponseBody
    @Operation(description = "Récupérer le dernier fichier signé d'une demande", responses = @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = byte[].class), mediaType = "application/pdf")))
    @PreAuthorize("@wsAccessTokenService.readWorkflowAccess(#id, #wsAccessToken)")
    public ResponseEntity<Void> getLastFileFromSignRequest(@PathVariable("id") Long id,
                                                           @ModelAttribute("wsAccessToken") String wsAccessToken, HttpServletResponse httpServletResponse) {
        try {
            signRequestService.getToSignFileResponse(id, "attachment", httpServletResponse);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return ResponseEntity.internalServerError().build();
    }

    @GetMapping(value = "/print-with-code/{id}")
    @ResponseBody
    @Operation(description = "Récupérer le dernier fichier signé d'une demande avec un datamatrix apposé dessus", responses = @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = byte[].class), mediaType = "application/pdf")))
    @PreAuthorize("@wsAccessTokenService.readWorkflowAccess(#id, #wsAccessToken)")
    public ResponseEntity<Void> printWithCode(@PathVariable("id") Long id,
                                              @ModelAttribute("wsAccessToken") String wsAccessToken, HttpServletResponse httpServletResponse) {
        try {
            signRequestService.getToSignFileResponseWithCode(id, httpServletResponse);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return ResponseEntity.internalServerError().build();
    }

    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Operation(description = "Récupérer toutes les demandes", responses = @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = List.class), mediaType = "application/pdf")))
    @PreAuthorize("@wsAccessTokenService.isAllAccess(#wsAccessToken)")
    public ResponseEntity<String> getAllSignRequests(@ModelAttribute("wsAccessToken") String wsAccessToken) {
        try {
            return ResponseEntity.ok(signRequestService.getAllToJSon());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return ResponseEntity.internalServerError().build();
    }

    @GetMapping(value = "/return-test")
    @PreAuthorize("@wsAccessTokenService.isAllAccess(#wsAccessToken)")
    @ResponseBody
    public ResponseEntity<Void> returnTest(@RequestParam("signRequestId") String signRequestId, @RequestParam("status") String status, @RequestParam("step") String step,
                                           @ModelAttribute("wsAccessToken") String wsAccessToken) {
        logger.info(signRequestId + ", " + status + ", " + step);
        return ResponseEntity.ok().build();
    }

}
