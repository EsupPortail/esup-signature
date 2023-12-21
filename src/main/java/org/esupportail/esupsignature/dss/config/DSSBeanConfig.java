package org.esupportail.esupsignature.dss.config;

import com.zaxxer.hikari.HikariDataSource;
import eu.europa.esig.dss.alert.ExceptionOnStatusAlert;
import eu.europa.esig.dss.asic.cades.signature.ASiCWithCAdESService;
import eu.europa.esig.dss.asic.xades.signature.ASiCWithXAdESService;
import eu.europa.esig.dss.cades.signature.CAdESService;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.service.crl.JdbcCacheCRLSource;
import eu.europa.esig.dss.service.crl.OnlineCRLSource;
import eu.europa.esig.dss.service.http.commons.CommonsDataLoader;
import eu.europa.esig.dss.service.http.commons.FileCacheDataLoader;
import eu.europa.esig.dss.service.http.commons.OCSPDataLoader;
import eu.europa.esig.dss.service.http.commons.TimestampDataLoader;
import eu.europa.esig.dss.service.http.proxy.ProxyConfig;
import eu.europa.esig.dss.service.ocsp.OnlineOCSPSource;
import eu.europa.esig.dss.service.tsp.OnlineTSPSource;
import eu.europa.esig.dss.service.x509.aia.JdbcCacheAIASource;
import eu.europa.esig.dss.spi.client.http.DSSFileLoader;
import eu.europa.esig.dss.spi.client.http.IgnoreDataLoader;
import eu.europa.esig.dss.spi.client.jdbc.JdbcCacheConnector;
import eu.europa.esig.dss.spi.tsl.TrustedListsCertificateSource;
import eu.europa.esig.dss.spi.x509.KeyStoreCertificateSource;
import eu.europa.esig.dss.spi.x509.aia.AIASource;
import eu.europa.esig.dss.spi.x509.aia.DefaultAIASource;
import eu.europa.esig.dss.spi.x509.tsp.TSPSource;
import eu.europa.esig.dss.tsl.alerts.LOTLAlert;
import eu.europa.esig.dss.tsl.alerts.TLAlert;
import eu.europa.esig.dss.tsl.alerts.detections.LOTLLocationChangeDetection;
import eu.europa.esig.dss.tsl.alerts.detections.OJUrlChangeDetection;
import eu.europa.esig.dss.tsl.alerts.detections.TLExpirationDetection;
import eu.europa.esig.dss.tsl.alerts.detections.TLSignatureErrorDetection;
import eu.europa.esig.dss.tsl.alerts.handlers.log.LogLOTLLocationChangeAlertHandler;
import eu.europa.esig.dss.tsl.alerts.handlers.log.LogOJUrlChangeAlertHandler;
import eu.europa.esig.dss.tsl.alerts.handlers.log.LogTLExpirationAlertHandler;
import eu.europa.esig.dss.tsl.alerts.handlers.log.LogTLSignatureErrorAlertHandler;
import eu.europa.esig.dss.tsl.function.OfficialJournalSchemeInformationURI;
import eu.europa.esig.dss.tsl.job.TLValidationJob;
import eu.europa.esig.dss.tsl.source.LOTLSource;
import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.validation.RevocationDataVerifier;
import eu.europa.esig.dss.validation.SignaturePolicyProvider;
import eu.europa.esig.dss.xades.signature.XAdESService;
import eu.europa.esig.dss.xml.common.DocumentBuilderFactoryBuilder;
import eu.europa.esig.dss.xml.common.SchemaFactoryBuilder;
import eu.europa.esig.dss.xml.common.ValidatorConfigurator;
import eu.europa.esig.dss.xml.common.XmlDefinerUtils;
import jakarta.annotation.PostConstruct;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import javax.xml.XMLConstants;
import java.io.File;
import java.io.IOException;

@Component
@EnableConfigurationProperties(DSSProperties.class)
public class DSSBeanConfig {

	private static final Logger logger = LoggerFactory.getLogger(DSSBeanConfig.class);

	private final DSSProperties dssProperties;

	private final ProxyConfig proxyConfig;

