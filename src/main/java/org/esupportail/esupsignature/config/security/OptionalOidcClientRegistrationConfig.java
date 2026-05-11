package org.esupportail.esupsignature.config.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientPropertiesMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(OAuth2ClientProperties.class)
public class OptionalOidcClientRegistrationConfig {

	private static final Logger logger = LoggerFactory.getLogger(OptionalOidcClientRegistrationConfig.class);

	@Bean
	public ClientRegistrationRepository clientRegistrationRepository(OAuth2ClientProperties oauth2ClientProperties) {
		Map<String, ClientRegistration> clientRegistrations = new LinkedHashMap<>();
		for (String registrationId : oauth2ClientProperties.getRegistration().keySet()) {
			try {
				ClientRegistration clientRegistration = getClientRegistration(registrationId, oauth2ClientProperties);
				if (clientRegistration != null) {
					clientRegistrations.put(registrationId, clientRegistration);
				}
			} catch (RuntimeException e) {
				logger.warn("OAuth2 provider '{}' indisponible au démarrage, authentification désactivée pour ce fournisseur : {}", registrationId, e.getMessage());
				logger.debug("Erreur de résolution du provider OAuth2 '{}'", registrationId, e);
			}
		}
		return new OptionalClientRegistrationRepository(clientRegistrations.values());
	}

	private ClientRegistration getClientRegistration(String registrationId, OAuth2ClientProperties oauth2ClientProperties) {
		OAuth2ClientProperties.Registration registration = oauth2ClientProperties.getRegistration().get(registrationId);
		if (registration == null) {
			return null;
		}
		OAuth2ClientProperties partialProperties = new OAuth2ClientProperties();
		partialProperties.getRegistration().put(registrationId, registration);
		copyProviderIfPresent(partialProperties, oauth2ClientProperties, registrationId);
		if (StringUtils.hasText(registration.getProvider())) {
			copyProviderIfPresent(partialProperties, oauth2ClientProperties, registration.getProvider());
		}
		return new OAuth2ClientPropertiesMapper(partialProperties).asClientRegistrations().get(registrationId);
	}

	private void copyProviderIfPresent(OAuth2ClientProperties targetProperties, OAuth2ClientProperties sourceProperties, String providerId) {
		OAuth2ClientProperties.Provider provider = sourceProperties.getProvider().get(providerId);
		if (provider != null) {
			targetProperties.getProvider().put(providerId, provider);
		}
	}

	private static final class OptionalClientRegistrationRepository implements ClientRegistrationRepository, Iterable<ClientRegistration> {

		private final Map<String, ClientRegistration> clientRegistrations;

		private OptionalClientRegistrationRepository(Collection<ClientRegistration> clientRegistrations) {
			Map<String, ClientRegistration> registrationMap = new LinkedHashMap<>();
			for (ClientRegistration clientRegistration : clientRegistrations) {
				registrationMap.put(clientRegistration.getRegistrationId(), clientRegistration);
			}
			this.clientRegistrations = Collections.unmodifiableMap(registrationMap);
		}

		@Override
		public ClientRegistration findByRegistrationId(String registrationId) {
			return clientRegistrations.get(registrationId);
		}

		@Override
		public Iterator<ClientRegistration> iterator() {
			return clientRegistrations.values().iterator();
		}
	}

}
