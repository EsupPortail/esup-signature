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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;

@Service
public class SMSUSmsService implements SmsService {

    private static final Logger logger = LoggerFactory.getLogger(SMSUSmsService.class);

    @Resource
    SmsProperties smsProperties;

    @Override
    public String getName() {
        return "SMSU";
    }

    @Override
    public void sendSms(String phoneNumber, String message) throws EsupSignatureRuntimeException {
        BasicCredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(smsProperties.getUsername(), smsProperties.getPassword().toCharArray());
        provider.setCredentials(new AuthScope(null, null, -1, null, null), credentials);
        HttpClient client = HttpClientBuilder.create()
                .setDefaultCredentialsProvider(provider)
                .build();
        try {
            HttpResponse response = client.execute(new HttpGet(smsProperties.getUrl() + "?action=SendSms&phoneNumber=" + phoneNumber + "&message=" + URLEncoder.encode(message, Charset.defaultCharset())));
            int statusCode = response.getCode();
            if (statusCode != HttpStatus.SC_OK) {
                throw new EsupSignatureRuntimeException("Problème d'envoi sms");
            } else {
                logger.info("SMS envoyé : " + phoneNumber);
            }
        } catch (IOException e) {
            throw new EsupSignatureRuntimeException("Problème d'envoi sms : " + e.getMessage(), e);
        }

    }

    @Override
    public boolean testSms() throws IOException {
        BasicCredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(smsProperties.getUsername(), smsProperties.getPassword().toCharArray());
        provider.setCredentials(new AuthScope(null, null, -1, null, null), credentials);
        HttpClient client = HttpClientBuilder.create()
                .setDefaultCredentialsProvider(provider)
                .build();
        HttpResponse response = client.execute(new HttpGet(smsProperties.getUrl()));
        return response.getCode() == HttpStatus.SC_OK;
    }
}