	public DSSBeanConfig(DSSProperties dssProperties, @Autowired(required = false) ProxyConfig proxyConfig) {
		this.dssProperties = dssProperties;
		this.proxyConfig = proxyConfig;
	}

	@PostConstruct
	public void setXmlSecurity() {
		XmlDefinerUtils xmlDefinerUtils = XmlDefinerUtils.getInstance();
		DocumentBuilderFactoryBuilder documentBuilderFactoryBuilder = DocumentBuilderFactoryBuilder.getSecureDocumentBuilderFactoryBuilder();
		documentBuilderFactoryBuilder.removeAttribute(XMLConstants.ACCESS_EXTERNAL_DTD);
		documentBuilderFactoryBuilder.removeAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA);
		xmlDefinerUtils.setDocumentBuilderFactoryBuilder(documentBuilderFactoryBuilder);
		SchemaFactoryBuilder schemaFactoryBuilder = SchemaFactoryBuilder.getSecureSchemaBuilder();
		schemaFactoryBuilder.removeAttribute(XMLConstants.ACCESS_EXTERNAL_DTD);
		schemaFactoryBuilder.removeAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA);
		xmlDefinerUtils.setSchemaFactoryBuilder(schemaFactoryBuilder);
		ValidatorConfigurator validatorConfigurator = ValidatorConfigurator.getSecureValidatorConfigurator();
		validatorConfigurator.removeAttribute(XMLConstants.ACCESS_EXTERNAL_DTD);
		validatorConfigurator.removeAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA);
		xmlDefinerUtils.setValidatorConfigurator(validatorConfigurator);
	}


	@Bean
	public TSPSource tspSource() {
		OnlineTSPSource tspSource = new OnlineTSPSource(dssProperties.getTspServer());
		TimestampDataLoader timestampDataLoader = new TimestampDataLoader();
		timestampDataLoader.setTimeoutConnection(10000);
		timestampDataLoader.setTrustStrategy(TrustAllStrategy.INSTANCE);
		if(proxyConfig != null) {
			timestampDataLoader.setProxyConfig(proxyConfig);
		}
		tspSource.setDataLoader(timestampDataLoader);
		return tspSource;
	}

	@Bean
	public CommonsDataLoader dataLoader() {
		CommonsDataLoader dataLoader = configureCommonsDataLoader(new CommonsDataLoader());
		dataLoader.setProxyConfig(proxyConfig);
		dataLoader.setSslProtocol("TLSv1.3");
		return dataLoader;
	}

	@Bean
	public CommonsDataLoader trustAllDataLoader() {
		CommonsDataLoader dataLoader = configureCommonsDataLoader(new CommonsDataLoader());
		dataLoader.setProxyConfig(proxyConfig);
		dataLoader.setTrustStrategy(TrustAllStrategy.INSTANCE);
		dataLoader.setSslProtocol("TLSv1.3");
		return dataLoader;
	}

	@Bean
	public OnlineCRLSource onlineCRLSource(CommonsDataLoader dataLoader) {
		OnlineCRLSource onlineCRLSource = new OnlineCRLSource();
		onlineCRLSource.setDataLoader(dataLoader);
		return onlineCRLSource;
	}

	@Bean
	public JdbcCacheCRLSource cachedCRLSource(OnlineCRLSource onlineCRLSource, JdbcCacheConnector jdbcCacheConnector) {
		JdbcCacheCRLSource jdbcCacheCRLSource = new JdbcCacheCRLSource();
		jdbcCacheCRLSource.setJdbcCacheConnector(jdbcCacheConnector);
		jdbcCacheCRLSource.setProxySource(onlineCRLSource);
		jdbcCacheCRLSource.setDefaultNextUpdateDelay((long) (60 * 3)); // 3 minutes
		return jdbcCacheCRLSource;
	}

	@Bean
	public JdbcCacheConnector jdbcCacheConnector() {
		return new JdbcCacheConnector(cacheDataSource());
	}

	@Bean
	public AIASource onlineAIASource(CommonsDataLoader dataLoader) {
		return new DefaultAIASource(dataLoader);
	}

	@Bean
	public JdbcCacheAIASource cachedAIASource(JdbcCacheConnector jdbcCacheConnector, AIASource onlineAIASource) {
		JdbcCacheAIASource jdbcCacheAIASource = new JdbcCacheAIASource();
		jdbcCacheAIASource.setJdbcCacheConnector(jdbcCacheConnector);
		jdbcCacheAIASource.setProxySource(onlineAIASource);
		return jdbcCacheAIASource;
	}

	@Bean
	public OCSPDataLoader ocspDataLoader() {
		OCSPDataLoader ocspDataLoader = configureCommonsDataLoader(new OCSPDataLoader());
		ocspDataLoader.setSslProtocol("TLSv1.3");
		return ocspDataLoader;
	}

	@Bean
	public OnlineOCSPSource onlineOcspSource(OCSPDataLoader ocspDataLoader) {
		OnlineOCSPSource onlineOCSPSource = new OnlineOCSPSource();
		onlineOCSPSource.setDataLoader(ocspDataLoader);
		return onlineOCSPSource;
	}

	@Bean
	public KeyStoreCertificateSource ojContentKeyStore() {
		try {
			return new KeyStoreCertificateSource(new ClassPathResource("/keystore.p12").getInputStream(), "PKCS12", "dss-password".toCharArray());
		} catch (IOException e) {
			throw new DSSException("Unable to load the file " + "keystore.p12", e);
		}
	}

	@Bean
	public DSSFileLoader onlineLoader(File tlCacheDirectory, CommonsDataLoader trustAllDataLoader) {
		FileCacheDataLoader onlineFileLoader = new FileCacheDataLoader();
		onlineFileLoader.setCacheExpirationTime(0);
		onlineFileLoader.setDataLoader(trustAllDataLoader);
		onlineFileLoader.setFileCacheDirectory(tlCacheDirectory);
		return onlineFileLoader;
	}

	@Bean
	public DSSFileLoader offlineLoader(File tlCacheDirectory) {
		FileCacheDataLoader offlineFileLoader = new FileCacheDataLoader();
		offlineFileLoader.setCacheExpirationTime(Long.MAX_VALUE);
		offlineFileLoader.setDataLoader(new IgnoreDataLoader());
		offlineFileLoader.setFileCacheDirectory(tlCacheDirectory);
		return offlineFileLoader;
	}

	@Bean(name = "european-trusted-list-certificate-source")
	public TrustedListsCertificateSource trustedListSource() {
		return new TrustedListsCertificateSource();
	}

	@Bean
	public TLValidationJob job(TrustedListsCertificateSource trustedListSource, LOTLSource europeanLOTL, DSSFileLoader offlineLoader, DSSFileLoader onlineLoader) {
		TLValidationJob job = new TLValidationJob();
		job.setTrustedListCertificateSource(trustedListSource);
		job.setListOfTrustedListSources(europeanLOTL);
		job.setOfflineDataLoader(offlineLoader);
		job.setOnlineDataLoader(onlineLoader);
		job.setDebug(false);
		return job;
	}

	@Bean
	public File tlCacheDirectory() {
		String tmpDirectory = System.getProperty("java.io.tmpdir");
		File tslCache = new File(tmpDirectory, "dss-tsl-loader");
		logger.info("dssPath : " + tslCache.getAbsolutePath());
		if (tslCache.mkdirs()) {
			logger.info("TL Cache folder : {}", tslCache.getAbsolutePath());
		}
		return tslCache;
	}

	@Bean(name = "european-lotl-source")
	public LOTLSource europeanLOTL(KeyStoreCertificateSource ojContentKeyStore) {
		LOTLSource lotlSource = new LOTLSource();
		lotlSource.setUrl(dssProperties.getLotlUrl());
		lotlSource.setCertificateSource(ojContentKeyStore);
		lotlSource.setSigningCertificatesAnnouncementPredicate(new OfficialJournalSchemeInformationURI(dssProperties.getOjUrl()));
		lotlSource.setPivotSupport(true);
		return lotlSource;
	}

	@Bean
	public CertificateVerifier certificateVerifier(OnlineOCSPSource onlineOcspSource, TrustedListsCertificateSource trustedListSource, JdbcCacheCRLSource cachedCRLSource) {
		CommonCertificateVerifier certificateVerifier = new CommonCertificateVerifier();
		certificateVerifier.setCrlSource(cachedCRLSource);
		certificateVerifier.setOcspSource(onlineOcspSource);
		certificateVerifier.setTrustedCertSources(trustedListSource);
		certificateVerifier.setAlertOnMissingRevocationData(new ExceptionOnStatusAlert());
		certificateVerifier.setCheckRevocationForUntrustedChains(false);
		return certificateVerifier;
	}

	@Bean
	public RevocationDataVerifier revocationDataVerifier() {
        return RevocationDataVerifier.createDefaultRevocationDataVerifier();
	}

	@Bean
	public SignaturePolicyProvider signaturePolicyProvider(CommonsDataLoader trustAllDataLoader) {
		SignaturePolicyProvider signaturePolicyProvider = new SignaturePolicyProvider();
		signaturePolicyProvider.setDataLoader(trustAllDataLoader);
		return signaturePolicyProvider;
	}

	@Bean
	public ClassPathResource defaultPolicy() {
		return new ClassPathResource(dssProperties.getDefaultValidationPolicy());
	}

	@Bean
	public CAdESService cadesService(CertificateVerifier certificateVerifier, TSPSource tspSource) {
		CAdESService service = new CAdESService(certificateVerifier);
		service.setTspSource(tspSource);
		return service;
	}

	@Bean
	public XAdESService xadesService(CertificateVerifier certificateVerifier, TSPSource tspSource) {
		XAdESService service = new XAdESService(certificateVerifier);
		service.setTspSource(tspSource);
		return service;
	}

	@Bean
	public PAdESService padesService(CertificateVerifier certificateVerifier, TSPSource tspSource) {
		PAdESService service = new PAdESService(certificateVerifier);
		service.setTspSource(tspSource);
		return service;
	}

	@Bean
	public ASiCWithCAdESService asicWithCadesService(CertificateVerifier certificateVerifier, TSPSource tspSource) {
		ASiCWithCAdESService service = new ASiCWithCAdESService(certificateVerifier);
		service.setTspSource(tspSource);
		return service;
	}

	@Bean
	public ASiCWithXAdESService asicWithXadesService(CertificateVerifier certificateVerifier, TSPSource tspSource) {
		ASiCWithXAdESService service = new ASiCWithXAdESService(certificateVerifier);
		service.setTspSource(tspSource);
		return service;
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

	public TLAlert tlSigningAlert() {
		TLSignatureErrorDetection signingDetection = new TLSignatureErrorDetection();
		LogTLSignatureErrorAlertHandler handler = new LogTLSignatureErrorAlertHandler();
		return new TLAlert(signingDetection, handler);
	}

	public TLAlert tlExpirationDetection() {
		TLExpirationDetection expirationDetection = new TLExpirationDetection();
		LogTLExpirationAlertHandler handler = new LogTLExpirationAlertHandler();
		return new TLAlert(expirationDetection, handler);
	}

	public LOTLAlert ojUrlAlert(LOTLSource source) {
		OJUrlChangeDetection ojUrlDetection = new OJUrlChangeDetection(source);
		LogOJUrlChangeAlertHandler handler = new LogOJUrlChangeAlertHandler();
		return new LOTLAlert(ojUrlDetection, handler);
	}

	public LOTLAlert lotlLocationAlert(LOTLSource source) {
		LOTLLocationChangeDetection lotlLocationDetection = new LOTLLocationChangeDetection(source);
		LogLOTLLocationChangeAlertHandler handler = new LogLOTLLocationChangeAlertHandler();
		return new LOTLAlert(lotlLocationDetection, handler);
	}

	private <C extends CommonsDataLoader> C configureCommonsDataLoader(C dataLoader) {
		dataLoader.setTimeoutConnection(5000);
		dataLoader.setTimeoutConnectionRequest(5000);
		dataLoader.setRedirectsEnabled(true);
		dataLoader.setProxyConfig(proxyConfig);
		return dataLoader;
	}

}
