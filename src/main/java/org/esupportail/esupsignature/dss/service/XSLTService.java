package org.esupportail.esupsignature.dss.service;

import eu.europa.esig.dss.detailedreport.DetailedReportFacade;
import eu.europa.esig.dss.diagnostic.DiagnosticDataFacade;
import eu.europa.esig.dss.diagnostic.jaxb.XmlDiagnosticData;
import eu.europa.esig.dss.simplereport.SimpleReportFacade;
import eu.europa.esig.dss.xml.utils.DSSXmlErrorListener;
import eu.europa.esig.dss.xml.utils.DomUtils;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;

@Component
public class XSLTService {

	private static final Logger logger = LoggerFactory.getLogger(XSLTService.class);

	private Templates templateShortReport;

	@PostConstruct
	public void init() throws TransformerConfigurationException, IOException {
		TransformerFactory transformerFactory = DomUtils.getSecureTransformerFactory();
		try (InputStream is = new ClassPathResource("/xslt/html/short-report-bootstrap4.xslt").getInputStream()) {
			templateShortReport = transformerFactory.newTemplates(new StreamSource(is));
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
		if(StringUtils.hasText(writer.toString())) {
			return writer.toString();
		}
		return null;
	}

	public String generateSimpleReport(String xmlSimpleReport) {
		try {
			return SimpleReportFacade.newFacade().generateHtmlReport(xmlSimpleReport);
		} catch (IOException | TransformerException e) {
			throw new RuntimeException(e);
		}
	}

	public String generateDetailedReport(String xmlDetailedReportReport) {
		try {
			return DetailedReportFacade.newFacade().generateHtmlReport(xmlDetailedReportReport);
		} catch (IOException | TransformerException e) {
			throw new RuntimeException(e);
		}
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