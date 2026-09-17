package org.esupportail.esupsignature.service.security.oauth;

import org.esupportail.esupsignature.config.security.oidc.OidcUserProperties;
import org.esupportail.esupsignature.service.security.OidcUserSecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OidcUserSecurityServiceResolver {

    private final OidcUserProperties oidcUserProperties;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final List<OidcUserSecurityService> oidcUserSecurityServices;

    public OidcUserSecurityServiceResolver(OidcUserProperties oidcUserProperties,
                                           @Autowired(required = false) ClientRegistrationRepository clientRegistrationRepository,
                                           List<OidcUserSecurityService> oidcUserSecurityServices) {
        this.oidcUserProperties = oidcUserProperties;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.oidcUserSecurityServices = oidcUserSecurityServices;
    }

    public Optional<OidcUserSecurityService> resolve(String registrationId) {
        return oidcUserSecurityServices.stream()
                .filter(service -> service.supports(registrationId))
                .findFirst()
                .or(() -> buildConfiguredService(registrationId));
    }

    public List<OidcUserSecurityService> getConfiguredServices() {
        return oidcUserProperties.getServices().entrySet().stream()
                .filter(entry -> hasClientRegistration(entry.getKey()))
                .filter(entry -> oidcUserSecurityServices.stream().noneMatch(service -> service.supports(entry.getKey())))
                .map(entry -> new ConfiguredOidcUserSecurityService(entry.getKey(), entry.getValue(), clientRegistrationRepository))
                .map(OidcUserSecurityService.class::cast)
                .toList();
    }

    public boolean isConfigured(String registrationId) {
        return oidcUserProperties.getServices().containsKey(registrationId);
    }

    private Optional<OidcUserSecurityService> buildConfiguredService(String registrationId) {
        OidcUserProperties.Service properties = oidcUserProperties.getServices().get(registrationId);
        if (properties == null || !hasClientRegistration(registrationId)) {
            return Optional.empty();
        }
        return Optional.of(new ConfiguredOidcUserSecurityService(registrationId, properties, clientRegistrationRepository));
    }

    private boolean hasClientRegistration(String registrationId) {
        return clientRegistrationRepository != null && clientRegistrationRepository.findByRegistrationId(registrationId) != null;
    }
}
