package org.esupportail.esupsignature.web.ws;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.service.DataService;
import org.esupportail.esupsignature.service.export.DataExportService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;

@RestController
@RequestMapping("/ws/forms")
public class FormWsController {

    @Resource
    private DataService dataService;

    @Resource
    private DataExportService dataExportService;

    @CrossOrigin
    @PostMapping(value = "/{id}/new")
    @Operation(description = "Création d'une nouvelle instance d'un formulaire")
    public Long start(@PathVariable Long id,
                      @RequestParam String eppn,
                      @RequestParam(required = false) @Parameter(description = "pattern : stepNumber*email") List<String> recipientEmails,
                      @RequestParam(required = false) @Parameter(description = "stepNumber") List<String> allSignToCompletes,
                      @RequestParam(required = false) List<String> targetEmails,
                      @RequestParam(required = false) String targetUrl
    ) {
        Data data = dataService.addData(id, eppn, eppn);
        try {
            SignBook signBook = dataService.sendForSign(data.getId(), recipientEmails, allSignToCompletes, null, targetEmails, targetUrl, eppn, eppn, true);

            return signBook.getSignRequests().get(0).getId();
        } catch (EsupSignatureException | EsupSignatureIOException e) {
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
