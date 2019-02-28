package org.esupportail.esupsignature.dss.web.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.esig.dss.DSSException;
import eu.europa.esig.dss.DSSUtils;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.x509.CertificateToken;
import eu.europa.esig.dss.x509.CommonCertificateSource;
import eu.europa.esig.dss.x509.KeyStoreCertificateSource;

public class BouncyKeyStoreCertificateSource extends CommonCertificateSource {

	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(KeyStoreCertificateSource.class);

	private KeyStore keyStore;
	private PasswordProtection passwordProtection;
	
	public BouncyKeyStoreCertificateSource(File ksFile, String ksType, String ksPassword) throws IOException {
		this(new FileInputStream(ksFile), ksType, ksPassword);
	}
	
	public BouncyKeyStoreCertificateSource(final InputStream ksStream, final String ksType, final String ksPassword) {
		super();
		initKeystore(ksStream, ksType, ksPassword);
	}
	
	private void initKeystore(final InputStream ksStream, final String ksType, final String ksPassword) {
		try {
			keyStore = KeyStore.getInstance(ksType, "BC");
			final char[] password = (ksPassword == null) ? null : ksPassword.toCharArray();
			keyStore.load(ksStream, password);
			passwordProtection = new PasswordProtection(password);
		} catch (GeneralSecurityException | IOException e) {
			throw new DSSException("Unable to initialize the keystore", e);
		} finally {
			Utils.closeQuietly(ksStream);
		}
	}
	
	/**
	 * This method allows to retrieve a certificate by its alias
	 * 
	 * @param alias
	 *            the certificate alias in the keystore
	 * @return the certificate
	 */
	public CertificateToken getCertificate(String alias) {
		try {
			String aliasToSearch = getKey(alias);
			if (keyStore.containsAlias(aliasToSearch)) {
				Certificate certificate = keyStore.getCertificate(aliasToSearch);
				return DSSUtils.loadCertificate(certificate.getEncoded());
			} else {
				LOG.warn("Certificate '{}' not found in the keystore", aliasToSearch);
				return null;
			}
		} catch (GeneralSecurityException e) {
			throw new DSSException("Unable to retrieve certificate from the keystore", e);
		}
	}

	/**
	 * This method returns all certificates from the keystore
	 */
	@Override
	public List<CertificateToken> getCertificates() {
		List<CertificateToken> list = new ArrayList<CertificateToken>();
		try {
			Enumeration<String> aliases = keyStore.aliases();
			while (aliases.hasMoreElements()) {
				Certificate certificate = keyStore.getCertificate(getKey(aliases.nextElement()));
				list.add(DSSUtils.loadCertificate(certificate.getEncoded()));
			}
		} catch (GeneralSecurityException e) {
			throw new DSSException("Unable to retrieve certificates from the keystore", e);
		}
		return Collections.unmodifiableList(list);
	}

	/**
	 * This method allows to add a list of certificates to the keystore
	 * 
	 * @param certificates
	 *            the list of certificates
	 */
	public void addAllCertificatesToKeyStore(List<CertificateToken> certificates) {
		for (CertificateToken certificateToken : certificates) {
			addCertificateToKeyStore(certificateToken);
		}
	}

	/**
	 * This method allows to add a certificate in the keystore. The generated alias will be the DSS ID.
	 * 
	 * @param certificateToken
	 *            the certificate to be added in the keystore
	 */
	public void addCertificateToKeyStore(CertificateToken certificateToken) {
		try {
			keyStore.setCertificateEntry(getKey(certificateToken.getDSSIdAsString()), certificateToken.getCertificate());
		} catch (GeneralSecurityException e) {
			throw new DSSException("Unable to add certificate to the keystore", e);
		}
	}

	/**
	 * This method allows to remove a certificate from the keystore
	 * 
	 * @param alias
	 *            the certificate alias
	 */
	public void deleteCertificateFromKeyStore(String alias) {
		try {
			if (keyStore.containsAlias(alias)) {
				keyStore.deleteEntry(alias);
				LOG.info("Certificate '{}' successfuly removed from the keystore", alias);
			} else {
				LOG.warn("Certificate '{}' not found in the keystore", alias);
			}
		} catch (GeneralSecurityException e) {
			throw new DSSException("Unable to delete certificate from the keystore", e);
		}
	}

	/**
	 * This method allows to remove all certificates from the keystore
	 */
	public void clearAllCertificates() {
		try {
			Enumeration<String> aliases = keyStore.aliases();
			while (aliases.hasMoreElements()) {
				String alias = aliases.nextElement();
				deleteCertificateFromKeyStore(alias);
			}
		} catch (GeneralSecurityException e) {
			throw new DSSException("Unable to clear certificates from the keystore", e);
		}
	}

	/**
	 * This method allows to store the keystore in the OutputStream
	 * 
	 * @param os
	 *            the OutpuStream where to store the keystore
	 */
	public void store(OutputStream os) {
		try {
			keyStore.store(os, passwordProtection.getPassword());
		} catch (GeneralSecurityException | IOException e) {
			throw new DSSException("Unable to store the keystore", e);
		}
	}

	private String getKey(String inputKey) {
		if ("PKCS12".equals(keyStore.getType())) {
			// workaround for https://bugs.openjdk.java.net/browse/JDK-8079616:
			return inputKey.toLowerCase(Locale.ROOT);
		}
		return inputKey;
	}
}
