package org.esupportail.esupsignature.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;

@Configuration
@EnableJdbcHttpSession
public class SessionConfig {

    @Bean
    public QueryCustomizer tableNameCustomizer() {
        return new QueryCustomizer();
    }

}