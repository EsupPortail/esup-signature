package org.esupportail.esupsignature.web.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.WriterException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.esupportail.esupsignature.dto.json.RecipientWsDto;
import org.esupportail.esupsignature.dto.json.SignRequestStepsDto;
import org.esupportail.esupsignature.dto.json.WorkflowStepDto;
import org.esupportail.esupsignature.entity.AuditTrail;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.RecipientService;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestParamsService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ws/signrequests")
public class SignRequestWsController {

    private static final Logger logger = LoggerFactory.getLogger(SignRequestWsController.class);

    private final SignRequestService signRequestService;

    private final RecipientService recipientService;

    private final SignBookService signBookService;

    private final SignRequestParamsService signRequestParamsService;

    public SignRequestWsController(SignRequestService signRequestService, RecipientService recipientService, SignBookService signBookService, SignRequestParamsService signRequestParamsService) {
        this.signRequestService = signRequestService;
        this.recipientService = recipientService;
        this.signBookService = signBookService;
        this.signRequestParamsService = signRequestParamsService;
    }

    @CrossOrigin
    @PostMapping(value ="/scan", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Recupération des paramètres de signature du documents")
    @PreAuthorize("@wsAccessTokenService.isAllAccess(#xApiKey)")
    public ResponseEntity<List<SignRequestParams>> getSignRequestParams(@Parameter(description = "Multipart stream du fichier à signer") @RequestParam MultipartFile[] multipartFiles,
                                                                        @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey
                                                                        ) throws IOException {
        return ResponseEntity.ok().body(signRequestParamsService.scanSignatureFields(multipartFiles[0].getInputStream(), 1, null, false));
    }

    @CrossOrigin
    @PostMapping(value ="/new", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Création d'une demande de signature")
    @PreAuthorize("@wsAccessTokenService.isAllAccess(#xApiKey)")
    public ResponseEntity<?> create(@Parameter(description = "Multipart stream du fichier à signer") @RequestParam MultipartFile[] multipartFiles,
                                    @RequestParam(required = false) @Parameter(description = "Multipart stream des pièces jointes") MultipartFile[] attachementMultipartFiles,
                                    @RequestParam(required = false) @Parameter(description = "Paramètres des étapes (objet json)", array = @ArraySchema(schema = @Schema( implementation = WorkflowStepDto.class)), example =
                                            """
                                                  [{
                                                  "title": "string",
                                                  "workflowId": 0,
                                                  "stepNumber": 1,
                                                  "description": "string",
                                                  "recipientsCCEmails": [
                                                    "string"
                                                  ],
                                                  "recipients": [
                                                    {
                                                      "step": 0,
                                                      "email": "string",
                                                      "phone": "string",
                                                      "name": "string",
                                                      "firstName": "string",
                                                      "forceSms": true
                                                    }
                                                  ],
                                                  "signRequestParams": [
                                                    {
                                                      "signPageNumber": 1,
                                                      "signDocumentNumber": 0,
                                                      "signWidth": 150,
                                                      "signHeight": 75,
                                                      "xPos": 0,
                                                      "yPos": 0
                                                    }
                                                  ],
                                                  "changeable": false,
                                                  "signLevel": 0,
                                                  "signType": "visa",
                                                  "repeatable": false,
                                                  "repeatableSignType": "visa",
                                                  "allSignToComplete": false,
                                                  "userSignFirst": false,
                                                  "multiSign": true,
                                                  "singleSignWithAnnotation": false,
                                                  "autoSign": false,
                                                  "forceAllSign": false,
                                                  "comment": "string",
                                                  "attachmentRequire": false,
                                                  "attachmentAlert": false,
                                                  "maxRecipients": 99,
                                                  "targetEmails": [
                                                    "string"
                                                  ]
                                                  }]
                                                  """) String stepsJsonString,
                                    @RequestParam(required = false) @Parameter(description = "EPPN du créateur/propriétaire de la demande") String createByEppn,
                                    @RequestParam(required = false) @Parameter(description = "Titre (facultatif)") String title,
                                    @RequestParam(required = false) @Parameter(description = "Liste des personnes en copie (emails). Ne prend pas en charge les groupes") List<String> recipientsCCEmails,
                                    @RequestParam(required = false) @Parameter(description = "Commentaire") String comment,
                                    @RequestParam(required = false) @Parameter(description = "Envoyer la demande automatiquement") Boolean pending,
                                    @RequestParam(required = false) @Parameter(description = "Emplacement final", example = "smb://drive.univ-ville.fr/forms-archive/") String targetUrl,
                                    @RequestParam(required = false) @Parameter(description = "Retour au format json (facultatif, false par défaut)") Boolean json,
                                    @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey,
                                    @RequestParam(required = false) @Parameter(deprecated = true, description = "Liste des participants") List<String> recipientsEmails,
                                    @RequestParam(required = false) @Parameter(deprecated = true, description = "Liste des participants (ancien nom)") List<String> recipientEmails,
                                    @RequestParam(required = false) @Parameter(deprecated = true, description = "Tout les participants doivent-ils signer ?") Boolean allSignToComplete,
                                    @RequestParam(required = false) @Parameter(deprecated = true, description = "Le créateur doit-il signer en premier ?") Boolean userSignFirst,
                                    @RequestParam(required = false) @Parameter(deprecated = true, description = "Forcer la signature de tous les documents") Boolean forceAllSign,
                                    @RequestParam(required = false) @Parameter(deprecated = true, description = "Type de signature", schema = @Schema(allowableValues = {"visa", "pdfImageStamp", "certSign", "nexuSign"}), examples = {@ExampleObject(value = "visa"), @ExampleObject(value = "pdfImageStamp"), @ExampleObject(value = "certSign"), @ExampleObject(value = "nexuSign")}) String signType,
                                    @RequestParam(required = false) @Parameter(deprecated = true, description = "EPPN du créateur/propriétaire de la demande (ancien nom)") String eppn
                       ) throws EsupSignatureException {
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
            Map<SignBook, String> signBookStringMap = signBookService.createAndSendSignBook(title, multipartFiles, pending, workflowStepDtos, createByEppn, true, forceAllSign, targetUrl);
            List<String> signRequestIds = signBookStringMap.keySet().stream().flatMap(sb -> sb.getSignRequests().stream().map(signRequest -> signRequest.getId().toString())).toList();
            if(signRequestIds.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("-1");
            }
            signRequestService.addAttachement(attachementMultipartFiles, null, Long.valueOf(signRequestIds.get(0)), createByEppn);
            if(json) {
                return ResponseEntity.ok(signRequestIds);
            } else {
                return ResponseEntity.ok(org.apache.commons.lang.StringUtils.join(signRequestIds, ","));
            }
        }
        return ResponseEntity.internalServerError().body("-1");
    }

    @CrossOrigin
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Récupération d'une demande de signature",
            responses = @ApiResponse(description = "SignRequest", content = @Content(schema = @Schema(implementation = SignRequest.class))))
    @PreAuthorize("@wsAccessTokenService.readWorkflowAccess(#id, #xApiKey)")
    public String get(@Parameter(description = "Identifiant de la demande", required = true) @PathVariable Long id,
                      @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey) throws JsonProcessingException {
        return signRequestService.getJson(id);
    }

