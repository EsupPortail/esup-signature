package org.esupportail.esupsignature.entity.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.esupportail.esupsignature.entity.Action;
import org.esupportail.esupsignature.entity.Recipient;

import java.io.IOException;
import java.util.Map;

public class RecipientActionMapSerializer extends JsonSerializer<Map<Recipient, Action>> {
    @Override
    public void serialize(Map<Recipient, Action> map, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        for (Map.Entry<Recipient, Action> entry : map.entrySet()) {
            jsonGenerator.writeObjectField("recipient", entry.getKey());
            jsonGenerator.writeObjectField("action", entry.getValue());
        }
        jsonGenerator.writeEndObject();
    }

    @Override
    public Class<Map<Recipient, Action>> handledType() {
        return (Class<Map<Recipient, Action>>) (Class) Map.class;
    }
}