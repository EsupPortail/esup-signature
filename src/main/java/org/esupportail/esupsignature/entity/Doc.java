package org.esupportail.esupsignature.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

@JsonIgnoreProperties
public class Doc {

    String title;
    String type;
    String token;
    String status;
    Date date;

    public Doc(@JsonProperty("title") String title, @JsonProperty("type") String type, @JsonProperty("token") String token, @JsonProperty("status") String status, @JsonProperty("date") Date date) {
        this.title = title;
        this.type = type;
        this.token = token;
        this.status = status;
        this.date = date;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String name) {
        this.title = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
