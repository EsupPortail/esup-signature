package org.esupportail.esupsignature.repository.custom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SessionRepositoryCustom {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<String> findAllSessionIds() {
        return jdbcTemplate.queryForList("SELECT SESSION_ID FROM SPRING_SESSION", String.class);
    }
}