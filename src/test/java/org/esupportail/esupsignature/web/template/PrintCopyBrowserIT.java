package org.esupportail.esupsignature.web.template;

import com.microsoft.playwright.BrowserContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PrintCopyBrowserIT {
    private static Playwright playwright;
    private static Browser browser;

    @BeforeAll
    static void startBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
    }

    @AfterAll
    static void closeBrowser() {
        try {
            if (browser != null) browser.close();
        } finally {
            if (playwright != null) playwright.close();
        }
    }

    @Test
    public void explanationOpensOutsideTheCompactActionBar() throws Exception {
        Document actions = Jsoup.parse(Files.readString(Path.of(
                "src/main/resources/templates/user/signrequests/includes/show-sections.html")));
        Element button = actions.selectFirst("#es-id-print-download-warning").clone();
        assertEquals("", button.text());
        assertTrue(actions.select("#es-id-sign-buttons #es-print-copy-explanation").isEmpty());
        Element modal = Jsoup.parse(Files.readString(Path.of(
                "src/main/resources/templates/user/signrequests/includes/modals/show-modals.html")))
                .selectFirst("#es-print-copy-modal").clone();
        modal.selectFirst("#es-print-copy-title").text("Printing copy");
        modal.selectFirst("#es-print-copy-explanation").text("Use the signed original to verify integrity.");
        modal.selectFirst("#es-print-copy-download").attr("href", "/ws-secure/global/print-with-code/42").text("Download");
        try (BrowserContext browserContext = browser.newContext()) {
            for (int width : new int[]{768, 1440}) {
                try (Page page = browserContext.newPage()) {
                    page.setViewportSize(width, 900);
                    page.setContent("<html><head></head><body><aside id='es-id-sign-buttons' style='width:68px;height:400px;overflow:auto'>"
                            + button.outerHtml() + "</aside>" + modal.outerHtml() + "</body></html>");
                    page.addStyleTag(new Page.AddStyleTagOptions().setContent(resource("bootstrap/5.3.8/css/bootstrap.min.css")));
                    page.addScriptTag(new Page.AddScriptTagOptions().setContent(resource("bootstrap/5.3.8/js/bootstrap.bundle.min.js")));
                    assertTrue((Boolean) page.locator("#es-id-sign-buttons").evaluate("el => el.scrollWidth === el.clientWidth"));
                    page.locator("#es-id-print-download-warning").click();
                    assertThat(page.locator("#es-print-copy-modal")).isVisible();
                    assertThat(page.locator("#es-print-copy-explanation")).isVisible();
                    assertThat(page.locator("#es-print-copy-download")).hasAttribute("href", "/ws-secure/global/print-with-code/42");
                    page.locator("#es-print-copy-modal .btn-close").click();
                    assertThat(page.locator("#es-print-copy-modal")).isHidden();
                }
            }
        }
    }

    @Test
    public void emailTransmissionKeepsTheActionBarCompactAndRequiresAnEmail() throws Exception {
        Document actions = Jsoup.parse(Files.readString(Path.of(
                "src/main/resources/templates/user/signrequests/includes/tools.html")));
        Element button = actions.selectFirst("#es-transmit-email-button").clone();
        assertEquals("es-id-nav-tools-actions", actions.selectFirst("#es-transmit-email-button").closest("#es-id-nav-tools-actions").id());
        assertTrue(button.hasClass("btn-outline-success"));
        button.selectFirst(".es-tools-secondary-label").text("Forward by email");
        Element modal = Jsoup.parse(Files.readString(Path.of(
                "src/main/resources/templates/user/signrequests/includes/modals/show-modals.html")))
                .selectFirst("#es-transmit-email-modal").clone();
        modal.selectFirst("#es-transmit-email-title").text("Forward by email");
        modal.selectFirst("#es-transmit-email-help").text("The signed original will be attached with your name.");
        try (BrowserContext browserContext = browser.newContext()) {
            for (int width : new int[]{390, 1440}) {
                try (Page page = browserContext.newPage()) {
                    page.setViewportSize(width, 900);
                    page.setContent("<html><head></head><body><div id='es-id-nav-tools-actions' class='d-flex flex-wrap gap-2'>"
                            + button.outerHtml() + "</div>" + modal.outerHtml() + "</body></html>");
                    page.addStyleTag(new Page.AddStyleTagOptions().setContent(resource("bootstrap/5.3.8/css/bootstrap.min.css")));
                    page.addScriptTag(new Page.AddScriptTagOptions().setContent(resource("bootstrap/5.3.8/js/bootstrap.bundle.min.js")));
                    assertTrue((Boolean) page.locator("#es-id-nav-tools-actions").evaluate("el => el.scrollWidth === el.clientWidth"));
                    page.locator("#es-transmit-email-button").click();
                    assertThat(page.locator("#es-transmit-email-modal")).isVisible();
                    assertEquals(false, page.locator("#es-transmit-email-form").evaluate("el => el.checkValidity()"));
                    page.locator("#es-transmit-email-recipient").fill("invalid");
                    assertEquals(false, page.locator("#es-transmit-email-form").evaluate("el => el.checkValidity()"));
                    page.locator("#es-transmit-email-recipient").fill("recipient@example.org");
                    assertEquals(false, page.locator("#es-transmit-email-comment").evaluate("el => el.required"));
                    page.locator("#es-transmit-email-comment").fill("Un commentaire\nsur deux lignes");
                    assertThat(page.locator("#es-transmit-email-comment")).hasValue("Un commentaire\nsur deux lignes");
                    assertEquals(true, page.locator("#es-transmit-email-form").evaluate("el => el.checkValidity()"));
                    page.locator("#es-transmit-email-modal .btn-close").click();
                    assertThat(page.locator("#es-transmit-email-modal")).isHidden();
                }
            }
        }
    }

    private String resource(String path) throws Exception {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("META-INF/resources/webjars/" + path)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
