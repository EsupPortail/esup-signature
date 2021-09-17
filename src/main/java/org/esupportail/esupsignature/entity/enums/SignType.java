package org.esupportail.esupsignature.entity.enums;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public enum SignType {
    hiddenVisa, visa, pdfImageStamp, certSign, nexuSign;

    private GlobalProperties globalProperties;

    public void setGlobalProperties(GlobalProperties globalProperties) {
        this.globalProperties = globalProperties;
    }

    @Component
    public static class ReportTypeServiceInjector {

        @Resource
        private GlobalProperties globalProperties;

        @PostConstruct
        public void postConstruct() {
            for (SignType rt : EnumSet.allOf(SignType.class))
                rt.setGlobalProperties(globalProperties);
        }
    }

    public static List<SignType> getAuthorisedValues() {
        List<SignType> signTypes = new ArrayList<>();
        for(SignType signType : values()) {
            if(!signType.globalProperties.getDisableCertStorage() || !signType.name().equals("certSign")) {
                signTypes.add(signType);
            }
        }
        return signTypes;
    }

}
