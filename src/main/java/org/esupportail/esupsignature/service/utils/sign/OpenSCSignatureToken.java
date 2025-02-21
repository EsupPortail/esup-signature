package org.esupportail.esupsignature.service.utils.sign;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.EncryptionAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureAlgorithm;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.Digest;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.esupportail.esupsignature.config.sign.SignProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.cert.CertificateEncodingException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class OpenSCSignatureToken implements SignatureTokenConnection {

    private static final Logger logger = LoggerFactory.getLogger(OpenSCSignatureToken.class);
    private final KeyStore.PasswordProtection passwordProtection;
    private String module = "";

    private final SignProperties signProperties;

    public OpenSCSignatureToken(KeyStore.PasswordProtection passwordProtection, SignProperties signProperties) {
        this.passwordProtection = passwordProtection;
        this.signProperties = signProperties;
        if(StringUtils.isNotBlank(signProperties.getOpenscCommandModule())) {
            this.module += " --module " + signProperties.getOpenscCommandModule();
        }
    }

    @Override
    public void close() {

    }

    @Override
    public SignatureValue sign(ToBeSigned toBeSigned, DigestAlgorithm digestAlgorithm, DSSPrivateKeyEntry dssPrivateKeyEntry) throws DSSException {
        final InputStream inputStream = new ByteArrayInputStream(toBeSigned.getBytes());
        if (!(dssPrivateKeyEntry instanceof OpenSCPrivateKeyEntry)) {
            throw new DSSException("Unsupported DSSPrivateKeyEntry instance " + dssPrivateKeyEntry.getClass() + " / Must be OpenSCPrivateKeyEntry.");
        }
        final EncryptionAlgorithm encryptionAlgo = dssPrivateKeyEntry.getEncryptionAlgorithm();
        final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.getAlgorithm(encryptionAlgo, digestAlgorithm);

        logger.info("OpenSC>>>Signature algorithm: " + signatureAlgorithm.getJCEId());
        File tmpDir = null;
        try {
            String password = String.valueOf(passwordProtection.getPassword());
            tmpDir = Files.createTempDirectory("esupdssclient").toFile();
            tmpDir.deleteOnExit();
            File toSignFile = new File(tmpDir + "/toSign");
            FileUtils.copyInputStreamToFile(inputStream, toSignFile);
            File signedFile = new File(tmpDir + "/signed");
            String command = MessageFormat.format(signProperties.getOpenscCommandSign(), getId(), password, toSignFile.getAbsolutePath(), signedFile.getAbsolutePath());
            launchProcess(command + module);
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
    public SignatureValue sign(ToBeSigned toBeSigned, SignatureAlgorithm signatureAlgorithm, DSSPrivateKeyEntry dssPrivateKeyEntry) throws DSSException {
        return null;
    }

    @Override
    public SignatureValue signDigest(Digest digest, DSSPrivateKeyEntry keyEntry) throws DSSException {
        return null;
    }

    @Override
    public SignatureValue signDigest(Digest digest, SignatureAlgorithm signatureAlgorithm, DSSPrivateKeyEntry dssPrivateKeyEntry) throws DSSException {
        return null;
    }

    @Override
    public List<DSSPrivateKeyEntry> getKeys() throws DSSException {
        final List<DSSPrivateKeyEntry> list = new ArrayList<>();
        list.add(getKey());
        return list;
    }

    public DSSPrivateKeyEntry getKey() throws DSSException {
        String command = MessageFormat.format(signProperties.getOpenscCommandGetKey(), getId());
        byte[] cert = launchProcess(command + module);
        CertificateToken certificateToken = DSSUtils.loadCertificate(cert);
        try {
            return new OpenSCPrivateKeyEntry(certificateToken.getCertificate().getEncoded());
        } catch (CertificateEncodingException e) {
            throw new DSSException(e);
        }
    }

    public String getId() {
        String id = signProperties.getOpenscCommandCertId();
        if(StringUtils.isBlank(id)) {
            byte[] ids = launchProcess(signProperties.getOpenscCommandGetId() + module);
            String[] lines = new String(ids).split("\n");
            if (lines.length > 0) {
                String lineWithID = "";
                for (String line : lines) {
                    if (line.contains("ID:")) {
                        lineWithID = line;
                        break;
                    }
                }
                if (lineWithID.split(":").length > 1) {
                    id = lineWithID.split(":")[1].trim();
                }
            } else {
                throw new DSSException("No ID found");
            }
        }
        return id;
    }

    public byte[] launchProcess(String command) throws DSSException {
        Process process = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("bash", "-c", signProperties.getOpenscPathLinux() + command);
            process = processBuilder.start();
            int exitVal = process.waitFor();
            if (exitVal == 0) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                IOUtils.copy(process.getInputStream(), outputStream);
                return outputStream.toByteArray();
            } else {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                IOUtils.copy(process.getInputStream(), outputStream);
                byte[] result = outputStream.toByteArray();
                logger.error("OpenSc command fail");
                StringBuilder output = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(result)));
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                logger.error(output.toString());
                throw new DSSException(output.toString());
            }
        } catch (InterruptedException | IOException e) {
            throw new DSSException(e);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

}