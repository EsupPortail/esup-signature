package org.esupportail.esupsignature.config.springsession;

import java.io.File;
import java.nio.file.Files;
import java.sql.CallableStatement;
import java.sql.Connection;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

//@Service
public class SpringSessionInit {

	private static final Logger logger = LoggerFactory.getLogger(SpringSessionInit.class);

	@Resource
	DataSource dataSource;

	@PostConstruct
	public void initSchemaIfNeeded() {
		try {
			Connection connection = dataSource.getConnection();
			File schemaPostgres = new ClassPathResource("schema-postgresql.sql").getFile();
			String sqlSchema = Files.readString(schemaPostgres.toPath());
			logger.debug(String.format("SQL Script to init spring-session schema : %s", sqlSchema));
			connection.beginRequest();
			CallableStatement statement = connection.prepareCall(sqlSchema);
			statement.execute();
			connection.commit();
			connection.close();
		} catch(Exception e) {
			throw new RuntimeException("Erreur durant l'import du schéma pour spring-session", e);
		}
	}

}
