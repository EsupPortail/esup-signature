package org.esupportail.esupsignature.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SchemaFix implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(SchemaFix.class);

    private final JdbcTemplate jdbcTemplate;

    public SchemaFix(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("ALTER TABLE user_account DROP CONSTRAINT IF EXISTS user_account_user_type_check");
            jdbcTemplate.execute(
                "ALTER TABLE user_account ADD CONSTRAINT user_account_user_type_check " +
                "CHECK (user_type IN ('ldap', 'shib', 'external', 'system', 'group', 'azuread'))"
            );
            logger.info("SchemaFix: contrainte user_account_user_type_check mise à jour avec succès (azuread inclus)");
        } catch (Exception e) {
            logger.warn("SchemaFix: impossible de mettre à jour la contrainte user_type: {}", e.getMessage());
        }
    }
}
