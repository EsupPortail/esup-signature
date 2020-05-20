package org.esupportail.esupsignature.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="security.ws")
public class WebSecurityProperties {

    private String[] wsAccessAuthorizeIps;
    public String[] getWsAccessAuthorizeIps() {
        return wsAccessAuthorizeIps;
    }
    public void setWsAccessAuthorizeIps(String[] wsAccessAuthorizeIps) { this.wsAccessAuthorizeIps = wsAccessAuthorizeIps; }

}
