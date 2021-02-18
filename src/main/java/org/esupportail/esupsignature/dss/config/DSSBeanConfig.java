package org.esupportail.esupsignature.dss.config;

import com.zaxxer.hikari.HikariDataSource;
import eu.europa.esig.dss.DomUtils;
import eu.europa.esig.dss.asic.cades.signature.ASiCWithCAdESService;
import eu.europa.esig.dss.asic.xades.signature.ASiCWithXAdESService;
import eu.europa.esig.dss.cades.signature.CAdESService;
import eu.europa.esig.dss.jaxb.ValidatorConfigurator;
import eu.europa.esig.dss.jaxb.XmlDefinerUtils;
import eu.europa.esig.dss.model.x509.CertificateToken;
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
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.spi.client.http.DSSFileLoader;
import eu.europa.esig.dss.spi.client.http.IgnoreDataLoader;
import eu.europa.esig.dss.spi.tsl.TrustedListsCertificateSource;
import eu.europa.esig.dss.spi.x509.CertificateSource;
import eu.europa.esig.dss.spi.x509.CommonTrustedCertificateSource;
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
import eu.europa.esig.dss.tsl.function.GrantedTrustService;
import eu.europa.esig.dss.tsl.function.OfficialJournalSchemeInformationURI;
import eu.europa.esig.dss.tsl.job.TLValidationJob;
import eu.europa.esig.dss.tsl.source.LOTLSource;
import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.ws.signature.common.RemoteDocumentSignatureServiceImpl;
import eu.europa.esig.dss.ws.signature.common.RemoteMultipleDocumentsSignatureServiceImpl;
import eu.europa.esig.dss.ws.validation.common.RemoteDocumentValidationService;
import eu.europa.esig.dss.xades.signature.XAdESService;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableConfigurationProperties(DSSProperties.class)
@ConditionalOnProperty({"dss.tsp-server"})
public class DSSBeanConfig {

	private static final Logger logger = LoggerFactory.getLogger(DSSBeanConfig.class);

	private DSSProperties dssProperties;

	public DSSBeanConfig(DSSProperties dssProperties) {
		this.dssProperties = dssProperties;
	}

	public DSSProperties getDssProperties() {
		return dssProperties;
	}

	@Autowired(required = false)
	private ProxyConfig proxyConfig;

	@PostConstruct
	public void cachedCRLSourceInitialization() throws SQLException {
		JdbcCacheCRLSource jdbcCacheCRLSource = cachedCRLSource();
		jdbcCacheCRLSource.initTable();
	}

	@PostConstruct
	public void cachedOCSPSourceInitialization() throws SQLException {
		JdbcCacheOCSPSource jdbcCacheOCSPSource = cachedOCSPSource();
		jdbcCacheOCSPSource.initTable();
	}

	@PreDestroy
	public void cachedCRLSourceClean() throws SQLException {
		JdbcCacheCRLSource jdbcCacheCRLSource = cachedCRLSource();
		jdbcCacheCRLSource.destroyTable();
	}

	@PreDestroy
	public void cachedOCSPSourceClean() throws SQLException {
		JdbcCacheOCSPSource jdbcCacheOCSPSource = cachedOCSPSource();
		jdbcCacheOCSPSource.destroyTable();
	}

	@Bean
	public CommonsDataLoader dataLoader() {
		CommonsDataLoader dataLoader = new CommonsDataLoader();
		if(proxyConfig != null) {
			dataLoader.setProxyConfig(proxyConfig);
		}
		dataLoader.setTimeoutConnection(10000);
		dataLoader.setTrustStrategy(new TrustAllStrategy());
		return dataLoader;
	}

	@Bean
	public OCSPDataLoader ocspDataLoader() {
		OCSPDataLoader ocspDataLoader = new OCSPDataLoader();
		if(proxyConfig != null) {
			ocspDataLoader.setProxyConfig(proxyConfig);
		}
		ocspDataLoader.setTimeoutConnection(10000);
		ocspDataLoader.setTrustStrategy(new TrustAllStrategy());
		return ocspDataLoader;
	}

	@Bean
	public OnlineCRLSource onlineCRLSource() {
		OnlineCRLSource onlineCRLSource = new OnlineCRLSource();
		onlineCRLSource.setDataLoader(dataLoader());
		return onlineCRLSource;
	}

