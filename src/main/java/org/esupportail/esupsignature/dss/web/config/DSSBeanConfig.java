package org.esupportail.esupsignature.dss.web.config;

import com.zaxxer.hikari.HikariDataSource;
import eu.europa.esig.dss.asic.signature.ASiCWithCAdESService;
import eu.europa.esig.dss.asic.signature.ASiCWithXAdESService;
import eu.europa.esig.dss.cades.signature.CAdESService;
import eu.europa.esig.dss.client.crl.JdbcCacheCRLSource;
import eu.europa.esig.dss.client.crl.OnlineCRLSource;
import eu.europa.esig.dss.client.http.commons.CommonsDataLoader;
import eu.europa.esig.dss.client.http.commons.OCSPDataLoader;
import eu.europa.esig.dss.client.http.proxy.ProxyConfig;
import eu.europa.esig.dss.client.ocsp.OnlineOCSPSource;
import eu.europa.esig.dss.client.tsp.OnlineTSPSource;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.signature.RemoteDocumentSignatureServiceImpl;
import eu.europa.esig.dss.signature.RemoteMultipleDocumentsSignatureServiceImpl;
import eu.europa.esig.dss.tsl.TrustedListsCertificateSource;
import eu.europa.esig.dss.tsl.service.TSLRepository;
import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.validation.RemoteDocumentValidationService;
import eu.europa.esig.dss.x509.crl.CRLSource;
import eu.europa.esig.dss.x509.tsp.TSPSource;
import eu.europa.esig.dss.xades.signature.XAdESService;
import org.esupportail.esupsignature.config.tomcat.TomcatAjpProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableConfigurationProperties(DSSProperties.class)
public class DSSBeanConfig {

	private DSSProperties dssProperties;

	public DSSBeanConfig(DSSProperties dssProperties) {
		this.dssProperties = dssProperties;
	}

//	@Autowired(required = false)
//	private ProxyConfig proxyConfig;
	
	@Bean
	public List<String> trustedCertificatUrlList() {
		List<String> trustedCertificatUrlList = new ArrayList<>();
		trustedCertificatUrlList.add("https://dl.cacerts.digicert.com/DigiCertAssuredIDRootCA.crt");
		trustedCertificatUrlList.add("https://www.certinomis.fr/publi/cer/AC_Racine_G3.cer");
		return trustedCertificatUrlList;
		
	}
	
	@Bean
	public CommonsDataLoader dataLoader() {
		CommonsDataLoader dataLoader = new CommonsDataLoader();
		return dataLoader;
	}

	@Bean
	public OCSPDataLoader ocspDataLoader() {
		OCSPDataLoader ocspDataLoader = new OCSPDataLoader();
		return ocspDataLoader;
	}

	@Bean
	public OnlineCRLSource onlineCRLSource() {
		OnlineCRLSource onlineCRLSource = new OnlineCRLSource();
		onlineCRLSource.setDataLoader(dataLoader());
		return onlineCRLSource;
	}

	@Bean
	public CRLSource cachedCRLSource() throws Exception {
		JdbcCacheCRLSource jdbcCacheCRLSource = new JdbcCacheCRLSource();
		jdbcCacheCRLSource.setDataSource(cacheDataSource());
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
	public TrustedListsCertificateSource trustedListSource() {
		TSLRepository tslRepository = new TSLRepository();
		TrustedListsCertificateSource certificateSource = new TrustedListsCertificateSource();
		tslRepository.setTrustedListsCertificateSource(certificateSource);
		return certificateSource;
	}

	@Bean
	public CertificateVerifier certificateVerifier() throws Exception {
		CommonCertificateVerifier certificateVerifier = new CommonCertificateVerifier();
		certificateVerifier.setTrustedCertSource(trustedListSource());
		certificateVerifier.setCrlSource(cachedCRLSource());
		certificateVerifier.setOcspSource(ocspSource());
		certificateVerifier.setDataLoader(dataLoader());

		certificateVerifier.setExceptionOnMissingRevocationData(true);
		certificateVerifier.setCheckRevocationForUntrustedChains(true);

		return certificateVerifier;
	}

	@Bean
	public ClassPathResource defaultPolicy() {
		return new ClassPathResource(dssProperties.getDefaultValidationPolicy());
	}

	@Bean TSPSource tspSource() {
		OnlineTSPSource tspSource = new OnlineTSPSource(dssProperties.getTspServer());
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
	
	public DataSource cacheDataSource() {
		HikariDataSource ds = new HikariDataSource();
		ds.setPoolName("DSS-Hikari-Pool");
		ds.setJdbcUrl(dssProperties.getCacheDataSourceUrl());
		ds.setDriverClassName(dssProperties.getCacheDataSourceDriverClassName());
		ds.setUsername(dssProperties.getCacheUsername());
		ds.setPassword(dssProperties.getCachePassword());
		ds.setAutoCommit(false);
		return ds;
	}

}