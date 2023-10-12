package org.esupportail.esupsignature.service;

import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.token.AbstractKeyStoreTokenConnection;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.Pkcs11SignatureToken;
import eu.europa.esig.dss.token.Pkcs12SignatureToken;
import jakarta.annotation.Resource;
import org.apache.commons.lang.SystemUtils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.sign.SignProperties;
import org.esupportail.esupsignature.entity.Certificat;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.exception.EsupSignatureKeystoreException;
import org.esupportail.esupsignature.repository.CertificatRepository;
import org.esupportail.esupsignature.repository.WorkflowStepRepository;
import org.esupportail.esupsignature.service.utils.sign.OpenSCSignatureToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.*;

@Service
public class CertificatService {

    private static final Logger logger = LoggerFactory.getLogger(CertificatService.class);

    @Resource
    private UserService userService;

    private UserKeystoreService userKeystoreService;

    @Autowired(required = false)
    public void setUserKeystoreService(UserKeystoreService userKeystoreService) {
        this.userKeystoreService = userKeystoreService;
    }

    @Resource
    private CertificatRepository certificatRepository;

    @Resource
    private DocumentService documentService;

    @Resource
    private SignProperties signProperties;

    @Resource
    private WorkflowStepRepository workflowStepRepository;

    @Resource
    private final GlobalProperties globalProperties;

    public CertificatService(GlobalProperties globalProperties) {
        this.globalProperties = globalProperties;
    }


    public Certificat getById(Long id) {
        return certificatRepository.findById(id).get();
    }

    public Certificat getCertificatByRole(String role) {
        return certificatRepository.findByRolesIn(Collections.singleton(role)).get(0);
    }

    @Transactional
    public List<Certificat> getCertificatByUser(String userEppn) {
        User user = userService.getByEppn(userEppn);
        Set<String> roles = user.getRoles();
        Set<Certificat> certificats = new HashSet<>(certificatRepository.findByRolesIn(roles));
        return new ArrayList<>(certificats);
    }

    public List<Certificat> getAllCertificats() {
        List<Certificat> certificats = new ArrayList<>();
        certificatRepository.findAll().forEach(certificats::add);
        return certificats;
    }

    @Transactional
    public void addCertificat(MultipartFile keystore, Set<String> roles, String password) throws IOException, EsupSignatureKeystoreException {
        if(userKeystoreService != null) {
            Certificat certificat = new Certificat();
            byte[] keystoreBytes = keystore.getBytes();
            Pkcs12SignatureToken pkcs12SignatureToken = userKeystoreService.getPkcs12Token(new ByteArrayInputStream(keystoreBytes), password);
            CertificateToken certificateToken = userKeystoreService.getCertificateToken(pkcs12SignatureToken);
            certificat.setKeystore(documentService.createDocument(new ByteArrayInputStream(keystoreBytes), userService.getSystemUser(), certificateToken.getSubject().getPrincipal().getName("CANONICAL"), keystore.getContentType()));
            certificat.setRoles(roles);
            certificat.setPassword(encryptPassword(password));
            certificat.setExpireDate(certificateToken.getNotAfter());
            certificatRepository.save(certificat);
        } else {
            logger.warn("impossible to add certificat without certificat verifier");
        }
    }

    @Transactional
    public void delete(Long id) {
        Certificat certificat = getById(id);
        List<WorkflowStep> workflowSteps = workflowStepRepository.findByCertificatId(id);
        for(WorkflowStep workflowStep : workflowSteps) {
            workflowStep.setCertificat(null);
        }
        certificatRepository.delete(certificat);
    }

