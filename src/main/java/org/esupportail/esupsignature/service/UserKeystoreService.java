package org.esupportail.esupsignature.service;

import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.spi.x509.KeyStoreCertificateSource;
import eu.europa.esig.dss.token.KSPrivateKeyEntry;
import eu.europa.esig.dss.token.KeyStoreSignatureTokenConnection;
import eu.europa.esig.dss.token.Pkcs12SignatureToken;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureKeystoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore.PasswordProtection;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;

@Service
public class UserKeystoreService {
	
	private static final Logger logger = LoggerFactory.getLogger(UserKeystoreService.class);

	private static String keystoreType = "PKCS12";

	
	public String getPemCertificat(InputStream keyStoreFile, String password)  {
		try {
			CertificateToken certificateToken = getCertificateToken(keyStoreFile, password);
			X509Certificate cert = certificateToken.getCertificate();
			byte[] prvkey = cert.getEncoded();
			String encoded = Base64.getEncoder().encodeToString(prvkey);
			return "-----BEGIN PRIVATE KEY-----" + encoded + "-----END PRIVATE KEY-----";
		} catch (CertificateException | EsupSignatureException e) {
			logger.error("error en get pem cert from keystore", e);
		}
		return null;
	}
	
	public SignatureTokenConnection getSignatureTokenConnection(InputStream keyStoreFile, String password) throws EsupSignatureKeystoreException {
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
	
	public CertificateToken getCertificateToken(InputStream keyStoreFile, String password) throws EsupSignatureKeystoreException {
		try {
			Pkcs12SignatureToken token = new Pkcs12SignatureToken(keyStoreFile, new PasswordProtection(password.toCharArray()));
			KSPrivateKeyEntry ksPrivateKeyEntry = (KSPrivateKeyEntry) token.getKeys().get(0);
			token.close();
			return ksPrivateKeyEntry.getCertificate();
		} catch (Exception e) {
			logger.error("open keystore fail", e);
			throw new EsupSignatureKeystoreException("get certificat token fail", e);
		}
	}
	
	public CertificateToken[] getCertificateTokenChain(InputStream keyStoreFile, String password) {
			KeyStoreSignatureTokenConnection token = new KeyStoreSignatureTokenConnection(keyStoreFile, keystoreType, new PasswordProtection(password.toCharArray()));
			KSPrivateKeyEntry ksPrivateKeyEntry = (KSPrivateKeyEntry) token.getKeys().get(0);
			CertificateToken[] certificateTokens = ksPrivateKeyEntry.getCertificateChain();
			token.close();
			return certificateTokens;
	}
	
	public String checkKeystore(InputStream keyStoreFile, String password) {
		String certInfo = "";		
		CertificateToken[] certificateTokens = getCertificateTokenChain(keyStoreFile, password);
		for(CertificateToken token : certificateTokens) {
			X509Certificate cert = token.getCertificate();
			certInfo += "\n" + cert.getType() + ""
				+ "\n" + cert.getSubjectDN()
				+ "\n" + cert.getSerialNumber();
		}
		
		return certInfo;
	}
	
	public KeyStoreCertificateSource getKeyStoreCertificateSource(File keyStoreFile, String password) throws IOException  {
		return new KeyStoreCertificateSource(keyStoreFile, keystoreType, password);

	}
	
	public String pemToBase64String(String pemCert) {
		 return pemCert.replaceAll("-----(BEGIN|END) CERTIFICATE-----", "").replaceAll("-----(BEGIN|END) PRIVATE KEY-----", "").replaceAll("\r\n", "").replaceAll(" ", "").trim();
	}
	
	public String getBase64PemCertificat(InputStream keyStoreFile, String password) {
		 return pemToBase64String(getPemCertificat(keyStoreFile, password));
	}
	

	
}
