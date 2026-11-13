package org.esupportail.esupsignature.web;

import org.esupportail.esupsignature.service.utils.sign.ValidationService;
import org.esupportail.esupsignature.dss.service.XSLTService;
import org.esupportail.esupsignature.service.utils.sign.SignService;
import org.esupportail.esupsignature.config.GlobalProperties;

import org.esupportail.esupsignature.entity.AuditTrail;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ConcurrentModel;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PublicControlValidationTest {
    private final LogService logs = mock(LogService.class);
    private final SignRequestService requests = mock(SignRequestService.class);
    private final AuditTrailService audits = mock(AuditTrailService.class);
    private final FileService files = mock(FileService.class);
    private final UserService users = mock(UserService.class);
    private final PreAuthorizeService rights = mock(PreAuthorizeService.class);
    private final ValidationService validation = mock(ValidationService.class);
    private final PublicController controller = new PublicController(null, new GlobalProperties(), logs, requests,
            audits, files, users, mock(XSLTService.class), rights, validation, mock(SignService.class), mock(MobileSignTokenService.class));
    private final byte[] uploaded = "%PDF-1.7 uploaded version".getBytes(StandardCharsets.US_ASCII);

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatedConsultationDoesNotLoadDetailsBeforeHashVerification() throws Exception {
        AuditTrail audit = new AuditTrail();
        audit.setToken("token");
        when(audits.getAuditTrailByToken("token")).thenReturn(audit);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user@example.org", "unused"));
        ConcurrentModel model = new ConcurrentModel();

        assertThat(controller.controlToken("token", model)).isEqualTo("public/control");

        assertThat(model.asMap()).containsOnlyKeys("token", "auditTrailChecked", "controlLoginRequired");
        assertThat(model.getAttribute("auditTrailChecked")).isEqualTo(false);
        verifyNoInteractions(validation, logs, requests, users, rights, files);
    }

    @Test
    void matchesTheUploadedVersionWithoutReadingTheStoredVersion() throws Exception {
        AuditTrail audit = new AuditTrail();
        audit.setToken("token");
        when(files.getFileChecksum(any(InputStream.class))).thenReturn("digest");
        when(audits.getAuditTrailFromCheksum("digest")).thenReturn(audit);
        when(requests.getSignRequestByToken("token")).thenReturn(Optional.empty());
        when(logs.getFullByToken("token")).thenReturn(List.of());
        ConcurrentModel model = new ConcurrentModel();

        controller.checkFile("token", new MockMultipartFile("multipartFile", uploaded), model, new MockHttpSession());

        verifyNoInteractions(validation);
        verify(requests, never()).getToValidateFile(anyLong());
        assertThat(model.getAttribute("auditTrailChecked")).isEqualTo(true);
        assertThat(model.getAttribute("error")).isEqualTo(false);
    }

    @Test
    void doesNotAttachAnotherDocumentsAuditToAnUploadWithAMismatchingToken() throws Exception {
        AuditTrail other = new AuditTrail();
        other.setToken("other-token");
        when(files.getFileChecksum(any(InputStream.class))).thenReturn("digest");
        when(audits.getAuditTrailFromCheksum("digest")).thenReturn(other);
        ConcurrentModel model = new ConcurrentModel();

        controller.checkFile("expected-token", new MockMultipartFile("multipartFile", uploaded), model, new MockHttpSession());

        assertThat(model.getAttribute("auditTrailChecked")).isEqualTo(false);
        assertThat(model.getAttribute("auditTrail")).isNull();
        assertThat(model.asMap()).containsOnlyKeys("token", "auditTrailChecked", "error", "controlLoginRequired");
        verifyNoInteractions(validation, logs, requests, users, rights);
    }
    @Test
    void unknownHashDoesNotRevealDetailsEvenForAnAuthenticatedUser() throws Exception {
        when(files.getFileChecksum(any(InputStream.class))).thenReturn("unknown-digest");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user@example.org", "unused"));
        ConcurrentModel model = new ConcurrentModel();

        controller.checkFile("expected-token", new MockMultipartFile("multipartFile", uploaded), model, new MockHttpSession());

        assertThat(model.asMap()).containsOnlyKeys("token", "auditTrailChecked", "error", "controlLoginRequired");
        assertThat(model.getAttribute("error")).isEqualTo(true);
        assertThat(model.getAttribute("auditTrailChecked")).isEqualTo(false);
        verifyNoInteractions(validation, logs, requests, users, rights);
    }

    @Test
    void roleUserCanReadOnlyReferenceMetadataWithoutValidatingAFile() throws Exception {
        AuditTrail audit = new AuditTrail();
        audit.setToken("token");
        audit.setDocumentName("original.pdf");
        audit.setDocumentCheckSum("expected-digest");
        when(audits.getAuditTrailByToken("token")).thenReturn(audit);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "reader", "unused", List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))));
        ConcurrentModel model = new ConcurrentModel();
        controller.controlToken("token", model);
        assertThat(model.getAttribute("referenceAvailable")).isEqualTo(true);
        assertThat(model.getAttribute("referenceDocumentName")).isEqualTo("original.pdf");
        assertThat(model.getAttribute("referenceDocumentChecksum")).isEqualTo("expected-digest");
        assertThat(model.getAttribute("controlLoginRequired")).isEqualTo(false);
        assertThat(model.getAttribute("auditTrailChecked")).isEqualTo(false);
        assertThat(model.asMap()).doesNotContainKeys("auditTrail", "logs", "signRequest", "validationSignatures");
        verifyNoInteractions(validation, logs, requests, users, rights, files);
    }

    @Test
    void roleUserCanSeeReferenceAfterMismatchWithoutValidatingTheUploadedFile() throws Exception {
        AuditTrail audit = new AuditTrail();
        audit.setToken("token");
        audit.setDocumentName("reference.pdf");
        when(audits.getAuditTrailByToken("token")).thenReturn(audit);
        when(files.getFileChecksum(any(InputStream.class))).thenReturn("rewritten");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "reader", "unused", List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))));
        ConcurrentModel model = new ConcurrentModel();
        controller.checkFile("token", new MockMultipartFile("multipartFile", uploaded), model, new MockHttpSession());
        assertThat(model.getAttribute("referenceAvailable")).isEqualTo(true);
        assertThat(model.getAttribute("auditTrailChecked")).isEqualTo(false);
        assertThat(model.getAttribute("error")).isEqualTo(true);
        assertThat(model.asMap()).doesNotContainKeys("auditTrail", "logs", "signRequest", "validationSignatures");
        verifyNoInteractions(validation, logs, requests, users, rights);
    }

    @Test
    void otpRoleCannotReadReferenceMetadata() throws Exception {
        AuditTrail audit = new AuditTrail();
        when(audits.getAuditTrailByToken("token")).thenReturn(audit);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "reader", "unused", List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_OTP"))));
        ConcurrentModel model = new ConcurrentModel();
        controller.controlToken("token", model);
        verify(audits, never()).getReferenceSignatures(anyString());
        assertThat(model.getAttribute("referenceAvailable")).isNull();
        assertThat(model.getAttribute("controlLoginRequired")).isEqualTo(true);
        assertThat(model.asMap()).doesNotContainKeys("referenceDocumentName", "referenceDocumentChecksum");
    }

    @Test
    void loginResumesTheSameControlThroughExistingCasAndOidcMechanisms() {
        when(audits.getAuditTrailByToken("token")).thenReturn(new AuditTrail());
        MockHttpSession session = new MockHttpSession();
        assertThat(controller.loginForControl("token", session)).isEqualTo(
                "redirect:/user/signrequests/control/token?redirect=/user/signrequests/control/token");
        assertThat(session.getAttribute("after_oauth_redirect")).isEqualTo("/user/signrequests/control/token");
        MockHttpSession invalidSession = new MockHttpSession();
        assertThat(controller.loginForControl("unknown", invalidSession)).isEqualTo("error");
        assertThat(invalidSession.getAttribute("after_oauth_redirect")).isNull();
    }

}
