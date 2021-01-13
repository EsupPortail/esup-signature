package org.esupportail.esupsignature.service.event;

import ch.rasc.sse.eventbus.SseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.esupportail.esupsignature.web.controller.ws.json.JsonMessage;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class EventService {

    private Map<String, String> sseClientIds = new HashMap<>();

    private final ApplicationEventPublisher applicationEventPublisher;

    public EventService(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public Map<String, String> getSseClientIds() {
        return sseClientIds;
    }

    public String getClientIdByEppn(String eppn) {
        return sseClientIds.get(eppn.split("@")[0]);
    }

    public void publishEvent(JsonMessage jsonMessage, String channel, String id) {
        ObjectMapper mapper = new ObjectMapper();
        String jsonMessageString= "";
        try {
            jsonMessageString = mapper.writeValueAsString(jsonMessage);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        byte[] bytes = jsonMessageString.getBytes(StandardCharsets.UTF_8);

        String isoEncodedString = new String(bytes, StandardCharsets.ISO_8859_1);

        if(id != null) {
            applicationEventPublisher.publishEvent(SseEvent.builder().event(channel).addClientId(id).data(isoEncodedString).build());
        } else {
            applicationEventPublisher.publishEvent(SseEvent.builder().event(channel).data(isoEncodedString).build());
        }
    }
}
