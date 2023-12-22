package org.esupportail.esupsignature.dss.config;

import eu.europa.esig.dss.service.http.proxy.ProxyConfig;
import eu.europa.esig.dss.service.http.proxy.ProxyProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
@EnableConfigurationProperties(DSSProxyProperties.class)
public class ProxyConfiguration {

	private DSSProxyProperties dssProxyProperties;

	public ProxyConfiguration(DSSProxyProperties dssProxyProperties) {
		this.dssProxyProperties = dssProxyProperties;
	}

	@Bean
	public ProxyConfig proxyConfig() {
		if (!dssProxyProperties.isHttpEnabled() && !dssProxyProperties.isHttpsEnabled()) {
			return null;
		}
		ProxyConfig config = new ProxyConfig();
		if (dssProxyProperties.isHttpEnabled()) {
			ProxyProperties httpProperties = new ProxyProperties();
			httpProperties.setHost(dssProxyProperties.getHttpHost());
			httpProperties.setPort(dssProxyProperties.getHttpPort());
			httpProperties.setUser(dssProxyProperties.getHttpUser());
			httpProperties.setPassword(dssProxyProperties.getHttpPassword().toCharArray());
			httpProperties.setExcludedHosts(Collections.singleton(dssProxyProperties.getHttpExcludedHosts()));
			config.setHttpProperties(httpProperties);
		}
		if (dssProxyProperties.isHttpsEnabled()) {
			ProxyProperties httpsProperties = new ProxyProperties();
			httpsProperties.setHost(dssProxyProperties.getHttpsHost());
			httpsProperties.setPort(dssProxyProperties.getHttpsPort());
			httpsProperties.setUser(dssProxyProperties.getHttpsUser());
			httpsProperties.setPassword(dssProxyProperties.getHttpsPassword().toCharArray());
			httpsProperties.setExcludedHosts(Collections.singleton(dssProxyProperties.getHttpsExcludedHosts()));
			config.setHttpsProperties(httpsProperties);
		}
		return config;
	}

}
