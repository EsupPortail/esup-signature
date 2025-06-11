package org.esupportail.esupsignature.service.security.oauth;

import org.esupportail.esupsignature.service.security.OidcOtpSecurityService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequestEntityConverter;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ValidatingOAuth2UserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private static final String MISSING_USER_INFO_URI_ERROR_CODE = "missing_user_info_uri";

    private static final String MISSING_USER_NAME_ATTRIBUTE_ERROR_CODE = "missing_user_name_attribute";

    private static final String INVALID_USER_INFO_RESPONSE_ERROR_CODE = "invalid_user_info_response";

    private final Converter<OAuth2UserRequest, RequestEntity<?>> requestEntityConverter = new OAuth2UserRequestEntityConverter();
    private final RestOperations restOperations;
    private final List<OidcOtpSecurityService> securityServices;
    private final ClientRegistrationRepository clientRegistrationRepository;


    public ValidatingOAuth2UserService(List<OidcOtpSecurityService> securityServices, ClientRegistrationRepository clientRegistrationRepository) {
        this.securityServices = securityServices;
        this.clientRegistrationRepository = clientRegistrationRepository;
        JwtHttpMessageConverter jwtConverter = new JwtHttpMessageConverter();
        jwtConverter.setSupportedMediaTypes(List.of(MediaType.valueOf("application/jwt")));
        RestTemplate restTemplate = new RestTemplate(List.of(jwtConverter));
        restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
        restTemplate.getMessageConverters().add(new JwtHttpMessageConverter());
        this.restOperations = restTemplate;
    }

    public JwtDecoder getJwtDecoder(String registrationId, SignatureAlgorithm signatureAlgorithm) {
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(registrationId);
        String jwkSetUri = registration.getProviderDetails().getJwkSetUri();
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                .jwsAlgorithm(signatureAlgorithm)
                .build();
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        Assert.notNull(userRequest, "userRequest cannot be null");
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OidcOtpSecurityService currentSecurityService = securityServices.stream().filter(s -> s.getCode().equals(registrationId)).findFirst().get();
        JwtDecoder jwtDecoder = getJwtDecoder(currentSecurityService.getCode(), currentSecurityService.getSignatureAlgorithm());
        if (!StringUtils.hasText(userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri())) {
            OAuth2Error oauth2Error = new OAuth2Error(
                    MISSING_USER_INFO_URI_ERROR_CODE,
                    "Missing required UserInfo Uri in UserInfoEndpoint for Client Registration: " +
                            userRequest.getClientRegistration().getRegistrationId(),
                    null
            );
            throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
        }
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();
        if (!StringUtils.hasText(userNameAttributeName)) {
            OAuth2Error oauth2Error = new OAuth2Error(
                    MISSING_USER_NAME_ATTRIBUTE_ERROR_CODE,
                    "Missing required \"user name\" attribute name in UserInfoEndpoint for Client Registration: " +
                            userRequest.getClientRegistration().getRegistrationId(),
                    null
            );
            throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
        }

        RequestEntity<?> request = this.requestEntityConverter.convert(userRequest);

        ResponseEntity<String> response;
        try {
            response = this.restOperations.exchange(request, String.class);
        } catch (OAuth2AuthorizationException ex) {
            OAuth2Error oauth2Error = ex.getError();
            StringBuilder errorDetails = new StringBuilder();
            errorDetails.append("Error details: [");
            errorDetails.append("UserInfo Uri: ").append(
                    userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri());
            errorDetails.append(", Error Code: ").append(oauth2Error.getErrorCode());
            if (oauth2Error.getDescription() != null) {
                errorDetails.append(", Error Description: ").append(oauth2Error.getDescription());
            }
            errorDetails.append("]");
            oauth2Error = new OAuth2Error(INVALID_USER_INFO_RESPONSE_ERROR_CODE,
                    "An error occurred while attempting to retrieve the UserInfo Resource: " + errorDetails.toString(), null);
            throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString(), ex);
        } catch (RestClientException ex) {
            OAuth2Error oauth2Error = new OAuth2Error(INVALID_USER_INFO_RESPONSE_ERROR_CODE,
                    "An error occurred while attempting to retrieve the UserInfo Resource: " + ex.getMessage(), null);
            throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString(), ex);
        }

        Jwt jwt = jwtDecoder.decode(response.getBody());

        Map<String, Object> userAttributes = jwt.getClaims();
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        authorities.add(new OAuth2UserAuthority(userAttributes));
        OAuth2AccessToken token = userRequest.getAccessToken();
        for (String authority : token.getScopes()) {
            authorities.add(new SimpleGrantedAuthority("SCOPE_" + authority));
        }
        return new DefaultOidcUser(authorities, new OidcIdToken(userRequest.getIdToken().getTokenValue(), userRequest.getIdToken().getIssuedAt(), userRequest.getIdToken().getExpiresAt(), jwt.getClaims()), userNameAttributeName);
    }

}