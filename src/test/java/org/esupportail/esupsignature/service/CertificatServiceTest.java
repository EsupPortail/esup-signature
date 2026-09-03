package org.esupportail.esupsignature.service;

import eu.europa.esig.dss.enumerations.CertificateQualification;
import eu.europa.esig.dss.model.DSSException;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.certificat.SealCertificatProperties;
import org.esupportail.esupsignature.config.sign.SignProperties;
import org.esupportail.esupsignature.dto.ui.global.CertificatViewDto;
import org.esupportail.esupsignature.entity.AppliVersion;
import org.esupportail.esupsignature.entity.Certificat;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.repository.AppliVersionRepository;
import org.esupportail.esupsignature.repository.CertificatRepository;
import org.esupportail.esupsignature.repository.WorkflowStepRepository;
import org.esupportail.esupsignature.service.mail.MailService;
import org.esupportail.esupsignature.service.utils.sign.OpenSCSignatureToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CertificatServiceTest {

    private CertificatService certificatService;
    private OpenSCSignatureToken openSCSignatureToken;
    private CertificatRepository certificatRepository;

    @BeforeEach
    void setUp() {
        GlobalProperties globalProperties = mock(GlobalProperties.class);
        SealCertificatProperties seal = new SealCertificatProperties(
                "Default", SealCertificatProperties.TokenType.OPENSC, null, null, "1234");
        when(globalProperties.getSealCertificatProperties()).thenReturn(Map.of("default", seal));

        AppliVersionRepository appliVersionRepository = mock(AppliVersionRepository.class);
        when(appliVersionRepository.findAll()).thenReturn(List.of(new AppliVersion("test")));

        certificatRepository = mock(CertificatRepository.class);
        certificatService = new CertificatService(
                globalProperties,
                new SignProperties(),
                mock(UserService.class),
                mock(UserKeystoreService.class),
                mock(eu.europa.esig.dss.spi.validation.CertificateVerifier.class),
                mock(MailService.class),
                certificatRepository,
                mock(DocumentService.class),
                mock(WorkflowStepRepository.class),
                appliVersionRepository);
        openSCSignatureToken = mock(OpenSCSignatureToken.class);
        ReflectionTestUtils.setField(certificatService, "openSCSignatureToken", openSCSignatureToken);
        certificatService.clearSealCertificatsCache();
    }

    @Test
    void missingSealCertificateIsCached() throws DSSException {
        when(openSCSignatureToken.getKeys()).thenThrow(new DSSException("token absent"));

        assertThat(certificatService.getSealCertificats()).isEmpty();
        assertThat(certificatService.getSealCertificats()).isEmpty();

        verify(openSCSignatureToken, times(1)).getKeys();
    }

    @Test
    void missingCheckedSealCertificateIsCached() throws DSSException {
        when(openSCSignatureToken.getKeys()).thenThrow(new DSSException("token absent"));

        assertThat(certificatService.getCheckedSealCertificates()).isEmpty();
        assertThat(certificatService.getCheckedSealCertificates()).isEmpty();

        verify(openSCSignatureToken, times(1)).getKeys();
    }

    @Test
    void missingSealCertificateIsSharedBetweenBothLookups() throws DSSException {
        when(openSCSignatureToken.getKeys()).thenThrow(new DSSException("token absent"));

        assertThat(certificatService.getSealCertificats()).isEmpty();
        assertThat(certificatService.getCheckedSealCertificates()).isEmpty();

        verify(openSCSignatureToken, times(1)).getKeys();
    }

    @Test
    void qualifiedElectronicSignatureCertificateIsAccepted() {
        assertThat(CertificatService.isQualifiedCertificateQualification(CertificateQualification.QCERT_FOR_ESIG_QSCD)).isTrue();
    }

    @Test
    void qualifiedElectronicSealCertificateIsAlsoAccepted() {
        assertThat(CertificatService.isQualifiedCertificateQualification(CertificateQualification.QCERT_FOR_ESEAL_QSCD)).isTrue();
    }

    @Test
    void qualifiedCertificateWithoutQscdIsRejected() {
        assertThat(CertificatService.isQualifiedCertificateQualification(CertificateQualification.QCERT_FOR_ESEAL)).isFalse();
    }

    @Test
    void certificateViewsContainOnlyDataNeededByTheUi() {
        Document keystore = new Document();
        keystore.setFileName("certificate.p12");
        Certificat certificat = new Certificat();
        certificat.setId(30L);
        certificat.setKeystore(keystore);
        certificat.setRoles(Set.of("ROLE_MANAGER"));
        when(certificatRepository.findAll()).thenReturn(List.of(certificat));

        List<CertificatViewDto> result = certificatService.getAllCertificatViews();

        assertThat(result).singleElement().satisfies(view -> {
            assertThat(view.getId()).isEqualTo(30L);
            assertThat(view.getFileName()).isEqualTo("certificate.p12");
            assertThat(view.getRoles()).containsExactly("ROLE_MANAGER");
        });
    }
}
