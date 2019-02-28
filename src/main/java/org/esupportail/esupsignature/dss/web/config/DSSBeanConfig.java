package org.esupportail.esupsignature.dss.web.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import eu.europa.esig.dss.DSSUtils;
import eu.europa.esig.dss.asic.signature.ASiCWithCAdESService;
import eu.europa.esig.dss.asic.signature.ASiCWithXAdESService;
import eu.europa.esig.dss.cades.signature.CAdESService;
import eu.europa.esig.dss.client.crl.JdbcCacheCRLSource;
import eu.europa.esig.dss.client.crl.OnlineCRLSource;
import eu.europa.esig.dss.client.http.commons.CommonsDataLoader;
import eu.europa.esig.dss.client.http.commons.FileCacheDataLoader;
import eu.europa.esig.dss.client.http.commons.OCSPDataLoader;
import eu.europa.esig.dss.client.http.proxy.ProxyConfig;
import eu.europa.esig.dss.client.ocsp.OnlineOCSPSource;
import eu.europa.esig.dss.client.tsp.OnlineTSPSource;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.signature.RemoteDocumentSignatureServiceImpl;
import eu.europa.esig.dss.signature.RemoteMultipleDocumentsSignatureServiceImpl;
import eu.europa.esig.dss.tsl.ServiceInfo;
import eu.europa.esig.dss.tsl.TrustedListsCertificateSource;
import eu.europa.esig.dss.tsl.service.TSLRepository;
import eu.europa.esig.dss.tsl.service.TSLValidationJob;
import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.validation.RemoteDocumentValidationService;
import eu.europa.esig.dss.x509.CertificateToken;
import eu.europa.esig.dss.x509.KeyStoreCertificateSource;
import eu.europa.esig.dss.x509.crl.CRLSource;
import eu.europa.esig.dss.x509.tsp.TSPSource;
import eu.europa.esig.dss.xades.signature.XAdESService;


@Configuration
public class DSSBeanConfig {
	
	@Value("${default.validation.policy}")
	private String defaultValidationPolicy;

	@Value("${tsp.server}")
	private String tspServer;
	
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

	@Value("${dss.server.signing.keystore.type}")
	private String serverSigningKeystoreType;

	@Value("${dss.server.signing.keystore.filename}")
	private String serverSigningKeystoreFilename;

	@Value("${dss.server.signing.keystore.password}")
	private String serverSigningKeystorePassword;

	@Autowired
	@Qualifier(value="cacheDataSource")
	private DataSource cacheDataSource;
	
	@Autowired
	@Qualifier(value="dataSource")
	private DataSource dataSource;

	// can be null
	@Autowired(required = false)
	private ProxyConfig proxyConfig;
	
	@Resource(name="trustedCertificatUrlList")
	private List<String> trustedCertificatUrlList;
	
	@Bean
	public CommonsDataLoader dataLoader() {
		CommonsDataLoader dataLoader = new CommonsDataLoader();
		dataLoader.setProxyConfig(proxyConfig);
		return dataLoader;
	}

	@Bean
	public OCSPDataLoader ocspDataLoader() {
		OCSPDataLoader ocspDataLoader = new OCSPDataLoader();
		ocspDataLoader.setProxyConfig(proxyConfig);
		return ocspDataLoader;
	}
/*
	@Bean
	public FileCacheDataLoader fileCacheDataLoader() {
		FileCacheDataLoader fileCacheDataLoader = new FileCacheDataLoader();
		fileCacheDataLoader.setDataLoader(dataLoader());
		// Per default uses "java.io.tmpdir" property
		// fileCacheDataLoader.setFileCacheDirectory(new File("/tmp"));
		return fileCacheDataLoader;
	}
*/
	@Bean
	public OnlineCRLSource onlineCRLSource() {
		OnlineCRLSource onlineCRLSource = new OnlineCRLSource();
		onlineCRLSource.setDataLoader(dataLoader());
		return onlineCRLSource;
	}

	@Bean
	public CRLSource cachedCRLSource() throws Exception {
		JdbcCacheCRLSource jdbcCacheCRLSource = new JdbcCacheCRLSource();
		jdbcCacheCRLSource.setDataSource(cacheDataSource);
		jdbcCacheCRLSource.setCachedSource(onlineCRLSource());
		return jdbcCacheCRLSource;
	}

	@Bean
	public OnlineOCSPSource ocspSource() {
		OnlineOCSPSource onlineOCSPSource = new OnlineOCSPSource();
		onlineOCSPSource.setDataLoader(ocspDataLoader());
		return onlineOCSPSource;
	}

	@Bean
	public TrustedListsCertificateSource trustedListSource() throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
		TSLRepository tslRepository = new TSLRepository();
		TrustedListsCertificateSource certificateSource = new TrustedListsCertificateSource();
		tslRepository.setTrustedListsCertificateSource(certificateSource);
		List<ServiceInfo> serviceInfos = new ArrayList<>();
		serviceInfos.add(new ServiceInfo());
		for(String trustedCertificatUrl : trustedCertificatUrlList) {
			InputStream in = new URL(trustedCertificatUrl).openStream();
			CertificateToken certificateToken = DSSUtils.loadCertificate(in);
			certificateSource.addCertificate(certificateToken, serviceInfos);
		}

