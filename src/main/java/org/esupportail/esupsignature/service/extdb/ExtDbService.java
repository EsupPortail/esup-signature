package org.esupportail.esupsignature.service.extdb;

import jakarta.annotation.Resource;
import org.esupportail.esupsignature.config.extdb.ExtDbConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

@Service
public class ExtDbService {

    private static final Logger logger = LoggerFactory.getLogger(ExtDbService.class);

    @Resource
    private ExtDbConfig extDbConfigs;

    public DataSource getDataSourceByName(String name) {
        DataSourceProperties dataSourceProperties = extDbConfigs.getExtDbProperties().getDataSources().get(name);
        logger.info("initialize db " + name + " with driver " + extDbConfigs.getExtDbProperties().getDataSources().get(name).getDriverClassName());
        return dataSourceProperties.initializeDataSourceBuilder().build();
    }

    public JdbcTemplate getJdbcTemplateByName(String name) {
        return new JdbcTemplate(getDataSourceByName(name));
    }

}
