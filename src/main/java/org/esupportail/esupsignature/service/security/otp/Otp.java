package org.esupportail.esupsignature.service.security.otp;

import org.esupportail.esupsignature.entity.Data;

public class Otp {

    private String phoneNumber;
    private String email;
    private String password;
    private Long signRequestId;
    private Data createDate;
    private boolean smsSended = false;
    private int tries = 0;
    private boolean forceSms = false;

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public boolean isSmsSended() {
        return smsSended;
    }

    public void setSmsSended(boolean smsSended) {
        this.smsSended = smsSended;
    }

    public int getTries() {
        return tries;
    }

    public void setTries(int tries) {
        this.tries = tries;
    }

    public boolean isForceSms() {
        return forceSms;
    }

    public void setForceSms(boolean forceSms) {
        this.forceSms = forceSms;
    }
}
