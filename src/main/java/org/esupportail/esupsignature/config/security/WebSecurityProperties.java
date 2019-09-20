package org.esupportail.esupsignature.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="security")
public class WebSecurityProperties {

    private String[] nfcWsAccessAuthorizeIps;
    public String[] getNfcWsAccessAuthorizeIps() {
        return nfcWsAccessAuthorizeIps;
    }
    public void setNfcWsAccessAuthorizeIps(String[] nfcWsAccessAuthorizeIps) { this.nfcWsAccessAuthorizeIps = nfcWsAccessAuthorizeIps; }

}
