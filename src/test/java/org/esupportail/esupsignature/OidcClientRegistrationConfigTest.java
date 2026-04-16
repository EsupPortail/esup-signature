package org.esupportail.esupsignature;

import org.esupportail.esupsignature.config.security.OptionalOidcClientRegistrationConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import static org.assertj.core.api.Assertions.assertThat;

class OidcClientRegistrationConfigTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(OptionalOidcClientRegistrationConfig.class);

	@Test
	void shouldKeepAvailableRegistrationsWhenAnotherProviderFails() {
		contextRunner
				.withPropertyValues(
						"spring.security.oauth2.client.provider.proconnect.issuer-uri=http://127.0.0.1:1/issuer",
						"spring.security.oauth2.client.registration.proconnect.provider=proconnect",
						"spring.security.oauth2.client.registration.proconnect.client-id=test-client",
						"spring.security.oauth2.client.registration.proconnect.client-secret=test-secret",
						"spring.security.oauth2.client.registration.proconnect.authorization-grant-type=authorization_code",
						"spring.security.oauth2.client.registration.proconnect.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}",
						"spring.security.oauth2.client.registration.proconnect.scope=openid,email",
						"spring.security.oauth2.client.provider.franceconnect.authorization-uri=https://example.org/oauth/authorize",
						"spring.security.oauth2.client.provider.franceconnect.token-uri=https://example.org/oauth/token",
						"spring.security.oauth2.client.provider.franceconnect.user-info-uri=https://example.org/oauth/userinfo",
						"spring.security.oauth2.client.provider.franceconnect.user-name-attribute=sub",
						"spring.security.oauth2.client.provider.franceconnect.jwk-set-uri=https://example.org/oauth/jwks",
						"spring.security.oauth2.client.registration.franceconnect.provider=franceconnect",
						"spring.security.oauth2.client.registration.franceconnect.client-id=test-client",
						"spring.security.oauth2.client.registration.franceconnect.client-secret=test-secret",
						"spring.security.oauth2.client.registration.franceconnect.authorization-grant-type=authorization_code",
						"spring.security.oauth2.client.registration.franceconnect.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}",
						"spring.security.oauth2.client.registration.franceconnect.scope=openid,email")
				.run(context -> {
					assertThat(context).hasNotFailed();
					ClientRegistrationRepository repository = context.getBean(ClientRegistrationRepository.class);
					assertThat(repository.findByRegistrationId("proconnect")).isNull();
					assertThat(repository.findByRegistrationId("franceconnect")).isNotNull();
				});
	}

}
