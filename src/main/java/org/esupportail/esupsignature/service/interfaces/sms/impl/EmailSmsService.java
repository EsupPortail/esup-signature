package org.esupportail.esupsignature.service.interfaces.sms.impl;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.esupportail.esupsignature.config.sms.SmsProperties;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.interfaces.sms.SmsService;
import org.esupportail.esupsignature.service.mail.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Service
public class EmailSmsService implements SmsService {

    private static final Logger logger = LoggerFactory.getLogger(EmailSmsService.class);

    private final MailService mailService;

    public EmailSmsService(MailService mailService) {
        this.mailService = mailService;
    }

    @Override
    public String getName() {
        return "EMAIL";
    }

    @Override
    public void sendSms(String userEmail, String phoneNumber, String message) throws EsupSignatureRuntimeException {
        try {
            mailService.sendMailCode(userEmail, message);
        } catch (Exception e) {
            logger.error("Failed to send code via email for phone number: {}", phoneNumber, e);
            throw new EsupSignatureRuntimeException("Failed to send code via email", e);
        }
    }

    @Override
    public boolean testSms() {
        return mailService != null;
    }
}
