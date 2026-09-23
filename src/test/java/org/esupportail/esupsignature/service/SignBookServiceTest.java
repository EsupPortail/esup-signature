package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.Action;
import org.esupportail.esupsignature.entity.LiveWorkflow;
import org.esupportail.esupsignature.entity.LiveWorkflowStep;
import org.esupportail.esupsignature.entity.Log;
import org.esupportail.esupsignature.entity.Recipient;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SignBookServiceTest {

    @Test
    void initializesAllSignRequestsBeforeAutoSigningSeveralDocuments() throws Exception {
        User systemUser = new User();
        systemUser.setId(1L);
        systemUser.setEppn("system");
        systemUser.setEmail("system");
        systemUser.setUserType(UserType.system);
        Recipient systemRecipient = recipient(10L, systemUser);

        LiveWorkflowStep autoSignStep = new LiveWorkflowStep();
        autoSignStep.setAutoSign(true);
        LiveWorkflow liveWorkflow = new LiveWorkflow();
        liveWorkflow.getLiveWorkflowSteps().add(autoSignStep);
        liveWorkflow.setCurrentStep(autoSignStep);

        User creator = new User();
        creator.setId(2L);
        creator.setEppn("creator");
        SignBook signBook = new SignBook();
        signBook.setId(42L);
        signBook.setCreateBy(creator);
        signBook.setStatus(SignRequestStatus.draft);
        signBook.setLiveWorkflow(liveWorkflow);
        SignRequest firstSignRequest = signRequest(100L, signBook);
        SignRequest secondSignRequest = signRequest(200L, signBook);
        signBook.getSignRequests().add(firstSignRequest);
        signBook.getSignRequests().add(secondSignRequest);

        SignRequestService signRequestService = mock(SignRequestService.class);
        UserService userService = mock(UserService.class);
        LiveWorkflowStepService liveWorkflowStepService = mock(LiveWorkflowStepService.class);
        RecipientService recipientService = mock(RecipientService.class);
        SignRequestParamsService signRequestParamsService = mock(SignRequestParamsService.class);
        DataService dataService = mock(DataService.class);
        LogService logService = mock(LogService.class);
        when(userService.getSystemUser()).thenReturn(systemUser);
        when(userService.getByEppn("creator")).thenReturn(creator);
        when(userService.getSchedulerUser()).thenReturn(creator);
        when(recipientService.createRecipient(systemUser)).thenReturn(systemRecipient);
        doAnswer(invocation -> {
            LiveWorkflowStep step = invocation.getArgument(0);
            step.getRecipients().add(invocation.getArgument(1));
            return null;
        }).when(liveWorkflowStepService).addRecipient(autoSignStep, systemRecipient);
        doAnswer(invocation -> {
            SignRequest signRequest = invocation.getArgument(0);
            signRequest.getRecipientHasSigned().put(systemRecipient, new Action());
            return null;
        }).when(signRequestService).pendingSignRequest(any(SignRequest.class), eq("creator"));
        when(signRequestService.isMoreWorkflowStep(signBook)).thenReturn(false);
        when(logService.create(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(new Log());

        SignBookService service = mock(SignBookService.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(service, "signRequestService", signRequestService);
        ReflectionTestUtils.setField(service, "userService", userService);
        ReflectionTestUtils.setField(service, "liveWorkflowStepService", liveWorkflowStepService);
        ReflectionTestUtils.setField(service, "recipientService", recipientService);
        ReflectionTestUtils.setField(service, "signRequestParamsService", signRequestParamsService);
        ReflectionTestUtils.setField(service, "dataService", dataService);
        ReflectionTestUtils.setField(service, "logService", logService);

        service.pendingSignBook(signBook, null, "creator", "creator", false, false);

        InOrder inOrder = inOrder(signRequestService);
        inOrder.verify(signRequestService).pendingSignRequest(firstSignRequest, "creator");
        inOrder.verify(signRequestService).pendingSignRequest(secondSignRequest, "creator");
        inOrder.verify(signRequestService).sign(firstSignRequest, "", "sealCert", "default", null, null, "system", "system", null, "", false);
        inOrder.verify(signRequestService).sign(secondSignRequest, "", "sealCert", "default", null, null, "system", "system", null, "", false);
        assertThat(firstSignRequest.getRecipientHasSigned()).containsKey(systemRecipient);
        assertThat(secondSignRequest.getRecipientHasSigned()).containsKey(systemRecipient);
        verify(recipientService).createRecipient(systemUser);
    }

    private SignRequest signRequest(Long id, SignBook signBook) {
        SignRequest signRequest = new SignRequest();
        signRequest.setId(id);
        signRequest.setStatus(SignRequestStatus.draft);
        signRequest.setParentSignBook(signBook);
        return signRequest;
    }

    private Recipient recipient(Long id, User user) {
        Recipient recipient = new Recipient();
        recipient.setId(id);
        recipient.setUser(user);
        return recipient;
    }
}
