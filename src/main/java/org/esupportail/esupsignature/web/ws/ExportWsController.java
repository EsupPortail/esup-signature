package org.esupportail.esupsignature.web.ws;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.repository.FormRepository;
import org.esupportail.esupsignature.service.export.DataExportService;
import org.esupportail.esupsignature.service.export.WorkflowExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ws/export/")
public class ExportWsController {

    private static final Logger logger = LoggerFactory.getLogger(ExportWsController.class);

    private final FormRepository formRepository;

    private final DataExportService dataExportService;

    private final WorkflowExportService workflowExportService;

    public ExportWsController(FormRepository formRepository, DataExportService dataExportService, WorkflowExportService workflowExportService) {
        this.formRepository = formRepository;
        this.dataExportService = dataExportService;
        this.workflowExportService = workflowExportService;
    }

    @CrossOrigin
    @GetMapping(value = "/form/{name}/datas/csv", produces="text/csv")
    @PreAuthorize("@wsAccessTokenService.isAllAccess(#xApiKey)")
    public ResponseEntity<Void> getFormDatasCsv(@PathVariable String name, @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey, HttpServletResponse response) throws IOException {
        List<Form> forms = formRepository.findFormByNameAndDeletedIsNullOrDeletedIsFalse(name);
        if (!forms.isEmpty()) {
            response.setContentType("text/csv; charset=utf-8");
            response.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode(forms.get(0).getName(), StandardCharsets.UTF_8) + ".csv");
            InputStream csvInputStream = dataExportService.getCsvDatasFromForms(forms.stream().map(Form::getWorkflow).collect(Collectors.toList()));
            IOUtils.copy(csvInputStream, response.getOutputStream());
            return ResponseEntity.ok().build();
        }
        logger.warn("form " + name + " not found");
        return ResponseEntity.notFound().build();
    }

    @CrossOrigin
    @PreAuthorize("@wsAccessTokenService.isAllAccess(#xApiKey)")
    @GetMapping(value = "/form/{name}/datas/json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Récupération des données d'un formulaire")
    public List<Map<String, String>> getFormDatasJson(@PathVariable String name, @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey) {
        return dataExportService.getDatasToExportByFormName(name);
    }

    @CrossOrigin
    @PreAuthorize("@wsAccessTokenService.createWorkflowAccess(#id, #xApiKey)")
    @GetMapping(value = "/workflow/{id}/datas/json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(security = @SecurityRequirement(name = "x-api-key"), description = "Récupération des demandes d'un circuit")
    public LinkedHashMap<String, String> getDatas(@PathVariable String id, @ModelAttribute("xApiKey") @Parameter(hidden = true) String xApiKey) {
        return workflowExportService.getJsonDatasFromWorkflow(id);
    }

}
