package org.esupportail.esupsignature.service.interfaces.sms.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.esupportail.esupsignature.config.sms.SmsProperties;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.interfaces.sms.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

@Service
public class OVHSmsService implements SmsService {

    private static final Logger logger = LoggerFactory.getLogger(OVHSmsService.class);

    @Resource
    private SmsProperties smsProperties;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "OVH";
    }

    @Override
    public void sendSms(String phoneNumber, String message) throws EsupSignatureRuntimeException {
        String METHOD = "POST";
        try {
            String ServiceName = objectMapper.readValue(getSmsAccount(), String[].class)[0];
            URL    QUERY  = new URL("https://eu.api.ovh.com/1.0/sms/"+ServiceName+"/jobs");
            String BODY   = "{\"receivers\":[\"+33" + phoneNumber.substring(1, 10) + "\"],\"message\":\""+ message + "\",\"priority\":\"high\",\"senderForResponse\":true}";
            StringBuffer response = getStringBuffer(METHOD, QUERY, BODY,  true);
            logger.info("sms sended : " + response.toString());
        } catch (IOException e) {
            throw new EsupSignatureRuntimeException(e.getMessage(), e);
        }

    }

    @Override
    public boolean testSms() throws IOException {
        return objectMapper.readValue(getSmsAccount(), String[].class).length > 0;
    }

    private String getSmsAccount() throws IOException {
        String METHOD = "GET";
        URL    QUERY  = new URL("https://eu.api.ovh.com/1.0/sms/");
        StringBuffer response = getStringBuffer(METHOD, QUERY, "", false);
        return  response.toString();
    }

    private StringBuffer getStringBuffer(String METHOD, URL QUERY, String BODY, boolean sendBody) throws IOException {
        long TSTAMP  = new Date().getTime()/1000;
        String AK = smsProperties.getApiKey();
        String AS = smsProperties.getApiSecret();
        String CK = smsProperties.getConsumerKey();
        String toSign    = AS + "+" + CK + "+" + METHOD + "+" + QUERY + "+" + BODY + "+" + TSTAMP;
        String signature = "$1$" + HashSHA1(toSign);
        HttpURLConnection req = (HttpURLConnection) QUERY.openConnection();
        req.setRequestMethod(METHOD);
        req.setRequestProperty("Content-Type",      "application/json");
        req.setRequestProperty("X-Ovh-Application", AK);
        req.setRequestProperty("X-Ovh-Consumer", CK);
        req.setRequestProperty("X-Ovh-Signature", signature);
        req.setRequestProperty("X-Ovh-Timestamp",   "" + TSTAMP);
        if(sendBody) {
            req.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(req.getOutputStream());
            wr.writeBytes(BODY);
            wr.flush();
            wr.close();
        }
        String inputLine;
        BufferedReader in;
        int responseCode = req.getResponseCode();
        if ( responseCode == 200 ) {
            in = new BufferedReader(new InputStreamReader(req.getInputStream()));
        } else {
            in = new BufferedReader(new InputStreamReader(req.getErrorStream()));
        }
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        return response;
    }


    public static String HashSHA1(String text)
    {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
            byte[] sha1hash = new byte[40];
            md.update(text.getBytes("iso-8859-1"), 0, text.length());
            sha1hash = md.digest();
            return convertToHex(sha1hash);
        } catch (NoSuchAlgorithmException e) {
            final String errmsg = "NoSuchAlgorithmException: " + text + " " + e;
            return errmsg;
        } catch (UnsupportedEncodingException e) {
            final String errmsg = "UnsupportedEncodingException: " + text + " " + e;
            return errmsg;
        }
    }

    private static String convertToHex(byte[] data)
    {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9))
                    buf.append((char) ('0' + halfbyte));
                else
                    buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while(two_halfs++ < 1);
        }
        return buf.toString();
    }
}
