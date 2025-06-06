package org.esupportail.esupsignature.service.security;

import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.util.Map;

public interface OidcOtpSecurityService extends SecurityService {
    String getCode();
    JwtDecoder getJwtDecoder();
    Map<String, Object> getAdditionalAuthorizationParameters();
}