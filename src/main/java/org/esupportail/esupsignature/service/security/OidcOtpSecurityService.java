package org.esupportail.esupsignature.service.security;

import org.esupportail.esupsignature.entity.enums.ExternalAuth;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;

import java.util.Map;

public interface OidcOtpSecurityService extends SecurityService {
    String getLogoutUrl();
    ExternalAuth getExternalAuth();
    SignatureAlgorithm getSignatureAlgorithm();
    Map<String, Object> getAdditionalAuthorizationParameters();
}