	@Bean
	public JdbcCacheCRLSource cachedCRLSource() {
		JdbcCacheCRLSource jdbcCacheCRLSource = new JdbcCacheCRLSource();
		jdbcCacheCRLSource.setDataSource(cacheDataSource());
		jdbcCacheCRLSource.setProxySource(onlineCRLSource());
		jdbcCacheCRLSource.setDefaultNextUpdateDelay((long) (60 * 3)); // 3 minutes
		return jdbcCacheCRLSource;
	}

	@Bean
	public JdbcCacheOCSPSource cachedOCSPSource() {
		JdbcCacheOCSPSource jdbcCacheOCSPSource = new JdbcCacheOCSPSource();
		jdbcCacheOCSPSource.setDataSource(cacheDataSource());
		jdbcCacheOCSPSource.setProxySource(onlineOcspSource());
		jdbcCacheOCSPSource.setDefaultNextUpdateDelay((long) (1000 * 60 * 3)); // 3 minutes
		return jdbcCacheOCSPSource;
	}

	@Bean
	public OnlineOCSPSource onlineOcspSource() {
		OnlineOCSPSource onlineOCSPSource = new OnlineOCSPSource();
		onlineOCSPSource.setDataLoader(ocspDataLoader());
		return onlineOCSPSource;
	}

	@Bean
	public KeyStoreCertificateSource ojContentKeyStore() throws IOException {
		File keystoreFile = new File(dssProperties.getKsFilename());
		KeyStoreCertificateSource keyStoreCertificateSource = null;
		if(keystoreFile.exists()) {
			logger.info("delete old oj file");
			keystoreFile.delete();
		}
		logger.info("creating oj file in " + keystoreFile.getAbsolutePath());
		if(keystoreFile.createNewFile()) {
			keyStoreCertificateSource = new KeyStoreCertificateSource((InputStream) null, dssProperties.getKsType(), dssProperties.getKsPassword());
		}
		return keyStoreCertificateSource;
	}

	@Bean
	public DSSFileLoader onlineLoader() {
		FileCacheDataLoader onlineFileLoader = new FileCacheDataLoader();
		onlineFileLoader.setCacheExpirationTime(0);
		onlineFileLoader.setDataLoader(dataLoader());
		onlineFileLoader.setFileCacheDirectory(tlCacheDirectory());
		return onlineFileLoader;
	}

	@Bean
	public DSSFileLoader offlineLoader() {
		FileCacheDataLoader offlineFileLoader = new FileCacheDataLoader();
		offlineFileLoader.setCacheExpirationTime(Long.MAX_VALUE);
		offlineFileLoader.setDataLoader(new IgnoreDataLoader());
		offlineFileLoader.setFileCacheDirectory(tlCacheDirectory());
		return offlineFileLoader;
	}

