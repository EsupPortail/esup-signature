package org.esupportail.esupsignature.web.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.esupportail.esupsignature.web.ws.json.JsonDtoWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/ws/workflows")
public class WorkflowWsController {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowWsController.class);

    @Resource
    private WorkflowService workflowService;

    @Resource
    private SignBookService signBookService;

    @CrossOrigin
    @PostMapping(value = "/{id}/new")
    @Operation(description = "Dépôt d'un document dans une nouvelle instance d'un circuit")
    public Long start(@PathVariable Long id,
                      @Parameter(description = "Multipart stream du fichier à signer") @RequestParam MultipartFile[] multipartFiles,
                      @RequestParam @Parameter(description = "Eppn du propriétaire du futur document") String createByEppn,
                      @RequestParam(required = false) @Parameter(description = "Titre (facultatif)") String title,
                      @RequestParam(required = false) @Parameter(description = "Liste des participants pour chaque étape", example = "[stepNumber*email]") List<String> recipientEmails,
                      @RequestParam(required = false) @Parameter(description = "Lites des numéros d'étape pour lesquelles tous les participants doivent signer", example = "[stepNumber]") List<String> allSignToCompletes,
                      @RequestParam(required = false) @Parameter(description = "Liste des destinataires finaux", example = "[email]") List<String> targetEmails,
                      @RequestParam(required = false) @Parameter(description = "Paramètres de signature", example = "[{\"xPos\":100, \"yPos\":100, \"signPageNumber\":1}, {\"xPos\":200, \"yPos\":200, \"signPageNumber\":1}]") String signRequestParamsJsonString,
                      @RequestParam(required = false) @Parameter(description = "Emplacements finaux", example = "[smb://drive.univ-ville.fr/forms-archive/]") List<String> targetUrls
    ) {
        try {
            SignRequest signRequest = signBookService.startWorkflow(id, multipartFiles, createByEppn, title, recipientEmails, allSignToCompletes, targetEmails, targetUrls, signRequestParamsJsonString);
            return signRequest.getId();
        } catch (EsupSignatureRuntimeException e) {
            logger.error(e.getMessage(), e);
            return -1L;
        }
    }

    @CrossOrigin
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(description = "Récupération d'un circuit", responses = @ApiResponse(description = "JsonDtoWorkflow", content = @Content(schema = @Schema(implementation = JsonDtoWorkflow.class))))
    public String get(@PathVariable Long id) throws JsonProcessingException {
        return workflowService.getByIdJson(id);
    }

    @CrossOrigin
    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(description = "Récupération d'une demande de signature", responses = @ApiResponse(description = "List<JsonDtoWorkflow>", content = @Content(schema = @Schema(implementation = List.class))))
    public String getAll() throws JsonProcessingException {
        return workflowService.getAllWorkflowsJson();
    }

}
