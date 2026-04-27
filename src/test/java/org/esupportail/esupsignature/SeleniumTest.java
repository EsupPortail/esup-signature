package org.esupportail.esupsignature;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
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
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SeleniumTest {

    private static final Logger logger = LoggerFactory.getLogger(SeleniumTest.class);
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

    private WebDriver driver;
    private WebDriverWait wait;
    private JavascriptExecutor js;

    @BeforeEach
    public void setUp() {
        boolean portAvailable = isPortAvailable(7070);
        if (portAvailable) {
            SpringApplication.run(EsupSignatureApplication.class, "--server.port=7070");
        }
        initializeDriver();
        waitForApplicationToBeReachable();
    }

    private void initializeDriver() {
        System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--window-size=" + BROWSER_WIDTH + "," + BROWSER_HEIGHT);
        chromeOptions.addArguments("--window-position=0,0");
        chromeOptions.addArguments("--force-device-scale-factor=1");
        chromeOptions.addArguments("--high-dpi-support=1");
        chromeOptions.addArguments("--disable-dev-shm-usage");
        chromeOptions.addArguments("--disable-gpu");
        chromeOptions.addArguments("--disable-software-rasterizer");
        String display = System.getenv("DISPLAY");
        String headlessMode = System.getProperty("selenium.headless", System.getenv("SELENIUM_HEADLESS"));
        boolean headless = "true".equalsIgnoreCase(headlessMode)
                || ((headlessMode == null || headlessMode.isBlank()) && (display == null || display.isBlank()));
        if (headless) {
            logger.warn("Headless mode activated");
            chromeOptions.addArguments("--headless=new");
            chromeOptions.addArguments("--no-sandbox");
        }
        driver = new ChromeDriver(chromeOptions);
        wait = new WebDriverWait(driver, DEFAULT_TIMEOUT);
        js = (JavascriptExecutor) driver;
        driver.manage().window().setPosition(new Point(0, 0));
        driver.manage().window().setSize(new Dimension(BROWSER_WIDTH, BROWSER_HEIGHT));
    }

    private void restartBrowserSession() {
        if (driver != null) {
            driver.quit();
        }
        initializeDriver();
        waitForApplicationToBeReachable();
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

    private void openUrl(String url) {
        RuntimeException lastException = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                driver.get(url);
                waitForUiToSettle();
                return;
            } catch (WebDriverException e) {
                lastException = e;
                if (e.getMessage() == null || !e.getMessage().contains("ERR_CONNECTION_REFUSED")) {
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

    private void waitForDocumentReady() {
        wait.until(webDriver -> "complete".equals(js.executeScript("return document.readyState")));
    }

    private void waitForToastBackdropToDisappear() {
        wait.until(webDriver -> Boolean.TRUE.equals(js.executeScript(
                "const backdrop = document.getElementById('toast-backdrop');" +
                        "return !backdrop || !backdrop.classList.contains('backdrop');"
        )));
    }

    private void waitForUiToSettle() {
        waitForDocumentReady();
        waitForToastBackdropToDisappear();
    }

    private void scrollIntoView(WebElement element) {
        js.executeScript("arguments[0].scrollIntoView({block:'center', inline:'center'});", element);
    }

    private void safeClick(By locator) {
        RuntimeException lastException = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
                safeClick(element);
                return;
            } catch (StaleElementReferenceException | ElementClickInterceptedException | TimeoutException e) {
                lastException = e;
                waitForUiToSettle();
            }
        }
        throw lastException;
    }

    private void safeClick(WebElement element) {
        waitForUiToSettle();
        scrollIntoView(element);
        try {
            wait.until(ExpectedConditions.elementToBeClickable(element)).click();
        } catch (ElementClickInterceptedException | TimeoutException e) {
            scrollIntoView(element);
            js.executeScript("arguments[0].click();", element);
        }
    }

    private void waitForWizardResult(String previousUrl) {
        wait.until(webDriver -> !previousUrl.equals(webDriver.getCurrentUrl())
                || !webDriver.findElements(By.id("addSignButton2")).isEmpty());
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("content")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("addSignButton2")));
        waitForUiToSettle();
    }

    private void waitForSignLaunchReady() {
        wait.until(webDriver -> {
            WebElement button = webDriver.findElement(By.id("signLaunchButton"));
            return button.isDisplayed() && button.isEnabled();
        });
    }

    private void deleteAllCustomSignImages() {
        By deleteButtons = By.cssSelector("[id^='deleteSign_']");
        for (int attempt = 0; attempt < 20; attempt++) {
            List<WebElement> currentButtons = driver.findElements(deleteButtons);
            if (currentButtons.isEmpty()) {
                return;
            }

            int countBefore = currentButtons.size();
            safeClick(deleteButtons);
            safeClick(By.cssSelector(".bootbox-accept"));
            waitForUiToSettle();
            wait.until(webDriver -> webDriver.findElements(deleteButtons).size() < countBefore);
        }

        Assertions.fail("Toutes les signatures personnalisées n'ont pas pu être supprimées.");
    }

    private boolean isAuthenticatedUiVisible() {
        return !driver.findElements(By.id("new-self-sign")).isEmpty()
                || !driver.findElements(By.id("new-fast-sign")).isEmpty()
                || !driver.findElements(By.id("user-toggle")).isEmpty();
    }

    private void waitForAuthenticatedUi() {
        wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(By.id("new-self-sign")),
                ExpectedConditions.presenceOfElementLocated(By.id("new-fast-sign")),
                ExpectedConditions.presenceOfElementLocated(By.id("user-toggle"))
        ));
        waitForUiToSettle();
    }

    private String getCurrentAuthenticatedDisplayName() {
        List<WebElement> labels = driver.findElements(By.id("navbar-user-display-name"));
        if (labels.isEmpty()) {
            return null;
        }
        String text = labels.get(0).getText();
        return text == null ? null : text.trim();
    }

    private void loginViaCasIfNeeded(String username, String password, String expectedDisplayName) {
        if (isAuthenticatedUiVisible() && (expectedDisplayName == null || expectedDisplayName.equals(getCurrentAuthenticatedDisplayName()))) {
            return;
        }

        openUrl(APP_URL + "/login/casentry");
        wait.until(ExpectedConditions.or(
                ExpectedConditions.urlContains("/cas/login"),
                ExpectedConditions.presenceOfElementLocated(By.id("fm1")),
                ExpectedConditions.presenceOfElementLocated(By.id("new-self-sign")),
                ExpectedConditions.presenceOfElementLocated(By.id("user-toggle"))
        ));

        if (isAuthenticatedUiVisible()) {
            if (expectedDisplayName != null) {
                wait.until(webDriver -> expectedDisplayName.equals(getCurrentAuthenticatedDisplayName()));
            }
            return;
        }

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("fm1")));
        WebElement usernameInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
        usernameInput.clear();
        usernameInput.sendKeys(username);

        WebElement passwordInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("password")));
        passwordInput.clear();
        passwordInput.sendKeys(password);

        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[name='execution']")));

        WebElement submitButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("submitBtn")));
        safeClick(submitButton);

        waitForAuthenticatedUi();
        if (expectedDisplayName != null) {
            wait.until(webDriver -> expectedDisplayName.equals(getCurrentAuthenticatedDisplayName()));
        }
    }

    private void loginAs(String username, String password, String expectedDisplayName) {
        openUrl(APP_URL + "/user");
        loginViaCasIfNeeded(username, password, expectedDisplayName);
    }

    private boolean isLogoutStateVisible() {
        String currentUrl = driver.getCurrentUrl();
        return currentUrl.contains("/cas/logout")
                || currentUrl.contains("/logged-out")
                || !driver.findElements(By.xpath("//*[contains(normalize-space(.), 'Vous êtes bien déconnecté')]" )).isEmpty()
                || !driver.findElements(By.id("fm1")).isEmpty()
                || currentUrl.contains("/cas/login");
    }

    private void submitLogoutForm(By buttonLocator) {
        WebElement logoutButton = wait.until(ExpectedConditions.presenceOfElementLocated(buttonLocator));
        js.executeScript(
                "const button = arguments[0];" +
                        "const form = button.closest('form');" +
                        "if (form && typeof form.requestSubmit === 'function') { form.requestSubmit(); }" +
                        "else if (form) { form.submit(); }" +
                        "else { button.click(); }",
                logoutButton
        );
    }

    private void logoutCurrentUser() {
        if (!isAuthenticatedUiVisible()) {
            return;
        }
        String currentUrl = driver.getCurrentUrl();
        if (driver.findElements(By.id("link-disconnect")).isEmpty() || !driver.findElement(By.id("link-disconnect")).isDisplayed()) {
            safeClick(By.id("user-toggle"));
        }
        if (!driver.findElements(By.id("link-disconnect")).isEmpty()) {
            submitLogoutForm(By.id("link-disconnect"));
        } else if (!driver.findElements(By.id("link-disconnect2")).isEmpty()) {
            submitLogoutForm(By.id("link-disconnect2"));
        } else {
            Assertions.fail("Le bouton de déconnexion est introuvable.");
        }

        wait.until(webDriver -> isLogoutStateVisible() || !currentUrl.equals(webDriver.getCurrentUrl()));

        if (!isLogoutStateVisible()) {
            openUrl(APP_URL + "/user");
            wait.until(webDriver -> isLogoutStateVisible() || !isAuthenticatedUiVisible());
        }

        driver.manage().deleteAllCookies();
        js.executeScript("window.localStorage.clear(); window.sessionStorage.clear();");
    }

    private void waitForSignCompletion() {
        wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(normalize-space(.), 'Vous avez signé ce document')]")),
                ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(normalize-space(.), 'Télécharger le document signé')]")),
                ExpectedConditions.invisibilityOfElementLocated(By.id("addSignButton2"))
        ));
        waitForUiToSettle();
    }

    private void waitForUserParamsPage() {
        wait.until(ExpectedConditions.urlContains("/user/users"));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("userParamsForm")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("saveButton")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("signImageBase64")));
        wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(By.id("vanilla-upload")),
                ExpectedConditions.presenceOfElementLocated(By.id("canvas"))
        ));
        waitForUiToSettle();
    }

    private void selectUserInSlimSelect(String selectId, String email) {
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id(selectId)));
        wait.until(webDriver -> Boolean.TRUE.equals(js.executeScript(
                "const select = document.getElementById(arguments[0]);" +
                        "return !!(select && select.slim);",
                selectId
        )));

        String result = (String) js.executeAsyncScript(
                "const selectId = arguments[0];" +
                        "const email = arguments[1];" +
                        "const callback = arguments[arguments.length - 1];" +
                        "const select = document.getElementById(selectId);" +
                        "if (!select || !select.slim) { callback('ERROR|SlimSelect introuvable pour ' + selectId); return; }" +
                        "fetch('/user/users/search-user?searchString=' + encodeURIComponent(email), { credentials: 'same-origin' })" +
                        "  .then(response => response.ok ? response.json() : Promise.reject(new Error('HTTP ' + response.status)))" +
                        "  .then(json => {" +
                        "      const options = (json || []).map(user => {" +
                        "          const mail = user.mail;" +
                        "          const displayName = user.displayName;" +
                        "          let text = mail;" +
                        "          if (displayName) {" +
                        "              text = displayName !== mail ? displayName + ' (' + mail + ')' : displayName;" +
                        "          }" +
                        "          return { text: text, value: mail };" +
                        "      });" +
                        "      const matchingOption = options.find(option => option.value === email);" +
                        "      if (!matchingOption) { callback('ERROR|Utilisateur introuvable pour ' + email); return; }" +
                        "      select.slim.setData(options);" +
                        "      select.slim.setSelected(email);" +
                        "      callback('OK|' + select.slim.getSelected().join(','));" +
                        "  })" +
                        "  .catch(error => callback('ERROR|' + (error && error.message ? error.message : String(error))));",
                selectId,
                email
        );

        if (result == null || !result.startsWith("OK|")) {
            Assertions.fail("La sélection du destinataire a échoué pour " + email + " : " + result);
        }

        wait.until(webDriver -> Boolean.TRUE.equals(js.executeScript(
                "const select = document.getElementById(arguments[0]);" +
                        "return !!(select && select.slim && select.slim.getSelected().includes(arguments[1]));",
                selectId,
                email
        )));
        waitForUiToSettle();
    }

    private void drawSignatureOnCanvas() {
        WebElement canvas = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("canvas")));
        wait.until(webDriver -> canvas.getSize().getWidth() > 0 && canvas.getSize().getHeight() > 0);
        js.executeScript(
                "const canvas = arguments[0];" +
                        "const rect = canvas.getBoundingClientRect();" +
                        "const fire = (type, x, y, buttons) => canvas.dispatchEvent(new MouseEvent(type, {" +
                        "clientX: rect.left + x, clientY: rect.top + y, bubbles: true, buttons: buttons}" +
                        "));" +
                        "fire('mousedown', 20, 20, 1);" +
                        "fire('mousemove', 60, 35, 1);" +
                        "fire('mousemove', 110, 45, 1);" +
                        "fire('mouseup', 110, 45, 0);",
                canvas
        );
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

        File file = File.createTempFile("selenium-signature-", ".png");
        ImageIO.write(image, "png", file);
        file.deleteOnExit();
        return file;
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    @Order(1)
    public void esupSignatureTitle() {
        openUrl(APP_URL);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("title")));
        String title = driver.getTitle();
        assertEquals("Esup Signature", title);
    }

    @Test
    @Order(2)
    public void esupSignatureAutoSignImage() throws IOException {
        loginAs(PRIMARY_USERNAME, DEFAULT_TEST_PASSWORD, PRIMARY_DISPLAY_NAME);

        safeClick(By.id("new-self-sign"));
        WebElement fileInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("multipartFiles")));
        fileInput.sendKeys(new ClassPathResource("/dummy.pdf").getFile().getAbsolutePath());

        String previousUrl = driver.getCurrentUrl();
        safeClick(By.id("fast-sign-button"));
        waitForWizardResult(previousUrl);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("page_1")));

        safeClick(By.id("addSignButton2"));
        String signRequestId = (String) js.executeScript("return window.location.href.substring(window.location.href.lastIndexOf('/') + 1);");
        String signBookId = driver.findElement(By.id("content")).getDomAttribute("data-es-signbook-id");

        waitForSignLaunchReady();
        safeClick(By.id("signLaunchButton"));
        wait.until(ExpectedConditions.elementToBeClickable(By.id("link-dashboard")));

        safeClick(By.id("link-dashboard"));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("signbook-" + signBookId)));

        safeClick(By.id("checkbox-signrequest-" + signRequestId));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("deleteMultipleButton")));

        safeClick(By.id("deleteMultipleButton"));
        safeClick(By.cssSelector(".bootbox-accept"));
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("signbook-" + signBookId)));
    }


    @Test
    @Order(3)
    public void esupSignatureFastSignRequest() throws IOException {
        loginAs(PRIMARY_USERNAME, DEFAULT_TEST_PASSWORD, PRIMARY_DISPLAY_NAME);

        safeClick(By.id("new-fast-sign"));
        WebElement fileInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("multipartFiles")));
        fileInput.sendKeys(new ClassPathResource("/dummy.pdf").getFile().getAbsolutePath());

        selectUserInSlimSelect("recipientsEmails-1", PRIMARY_EMAIL);

        String previousUrl = driver.getCurrentUrl();
        safeClick(By.id("send-pending-button"));
        waitForWizardResult(previousUrl);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("page_1")));

        safeClick(By.id("addSignButton2"));
        String signRequestId = (String) js.executeScript("return window.location.href.substring(window.location.href.lastIndexOf('/') + 1);");
        String signBookId = driver.findElement(By.id("content")).getDomAttribute("data-es-signbook-id");

        waitForSignLaunchReady();
        safeClick(By.id("signLaunchButton"));
        wait.until(ExpectedConditions.elementToBeClickable(By.id("link-dashboard")));

        safeClick(By.id("link-dashboard"));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("signbook-" + signBookId)));

        safeClick(By.id("checkbox-signrequest-" + signRequestId));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("deleteMultipleButton")));

        safeClick(By.id("deleteMultipleButton"));
        safeClick(By.cssSelector(".bootbox-accept"));
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("signbook-" + signBookId)));
    }

    @Test
    @Order(4)
    public void esupSignatureCreateSignImage() throws IOException {
        loginAs(PRIMARY_USERNAME, DEFAULT_TEST_PASSWORD, PRIMARY_DISPLAY_NAME);

        openUrl(APP_URL + "/user/users");
        waitForUserParamsPage();

        if (!driver.findElements(By.cssSelector("[id^='deleteSign_']")).isEmpty()) {
            deleteAllCustomSignImages();
            wait.until(webDriver -> webDriver.findElements(By.cssSelector("[id^='deleteSign_']")).isEmpty());
        }

        WebElement uploadInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("vanilla-upload")));
        uploadInput.sendKeys(createTempSignatureImage().getAbsolutePath());
        wait.until(webDriver -> {
            WebElement signImageBase64Input = webDriver.findElement(By.id("signImageBase64"));
            String value = signImageBase64Input.getDomProperty("value");
            return value != null && !value.isBlank();
        });

        WebElement btn = driver.findElement(By.id("saveButton"));
        safeClick(btn);
        wait.until(ExpectedConditions.stalenessOf(btn));

        waitForUiToSettle();
        wait.until(webDriver -> !webDriver.findElements(By.cssSelector("[id^='deleteSign_']")).isEmpty());
        deleteAllCustomSignImages();
        wait.until(webDriver -> webDriver.findElements(By.cssSelector("[id^='deleteSign_']")).isEmpty());
    }

    @Test
    @Order(5)
    public void esupSignatureFastSignRequestBetweenTwoUsers() throws IOException {
        loginAs(PRIMARY_USERNAME, DEFAULT_TEST_PASSWORD, PRIMARY_DISPLAY_NAME);

        safeClick(By.id("new-fast-sign"));
        WebElement fileInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("multipartFiles")));
        fileInput.sendKeys(new ClassPathResource("/dummy.pdf").getFile().getAbsolutePath());

        selectUserInSlimSelect("recipientsEmails-1", SECONDARY_EMAIL);

        String previousUrl = driver.getCurrentUrl();
        WebElement sendPendingButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("send-pending-button")));
        safeClick(sendPendingButton);
        wait.until(webDriver -> !previousUrl.equals(webDriver.getCurrentUrl()) || webDriver.findElements(By.id("send-pending-button")).isEmpty());

        openUrl(APP_URL + "/user/signbooks");
        WebElement signBookRow = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("tr[id^='signbook-']")));
        String signBookId = signBookRow.getDomAttribute("id").replace("signbook-", "");
        WebElement signRequestCheckbox = signBookRow.findElement(By.cssSelector("input[id^='checkbox-signrequest-']"));
        String signRequestId = signRequestCheckbox.getDomAttribute("id").replace("checkbox-signrequest-", "");
        logoutCurrentUser();
        restartBrowserSession();

        loginAs(SECONDARY_USERNAME, DEFAULT_TEST_PASSWORD, SECONDARY_DISPLAY_NAME);
        openUrl(APP_URL + "/user/signrequests/" + signRequestId);
        wait.until(ExpectedConditions.elementToBeClickable(By.id("addSignButton2")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("page_1")));

        safeClick(By.id("addSignButton2"));
        waitForSignLaunchReady();
        safeClick(By.id("signLaunchButton"));
        waitForSignCompletion();
        logoutCurrentUser();
        restartBrowserSession();

        loginAs(PRIMARY_USERNAME, DEFAULT_TEST_PASSWORD, PRIMARY_DISPLAY_NAME);
        openUrl(APP_URL + "/user/signbooks");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("signbook-" + signBookId)));

        safeClick(By.id("checkbox-signrequest-" + signRequestId));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("deleteMultipleButton")));
        safeClick(By.id("deleteMultipleButton"));
        safeClick(By.cssSelector(".bootbox-accept"));
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("signbook-" + signBookId)));
    }

    // Méthode de comparaison d'images
    private static boolean compareImages(BufferedImage imgA, BufferedImage imgB) {
        // Vérifier la taille de l'image
        if (imgA.getWidth() != imgB.getWidth() || imgA.getHeight() != imgB.getHeight()) {
            return false;
        }

        // Comparer pixel par pixel
        for (int y = 0; y < imgA.getHeight(); y++) {
            for (int x = 0; x < imgA.getWidth(); x++) {
                if (imgA.getRGB(x, y) != imgB.getRGB(x, y)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isPortAvailable(int port) {
        try (ServerSocket ignored = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
