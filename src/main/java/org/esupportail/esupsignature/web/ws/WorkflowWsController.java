package org.esupportail.esupsignature.web.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.Resource;
import org.esupportail.esupsignature.dto.WorkflowDto;
import org.esupportail.esupsignature.dto.WorkflowStepDto;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.RecipientService;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestParamsService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/ws/workflows")
public class WorkflowWsController {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowWsController.class);

    @Resource
    private WorkflowService workflowService;

    @Resource
    private SignBookService signBookService;

    @Resource
    private SignRequestParamsService signRequestParamsService;

    @Resource
    private RecipientService recipientService;

    @CrossOrigin
    @PostMapping(value = "/{id}/new", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Dépôt d'un document dans une nouvelle instance d'un circuit")
    @PreAuthorize("@wsAccessTokenService.createWorkflowAccess(#id, #xApiKey)")
    public ResponseEntity<?> start(@PathVariable Long id,
                                   @RequestParam @Parameter(description = "Multipart stream du fichier à signer") MultipartFile[] multipartFiles,
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
        if(json == null) {
            json = false;
        }
        if(createByEppn == null) {
            throw new EsupSignatureRuntimeException("Required request parameter 'createByEppn' for method parameter type String is not present");
        }
        if(recipientEmails == null && recipientsEmails != null && !recipientsEmails.isEmpty()) {
            recipientEmails = recipientsEmails;
        }
        List<WorkflowStepDto> steps = new ArrayList<>();
        if(stepsJsonString == null && recipientEmails != null) {
            steps = recipientService.convertRecipientEmailsToStep(recipientEmails);
        } else if(stepsJsonString != null) {
            steps = recipientService.convertRecipientJsonStringToWorkflowStepDtos(stepsJsonString);
        }
        List<SignRequestParams> signRequestParamses = new ArrayList<>();
        if (signRequestParamsJsonString != null) {
            signRequestParamses = signRequestParamsService.getSignRequestParamsesFromJson(signRequestParamsJsonString);
        }
        try {
            List<Long> signRequestIds = signBookService.startWorkflow(id, multipartFiles, createByEppn, title, steps, targetEmails, targetUrls, signRequestParamses, scanSignatureFields, sendEmailAlert, comment);
            if(json) {
                return ResponseEntity.ok(signRequestIds);
            } else {
                return ResponseEntity.ok(org.apache.commons.lang.StringUtils.join(signRequestIds, ","));
            }
        } catch (EsupSignatureRuntimeException e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.ok("-1");
        }
    }

    @CrossOrigin
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Récupération d'un circuit",
            responses = @ApiResponse(description = "JsonDtoWorkflow", content = @Content(schema = @Schema(implementation = WorkflowDto.class))))
    @PreAuthorize("@wsAccessTokenService.isAllAccess(#xApiKey)")
    public String get(@PathVariable Long id,
                      @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey) throws JsonProcessingException {
        return workflowService.getByIdJson(id);
    }

    @CrossOrigin
    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Récupération de la liste des circuits disponibles", responses = @ApiResponse(description = "List<JsonDtoWorkflow>", content = @Content(schema = @Schema(implementation = List.class))))
    @PreAuthorize("@wsAccessTokenService.isAllAccess(#xApiKey)")
    public String getAll(@ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey) throws JsonProcessingException {
        return workflowService.getAllWorkflowsJson();
    }

}
