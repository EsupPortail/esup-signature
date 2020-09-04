package org.esupportail.esupsignature.dss.service;


import eu.europa.esig.dss.spi.tsl.LOTLInfo;
import eu.europa.esig.dss.spi.tsl.TLValidationJobSummary;
import eu.europa.esig.dss.spi.tsl.TrustedListsCertificateSource;
import eu.europa.esig.dss.spi.x509.CommonTrustedCertificateSource;
import eu.europa.esig.dss.spi.x509.KeyStoreCertificateSource;
import eu.europa.esig.dss.utils.Utils;
import org.esupportail.esupsignature.dss.config.DSSBeanConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

@Service
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

	public void getCertificats() throws IOException {
		log.info("start offline refreshing oj keystore");
		dssBeanConfig.job().offlineRefresh();
		refresh();
		ojContentKeyStore.addAllCertificatesToKeyStore(trustedListsCertificateSource.getCertificates());
		ojContentKeyStore.addAllCertificatesToKeyStore(myTrustedCertificateSource.getCertificates());
		OutputStream fos = new FileOutputStream(dssBeanConfig.getDssProperties().getKsFilename());
		ojContentKeyStore.store(fos);
		Utils.closeQuietly(fos);
		log.info("init trusted lists OK");
	}
	
	public void refresh() throws IOException {
		if(checkOjFreshness()) {
			log.info("start online refreshing oj keystore");
			dssBeanConfig.job().onlineRefresh();
		} else {
			log.info("no online refresh needed for trusted lists");
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
