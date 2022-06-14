package org.esupportail.esupsignature.service;

import eu.europa.esig.dss.token.AbstractKeyStoreTokenConnection;
import eu.europa.esig.dss.token.Pkcs11SignatureToken;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.sign.SignProperties;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignWith;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

@Service
@EnableConfigurationProperties({GlobalProperties.class, SignProperties.class})
public class SignWithService {

    @Resource
    private UserService userService;

    @Resource
    private DataService dataService;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private CertificatService certificatService;

    @Resource

    private final GlobalProperties globalProperties;

    public SignWithService(GlobalProperties globalProperties) {
        this.globalProperties = globalProperties;
    }

    @Transactional
    public List<SignWith> getAuthorizedSignWiths(String userEppn, SignRequest signRequest) {
        User user = userService.getUserByEppn(userEppn);
        List<SignWith> signWiths = new ArrayList<>(List.of(SignWith.values()));
        if(globalProperties.getDisableCertStorage() || user.getKeystore() == null) {
            signWiths.remove(SignWith.userCert);
        }
        if(globalProperties.getSealCertificatDriver() == null || !user.getRoles().contains("ROLE_SEAL")) {
            signWiths.remove(SignWith.sealCert);
        }
        if(certificatService.getCertificatByUser(user.getEppn()).size() == 0) {
            signWiths.remove(SignWith.groupCert);
        }
        if(globalProperties.getOpenXPKIServerUrl() == null) {
            signWiths.remove(SignWith.openPkiCert);
        }
        if(signRequest.getCurrentSignType() != null) {
            signWiths.removeIf(signWith -> signWith.getValue() < signRequest.getCurrentSignType().getValue());
        }
        if(dataService.getBySignBook(signRequest.getParentSignBook()) != null && signRequestService.isMoreWorkflowStep(signRequest.getParentSignBook())) {
            signWiths.removeIf(signWith -> signWith.getValue() > 2);
        }
        return signWiths;
    }

    public boolean checkSealCertificat(String userEppn) {
        User user = userService.getUserByEppn(userEppn);
        if(globalProperties.getSealCertificatDriver() != null && user.getRoles().contains("ROLE_SEAL")) {
            try {
                KeyStore.PasswordProtection passwordProtection = new KeyStore.PasswordProtection(globalProperties.getSealCertificatPin().toCharArray());
                AbstractKeyStoreTokenConnection abstractKeyStoreTokenConnection = new Pkcs11SignatureToken(globalProperties.getSealCertificatDriver(), passwordProtection);
                abstractKeyStoreTokenConnection.getKeys();
                return true;
            } catch (Exception e) {
                return false;
            }
        } else {
            return true;
        }
    }
}
