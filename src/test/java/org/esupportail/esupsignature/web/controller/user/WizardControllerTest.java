package org.esupportail.esupsignature.web.controller.user;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.sql.SQLException;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class WizardControllerTest {

    @Test
    void returnsReadableMessageForOversizedDatabaseValue() {
        StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage("wizard.save.value-too-long", Locale.FRENCH, "Valeur trop longue");
        WizardController controller = new WizardController(null, null, null, null, null, null, null, null, null, messages);
        DataIntegrityViolationException exception = new DataIntegrityViolationException("SQL details",
                new SQLException("Database details", "22001"));

        assertThat(controller.handleSaveException(exception, Locale.FRENCH).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(controller.handleSaveException(exception, Locale.FRENCH).getBody()).containsEntry("message", "Valeur trop longue");
    }

    @Test
    void hidesDatabaseDetailsForOtherFailures() {
        StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage("wizard.save.error", Locale.FRENCH, "Veuillez réessayer");
        WizardController controller = new WizardController(null, null, null, null, null, null, null, null, null, messages);

        assertThat(controller.handleSaveException(new DataIntegrityViolationException("SQL details"), Locale.FRENCH)
                .getBody()).containsEntry("message", "Veuillez réessayer");
    }
}
