package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.service.utils.WebUtilsService;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.LiveWorkflow;
import org.esupportail.esupsignature.entity.LiveWorkflowStep;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.enums.ArchiveStatus;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class SignRequestDownloadTest {

    private SignRequestService service;
    private SignRequest signRequest;
    private Workflow workflow;

    @BeforeEach
    void setUp() {
        service = mock(SignRequestService.class, CALLS_REAL_METHODS);
        workflow = new Workflow();
        workflow.setForbidDownloadsBeforeEnd(true);
        LiveWorkflow liveWorkflow = mock(LiveWorkflow.class);
        when(liveWorkflow.getWorkflow()).thenReturn(workflow);
        when(liveWorkflow.getCurrentStepNumber()).thenReturn(3);
        when(liveWorkflow.getLiveWorkflowSteps()).thenReturn(List.of(
                new LiveWorkflowStep(), new LiveWorkflowStep(), new LiveWorkflowStep()));
        SignBook signBook = new SignBook();
        signBook.setLiveWorkflow(liveWorkflow);
        signBook.setArchiveStatus(ArchiveStatus.none);
        signRequest = new SignRequest();
        signRequest.setId(42L);
        signRequest.setParentSignBook(signBook);
        signRequest.setStatus(SignRequestStatus.pending);
        signRequest.setArchiveStatus(ArchiveStatus.none);
        doReturn(signRequest).when(service).getById(42L);
    }

    @ParameterizedTest
    @EnumSource(value = SignRequestStatus.class, names = {"pending", "signed", "checked", "draft"})
    void blocksDownloadsBeforeWorkflowEnd(SignRequestStatus status) {
        signRequest.setStatus(status);

        assertThat(service.canDownloadDocument(signRequest)).isFalse();
        assertThat(service.canDownloadLayeredPdfAtStep(signRequest, 1)).isFalse();
        assertThat(service.canDownloadLayeredPdfAtStep(signRequest, 2)).isFalse();
    }

    @Test
    void blocksAllDownloadServicesBeforeReadingDocuments() {
        assertForbidden(() -> service.getToSignFileResponse(42L, "attachment", new MockHttpServletResponse(), false));
        assertForbidden(() -> service.getToSignFileResponse(42L, "inline", new MockHttpServletResponse(), false));
        assertForbidden(() -> service.getLayeredPdfAtStep(42L, 2));
        assertForbidden(() -> service.getDocumentFromArchive(42L, 1));
        assertForbidden(() -> service.getSignedFileAndReportResponse(42L, null, new MockHttpServletResponse(), false));
    }

    @Test
    void allowsOnlyPreviousStepsWhenDownloadsAreEnabled() {
        workflow.setForbidDownloadsBeforeEnd(false);

        assertThat(service.canDownloadDocument(signRequest)).isTrue();
        assertThat(service.canDownloadLayeredPdfAtStep(signRequest, 1)).isTrue();
        assertThat(service.canDownloadLayeredPdfAtStep(signRequest, 2)).isTrue();
        assertThat(service.canDownloadLayeredPdfAtStep(signRequest, 0)).isFalse();
        assertThat(service.canDownloadLayeredPdfAtStep(signRequest, 3)).isFalse();
        assertThat(service.canDownloadLayeredPdfAtStep(signRequest, 4)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = SignRequestStatus.class, names = {"completed", "exported", "refused", "archived", "cleaned"})
    void allowsDownloadsAfterWorkflowEnd(SignRequestStatus status) {
        signRequest.setStatus(status);

        assertThat(service.canDownloadDocument(signRequest)).isTrue();
        assertThat(service.canDownloadLayeredPdfAtStep(signRequest, 1)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"signer@example.org", "external-signer@external"})
    void consultationRightsDoNotBypassDownloadRestriction(String userEppn) {
        PreAuthorizeService authorization = mock(PreAuthorizeService.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(authorization, "signRequestService", service);
        doReturn(true).when(authorization).signRequestView(42L, userEppn, userEppn);

        assertThat(authorization.signRequestLayeredDownload(42L, 2, userEppn, userEppn)).isFalse();
        signRequest.setStatus(SignRequestStatus.completed);
        assertThat(authorization.signRequestLayeredDownload(42L, 2, userEppn, userEppn)).isTrue();
    }

    @Test
    void preservesExplicitPreviewAndManagerAccess() throws Exception {
        WebUtilsService webUtilsService = mock(WebUtilsService.class);
        ReflectionTestUtils.setField(service, "webUtilsService", webUtilsService);
        Document document = mock(Document.class);
        ByteArrayInputStream content = new ByteArrayInputStream(new byte[]{1, 2, 3});
        when(document.getFileName()).thenReturn("document.pdf");
        when(document.getContentType()).thenReturn("application/pdf");
        when(document.getInputStream()).thenReturn(content);
        doReturn(List.of(document)).when(service).getToSignDocuments(42L);
        MockHttpServletResponse previewResponse = new MockHttpServletResponse();
        MockHttpServletResponse managerResponse = new MockHttpServletResponse();

        service.getToSignFileResponse(42L, "inline", previewResponse, true);
        service.getToSignFileResponse(42L, "attachment", managerResponse, true);

        verify(webUtilsService).copyFileStreamToHttpResponse("document.pdf", "application/pdf", "inline", content, previewResponse);
        verify(webUtilsService).copyFileStreamToHttpResponse("document.pdf", "application/pdf", "attachment", content, managerResponse);
    }

    @Test
    void handlesMissingWorkflowAndNullableOption() {
        workflow.setForbidDownloadsBeforeEnd(null);
        assertThat(service.canDownloadDocument(signRequest)).isTrue();
        when(signRequest.getParentSignBook().getLiveWorkflow().getWorkflow()).thenReturn(null);
        assertThat(service.canDownloadDocument(signRequest)).isTrue();
        assertThat(service.canDownloadDocument(null)).isFalse();
    }

    private void assertForbidden(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOfSatisfying(ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }
}
