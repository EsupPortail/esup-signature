package org.esupportail.esupsignature;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
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
import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SeleniumTest {

    private static final Logger logger = LoggerFactory.getLogger(SeleniumTest.class);

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
            driver = new ChromeDriver(chromeOptions);
        } else {
            driver = new ChromeDriver();
        }
        wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        js = (JavascriptExecutor) driver;
        driver.manage().window().setSize(new  org.openqa.selenium.Dimension(1920, 1016));
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
        driver.get("http://localhost:7070");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("title")));
        String title = driver.getTitle();
        assertEquals("Esup Signature", title);
    }

    @Test
    @Order(2)
    public void esupSignatureAutoSignImage() throws IOException {
        // Naviguer vers la page utilisateur
        driver.get("http://localhost:7070/user");
        // Cliquer sur le bouton "new-self-sign"
        wait.until(ExpectedConditions.elementToBeClickable(By.id("new-self-sign"))).click();
        // Sélectionner le fichier à téléverser
        WebElement fileInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("multipartFiles")));
        fileInput.sendKeys(new ClassPathResource("/dummy.pdf").getFile().getAbsolutePath());
        // Cliquer sur le bouton "fast-sign-button"
        WebElement fastSignButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("fast-sign-button")));
        fastSignButton.click();
        try {
            wait.until(ExpectedConditions.invisibilityOf(fastSignButton));
        } catch (Exception e) {
            logger.warn("Element non trouvé dans le délai spécifié.", e);
        }
        // Cliquer sur le bouton "addSignButton2"
        wait.until(ExpectedConditions.elementToBeClickable(By.id("addSignButton2"))).click();
        // Exécuter du JavaScript pour récupérer "signRequestId"
        String signRequestId = (String) js.executeScript("return window.location.href.substring(window.location.href.lastIndexOf('/') + 1);");
        // Récupérer l'attribut "signBookId"
        String signBookId = driver.findElement(By.id("content")).getDomAttribute("data-es-signbook-id");
        // Cliquer sur le bouton "signLaunchButton"
        wait.until(ExpectedConditions.elementToBeClickable(By.id("signLaunchButton"))).click();
        // Cliquer sur le bouton "checkValidateSignButtonEnd"
        wait.until(ExpectedConditions.elementToBeClickable(By.id("checkValidateSignButtonEnd")));
        WebElement btn = driver.findElement(By.id("checkValidateSignButtonEnd"));
        btn.click();
        wait.until(ExpectedConditions.stalenessOf(btn));
        // Cliquer sur le bouton "link-dashboard"
        wait.until(ExpectedConditions.elementToBeClickable(By.id("link-dashboard"))).click();
        // Attendre la présence de l'élément "signbook-${signBookId}"
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("signbook-" + signBookId)));
        // Cliquer sur le bouton "checkbox-signrequest-${signRequestId}"
        wait.until(ExpectedConditions.elementToBeClickable(By.id("checkbox-signrequest-" + signRequestId))).click();
        // Attendre la visibilité du bouton "deleteMultipleButton"
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("deleteMultipleButton")));
        // Cliquer sur le bouton "deleteMultipleButton"
        wait.until(ExpectedConditions.elementToBeClickable(By.id("deleteMultipleButton"))).click();
        // Cliquer sur le bouton "bootbox-accept"
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".bootbox-accept"))).click();
        // Attendre que l'élément "signbook-${signBookId}" ne soit plus présent
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("signbook-" + signBookId)));
    }


    @Test
    @Order(3)
    public void esupSignatureFastSignRequest() throws IOException {
        driver.get("http://localhost:7070/user");
        // Click "new-fast-sign" button
        wait.until(ExpectedConditions.elementToBeClickable(By.id("new-fast-sign"))).click();
        // Type file path
        WebElement fileInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("multipartFiles")));
        fileInput.sendKeys(new ClassPathResource("/dummy.pdf").getFile().getAbsolutePath());
        String dataId = driver.findElement(By.id("recipientsEmails-1")).getDomAttribute("data-id");
        WebElement selectContainer = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("div[data-id='" + dataId + "']")));
        selectContainer.click();
        WebElement searchInput = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.cssSelector("div.ss-content[data-id='" + dataId + "'] .ss-search > input")
                )
        );
        searchInput.click();
        searchInput.sendKeys("justin");
        WebElement option = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.cssSelector("div.ss-content[data-id='" + dataId + "'] .ss-list .ss-option")
                )
        );
        option.click();
        // Click "send-pending-button"
        WebElement sendPendingButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("send-pending-button")));
        sendPendingButton.click();
        wait.until(ExpectedConditions.invisibilityOf(sendPendingButton));
        // Click "addSignButton2"
        wait.until(ExpectedConditions.elementToBeClickable(By.id("addSignButton2"))).click();
        // Execute JavaScript to store "signRequestId"
        String signRequestId = (String) js.executeScript("return window.location.href.substring(window.location.href.lastIndexOf('/') + 1);");
        // Store attribute "signBookId"
        String signBookId = driver.findElement(By.id("content")).getDomAttribute("data-es-signbook-id");
        // Click "signLaunchButton"
        wait.until(ExpectedConditions.elementToBeClickable(By.id("signLaunchButton"))).click();
        // Click "checkValidateSignButtonEnd"
        wait.until(ExpectedConditions.elementToBeClickable(By.id("checkValidateSignButtonEnd")));
        WebElement btn = driver.findElement(By.id("checkValidateSignButtonEnd"));
        btn.click();
        wait.until(ExpectedConditions.stalenessOf(btn));
        // Click "link-dashboard" button
        wait.until(ExpectedConditions.elementToBeClickable(By.id("link-dashboard"))).click();
        // Wait for element present "signbook-${signBookId}"
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("signbook-" + signBookId)));
        // Click "checkbox-signrequest-${signRequestId}"
        wait.until(ExpectedConditions.elementToBeClickable(By.id("checkbox-signrequest-" + signRequestId))).click();
        // Wait for element visible "deleteMultipleButton"
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("deleteMultipleButton")));
        // Click "deleteMultipleButton"
        wait.until(ExpectedConditions.elementToBeClickable(By.id("deleteMultipleButton"))).click();
        // Click "bootbox-accept"
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".bootbox-accept"))).click();
        // Wait for element not present "signbook-${signBookId}"
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("signbook-" + signBookId)));
    }

    @Test
    @Order(4)
    public void esupSignatureCreateSignImage() {
        driver.get("http://localhost:7070/user");

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("user-toggle"))).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("link-user-params"))).click();

        WebElement canvas = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("canvas")));

        Actions actions = new Actions(driver);
        actions.moveToElement(canvas, -1, -1).click().perform();
        actions.moveToElement(canvas, 0, 0).click().perform();
        actions.moveToElement(canvas, 1, 1).click().perform();
        wait.until(ExpectedConditions.elementToBeClickable(By.id("saveButton")));
        WebElement btn = driver.findElement(By.id("saveButton"));
        btn.click();
        wait.until(ExpectedConditions.stalenessOf(btn));
        try {
            // Attendre que la modal "wait" soit visible
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("toast-backdrop")));
            // Attendre que la modal "wait" ne soit plus visible
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("toast-backdrop")));
        } catch (Exception e) {
            // Click "checkValidateSignButtonEnd"
            System.out.println("Element non trouvé dans le délai spécifié.");
        }

        List<WebElement> elements = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.cssSelector("[id^='deleteSign_']")));
        for(WebElement element : elements) {
            element.click();
            wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".bootbox-accept"))).click();
        }
        wait.until(ExpectedConditions.invisibilityOfAllElements(elements));
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
