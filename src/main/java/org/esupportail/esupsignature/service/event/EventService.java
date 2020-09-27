package org.esupportail.esupsignature.service.event;

import ch.rasc.sse.eventbus.SseEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.web.controller.ws.json.JsonMessage;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@Async
public class EventService {

    public final ApplicationEventPublisher eventPublisher;

    public EventService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
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
        String utf8EncodedString = new String(bytes, StandardCharsets.ISO_8859_1);
        if(user != null) {
            eventPublisher.publishEvent(SseEvent.builder().event(channel).addClientId(user.getEppn()).data(utf8EncodedString).build());
        } else {
            eventPublisher.publishEvent(SseEvent.builder().event(channel).data(utf8EncodedString).build());
        }
    }
}
