//package org.esupportail.esupsignature.dss.web.config;
//
//import eu.europa.esig.dss.client.http.proxy.ProxyConfig;
//import eu.europa.esig.dss.client.http.proxy.ProxyProperties;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.context.properties.EnableConfigurationProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//@EnableConfigurationProperties(DSSProxyProperties.class)
//public class ProxyConfiguration {
//
//	private DSSProxyProperties dssProxyProperties;
//
//	public ProxyConfiguration(DSSProxyProperties dssProxyProperties) {
//		this.dssProxyProperties = dssProxyProperties;
//	}
//
//	@Bean
//	public ProxyConfig proxyConfig() {
//		if (!dssProxyProperties.isHttpEnabled() && !dssProxyProperties.isHttpsEnabled()) {
//			return null;
//		}
//		ProxyConfig config = new ProxyConfig();
//		if (dssProxyProperties.isHttpEnabled()) {
//			ProxyProperties httpProperties = new ProxyProperties();
//			httpProperties.setHost(dssProxyProperties.getHttpHost());
//			httpProperties.setPort(dssProxyProperties.getHttpPort());
//			httpProperties.setUser(dssProxyProperties.getHttpUser());
//			httpProperties.setPassword(dssProxyProperties.getHttpPassword());
//			httpProperties.setExcludedHosts(dssProxyProperties.getHttpExcludedHosts());
//			config.setHttpProperties(httpProperties);
//		}
//		if (dssProxyProperties.isHttpsEnabled()) {
//			ProxyProperties httpsProperties = new ProxyProperties();
//			httpsProperties.setHost(dssProxyProperties.getHttpsHost());
//			httpsProperties.setPort(dssProxyProperties.getHttpsPort());
//			httpsProperties.setUser(dssProxyProperties.getHttpsUser());
//			httpsProperties.setPassword(dssProxyProperties.getHttpsPassword());
//			httpsProperties.setExcludedHosts(dssProxyProperties.getHttpsExcludedHosts());
//			config.setHttpsProperties(httpsProperties);
//		}
//		return config;
//	}
//
//}
