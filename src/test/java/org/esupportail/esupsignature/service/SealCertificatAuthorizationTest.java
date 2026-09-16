package org.esupportail.esupsignature.service;

import eu.europa.esig.dss.spi.validation.CertificateVerifier;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.certificat.SealCertificatProperties;
import org.esupportail.esupsignature.config.sign.SignProperties;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignWith;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.repository.AppliVersionRepository;
import org.esupportail.esupsignature.repository.CertificatRepository;
import org.esupportail.esupsignature.repository.WorkflowStepRepository;
import org.esupportail.esupsignature.service.mail.MailService;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@SuppressWarnings("deprecation")
class SealCertificatAuthorizationTest {

    @Test
    void legacyFlatSealConfigurationKeepsExternalAuthorization() {
        GlobalProperties globalProperties = new GlobalProperties();
        globalProperties.setSealCertificatType(SealCertificatProperties.TokenType.OPENSC);
        globalProperties.setSealCertificatPin("1234");
        globalProperties.setSealForExternals(true);

        globalProperties.init();

        assertThat(globalProperties.getSealCertificatProperties()).containsOnlyKeys("default");
        assertThat(globalProperties.getSealCertificatProperties().get("default").getSealForExternals()).isTrue();
    }

    @Test
    void legacyExternalSettingOnlyAppliesToDefaultSeal() {
        GlobalProperties globalProperties = new GlobalProperties();
        SealCertificatProperties defaultSeal = seal("default", null, "ROLE_SEAL");
        SealCertificatProperties otherSeal = seal("other", null, "ROLE_OTHER");
        globalProperties.setSealCertificatProperties(new LinkedHashMap<>(Map.of(
                "default", defaultSeal,
                "other", otherSeal
        )));
        globalProperties.setSealForExternals(true);

        globalProperties.init();

        assertThat(defaultSeal.getSealForExternals()).isTrue();
        assertThat(otherSeal.getSealForExternals()).isFalse();
    }

    @Test
    void perSealExternalSettingDefaultsToFalse() {
        assertThat(new SealCertificatProperties().getSealForExternals()).isFalse();
    }

    @Test
    void explicitPerSealSettingOverridesLegacySetting() {
        GlobalProperties globalProperties = new GlobalProperties();
        SealCertificatProperties defaultSeal = seal("default", false, "ROLE_SEAL");
        globalProperties.setSealCertificatProperties(new LinkedHashMap<>(Map.of("default", defaultSeal)));
        globalProperties.setSealForExternals(true);

        globalProperties.init();

        assertThat(defaultSeal.getSealForExternals()).isFalse();
    }

    @Test
    void externalCanOnlyUseEnabledSealOnAlreadySignedDocument() {
        GlobalProperties globalProperties = new GlobalProperties();
        SealCertificatProperties externalSeal = seal("external", true, "ROLE_SEAL");
        SealCertificatProperties restrictedSeal = seal("restricted", false, "ROLE_SEAL");
        globalProperties.setSealCertificatProperties(new LinkedHashMap<>(Map.of(
                "default", restrictedSeal,
                "external", externalSeal
        )));
        globalProperties.init();

        User external = user("external-user", UserType.external, "ROLE_OTP");
        CertificatService certificatService = certificatService(globalProperties, external, List.of(externalSeal, restrictedSeal));

        assertThat(certificatService.getAuthorizedSealCertificatProperties(external.getEppn(), false)).isEmpty();
        assertThat(certificatService.getAuthorizedSealCertificatProperties(external.getEppn(), true))
                .containsExactly(externalSeal);
        assertThat(certificatService.isSealCertificatAuthorized(external.getEppn(), "external", true)).isTrue();
        assertThat(certificatService.isSealCertificatAuthorized(external.getEppn(), "restricted", true)).isFalse();
    }

    @Test
    void unavailableCertificateDoesNotRemoveExternalAuthorization() {
        GlobalProperties globalProperties = new GlobalProperties();
        SealCertificatProperties externalSeal = seal("default", true, "ROLE_SEAL");
        globalProperties.setSealCertificatProperties(new LinkedHashMap<>(Map.of("default", externalSeal)));
        globalProperties.init();

        User external = user("external-user", UserType.external, "ROLE_OTP");
        CertificatService certificatService = certificatService(globalProperties, external, List.of());

        assertThat(certificatService.getAuthorizedSealCertificatProperties(external.getEppn(), true))
                .containsExactly(externalSeal);
    }

