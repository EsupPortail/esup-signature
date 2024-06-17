package org.esupportail.esupsignature;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Order;
import org.junit.runner.RunWith;
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
        driver.manage().window().maximize();    }

    @After
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    @Order(1)
    public void esupSignatureTitle() {
        driver.get("http://localhost:8080");
        String title = driver.getTitle();
        assertEquals("Esup Signature", title);
    }

    @Test
    @Order(2)
    public void esupSignatureAutoSignImage() throws IOException {
        driver.get("http://localhost:8080/user");
        driver.manage().window().setSize(new  org.openqa.selenium.Dimension(1920, 1016));

        // Click "new-self-sign" button
        wait.until(ExpectedConditions.elementToBeClickable(By.id("new-self-sign"))).click();

        // Type file path
        WebElement fileInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("multipartFiles")));
        fileInput.sendKeys(new ClassPathResource("/dummy.pdf").getFile().getAbsolutePath());

        // Click "fast-sign-button" button
        wait.until(ExpectedConditions.elementToBeClickable(By.id("fast-sign-button"))).click();

        // Click "addSignButton" button
        wait.until(ExpectedConditions.elementToBeClickable(By.id("addSignButton"))).click();

        // Execute JavaScript to store "signRequestId"
        String signRequestId = (String) js.executeScript("return window.location.href.substring(window.location.href.lastIndexOf('/') + 1);");

        // Store attribute "signBookId"
        String signBookId = driver.findElement(By.id("content")).getAttribute("data-es-signbook-id");

        // Click "signLaunchButton" button
        wait.until(ExpectedConditions.elementToBeClickable(By.id("signLaunchButton"))).click();

        // Click "checkValidateSignButtonEnd" button
        wait.until(ExpectedConditions.elementToBeClickable(By.id("checkValidateSignButtonEnd"))).click();

        // Click "link-dashboard" button
        wait.until(ExpectedConditions.elementToBeClickable(By.id("link-dashboard"))).click();

        // Wait for element present "signbook-${signBookId}"
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("signbook-" + signBookId)));

        // Click "checkbox-signrequest-${signRequestId}" button
        wait.until(ExpectedConditions.elementToBeClickable(By.id("checkbox-signrequest-" + signRequestId))).click();

        // Wait for element visible "deleteMultipleButton"
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("deleteMultipleButton")));

        // Click "deleteMultipleButton" button
        wait.until(ExpectedConditions.elementToBeClickable(By.id("deleteMultipleButton"))).click();

        // Click "bootbox-accept" button
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".bootbox-accept"))).click();

        // Wait for element not present "signbook-${signBookId}"
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("signbook-" + signBookId)));
    }

    @Test
    @Order(3)
    public void esupSignatureFastSignRequest() throws IOException {
        driver.get("http://localhost:8080/user");
        driver.manage().window().setSize(new  org.openqa.selenium.Dimension(1920, 1016));

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

        // Click "link-dashboard"
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