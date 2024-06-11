package org.esupportail.esupsignature.dss.service;


import eu.europa.esig.dss.spi.tsl.LOTLInfo;
import eu.europa.esig.dss.spi.tsl.TLInfo;
import eu.europa.esig.dss.spi.tsl.TLValidationJobSummary;
import eu.europa.esig.dss.spi.x509.CommonTrustedCertificateSource;
import eu.europa.esig.dss.spi.x509.KeyStoreCertificateSource;
import eu.europa.esig.dss.tsl.job.TLValidationJob;
import org.esupportail.esupsignature.dss.config.DSSBeanConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@ConditionalOnBean(DSSBeanConfig.class)
public class OJService {

	private static final Logger logger = LoggerFactory.getLogger(OJService.class);

	private final TLValidationJob job;

	private final KeyStoreCertificateSource ojContentKeyStore;

	private final CommonTrustedCertificateSource myTrustedCertificateSource;

	public OJService(TLValidationJob job, KeyStoreCertificateSource ojContentKeyStore, CommonTrustedCertificateSource myTrustedCertificateSource) {
		this.job = job;
		this.ojContentKeyStore = ojContentKeyStore;
		this.myTrustedCertificateSource = myTrustedCertificateSource;
	}

	public void getCertificats() throws IOException {
		logger.info("Updating DSS OJ...");
		ojContentKeyStore.addAllCertificatesToKeyStore(myTrustedCertificateSource.getCertificates());
		job.offlineRefresh();
		logger.info("Updating DSS OJ offline done.");
		if(refreshIsNeeded()) {
			job.onlineRefresh();
			logger.info("Updating DSS OJ online done.");
		}
		logger.info("Updating DSS OJ done.");

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
	@EventListener(ContextRefreshedEvent.class)
	public void onApplicationEvent(ContextRefreshedEvent event) {
		try {
			getCertificats();
		} catch (IOException e) {
			logger.error("Error updating certificates", e);
		}	}

}
