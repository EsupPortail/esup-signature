package org.esupportail.esupsignature.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.token.AbstractKeyStoreTokenConnection;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.Pkcs12SignatureToken;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.sign.SignProperties;
import org.esupportail.esupsignature.entity.Certificat;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.exception.EsupSignatureKeystoreException;
import org.esupportail.esupsignature.repository.CertificatRepository;
import org.esupportail.esupsignature.repository.WorkflowStepRepository;
import org.esupportail.esupsignature.service.mail.MailService;
import org.esupportail.esupsignature.service.utils.sign.OpenSCSignatureToken;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class CertificatService implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(CertificatService.class);

    private final OpenSCSignatureToken openSCSignatureToken;

    private static LoadingCache<String, List<DSSPrivateKeyEntry>> privateKeysCache;

    private static boolean isCertificatWasPresent = false;

    private final UserService userService;

    private final UserKeystoreService userKeystoreService;

    private final MailService mailService;

    private final CertificatRepository certificatRepository;

    private final DocumentService documentService;

    private final WorkflowStepRepository workflowStepRepository;

    private final GlobalProperties globalProperties;

    private final SignProperties signProperties;

    public CertificatService(GlobalProperties globalProperties, SignProperties signProperties, UserService userService, @Autowired(required = false) UserKeystoreService userKeystoreService, MailService mailService, CertificatRepository certificatRepository, DocumentService documentService, WorkflowStepRepository workflowStepRepository) {
        this.globalProperties = globalProperties;
        this.userService = userService;
        this.userKeystoreService = userKeystoreService;
        this.mailService = mailService;
        this.certificatRepository = certificatRepository;
        this.documentService = documentService;
        this.workflowStepRepository = workflowStepRepository;
        this.openSCSignatureToken = new OpenSCSignatureToken(new KeyStore.PasswordProtection(globalProperties.getSealCertificatPin().toCharArray()), signProperties);
        this.signProperties = signProperties;
        privateKeysCache = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build(new CacheLoader<>() {
            @Override
            public @NotNull List<DSSPrivateKeyEntry> load(@NotNull String s) {
                return new ArrayList<>();
            }
        });
    }


    public Certificat getById(Long id) {
        return certificatRepository.findById(id).get();
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
            certificat.setCreateDate(certificateToken.getCreationDate());
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

    public SignatureTokenConnection getSealToken() {
        if((globalProperties.getSealCertificatDriver() != null && globalProperties.getSealCertificatType().equals(GlobalProperties.TokenType.PKCS11)) || globalProperties.getSealCertificatType().equals(GlobalProperties.TokenType.PKCS12)) {
           return getPkcsToken();
        } else if(globalProperties.getSealCertificatType().equals(GlobalProperties.TokenType.OPENSC)){
            return openSCSignatureToken;
        }
        return null;
    }

    public AbstractKeyStoreTokenConnection getPkcsToken() throws EsupSignatureKeystoreException {
        if(globalProperties.getSealCertificatType() != null && globalProperties.getSealCertificatPin() != null) {
            if (globalProperties.getSealCertificatDriver() != null && globalProperties.getSealCertificatType().equals(GlobalProperties.TokenType.PKCS11)) {
                KeyStore.PasswordProtection passwordProtection = new KeyStore.PasswordProtection(globalProperties.getSealCertificatPin().toCharArray());
                return new eu.europa.esig.dss.token.Pkcs11SignatureToken(globalProperties.getSealCertificatDriver(), passwordProtection);
            } else if (globalProperties.getSealCertificatFile() != null && globalProperties.getSealCertificatType().equals(GlobalProperties.TokenType.PKCS12)) {
                try {
                    return userKeystoreService.getPkcs12Token(new FileInputStream(globalProperties.getSealCertificatFile()), globalProperties.getSealCertificatPin());
                } catch (FileNotFoundException e) {
                    logger.error(e.getMessage());
                }
            }
        }
        throw new EsupSignatureKeystoreException("no seal certificat present (no type or no pin");
    }

    public CertificateToken getOpenSCKey() throws DSSException {
        try {
            return DSSUtils.loadCertificate(openSCSignatureToken.getKey().getCertificate().getEncoded());
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    public List<DSSPrivateKeyEntry> getSealCertificats() {
        if(privateKeysCache.getIfPresent("keys") != null) return privateKeysCache.getIfPresent("keys");
        List<DSSPrivateKeyEntry> dssPrivateKeyEntries = new ArrayList<>();
        try {
            if ((globalProperties.getSealCertificatDriver() != null && globalProperties.getSealCertificatType().equals(GlobalProperties.TokenType.PKCS11)) || globalProperties.getSealCertificatType().equals(GlobalProperties.TokenType.PKCS12)) {
                dssPrivateKeyEntries = getPkcsToken().getKeys();
            } else if (globalProperties.getSealCertificatType().equals(GlobalProperties.TokenType.OPENSC)) {
                dssPrivateKeyEntries = openSCSignatureToken.getKeys();
            }
        } catch (Exception e) {
            logger.debug("no seal certificat found", e);
        }
        if(!dssPrivateKeyEntries.isEmpty()) {
            if(!isCertificatWasPresent) {
                String message = "certificat was found on " + globalProperties.getRootUrl();
                mailService.sendAdminError("Seal certificat UP", message);
            }
            isCertificatWasPresent = true;
            privateKeysCache.put("keys", dssPrivateKeyEntries);
        } else if(isCertificatWasPresent) {
            String message = "certificat was present but not found on " + globalProperties.getRootUrl();
            mailService.sendAdminError("Seal certificat DOWN", message);
            isCertificatWasPresent = false;
            logger.error(message);
        }
        return dssPrivateKeyEntries;
    }

    public boolean checkCertificatProblem() {
        boolean certificatProblem = false;
        List<DSSPrivateKeyEntry> dssPrivateKeyEntries = getSealCertificats();
        if(isCertificatWasPresent && dssPrivateKeyEntries.isEmpty()) {
            certificatProblem = true;
        }
        Date lastDate = new DateTime().minusDays(globalProperties.getNbDaysBeforeCertifWarning()).toDate();
        for(Certificat certificat : getAllCertificats()) {
            if(certificat.getExpireDate().before(lastDate)) {
                certificatProblem = true;
                break;
            }
        }
        for(eu.europa.esig.dss.token.DSSPrivateKeyEntry dssPrivateKeyEntry : dssPrivateKeyEntries) {
            if(dssPrivateKeyEntry.getCertificate().getNotAfter().before(lastDate)) {
                certificatProblem = true;
                break;
            }
        }
        return certificatProblem;
    }

    @Override
    public Health health() {
        if(isCertificatWasPresent) {
            return Health.up().withDetail("seal certificat", "UP").build();
        } else {
            return Health.down().withDetail("seal certificat", "DOWN").build();
        }
    }
}