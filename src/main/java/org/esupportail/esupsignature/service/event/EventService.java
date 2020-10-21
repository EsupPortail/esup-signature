package org.esupportail.esupsignature.service.event;

import ch.rasc.sse.eventbus.SseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.web.controller.ws.json.JsonMessage;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@Async
public class EventService {

    public final ApplicationEventPublisher applicationEventPublisher;

    public EventService(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publishEvent(JsonMessage jsonMessage, String channel, User user) {
        ObjectMapper mapper = new ObjectMapper();
        String jsonMessageString= "";
        try {
            jsonMessageString = mapper.writeValueAsString(jsonMessage);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        byte[] bytes = jsonMessageString.getBytes(StandardCharsets.UTF_8);

        String isoEncodedString = new String(bytes, StandardCharsets.ISO_8859_1);

        if(user != null) {
            applicationEventPublisher.publishEvent(SseEvent.builder().event(channel).addClientId(user.getEppn().split("@")[0]).data(isoEncodedString).build());
        } else {
            applicationEventPublisher.publishEvent(SseEvent.builder().event(channel).data(isoEncodedString).build());
        }
    }
}
