package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@EnableConfigurationProperties(GlobalProperties.class)
public class SignTypeService {

    private final GlobalProperties globalProperties;

    public SignTypeService(GlobalProperties globalProperties) {
        this.globalProperties = globalProperties;
    }

    public List<SignType> getAuthorizedSignTypes() {
        List<SignType> signTypes = globalProperties.getAuthorizedSignTypes();
        if(globalProperties.getDisableCertStorage() && (globalProperties.getOpenXPKIServerUrl() == null || globalProperties.getOpenXPKIServerUrl().isEmpty())) {
            signTypes.remove(SignType.certSign);
        }
        return signTypes;
    }

}
