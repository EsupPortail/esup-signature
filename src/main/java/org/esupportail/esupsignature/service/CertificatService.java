package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.Certificat;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.repository.CertificatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;

@Service
public class CertificatService {

    @Resource
    private CertificatRepository certificatRepository;

    @Resource
    private DocumentService documentService;

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
        certificat.setPassword(password);
        certificatRepository.save(certificat);
    }

    @Transactional
    public void delete(Long id) {
        Certificat certificat = getById(id);
        certificatRepository.delete(certificat);
    }

}
