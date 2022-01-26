package org.esupportail.esupsignature.dss.service;


import eu.europa.esig.dss.spi.tsl.LOTLInfo;
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
		LOTLInfo lotlInfo = summary.getLOTLInfos().get(0);
		return !lotlInfo.getValidationCacheInfo().isValid()
				|| lotlInfo.getValidationCacheInfo().isRefreshNeeded()
				|| lotlInfo.getParsingCacheInfo().isRefreshNeeded()
				|| lotlInfo.getDownloadCacheInfo().isRefreshNeeded();
	}

	@Async
	@EventListener(ApplicationReadyEvent.class)
	public void init() throws EsupSignatureException {
		logger.info("Checking Workflow classes...");
		workflowService.copyClassWorkflowsIntoDatabase();
		logger.info("Check done.");
		logger.info("Updating DSS OJ...");
		getCertificats();
		logger.info("Update done.");
	}

}
