package org.esupportail.esupsignature.service.security.otp;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.Otp;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.repository.OtpRepository;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.mail.MailService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OtpServiceTest {

    @Test
    void generatesAndSendsOtpForTheRequestedExternalUser() {
        User firstExternalUser = externalUser(10L, "first@example.org");
        User secondExternalUser = externalUser(20L, "second@example.org");
        SignBook signBook = new SignBook();
        signBook.setId(42L);
        signBook.setStatus(SignRequestStatus.pending);

        SignBookRepository signBookRepository = mock(SignBookRepository.class);
        OtpRepository otpRepository = mock(OtpRepository.class);
        MailService mailService = mock(MailService.class);
        UserService userService = mock(UserService.class);
        GlobalProperties globalProperties = mock(GlobalProperties.class);
        when(globalProperties.getOtpValidity()).thenReturn(15);
        when(globalProperties.getSmsRequired()).thenReturn(false);
        when(signBookRepository.findById(42L)).thenReturn(Optional.of(signBook));
        when(userService.getById(10L)).thenReturn(firstExternalUser);
        when(userService.getById(20L)).thenReturn(secondExternalUser);

        OtpService service = new OtpService(signBookRepository, otpRepository, mailService, userService, globalProperties, null);
        service.generateOtpForSignRequest(42L, 10L, null, true);
        Otp secondOtp = service.generateOtpForSignRequest(42L, 20L, null, true);

        ArgumentCaptor<Otp> sentOtpCaptor = ArgumentCaptor.forClass(Otp.class);
        verify(mailService, times(2)).sendOtp(sentOtpCaptor.capture(), same(signBook), eq(true));
        assertThat(sentOtpCaptor.getAllValues())
                .extracting(Otp::getUser)
                .containsExactly(firstExternalUser, secondExternalUser);
        assertThat(secondOtp.getUser()).isSameAs(secondExternalUser);
        assertThat(secondOtp.getUser().getEmail()).isEqualTo("second@example.org");
    }

    @Test
    void generatesOtpForReplayWithoutSendingTheStandardOtpEmail() {
        User externalUser = externalUser(10L, "external@example.org");
        SignBook signBook = new SignBook();
        signBook.setId(42L);
        signBook.setStatus(SignRequestStatus.pending);

        SignBookRepository signBookRepository = mock(SignBookRepository.class);
        OtpRepository otpRepository = mock(OtpRepository.class);
        MailService mailService = mock(MailService.class);
        UserService userService = mock(UserService.class);
        GlobalProperties globalProperties = mock(GlobalProperties.class);
        when(globalProperties.getOtpValidity()).thenReturn(15);
        when(globalProperties.getSmsRequired()).thenReturn(false);
        when(signBookRepository.findById(42L)).thenReturn(Optional.of(signBook));
        when(userService.getById(10L)).thenReturn(externalUser);

        OtpService service = new OtpService(signBookRepository, otpRepository, mailService, userService, globalProperties, null);
        Otp otp = service.generateOtpForReplay(42L, 10L, null);

        assertThat(otp.getUser()).isSameAs(externalUser);
        assertThat(otp.getUrlId()).isNotBlank();
        verify(otpRepository).save(otp);
        verify(mailService, never()).sendOtp(any(Otp.class), same(signBook), eq(true));
    }

    private User externalUser(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEppn(email);
        user.setEmail(email);
        user.setUserType(UserType.external);
        return user;
    }
}
