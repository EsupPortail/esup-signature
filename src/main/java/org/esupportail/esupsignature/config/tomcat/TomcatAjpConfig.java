package org.esupportail.esupsignature.config.tomcat;

import org.apache.catalina.connector.Connector;
import org.esupportail.esupsignature.config.security.WebSecurityProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TomcatAjpProperties.class)
public class TomcatAjpConfig {

    private TomcatAjpProperties tomcatAjpProperties;

    public TomcatAjpConfig(TomcatAjpProperties tomcatAjpProperties) {
        this.tomcatAjpProperties = tomcatAjpProperties;
    }

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> servletContainer() {
        return server -> {
            if (server instanceof TomcatServletWebServerFactory) {
                ((TomcatServletWebServerFactory) server).addAdditionalTomcatConnectors(redirectConnector());
            }
        };
    }

    private Connector redirectConnector() {
        Connector ajpConnector = new Connector("AJP/1.3");
        ajpConnector.setPort(tomcatAjpProperties.getPort());
        ajpConnector.setSecure(false);
        ajpConnector.setAllowTrace(false);
        ajpConnector.setScheme("http");
        return ajpConnector;
    }
}