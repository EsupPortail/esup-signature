package org.esupportail.esupsignature.web.controller.user;

import eu.europa.esig.dss.model.MimeType;
import eu.europa.esig.dss.validation.executor.ValidationLevel;
import eu.europa.esig.dss.validation.reports.Reports;
import org.esupportail.esupsignature.dss.service.FOPService;
import org.esupportail.esupsignature.dss.service.XSLTService;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.ValidationService;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

@Controller
@SessionAttributes({ "simpleReportXml", "detailedReportXml" })
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
	private FileService fileService;

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
		Reports reports = validationService.validate(multipartSignedDoc.getInputStream(), multipartSignature.getInputStream());
		if(reports != null) {
			String xmlSimpleReport = reports.getXmlSimpleReport();
			model.addAttribute("simpleReport", xsltService.generateSimpleReport(xmlSimpleReport));
			String xmlDetailedReport = reports.getXmlDetailedReport();
			model.addAttribute("detailedReport", xsltService.generateDetailedReport(xmlDetailedReport));
			model.addAttribute("detailedReportXml", reports.getXmlDetailedReport());
			model.addAttribute("diagnosticTree", reports.getXmlDiagnosticData());
		} else {
			model.addAttribute("simpleReport", "<h2>Impossible de valider ce document</h2>");
			model.addAttribute("detailedReport", "<h2>Impossible de valider ce document</h2>");
		}
		if(multipartSignedDoc.getContentType().contains("pdf")) {
			try {
				model.addAttribute("pdfaReport", pdfService.checkPDFA(multipartSignedDoc.getInputStream(), true));
			} catch (EsupSignatureException e) {
				e.printStackTrace();
			}
		} else {
			model.addAttribute("pdfaReport", Arrays.asList("danger", "Impossible de valider ce document"));
		}
		
		return "user/validation/result";
	}
	
	@GetMapping(value = "/document/{id}")
	public String validateDocument(@PathVariable(name="id") long id, Model model) throws IOException {
		File file = signRequestService.getToValidateFile(id);
		Reports reports = validationService.validate(new FileInputStream(file), null);
		String xmlSimpleReport = reports.getXmlSimpleReport();
		model.addAttribute("simpleReport", xsltService.generateSimpleReport(xmlSimpleReport));
		String xmlDetailedReport = reports.getXmlDetailedReport();
		model.addAttribute("detailedReport", xsltService.generateDetailedReport(xmlDetailedReport));
		model.addAttribute("detailedReportXml", reports.getXmlDetailedReport());
		model.addAttribute("diagnosticTree", reports.getXmlDiagnosticData());
		try {
			model.addAttribute("pdfaReport", pdfService.checkPDFA(new FileInputStream(file), true));
		} catch (EsupSignatureException e) {
			logger.error("enable to check pdf");
		}
		return "user/validation/result";
	}

	@GetMapping(value = "/download-simple-report")
	public void downloadSimpleReport(HttpSession session, HttpServletResponse response) {
		try {
			String simpleReport = (String) session.getAttribute("simpleReportXml");

			response.setContentType(MimeType.PDF.getMimeTypeString());
			response.setHeader("Content-disposition", "attachment; filename=DSS-Simple-report.pdf");

			fopService.generateSimpleReport(simpleReport, response.getOutputStream());
		} catch (Exception e) {
			logger.error("An error occured while generating pdf for simple report : " + e.getMessage(), e);
		}
	}

	@GetMapping(value = "/download-detailed-report")
	public void downloadDetailedReport(HttpSession session, HttpServletResponse response) {
		try {
			String detailedReport = (String) session.getAttribute("detailedReportXml");

			response.setContentType(MimeType.PDF.getMimeTypeString());
			response.setHeader("Content-disposition", "attachment; filename=DSS-Detailed-report.pdf");

			fopService.generateDetailedReport(detailedReport, response.getOutputStream());
		} catch (Exception e) {
			logger.error("An error occured while generating pdf for detailed report : " + e.getMessage(), e);
		}
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