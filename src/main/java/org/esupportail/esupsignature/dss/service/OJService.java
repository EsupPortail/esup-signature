package org.esupportail.esupsignature.dss.service;


import eu.europa.esig.dss.spi.tsl.LOTLInfo;
import eu.europa.esig.dss.spi.tsl.TLInfo;
import eu.europa.esig.dss.spi.tsl.TLValidationJobSummary;
import eu.europa.esig.dss.spi.x509.CommonTrustedCertificateSource;
import eu.europa.esig.dss.spi.x509.KeyStoreCertificateSource;
import eu.europa.esig.dss.tsl.job.TLValidationJob;
import jakarta.annotation.Resource;
import org.esupportail.esupsignature.dss.config.DSSBeanConfig;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.WorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
	private KeyStoreCertificateSource ojContentKeyStore;

	@Resource
	private CommonTrustedCertificateSource myTrustedCertificateSource;

	public void getCertificats() throws IOException {
		ojContentKeyStore.addAllCertificatesToKeyStore(myTrustedCertificateSource.getCertificates());
		job.offlineRefresh();
		if(refreshIsNeeded()) {
			job.onlineRefresh();
		}
	}

	public boolean refreshIsNeeded() throws IOException {
		TLValidationJobSummary summary = job.getSummary();
		if(summary == null) return true;
		boolean checkTl = false;
		for (LOTLInfo lotlInfo : summary.getLOTLInfos()) {
			if(!lotlInfo.getValidationCacheInfo().isValid()
					|| lotlInfo.getValidationCacheInfo().isRefreshNeeded()
					|| lotlInfo.getParsingCacheInfo().isRefreshNeeded()
					|| lotlInfo.getDownloadCacheInfo().isRefreshNeeded()) {
				checkTl = true;
			}
		}
		for (TLInfo tlInfo : summary.getOtherTLInfos()) {
			if(!tlInfo.getValidationCacheInfo().isValid()
					|| tlInfo.getValidationCacheInfo().isRefreshNeeded()
					|| tlInfo.getParsingCacheInfo().isRefreshNeeded()
					|| tlInfo.getDownloadCacheInfo().isRefreshNeeded()) {
				checkTl = true;
			}
		}
		return checkTl;
	}

	@Async
	@EventListener(ApplicationReadyEvent.class)
	public void init() throws EsupSignatureRuntimeException, IOException {
		logger.info("Checking Workflow classes...");
		workflowService.copyClassWorkflowsIntoDatabase();
		logger.info("Check done.");
		logger.info("Updating DSS OJ...");
		getCertificats();
		logger.info("Update done.");
	}

}
