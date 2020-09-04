package org.esupportail.esupsignature.service.sms;

import org.esupportail.esupsignature.exception.EsupSignatureException;

public interface SmsService {

    public String getName();
    public void sendSms(String phoneNumber, String message) throws EsupSignatureException;
}
