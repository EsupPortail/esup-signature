package org.esupportail.esupsignature.repository.custom;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SessionRepositoryCustom {

    private final JdbcTemplate jdbcTemplate;

    public SessionRepositoryCustom(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> findAllSessionIds() {
        return jdbcTemplate.queryForList("SELECT SESSION_ID FROM SPRING_SESSION", String.class);
    }
}