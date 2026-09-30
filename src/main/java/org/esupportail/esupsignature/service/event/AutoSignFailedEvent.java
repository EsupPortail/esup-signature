package org.esupportail.esupsignature.service.event;

public class AutoSignFailedEvent {

    private final Long liveWorkflowStepId;

    public AutoSignFailedEvent(Long liveWorkflowStepId) {
        this.liveWorkflowStepId = liveWorkflowStepId;
    }

    public Long getLiveWorkflowStepId() {
        return liveWorkflowStepId;
    }
}
