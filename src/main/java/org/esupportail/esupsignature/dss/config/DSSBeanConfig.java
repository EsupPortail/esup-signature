package org.esupportail.esupsignature.dss.config;

import com.zaxxer.hikari.HikariDataSource;
import eu.europa.esig.dss.asic.cades.signature.ASiCWithCAdESService;
import eu.europa.esig.dss.asic.xades.signature.ASiCWithXAdESService;
import eu.europa.esig.dss.cades.signature.CAdESService;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.service.crl.JdbcCacheCRLSource;
import eu.europa.esig.dss.service.crl.OnlineCRLSource;
import eu.europa.esig.dss.service.http.commons.CommonsDataLoader;
import eu.europa.esig.dss.service.http.commons.FileCacheDataLoader;
import eu.europa.esig.dss.service.http.commons.OCSPDataLoader;
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
import eu.europa.esig.dss.tsl.alerts.detections.LOTLLocationChangeDetection;
import eu.europa.esig.dss.tsl.alerts.detections.OJUrlChangeDetection;
import eu.europa.esig.dss.tsl.alerts.handlers.log.LogLOTLLocationChangeAlertHandler;
import eu.europa.esig.dss.tsl.alerts.handlers.log.LogOJUrlChangeAlertHandler;
import eu.europa.esig.dss.tsl.function.OfficialJournalSchemeInformationURI;
import eu.europa.esig.dss.tsl.job.TLValidationJob;
import eu.europa.esig.dss.tsl.source.LOTLSource;
import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.ws.signature.common.RemoteDocumentSignatureServiceImpl;
import eu.europa.esig.dss.ws.signature.common.RemoteMultipleDocumentsSignatureServiceImpl;
import eu.europa.esig.dss.ws.validation.common.RemoteDocumentValidationService;
import eu.europa.esig.dss.xades.signature.XAdESService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableConfigurationProperties(DSSProperties.class)
public class DSSBeanConfig {

	private static final Logger log = LoggerFactory.getLogger(DSSBeanConfig.class);

	private DSSProperties dssProperties;

	public DSSBeanConfig(DSSProperties dssProperties) {
		this.dssProperties = dssProperties;
	}

	public DSSProperties getDssProperties() {
		return dssProperties;
	}

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
		if(!keystoreFile.exists()) {
			if(keystoreFile.createNewFile()) {
				keyStoreCertificateSource = new KeyStoreCertificateSource((InputStream) null, dssProperties.getKsType(), dssProperties.getKsPassword());
			}
		} else {
			keyStoreCertificateSource = new KeyStoreCertificateSource(keystoreFile, dssProperties.getKsType(), dssProperties.getKsPassword());
		}
		return keyStoreCertificateSource;
	}

	@Bean
	public DSSFileLoader onlineLoader() {
		FileCacheDataLoader offlineFileLoader = new FileCacheDataLoader();
		offlineFileLoader.setCacheExpirationTime(0);
		offlineFileLoader.setDataLoader(dataLoader());
		offlineFileLoader.setFileCacheDirectory(tlCacheDirectory());
		return offlineFileLoader;
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
		job.setTrustedListCertificateSource(trustedListSource());
		job.setListOfTrustedListSources(europeanLOTL());
		job.setOfflineDataLoader(offlineLoader());
		job.setOnlineDataLoader(onlineLoader());
		return job;
	}

	@Bean
	public File tlCacheDirectory() {
		File rootFolder = new File(System.getProperty("java.io.tmpdir"));
		File tslCache = new File(rootFolder, "dss-tsl-loader");
		if (tslCache.mkdirs()) {
			log.info("TL Cache folder : {}", tslCache.getAbsolutePath());
		}
		return tslCache;
	}

	@Bean(name = "european-lotl-source")
	public LOTLSource europeanLOTL() throws IOException {
		LOTLSource lotlSource = new LOTLSource();
		lotlSource.setUrl(dssProperties.getLotlUrl());
		lotlSource.setCertificateSource(ojContentKeyStore());
		lotlSource.setSigningCertificatesAnnouncementPredicate(new OfficialJournalSchemeInformationURI(dssProperties.getOjUrl()));
		lotlSource.setPivotSupport(true);
//		lotlSource.setLotlPredicate(new EULOTLOtherTSLPointer().and(new XMLOtherTSLPointer()));
//		lotlSource.setTlPredicate(new EUTLOtherTSLPointer().and(new XMLOtherTSLPointer()));
//		lotlSource.setTrustServicePredicate(new GrantedTrustService());
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
			log.info("adding trusted certificat : " + trustedCertificatUrl);
			InputStream in = null;
			try {
				in = new URL(trustedCertificatUrl).openStream();
			} catch (IOException e) {
				e.printStackTrace();
			}
			CertificateToken certificateToken = DSSUtils.loadCertificate(in);
			certSource.addCertificate(certificateToken);
		}
		return certSource;
	}

	@Bean
	public CertificateVerifier certificateVerifier() {
		List<CertificateSource> trustedCertSources = new ArrayList<>();
		trustedCertSources.add(trustedListSource());
		trustedCertSources.add(myTrustedCertificateSource());
		CommonCertificateVerifier certificateVerifier = new CommonCertificateVerifier(trustedCertSources, cachedCRLSource(), cachedOCSPSource(), dataLoader());
//		certificateVerifier.setTrustedCertSources(trustedListSource(), myTrustedCertificateSource());
//		certificateVerifier.setCrlSource(cachedCRLSource());
//		certificateVerifier.setOcspSource(cachedOCSPSource());
//		certificateVerifier.setDataLoader(dataLoader());
////		certificateVerifier.setExceptionOnMissingRevocationData(false);
////		certificateVerifier.setExceptionOnInvalidTimestamp(false);
//		certificateVerifier.setCheckRevocationForUntrustedChains(false);
		return certificateVerifier;
	}

	@Bean
	public ClassPathResource defaultPolicy() {
		return new ClassPathResource(dssProperties.getDefaultValidationPolicy());
	}

	@Bean
	public TSPSource tspSource() {
		OnlineTSPSource tspSource = new OnlineTSPSource(dssProperties.getTspServer());
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

	@Bean
	public LOTLAlert ojUrlAlert(LOTLSource source) {
		OJUrlChangeDetection ojUrlDetection = new OJUrlChangeDetection(source);
		LogOJUrlChangeAlertHandler handler = new LogOJUrlChangeAlertHandler();
		return new LOTLAlert(ojUrlDetection, handler);
	}

	@Bean
	public LOTLAlert lotlLocationAlert(LOTLSource source) {
		LOTLLocationChangeDetection lotlLocationDetection = new LOTLLocationChangeDetection(source);
		LogLOTLLocationChangeAlertHandler handler = new LogLOTLLocationChangeAlertHandler();
		return new LOTLAlert(lotlLocationDetection, handler);
	}

}