package org.esupportail.esupsignature.service.extdb;

import org.esupportail.esupsignature.config.extdb.ExtDbConfig;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.sql.DataSource;

@Service
public class ExtDbService {

    @Resource
    private ExtDbConfig extDbConfigs;

    public DataSource getDataSourceByName(String name) {
        for(DataSourceProperties dataSourceProperties : extDbConfigs.getExtDbProperties().getDataSources()) {
            if(dataSourceProperties.getName().equals(name)) {
                return dataSourceProperties.initializeDataSourceBuilder().build();
            }
        }
        return null;
    }

    public JdbcTemplate getJdbcTemplateByName(String name) {
        return new JdbcTemplate(getDataSourceByName(name));
    }

}
