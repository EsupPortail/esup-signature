package org.esupportail.esupsignature.dss.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import eu.europa.esig.dss.alert.ExceptionOnStatusAlert;
import eu.europa.esig.dss.alert.SilentOnStatusAlert;
import eu.europa.esig.dss.asic.cades.signature.ASiCWithCAdESService;
import eu.europa.esig.dss.asic.xades.signature.ASiCWithXAdESService;
import eu.europa.esig.dss.cades.signature.CAdESService;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.pdf.IPdfObjFactory;
import eu.europa.esig.dss.pdf.PdfSignatureFieldPositionChecker;
import eu.europa.esig.dss.pdf.pdfbox.PdfBoxNativeObjectFactory;
import eu.europa.esig.dss.service.crl.JdbcCacheCRLSource;
import eu.europa.esig.dss.service.crl.OnlineCRLSource;
import eu.europa.esig.dss.service.http.commons.CommonsDataLoader;
import eu.europa.esig.dss.service.http.commons.FileCacheDataLoader;
import eu.europa.esig.dss.service.http.commons.OCSPDataLoader;
import eu.europa.esig.dss.service.http.commons.TimestampDataLoader;
import eu.europa.esig.dss.service.http.proxy.ProxyConfig;
import eu.europa.esig.dss.service.ocsp.JdbcCacheOCSPSource;
import eu.europa.esig.dss.service.ocsp.OnlineOCSPSource;
import eu.europa.esig.dss.service.tsp.OnlineTSPSource;
import eu.europa.esig.dss.service.x509.aia.JdbcCacheAIASource;
import eu.europa.esig.dss.spi.client.http.DSSCacheFileLoader;
import eu.europa.esig.dss.spi.client.http.DSSFileLoader;
import eu.europa.esig.dss.spi.client.http.IgnoreDataLoader;
import eu.europa.esig.dss.spi.client.jdbc.JdbcCacheConnector;
import eu.europa.esig.dss.spi.policy.SignaturePolicyProvider;
import eu.europa.esig.dss.spi.tsl.TrustedListsCertificateSource;
import eu.europa.esig.dss.spi.validation.CertificateVerifier;
import eu.europa.esig.dss.spi.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.spi.validation.OCSPFirstRevocationDataLoadingStrategyFactory;
import eu.europa.esig.dss.spi.validation.RevocationDataVerifier;
import eu.europa.esig.dss.spi.x509.KeyStoreCertificateSource;
import eu.europa.esig.dss.spi.x509.aia.AIASource;
import eu.europa.esig.dss.spi.x509.aia.DefaultAIASource;
import eu.europa.esig.dss.spi.x509.tsp.CompositeTSPSource;
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
import eu.europa.esig.dss.tsl.cache.CacheCleaner;
import eu.europa.esig.dss.tsl.function.OfficialJournalSchemeInformationURI;
import eu.europa.esig.dss.tsl.job.TLValidationJob;
import eu.europa.esig.dss.tsl.source.LOTLSource;
import eu.europa.esig.dss.xades.signature.XAdESService;
import eu.europa.esig.dss.xml.common.DocumentBuilderFactoryBuilder;
import eu.europa.esig.dss.xml.common.SchemaFactoryBuilder;
import eu.europa.esig.dss.xml.common.ValidatorConfigurator;
import eu.europa.esig.dss.xml.common.XmlDefinerUtils;
import jakarta.annotation.PostConstruct;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.esupportail.esupsignature.config.sign.SignProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.xml.XMLConstants;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

