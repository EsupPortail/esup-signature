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
import org.esupportail.esupsignature.dto.ui.global.UiMessageDto;
import org.esupportail.esupsignature.dto.ws.RecipientWsDto;
import org.esupportail.esupsignature.dto.ws.SignRequestStepsWsDto;
import org.esupportail.esupsignature.dto.ws.WorkflowStepDto;
import org.esupportail.esupsignature.entity.Action;
import org.esupportail.esupsignature.entity.AuditTrail;
import org.esupportail.esupsignature.entity.Recipient;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.entity.enums.ActionType;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.service.interfaces.fs.FsFile;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureMailException;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ws/signrequests")
public class SignRequestWsController {

    private static final Logger logger = LoggerFactory.getLogger(SignRequestWsController.class);

    private final SignRequestService signRequestService;

    private final RecipientService recipientService;

    private final SignBookService signBookService;

    private final SignRequestParamsService signRequestParamsService;
    private final LiveWorkflowStepService liveWorkflowStepService;
    private final PaperlessService paperlessService;

    public SignRequestWsController(SignRequestService signRequestService, RecipientService recipientService, SignBookService signBookService, SignRequestParamsService signRequestParamsService, LiveWorkflowStepService liveWorkflowStepService, PaperlessService paperlessService) {
        this.signRequestService = signRequestService;
        this.recipientService = recipientService;
        this.signBookService = signBookService;
        this.signRequestParamsService = signRequestParamsService;
        this.liveWorkflowStepService = liveWorkflowStepService;
        this.paperlessService = paperlessService;
    }

