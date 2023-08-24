package org.esupportail.esupsignature.dss.service;

import eu.europa.esig.dss.detailedreport.DetailedReportFacade;
import eu.europa.esig.dss.simplereport.SimpleReportFacade;
import org.apache.fop.apps.*;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.xml.transform.Result;
import javax.xml.transform.sax.SAXResult;
import java.io.File;
import java.io.OutputStream;

@Component
public class FOPService {

	private FopFactory fopFactory;
	private FOUserAgent foUserAgent;

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

}
