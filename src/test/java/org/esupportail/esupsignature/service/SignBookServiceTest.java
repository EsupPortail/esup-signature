package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.Action;
import org.esupportail.esupsignature.entity.LiveWorkflow;
import org.esupportail.esupsignature.entity.LiveWorkflowStep;
import org.esupportail.esupsignature.entity.Log;
import org.esupportail.esupsignature.entity.Otp;
import org.esupportail.esupsignature.entity.Recipient;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.ArchiveStatus;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.UiParams;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.dto.ws.RecipientWsDto;
import org.esupportail.esupsignature.service.security.otp.OtpService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SignBookServiceTest {

    @Test
    void addsSavedWorkflowToFavoritesOnceWhenItContainsSeveralSteps() {
        User user = new User();
        user.setEppn("creator");

        LiveWorkflow liveWorkflow = new LiveWorkflow();
        liveWorkflow.getLiveWorkflowSteps().add(new LiveWorkflowStep());
        liveWorkflow.getLiveWorkflowSteps().add(new LiveWorkflowStep());
        SignBook signBook = new SignBook();
        signBook.setLiveWorkflow(liveWorkflow);

        Workflow workflow = new Workflow();
        workflow.setId(42L);
        UserService userService = mock(UserService.class);
        WorkflowService workflowService = mock(WorkflowService.class);
        WorkflowStepService workflowStepService = mock(WorkflowStepService.class);
        when(userService.getByEppn("creator")).thenReturn(user);
        when(workflowService.createWorkflow("Circuit", "Circuit", user, null)).thenReturn(workflow);
        when(workflowStepService.createWorkflowStep(any(LiveWorkflowStep.class), any(RecipientWsDto[].class)))
                .thenReturn(new WorkflowStep());

        SignBookService service = mock(SignBookService.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(service, "userService", userService);
        ReflectionTestUtils.setField(service, "workflowService", workflowService);
        ReflectionTestUtils.setField(service, "workflowStepService", workflowStepService);
        doReturn(signBook).when(service).getById(10L);

        service.saveSignBookAsWorkflow(10L, "Circuit", "Circuit", "creator");

        verify(userService, times(1)).toggleFavorite("creator", 42L, UiParams.favoriteWorkflows);
    }

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
        when(signRequestService.pendingSignRequest(any(SignRequest.class), eq("creator"))).thenAnswer(invocation -> {
            SignRequest signRequest = invocation.getArgument(0);
            signRequest.getRecipientHasSigned().put(systemRecipient, new Action());
            return true;
        });
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
    }

    private SignRequestParams signRequestParams(int page, int x, int y) {
        SignRequestParams params = new SignRequestParams();
        params.setSignPageNumber(page);
        params.setxPos(x);
        params.setyPos(y);
        return params;
    }

    private SignRequest signRequest(Long id, SignBook signBook) {
        SignRequest signRequest = new SignRequest();
        signRequest.setId(id);
        signRequest.setStatus(SignRequestStatus.draft);
        signRequest.setParentSignBook(signBook);
        return signRequest;
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
