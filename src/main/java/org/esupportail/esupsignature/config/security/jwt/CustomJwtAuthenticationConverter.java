package org.esupportail.esupsignature.config.security.jwt;

import org.esupportail.esupsignature.service.security.jwt.JwtAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.Collection;

public class CustomJwtAuthenticationConverter extends JwtAuthenticationConverter {

	private static final Logger LOGGER = LoggerFactory.getLogger(CustomJwtAuthenticationConverter.class);

	private final JwtAuthService authoritiesService;

	public CustomJwtAuthenticationConverter(
		JwtAuthService authoritiesService
	) {
		setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
		setPrincipalClaimName("uid");
		this.authoritiesService = authoritiesService;
	}

	private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {

		LOGGER.debug("Extracting authorities from JWT");
		return authoritiesService.getGrantedAuthorities(jwt);
	}
}