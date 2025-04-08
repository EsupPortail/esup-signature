package org.esupportail.esupsignature.entity.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.esupportail.esupsignature.entity.Action;
import org.esupportail.esupsignature.entity.Recipient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class RecipientActionMapSerializer extends JsonSerializer<Map<Recipient, Action>> {

    private static final Logger logger = LoggerFactory.getLogger(RecipientActionMapSerializer.class);


    @Override
    public void serialize(Map<Recipient, Action> map, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject(map);
        AtomicInteger i = new AtomicInteger();
        map.entrySet().stream()
                .sorted(Comparator.comparingLong(e -> e.getValue().getId()))
                .forEach(entry -> {
                    try {
                        jsonGenerator.writeObjectField("recipient-" + i, entry.getKey());
                        jsonGenerator.writeObjectField("action-" + i, entry.getValue());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    i.getAndIncrement();
                });
        jsonGenerator.writeEndObject();
    }

    @Override
    public Class<Map<Recipient, Action>> handledType() {
        return (Class<Map<Recipient, Action>>) (Class<?>) Map.class;
    }

}