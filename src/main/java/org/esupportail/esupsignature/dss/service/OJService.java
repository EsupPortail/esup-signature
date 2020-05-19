package org.esupportail.esupsignature.dss.service;


import eu.europa.esig.dss.spi.tsl.*;
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
		dssBeanConfig.job().onlineRefresh();
		ojContentKeyStore.addAllCertificatesToKeyStore(trustedListsCertificateSource.getCertificates());
		ojContentKeyStore.addAllCertificatesToKeyStore(myTrustedCertificateSource.getCertificates());
		OutputStream fos = new FileOutputStream(dssBeanConfig.getDssProperties().getKsFilename());
		ojContentKeyStore.store(fos);
		Utils.closeQuietly(fos);
		log.info("refreshing oj keystore OK");
	}
	
	public void refresh() throws IOException {
		log.info("start online refreshing oj keystore");
		dssBeanConfig.job().onlineRefresh();
	}

}
