package org.esupportail.esupsignature.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalPropertiesTest {
    @Test
    void emailTransmissionIsEnabledAndUsesTheUserByDefault() {
        GlobalProperties properties = new GlobalProperties();
        assertThat(properties.getEnableSendByEmail()).isTrue();
        assertThat(properties.getSendByEmailFromUser()).isTrue();
        org.esupportail.esupsignature.dto.ui.global.UiGlobalPropertiesDto ui =
                org.esupportail.esupsignature.dto.ui.global.UiGlobalPropertiesDto.fromGlobalProperties(properties);
        assertThat(ui.getEnableSendByEmail()).isTrue();
        assertThat(ui.getSendByEmailFromUser()).isTrue();
    }

    @Test
    void emailTransmissionFlagsBindAndReachTheUi() {
        org.springframework.boot.context.properties.bind.Binder binder = new org.springframework.boot.context.properties.bind.Binder(
                new org.springframework.boot.context.properties.source.MapConfigurationPropertySource(java.util.Map.of(
                        "global.enable-send-by-email", "false", "global.send-by-email-from-user", "false")));
        GlobalProperties properties = binder.bind("global", GlobalProperties.class).get();
        assertThat(properties.getEnableSendByEmail()).isFalse();
        assertThat(properties.getSendByEmailFromUser()).isFalse();
        org.esupportail.esupsignature.dto.ui.global.UiGlobalPropertiesDto ui =
                org.esupportail.esupsignature.dto.ui.global.UiGlobalPropertiesDto.fromGlobalProperties(properties);
        assertThat(ui.getEnableSendByEmail()).isFalse();
        assertThat(ui.getSendByEmailFromUser()).isFalse();
    }

}
