package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.AuditStep;
import org.esupportail.esupsignature.entity.AuditTrail;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.repository.AuditStepRepository;
import org.esupportail.esupsignature.repository.AuditTrailRepository;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuditTrailService {

    @Resource
    private AuditTrailRepository auditTrailRepository;

    @Resource
    private AuditStepRepository auditStepRepository;

    @Resource
    private UserService userService;

    @Resource
    private FileService fileService;

    @Transactional
    public AuditTrail create(String token) {
        AuditTrail auditTrail = new AuditTrail();
        auditTrail.setToken(token);
        auditTrailRepository.save(auditTrail);
        return auditTrail;
    }

    public void  closeAuditTrail(String token, Document document, InputStream inputStream) throws IOException {
        AuditTrail auditTrail = auditTrailRepository.findByToken(token);
        auditTrail.setDocumentCheckSum(fileService.getFileChecksum(inputStream));
        auditTrail.setDocumentName(document.getFileName());
        auditTrail.setDocumentType(document.getContentType());
        auditTrail.setDocumentSize(document.getSize());
        auditTrail.setDocumentId(document.getId().toString());
    }

    @Transactional
    public void addAuditStep(String token, String userEppn, String certificat, String timeStampCertificat, Date timeStampDate, Boolean allScrolled, Integer page, Integer posX, Integer posY) {
        AuditTrail auditTrail = auditTrailRepository.findByToken(token);
        AuditStep auditStep = createAuditStep(userEppn, certificat, timeStampCertificat, timeStampDate, allScrolled, page, posX, posY);
        auditTrail.getAuditSteps().add(auditStep);
    }

    @Transactional
    public AuditStep createAuditStep(String userEppn, String certificat, String timeStampCertificat, Date timeStampDate, Boolean allScrolled, Integer page, Integer posX, Integer posY) {
        User user = userService.getUserByEppn(userEppn);
        AuditStep auditStep = new AuditStep();
        auditStep.setName(user.getName());
        auditStep.setFirstname(user.getFirstname());
        auditStep.setEmail(user.getEmail());
        auditStep.setPage(page);
        auditStep.setPosX(posX);
        auditStep.setPosY(posY);
        auditStep.setLogin(user.getEppn());
        auditStep.setSignCertificat(certificat);
        auditStep.setTimeStampCertificat(timeStampCertificat);
        auditStep.setTimeStampDate(timeStampDate);
        auditStep.setAllScrolled(allScrolled);
        Map<String, String> authenticationDetails = new HashMap<>();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        authenticationDetails.put("name", authentication.getName());
        authenticationDetails.put("credential", authentication.getCredentials().toString());
        authenticationDetails.put("type", authentication.getClass().getTypeName());
        auditStep.setAuthenticationDetails(authenticationDetails);
        auditStepRepository.save(auditStep);
        return auditStep;
    }

    @Transactional
    public AuditTrail getAuditTrailFromCheksum(String checkSum) {
        return auditTrailRepository.findByDocumentCheckSum(checkSum);
    }

}
