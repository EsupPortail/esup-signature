package org.esupportail.esupsignature.service.utils.sign;

import eu.europa.esig.dss.enumerations.ImageScaling;
import eu.europa.esig.dss.enumerations.VisualSignatureRotation;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.pades.SignatureFieldParameters;
import eu.europa.esig.dss.pdf.AnnotationBox;
import eu.europa.esig.dss.pdf.visible.SignatureFieldDimensionAndPositionBuilder;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.sign.SignProperties;
import org.esupportail.esupsignature.dss.DssUtilsService;
import org.esupportail.esupsignature.dss.config.DSSProperties;
import org.esupportail.esupsignature.entity.LiveWorkflow;
import org.esupportail.esupsignature.entity.LiveWorkflowStep;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.entity.enums.SignLevel;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.service.CertificatService;
import org.esupportail.esupsignature.service.DocumentService;
import org.esupportail.esupsignature.service.UserKeystoreService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.interfaces.certificat.impl.OpenXPKICertificatGenerationService;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.esupportail.esupsignature.service.utils.pdf.PdfParameters;
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class SignServiceSignLevelTest {

    private CertificatService certificatService;
    private SignService signService;
    private CertificateToken certificateToken;

    @BeforeEach
    void setUp() {
        certificatService = mock(CertificatService.class);
        certificateToken = mock(CertificateToken.class);
        signService = new SignService(
                mock(OpenXPKICertificatGenerationService.class),
                new SignProperties(),
                mock(eu.europa.esig.dss.pades.signature.PAdESService.class),
                mock(eu.europa.esig.dss.asic.cades.signature.ASiCWithCAdESService.class),
                mock(eu.europa.esig.dss.asic.xades.signature.ASiCWithXAdESService.class),
                mock(DocumentService.class),
                mock(FileService.class),
                mock(PdfService.class),
                mock(UserService.class),
                mock(UserKeystoreService.class),
                certificatService,
                mock(ValidationService.class),
                new DSSProperties(),
                mock(GlobalProperties.class),
                mock(DssUtilsService.class),
                mock(SignRequestRepository.class));
    }

    @Test
    void advancedLevelAcceptsEveryCertificateWithoutDssQualificationCheck() {
        signService.checkRequiredCertificateLevel(signRequest(SignLevel.advanced), certificateToken, false);

        verifyNoInteractions(certificatService);
    }

    @Test
    void qualifiedLevelAcceptsQualifiedSignatureCertificate() {
        when(certificatService.isQualifiedCertificate(certificateToken, false)).thenReturn(true);

        signService.checkRequiredCertificateLevel(signRequest(SignLevel.qualified), certificateToken, false);
    }

    @Test
    void qualifiedLevelRejectsNonQualifiedSignatureCertificate() {
        when(certificatService.isQualifiedCertificate(certificateToken, false)).thenReturn(false);

        assertThatThrownBy(() -> signService.checkRequiredCertificateLevel(signRequest(SignLevel.qualified), certificateToken, false))
                .isInstanceOf(EsupSignatureRuntimeException.class)
                .hasMessageContaining("signature électronique qualifiée");
    }

    @Test
    void qualifiedLevelChecksSealUsageForSealCertificate() {
        when(certificatService.isQualifiedCertificate(certificateToken, true)).thenReturn(false);

        assertThatThrownBy(() -> signService.checkRequiredCertificateLevel(signRequest(SignLevel.qualified), certificateToken, true))
                .isInstanceOf(EsupSignatureRuntimeException.class)
                .hasMessageContaining("cachet électronique qualifié");
    }

    @Test
    void visiblePadesSignaturePreservesImageAspectRatio() {
        var imageParameters = signService.createSignatureImageParameters();

        assertThat(imageParameters.getImageScaling()).isEqualTo(ImageScaling.ZOOM_AND_CENTER);
        assertThat(imageParameters.getFieldParameters().getRotation()).isEqualTo(VisualSignatureRotation.AUTOMATIC);
    }

    @Test
    void visiblePadesSignatureCompensatesDssAppearanceTranslationOn270DegreePage() {
        var imageParameters = signService.createSignatureImageParameters();
        SignatureFieldParameters fieldParameters = imageParameters.getFieldParameters();
        SignRequestParams requestParams = new SignRequestParams();
        requestParams.setxPos(82);
        requestParams.setyPos(292);

        signService.configureSignatureFieldPosition(
                fieldParameters, new PdfParameters(842, 595, 270, 7), requestParams, 166, 100, 1f);

        assertThat(fieldParameters.getWidth()).isEqualTo(166);
        assertThat(fieldParameters.getHeight()).isEqualTo(100);
        assertThat(fieldParameters.getOriginX()).isEqualTo(82);
        assertThat(fieldParameters.getOriginY()).isEqualTo(284);

        var pdfBox = new SignatureFieldDimensionAndPositionBuilder(
                imageParameters, null, new AnnotationBox(0, 0, 842, 595), 270)
                .build()
                .getAnnotationBox();
        // Compensate in the opposite direction from DSS's translation of the rotated
        // appearance stream.
        assertThat(pdfBox.getMinX()).isEqualTo(458);
        assertThat(pdfBox.getMinY()).isEqualTo(347);
        assertThat(pdfBox.getWidth()).isEqualTo(100);
        assertThat(pdfBox.getHeight()).isEqualTo(166);
    }

    @Test
    void visiblePadesSignatureKeepsTopBasedCoordinatesOnUnrotatedPage() {
        SignatureFieldParameters fieldParameters = new SignatureFieldParameters();
        SignRequestParams requestParams = new SignRequestParams();
        requestParams.setxPos(51);
        requestParams.setyPos(306);

        signService.configureSignatureFieldPosition(
                fieldParameters, new PdfParameters(595, 842, 0, 1), requestParams, 166, 100, 1f);

        assertThat(fieldParameters.getOriginX()).isEqualTo(51);
        assertThat(fieldParameters.getOriginY()).isEqualTo(306);
    }

    private SignRequest signRequest(SignLevel minSignLevel) {
        LiveWorkflowStep step = new LiveWorkflowStep();
        step.setMaxSignLevel(SignLevel.qualified);
        step.setMinSignLevel(minSignLevel);
        LiveWorkflow liveWorkflow = new LiveWorkflow();
        liveWorkflow.setCurrentStep(step);
        SignBook signBook = new SignBook();
        signBook.setLiveWorkflow(liveWorkflow);
        SignRequest signRequest = new SignRequest();
        signRequest.setParentSignBook(signBook);
        return signRequest;
    }
}