    @CrossOrigin
    @GetMapping(value = "/{id}/steps", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Récupération d'une demande de signature",
            responses = @ApiResponse(description = "SignRequest", content = @Content(schema = @Schema(implementation = SignRequest.class))))
    @PreAuthorize("@wsAccessTokenService.readWorkflowAccess(#id, #xApiKey)")
    public List<SignRequestStepsDto> getSteps(@Parameter(description = "Identifiant de la demande", required = true) @PathVariable Long id,
                                              @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey) throws JsonProcessingException {
        return signRequestService.getStepsDto(id);
    }

    @CrossOrigin
    @GetMapping(value = "/status/{id}")
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Récupération du statut d'une demande de signature")
    @PreAuthorize("@wsAccessTokenService.readWorkflowAccess(#id, #xApiKey)")
    public String getStatus(@Parameter(description = "Identifiant de la demande") @PathVariable Long id,
                            @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey) {
        return signRequestService.getStatus(id);
    }

    @CrossOrigin
    @GetMapping(value = "/audit-trail/{id}")
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Récupération du dossier de preuve de la demande", responses = @ApiResponse(description = "AuditTrail", content = @Content(schema = @Schema(implementation = AuditTrail.class))))
    @PreAuthorize("@wsAccessTokenService.readWorkflowAccess(#id, #xApiKey)")
    public String getAuditTail(@Parameter(description = "Dossier de preuve de la demande") @PathVariable Long id,
                               @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey) throws JsonProcessingException {
        return signRequestService.getAuditTrailJson(id);
    }

