package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class SignTypeService {

    @Resource
    private GlobalProperties globalProperties;

    public List<SignType> getAuthorizedSignTypes() {
        List<SignType> signTypes = new ArrayList<>();
        for(SignType signType : SignType.values()) {
            if(!globalProperties.getDisableCertStorage() || !signType.equals(SignType.certSign)) {
                signTypes.add(signType);
            }
        }
        return signTypes;
    }
}
