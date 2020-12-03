package org.esupportail.esupsignature.service;

import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.spi.x509.KeyStoreCertificateSource;
import eu.europa.esig.dss.token.KSPrivateKeyEntry;
import eu.europa.esig.dss.token.Pkcs12SignatureToken;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import eu.europa.esig.dss.validation.CertificateValidator;
import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.executor.certificate.CertificateProcessExecutor;
import eu.europa.esig.dss.validation.reports.CertificateReports;
import org.esupportail.esupsignature.dss.DssUtils;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureKeystoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore.PasswordProtection;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

@Service
@ConditionalOnBean(CertificateVerifier.class)
public class UserKeystoreService {
	
	private static final Logger logger = LoggerFactory.getLogger(UserKeystoreService.class);

	private static String keystoreType = "PKCS12";

	@Resource
	private CertificateVerifier certificateVerifier;

	public String getPemCertificat(Pkcs12SignatureToken pkcs12SignatureToken)  {
		try {
			CertificateToken certificateToken = getCertificateToken(pkcs12SignatureToken);
			X509Certificate cert = certificateToken.getCertificate();
			byte[] prvkey = cert.getEncoded();
			String encoded = Base64.getEncoder().encodeToString(prvkey);
			return "-----BEGIN PRIVATE KEY-----" + encoded + "-----END PRIVATE KEY-----";
		} catch (CertificateException | EsupSignatureException e) {
			logger.error("error en get pem cert from keystore", e);
		}
		return null;
	}
	
	public Pkcs12SignatureToken getPkcs12Token(InputStream keyStoreFile, String password) throws EsupSignatureKeystoreException {
		try {
			Pkcs12SignatureToken token = new Pkcs12SignatureToken(keyStoreFile, new PasswordProtection(password.toCharArray()));
			return token;
		} catch (Exception e) {
			if(e.getCause().getMessage().equals("keystore password was incorrect")) {
				logger.warn("keystore password was incorrect");
			} else {
				logger.error("open keystore fail", e);
			}
			throw new EsupSignatureKeystoreException("open keystore fail", e);
		}
	}

	public CertificateToken getCertificateToken(Pkcs12SignatureToken token) throws EsupSignatureKeystoreException {
		try {
			KSPrivateKeyEntry ksPrivateKeyEntry = (KSPrivateKeyEntry) token.getKeys().get(0);
			return ksPrivateKeyEntry.getCertificate();
		} catch (Exception e) {
			logger.error("open keystore fail", e);
			throw new EsupSignatureKeystoreException("get certificat token fail", e);
		}
	}
	
	public CertificateToken[] getCertificateTokenChain(Pkcs12SignatureToken token) {
			KSPrivateKeyEntry ksPrivateKeyEntry = (KSPrivateKeyEntry) token.getKeys().get(0);
			CertificateToken[] certificateTokens = ksPrivateKeyEntry.getCertificateChain();
			return certificateTokens;
	}

	public String checkKeystore(InputStream keyStoreFile, String password) throws EsupSignatureKeystoreException {
		String certInfo = "";
		Pkcs12SignatureToken pkcs12SignatureToken = getPkcs12Token(keyStoreFile, password);
		CertificateToken certificateToken = getCertificateToken(pkcs12SignatureToken);
		SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
		certInfo += "\nNom : " + certificateToken.getSubject().getPrincipal().getName("CANONICAL");
		certInfo += "\nDate de création : " + format.format(certificateToken.getCreationDate());
		certInfo += "\nDate de fin : " + format.format(certificateToken.getNotAfter());
		certInfo += "\nValide : " + certificateToken.isValidOn(new Date());
		CertificateValidator certificateValidator = CertificateValidator.fromCertificate(certificateToken);
		certificateValidator.setCertificateVerifier(this.certificateVerifier);
		CertificateReports certificateReports = certificateValidator.validate();
		certInfo += "\nRevocation : " + certificateReports.getSimpleReport().getCertificateRevocationReason(certificateToken.getDSSId().asXmlId());
		certInfo += "\nDate de révocation : " + certificateReports.getSimpleReport().getCertificateRevocationDate(certificateToken.getDSSId().asXmlId());

		CertificateToken[] certificateTokens = getCertificateTokenChain(pkcs12SignatureToken);
		for(CertificateToken token : certificateTokens) {
			X509Certificate cert = token.getCertificate();
			certInfo += "\n\n" + cert.getType() + ""
				+ "\n" + cert.getSubjectDN()
				+ "\n" + cert.getSerialNumber();
		}
		pkcs12SignatureToken.close();
		return certInfo;
	}
	
	public KeyStoreCertificateSource getKeyStoreCertificateSource(File keyStoreFile, String password) throws IOException  {
		return new KeyStoreCertificateSource(keyStoreFile, keystoreType, password);

	}
	
	public String pemToBase64String(String pemCert) {
		 return pemCert.replaceAll("-----(BEGIN|END) CERTIFICATE-----", "").replaceAll("-----(BEGIN|END) PRIVATE KEY-----", "").replaceAll("\r\n", "").replaceAll(" ", "").trim();
	}
	
	public String getBase64PemCertificat(InputStream keyStoreFile, String password) throws EsupSignatureKeystoreException {
		 return pemToBase64String(getPemCertificat(getPkcs12Token(keyStoreFile, password)));
	}
	

	
}
