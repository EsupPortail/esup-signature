package org.esupportail.esupsignature.web.wssecure;

import eu.europa.esig.dss.validation.reports.Reports;
import org.esupportail.esupsignature.dss.service.XSLTService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.ValidationService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/ws-secure/validation")
public class ValidationWsSecureController {

    @Resource
    private XSLTService xsltService;

    @Resource
    private ValidationService validationService;

    @Resource
    private SignRequestService signRequestService;

    @GetMapping(value = "/short/{id}")
    @ResponseBody
    public String shortValidateDocument(@PathVariable(name="id") long id) throws IOException {
        File file = signRequestService.getToValidateFile(id);
        if(file != null) {
            Reports reports = validationService.validate(new FileInputStream(file), null);
            if (reports != null) {
                String xmlSimpleReport = reports.getXmlSimpleReport();
                return xsltService.generateShortReport(xmlSimpleReport);
            }
        }
        return null;

    }
}
