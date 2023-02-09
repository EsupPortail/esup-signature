package org.esupportail.esupsignature.service.interfaces.sms;

import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;

import java.io.IOException;

public interface SmsService {

    String getName();
    void sendSms(String phoneNumber, String message) throws EsupSignatureRuntimeException;
    boolean testSms() throws IOException;
}
