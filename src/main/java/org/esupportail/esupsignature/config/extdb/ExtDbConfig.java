package org.esupportail.esupsignature.config.extdb;

import org.springframework.context.annotation.Configuration;

@Configuration
public class ExtDbConfig {

    private ExtDbProperties extDbProperties;

    public ExtDbConfig(ExtDbProperties extDbProperties) {
        this.extDbProperties = extDbProperties;
    }

    public ExtDbProperties getExtDbProperties() {
        return extDbProperties;
    }

    public void setExtDbProperties(ExtDbProperties extDbProperties) {
        this.extDbProperties = extDbProperties;
    }
}
