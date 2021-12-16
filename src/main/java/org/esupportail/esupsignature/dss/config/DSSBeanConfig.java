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
import eu.europa.esig.dss.service.ocsp.JdbcCacheOCSPSource;
import eu.europa.esig.dss.service.ocsp.OnlineOCSPSource;
import eu.europa.esig.dss.service.tsp.OnlineTSPSource;
import eu.europa.esig.dss.spi.client.http.DSSFileLoader;
import eu.europa.esig.dss.spi.client.http.IgnoreDataLoader;
import eu.europa.esig.dss.spi.client.jdbc.JdbcCacheConnector;
import eu.europa.esig.dss.spi.tsl.TrustedListsCertificateSource;
import eu.europa.esig.dss.spi.x509.KeyStoreCertificateSource;
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
import eu.europa.esig.dss.xades.signature.XAdESService;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

@Component
@EnableConfigurationProperties(DSSProperties.class)
public class DSSBeanConfig {

	private static final Logger logger = LoggerFactory.getLogger(DSSBeanConfig.class);

	private final DSSProperties dssProperties;

	public DSSBeanConfig(DSSProperties dssProperties) {
		this.dssProperties = dssProperties;
	}

	@Autowired(required = false)
	private ProxyConfig proxyConfig;

	@PostConstruct
	public void cachedCRLSourceInitialization() throws SQLException {
		cachedCRLSource().initTable();
	}

	@PreDestroy
	public void cachedCRLSourceClean() throws SQLException {
		cachedCRLSource().destroyTable();
	}

	@Bean
	public TSPSource tspSource() {
		OnlineTSPSource tspSource = new OnlineTSPSource(dssProperties.getTspServer());
		TimestampDataLoader timestampDataLoader = new TimestampDataLoader();
		timestampDataLoader.setTimeoutConnection(10000);
		timestampDataLoader.setTrustStrategy(new TrustAllStrategy());
		if(proxyConfig != null) {
			timestampDataLoader.setProxyConfig(proxyConfig);
		}
		tspSource.setDataLoader(timestampDataLoader);
		return tspSource;
	}

	@Bean
	public CommonsDataLoader dataLoader() {
		CommonsDataLoader dataLoader = new CommonsDataLoader();
		dataLoader.setProxyConfig(proxyConfig);
		return dataLoader;
	}

	public CommonsDataLoader trustAllDataLoader() {
		CommonsDataLoader dataLoader = new CommonsDataLoader();
		dataLoader.setProxyConfig(proxyConfig);
		dataLoader.setTrustStrategy(TrustAllStrategy.INSTANCE);
		return dataLoader;
	}

	@Bean
	public OCSPDataLoader ocspDataLoader() {
		OCSPDataLoader ocspDataLoader = new OCSPDataLoader();
		ocspDataLoader.setProxyConfig(proxyConfig);
		return ocspDataLoader;
	}

	public OnlineCRLSource onlineCRLSource() {
		OnlineCRLSource onlineCRLSource = new OnlineCRLSource();
		onlineCRLSource.setDataLoader(trustAllDataLoader());
		return onlineCRLSource;
	}

	public JdbcCacheCRLSource cachedCRLSource() {
		JdbcCacheCRLSource jdbcCacheCRLSource = new JdbcCacheCRLSource();
		jdbcCacheCRLSource.setJdbcCacheConnector(new JdbcCacheConnector(cacheDataSource()));
		jdbcCacheCRLSource.setProxySource(onlineCRLSource());
		jdbcCacheCRLSource.setDefaultNextUpdateDelay((long) (60 * 3)); // 3 minutes
		return jdbcCacheCRLSource;
	}

	public JdbcCacheOCSPSource cachedOCSPSource(OnlineOCSPSource onlineOcspSource) {
		JdbcCacheOCSPSource jdbcCacheOCSPSource = new JdbcCacheOCSPSource();
		jdbcCacheOCSPSource.setJdbcCacheConnector(new JdbcCacheConnector(cacheDataSource()));
		jdbcCacheOCSPSource.setProxySource(onlineOcspSource);
		jdbcCacheOCSPSource.setDefaultNextUpdateDelay((long) (1000 * 60 * 3)); // 3 minutes
		return jdbcCacheOCSPSource;
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
			return new KeyStoreCertificateSource(new ClassPathResource("/keystore.p12").getInputStream(), "PKCS12", "dss-password");
		} catch (IOException e) {
			throw new DSSException("Unable to load the file " + "keystore.p12", e);
		}
	}

	@Bean
	public DSSFileLoader onlineLoader(File tlCacheDirectory) {
		FileCacheDataLoader onlineFileLoader = new FileCacheDataLoader();
		onlineFileLoader.setCacheExpirationTime(0);
		onlineFileLoader.setDataLoader(trustAllDataLoader());
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
		File rootFolder = new File(System.getProperty("java.io.tmpdir"));
		File tslCache = new File(rootFolder, "dss-tsl-loader");
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
	public CertificateVerifier certificateVerifier(OnlineOCSPSource onlineOcspSource, CommonsDataLoader dataLoader, TrustedListsCertificateSource trustedListSource) {
		CommonCertificateVerifier certificateVerifier = new CommonCertificateVerifier();
		certificateVerifier.setCrlSource(cachedCRLSource());
		certificateVerifier.setOcspSource(onlineOcspSource);
		certificateVerifier.setDataLoader(dataLoader);
		certificateVerifier.setTrustedCertSources(trustedListSource);
		certificateVerifier.setAlertOnMissingRevocationData(new ExceptionOnStatusAlert());
		certificateVerifier.setCheckRevocationForUntrustedChains(false);
		return certificateVerifier;
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

}
