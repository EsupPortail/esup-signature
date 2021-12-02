package org.esupportail.esupsignature.config.security.shib;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "security.shib", name = "principal-request-header")
@EnableConfigurationProperties(ShibProperties.class)
public class ShibConfig {


}
