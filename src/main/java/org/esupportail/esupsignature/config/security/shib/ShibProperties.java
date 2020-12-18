package org.esupportail.esupsignature.config.security.shib;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.shib")
public class ShibProperties {

    private String title;
    private String idpUrl;
    private String principalRequestHeader;
    private String credentialsRequestHeader;
    private String domainsWhiteListUrl;

	/**
	 * Properties to set for development/test purposes .
	 */
    private DevShibProperties dev;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIdpUrl() {
        return idpUrl;
    }

    public void setIdpUrl(String idpUrl) {
        this.idpUrl = idpUrl;
    }

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

    public String getDomainsWhiteListUrl() {
        return domainsWhiteListUrl;
    }

    public void setDomainsWhiteListUrl(String domainsWhiteListUrl) {
        this.domainsWhiteListUrl = domainsWhiteListUrl;
    }

	public DevShibProperties getDev() {
		return dev;
	}

	public void setDev(DevShibProperties dev) {
		this.dev = dev;
	}
  
}
