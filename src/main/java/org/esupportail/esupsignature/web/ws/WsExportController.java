package org.esupportail.esupsignature.web.ws;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.repository.FormRepository;
import org.esupportail.esupsignature.service.export.DataExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/ws/export/")
public class WsExportController {

    private static final Logger logger = LoggerFactory.getLogger(WsExportController.class);

    @Resource
    private FormRepository formRepository;

    @Resource
    private DataExportService dataExportService;

    @GetMapping(value = "/form/{name}/datas/csv", produces="text/csv")
    @PreAuthorize("@wsAccessTokenService.isAllAccess(#wsAccessToken)")
    public ResponseEntity<Void> getFormDatasCsv(@PathVariable String name, @RequestParam(required = false) @Parameter(description = "WS Access Token (facultatif)") String wsAccessToken, HttpServletResponse response) {
        List<Form> forms = formRepository.findFormByNameAndDeletedIsNullOrDeletedIsFalse(name);
        if (!forms.isEmpty()) {
            try {
                response.setContentType("text/csv; charset=utf-8");
                response.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode(forms.get(0).getName(), StandardCharsets.UTF_8) + ".csv");
                InputStream csvInputStream = dataExportService.getCsvDatasFromForms(forms.stream().map(Form::getWorkflow).collect(Collectors.toList()));
                IOUtils.copy(csvInputStream, response.getOutputStream());
                return ResponseEntity.ok().build();
            } catch (Exception e) {
                logger.error("get file error", e);
            }
        } else {
            logger.warn("form " + name + " not found");
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.internalServerError().build();
    }

    @ResponseBody
    @PreAuthorize("@wsAccessTokenService.isAllAccess(#wsAccessToken)")
    @GetMapping(value = "/form/{name}/datas/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Map<String, String>> getFormDatasJson(@PathVariable String name, @RequestParam(required = false) @Parameter(description = "WS Access Token (facultatif)") String wsAccessToken) {
        return dataExportService.getDatasToExportByFormName(name);
    }

}
