package org.esupportail.esupsignature.service.extdb;

import org.esupportail.esupsignature.config.extdb.ExtDbConfig;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.sql.DataSource;

@Service
public class ExtDbService {

    @Resource
    private ExtDbConfig extDbConfigs;

    public DataSource getDataSourceByName(String name) {
            return extDbConfigs.getExtDbProperties().getDataSources().get(name).initializeDataSourceBuilder().build();
    }

    public JdbcTemplate getJdbcTemplateByName(String name) {
        return new JdbcTemplate(getDataSourceByName(name));
    }

}
