package org.esupportail.esupsignature.dss.web.service;


import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.spi.tsl.*;
import eu.europa.esig.dss.spi.util.TimeDependentValues;
import eu.europa.esig.dss.spi.x509.CertificateSource;
import eu.europa.esig.dss.spi.x509.CommonTrustedCertificateSource;
import eu.europa.esig.dss.spi.x509.KeyStoreCertificateSource;
import eu.europa.esig.dss.tsl.source.LOTLSource;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.CertificateVerifier;
import org.esupportail.esupsignature.dss.web.config.DSSBeanConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

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
	private CertificateVerifier certificateVerifier;

	public void getCertificats() throws IOException {
		log.info("start offline refreshing oj keystore");
		dssBeanConfig.job().offlineRefresh();
		ojContentKeyStore.addAllCertificatesToKeyStore(trustedListsCertificateSource.getCertificates());
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
