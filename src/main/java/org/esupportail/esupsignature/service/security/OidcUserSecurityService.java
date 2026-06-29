package org.esupportail.esupsignature.service.security;

import org.esupportail.esupsignature.entity.enums.UserType;

import java.util.List;

public interface OidcUserSecurityService extends OidcSecurityService {

    boolean supports(String registrationId);

    String getPrincipalClaim();

    String getEmailClaim();

    String getFirstnameClaim();

    String getLastnameClaim();

    List<String> getGroupsClaims();

    UserType getUserType();
}
