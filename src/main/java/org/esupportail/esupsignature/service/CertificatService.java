package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.config.sign.SignProperties;
import org.esupportail.esupsignature.entity.Certificat;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.repository.CertificatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.Key;
import java.util.*;

@Service
public class CertificatService {

    private static final Logger logger = LoggerFactory.getLogger(CertificatService.class);

    @Resource
    private CertificatRepository certificatRepository;

    @Resource
    private DocumentService documentService;

    @Resource
    private SignProperties signProperties;


    public Certificat getById(Long id) {
        return certificatRepository.findById(id).get();
    }

    public Certificat getCertificatByRole(String role) {
        return certificatRepository.findByRolesIn(Collections.singletonList(role)).get(0);
    }

    public List<Certificat> getCertificatByUser(User user) {
        Set<Certificat> certificats = new HashSet<>(certificatRepository.findByRolesIn(user.getRoles()));
        return new ArrayList<>(certificats);
    }

    public List<Certificat> getAllCertificats() {
        List<Certificat> certificats = new ArrayList<>();
        certificatRepository.findAll().forEach(certificats::add);
        return certificats;
    }

    @Transactional
    public void addCertificat(MultipartFile keystore, List<String> roles, String password) throws IOException {
        Certificat certificat = new Certificat();
        certificat.setKeystore(documentService.createDocument(keystore.getInputStream(), keystore.getName(), keystore.getContentType()));
        certificat.setRoles(roles);
        certificat.setPassword(encryptPassword(password));
        certificatRepository.save(certificat);
    }

    @Transactional
    public void delete(Long id) {
        Certificat certificat = getById(id);
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

}