    @CrossOrigin
    @PostMapping(value = "/update-recipients/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Modifier les destinataires d'une étape de demande de signature")
    @PreAuthorize("@wsAccessTokenService.updateWorkflowAccess(#id, #xApiKey)")
    public ResponseEntity<String> updateRecipients(@PathVariable Long id,
                                                   @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey,
                                                   @RequestPart("recipientWsDtosString") String recipientWsDtosString,
                                                   @RequestParam Integer stepNumber) throws JsonProcessingException, EsupSignatureException {
        List<RecipientWsDto> recipientWsDtos = new ObjectMapper().readValue(recipientWsDtosString, new TypeReference<List<RecipientWsDto>>() {});
        SignRequest signRequest = signRequestService.getById(id);
        signRequestService.replaceRecipientsToWorkflowStep(signRequest.getParentSignBook().getId(), stepNumber, recipientWsDtos);
        return ResponseEntity.ok().build();
    }

    @CrossOrigin
    @DeleteMapping("/{id}")
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Supprimer une demande de signature définitivement")
    @PreAuthorize("@wsAccessTokenService.updateWorkflowAccess(#id, #xApiKey)")
    public ResponseEntity<String> delete(@PathVariable Long id,
                                         @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey) {
        Long signBookId = signRequestService.getParentIdIfSignRequestUnique(id);
        if(signBookId != null) {
            signBookService.deleteDefinitive(signBookId, "system");
        } else {
            signRequestService.deleteDefinitive(id, "system");
        }
        return ResponseEntity.ok().build();
    }

    @CrossOrigin
    @DeleteMapping("/soft/{id}")
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Supprimer une demande de signature")
    @PreAuthorize("@wsAccessTokenService.updateWorkflowAccess(#id, #xApiKey)")
    public ResponseEntity<String> softDelete(@PathVariable Long id,
                                             @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey) {
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
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Supprimer le parapheur dans lequel se trouve la demande ciblée")
    @PreAuthorize("@wsAccessTokenService.updateWorkflowAccess(#id, #xApiKey)")
    public ResponseEntity<String> deleteSignBook(@PathVariable Long id,
                                                 @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey) {
        SignRequest signRequest = signRequestService.getById(id);
        signBookService.deleteDefinitive(signRequest.getParentSignBook().getId(), "system");
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/get-last-file/{id}")
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Récupérer le dernier fichier signé d'une demande", responses = @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = byte[].class), mediaType = MediaType.APPLICATION_PDF_VALUE)))
    @PreAuthorize("@wsAccessTokenService.readWorkflowAccess(#id, #xApiKey)")
    public ResponseEntity<Void> getLastFileFromSignRequest(@PathVariable("id") Long id,
                                                           @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey, HttpServletResponse httpServletResponse) throws IOException, EsupSignatureException {
        signRequestService.getToSignFileResponse(id, "form-data", httpServletResponse, false);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/get-last-file-and-report/{id}")
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Récupérer le dernier fichier signé d'une demande", responses = @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = byte[].class), mediaType = MediaType.APPLICATION_PDF_VALUE)))
    @PreAuthorize("@wsAccessTokenService.readWorkflowAccess(#id, #xApiKey)")
    public ResponseEntity<Void> getLastFileAndReport(@PathVariable("id") Long id,
                                                     @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        try {
            signRequestService.getSignedFileAndReportResponse(id, httpServletRequest, httpServletResponse, true);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping(value = "/print-with-code/{id}")
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Récupérer le dernier fichier signé d'une demande avec un datamatrix apposé dessus", responses = @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = byte[].class), mediaType = MediaType.APPLICATION_PDF_VALUE)))
    @PreAuthorize("@wsAccessTokenService.readWorkflowAccess(#id, #xApiKey)")
    public ResponseEntity<Void> printWithCode(@PathVariable("id") Long id,
                                              @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey, HttpServletResponse httpServletResponse) throws IOException, WriterException {
        signRequestService.getToSignFileResponseWithCode(id, httpServletResponse);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Récupérer toutes les demandes", responses = @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = List.class), mediaType = MediaType.APPLICATION_JSON_VALUE)))
    @PreAuthorize("@wsAccessTokenService.isTokenExist(#xApiKey)")
    public ResponseEntity<String> getAllSignRequests(@ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey) throws JsonProcessingException {
        return ResponseEntity.ok(signRequestService.getAllToJSon(xApiKey));
    }

    @GetMapping(value = "/return-test")
    @PreAuthorize("@wsAccessTokenService.isAllAccess(#xApiKey)")
    public ResponseEntity<Void> returnTest(@RequestParam("signRequestId") String signRequestId, @RequestParam("status") String status, @RequestParam("step") String step,
                                           @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey) {
        logger.info(signRequestId + ", " + status + ", " + step);
        return ResponseEntity.ok().build();
    }

}
