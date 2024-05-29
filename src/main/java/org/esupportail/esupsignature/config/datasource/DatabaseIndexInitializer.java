package org.esupportail.esupsignature.config.datasource;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseIndexInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseIndexInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseIndexInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        try {
            jdbcTemplate.execute("create unique index if not exists sign_book_team_pkey on sign_book_team (sign_book_id, team_id)");
        } catch (Exception e) {
            logger.warn(e.getMessage());
            logger.warn("si besoin voici une rêquete pour controller les éventuels doublons dans la table sign_book_team :\n" +
                    "SELECT sign_book_id, team_id, COUNT(*) as occurrence_count\n" +
                    "FROM sign_book_team\n" +
                    "GROUP BY sign_book_id, team_id\n" +
                    "HAVING COUNT(*) > 1;");
            logger.warn("Il s'agir certainement de demandes supprimées orphelines. Après vérification vous devrez nettoyer cette table des doublons.");
        }
        try {
            jdbcTemplate.execute("create unique index if not exists live_workflow_step_recipients_recipients_id_key on live_workflow_step_recipients (recipients_id)");
        } catch (Exception e) {
            logger.warn(e.getMessage());
        }
        try {
            jdbcTemplate.execute("create unique index if not exists live_workflow_current_step_id_key on live_workflow (current_step_id)");
        } catch (Exception e) {
            logger.warn(e.getMessage());
        }
        try {
            jdbcTemplate.execute("create unique index if not exists data_sign_book_id_key on data (sign_book_id)");
        } catch (Exception e) {
            logger.warn(e.getMessage());
        }
        try {
            jdbcTemplate.execute("create unique index if not exists sign_book_hided_by_pkey on sign_book_hided_by (hided_by_id, sign_book_id)");
        } catch (Exception e) {
            logger.warn(e.getMessage());
        }
    }

}
