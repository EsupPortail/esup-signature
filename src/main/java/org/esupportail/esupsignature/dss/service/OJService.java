package org.esupportail.esupsignature.dss.service;


import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.annotation.Resource;

import org.esupportail.esupsignature.dss.config.DSSBeanConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import eu.europa.esig.dss.spi.tsl.LOTLInfo;
import eu.europa.esig.dss.spi.tsl.TLValidationJobSummary;
import eu.europa.esig.dss.spi.tsl.TrustedListsCertificateSource;
import eu.europa.esig.dss.spi.x509.CommonTrustedCertificateSource;
import eu.europa.esig.dss.spi.x509.KeyStoreCertificateSource;
import eu.europa.esig.dss.utils.Utils;

@Service
@ConditionalOnBean(DSSBeanConfig.class)
public class OJService {

	private static final Logger log = LoggerFactory.getLogger(OJService.class);

	@Resource
	private DSSBeanConfig dssBeanConfig;

	@Resource
	@Qualifier("european-trusted-list-certificate-source")
	private TrustedListsCertificateSource trustedListsCertificateSource;

	@Resource
	private KeyStoreCertificateSource ojContentKeyStore;

	@Resource
	private CommonTrustedCertificateSource myTrustedCertificateSource;

	public void getCertificats() {
		try {
			log.info("start offline refreshing oj keystore");
			dssBeanConfig.job().offlineRefresh();
			refresh();
			ojContentKeyStore.addAllCertificatesToKeyStore(trustedListsCertificateSource.getCertificates());
			ojContentKeyStore.addAllCertificatesToKeyStore(myTrustedCertificateSource.getCertificates());
			OutputStream fos = new FileOutputStream(dssBeanConfig.getDssProperties().getKsFilename());
			ojContentKeyStore.store(fos);
			Utils.closeQuietly(fos);
			log.info("init trusted lists OK");
		} catch(IOException e) {
			log.error("Error getting certificats", e);
		}
	}
	
	public void refresh() {
		try {
			if(checkOjFreshness()) {
				log.info("start online refreshing oj keystore");
				dssBeanConfig.job().onlineRefresh();
			} else {
				log.info("no online refresh needed for trusted lists");
			}
		} catch(IOException e) {
			log.error("Error refreshing dss", e);
		}
	}	

	public boolean checkOjFreshness() {
		TLValidationJobSummary summary = trustedListsCertificateSource.getSummary();
		LOTLInfo lotlInfo = summary.getLOTLInfos().get(0);
		return lotlInfo.getValidationCacheInfo().isRefreshNeeded()
				|| lotlInfo.getParsingCacheInfo().isRefreshNeeded()
				|| lotlInfo.getDownloadCacheInfo().isRefreshNeeded();
	}

}
