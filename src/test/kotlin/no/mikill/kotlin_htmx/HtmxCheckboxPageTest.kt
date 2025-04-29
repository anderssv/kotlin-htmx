package no.mikill.kotlin_htmx

import io.github.bonigarcia.wdm.WebDriverManager
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.openqa.selenium.*
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration
import kotlin.random.Random


class HtmxCheckboxPageTest {

    private val headless = true
    private lateinit var driver1: WebDriver
    private lateinit var driver2: WebDriver
    private lateinit var server: EmbeddedServer<*, *>
    private val checkboxPageUrl = "/demo/htmx/checkboxes"
    private var serverUrl: String? = null

    @Before
    fun setUp() {
        fun getScreenDimensions(driver: WebDriver): Pair<Int, Int> {
            driver.manage().window().maximize()
            val js = driver as JavascriptExecutor
            val screenWidth = js.executeScript("return window.screen.width") as Long
            val screenHeight = js.executeScript("return window.screen.height") as Long
            return Pair(screenWidth.toInt(), screenHeight.toInt())
        }

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
        if (headless) options.addArguments("--headless")
        options.addArguments("--disable-gpu")
        options.addArguments("--no-sandbox")
        options.addArguments("--disable-dev-shm-usage")

        // Initialize two drivers with options
        driver1 = ChromeDriver(options)
        driver2 = ChromeDriver(options)

        if (!headless) { // Only bother if windows showing
            val (screenWidth, screenHeight) = getScreenDimensions(driver1)
            driver1.manage().window().size = Dimension(screenWidth / 2, screenHeight)
            driver2.manage().window().size = Dimension(screenWidth / 2, screenHeight)
            driver1.manage().window().position = Point(0, 0)
            driver2.manage().window().position = Point(screenWidth / 2, 0)
        }

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
        fun WebDriver.openAndScrollToCheckbox() {
            get(serverUrl!! + checkboxPageUrl)
            if (!headless) findElement(By.id("1"))
                .also { (this as JavascriptExecutor).executeScript("arguments[0].scrollIntoView(true)", it) }
        }

        driver1.openAndScrollToCheckbox()

        // Wait for the page to load
        val wait1 = WebDriverWait(driver1, Duration.ofSeconds(10))
        wait1.until(ExpectedConditions.presenceOfElementLocated(By.tagName("input")))

        // Verify the page title contains expected text
        val pageTitle = driver1.title
        assertThat(pageTitle).contains("HTMX + SSE Checkboxes demo")

        // Verify page content contains expected text
        val pageContent = driver1.findElement(By.tagName("body")).text
        assertThat(pageContent).contains("This page shows synchronization between browser windows")

        // Open the same page in the second browser
        driver2.openAndScrollToCheckbox()
        val wait2 = WebDriverWait(driver2, Duration.ofSeconds(10))
        wait2.until(ExpectedConditions.presenceOfElementLocated(By.tagName("input")))

        // Generate 20 random checkbox IDs (between 0 and 600)
        val numberOfCheckboxes = 600
        val randomCheckboxIds = generateSequence { Random.nextInt(0, numberOfCheckboxes) }
            .distinct()
            .take(20)
            .map { it.toString() }
            .toList()

        // Store initial states of all checkboxes
        val initialStates = randomCheckboxIds.associateWith { checkboxId ->
            val checkbox1 = driver1.findElement(By.id(checkboxId))
            val initialState = checkbox1.isSelected

            // Verify the initial state is the same in both browsers
            val checkbox2 = driver2.findElement(By.id(checkboxId))
            assertThat(checkbox2.isSelected).isEqualTo(initialState)

            initialState
        }

        // First click all checkboxes - re-find each element before clicking to avoid stale element issues
        randomCheckboxIds.forEach { checkboxId ->
            // Re-find the element before each click to avoid stale element references
            val checkbox1 = driver1.findElement(By.id(checkboxId))
            checkbox1.click()
            // Small wait between clicks to allow the page to update
            Thread.sleep(if (!headless) 500 else 5)
        }

        // Wait once for all updates to fully propagate to the second browser
        Thread.sleep(1000)

        // Then verify all checkboxes
        randomCheckboxIds.forEach { checkboxId ->
            val initialState = initialStates[checkboxId]!!

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
}
