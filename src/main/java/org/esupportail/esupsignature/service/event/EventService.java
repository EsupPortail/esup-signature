package org.esupportail.esupsignature.service.event;

import ch.rasc.sse.eventbus.SseEvent;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.web.controller.ws.json.JsonMessage;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Async
public class EventService {

    public final ApplicationEventPublisher eventPublisher;


    public EventService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishEvent(JsonMessage jsonMessage) {
        this.eventPublisher.publishEvent(SseEvent.builder().event("global").data(jsonMessage).build());

    }

    public void publishEvent(JsonMessage jsonMessage, User user) {
        this.eventPublisher.publishEvent(SseEvent.builder().event("user").addClientId(user.getEppn()).data(jsonMessage).build());
    }
}
