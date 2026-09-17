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
    private static boolean errorLogged = false;
    private final KeyStore.PasswordProtection passwordProtection;
    private String module = "";

    private String slot = "";
    private String resolvedId;
    private final SignProperties signProperties;

    public OpenSCSignatureToken(KeyStore.PasswordProtection passwordProtection, SignProperties signProperties, String module, Integer slotId) {
        this.passwordProtection = passwordProtection;
        this.signProperties = signProperties;
        logger.debug("OpenSC>>>Constructor input module='{}', slotId='{}', openscPathLinux='{}', commandGetId='{}', commandGetKey='{}'",
                module,
                slotId,
                signProperties.getOpenscPathLinux(),
                signProperties.getOpenscCommandGetId(),
                signProperties.getOpenscCommandGetKey());
        if(StringUtils.isNotBlank(module)) {
            this.module = " --module " + module;
        }
        if(slotId != null) {
            this.slot = " --slot " + slotId;
        } else {
            detectSlot();
        }
        logger.debug("OpenSC>>>Initialized with module parameter: {} and slot: {}", this.module.isEmpty() ? "none" : this.module, this.slot);
    }

    private void detectSlot() {
        try {
            byte[] output = launchProcess("pkcs11-tool -L" + module, false);
            String result = new String(output);
            logger.debug("OpenSC>>>pkcs11-tool -L output:\n{}", result);
            String[] lines = result.split("\n");
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].contains("Slot")) {
                    for (int j = i + 1; j < lines.length && j < i + 5; j++) {
                        if (lines[j].contains("token label")) {
                            String slotLine = lines[i];
                            int start = slotLine.indexOf("(");
                            int end = slotLine.indexOf(")");
                            if (start != -1 && end != -1) {
                                this.slot = " --slot " + slotLine.substring(start + 1, end);
                                logger.debug("OpenSC>>>Detected token slot from line '{}': {}", slotLine, this.slot);
                                return;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("OpenSC>>>Error detecting slot (OpenSC might not be installed): {}", e.getMessage());
        }
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
            logger.debug("OpenSC>>>Executing sign command: {}", maskSensitiveCommand(command + module + slot));
            launchProcess(command + module + slot);
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
        String id = getId();
        logger.debug("OpenSC>>>Resolved ID before certificate read: '{}', module='{}', slot='{}'", id, module, slot);
        byte[] cert;
        try {
            cert = readCertificate(id, true);
            resolvedId = id;
        } catch (DSSException e) {
            logger.warn("OpenSC>>>Failed to get certificate by ID: {}, attempting fallback", id);
            cert = findFallbackCertificate(id);
            if (cert == null) {
                throw e;
            }
        }
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

    private byte[] readCertificate(String id) {
        return readCertificate(id, true);
    }

    private byte[] readCertificate(String id, boolean logError) {
        String command = MessageFormat.format(signProperties.getOpenscCommandGetKey(), id);
        logger.debug("OpenSC>>>Executing getKey command: {}", command + module + slot);
        return launchProcess(command + module + slot, logError);
    }

    private byte[] findFallbackCertificate(String failedId) {
        List<String> certificateIds = new ArrayList<>();
        addZeroPaddedHexId(certificateIds, failedId);
        for (String certificateId : getCertificateIds()) {
            if (!certificateIds.contains(certificateId)) {
                certificateIds.add(certificateId);
            }
            addZeroPaddedHexId(certificateIds, certificateId);
        }
        logger.debug("OpenSC>>>Fallback candidate certificate IDs after normalization: {}", certificateIds);
        if (certificateIds.isEmpty()) {
            logger.warn("OpenSC>>>No certificate ID found during fallback");
            return null;
        }

        for (String certificateId : certificateIds) {
            if (StringUtils.equals(certificateId, failedId)) {
                continue;
            }
            try {
                logger.info("OpenSC>>>Trying fallback with certificate ID: {}", certificateId);
                byte[] cert = readCertificate(certificateId, false);
                resolvedId = certificateId;
                return cert;
            } catch (DSSException fallbackException) {
                logger.debug("OpenSC>>>Fallback failed for certificate ID {}: {}", certificateId, fallbackException.getMessage());
            }
        }

        return null;
    }

    private void addZeroPaddedHexId(List<String> ids, String id) {
        if (StringUtils.isBlank(id) || id.length() % 2 == 0 || !id.matches("[0-9a-fA-F]+")) {
            return;
        }
        String paddedId = "0" + id;
        if (!ids.contains(paddedId)) {
            ids.add(paddedId);
        }
        String twoBytesPaddedId = "00" + paddedId;
        if (!ids.contains(twoBytesPaddedId)) {
            ids.add(twoBytesPaddedId);
        }
    }

    private List<String> getCertificateIds() {
        List<String> ids = new ArrayList<>();
        addIdsFromCommand(ids, "pkcs11-tool -O --type cert" + module + slot);
        if (ids.isEmpty()) {
            addIdsFromCommand(ids, "pkcs11-tool -O" + module + slot);
        }
        if (ids.isEmpty() && StringUtils.isNotBlank(slot)) {
            logger.debug("OpenSC>>>No certificate ID found with slot {}, retrying without slot", slot);
            addIdsFromCommand(ids, "pkcs11-tool -O --type cert" + module);
        }
        logger.debug("OpenSC>>>Certificate IDs discovered from OpenSC listings: {}", ids);
        return ids;
    }

    private void addIdsFromCommand(List<String> ids, String command) {
        try {
            logger.debug("OpenSC>>>Listing IDs with command: {}", command);
            byte[] output = launchProcess(command, false);
            String outputString = new String(output);
            logger.debug("OpenSC>>>Listing output for '{}':\n{}", command, outputString);
            for (String id : extractIds(outputString)) {
                if (!ids.contains(id)) {
                    ids.add(id);
                }
            }
        } catch (DSSException e) {
            logger.debug("OpenSC>>>Unable to list certificate IDs with command '{}': {}", command, e.getMessage());
        }
    }

    public String getId() {
        if (StringUtils.isNotBlank(resolvedId)) {
            logger.debug("OpenSC>>>Using resolved certificate ID: {}", resolvedId);
            return resolvedId;
        }

        String id = signProperties.getOpenscCommandCertId();
        if(StringUtils.isBlank(id)) {
            logger.debug("OpenSC>>>No configured certificate ID, starting auto-detection");
            for (String command : getIdDetectionCommands()) {
                id = extractId(command, false);
                if (id != null) {
                    break;
                }
            }
            if (id == null) {
                if(!errorLogged) {
                    logger.error("OpenSC configuration requires OpenSC tools, but they were not found or no ID was detected.");
                    errorLogged = true;
                } else {
                    logger.debug("OpenSC configuration requires OpenSC tools, but they were not found or no ID was detected.");
                }
                throw new DSSException("No ID found");
            }
        } else {
            id = normalizeId(id);
            logger.debug("OpenSC>>>Using configured certificate ID: {}", id);
        }
        logger.debug("OpenSC>>>getId returning '{}'", id);
        return id;
    }

    private List<String> getIdDetectionCommands() {
        List<String> commands = new ArrayList<>();
        addIdDetectionCommands(commands, slot);
        if (StringUtils.isNotBlank(slot)) {
            addIdDetectionCommands(commands, "");
        }
        return commands;
    }

    private void addIdDetectionCommands(List<String> commands, String slotOption) {
        addCommand(commands, signProperties.getOpenscCommandGetId() + module + slotOption);
        addCommand(commands, "pkcs11-tool -O --type pubkey" + module + slotOption);
        addCommand(commands, "pkcs11-tool -O --type cert" + module + slotOption);
        addCommand(commands, "pkcs11-tool -O" + module + slotOption);
    }

    private void addCommand(List<String> commands, String command) {
        if (StringUtils.isNotBlank(command) && !commands.contains(command)) {
            commands.add(command);
        }
    }

    private String extractId(String command, boolean logError) {
        logger.debug("OpenSC>>>Executing command for ID extraction: {}", command);
        try {
            byte[] outputBytes = launchProcess(command, logError);
            String output = new String(outputBytes);
            logger.debug("OpenSC>>>ID extraction output:\n{}", output);
            List<String> ids = extractIds(output);
            if (!ids.isEmpty()) {
                logger.debug("OpenSC>>>Normalized IDs from command '{}': {}", command, ids);
                return ids.get(0);
            }
            logger.debug("OpenSC>>>No ID found in output for command '{}'", command);
        } catch (Exception e) {
            if (logError) {
                logger.warn("OpenSC>>>Error during ID extraction: {}", e.getMessage());
            } else {
                logger.debug("OpenSC>>>Error during ID extraction: {}", e.getMessage());
            }
        }
        return null;
    }

    static List<String> extractIds(String output) {
        List<String> ids = new ArrayList<>();
        String[] lines = output.split("\n");
        for (String line : lines) {
            int idIndex = line.indexOf("ID:");
            if (idIndex == -1) {
                continue;
            }
            logger.debug("OpenSC>>>Found ID line: {}", line);
            String id = normalizeId(line.substring(idIndex + 3));
            if (StringUtils.isNotBlank(id) && !ids.contains(id)) {
                ids.add(id);
            }
        }
        return ids;
    }

    static String normalizeId(String rawId) {
        if (StringUtils.isBlank(rawId)) {
            logger.debug("OpenSC>>>normalizeId received blank raw ID");
            return null;
        }
        String id = rawId.trim();
        String hexId = extractHexId(id);
        if (StringUtils.isNotBlank(hexId)) {
            logger.trace("OpenSC>>>normalizeId raw='{}' normalized from hex='{}'", rawId, hexId);
            return hexId;
        }
        int commentIndex = id.indexOf("(");
        if (commentIndex != -1) {
            id = id.substring(0, commentIndex).trim();
        }
        if (id.contains(" ")) {
            id = id.split("\\s+")[0].trim();
        }
        String normalizedId = StringUtils.strip(id, "'\"");
        logger.trace("OpenSC>>>normalizeId raw='{}' normalized='{}'", rawId, normalizedId);
        return normalizedId;
    }

    private static String extractHexId(String id) {
        int hexMarkerIndex = id.indexOf("0x");
        if (hexMarkerIndex == -1) {
            return null;
        }
        int hexEndIndex = id.indexOf(")", hexMarkerIndex);
        String hexId = hexEndIndex == -1 ? id.substring(hexMarkerIndex + 2) : id.substring(hexMarkerIndex + 2, hexEndIndex);
        hexId = hexId.replace(":", "").replace(" ", "").trim();
        return hexId.matches("[0-9a-fA-F]+") ? hexId.toLowerCase() : null;
    }

    public String getOpenSCVersion() {
        try {
            byte[] output = launchProcess("opensc-tool -i", false);
            String outputStr = new String(output).trim();
            return outputStr.split("\n")[0].trim();
        } catch (Exception e) {
            logger.debug("OpenSC not detected via opensc-tool: {}", e.getMessage());
            return null;
        }
    }

    public byte[] launchProcess(String command) throws DSSException {
        return launchProcess(command, true);
    }

    public byte[] launchProcess(String command, boolean logError) throws DSSException {
        Process process = null;

        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            String os = System.getProperty("os.name").toLowerCase();
            String fullCommand = command;
            String openscPath = signProperties.getOpenscPathLinux();
            if (StringUtils.isNotBlank(openscPath) && !openscPath.endsWith("/") && !openscPath.endsWith("\\")) {
                if (os.contains("win")) {
                    openscPath += "\\";
                } else {
                    openscPath += "/";
                }
            }

            if (os.contains("win")) {
                fullCommand = (StringUtils.isNotBlank(openscPath) ? openscPath.replace("/", "\\") : "") + command;
                processBuilder.command("cmd.exe", "/c", fullCommand);
                String path = System.getenv("Path");
                if (path != null && StringUtils.isNotBlank(openscPath)) {
                    processBuilder.environment().put("Path", path + ";" + openscPath.replace("/", "\\"));
                }
            } else {
                fullCommand = (StringUtils.isNotBlank(openscPath) ? openscPath : "") + command;
                processBuilder.command("bash", "-c", fullCommand);
            }
            logger.debug("OpenSC>>>Full command: {}", maskSensitiveCommand(fullCommand));
            logger.trace("OpenSC>>>Process command array: {}", maskSensitiveCommand(processBuilder.command().toString()));
            processBuilder.redirectErrorStream(false);

            process = processBuilder.start();

            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            IOUtils.copy(process.getInputStream(), stdout);

            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            IOUtils.copy(process.getErrorStream(), stderr);

            int exitVal = process.waitFor();

            byte[] result = stdout.toByteArray();
            String err = stderr.toString();
            String out = new String(result);

            logger.debug("OpenSC>>>Command exit code: {}, stdout size: {}, stderr size: {}", exitVal, result.length, err.length());
            if (!out.isBlank() && shouldLogStdout(command)) {
                logger.trace("OpenSC stdout for command '{}':\n{}", maskSensitiveCommand(fullCommand), out);
            }

            if (!err.isBlank() && logError) {
                logger.warn("OpenSC stderr:\n{}", err);
            }

            if (exitVal == 0) {
                logger.debug("OpenSC>>>Command executed successfully");
                return result;
            }

            String errorMessage = "OpenSC command failed with exit code: " + exitVal;
            if (!err.isBlank()) {
                errorMessage += ". Error: " + err.trim();
            }

            if (logError) {
                logger.error("OpenSC>>>{}", errorMessage);
                logger.error("OpenSC>>>stdout size: {}", result.length);
            }

            throw new DSSException(errorMessage);

        } catch (InterruptedException | IOException e) {
            logger.error("OpenSC>>>Exception during command execution", e);
            throw new DSSException(e);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private static boolean shouldLogStdout(String command) {
        return command.contains(" -O") || command.contains(" -L") || command.contains("opensc-tool -i");
    }

    private static String maskSensitiveCommand(String command) {
        if (StringUtils.isBlank(command)) {
            return command;
        }
        return command
                .replaceAll("(?i)(\\s-p\\s+)(\"[^\"]*\"|'[^']*'|\\S+)", "$1******")
                .replaceAll("(?i)(\\s--pin\\s+)(\"[^\"]*\"|'[^']*'|\\S+)", "$1******")
                .replaceAll("(?i)(\\s--pin=)(\"[^\"]*\"|'[^']*'|\\S+)", "$1******");
    }
}
