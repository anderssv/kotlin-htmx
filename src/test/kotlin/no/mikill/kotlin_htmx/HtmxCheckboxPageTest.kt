package no.mikill.kotlin_htmx

import io.github.bonigarcia.wdm.WebDriverManager
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
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

    private lateinit var driver1: WebDriver
    private lateinit var driver2: WebDriver
    private lateinit var server: EmbeddedServer<*, *>
    private val checkboxPageUrl = "/demo/htmx/checkboxes"
    private var serverUrl: String? = null

    @Before
    fun setUp() {
        // Start KTor server
        server = embeddedServer(Netty, port = 0, host = "0.0.0.0") {
            module()
        }.start(wait = false)

        val port = runBlocking { server.engine.resolvedConnectors().first().port }
        serverUrl = "http://localhost:$port"

        // Set up WebDriver
        WebDriverManager.chromedriver().setup()

        // Configure Chrome options for headless mode
        val options = ChromeOptions()
        options.addArguments("--headless")
        options.addArguments("--disable-gpu")
        options.addArguments("--no-sandbox")
        options.addArguments("--disable-dev-shm-usage")

        // Initialize two drivers with options
        driver1 = ChromeDriver(options)
        driver2 = ChromeDriver(options)

        // Set implicit wait time
        driver1.manage().timeouts().implicitlyWait(Duration.ofSeconds(10))
        driver2.manage().timeouts().implicitlyWait(Duration.ofSeconds(10))
    }

    @After
    fun tearDown() {
        // Close the browsers
        driver1.quit()
        driver2.quit()

        // Stop the server
        server.stop(1000, 2000)
    }

    @Test
    fun testHtmxCheckboxPage() {
        // Navigate to the checkbox page
        driver1.get(serverUrl!! + checkboxPageUrl)

        // Wait for the page to load
        val wait1 = WebDriverWait(driver1, Duration.ofSeconds(10))
        wait1.until(ExpectedConditions.presenceOfElementLocated(By.tagName("input")))

        // Verify the page title contains expected text
        val pageTitle = driver1.title
        assertThat(pageTitle).contains("HTMX + SSE Checkboxes demo")

        // Verify page content contains expected text
        val pageContent = driver1.findElement(By.tagName("body")).text
        assertThat(pageContent).contains("This page shows synchronization between browser windows")

        // Find the first checkbox and get its initial state
        val checkboxId = "0"
        val firstCheckbox = driver1.findElement(By.id(checkboxId))
        val initialState = firstCheckbox.isSelected

        // Open the same page in the second browser
        driver2.get(serverUrl!! + checkboxPageUrl)
        val wait2 = WebDriverWait(driver2, Duration.ofSeconds(10))
        wait2.until(ExpectedConditions.presenceOfElementLocated(By.tagName("input")))

        // Verify the initial state of the checkbox is the same in both browsers
        val secondBrowserCheckbox = driver2.findElement(By.id(checkboxId))
        assertThat(secondBrowserCheckbox.isSelected).isEqualTo(initialState)

        // Click the checkbox in the first browser
        firstCheckbox.click()

        // Wait for the update to propagate to the second browser
        Thread.sleep(1000)

        // Re-find the checkbox in both browsers (to avoid stale element reference)
        val updatedCheckbox1 = driver1.findElement(By.id(checkboxId))
        val updatedCheckbox2 = driver2.findElement(By.id(checkboxId))

        // Verify the checkbox state has changed in the first browser
        val newState1 = updatedCheckbox1.isSelected
        assertThat(newState1).isNotEqualTo(initialState)

        // Verify the checkbox state has also changed in the second browser
        val newState2 = updatedCheckbox2.isSelected
        assertThat(newState2).isEqualTo(newState1)
        assertThat(newState2).isNotEqualTo(initialState)
    }
}
