package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.enums.SignWith;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class SignTypeService {

    private final GlobalProperties globalProperties;

    public SignTypeService(GlobalProperties globalProperties) {
        this.globalProperties = globalProperties;
    }

    public List<SignWith> getAuthorizedSignTypes(List<String> roles) {
        List<SignWith> signWith = new ArrayList<>(globalProperties.getAuthorizedSignTypes());
        if(!roles.contains("ROLE_SEAL")) {
            signWith.remove(SignWith.sealCert);
        }
        if(globalProperties.getDisableCertStorage()) {
            signWith.remove(SignWith.userCert);
        }
        if((globalProperties.getOpenXPKIServerUrl() == null || globalProperties.getOpenXPKIServerUrl().isEmpty())) {
            signWith.remove(SignWith.openPkiCert);
        }
        return signWith;
    }

    public SignWith getLessSignType(int minLevel) {
        List<SignWith> signWith = globalProperties.getAuthorizedSignTypes();
        return signWith.stream().filter(s -> s.getValue() >= minLevel).min(Comparator.comparing(SignWith::getValue)).orElse(null);
    }

}
