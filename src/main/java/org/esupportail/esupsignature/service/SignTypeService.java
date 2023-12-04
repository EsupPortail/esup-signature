package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@EnableConfigurationProperties(GlobalProperties.class)
public class SignTypeService {

    private final GlobalProperties globalProperties;

    public SignTypeService(GlobalProperties globalProperties) {
        this.globalProperties = globalProperties;
    }

    public List<SignType> getAuthorizedSignTypes(List<String> roles) {
        List<SignType> signTypes = new ArrayList<>(globalProperties.getAuthorizedSignTypes());
        if(!roles.contains("ROLE_SEAL") &&  globalProperties.getDisableCertStorage() && (globalProperties.getOpenXPKIServerUrl() == null || globalProperties.getOpenXPKIServerUrl().isEmpty())) {
            signTypes.remove(SignType.certSign);
        }
        return signTypes;
    }

    public SignType getLessSignType(int minLevel) {
        List<SignType> signTypes = globalProperties.getAuthorizedSignTypes();
        return signTypes.stream().filter(s -> s.getValue() >= minLevel).min(Comparator.comparing(SignType::getValue)).orElse(null);
    }

}
