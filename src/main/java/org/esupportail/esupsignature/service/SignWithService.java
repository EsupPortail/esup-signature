package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.sign.SignProperties;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.entity.enums.SignWith;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@EnableConfigurationProperties({GlobalProperties.class, SignProperties.class})
public class SignWithService {

    @Resource
    private UserService userService;

    @Resource
    private CertificatService certificatService;

    private final GlobalProperties globalProperties;

    public SignWithService(GlobalProperties globalProperties) {
        this.globalProperties = globalProperties;
    }

    @Transactional
    public List<SignWith> getAuthorizedSignWiths(String userEppn, SignType signType) {
        User user = userService.getUserByEppn(userEppn);
        List<SignWith> signWiths = new ArrayList<>(List.of(SignWith.values()));
        if(signType != null) {
            signWiths.removeIf(signWith -> signWith.getValue() < signType.getValue());
        }
        if(globalProperties.getDisableCertStorage() || user.getKeystore() == null) {
            signWiths.remove(SignWith.userCert);
        }
        if(globalProperties.getSealCertificatDriver() == null) {
            signWiths.remove(SignWith.sealCert);
        }
        if(certificatService.getCertificatByUser(user.getEppn()).size() == 0) {
            signWiths.remove(SignWith.groupCert);
        }
        if(globalProperties.getOpenXPKIServerUrl() == null) {
            signWiths.remove(SignWith.openPkiCert);
        }
        return signWiths;
    }

}
