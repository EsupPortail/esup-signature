package org.esupportail.esupsignature.web.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.DataService;
import org.esupportail.esupsignature.service.FormService;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.export.DataExportService;
import org.esupportail.esupsignature.web.ws.json.JsonDtoWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ws/forms")
public class FormWsController {

    private static final Logger logger = LoggerFactory.getLogger(FormWsController.class);

    @Resource
    private DataService dataService;

    @Resource
    private FormService formService;

    @Resource
    private SignBookService signBookService;

    @Resource
    private DataExportService dataExportService;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private ObjectMapper objectMapper;

    @CrossOrigin
    @PostMapping(value = "/{id}/new")
    @Operation(description = "Création d'une nouvelle instance d'un formulaire")
    public Long start(@PathVariable Long id,
                      @RequestParam(required = false) @Parameter(description = "Eppn du propriétaire du futur document (ancien nom)") String eppn,
                      @RequestParam(required = false) @Parameter(description = "Eppn du propriétaire du futur document : eppn ou createByEppn requis") String createByEppn,
                      @RequestParam(required = false) @Parameter(description = "Liste des participants pour chaque étape (ancien nom)", example = "[stepNumber*email]") List<String> recipientEmails,
                      @RequestParam(required = false) @Parameter(description = "Liste des participants pour chaque étape", example = "[stepNumber*email]") List<String> recipientsEmails,
                      @RequestParam(required = false) @Parameter(description = "Liste des personnes en copie (emails). Ne prend pas en charge les groupes")  List<String> recipientsCCEmails,
                      @RequestParam(required = false) @Parameter(description = "Liste des types de signature pour chaque étape", example = "[stepNumber*signTypes]") List<String> signTypes,
                      @RequestParam(required = false) @Parameter(description = "Lites des numéros d'étape pour lesquelles tous les participants doivent signer", example = "[stepNumber]") List<String> allSignToCompletes,
                      @RequestParam(required = false) @Parameter(description = "Liste des destinataires finaux", example = "[email]") List<String> targetEmails,
                      @RequestParam(required = false) @Parameter(description = "Paramètres de signature", example = "[{\"xPos\":100, \"yPos\":100, \"signPageNumber\":1}, {\"xPos\":200, \"yPos\":200, \"signPageNumber\":1}]") String signRequestParamsJsonString,
                      @RequestParam(required = false) @Parameter(description = "Emplacements finaux", example = "[smb://drive.univ-ville.fr/forms-archive/]") List<String> targetUrls,
                      @RequestParam(required = false) @Parameter(description = "Données par défaut à remplir dans le formulaire", example = "{'field1' : 'toto, 'field2' : 'tata'}") String formDatas,
                      @RequestParam(required = false) @Parameter(description = "Titre (facultatif)") String title) {
        logger.debug("init new form instance : " + id);
        if(recipientEmails == null && recipientsEmails != null && recipientsEmails.size() > 0) {
            recipientEmails = recipientsEmails;
        }
        if(createByEppn == null && eppn != null && !eppn.isEmpty()) {
            createByEppn = eppn;
        }
        if(createByEppn == null) {
            throw new EsupSignatureRuntimeException("Required request parameter 'createByEppn' for method parameter type String is not present");
        }
        try {
            Data data = dataService.addData(id, createByEppn);
            TypeReference<Map<String, String>> type = new TypeReference<>(){};
            Map<String, String> datas = new HashMap<>();
            if(formDatas != null) {
                datas = objectMapper.readValue(formDatas, type);
            }
            SignBook signBook = signBookService.sendForSign(data.getId(), recipientEmails, signTypes, allSignToCompletes, null, targetEmails, targetUrls, createByEppn, createByEppn, true, datas, null, signRequestParamsJsonString, title);
            signBookService.addViewers(signBook.getId(), recipientsCCEmails);
            return signBook.getSignRequests().get(0).getId();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return -1L;
        }
    }

    @CrossOrigin
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(description = "Récupération d'un circuit", responses = @ApiResponse(description = "JsonDtoWorkflow", content = @Content(schema = @Schema(implementation = JsonDtoWorkflow.class))))
    public String get(@PathVariable Long id) throws JsonProcessingException {
        return formService.getByIdJson(id);
    }

    @CrossOrigin
    @PostMapping(value = "/{id}/new-doc")
    @Operation(description = "Création d'une nouvelle instance d'un formulaire")
    public Long startWithDoc(@PathVariable Long id,
                             @RequestParam @Parameter(description = "Multipart stream du fichier à signer") MultipartFile[] multipartFiles,
                             @RequestParam @Parameter(description = "Eppn du propriétaire du futur document") String createByEppn,
                             @RequestParam(required = false) @Parameter(description = "Multipart stream des pièces jointes") MultipartFile[] attachementMultipartFiles,
                             @RequestParam(required = false) @Parameter(description = "Liste des participants pour chaque étape (ancien nom)", example = "[stepNumber*email]") List<String> recipientEmails,
                             @RequestParam(required = false) @Parameter(description = "Liste des participants pour chaque étape", example = "[stepNumber*email]") List<String> recipientsEmails,
                             @RequestParam(required = false) @Parameter(description = "Liste des types de signature pour chaque étape", example = "[stepNumber*signTypes]") List<String> signTypes,
                             @RequestParam(required = false) @Parameter(description = "Lites des numéros d'étape pour lesquelles tous les participants doivent signer", example = "[stepNumber]") List<String> allSignToCompletes,
                             @RequestParam(required = false) @Parameter(description = "Liste des destinataires finaux", example = "[email]") List<String> targetEmails,
                             @RequestParam(required = false) @Parameter(description = "Emplacements finaux", example = "[smb://drive.univ-ville.fr/forms-archive/]") List<String> targetUrls,
                             @RequestParam(required = false) @Parameter(description = "Paramètres de signature", example = "[{\"xPos\":100, \"yPos\":100, \"signPageNumber\":1}, {\"xPos\":200, \"yPos\":200, \"signPageNumber\":1}]") String signRequestParamsJsonString,
                             @RequestParam(required = false) @Parameter(description = "Données par défaut à remplir dans le formulaire", example = "{'field1' : 'toto, 'field2' : 'tata'}") String formDatas,
                             @RequestParam(required = false) @Parameter(description = "Titre") String title
    ) {
        if(recipientEmails == null && recipientsEmails.size() > 0) {
            recipientEmails = recipientsEmails;
        }
        Data data = dataService.addData(id, createByEppn);
        try {
            TypeReference<Map<String, String>> type = new TypeReference<>(){};
            Map<String, String> datas = new HashMap<>();
            if(formDatas != null) {
                datas.putAll(objectMapper.readValue(formDatas, type));
            }
            SignBook signBook = signBookService.sendForSign(data.getId(), recipientEmails, signTypes, allSignToCompletes, null, targetEmails, targetUrls, createByEppn, createByEppn, true, datas, multipartFiles[0].getInputStream(), signRequestParamsJsonString, title);
            signRequestService.addAttachement(attachementMultipartFiles, null, signBook.getSignRequests().get(0).getId());
            return signBook.getSignRequests().get(0).getId();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return -1L;
        }
    }

    @CrossOrigin
    @PostMapping(value = "/get-datas/{id}")
    @Operation(description = "Récupération des données d'un formulaire")
    public LinkedHashMap<String, String> getDatas(@PathVariable Long id) {
        return dataExportService.getJsonDatasFromSignRequest(id);
    }
}
