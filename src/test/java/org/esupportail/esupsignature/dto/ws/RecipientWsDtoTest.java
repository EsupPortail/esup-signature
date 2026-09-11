package org.esupportail.esupsignature.dto.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecipientWsDtoTest {

    @Test
    void deserializationAcceptsCreatorPlaceholder() throws Exception {
        RecipientWsDto recipient = new ObjectMapper().readValue("{\"email\":\"creator\"}", RecipientWsDto.class);

        assertEquals("creator", recipient.getEmail());
    }

    @Test
    void setEmailStillRejectsInvalidEmail() {
        RecipientWsDto recipient = new RecipientWsDto();

        assertThrows(IllegalArgumentException.class, () -> recipient.setEmail("invalid"));
    }
}