		KeyStoreCertificateSource keyStoreCertificateSource; 
		File keystoreFile = new File("src/main/resources/keystore.p12");
		if(keystoreFile.exists()) {
			keyStoreCertificateSource = new KeyStoreCertificateSource(keystoreFile, ksType, ksPassword);
		}else {
			keyStoreCertificateSource  = new KeyStoreCertificateSource((InputStream) null, ksType, ksPassword);
		}

		TSLValidationJob validationJob = new TSLValidationJob();
		validationJob.setDataLoader(dataLoader());
		validationJob.setRepository(tslRepository);
		validationJob.setLotlUrl(lotlUrl);
		validationJob.setLotlRootSchemeInfoUri(lotlRootSchemeInfoUri);
		validationJob.setLotlCode(lotlCountryCode);
		validationJob.setOjUrl(ojUrl);
		validationJob.setOjContentKeyStore(keyStoreCertificateSource);
		validationJob.setCheckLOTLSignature(true);
		validationJob.setCheckTSLSignatures(true);
		validationJob.refresh();
		
		keyStoreCertificateSource.addAllCertificatesToKeyStore(certificateSource.getCertificates());
		/*
		OutputStream fos = new FileOutputStream("src/main/resources/keystore.p12");
		keyStoreCertificateSource.store(fos);
		Utils.closeQuietly(fos);
		*/
		return certificateSource;
	}

	@Bean
	public CertificateVerifier certificateVerifier() throws Exception {
		CommonCertificateVerifier certificateVerifier = new CommonCertificateVerifier();
		certificateVerifier.setTrustedCertSource(trustedListSource());
		certificateVerifier.setCrlSource(cachedCRLSource());
		certificateVerifier.setOcspSource(ocspSource());
		certificateVerifier.setDataLoader(dataLoader());

		// Default configs
		certificateVerifier.setExceptionOnMissingRevocationData(true);
		certificateVerifier.setCheckRevocationForUntrustedChains(true);

		return certificateVerifier;
	}

	@Bean
	public ClassPathResource defaultPolicy() {
		return new ClassPathResource(defaultValidationPolicy);
	}

	@Bean TSPSource tspSource() {
		OnlineTSPSource tspSource = new OnlineTSPSource(tspServer);
		return tspSource;
	}
	
	@Bean
	public CAdESService cadesService() throws Exception {
		CAdESService service = new CAdESService(certificateVerifier());
		service.setTspSource(tspSource());
		return service;
	}

	@Bean
	public XAdESService xadesService() throws Exception {
		XAdESService service = new XAdESService(certificateVerifier());
		service.setTspSource(tspSource());
		return service;
	}

	@Bean
	public PAdESService padesService() throws Exception {
		PAdESService service = new PAdESService(certificateVerifier());
		service.setTspSource(tspSource());
		return service;
	}

	@Bean
	public ASiCWithCAdESService asicWithCadesService() throws Exception {
		ASiCWithCAdESService service = new ASiCWithCAdESService(certificateVerifier());
		service.setTspSource(tspSource());
		return service;
	}

	@Bean
	public ASiCWithXAdESService asicWithXadesService() throws Exception {
		ASiCWithXAdESService service = new ASiCWithXAdESService(certificateVerifier());
		service.setTspSource(tspSource());
		return service;
	}

	@Bean
	public RemoteDocumentSignatureServiceImpl remoteSignatureService() throws Exception {
		RemoteDocumentSignatureServiceImpl service = new RemoteDocumentSignatureServiceImpl();
		service.setAsicWithCAdESService(asicWithCadesService());
		service.setAsicWithXAdESService(asicWithXadesService());
		service.setCadesService(cadesService());
		service.setXadesService(xadesService());
		service.setPadesService(padesService());
		return service;
	}

	@Bean
	public RemoteMultipleDocumentsSignatureServiceImpl remoteMultipleDocumentsSignatureService() throws Exception {
		RemoteMultipleDocumentsSignatureServiceImpl service = new RemoteMultipleDocumentsSignatureServiceImpl();
		service.setAsicWithCAdESService(asicWithCadesService());
		service.setAsicWithXAdESService(asicWithXadesService());
		service.setXadesService(xadesService());
		return service;
	}

	@Bean
	public RemoteDocumentValidationService remoteValidationService() throws Exception {
		RemoteDocumentValidationService service = new RemoteDocumentValidationService();
		service.setVerifier(certificateVerifier());
		return service;
	}

/*
	@Bean
	public KeyStoreSignatureTokenConnection remoteToken() throws IOException {
		return new KeyStoreSignatureTokenConnection(new ClassPathResource(serverSigningKeystoreFilename).getFile(), serverSigningKeystoreType,
				new PasswordProtection(serverSigningKeystorePassword.toCharArray()));
	}

	@Bean
	public RemoteSignatureTokenConnection serverToken() throws IOException {
		RemoteSignatureTokenConnectionImpl remoteSignatureTokenConnectionImpl = new RemoteSignatureTokenConnectionImpl();
		remoteSignatureTokenConnectionImpl.setToken(remoteToken());
		return remoteSignatureTokenConnectionImpl;
	}
*/
	
	@Bean
	public TSLRepository tslRepository(TrustedListsCertificateSource trustedListSource) {
		TSLRepository tslRepository = new TSLRepository();
		tslRepository.setTrustedListsCertificateSource(trustedListSource);
		return tslRepository;
	}
	


}