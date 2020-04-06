package org.esupportail.esupsignature.config.tomcat;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="tomcat.ajp")
public class TomcatAjpProperties {

    private int port;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
