package org.esupportail.esupsignature.dss.service;


import eu.europa.esig.dss.jaxb.common.DocumentBuilderFactoryBuilder;
import eu.europa.esig.dss.jaxb.common.SchemaFactoryBuilder;
import eu.europa.esig.dss.jaxb.common.ValidatorConfigurator;
import eu.europa.esig.dss.jaxb.common.XmlDefinerUtils;
import eu.europa.esig.dss.spi.tsl.LOTLInfo;
import eu.europa.esig.dss.spi.tsl.TLInfo;
import eu.europa.esig.dss.spi.tsl.TLValidationJobSummary;
import eu.europa.esig.dss.spi.tsl.TrustedListsCertificateSource;
import eu.europa.esig.dss.spi.x509.CommonTrustedCertificateSource;
import eu.europa.esig.dss.spi.x509.KeyStoreCertificateSource;
import eu.europa.esig.dss.tsl.job.TLValidationJob;
import org.esupportail.esupsignature.dss.config.DSSBeanConfig;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.service.WorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.xml.XMLConstants;
import java.io.IOException;

@Service
@ConditionalOnBean(DSSBeanConfig.class)
public class OJService {

	private static final Logger logger = LoggerFactory.getLogger(OJService.class);

	@Resource
	private WorkflowService workflowService;

	@Resource
	private TLValidationJob job;

	@Resource
	@Qualifier("european-trusted-list-certificate-source")
	private TrustedListsCertificateSource trustedListsCertificateSource;

	@Resource
	private KeyStoreCertificateSource ojContentKeyStore;

	@Resource
	private CommonTrustedCertificateSource myTrustedCertificateSource;

	public void getCertificats() {
		ojContentKeyStore.addAllCertificatesToKeyStore(myTrustedCertificateSource.getCertificates());
		job.offlineRefresh();
		job.onlineRefresh();
	}
	
	public void refresh() {
		try {
			if(checkOjFreshness()) {
				logger.info("start online refreshing oj keystore");
				job.onlineRefresh();
			} else {
				logger.info("no online refresh needed for trusted lists");
			}
		} catch(IOException e) {
			logger.error("Error refreshing dss", e);
		}
	}	

	public boolean checkOjFreshness() throws IOException {
		TLValidationJobSummary summary = trustedListsCertificateSource.getSummary();
		if(summary == null) return true;
		boolean checkTl = false;
		for (LOTLInfo lotlInfo : trustedListsCertificateSource.getSummary().getLOTLInfos()) {
			if(lotlInfo.getValidationCacheInfo().isRefreshNeeded()) {
				checkTl = !lotlInfo.getValidationCacheInfo().isValid()
						|| lotlInfo.getValidationCacheInfo().isRefreshNeeded()
						|| lotlInfo.getParsingCacheInfo().isRefreshNeeded()
						|| lotlInfo.getDownloadCacheInfo().isRefreshNeeded();
				break;
			}
		}
		for (TLInfo tlInfo : trustedListsCertificateSource.getSummary().getOtherTLInfos()) {
			if(tlInfo.getValidationCacheInfo().isRefreshNeeded()) {
				checkTl = !tlInfo.getValidationCacheInfo().isValid()
						|| tlInfo.getValidationCacheInfo().isRefreshNeeded()
						|| tlInfo.getParsingCacheInfo().isRefreshNeeded()
						|| tlInfo.getDownloadCacheInfo().isRefreshNeeded();
				break;
			}
		}
		return checkTl;
	}

	@Async
	@EventListener(ApplicationReadyEvent.class)
	public void init() throws EsupSignatureException {
		XmlDefinerUtils xmlDefinerUtils = XmlDefinerUtils.getInstance();
		DocumentBuilderFactoryBuilder documentBuilderFactoryBuilder = DocumentBuilderFactoryBuilder.getSecureDocumentBuilderFactoryBuilder();
		documentBuilderFactoryBuilder.removeAttribute(XMLConstants.ACCESS_EXTERNAL_DTD);
		documentBuilderFactoryBuilder.removeAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA);
		xmlDefinerUtils.setDocumentBuilderFactoryBuilder(documentBuilderFactoryBuilder);
		SchemaFactoryBuilder schemaFactoryBuilder = SchemaFactoryBuilder.getSecureSchemaBuilder();
		schemaFactoryBuilder.removeAttribute(XMLConstants.ACCESS_EXTERNAL_DTD);
		schemaFactoryBuilder.removeAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA);
		xmlDefinerUtils.setSchemaFactoryBuilder(schemaFactoryBuilder);
		ValidatorConfigurator validatorConfigurator = ValidatorConfigurator.getSecureValidatorConfigurator();
		validatorConfigurator.removeAttribute(XMLConstants.ACCESS_EXTERNAL_DTD);
		validatorConfigurator.removeAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA);
		xmlDefinerUtils.setValidatorConfigurator(validatorConfigurator);
		logger.info("Checking Workflow classes...");
		workflowService.copyClassWorkflowsIntoDatabase();
		logger.info("Check done.");
		logger.info("Updating DSS OJ...");
		getCertificats();
		logger.info("Update done.");
	}

}
