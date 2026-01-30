package org.esupportail.esupsignature.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import eu.europa.esig.dss.enumerations.CertificateQualification;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.simplecertificatereport.SimpleCertificateReport;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.spi.validation.CertificateVerifier;
import eu.europa.esig.dss.token.AbstractKeyStoreTokenConnection;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.Pkcs12SignatureToken;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import eu.europa.esig.dss.validation.CertificateValidator;
import eu.europa.esig.dss.validation.reports.CertificateReports;
import jakarta.annotation.PostConstruct;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.certificat.SealCertificatProperties;
import org.esupportail.esupsignature.config.sign.SignProperties;
import org.esupportail.esupsignature.entity.AppliVersion;
import org.esupportail.esupsignature.entity.Certificat;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.exception.EsupSignatureKeystoreException;
import org.esupportail.esupsignature.repository.AppliVersionRepository;
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
import org.springframework.util.StringUtils;
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
    private static LoadingCache<String, List<DSSPrivateKeyEntry>> dssPrivateKeyEntriesCache;
    private static LoadingCache<String, List<SealCertificatProperties>> sealCertificatPropertiesCache;
    private static boolean isCertificatWasPresent = false;
    private static boolean firstStart = true;
    private final UserService userService;
    private final UserKeystoreService userKeystoreService;
    private final CertificateVerifier certificateVerifier;
    private final MailService mailService;
    private final CertificatRepository certificatRepository;
    private final DocumentService documentService;
    private final WorkflowStepRepository workflowStepRepository;
    private final GlobalProperties globalProperties;
    private final SignProperties signProperties;
    private final AppliVersionRepository appliVersionRepository;

    public CertificatService(GlobalProperties globalProperties, SignProperties signProperties, UserService userService, @Autowired(required = false) UserKeystoreService userKeystoreService, CertificateVerifier certificateVerifier, MailService mailService, CertificatRepository certificatRepository, DocumentService documentService, WorkflowStepRepository workflowStepRepository, AppliVersionRepository appliVersionRepository) {
        this.globalProperties = globalProperties;
        this.userService = userService;
        this.userKeystoreService = userKeystoreService;
        this.certificateVerifier = certificateVerifier;
        this.mailService = mailService;
        this.certificatRepository = certificatRepository;
        this.documentService = documentService;
        this.workflowStepRepository = workflowStepRepository;
        this.appliVersionRepository = appliVersionRepository;
        if(!globalProperties.getSealCertificatProperties().isEmpty()) {
            this.openSCSignatureToken = new OpenSCSignatureToken(new KeyStore.PasswordProtection(globalProperties.getSealCertificatProperties().get("default").getSealCertificatPin().toCharArray()), signProperties);
        } else {
            this.openSCSignatureToken = null;
        }
        this.signProperties = signProperties;
        dssPrivateKeyEntriesCache = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build(new CacheLoader<>() {
            @Override
            public @NotNull List<DSSPrivateKeyEntry> load(@NotNull String s) {
                return new ArrayList<>();
            }
        });

        sealCertificatPropertiesCache = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build(new CacheLoader<>() {
            @Override
            public @NotNull List<SealCertificatProperties> load(@NotNull String s) {
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

    public SignatureTokenConnection getSealToken(SealCertificatProperties sealCertificatProperties) {
        if((StringUtils.hasText(sealCertificatProperties.getSealCertificatDriver()) && sealCertificatProperties.getSealCertificatType().equals(SealCertificatProperties.TokenType.PKCS11)) || sealCertificatProperties.getSealCertificatType().equals(org.esupportail.esupsignature.config.certificat.SealCertificatProperties.TokenType.PKCS12)) {
           return getPkcsToken(sealCertificatProperties);
        } else if(sealCertificatProperties.getSealCertificatType().equals(SealCertificatProperties.TokenType.OPENSC)){
            return openSCSignatureToken;
        }
        return null;
    }

    public AbstractKeyStoreTokenConnection getPkcsToken(SealCertificatProperties sealCertificatProperties) throws EsupSignatureKeystoreException {
        if (StringUtils.hasText(sealCertificatProperties.getSealCertificatPin())) {
            if (StringUtils.hasText(sealCertificatProperties.getSealCertificatDriver()) && sealCertificatProperties.getSealCertificatType().equals(SealCertificatProperties.TokenType.PKCS11)) {
                KeyStore.PasswordProtection passwordProtection = new KeyStore.PasswordProtection(sealCertificatProperties.getSealCertificatPin().toCharArray());
                return new eu.europa.esig.dss.token.Pkcs11SignatureToken(sealCertificatProperties.getSealCertificatDriver(), passwordProtection, sealCertificatProperties.getSealCertificatSlotId());
            } else if (StringUtils.hasText(sealCertificatProperties.getSealCertificatFile()) && sealCertificatProperties.getSealCertificatType().equals(SealCertificatProperties.TokenType.PKCS12)) {
                try {
                    return userKeystoreService.getPkcs12Token(new FileInputStream(sealCertificatProperties.getSealCertificatFile()), sealCertificatProperties.getSealCertificatPin());
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

    @PostConstruct
    public List<DSSPrivateKeyEntry> getSealCertificats() {
        List<AppliVersion> appliVersions = new ArrayList<>();
        appliVersionRepository.findAll().forEach(appliVersions::add);
        if(appliVersions.isEmpty()) {
            AppliVersion appliVersion = new AppliVersion("0.1");
            appliVersions.add(appliVersion);
        }
        if(appliVersions.get(0).isStopCheckSealCertificat()) {
            logger.error("no seal certificat found or configuration error");
            logger.error("La vérification du certificat cachet est bloquée, probablement pour cause de mauvais mot de passe.\n" +
                    "Après contrôle de votre installation/configuration, lancer cette requête pour débloqué la vérification : 'UPDATE appli_version set stop_check_seal_certificat = false;'");
            return new ArrayList<>();
        }
        if(dssPrivateKeyEntriesCache.getIfPresent("keys") != null) return dssPrivateKeyEntriesCache.getIfPresent("keys");
        List<DSSPrivateKeyEntry> dssPrivateKeyEntries = new ArrayList<>();
        for(SealCertificatProperties sealCertificatProperties : globalProperties.getSealCertificatProperties().values()) {
            try {
                if (sealCertificatProperties.getSealCertificatType() != null &&
                        ((sealCertificatProperties.getSealCertificatType().equals(SealCertificatProperties.TokenType.PKCS11) && StringUtils.hasText(sealCertificatProperties.getSealCertificatDriver()))
                                ||
                                (sealCertificatProperties.getSealCertificatType().equals(SealCertificatProperties.TokenType.PKCS12) && StringUtils.hasText(sealCertificatProperties.getSealCertificatFile())))
                ) {
                    dssPrivateKeyEntries.add(getPkcsToken(sealCertificatProperties).getKeys().get(0));
                } else if (sealCertificatProperties.getSealCertificatType() != null && sealCertificatProperties.getSealCertificatType().equals(SealCertificatProperties.TokenType.OPENSC)) {
                    dssPrivateKeyEntries.add(openSCSignatureToken.getKeys().get(0));
                }
            } catch (Exception e) {
                logger.debug("no seal certificat found or configuration error : " + e.getMessage(), e);
                Throwable cause = e;
                while (cause != null) {
                    String pkcs11Error = cause.getMessage();
                    if (pkcs11Error != null && pkcs11Error.contains("PIN")) {
                        logger.error("seal certificat PIN error");
                        String message = "La vérification du certificat cachet est maintenant bloquée pour éviter un verrouillage définitif du certificat \n" +
                                "Après contrôle de votre installation/configuration, lancer cette requête pour débloqué la vérification : 'UPDATE appli_version set stop_check_seal_certificat = false;'";
                        logger.error(message);
                        appliVersions.get(0).setStopCheckSealCertificat(true);
                        appliVersionRepository.save(appliVersions.get(0));
                        mailService.sendAdminError("Attention erreur de code PIN au niveau de votre configuration de certificat cachet !", message);
                        break;
                    }
                    cause = cause.getCause();
                }
            }
        }
        if(!dssPrivateKeyEntries.isEmpty()) {
            if(!isCertificatWasPresent) {
                if(!firstStart) {
                    String message = "certificat was found on " + globalProperties.getRootUrl();
                    mailService.sendAdminError("Seal certificat UP", message);
                } else {
                    firstStart = false;
                }
            }
            isCertificatWasPresent = true;
            dssPrivateKeyEntriesCache.put("keys", dssPrivateKeyEntries);
        } else if(isCertificatWasPresent) {
            String message = "certificat was present but not found on " + globalProperties.getRootUrl();
            mailService.sendAdminError("Seal certificat DOWN", message);
            isCertificatWasPresent = false;
            logger.error(message);
        }
        return dssPrivateKeyEntries;
    }

    public boolean checkCertificatProblem(Set<String> roles) {
        if(!roles.contains("ROLE_ADMIN")) return false;
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

    public List<SealCertificatProperties> getCheckedSealCertificates() {
        if(sealCertificatPropertiesCache.getIfPresent("keys") != null && !sealCertificatPropertiesCache.getIfPresent("keys").isEmpty()) return sealCertificatPropertiesCache.getIfPresent("keys");
        List<SealCertificatProperties> sealCertificatPropertieses = new ArrayList<>();
        for(Map.Entry<String, SealCertificatProperties> sealCertificatProperties : globalProperties.getSealCertificatProperties().entrySet()) {
            try {
                sealCertificatProperties.getValue().sealCertificatName = sealCertificatProperties.getKey();
                DSSPrivateKeyEntry dssPrivateKeyEntry = null;
                if (sealCertificatProperties.getValue().getSealCertificatType() != null &&
                        ((sealCertificatProperties.getValue().getSealCertificatType().equals(SealCertificatProperties.TokenType.PKCS11) && StringUtils.hasText(sealCertificatProperties.getValue().getSealCertificatDriver()))
                                ||
                                (sealCertificatProperties.getValue().getSealCertificatType().equals(SealCertificatProperties.TokenType.PKCS12) && StringUtils.hasText(sealCertificatProperties.getValue().getSealCertificatFile())))
                ) {
                    dssPrivateKeyEntry = getPkcsToken(sealCertificatProperties.getValue()).getKeys().get(0);
                } else if (sealCertificatProperties.getValue().getSealCertificatType() != null && sealCertificatProperties.getValue().getSealCertificatType().equals(SealCertificatProperties.TokenType.OPENSC)) {
                    dssPrivateKeyEntry = openSCSignatureToken.getKeys().get(0);
                }
                if(dssPrivateKeyEntry == null) continue;
                CertificateValidator validator = CertificateValidator.fromCertificate(dssPrivateKeyEntry.getCertificate());
                validator.setCertificateVerifier(certificateVerifier);
                CertificateReports reports = validator.validate();
                SimpleCertificateReport simpleReport = reports.getSimpleReport();
                CertificateQualification qualificationAtValidationTime = simpleReport.getQualificationAtValidationTime();
                sealCertificatProperties.getValue().dssPrivateKeyEntry = dssPrivateKeyEntry;
                sealCertificatProperties.getValue().eIDasValidity = qualificationAtValidationTime.isQc() && qualificationAtValidationTime.isQscd() && qualificationAtValidationTime.isForEseal();
                sealCertificatPropertieses.add(sealCertificatProperties.getValue());
            } catch (Exception e) {
                logger.debug(e.getMessage());
            }
        }
        sealCertificatPropertieses = sealCertificatPropertieses.stream().sorted(Comparator.comparing(SealCertificatProperties::getSealCertificatName)).toList();
        sealCertificatPropertiesCache.put("keys", sealCertificatPropertieses);
        return sealCertificatPropertieses;
    }

    @Override
    public Health health() {
        if(globalProperties.getSealCertificatProperties().isEmpty()) return Health.up().build();
        if(isCertificatWasPresent) {
            return Health.up().withDetail("seal certificat", "UP").build();
        } else {
            return Health.down().withDetail("seal certificat", "DOWN").build();
        }
    }

    public List<SealCertificatProperties> getAuthorizedSealCertificatProperties(String userEppn) {
        User user = userService.getByEppn(userEppn);
        List<SealCertificatProperties> sealCertificatProperties = new ArrayList<>();
        try {
            sealCertificatProperties = new ArrayList<>(getCheckedSealCertificates().stream().filter(sc -> sc.containsRole(user.getRoles().stream().toList())).toList());
            for(SealCertificatProperties sealCertificatProperties1 : globalProperties.getSealCertificatProperties().values().stream().filter(sc -> sc.containsRole(user.getRoles().stream().toList())).toList()) {
                if(!sealCertificatProperties.contains(sealCertificatProperties1) && globalProperties.getSealCertificatProperties().values().stream().anyMatch(sc1 -> sc1.getSealSpareOf().equals(sealCertificatProperties1.sealCertificatName))) {
                    sealCertificatProperties.add(sealCertificatProperties1);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return sealCertificatProperties;
    }

    public void clearSealCertificatsCache() {
        sealCertificatPropertiesCache.refresh("keys");
    }
}