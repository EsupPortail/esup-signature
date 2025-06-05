package org.esupportail.esupsignature.service.security;

import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.util.Map;

public interface OidcSecurityService extends SecurityService {
    JwtDecoder getJwtDecoder();
    Map<String, Object> getAdditionalAuthorizationParameters();
}