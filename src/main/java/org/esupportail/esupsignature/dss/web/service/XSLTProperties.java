package org.esupportail.esupsignature.dss.web.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="xslt")
public class XSLTProperties {

    private String rootTrustmarkUrlInTlBrowser;
    private String rootCountryUrlInTlBrowser;

    public String getRootTrustmarkUrlInTlBrowser() {
        return rootTrustmarkUrlInTlBrowser;
    }

    public void setRootTrustmarkUrlInTlBrowser(String rootTrustmarkUrlInTlBrowser) {
        this.rootTrustmarkUrlInTlBrowser = rootTrustmarkUrlInTlBrowser;
    }

    public String getRootCountryUrlInTlBrowser() {
        return rootCountryUrlInTlBrowser;
    }

    public void setRootCountryUrlInTlBrowser(String rootCountryUrlInTlBrowser) {
        this.rootCountryUrlInTlBrowser = rootCountryUrlInTlBrowser;
    }
}
