package org.esupportail.esupsignature.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateCspSafetyTest {

    private static final Pattern INLINE_CONFIRM_HANDLER = Pattern.compile(
            "(?is)(?:th:)?on[a-z]+\\s*=\\s*(?:\"[^\"]*\\bconfirm\\s*\\(|'[^']*\\bconfirm\\s*\\()"
    );

    @Test
    void confirmationsShouldNotUseInlineEventHandlers() throws IOException {
        Path templatesDirectory = Path.of("src/main/resources/templates");
        List<String> violations = new ArrayList<>();

        try(var paths = Files.walk(templatesDirectory)) {
            paths.filter(path -> path.toString().endsWith(".html"))
                    .forEach(path -> findInlineConfirmHandler(path, violations));
        }

        assertTrue(violations.isEmpty(),
                "Les confirmations inline sont bloquées par la CSP et laissent le formulaire partir sans confirmation : "
                        + String.join(", ", violations));
    }

    private void findInlineConfirmHandler(Path path, List<String> violations) {
        try {
            String template = Files.readString(path);
            if(INLINE_CONFIRM_HANDLER.matcher(template).find()) {
                violations.add(path.toString());
            }
        } catch(IOException e) {
            throw new IllegalStateException("Impossible de lire le template " + path, e);
        }
    }
}
