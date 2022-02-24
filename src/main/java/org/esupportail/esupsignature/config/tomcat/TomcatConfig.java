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
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Configuration
@EnableConfigurationProperties({TomcatAjpProperties.class, GlobalProperties.class})
public class TomcatConfig {

    @Resource
    Environment environment;

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
    @Order(2)
    @ConditionalOnMissingBean(TomcatServletWebServerFactory.class)
    TomcatServletWebServerFactory tomcatFactory() {
        return new TomcatServletWebServerFactory() {

            @Override
            protected void postProcessContext(Context context) {
                context.setResources(new ExtractingRoot());
            }
        };
    }

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> servletContainerCustomizer() {
        boolean reloadable = List.of(environment.getActiveProfiles()).contains("dev");
        return container -> container.addContextCustomizers(
                cntxt -> cntxt.setReloadable(reloadable));
    }

}