package org.esupportail.esupsignature.config.tomcat;

import jakarta.annotation.Resource;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.webresources.ExtractingRoot;
import org.apache.coyote.ajp.AbstractAjpProtocol;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Configuration
public class TomcatConfig {

    private static final Logger logger = LoggerFactory.getLogger(TomcatConfig.class);

    @Resource
    Environment environment;

    private final TomcatAjpProperties tomcatAjpProperties;

    private final GlobalProperties globalProperties;

    public TomcatConfig(TomcatAjpProperties tomcatAjpProperties, GlobalProperties globalProperties) {
        this.tomcatAjpProperties = tomcatAjpProperties;
        this.globalProperties = globalProperties;
    }

    @Bean
    @ConditionalOnProperty(prefix = "tomcat.ajp", name = "port")
    public TomcatServletWebServerFactory servletContainer() throws URISyntaxException {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
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
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> servletContainerCustomizer() {
        final boolean reloadable = List.of(environment.getActiveProfiles()).contains("dev");
        logger.info("SetReloadable to " + reloadable);
        return new WebServerFactoryCustomizer<TomcatServletWebServerFactory>() {
            @Override
            public void customize(TomcatServletWebServerFactory container) {
                container.addContextCustomizers(
                        cntxt -> {
                            cntxt.setReloadable(reloadable);
                            cntxt.setResources(new ExtractingRoot());
                        }
                );
            }
        };
    }

}