package org.esupportail.esupsignature;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.time.Duration;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EsupSignatureApplication.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SeleniumTest {

    private WebDriver driver;
    private WebDriverWait wait;
    private JavascriptExecutor js;

    @Before
    public void setUp() {
        System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");
        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        js = (JavascriptExecutor) driver;
        driver.manage().window().setSize(new  org.openqa.selenium.Dimension(1920, 1016));
    }

    @After
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void testA_esupSignatureTitle() {
        driver.get("http://localhost:7070");
        String title = driver.getTitle();
        assertEquals("Esup Signature", title);
    }

    @Test
    public void testB_esupSignatureAutoSignImage() throws IOException {
        // Naviguer vers la page utilisateur
        driver.get("http://localhost:7070/user");

        // Cliquer sur le bouton "new-self-sign"
        wait.until(ExpectedConditions.elementToBeClickable(By.id("new-self-sign"))).click();

        // Sélectionner le fichier à téléverser
        WebElement fileInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("multipartFiles")));
        fileInput.sendKeys(new ClassPathResource("/dummy.pdf").getFile().getAbsolutePath());

        // Cliquer sur le bouton "fast-sign-button"
        wait.until(ExpectedConditions.elementToBeClickable(By.id("fast-sign-button"))).click();

        // Cliquer sur le bouton "addSignButton"
        wait.until(ExpectedConditions.elementToBeClickable(By.id("addSignButton"))).click();

        // Exécuter du JavaScript pour récupérer "signRequestId"
        String signRequestId = (String) js.executeScript("return window.location.href.substring(window.location.href.lastIndexOf('/') + 1);");

        // Récupérer l'attribut "signBookId"
        String signBookId = driver.findElement(By.id("content")).getAttribute("data-es-signbook-id");

        // Cliquer sur le bouton "signLaunchButton"
        wait.until(ExpectedConditions.elementToBeClickable(By.id("signLaunchButton"))).click();

        // Cliquer sur le bouton "checkValidateSignButtonEnd"
        wait.until(ExpectedConditions.elementToBeClickable(By.id("checkValidateSignButtonEnd"))).click();

        try {
            // Attendre que la modal "wait" soit visible
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("wait")));

            // Attendre que la modal "wait" ne soit plus visible
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("wait")));
        } catch (Exception e) {
            // Click "checkValidateSignButtonEnd"
            System.out.println("Element non trouvé dans le délai spécifié.");
        }

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
    public void testC_esupSignatureFastSignRequest() throws IOException {
        driver.get("http://localhost:7070/user");

        // Click "new-fast-sign" button
        wait.until(ExpectedConditions.elementToBeClickable(By.id("new-fast-sign"))).click();

        // Type file path
        WebElement fileInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("multipartFiles")));
        fileInput.sendKeys(new ClassPathResource("/dummy.pdf").getFile().getAbsolutePath());

        // Click the recipient field
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("#recipientsEmails-1 + div"))).click();

        // Type "justin"
        WebElement searchInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".ss-search > input")));
        searchInput.sendKeys("justin");

        // Select the recipient from the list
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".ss-list > .ss-option"))).click();

        // Click "send-pending-button"
        wait.until(ExpectedConditions.elementToBeClickable(By.id("send-pending-button"))).click();

        // Click "addSignButton"
        wait.until(ExpectedConditions.elementToBeClickable(By.id("addSignButton"))).click();

        // Execute JavaScript to store "signRequestId"
        String signRequestId = (String) js.executeScript("return window.location.href.substring(window.location.href.lastIndexOf('/') + 1);");

        // Store attribute "signBookId"
        String signBookId = driver.findElement(By.id("content")).getAttribute("data-es-signbook-id");

        // Click "signLaunchButton"
        wait.until(ExpectedConditions.elementToBeClickable(By.id("signLaunchButton"))).click();

        // Click "checkValidateSignButtonEnd"
        wait.until(ExpectedConditions.elementToBeClickable(By.id("checkValidateSignButtonEnd"))).click();

        try {
            // Attendre que la modal "wait" soit visible
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("wait")));

            // Attendre que la modal "wait" ne soit plus visible
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("wait")));
        } catch (Exception e) {
            // Click "checkValidateSignButtonEnd"
            System.out.println("Element non trouvé dans le délai spécifié.");
        }

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
}