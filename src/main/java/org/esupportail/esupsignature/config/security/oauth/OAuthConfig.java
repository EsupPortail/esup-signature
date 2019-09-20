package org.esupportail.esupsignature.config.security.oauth;

import org.esupportail.esupsignature.security.oauth.OAuthSecurityServiceImpl;
import org.springframework.boot.autoconfigure.security.oauth2.client.ClientsConfiguredCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
@Conditional(ClientsConfiguredCondition.class)
public class OAuthConfig {

    @Bean
    public OAuthSecurityServiceImpl oAuthSecurityService() {
        return new OAuthSecurityServiceImpl();
    }
}
