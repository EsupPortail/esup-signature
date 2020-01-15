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
	
	@Resource
	private List<String> trustedCertificatUrlList;
	
	@Autowired
	private TrustedListsCertificateSource trustedListSource;
	
	@Resource
	private FileService fileService;
	
	//TODO reactive
	//@PostConstruct
	public void getCertificats() throws MalformedURLException, IOException, KeyStoreException, NoSuchProviderException, NoSuchAlgorithmException, CertificateException {
		Security.addProvider(new BouncyCastleProvider());
		List<ServiceInfo> serviceInfos = getServicesInfos();
		File keystoreFile = new File(ksFilename);
		if(keystoreFile.exists()) {
			try {
				KeyStore keystore = KeyStore.getInstance(ksType, "BC");
			    FileInputStream is = new FileInputStream(ksFilename);
			    keystore.load(is, ksPassword.toCharArray());
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
				KeyStoreCertificateSource keyStoreCertificateSource  = new KeyStoreCertificateSource((InputStream) null, ksType, ksPassword);
				keyStoreCertificateSource.addAllCertificatesToKeyStore(trustedListSource.getCertificates());
				OutputStream fos = new FileOutputStream(ksFilename);
				keyStoreCertificateSource.store(fos);
				Utils.closeQuietly(fos);
			}
		}
		refresh();
	}
	
	public void refresh() {
		log.info("start refreshing oj keystore");
		KeyStoreCertificateSource keyStoreCertificateSource  = new KeyStoreCertificateSource((InputStream) null, ksType, ksPassword);
		List<ServiceInfo> serviceInfos = getServicesInfos();
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
			File keystoreFile = new File(ksFilename);
			File keystoreFileSav = new File(ksFilename + ".bak");
			keystoreFileSav.createNewFile();
			fileService.copyFile(keystoreFile, keystoreFileSav);
			
			validationJob.refresh();
			for(String trustedCertificatUrl : trustedCertificatUrlList) {
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
			OutputStream fos = new FileOutputStream(ksFilename);
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
