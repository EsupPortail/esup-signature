package org.esupportail.esupsignature.config.tomcat;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.ajp.AbstractAjpProtocol;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TomcatAjpProperties.class)
@ConditionalOnProperty(prefix = "tomcat.ajp", name = "port")
public class TomcatAjpConfig {

    private TomcatAjpProperties tomcatAjpProperties;

    public TomcatAjpConfig(TomcatAjpProperties tomcatAjpProperties) {
        this.tomcatAjpProperties = tomcatAjpProperties;
    }

    @Bean
    public TomcatServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        Connector ajpConnector = new Connector("AJP/1.3");
        ajpConnector.setPort(tomcatAjpProperties.getPort());
        ajpConnector.setSecure(true);
        ajpConnector.setAllowTrace(false);
        ajpConnector.setScheme("https");
        ajpConnector.setAsyncTimeout(1200000);
        ajpConnector.setURIEncoding("UTF-8");
        ((AbstractAjpProtocol) ajpConnector.getProtocolHandler()).setSecretRequired(false);
        // TODO : Only If Shib ?
        ((AbstractAjpProtocol) ajpConnector.getProtocolHandler()).setTomcatAuthentication(false);
        // Avoid java.lang.IllegalStateException: More than the maximum allowed number of headers, [100], were detected.
        // (exception occures with shib)
        ((AbstractAjpProtocol) ajpConnector.getProtocolHandler()).setMaxHeaderCount(400);
        tomcat.addAdditionalTomcatConnectors(ajpConnector);
        return tomcat;
    }

}