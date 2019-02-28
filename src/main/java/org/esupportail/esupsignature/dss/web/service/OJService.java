package org.esupportail.esupsignature.dss.web.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
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
	
	@Scheduled(fixedDelay=Long.MAX_VALUE)
	public void getCertificats() throws MalformedURLException, IOException {
		Security.addProvider(new BouncyCastleProvider());
		List<ServiceInfo> serviceInfos = getServicesInfos();
		File keystoreFile = new File(ksFilename);
		KeyStoreCertificateSource keyStoreCertificateSource;
		if(keystoreFile.exists()) {
			try {
				keyStoreCertificateSource = new KeyStoreCertificateSource(keystoreFile, ksType, ksPassword);
				for(CertificateToken certificateToken : keyStoreCertificateSource.getCertificates()) {
					trustedListSource.addCertificate(certificateToken, serviceInfos);
				}
				log.info("Retrieve certificats from oj keystore OK");
			} catch (DSSException e) {
				log.error("Error on retrieve certificats from oj keystore", e);
			}
		} else {
			log.info("create oj keystore");
			File parent = keystoreFile.getParentFile();
			if (parent != null && !parent.exists() && !parent.mkdirs()) {
			    log.error("Couldn't create dir: " + parent);
			} else {
				keystoreFile.createNewFile();
				keyStoreCertificateSource  = new KeyStoreCertificateSource((InputStream) null, ksType, ksPassword);
				keyStoreCertificateSource.addAllCertificatesToKeyStore(trustedListSource.getCertificates());
				OutputStream fos = new FileOutputStream(ksFilename);
				keyStoreCertificateSource.store(fos);
				Utils.closeQuietly(fos);
			}
		}
		refresh();
	}
	
	public void refresh() throws MalformedURLException, IOException {
		log.info("start refreshing oj keystore");
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
		
		List<ServiceInfo> serviceInfos = getServicesInfos();
		
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
	
	public List<ServiceInfo> getServicesInfos() {
		List<ServiceInfo> serviceInfos = new ArrayList<>();
		serviceInfos.add(new ServiceInfo());
		return serviceInfos;
	}
}
