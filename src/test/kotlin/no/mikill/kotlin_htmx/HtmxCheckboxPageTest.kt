package no.mikill.kotlin_htmx

import io.github.bonigarcia.wdm.WebDriverManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

class HtmxCheckboxPageTest {

    private lateinit var driver: WebDriver
    private val baseUrl = "http://0.0.0.0:8080"
    private val checkboxPageUrl = "$baseUrl/demo/htmx/checkboxes"

    @Before
    fun setUp() {
        // Set up WebDriver
        WebDriverManager.chromedriver().setup()

        // Configure Chrome options for headless mode
        val options = ChromeOptions()
        options.addArguments("--headless")
        options.addArguments("--disable-gpu")
        options.addArguments("--no-sandbox")
        options.addArguments("--disable-dev-shm-usage")

        // Initialize the driver with options
        driver = ChromeDriver(options)

        // Set implicit wait time
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10))
    }

    @After
    fun tearDown() {
        // Close the browser
        driver.quit()
    }

    @Test
    fun testHtmxCheckboxPage() {
        // Navigate to the checkbox page
        driver.get(checkboxPageUrl)

        // Wait for the page to load
        val wait = WebDriverWait(driver, Duration.ofSeconds(10))
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("input")))

        // Verify the page title contains expected text
        val pageTitle = driver.title
        assertThat(pageTitle).contains("HTMX + SSE Checkboxes demo")

        // Verify page content contains expected text
        val pageContent = driver.findElement(By.tagName("body")).text
        assertThat(pageContent).contains("This page shows synchronization between browser windows")

        // Find the first checkbox and get its initial state
        val checkboxId = "0"
        val firstCheckbox = driver.findElement(By.id(checkboxId))
        val initialState = firstCheckbox.isSelected

        // Click the checkbox
        firstCheckbox.click()

        // Wait a moment for HTMX to update the DOM
        Thread.sleep(1000)

        // Re-find the checkbox (to avoid stale element reference)
        val updatedCheckbox = driver.findElement(By.id(checkboxId))

        // Verify the checkbox state has changed
        val newState = updatedCheckbox.isSelected
        assertThat(newState).isNotEqualTo(initialState)
    }
}
