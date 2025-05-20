package org.esupportail.esupsignature.config.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableJdbcHttpSession
public class SessionConfig implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(SessionConfig.class);


    private final JdbcTemplate jdbcTemplate;

    public SessionConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Bean
    public QueryCustomizer tableNameCustomizer() {
        return new QueryCustomizer();
    }

    @Override
    public void afterPropertiesSet() {
        try {
            String schemaSql = StreamUtils.copyToString(
                    new ClassPathResource("org/springframework/session/jdbc/schema-postgresql.sql").getInputStream(),
                    StandardCharsets.UTF_8
            );
            jdbcTemplate.execute(schemaSql);
            logger.info("Spring Session JDBC schema initialized successfully.");
        } catch (Exception e) {
            logger.info("Spring Session JDBC schema already exists, skipping initialization.");
        }
    }

}