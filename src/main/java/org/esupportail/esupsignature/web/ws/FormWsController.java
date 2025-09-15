package org.esupportail.esupsignature.web.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.esupportail.esupsignature.dto.json.RecipientWsDto;
import org.esupportail.esupsignature.dto.json.SignRequestParamsWsDto;
import org.esupportail.esupsignature.dto.json.WorkflowDto;
import org.esupportail.esupsignature.dto.json.WorkflowStepDto;
import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.repository.SignRequestParamsRepository;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.export.DataExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ws/forms")
public class FormWsController {

    private static final Logger logger = LoggerFactory.getLogger(FormWsController.class);

    private final DataService dataService;

    private final UserService userService;

    private final FormService formService;

    private final RecipientService recipientService;

    private final SignBookService signBookService;

    private final DataExportService dataExportService;

    private final SignRequestService signRequestService;

    private final ObjectMapper objectMapper;
    private final SignRequestParamsRepository signRequestParamsRepository;

    public FormWsController(DataService dataService, UserService userService, FormService formService, RecipientService recipientService, SignBookService signBookService, DataExportService dataExportService, SignRequestService signRequestService, ObjectMapper objectMapper, SignRequestParamsRepository signRequestParamsRepository) {
        this.dataService = dataService;
        this.userService = userService;
        this.formService = formService;
        this.recipientService = recipientService;
        this.signBookService = signBookService;
        this.dataExportService = dataExportService;
        this.signRequestService = signRequestService;
        this.objectMapper = objectMapper;
        this.signRequestParamsRepository = signRequestParamsRepository;
    }

    @CrossOrigin
    @PostMapping(value = "/{id}/new")
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Création d'une nouvelle instance d'un formulaire")
    @PreAuthorize("@wsAccessTokenService.isAllAccess(#xApiKey)")
    public ResponseEntity<?> start(@PathVariable Long id,
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
                                   @RequestParam(required = false) @Parameter(description = "Liste des personnes en copie (emails). Ne prend pas en charge les groupes")  List<String> recipientsCCEmails,
                                   @RequestParam(required = false) @Parameter(description = "Liste des destinataires finaux (emails)", example = "[email]") List<String> targetEmails,
                                   @RequestParam(required = false) @Parameter(description = "Emplacements finaux", example = "[smb://drive.univ-ville.fr/forms-archive/]") List<String> targetUrls,
                                   @RequestParam(required = false) @Parameter(description = "Données par défaut à remplir dans le formulaire", example = "{'field1' : 'toto, 'field2' : 'tata'}") String formDatas,
                                   @RequestParam(required = false, defaultValue = "true") @Parameter(description = "Envoyer une alerte mail") Boolean sendEmailAlert,
                                   @RequestParam(required = false) @Parameter(description = "commentaire") String comment,
                                   @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey,
                                   @RequestParam(required = false) @Parameter(description = "Retour au format json (facultatif, false par défaut)") Boolean json,
                                   @RequestParam(required = false) @Parameter(deprecated = true, description = "Liste des participants pour chaque étape (ancien nom)", example = "[stepNumber*email] ou [stepNumber*email*phone]") List<String> recipientEmails,
                                   @RequestParam(required = false) @Parameter(deprecated = true, description = "Liste des participants pour chaque étape", example = "[stepNumber*email] ou [stepNumber*email*phone]") List<String> recipientsEmails,
                                   @RequestParam(required = false) @Parameter(deprecated = true, description = "Liste des types de signature pour chaque étape", example = "[stepNumber*signTypes]") List<String> signTypes,
                                   @RequestParam(required = false) @Parameter(deprecated = true, description = "Lites des numéros d'étape pour lesquelles tous les participants doivent signer", example = "[stepNumber]") List<String> allSignToCompletes,
                                   @RequestParam(required = false) @Parameter(deprecated = true, description = "Paramètres de signature", example = "[{\"xPos\":100, \"yPos\":100, \"signPageNumber\":1}, {\"xPos\":200, \"yPos\":200, \"signPageNumber\":1}]") String signRequestParamsJsonString,
                                   @RequestParam(required = false) @Parameter(deprecated = true, description = "Eppn du propriétaire du futur document (ancien nom)") String eppn


    ) throws JsonProcessingException {
        logger.debug("init new form instance : " + id);
        if(json == null) {
            json = false;
        }
        if(recipientEmails == null && recipientsEmails != null && !recipientsEmails.isEmpty()) {
            recipientEmails = recipientsEmails;
        }
        if(stepsJsonString == null && recipientEmails != null) {
            stepsJsonString = objectMapper.writeValueAsString(recipientService.convertRecipientEmailsToStep(recipientEmails));
        }
        if(createByEppn == null && eppn != null && !eppn.isEmpty()) {
            createByEppn = eppn;
        }
        if(createByEppn == null) {
            return ResponseEntity.badRequest().body("Required request parameter 'createByEppn' for method parameter type String is not present");
        }
        Data data = dataService.addData(id, createByEppn);
        TypeReference<Map<String, String>> type = new TypeReference<>(){};
        Map<String, String> datas = new HashMap<>();
        if(formDatas != null) {
            datas = objectMapper.readValue(formDatas, type);
        }
        List<WorkflowStepDto> workflowStepDtos = recipientService.convertRecipientJsonStringToWorkflowStepDtos(stepsJsonString);
        if (signRequestParamsJsonString != null) {
            List<SignRequestParamsWsDto> signRequestParamsWsDtos = userService.getSignRequestParamsWsDtosFromJson(signRequestParamsJsonString, "system");
            for(WorkflowStepDto workflowStepDto : workflowStepDtos) {
                workflowStepDto.setSignRequestParams(signRequestParamsWsDtos);
            }
        }
        SignBook signBook = signBookService.sendForSign(data.getId(), workflowStepDtos, targetEmails, targetUrls, createByEppn, createByEppn, true, datas, null, title, sendEmailAlert, comment);
        signBookService.addViewers(signBook.getId(), recipientsCCEmails);
        if(json) {
            return ResponseEntity.ok(signBook.getSignRequests().get(0).getId());
        } else {
            return ResponseEntity.ok(signBook.getSignRequests().get(0).getId().toString());
        }
    }

