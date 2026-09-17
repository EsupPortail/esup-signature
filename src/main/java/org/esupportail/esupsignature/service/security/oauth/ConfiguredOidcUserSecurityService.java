package org.esupportail.esupsignature.service.security.oauth;

import org.apereo.cas.client.util.AbstractConfigurationFilter;
import org.esupportail.esupsignature.config.security.oidc.OidcUserProperties;
import org.esupportail.esupsignature.entity.enums.ExternalAuth;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.service.security.OidcUserSecurityService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import java.util.List;
import java.util.Map;

public class ConfiguredOidcUserSecurityService implements OidcUserSecurityService {

    private final String registrationId;
    private final OidcUserProperties.Service properties;
    private final ClientRegistrationRepository clientRegistrationRepository;

    public ConfiguredOidcUserSecurityService(String registrationId, OidcUserProperties.Service properties, ClientRegistrationRepository clientRegistrationRepository) {
        this.registrationId = registrationId;
        this.properties = properties;
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Override
    public boolean supports(String registrationId) {
        return this.registrationId.equals(registrationId);
    }

    @Override
    public String getTitle() {
        return StringUtils.hasText(properties.getTitle()) ? properties.getTitle() : registrationId;
    }

    @Override
    public String getDescription() {
        return properties.getDescription();
    }

    @Override
    public String getCode() {
        return registrationId;
    }

    @Override
    public String getLoginUrl() {
        return StringUtils.hasText(properties.getLoginUrl()) ? properties.getLoginUrl() : "/login/" + registrationId + "-entry";
    }

    @Override
    public String getLoggedOutUrl() {
        return properties.getLoggedOutUrl();
    }

    @Override
    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/" + registrationId);
    }

    @Override
    public GenericFilterBean getAuthenticationProcessingFilter() {
        return null;
    }

    @Override
    public UserDetailsService getUserDetailsService() {
        return null;
    }

    @Override
    public AbstractConfigurationFilter getSingleSignOutFilter() {
        return null;
    }

    @Override
    public String getLogoutUrl() {
        if (clientRegistrationRepository == null) {
            return "";
        }
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(registrationId);
        if (registration == null) {
            return "";
        }
        return registration.getProviderDetails().getConfigurationMetadata()
                .getOrDefault("end_session_endpoint", "")
                .toString();
    }

    @Override
    public ExternalAuth getExternalAuth() {
        return ExternalAuth.open;
    }

    @Override
    public SignatureAlgorithm getSignatureAlgorithm() {
        return properties.getSignatureAlgorithm();
    }

    @Override
    public Map<String, Object> getAdditionalAuthorizationParameters() {
        return properties.getAdditionalAuthorizationParameters();
    }

    @Override
    public String getPrincipalClaim() {
        return properties.getPrincipalClaim();
    }

    @Override
    public String getEmailClaim() {
        return properties.getEmailClaim();
    }

    @Override
    public String getFirstnameClaim() {
        return properties.getFirstnameClaim();
    }

    @Override
    public String getLastnameClaim() {
        return properties.getLastnameClaim();
    }

    @Override
    public List<String> getGroupsClaims() {
        return properties.getGroupsClaims();
    }

    @Override
    public UserType getUserType() {
        return properties.getUserType();
    }
}
