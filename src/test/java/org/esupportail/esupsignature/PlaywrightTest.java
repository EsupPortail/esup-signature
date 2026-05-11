package org.esupportail.esupsignature;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import org.esupportail.esupsignature.playwright.PlaywrightFailureTracker;
import org.esupportail.esupsignature.playwright.PlaywrightTestSupport;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PlaywrightTest {

    private static final Logger logger = LoggerFactory.getLogger(PlaywrightTest.class);
    private static final String APP_URL = "http://localhost:7070";
    private static final String PRIMARY_USERNAME = "0";
    private static final String PRIMARY_DISPLAY_NAME = "Test User";
    private static final String PRIMARY_EMAIL = "0@example.org";
    private static final String SECONDARY_USERNAME = "testadmin";
    private static final String SECONDARY_DISPLAY_NAME = "Admin User";
    private static final String SECONDARY_EMAIL = "testadmin@example.org";
    private static final String DEFAULT_TEST_PASSWORD = "password";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final int BROWSER_WIDTH = 1920;
    private static final int BROWSER_HEIGHT = 1080;

    @RegisterExtension
    final PlaywrightFailureTracker failureTracker = new PlaywrightFailureTracker();

    private PlaywrightTestSupport playwrightSupport;
    private Page page;

    @BeforeEach
    public void setUp(TestInfo testInfo) {
        boolean portAvailable = isPortAvailable(7070);
        if (portAvailable) {
            logger.info("Starting application for Playwright tests on port 7070");
            SpringApplication.run(
                    EsupSignatureApplication.class,
                    "--server.port=7070",
                    "--spring.config.location=file:" + resolveTestConfigLocation()
            );
        }
        waitForApplicationToBeReachable();
        playwrightSupport = new PlaywrightTestSupport(testInfo.getDisplayName(), BROWSER_WIDTH, BROWSER_HEIGHT, DEFAULT_TIMEOUT);
        page = playwrightSupport.newSession("default").page();
    }

    private String resolveTestConfigLocation() {
        try {
            return new ClassPathResource("application-test.yml").getFile().getAbsolutePath();
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de localiser application-test.yml pour les tests Playwright.", e);
        }
    }

    private void waitForApplicationToBeReachable() {
        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        while (System.nanoTime() < deadline) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("127.0.0.1", 7070), 1_000);
                return;
            } catch (IOException e) {
                sleep(Duration.ofMillis(500));
            }
        }
        Assertions.fail("L'application n'a pas démarré sur le port 7070 dans le délai imparti.");
    }

    private void openUrl(Page currentPage, String url) {
        RuntimeException lastException = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                currentPage.navigate(url);
                waitForUiToSettle(currentPage);
                return;
            } catch (PlaywrightException e) {
                lastException = e;
                if (e.getMessage() == null || (!e.getMessage().contains("ERR_CONNECTION_REFUSED") && !e.getMessage().contains("ECONNREFUSED"))) {
                    throw e;
                }
                waitForApplicationToBeReachable();
                sleep(Duration.ofSeconds(1));
            }
        }
        throw lastException;
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interruption inattendue pendant l'attente du test.", e);
        }
    }

    private void waitForDocumentReady(Page currentPage) {
        currentPage.waitForFunction("() => document.readyState === 'complete'");
    }

    private void waitForToastBackdropToDisappear(Page currentPage) {
        currentPage.waitForFunction(
                "() => { const backdrop = document.getElementById('toast-backdrop'); return !backdrop || !backdrop.classList.contains('backdrop'); }"
        );
    }

    private void waitForUiToSettle(Page currentPage) {
        waitForDocumentReady(currentPage);
        waitForToastBackdropToDisappear(currentPage);
    }

    private void safeClick(Page currentPage, String selector) {
        RuntimeException lastException = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                currentPage.waitForSelector(selector);
                waitForUiToSettle(currentPage);
                currentPage.locator(selector).first().scrollIntoViewIfNeeded();
                try {
                    currentPage.click(selector);
                } catch (PlaywrightException e) {
                    currentPage.evalOnSelector(selector, "element => element.click()");
                }
                waitForUiToSettle(currentPage);
                return;
            } catch (PlaywrightException e) {
                lastException = e;
                waitForUiToSettle(currentPage);
            }
        }
        throw lastException;
    }

    private void waitForWizardResult(Page currentPage, String previousUrl) {
        currentPage.waitForFunction(
                "previousUrl => window.location.href !== previousUrl || !!document.getElementById('addSignButton2')",
                previousUrl
        );
        currentPage.waitForSelector("#content");
        currentPage.waitForSelector("#addSignButton2");
        waitForUiToSettle(currentPage);
    }

    private void waitForSignLaunchReady(Page currentPage) {
        currentPage.waitForFunction(
                "() => { const button = document.getElementById('signLaunchButton'); return !!button && !button.disabled && button.offsetParent !== null; }"
        );
    }

    private void deleteAllCustomSignImages(Page currentPage) {
        String selector = "[id^='deleteSign_']";
        for (int attempt = 0; attempt < 20; attempt++) {
            int countBefore = currentPage.locator(selector).count();
            if (countBefore == 0) {
                return;
            }

            safeClick(currentPage, selector);
            safeClick(currentPage, ".bootbox-accept");
            waitForUiToSettle(currentPage);
            currentPage.waitForFunction(
                    "previousCount => document.querySelectorAll(\"[id^='deleteSign_']\").length < previousCount",
                    countBefore
            );
        }

        Assertions.fail("Toutes les signatures personnalisées n'ont pas pu être supprimées.");
    }

    private boolean isAuthenticatedUiVisible(Page currentPage) {
        return currentPage.locator("#new-self-sign").count() > 0
                || currentPage.locator("#new-fast-sign").count() > 0
                || currentPage.locator("#user-toggle").count() > 0;
    }

    private void waitForAuthenticatedUi(Page currentPage) {
        currentPage.waitForFunction(
                "() => !!document.getElementById('new-self-sign') || !!document.getElementById('new-fast-sign') || !!document.getElementById('user-toggle')"
        );
        waitForUiToSettle(currentPage);
    }

    private String getCurrentAuthenticatedDisplayName(Page currentPage) {
        if (currentPage.locator("#navbar-user-display-name").count() == 0) {
            return null;
        }
        String text = currentPage.textContent("#navbar-user-display-name");
        return text == null ? null : text.trim();
    }

    private void loginViaCasIfNeeded(Page currentPage, String username, String password, String expectedDisplayName) {
        if (isAuthenticatedUiVisible(currentPage) && (expectedDisplayName == null || expectedDisplayName.equals(getCurrentAuthenticatedDisplayName(currentPage)))) {
            return;
        }

        openUrl(currentPage, APP_URL + "/login/casentry");
        currentPage.waitForFunction(
                "() => window.location.href.includes('/cas/login') || !!document.getElementById('fm1') || !!document.getElementById('new-self-sign') || !!document.getElementById('user-toggle')"
        );

        if (isAuthenticatedUiVisible(currentPage)) {
            if (expectedDisplayName != null) {
                currentPage.waitForFunction(
                        "expectedDisplayName => { const label = document.getElementById('navbar-user-display-name'); return !!label && label.textContent && label.textContent.trim() === expectedDisplayName; }",
                        expectedDisplayName
                );
            }
            return;
        }

        currentPage.waitForSelector("#fm1");
        currentPage.fill("#username", username);
        currentPage.fill("#password", password);
        currentPage.waitForFunction("() => !!document.querySelector(\"input[name='execution']\")");
        safeClick(currentPage, "#submitBtn");
        waitForAuthenticatedUi(currentPage);
        if (expectedDisplayName != null) {
            currentPage.waitForFunction(
                    "expectedDisplayName => { const label = document.getElementById('navbar-user-display-name'); return !!label && label.textContent && label.textContent.trim() === expectedDisplayName; }",
                    expectedDisplayName
            );
        }
    }

    private void loginAs(Page currentPage, String username, String password, String expectedDisplayName) {
        openUrl(currentPage, APP_URL + "/user");
        loginViaCasIfNeeded(currentPage, username, password, expectedDisplayName);
    }

    private boolean isLogoutStateVisible(Page currentPage) {
        Object visible = currentPage.evaluate(
                "() => window.location.href.includes('/cas/logout') || " +
                        "window.location.href.includes('/logged-out') || " +
                        "document.body.innerText.includes('Vous êtes bien déconnecté') || " +
                        "!!document.getElementById('fm1') || " +
                        "window.location.href.includes('/cas/login')"
        );
        return Boolean.TRUE.equals(visible);
    }

    private void submitLogoutForm(Page currentPage, String selector) {
        currentPage.waitForSelector(selector);
        currentPage.evalOnSelector(selector,
                "button => { const form = button.closest('form'); if (form && typeof form.requestSubmit === 'function') { form.requestSubmit(); } else if (form) { form.submit(); } else { button.click(); } }");
    }

    private void logoutCurrentUser(Page currentPage) {
        if (!isAuthenticatedUiVisible(currentPage)) {
            return;
        }
        String currentUrl = currentPage.url();
        if (!currentPage.isVisible("#link-disconnect")) {
            safeClick(currentPage, "#user-toggle");
        }
        if (currentPage.locator("#link-disconnect").count() > 0) {
            submitLogoutForm(currentPage, "#link-disconnect");
        } else if (currentPage.locator("#link-disconnect2").count() > 0) {
            submitLogoutForm(currentPage, "#link-disconnect2");
        } else {
            Assertions.fail("Le bouton de déconnexion est introuvable.");
        }

        currentPage.waitForFunction(
                "currentUrl => window.location.href !== currentUrl || window.location.href.includes('/cas/logout') || window.location.href.includes('/logged-out') || document.body.innerText.includes('Vous êtes bien déconnecté') || !!document.getElementById('fm1') || window.location.href.includes('/cas/login')",
                currentUrl
        );

        if (!isLogoutStateVisible(currentPage)) {
            openUrl(currentPage, APP_URL + "/user");
            currentPage.waitForFunction(
                    "() => window.location.href.includes('/cas/logout') || window.location.href.includes('/logged-out') || document.body.innerText.includes('Vous êtes bien déconnecté') || !!document.getElementById('fm1') || window.location.href.includes('/cas/login') || (!document.getElementById('new-self-sign') && !document.getElementById('new-fast-sign') && !document.getElementById('user-toggle'))"
            );
        }

        currentPage.context().clearCookies();
        currentPage.evaluate("() => { window.localStorage.clear(); window.sessionStorage.clear(); }");
    }

    private void waitForSignCompletion(Page currentPage) {
        currentPage.waitForFunction(
                "() => document.body.innerText.includes('Vous avez signé ce document') || document.body.innerText.includes('Télécharger le document signé') || !document.getElementById('addSignButton2')"
        );
        waitForUiToSettle(currentPage);
    }

    private void waitForUserParamsPage(Page currentPage) {
        currentPage.waitForFunction("() => window.location.href.includes('/user/users')");
        currentPage.waitForSelector("#userParamsForm");
        currentPage.waitForSelector("#saveButton");
        currentPage.waitForFunction("() => !!document.getElementById('signImageBase64')");
        currentPage.waitForFunction("() => !!document.getElementById('vanilla-upload') || !!document.getElementById('canvas')");
        waitForUiToSettle(currentPage);
    }

    private void selectUserInSlimSelect(Page currentPage, String selectId, String email) {
        currentPage.waitForSelector("#" + selectId);
        currentPage.waitForFunction(
                "selectId => { const select = document.getElementById(selectId); return !!(select && select.slim); }",
                selectId
        );

        String result = (String) currentPage.evaluate("""
                ([selectId, email]) => new Promise(resolve => {
                    const select = document.getElementById(selectId);
                    if (!select || !select.slim) {
                        resolve(`ERROR|SlimSelect introuvable pour ${selectId}`);
                        return;
                    }
                    fetch('/user/users/search-user?searchString=' + encodeURIComponent(email), { credentials: 'same-origin' })
                        .then(response => response.ok ? response.json() : Promise.reject(new Error('HTTP ' + response.status)))
                        .then(json => {
                            const options = (json || []).map(user => {
                                const mail = user.mail;
                                const displayName = user.displayName;
                                let text = mail;
                                if (displayName) {
                                    text = displayName !== mail ? `${displayName} (${mail})` : displayName;
                                }
                                return { text, value: mail };
                            });
                            const matchingOption = options.find(option => option.value === email);
                            if (!matchingOption) {
                                resolve(`ERROR|Utilisateur introuvable pour ${email}`);
                                return;
                            }
                            select.slim.setData(options);
                            select.slim.setSelected(email);
                            resolve('OK|' + select.slim.getSelected().join(','));
                        })
                        .catch(error => resolve('ERROR|' + (error && error.message ? error.message : String(error))));
                })
                """, List.of(selectId, email));

        if (result == null || !result.startsWith("OK|")) {
            Assertions.fail("La sélection du destinataire a échoué pour " + email + " : " + result);
        }

        currentPage.waitForFunction(
                "([selectId, email]) => { const select = document.getElementById(selectId); return !!(select && select.slim && select.slim.getSelected().includes(email)); }",
                List.of(selectId, email)
        );
        waitForUiToSettle(currentPage);
    }

    private File createTempSignatureImage() throws IOException {
        BufferedImage image = new BufferedImage(600, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 600, 300);
        graphics.setColor(Color.BLACK);
        graphics.setStroke(new BasicStroke(8f));
        graphics.drawLine(50, 220, 180, 120);
        graphics.drawLine(180, 120, 320, 210);
        graphics.drawLine(320, 210, 520, 80);
        graphics.dispose();

        File file = File.createTempFile("playwright-signature-", ".png");
        ImageIO.write(image, "png", file);
        file.deleteOnExit();
        return file;
    }

    private Path dummyPdfPath() throws IOException {
        return new ClassPathResource("/dummy.pdf").getFile().toPath();
    }

    private String extractCurrentSignRequestId(Page currentPage) {
        String currentUrl = currentPage.url();
        return currentUrl.substring(currentUrl.lastIndexOf('/') + 1);
    }

    @AfterEach
    public void tearDown() {
        try {
            if (playwrightSupport != null) {
                playwrightSupport.close(failureTracker.getFailure());
            }
        } finally {
            failureTracker.clearFailure();
        }
    }

    @Test
    @Order(1)
    public void esupSignatureTitle() {
        openUrl(page, APP_URL);
        assertEquals("Esup Signature", page.title());
    }

    @Test
    @Order(2)
    public void esupSignatureAutoSignImage() throws IOException {
        loginAs(page, PRIMARY_USERNAME, DEFAULT_TEST_PASSWORD, PRIMARY_DISPLAY_NAME);

        safeClick(page, "#new-self-sign");
        page.setInputFiles("#multipartFiles", dummyPdfPath());

        String previousUrl = page.url();
        safeClick(page, "#fast-sign-button");
        waitForWizardResult(page, previousUrl);
        page.waitForSelector("#page_1");

        safeClick(page, "#addSignButton2");
        String signRequestId = extractCurrentSignRequestId(page);
        String signBookId = page.getAttribute("#content", "data-es-signbook-id");

        waitForSignLaunchReady(page);
        safeClick(page, "#signLaunchButton");
        page.waitForSelector("#link-dashboard");

        safeClick(page, "#link-dashboard");
        page.waitForSelector("#signbook-" + signBookId);

        safeClick(page, "#checkbox-signrequest-" + signRequestId);
        page.waitForSelector("#deleteMultipleButton");

        safeClick(page, "#deleteMultipleButton");
        safeClick(page, ".bootbox-accept");
        page.waitForFunction("signBookId => !document.getElementById('signbook-' + signBookId)", signBookId);
    }

    @Test
    @Order(3)
    public void esupSignatureFastSignRequest() throws IOException {
        loginAs(page, PRIMARY_USERNAME, DEFAULT_TEST_PASSWORD, PRIMARY_DISPLAY_NAME);

        safeClick(page, "#new-fast-sign");
        page.setInputFiles("#multipartFiles", dummyPdfPath());

        selectUserInSlimSelect(page, "recipientsEmails-1", PRIMARY_EMAIL);

        String previousUrl = page.url();
        safeClick(page, "#send-pending-button");
        waitForWizardResult(page, previousUrl);
        page.waitForSelector("#page_1");

        safeClick(page, "#addSignButton2");
        String signRequestId = extractCurrentSignRequestId(page);
        String signBookId = page.getAttribute("#content", "data-es-signbook-id");

        waitForSignLaunchReady(page);
        safeClick(page, "#signLaunchButton");
        page.waitForSelector("#link-dashboard");

        safeClick(page, "#link-dashboard");
        page.waitForSelector("#signbook-" + signBookId);

        safeClick(page, "#checkbox-signrequest-" + signRequestId);
        page.waitForSelector("#deleteMultipleButton");

        safeClick(page, "#deleteMultipleButton");
        safeClick(page, ".bootbox-accept");
        page.waitForFunction("signBookId => !document.getElementById('signbook-' + signBookId)", signBookId);
    }

    @Test
    @Order(4)
    public void esupSignatureCreateSignImage() throws IOException {
        loginAs(page, PRIMARY_USERNAME, DEFAULT_TEST_PASSWORD, PRIMARY_DISPLAY_NAME);

        openUrl(page, APP_URL + "/user/users");
        waitForUserParamsPage(page);

        if (page.locator("[id^='deleteSign_']").count() > 0) {
            deleteAllCustomSignImages(page);
            page.waitForFunction("() => document.querySelectorAll(\"[id^='deleteSign_']\").length === 0");
        }

        page.setInputFiles("#vanilla-upload", createTempSignatureImage().toPath());
        page.waitForFunction(
                "() => { const input = document.getElementById('signImageBase64'); return !!input && !!input.value && input.value.trim().length > 0; }"
        );

        safeClick(page, "#saveButton");
        waitForUiToSettle(page);
        page.waitForFunction("() => document.querySelectorAll(\"[id^='deleteSign_']\").length > 0");

        deleteAllCustomSignImages(page);
        page.waitForFunction("() => document.querySelectorAll(\"[id^='deleteSign_']\").length === 0");
    }

    @Test
    @Order(5)
    public void esupSignatureFastSignRequestBetweenTwoUsers() throws IOException {
        loginAs(page, PRIMARY_USERNAME, DEFAULT_TEST_PASSWORD, PRIMARY_DISPLAY_NAME);

        safeClick(page, "#new-fast-sign");
        page.setInputFiles("#multipartFiles", dummyPdfPath());

        selectUserInSlimSelect(page, "recipientsEmails-1", SECONDARY_EMAIL);

        String previousUrl = page.url();
        safeClick(page, "#send-pending-button");
        page.waitForFunction(
                "previousUrl => window.location.href !== previousUrl || !document.getElementById('send-pending-button')",
                previousUrl
        );

        openUrl(page, APP_URL + "/user/signbooks");
        page.waitForSelector("tr[id^='signbook-']");
        String signBookId = page.getAttribute("tr[id^='signbook-']", "id").replace("signbook-", "");
        String signRequestId = page.getAttribute("tr[id^='signbook-'] input[id^='checkbox-signrequest-']", "id")
                .replace("checkbox-signrequest-", "");
        logoutCurrentUser(page);

        PlaywrightTestSupport.UiSession secondarySession = playwrightSupport.newSession("secondary");
        Page secondaryPage = secondarySession.page();
        loginAs(secondaryPage, SECONDARY_USERNAME, DEFAULT_TEST_PASSWORD, SECONDARY_DISPLAY_NAME);
        openUrl(secondaryPage, APP_URL + "/user/signrequests/" + signRequestId);
        secondaryPage.waitForSelector("#addSignButton2");
        secondaryPage.waitForSelector("#page_1");

        safeClick(secondaryPage, "#addSignButton2");
        waitForSignLaunchReady(secondaryPage);
        safeClick(secondaryPage, "#signLaunchButton");
        waitForSignCompletion(secondaryPage);
        logoutCurrentUser(secondaryPage);

        PlaywrightTestSupport.UiSession cleanupSession = playwrightSupport.newSession("cleanup-primary");
        Page cleanupPage = cleanupSession.page();
        loginAs(cleanupPage, PRIMARY_USERNAME, DEFAULT_TEST_PASSWORD, PRIMARY_DISPLAY_NAME);
        openUrl(cleanupPage, APP_URL + "/user/signbooks");
        cleanupPage.waitForSelector("#signbook-" + signBookId);

        safeClick(cleanupPage, "#checkbox-signrequest-" + signRequestId);
        cleanupPage.waitForSelector("#deleteMultipleButton");
        safeClick(cleanupPage, "#deleteMultipleButton");
        safeClick(cleanupPage, ".bootbox-accept");
        cleanupPage.waitForFunction("signBookId => !document.getElementById('signbook-' + signBookId)", signBookId);
    }

    private boolean isPortAvailable(int port) {
        try (ServerSocket ignored = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}

