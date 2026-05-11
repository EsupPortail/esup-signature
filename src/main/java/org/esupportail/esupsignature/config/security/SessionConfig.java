package org.esupportail.esupsignature.config.security;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;

import javax.sql.DataSource;

@Configuration
@EnableJdbcHttpSession
public class SessionConfig implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(SessionConfig.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public SessionConfig(@Qualifier("dataSource") DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Bean
    public QueryCustomizer tableNameCustomizer() {
        return new QueryCustomizer();
    }

    @Override
    public void afterPropertiesSet() {
        if (springSessionSchemaExists()) {
            logger.info("Spring Session JDBC schema already exists, skipping initialization.");
            return;
        }

        ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator(
                new ClassPathResource("org/springframework/session/jdbc/schema-postgresql.sql")
        );
        databasePopulator.setContinueOnError(false);
        databasePopulator.setIgnoreFailedDrops(true);

        try {
            DatabasePopulatorUtils.execute(databasePopulator, dataSource);
            logger.info("Spring Session JDBC schema initialized successfully.");
        } catch (Exception e) {
            throw new IllegalStateException("Impossible d'initialiser le schéma Spring Session JDBC.", e);
        }
    }

    private boolean springSessionSchemaExists() {
        Boolean exists = jdbcTemplate.queryForObject(
                "select to_regclass('public.spring_session') is not null and to_regclass('public.spring_session_attributes') is not null",
                Boolean.class
        );
        return Boolean.TRUE.equals(exists);
    }

}

