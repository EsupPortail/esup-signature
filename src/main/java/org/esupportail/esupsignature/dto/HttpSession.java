package org.esupportail.esupsignature.dto;

import java.util.Date;

public class HttpSession {

    Date createdDate;

    Date lastRequest;

    String sessionId;

    String remoteIp;

    String originRequestUri;

    String userEppn;

    boolean expired;

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getLastRequest() {
        return lastRequest;
    }

    public void setLastRequest(Date lastRequest) {
        this.lastRequest = lastRequest;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getRemoteIp() {
        return remoteIp;
    }

    public void setRemoteIp(String remoteIp) {
        this.remoteIp = remoteIp;
    }

    public String getOriginRequestUri() {
        return originRequestUri;
    }

    public void setOriginRequestUri(String originRequestUri) {
        this.originRequestUri = originRequestUri;
    }

    public String getUserEppn() {
        return userEppn;
    }

    public void setUserEppn(String userEppn) {
        this.userEppn = userEppn;
    }

    public boolean isExpired() {
        return expired;
    }

    public void setExpired(boolean expired) {
        this.expired = expired;
    }
}
