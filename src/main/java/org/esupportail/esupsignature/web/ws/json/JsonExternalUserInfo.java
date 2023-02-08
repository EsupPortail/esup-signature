package org.esupportail.esupsignature.web.ws.json;

public class JsonExternalUserInfo {

    private String email;
    private String name;
    private String firstname;
    private String phone;
    private String forcesms;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getForcesms() {
        return forcesms;
    }

    public void setForcesms(String forcesms) {
        this.forcesms = forcesms;
    }
}
