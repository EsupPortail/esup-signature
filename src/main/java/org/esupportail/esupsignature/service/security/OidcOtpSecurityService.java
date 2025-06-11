package org.esupportail.esupsignature.service.security;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;

import java.util.Map;

public interface OidcOtpSecurityService extends SecurityService {
    String getCode();
    SignatureAlgorithm getSignatureAlgorithm();
    Map<String, Object> getAdditionalAuthorizationParameters();
}