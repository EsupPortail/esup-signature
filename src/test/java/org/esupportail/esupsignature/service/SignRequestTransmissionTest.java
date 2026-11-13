package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.service.interfaces.fs.FsFile;
import org.esupportail.esupsignature.service.mail.MailService;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.ByteArrayInputStream;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SignRequestTransmissionTest {
    @Test
    void refusesAnUnfinishedDocumentBeforeOpeningItsFile() throws Exception {
        SignRequestService service = mock(SignRequestService.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(service, "globalProperties", new org.esupportail.esupsignature.config.GlobalProperties());
        StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage("signRequest.transmit.unavailable", org.springframework.context.i18n.LocaleContextHolder.getLocale(), "Unavailable");
        ReflectionTestUtils.setField(service, "messageSource", messages);
        SignRequest request = new SignRequest();
        request.setStatus(SignRequestStatus.pending);
        doReturn(request).when(service).getById(42L);
        assertThatThrownBy(() -> service.transmitSignedDocument(42L, "recipient@example.org", "actual", "Test comment"))
                .isInstanceOf(AccessDeniedException.class);
        verify(service, never()).getLastSignedFsFile(any());
    }

    @Test
    void usesTheAuthenticatedUserAndPropagatesDeliveryFailure() throws Exception {
        SignRequestService service = mock(SignRequestService.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(service, "globalProperties", new org.esupportail.esupsignature.config.GlobalProperties());
        UserService users = mock(UserService.class);
        MailService mail = mock(MailService.class);
        ReflectionTestUtils.setField(service, "userService", users);
        ReflectionTestUtils.setField(service, "mailService", mail);
        SignRequest request = new SignRequest();
        request.setStatus(SignRequestStatus.completed);
        User sender = new User();
        FsFile file = new FsFile(new ByteArrayInputStream(new byte[]{1, 2}), "signed.pdf", "application/pdf");
        doReturn(request).when(service).getById(42L);
        doReturn(true).when(service).canDownloadDocument(request);
        doReturn(file).when(service).getLastSignedFsFile(request);
        when(users.getByEppn("actual")).thenReturn(sender);
        when(mail.transmitSignedFile(request, file, "recipient@example.org", sender, "Test comment")).thenReturn(false);
        assertThat(service.transmitSignedDocument(42L, "recipient@example.org", "actual", "Test comment")).isFalse();
        verify(mail).transmitSignedFile(request, file, "recipient@example.org", sender, "Test comment");
    }
    @Test
    void disabledEmailTransmissionRejectsDirectServiceCalls() throws Exception {
        SignRequestService service = mock(SignRequestService.class, CALLS_REAL_METHODS);
        org.esupportail.esupsignature.config.GlobalProperties properties = new org.esupportail.esupsignature.config.GlobalProperties();
        properties.setEnableSendByEmail(false);
        ReflectionTestUtils.setField(service, "globalProperties", properties);
        StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage("signRequest.transmit.unavailable", org.springframework.context.i18n.LocaleContextHolder.getLocale(), "Unavailable");
        ReflectionTestUtils.setField(service, "messageSource", messages);
        SignRequest request = new SignRequest();
        request.setStatus(SignRequestStatus.completed);
        doReturn(request).when(service).getById(42L);
        assertThatThrownBy(() -> service.transmitSignedDocument(42L, "recipient@example.org", "actual", "Test comment"))
                .isInstanceOf(AccessDeniedException.class);
        verify(service, never()).getLastSignedFsFile(any());
        verify(service, never()).canDownloadDocument(any());
    }

}
