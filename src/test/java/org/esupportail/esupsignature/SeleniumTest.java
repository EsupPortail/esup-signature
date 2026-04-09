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

import java.awt.image.BufferedImage;
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
    private static final String CAS_USERNAME = "0";
    private static final String CAS_PASSWORD = "password";
    private static final String CURRENT_USER_EMAIL = "0@example.org";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private WebDriver driver;
    private WebDriverWait wait;
    private JavascriptExecutor js;

    @BeforeEach
    public void setUp() {
        boolean portAvailable = isPortAvailable(7070);
        if (portAvailable) {
            SpringApplication.run(EsupSignatureApplication.class, "--server.port=7070");
        }
        System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");
        ChromeOptions chromeOptions = new ChromeOptions();
        String display = System.getenv("DISPLAY");
        if (display == null || display.isEmpty()) {
            logger.warn("Headless mode activated");
            chromeOptions.addArguments("--remote-debugging-port=9222");
            chromeOptions.addArguments("--headless");
            chromeOptions.addArguments("--no-sandbox");
            chromeOptions.addArguments("--disable-dev-shm-usage");
            chromeOptions.addArguments("--disable-gpu");
            chromeOptions.addArguments("--disable-software-rasterizer");
            chromeOptions.addArguments("--window-size=1920,1016");
            driver = new ChromeDriver(chromeOptions);
        } else {
            driver = new ChromeDriver();
        }
        wait = new WebDriverWait(driver, DEFAULT_TIMEOUT);
        js = (JavascriptExecutor) driver;
        driver.manage().window().setSize(new  org.openqa.selenium.Dimension(1920, 1016));
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
            safeClick(currentButtons.get(0));
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

    private void loginViaCasIfNeeded() {
        waitForUiToSettle();
        wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(By.id("new-self-sign")),
                ExpectedConditions.presenceOfElementLocated(By.id("new-fast-sign")),
                ExpectedConditions.presenceOfElementLocated(By.id("user-toggle")),
                ExpectedConditions.presenceOfElementLocated(By.id("fm1")),
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("a[href*='/login/casentry']")),
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("a[href*='/login/shibentry']"))
        ));

        if (isAuthenticatedUiVisible()) {
            return;
        }

        if (!driver.findElements(By.cssSelector("a[href*='/login/shibentry']")).isEmpty()) {
            safeClick(By.cssSelector("a[href*='/login/shibentry']"));
            waitForAuthenticatedUi();
            return;
        }

        if (!driver.findElements(By.cssSelector("a[href*='/login/casentry']")).isEmpty()) {
            safeClick(By.cssSelector("a[href*='/login/casentry']"));
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.presenceOfElementLocated(By.id("fm1")),
                    ExpectedConditions.presenceOfElementLocated(By.id("new-self-sign")),
                    ExpectedConditions.presenceOfElementLocated(By.id("user-toggle"))
            ));
            if (isAuthenticatedUiVisible()) {
                return;
            }
        }

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("fm1")));
        WebElement usernameInput = wait.until(ExpectedConditions.elementToBeClickable(By.id("username")));
        usernameInput.clear();
        usernameInput.sendKeys(CAS_USERNAME);

        WebElement passwordInput = wait.until(ExpectedConditions.elementToBeClickable(By.id("password")));
        passwordInput.clear();
        passwordInput.sendKeys(CAS_PASSWORD);

        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[name='execution']")));

        WebElement submitButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("submitBtn")));
        safeClick(submitButton);

        waitForAuthenticatedUi();
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
        openUrl(APP_URL + "/user");
        loginViaCasIfNeeded();

        safeClick(By.id("new-self-sign"));
        WebElement fileInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("multipartFiles")));
        fileInput.sendKeys(new ClassPathResource("/dummy.pdf").getFile().getAbsolutePath());

        String previousUrl = driver.getCurrentUrl();
        safeClick(By.id("fast-sign-button"));
        waitForWizardResult(previousUrl);

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
        openUrl(APP_URL + "/user");
        loginViaCasIfNeeded();

        safeClick(By.id("new-fast-sign"));
        WebElement fileInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("multipartFiles")));
        fileInput.sendKeys(new ClassPathResource("/dummy.pdf").getFile().getAbsolutePath());

        String dataId = driver.findElement(By.id("recipientsEmails-1")).getDomAttribute("data-id");
        WebElement selectContainer = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("div[data-id='" + dataId + "']")));
        safeClick(selectContainer);
        WebElement searchInput = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.cssSelector("div.ss-content[data-id='" + dataId + "'] .ss-search > input")
                )
        );
        safeClick(searchInput);
        searchInput.sendKeys(CURRENT_USER_EMAIL);
        WebElement option = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.cssSelector("div.ss-content[data-id='" + dataId + "'] .ss-list .ss-option")
                )
        );
        safeClick(option);

        String previousUrl = driver.getCurrentUrl();
        safeClick(By.id("send-pending-button"));
        waitForWizardResult(previousUrl);

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
    public void esupSignatureCreateSignImage() {
        openUrl(APP_URL + "/user");
        loginViaCasIfNeeded();

        safeClick(By.id("user-toggle"));

        safeClick(By.id("link-user-params"));
        waitForUiToSettle();

        WebElement canvas = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("canvas")));

        Actions actions = new Actions(driver);
        actions.moveToElement(canvas, -1, -1).click().perform();
        actions.moveToElement(canvas, 0, 0).click().perform();
        actions.moveToElement(canvas, 1, 1).click().perform();

        int initialDeleteButtonCount = driver.findElements(By.cssSelector("[id^='deleteSign_']")).size();
        WebElement btn = driver.findElement(By.id("saveButton"));
        safeClick(btn);
        wait.until(ExpectedConditions.stalenessOf(btn));

        waitForUiToSettle();
        wait.until(webDriver -> webDriver.findElements(By.cssSelector("[id^='deleteSign_']")).size() > initialDeleteButtonCount);
        deleteAllCustomSignImages();
        wait.until(webDriver -> webDriver.findElements(By.cssSelector("[id^='deleteSign_']")).isEmpty());
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