    @Test
    void signedExternalDocumentOffersSealAsSigningMode() {
        GlobalProperties globalProperties = new GlobalProperties();
        User external = user("external-user", UserType.external, "ROLE_OTP");
        SealCertificatProperties externalSeal = seal("default", true, "ROLE_SEAL");
        UserService userService = mock(UserService.class);
        CertificatService certificatService = mock(CertificatService.class);
        when(userService.getByEppn(external.getEppn())).thenReturn(external);
        when(certificatService.getAuthorizedSealCertificatProperties(external.getEppn(), true)).thenReturn(List.of(externalSeal));
        when(certificatService.getAuthorizedSealCertificatProperties(external.getEppn(), false)).thenReturn(List.of());
        when(certificatService.getCertificatByUser(external.getEppn())).thenReturn(List.of());
        SignWithService signWithService = new SignWithService(userService, certificatService, globalProperties);

        assertThat(signWithService.getAuthorizedSignWiths(external.getEppn(), true)).contains(SignWith.sealCert);
        assertThat(signWithService.getAuthorizedSignWiths(external.getEppn(), false)).doesNotContain(SignWith.sealCert);
    }

    @Test
    void matchingRoleStillAuthorizesSealWithoutExistingSignature() {
        GlobalProperties globalProperties = new GlobalProperties();
        SealCertificatProperties roleSeal = seal("default", false, "ROLE_RH");
        globalProperties.setSealCertificatProperties(new LinkedHashMap<>(Map.of("default", roleSeal)));
        globalProperties.init();

        User internal = user("internal-user", UserType.ldap, "ROLE_RH");
        CertificatService certificatService = certificatService(globalProperties, internal, List.of(roleSeal));

        assertThat(certificatService.isSealCertificatAuthorized(internal.getEppn(), "default", false)).isTrue();
    }

    @Test
    void signedFileSettingAuthorizesConfiguredSealsForInternalUser() {
        GlobalProperties globalProperties = new GlobalProperties();
        SealCertificatProperties defaultSeal = seal("default", false, "ROLE_SEAL");
        SealCertificatProperties restrictedSeal = seal("restricted", false, "ROLE_RH");
        globalProperties.setSealCertificatProperties(new LinkedHashMap<>(Map.of(
                "default", defaultSeal,
                "restricted", restrictedSeal
        )));
        globalProperties.setSealAuthorizedForSignedFiles(true);
        globalProperties.init();

        User internal = user("internal-user", UserType.ldap, "ROLE_USER");
        CertificatService certificatService = certificatService(globalProperties, internal, List.of(defaultSeal, restrictedSeal));

        assertThat(certificatService.isSealCertificatAuthorized(internal.getEppn(), "default", true)).isTrue();
        assertThat(certificatService.isSealCertificatAuthorized(internal.getEppn(), "restricted", true)).isTrue();
        assertThat(certificatService.isSealCertificatAuthorized(internal.getEppn(), "default", false)).isFalse();
    }

    private CertificatService certificatService(GlobalProperties globalProperties, User user, List<SealCertificatProperties> checkedSeals) {
        UserService userService = mock(UserService.class);
        when(userService.getByEppn(user.getEppn())).thenReturn(user);
        CertificatService certificatService = spy(new CertificatService(
                globalProperties,
                mock(SignProperties.class),
                userService,
                mock(UserKeystoreService.class),
                mock(CertificateVerifier.class),
                mock(MailService.class),
                mock(CertificatRepository.class),
                mock(DocumentService.class),
                mock(WorkflowStepRepository.class),
                mock(AppliVersionRepository.class)
        ));
        doReturn(checkedSeals).when(certificatService).getCheckedSealCertificates();
        return certificatService;
    }

    private SealCertificatProperties seal(String name, Boolean sealForExternals, String... roles) {
        SealCertificatProperties seal = new SealCertificatProperties();
        seal.sealCertificatName = name;
        if (sealForExternals != null) {
            seal.setSealForExternals(sealForExternals);
        }
        seal.setRoles(List.of(roles));
        return seal;
    }

    private User user(String eppn, UserType userType, String... roles) {
        User user = new User();
        user.setEppn(eppn);
        user.setUserType(userType);
        user.setRoles(Set.of(roles));
        return user;
    }
}
