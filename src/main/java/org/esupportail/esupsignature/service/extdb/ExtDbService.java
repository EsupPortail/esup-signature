package org.esupportail.esupsignature.service.extdb;

import org.esupportail.esupsignature.config.extdb.ExtDbConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import javax.sql.DataSource;

@Service
public class ExtDbService {

    private static final Logger logger = LoggerFactory.getLogger(ExtDbService.class);

    @Resource
    private ExtDbConfig extDbConfigs;

    public DataSource getDataSourceByName(String name) {
        logger.info("initialize db " + name + " with type " + extDbConfigs.getExtDbProperties().getDataSources().get(name).getType());
        return extDbConfigs.getExtDbProperties().getDataSources().get(name).initializeDataSourceBuilder().build();
    }

    public JdbcTemplate getJdbcTemplateByName(String name) {
        return new JdbcTemplate(getDataSourceByName(name));
    }

}
