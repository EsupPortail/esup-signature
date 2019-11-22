package org.esupportail.esupsignature.config.sign;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SignProperties.class)
public class SignConfig {

    private SignProperties signProperties;

    public SignConfig(SignProperties signProperties) {
        this.signProperties = signProperties;
    }

    public SignProperties getSignProperties() {
        return signProperties;
    }
}
