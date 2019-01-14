package org.esupportail.esupsignature.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

import org.springframework.stereotype.Service;

@Service
public class UserKeystoreService {

	public File createKeystore(String keyStoreName, String privateKey, String password) throws Exception {
		
		KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());

	
		char[] passwordChars = password.toCharArray();

		System.err.println("password : " + passwordChars[0]);
		
		//TODO : privkey to x.509
		
		CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
		String certB64 = privateKey.replaceAll("-----(BEGIN|END) CERTIFICATE-----", "").replaceAll("\n", "").replaceAll(" ", "").trim();
		System.err.println(certB64);
		byte encodedCert[] = Base64.getDecoder().decode(certB64);
		ByteArrayInputStream inputStream  =  new ByteArrayInputStream(encodedCert);

		X509Certificate cert = (X509Certificate) certFactory.generateCertificate(inputStream);
	            
		File keystoreFile = File.createTempFile(keyStoreName, ".keystore");
		FileOutputStream out = new FileOutputStream(keystoreFile);
		keystore.load(null, passwordChars);
		keystore.setCertificateEntry(keyStoreName, cert);
		keystore.store(out, passwordChars);
		out.close();
		return keystoreFile;
	}
	
}
