package org.esupportail.esupsignature.config.security.cas;

import org.esupportail.esupsignature.config.security.WebSecurityProperties;
import org.esupportail.esupsignature.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@ConditionalOnProperty(name = "spring.security.oauth2.client.provider.cas.issuer-uri")
public class CasJwtDecoder implements JwtDecoder {

    @Value("${spring.security.oauth2.client.provider.cas.issuer-uri:}")
    private String issuerUri;

    private final WebSecurityProperties webSecurityProperties;
    private final UserService userService;

    public CasJwtDecoder(WebSecurityProperties webSecurityProperties, UserService userService) {
        this.webSecurityProperties = webSecurityProperties;
        this.userService = userService;
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                throw new JwtException("Format du JWT invalide");
            }
            Jwt jwt = JwtDecoders.fromOidcIssuerLocation(issuerUri).decode(token);
            if(Collections.disjoint(jwt.getAudience(), webSecurityProperties.getJwtWsAuthorizedAudiences())) {
                throw new JwtException("Audience du JWT non autorisée");
            }
            userService.createUserWithEppn(jwt.getClaimAsString("eduPersonPrincipalName"));
            return jwt;

        } catch (Exception e) {
            throw new JwtException("Impossible de décoder le JWT : " + e.getMessage());
        }
    }

}