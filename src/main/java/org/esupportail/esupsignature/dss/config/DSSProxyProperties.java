package org.esupportail.esupsignature.dss.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="proxy")
public class DSSProxyProperties {

    private boolean httpEnabled;
    private String httpHost;
    private int httpPort;
    private String httpUser;
    private String httpPassword;
    private String httpExcludedHosts = "";
    private boolean httpsEnabled;
    private String httpsHost;
    private int httpsPort;
    private String httpsUser;
    private String httpsPassword;
    private String httpsExcludedHosts = "";

    public boolean isHttpEnabled() {
        return httpEnabled;
    }

    public void setHttpEnabled(boolean httpEnabled) {
        this.httpEnabled = httpEnabled;
    }

    public String getHttpHost() {
        return httpHost;
    }

    public void setHttpHost(String httpHost) {
        this.httpHost = httpHost;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public String getHttpUser() {
        return httpUser;
    }

    public void setHttpUser(String httpUser) {
        this.httpUser = httpUser;
    }

    public String getHttpPassword() {
        return httpPassword;
    }

    public void setHttpPassword(String httpPassword) {
        this.httpPassword = httpPassword;
    }

    public String getHttpExcludedHosts() {
        return httpExcludedHosts;
    }

    public void setHttpExcludedHosts(String httpExcludedHosts) {
        this.httpExcludedHosts = httpExcludedHosts;
    }

    public boolean isHttpsEnabled() {
        return httpsEnabled;
    }

    public void setHttpsEnabled(boolean httpsEnabled) {
        this.httpsEnabled = httpsEnabled;
    }

    public String getHttpsHost() {
        return httpsHost;
    }

    public void setHttpsHost(String httpsHost) {
        this.httpsHost = httpsHost;
    }

    public int getHttpsPort() {
        return httpsPort;
    }

    public void setHttpsPort(int httpsPort) {
        this.httpsPort = httpsPort;
    }

    public String getHttpsUser() {
        return httpsUser;
    }

    public void setHttpsUser(String httpsUser) {
        this.httpsUser = httpsUser;
    }

    public String getHttpsPassword() {
        return httpsPassword;
    }

    public void setHttpsPassword(String httpsPassword) {
        this.httpsPassword = httpsPassword;
    }

    public String getHttpsExcludedHosts() {
        return httpsExcludedHosts;
    }

    public void setHttpsExcludedHosts(String httpsExcludedHosts) {
        this.httpsExcludedHosts = httpsExcludedHosts;
    }
}
