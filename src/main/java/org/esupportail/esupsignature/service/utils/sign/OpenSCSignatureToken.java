package org.esupportail.esupsignature.service.utils.sign;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.EncryptionAlgorithm;
import eu.europa.esig.dss.enumerations.MaskGenerationFunction;
import eu.europa.esig.dss.enumerations.SignatureAlgorithm;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.Digest;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.token.AbstractKeyStoreTokenConnection;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Product adapter for {@link OpenSC}.
 *
 * @author David Lemaignent (david.lemaignent@univ-rouen.fr)
 */
public class OpenSCSignatureToken extends AbstractKeyStoreTokenConnection {

    private static final Logger LOG = LoggerFactory.getLogger(OpenSCSignatureToken.class);
    private final KeyStore.PasswordProtection passwordProtection;
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    public OpenSCSignatureToken(KeyStore.PasswordProtection passwordProtection) {
        this.passwordProtection = passwordProtection;
    }

    @Override
    public void close() {

    }

    @Override
    public SignatureValue sign(ToBeSigned toBeSigned, DigestAlgorithm digestAlgorithm, DSSPrivateKeyEntry dssPrivateKeyEntry) throws DSSException {
        final InputStream inputStream = new ByteArrayInputStream(toBeSigned.getBytes());
        final EncryptionAlgorithm encryptionAlgo = dssPrivateKeyEntry.getEncryptionAlgorithm();
        final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.getAlgorithm(encryptionAlgo, digestAlgorithm);

        LOG.info("OpenSC>>>Signature algorithm: " + signatureAlgorithm.getJCEId());
        File tmpDir = null;
        try {
            String password = String.valueOf(passwordProtection.getPassword());
            tmpDir = Files.createTempDirectory("esupdssclient").toFile();
            tmpDir.deleteOnExit();
            File toSignFile = new File(tmpDir + "/toSign");
            FileUtils.copyInputStreamToFile(inputStream, toSignFile);
            File signedFile = new File(tmpDir + "/signed");
            launchProcess("pkcs11-tool --sign -v -p " + password + " --id 0001 --mechanism SHA256-RSA-PKCS --input-file " + toSignFile.getAbsolutePath() + " --output-file " + signedFile.getAbsolutePath());
            SignatureValue value = new SignatureValue();
            value.setAlgorithm(signatureAlgorithm);
            value.setValue(FileUtils.readFileToByteArray(signedFile));
            toSignFile.delete();
            signedFile.delete();
            return value;
        } catch (IOException e) {
            throw new DSSException(e);
        } finally {
            tmpDir.delete();
        }
    }

    @Override
    public SignatureValue sign(ToBeSigned toBeSigned, DigestAlgorithm digestAlgorithm, MaskGenerationFunction maskGenerationFunction, DSSPrivateKeyEntry dssPrivateKeyEntry) throws DSSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SignatureValue sign(ToBeSigned toBeSigned, SignatureAlgorithm signatureAlgorithm, DSSPrivateKeyEntry dssPrivateKeyEntry) throws DSSException {
        return sign(toBeSigned, signatureAlgorithm, dssPrivateKeyEntry);
    }

    @Override
    public SignatureValue signDigest(Digest digest, DSSPrivateKeyEntry dssPrivateKeyEntry) throws DSSException {
        return null;
    }

    @Override
    public SignatureValue signDigest(Digest digest, MaskGenerationFunction maskGenerationFunction, DSSPrivateKeyEntry dssPrivateKeyEntry) throws DSSException {
        return null;
    }

    @Override
    public SignatureValue signDigest(Digest digest, SignatureAlgorithm signatureAlgorithm, DSSPrivateKeyEntry dssPrivateKeyEntry) throws DSSException {
        return null;
    }

    @Override
    protected KeyStore getKeyStore() throws DSSException {
        return null;
    }

    @Override
    protected KeyStore.PasswordProtection getKeyProtectionParameter() {
        return null;
    }

    @Override
    public List<DSSPrivateKeyEntry> getKeys() throws DSSException {
        final List<DSSPrivateKeyEntry> list = new ArrayList<>();
        list.add(getKey());
        return list;
    }

    public DSSPrivateKeyEntry getKey() throws DSSException {
        byte[] cert = launchProcess("pkcs11-tool -r --id 0001 --type cert");
        CertificateToken certificateToken = DSSUtils.loadCertificate(cert);
        try {
            return new OpenSCPrivateKeyEntry(certificateToken.getCertificate().getEncoded());
        } catch (CertificateEncodingException e) {
            throw new DSSException(e);
        }
    }

    public synchronized byte[] launchProcess(String command) throws DSSException {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            if(SystemUtils.IS_OS_WINDOWS) {
                processBuilder.command("cmd", "/C", command);
                Map<String, String> envs = processBuilder.environment();
                System.out.println(envs.get("Path"));
                envs.put("Path", "C:\\Program Files\\OpenSC Project\\OpenSC\\tools");
            } else {
                processBuilder.command("bash", "-c", command);
            }
            Process process = processBuilder.start();
            int exitVal = process.waitFor();
            if (exitVal == 0) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                transferTo(process.getInputStream(), outputStream);
                return outputStream.toByteArray();
            } else {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                transferTo(process.getErrorStream(), outputStream);
                byte[] result = outputStream.toByteArray();
                LOG.error("OpenSc command fail");
                StringBuilder output = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(result)));
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                LOG.error(output.toString());
                throw new DSSException(output.toString());
            }
        } catch (InterruptedException | IOException e) {
            throw new DSSException(e);

        }
    }


    public long transferTo(InputStream in, OutputStream out) throws IOException {
        Objects.requireNonNull(out, "out");
        long transferred = 0;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int read;
        while ((read = in.read(buffer, 0, DEFAULT_BUFFER_SIZE)) >= 0) {
            out.write(buffer, 0, read);
            transferred += read;
        }
        return transferred;
    }

}
