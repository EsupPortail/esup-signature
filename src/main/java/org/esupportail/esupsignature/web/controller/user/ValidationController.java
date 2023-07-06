package org.esupportail.esupsignature.web.controller.user;

import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.diagnostic.DiagnosticDataFacade;
import eu.europa.esig.dss.diagnostic.RevocationWrapper;
import eu.europa.esig.dss.diagnostic.TimestampWrapper;
import eu.europa.esig.dss.diagnostic.jaxb.XmlDiagnosticData;
import eu.europa.esig.dss.enumerations.MimeTypeEnum;
import eu.europa.esig.dss.enumerations.RevocationType;
import eu.europa.esig.dss.enumerations.TimestampType;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.executor.ValidationLevel;
import eu.europa.esig.dss.validation.reports.Reports;
import eu.europa.esig.validationreport.jaxb.ValidationReportType;
import org.esupportail.esupsignature.dss.service.FOPService;
import org.esupportail.esupsignature.dss.service.XSLTService;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.esupportail.esupsignature.service.utils.sign.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Controller
@SessionAttributes({ "simpleReportXml", "detailedReportXml", "diagnosticDataXml" })
@RequestMapping("/user/validation")
@ConditionalOnBean(ValidationService.class)
public class ValidationController {
	
	private static final Logger logger = LoggerFactory.getLogger(ValidationController.class);

	@ModelAttribute("activeMenu")
	public String getActiveMenu() {
		return "validation";
	}

	@Resource
	private XSLTService xsltService;

	@Resource
	private FOPService fopService;
		
	@Resource
	private ValidationService validationService;

	@Resource
	private PdfService pdfService;

	@Resource
	private SignRequestService signRequestService;
	
	@GetMapping
	public String showValidationForm() {
		return "user/validation/form";
	}

	@PostMapping
	public String validate(@RequestParam(name = "multipartSignedDoc") MultipartFile multipartSignedDoc, @RequestParam(name = "multipartSignature", required = false) MultipartFile multipartSignature, Model model) throws IOException {
		InputStream docInputStream = multipartSignedDoc.getInputStream();
		InputStream sigInputStream = multipartSignature.getInputStream();
		extracted(docInputStream, sigInputStream, model);
		return "user/validation/result";
	}

	private void extracted(InputStream docInputStream, InputStream sigInputStream, Model model) throws IOException {
		byte[] docBytes = docInputStream.readAllBytes();
		Reports reports = validationService.validate(new ByteArrayInputStream(docBytes), sigInputStream);
		if(reports != null) {
			String xmlSimpleReport = reports.getXmlSimpleReport();
			model.addAttribute("simpleReport", xsltService.generateSimpleReport(xmlSimpleReport));
			model.addAttribute("simpleReportXml", reports.getXmlSimpleReport());
			String xmlDetailedReport = reports.getXmlDetailedReport();
			model.addAttribute("detailedReport", xsltService.generateDetailedReport(xmlDetailedReport));
			model.addAttribute("detailedReportXml", reports.getXmlDetailedReport());
			ValidationReportType etsiValidationReportJaxb = reports.getEtsiValidationReportJaxb();

			if (etsiValidationReportJaxb != null) {
				model.addAttribute("etsiValidationReport", reports.getXmlValidationReport());
			}
			model.addAttribute("diagnosticDataXml", reports.getXmlDiagnosticData());
		} else {
			model.addAttribute("simpleReport", "<h2>Impossible de valider ce document</h2>");
			model.addAttribute("detailedReport", "<h2>Impossible de valider ce document</h2>");
		}
		try {
			model.addAttribute("pdfaReport", pdfService.checkPDFA(docBytes, true));
		} catch (EsupSignatureRuntimeException e) {
			model.addAttribute("pdfaReport", Arrays.asList("danger", "Impossible de valider ce document"));
			logger.error(e.getMessage());
		}
	}

	@GetMapping(value = "/document/{id}")
	public String validateDocument(@PathVariable(name="id") long id, Model model) throws IOException {
		extracted(signRequestService.getToValidateFile(id), null, model);
		return "user/validation/result";
	}

	@GetMapping(value = "/download-simple-report")
	public void downloadSimpleReport(HttpSession session, HttpServletResponse response) {
		try {
			String simpleReport = session.getAttribute("simpleReportXml").toString();
			response.setContentType(MimeTypeEnum.PDF.getMimeTypeString());
			response.setHeader("Content-Disposition", "attachment; filename=Rapport-Signature.pdf");
			fopService.generateSimpleReport(simpleReport, response.getOutputStream());
		} catch (Exception e) {
			logger.error("An error occured while generating pdf for simple report : " + e.getMessage(), e);
		}
	}

	@GetMapping(value = "/download-detailed-report")
	public void downloadDetailedReport(HttpSession session, HttpServletResponse response) {
		try {
			String detailedReport = session.getAttribute("detailedReportXml").toString();
			response.setContentType(MimeTypeEnum.PDF.getMimeTypeString());
			response.setHeader("Content-Disposition", "attachment; filename=Rapport-Signature-Complet.pdf");
			fopService.generateDetailedReport(detailedReport, response.getOutputStream());
		} catch (Exception e) {
			logger.error("An error occured while generating pdf for detailed report : " + e.getMessage(), e);
		}
	}

