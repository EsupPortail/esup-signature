package org.esupportail.esupsignature.config.extdb;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix="extdb")
public class ExtDbProperties {

    Map<String, DataSourceProperties> dataSources = new HashMap<>();

    public Map<String, DataSourceProperties> getDataSources() {
        return dataSources;
    }

}