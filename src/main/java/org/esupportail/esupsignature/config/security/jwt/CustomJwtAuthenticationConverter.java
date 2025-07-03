package org.esupportail.esupsignature.config.security.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.ArrayList;
import java.util.Collection;

public class CustomJwtAuthenticationConverter extends JwtAuthenticationConverter {

	private static final Logger LOGGER = LoggerFactory.getLogger(CustomJwtAuthenticationConverter.class);

	public CustomJwtAuthenticationConverter() {
		setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
		setPrincipalClaimName("uid");
	}

	private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
		Collection<GrantedAuthority> authorities = new ArrayList<>();
		authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
		return authorities;
	}
}