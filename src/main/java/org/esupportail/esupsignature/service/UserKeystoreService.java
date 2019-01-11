package org.esupportail.esupsignature.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import org.springframework.stereotype.Service;

@Service
public class UserKeystoreService {

	public File createKeystore(String keyStoreName, String privateKey, String password) throws Exception {
		
		System.err.println(privateKey);
		KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());

		char[] passwordChars = password.toCharArray();

		//TODO : privkey to x.509
		
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		InputStream certstream = new ByteArrayInputStream(privateKey.getBytes());
		Certificate certs =  cf.generateCertificate(certstream);

		File keystoreFile = File.createTempFile(keyStoreName, ".keystore");
		FileInputStream in = new FileInputStream(keystoreFile);
		keystore.load(in, passwordChars);
		in.close();

		keystore.setCertificateEntry(keyStoreName, certs);

		FileOutputStream out = new FileOutputStream(keystoreFile);
		keystore.store(out, passwordChars);
		out.close();
		return keystoreFile;
	}
	
}
