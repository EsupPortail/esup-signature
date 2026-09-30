package org.esupportail.esupsignature.service.event;

import org.esupportail.esupsignature.service.SignBookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AutoSignRequestedEventListener {

    private static final Logger logger = LoggerFactory.getLogger(AutoSignRequestedEventListener.class);

    private final SignBookService signBookService;

    public AutoSignRequestedEventListener(SignBookService signBookService) {
        this.signBookService = signBookService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void processAutoSign(AutoSignRequestedEvent event) {
        try {
            signBookService.pendingSignBook(event.getUserEppn(), event.getAuthUserEppn(), event.getSignBookId());
        } catch (RuntimeException e) {
            logger.error("Unable to process automatic signature for sign book {}", event.getSignBookId(), e);
        }
    }
}
