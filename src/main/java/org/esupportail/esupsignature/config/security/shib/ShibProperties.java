package org.esupportail.esupsignature.config.security.shib;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.shib")
public class ShibProperties {


    private String principalRequestHeader;
    private String credentialsRequestHeader;

    public String getPrincipalRequestHeader() {
        return principalRequestHeader;
    }

    public void setPrincipalRequestHeader(String principalRequestHeader) {
        this.principalRequestHeader = principalRequestHeader;
    }

    public String getCredentialsRequestHeader() {
        return credentialsRequestHeader;
    }

    public void setCredentialsRequestHeader(String credentialsRequestHeader) {
        this.credentialsRequestHeader = credentialsRequestHeader;
    }
}
