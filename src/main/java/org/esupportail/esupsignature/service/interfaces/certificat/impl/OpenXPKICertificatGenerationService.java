package org.esupportail.esupsignature.service.interfaces.certificat.impl;

import eu.europa.esig.dss.token.Pkcs12SignatureToken;
import jakarta.annotation.Resource;
import org.apache.commons.text.RandomStringGenerator;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.interfaces.certificat.CertificatGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

@Service
@ConditionalOnProperty("global.open-x-p-k-i-server-url")
public class OpenXPKICertificatGenerationService implements CertificatGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(OpenXPKICertificatGenerationService.class);

    @Resource
    private GlobalProperties globalProperties;

    @Override
    public Pkcs12SignatureToken generateTokenForUser(User user) {
        try {
            logger.info("creating csr");
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair pair = kpg.generateKeyPair();
            X500Principal x500Principal = new X500Principal("emailAddress=" + user.getEmail() + ", CN=" + user.getFirstname() + " " + user.getName() + ", O=" + globalProperties.getDomain() + ", OU=UNIVROUEN, C=FR");
            PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(x500Principal, pair.getPublic());
            JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder("SHA256withRSA");
            ContentSigner signer = csBuilder.build(pair.getPrivate());
            PKCS10CertificationRequest csr = p10Builder.build(signer);
            StringWriter signedCertificatePEMDataStringWriter = new StringWriter();
            JcaPEMWriter pemWriter = new JcaPEMWriter(signedCertificatePEMDataStringWriter);
            pemWriter.writeObject(csr);
            pemWriter.close();
            String pcks10 = signedCertificatePEMDataStringWriter.toString();
            logger.info(pcks10);
            LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
            map.add("method", "RequestCertificate");
            map.add("pkcs10", pcks10);
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map, headers);
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            RestTemplate restTemplate = new RestTemplate();
            Root root = restTemplate.postForObject(globalProperties.getOpenXPKIServerUrl(), requestEntity, Root.class);
            assert root != null;
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate[] certificates = new Certificate[2];
            Certificate cert = cf.generateCertificate(new ByteArrayInputStream(root.result.data.certificate.getBytes()));
            Certificate chain = cf.generateCertificate(new ByteArrayInputStream(root.result.data.chain.getBytes()));
            certificates[0] = cert;
            certificates[1] = chain;
            KeyStore pkcs12 = KeyStore.getInstance("PKCS12");
            RandomStringGenerator pwdGenerator = new RandomStringGenerator.Builder().withinRange(33, 45)
                    .build();
            char[] password = pwdGenerator.generate(8, 12).toCharArray();
            pkcs12.load(null, null);
            pkcs12.setKeyEntry("temp", pair.getPrivate(), password, certificates);
            ByteArrayOutputStream p12 = new ByteArrayOutputStream();
            pkcs12.store(p12, password);
            return new Pkcs12SignatureToken(p12.toByteArray(), new KeyStore.PasswordProtection(password));
        } catch(NoSuchAlgorithmException | IOException | OperatorCreationException | CertificateException | KeyStoreException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public static class Data{
        public String cert_identifier;
        public String transaction_id;
        public String error_code;
        public String chain;
        public String certificate;
    }

    public static class Result{
        public int id;
        public String state;
        public int pid;
        public Data data;
        public String proc_state;
    }

    public static class Root{
        public Result result;
    }
}