package org.esupportail.esupsignature.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureKeystoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import eu.europa.esig.dss.token.KSPrivateKeyEntry;
import eu.europa.esig.dss.token.KeyStoreSignatureTokenConnection;
import eu.europa.esig.dss.token.Pkcs12SignatureToken;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import eu.europa.esig.dss.x509.CertificateToken;
import eu.europa.esig.dss.x509.KeyStoreCertificateSource;

@Service
public class UserKeystoreService {
	
	private static final Logger log = LoggerFactory.getLogger(UserKeystoreService.class);

	private static String keystoreType = "PKCS12";

	public File createKeystore(String keyStoreName, String alias, String pemCert, String password) throws Exception {
		
		KeyStore keystore = KeyStore.getInstance(keystoreType);
		char[] passwordChars = password.toCharArray();
	
		CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
		String certB64 = pemToBase64String(pemCert);
		byte encodedCert[] = Base64.getDecoder().decode(certB64);
		ByteArrayInputStream inputStream  =  new ByteArrayInputStream(encodedCert);

		X509Certificate cert = (X509Certificate) certFactory.generateCertificate(inputStream);
	            
		File keystoreFile = File.createTempFile(keyStoreName, ".p12");
		FileOutputStream out = new FileOutputStream(keystoreFile);
		keystore.load(null, passwordChars);
		keystore.setCertificateEntry(alias, cert);
		keystore.store(out, passwordChars);
		out.close();
		return keystoreFile;
	}
	
	public String getPemCertificat(File keyStoreFile, String password)  {
		try {
			CertificateToken certificateToken = getCertificateToken(keyStoreFile, password);
			X509Certificate cert = certificateToken.getCertificate();
			byte[] prvkey = cert.getEncoded();
			String encoded = Base64.getEncoder().encodeToString(prvkey);
			return "-----BEGIN PRIVATE KEY-----" + encoded + "-----END PRIVATE KEY-----";
		} catch (CertificateException | EsupSignatureException e) {
			log.error("error en get pem cert from keystore", e);
		}
		return null;
	}
	
	public SignatureTokenConnection getSignatureTokenConnection(File keyStoreFile, String password) throws EsupSignatureKeystoreException {
		try {
			Pkcs12SignatureToken token = new Pkcs12SignatureToken(keyStoreFile, new PasswordProtection(password.toCharArray()));
			return token;
		} catch (Exception e) {
			log.error("open keystore fail", e);
			throw new EsupSignatureKeystoreException("open keystore fail", e);
		}
	}
	
	public CertificateToken getCertificateToken(File keyStoreFile, String password) throws EsupSignatureKeystoreException {
		try {
			KeyStoreSignatureTokenConnection token = new KeyStoreSignatureTokenConnection(keyStoreFile, keystoreType, new PasswordProtection(password.toCharArray()));
			KSPrivateKeyEntry ksPrivateKeyEntry = (KSPrivateKeyEntry) token.getKeys().get(0);
			token.close();
			return ksPrivateKeyEntry.getCertificate();
		} catch (Exception e) {
			log.error("open keystore fail", e);
			throw new EsupSignatureKeystoreException("get certificat token fail", e);
		}
	}
	
	public CertificateToken[] getCertificateTokenChain(File keyStoreFile, String password)throws EsupSignatureKeystoreException   {
		try {
			KeyStoreSignatureTokenConnection token = new KeyStoreSignatureTokenConnection(keyStoreFile, keystoreType, new PasswordProtection(password.toCharArray()));
			KSPrivateKeyEntry ksPrivateKeyEntry = (KSPrivateKeyEntry) token.getKeys().get(0);
			CertificateToken[] certificateTokens = ksPrivateKeyEntry.getCertificateChain();
			token.close();
			return certificateTokens;
		} catch (IOException e) {
			log.error("open keystore fail", e);
			throw new EsupSignatureKeystoreException("get certificat chain fail", e);
		}
		
	}
	
	public String checkKeystore(File keyStoreFile, String password) throws Exception {
		String certInfo = "";		
		CertificateToken certificateToken = getCertificateToken(keyStoreFile, password);
		certInfo += "\n" + certificateToken.getCertificate().getType() + ""
				+ "\n" + certificateToken.getCertificate().getSubjectDN()
				+ "\n" + certificateToken.getCertificate().getSerialNumber();
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
	
	public String getBase64PemCertificat(File keyStoreFile, String password) {
		 return pemToBase64String(getPemCertificat(keyStoreFile, password));
	}
	

	
}
