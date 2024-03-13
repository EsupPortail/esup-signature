package org.esupportail.esupsignature.entity.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.esupportail.esupsignature.entity.Action;
import org.esupportail.esupsignature.entity.Recipient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class RecipientActionMapSerializer extends JsonSerializer<Map<Recipient, Action>> {

    private static final Logger logger = LoggerFactory.getLogger(RecipientActionMapSerializer.class);


    @Override
    public void serialize(Map<Recipient, Action> map, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject(map);
        int i = 0;
        for (Map.Entry<Recipient, Action> entry : map.entrySet()) {
            jsonGenerator.writeObjectField("recipient-" + i, entry.getKey());
            jsonGenerator.writeObjectField("action-" + i, entry.getValue());
            i++;
        }
        jsonGenerator.writeEndObject();
    }

    @Override
    public Class<Map<Recipient, Action>> handledType() {
        return (Class<Map<Recipient, Action>>) (Class<?>) Map.class;
    }

}