	@Bean
	public TLValidationJob job() throws IOException {
		TLValidationJob job = new TLValidationJob();
		LOTLSource lotlSource = europeanLOTL();
		job.setTrustedListCertificateSource(trustedListSource());
		job.setListOfTrustedListSources(lotlSource);
		job.setOfflineDataLoader(offlineLoader());
		job.setOnlineDataLoader(onlineLoader());
		job.setLOTLAlerts(Arrays.asList(ojUrlAlert(lotlSource), lotlLocationAlert(lotlSource)));
		job.setTLAlerts(Arrays.asList(tlSigningAlert(), tlExpirationDetection()));
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
	public LOTLSource europeanLOTL() throws IOException {
		LOTLSource lotlSource = new LOTLSource();
		lotlSource.setUrl(dssProperties.getLotlUrl());
		lotlSource.setCertificateSource(ojContentKeyStore());
		lotlSource.setSigningCertificatesAnnouncementPredicate(new OfficialJournalSchemeInformationURI(dssProperties.getOjUrl()));
		lotlSource.setTrustServicePredicate(new GrantedTrustService());
		lotlSource.setPivotSupport(true);
		return lotlSource;
	}

	@Bean(name = "european-trusted-list-certificate-source")
	public TrustedListsCertificateSource trustedListSource() {
		return new TrustedListsCertificateSource();
	}

	@Bean
	public CommonTrustedCertificateSource myTrustedCertificateSource() {
		CommonTrustedCertificateSource certSource = new CommonTrustedCertificateSource();
		for(String trustedCertificatUrl : dssProperties.getTrustedCertificatUrlList()) {
			logger.info("adding trusted certificat : " + trustedCertificatUrl);
			try {
				InputStream in = new URL(trustedCertificatUrl).openStream();
				CertificateToken certificateToken = DSSUtils.loadCertificate(in);
				certSource.addCertificate(certificateToken);
			} catch (IOException e) {
				logger.warn("unable to add trusted certificat : " + trustedCertificatUrl, e);
			}
		}
		return certSource;
	}

	@Bean
	public CertificateVerifier certificateVerifier() {
		List<CertificateSource> trustedCertSources = new ArrayList<>();
		trustedCertSources.add(trustedListSource());
		trustedCertSources.add(myTrustedCertificateSource());
		CommonCertificateVerifier commonCertificateVerifier = new CommonCertificateVerifier(trustedCertSources, cachedCRLSource(), cachedOCSPSource(), dataLoader());
		return commonCertificateVerifier;
	}

	@Bean
	public ClassPathResource defaultPolicy() throws IOException {
		ClassPathResource classPathResource = new ClassPathResource(dssProperties.getDefaultValidationPolicy());
		if(!classPathResource.exists()) {
			logger.error("Default Validation Policy doesn't exist");
		}
		return new ClassPathResource(dssProperties.getDefaultValidationPolicy());
	}

	@Bean
	public TSPSource tspSource() {
		OnlineTSPSource tspSource = new OnlineTSPSource(dssProperties.getTspServer());
		TimestampDataLoader timestampDataLoader = new TimestampDataLoader();
		if(proxyConfig != null) {
			timestampDataLoader.setProxyConfig(proxyConfig);
		}
		tspSource.setDataLoader(timestampDataLoader);
		return tspSource;
	}

	@Bean
	public CAdESService cadesService() {
		CAdESService service = new CAdESService(certificateVerifier());
		service.setTspSource(tspSource());
		return service;
	}

	@Bean
	public XAdESService xadesService() {
		XAdESService service = new XAdESService(certificateVerifier());
		service.setTspSource(tspSource());
		return service;
	}

	@Bean
	public PAdESService padesService() {
		PAdESService service = new PAdESService(certificateVerifier());
		service.setTspSource(tspSource());
		return service;
	}

	@Bean
	public ASiCWithCAdESService asicWithCadesService() {
		ASiCWithCAdESService service = new ASiCWithCAdESService(certificateVerifier());
		service.setTspSource(tspSource());
		return service;
	}

	@Bean
	public ASiCWithXAdESService asicWithXadesService() {
		ASiCWithXAdESService service = new ASiCWithXAdESService(certificateVerifier());
		service.setTspSource(tspSource());
		return service;
	}

	@Bean
	public RemoteDocumentSignatureServiceImpl remoteSignatureService() {
		RemoteDocumentSignatureServiceImpl service = new RemoteDocumentSignatureServiceImpl();
		service.setAsicWithCAdESService(asicWithCadesService());
		service.setAsicWithXAdESService(asicWithXadesService());
		service.setCadesService(cadesService());
		service.setXadesService(xadesService());
		service.setPadesService(padesService());
		return service;
	}

	@Bean
	public RemoteMultipleDocumentsSignatureServiceImpl remoteMultipleDocumentsSignatureService() {
		RemoteMultipleDocumentsSignatureServiceImpl service = new RemoteMultipleDocumentsSignatureServiceImpl();
		service.setAsicWithCAdESService(asicWithCadesService());
		service.setAsicWithXAdESService(asicWithXadesService());
		service.setXadesService(xadesService());
		return service;
	}

	@Bean
	public XmlDefinerUtils xmlDefinerUtils() throws ParserConfigurationException {
		XmlDefinerUtils xmlDefinerUtils = XmlDefinerUtils.getInstance();
		ValidatorConfigurator.getSecureValidatorConfigurator();
		DomUtils.enableFeature("http://xml.org/sax/features/external-general-entities");
		DomUtils.disableFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd");
		return xmlDefinerUtils;
	}

	@Bean
	public RemoteDocumentValidationService remoteValidationService() throws Exception {
		RemoteDocumentValidationService service = new RemoteDocumentValidationService();
		service.setVerifier(certificateVerifier());
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
		try {
			ds.getConnection();
		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}
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