    @CrossOrigin
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Récupération d'un circuit", responses = @ApiResponse(description = "JsonDtoWorkflow", content = @Content(schema = @Schema(implementation = WorkflowDto.class))))
    @PreAuthorize("@wsAccessTokenService.isAllAccess(#xApiKey)")
    public String get(@PathVariable Long id, @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey) throws JsonProcessingException {
        return formService.getByIdJson(id);
    }

    @CrossOrigin
    @PostMapping(value = "/{id}/new-doc", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Création d'une nouvelle instance d'un formulaire")
    @PreAuthorize("@wsAccessTokenService.isAllAccess(#xApiKey)")
    public ResponseEntity<?> startWithDoc(@PathVariable Long id,
                                          @RequestParam @Parameter(description = "Multipart stream du fichier à signer") MultipartFile[] multipartFiles,
                                          @RequestParam @Parameter(description = "Eppn du propriétaire du futur document") String createByEppn,
                                          @RequestParam(required = false) @Parameter(description = "Multipart stream des pièces jointes") MultipartFile[] attachementMultipartFiles,
                                          @RequestParam(required = false) @Parameter(description = "Liste des participants pour chaque étape (objet json)", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = RecipientWsDto.class)))) List<WorkflowStepDto> steps,
                                          @RequestParam(required = false) @Parameter(description = "Liste des participants pour chaque étape (ancien nom)", example = "[stepNumber*email]") List<String> recipientEmails,
                                          @RequestParam(required = false) @Parameter(description = "Liste des participants pour chaque étape", example = "[stepNumber*email]") List<String> recipientsEmails,
                                          @RequestParam(required = false) @Parameter(description = "Liste des types de signature pour chaque étape", example = "[stepNumber*signTypes]") List<String> signTypes,
                                          @RequestParam(required = false) @Parameter(description = "Lites des numéros d'étape pour lesquelles tous les participants doivent signer", example = "[stepNumber]") List<String> allSignToCompletes,
                                          @RequestParam(required = false) @Parameter(description = "Liste des destinataires finaux", example = "[email]") List<String> targetEmails,
                                          @RequestParam(required = false) @Parameter(description = "Emplacements finaux", example = "[smb://drive.univ-ville.fr/forms-archive/]") List<String> targetUrls,
                                          @RequestParam(required = false) @Parameter(description = "Données par défaut à remplir dans le formulaire", example = "{'field1' : 'toto, 'field2' : 'tata'}") String formDatas,
                                          @RequestParam(required = false) @Parameter(description = "Titre") String title,
                                          @RequestParam(required = false, defaultValue = "true") @Parameter(description = "Envoyer une alerte mail") Boolean sendEmailAlert,
                                          @RequestParam(required = false) @Parameter(description = "Retour au format json (facultatif, false par défaut)") Boolean json,
                                          @RequestParam(required = false) @Parameter(description = "commentaire") String comment,
                                          @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey
    ) throws IOException {
        if(json == null) {
            json = false;
        }
        if(recipientEmails == null && !recipientsEmails.isEmpty()) {
            recipientEmails = recipientsEmails;
        }
        if(steps == null && recipientEmails != null) {
            steps = recipientService.convertRecipientEmailsToStep(recipientEmails);
        }
        Data data = dataService.addData(id, createByEppn);
        TypeReference<Map<String, String>> type = new TypeReference<>(){};
        Map<String, String> datas = new HashMap<>();
        if(formDatas != null) {
            datas.putAll(objectMapper.readValue(formDatas, type));
        }
        SignBook signBook = signBookService.sendForSign(data.getId(), steps, targetEmails, targetUrls, createByEppn, createByEppn, true, datas, multipartFiles[0].getInputStream(), title, sendEmailAlert, comment);
        signRequestService.addAttachement(attachementMultipartFiles, null, signBook.getSignRequests().get(0).getId(), createByEppn);
        if(json) {
            return ResponseEntity.ok(signBook.getSignRequests().get(0).getId());
        } else {
            return ResponseEntity.ok(signBook.getSignRequests().get(0).getId().toString());
        }
    }

    @CrossOrigin
    @Deprecated
    @PostMapping(value = "/get-datas/{id}")
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Récupération des données d'un formulaire (POST)", deprecated = true)
    @PreAuthorize("@wsAccessTokenService.isAllAccess(#xApiKey)")
    public LinkedHashMap<String, String> postGetDatas(@PathVariable Long id, @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey) {
        return dataExportService.getJsonDatasFromSignRequest(id);
    }

    @CrossOrigin
    @GetMapping(value = "/get-datas/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Récupération des données d'un formulaire")
    @PreAuthorize("@wsAccessTokenService.isAllAccess(#xApiKey)")
    public LinkedHashMap<String, String> getDatas(@PathVariable Long id, @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey) {
        return dataExportService.getJsonDatasFromSignRequest(id);
    }

    @CrossOrigin
    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Récupération de la liste des formulaires disponibles", responses = @ApiResponse(description = "List<JsonDtoWorkflow>", content = @Content(schema = @Schema(implementation = List.class))))
    @PreAuthorize("@wsAccessTokenService.isAllAccess(#xApiKey)")
    public String getAll(@ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey) throws JsonProcessingException {
        return formService.getAllFormsJson();
    }
}
