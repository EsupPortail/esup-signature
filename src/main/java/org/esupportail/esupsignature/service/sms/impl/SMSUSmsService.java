package org.esupportail.esupsignature.service.sms.impl;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.esupportail.esupsignature.config.sms.SmsProperties;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.service.DataService;
import org.esupportail.esupsignature.service.sms.SmsService;
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
    public void sendSms(String phoneNumber, String message) throws EsupSignatureException {
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(smsProperties.getUsername(), smsProperties.getPassword());
        provider.setCredentials(AuthScope.ANY, credentials);

        HttpClient client = HttpClientBuilder.create()
                .setDefaultCredentialsProvider(provider)
                .build();

        try {
            HttpResponse response = client.execute(new HttpGet(smsProperties.getUrl() + "/?action=SendSms&phoneNumber=" + phoneNumber + "&message=" + URLEncoder.encode(message, Charset.defaultCharset())));
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                throw new EsupSignatureException("Problème d'envoi sms");
            } else {
                logger.info("SMS envoyé : " + phoneNumber);
            }
        } catch (IOException e) {
            throw new EsupSignatureException("Problème d'envoi sms : " + e.getMessage(), e);
        }

    }
}