@Configuration
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
	public JdbcCacheConnector jdbcCacheConnector() {
		HikariConfig hikariConfig = new HikariConfig();
		hikariConfig.setPoolName("DSS-Hikari-Pool");
		hikariConfig.setDriverClassName(dssProperties.getCacheDataSourceDriverClassName());
		hikariConfig.setJdbcUrl(dssProperties.getCacheDataSourceUrl());
		hikariConfig.setUsername(dssProperties.getCacheUsername());
		hikariConfig.setPassword(dssProperties.getCachePassword());
		hikariConfig.setAutoCommit(false);
		return new JdbcCacheConnector(new HikariDataSource(hikariConfig));
	}

	@Bean
	public TimestampDataLoader timestampDataLoader() {
		TimestampDataLoader timestampDataLoader = new TimestampDataLoader();
		timestampDataLoader.setTimeoutConnection(10000);
		timestampDataLoader.setTrustStrategy(TrustAllStrategy.INSTANCE);
		if(proxyConfig != null) {
			timestampDataLoader.setProxyConfig(proxyConfig);
		}
		return timestampDataLoader;
	}

	@Bean
	public TSPSource tspSource(TimestampDataLoader timestampDataLoader) {
		CompositeTSPSource compositeTSPSource = new CompositeTSPSource();
		Map<String, TSPSource> tspSourceMap = new HashMap<>();
		for(String tspServer : dssProperties.getTspServers()) {
			OnlineTSPSource tspSource = new OnlineTSPSource(tspServer);
			tspSource.setDataLoader(timestampDataLoader);
			tspSourceMap.put(tspServer, tspSource);
		}
		compositeTSPSource.setTspSources(tspSourceMap);
		return compositeTSPSource;
	}

	@Bean
	public CommonsDataLoader dataLoader() {
		CommonsDataLoader dataLoader = configureCommonsDataLoader(new CommonsDataLoader());
		dataLoader.setSslProtocol("TLSv1.3");
		return dataLoader;
	}

	@Bean
	public CommonsDataLoader trustAllDataLoader() {
		CommonsDataLoader dataLoader = configureCommonsDataLoader(new CommonsDataLoader());
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
	public JdbcCacheCRLSource jdbcCacheCRLSource(OnlineCRLSource onlineCRLSource, JdbcCacheConnector jdbcCacheConnector) throws SQLException {
		JdbcCacheCRLSource jdbcCacheCRLSource = new JdbcCacheCRLSource();
		jdbcCacheCRLSource.setJdbcCacheConnector(jdbcCacheConnector);
		jdbcCacheCRLSource.setProxySource(onlineCRLSource);
		Long oneWeek = (long) (60 * 60 * 24 * 7);
		jdbcCacheCRLSource.setDefaultNextUpdateDelay(oneWeek);
		jdbcCacheCRLSource.initTable();
		return jdbcCacheCRLSource;
	}

	@Bean
	public AIASource onlineAIASource(CommonsDataLoader dataLoader) {
		return new DefaultAIASource(dataLoader);
	}

	@Bean
	public JdbcCacheAIASource jdbcCacheAIASource(AIASource onlineAIASource, JdbcCacheConnector jdbcCacheConnector) throws SQLException {
		JdbcCacheAIASource jdbcCacheAIASource = new JdbcCacheAIASource();
		jdbcCacheAIASource.setJdbcCacheConnector(jdbcCacheConnector);
		jdbcCacheAIASource.setProxySource(onlineAIASource);
		jdbcCacheAIASource.initTable();
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
	public JdbcCacheOCSPSource jdbcCacheOCSPSource(OnlineOCSPSource onlineOcspSource, JdbcCacheConnector jdbcCacheConnector) throws SQLException {
		JdbcCacheOCSPSource jdbcCacheOCSPSource = new JdbcCacheOCSPSource();
		jdbcCacheOCSPSource.setJdbcCacheConnector(jdbcCacheConnector);
		jdbcCacheOCSPSource.setProxySource(onlineOcspSource);
		Long threeMinutes = (long) (60 * 3);
		jdbcCacheOCSPSource.setDefaultNextUpdateDelay(threeMinutes);
		jdbcCacheOCSPSource.initTable();
		return jdbcCacheOCSPSource;
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
		offlineFileLoader.setCacheExpirationTime(-1);
		offlineFileLoader.setDataLoader(new IgnoreDataLoader());
		offlineFileLoader.setFileCacheDirectory(tlCacheDirectory);
		return offlineFileLoader;
	}

	@Bean(name = "european-trusted-list-certificate-source")
	public TrustedListsCertificateSource trustedListSource() {
		return new TrustedListsCertificateSource();
	}

	@Bean
	public TLValidationJob tlValidationJob(TrustedListsCertificateSource trustedListSource, LOTLSource europeanLOTL, DSSFileLoader offlineLoader, DSSFileLoader onlineLoader, CacheCleaner cacheCleaner) {
		TLValidationJob tlValidationJob = new TLValidationJob();
		if(!dssProperties.getMultiThreadTlValidation()) {
			tlValidationJob.setExecutorService(Executors.newSingleThreadExecutor());
		}
		tlValidationJob.setTrustedListCertificateSource(trustedListSource);
		tlValidationJob.setListOfTrustedListSources(europeanLOTL);
		tlValidationJob.setOfflineDataLoader(offlineLoader);
		tlValidationJob.setOnlineDataLoader(onlineLoader);
		tlValidationJob.setLOTLAlerts(Arrays.asList(ojUrlAlert(europeanLOTL), lotlLocationAlert(europeanLOTL)));
		tlValidationJob.setTLAlerts(Arrays.asList(tlSigningAlert(), tlExpirationDetection()));
		tlValidationJob.setDebug(false);
		tlValidationJob.setCacheCleaner(cacheCleaner);
		return tlValidationJob;
	}

	@Bean
	public CacheCleaner cacheCleaner(DSSFileLoader offlineLoader) {
		CacheCleaner cacheCleaner = new CacheCleaner();
		cacheCleaner.setCleanMemory(true);
		cacheCleaner.setCleanFileSystem(true);
		cacheCleaner.setDSSFileLoader((DSSCacheFileLoader) offlineLoader);

		return cacheCleaner;
	}

	@Bean
	public File tlCacheDirectory() {
		String tmpDirectory = System.getProperty("java.io.tmpdir");
		File tslCache = new File(tmpDirectory, "dss-tsl-loader");
        logger.info("dss cache path : {}", tslCache.getAbsolutePath());
		if (tslCache.mkdirs()) {
            logger.info("creating non existing cache folder : {}", tslCache.getAbsolutePath());
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
	public RevocationDataVerifier revocationDataVerifier() {
		return RevocationDataVerifier.createDefaultRevocationDataVerifier();
	}

	@Bean
	public CertificateVerifier certificateVerifier(JdbcCacheOCSPSource jdbcCacheOCSPSource, JdbcCacheCRLSource jdbcCacheCRLSource, JdbcCacheAIASource jdbcCacheAIASource, TrustedListsCertificateSource trustedListSource, RevocationDataVerifier revocationDataVerifier, SignProperties signProperties) {
		CommonCertificateVerifier certificateVerifier = new CommonCertificateVerifier();
		certificateVerifier.setCrlSource(jdbcCacheCRLSource);
		certificateVerifier.setOcspSource(jdbcCacheOCSPSource);
		certificateVerifier.setAIASource(jdbcCacheAIASource);
		certificateVerifier.setTrustedCertSources(trustedListSource);
		certificateVerifier.setRevocationDataVerifier(revocationDataVerifier);
		certificateVerifier.setAlertOnMissingRevocationData(new ExceptionOnStatusAlert());
		certificateVerifier.setRevocationDataLoadingStrategyFactory(new OCSPFirstRevocationDataLoadingStrategyFactory());
		certificateVerifier.setCheckRevocationForUntrustedChains(dssProperties.getCheckRevocationForUntrustedChains());
		if(signProperties.getSignWithExpiredCertificate()) {
			certificateVerifier.setAlertOnExpiredCertificate(new SilentOnStatusAlert());
		} else {
			certificateVerifier.setAlertOnExpiredCertificate(new ExceptionOnStatusAlert());
		}
		return certificateVerifier;
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
		CAdESService cAdESService = new CAdESService(certificateVerifier);
		cAdESService.setTspSource(tspSource);
		return cAdESService;
	}

	@Bean
	public XAdESService xadesService(CertificateVerifier certificateVerifier, TSPSource tspSource) {
		XAdESService xAdESService = new XAdESService(certificateVerifier);
		xAdESService.setTspSource(tspSource);
		return xAdESService;
	}

	@Bean
	public PAdESService padesService(CertificateVerifier certificateVerifier, TSPSource tspSource) {
		PAdESService pAdESService = new PAdESService(certificateVerifier);
		pAdESService.setTspSource(tspSource);
		if(dssProperties.getAcceptSignatureFieldOverlap()) {
			IPdfObjFactory iPdfObjFactory = new PdfBoxNativeObjectFactory();
			PdfSignatureFieldPositionChecker pdfSignatureFieldPositionChecker = new PdfSignatureFieldPositionChecker();
			pdfSignatureFieldPositionChecker.setAlertOnSignatureFieldOverlap(new SilentOnStatusAlert());
			iPdfObjFactory.setPdfSignatureFieldPositionChecker(pdfSignatureFieldPositionChecker);
			pAdESService.setPdfObjFactory(iPdfObjFactory);
		}
		return pAdESService;
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
