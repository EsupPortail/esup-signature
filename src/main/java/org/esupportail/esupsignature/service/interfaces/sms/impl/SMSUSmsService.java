package org.esupportail.esupsignature.service.interfaces.sms.impl;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.esupportail.esupsignature.config.sms.SmsProperties;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.interfaces.sms.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SMSUSmsService implements SmsService {

    private static final Logger logger = LoggerFactory.getLogger(SMSUSmsService.class);

    private final SmsProperties smsProperties;

    public SMSUSmsService(SmsProperties smsProperties) {
        this.smsProperties = smsProperties;
    }


    @Override
    public String getName() {
        return "SMSU";
    }

    @Override
    public void sendSms(String phoneNumber, String message) throws EsupSignatureRuntimeException {
        BasicCredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(smsProperties.getUsername(), smsProperties.getPassword().toCharArray());
        provider.setCredentials(new AuthScope(null, null, -1, null, null), credentials);
        CloseableHttpClient client = HttpClients.custom()
                .setDefaultCredentialsProvider(provider)
                .build();

        HttpPost post = new HttpPost(smsProperties.getUrl() + "?action=SendSms");
        Map<String, String> postParams = new HashMap<>();
        postParams.put("phoneNumber", phoneNumber);
        postParams.put("message", message);
        String postBody = postParams.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" +
                        URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        post.setEntity(new StringEntity(postBody, ContentType.APPLICATION_FORM_URLENCODED));
        try {
            HttpResponse response = client.execute(post);
            int statusCode = response.getCode();
            client.close();
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
