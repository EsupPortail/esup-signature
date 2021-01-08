package org.esupportail.esupsignature.config.springsession;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.session.SaveMode;
import org.springframework.session.jdbc.config.annotation.SpringSessionDataSource;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;

import javax.sql.DataSource;

@Configuration
@EnableJdbcHttpSession(saveMode = SaveMode.ON_SET_ATTRIBUTE)
public class SpringSessionConfig {

    @Bean("springSessionDataSource")
    @SpringSessionDataSource
    public DataSource sessionDataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("sessions")
                .addScript("org/springframework/session/jdbc/schema-h2.sql").build();
    }

}