package org.esupportail.esupsignature.config.datasource;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseIndexInitializer {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseIndexInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        jdbcTemplate.execute("create unique index if not exists sign_book_team_pkey on sign_book_team (sign_book_id, team_id)");
        jdbcTemplate.execute("create unique index if not exists live_workflow_step_recipients_recipients_id_key on live_workflow_step_recipients (recipients_id)");
        jdbcTemplate.execute("create unique index if not exists live_workflow_current_step_id_key on live_workflow (current_step_id)");
        jdbcTemplate.execute("create unique index if not exists data_sign_book_id_key on data (sign_book_id)");
        jdbcTemplate.execute("create unique index if not exists sign_book_hided_by_pkey on sign_book_hided_by (hided_by_id, sign_book_id)");
    }

}
