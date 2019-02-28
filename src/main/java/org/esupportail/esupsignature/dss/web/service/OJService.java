package org.esupportail.esupsignature.dss.web.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import eu.europa.esig.dss.DSSException;
import eu.europa.esig.dss.DSSUtils;
import eu.europa.esig.dss.client.http.DataLoader;
import eu.europa.esig.dss.tsl.ServiceInfo;
import eu.europa.esig.dss.tsl.TrustedListsCertificateSource;
import eu.europa.esig.dss.tsl.service.TSLRepository;
import eu.europa.esig.dss.tsl.service.TSLValidationJob;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.x509.CertificateToken;
import eu.europa.esig.dss.x509.KeyStoreCertificateSource;

@Service
public class OJService {

	private static final Logger log = LoggerFactory.getLogger(OJService.class);
	
	@Value("${current.lotl.url}")
	private String lotlUrl;

	@Value("${lotl.country.code}")
	private String lotlCountryCode;

	@Value("${lotl.root.scheme.info.uri}")
	private String lotlRootSchemeInfoUri;

	@Value("${current.oj.url}")
	private String ojUrl;

	@Value("${oj.content.keystore.type}")
	private String ksType;

	@Value("${oj.content.keystore.filename}")
	private String ksFilename;

	@Value("${oj.content.keystore.password}")
	private String ksPassword;
	
	@Autowired
	private DataLoader dataLoader;
	
	@Resource(name="trustedCertificatUrlList")
	private List<String> trustedCertificatUrlList;
	
	@Autowired
	private TrustedListsCertificateSource trustedListSource;
	
	@PostConstruct
	public void refresh() throws MalformedURLException, IOException {
		
		File keystoreFile = new File(ksFilename);
		KeyStoreCertificateSource keyStoreCertificateSource = new KeyStoreCertificateSource(keystoreFile, ksType, ksPassword);
		
		TSLRepository tslRepository = new TSLRepository();
		
		tslRepository.setTrustedListsCertificateSource(trustedListSource);
		
		TSLValidationJob validationJob = new TSLValidationJob();
		validationJob.setDataLoader(dataLoader);
		validationJob.setRepository(tslRepository);
		validationJob.setLotlUrl(lotlUrl);
		validationJob.setLotlRootSchemeInfoUri(lotlRootSchemeInfoUri);
		validationJob.setLotlCode(lotlCountryCode);
		validationJob.setOjUrl(ojUrl);
		validationJob.setOjContentKeyStore(keyStoreCertificateSource);
		validationJob.setCheckLOTLSignature(true);
		validationJob.setCheckTSLSignatures(true);
		
		try {
			validationJob.refresh();
		} catch (DSSException e) {
			log.error("oj refresh error", e);
		}
		
		List<ServiceInfo> serviceInfos = new ArrayList<>();
		serviceInfos.add(new ServiceInfo());
		
		for(String trustedCertificatUrl : trustedCertificatUrlList) {
			InputStream in = new URL(trustedCertificatUrl).openStream();
			CertificateToken certificateToken = DSSUtils.loadCertificate(in);
			if(!trustedListSource.getCertificates().contains(certificateToken)) {
				trustedListSource.addCertificate(certificateToken, serviceInfos);
			}
		}
		
		keyStoreCertificateSource.addAllCertificatesToKeyStore(trustedListSource.getCertificates());
		OutputStream fos = new FileOutputStream(ksFilename);
		keyStoreCertificateSource.store(fos);
		Utils.closeQuietly(fos);
	}
}
