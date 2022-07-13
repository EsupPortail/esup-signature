package org.esupportail.esupsignature.config.datasource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.sql.DataSource;

@Configuration
@EnableJpaRepositories(basePackages = "org.esupportail.esupsignature.repository",
        excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.esupportail.esupsignature.repository.ldap.*")
        })
public class DataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }

}
