package org.esupportail.esupsignature.service.interfaces.sms;

import org.esupportail.esupsignature.exception.EsupSignatureException;

import java.io.IOException;

public interface SmsService {

    String getName();
    void sendSms(String phoneNumber, String message) throws EsupSignatureException;
    boolean testSms() throws IOException;
}
