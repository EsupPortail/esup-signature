package org.esupportail.esupsignature.config.security;

import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;

public class QueryCustomizer
        implements SessionRepositoryCustomizer<JdbcIndexedSessionRepository> {

    private static final String CREATE_SESSION_ATTRIBUTE_QUERY = """
            INSERT INTO %TABLE_NAME%_ATTRIBUTES (SESSION_PRIMARY_ID, ATTRIBUTE_NAME, ATTRIBUTE_BYTES) 
            VALUES (?, ?, ?)
            ON CONFLICT (SESSION_PRIMARY_ID, ATTRIBUTE_NAME)
            DO NOTHING
            """;

    private static final String UPDATE_SESSION_ATTRIBUTE_QUERY = """
		UPDATE %TABLE_NAME%_ATTRIBUTES
		SET ATTRIBUTE_BYTES = ?
		WHERE SESSION_PRIMARY_ID = ?
		AND ATTRIBUTE_NAME = ?
		""";

    @Override
    public void customize(JdbcIndexedSessionRepository sessionRepository) {
        sessionRepository.setCreateSessionAttributeQuery(CREATE_SESSION_ATTRIBUTE_QUERY);
        sessionRepository.setUpdateSessionAttributeQuery(UPDATE_SESSION_ATTRIBUTE_QUERY);
    }

}
