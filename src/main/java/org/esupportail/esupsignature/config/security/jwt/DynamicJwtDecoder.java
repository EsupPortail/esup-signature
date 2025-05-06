package org.esupportail.esupsignature.config.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.util.Base64URL;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DynamicJwtDecoder implements JwtDecoder {

    private final Map<String, JwtDecoder> cache = new ConcurrentHashMap<>();

    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            // Extraire le payload du JWT
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                throw new JwtException("Format du JWT invalide");
            }
            String payloadJson = new String(Base64URL.from(parts[1]).decode());
            Map<?, ?> claims = new ObjectMapper().readValue(payloadJson, Map.class);
            String issuer = (String) claims.get("iss");
            if (issuer == null) {
                throw new JwtException("Issuer non trouvé dans le JWT");
            }

            // Récupérer ou créer un JwtDecoder pour cet issuer
            return cache.computeIfAbsent(issuer, JwtDecoders::fromIssuerLocation).decode(token);

        } catch (Exception e) {
            throw new JwtException("Impossible de décoder le JWT", e);
        }
    }
}