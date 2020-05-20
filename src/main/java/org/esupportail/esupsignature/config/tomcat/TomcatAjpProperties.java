package org.esupportail.esupsignature.config.tomcat;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="tomcat.ajp")
public class TomcatAjpProperties {

    private Integer port;

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }
}
