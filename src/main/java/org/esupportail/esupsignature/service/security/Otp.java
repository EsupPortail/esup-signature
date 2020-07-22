package org.esupportail.esupsignature.service.security;

import org.esupportail.esupsignature.entity.Data;

public class Otp {

    private String phoneNumber;
    private String password;
    private Long signRequestId;
    private Data createDate;
    private int tries = 0;

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getSignRequestId() {
        return signRequestId;
    }

    public void setSignRequestId(Long signRequestId) {
        this.signRequestId = signRequestId;
    }

    public Data getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Data createDate) {
        this.createDate = createDate;
    }

    public int getTries() {
        return tries;
    }

    public void setTries(int tries) {
        this.tries = tries;
    }
}
