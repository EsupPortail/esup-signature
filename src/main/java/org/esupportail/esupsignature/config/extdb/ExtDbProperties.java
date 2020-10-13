package org.esupportail.esupsignature.config.extdb;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="extdb")
public class ExtDbProperties {

    DataSourceProperties[] dataSources;

    public DataSourceProperties[] getDataSources() {
        return dataSources;
    }

    public void setDataSources(DataSourceProperties[] dataSources) {
        this.dataSources = dataSources;
    }

}