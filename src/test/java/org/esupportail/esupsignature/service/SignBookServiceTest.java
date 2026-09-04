package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.LiveWorkflow;
import org.esupportail.esupsignature.entity.LiveWorkflowStep;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

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

    private SignRequestParams signRequestParams(int page, int x, int y) {
        SignRequestParams params = new SignRequestParams();
        params.setSignPageNumber(page);
        params.setxPos(x);
        params.setyPos(y);
        return params;
    }
}
