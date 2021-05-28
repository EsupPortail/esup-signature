package org.esupportail.esupsignature.dss.service;

import eu.europa.esig.dss.DSSXmlErrorListener;
import eu.europa.esig.dss.DomUtils;
import eu.europa.esig.dss.diagnostic.DiagnosticDataFacade;
import eu.europa.esig.dss.diagnostic.jaxb.XmlDiagnosticData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;

@Component
public class XSLTService {

	private static final Logger logger = LoggerFactory.getLogger(XSLTService.class);

	private Templates templateShortReport;
	private Templates templateSimpleReport;
	private Templates templateDetailedReport;

	@PostConstruct
	public void init() throws TransformerConfigurationException, IOException {
		TransformerFactory transformerFactory = DomUtils.getSecureTransformerFactory();

		try (InputStream is = XSLTService.class.getResourceAsStream("/xslt/html/short-report-bootstrap4.xslt")) {
			templateShortReport = transformerFactory.newTemplates(new StreamSource(is));
		}

		try (InputStream is = XSLTService.class.getResourceAsStream("/xslt/html/simple-report-bootstrap4.xslt")) {
			templateSimpleReport = transformerFactory.newTemplates(new StreamSource(is));
		}

		try (InputStream is = XSLTService.class.getResourceAsStream("/xslt/html/detailed-report-bootstrap4.xslt")) {
			templateDetailedReport = transformerFactory.newTemplates(new StreamSource(is));
		}
	}

	public String generateShortReport(String simpleReport) {
		Writer writer = new StringWriter();
		try {
			Transformer transformer = templateShortReport.newTransformer();
			transformer.setErrorListener(new DSSXmlErrorListener());
			transformer.transform(new StreamSource(new StringReader(simpleReport)), new StreamResult(writer));
		} catch (Exception e) {
			logger.error("Error while generating simple report : " + e.getMessage(), e);
		}
		return writer.toString();
	}

	public String generateSimpleReport(String simpleReport) {
		Writer writer = new StringWriter();
		try {
			Transformer transformer = templateSimpleReport.newTransformer();
			transformer.setErrorListener(new DSSXmlErrorListener());
			transformer.transform(new StreamSource(new StringReader(simpleReport)), new StreamResult(writer));
		} catch (Exception e) {
			logger.error("Error while generating simple report : " + e.getMessage(), e);
		}
		return writer.toString();
	}

	public String generateDetailedReport(String detailedReport) {
		Writer writer = new StringWriter();
		try {
			Transformer transformer = templateDetailedReport.newTransformer();
			transformer.setErrorListener(new DSSXmlErrorListener());
			transformer.transform(new StreamSource(new StringReader(detailedReport)), new StreamResult(writer));
		} catch (Exception e) {
			logger.error("Error while generating detailed report : " + e.getMessage(), e);
		}
		return writer.toString();
	}

	public String generateSVG(String diagnosticDataXml) {
		try (Writer writer = new StringWriter()) {
			XmlDiagnosticData diagnosticData = DiagnosticDataFacade.newFacade().unmarshall(diagnosticDataXml);
			DiagnosticDataFacade.newFacade().generateSVG(diagnosticData, new StreamResult(writer));
			return writer.toString();
		} catch (Exception e) {
			logger.error("Error while generating the SVG : " + e.getMessage(), e);
			return null;
		}
	}

}