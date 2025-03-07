package org.esupportail.esupsignature.dss.service;

import eu.europa.esig.dss.detailedreport.DetailedReportFacade;
import eu.europa.esig.dss.simplecertificatereport.SimpleCertificateReportXmlDefiner;
import eu.europa.esig.dss.simplereport.SimpleReportFacade;
import eu.europa.esig.dss.xml.utils.DSSXmlErrorListener;
import jakarta.annotation.PostConstruct;
import org.apache.fop.apps.*;
import org.esupportail.esupsignature.dss.config.DSSProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.OutputStream;
import java.io.StringReader;

@Component
public class FOPService {

	private static final Logger logger = LoggerFactory.getLogger(FOPService.class);
	private final DSSProperties dSSProperties;

	private FopFactory fopFactory;
	private FOUserAgent foUserAgent;

	public FOPService(DSSProperties dSSProperties) {
		this.dSSProperties = dSSProperties;
	}

	@PostConstruct
	public void init() throws Exception {
		FopFactoryBuilder builder = new FopFactoryBuilder(new File(".").toURI());
		builder.setAccessibility(true);
		fopFactory = builder.build();
		foUserAgent = fopFactory.newFOUserAgent();
		foUserAgent.setCreator("DSS Webapp");
		foUserAgent.setAccessibility(true);
	}

	public void generateSimpleReport(String simpleReport, OutputStream os) throws Exception {
		Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, os);
		Result result = new SAXResult(fop.getDefaultHandler());
		SimpleReportFacade.newFacade().generatePdfReport(simpleReport, result);
	}

	public void generateDetailedReport(String detailedReport, OutputStream os) throws Exception {
		Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, os);
		Result result = new SAXResult(fop.getDefaultHandler());
		DetailedReportFacade.newFacade().generatePdfReport(detailedReport, result);
	}

	public void generateSimpleCertificateReport(String simpleCertificateReport, OutputStream os) throws Exception {
		Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, os);
		Result result = new SAXResult(fop.getDefaultHandler());
		try (StringReader reader = new StringReader(simpleCertificateReport)) {
			Transformer transformer = SimpleCertificateReportXmlDefiner.getPdfTemplates().newTransformer();
			transformer.setErrorListener(new DSSXmlErrorListener());
			transformer.setParameter("rootUrlInTlBrowser", dSSProperties.getRootUrlInTlBrowser());
			transformer.transform(new StreamSource(reader), result);
		} catch (Exception e) {
			logger.error("Error while generating simple certificate report : " + e.getMessage(), e);
		}
	}

}