    @CrossOrigin
    @PostMapping(value ="/scan", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Recupération des paramètres de signature du documents")
    @PreAuthorize("@wsAccessTokenService.isAllAccess(#xApiKey)")
    public ResponseEntity<List<SignRequestParams>> getSignRequestParams(@Parameter(description = "Multipart stream du fichier à signer") @RequestParam MultipartFile[] multipartFiles,
                                                                        @RequestParam(required = false, defaultValue = "false") @Parameter(description = "Trier les champs signature par leurs noms") Boolean orderSignsByName,
                                                                        @RequestParam(required = false) @Parameter(description = "Pattern de détéction d'emplacement") String signRequestParamsDetectionPattern,
                                                                        @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey
                                                                        ) throws IOException {
        return ResponseEntity.ok().body(signRequestParamsService.scanSignatureFields(multipartFiles[0].getInputStream(), null, 1, signRequestParamsDetectionPattern, false, orderSignsByName));
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
                                                      "pdSignatureFieldName": "string",
                                                      "signPageNumber": 1,
                                                      "signDocumentNumber": 0,
                                                      "signWidth": 200,
                                                      "signHeight": 100,
                                                      "xPos": 0,
                                                      "yPos": 0
                                                    }
                                                  ],
                                                  "convertToPDFA": true,
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
                                    @RequestParam(required = false) @Parameter(description = "Pattern de détéction d'emplacement") String signRequestParamsDetectionPattern,
                                    @RequestParam(required = false) @Parameter(description = "Conserve les champs de signature") Boolean keepSignFields,
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
                                    @RequestParam(required = false) @Parameter(deprecated = true, description = "Type de signature", schema = @Schema(allowableValues = {"visa", "hiddenVisa", "signature"}), examples = {@ExampleObject(value = "visa"), @ExampleObject(value = "signature")}) String signType,
                                    @RequestParam(required = false) @Parameter(deprecated = true, description = "EPPN du créateur/propriétaire de la demande (ancien nom)") String eppn,
                                    @RequestParam(required = false) @Parameter(description = "Tags à associer à la demande. Format : Groupe/Tag (ex: Type/Attestation). Le groupe et le tag sont créés automatiquement s'ils n'existent pas. Plusieurs valeurs possibles (un par paramètre tags).") List<String> tags
                       ) throws EsupSignatureException {
        if(json == null) {
            json = false;
        }
        if(createByEppn == null && StringUtils.hasText(eppn)) {
            createByEppn = eppn;
        }
        if(createByEppn == null) {
            throw new EsupSignatureRuntimeException("Required signRequestLight parameter 'createByEppn' for method parameter type String is not present");
        }
        if(recipientEmails == null && recipientsEmails != null && !recipientsEmails.isEmpty()) {
            recipientEmails = recipientsEmails;
        }
        if(keepSignFields == null) {
            keepSignFields = false;
        }
        List<WorkflowStepDto> workflowStepDtos;
        if(stepsJsonString == null && recipientEmails != null) {
            workflowStepDtos = recipientService.convertRecipientEmailsToStep(recipientEmails);
            workflowStepDtos.forEach(workflowStepDto -> {
                workflowStepDto.setSignType(SignType.fromString(signType));
                workflowStepDto.setAllSignToComplete(allSignToComplete);
                workflowStepDto.setUserSignFirst(userSignFirst);
                workflowStepDto.setComment(comment);
                workflowStepDto.setRecipientsCCEmails(recipientsCCEmails);
            });
        } else {
            workflowStepDtos = recipientService.convertStepsJsonStringToWorkflowStepDtos(stepsJsonString);
        }
        if(workflowStepDtos != null) {
            Map<SignBook, String> signBookStringMap = signBookService.createAndSendSignBook(title, multipartFiles, pending, workflowStepDtos, createByEppn, true, forceAllSign, targetUrl, signRequestParamsDetectionPattern, keepSignFields);
            List<String> signRequestIds = signBookStringMap.keySet().stream().flatMap(sb -> sb.getSignRequests().stream().map(signRequest -> signRequest.getId().toString())).toList();
            if(signRequestIds.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("-1");
            }
            // Attacher les tags hiérarchiques si fournis
            if (tags != null && !tags.isEmpty()) {
                Long signBookId = signBookStringMap.keySet().iterator().next().getId();
                signBookService.addTagsByPath(signBookId, tags);
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
    @PostMapping(value = "/replay-notif/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Relance une demande de signature",
            responses = @ApiResponse(description = "SignRequest", content = @Content(schema = @Schema(implementation = SignRequest.class))))
    @PreAuthorize("@wsAccessTokenService.readWorkflowAccess(#id, #xApiKey)")
    public Boolean replayNotif(@PathVariable("id") Long id, @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey) throws EsupSignatureMailException {
        return signRequestService.replayNotif(id, "system");
    }

    @CrossOrigin
    @GetMapping(value = {"/{id}/steps", "/steps/{id}" }, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Récupération des étapes d'une demande",
            responses = @ApiResponse(description = "SignRequest", content = @Content(schema = @Schema(implementation = SignRequest.class))))
    @PreAuthorize("@wsAccessTokenService.readWorkflowAccess(#id, #xApiKey)")
    public List<SignRequestStepsWsDto> getSteps(@Parameter(description = "Identifiant de la demande", required = true) @PathVariable Long id,
                                                @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey) throws JsonProcessingException {
        return signRequestService.getStepsDto(id);
    }

    @CrossOrigin
    @GetMapping(value = {"/{id}/status", "/status/{id}"})
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Récupération du statut d'une demande de signature")
    @PreAuthorize("@wsAccessTokenService.readWorkflowAccess(#id, #xApiKey)")
    public String getStatus(@Parameter(description = "Identifiant de la demande") @PathVariable Long id,
                            @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey) {
        return signRequestService.getStatus(id);
    }

    @CrossOrigin
    @GetMapping(value = {"/{id}/audit-trail", "/audit-trail/{id}"})
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Récupération du dossier de preuve de la demande", responses = @ApiResponse(description = "AuditTrail", content = @Content(schema = @Schema(implementation = AuditTrail.class))))
    @PreAuthorize("@wsAccessTokenService.readWorkflowAccess(#id, #xApiKey)")
    public String getAuditTail(@Parameter(description = "Dossier de preuve de la demande") @PathVariable Long id,
                               @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey) throws JsonProcessingException {
        return signRequestService.getAuditTrailJson(id);
    }

    @CrossOrigin
    @PostMapping(value = {"/{id}/update-recipients", "/update-recipients/{id}"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Modifier les destinataires d'une étape de demande de signature")
    @PreAuthorize("@wsAccessTokenService.updateWorkflowAccess(#id, #xApiKey)")
    public ResponseEntity<String> updateRecipients(@PathVariable Long id,
                                                   @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey,
                                                   @RequestPart("recipientWsDtosString") String recipientWsDtosString,
                                                   @RequestParam Integer stepNumber) throws JsonProcessingException, EsupSignatureException {
        List<RecipientWsDto> recipientWsDtos = new ObjectMapper().readValue(recipientWsDtosString, new TypeReference<List<RecipientWsDto>>() {});
        SignRequest signRequest = signRequestService.getById(id);
        liveWorkflowStepService.replaceRecipientsToWorkflowStep(signRequest.getParentSignBook().getId(), stepNumber, recipientWsDtos);
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
        signRequestService.getToSignFileResponse(id, "inline", httpServletResponse, false);
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

    @CrossOrigin
    @PostMapping(value = "/{id}/attach-paperless")
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Télécharge un document Paperless et le joint comme pièce jointe à une demande de signature")
    @PreAuthorize("@wsAccessTokenService.updateWorkflowAccess(#id, #xApiKey)")
    public ResponseEntity<?> attachPaperlessDocument(
            @PathVariable("id") Long id,
            @RequestParam("paperlessDocumentId") @Parameter(description = "ID du document Paperless à joindre", required = true) Long paperlessDocumentId,
            @RequestParam("createByEppn") @Parameter(description = "EPPN du créateur de la demande (utilisé pour la vérification des droits d'édition)", required = true) String createByEppn,
            @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey) {
        if (!paperlessService.isConfigured()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Paperless-ngx non configuré dans l'administration");
        }
        SignRequest signRequest = signRequestService.getById(id);
        if (signRequest == null) {
            return ResponseEntity.notFound().build();
        }
        MultipartFile paperlessFile;
        try {
            paperlessFile = paperlessService.fetchDocument(paperlessDocumentId);
        } catch (Exception e) {
            logger.error("Impossible de récupérer le document Paperless id={}", paperlessDocumentId, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Erreur Paperless : " + e.getMessage());
        }
        try {
            boolean added = signRequestService.addAttachement(new MultipartFile[]{paperlessFile}, null, id, createByEppn);
            if (!added) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Droits insuffisants pour ajouter une pièce jointe à cette demande (createByEppn incorrect ou demande non éditable)");
            }
        } catch (EsupSignatureIOException e) {
            logger.error("Impossible de joindre le document Paperless id={} à la demande id={}", paperlessDocumentId, id, e);
            return ResponseEntity.internalServerError().body("Erreur lors de l'ajout de la pièce jointe : " + e.getMessage());
        }
        logger.info("Document Paperless id={} joint à la demande id={}", paperlessDocumentId, id);
        return ResponseEntity.ok(Map.of(
                "signRequestId", id,
                "paperlessDocumentId", paperlessDocumentId,
                "filename", paperlessFile.getOriginalFilename()
        ));
    }

    @GetMapping(value = "/return-test")
    @PreAuthorize("@wsAccessTokenService.isAllAccess(#xApiKey)")
    public ResponseEntity<Void> returnTest(@RequestParam("signRequestId") String signRequestId, @RequestParam("status") String status, @RequestParam("step") String step,
                                           @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey) {
        logger.info(signRequestId + ", " + status + ", " + step);
        return ResponseEntity.ok().build();
    }

    @CrossOrigin
    @PostMapping(value = "/{id}/clean-and-link")
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Nettoyer les documents de la demande de signature et associer un lien externe (Paperless)")
    @PreAuthorize("@wsAccessTokenService.updateWorkflowAccess(#id, #xApiKey)")
    public ResponseEntity<Void> cleanAndLink(@PathVariable("id") Long id,
                                             @RequestParam("externalUrl") String externalUrl,
                                             @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey) {
        signRequestService.cleanAndLink(id, externalUrl, "system");
        return ResponseEntity.ok().build();
    }

    @CrossOrigin
    @PostMapping(value = "/{id}/archive-to-paperless")
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Uploade le document signé dans Paperless en reprenant les métadonnées du document source, puis nettoie les fichiers locaux. Retourne l'ID Paperless du document signé.")
    @PreAuthorize("@wsAccessTokenService.updateWorkflowAccess(#id, #xApiKey)")
    public ResponseEntity<?> archiveToPaperless(@PathVariable("id") Long id,
                                                @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey) {
        if (!paperlessService.isConfigured()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Paperless-ngx non configuré dans l'administration");
        }

        SignRequest signRequest = signRequestService.getById(id);
        if (signRequest == null) {
            return ResponseEntity.notFound().build();
        }

        Long sourceDocumentId = signRequest.getPaperlessSourceDocumentId();
        if (sourceDocumentId == null) {
            return ResponseEntity.badRequest().body("Cette demande n'est pas associée à un document Paperless source");
        }

        FsFile signedFsFile;
        try {
            signedFsFile = signRequestService.getLastSignedFsFile(signRequest);
        } catch (Exception e) {
            logger.error("Impossible de récupérer le document signé pour signRequest={}", id, e);
            return ResponseEntity.internalServerError().body("Impossible de récupérer le document signé : " + e.getMessage());
        }

        if (signedFsFile == null) {
            return ResponseEntity.badRequest().body("Aucun document signé disponible pour cette demande");
        }

        String signataires = signRequest.getRecipientHasSigned().entrySet().stream()
                .filter(e -> e.getValue() != null && !ActionType.none.equals(e.getValue().getActionType()))
                .map(e -> e.getKey().getUser().getEmail())
                .collect(Collectors.joining(", "));

        byte[] pdfBytes;
        try {
            pdfBytes = signedFsFile.getInputStream().readAllBytes();
        } catch (Exception e) {
            logger.error("Impossible de lire le PDF signé pour signRequest={}", id, e);
            return ResponseEntity.internalServerError().body("Erreur lecture PDF signé : " + e.getMessage());
        }

        String filename = StringUtils.hasText(signedFsFile.getName()) ? signedFsFile.getName() : "document-signe.pdf";

        Long newPaperlessId;
        try {
            newPaperlessId = paperlessService.uploadSignedDocument(sourceDocumentId, pdfBytes, filename, id, signataires);
        } catch (Exception e) {
            logger.error("Échec upload Paperless pour signRequest={}", id, e);
            return ResponseEntity.internalServerError().body("Échec upload Paperless : " + e.getMessage());
        }

        String downloadUrl = paperlessService.getPaperlessUrl().replaceAll("/$", "")
                + "/api/documents/" + newPaperlessId + "/download/";
        signRequestService.cleanAndLinkWithPaperlessId(id, downloadUrl, newPaperlessId, "system");

        logger.info("signRequest={} archivé dans Paperless id={}", id, newPaperlessId);
        return ResponseEntity.ok(Map.of(
                "paperlessDocumentId", newPaperlessId,
                "paperlessUrl", downloadUrl,
                "signRequestId", id
        ));
    }

}
