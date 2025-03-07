package org.esupportail.esupsignature.config.sms;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sms")
public class SmsProperties {

    private boolean enableSms;
    /**
     * Nom du service de SMS (SMSU, OVH)
     */
    private String serviceName;
    /**
     * URL du service
     */
    private String url;
    /**
     * Login
     */
    private String username;
    /**
     * Mot de passe
     */
    private String password;
    /**
     * OVH : clé API (X-Ovh-Application)
     */
    private String apiKey;
    /**
     * OVH : mot de passe API pour le calcul de la signature (X-Ovh-Signature)
     */
    private String apiSecret;
    /**
     * OVH : clé client (X-Ovh-Consumer)
     */
    private String consumerKey;
    /**
     * OVH : true permet de mettre un nom alpha numérique en sender au lieu d’un numéro court
     */
    private boolean senderForResponse = true;
    /**
     * OVH : true désactive la possibilité de répondre au SMS
     */
    private boolean noStopClause = false;

    public Boolean getEnableSms() {
        return enableSms;
    }

    public void setEnableSms(Boolean enableSms) {
        this.enableSms = enableSms;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiSecret() {
        return password;
    }

    public void setApiSecret(String apiSecret) {
        this.password = apiSecret;
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    public boolean getSenderForResponse() {
        return senderForResponse;
    }

    public void setSenderForResponse(boolean senderForResponse) {
        this.senderForResponse = senderForResponse;
    }

    public boolean isNoStopClause() {
        return noStopClause;
    }

    public void setNoStopClause(boolean noStopClause) {
        this.noStopClause = noStopClause;
    }
}
