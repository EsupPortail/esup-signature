package org.esupportail.esupsignature.web.template;

import org.esupportail.esupsignature.dto.audit.ReferenceSignatureDto;
import org.esupportail.esupsignature.entity.AuditTrail;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

class Dev2ControlTemplateTest {
    @Test
    void referenceDetailsStaySeparateFromVerifiedAuditInformation() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("i18n.messages");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
        engine.setTemplateEngineMessageSource(messages);
        Context context = new Context(Locale.FRENCH);
        context.setVariable("token", "known-token");
        context.setVariable("auditTrailChecked", false);
        context.setVariable("referenceAvailable", true);
        context.setVariable("error", true);
        context.setVariable("referenceDocumentName", "reference.pdf");
        context.setVariable("referenceDocumentChecksum", "reference-digest");
        ReferenceSignatureDto signature = new ReferenceSignatureDto();
        signature.setName("Martin");
        signature.setFirstname("Alice");
        signature.setEmail("alice@example.org");
        signature.setDate(new Date(0));
        context.setVariable("referenceSignatures", List.of(signature));
        context.setVariable("_csrf", Map.of("parameterName", "_csrf", "token", "test"));
        AuditTrail audit = new AuditTrail();
        audit.setDocumentName("confidential-audit.pdf");
        context.setVariable("auditTrail", audit);
        String html = engine.process("public/control", Set.of("control"), context);
        assertThat(html).contains("es-control-reference", "reference.pdf", "reference-digest", "Alice", "Martin", "alice@example.org", "1970", "multipartFile")
                .doesNotContain("confidential-audit.pdf", "es-control-login-link");
        context.setVariable("referenceAvailable", false);
        context.setVariable("controlLoginRequired", true);
        String anonymous = engine.process("public/control", Set.of("control"), context);
        assertThat(anonymous).contains("/public/control/known-token/login", "es-control-login-link")
                .doesNotContain("reference.pdf", "reference-digest", "alice@example.org", "confidential-audit.pdf");

    }
}
