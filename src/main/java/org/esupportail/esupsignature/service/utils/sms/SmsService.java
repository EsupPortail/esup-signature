package org.esupportail.esupsignature.service.utils.sms;

import org.esupportail.esupsignature.exception.EsupSignatureException;

public interface SmsService {

    public String getName();
    public void sendSms(String phoneNumber, String message) throws EsupSignatureException;
}
