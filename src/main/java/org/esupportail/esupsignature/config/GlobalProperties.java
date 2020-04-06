package org.esupportail.esupsignature.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="global")
public class GlobalProperties {

    private String rootUrl;
    private String nexuUrl;
    private String nexuVersion;
    private String nexuDownloadUrl;

    public String getRootUrl() {
        return rootUrl;
    }

    public void setRootUrl(String rootUrl) {
        this.rootUrl = rootUrl;
    }

    public String getNexuUrl() {
        return nexuUrl;
    }

    public void setNexuUrl(String nexuUrl) {
        this.nexuUrl = nexuUrl;
    }

    public String getNexuVersion() {
        return nexuVersion;
    }

    public void setNexuVersion(String nexuVersion) {
        this.nexuVersion = nexuVersion;
    }

    public String getNexuDownloadUrl() {
        return nexuDownloadUrl;
    }

    public void setNexuDownloadUrl(String nexuDownloadUrl) {
        this.nexuDownloadUrl = nexuDownloadUrl;
    }
}
