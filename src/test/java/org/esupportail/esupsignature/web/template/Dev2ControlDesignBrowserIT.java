package org.esupportail.esupsignature.web.template;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.esupportail.esupsignature.dto.audit.ReferenceSignatureDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

class Dev2ControlDesignBrowserIT {
    @Test
    void controlCardsMatchHomeSurfacesAndRemainUsableOnMobile() throws Exception {
        Element home = Jsoup.parse(Files.readString(Path.of("src/main/resources/templates/user/home/index.html")))
                .selectFirst("#shortcuts").clone().empty();
        home.attr("id", "es-home-design-reference").removeClass("h-100").text("Accueil");
        Context context = new Context(Locale.FRENCH);
        context.setVariable("token", "a8c36141-2447-46c5-b9f1-d59c1f56b143");
        context.setVariable("auditTrailChecked", false);
        context.setVariable("referenceAvailable", true);
        context.setVariable("referenceDocumentName", "Contrat-".repeat(20) + ".pdf");
        context.setVariable("referenceDocumentType", "application/pdf");
        context.setVariable("referenceDocumentSize", 123456L);
        context.setVariable("referenceDocumentChecksum", "0123456789abcdef".repeat(4));
        context.setVariable("_csrf", Map.of("parameterName", "_csrf", "token", "test"));
        ReferenceSignatureDto signature = new ReferenceSignatureDto();
        signature.setName("Martin");
        signature.setFirstname("Alice");
        signature.setEmail("alice.martin@example.org");
        signature.setDate(new Date(0));
        context.setVariable("referenceSignatures", List.of(signature));
        SpringTemplateEngine engine = engine();
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            for (boolean reference : new boolean[]{true, false}) {
                context.setVariable("referenceAvailable", reference);
                context.setVariable("controlLoginRequired", !reference);
                String html = engine.process("public/control", Set.of("control"), context);
                String selector = "#es-control-login";
                if (reference) {
                    selector = "#es-control-reference";
                }
                for (int width : new int[]{390, 1440}) {
                    try (Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(width, 1000))) {
                        page.route("**/*", route -> route.abort());
                        page.setContent("<html><head></head><body class='es-public-page p-2 bg-body-tertiary'><div id='es-id-content'>"
                                + html + "</div><div class='home-page mt-2'>" + home.outerHtml() + "</div></body></html>");
                        page.addStyleTag(new Page.AddStyleTagOptions().setContent(resource("META-INF/resources/webjars/bootstrap/5.3.8/css/bootstrap.min.css")));
                        page.addStyleTag(new Page.AddStyleTagOptions().setContent(Files.readString(Path.of("src/main/resources/static/css/style.css"))));
                        page.addStyleTag(new Page.AddStyleTagOptions().setContent(Files.readString(Path.of("src/main/resources/static/css/medias.css"))));
                        String properties = "el => { const s = getComputedStyle(el); return [s.backgroundColor, s.borderRadius, s.borderTopColor, s.borderTopWidth]; }";
                        assertThat(page.locator(selector).evaluate(properties))
                                .isEqualTo(page.locator("#es-home-design-reference").evaluate(properties));
                        assertThat((Boolean) page.evaluate("document.documentElement.scrollWidth <= innerWidth")).isTrue();
                        assertThat(page.locator(selector).boundingBox().width).isLessThanOrEqualTo((double) width);
                        Path screenshot = Path.of(System.getProperty("playwright.artifacts.dir", "target/playwright-artifacts"),
                                "public-control-" + reference + "-" + width + ".png");
                        Files.createDirectories(screenshot.getParent());
                        page.screenshot(new Page.ScreenshotOptions().setPath(screenshot).setFullPage(true));
                    }
                }
            }
        }
    }

    private SpringTemplateEngine engine() {
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
        return engine;
    }

    private String resource(String path) throws Exception {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
