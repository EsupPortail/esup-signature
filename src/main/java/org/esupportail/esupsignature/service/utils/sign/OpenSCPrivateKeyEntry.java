package org.esupportail.esupsignature.service.utils.sign;

import eu.europa.esig.dss.enumerations.EncryptionAlgorithm;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenSCPrivateKeyEntry implements DSSPrivateKeyEntry {
    private static final Logger LOG = LoggerFactory.getLogger(OpenSCPrivateKeyEntry.class);
    private CertificateToken signingCert;
    private EncryptionAlgorithm encryptionAlgorithm;
    private CertificateToken[] certificateChain = new CertificateToken[1];

    public OpenSCPrivateKeyEntry(byte[] signingCert) {
        this.initialise(signingCert);
    }

    private void initialise(byte[] signingCertBinary) {
        this.signingCert = DSSUtils.loadCertificate(signingCertBinary);
        LOG.info("OpenSC>>>Signing certificate subject name/serial number: " + this.signingCert.getCertificate().getSubjectX500Principal().getName() + "/" + this.signingCert.getSerialNumber());
        String encryptionAlgo = this.signingCert.getPublicKey().getAlgorithm();
        this.encryptionAlgorithm = EncryptionAlgorithm.forName(encryptionAlgo);
        LOG.info("OpenSC>>>EncryptionAlgorithm from public key: " + this.encryptionAlgorithm.getName());
        this.certificateChain[0] = this.signingCert;
    }

    public CertificateToken getCertificate() {
        return this.signingCert;
    }

    public CertificateToken[] getCertificateChain() {
        return this.certificateChain;
    }

    public EncryptionAlgorithm getEncryptionAlgorithm() throws DSSException {
        return this.encryptionAlgorithm;
    }

}
