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

    public OpenSCSignatureToken(KeyStore.PasswordProtection passwordProtection, SignProperties signProperties, String module) {
        this.passwordProtection = passwordProtection;
        this.signProperties = signProperties;
        if(StringUtils.isNotBlank(module)) {
            this.module += " --module " + module;
        }
        logger.debug("OpenSC>>>Initialized with module parameter: {}", this.module.isEmpty() ? "none (will use default)" : this.module);
    }

    @Override
    public void close() {
        logger.debug("OpenSC>>>Closing connection");
    }

    @Override
    public SignatureValue sign(ToBeSigned toBeSigned, DigestAlgorithm digestAlgorithm, DSSPrivateKeyEntry dssPrivateKeyEntry) throws DSSException {
        final InputStream inputStream = new ByteArrayInputStream(toBeSigned.getBytes());
        if (!(dssPrivateKeyEntry instanceof OpenSCPrivateKeyEntry)) {
            throw new DSSException("Unsupported DSSPrivateKeyEntry instance " + dssPrivateKeyEntry.getClass() + " / Must be OpenSCPrivateKeyEntry.");
        }
        final EncryptionAlgorithm encryptionAlgo = dssPrivateKeyEntry.getEncryptionAlgorithm();
        final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.getAlgorithm(encryptionAlgo, digestAlgorithm);

        logger.debug("OpenSC>>>Signature algorithm: {}", signatureAlgorithm.getJCEId());
        File tmpDir = null;
        try {
            String password = String.valueOf(passwordProtection.getPassword());
            tmpDir = Files.createTempDirectory("esupdssclient").toFile();
            tmpDir.deleteOnExit();
            File toSignFile = new File(tmpDir + "/toSign");
            FileUtils.copyInputStreamToFile(inputStream, toSignFile);
            File signedFile = new File(tmpDir + "/signed");
            String command = MessageFormat.format(signProperties.getOpenscCommandSign(), getId(), password, toSignFile.getAbsolutePath(), signedFile.getAbsolutePath());
            logger.debug("OpenSC>>>Executing sign command: {}", command + module);
            launchProcess(command + module);
            SignatureValue value = new SignatureValue();
            value.setAlgorithm(signatureAlgorithm);
            value.setValue(FileUtils.readFileToByteArray(signedFile));
            toSignFile.delete();
            signedFile.delete();
            logger.debug("OpenSC>>>Signature created successfully");
            return value;
        } catch (IOException e) {
            logger.error("OpenSC>>>IO error during signing", e);
            throw new DSSException(e);
        } finally {
            if(tmpDir != null) {
                tmpDir.delete();
            }
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
        logger.debug("OpenSC>>>Getting keys");
        final List<DSSPrivateKeyEntry> list = new ArrayList<>();
        list.add(getKey());
        return list;
    }

    public DSSPrivateKeyEntry getKey() throws DSSException {
        logger.debug("OpenSC>>>Getting certificate for signature");
        String command = MessageFormat.format(signProperties.getOpenscCommandGetKey(), getId());
        logger.debug("OpenSC>>>Executing getKey command: {}", command + module);
        byte[] cert = launchProcess(command + module);
        logger.debug("OpenSC>>>Certificate retrieved, size: {} bytes", cert.length);
        CertificateToken certificateToken = DSSUtils.loadCertificate(cert);
        logger.debug("OpenSC>>>Certificate loaded successfully");
        try {
            return new OpenSCPrivateKeyEntry(certificateToken.getCertificate().getEncoded());
        } catch (CertificateEncodingException e) {
            logger.error("OpenSC>>>Error encoding certificate", e);
            throw new DSSException(e);
        }
    }

    public String getId() {
        String id = signProperties.getOpenscCommandCertId();
        if(StringUtils.isBlank(id)) {
            String command = signProperties.getOpenscCommandGetId() + module;
            logger.debug("OpenSC>>>Executing getId command: {}", command);
            byte[] ids = launchProcess(command);
            String output = new String(ids);
            logger.debug("OpenSC>>>getId output:\n{}", output);
            String[] lines = output.split("\n");
            if (lines.length > 0) {
                String lineWithID = "";
                for (String line : lines) {
                    if (line.contains("ID:")) {
                        lineWithID = line;
                        logger.debug("OpenSC>>>Found ID line: {}", lineWithID);
                        break;
                    }
                }
                if (lineWithID.split(":").length > 1) {
                    id = lineWithID.split(":")[1].trim();
                    logger.debug("OpenSC>>>Extracted ID: {}", id);
                }
            } else {
                logger.error("OpenSC>>>No output from getId command");
                throw new DSSException("No ID found");
            }
        } else {
            logger.debug("OpenSC>>>Using configured certificate ID: {}", id);
        }
        return id;
    }

    public byte[] launchProcess(String command) throws DSSException {
        Process process = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            String fullCommand = signProperties.getOpenscPathLinux() + command;
            logger.debug("OpenSC>>>Full command: {}", fullCommand);
            processBuilder.command("bash", "-c", fullCommand);

            // Rediriger stderr vers stdout pour capturer tous les messages
            processBuilder.redirectErrorStream(true);

            process = processBuilder.start();
            int exitVal = process.waitFor();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            IOUtils.copy(process.getInputStream(), outputStream);
            byte[] result = outputStream.toByteArray();

            if (exitVal == 0) {
                logger.debug("OpenSC>>>Command executed successfully");
                return result;
            } else {
                logger.error("OpenSC>>>Command failed with exit code: {}", exitVal);
                String output = new String(result);
                logger.error("OpenSC>>>Error output:\n{}", output);
                throw new DSSException("OpenSC command failed with exit code " + exitVal + ": " + output);
            }
        } catch (InterruptedException | IOException e) {
            logger.error("OpenSC>>>Exception during command execution", e);
            throw new DSSException(e);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }
}