	@RequestMapping(value = "/download-diagnostic-data")
	public void downloadDiagnosticData(HttpSession session, HttpServletResponse response) {
		String report = session.getAttribute("diagnosticDataXml").toString();

		response.setContentType(MimeTypeEnum.XML.getMimeTypeString());
		response.setHeader("Content-Disposition", "attachment; filename=DSS-Diagnotic-data.xml");
		try {
			Utils.write(report.getBytes(StandardCharsets.UTF_8), response.getOutputStream());
		} catch (IOException e) {
			logger.error("An error occurred while downloading diagnostic data : " + e.getMessage(), e);
		}
	}

	@RequestMapping(value = "/diag-data.svg")
	public @ResponseBody
	ResponseEntity<String> downloadSVG(HttpSession session, HttpServletResponse response) {
		String report = session.getAttribute("diagnosticDataXml").toString();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.valueOf(MimeTypeEnum.SVG.getMimeTypeString()));
		return new ResponseEntity<>(xsltService.generateSVG(report), headers,
				HttpStatus.OK);
	}

	@RequestMapping(value = "/download-revocation")
	public void downloadRevocationData(@RequestParam(value = "id") String id, @RequestParam(value = "format") String format, HttpSession session,
									   HttpServletResponse response) throws Exception {
		DiagnosticData diagnosticData = getDiagnosticData(session);
		RevocationWrapper revocationData = diagnosticData.getRevocationById(id);
		if (revocationData == null) {
			String message = "Revocation data " + id + " not found";
			logger.warn(message);
			throw new Exception(message);
		}
		String filename = revocationData.getId();
		MimeTypeEnum mimeType;
		byte[] binaries;

		if (RevocationType.CRL.equals(revocationData.getRevocationType())) {
			mimeType = MimeTypeEnum.CRL;
			filename += ".crl";

			if (Utils.areStringsEqualIgnoreCase(format, "pem")) {
				String pem = "-----BEGIN CRL-----\n";
				pem += Utils.toBase64(revocationData.getBinaries());
				pem += "\n-----END CRL-----";
				binaries = pem.getBytes();
			} else {
				binaries = revocationData.getBinaries();
			}
		} else {
			mimeType = MimeTypeEnum.BINARY;
			filename += ".ocsp";
			binaries = revocationData.getBinaries();
		}

		addTokenToResponse(response, filename, mimeType, binaries);
	}

	protected void addTokenToResponse(HttpServletResponse response, String filename, MimeTypeEnum mimeType, byte[] binaries) {
		response.setContentType(MimeTypeEnum.TST.getMimeTypeString());
		response.setHeader("Content-Disposition", "attachment; filename=" + filename);
		try (InputStream is = new ByteArrayInputStream(binaries); OutputStream os = response.getOutputStream()) {
			Utils.copy(is, os);
		} catch (IOException e) {
			logger.error("An error occurred while downloading a file : " + e.getMessage(), e);
		}
	}

	@RequestMapping(value = "/download-timestamp")
	public void downloadTimestamp(@RequestParam(value = "id") String id, @RequestParam(value = "format") String format, HttpSession session,
								  HttpServletResponse response) throws Exception {
		DiagnosticData diagnosticData = getDiagnosticData(session);
		TimestampWrapper timestamp = diagnosticData.getTimestampById(id);
		if (timestamp == null) {
			String message = "Timestamp " + id + " not found";
			logger.warn(message);
			throw new Exception(message);
		}
		TimestampType type = timestamp.getType();

		byte[] binaries;
		if (Utils.areStringsEqualIgnoreCase(format, "pem")) {
			String pem = "-----BEGIN TIMESTAMP-----\n";
			pem += Utils.toBase64(timestamp.getBinaries());
			pem += "\n-----END TIMESTAMP-----";
			binaries = pem.getBytes();
		} else {
			binaries = timestamp.getBinaries();
		}

		String filename = type.name() + ".tst";
		addTokenToResponse(response, filename, MimeTypeEnum.TST, binaries);
	}

	public DiagnosticData getDiagnosticData(HttpSession session) {
		String diagnosticDataXml = session.getAttribute("diagnosticDataXml").toString();
		try {
			XmlDiagnosticData xmlDiagData = DiagnosticDataFacade.newFacade().unmarshall(diagnosticDataXml);
			return new DiagnosticData(xmlDiagData);
		} catch (Exception e) {
			logger.error("An error occurred while generating DiagnosticData from XML : " + e.getMessage(), e);
		}
		return null;
	}

	@ModelAttribute("validationLevels")
	public ValidationLevel[] getValidationLevels() {
		return new ValidationLevel[] { ValidationLevel.BASIC_SIGNATURES, ValidationLevel.LONG_TERM_DATA, ValidationLevel.ARCHIVAL_DATA };
	}

	@ModelAttribute("displayDownloadPdf")
	public boolean isDisplayDownloadPdf() {
		return true;
	}

}