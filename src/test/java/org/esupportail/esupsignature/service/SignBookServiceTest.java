package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.Action;
import org.esupportail.esupsignature.entity.LiveWorkflow;
import org.esupportail.esupsignature.entity.LiveWorkflowStep;
import org.esupportail.esupsignature.entity.Otp;
import org.esupportail.esupsignature.entity.Recipient;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.ArchiveStatus;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.service.security.otp.OtpService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SignBookServiceTest {

    @Test
    void dispatchesDetectedSignatureFieldsToSuccessiveStepsWithoutWorkflow() {
        SignRequestParams firstParam = signRequestParams(1, 10, 20);
        SignRequestParams secondParam = signRequestParams(2, 30, 40);
        secondParam.setSignDocumentNumber(7);

        LiveWorkflowStep firstStep = new LiveWorkflowStep();
        LiveWorkflowStep secondStep = new LiveWorkflowStep();
        LiveWorkflow liveWorkflow = new LiveWorkflow();
        liveWorkflow.getLiveWorkflowSteps().add(firstStep);

        SignBook signBook = new SignBook();
        signBook.setLiveWorkflow(liveWorkflow);
        SignRequest signRequest = new SignRequest();
        signRequest.setParentSignBook(signBook);
        signRequest.getSignRequestParams().add(firstParam);
        signRequest.getSignRequestParams().add(secondParam);
        signBook.getSignRequests().add(signRequest);

        SignBookService service = mock(SignBookService.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.invokeMethod(service, "dispatchSignRequestParams", signRequest);

        assertThat(firstStep.getSignRequestParams()).containsExactly(firstParam);
        assertThat(secondParam.getSignDocumentNumber()).isEqualTo(7);

        liveWorkflow.getLiveWorkflowSteps().add(secondStep);
        ReflectionTestUtils.invokeMethod(service, "dispatchSignRequestParams", signRequest);

        assertThat(firstStep.getSignRequestParams()).containsExactly(firstParam);
        assertThat(secondStep.getSignRequestParams()).containsExactly(secondParam);
        assertThat(firstParam.getSignDocumentNumber()).isZero();
        assertThat(secondParam.getSignDocumentNumber()).isZero();
    }

    @Test
    void renewsOtpForTheUserFromTheExpiredLinkWhenSeveralExternalRecipientsExist() throws Exception {
        User firstExternalUser = externalUser(10L, "first@example.org", "+33111111111");
        User secondExternalUser = externalUser(20L, "second@example.org", "+33222222222");

        Recipient firstRecipient = recipient(100L, firstExternalUser);
        Recipient secondRecipient = recipient(200L, secondExternalUser);
        SignRequest signRequest = new SignRequest();
        signRequest.setArchiveStatus(ArchiveStatus.none);
        signRequest.setDeleted(false);
        LinkedHashMap<Recipient, Action> recipientActions = new LinkedHashMap<>();
        recipientActions.put(firstRecipient, new Action());
        recipientActions.put(secondRecipient, new Action());
        signRequest.setRecipientHasSigned(recipientActions);

        SignBook signBook = new SignBook();
        signBook.setId(42L);
        signBook.getSignRequests().add(signRequest);

        Otp expiredOtp = new Otp();
        expiredOtp.setUrlId("expired-link");
        expiredOtp.setSignBook(signBook);
        expiredOtp.setUser(secondExternalUser);
        expiredOtp.setSignature(true);

        Otp renewedOtp = new Otp();
        OtpService otpService = mock(OtpService.class);
        when(otpService.getOtpFromDatabase("expired-link")).thenReturn(expiredOtp);
        when(otpService.generateOtpForSignRequest(42L, 20L, "+33222222222", true)).thenReturn(renewedOtp);

        SignBookService service = mock(SignBookService.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(service, "otpService", otpService);

        assertThat(service.renewOtp("expired-link")).isSameAs(renewedOtp);
        verify(otpService).generateOtpForSignRequest(42L, 20L, "+33222222222", true);
        verify(otpService, never()).generateOtpForSignRequest(eq(42L), eq(10L), any(), eq(true));
    }

    @Test
    void deletesPreviousUsersOtpsWhenTransferringTheCurrentStep() {
        User previousUser = externalUser(10L, "previous@example.org", "+33111111111");
        User replacementUser = externalUser(20L, "replacement@example.org", "+33222222222");
        Recipient recipient = recipient(100L, previousUser);

        LiveWorkflowStep currentStep = new LiveWorkflowStep();
        currentStep.getRecipients().add(recipient);
        LiveWorkflow liveWorkflow = new LiveWorkflow();
        liveWorkflow.getLiveWorkflowSteps().add(currentStep);
        liveWorkflow.setCurrentStep(currentStep);

        SignRequest signRequest = new SignRequest();
        signRequest.setStatus(SignRequestStatus.pending);
        SignBook signBook = new SignBook();
        signBook.setId(42L);
        signBook.setLiveWorkflow(liveWorkflow);
        signBook.getSignRequests().add(signRequest);
        signBook.getTeam().add(previousUser);

        OtpService otpService = mock(OtpService.class);
        SignBookService service = mock(SignBookService.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(service, "otpService", otpService);
        doReturn(signBook).when(service).getById(42L);

        service.transfertSignRequest(42L, false, previousUser, replacementUser, false);

        verify(otpService).deleteOtp(42L, previousUser);
        verify(otpService).generateOtpForSignRequest(42L, 20L, "+33222222222", true);
        assertThat(recipient.getUser()).isSameAs(replacementUser);
    }

    private SignRequestParams signRequestParams(int page, int x, int y) {
        SignRequestParams params = new SignRequestParams();
        params.setSignPageNumber(page);
        params.setxPos(x);
        params.setyPos(y);
        return params;
    }

    private User externalUser(Long id, String email, String phone) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPhone(phone);
        user.setUserType(UserType.external);
        return user;
    }

    private Recipient recipient(Long id, User user) {
        Recipient recipient = new Recipient();
        recipient.setId(id);
        recipient.setUser(user);
        return recipient;
    }
}
