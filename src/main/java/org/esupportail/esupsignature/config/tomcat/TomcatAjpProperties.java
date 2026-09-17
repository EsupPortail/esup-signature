package org.esupportail.esupsignature.config.tomcat;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="tomcat.ajp")
public class TomcatAjpProperties {

    private String address = "127.0.0.1";

    private Integer port;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }
}
