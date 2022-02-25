package org.esupportail.esupsignature.config.tomcat;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.webresources.ExtractingRoot;
import org.apache.coyote.ajp.AbstractAjpProtocol;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.net.URI;
import java.net.URISyntaxException;

@Configuration
@EnableConfigurationProperties({TomcatAjpProperties.class, GlobalProperties.class})
public class TomcatConfig {

    private final TomcatAjpProperties tomcatAjpProperties;

    private final GlobalProperties globalProperties;

    public TomcatConfig(TomcatAjpProperties tomcatAjpProperties, GlobalProperties globalProperties) {
        this.tomcatAjpProperties = tomcatAjpProperties;
        this.globalProperties = globalProperties;
    }

    @Bean
    @Order(1)
    @ConditionalOnProperty(prefix = "tomcat.ajp", name = "port")
    public TomcatServletWebServerFactory servletContainer() throws URISyntaxException {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                context.setResources(new ExtractingRoot());
                context.setReloadable(false);
            }
        };
        tomcat.addContextCustomizers(cntxt -> cntxt.setReloadable(false));
        Connector ajpConnector = new Connector("AJP/1.3");
        ajpConnector.setPort(tomcatAjpProperties.getPort());
        ajpConnector.setAllowTrace(false);
        ajpConnector.setScheme(new URI(globalProperties.getRootUrl()).getScheme());
        if("https".equals(ajpConnector.getScheme())) {
            ajpConnector.setSecure(true);
        }
        ajpConnector.setAsyncTimeout(1200000);
        ajpConnector.setURIEncoding("UTF-8");
        ((AbstractAjpProtocol<?>) ajpConnector.getProtocolHandler()).setSecretRequired(false);
        ((AbstractAjpProtocol<?>) ajpConnector.getProtocolHandler()).setTomcatAuthentication(false);
        ((AbstractAjpProtocol<?>) ajpConnector.getProtocolHandler()).setMaxHeaderCount(400);
        tomcat.addAdditionalTomcatConnectors(ajpConnector);
        return tomcat;
    }

    @Bean
    @Order(2)
    @ConditionalOnMissingBean(TomcatServletWebServerFactory.class)
    public TomcatServletWebServerFactory tomcatFactory() {
        return new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                context.setResources(new ExtractingRoot());
                context.setReloadable(false);
            }
        };
    }

}