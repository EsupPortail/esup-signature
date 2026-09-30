package org.esupportail.esupsignature.service.event;

import org.esupportail.esupsignature.entity.LiveWorkflowStep;
import org.esupportail.esupsignature.repository.LiveWorkflowStepRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AutoSignFailedEventListener {

    private final LiveWorkflowStepRepository liveWorkflowStepRepository;

    public AutoSignFailedEventListener(LiveWorkflowStepRepository liveWorkflowStepRepository) {
        this.liveWorkflowStepRepository = liveWorkflowStepRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK, fallbackExecution = true)
    public void markAutoSignAsFailed(AutoSignFailedEvent event) {
        if(event.getLiveWorkflowStepId() == null) {
            return;
        }
        LiveWorkflowStep liveWorkflowStep = liveWorkflowStepRepository.findById(event.getLiveWorkflowStepId()).orElse(null);
        if(liveWorkflowStep != null) {
            liveWorkflowStep.setAutoSignStatus(false);
        }
    }
}