    public String encryptPassword(String password) {
        try {
            Key aesKey = new SecretKeySpec(signProperties.getAesKey().getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
            String charSet = "UTF-8";
            byte[] encrypted = cipher.doFinal(password.getBytes(charSet));
            return new String(Base64.getEncoder().encode(encrypted));
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    public String decryptPassword(String key) {
        try {
            Key aesKey = new SecretKeySpec(signProperties.getAesKey().getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, aesKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(key)));
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    public AbstractKeyStoreTokenConnection getSealToken() {
        if (globalProperties.getSealCertificatDriver() != null) {
            if(globalProperties.getSealCertificatType().equals("PKCS11") || globalProperties.getSealCertificatType().equals("PKCS12")) {
               return getSealTokenPKCS();
            } else if(globalProperties.getSealCertificatType().equals("OPENSC")){
            return getSealTokenOpenSC();
            }
        }
        return null;
    }

    public AbstractKeyStoreTokenConnection getSealTokenPKCS() throws EsupSignatureKeystoreException {
        if(globalProperties.getSealCertificatType() != null && globalProperties.getSealCertificatPin() != null) {
            if (globalProperties.getSealCertificatDriver() != null && globalProperties.getSealCertificatType().equals("PKCS11")) {
                KeyStore.PasswordProtection passwordProtection = new KeyStore.PasswordProtection(globalProperties.getSealCertificatPin().toCharArray());
                return new Pkcs11SignatureToken(globalProperties.getSealCertificatDriver(), passwordProtection);
            } else if (globalProperties.getSealCertificatFile() != null && globalProperties.getSealCertificatType().equals("PKCS12")) {
                try {
                    return userKeystoreService.getPkcs12Token(new FileInputStream(globalProperties.getSealCertificatFile()), globalProperties.getSealCertificatPin());
                } catch (FileNotFoundException e) {
                    logger.error(e.getMessage());
                }
            }
        }
        throw new EsupSignatureKeystoreException("no seal certificat present (no type or no pin");
    }

    public OpenSCSignatureToken getSealTokenOpenSC() throws EsupSignatureKeystoreException {
        return new OpenSCSignatureToken(new KeyStore.PasswordProtection(globalProperties.getSealCertificatPin().toCharArray()));
    }

    public CertificateToken getKey() throws DSSException {
        byte[] cert = launchProcess("pkcs11-tool -r --id 0001 --type cert");
        return DSSUtils.loadCertificate(cert);
    }

    public CertificateToken[] getCertificateChain() {
        byte[] cert = launchProcess("pkcs11-tool -r --id 0001 --type cert");
        return new CertificateToken[]{DSSUtils.loadCertificate(cert)};
    }

    public byte[] launchProcess(String command) throws DSSException {
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

        }
    }

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    public void transferTo(InputStream in, OutputStream out) throws IOException {
        Objects.requireNonNull(out, "out");
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int read;
        while ((read = in.read(buffer, 0, DEFAULT_BUFFER_SIZE)) >= 0) {
            out.write(buffer, 0, read);
        }
    }

    public KeyStore getSealKeyStore() throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException, EsupSignatureKeystoreException {
        if(globalProperties.getSealCertificatType() != null && globalProperties.getSealCertificatPin() != null) {
            if (globalProperties.getSealCertificatDriver() != null && globalProperties.getSealCertificatType().equals("PKCS11")) {
                KeyStore keyStore = KeyStore.getInstance("PKCS11");
                KeyStore.PasswordProtection passwordProtection = new KeyStore.PasswordProtection(globalProperties.getSealCertificatPin().toCharArray());
                keyStore.load(CertificatService.class.getResourceAsStream(globalProperties.getSealCertificatDriver()), passwordProtection.getPassword());
                return keyStore;
            } else if (globalProperties.getSealCertificatFile() != null && globalProperties.getSealCertificatType().equals("PKCS12")) {
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                KeyStore.PasswordProtection passwordProtection = new KeyStore.PasswordProtection(globalProperties.getSealCertificatPin().toCharArray());
                keyStore.load(new FileInputStream(globalProperties.getSealCertificatFile()), passwordProtection.getPassword());
                return keyStore;
            }
        }
        throw new EsupSignatureKeystoreException("no seal certificat present");
    }

    public PrivateKey getSealPrivateKey() throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, UnrecoverableKeyException, EsupSignatureKeystoreException {
        KeyStore keyStore = getSealKeyStore();
        KeyStore.PasswordProtection passwordProtection = new KeyStore.PasswordProtection(globalProperties.getSealCertificatPin().toCharArray());
        return (PrivateKey) keyStore.getKey(keyStore.aliases().nextElement(), passwordProtection.getPassword());

    }

    public List<DSSPrivateKeyEntry> getSealCertificats() {
        List<DSSPrivateKeyEntry> dssPrivateKeyEntries = new ArrayList<>();
        try {
            if (globalProperties.getSealCertificatDriver() != null) {
                if (globalProperties.getSealCertificatType().equals("PKCS11") || globalProperties.getSealCertificatType().equals("PKCS12")) {
                    dssPrivateKeyEntries = getSealTokenPKCS().getKeys();
                } else if (globalProperties.getSealCertificatType().equals("OPENSC")) {
                    dssPrivateKeyEntries = getSealTokenOpenSC().getKeys();
                }
            }
        } catch (Exception e) {
            logger.debug("no seal certificat found", e);
        }
        return dssPrivateKeyEntries;
    }

}
