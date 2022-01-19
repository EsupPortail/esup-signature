package org.esupportail.esupsignature.service.interfaces.certificat.impl;

import eu.europa.esig.dss.token.Pkcs12SignatureToken;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.interfaces.certificat.CertificatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.ArrayList;
import java.util.List;

@Service
public class OpenXPKICertificatService implements CertificatService {

    private static final Logger logger = LoggerFactory.getLogger(OpenXPKICertificatService.class);

    @Override
    public Pkcs12SignatureToken generateTokenForUser(User user) {
        try {
            logger.info("creating csr");
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair pair = kpg.generateKeyPair();
            PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(
                    new X500Principal("CN=Requested Test Certificate"), pair.getPublic());
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
            Root root = restTemplate.postForObject("http://192.168.0.25/rpc/enroll", requestEntity, Root.class);
            assert root != null;
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            List<Certificate> certificates = new ArrayList<>();
            Certificate cert = cf.generateCertificate(new ByteArrayInputStream(root.result.data.certificate.getBytes()));
            Certificate chain = cf.generateCertificate(new ByteArrayInputStream(root.result.data.chain.getBytes()));
            certificates.add(cert);
//            certificates.add(chain);
            KeyStore pkcs12 = KeyStore.getInstance("PKCS12");
            pkcs12.load(null, null);
            pkcs12.setKeyEntry("privatekeyalias", pair.getPrivate(), "entrypassphrase".toCharArray(), certificates.toArray(new Certificate[0]));
            ByteArrayOutputStream p12 = new ByteArrayOutputStream();
            pkcs12.store(p12, "toto".toCharArray());
            return new Pkcs12SignatureToken(p12.toByteArray(), new KeyStore.PasswordProtection("toto".toCharArray()));
        } catch(NoSuchAlgorithmException | IOException | OperatorCreationException e) {
            logger.error(e.getMessage(), e);
        } catch(CertificateException e) {
            e.printStackTrace();
        } catch(KeyStoreException e) {
            e.printStackTrace();
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