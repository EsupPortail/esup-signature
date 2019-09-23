package org.esupportail.esupsignature.web.controller.user;

import eu.europa.esig.dss.MimeType;
import eu.europa.esig.dss.validation.executor.ValidationLevel;
import eu.europa.esig.dss.validation.reports.Reports;
import org.esupportail.esupsignature.dss.web.model.ValidationForm;
import org.esupportail.esupsignature.dss.web.service.FOPService;
import org.esupportail.esupsignature.dss.web.service.XSLTService;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.service.FileService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.ValidationService;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.io.IOException;
import java.util.Arrays;

@Controller
@SessionAttributes({ "simpleReportXml", "detailedReportXml" })
@RequestMapping(value = "/user/validation")
public class ValidationController {
	
	private static final Logger logger = LoggerFactory.getLogger(ValidationController.class);

	@ModelAttribute("active")
	public String getActiveMenu() {
		return "user/validation";
	}

	@Resource
	private UserService userService;
	
	@ModelAttribute("user")
	public User getUser() {
		return userService.getUserFromAuthentication();
	}
	
	@Autowired
	private XSLTService xsltService;

	@Autowired
	private FOPService fopService;
		
	@Resource
	private ValidationService validationService;
	
	@Resource
	private FileService fileService;

	@Resource
	private PdfService pdfService;
	
	@Autowired
	private SignRequestRepository signRequestRepository;
	
	@Resource
	private SignRequestService signRequestService;
	
	@RequestMapping(method = RequestMethod.GET)
	public String showValidationForm(Model model, HttpServletRequest request) {
		ValidationForm validationForm = new ValidationForm();
		validationForm.setValidationLevel(ValidationLevel.ARCHIVAL_DATA);
		validationForm.setDefaultPolicy(true);
		model.addAttribute("validationForm", validationForm);
		return "user/validation";
	}

	@RequestMapping(method = RequestMethod.POST)
	public String validate(@ModelAttribute("multipartFile") @Valid MultipartFile multipartFile, Model model) throws IOException {
		Reports reports = validationService.validate(multipartFile);
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
		if(multipartFile.getContentType().contains("pdf")) {
			model.addAttribute("pdfaReport", pdfService.checkPDFA(multipartFile.getInputStream()));
		} else {
			model.addAttribute("pdfaReport", Arrays.asList("danger", "Impossible de valider ce document"));
		}
		
		return "user/validation-result";
	}
	
	@Transactional
	@RequestMapping(value = "/document/{id}")
	public String validateDocument(@PathVariable(name="id") long id, Model model) throws IOException {
		SignRequest signRequest = signRequestRepository.findById(id).get();
		Document toValideDocument = signRequestService.getLastSignedDocument(signRequest);
		Reports reports = validationService.validate(new MockMultipartFile(toValideDocument.getFileName(), toValideDocument.getInputStream()));
		
		String xmlSimpleReport = reports.getXmlSimpleReport();
		model.addAttribute("simpleReport", xsltService.generateSimpleReport(xmlSimpleReport));

		String xmlDetailedReport = reports.getXmlDetailedReport();
		model.addAttribute("detailedReport", xsltService.generateDetailedReport(xmlDetailedReport));
		model.addAttribute("detailedReportXml", reports.getXmlDetailedReport());
		model.addAttribute("diagnosticTree", reports.getXmlDiagnosticData());
		model.addAttribute("pdfaReport", pdfService.checkPDFA(toValideDocument.getInputStream()));
		
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