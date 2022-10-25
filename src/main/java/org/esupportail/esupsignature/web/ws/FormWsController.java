package org.esupportail.esupsignature.web.ws;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.service.DataService;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.export.DataExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
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
    private SignBookService signBookService;

    @Resource
    private DataExportService dataExportService;

    @Resource
    private SignRequestService signRequestService;

    @CrossOrigin
    @PostMapping(value = "/{id}/new")
    @Operation(description = "Création d'une nouvelle instance d'un formulaire")
    public Long start(@PathVariable Long id,
                      @RequestParam(required = false) @Parameter(description = "Eppn du propriétaire du futur document (ancien nom)") String eppn,
                      @RequestParam(required = false) @Parameter(description = "Eppn du propriétaire du futur document") String createByEppn,
                      @RequestParam(required = false) @Parameter(description = "Liste des participants pour chaque étape", example = "[stepNumber*email]") List<String> recipientEmails,
                      @RequestParam(required = false) @Parameter(description = "Liste des participants pour chaque étape", example = "[stepNumber*signTypes]") List<String> signTypes,
                      @RequestParam(required = false) @Parameter(description = "Lites des numéros d'étape pour lesquelles tous les participants doivent signer", example = "[stepNumber]") List<String> allSignToCompletes,
                      @RequestParam(required = false) @Parameter(description = "Liste des destinataires finaux", example = "[email]") List<String> targetEmails,
                      @RequestParam(required = false) @Parameter(description = "Paramètres de signature", example = "[{\"xPos\":100, \"yPos\":100, \"signPageNumber\":1}, {\"xPos\":200, \"yPos\":200, \"signPageNumber\":1}]") String signRequestParamsJsonString,
                      @RequestParam(required = false) @Parameter(description = "Emplacements finaux", example = "[smb://drive.univ-ville.fr/forms-archive/]") List<String> targetUrls,
                      @RequestParam(required = false) @Parameter(description = "Données par défaut à remplir dans le formulaire", example = "{'field1' : 'toto, 'field2' : 'tata'}") String formDatas,
                      @RequestParam(required = false) @Parameter(description = "Titre") String title
    ) {
        if(createByEppn == null && eppn != null && !eppn.isEmpty()) {
            createByEppn = eppn;
        }
        try {
            Data data = dataService.addData(id, createByEppn);
            ObjectMapper objectMapper = new ObjectMapper();
            TypeReference<Map<String, String>> type = new TypeReference<>(){};
            Map<String, String> datas = new HashMap<>();
            if(formDatas != null) {
                objectMapper.readValue(formDatas, type);
            }
            SignBook signBook = signBookService.sendForSign(data.getId(), recipientEmails, signTypes, allSignToCompletes, null, targetEmails, targetUrls, createByEppn, createByEppn, true, datas, null, signRequestParamsJsonString, title);
            return signBook.getSignRequests().get(0).getId();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return -1L;
        }
    }

    @CrossOrigin
    @PostMapping(value = "/{id}/new-doc")
    @Operation(description = "Création d'une nouvelle instance d'un formulaire")
    public Long startWithDoc(@PathVariable Long id,
                             @RequestParam @Parameter(description = "Multipart stream du fichier à signer") MultipartFile[] multipartFiles,
                             @RequestParam @Parameter(description = "Eppn du propriétaire du futur document") String createByEppn,
                             @RequestParam(required = false) @Parameter(description = "Multipart stream des pièces jointes") MultipartFile[] attachementMultipartFiles,
                             @RequestParam(required = false) @Parameter(description = "Liste des participants pour chaque étape", example = "[stepNumber*email]") List<String> recipientEmails,
                             @RequestParam(required = false) @Parameter(description = "Liste des types de signature pour chaque étape", example = "[stepNumber*signTypes]") List<String> signTypes,
                             @RequestParam(required = false) @Parameter(description = "Lites des numéros d'étape pour lesquelles tous les participants doivent signer", example = "[stepNumber]") List<String> allSignToCompletes,
                             @RequestParam(required = false) @Parameter(description = "Liste des destinataires finaux", example = "[email]") List<String> targetEmails,
                             @RequestParam(required = false) @Parameter(description = "Emplacements finaux", example = "[smb://drive.univ-ville.fr/forms-archive/]") List<String> targetUrls,
                             @RequestParam(required = false) @Parameter(description = "Paramètres de signature", example = "[{\"xPos\":100, \"yPos\":100, \"signPageNumber\":1}, {\"xPos\":200, \"yPos\":200, \"signPageNumber\":1}]") String signRequestParamsJsonString,
                             @RequestParam(required = false) @Parameter(description = "Données par défaut à remplir dans le formulaire", example = "{'field1' : 'toto, 'field2' : 'tata'}") String formDatas,
                             @RequestParam(required = false) @Parameter(description = "Titre") String title
    ) {
        Data data = dataService.addData(id, createByEppn);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
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
