package org.esupportail.esupsignature.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Scope(value="session")
public class UserKeystoreService {
	
	private String password;
	
	long startTime;

	public void setPassword(String password) {
		startTime = System.currentTimeMillis();
		this.password = password;
	}

	public File createKeystore(String keyStoreName, String alias, String pemCert, String password) throws Exception {
		
		KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
		char[] passwordChars = password.toCharArray();
	
		CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
		String certB64 = pemToBase64String(pemCert);
		byte encodedCert[] = Base64.getDecoder().decode(certB64);
		ByteArrayInputStream inputStream  =  new ByteArrayInputStream(encodedCert);

		X509Certificate cert = (X509Certificate) certFactory.generateCertificate(inputStream);
	            
		File keystoreFile = File.createTempFile(keyStoreName, ".keystore");
		FileOutputStream out = new FileOutputStream(keystoreFile);
		keystore.load(null, passwordChars);
		keystore.setCertificateEntry(alias, cert);
		keystore.store(out, passwordChars);
		out.close();
		return keystoreFile;
	}
	
	public String getPemCertificat(File keyStoreFile, String keyStoreName, String alias) throws Exception {
		
		KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
		char[] passwordChars = password.toCharArray();
            
		keystore.load(new FileInputStream(keyStoreFile), passwordChars);
		
		X509Certificate cert = (X509Certificate) keystore.getCertificate(alias);;
		byte[] prvkey = cert.getEncoded();
		String encoded = Base64.getEncoder().encodeToString(prvkey);
		return "-----BEGIN PRIVATE KEY-----" + encoded + "-----END PRIVATE KEY-----";
	}
	
	public String checkKeystore(File keyStoreFile, String keyStoreName, String alias) throws Exception {
		
		KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
		char[] passwordChars = password.toCharArray();
        keystore.load(new FileInputStream(keyStoreFile), passwordChars);
		X509Certificate cert = (X509Certificate) keystore.getCertificate(alias);
		String certInfo = cert.getType() + ""
				+ "\n" + cert.getSubjectDN()
				+ "\n" + cert.getSerialNumber();
		
		return certInfo;
	}
	
	public String pemToBase64String(String pemCert) {
		 return pemCert.replaceAll("-----(BEGIN|END) CERTIFICATE-----", "").replaceAll("-----(BEGIN|END) PRIVATE KEY-----", "").replaceAll("\r\n", "").replaceAll(" ", "").trim();
	}
	
	@Scheduled(fixedDelay = 5000)
	public void clearPassword () {
		if(startTime > 0) {
			if(System.currentTimeMillis() - startTime > 300000) {
				password = "";
				startTime = 0;
			}
		}
	}
	
}
