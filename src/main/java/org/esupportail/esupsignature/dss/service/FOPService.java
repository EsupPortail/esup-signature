package org.esupportail.esupsignature.dss.service;

import eu.europa.esig.dss.DSSXmlErrorListener;
import eu.europa.esig.dss.DomUtils;
import eu.europa.esig.dss.detailedreport.DetailedReportFacade;
import eu.europa.esig.dss.simplereport.SimpleReportFacade;
import eu.europa.esig.dss.utils.Utils;
import org.apache.fop.apps.*;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import javax.annotation.PostConstruct;
import javax.xml.transform.Result;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

@Component
public class FOPService {

	private FopFactory fopFactory;
	private FOUserAgent foUserAgent;
	private Templates templateSimpleReport;
	private Templates templateDetailedReport;

	@PostConstruct
	public void init() throws Exception {

		FopFactoryBuilder builder = new FopFactoryBuilder(new File(".").toURI());
		builder.setAccessibility(true);

		fopFactory = builder.build();

		foUserAgent = fopFactory.newFOUserAgent();
		foUserAgent.setCreator("DSS Webapp");
		foUserAgent.setAccessibility(true);

		TransformerFactory transformerFactory = DomUtils.getSecureTransformerFactory();

		InputStream simpleIS = FOPService.class.getResourceAsStream("/xslt/pdf/simple-report.xslt");
		templateSimpleReport = transformerFactory.newTemplates(new StreamSource(simpleIS));
		Utils.closeQuietly(simpleIS);

		InputStream detailedIS = FOPService.class.getResourceAsStream("/xslt/pdf/detailed-report.xslt");
		templateDetailedReport = transformerFactory.newTemplates(new StreamSource(detailedIS));
		Utils.closeQuietly(detailedIS);
	}

	public void generateSimpleReport(String simpleReport, OutputStream os) throws Exception {
		Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, os);
		Result result = new SAXResult(fop.getDefaultHandler());
		SimpleReportFacade.newFacade().generatePdfReport(simpleReport, result);
	}

	public void generateSimpleReport(Document dom, OutputStream os) throws Exception {
		Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, os);
		Result res = new SAXResult(fop.getDefaultHandler());
		Transformer transformer = templateSimpleReport.newTransformer();
		transformer.setErrorListener(new DSSXmlErrorListener());
		transformer.transform(new DOMSource(dom), res);
	}

	public void generateDetailedReport(String detailedReport, OutputStream os) throws Exception {
		Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, os);
		Result result = new SAXResult(fop.getDefaultHandler());
		DetailedReportFacade.newFacade().generatePdfReport(detailedReport, result);
	}

}
