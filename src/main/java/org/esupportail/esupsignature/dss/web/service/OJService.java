package org.esupportail.esupsignature.dss.web.service;

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
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.esupportail.esupsignature.service.file.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

@Service
@EnableConfigurationProperties(OJProperties.class)
public class OJService {

	private static final Logger log = LoggerFactory.getLogger(OJService.class);

	private OJProperties ojProperties;

	public OJService(OJProperties ojProperties) {
		this.ojProperties = ojProperties;
	}

	@Resource
	private DataLoader dataLoader;

	@Resource
	private TrustedListsCertificateSource trustedListSource;
	
	@Resource
	private FileService fileService;

	public void getCertificats() throws IOException, KeyStoreException, NoSuchProviderException, NoSuchAlgorithmException, CertificateException {
		Security.addProvider(new BouncyCastleProvider());
		List<ServiceInfo> serviceInfos = getServicesInfos();
		File keystoreFile = new File(ojProperties.getKsFilename());
		if(keystoreFile.exists()) {
			try {
				KeyStore keystore = KeyStore.getInstance(ojProperties.getKsType(), "BC");
			    FileInputStream is = new FileInputStream(ojProperties.getKsFilename());
			    keystore.load(is, ojProperties.getKsPassword().toCharArray());
			    Enumeration<String> aliases = keystore.aliases();
				while (aliases.hasMoreElements()) {
					trustedListSource.addCertificate(DSSUtils.loadCertificate(keystore.getCertificate(aliases.nextElement()).getEncoded()), serviceInfos);
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
				KeyStoreCertificateSource keyStoreCertificateSource  = new KeyStoreCertificateSource((InputStream) null, ojProperties.getKsType(), ojProperties.getKsPassword());
				keyStoreCertificateSource.addAllCertificatesToKeyStore(trustedListSource.getCertificates());
				OutputStream fos = new FileOutputStream(ojProperties.getKsFilename());
				keyStoreCertificateSource.store(fos);
				Utils.closeQuietly(fos);
			}
			refresh();
		}
	}
	
	public void refresh() {
		log.info("start refreshing oj keystore");
		KeyStoreCertificateSource keyStoreCertificateSource  = new KeyStoreCertificateSource((InputStream) null, ojProperties.getKsType(), ojProperties.getKsPassword());
		List<ServiceInfo> serviceInfos = getServicesInfos();
		TSLRepository tslRepository = new TSLRepository();
		tslRepository.setTrustedListsCertificateSource(trustedListSource);
		
		TSLValidationJob validationJob = new TSLValidationJob();
		validationJob.setDataLoader(dataLoader);
		validationJob.setRepository(tslRepository);
		validationJob.setLotlUrl(ojProperties.getLotlUrl());
		validationJob.setLotlRootSchemeInfoUri(ojProperties.getLotlRootSchemeInfoUri());
		validationJob.setLotlCode(ojProperties.getLotlCountryCode());
		validationJob.setOjUrl(ojProperties.getOjUrl());
		validationJob.setOjContentKeyStore(keyStoreCertificateSource);
		validationJob.setCheckLOTLSignature(true);
		validationJob.setCheckTSLSignatures(true);
		
		try {
			File keystoreFile = new File(ojProperties.getKsFilename());
			File keystoreFileSav = new File(ojProperties.getKsFilename() + ".bak");
			keystoreFileSav.createNewFile();
			fileService.copyFile(keystoreFile, keystoreFileSav);
			
			validationJob.refresh();
			for(String trustedCertificatUrl : ojProperties.getTrustedCertificatUrlList()) {
				InputStream in = new URL(trustedCertificatUrl).openStream();
				CertificateToken certificateToken = DSSUtils.loadCertificate(in);
				if(!trustedListSource.getCertificates().contains(certificateToken)) {
					trustedListSource.addCertificate(certificateToken, serviceInfos);
					log.info(trustedCertificatUrl + " added to truststore");
				} else {
					log.info(trustedCertificatUrl + " already in truststore");
				}
			}
			keyStoreCertificateSource.addAllCertificatesToKeyStore(trustedListSource.getCertificates());
			OutputStream fos = new FileOutputStream(ojProperties.getKsFilename());
			keyStoreCertificateSource.store(fos);
			Utils.closeQuietly(fos);
			log.info("refreshing oj keystore OK");
		} catch (DSSException | IOException e) {
			log.error("oj refresh error", e);
		}
	}
	
	public List<ServiceInfo> getServicesInfos() {
		List<ServiceInfo> serviceInfos = new ArrayList<>();
		serviceInfos.add(new ServiceInfo());
		return serviceInfos;
	}
}
