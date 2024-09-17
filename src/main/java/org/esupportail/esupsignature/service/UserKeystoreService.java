package org.esupportail.esupsignature.service;

import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.spi.validation.CertificateVerifier;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.Pkcs12SignatureToken;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import eu.europa.esig.dss.validation.CertificateValidator;
import eu.europa.esig.dss.validation.reports.CertificateReports;
import jakarta.annotation.Resource;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.exception.EsupSignatureKeystoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.security.KeyStore.PasswordProtection;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class UserKeystoreService {

	private static final Logger logger = LoggerFactory.getLogger(UserKeystoreService.class);

	private final CertificateVerifier certificateVerifier;

	public UserKeystoreService(CertificateVerifier certificateVerifier) {
		this.certificateVerifier = certificateVerifier;
	}

	@Resource
	private UserService userService;
	
	public Pkcs12SignatureToken getPkcs12Token(InputStream keyStoreFile, String password) throws EsupSignatureKeystoreException {
		try {
			return new Pkcs12SignatureToken(keyStoreFile, new PasswordProtection(password.toCharArray()));
		} catch (Exception e) {
			if(e.getCause() != null && e.getCause().getMessage() != null && e.getCause().getMessage().equals("keystore password was incorrect")) {
				logger.warn("keystore password was incorrect");
			} else {
				logger.warn("open keystore fail : " + e.getMessage());
			}
			throw new EsupSignatureKeystoreException("Mot de passe incorrect");
		}
	}

	public CertificateToken getCertificateToken(SignatureTokenConnection token) throws EsupSignatureKeystoreException {
		try {
			DSSPrivateKeyEntry ksPrivateKeyEntry = token.getKeys().get(0);
			return ksPrivateKeyEntry.getCertificate();
		} catch (Exception e) {
			logger.warn("open keystore fail : " + e.getMessage());
			throw new EsupSignatureKeystoreException("Impossible d'obtenir le certificat", e);
		}
	}

	public CertificateToken[] getCertificateTokenChain(SignatureTokenConnection token) {
		DSSPrivateKeyEntry ksPrivateKeyEntry = token.getKeys().get(0);
        return ksPrivateKeyEntry.getCertificateChain();
	}

	@Transactional
	public String checkKeystore(String authUserEppn, String password) throws EsupSignatureKeystoreException {
		User authUser = userService.getByEppn(authUserEppn);
		return checkKeystore(authUser.getKeystore().getInputStream(), password);
	}

	@Transactional
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
		certInfo = getString(certInfo, pkcs12SignatureToken);
		pkcs12SignatureToken.close();
		return certInfo;
	}

	private String getString(String certInfo, Pkcs12SignatureToken pkcs12SignatureToken) {
		CertificateToken[] certificateTokens = getCertificateTokenChain(pkcs12SignatureToken);
		for(CertificateToken token : certificateTokens) {
			X509Certificate cert = token.getCertificate();
			certInfo += "\n\n" + cert.getType() + ""
				+ "\n" + cert.getSubjectX500Principal()
				+ "\n" + cert.getSerialNumber();
		}
		return certInfo;
	}

}
