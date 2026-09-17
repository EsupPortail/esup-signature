package org.esupportail.esupsignature.config.security.oidc;

import org.esupportail.esupsignature.entity.enums.UserType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "security.oidc-user")
public class OidcUserProperties {

    private Map<String, Service> services = new LinkedHashMap<>();

    public Map<String, Service> getServices() {
        return services;
    }

    public void setServices(Map<String, Service> services) {
        this.services = services;
    }

    public static class Service {

        private String title;
        private String description = "";
        private String loginUrl;
        private String loggedOutUrl = "/logged-out";
        private String principalClaim = "sub";
        private String emailClaim = "email";
        private String firstnameClaim = "given_name";
        private String lastnameClaim = "family_name";
        private List<String> groupsClaims = new ArrayList<>();
        private UserType userType = UserType.ldap;
        private SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.RS256;
        private Map<String, Object> additionalAuthorizationParameters = new LinkedHashMap<>();

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getLoginUrl() {
            return loginUrl;
        }

        public void setLoginUrl(String loginUrl) {
            this.loginUrl = loginUrl;
        }

        public String getLoggedOutUrl() {
            return loggedOutUrl;
        }

        public void setLoggedOutUrl(String loggedOutUrl) {
            this.loggedOutUrl = loggedOutUrl;
        }

        public String getPrincipalClaim() {
            return principalClaim;
        }

        public void setPrincipalClaim(String principalClaim) {
            this.principalClaim = principalClaim;
        }

        public String getEmailClaim() {
            return emailClaim;
        }

        public void setEmailClaim(String emailClaim) {
            this.emailClaim = emailClaim;
        }

        public String getFirstnameClaim() {
            return firstnameClaim;
        }

        public void setFirstnameClaim(String firstnameClaim) {
            this.firstnameClaim = firstnameClaim;
        }

        public String getLastnameClaim() {
            return lastnameClaim;
        }

        public void setLastnameClaim(String lastnameClaim) {
            this.lastnameClaim = lastnameClaim;
        }

        public List<String> getGroupsClaims() {
            return groupsClaims;
        }

        public void setGroupsClaims(List<String> groupsClaims) {
            this.groupsClaims = groupsClaims;
        }

        public UserType getUserType() {
            return userType;
        }

        public void setUserType(UserType userType) {
            this.userType = userType;
        }

        public SignatureAlgorithm getSignatureAlgorithm() {
            return signatureAlgorithm;
        }

        public void setSignatureAlgorithm(SignatureAlgorithm signatureAlgorithm) {
            this.signatureAlgorithm = signatureAlgorithm;
        }

        public Map<String, Object> getAdditionalAuthorizationParameters() {
            return additionalAuthorizationParameters;
        }

        public void setAdditionalAuthorizationParameters(Map<String, Object> additionalAuthorizationParameters) {
            this.additionalAuthorizationParameters = additionalAuthorizationParameters;
        }
    }
}
