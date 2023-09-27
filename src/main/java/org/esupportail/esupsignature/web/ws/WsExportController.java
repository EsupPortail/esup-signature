package org.esupportail.esupsignature.web.ws;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.repository.FormRepository;
import org.esupportail.esupsignature.service.export.DataExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
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
    public ResponseEntity<Void> getFormDatasCsv(@PathVariable String name, HttpServletResponse response) {
        List<Form> forms = formRepository.findFormByNameAndDeletedIsNullOrDeletedIsFalse(name);
        if (forms.size() > 0) {
            try {
                response.setContentType("text/csv; charset=utf-8");
                response.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode(forms.get(0).getName(), StandardCharsets.UTF_8.toString()) + ".csv");
                InputStream csvInputStream = dataExportService.getCsvDatasFromForms(forms.stream().map(Form::getWorkflow).collect(Collectors.toList()));
                IOUtils.copy(csvInputStream, response.getOutputStream());
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (Exception e) {
                logger.error("get file error", e);
            }
        } else {
            logger.warn("form " + name + " not found");
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ResponseBody
    @GetMapping(value = "/form/{name}/datas/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Map<String, String>> getFormDatasJson(@PathVariable String name) {
        return dataExportService.getDatasToExportByFormName(name);
    }

}
