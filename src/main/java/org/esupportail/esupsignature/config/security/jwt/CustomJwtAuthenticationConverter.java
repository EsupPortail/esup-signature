package org.esupportail.esupsignature.config.security.jwt;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CustomJwtAuthenticationConverter implements Converter<Jwt, AbstractOAuth2TokenAuthenticationToken<Jwt>> {

	private final String internalDomain;

	public CustomJwtAuthenticationConverter(String internalDomain) {
		this.internalDomain = internalDomain;
	}

	@Override
	public AbstractOAuth2TokenAuthenticationToken<Jwt> convert(Jwt jwt) {
		return new JwtAuthenticationToken(jwt, extractAuthorities(jwt), resolvePrincipal(jwt));
	}

	private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
		Collection<GrantedAuthority> authorities = new ArrayList<>();
		authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
		return authorities;
	}

	private String resolvePrincipal(Jwt jwt) {
		for(String claimName : List.of("eduPersonPrincipalName", "uid", "sub")) {
			String principal = jwt.getClaimAsString(claimName);
			if(StringUtils.hasText(principal)) {
				return buildEppn(principal);
			}
		}
		return "";
	}

	private String buildEppn(String principal) {
		String eppn = principal.trim();
		if(eppn.contains("@") || !StringUtils.hasText(internalDomain)) {
			return eppn;
		}
		return eppn + "@" + internalDomain;
	}
}
