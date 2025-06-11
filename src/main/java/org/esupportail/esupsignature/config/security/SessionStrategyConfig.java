package org.esupportail.esupsignature.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;

@Configuration
public class SessionStrategyConfig {

    @Bean
    public SessionRegistryImpl sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public RegisterSessionAuthenticationStrategy sessionAuthenticationStrategy(SessionRegistryImpl sessionRegistry) {
        return new RegisterSessionAuthenticationStrategy(sessionRegistry);
    }
}
