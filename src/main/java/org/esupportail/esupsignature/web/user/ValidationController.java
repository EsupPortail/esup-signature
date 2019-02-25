package org.esupportail.esupsignature.web.user;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.esupportail.esupsignature.dss.web.WebAppUtils;
import org.esupportail.esupsignature.dss.web.model.ValidationForm;
import org.esupportail.esupsignature.dss.web.service.FOPService;
import org.esupportail.esupsignature.dss.web.service.XSLTService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.multipart.MultipartFile;

import eu.europa.esig.dss.MimeType;
import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.executor.ValidationLevel;
import eu.europa.esig.dss.validation.reports.Reports;

@Controller
@SessionAttributes({ "simpleReportXml", "detailedReportXml" })
@RequestMapping(value = "/user/validation")
public class ValidationController {
	
	private static final Logger logger = LoggerFactory.getLogger(ValidationController.class);

	@ModelAttribute("active")
	public String getActiveMenu() {
		return "user/validation";
	}
	
	@Autowired
	private CertificateVerifier certificateVerifier;

	@Autowired
	private XSLTService xsltService;

	@Autowired
	private FOPService fopService;

	@Autowired
	private Resource defaultPolicy;

	@RequestMapping(method = RequestMethod.GET)
	public String showValidationForm(Model model, HttpServletRequest request) {
		ValidationForm validationForm = new ValidationForm();
		validationForm.setValidationLevel(ValidationLevel.ARCHIVAL_DATA);
		validationForm.setDefaultPolicy(true);
		model.addAttribute("validationForm", validationForm);
		return "user/validation";
	}

	@RequestMapping(method = RequestMethod.POST)
	public String validate(@ModelAttribute("multipartFile") @Valid MultipartFile multipartFile, Model model) {
		//TODO service ?
		SignedDocumentValidator documentValidator = SignedDocumentValidator.fromDocument(WebAppUtils.toDSSDocument(multipartFile));
		documentValidator.setCertificateVerifier(certificateVerifier);
		documentValidator.setValidationLevel(ValidationLevel.BASIC_SIGNATURES);

		Reports reports = null;
		try (InputStream is = defaultPolicy.getInputStream()) {
			reports = documentValidator.validateDocument(is);
		} catch (IOException e) {
			logger.error("Unable to parse policy : " + e.getMessage(), e);
		}


		String xmlSimpleReport = reports.getXmlSimpleReport();
		model.addAttribute("simpleReport", xsltService.generateSimpleReport(xmlSimpleReport));

		String xmlDetailedReport = reports.getXmlDetailedReport();
		model.addAttribute("detailedReport", xsltService.generateDetailedReport(xmlDetailedReport));
		model.addAttribute("detailedReportXml", reports.getXmlDetailedReport());
		model.addAttribute("diagnosticTree", reports.getXmlDiagnosticData());

		return "user/validation-result";
	}

	@RequestMapping(value = "/download-simple-report")
	public void downloadSimpleReport(HttpSession session, HttpServletResponse response) {
		try {
			String simpleReport = (String) session.getAttribute("simpleReportXml");

			response.setContentType(MimeType.PDF.getMimeTypeString());
			response.setHeader("Content-Disposition", "attachment; filename=DSS-Simple-report.pdf");

			fopService.generateSimpleReport(simpleReport, response.getOutputStream());
		} catch (Exception e) {
			logger.error("An error occured while generating pdf for simple report : " + e.getMessage(), e);
		}
	}

	@RequestMapping(value = "/download-detailed-report")
	public void downloadDetailedReport(HttpSession session, HttpServletResponse response) {
		try {
			String detailedReport = (String) session.getAttribute("detailedReportXml");

			response.setContentType(MimeType.PDF.getMimeTypeString());
			response.setHeader("Content-Disposition", "attachment; filename=DSS-Detailed-report.pdf");

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