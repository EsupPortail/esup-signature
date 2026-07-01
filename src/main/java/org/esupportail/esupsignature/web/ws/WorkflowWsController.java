package org.esupportail.esupsignature.web.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.dto.ws.SignRequestParamsWsDto;
import org.esupportail.esupsignature.dto.projection.jpa.WorkflowDto;
import org.esupportail.esupsignature.dto.ws.WorkflowStepDto;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.export.WorkflowExportService;
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
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

@RestController
@RequestMapping("/ws/workflows")
public class WorkflowWsController {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowWsController.class);

    private static final byte[] EXCEL_UTF8_HACK = new byte[] { (byte)0xEF, (byte)0xBB, (byte)0xBF};

    private final WorkflowService workflowService;
    private final WorkflowExportService workflowExportService;
    private final SignBookService signBookService;
    private final RecipientService recipientService;
    private final UserService userService;
    private final SignRequestService signRequestService;
    private final SignRequestParamsService signRequestParamsService;
    private final org.esupportail.esupsignature.service.PaperlessService paperlessService;

    public WorkflowWsController(WorkflowService workflowService, WorkflowExportService workflowExportService, SignBookService signBookService, RecipientService recipientService, UserService userService, SignRequestService signRequestService, SignRequestParamsService signRequestParamsService, @org.springframework.beans.factory.annotation.Autowired(required = false) org.esupportail.esupsignature.service.PaperlessService paperlessService) {
        this.workflowService = workflowService;
        this.workflowExportService = workflowExportService;
        this.signBookService = signBookService;
        this.recipientService = recipientService;
        this.userService = userService;
        this.signRequestService = signRequestService;
        this.signRequestParamsService = signRequestParamsService;
        this.paperlessService = paperlessService;
    }

    @CrossOrigin
    @PostMapping(value ="/{id}/scan", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Recupération des paramètres de signature du documents")
    @PreAuthorize("@wsAccessTokenService.createWorkflowAccess(#id, #xApiKey)")
    public ResponseEntity<List<SignRequestParams>> getSignRequestParams(@PathVariable String id,
                                                                        @Parameter(description = "Multipart stream du fichier à signer") @RequestParam MultipartFile[] multipartFiles,
                                                                        @RequestParam(required = false, defaultValue = "false") @Parameter(description = "Trier les champs signature par leurs noms") Boolean orderSignsByName,
                                                                        @RequestParam(required = false) @Parameter(description = "Pattern de détéction d'emplacement") String signRequestParamsDetectionPattern,
                                                                        @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey
    ) throws IOException {
        Workflow workflow = workflowService.getByIdOrToken(id);
        if(workflow != null && StringUtils.hasText(workflow.getSignRequestParamsDetectionPattern()) && !StringUtils.hasText(signRequestParamsDetectionPattern)) {
            signRequestParamsDetectionPattern = workflow.getSignRequestParamsDetectionPattern();
        }
        return ResponseEntity.ok().body(signRequestParamsService.scanSignatureFields(multipartFiles[0].getInputStream(), null,1, signRequestParamsDetectionPattern,false, orderSignsByName));
    }

    @CrossOrigin
    @PostMapping(value = "/{id}/new", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Dépôt d'un document dans une nouvelle instance d'un circuit")
    @PreAuthorize("@wsAccessTokenService.createWorkflowAccess(#id, #xApiKey)")
    public ResponseEntity<?> start(@PathVariable String id,
                                   @RequestParam @Parameter(description = "Multipart stream du fichier à signer") MultipartFile[] multipartFiles,
                                   @RequestParam(required = false, defaultValue = "false") @Parameter(description = "Trier les champs signature par leurs noms") Boolean orderSignsByName,
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
                                                      "signWidth": 200,
                                                      "signHeight": 100,
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
                                   @RequestParam(required = false, defaultValue = "false") @Parameter(description = "Scanner les champs signature (false par défaut)") Boolean scanSignatureFields,
                                   @RequestParam(required = false) @Parameter(description = "Liste des destinataires finaux", example = "[email]") List<String> targetEmails,
                                   @RequestParam(required = false) @Parameter(description = "Emplacements finaux", example = "[smb://drive.univ-ville.fr/forms-archive/]") List<String> targetUrls,
                                   @RequestParam(required = false, defaultValue = "true") @Parameter(description = "Envoyer une alerte mail") Boolean sendEmailAlert,
                                   @RequestParam(required = false) @Parameter(description = "commentaire") String comment,
                                   @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey,
                                   @RequestParam(required = false) @Parameter(description = "Retour au format json (facultatif, false par défaut)") Boolean json,
                                   @RequestParam(required = false) @Parameter(description = "Paramètres de signature", example = "[{\"xPos\":100, \"yPos\":100, \"signPageNumber\":1}, {\"xPos\":200, \"yPos\":200, \"signPageNumber\":1}]") String signRequestParamsJsonString,
                                   @RequestParam(required = false) @Parameter(deprecated = true, description = "Liste des participants pour chaque étape", example = "[stepNumber*email] ou [stepNumber*email*phone]") List<String> recipientsEmails,
                                   @RequestParam(required = false) @Parameter(deprecated = true, description = "Liste des participants pour chaque étape (ancien nom)", example = "[stepNumber*email] ou [stepNumber*email*phone]") List<String> recipientEmails,
                                   @RequestParam(required = false) @Parameter(deprecated = true, description = "Lites des numéros d'étape pour lesquelles tous les participants doivent signer", example = "[stepNumber]") List<String> allSignToCompletes
                                   ) {
        logger.debug("init new workflow instance : " + id);
        logger.debug("stepsJsonString : " + stepsJsonString);
        if(json == null) {
            json = false;
        }
        if(createByEppn == null) {
            return ResponseEntity.badRequest().body("Required signRequestLight parameter 'createByEppn' for method parameter type String is not present");
        }
        if(recipientEmails == null && recipientsEmails != null && !recipientsEmails.isEmpty()) {
            recipientEmails = recipientsEmails;
        }
        List<WorkflowStepDto> steps = new ArrayList<>();
        if(stepsJsonString == null && recipientEmails != null) {
            steps = recipientService.convertRecipientEmailsToStep(recipientEmails);
        } else if(stepsJsonString != null) {
            steps = recipientService.convertStepsJsonStringToWorkflowStepDtos(stepsJsonString);
        }
        if (signRequestParamsJsonString != null) {
            List<SignRequestParamsWsDto> signRequestParamsWsDtos = userService.getSignRequestParamsWsDtosFromJson(signRequestParamsJsonString, "system");
            int i = 0;
            for(WorkflowStepDto step : steps) {
                if(signRequestParamsWsDtos.size() > i ) {
                    if(!step.getSignType().equals(SignType.hiddenVisa)) {
                        step.getSignRequestParams().add(signRequestParamsWsDtos.get(i));
                        i++;
                    }
                } else {
                    break;
                }
            }
        }
        try {
            List<Long> signRequestIds = signBookService.startWorkflow(id, multipartFiles, createByEppn, title, steps, targetEmails, targetUrls, scanSignatureFields, orderSignsByName, sendEmailAlert, comment);
            if(signRequestIds.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("-1");
            }
            signRequestService.addAttachement(attachementMultipartFiles, null, signRequestIds.get(0), createByEppn);
            if(json) {
                return ResponseEntity.ok(signRequestIds);
            } else {
                return ResponseEntity.ok(org.apache.commons.lang.StringUtils.join(signRequestIds, ","));
            }
        } catch (EsupSignatureException e) {
            logger.warn(e.getMessage() + " for : " + id);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @CrossOrigin
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Récupération d'un circuit",
            responses = @ApiResponse(description = "JsonDtoWorkflow", content = @Content(schema = @Schema(implementation = WorkflowDto.class))))
    @PreAuthorize("@wsAccessTokenService.isAllAccess(#xApiKey)")
    public ResponseEntity<String> get(@PathVariable String id,
                      @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey) throws JsonProcessingException {
        String json = workflowService.getByIdJson(id);
        if("null".equals(json)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(workflowService.getByIdJson(id));
    }

    @CrossOrigin
    @GetMapping(value = "/{id}/signrequests", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Récupération des demandes d'un circuit",
            responses = @ApiResponse(description = "JsonDtoWorkflow", content = @Content(schema = @Schema(implementation = WorkflowDto.class))))
    @PreAuthorize("@wsAccessTokenService.createWorkflowAccess(#id, #xApiKey)")
    public String getSignRequests(@PathVariable String id, @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey) throws JsonProcessingException {
        return workflowService.getSignRequestById(id);
    }

    @CrossOrigin
    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Récupération de la liste des circuits disponibles", responses = @ApiResponse(description = "List<JsonDtoWorkflow>", content = @Content(schema = @Schema(implementation = List.class))))
    @PreAuthorize("@wsAccessTokenService.isAllAccess(#xApiKey)")
    public String getAll(@ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey) throws JsonProcessingException {
        return workflowService.getAllWorkflowsJson();
    }

    @CrossOrigin
    @GetMapping(value = "/get-datas/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Récupération des données d'un formulaire")
    @PreAuthorize("@wsAccessTokenService.createWorkflowAccess(#id, #xApiKey)")
    public LinkedHashMap<String, String> getDatas(@PathVariable String id, @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey) {
        return workflowExportService.getJsonDatasFromWorkflow(id);
    }

    @CrossOrigin
    @GetMapping(value = "/{id}/datas/csv", produces = {"text/csv", "*/*"})
    @PreAuthorize("@wsAccessTokenService.workflowCsv(#id, #xApiKey)")
    public ResponseEntity<Void> getWorkflowDatasCsv(@PathVariable String id, @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey, HttpServletResponse response) {
        Workflow workflow = workflowService.getByIdOrToken(id);
        try {
            response.setContentType("text/csv; charset=utf-8");
            response.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode(workflow.getName().replace(" ", "-"), StandardCharsets.UTF_8.toString()) + ".csv");
            response.getOutputStream().write(EXCEL_UTF8_HACK);
            InputStream csvInputStream = workflowExportService.getCsvDatasFromWorkflow(Collections.singletonList(workflow));
            IOUtils.copy(csvInputStream, response.getOutputStream());
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            logger.error("get file error", e);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @CrossOrigin
    @PostMapping(value = "/{id}/new-from-paperless")
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Création d'une demande de signature à partir d'un document stocké dans Paperless-ngx")
    @PreAuthorize("@wsAccessTokenService.createWorkflowAccess(#id, #xApiKey)")
    public ResponseEntity<?> startFromPaperless(
            @PathVariable String id,
            @RequestParam @Parameter(description = "ID du document dans Paperless-ngx") Long paperlessDocumentId,
            @RequestParam(required = false) @Parameter(description = "Paramètres des étapes (objet json)") String stepsJsonString,
            @RequestParam(required = false) @Parameter(description = "EPPN du créateur/propriétaire de la demande") String createByEppn,
            @RequestParam(required = false) @Parameter(description = "Titre (facultatif, sinon le nom du fichier Paperless est utilisé)") String title,
            @RequestParam(required = false) @Parameter(description = "Scanner les champs signature (false par défaut)") Boolean scanSignatureFields,
            @RequestParam(required = false, defaultValue = "false") @Parameter(description = "Trier les champs signature par leurs noms") Boolean orderSignsByName,
            @RequestParam(required = false) @Parameter(description = "Liste des destinataires finaux", example = "[email]") List<String> targetEmails,
            @RequestParam(required = false) @Parameter(description = "Emplacements finaux") List<String> targetUrls,
            @RequestParam(required = false, defaultValue = "true") @Parameter(description = "Envoyer une alerte mail") Boolean sendEmailAlert,
            @RequestParam(required = false) @Parameter(description = "Commentaire") String comment,
            @RequestParam(required = false) @Parameter(description = "Retour au format json (false par défaut)") Boolean json,
            @RequestParam(required = false) @Parameter(description = "Liste des participants (ancien format)") List<String> recipientEmails,
            @RequestParam(required = false) @Parameter(description = "Tags à associer à la demande. Format : Groupe/Tag (ex: Type/Attestation). Créés automatiquement si inexistants. Plusieurs valeurs possibles.") List<String> tags,
            @RequestParam(required = false) @Parameter(description = "IDs Paperless de documents à joindre en pièces jointes (plusieurs valeurs possibles).") List<Long> attachmentPaperlessIds,
            @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey) {

        if (paperlessService == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE).body("Paperless-ngx non disponible");
        }
        if (createByEppn == null) {
            return ResponseEntity.badRequest().body("createByEppn is required");
        }
        if (!paperlessService.isConfigured()) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE).body("Paperless-ngx non configuré dans l'administration");
        }
        if (json == null) json = false;
        if (scanSignatureFields == null) scanSignatureFields = false;
        if (orderSignsByName == null) orderSignsByName = false;

        org.springframework.web.multipart.MultipartFile paperlessFile;
        try {
            paperlessFile = paperlessService.fetchDocument(paperlessDocumentId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error fetching document from Paperless id={}", paperlessDocumentId, e);
            return ResponseEntity.internalServerError().body("Erreur lors du téléchargement depuis Paperless : " + e.getMessage());
        }

        if (title == null || title.isBlank()) {
            title = paperlessFile.getOriginalFilename();
        }

        java.util.List<org.esupportail.esupsignature.dto.ws.WorkflowStepDto> steps = new java.util.ArrayList<>();
        if (stepsJsonString == null && recipientEmails != null) {
            steps = recipientService.convertRecipientEmailsToStep(recipientEmails);
        } else if (stepsJsonString != null) {
            steps = recipientService.convertRecipientJsonStringToWorkflowStepDtos(stepsJsonString);
        }

        try {
            java.util.List<Long> signRequestIds = signBookService.startWorkflow(id, new org.springframework.web.multipart.MultipartFile[]{paperlessFile}, createByEppn, title, steps, targetEmails, targetUrls, scanSignatureFields, orderSignsByName, sendEmailAlert, comment);
            if (signRequestIds.isEmpty()) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST).body("-1");
            }
            signRequestService.setPaperlessSourceDocumentId(signRequestIds, paperlessDocumentId);
            if (tags != null && !tags.isEmpty()) {
                Long signBookId = signRequestService.getById(signRequestIds.get(0)).getParentSignBook().getId();
                signBookService.addTagsByPath(signBookId, tags);
            }
            if (attachmentPaperlessIds != null && !attachmentPaperlessIds.isEmpty()) {
                Long signRequestId = signRequestIds.get(0);
                for (Long attachId : attachmentPaperlessIds) {
                    try {
                        org.springframework.web.multipart.MultipartFile attachFile = paperlessService.fetchDocument(attachId);
                        signRequestService.addAttachement(new org.springframework.web.multipart.MultipartFile[]{attachFile}, null, signRequestId, createByEppn);
                    } catch (Exception e) {
                        logger.warn("Impossible de joindre le document Paperless id={} : {}", attachId, e.getMessage());
                    }
                }
            }
            if (json) {
                return ResponseEntity.ok(signRequestIds);
            } else {
                return ResponseEntity.ok(org.apache.commons.lang.StringUtils.join(signRequestIds, ","));
            }
        } catch (Exception e) {
            logger.warn("{} for workflow: {}", e.getMessage(), id);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}

