package org.esupportail.esupsignature.web.wssecure;

import eu.europa.esig.dss.enumerations.MimeTypeEnum;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.reports.Reports;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.esupportail.esupsignature.dss.service.FOPService;
import org.esupportail.esupsignature.dss.service.XSLTService;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.utils.sign.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/ws-secure/validation")
public class ValidationWsSecureController {

    private static final Logger logger = LoggerFactory.getLogger(ValidationWsSecureController.class);

    protected static final String SIMPLE_REPORT_ATTRIBUTE = "simpleReport";
    protected static final String DETAILED_REPORT_ATTRIBUTE = "detailedReport";

    protected static final String XML_SIMPLE_REPORT_ATTRIBUTE = "simpleReportXml";
    protected static final String XML_SIMPLE_CERTIFICATE_REPORT_ATTRIBUTE = "simpleCertificateReportXml";
    protected static final String XML_DETAILED_REPORT_ATTRIBUTE = "detailedReportXml";
    protected static final String XML_DIAGNOSTIC_DATA_ATTRIBUTE = "diagnosticDataXml";
    protected static final String ETSI_VALIDATION_REPORT_ATTRIBUTE = "etsiValidationReport";

    protected static final String ALL_CERTIFICATES_ATTRIBUTE = "allCertificates";
    protected static final String ALL_REVOCATION_DATA_ATTRIBUTE = "allRevocationData";
    protected static final String ALL_TIMESTAMPS_ATTRIBUTE = "allTimestamps";

    private final XSLTService xsltService;
    private final FOPService fopService;
    private final ValidationService validationService;
    private final SignRequestService signRequestService;

    public ValidationWsSecureController(XSLTService xsltService, FOPService fopService, ValidationService validationService, SignRequestService signRequestService) {
        this.xsltService = xsltService;
        this.fopService = fopService;
        this.validationService = validationService;
        this.signRequestService = signRequestService;
    }

    @GetMapping(value = "/short/{id}")
    @ResponseBody
    public String shortValidateDocument(@PathVariable(name="id") long id) throws IOException {
        InputStream inputStream = signRequestService.getToValidateFile(id);
        if(inputStream != null && inputStream.available() > 0) {
            Reports reports = validationService.validate(inputStream, null);
            if (reports != null) {
                String xmlSimpleReport = reports.getXmlSimpleReport();
                return xsltService.generateShortReport(xmlSimpleReport);
            }
        }
        return null;
    }

    @RequestMapping(value = "/download-simple-report")
    public void downloadSimpleReport(HttpSession session, HttpServletResponse response) throws EsupSignatureException {
        final String simpleReport = (String) session.getAttribute(XML_SIMPLE_REPORT_ATTRIBUTE);
        final String simpleCertificateReport = (String) session.getAttribute(XML_SIMPLE_CERTIFICATE_REPORT_ATTRIBUTE);
        if (Utils.isStringNotEmpty(simpleReport)) {
            try {
                response.setContentType(MimeTypeEnum.PDF.getMimeTypeString());
                response.setHeader("Content-Disposition", "attachment; filename=DSS-Simple-report.pdf");
                fopService.generateSimpleReport(simpleReport, response.getOutputStream());
            } catch (Exception e) {
                logger.error("An error occurred while generating pdf for simple report : " + e.getMessage(), e);
            }
        } else if (Utils.isStringNotEmpty(simpleCertificateReport)) {
            try {
                response.setContentType(MimeTypeEnum.PDF.getMimeTypeString());
                response.setHeader("Content-Disposition", "attachment; filename=DSS-Simple-certificate-report.pdf");
                fopService.generateSimpleCertificateReport(simpleCertificateReport, response.getOutputStream());
            } catch (Exception e) {
                logger.error("An error occurred while generating pdf for simple certificate report : " + e.getMessage(), e);
            }
        } else {
            throw new EsupSignatureException("Simple report not found");
        }
    }

    @RequestMapping(value = "/download-detailed-report")
    public void downloadDetailedReport(HttpSession session, HttpServletResponse response) throws EsupSignatureException {
        final String detailedReport = (String) session.getAttribute(XML_DETAILED_REPORT_ATTRIBUTE);
        if (detailedReport == null) {
            throw new EsupSignatureException("Detailed report not found");
        }
        try {
            response.setContentType(MimeTypeEnum.PDF.getMimeTypeString());
            response.setHeader("Content-Disposition", "attachment; filename=DSS-Detailed-report.pdf");
            fopService.generateDetailedReport(detailedReport, response.getOutputStream());
        } catch (Exception e) {
            logger.error("An error occurred while generating pdf for detailed report : " + e.getMessage(), e);
        }
    }

    @RequestMapping(value = "/download-diagnostic-data")
    public void downloadDiagnosticData(HttpSession session, HttpServletResponse response) throws EsupSignatureException {
        String diagnosticData = (String) session.getAttribute(XML_DIAGNOSTIC_DATA_ATTRIBUTE);
        if (diagnosticData == null) {
            throw new EsupSignatureException("Diagnostic data not found");
        }

        try (InputStream is = new ByteArrayInputStream(diagnosticData.getBytes());
             OutputStream os = response.getOutputStream()) {
            response.setContentType(MimeTypeEnum.XML.getMimeTypeString());
            response.setHeader("Content-Disposition", "attachment; filename=DSS-Diagnostic-data.xml");
            Utils.copy(is, os);

        } catch (IOException e) {
            logger.error("An error occurred while downloading diagnostic data : " + e.getMessage(), e);
        }
    }

    @RequestMapping(value = "/diag-data.svg")
    public @ResponseBody ResponseEntity<String> downloadSVG(HttpSession session, HttpServletResponse response) throws EsupSignatureException {
        String diagnosticData = (String) session.getAttribute(XML_DIAGNOSTIC_DATA_ATTRIBUTE);
        if (diagnosticData == null) {
            throw new EsupSignatureException("Diagnostic data not found");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(MimeTypeEnum.SVG.getMimeTypeString()));
        ResponseEntity<String> svgEntity = new ResponseEntity<>(xsltService.generateSVG(diagnosticData), headers,
                HttpStatus.OK);
        return svgEntity;
    }
}
